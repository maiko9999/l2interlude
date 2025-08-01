package net.sf.l2j.gameserver.scripting.script.ai.boss.orfen;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.PartyLeaderWarrior.PartyLeaderWarriorAggressive;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Orfen extends PartyLeaderWarriorAggressive
{
	public Orfen()
	{
		super("ai/boss/orfen");
	}
	
	public Orfen(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29014 // orfen
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));
		
		npc._flag = 0;
		npc._i_ai0 = 0;
		
		startQuestTimerAtFixedRate("3001", npc, null, 10000, 10000);
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3001"))
		{
			if (npc._i_ai0 == 1)
			{
				if (npc.getStatus().getHpRatio() > 0.95)
				{
					final int i6 = Rnd.get(100);
					if (i6 < 33)
						npc.teleportTo(getNpcIntAIParam(npc, "b03_x2"), getNpcIntAIParam(npc, "b03_y2"), getNpcIntAIParam(npc, "b03_z2"), 0);
					else if (i6 < 66)
						npc.teleportTo(getNpcIntAIParam(npc, "b03_x3"), getNpcIntAIParam(npc, "b03_y3"), getNpcIntAIParam(npc, "b03_z3"), 0);
					else
						npc.teleportTo(getNpcIntAIParam(npc, "b03_x4"), getNpcIntAIParam(npc, "b03_y4"), getNpcIntAIParam(npc, "b03_z4"), 0);
					
					npc._i_ai0 = 0;
					npc._flag++;
					
					npc.removeAllAttackDesire();
				}
				else if (!npc.isInMyTerritory())
					npc.teleportTo(getNpcIntAIParam(npc, "b03_x1"), getNpcIntAIParam(npc, "b03_y1"), getNpcIntAIParam(npc, "b03_z1"), 0);
			}
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onOutOfTerritory(Npc npc)
	{
		npc.removeAllAttackDesire();
		
		if (npc._i_ai0 == 1)
			npc.teleportTo(getNpcIntAIParam(npc, "b03_x1"), getNpcIntAIParam(npc, "b03_y1"), getNpcIntAIParam(npc, "b03_z1"), 0);
		else if (npc._i_ai0 == 0)
		{
			final int i6 = Rnd.get(100);
			if (i6 < 33)
				npc.teleportTo(getNpcIntAIParam(npc, "b03_x2"), getNpcIntAIParam(npc, "b03_y2"), getNpcIntAIParam(npc, "b03_z2"), 0);
			else if (i6 < 66)
				npc.teleportTo(getNpcIntAIParam(npc, "b03_x3"), getNpcIntAIParam(npc, "b03_y3"), getNpcIntAIParam(npc, "b03_z3"), 0);
			else
				npc.teleportTo(getNpcIntAIParam(npc, "b03_x4"), getNpcIntAIParam(npc, "b03_y4"), getNpcIntAIParam(npc, "b03_z4"), 0);
		}
		
		npc._flag++;
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!Config.RAID_DISABLE_CURSE && attacker.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
			npc.getAI().addCastDesire(attacker, 4515, 1, 1000000);
		
		final L2Skill hinderStrider = SkillTable.getInstance().getInfo(4258, 1);
		if (attacker.isRiding() && getAbnormalLevel(attacker, hinderStrider) <= 0)
			npc.getAI().addCastDesire(attacker, hinderStrider, 1000000);
		
		if (npc._i_ai0 == 0 && npc.getStatus().getHpRatio() < 0.5)
		{
			npc.teleportTo(getNpcIntAIParam(npc, "b03_x1"), getNpcIntAIParam(npc, "b03_y1"), getNpcIntAIParam(npc, "b03_z1"), 0);
			
			npc._i_ai0 = 1;
			npc._flag++;
			
			npc.removeAllAttackDesire();
		}
		
		if (attacker instanceof Playable)
		{
			if (npc.distance2D(attacker) > 300 && Rnd.get(100) < 10 && npc.distance2D(attacker) < 1000)
			{
				final int i0 = Rnd.get(100);
				if (i0 < 33)
					npc.broadcastNpcSay(NpcStringId.ID_1000028, attacker.getName());
				else if (i0 < 66)
					npc.broadcastNpcSay(NpcStringId.ID_1000029, attacker.getName());
				else
					npc.broadcastNpcSay(NpcStringId.ID_1000030, attacker.getName());
				
				attacker.teleportTo(npc.getPosition(), 0);
				
				npc.getAI().addCastDesire(attacker, 4063, 1, 1000000);
			}
			else if (Rnd.get(100) < 20)
				npc.getAI().addCastDesire(npc, 4064, 1, 1000000);
			
			npc.getAI().addAttackDesire(attacker, (int) (((((double) damage) / npc.getStatus().getMaxMp()) / 0.05) * 10000));
		}
	}
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (!npc.isDead())
		{
			if (!Config.RAID_DISABLE_CURSE && caster.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
			{
				npc.getAI().addCastDesire(caster, 4215, 1, 1000000);
				return;
			}
			
			if (skill.getAggroPoints() > 0 && Rnd.get(100) < 20 && npc.distance2D(caster) < 1000)
			{
				final int i0 = Rnd.get(100);
				if (i0 < 25)
					npc.broadcastNpcSay(NpcStringId.ID_1000028, caster.getName());
				else if (i0 < 50)
					npc.broadcastNpcSay(NpcStringId.ID_1000029, caster.getName());
				else if (i0 < 75)
					npc.broadcastNpcSay(NpcStringId.ID_1000030, caster.getName());
				else
					npc.broadcastNpcSay(NpcStringId.ID_1000031, caster.getName());
				
				caster.teleportTo(npc.getPosition(), 0);
				
				npc.getAI().addCastDesire(caster, 4063, 1, 1000000);
			}
			
			super.onSeeSpell(npc, caster, skill, targets, isPet);
		}
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		if (caller != called && target instanceof Playable)
			called.getAI().addAttackDesire(target, (int) (((((((double) damage) / called.getStatus().getMaxMp()) / 0.05) * damage) * caller._weightPoint) * 1000));
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
			called.getAI().addAttackDesire(attacker, (int) (((((double) damage) / called.getStatus().getMaxMp()) / 0.05) * 3000));
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));
	}
}