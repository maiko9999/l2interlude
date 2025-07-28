package net.sf.l2j.gameserver.model.spawn;

import java.io.InvalidClassException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.l2j.commons.geometry.Territory;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.StaticSpawnData;
import net.sf.l2j.gameserver.enums.MakerSpawnTime;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.memo.SpawnMemo;
import net.sf.l2j.gameserver.model.records.PrivateData;
import net.sf.l2j.gameserver.model.records.custom.StaticSpawn;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.taskmanager.MakerSpawnScheduleTaskManager;

/**
 * This class manages the spawn and respawn of {@link Npc}s defined by {@link NpcMaker} in a territory based system.<br>
 * The {@link SpawnLocation} can be:
 * <ul>
 * <li>Fixed coordinates.
 * <li>Random one of defined coordinates.
 * <li>Random coordinate from a {@link Territory} of linked {@link NpcMaker}.
 * </ul>
 */
public final class MultiSpawn extends ASpawn
{
	private static final int RANDOM_WALK_LOOP_LIMIT = 3;
	
	private final NpcMaker _npcMaker;
	private final int _total;
	private final int[][] _coords;
	
	private final Set<Npc> _npcs = ConcurrentHashMap.newKeySet();
	
	private AtomicInteger _spawnedCount = new AtomicInteger(0);
	
	public MultiSpawn(NpcMaker npcMaker, NpcTemplate template, int total, int respawnDelay, int respawnRandom, List<PrivateData> privateData, SpawnMemo aiParams, int[][] coords, SpawnData spawnData) throws SecurityException, ClassNotFoundException, NoSuchMethodException, InvalidClassException
	{
		super(template);
		
		_respawnDelay = Math.max(0, respawnDelay);
		_respawnRandom = Math.min(respawnDelay, Math.max(0, respawnRandom));
		
		_privateData = privateData;
		_aiParams = aiParams;
		
		_npcMaker = npcMaker;
		_coords = coords;
		_spawnData = spawnData;
		
		// Database name is specified -> single spawn (ignore total value, only 1 instance of NPC may exist).
		if (_spawnData != null)
			_total = 1;
		// Coordinates specified -> fixed spawn.
		else if (_coords != null)
			_total = total;
		// Coordinates not specified -> random spawn.
		else
			_total = (int) Math.round(total * Config.SPAWN_MULTIPLIER);
	}
	
	@Override
	public Npc getNpc()
	{
		if (_npcs.isEmpty())
			return null;
		
		for (Npc npc : _npcs)
		{
			if (!npc.isDecayed())
				return npc;
		}
		
		return null;
	}
	
	@Override
	public SpawnLocation getSpawnLocation()
	{
		// "anywhere", spawn is random, generate random coordinates from territory.
		if (_coords == null)
			return _npcMaker.getTerritory().getRandomGeoLocation(_npcMaker.getBannedTerritory());
		
		// "fixed", spawn is defined by one set of coordinates.
		if (_coords.length == 1)
		{
			final SpawnLocation spawnLoc = new SpawnLocation(_coords[0][0], _coords[0][1], _coords[0][2], _coords[0][3]);
			spawnLoc.setZ(GeoEngine.getInstance().getHeight(spawnLoc));
			return spawnLoc;
		}
		
		// "fixed_random", spawn is defined by more sets of coordinates, pick one random.
		int chance = Rnd.get(100);
		for (int[] coord : _coords)
		{
			chance -= coord[4];
			if (chance < 0)
			{
				final SpawnLocation spawnLoc = new SpawnLocation(coord[0], coord[1], coord[2], Rnd.get(65536));
				spawnLoc.setZ(GeoEngine.getInstance().getHeight(spawnLoc));
				return spawnLoc;
			}
		}
		
		// Should never happen.
		return null;
	}
	
	@Override
	public Location getRandomWalkLocation(Npc npc, int offset)
	{
		// Generate a new Location object based on Npc position.
		final Location loc = npc.getPosition().clone();
		
		// Npc position is out of the territory, return a random location based on NpcMaker's Territory.
		if (!_npcMaker.getTerritory().isInside(loc))
			return _npcMaker.getTerritory().getRandomGeoLocation();
		
		// Attempt three times to find a random Location matching the offset and banned territory.
		for (int loop = 0; loop < RANDOM_WALK_LOOP_LIMIT; loop++)
		{
			// Generate random location based on offset. Reset each attempt to current Npc position.
			loc.set(npc.getPosition());
			loc.addRandomOffsetBetween(offset / Rnd.get(2, 4), offset);
			
			// Validate location using NpcMaker's territory.
			if (!_npcMaker.getTerritory().isInside(loc))
				continue;
			
			// Validate location using NpcMaker's banned territory.
			if (_npcMaker.getBannedTerritory() != null && _npcMaker.getBannedTerritory().isInside(loc))
				continue;
			
			// Validate location using geodata.
			loc.set(GeoEngine.getInstance().getValidLocation(npc, loc));
			return loc;
		}
		
		// We didn't find a valid Location ; reuse Npc spawn location.
		loc.set(npc.getSpawnLocation());
		
		return loc;
	}
	
	@Override
	public boolean isInMyTerritory(WorldObject worldObject)
	{
		final Location loc = worldObject.getPosition().clone();
		
		// Check location using NpcMaker's banned territory.
		if (_npcMaker.getBannedTerritory() != null && _npcMaker.getBannedTerritory().isInside(loc))
			return false;
		
		// Check location using NpcMaker's territory.
		return _npcMaker.getTerritory().isInside(loc);
	}
	
	@Override
	public Npc doSpawn(boolean isSummonSpawn, Creature summoner)
	{
		Npc toRespawn = null;
		
		// Manually iterate through the set to find a ready-to-respawn NPC
		for (Npc npc : _npcs)
		{
			if (npc.isReadyForRespawn())
			{
				toRespawn = npc;
				break;
			}
		}
		
		if (toRespawn != null)
		{
			toRespawn.setReadyForRespawn(false);
			
			// Atomically check and respawn the NPC
			if (_npcs.remove(toRespawn))
			{
				doRespawn(toRespawn);
				_npcs.add(toRespawn);
			}
			else
				toRespawn = null;
		}
		
		if (toRespawn == null)
		{
			// If no NPC is ready for respawn, spawn a new one
			toRespawn = super.doSpawn(isSummonSpawn, summoner);
			if (toRespawn == null)
			{
				LOGGER.warn("Can not spawn id {} from maker {}.", getNpcId(), _npcMaker.getName());
				return null;
			}
			_npcs.add(toRespawn);
		}
		
		// Process dynamic Residence setting.
		final MakerSpawnTime mst = _npcMaker.getMakerSpawnTime();
		if (mst != null && mst != MakerSpawnTime.DOOR_OPEN)
		{
			final String[] params = _npcMaker.getMakerSpawnTimeParams();
			if (params != null)
				toRespawn.setResidence(params[0]);
		}
		
		return toRespawn;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		StaticSpawn staticSpawn = StaticSpawnData.getInstance().getById(getTemplate().getNpcId());
		if (staticSpawn != null && staticSpawn.isEnabled())
		{
			if (staticSpawn.earthQuake())
				npc.broadcastPacket(new Earthquake(npc, 20, 10, true));
			
			if (staticSpawn.announce())
				World.announceToOnlinePlayers(10_047, npc.getName());
		}
		
		if (npc instanceof RaidBoss raidboss && Config.ANNOUNCE_SPAWN_RAIDBOSS)
			World.announceToOnlinePlayers(10_237, raidboss.getName(), raidboss.getStatus().getLevel());
		
		if (npc instanceof GrandBoss grandboss && Config.ANNOUNCE_SPAWN_GRANDBOSS)
			World.announceToOnlinePlayers(10_238, grandboss.getName(), grandboss.getStatus().getLevel());
		
		npc.setReadyForRespawn(false);
		
		// Notify NpcMaker.
		_npcMaker.onSpawn(npc);
		
		doSave();
	}
	
	@Override
	public void doDelete()
	{
		// Copying set prevents deleteAll to trigger onNpcDeleted and double spawns.
		Set<Npc> tmpNpcs = Set.copyOf(_npcs);
		
		_npcs.clear();
		
		tmpNpcs.forEach(npc ->
		{
			// Cancel respawn task.
			npc.cancelRespawn();
			
			// Delete privates which were manually spawned via createOnePrivate / createOnePrivateEx.
			if (npc.isMaster())
				npc.getMinions().forEach(Npc::deleteMe);
			
			// Delete the NPC.
			npc.deleteMe();
		});
		

		_spawnedCount.set(0);
		
		// Reset spawn data.
		if (_spawnData != null)
			_spawnData.setStatus((byte) -1);
	}
	
	@Override
	public long calculateRespawnDelay()
	{
		if (_spawnData != null)
		{
			StaticSpawn staticSpawn = StaticSpawnData.getInstance().getById(getTemplate().getNpcId());
			if (staticSpawn != null && staticSpawn.isEnabled())
				return (staticSpawn.calcNextDate() - System.currentTimeMillis()) / 1000 + Rnd.get(staticSpawn.randomTime());
		}
		
		return super.calculateRespawnDelay();
	}
	
	@Override
	public void onDecay(Npc npc)
	{
		decreaseSpawnedCount(1);
		_npcMaker.onDecay(npc);
		
		// Notify NpcMaker.
		if (getRespawnDelay() > 0)
		{
			SpawnData spawnData = npc.getSpawn().getSpawnData();
			long respawnDelay = 0;
			if (spawnData != null && spawnData.getRespawnTime() > System.currentTimeMillis())
				respawnDelay = (spawnData.getRespawnTime() - System.currentTimeMillis());
			
			if (respawnDelay == 0)
				respawnDelay = (npc.getSpawn().calculateRespawnDelay() * 1000L);
			
			// Check spawn data and set respawn.
			if (_spawnData != null)
				_spawnData.setRespawn(respawnDelay);
			
			npc.setReadyForRespawn(true);

			doSave();
		}
		else
		{
			// Respawn is disabled, delete NPC.
			_npcs.remove(npc);
		}
	}
	
	@Override
	public String toString()
	{
		return "MultiSpawn [id=" + getNpcId() + "]";
	}
	
	@Override
	public String getDescription()
	{
		return "NpcMaker: " + _npcMaker.getName();
	}
	
	@Override
	public void updateSpawnData()
	{
		if (_spawnData == null)
			return;
		
		_npcs.forEach(npc -> _spawnData.setStats(npc));
	}
	
	@Override
	public void sendScriptEvent(int eventId, int arg1, int arg2)
	{
		_npcs.forEach(npc -> npc.sendScriptEvent(eventId, arg1, arg2));
	}
	
	public NpcMaker getNpcMaker()
	{
		return _npcMaker;
	}
	
	public int[][] getCoords()
	{
		return _coords;
	}
	
	public int getTotal()
	{
		return _total;
	}
	
	public Set<Npc> getNpcs()
	{
		return _npcs;
	}
	
	public Set<Npc> getDecayedNpcs()
	{
		Set<Npc> decayedNpcs = ConcurrentHashMap.newKeySet();
		for (Npc npc : _npcs)
		{
			if (npc.isDecayed())
				decayedNpcs.add(npc);
		}
		
		return decayedNpcs;
	}
	
	public boolean increaseSpawnedCount(int count)
	{
		final int newspawnedCount = _spawnedCount.get() + count;
		if (newspawnedCount >= 0 && newspawnedCount <= _total)
		{
			_spawnedCount.set(newspawnedCount);
			return true;
		}
		return false;
	}
	
	public boolean decreaseSpawnedCount(int count)
	{
		final int newspawnedCount = _spawnedCount.get() - count;
		if (newspawnedCount >= 0 && newspawnedCount <= _total)
		{
			_spawnedCount.set(newspawnedCount);
			return true;
		}
		return false;
	}
	
	public int getSpawnedCount()
	{
		return _spawnedCount.get();
	}
	
	public Set<Npc> doSpawn(int count, boolean isSummonSpawn)
	{
		for (int i = 0; i < count; i++)
			doSpawn(isSummonSpawn, null);
		
		return _npcs;
	}
	
	public void scheduleSpawn(long delay)
	{
		MakerSpawnScheduleTaskManager.getInstance().addSpawn(this, delay);
	}
	
	public void cancelScheduledSpawns()
	{
		MakerSpawnScheduleTaskManager.getInstance().cancelMakerRespawns(this);
	}
	
	public int getRespawningNpcCount()
	{
		return MakerSpawnScheduleTaskManager.getInstance().getRespawningNpcCount(this);
	}
	
	public void loadDBNpcInfo()
	{
		_npcMaker.getMaker().onNpcDBInfo(this, _spawnData, _npcMaker);
	}
}