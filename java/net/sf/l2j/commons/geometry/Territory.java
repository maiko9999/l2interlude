package net.sf.l2j.commons.geometry;

import java.util.List;
import java.util.Set;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * Define a 3D shaped area inside the world.<br>
 * <br>
 * The associated shape extends {@link AShape}, and is based on the number of accorded coords.
 */
public class Territory
{
	private static final CLogger LOGGER = new CLogger(Territory.class.getName());
	
	private static final int MAX_ITERATIONS = 10;
	
	private final String _name;
	
	private final int _minZ;
	private final int _maxZ;
	private final int _avgZ;
	
	private final AShape _shape;
	
	public Territory(StatSet set)
	{
		this(set.getString("name", null), set.getInteger("minZ"), set.getInteger("maxZ"), set.getList("coords"));
	}
	
	public Territory(String name, int minZ, int maxZ, List<Point2D> coords)
	{
		final int coordsNumber = coords.size();
		if (coordsNumber >= 4)
			_shape = new Polygon(coords);
		else if (coordsNumber == 3)
			_shape = new Triangle(coords);
		else if (coordsNumber == 2)
			_shape = new Rectangle(coords);
		else
			throw new IllegalArgumentException("Invalid number of coordinates for Territory");
		
		_name = name;
		
		_minZ = minZ;
		_maxZ = maxZ;
		_avgZ = (_minZ + _maxZ) / 2;
	}
	
	public Territory(String name, int minZ, int maxZ, Set<Triangle> shapes)
	{
		_shape = new Polygon(shapes);
		
		_name = name;
		
		_minZ = minZ;
		_maxZ = maxZ;
		_avgZ = (_minZ + _maxZ) / 2;
	}
	
	/**
	 * @return This {@link Territory} name.
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * @return This {@link Territory} minimum Z coordinate.
	 */
	public final int getMinZ()
	{
		return _minZ;
	}
	
	/**
	 * @return This {@link Territory} maximum Z coordinate.
	 */
	public final int getMaxZ()
	{
		return _maxZ;
	}
	
	/**
	 * @return This {@link Territory} average Z coordinate.
	 */
	public final int getAvgZ()
	{
		return _avgZ;
	}
	
	/**
	 * @return The {@link AShape} associated to this {@link Territory}.
	 */
	public final AShape getShape()
	{
		return _shape;
	}
	
	/**
	 * @param x : The X coordinate to test.
	 * @param y : The Y coordinate to test.
	 * @return True if the tested 2D point is part of this {@link Territory}, or false otherwise.
	 */
	public boolean isInside(int x, int y)
	{
		return _shape.isInside(x, y);
	}
	
	/**
	 * @param x : The X coordinate to test.
	 * @param y : The Y coordinate to test.
	 * @param z : The Z coordinate to test.
	 * @return True if the tested 3D point is part of this {@link Territory}, or false otherwise.
	 */
	public boolean isInside(int x, int y, int z)
	{
		if (z < _minZ || z > _maxZ)
			return false;
		
		return _shape.isInside(x, y);
	}
	
	public boolean isInside(Location loc)
	{
		return loc != null && isInside(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean isInside(WorldObject object)
	{
		return object != null && isInside(object.getPosition());
	}
	
	/**
	 * @return A random {@link SpawnLocation} inside this {@link AShape}, validated by surrounding geodata.
	 */
	public SpawnLocation getRandomGeoLocation()
	{
		Location loc = null;
		
		int failedZ = 0;
		int failedGeo = 0;
		
		// Try to find Location within MAX_ITERATIONS iterations.
		for (int i = 0; i < MAX_ITERATIONS; i++)
		{
			// Get random X, Y coordinates inside Triangle ; get real Z coordinate based on Geodata.
			loc = _shape.getRandomLocation();
			loc.setZ(GeoEngine.getInstance().getHeight(loc.getX(), loc.getY(), _avgZ));
			
			// Check Z coordinate to exceed limits and eventually get new coordinates.
			if (loc.getZ() < _minZ || loc.getZ() > _maxZ)
			{
				failedZ++;
				continue;
			}
			
			// Check close area for available movement.
			if (!GeoEngine.getInstance().canMoveAround(loc.getX(), loc.getY(), loc.getZ()))
			{
				failedGeo++;
				continue;
			}
			
			// Return new SpawnLocation with Location content and random heading.
			return new SpawnLocation(loc.getX(), loc.getY(), loc.getZ(), Rnd.get(65536));
		}
		
		// Send a log if correct position wasn't found.
		if (Config.DEVELOPER)
			LOGGER.warn("Territory name \"{}\", wrong Z {}, wrong geo {}", _name, failedZ, failedGeo);
		
		// Use last estimated Location if existing, or null.
		return (loc == null) ? null : new SpawnLocation(loc.getX(), loc.getY(), loc.getZ(), Rnd.get(65536));
	}
	
	/**
	 * @param bannedTerritory : The banned {@link Territory} to test.
	 * @return A random {@link SpawnLocation} inside this {@link AShape}, validated by surrounding geodata and eventual banned {@link Territory}.
	 */
	public SpawnLocation getRandomGeoLocation(Territory bannedTerritory)
	{
		Location loc = null;
		
		int failedZ = 0;
		int failedGeo = 0;
		
		// Try to find Location within MAX_ITERATIONS iterations.
		for (int i = 0; i < MAX_ITERATIONS; i++)
		{
			// Get random X, Y coordinates inside Triangle ; get real Z coordinate based on Geodata.
			loc = _shape.getRandomLocation();
			loc.setZ(GeoEngine.getInstance().getHeight(loc.getX(), loc.getY(), _avgZ));
			
			// If a banned territory is set and generated Location is part of it, break current execution without doing anything.
			if (bannedTerritory != null && bannedTerritory.isInside(loc))
				continue;
			
			// Check Z coordinate to exceed limits and eventually get new coordinates.
			if (loc.getZ() < _minZ || loc.getZ() > _maxZ)
			{
				failedZ++;
				continue;
			}
			
			// Check close area for available movement.
			if (!GeoEngine.getInstance().canMoveAround(loc.getX(), loc.getY(), loc.getZ()))
			{
				failedGeo++;
				continue;
			}
			
			// Return new SpawnLocation with Location content and random heading.
			return new SpawnLocation(loc.getX(), loc.getY(), loc.getZ(), Rnd.get(65536));
		}
		
		// Send a log if correct position wasn't found.
		if (Config.DEVELOPER)
			LOGGER.warn("Territory name \"{}\", wrong Z {}, wrong geo {}", _name, failedZ, failedGeo);
		
		// Use last estimated Location if existing, or null.
		return (loc == null) ? null : new SpawnLocation(loc.getX(), loc.getY(), loc.getZ(), Rnd.get(65536));
	}
	
	public void visualize(ExServerPrimitive debug)
	{
		_shape.visualize3D(_name, debug, _minZ, _maxZ);
	}
}