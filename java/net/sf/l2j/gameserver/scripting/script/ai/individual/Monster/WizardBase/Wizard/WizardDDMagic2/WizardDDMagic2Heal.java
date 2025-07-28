package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WizardDDMagic2Heal extends WizardDDMagic2
{
	public WizardDDMagic2Heal()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2");
	}
	
	public WizardDDMagic2Heal(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20292,
		20807,
		21414,
		20657,
		20923,
		20576,
		20633,
		22030,
		22037,
		22125
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
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (((called.getAI().getLifeTime() > 7 && attacker instanceof Playable) && !called.getAI().getHateList().isEmpty()) && Rnd.get(100) < 33)
		{
			L2Skill magicHeal = getNpcSkillByType(called, NpcSkillType.MAGIC_HEAL);
			
			called.getAI().addCastDesire(caller, magicHeal, 1000000);
		}
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
}