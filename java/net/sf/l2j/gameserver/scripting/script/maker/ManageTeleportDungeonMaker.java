package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class ManageTeleportDungeonMaker extends DefaultMaker
{
	public ManageTeleportDungeonMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		final MultiSpawn def0 = (MultiSpawn) SpawnManager.getInstance().getSpawn(maker.getMakerMemo().get("manager_npc_name"));
		if (def0 != null)
		{
			final int spawnCount = def0.getTotal() - def0.getSpawnedCount();
			if (spawnCount > 0 && maker.increaseSpawnedCount(def0, spawnCount))
				def0.doSpawn(spawnCount, false);
		}
		maker.getMakerMemo().set("i_ai0", 0);
	}
	
	@Override
	public void onMakerScriptEvent(String name, NpcMaker maker, int int1, int int2)
	{
		final String managerNpcAlias = maker.getMakerMemo().get("manager_npc_name");
		
		switch (name)
		{
			case "0":
				maker.getMakerMemo().set("i_ai0", 0);
				maker.setSpawnedCount(0);
				for (MultiSpawn ms : maker.getSpawns())
				{
					ms.cancelScheduledSpawns();
					if (!managerNpcAlias.equalsIgnoreCase(ms.getTemplate().getAlias()))
						ms.doDelete();
				}
				break;
			
			case "1":
				final int i_ai0 = maker.getMakerMemo().getInteger("i_ai0");
				if (i_ai0 == 0)
				{
					maker.getMakerMemo().set("i_ai0", 1);
					maker.setSpawnedCount(0);
					for (MultiSpawn ms : maker.getSpawns())
					{
						ms.cancelScheduledSpawns();
						if (!managerNpcAlias.equalsIgnoreCase(ms.getTemplate().getAlias()))
						{
							if (maker.increaseSpawnedCount(ms, ms.getTotal()))
								ms.doSpawn(ms.getTotal(), false);
						}
					}
				}
				break;
			
			case "2":
				for (MultiSpawn ms : maker.getSpawns())
				{
					if (managerNpcAlias.equalsIgnoreCase(ms.getTemplate().getAlias()))
					{
						for (Npc npc : ms.getNpcs())
							npc.sendScriptEvent(Integer.parseInt(name), 0, 0);
					}
				}
				break;
			
			case "3":
				maker.getMakerMemo().set("i_ai0", 0);
				
				for (MultiSpawn ms : maker.getSpawns())
				{
					if (managerNpcAlias.equalsIgnoreCase(ms.getTemplate().getAlias()))
					{
						for (Npc npc : ms.getNpcs())
							npc.sendScriptEvent(Integer.parseInt(name), 0, 0);
					}
					else
						ms.doDelete();
				}
				break;
		}
		super.onMakerScriptEvent(name, maker, int1, int2);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		if (maker.getMakerMemo().getInteger("i_ai0", 0) == 1)
		{
			if (maker.increaseSpawnedCount(ms, 1))
				ms.scheduleSpawn(ms.calculateRespawnDelay() * 1000);
		}
	}
	
	@Override
	public void onNpcCreated(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		if (maker.getMakerMemo().getInteger("i_ai0", 0) == 0)
		{
			final String managerNpcAlias = maker.getMakerMemo().get("manager_npc_name");
			if (!managerNpcAlias.equalsIgnoreCase(npc.getTemplate().getAlias()))
				npc.deleteMe();
		}
	}
}