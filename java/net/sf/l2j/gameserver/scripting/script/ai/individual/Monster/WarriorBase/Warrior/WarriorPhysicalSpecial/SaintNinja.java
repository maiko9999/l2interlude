package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorPhysicalSpecial;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SaintNinja extends WarriorPhysicalSpecial
{
	public SaintNinja()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorPhysicalSpecial");
	}
	
	public SaintNinja(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21539,
		21540,
		21524,
		21525,
		21531,
		21658
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 0;
		
		npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.SELF_BUFF), 1000000);
		
		if (getNpcIntAIParam(npc, "IsMainForm") == 0)
			startQuestTimer("2000", npc, null, (60000 * 5));
		
		startQuestTimer("2001", npc, null, 60000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (Rnd.get(100) < 80 && getNpcIntAIParam(npc, "IsMainForm") == 1 && npc._i_ai0 == 0)
		{
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "OtherSelf"), npc.getX() + Rnd.get(20), npc.getY() + Rnd.get(20), npc.getZ(), 32768, 0, false, 1000, attacker.getObjectId(), 1);
			
			npc._i_ai0 = 1;
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2000"))
		{
			if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
			{
				npc.deleteMe();
				return null;
			}
			
			startQuestTimer("2000", npc, null, (60000 * 5));
		}
		else if (name.equalsIgnoreCase("2001"))
		{
			if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
				npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.TELEPORT_EFFECT), 1000000);
			
			startQuestTimer("2001", npc, null, (60000 * 5));
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (called.getAI().getCurrentIntention().getType() != IntentionType.ATTACK && called.distance2D(attacker) > 300)
		{
			called.abortAll(false);
			called.teleportTo(attacker.getPosition(), 0);
			called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.TELEPORT_EFFECT), 1000000);
			
			if (attacker instanceof Playable)
			{
				double f0 = getHateRatio(called, attacker);
				f0 = (((1.0 * damage) / (called.getStatus().getLevel() + 7)) + ((f0 / 100) * ((1.0 * damage) / (called.getStatus().getLevel() + 7))));
				
				called.getAI().addAttackDesire(attacker, (int) (f0 * 30));
			}
		}
	}
}