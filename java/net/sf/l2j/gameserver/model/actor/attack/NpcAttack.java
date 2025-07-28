package net.sf.l2j.gameserver.model.actor.attack;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * This class groups all attack data related to a {@link Npc}.
 */
public class NpcAttack extends CreatureAttack<Npc>
{
	public NpcAttack(Npc actor)
	{
		super(actor);
	}
	
	@Override
	public boolean canAttack(Creature target)
	{
		if (!super.canAttack(target))
			return false;
		
		return !target.isFakeDeath();
	}
	
	@Override
	protected void onFinishedAttackBow(Creature mainTarget)
	{
		for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.ATTACK_FINISHED))
			quest.onAttackFinished(_actor, mainTarget);
		
		super.onFinishedAttackBow(mainTarget);
	}
	
	@Override
	protected void onFinishedAttack(Creature mainTarget)
	{
		for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.ATTACK_FINISHED))
			quest.onAttackFinished(_actor, mainTarget);
		
		super.onFinishedAttack(mainTarget);
	}
}