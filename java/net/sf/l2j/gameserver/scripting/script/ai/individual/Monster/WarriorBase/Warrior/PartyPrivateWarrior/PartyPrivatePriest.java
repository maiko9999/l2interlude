package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.PartyPrivateWarrior;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class PartyPrivatePriest extends PartyPrivateWarrior
{
	public PartyPrivatePriest()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/PartyPrivateWarrior");
	}
	
	public PartyPrivatePriest(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20970,
		20958,
		20960,
		20945,
		20979,
		20942,
		20936,
		20757,
		20951
	};
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("5001"))
			npc.lookNeighbor(300);
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("5001", npc, null, 20000, 20000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		final Npc master = npc.getMaster();
		if (master != null && creature == master && npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			final L2Skill buff = getNpcSkillByType(npc, NpcSkillType.BUFF);
			if (getAbnormalLevel(master, buff) <= 0)
				npc.getAI().addCastDesire(master, buff, 1000000);
		}
		super.onSeeCreature(npc, creature);
	}
}