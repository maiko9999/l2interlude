package net.sf.l2j.commons.geometry;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.geometry.algorithm.Kong;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * A polygon consisting of various {@link Triangle}s.
 */
public class Polygon extends AShape
{
	protected final Collection<Triangle> _shapes;
	
	public Polygon(StatSet set)
	{
		this(set.getList("coords"));
	}
	
	public Polygon(List<Point2D> coords)
	{
		_shapes = Kong.doTriangulation(coords);
		
		for (Triangle shape : _shapes)
			_area += shape.getArea();
	}
	
	public Polygon(Set<Triangle> shapes)
	{
		_shapes = shapes;
		
		for (Triangle shape : _shapes)
			_area += shape.getArea();
	}
	
	@Override
	public boolean isInside(int x, int y)
	{
		for (Triangle shape : _shapes)
		{
			if (shape.isInside(x, y))
				return true;
		}
		return false;
	}
	
	@Override
	public boolean intersects(int x1, int y1, int x2, int y2)
	{
		for (Triangle shape : _shapes)
		{
			if (shape.intersects(x1, y1, x2, y2))
				return true;
		}
		return false;
	}
	
	@Override
	public Location getRandomLocation()
	{
		long size = Rnd.get(_area);
		
		for (Triangle shape : _shapes)
		{
			size -= shape.getArea();
			if (size < 0)
				return shape.getRandomLocation();
		}
		
		// should never happen
		return null;
	}
	
	@Override
	public void visualize(String info, ExServerPrimitive debug, int z)
	{
		for (Triangle shape : _shapes)
			shape.visualize(info, debug, z);
	}
	
	@Override
	public void visualize3D(String info, ExServerPrimitive debug, int minZ, int maxZ)
	{
		for (Triangle shape : _shapes)
			shape.visualize3D(info, debug, minZ, maxZ);
	}
	
	public Collection<Triangle> getShapes()
	{
		return _shapes;
	}
}