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
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class SsqEventFastType extends SsqEventBasicWarrior
{
	public SsqEventFastType()
	{
		super("ai/ssq");
	}
	
	public SsqEventFastType(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18015,
		18025,
		18035,
		18045,
		18055,
		18065,
		18075,
		18085,
		18095,
		18105
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		// param3: Created Index of monster
		// param2: Conveys the time the NPC was born.
		// param1: Informs the x-coordinate where the NPC is located (y-coordinate is arg2 in script_event)
		
		npc._i_ai0 = 0;
		
		if ((npc._param3 & 0x10000000) != 0)
		{
			npc._i_ai0 = 1;
			npc._param3 = npc._param3 - 0x10000000;
		}
		
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
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			if (creature instanceof Playable && npc.getSpawn().isInMyTerritory(creature))
				npc.getAI().addAttackDesire(creature, 200);
		}
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		if (attacker instanceof Playable)
			npc.getAI().addAttackDesire(attacker, 1, 100);
		
		int i0 = 0;
		
		if (getNpcIntAIParam(npc, "isStrong") == 1)
			i0 = 20;
		else
			i0 = 3;
		
		if (attacker instanceof Playable)
		{
			final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
			if (mostHated != null)
			{
				if (Rnd.get(100) < i0 && mostHated == attacker)
					npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL), 1000000000);
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
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
				
				if (eventId == 10014)
				{
					final Creature c0 = (Creature) World.getInstance().getObject(arg1);
					int i0 = (int) ((1.0 * GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0) / (60 * 17 * 2) * 100);
					if (i0 > Rnd.get(100) && c0 != null)
					{
						npc.removeAllAttackDesire();
						if (c0 instanceof Playable)
							npc.getAI().addAttackDesire(c0, 5, 500);
					}
				}
			}
		}
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (npc._i_ai0 == 0)
			broadcastScriptEvent(npc, 10009, npc._param3, 1500);
		else
			broadcastScriptEvent(npc, 10010, npc._param3, 1500);
		
		if (killer instanceof Player player)
		{
			if (npc._i_ai1 == 1)
				ssqEventGiveItem(npc, player, 2);
			else if (getNpcIntAIParam(npc, "IsStrong") == 0)
				ssqEventGiveItem(npc, player, 5);
			else
				ssqEventGiveItem(npc, player, 8);
		}
	}
}