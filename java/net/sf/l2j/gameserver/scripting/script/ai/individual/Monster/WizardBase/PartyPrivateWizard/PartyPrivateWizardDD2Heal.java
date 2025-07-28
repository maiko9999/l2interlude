package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.PartyPrivateWizard;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.container.attackable.HateList;
import net.sf.l2j.gameserver.skills.L2Skill;

public class PartyPrivateWizardDD2Heal extends PartyPrivateWizardDD2
{
	public PartyPrivateWizardDD2Heal()
	{
		super("ai/individual/Monster/WizardBase/PartyPrivateWizard");
	}
	
	public PartyPrivateWizardDD2Heal(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21543,
		21546,
		21823
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		super.onAttacked(npc, attacker, damage, skill);
		if (attacker instanceof Playable)
		{
			if (Rnd.get(100) < 33 && ((npc.getStatus().getHp() / npc.getStatus().getMaxHp()) * 100) < 70)
			{
				L2Skill magicHeal = getNpcSkillByType(npc, NpcSkillType.MAGIC_HEAL);
				
				npc.getAI().addCastDesire(npc, magicHeal, 1000000);
			}
		}
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		super.onPartyAttacked(caller, called, target, damage);
		if (target instanceof Playable)
		{
			if (Rnd.get(100) < 33 && ((caller.getStatus().getHp() / caller.getStatus().getMaxHp()) * 100) < 70)
			{
				L2Skill magicHeal = getNpcSkillByType(called, NpcSkillType.MAGIC_HEAL);
				
				called.getAI().addCastDesire(caller, magicHeal, 1000000);
			}
		}
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		HateList hateList = called.getAI().getHateList();
		
		if ((called.getAI().getLifeTime() > 7 && attacker instanceof Playable) && hateList.isEmpty() && Rnd.get(100) < 33)
		{
			L2Skill magicHeal = getNpcSkillByType(called, NpcSkillType.MAGIC_HEAL);
			
			called.getAI().addCastDesire(caller, magicHeal, 1000000);
		}
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
}