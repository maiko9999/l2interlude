package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2.WizardGrowth;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2.WizardDDMagic2;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WizardGrowthStep2 extends WizardDDMagic2
{
	public WizardGrowthStep2()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2/WizardGrowth");
	}
	
	public WizardGrowthStep2(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21453,
		21455,
		21457,
		21459,
		21472,
		21474,
		21476,
		21478,
		21491,
		21493,
		21495,
		21497
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai4 = 1;
		npc._c_ai0 = (Creature) World.getInstance().getObject(npc._param1);
		npc._i_ai3 = npc._param2;
		
		if (npc._c_ai0 != null)
			npc.getAI().getHateList().addHateInfo(npc._c_ai0, 100);
		
		npc._i_ai2 = 0;
		
		if (Rnd.get(100) < 33)
			npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.SELF_BUFF), 1000000);
		
		if (npc._c_ai0 != null)
		{
			final L2Skill wLongRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_LONG_RANGE_DD_MAGIC);
			final L2Skill wShortRangeDDMagic = getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC);
			
			if (npc.distance2D(npc._c_ai0) > 100)
			{
				if (npc.getCast().meetsHpMpConditions(npc, wLongRangeDDMagic))
					npc.getAI().addCastDesire(npc._c_ai0, wLongRangeDDMagic, 1000000);
				else
				{
					npc._i_ai0 = 1;
					npc.getAI().addAttackDesire(npc._c_ai0, 1000);
				}
			}
			else if (npc.getCast().meetsHpMpConditions(npc, wShortRangeDDMagic))
				npc.getAI().addCastDesire(npc._c_ai0, wShortRangeDDMagic, 1000000);
			else
			{
				npc._i_ai0 = 1;
				npc.getAI().addAttackDesire(npc._c_ai0, 1000);
			}
		}
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		int i0 = 0;
		int i1 = 0;
		final int takeSocial = getNpcIntAIParamOrDefault(npc, "TakeSocial", 5);
		
		if (skill != null && skill.getId() == npc._i_ai3)
		{
			if (takeSocial != 0)
			{
				final int moveAroundSocial = getNpcIntAIParam(npc, "MoveAroundSocial");
				npc.getAI().addSocialDesire(1, (moveAroundSocial * 1000) / 30, 200);
			}
			i0 = 1;
		}
		
		if (i0 == 1)
		{
			if (Rnd.get(100) < 5)
			{
				i0 = Rnd.get(5) + 2014;
				npc.broadcastNpcSay(NpcStringId.get(i0));
			}
			
			if (npc._c_ai0 != null)
			{
				if (npc._c_ai0 == attacker)
				{
					npc._i_ai2 = (npc._i_ai2 + 1);
					if (npc._i_ai4 == 1 && npc._c_ai0 == attacker)
					{
						if (Rnd.get(100) <= (npc._i_ai2 * 33))
						{
							startQuestTimer("2001", npc, null, (takeSocial * 1000) / 30);
							npc._i_ai4 = 3;
						}
					}
					else if (npc._i_ai4 != 3)
					{
						npc._i_ai4 = 2;
						i1 = 1;
					}
				}
			}
			
			if (i1 == 1 && attacker instanceof Playable)
			{
				double hateRatio = getHateRatio(npc, attacker);
				hateRatio = (((1.0 * damage) / (npc.getStatus().getLevel() + 7)) + ((hateRatio / 100) * ((1.0 * damage) / (npc.getStatus().getLevel() + 7))));
				npc.getAI().addAttackDesire(attacker, hateRatio * 100);
			}
		}
		else
			super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if ((name.equalsIgnoreCase("2001") && (npc._i_ai4 == 1 || npc._i_ai4 == 3)) && !npc.isDead())
		{
			final int heading = npc.getHeading();
			final int silhouette1 = getNpcIntAIParam(npc, "silhouette1");
			final int silhouette2 = getNpcIntAIParam(npc, "silhouette2");
			
			if (Rnd.get(100) < 50)
				createOnePrivateEx(npc, silhouette1, npc.getX(), npc.getY(), npc.getZ(), heading, 0, true, npc._c_ai0.getObjectId(), npc._i_ai3, 0);
			else
				createOnePrivateEx(npc, silhouette2, npc.getX(), npc.getY(), npc.getZ(), heading, 0, true, npc._c_ai0.getObjectId(), npc._i_ai3, 0);
			npc.deleteMe();
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onScriptEvent(Npc npc, int eventId, int arg1, int arg2)
	{
		if (eventId == 10018)
			if (Rnd.get(100) < 20)
			{
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "silhouette_ex2"), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), 0, true, npc._c_ai0.getObjectId(), npc._i_ai3, 0);
				npc.deleteMe();
			}
		
		super.onScriptEvent(npc, eventId, arg1, arg2);
	}
}