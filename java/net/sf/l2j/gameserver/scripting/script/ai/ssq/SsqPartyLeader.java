package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SsqPartyLeader extends SsqEventBasicWarrior
{
	public SsqPartyLeader()
	{
		super("ai/ssq");
	}
	
	public SsqPartyLeader(String descr)
	{
		super(descr);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._weightPoint = 10;
		
		createPrivates(npc);
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.getSpawn().isInMyTerritory(attacker))
			return;
		
		// The character is Desire that enters when you take 5% damage. Increases in proportion to damage.
		if (attacker instanceof Playable)
		{
			if (damage == 0)
				damage = 1;
			
			npc.getAI().addAttackDesire(attacker, (1.0 * damage / (npc.getStatus().getLevel() + 7)) * 100);
		}
		
		// Responds to hold magic
		final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
		if (mostHated != null)
		{
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
		super.onAttacked(npc, attacker, damage, skill);
	}
}