package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.LV3Monster;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class LV3Orc extends LV3Monster
{
	public LV3Orc()
	{
		super("ai/individual/Monster/LV3Monster");
	}
	
	public LV3Orc(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		27294,
		27295
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 0;
		npc.setEnchantEffect(10);
		npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.BIG_BODY_SKILL), 1000000);
		
		startQuestTimerAtFixedRate("4000", npc, null, 1000, 1000);
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("4000"))
		{
			if (!npc.isStunned())
			{
				if (npc._i_ai0 == 1)
				{
					npc.removeAllDesire();
					npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.DEBUFF1), 1000000);
					npc._i_ai0++;
					npc.getAI().addAttackDesire(npc._c_ai0, 200);
				}
				else if (npc._i_ai0 == 3)
				{
					npc.removeAllDesire();
					npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.DEBUFF2), 1000000);
					npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.NORMAL_BODY_SKILL), 1000000);
					npc._i_ai0++;
					npc.getAI().addAttackDesire(npc._c_ai0, 200);
				}
				else if (npc._i_ai0 == 5)
				{
					npc.removeAllDesire();
					npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.DEBUFF3), 1000000);
					npc._i_ai0++;
					npc.getAI().addAttackDesire(npc._c_ai0, 1, 200);
				}
			}
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			if (damage == 0)
				damage = 1;
			
			npc.getAI().addAttackDesire(attacker, (((1.0 * damage) / (npc.getStatus().getLevel() + 7)) * 100));
		}
		if (skill != null && npc.isStunned() && skill.getId() == 1245)
		{
			switch (npc._i_ai0)
			{
				case 0:
					npc._i_ai0++;
					npc.setEnchantEffect(0);
					break;
				
				case 2:
					npc._i_ai0++;
					break;
				
				case 4:
					npc._i_ai0++;
					break;
			}
		}
		
		if (attacker instanceof Playable)
		{
			if (npc.getAI().getTopDesireTarget() != null)
			{
				if (Rnd.get(100) < 33 && npc.getAI().getTopDesireTarget() == attacker)
				{
					switch (Rnd.get(3))
					{
						case 0:
							npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL1), 1000);
							break;
						
						case 1:
							npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL2), 1000);
							break;
						
						case 2:
							npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL3), 1000);
							break;
					}
				}
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
}