package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorPriest;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.Warrior;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorPriest extends Warrior
{
	public WarriorPriest()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorPriest");
	}
	
	public WarriorPriest(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20925,
		20929,
		20931,
		21646,
		20934
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("5001", npc, null, 20000, 20000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (!(creature instanceof Playable) && npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			final L2Skill buff = getNpcSkillByType(npc, NpcSkillType.BUFF);
			if (getAbnormalLevel(creature, buff) <= 0)
				npc.getAI().addCastDesire(creature, buff, 1000000);
		}
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("5001"))
			npc.lookNeighbor(300);
		
		return super.onTimer(name, npc, player);
	}
}