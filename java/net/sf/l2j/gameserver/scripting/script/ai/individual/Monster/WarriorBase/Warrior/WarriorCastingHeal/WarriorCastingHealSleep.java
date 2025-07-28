package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastingHeal;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorCastingHealSleep extends WarriorCastingHeal
{
	public WarriorCastingHealSleep()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCastingHeal");
	}
	
	public WarriorCastingHealSleep(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21664,
		21687,
		21710,
		21733,
		21756,
		21779
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
		
		if (attacker instanceof Playable && mostHated != null)
		{
			if (Rnd.get(100) < 33 && mostHated != attacker)
			{
				L2Skill magicSleep = getNpcSkillByType(npc, NpcSkillType.MAGIC_SLEEP);
				
				npc.getAI().addCastDesire(attacker, magicSleep, 1000000);
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		Creature mostHated = called.getAI().getAggroList().getMostHatedCreature();
		
		if (mostHated != null)
		{
			if (Rnd.get(100) < 33 && mostHated == attacker)
			{
				L2Skill magicSleep = getNpcSkillByType(called, NpcSkillType.MAGIC_SLEEP);
				
				called.getAI().addCastDesire(attacker, magicSleep, 1000000);
			}
		}
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
}