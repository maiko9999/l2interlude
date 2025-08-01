package net.sf.l2j.gameserver.scripting.script.ai.boss.sirra;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.memo.GlobalMemo;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SculptureGarden extends DefaultNpc
{
	private final L2Skill RESIST_COLD = SkillTable.getInstance().getInfo(4479, 1);
	
	public SculptureGarden()
	{
		super("ai/boss/sirra");
	}
	
	public SculptureGarden(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		32030 // sculpture_of_garden
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		final Creature c0 = GlobalMemo.getInstance().getCreature("7");
		if (c0 == null)
			GlobalMemo.getInstance().set("7", npc.getObjectId());
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Player)
			npc._c_ai0 = creature;
		
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public void onScriptEvent(Npc npc, int eventId, int arg1, int arg2)
	{
		if (eventId == 10027)
			npc.getSpawn().instantTeleportInMyTerritory(115792, -125760, -3373, 200);
		else if (eventId == 11038)
		{
			npc.lookNeighbor(1000);
			
			if (npc._c_ai0 != null)
			{
				final Party party0 = npc._c_ai0.getParty();
				if (party0 != null)
					for (Player partyMember : party0.getMembers())
						if (npc.getSpawn().isInMyTerritory(partyMember))
							callSkill(npc, partyMember, RESIST_COLD);
			}
		}
	}
}