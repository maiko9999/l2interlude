package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.RaidBoss.RaidBossParty;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.RaidBoss.RaidBossStandard;

public class RaidBossParty extends RaidBossStandard
{
	public RaidBossParty()
	{
		super("ai/individual/Monster/RaidBoss/RaidBossAlone/RaidBossParty");
	}
	
	public RaidBossParty(String descr)
	{
		super(descr);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._weightPoint = 10;
		npc.getMinions().clear();
		
		createPrivates(npc);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		if (caller != called)
		{
			if (target.getStatus().getLevel() <= (called.getStatus().getLevel() + 8))
			{
				if (target instanceof Playable)
				{
					if (damage == 0)
						damage = 1;
					
					called.getAI().addAttackDesire(target, (int) (((1.0 * damage) / (called.getStatus().getLevel() + 7)) * 20000));
				}
			}
			
			if (called.getMove().getGeoPathFailCount() > (8 + Rnd.get(13)))
			{
				final Creature topDesireTarget = called.getAI().getTopDesireTarget();
				if (topDesireTarget != null && called.distance2D(topDesireTarget) < 1000)
					called.teleportTo(topDesireTarget.getPosition(), 0);
				else
				{
					called.removeAllAttackDesire();
					
					if (target instanceof Playable)
					{
						if (damage == 0)
							damage = 1;
						
						called.getAI().addAttackDesire(target, (int) (((1.0 * damage) / (called.getStatus().getLevel() + 7)) * 20000));
					}
					called.teleportTo(target.getPosition(), 0);
				}
			}
		}
		
		if (called.isInsideZone(ZoneId.PEACE))
		{
			called.teleportTo(called.getSpawnLocation(), 0);
			called.removeAllAttackDesire();
		}
	}
	
	@Override
	public void onPartyDied(Npc caller, Npc called)
	{
		if (caller != called && called.isMaster() && !called.isDead())
			caller.scheduleRespawn((caller.getSpawn().getRespawnDelay() * 1000));
	}
}