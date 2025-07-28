package ext.mods.FarmEventRandom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;

import ext.mods.FarmEventRandom.holder.MessagesHolder;
import ext.mods.FarmEventRandom.holder.RamdomConfig;
import ext.mods.FarmEventRandom.holder.RandomSpawnHolder;

public class RandomData implements IXmlReader
{
	private RamdomConfig _configs;
	private final Map<String, Map<Integer, List<RandomSpawnHolder>>> _spawnsList = new HashMap<>();
	
	private final Map<String, MessagesHolder> _messages = new HashMap<>();
	
	public RandomData()
	{
		load();
	}
	
	public void reload()
	{
		_spawnsList.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/random_event.xml");
		LOGGER.info("Loaded random events.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "ramdoms", eventsNode ->
		{
			
			forEach(eventsNode, "event", eventNode ->
			{
				forEach(eventNode, "configs", confisNode ->
				{
					forEach(confisNode, "config", configNode ->
					{
						StatSet set = parseAttributes(configNode);
						_configs = new RamdomConfig(set);
					});
					
				});
				
				forEach(eventNode, "messages", msgsNode ->
				{
					MessagesHolder holder = new MessagesHolder();
					
					forEach(msgsNode, "message", msgNode ->
					{
						StatSet set2 = parseAttributes(msgNode);
						
						holder.addOnPrepare(set2.getString("onPrepare", ""));
						holder.addOnStart(set2.getString("onStart", ""));
						holder.addOnZone(set2.getString("onZone", ""));
						holder.addOnEnd(set2.getString("onEnd", ""));
						holder.addOnAuto(set2.getString("onAuto", ""));
					});
					
					_messages.put(_configs.getName(), holder);
				});
				
				Map<Integer, List<RandomSpawnHolder>> zoneSpawns = new HashMap<>();
				
				forEach(eventNode, "spawns", spawnsNode ->
				{
					int zoneId = parseInteger(spawnsNode.getAttributes(), "zoneId");
					
					List<RandomSpawnHolder> spawnList = zoneSpawns.computeIfAbsent(zoneId, k -> new ArrayList<>());
					
					forEach(spawnsNode, "spawn", spawnNode ->
					{
						StatSet sets = parseAttributes(spawnNode);
						RandomSpawnHolder spawn = new RandomSpawnHolder(sets);
						spawnList.add(spawn);
					});
				});
				
				_spawnsList.put(_configs.getName(), zoneSpawns);
				
			});
		});
	}
	
	public RamdomConfig getConfig()
	{
		return _configs;
	}
	
	public List<RandomSpawnHolder> getSpawns(String eventName, int zoneId)
	{
		Map<Integer, List<RandomSpawnHolder>> zones = _spawnsList.get(eventName);
		if (zones == null)
			return new ArrayList<>();
		
		return zones.getOrDefault(zoneId, new ArrayList<>());
	}
	
	public MessagesHolder getMessages(String eventName)
	{
		return _messages.get(eventName);
	}
	
	public static RandomData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RandomData _instance = new RandomData();
	}
}
