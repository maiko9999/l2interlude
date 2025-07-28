package net.sf.l2j.commons.geometry;

import java.awt.Color;
import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

public class Rectangle extends AShape
{
	// rectangle origin coordinates
	protected final int _x;
	protected final int _y;
	
	// rectangle width and height
	protected final int _w;
	protected final int _h;
	
	public Rectangle(List<Point2D> coords)
	{
		final Point2D p1 = coords.get(0);
		final Point2D p2 = coords.get(1);
		
		_x = Math.min(p1.getX(), p2.getX());
		_y = Math.min(p1.getY(), p2.getY());
		
		_w = Math.abs(p2.getX() - p1.getX());
		_h = Math.abs(p2.getY() - p1.getY());
		
		_center = new Point2D(_x + (_w / 2), _y + (_h / 2));
		_area = (long) _w * _h;
	}
	
	@Override
	public boolean isInside(int x, int y)
	{
		int d = x - _x;
		if (d < 0 || d > _w)
			return false;
		
		d = y - _y;
		if (d < 0 || d > _h)
			return false;
		
		return true;
	}
	
	@Override
	public boolean intersects(int x1, int y1, int x2, int y2)
	{
		return _x < x2 && _x + _w > x1 && _y < y2 && _y + _h > y1;
	}
	
	@Override
	public Location getRandomLocation()
	{
		return new Location(_x + Rnd.get(_w), _y + Rnd.get(_h), 0);
	}
	
	@Override
	public void visualize(String info, ExServerPrimitive debug, int z)
	{
		final int x2 = _x + _w;
		final int y2 = _y + _h;
		z -= 32;
		
		debug.addLine(info, Color.YELLOW, true, _x, _y, z, _x, y2, z);
		debug.addLine(info, Color.YELLOW, true, _x, y2, z, x2, y2, z);
		debug.addLine(info, Color.YELLOW, true, x2, y2, z, x2, _y, z);
		debug.addLine(info, Color.YELLOW, true, x2, _y, z, _x, _y, z);
	}
	
	@Override
	public void visualize3D(String info, ExServerPrimitive debug, int minZ, int maxZ)
	{
		final int x2 = _x + _w;
		final int y2 = _y + _h;
		
		// Bottom face (minZ).
		debug.addLine(info, Color.GREEN, true, _x, _y, minZ, _x, y2, minZ);
		debug.addLine(info, Color.GREEN, true, _x, y2, minZ, x2, y2, minZ);
		debug.addLine(info, Color.GREEN, true, x2, y2, minZ, x2, _y, minZ);
		debug.addLine(info, Color.GREEN, true, x2, _y, minZ, _x, _y, minZ);
		
		// Top face (maxZ).
		debug.addLine(info, Color.RED, true, _x, _y, maxZ, _x, y2, maxZ);
		debug.addLine(info, Color.RED, true, _x, y2, maxZ, x2, y2, maxZ);
		debug.addLine(info, Color.RED, true, x2, y2, maxZ, x2, _y, maxZ);
		debug.addLine(info, Color.RED, true, x2, _y, maxZ, _x, _y, maxZ);
		
		// Vertical edges connecting minZ and maxZ faces.
		debug.addLine(info, Color.YELLOW, true, _x, _y, minZ, _x, _y, maxZ);
		debug.addLine(info, Color.YELLOW, true, _x, y2, minZ, _x, y2, maxZ);
		debug.addLine(info, Color.YELLOW, true, x2, y2, minZ, x2, y2, maxZ);
		debug.addLine(info, Color.YELLOW, true, x2, _y, minZ, x2, _y, maxZ);
	}
}