package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class OnDayNightSpawnMaker extends DefaultMaker
{
	public OnDayNightSpawnMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		final int isNight = maker.getMakerMemo().getInteger("IsNight");
		
		if (GameTimeTaskManager.getInstance().isNight())
		{
			if (isNight == 1)
			{
				maker.getMakerMemo().set("i_ai0", 1);
				
				super.onStart(maker);
			}
			else
				maker.getMakerMemo().set("i_ai0", 0);
		}
		else if (isNight == 0)
		{
			maker.getMakerMemo().set("i_ai0", 1);
			
			super.onStart(maker);
		}
		else
			maker.getMakerMemo().set("i_ai0", 0);
		
		ThreadPool.scheduleAtFixedRate(() -> onTimer("3000", maker), 1000, 60000);
	}
	
	@Override
	public void onNpcCreated(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final int i_ai0 = maker.getMakerMemo().getInteger("i_ai0");
		final int isNight = maker.getMakerMemo().getInteger("IsNight");
		
		if (GameTimeTaskManager.getInstance().isNight())
		{
			if (i_ai0 == 0)
				if (isNight == 0)
					npc.deleteMe();
		}
		else if (i_ai0 == 0)
		{
			if (isNight == 1)
				npc.deleteMe();
		}
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final int isNight = maker.getMakerMemo().getInteger("IsNight");
		
		if (GameTimeTaskManager.getInstance().isNight())
		{
			if (isNight == 1)
			{
				if (maker.increaseSpawnedCount(ms, 1))
					ms.scheduleSpawn(ms.calculateRespawnDelay() * 1000);
			}
		}
		else if (isNight == 0)
		{
			if (maker.increaseSpawnedCount(ms, 1))
				ms.scheduleSpawn(ms.calculateRespawnDelay() * 1000);
		}
	}
	
	@Override
	public void onTimer(String name, NpcMaker maker)
	{
		if (name.equalsIgnoreCase("3000"))
		{
			final int i_ai0 = maker.getMakerMemo().getInteger("i_ai0");
			final int isNight = maker.getMakerMemo().getInteger("IsNight");
			
			if (GameTimeTaskManager.getInstance().isNight())
			{
				if (i_ai0 == 0)
				{
					if (isNight == 1)
					{
						maker.getMaker().onMakerScriptEvent("1001", maker, 0, 0);
						maker.getMakerMemo().set("i_ai0", 1);
					}
				}
				else if (isNight == 0)
				{
					maker.getMaker().onMakerScriptEvent("1000", maker, 0, 0);
					maker.getMakerMemo().set("i_ai0", 0);
				}
			}
			else if (i_ai0 == 0)
			{
				if (isNight == 0)
				{
					maker.getMaker().onMakerScriptEvent("1001", maker, 0, 0);
					maker.getMakerMemo().set("i_ai0", 1);
				}
			}
			else if (isNight == 1)
			{
				maker.getMaker().onMakerScriptEvent("1000", maker, 0, 0);
				maker.getMakerMemo().set("i_ai0", 0);
			}
		}
	}
}