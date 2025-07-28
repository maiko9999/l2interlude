package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class RandomSpawnTreasurebox extends DefaultMaker
{
	public RandomSpawnTreasurebox(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		if (!maker.isOnStart())
			return;
		
		for (int i = 0; i < maker.getMaximumNpc(); i++)
		{
			final MultiSpawn rndMs = Rnd.get(maker.getSpawns());
			if (maker.increaseSpawnedCount(rndMs, 1))
				rndMs.doSpawn(1, false);
		}
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final MultiSpawn rndMs = Rnd.get(maker.getSpawns());
		if (maker.increaseSpawnedCount(rndMs, 1))
			rndMs.scheduleSpawn(rndMs.calculateRespawnDelay() * 1000);
	}
}