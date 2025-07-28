package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.TimeAttackEventRoom;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SsqEventBomb extends SsqEventBasicWarrior
{
	public SsqEventBomb()
	{
		super("ai/ssq");
	}
	
	public SsqEventBomb(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18299,
		18301,
		18303,
		18305,
		18307,
		18309,
		18311,
		18313,
		18315,
		18317
	};
	
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
			}
		}
		super.onScriptEvent(npc, eventId, arg1, arg2);
	}
	
	@Override
	public void onMoveToFinished(Npc npc, int x, int y, int z)
	{
		npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.DD_MAGIC), 1000000);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		if (success)
			npc.doDie(npc);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
	}
	
	// EventHandler DESIRE_MANIPULATION(speller, desire)
	// {
	// }
	
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
		
		final Player player = attacker.getActingPlayer();
		if (player != null && player.getClassId().getType() == ClassType.MYSTIC)
			npc._i_ai1 = 1;
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