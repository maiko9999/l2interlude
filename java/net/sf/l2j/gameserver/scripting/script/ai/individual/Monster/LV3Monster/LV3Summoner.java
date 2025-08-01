package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.LV3Monster;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class LV3Summoner extends LV3Monster
{
	public LV3Summoner()
	{
		super("ai/individual/Monster/LV3Monster");
	}
	
	public LV3Summoner(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		27313,
		27314,
		27315
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("4000", npc, null, 1000, 1000);
		startQuestTimerAtFixedRate("4001", npc, null, 3000, 3000);
		npc._i_ai0 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			if (damage == 0)
				damage = 1;
			
			npc.getAI().addAttackDesire(attacker, ((1.0 * damage) / (npc.getStatus().getLevel() + 7)) * 100);
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
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("4000"))
		{
			if (getAbnormalLevel(npc, getNpcSkillByType(npc, NpcSkillType.BUFF1)) > 0)
				npc._i_ai0 = 1;
			else
				npc._i_ai0 = 0;
		}
		
		if (name.equalsIgnoreCase("4001"))
		{
			if (npc._c_ai0 != null && npc._i_ai0 == 0 && Rnd.get(100) < 50)
			{
				npc.removeAllDesire();
				npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.BUFF1), 1000000);
				npc.getAI().addAttackDesire(npc._c_ai0, 200);
				
				npc._i_ai0 = 1;
			}
		}
		
		return super.onTimer(name, npc, player);
	}
}