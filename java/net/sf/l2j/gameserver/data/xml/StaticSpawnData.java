package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.records.custom.StaticSpawn;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class StaticSpawnData implements IXmlReader
{
	private final Map<Integer, StaticSpawn> _staticSpawn = new HashMap<>();
	
	protected StaticSpawnData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("xml/staticSpawn.xml");
		LOGGER.info("Loaded {} raidboss static spawn.", _staticSpawn.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "npc", npcNode ->
		{
			final NamedNodeMap attrs = npcNode.getAttributes();
			boolean isEnable = parseBoolean(attrs, "isEnable", false);
			int id = parseInteger(attrs, "id");
			
			List<String> days = Arrays.asList(parseString(attrs, "days").split(";"));
			List<String> time = Arrays.asList(parseString(attrs, "time").split(";"));
			
			int randomTime = parseInteger(attrs, "randomTime", 0) * 60;
			
			boolean earthQuake = parseBoolean(attrs, "earthQuake", false);
			boolean announce = parseBoolean(attrs, "announce", false);
			
			final StaticSpawn staticSpawn = new StaticSpawn(isEnable, id, days, time, randomTime, earthQuake, announce);
			_staticSpawn.put(id, staticSpawn);
		}));
	}
	
	public StaticSpawn getById(int id)
	{
		return _staticSpawn.get(id);
	}
	
	public static StaticSpawnData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final StaticSpawnData INSTANCE = new StaticSpawnData();
	}
}