package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.model.spawn.SpawnData;

public class DefaultUseDBMaker extends DefaultMaker
{
	public DefaultUseDBMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		for (MultiSpawn ms : maker.getSpawns())
			ms.loadDBNpcInfo();
	}
	
	@Override
	public void onNpcDBInfo(MultiSpawn ms, SpawnData spawnData, NpcMaker maker)
	{
		if (maker.increaseSpawnedCount(ms, 1))
			ms.doSpawn(false);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		ms.setDBLoaded(false);
		
		if (maker.getSpawnedCount() == 0)
		{
			final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("maker_name"));
			if (maker0 != null)
				maker0.getMaker().onMakerScriptEvent("1001", maker0, 0, 0);
		}
		
		if (maker.increaseSpawnedCount(ms, 1))
		{
			SpawnData spawnData = ms.getSpawnData();
			long respawnDelay = 0;
			if (spawnData != null)
			{
				if (spawnData.getRespawnTime() > System.currentTimeMillis())
					respawnDelay = (spawnData.getRespawnTime() - System.currentTimeMillis());
				
				if (respawnDelay == 0)
					respawnDelay = ms.calculateRespawnDelay() * 1000L;
				
				spawnData.setRespawn(respawnDelay);
			}
			
			ms.scheduleSpawn(respawnDelay);
		}
	}
}