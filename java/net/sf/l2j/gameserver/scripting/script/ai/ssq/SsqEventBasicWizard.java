package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.model.TimeAttackEventRoom;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.attackable.HateList;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class SsqEventBasicWizard extends DefaultNpc
{
	protected static final double PARTY_ATTACKED_WEIGHT_POINT = 1.0;
	protected static final double CLAN_ATTACKED_WEIGHT_POINT = 1.0;
	
	public SsqEventBasicWizard()
	{
		super("ai/ssq");
	}
	
	public SsqEventBasicWizard(String descr)
	{
		super(descr);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("1002", npc, null, 10000);
		npc._i_ai0 = 0; // If 0, wizard use is possible: If 1, wizard use is not possible.
		
		startQuestTimer("2001", npc, null, 10000);
		
		npc._i_ai1 = 0;
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		final HateList hateList = npc.getAI().getHateList();
		
		if (name.equalsIgnoreCase("1002"))
		{
			startQuestTimer("1002", npc, null, 10000);
			hateList.refresh();
			hateList.removeIfOutOfRange(1000);
		}
		
		if (name.equalsIgnoreCase("1003"))
		{
			if (npc.isMuted())
				startQuestTimer("1003", npc, null, 10000);
			else
			{
				npc.removeAllAttackDesire();
				
				npc._i_ai0 = 0; // Can use magic.
				
				final Creature mostHated = hateList.getMostHatedCreature();
				if (mostHated != null)
					onAttacked(npc, mostHated, 100, null);
			}
		}
		
		if (name.equalsIgnoreCase("2001"))
		{
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._param2 > 60 * 17 + 50)
				npc.deleteMe();
			else
			{
				int i0 = 0;
				
				final Creature mostHatedHI = npc.getAI().getHateList().getMostHatedCreature();
				if (mostHatedHI != null)
					i0 = 1;
				
				if (i0 == 1)
				{
					if (!npc.getSpawn().isInMyTerritory(mostHatedHI))
						npc.removeAllAttackDesire();
				}
				
				final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
				if (topDesireTarget != null)
				{
					if (!npc.getSpawn().isInMyTerritory(topDesireTarget))
						npc.removeAttackDesire(topDesireTarget);
				}
				
				startQuestTimer("2001", npc, null, 10000);
			}
		}
		
		return null;
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		// The character is Desire that enters when you take 5% damage. Increases in proportion to damage.
		if (attacker instanceof Playable)
		{
			final HateList hateList = npc.getAI().getHateList();
			if (hateList.size() == 0)
				hateList.addHateInfo(attacker, (1.0 * damage / npc.getStatus().getLevel() + 7) * 100);
			else
				hateList.addHateInfo(attacker, (1.0 * damage / npc.getStatus().getLevel() + 7) * 100);
		}
		
		if (npc._i_ai0 == 1)
		{
			if (attacker instanceof Playable)
			{
				if (damage == 0)
					damage = 1;
				
				npc.getAI().addAttackDesire(attacker, ((1.0 * damage) / (npc.getStatus().getLevel() + 7)) * 100);
			}
		}
		
		// Check if you are affected by the Silence skill
		if (npc.isMuted())
		{
			npc._i_ai0 = 1;
			startQuestTimer("1003", npc, null, 10000);
		}
		
		final Player player = attacker.getActingPlayer();
		if (player != null && player.getClassId().getType() == ClassType.MYSTIC)
			npc._i_ai1 = 1;
		
		if (getNpcIntAIParam(npc, "RoomIndex") != 0 && getNpcIntAIParam(npc, "SSQPart") != 0)
		{
			if (!npc.getSpawn().isInMyTerritory(attacker))
				npc._i_ai1 = 1;
			else
			{
				Party party0 = TimeAttackEventRoom.getInstance().getParty(getNpcIntAIParam(npc, "RoomIndex"), getNpcIntAIParam(npc, "SSQPart"));
				Party party1 = attacker.getParty();
				if (party0 == null || party1 == null)
					npc._i_ai1 = 1;
				else if (party0 != party1 && attacker instanceof Player)
					npc._i_ai1 = 1;
			}
		}
		else
			npc.broadcastNpcSay("Puncture: The monster’s affiliation is unclear.");
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (!called.getSpawn().isInMyTerritory(attacker))
			return;
		
		final Player player = attacker.getActingPlayer();
		if (player != null && player.getClassId().getType() == ClassType.MYSTIC)
			called._i_ai1 = 1;
		
		if (getNpcIntAIParam(called, "RoomIndex") != 0 && getNpcIntAIParam(called, "SSQPart") != 0)
		{
			if (!called.getSpawn().isInMyTerritory(attacker))
				called._i_ai1 = 1;
			else
			{
				Party party0 = TimeAttackEventRoom.getInstance().getParty(getNpcIntAIParam(called, "RoomIndex"), getNpcIntAIParam(called, "SSQPart"));
				Party party1 = attacker.getParty();
				if (party0 == null || party1 == null)
					called._i_ai1 = 1;
				else if (party0 != party1 && attacker instanceof Player)
					called._i_ai1 = 1;
			}
		}
		else
			called.broadcastNpcSay("Puncture: The monster’s affiliation is unclear.");
		
		if (attacker instanceof Playable)
		{
			final HateList hateList = called.getAI().getHateList();
			if (hateList.size() == 0)
				hateList.addHateInfo(attacker, (damage / called.getStatus().getLevel() + 7) * 30);
			else
				hateList.addHateInfo(attacker, (damage / called.getStatus().getLevel() + 7) * 30);
		}
		
		if (called._i_ai0 == 1)
		{
			if (attacker instanceof Playable)
			{
				if (damage == 0)
					damage = 1;
				
				called.getAI().addAttackDesire(attacker, ((1.0 * damage) / (called.getStatus().getLevel() + 7)) * 30);
			}
		}
	}
	
	//
	// EventHandler DESIRE_MANIPULATION(speller, desire)
	// {
	// MACRO<return_from_outside_attack>(speller)
	//
	// MakeAttackEvent(speller, desire, 0);
	// }
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (getNpcIntAIParam(npc, "RoomIndex") != 0 && getNpcIntAIParam(npc, "SSQPart") != 0)
		{
			if (!npc.getSpawn().isInMyTerritory(caster))
				npc._i_ai1 = 1;
			else
			{
				for (WorldObject target : targets)
				{
					Party party0 = TimeAttackEventRoom.getInstance().getParty(getNpcIntAIParam(npc, "RoomIndex"), getNpcIntAIParam(npc, "SSQPart"));
					Party party1 = caster.getParty();
					if (party0 == null || party1 == null)
						npc._i_ai1 = 1;
					else if (party0 != party1 && target instanceof Player)
						npc._i_ai1 = 1;
				}
			}
		}
		else
			npc.broadcastNpcSay("Puncture: The monster’s affiliation is unclear.");
	}
}