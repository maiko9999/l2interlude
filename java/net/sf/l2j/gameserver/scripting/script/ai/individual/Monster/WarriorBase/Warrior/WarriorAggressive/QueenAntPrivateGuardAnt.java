package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorAggressive;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class QueenAntPrivateGuardAnt extends WarriorAggressive
{
	public QueenAntPrivateGuardAnt()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorAggressive");
	}
	
	public QueenAntPrivateGuardAnt(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29004, // guard_ant
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("3001", npc, null, 90000 + Rnd.get(240000));
		startQuestTimerAtFixedRate("3002", npc, null, 10000, 10000);
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3001"))
		{
			if (Rnd.get(100) < 66)
				npc.getAI().getAggroList().randomizeAttack();
			
			startQuestTimer("3001", npc, null, 90000 + Rnd.get(240000));
		}
		
		if (name.equalsIgnoreCase("3002"))
		{
			if (!npc.isInMyTerritory())
			{
				npc.teleportTo(npc.getSpawnLocation(), 0);
				npc.removeAllAttackDesire();
			}
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			double f0 = getHateRatio(called, attacker);
			f0 = (((1.0 * damage) / (called.getStatus().getLevel() + 7)) + ((f0 / 100) * ((1.0 * damage) / (called.getStatus().getLevel() + 7))));
			called.getAI().addAttackDesire(attacker, (int) (f0 * 50));
		}
	}
}