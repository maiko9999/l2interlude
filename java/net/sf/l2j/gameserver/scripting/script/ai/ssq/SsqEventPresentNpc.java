package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.model.TimeAttackEventRoom;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SsqEventPresentNpc extends DefaultNpc
{
	public SsqEventPresentNpc()
	{
		super("ai/ssq");
	}
	
	public SsqEventPresentNpc(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18109,
		18110,
		18111,
		18112,
		18113,
		18114,
		18115,
		18116,
		18117,
		18118
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai1 = 0;
		startQuestTimer("1001", npc, null, 2 * 60 * 1000);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("1001"))
			npc.deleteMe();
		
		return null;
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
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
		
		if (skill != null && skill.getId() != 0)
			npc.getAI().addFleeDesire(attacker, Config.MAX_DRIFT_RANGE, 100);
		else if (Rnd.get(100) < 30)
			npc.getAI().addFleeDesire(attacker, Config.MAX_DRIFT_RANGE, 100);
		
		final Player player = attacker.getActingPlayer();
		if (player != null && player.getClassId().getType() == ClassType.MYSTIC)
			npc._i_ai1 = 1;
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (skill != null && skill.getId() != 0)
			called.getAI().addFleeDesire(attacker, Config.MAX_DRIFT_RANGE, 100);
		else if (Rnd.get(100) < 30)
			called.getAI().addFleeDesire(attacker, Config.MAX_DRIFT_RANGE, 100);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (killer instanceof Player player)
		{
			if (npc._i_ai1 == 1)
				ssqEventGiveItem(npc, player, 3);
			else
				ssqEventGiveItem(npc, player, 30);
		}
	}
}