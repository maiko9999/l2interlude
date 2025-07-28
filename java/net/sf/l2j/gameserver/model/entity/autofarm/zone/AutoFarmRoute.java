package net.sf.l2j.gameserver.model.entity.autofarm.zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager;
import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager.AutoFarmType;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

public class AutoFarmRoute extends AutoFarmArea
{
	private int _index;
	private int _radius;
	private boolean _reversePath;
	private boolean _isOnARoute;
	private boolean _reachedFirstNode;
	
	public AutoFarmRoute(int id, String name, int ownerId)
	{
		super(id, name, ownerId, AutoFarmType.ROTA);
	}
	
	public AutoFarmRoute(String name, int ownerId)
	{
		super(name, ownerId, AutoFarmType.ROTA);
	}
	
	@Override
	public void visualizeZone(ExServerPrimitive debug)
	{
		getZone().visualizeZone("ROTA " + getName(), debug);
	}
	
	@Override
	public AutoFarmRoute getRouteZone()
	{
		return this;
	}
	
	@Override
	public List<Location> getNodes()
	{
		return super.getNodes();
	}
	
	@Override
	public List<Monster> getMonsters()
	{
		// We increased the range to get more options, but only the compatible ones will be returned.
		if (getProfile().getFinalRadius() < 100)
		{
			final List<Monster> monsters = new ArrayList<>();
			for (Monster m : getOwner().getKnownTypeInRadius(Monster.class, getProfile().getFinalRadius() * 2))
			{
				if (m.isInStrictRadius(m, getProfile().getFinalRadius()))
				{
					monsters.add(m);
					continue;
				}
				
				// Monsters that are out of attack range are only returned if they are trying to attack the player.
				if (m.getAI().getAggroList().getHate(getOwner()) > 0)
				{
					monsters.add(m);
					continue;
				}
			}
			
			return monsters;
		}
		
		return getOwner().getKnownTypeInRadius(Monster.class, getProfile().getFinalRadius());
	}
	
	@Override
	public Set<String> getMonsterHistory()
	{
		_monsterHistory.addAll(getKnownTypeInRadius(Monster.class, AutoFarmManager.MAX_ROUTE_LINE_LENGTH).stream().map(Monster::getName).toList());
		return _monsterHistory;
	}
	
	public void reset()
	{
		_isOnARoute = false;
		_reachedFirstNode = false;
	}
	
	/**
	 * @return The route has not been started yet.
	 */
	public boolean isOwnerOnARoute()
	{
		return _isOnARoute;
	}
	
	/**
	 * @return The player is just starting the route (has not yet reached the closest index).
	 */
	public boolean reachedFirstNode()
	{
		return _reachedFirstNode;
	}
	
	public int getRadius()
	{
		return _radius;
	}
	
	public void setRadius(int value)
	{
		_radius = value;
	}
	
	/*
	 * from NpcAI
	 */
	public void moveToNextPoint()
	{
		final Player player = getOwner();
		
		// Choose the nearest Location if we weren't on a route.
		if (!_isOnARoute)
		{
			final Location nearestNode = getNodes().stream().min(Comparator.comparingDouble(wl -> player.distance3D(wl))).get();
			_index = getNodes().indexOf(nearestNode);
		}
		else if (player.isIn3DRadius(getNodes().get(_index), 50))
		{
			// The player is having trouble moving.
			if (player.getMove().getGeoPathFailCount() >= 10)
			{
				reset();
				AutoFarmManager.getInstance().stopPlayer(player, "Character fora da rota");
				return;
			}
			
			// The player has just reached the first node; we can unlock the other actions of the routine.
			if (_isOnARoute && !_reachedFirstNode)
				_reachedFirstNode = true;
			
			// Actor is on reverse path. Decrease the index.
			if (_reversePath && _index > 0)
			{
				_index--;
				
				if (_index == 0)
					_reversePath = false;
			}
			// Set the next node value.
			else if (_index < getNodes().size() - 1)
				_index++;
			// Reset the index, and return the behavior to normal state.
			else
			{
				_index = getNodes().size() - 2;
				_reversePath = true;
			}
		}
		
		// Retrieve next node.
		Location node = getNodes().get(_index);
		
		// Test the path. If no path is found, we set the reverse path.
		if (!GeoEngine.getInstance().canMoveToTarget(player.getPosition(), node))
		{
			final List<Location> path = GeoEngine.getInstance().findPath(player.getX(), player.getY(), player.getZ(), node.getX(), node.getY(), node.getZ(), true, null);
			if (path.isEmpty())
			{
				player.getMove().addGeoPathFailCount();
				
				if (_index == 0)
				{
					_index = getNodes().size() - 2;
					_reversePath = true;
				}
				else
					_index--;
				
				node = getNodes().get(_index);
			}
		}
		
		player.getAI().tryToMoveTo(node, null);
		_isOnARoute = true;
	}
	
	/*
	 * Adapted from WorldObject.
	 */
	private final <A extends WorldObject> List<A> getKnownTypeInRadius(Class<A> type, int radius)
	{
		final List<A> result = new ArrayList<>();
		final int depth = (radius <= 2048) ? 1 : (int) ((radius / 2048) + 1);
		
		for (Location loc : getNodes())
		{
			final WorldRegion wr = World.getInstance().getRegion(loc);
			wr.forEachRegion(depth, r -> r.forEachType(type, getOwner(), o -> o.isInStrictRadius(o, radius), result::add));
			
		}
		return result;
	}
}