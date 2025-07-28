package net.sf.l2j.commons.pool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler
{
	private static final Logger LOGGER = Logger.getLogger(RejectedExecutionHandlerImpl.class.getName());
	
	@Override
	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor)
	{
		if (executor.isShutdown())
		{
			return;
		}
		
		LOGGER.warning(runnable.getClass().getSimpleName() + runnable + " from " + executor + " " + new RejectedExecutionException());
		
		if (Thread.currentThread().getPriority() > Thread.NORM_PRIORITY)
			new Thread(runnable).start();
		else
			runnable.run();
	}
}