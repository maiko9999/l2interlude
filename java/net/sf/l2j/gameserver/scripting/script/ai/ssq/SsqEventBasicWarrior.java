package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.model.TimeAttackEventRoom;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class SsqEventBasicWarrior extends DefaultNpc
{
	public SsqEventBasicWarrior()
	{
		super("ai/ssq");
	}
	
	public SsqEventBasicWarrior(String descr)
	{
		super(descr);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("2001", npc, null, 10000);
		npc._i_ai1 = 0;
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2001"))
		{
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._param2 > 60 * 17 + 50)
				npc.deleteMe();
			else
			{
				final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
				if (mostHated != null)
				{
					if (!npc.getSpawn().isInMyTerritory(mostHated))
						npc.removeAttackDesire(mostHated);
				}
				startQuestTimer("2001", npc, null, 10000);
			}
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
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
			if (damage == 0)
				damage = 1;
			
			called.getAI().addAttackDesire(attacker, ((1.0 * damage) / (called.getStatus().getLevel() + 7)) * 30);
		}
	}
	
	// EventHandler DESIRE_MANIPULATION(speller, desire)
	// {
	// MACRO<return_from_outside_attack>(speller)
	//
	// MakeAttackEvent(speller, desire, 0);
	//
	// if(InMyTerritory(target) == 0)
	// return;
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
				Party party0 = TimeAttackEventRoom.getInstance().getParty(getNpcIntAIParam(npc, "RoomIndex"), getNpcIntAIParam(npc, "SSQPart"));
				Party party1 = caster.getParty();
				if (party0 == null || party1 == null)
					npc._i_ai1 = 1;
				else if (party0 != party1/* && caster instanceof Player */)
					npc._i_ai1 = 1;
			}
		}
		else
			npc.broadcastNpcSay("Puncture: The monster’s affiliation is unclear.");
			
		// if( RoomIndex != 0 && SSQPart != 0 )
		// {
		// if( myself::InMyTerritory(speller) == 0 )
		// {
		// myself.alive = 1;
		// }
		// else
		// {
		// party0 = gg::GetPartyFromEventRoom(RoomIndex, SSQPart);
		// party1 = gg::GetParty(speller);
		// if( myself::IsNullParty(party0) || myself::IsNullParty(party1) )
		// {
		// myself.alive = 1;
		// }
		// else if( party0.id != party1.id && speller.is_pc == 1 )
		// {
		// myself.alive = 1;
		// }
		// }
		// }
		// else
		// {
		// myself::Say("uё : ¬¤0X ЊЌt €„…");
		// }
	}
}