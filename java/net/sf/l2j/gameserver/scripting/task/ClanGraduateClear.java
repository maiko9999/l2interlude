package net.sf.l2j.gameserver.scripting.task;

import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

public final class ClanGraduateClear extends ScheduledQuest
{
	public ClanGraduateClear()
	{
		super(-1, "task");
	}
	
	@Override
	public final void onStart()
	{
		ClanTable.getInstance().clearGraduates();
	}
	
	@Override
	public final void onEnd()
	{
		// Do nothing.
	}
}