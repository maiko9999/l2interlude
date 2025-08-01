package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.PartyPrivateWarrior;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class RoyalRushPartyPrivateBomb extends PartyPrivateWarrior
{
	public RoyalRushPartyPrivateBomb()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/PartyPrivateWarrior");
	}
	
	public RoyalRushPartyPrivateBomb(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21437
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
			
			if (topDesireTarget != null && topDesireTarget == attacker)
			{
				if (Rnd.get(100) < 33)
					npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL1), 1000000);
				
				if (Rnd.get(100) < 33 && npc.getStatus().getHpRatio() < 0.4 && npc.hasMaster() && !npc.getMaster().isDead())
				{
					npc.getAI().addCastDesire(npc.getMaster(), getNpcSkillByType(npc, NpcSkillType.MAGIC_HEAL), 1000000);
					npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.BOMB), 1000000);
				}
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		if (Rnd.get(100) < 33 && called.getStatus().getHpRatio() < 0.4 && called.hasMaster() && !called.getMaster().isDead())
		{
			called.getAI().addCastDesire(called.getMaster(), getNpcSkillByType(called, NpcSkillType.MAGIC_HEAL), 1000000);
			called.getAI().addCastDesire(called, getNpcSkillByType(called, NpcSkillType.BOMB), 1000000);
		}
		
		super.onPartyAttacked(caller, called, target, damage);
	}
}