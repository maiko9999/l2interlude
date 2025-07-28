package net.sf.l2j.gameserver.scripting.task;

import net.sf.l2j.gameserver.data.manager.PcCafeManager;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

public final class PcCafeDailyUpdate extends ScheduledQuest
{
	public PcCafeDailyUpdate()
	{
		super(-1, "tasks");
	}
	
	@Override
	public final void onStart()
	{
		PcCafeManager.getInstance().onReset();
	}
	
	@Override
	public final void onEnd()
	{
	}
}