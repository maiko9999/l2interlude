package net.sf.l2j.gameserver.taskmanager;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.spawn.MultiSpawn;

public final class MakerSpawnScheduleTaskManager implements Runnable
{
	private final Map<MultiSpawn, ConcurrentLinkedQueue<Long>> _spawns = new ConcurrentHashMap<>();
	
	protected MakerSpawnScheduleTaskManager()
	{
		// Run task each second.
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_spawns.isEmpty())
			return;
		
		// Get current time.
		final long time = System.currentTimeMillis();
		
		Iterator<Map.Entry<MultiSpawn, ConcurrentLinkedQueue<Long>>> iterator = _spawns.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<MultiSpawn, ConcurrentLinkedQueue<Long>> entry = iterator.next();
			MultiSpawn spawn = entry.getKey();
			ConcurrentLinkedQueue<Long> values = entry.getValue();
			
			if (values == null || values.isEmpty())
				iterator.remove(); // Remove the entry from the map
			else
			{
				// Using Iterator for safe removal during iteration
				Iterator<Long> innerIterator = values.iterator();
				while (innerIterator.hasNext())
				{
					Long value = innerIterator.next();
					if (time >= value)
					{
						spawn.doSpawn(false, null);
						// Remove the element using iterator's remove method
						innerIterator.remove();
					}
				}
			}
		}
	}
	
	public void addSpawn(MultiSpawn spawn, Long value)
	{
		_spawns.computeIfAbsent(spawn, k -> new ConcurrentLinkedQueue<>()).add(System.currentTimeMillis() + value);
	}
	
	public void cancelMakerRespawns(MultiSpawn multiSpawn)
	{
		_spawns.remove(multiSpawn);
	}
	
	public int getRespawningNpcCount(MultiSpawn multiSpawn)
	{
		final ConcurrentLinkedQueue<Long> spawnList = _spawns.get(multiSpawn);
		if (spawnList == null || spawnList.isEmpty())
			return 0;
		
		return spawnList.size();
	}
	
	public static final MakerSpawnScheduleTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MakerSpawnScheduleTaskManager INSTANCE = new MakerSpawnScheduleTaskManager();
	}
}