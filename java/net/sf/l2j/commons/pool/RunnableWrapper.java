package net.sf.l2j.commons.pool;

import java.lang.Thread.UncaughtExceptionHandler;

public class RunnableWrapper implements Runnable
{
	private final Runnable _runnable;
	
	public RunnableWrapper(Runnable runnable)
	{
		_runnable = runnable;
	}
	
	@Override
	public void run()
	{
		try
		{
			_runnable.run();
		}
		catch (Throwable e)
		{
			final Thread t = Thread.currentThread();
			final UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
			if (h != null)
			{
				h.uncaughtException(t, e);
			}
		}
	}
}
