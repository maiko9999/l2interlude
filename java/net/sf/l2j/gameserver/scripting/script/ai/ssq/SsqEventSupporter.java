package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SsqEventSupporter extends SsqEventBasicWarrior
{
	public SsqEventSupporter()
	{
		super("ai/ssq");
	}
	
	public SsqEventSupporter(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18017,
		18018,
		18027,
		18028,
		18037,
		18038,
		18047,
		18048,
		18057,
		18058,
		18067,
		18068,
		18077,
		18078,
		18087,
		18088,
		18097,
		18098,
		18107,
		18108
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("2000", npc, null, 3000);
		super.onCreated(npc);
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
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		// Responds to hold magic.
		final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
		if (mostHated != null)
		{
			if (npc.getMove().getGeoPathFailCount() >= 10 && attacker == mostHated && npc.getStatus().getHpRatio() != 1.0)
			{
				npc.abortAll(false);
				npc.teleportTo(attacker.getX(), attacker.getY(), attacker.getZ(), 0);
			}
			
			final int i0 = getAbnormalLevel(npc, 1201, 1);
			if (i0 >= 0 && npc.distance2D(attacker) > 40)
			{
				if (npc.getAttack().canAttack(mostHated))
				{
					if (attacker instanceof Playable)
					{
						if (damage == 0)
							damage = 1;
						
						npc.getAI().addAttackDesire(attacker, (1.0 * damage / (npc.getStatus().getLevel() + 7)) * 100);
					}
				}
				else
				{
					npc.getAI().getAggroList().stopHate(mostHated);
					
					if (attacker instanceof Playable)
					{
						if (damage == 0)
							damage = 1;
						
						npc.getAI().addAttackDesire(attacker, (1.0 * damage / (npc.getStatus().getLevel() + 7)) * 100);
					}
				}
			}
		}
		
		if (getNpcIntAIParam(npc, "IsStrong") == 0)
		{
			if (Rnd.get(100) < 3)
				npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL), 1000000000);
		}
		else if (getNpcIntAIParam(npc, "IsStrong") == 1)
		{
			if (Rnd.get(100) < 20)
				npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL), 1000000000);
		}
		
		if (attacker instanceof Playable)
		{
			if (damage == 0)
				damage = 1;
			
			npc.getAI().addAttackDesire(attacker, (1.0 * damage / (npc.getStatus().getLevel() + 7)) * 100);
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2000"))
		{
			npc.lookNeighbor(600);
			startQuestTimer("2000", npc, null, 3000);
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (killer instanceof Player player)
		{
			if (npc._i_ai1 == 1)
				ssqEventGiveItem(npc, player, 2);
			else
			{
				if (getNpcIntAIParam(npc, "IsStrong") == 0)
					ssqEventGiveItem(npc, player, 3);
				else
					ssqEventGiveItem(npc, player, 6);
			}
		}
		
		broadcastScriptEvent(npc, 10011, 0, 1500);
	}
}