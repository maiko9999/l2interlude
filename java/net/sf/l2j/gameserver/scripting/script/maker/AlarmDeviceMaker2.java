package net.sf.l2j.gameserver.scripting.script.maker;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class AlarmDeviceMaker2 extends AlarmDeviceMaker1
{
	public AlarmDeviceMaker2(String name)
	{
		super(name);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		if (maker.getSpawnedCount() == 0)
		{
			onMakerScriptEvent("10008", maker, 0, 0);
			
			final MultiSpawn def0 = maker.getSpawns().get(0);
			if (def0 != null)
				def0.sendScriptEvent(10025, maker.getMakerMemo().getInteger("i_ai0"), 0);
			
			NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker("godard32_2515_22m1");
			if (maker0 != null)
				maker0.getMaker().onMakerScriptEvent("10008", maker0, 0, 0);
			
			maker0 = SpawnManager.getInstance().getNpcMaker("godard32_2515_20m1");
			if (maker0 != null)
				maker0.getMaker().onMakerScriptEvent("10008", maker0, 0, 0);
			
			maker0 = SpawnManager.getInstance().getNpcMaker("godard32_2515_21m1");
			if (maker0 != null)
				maker0.getMaker().onMakerScriptEvent("10008", maker0, 0, 0);
			
			maker.getMakerMemo().set("i_ai0", 0);
		}
	}
}