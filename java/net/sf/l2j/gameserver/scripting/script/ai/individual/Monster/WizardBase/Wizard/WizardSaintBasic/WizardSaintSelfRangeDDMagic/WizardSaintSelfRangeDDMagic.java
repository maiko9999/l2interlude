package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardSaintBasic.WizardSaintSelfRangeDDMagic;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardSaintBasic.WizardSaintBasic;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WizardSaintSelfRangeDDMagic extends WizardSaintBasic
{
	public WizardSaintSelfRangeDDMagic()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardSaintBasic/WizardSaintSelfRangeDDMagic");
	}
	
	public WizardSaintSelfRangeDDMagic(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21650
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		final Party party0 = attacker.getParty();
		final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
		
		if ((party0 != null || attacker != topDesireTarget) && npc.distance2D(attacker) < 40)
		{
			if (Rnd.get(100) < 33)
			{
				final L2Skill wSelfRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_SELF_RANGE_DD_MAGIC);
				if (npc.getCast().meetsHpMpConditions(npc, wSelfRangeDDMagic))
					npc.getAI().addCastDesire(npc, wSelfRangeDDMagic, 1000000, false);
				else
				{
					npc._i_ai0 = 1;
					
					npc.getAI().addAttackDesire(attacker, 1000);
				}
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
		
		if (attacker instanceof Playable)
		{
			if (npc._i_ai0 == 0)
			{
				if (npc.distance2D(attacker) > 100 && Rnd.get(100) < 80)
				{
					if (!npc.getAI().getHateList().isEmpty() || Rnd.get(100) < 2)
					{
						final L2Skill wMiddleRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_MIDDLE_RANGE_DD_MAGIC);
						if (npc.getCast().meetsHpMpConditions(attacker, wMiddleRangeDDMagic))
							npc.getAI().addCastDesire(attacker, wMiddleRangeDDMagic, 1000000, false);
						else
						{
							npc._i_ai0 = 1;
							
							npc.getAI().addAttackDesire(attacker, 1000);
						}
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
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onMoveToFinished(Npc npc, int x, int y, int z)
	{
		final Creature mostHateHI = npc.getAI().getHateList().getMostHatedCreature();
		if (mostHateHI != null)
		{
			final L2Skill debuff = getNpcSkillByType(npc, NpcSkillType.DEBUFF);
			if (Rnd.get(100) < 33 && getAbnormalLevel(mostHateHI, debuff) <= 0)
			{
				if (npc.getCast().meetsHpMpConditions(mostHateHI, debuff))
					npc.getAI().addCastDesire(mostHateHI, debuff, 1000000);
				else
				{
					npc._i_ai0 = 1;
					
					npc.getAI().addAttackDesire(mostHateHI, 1000);
				}
			}
			
			if (Rnd.get(100) < 33 && npc.distance2D(mostHateHI) < 40)
			{
				final L2Skill wShortRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC);
				if (npc.getCast().meetsHpMpConditions(mostHateHI, wShortRangeDDMagic))
					npc.getAI().addCastDesire(mostHateHI, wShortRangeDDMagic, 1000000);
				else
				{
					npc._i_ai0 = 1;
					
					npc.getAI().addAttackDesire(mostHateHI, 1000);
				}
			}
			else
			{
				final L2Skill wMiddleRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_MIDDLE_RANGE_DD_MAGIC);
				if (npc.getCast().meetsHpMpConditions(mostHateHI, wMiddleRangeDDMagic))
					npc.getAI().addCastDesire(mostHateHI, wMiddleRangeDDMagic, 1000000, false);
				else
				{
					npc._i_ai0 = 1;
					
					npc.getAI().addAttackDesire(mostHateHI, 1000);
				}
			}
		}
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		final Creature mostHateHI = npc.getAI().getHateList().getMostHatedCreature();
		if (mostHateHI != null)
		{
			if (npc._i_ai0 == 0)
			{
				final L2Skill debuff = getNpcSkillByType(npc, NpcSkillType.DEBUFF);
				
				if (npc.distance2D(mostHateHI) < 100)
					npc.getAI().addFleeDesire(mostHateHI, Config.MAX_DRIFT_RANGE, 1000000);
				else if (Rnd.get(100) < 33 && getAbnormalLevel(mostHateHI, debuff) <= 0)
				{
					if (npc.getCast().meetsHpMpConditions(mostHateHI, debuff))
						npc.getAI().addCastDesire(mostHateHI, debuff, 1000000);
					else
					{
						npc._i_ai0 = 1;
						
						npc.getAI().addAttackDesire(mostHateHI, 1000);
					}
				}
				
				if (Rnd.get(100) < 33 && npc.distance2D(mostHateHI) < 40)
				{
					final L2Skill wShortRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC);
					if (npc.getCast().meetsHpMpConditions(mostHateHI, wShortRangeDDMagic))
						npc.getAI().addCastDesire(mostHateHI, wShortRangeDDMagic, 1000000, false);
					else
					{
						npc._i_ai0 = 1;
						
						npc.getAI().addAttackDesire(mostHateHI, 1000);
					}
				}
				else
				{
					final L2Skill wMiddleRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_MIDDLE_RANGE_DD_MAGIC);
					if (npc.getCast().meetsHpMpConditions(mostHateHI, wMiddleRangeDDMagic))
						npc.getAI().addCastDesire(mostHateHI, wMiddleRangeDDMagic, 1000000, false);
					else
					{
						npc._i_ai0 = 1;
						
						npc.getAI().addAttackDesire(mostHateHI, 1000);
					}
				}
			}
			else
				npc.getAI().addAttackDesire(mostHateHI, 1000);
		}
	}
}