package net.sf.l2j.gameserver.scripting.script.maker;

import java.util.Calendar;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class RoyalRushMaker extends DefaultMaker
{
	public RoyalRushMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		ThreadPool.scheduleAtFixedRate(() -> onTimer("3000", maker), 1000, 1000);
	}
	
	@Override
	public void onNpcCreated(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final Calendar c = Calendar.getInstance();
		
		final int currentMinute = c.get(Calendar.MINUTE);
		if (currentMinute > 49 && currentMinute < 60)
			npc.deleteMe();
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final Calendar c = Calendar.getInstance();
		
		final int currentMinute = c.get(Calendar.MINUTE);
		if (ms.calculateRespawnDelay() != 0 && !(currentMinute > 49 && currentMinute < 59))
			super.onNpcDeleted(npc, ms, maker);
	}
	
	@Override
	public void onTimer(String name, NpcMaker maker)
	{
		if (name.equalsIgnoreCase("3000"))
		{
			final Calendar c = Calendar.getInstance();
			
			final int currentMinute = c.get(Calendar.MINUTE);
			if (currentMinute == 54)
			{
				final int currentSecond = c.get(Calendar.SECOND);
				if (currentSecond == 0)
					maker.getMaker().onMakerScriptEvent("1000", maker, 0, 0);
				else if (currentSecond == 1)
				{
					if (maker.getSpawnedCount() > 0)
						maker.getMaker().onMakerScriptEvent("1000", maker, 0, 0);
				}
				else if (currentSecond == 2)
				{
					if (maker.getSpawnedCount() > 0)
						maker.getMaker().onMakerScriptEvent("1000", maker, 0, 0);
				}
			}
		}
	}
}