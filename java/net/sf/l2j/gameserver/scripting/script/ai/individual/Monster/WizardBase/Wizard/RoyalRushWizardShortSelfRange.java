package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class RoyalRushWizardShortSelfRange extends Wizard
{
	public RoyalRushWizardShortSelfRange()
	{
		super("ai/individual/Monster/WizardBase/Wizard");
	}
	
	public RoyalRushWizardShortSelfRange(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18140,
		18172,
		18194,
		18229
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		super.onAttacked(npc, attacker, damage, skill);
		
		if (attacker instanceof Playable)
		{
			if (npc._i_ai0 == 0)
			{
				final L2Skill wSelfRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_SELF_RANGE_DD_MAGIC);
				final L2Skill wShortRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC);
				
				if (!npc.getAI().getHateList().isEmpty())
				{
					if (Rnd.get(100) < 33)
					{
						if (npc.getCast().meetsHpMpConditions(attacker, wSelfRangeDDMagic))
							npc.getAI().addCastDesire(npc, wSelfRangeDDMagic, 1000000);
						else
						{
							npc._i_ai0 = 1;
							npc.getAI().addAttackDesire(attacker, 1000);
						}
					}
					else if (npc.getCast().meetsHpMpConditions(attacker, wShortRangeDDMagic))
						npc.getAI().addCastDesire(attacker, wShortRangeDDMagic, 1000000);
					else
					{
						npc._i_ai0 = 1;
						npc.getAI().addAttackDesire(attacker, 1000);
					}
				}
				else if (Rnd.get(100) < 2)
				{
					if (npc.getCast().meetsHpMpConditions(attacker, wShortRangeDDMagic))
						npc.getAI().addCastDesire(attacker, wShortRangeDDMagic, 1000000);
					else
					{
						npc._i_ai0 = 1;
						npc.getAI().addAttackDesire(attacker, 1000);
					}
				}
			}
			else
			{
				double f0 = getHateRatio(npc, attacker);
				f0 = (((1.0 * damage) / (npc.getStatus().getLevel() + 7)) + ((f0 / 100) * ((1.0 * damage) / (npc.getStatus().getLevel() + 7))));
				npc.getAI().addAttackDesire(attacker, f0 * 100);
			}
		}
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		called.getAI().getHateList().refresh();
		if ((called.getAI().getLifeTime() > 7 && attacker instanceof Playable) && called.getAI().getHateList().isEmpty())
		{
			if (called.getCast().meetsHpMpConditions(attacker, getNpcSkillByType(called, NpcSkillType.W_SHORT_RANGE_DD_MAGIC)))
				called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.W_SHORT_RANGE_DD_MAGIC), 1000000);
			else
			{
				called._i_ai0 = 1;
				called.getAI().addAttackDesire(attacker, 1000);
			}
		}
		
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		final Creature mostHated = npc.getAI().getHateList().getMostHatedCreature();
		if (mostHated != null && npc._i_ai0 != 1)
		{
			if (npc.getCast().meetsHpMpConditions(mostHated, getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC)))
				npc.getAI().addCastDesire(mostHated, getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC), 1000000);
			else
			{
				npc._i_ai0 = 1;
				npc.getAI().addAttackDesire(mostHated, 1000);
			}
		}
	}
}