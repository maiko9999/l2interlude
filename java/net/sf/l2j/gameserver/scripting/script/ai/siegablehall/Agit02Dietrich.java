package net.sf.l2j.gameserver.scripting.script.ai.siegablehall;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Agit02Dietrich extends DefaultNpc
{
	public Agit02Dietrich()
	{
		super("ai/siegeablehall");
	}
	
	public Agit02Dietrich(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		35408
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc.broadcastNpcShout(NpcStringId.ID_1000277);
		
		startQuestTimerAtFixedRate("1001", npc, null, 1000, 60000);
		
		npc._c_ai0 = npc;
		npc._i_ai0 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("1001"))
		{
			if (npc.hasMaster() && !npc.isInMyTerritory() && Rnd.get(3) < 1)
			{
				npc.teleportTo(npc.getMaster().getPosition(), 0);
				npc.removeAllAttackDesire();
			}
			
			if (Rnd.get(5) < 1)
				npc.getAI().getAggroList().randomizeAttack();
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		if (called._c_ai0 != called && Rnd.get(3) < 1 && called.getSpawn().isInMyTerritory(called._c_ai0) && called.getCast().canAttemptCast(called, SkillTable.getInstance().getInfo(4238, 1)))
			called.getAI().addMoveToDesire(called._c_ai0.getPosition(), 100000000);
		
		if (target instanceof Playable)
			called.getAI().addAttackDesire(target, (((damage * 1.0 / called.getStatus().getMaxHp()) / 0.05) * damage) * caller._weightPoint / 1000000);
		
		if (called._i_ai0 == 0 && called.hasMaster() && called.getMaster().getStatus().getHpRatio() < 0.05)
		{
			called._i_ai0 = 1;
			called.getAI().addCastDesire(called, 4235, 1, 1000000000);
			called.broadcastNpcSay(NpcStringId.ID_1000280);
		}
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		if (skill.getId() == 4235)
		{
			npc.teleportTo(177134, -18807, -2263, 0);
			npc.removeAllAttackDesire();
		}
	}
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		switch (caster.getClassId())
		{
			case BISHOP, PROPHET, ELVEN_ELDER, SHILLIEN_ELDER, OVERLORD, WARCRYER:
				npc._c_ai0 = caster;
				break;
		}
		
		if (skill.getAggroPoints() > 0 && npc.getAI().getCurrentIntention().getType() == IntentionType.ATTACK && npc.getAI().getTopDesireTarget() == caster)
			npc.getAI().addAttackDesire(caster, (((skill.getAggroPoints() * 1.0 / npc.getStatus().getMaxHp()) / 0.05) * 150));
	}
	
	@Override
	public void onMoveToFinished(Npc npc, int x, int y, int z)
	{
		npc.getAI().addCastDesire(npc, 4238, 1, 1000000);
	}
}