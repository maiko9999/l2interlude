package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class SsqEventPartyLeader extends SsqPartyLeader
{
	public SsqEventPartyLeader()
	{
		super("ai/ssq");
	}
	
	public SsqEventPartyLeader(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18009,
		18019,
		18029,
		18039,
		18049,
		18059,
		18069,
		18079,
		18089,
		18099
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._flag = npc._param1;
		
		// Disappears after 5 minutes.
		int i0 = 60 * 5 - (GameTimeTaskManager.getInstance().getCurrentTick() - npc._param2);
		if (i0 < 1)
		{
			npc.deleteMe();
			return;
		}
		
		startQuestTimer("2000", npc, null, i0 * 1000);
		startQuestTimer("2002", npc, null, 5000); // LookNightbor every 5 seconds.
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			if (creature instanceof Playable && npc.getSpawn().isInMyTerritory(creature))
				npc.getAI().addAttackDesire(creature, 200);
		}
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2000"))
		{
			if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
				npc.deleteMe();
		}
		
		if (name.equalsIgnoreCase("2002"))
		{
			npc.lookNeighbor(500);
			startQuestTimer("2001", npc, null, 5000);
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		if (Rnd.get(100) < 10 && npc.distance2D(attacker) > 100)
			npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.DD_MAGIC), 1000000000);
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		if (!called.getSpawn().isInMyTerritory(target))
			return;
		
		if (caller != called && called._param1 == caller._flag)
		{
			if (target instanceof Playable)
				return;
			
			if (damage == 0)
				damage = 1;
			
			called.getAI().addAttackDesire(target, (int) (((1.0 * damage) / (called.getStatus().getLevel() + 7)) * damage * caller._weightPoint * 10));
		}
		
		// If the party member's energy is below 50%, an archer will spawn on the platform.
		if (called.getStatus().getHpRatio() * 100 < 50 && called._i_ai0 == 0)
		{
			broadcastScriptEvent(called, 10004, called._param1, 1500);
			called._i_ai0 = 1;
		}
	}
	
	@Override
	public void onPartyDied(Npc caller, Npc called)
	{
		if (caller == called)
			broadcastScriptEvent(called, 10005, called._param1, 1500); // Send an event.
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (killer instanceof Player player)
		{
			if (npc._i_ai1 == 1)
				ssqEventGiveItem(npc, player, 3);
			else
				ssqEventGiveItem(npc, player, 15);
		}
	}
}