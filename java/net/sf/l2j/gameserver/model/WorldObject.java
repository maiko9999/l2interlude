package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Fence;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.boat.BoatItinerary;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

import ext.mods.InstanceMap.InstanceManager;
import ext.mods.InstanceMap.MapInstance;

/**
 * Mother class of all interactive objects in the world (PC, NPC, Item...)
 */
public abstract class WorldObject
{
	public static final CLogger LOGGER = new CLogger(WorldObject.class.getName());
	
	private String _name;
	private int _objectId;
	protected boolean _townZone;
	
	private final SpawnLocation _position = new SpawnLocation(0, 0, 0, 0);
	private WorldRegion _region;
	
	private boolean _isVisible;
	
	protected WorldObject(int objectId)
	{
		_objectId = objectId;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(_objectId);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		final WorldObject other = (WorldObject) obj;
		return _objectId == other._objectId;
	}
	
	@Override
	public String toString()
	{
		return getName() + " [objId=" + getObjectId() + "]";
	}
	
	public boolean isAttackableBy(Creature attacker)
	{
		return false;
	}
	
	public boolean isAttackableWithoutForceBy(Playable attacker)
	{
		return false;
	}
	
	public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onSpawn()
	{
	}
	
	/**
	 * Remove this {@link WorldObject} from the world.
	 */
	public void decayMe()
	{
		setIsVisible(false);
		
		World.getInstance().removeObject(this);
	}
	
	public void refreshID()
	{
		World.getInstance().removeObject(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
	}
	
	/**
	 * Spawn this {@link WorldObject} and add it in the world as a visible object.
	 */
	public final void spawnMe()
	{
		_isVisible = true;
		
		setRegion(World.getInstance().getRegion(_position));
		
		World.getInstance().addObject(this);
		
		onSpawn();
	}
	
	/**
	 * Initialize the position of this {@link WorldObject} and add it in the world as a visible object.
	 * @param loc : The location used as reference X/Y/Z.
	 */
	public final void spawnMe(Location loc)
	{
		spawnMe(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Initialize the position of this {@link WorldObject} and add it in the world as a visible object.
	 * @param loc : The location used as reference X/Y/Z.
	 * @param heading : The heading position to set.
	 */
	public final void spawnMe(Location loc, int heading)
	{
		spawnMe(loc.getX(), loc.getY(), loc.getZ(), heading);
	}
	
	/**
	 * Initialize the position of this {@link WorldObject} and add it in the world as a visible object.
	 * @param loc : The location used as reference X/Y/Z.
	 */
	public final void spawnMe(SpawnLocation loc)
	{
		spawnMe(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading());
	}
	
	public final void spawnMe(BoatItinerary itinerary)
	{
		spawnMe(itinerary.getInfo()[0].getDock().getDockLoc(), itinerary.getHeading());
	}
	
	/**
	 * Initialize the position of this {@link WorldObject} and add it in the world as a visible object.
	 * @param x : The X position to set.
	 * @param y : The Y position to set.
	 * @param z : The Z position to set.
	 */
	public final void spawnMe(int x, int y, int z)
	{
		_position.set(Math.clamp(x, World.WORLD_X_MIN, World.WORLD_X_MAX), Math.clamp(y, World.WORLD_Y_MIN, World.WORLD_Y_MAX), z);
		
		spawnMe();
	}
	
	/**
	 * Initialize the position of this {@link WorldObject} and add it in the world as a visible object.
	 * @param x : The X position to set.
	 * @param y : The Y position to set.
	 * @param z : The Z position to set.
	 * @param heading : The heading position to set.
	 */
	public final void spawnMe(int x, int y, int z, int heading)
	{
		_position.set(Math.clamp(x, World.WORLD_X_MIN, World.WORLD_X_MAX), Math.clamp(y, World.WORLD_Y_MIN, World.WORLD_Y_MAX), z, heading);
		
		spawnMe();
	}
	
	/**
	 * @return the visibilty state of this {@link WorldObject}.
	 */
	public final boolean isVisible()
	{
		return _region != null && _isVisible;
	}
	
	public final void setIsVisible(boolean value)
	{
		_isVisible = value;
		
		if (!_isVisible)
			setRegion(null);
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public void setName(String value)
	{
		_name = value;
	}
	
	public final int getObjectId()
	{
		return _objectId;
	}
	
	public Player getActingPlayer()
	{
		return null;
	}
	
	/**
	 * Sends the Server->Client info packet for this {@link WorldObject}.
	 * @param player : The packet receiver.
	 */
	public void sendInfo(Player player)
	{
	}
	
	/**
	 * Check if this {@link WorldObject} has charged shot.
	 * @param type : The type of the shot to be checked.
	 * @return true if the object has charged shot.
	 */
	public boolean isChargedShot(ShotType type)
	{
		return false;
	}
	
	/**
	 * Charging shot into this {@link WorldObject}.
	 * @param type : The type of the shot to be (un)charged.
	 * @param charged : true if we charge, false if we uncharge.
	 */
	public void setChargedShot(ShotType type, boolean charged)
	{
	}
	
	/**
	 * Try to recharge a shot.
	 * @param physical : The skill is using Soulshots.
	 * @param magical : The skill is using Spiritshots.
	 */
	public void rechargeShots(boolean physical, boolean magical)
	{
	}
	
	/**
	 * Check if this {@link WorldObject} is in the given {@link ZoneId}.
	 * @param zone : The ZoneId to check.
	 * @return true if the object is in that ZoneId.
	 */
	public boolean isInsideZone(ZoneId zone)
	{
		return false;
	}
	
	/**
	 * Set the position of this {@link WorldObject} and if necessary modify its _region.
	 * @param x : The X position to set.
	 * @param y : The Y position to set.
	 * @param z : The Z position to set.
	 */
	public final void setXYZ(int x, int y, int z)
	{
		_position.set(x, y, z);
		
		if (!isVisible())
			return;
		
		final WorldRegion region = World.getInstance().getRegion(_position);
		if (region != _region)
			setRegion(region);
	}
	
	/**
	 * Set the position of this {@link WorldObject} and if necessary modify its _region.
	 * @param loc : The SpawnLocation used as reference.
	 */
	public final void setXYZ(SpawnLocation loc)
	{
		_position.set(loc);
		
		if (!isVisible())
			return;
		
		final WorldRegion region = World.getInstance().getRegion(_position);
		if (region != _region)
			setRegion(region);
	}
	
	/**
	 * Set the position of this {@link WorldObject} using a WorldObject reference position, and if necessary modify its _region.
	 * @param object : The WorldObject used as reference.
	 */
	public final void setXYZ(WorldObject object)
	{
		setXYZ(object.getPosition());
	}
	
	/**
	 * Set the position of this {@link WorldObject} and make it invisible.
	 * @param x : The X position to set.
	 * @param y : The Y position to set.
	 * @param z : The Z position to set.
	 */
	public final void setXYZInvisible(int x, int y, int z)
	{
		_position.set(Math.clamp(x, World.WORLD_X_MIN, World.WORLD_X_MAX), Math.clamp(y, World.WORLD_Y_MIN, World.WORLD_Y_MAX), z);
		
		setIsVisible(false);
	}
	
	public final void setXYZInvisible(Location loc)
	{
		setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public final int getX()
	{
		return _position.getX();
	}
	
	public final int getY()
	{
		return _position.getY();
	}
	
	public final int getZ()
	{
		return _position.getZ();
	}
	
	public final int getHeading()
	{
		return _position.getHeading();
	}
	
	public final SpawnLocation getPosition()
	{
		return _position;
	}
	
	public final WorldRegion getRegion()
	{
		return _region;
	}
	
	/**
	 * Update current and surrounding {@link WorldRegion}s, based on both current region and region set as parameter.
	 * @param newRegion : null to remove the {@link WorldObject}, or the new region.
	 */
	public void setRegion(WorldRegion newRegion)
	{
		List<WorldRegion> oldAreas = Collections.emptyList();
		
		if (_region != null)
		{
			_region.removeVisibleObject(this);
			oldAreas = _region.getSurroundingRegions();
		}
		
		List<WorldRegion> newAreas = Collections.emptyList();
		
		if (newRegion != null)
		{
			newRegion.addVisibleObject(this);
			newAreas = newRegion.getSurroundingRegions();
		}
		
		// For every old surrounding area NOT SHARED with new surrounding areas.
		for (WorldRegion region : oldAreas)
		{
			if (!newAreas.contains(region))
			{
				// Refresh infos related to zones.
				for (ZoneType zone : region.getZones())
					zone.removeKnownObject(this);
				
				// Update all objects.
				region.getObjects().forEach(o ->
				{
					if (o == this)
						return;
					
					o.removeKnownObject(this);
					removeKnownObject(o);
				});
				
				// Desactivate the old neighbor region.
				if (this instanceof Player && region.isEmptyNeighborhood())
					region.setActive(false);
			}
		}
		
		// For every new surrounding area NOT SHARED with old surrounding areas.
		for (WorldRegion region : newAreas)
		{
			if (!oldAreas.contains(region))
			{
				// Refresh infos related to zones.
				for (ZoneType zone : region.getZones())
				{
					
					zone.addKnownObject(this);
				}
				// Update all objects.
				region.getObjects().forEach(o ->
				{
					if (o == this)
						return;
					
					o.addKnownObject(this);
					addKnownObject(o);
				});
				
				// Activate the new neighbor region.
				if (this instanceof Player)
					region.setActive(true);
			}
		}
		
		_region = newRegion;
		
		refreshKnownList();
	}
	
	public void refreshKnownList()
	{
		if (_region == null)
			return;
		
		for (WorldRegion reg : _region.getSurroundingRegions())
		{
			for (WorldObject obj : reg.getObjects())
			{
				if (obj == this || obj instanceof Door || obj instanceof Fence)
					continue;
				
				if (getInstanceMap().getId() != obj.getInstanceMap().getId())
				{
					obj.removeKnownObject(this);
					removeKnownObject(obj);
				}
			}
		}
	}
	
	/**
	 * Add a {@link WorldObject} to knownlist.
	 * @param object : An object to be added.
	 */
	public void addKnownObject(WorldObject object)
	{
	}
	
	/**
	 * Remove a {@link WorldObject} from knownlist.
	 * @param object : An object to be removed.
	 */
	public void removeKnownObject(WorldObject object)
	{
	}
	
	/**
	 * @param target : The {@link WorldObject} to check.
	 * @return True if the {@link WorldObject} set as parameter is registered in same grid of regions than this WorldObject, false otherwise.
	 */
	public boolean knows(WorldObject target)
	{
		// Object doesn't exist, return false.
		if (target == null)
			return false;
		
		// No region set for the current WorldObject, return false.
		final WorldRegion region = _region;
		if (region == null)
			return false;
		
		// No region set for the target, return false.
		final WorldRegion targetRegion = target.getRegion();
		if (targetRegion == null)
			return false;
		
		// Return true if one surrounding WorldRegions of this WorldObject matches with target WorldRegion.
		return region.getSurroundingRegions().contains(targetRegion);
	}
	
	/**
	 * Return the knownlist of this {@link WorldObject} for a given object type.
	 * @param <A> : The object type must be an instance of {@ling WorldObject}.
	 * @param type : The class specifying object type.
	 * @return List<A> : The knownlist of given object type.
	 */
	public final <A extends WorldObject> List<A> getKnownType(Class<A> type)
	{
		final WorldRegion region = _region;
		if (region == null)
			return Collections.emptyList();
		
		final List<A> result = new ArrayList<>();
		
		// Collect the object of the specifiend type.
		
		return result;
	}
	
	/**
	 * Run a {@link Consumer} upon surrounding {@link WorldObject}s of this {@link WorldObject}.
	 * @param <A> : A class extending {@link WorldObject}.
	 * @param type : The type of {@link WorldObject}s to affect.
	 * @param action : The {@link Consumer} to use.
	 */
	public <A extends WorldObject> void forEachKnownType(Class<A> type, Consumer<A> action)
	{
		final WorldRegion region = _region;
		if (region == null)
			return;

		region.forEachSurroundingRegion(r -> r.forEachType(type, this, o -> this != o, action));
	}

	
	/**
	 * Run a {@link Consumer} upon filtered surrounding {@link WorldObject}s of this {@link WorldObject}.
	 * @param <A> : A class extending {@link WorldObject}.
	 * @param type : The type of {@link WorldObject}s to affect.
	 * @param filter : A {@link Predicate} used as filter.
	 * @param action : The {@link Consumer} to use.
	 */
	public <A extends WorldObject> void forEachKnownType(Class<A> type, Predicate<A> filter, Consumer<A> action)
	{
		final WorldRegion region = _region;
		if (region == null)
			return;

		region.forEachSurroundingRegion(r -> r.forEachType(type, this, o -> this != o && filter.test(o), action));
	}

	
	/**
	 * Return the filtered knownlist of this {@link WorldObject} for a given object type.
	 * @param <A> : The object type must be an instance of {@link WorldObject}.
	 * @param type : The class specifying object type.
	 * @param filter : A {@link Predicate} used as filter.
	 * @return List<A> : The knownlist of given object type.
	 */
	public final <A extends WorldObject> List<A> getKnownType(Class<A> type, Predicate<A> filter)
	{
		final WorldRegion region = _region;
		if (region == null)
			return Collections.emptyList();

		final List<A> result = new ArrayList<>();

		region.forEachSurroundingRegion(r -> r.forEachType(type, this, o -> this != o && filter.test(o), result::add));

		return result;
	}

	
	/**
	 * Run a {@link Consumer} upon surrounding {@link WorldObject}s of this {@link WorldObject} in a defined radius.
	 * @param <A> : A class extending {@link WorldObject}.
	 * @param type : The type of {@link WorldObject}s to affect.
	 * @param radius : The radius to check in which object must be located.
	 * @param action : The {@link Consumer} to use.
	 */
	public <A extends WorldObject> void forEachKnownTypeInRadius(Class<A> type, int radius, Consumer<A> action)
	{
		final WorldRegion region = _region;
		if (region == null)
			return;

		final int depth = (radius <= World.REGION_SIZE) ? 1 : (int) ((radius / World.REGION_SIZE) + 1);

		region.forEachRegion(depth, r -> r.forEachType(type, this, o -> this != o && isInStrictRadius(o, radius), action));
	}

	
	/**
	 * Return the knownlist of this {@link WorldObject} for a given object type within specified radius.
	 * @param <A> : The object type must be an instance of WorldObject.
	 * @param type : The class specifying object type.
	 * @param radius : The radius to check in which object must be located.
	 * @return List<A> : The knownlist of given object type.
	 */
	public final <A extends WorldObject> List<A> getKnownTypeInRadius(Class<A> type, int radius)
	{
		final WorldRegion region = _region;
		if (region == null)
			return Collections.emptyList();

		final List<A> result = new ArrayList<>();
		final int depth = (radius <= World.REGION_SIZE) ? 1 : (int) ((radius / World.REGION_SIZE) + 1);

		region.forEachRegion(depth, r -> r.forEachType(type, this, o -> this != o && isInStrictRadius(o, radius), result::add));

		return result;
	}

	
	/**
	 * Run a {@link Consumer} upon filtered surrounding {@link WorldObject}s of this {@link WorldObject} in a defined radius.
	 * @param <A> : A class extending {@link WorldObject}.
	 * @param type : The type of {@link WorldObject}s to affect.
	 * @param radius : The radius to check in which object must be located.
	 * @param filter : A {@link Predicate} used as filter.
	 * @param action : The {@link Consumer} to use.
	 */
	public <A extends WorldObject> void forEachKnownTypeInRadius(Class<A> type, int radius, Predicate<A> filter, Consumer<A> action)
	{
		final WorldRegion region = _region;
		if (region == null)
			return;
		
		final int depth = (radius <= World.REGION_SIZE) ? 1 : (int) ((radius / World.REGION_SIZE) + 1);
		
		region.forEachRegion(depth, r -> r.forEachType(type, this, o -> this != o && isInStrictRadius(o, radius) && filter.test(o), action));
	}
	
	/**
	 * Return the filtered knownlist of this {@link WorldObject} for a given object type within specified radius.
	 * @param <A> : The object type must be an instance of WorldObject.
	 * @param type : The class specifying object type.
	 * @param radius : The radius to check in which object must be located.
	 * @param filter : A {@link Predicate} used as filter.
	 * @return List<A> : The knownlist of given object type.
	 */
	public final <A extends WorldObject> List<A> getKnownTypeInRadius(Class<A> type, int radius, Predicate<A> filter)
	{
		final WorldRegion region = _region;
		if (region == null)
			return Collections.emptyList();
		
		final List<A> result = new ArrayList<>();
		final int depth = (radius <= World.REGION_SIZE) ? 1 : (int) ((radius / World.REGION_SIZE) + 1);
		
		region.forEachRegion(depth, r -> r.forEachType(type, this, o -> this != o && isInStrictRadius(o, radius) && filter.test(o), result::add));
		
		return result;
	}
	
	/**
	 * Refresh the knownlist for this {@link WorldObject}. Only used by teleport process.
	 */
	public final void refreshKnownlist()
	{
		final WorldRegion region = _region;
		if (region == null)
			return;
		
		region.forEachSurroundingRegion(r -> r.getObjects().forEach(o ->
		{
			if (o == this)
				return;
			
			o.addKnownObject(this);
			addKnownObject(o);
		}));
	}
	
	/**
	 * Test and retrieve all {@link ZoneType}s surrounding this {@link WorldObject}.
	 * @param checkIfInside : If true, we enforce an {@link ZoneType#isInsideZone(WorldObject)} check.
	 * @return a {@link List} of {@link ZoneType}.
	 */
	public final List<ZoneType> getZones(boolean checkIfInside)
	{
		final WorldRegion region = _region;
		if (region == null)
			return Collections.emptyList();
		
		final List<ZoneType> zones = new ArrayList<>();
		
		// Test first current WorldRegion.
		for (ZoneType zt : region.getZones())
		{
			if (zones.contains(zt))
				continue;
			
			if (checkIfInside && !zt.isInsideZone(this))
				continue;
			
			zones.add(zt);
		}
		
		// Then test surrounding WorldRegions.
		region.forEachSurroundingRegion(r ->
		{
			for (ZoneType zt : r.getZones())
			{
				if (zones.contains(zt))
					continue;
				
				if (checkIfInside && !zt.isInsideZone(this))
					continue;
				
				zones.add(zt);
			}
		});
		return zones;
	}
	
	/**
	 * Fire actions related to region activation.<br>
	 * <br>
	 * A region activation occurs when one {@link Player} enters for the first time in this {@link WorldObject}'s {@link WorldRegion} surroundings (self region included).<br>
	 * <br>
	 * Additional Player entrances don't activate it. This state is verified by {@link WorldRegion#isActive()}.
	 */
	public void onActiveRegion()
	{
	}
	
	/**
	 * Fire actions related to region desactivation.<br>
	 * <br>
	 * A region desactivation occurs when the last {@link Player} left this {@link WorldObject}'s {@link WorldRegion} surroundings (self region included).<br>
	 * <br>
	 * This state is verified by {@link WorldRegion#isActive()}.
	 */
	public void onInactiveRegion()
	{
	}
	
	/**
	 * @param object : The {@link WorldObject} to test.
	 * @param radius : The radius to test.
	 * @return True is this {@link WorldObject} is inside the given radius around the {@link WorldObject} set as parameter.
	 */
	public final boolean isIn3DRadius(WorldObject object, int radius)
	{
		return _position.isIn3DRadius(object.getPosition(), radius);
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @param radius : The radius to test.
	 * @return True is this {@link WorldObject} is inside the given radius around the {@link Location} set as parameter.
	 */
	public final boolean isIn3DRadius(Location loc, int radius)
	{
		return _position.isIn3DRadius(loc, radius);
	}
	
	/**
	 * @param x : The X coord to test.
	 * @param y : The Y coord to test.
	 * @param z : The Z coord to test.
	 * @param radius : The radius to test.
	 * @return True is this {@link WorldObject} is inside the given radius around the {@link Location} set as parameter.
	 */
	public final boolean isIn3DRadius(int x, int y, int z, int radius)
	{
		return _position.isIn3DRadius(x, y, z, radius);
	}
	
	/**
	 * @param object : The {@link WorldObject} to test.
	 * @return The distance between this {WorldObject} and the {@link WorldObject} set as parameter.
	 */
	public final double distance3D(WorldObject object)
	{
		return _position.distance3D(object.getPosition());
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @return The distance between this {WorldObject} and the {@link Location} set as parameter.
	 */
	public final double distance3D(Location loc)
	{
		return _position.distance3D(loc);
	}
	
	/**
	 * @param object : The {@link WorldObject} to test.
	 * @param radius : The radius to test.
	 * @return True is this {@link WorldObject} is inside the given radius around the {@link WorldObject} set as parameter.
	 */
	public final boolean isIn2DRadius(WorldObject object, int radius)
	{
		return _position.isIn2DRadius(object.getPosition(), radius);
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @param radius : The radius to test.
	 * @return True is this {@link WorldObject} is inside the given radius around the {@link Location} set as parameter.
	 */
	public final boolean isIn2DRadius(Location loc, int radius)
	{
		return _position.isIn2DRadius(loc, radius);
	}
	
	/**
	 * @param x : The X coord to test.
	 * @param y : The Y coord to test.
	 * @param radius : The radius to test.
	 * @return True is this {@link WorldObject} is inside the given radius around the {@link Location} set as parameter.
	 */
	public final boolean isIn2DRadius(int x, int y, int radius)
	{
		return _position.isIn2DRadius(x, y, radius);
	}
	
	/**
	 * @param object : The {@link WorldObject} to test.
	 * @return The distance - without counting Z - between this {WorldObject} and the {@link WorldObject} set as parameter.
	 */
	public final double distance2D(WorldObject object)
	{
		return _position.distance2D(object.getPosition());
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @return The distance - without counting Z - between this {WorldObject} and the {@link Location} set as parameter.
	 */
	public final double distance2D(Location loc)
	{
		return _position.distance2D(loc);
	}
	
	/**
	 * @param target : The {@link WorldObject} target to check.
	 * @return True if this {@link WorldObject} is behind the {@link WorldObject} target.
	 */
	public final boolean isBehind(WorldObject target)
	{
		return _position.isBehind(target);
	}
	
	/**
	 * @param target : The {@link WorldObject} target to check.
	 * @return True if this {@link WorldObject} is in front of the {@link WorldObject} target.
	 */
	public final boolean isInFrontOf(WorldObject target)
	{
		return _position.isInFrontOf(target);
	}
	
	/**
	 * @param target : The {@link WorldObject} target to check.
	 * @param maxAngle : The angle to check.
	 * @return True if this {@link WorldObject} is facing the {@link WorldObject} target.
	 */
	public final boolean isFacing(WorldObject target, int maxAngle)
	{
		return _position.isFacing(target, maxAngle);
	}
	
	/**
	 * @param player
	 */
	public void onInteract(Player player)
	{
		
	}
	
	public Monster getMonster()
	{
		return null;
	}
	
	public boolean isVisibleTo(WorldObject obj)
	{
		return true;
	}
	
	public boolean getTownZone()
	{
		return _townZone;
	}
	
	public void setTownZone(boolean val)
	{
		_townZone = val;
	}
	
	/**
	 * @param obj : The {@link WorldObject} to test.
	 * @param radius : The radius to check.
	 * @return True if this {@link WorldObject} is in the radius of the {@link WorldObject} set as parameter. Collisions of both {@link WorldObject}s are considered.
	 */
	public boolean isInStrictRadius(WorldObject obj, int radius)
	{
		// Calculate total collision radius of both objects.
		double totalRadius = 0;
		if (this instanceof Creature creature)
			totalRadius += creature.getCollisionRadius();
		
		if (obj instanceof Creature creature)
			totalRadius += creature.getCollisionRadius();
		
		return distance3D(obj) < radius + totalRadius;
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @param radius : The radius to check.
	 * @return True if this {@link WorldObject} is in the radius of the {@link Location} set as parameter. Collisions of this {@link WorldObject} is considered.
	 */
	public boolean isInStrictRadius(Location loc, int radius)
	{
		// Calculate total collision radius of both objects.
		double totalRadius = 0;
		if (this instanceof Creature creature)
			totalRadius += creature.getCollisionRadius();
		
		return distance3D(loc) < radius + totalRadius;
	}
	
	private MapInstance _instance = InstanceManager.getInstance().getInstance(0);
	
	public MapInstance getInstanceMap()
	{
		return _instance;
	}
	
	public boolean isSameInstanceMap(WorldObject other)
	{
		return getInstanceMap().getId() == other.getInstanceMap().getId();
	}
	
	public void setInstanceMap(MapInstance instance, boolean silent)
	{
		_instance = instance;
		
		if (!silent)
		{
			decayMe();
			spawnMe();
		}
	}
}