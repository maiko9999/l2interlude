package net.sf.l2j.commons.util;

import java.util.concurrent.TimeUnit;

import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class LinTime
{
	private static volatile long dt;
	
	public static void resetDeltaTime()
	{
		dt = 0;
	}
	
	public static void setDeltaTime(long dt)
	{
		LinTime.dt = dt;
	}
	
	public static void addDeltaTime(long dt)
	{
		LinTime.dt += dt;
	}
	
	public static long deltaTime()
	{
		return dt;
	}
	
	public static long currentTimeMillis()
	{
		return System.currentTimeMillis() + dt;
	}
	
	public static long elapsedTime()
	{
		return GameTimeTaskManager.getInstance().getCurrentTick() + TimeUnit.MILLISECONDS.toSeconds(dt);
	}
}