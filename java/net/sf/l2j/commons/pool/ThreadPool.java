package net.sf.l2j.commons.pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;

/**
 * This class handles thread pooling system. It relies on two ThreadPoolExecutor arrays, which poolers number is generated using config.
 * <p>
 * Those arrays hold following pools :
 * </p>
 * <ul>
 * <li>Scheduled pool keeps a track about incoming, future events.</li>
 * <li>Instant pool handles short-life events.</li>
 * </ul>
 */
public final class ThreadPool
{
	private ThreadPool()
	{
		throw new IllegalStateException("Utility class");
	}
	
	protected static final CLogger LOGGER = new CLogger(ThreadPool.class.getName());
	
	private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
	
	private static final ScheduledThreadPoolExecutor _scheduledPools = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_POOL_COUNT, new ThreadProvider("ScheduledThread"), new ThreadPoolExecutor.CallerRunsPolicy());
	private static final ThreadPoolExecutor _instantPools = new ThreadPoolExecutor(Config.INSTANT_THREAD_POOL_COUNT, Integer.MAX_VALUE, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadProvider("Thread"));
	
	/**
	 * Init the different pools, based on Config. It is launched only once, on Gameserver instance.
	 */
	public static void init()
	{
		LOGGER.info("ThreadPool: Initialized");
		
		// Configure ScheduledThreadPoolExecutor.
		_scheduledPools.setRejectedExecutionHandler(new RejectedExecutionHandlerImpl());
		_scheduledPools.setRemoveOnCancelPolicy(true);
		_scheduledPools.prestartAllCoreThreads();
		
		// Configure ThreadPoolExecutor.
		_instantPools.setRejectedExecutionHandler(new RejectedExecutionHandlerImpl());
		_instantPools.prestartAllCoreThreads();
		
		// Schedule the purge task.
		scheduleAtFixedRate(ThreadPool::purge, 60000, 60000);
		
		// Log information.
		LOGGER.info("...scheduled pool executor with " + Config.SCHEDULED_THREAD_POOL_COUNT + " total threads.");
		LOGGER.info("...instant pool executor with " + Config.INSTANT_THREAD_POOL_COUNT + " total threads.");
	}
	
	public static void purge()
	{
		_scheduledPools.purge();
		_instantPools.purge();
	}
	
	/**
	 * Schedules a one-shot action that becomes enabled after a delay. The pool is chosen based on pools activity.
	 * @param r : the task to execute.
	 * @param delay : the time from now to delay execution.
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will return null upon completion.
	 */
	public static ScheduledFuture<?> schedule(Runnable r, long delay)
	{
		try
		{
			return _scheduledPools.schedule(new RunnableWrapper(r), validate(delay), TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			LOGGER.error("ThreadPool#schedule failed upon {}.", e, r.toString());
			return null;
		}
	}
	
	/**
	 * Schedules a periodic action that becomes enabled after a delay. The pool is chosen based on pools activity.
	 * @param r : the task to execute.
	 * @param delay : the time from now to delay execution.
	 * @param period : the period between successive executions.
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will throw an exception upon cancellation.
	 */
	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period)
	{
		try
		{
			return _scheduledPools.scheduleAtFixedRate(new RunnableWrapper(r), validate(delay), validate(period), TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			LOGGER.error("ThreadPool#scheduleAtFixedRate failed upon {}.", e, r.toString());
			return null;
		}
	}
	
	/**
	 * Executes the given task sometime in the future.
	 * @param r : the task to execute.
	 */
	public static void execute(Runnable r)
	{
		try
		{
			_instantPools.execute(new RunnableWrapper(r));
		}
		catch (Exception e)
		{
			LOGGER.error("ThreadPool#execute failed upon {}.", e, r.toString());
		}
	}
	
	/**
	 * @param delay : The delay to validate.
	 * @return a secured value, from 0 to MAX_DELAY.
	 */
	private static long validate(long delay)
	{
		return Math.max(0, Math.min(MAX_DELAY, delay));
	}
	
	/**
	 * Shutdown thread pooling system correctly. Send different informations.
	 */
	public static void shutdown()
	{
		try
		{
			LOGGER.info("ThreadPool: Shutting down.");
			
			_scheduledPools.shutdownNow();
			_instantPools.shutdownNow();
		}
		catch (Throwable t)
		{
			LOGGER.info("ThreadPool: Problem at Shutting down. " + t.getMessage());
		}
	}
}