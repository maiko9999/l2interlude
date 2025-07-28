package net.sf.l2j.gameserver.model.actor.move;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.location.Location;

public class SummonMove extends CreatureMove<Summon>
{
	public SummonMove(Summon actor)
	{
		super(actor);
	}
	
	@Override
	public void avoidAttack(Creature attacker)
	{
		final Player owner = _actor.getOwner();
		
		if (owner == null || owner == attacker || !owner.isIn3DRadius(_actor, 2 * Config.SUMMON_DRIFT_RANGE) || !owner.isInCombat())
			return;
		
		if (_actor.getAI().getCurrentIntention().getType() != IntentionType.IDLE && _actor.getAI().getCurrentIntention().getType() != IntentionType.FOLLOW)
			return;
		
		if (_actor.isMoving() || _actor.isDead() || _actor.isMovementDisabled())
			return;
		
		final Location fleeLoc = owner.getPosition().clone();
		fleeLoc.addStrictOffset(Config.SUMMON_DRIFT_RANGE);
		fleeLoc.set(GeoEngine.getInstance().getValidLocation(_actor.getPosition(), fleeLoc));
		
		_actor.getAI().tryToMoveTo(fleeLoc, null);
	}
	
	@Override
	protected void offensiveFollowTask(Creature target, int offset)
	{
		// No follow task, return.
		if (_followTask == null)
			return;
		
		// Invalid pawn to follow, or the pawn isn't registered on knownlist.
		if (!_actor.knows(target))
		{
			_actor.getAI().setFollowStatus(false);
			_actor.getAI().tryToIdle();
			return;
		}
		
		final Location targetLoc = _actor.getPosition().clone();
		targetLoc.setLocationMinusOffset(target.getPosition(), target.getStatus().getMoveSpeed() / -2);
		
		// Don't bother moving if already in radius.
		if (getMoveType() == MoveType.GROUND ? _actor.isIn2DRadius(targetLoc, offset) : _actor.isIn3DRadius(targetLoc, offset))
			return;
		
		_pawn = target;
		_offset = offset;
		
		moveToLocation(targetLoc, true);
	}
	
	@Override
	protected void friendlyFollowTask(Creature target, int offset)
	{
		// No follow task, return.
		if (_followTask == null || _actor.isMovementDisabled())
			return;
		
		// Invalid pawn to follow, or the pawn isn't registered on knownlist.
		if (!_actor.knows(target))
		{
			_actor.getAI().setFollowStatus(false);
			_actor.getAI().tryToIdle();
			return;
		}
		
		final Location targetLoc = _actor.getPosition().clone();
		targetLoc.setLocationMinusOffset(target.getPosition(), (int) (offset + target.getCollisionRadius() + _actor.getCollisionRadius()));
		
		// Don't bother moving if already in radius.
		if (getMoveType() == MoveType.GROUND ? _actor.isIn2DRadius(targetLoc, offset) : _actor.isIn3DRadius(targetLoc, offset))
			return;
		
		_pawn = null;
		_offset = 0;
		
		if (target.getActingPlayer() != _actor.getOwner())
		{
			_actor.tryToPassBoatEntrance(targetLoc, false);
			return;
		}
		
		moveToLocation(targetLoc, true);
	}
}