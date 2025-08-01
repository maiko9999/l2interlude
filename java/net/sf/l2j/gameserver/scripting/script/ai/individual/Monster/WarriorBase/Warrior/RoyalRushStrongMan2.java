package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior;

import java.util.Calendar;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class RoyalRushStrongMan2 extends Warrior
{
	public RoyalRushStrongMan2()
	{
		super("ai/individual/Monster/WarriorBase/Warrior");
	}
	
	public RoyalRushStrongMan2(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18159,
		18161,
		18163,
		18165
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc.lookNeighbor(300);
		npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.SELF_BUFF), 1000000);
		
		startQuestTimerAtFixedRate("6001", npc, null, 60000, 60000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (!(creature instanceof Playable))
		{
			super.onSeeCreature(npc, creature);
			
			return;
		}
		if (npc.getAI().getLifeTime() > 7 && npc.isInMyTerritory() && npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
			if (topDesireTarget != null)
			{
				if (Rnd.get(100) < 33 && topDesireTarget == creature)
					npc.getAI().addCastDesire(topDesireTarget, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL1), 1000000);
			}
		}
		
		tryToAttack(npc, creature);
		
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
			if (topDesireTarget != null)
			{
				if (Rnd.get(100) < 33 && topDesireTarget == attacker)
					npc.getAI().addCastDesire(topDesireTarget, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL1), 1000000);
				
				if (Rnd.get(100) < 33 && topDesireTarget != attacker)
					npc.getAI().addCastDesire(topDesireTarget, getNpcSkillByType(npc, NpcSkillType.DD_MAGIC1), 1000000);
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if ((called.getAI().getLifeTime() > 7 && attacker instanceof Playable) && called.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			if (Rnd.get(100) < 33)
				called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.PHYSICAL_SPECIAL1), 1000000);
		}
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("6001"))
		{
			if (Calendar.getInstance().get(Calendar.MINUTE) >= 55)
				npc.deleteMe();
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		createOnePrivateEx(npc, 31455, npc.getX(), npc.getY(), npc.getZ(), 0, 0, true);
		
		super.onMyDying(npc, killer);
	}
}