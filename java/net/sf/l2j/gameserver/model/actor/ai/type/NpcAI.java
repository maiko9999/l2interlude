package net.sf.l2j.gameserver.model.actor.ai.type;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.Desire;
import net.sf.l2j.gameserver.model.actor.ai.DesireQueue;
import net.sf.l2j.gameserver.model.actor.ai.Intention;
import net.sf.l2j.gameserver.model.actor.container.attackable.AggroList;
import net.sf.l2j.gameserver.model.actor.container.attackable.HateList;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.entity.CursedWeapon;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.WalkerLocation;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.WalkerTaskManager;

public class NpcAI<T extends Npc> extends CreatureAI<T>
{
	private static final int SOCIAL_INTERVAL = 12000;
	
	protected final DesireQueue _desireQueue;
	protected final Set<Creature> _seenCreatures = ConcurrentHashMap.newKeySet();
	protected final AggroList _aggroList;
	protected final HateList _hateList;
	
	private ScheduledFuture<?> _clearAggroTask;
	
	private int _routeIndex;
	private boolean _isOnARoute;
	private boolean _isReversePath;
	
	private int _runAiStep;
	private int _lifeTime;
	
	private boolean _isOOT;
	
	private long _lastSocialBroadcast;
	
	private Creature _topDesireTarget;
	
	protected Desire _lastDesire;
	protected Desire _nextDesire;
	
	public NpcAI(T npc)
	{
		super(npc);
		
		_desireQueue = new DesireQueue();
		_seenCreatures.clear();
		_aggroList = new AggroList(npc);
		_hateList = new HateList(npc);
	}
	
	@Override
	public void stopAITask()
	{
		_isOnARoute = false;
		
		super.stopAITask();
	}
	
	@Override
	public void thinkIdle()
	{
		_actor.abortAll(true);
		_actor.forceWalkStance();
		
		// Retrieve scripts associated to NO_DESIRE.
		for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.NO_DESIRE))
			quest.onNoDesire(_actor);
	}
	
	@Override
	protected void thinkSocial()
	{
		clearCurrentDesire();
		
		if (_actor.denyAiAction())
			return;
		
		_lastSocialBroadcast = System.currentTimeMillis() + _currentIntention.getTimer();
		
		_actor.getMove().stop();
		_actor.broadcastPacket(new SocialAction(_actor, _currentIntention.getItemObjectId()));
	}
	
	@Override
	protected void thinkFollow()
	{
		// Follow should be done once every 2 seconds
		if (_runAiStep % 2 == 0)
			return;
		
		_actor.setWalkOrRun(true);
		
		if (_actor.denyAiAction() || _actor.isMovementDisabled())
			return;
		
		final Creature target = _currentIntention.getFinalTarget();
		
		// If target is null, self or dead, stop the follow Desire.
		if (target == null || _actor == target || target.isDead())
		{
			clearCurrentDesire();
			return;
		}
		
		// Actor is stuck, reset its position.
		if (_actor.getMove().getGeoPathFailCount() >= 10)
		{
			_actor.teleportTo(target.getPosition(), 10);
			return;
		}
		
		// If follow state is about following a master, recalculate follow points and move the NPC if needed, based on other minions' positions.
		if (_actor.hasMaster() && target == _actor.getMaster())
		{
			List<Integer> followSlots = ((Npc) target).getFollowSlots();
			
			// Do not initiate follow if distance change is too low
			if (followSlots.stream().filter(Objects::nonNull).count() == _actor.getMaster().getMinions().size() && _actor.getLastFollowingLoc() != null && target.distance2D(_actor.getLastFollowingLoc()) < 5)
				return;
			
			// Randomly process the follow state.
			if (Rnd.get(100) >= 70)
				return;
			
			final int followDistance = 150;
			final Location masterLoc = target.getPosition().clone();
			
			int rndNum = Rnd.get(1000000);
			int slotHolder = -1;
			double distHolder = 10000.0;
			
			Location finalLoc = _actor.getPosition().clone();
			
			for (int i = 0; i < 8; i++)
			{
				int idx = (i + rndNum) % 8;
				
				if (_lastDesire == null || _lastDesire.getType() != IntentionType.FOLLOW)
					followSlots.set(idx, null);
				
				double tmpX = Math.cos(0.785 * idx) * followDistance;
				double tmpY = Math.sin(0.785 * idx) * followDistance;
				Location newPos = new Location((int) (masterLoc.getX() + tmpX), (int) (masterLoc.getY() + tmpY), masterLoc.getZ());
				
				final Integer objectId = followSlots.get(idx);
				if (objectId != null)
				{
					if (objectId == _actor.getObjectId())
						followSlots.set(idx, null);
					else
					{
						final Creature slotCreature = (Creature) World.getInstance().getObject(objectId);
						if (slotCreature != null && slotCreature.distance2D(newPos) <= 100.0)
							continue;
					}
				}
				
				final double distanceToNewPos = _actor.distance2D(newPos);
				if (distHolder > distanceToNewPos)
				{
					distHolder = distanceToNewPos;
					slotHolder = idx;
					finalLoc = newPos;
				}
			}
			
			if (slotHolder != -1)
				followSlots.set(slotHolder, _actor.getObjectId());
			
			final int heading = (int) ((Math.atan2(_actor.getY() - masterLoc.getY(), _actor.getX() - masterLoc.getX()) * 360.0 / (2 * Math.PI) + 360.0) % 360);
			final int newSlot = (heading + 22) / 45;
			final int distBetween = (int) _actor.distance3D(masterLoc);
			
			if (followDistance > distBetween && newSlot == slotHolder)
				finalLoc = _actor.getPosition().clone();
			
			_actor.getMove().maybeMoveToLocation(finalLoc, 0, true, false);
			_actor.setLastFollowingLoc(masterLoc);
		}
		else if (!_actor.isIn2DRadius(target, 150) && Rnd.get(100) > 50 && !_actor.isMoving())
		{
			final double distance = Math.sqrt(Rnd.nextDouble()) * 300;
			final double angle = Rnd.nextDouble() * Math.PI * 2;
			
			Location toMoveLoc = new Location((int) (distance * Math.cos(angle) + target.getX()), (int) (distance * Math.sin(angle) + target.getY()), target.getZ());
			
			_actor.getMove().maybeMoveToLocation(toMoveLoc, 0, true, false);
		}
	}
	
	@Override
	protected void thinkNothing()
	{
		// Nothing
	}
	
	@Override
	protected ItemInstance thinkPickUp()
	{
		if (_actor.denyAiAction())
			return null;
		
		final WorldObject target = World.getInstance().getObject(_currentIntention.getItemObjectId());
		if (!(target instanceof ItemInstance item) || isTargetLost(target))
			return null;
		
		if (item.getLocation() != ItemLocation.VOID)
			return null;
		
		if (_actor.getMove().maybeMoveToLocation(target.getPosition(), 36, false, false))
			return null;
		
		for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.PICKED_ITEM))
			quest.onPickedItem(_actor, item);
		
		final CursedWeapon cw = CursedWeaponManager.getInstance().getCursedWeapon(item.getItemId());
		if (cw != null)
			cw.endOfLife();
		else
			item.decayMe();
		
		clearCurrentDesire();
		
		return item;
	}
	
	@Override
	protected void thinkMoveRoute()
	{
		if (!_isOnARoute || _actor.getNpcId() == 32108)
			moveToNextPoint();
	}
	
	@Override
	protected void thinkMoveTo()
	{
		if (_actor.getPosition().equals(_currentIntention.getLoc()))
		{
			for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.MOVE_TO_FINISHED))
				quest.onMoveToFinished(_actor, _actor.getX(), _actor.getY(), _actor.getZ());
			
			clearCurrentDesire();
			
			return;
		}
		
		super.thinkMoveTo();
	}
	
	@Override
	protected void onEvtArrived()
	{
		switch (_currentIntention.getType())
		{
			case MOVE_TO, FLEE:
				for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.MOVE_TO_FINISHED))
					quest.onMoveToFinished(_actor, _actor.getX(), _actor.getY(), _actor.getZ());
			case WANDER:
				clearCurrentDesire();
				break;
			
			case FOLLOW:
				return;
		}
		
		if (_actor.getPosition().equals(_actor.getSpawnLocation()))
			_actor.getPosition().setHeading(_actor.getSpawnLocation().getHeading());
		
		if (!_actor.isInMyTerritory())
		{
			if (!_isOOT)
			{
				for (Quest quest : _actor.getTemplate().getEventQuests(EventHandler.OUT_OF_TERRITORY))
					quest.onOutOfTerritory(_actor);
				
				if (_clearAggroTask != null)
				{
					_clearAggroTask.cancel(true);
					_clearAggroTask = null;
				}
				
				_clearAggroTask = ThreadPool.scheduleAtFixedRate(() ->
				{
					if (_actor.isInMyTerritory())
						return;
					
					final long currentTime = System.currentTimeMillis();
					for (AggroInfo ai : _aggroList.values())
					{
						if (currentTime - ai.getTimestamp() >= 90000)
							ai.stopHate();
					}
				}, 100, 10000);
				
				_isOOT = true;
			}
		}
		else
		{
			_isOOT = false;
			
			if (_clearAggroTask != null)
			{
				_clearAggroTask.cancel(true);
				_clearAggroTask = null;
			}
		}
		
		// Don't move if current intention is not MOVE_ROUTE
		if (_currentIntention.getType() != IntentionType.MOVE_ROUTE)
		{
			_isOnARoute = false;
			return;
		}
		
		// Retrieve walking route, if any.
		final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(_currentIntention.getRouteName(), _actor.getTemplate().getAlias());
		if (route.isEmpty())
		{
			_isOnARoute = false;
			return;
		}
		
		_isOnARoute = true;
		
		// Retrieve current node.
		final WalkerLocation node = route.get(_routeIndex);
		
		// If node got a NpcStringId, broadcast it.
		if (node.getNpcStringId() != null)
			_actor.broadcastNpcSay(node.getNpcStringId());
		
		// We freeze the NPC and store it on WalkerTaskManager, which will release it in the future.
		if (node.getDelay() > 0)
		{
			// If node got a SocialAction id, broadcast it.
			if (node.getSocialId() > 0)
				_actor.broadcastPacket(new SocialAction(_actor, node.getSocialId()));
			
			// Delay the movement.
			WalkerTaskManager.getInstance().add(_actor, node.getDelay());
		}
		else
			moveToNextPoint();
	}
	
	@Override
	protected void onEvtArrivedBlocked()
	{
		switch (_currentIntention.getType())
		{
			case MOVE_TO, FLEE, WANDER:
				clearCurrentDesire();
				break;
		}
		super.onEvtArrivedBlocked();
	}
	
	@Override
	protected void onEvtFinishedCasting()
	{
		runAI(false);
	}
	
	@Override
	protected void onEvtTeleported()
	{
		// Do nothing
	}
	
	public void setBackToPeace()
	{
		_aggroList.clear();
		_hateList.clear();
	}
	
	public final AggroList getAggroList()
	{
		return _aggroList;
	}
	
	public final HateList getHateList()
	{
		return _hateList;
	}
	
	public int getRouteIndex()
	{
		return _routeIndex;
	}
	
	public boolean isReversePath()
	{
		return _isReversePath;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
	
	public void resetLifeTime()
	{
		_lifeTime = 0;
	}
	
	public Creature getTopDesireTarget()
	{
		return _topDesireTarget;
	}
	
	public void setTopDesireTarget(Creature target)
	{
		_topDesireTarget = target;
	}
	
	public void runAI()
	{
		runAI(true);
	}
	
	public void runAI(boolean updateTick)
	{
		if (_lastDesire != null && _lastDesire.getType() == IntentionType.IDLE)
			getDesires().remove(_lastDesire);
		
		if (updateTick)
		{
			// ON_CREATURE_SEE implementation.
			final List<Quest> scripts = _actor.getTemplate().getEventQuests(EventHandler.SEE_CREATURE);
			if (!scripts.isEmpty())
			{
				// Get all visible objects inside its Aggro Range
				_actor.forEachKnownType(Playable.class, pl ->
				{
					// Do not trigger event for specific Player conditions.
					final Player player = pl.getActingPlayer();
					if (player.isSpawnProtected() || player.isFlying() || !player.getAppearance().isVisible())
						return;
					
					// Non-Boss type NPCs have fixed 400 Aggro Range
					final boolean isInRange = _actor.isIn3DRadius(pl, _actor.getSeeRange());
					
					if (!_actor.isRaidBoss() && _seenCreatures.contains(pl))
					{
						if (_actor instanceof Guard)
							_seenCreatures.remove(pl);
						else if (!isInRange)
							_seenCreatures.remove(pl);
					}
					else if (_actor.isRaidBoss() && isInRange && Math.abs(pl.getZ() - _actor.getZ()) <= 500)
					{
						if (pl.isMoving() || _actor.isMoving())
						{
							for (Quest quest : scripts)
								quest.onSeeCreature(_actor, pl);
						}
					}
					else if (isInRange)
					{
						if (pl.isSilentMoving() && !_actor.getTemplate().canSeeThrough())
							return;
						
						_seenCreatures.add(pl);
						
						for (Quest quest : scripts)
							quest.onSeeCreature(_actor, pl);
					}
				});
			}
		}
		
		// Remove invalid Desires.
		_desireQueue.getDesires().removeIf(d -> d.getType() == IntentionType.CAST && (d.getWeight() <= 0 || !_actor.getCast().meetsHpMpDisabledConditions(d.getFinalTarget(), d.getSkill())));
		_desireQueue.getDesires().removeIf(d -> d.getFinalTarget() != null && (!_actor.knows(d.getFinalTarget()) || d.getFinalTarget().isAlikeDead()));
		_desireQueue.getDesires().removeIf(d -> d.getTarget() != null && (!_actor.knows(d.getTarget())));
		
		if (!_actor.isOutOfControl())
		{
			final boolean instantRun = _lifeTime == 0 && getDesires().stream().anyMatch(d -> d.getType() == IntentionType.ATTACK);
			
			// Don't choose a new intention if already doing something.
			if (!_actor.getCast().isCastingNow() && (_lifeTime > 0 || instantRun) && _lastSocialBroadcast <= System.currentTimeMillis())
			{
				if (!(_currentIntention.getType() == IntentionType.FLEE && getDesires().contains(_currentIntention)))
				{
					if (getDesires().isEmpty() && _nextDesire == null)
					{
						if (_currentIntention.getType() != IntentionType.IDLE)
							doIdleIntention();
					}
					else if (!_actor.getAttack().isAttackingNow())
					{
						final Desire toDoDesire = (_nextDesire != null) ? _nextDesire : _desireQueue.getLast();
						
						if (toDoDesire != null && !(_currentIntention.getType() == IntentionType.WANDER && toDoDesire.getType() == IntentionType.WANDER))
						{
							if (toDoDesire.getType() == IntentionType.ATTACK && (_lastDesire == null || (_lastDesire.getType() == IntentionType.WANDER || _lastDesire.getType() == IntentionType.IDLE)))
								_nextDesire = toDoDesire;
							else
								_nextDesire = null;
							
							if (toDoDesire.getType() != IntentionType.MOVE_ROUTE)
								_isOnARoute = false;
							
							doIntention(toDoDesire);
							
							_topDesireTarget = toDoDesire.getFinalTarget();
							
							_lastDesire = toDoDesire;
						}
					}
				}
			}
		}
		
		if (updateTick)
		{
			if (getDesires().isEmpty() && _lifeTime > 0 && !_actor.getCast().isCastingNow())
			{
				thinkIdle();
				
				_topDesireTarget = null;
			}
			
			// Rise the timer.
			_runAiStep++;
			
			// Lifetime is updated no matter what.
			_lifeTime++;
			
			// If a multiple of 3 was reached.
			if (_runAiStep % 3 == 0)
			{
				_aggroList.refresh();
				_hateList.refresh();
				
				// Automatically decrease the attack aggro and the Desires weight every 3 seconds.
				_aggroList.reduceAllHate(6.6);
				_desireQueue.autoDecreaseWeight();
				
				if (_currentIntention.getType() == IntentionType.ATTACK || _currentIntention.getType() == IntentionType.CAST)
					_actor.setWalkOrRun(true);
				
				_runAiStep = 0;
			}
		}
	}
	
	/**
	 * Move the {@link Npc} to the next {@link WalkerLocation} of his route.
	 */
	public void moveToNextPoint()
	{
		if (_actor.getNpcId() == 32108)
		{
			// Retrieve walking route, if any.
			final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(_currentIntention.getRouteName(), _actor.getTemplate().getAlias());
			if (route.isEmpty())
			{
				_isOnARoute = false;
				return;
			}
			
			WalkerLocation nextNode;
			
			// Choose the nearest WalkerLocation if we weren't on a route.
			if (!_isOnARoute)
			{
				nextNode = route.stream().min(Comparator.comparingDouble(_actor::distance3D)).orElse(null);
				if (nextNode != null)
				{
					// We're anew on a route.
					_isOnARoute = true;
					
					// Pick the route index related to the nearest WalkerLocation.
					_routeIndex = route.indexOf(nextNode);
				}
			}
			else
			{
				// Actor is on reverse path. Decrease the index.
				if (_isReversePath && _routeIndex > 0)
				{
					_routeIndex--;
					
					if (_routeIndex == 0)
						_isReversePath = false;
				}
				// Pick next node if we're in middle path.
				else if (_routeIndex < route.size() - 1)
					_routeIndex++;
				// We ended the path ; we loop over it, returning to first point (loop is default behavior).
				else
				{
					_routeIndex = 0;
					
					// If Location can't be directly reached, it's a no-loop behavior.
					if (!GeoEngine.getInstance().canMoveToTarget(_actor.getPosition(), route.get(_routeIndex)))
					{
						// We set reverse pathing.
						_isReversePath = true;
						
						// We choose before-last node in order to start reverse path (no-loop path, normal behavior for few routes).
						_routeIndex = route.size() - 2;
					}
				}
				
				nextNode = route.get(_routeIndex);
			}
			
			// Pick the WalkerLocation related to route index, and move to it.
			_actor.getMove().maybeMoveToLocation(nextNode, 500, false, false);
		}
		else
		{
			// Don't move if current intention is not MOVE_ROUTE
			if (_currentIntention.getType() != IntentionType.MOVE_ROUTE)
			{
				_isOnARoute = false;
				return;
			}
			
			// Retrieve walking route, if any.
			final List<WalkerLocation> route = WalkerRouteData.getInstance().getWalkerRoute(_currentIntention.getRouteName(), _actor.getTemplate().getAlias());
			if (route.isEmpty())
			{
				_isOnARoute = false;
				return;
			}
			
			WalkerLocation nextNode;
			
			// Choose the nearest WalkerLocation if we weren't on a route.
			if (!_isOnARoute)
			{
				nextNode = route.stream().min(Comparator.comparingDouble(_actor::distance3D)).orElse(null);
				if (nextNode != null)
				{
					// We're anew on a route.
					_isOnARoute = true;
					
					// Pick the route index related to the nearest WalkerLocation.
					_routeIndex = route.indexOf(nextNode);
				}
			}
			else
			{
				// For all cases, if actor is stuck, reset its behavior.
				if (_actor.getMove().getGeoPathFailCount() >= 10)
				{
					_routeIndex = 0;
					_isOnARoute = true;
					_isReversePath = false;
					
					_actor.teleportTo(route.get(_routeIndex), 0);
				}
				
				// Actor is on reverse path. Decrease the index.
				if (_isReversePath && _routeIndex > 0)
				{
					_routeIndex--;
					
					if (_routeIndex == 0)
						_isReversePath = false;
				}
				// Pick next node if we're in middle path.
				else if (_routeIndex < route.size() - 1)
					_routeIndex++;
				// We ended the path ; we loop over it, returning to first point (loop is default behavior).
				else
				{
					_routeIndex = 0;
					
					// If Location can't be directly reached, it's a no-loop behavior.
					if (!GeoEngine.getInstance().canMoveToTarget(_actor.getPosition(), route.get(_routeIndex)))
					{
						// We set reverse pathing.
						_isReversePath = true;
						
						// We choose before-last node in order to start reverse path (no-loop path, normal behavior for few routes).
						_routeIndex = route.size() - 2;
					}
				}
				
				nextNode = route.get(_routeIndex);
			}
			
			// Pick the WalkerLocation related to route index, and move to it.
			_actor.getMove().maybeMoveToLocation(nextNode, 0, true, false);
		}
	}
	
	public Set<Desire> getDesires()
	{
		return _desireQueue.getDesires();
	}
	
	public Set<Creature> getSeenCreatures()
	{
		return _seenCreatures;
	}
	
	/**
	 * Clear the {@link Desire} matching the {@link Intention} set as current intention.
	 */
	public void clearCurrentDesire()
	{
		getDesires().remove(_currentIntention);
	}
	
	public void addAttackDesireHold(Creature target, double weight)
	{
		addAttackDesire(target, 0, weight, true, false);
	}
	
	public void addAttackDesireHold(Creature target, int damage, double weight)
	{
		addAttackDesire(target, damage, weight, true, false);
	}
	
	public void addAttackDesireHold(Creature target, int damage, double weight, boolean updateAggro)
	{
		addAttackDesire(target, damage, weight, updateAggro, false);
	}
	
	public void addAttackDesire(Creature target, double weight)
	{
		addAttackDesire(target, 2, weight, true);
	}
	
	public void addAttackDesire(Creature target, int damage, double weight)
	{
		addAttackDesire(target, damage, weight, true);
	}
	
	public void addAttackDesire(Creature target, int damage, double weight, boolean updateAggro)
	{
		addAttackDesire(target, damage, weight, updateAggro, true);
	}
	
	public void addAttackDesire(Creature target, int damage, double weight, boolean updateAggro, boolean canMoveToTarget)
	{
		if (target == null)
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsAttack(target, false, false, canMoveToTarget);
		
		_desireQueue.addOrUpdate(desire);
		
		if (_aggroList.getMostHatedCreature() == null)
			runAI(false);
		
		if (target instanceof Player player && player.getClassId().getType() == ClassType.PRIEST && damage > 0)
			updateAggro = true;
		
		if (updateAggro)
			_aggroList.addDamageHate(target, damage, weight);
	}
	
	public void addCastDesireHold(Creature target, int skillId, int skillLevel, double weight)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if (skill == null)
			return;
		
		addCastDesire(target, skill, weight, true, false);
	}
	
	public void addCastDesireHold(Creature target, L2Skill skill, double weight)
	{
		addCastDesire(target, skill, weight, true, false);
	}
	
	public void addCastDesireHold(Creature target, L2Skill skill, double weight, boolean checkConditions)
	{
		addCastDesire(target, skill, weight, checkConditions, false);
	}
	
	public void addCastDesire(Creature target, int skillId, int skillLevel, double weight)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if (skill == null)
			return;
		
		addCastDesire(target, skill, weight);
	}
	
	public void addCastDesire(Creature target, int skillId, int skillLevel, double weight, boolean checkConditions)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if (skill == null)
			return;
		
		addCastDesire(target, skill, weight, checkConditions);
	}
	
	public void addCastDesire(Creature target, L2Skill skill, double weight)
	{
		addCastDesire(target, skill, weight, true);
	}
	
	public void addCastDesire(Creature target, L2Skill skill, double weight, boolean checkConditions)
	{
		addCastDesire(target, skill, weight, checkConditions, true);
	}
	
	public void addCastDesire(Creature target, L2Skill skill, double weight, boolean checkConditions, boolean canMoveToTarget)
	{
		// Abort if the skill is null
		if (target == null || skill == null)
			return;
		
		if (checkConditions)
		{
			// Abort if the skill is already in reuse.
			if (!_actor.getCast().canAttemptCast(target, skill))
				return;
			
			// Abort if the skill uses mana, but the Npc doesn't have enough mana.
			final double mpConsume = _actor.getStatus().getMpConsume(skill);
			if (mpConsume > 0 && mpConsume > _actor.getStatus().getMp())
				return;
			
			// Abort if the skill uses HPs, but the Npc doesn't have enough HPs.
			final double hpConsume = skill.getHpConsume();
			if (hpConsume > 0 && hpConsume > _actor.getStatus().getHp())
				return;
		}
		
		// Don't add "Hold" type cast if skill can't be cast without moving
		if (!canMoveToTarget && !_actor.isIn2DRadius(target, (int) (skill.getCastRange() + _actor.getCollisionRadius() + target.getCollisionRadius())))
			return;
		
		if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
			return;
		
		Desire desire = new Desire(weight);
		desire.updateAsCast(_actor, target, skill, false, false, 0, canMoveToTarget);
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addFleeDesire(Creature target, int distance, double weight)
	{
		if (target == null || _actor.isMovementDisabled())
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsFlee(target, _actor.getPosition(), distance);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addFollowDesire(Creature target, double weight)
	{
		if (target == null)
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsFollow(target, false);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addInteractDesire(WorldObject target, double weight)
	{
		if (target == null)
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsInteract(target, false, false);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addMoveRouteDesire(String routeName, double weight)
	{
		final Desire desire = new Desire(weight);
		desire.updateAsMoveRoute(routeName);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addMoveToDesire(Location loc, double weight)
	{
		if (loc == null || _actor.isMovementDisabled())
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsMoveTo(loc, null);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addSocialDesire(int id, int timer, double weight)
	{
		if (_actor.isAISleeping())
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsSocial(id, timer);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addWanderDesire(int timer, double weight)
	{
		final Desire desire = new Desire(weight);
		desire.updateAsWander(timer);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addDoNothingDesire(int timer, double weight)
	{
		final Desire desire = new Desire(weight);
		desire.updateAsNothing(timer);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	public void addPickUpDesire(int itemObjectId, double weight)
	{
		if (itemObjectId == 0)
			return;
		
		final Desire desire = new Desire(weight);
		desire.updateAsPickUp(itemObjectId, false);
		
		_desireQueue.addOrUpdate(desire);
	}
	
	/**
	 * Broadcast a {@link SocialAction} packet with a specific id. It refreshs the timer.
	 * @param id : The animation id to broadcast.
	 */
	public void onRandomAnimation(int id)
	{
		if (_actor.denyAiAction())
			return;
		
		final long now = System.currentTimeMillis();
		if (now - _lastSocialBroadcast > SOCIAL_INTERVAL)
		{
			_lastSocialBroadcast = now;
			_actor.broadcastPacket(new SocialAction(_actor, id));
		}
	}
	
	public void stopRoute()
	{
		_isOnARoute = false;
	}
	
	public void cleanupForNextSpawn()
	{
		getDesires().clear();
		
		_seenCreatures.clear();
		_nextDesire = null;
		_isOnARoute = false;
		_topDesireTarget = null;
		_lastSocialBroadcast = 0l;
	}
}