package net.sf.l2j.gameserver.scripting.script.maker;

import java.util.Calendar;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;

public class RoyalReqNextMaker extends RoyalRushMaker
{
	public RoyalReqNextMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		final int bossMaker = maker.getMakerMemo().getInteger("BossMaker", -1);
		if (bossMaker == 1)
		{
			if (npc.getNpcId() == 25339 || npc.getNpcId() == 25342 || npc.getNpcId() == 25346 || npc.getNpcId() == 25349)
			{
				final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("next_maker_name"));
				if (maker0 != null)
					maker0.getMaker().onMakerScriptEvent("1002", maker0, 0, 0);
			}
		}
		else
		{
			final int reqCount = maker.getMakerMemo().getInteger("req_count", -1);
			if (maker.getSpawnedCount() == reqCount)
			{
				final Calendar c = Calendar.getInstance();
				
				final int currentMinute = c.get(Calendar.MINUTE);
				if (currentMinute >= 0 && currentMinute <= 50)
				{
					final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("next_maker_name"));
					if (maker0 != null)
						maker0.getMaker().onMakerScriptEvent("1001", maker0, 0, 0);
				}
			}
		}
	}
}