package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class FrintezzaEvilateMaker extends DefaultMaker
{
	public FrintezzaEvilateMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onMakerScriptEvent(String name, NpcMaker maker, int int1, int int2)
	{
		if (name.equalsIgnoreCase("1000"))
			maker.deleteAll();
		else if (name.equalsIgnoreCase("1001"))
		{
			if (checkHasSpawnCondition(maker))
				return;
			
			for (MultiSpawn ms : maker.getSpawns())
			{
				int toSpawnCount = ms.getTotal() - ms.getSpawnedCount();
				
				if (toSpawnCount > 0 && maker.increaseSpawnedCount(ms, toSpawnCount))
					ms.doSpawn(false);
			}
		}
	}
}