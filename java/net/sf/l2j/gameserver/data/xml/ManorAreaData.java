package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.model.manor.ManorArea;
import net.sf.l2j.gameserver.model.manor.Seed;
import net.sf.l2j.gameserver.model.residence.castle.Castle;

import org.w3c.dom.Document;

/**
 * This class loads and stores {@link ManorArea}s.<br>
 * <br>
 * {@link ManorArea} is a polygon/territory linked to a specific {@link Castle}. This allow {@link Seed} checks while sowing.
 */
public class ManorAreaData implements IXmlReader
{
	private final List<ManorArea> _manorAreas = new ArrayList<>();
	
	protected ManorAreaData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("xml/manorAreas.xml");
		LOGGER.info("Loaded {} manor areas.", _manorAreas.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		final List<Point2D> coords = new ArrayList<>();
		
		forEach(doc, "list", listNode -> forEach(listNode, "area", areaNode ->
		{
			// Load generic values.
			final StatSet set = parseAttributes(areaNode);
			
			// Load nodes.
			forEach(areaNode, "node", nodeNode -> coords.add(parsePoint2D(nodeNode)));
			set.set("coords", coords);
			
			// Create manor area and store it in the List.
			_manorAreas.add(new ManorArea(set));
			
			// Clear coordinates.
			coords.clear();
		}));
	}
	
	/**
	 * @return The {@List} of available {@link ManorArea}s.
	 */
	public final List<ManorArea> getManorAreas()
	{
		return _manorAreas;
	}
	
	/**
	 * @param monster : The {@link Monster} to evaluate.
	 * @return The {@link ManorArea} of the given {@link Monster}.
	 */
	public final ManorArea getManorArea(Monster monster)
	{
		// The manor area is determined based on monster's spawn location, not its actual location.
		final Location loc = monster.getSpawnLocation();
		
		return _manorAreas.stream().filter(ma -> ma.isInside(loc.getX(), loc.getY())).findFirst().orElse(null);
	}
	
	public static ManorAreaData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ManorAreaData INSTANCE = new ManorAreaData();
	}
}