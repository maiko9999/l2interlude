package net.sf.l2j.gameserver.scripting.script.maker;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.enums.SiegeStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.model.residence.castle.Siege;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.model.spawn.SpawnData;

public class BenomMaker extends DefaultMaker
{
	private ScheduledFuture<?> _controlTowerTask;
	
	public BenomMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		final int castleId = maker.getMakerMemo().getInteger("CastleID", 8);
		
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if (castle != null)
			castle.getSiege().addMakerEvent(maker);
		
		if (maker.getSpawns().size() > 1)
		{
			MultiSpawn def0 = maker.getSpawns().get(0);
			if (maker.increaseSpawnedCount(def0, def0.getTotal()))
				def0.doSpawn(def0.getTotal(), false);
			
			def0 = maker.getSpawns().get(1);
			
			if (castle != null && castle.getSiege().getStatus() != SiegeStatus.REGISTRATION_OPENED)
				def0.loadDBNpcInfo();
		}
	}
	
	@Override
	public void onNpcDBInfo(MultiSpawn ms, SpawnData spawnData, NpcMaker maker)
	{
		if (maker.increaseSpawnedCount(ms, 1))
			if (ms.getSpawnData() != null && !ms.getSpawnData().checkDead())
				if (ms.getSpawnData().checkAlive(ms.getSpawnLocation(), ms.getTemplate().getBaseHpMax(0), ms.getTemplate().getBaseMpMax(0)))
					ms.doSpawn(true);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
	}
	
	@Override
	public void onSiegeEvent(Siege siege, NpcMaker maker)
	{
		final List<MultiSpawn> spawnDefs = maker.getSpawns();
		
		switch (siege.getStatus())
		{
			case IN_PROGRESS:
				if (_controlTowerTask != null)
				{
					_controlTowerTask.cancel(false);
					_controlTowerTask = null;
				}
				
				_controlTowerTask = ThreadPool.scheduleAtFixedRate(() ->
				{
					if (siege.getStatus() != SiegeStatus.IN_PROGRESS)
					{
						_controlTowerTask.cancel(false);
						_controlTowerTask = null;
						return;
					}
					
					if (siege.getCastle().getAliveLifeTowerCount() < 2 && maker.getSpawns().size() > 1)
					{
						MultiSpawn def0 = maker.getSpawns().get(1);
						
						Npc c0 = def0.getNpc();
						if (c0 != null)
							c0.sendScriptEvent(10100, 1, 0);
						else
						{
							def0.loadDBNpcInfo();
							return;
						}
						
						def0 = maker.getSpawns().get(0);
						
						c0 = def0.getNpc();
						if (c0 != null)
							c0.sendScriptEvent(10101, 1, 0);
						
						_controlTowerTask.cancel(false);
						_controlTowerTask = null;
					}
					
				}, 0, 30000);
				break;
			
			case REGISTRATION_OPENED:
				if (spawnDefs.size() > 1)
					spawnDefs.get(1).doDelete();
				break;
			
			case REGISTRATION_OVER:
				if (spawnDefs.size() > 1 && maker.increaseSpawnedCount(spawnDefs.get(1), spawnDefs.get(1).getTotal()))
					spawnDefs.get(1).doSpawn(spawnDefs.get(1).getTotal(), true);
				break;
		}
	}
}