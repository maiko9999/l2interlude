package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SsqEventSlowType extends SsqEventBasicWarrior
{
	public SsqEventSlowType()
	{
		super("ai/ssq");
	}
	
	public SsqEventSlowType(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18012,
		18014,
		18022,
		18024,
		18032,
		18034,
		18042,
		18044,
		18052,
		18054,
		18062,
		18064,
		18072,
		18074,
		18082,
		18084,
		18092,
		18094,
		18102,
		18104
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		// param3 : Monster's Created Index
		// param2: Conveys the time the NPC was born
		// param1: Informs the x-coordinate where the NPC is located (y-coordinate is arg2 in script_event)
		
		startQuestTimer("2002", npc, null, 5000); // LookNightbor every 5 seconds
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2002"))
		{
			npc.lookNeighbor(600);
			startQuestTimer("2002", npc, null, 5000);
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		int i0 = 0;
		
		if (attacker instanceof Playable)
			npc.getAI().addAttackDesire(attacker, 1, 100);
		
		if (getNpcIntAIParam(npc, "isStrong") == 1)
			i0 = 40;
		else
			i0 = 3;
		
		if (attacker instanceof Playable)
		{
			final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
			if (mostHated != null)
			{
				if (Rnd.get(100) < i0 && mostHated == attacker)
					npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.DD_MAGIC), 1000000000);
			}
		}
		
		if (Rnd.get(100) < 10 && attacker instanceof Player && npc.hasMaster() && !npc.getMaster().isDead())
			npc.sendScriptEvent(10012, npc.getObjectId(), 0);
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (Rnd.get(100) < 10 && attacker instanceof Player && called.hasMaster() && !called.getMaster().isDead())
			called.sendScriptEvent(10012, called.getObjectId(), 0);
		
		super.onClanAttacked(caller, called, attacker, damage, skill);
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
	public void onScriptEvent(Npc npc, int eventId, int arg1, int arg2)
	{
		final Creature creature = (Creature) World.getInstance().getObject(arg2);
		if (creature != null)
		{
			if (npc.getSpawn().isInMyTerritory(creature))
			{
				// The event NPC in the center collects monsters by shouting.
				if (eventId == 10007)
				{
					if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK && npc.getAI().getCurrentIntention().getType() != IntentionType.CAST)
						npc.getAI().addMoveToDesire(new Location(npc._param1, arg1, npc.getZ()), 50);
				}
				
				if (eventId == 10013)
				{
					final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
					final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
					if (mostHated != null && topDesireTarget instanceof Playable)
					{
						broadcastScriptEvent(npc, 10014, topDesireTarget.getObjectId(), 800);
						
						switch (Rnd.get(3))
						{
							case 0:
								npc.broadcastNpcSay(NpcStringId.ID_1000291, topDesireTarget.getName());
								break;
							case 1:
								npc.broadcastNpcSay(NpcStringId.ID_1000398, topDesireTarget.getName());
								break;
							case 2:
								npc.broadcastNpcSay(NpcStringId.ID_1000399, topDesireTarget.getName());
								break;
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		broadcastScriptEvent(npc, 10008, npc._param3, 1500);
		if (killer instanceof Player player)
		{
			if (npc._i_ai1 == 1)
				ssqEventGiveItem(npc, player, 3);
			else if (getNpcIntAIParam(npc, "IsStrong") == 0)
				ssqEventGiveItem(npc, player, 10);
			else
				ssqEventGiveItem(npc, player, 15);
		}
	}
}