package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SsqEventFastTypePrivate extends SsqEventBasicWarrior
{
	public SsqEventFastTypePrivate()
	{
		super("ai/ssq");
	}
	
	public SsqEventFastTypePrivate(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18300,
		18302,
		18304,
		18306,
		18308,
		18310,
		18312,
		18314,
		18316,
		18318
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		final Creature creature = (Creature) World.getInstance().getObject(npc._param3);
		if (creature != null)
		{
			if (creature instanceof Playable)
				npc.getAI().addAttackDesire(creature, 1, 100);
		}
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		if (attacker instanceof Playable)
			npc.getAI().addAttackDesire(attacker, 1, 100);
		
		final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
		
		if (attacker instanceof Playable && mostHated == attacker && Rnd.get(100) < 25)
		{
			final L2Skill physicalSpecial = getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL);
			if (getAbnormalLevel(attacker, physicalSpecial) <= 0)
				npc.getAI().addCastDesire(attacker, physicalSpecial, 1000000);
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onScriptEvent(Npc npc, int eventId, int arg1, int arg2)
	{
		final Creature c1 = (Creature) World.getInstance().getObject(arg2);
		if (c1 != null)
		{
			if (npc.getSpawn().isInMyTerritory(c1))
			{
				// The event NPC in the center collects monsters by shouting.
				if (eventId == 10007)
				{
					if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK && npc.getAI().getCurrentIntention().getType() != IntentionType.CAST)
						npc.getAI().addMoveToDesire(new Location(npc._param1, arg1, npc.getZ()), 50);
				}
			}
		}
		
		super.onScriptEvent(npc, eventId, arg1, arg2);
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
}