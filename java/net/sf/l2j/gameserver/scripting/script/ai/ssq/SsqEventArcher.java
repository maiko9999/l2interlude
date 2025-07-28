package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Appears at a fixed location and battles the wizard with high attack desire.
 */
public class SsqEventArcher extends SsqEventBasicWarrior
{
	public SsqEventArcher()
	{
		super("ai/ssq");
	}
	
	public SsqEventArcher(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18016,
		18026,
		18036,
		18046,
		18056,
		18066,
		18076,
		18086,
		18096,
		18106
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("2000", npc, null, 3000); // Look around often.
		npc._i_ai0 = 0; // Sleep timer activated?
		npc._i_ai4 = 0; // Has the PC abstained?
		super.onCreated(npc);
	}
	
	@Override
	public void onScriptEvent(Npc npc, int eventId, int arg1, int arg2)
	{
		if (eventId == 10015)
		{
			npc.removeAllAttackDesire();
			npc._i_ai4 = 1;
		}
	}
	
	@Override
	public void onNoDesire(Npc npc)
	{
		npc.getAI().addMoveToDesire(new Location(npc.getSpawnLocation().getX(), npc.getSpawnLocation().getY(), npc.getSpawnLocation().getZ()), 30);
	}
	
	@Override
	public void onMoveToFinished(Npc npc, int x, int y, int z)
	{
		if (x == npc.getSpawnLocation().getX() && y == npc.getSpawnLocation().getX() && z == npc.getSpawnLocation().getZ())
			npc.getAI().addDoNothingDesire(40, 30);
		else
			npc.getAI().addMoveToDesire(new Location(npc.getSpawnLocation().getX(), npc.getSpawnLocation().getY(), npc.getSpawnLocation().getZ()), 30);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (npc._i_ai4 == 1)
			return;
		
		if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
		{
			if (!(creature instanceof Playable))
				return;
			
			if (npc.getSpawn().isInMyTerritory(creature))
				npc.getAI().addAttackDesireHold(creature, 200);
			
			// If you are a wizard, attack first.
			if (creature instanceof Player player && (ClassId.isInGroup(player, "@mage_group") || ClassId.isInGroup(player, "@wizard_group")))
			{
				if (!(creature instanceof Playable))
					return;
				
				if (npc.getSpawn().isInMyTerritory(creature))
					npc.getAI().addAttackDesireHold(creature, 200);
				
				if (npc.getSpawn().isInMyTerritory(creature))
					npc.getAI().addAttackDesireHold(creature, 200);
			}
		}
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		// Look around at regular intervals.
		if (name.equalsIgnoreCase("2000"))
		{
			npc.lookNeighbor(600);
			startQuestTimer("2000", npc, null, 3000);
			
			final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
			if (npc.getAI().getCurrentIntention().getType() == IntentionType.ATTACK && topDesireTarget != null)
			{
				if (npc.distance2D(topDesireTarget) > 500)
					npc.removeAttackDesire(topDesireTarget);
			}
		}
		
		if (name.equalsIgnoreCase("2007"))
			npc.deleteMe();
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc._i_ai4 == 1)
			return;
		
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		// The character is Desire that enters when taking 5% damage. Increases in proportion to damage.
		if (attacker instanceof Playable)
			npc.getAI().addAttackDesire(attacker, (1.0 * damage / (npc.getStatus().getLevel() + 7)) * 100);
		
		if (getAbnormalLevel(npc, 1069, 1) >= 0 && npc._i_ai0 == 0)
		{
			startQuestTimer("2007", npc, null, 5000);
			npc._i_ai0 = 1;
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		broadcastScriptEvent(npc, 10006, npc._param1, 1500);
		
		if (killer instanceof Player player)
		{
			if (npc._i_ai1 == 1)
				ssqEventGiveItem(npc, player, 2);
			else
				ssqEventGiveItem(npc, player, 5);
		}
	}
}