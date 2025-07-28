package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WizardHealer extends WizardDDMagic2
{
	public WizardHealer()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2");
	}
	
	public WizardHealer(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21300,
		21305
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.BUFF), 1000000);
		
		npc._i_ai1 = 0;
		npc._i_ai2 = 0;
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		final double hpRatio = npc.getStatus().getHpRatio();
		if (Rnd.get(100) < 33 && hpRatio > 0.5)
		{
			if (npc._i_ai2 == 0)
			{
				npc.getAI().addFleeDesire(attacker, 500, 1000000);
				
				if (npc.getMove().getGeoPathFailCount() > 3 && attacker == npc.getAI().getTopDesireTarget() && hpRatio < 1.)
					npc._i_ai2 = 1;
			}
			else
				super.onAttacked(npc, attacker, damage, skill);
		}
		else
			super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		final int i0 = Rnd.get(100);
		
		if (caller.getStatus().getHpRatio() > 0.5)
		{
			if (i0 < 33)
			{
				called.getAI().addCastDesire(caller, getNpcSkillByType(called, NpcSkillType.BUFF), 1000000);
				called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.DEBUFF), 1000000);
			}
		}
		else if (i0 < 50)
			called.getAI().addCastDesire(caller, getNpcSkillByType(called, NpcSkillType.HEAL), 1000000);
		
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
	
	@Override
	public void onClanDied(Npc caller, Npc called, Creature killer)
	{
		called.getAI().addFleeDesire(killer, 500, 1000000);
	}
}