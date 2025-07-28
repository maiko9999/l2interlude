package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class RandomSpawnMaker extends DefaultMaker
{
	public RandomSpawnMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		if (!maker.isOnStart())
			return;
		
		final MultiSpawn rndMs = Rnd.get(maker.getSpawns());
		if (maker.increaseSpawnedCount(rndMs, rndMs.getTotal()))
			rndMs.doSpawn(rndMs.getTotal(), false);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final MultiSpawn rndMs = Rnd.get(maker.getSpawns());
		final int i2 = (rndMs.getTotal() - rndMs.getSpawnedCount());
		if (i2 > 0 && maker.increaseSpawnedCount(rndMs, 1))
			rndMs.scheduleSpawn(rndMs.calculateRespawnDelay() * 1000);
	}
}