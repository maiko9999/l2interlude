package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class ExclusiveSpawnNormalMaker extends DefaultMaker
{
	public ExclusiveSpawnNormalMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onStart(NpcMaker maker)
	{
		maker.getMakerMemo().set("i_ai0", 0);
		
		super.onStart(maker);
	}
	
	@Override
	public void onNpcCreated(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		if (npc.getTemplate().getAlias().equalsIgnoreCase(maker.getMakerMemo().get("unique_npc")))
		{
			maker.getMakerMemo().set("i_ai0", 1);
			
			final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("maker_name"));
			if (maker0 != null)
				maker0.getMaker().onMakerScriptEvent("1000", maker0, 1, 0);
		}
		else if (maker.getMakerMemo().getInteger("i_ai0") == 1)
			npc.deleteMe();
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		if (npc.getTemplate().getAlias().equalsIgnoreCase(maker.getMakerMemo().get("unique_npc")))
		{
			maker.getMakerMemo().set("i_ai0", 0);
			
			final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("maker_name"));
			if (maker0 != null)
				maker0.getMaker().onMakerScriptEvent("1001", maker0, 0, 0);
			
			super.onNpcDeleted(npc, ms, maker);
		}
		else if (maker.getMakerMemo().getInteger("i_ai0") == 0)
		{
			if (maker.increaseSpawnedCount(ms, 1))
				ms.scheduleSpawn(ms.calculateRespawnDelay() * 1000);
		}
	}
	
	@Override
	public void onMakerScriptEvent(String name, NpcMaker maker, int int1, int int2)
	{
		maker.getMakerMemo().set("i_ai0", int1);
		
		super.onMakerScriptEvent(name, maker, int1, int2);
	}
}