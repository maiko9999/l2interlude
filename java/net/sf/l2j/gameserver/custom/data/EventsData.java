package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.records.custom.EventItem;
import net.sf.l2j.gameserver.model.records.custom.EventsInfo;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class EventsData implements IXmlReader
{
	private final List<EventsInfo> _events = new ArrayList<>();
	
	protected EventsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/events.xml");
		LOGGER.info("Loaded {} events.", _events.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "event", event ->
		{
			String eventName = parseString(event.getAttributes(), "name");
			
			final List<EventItem> items = new ArrayList<>();
			
			forEach(event, "item", itemNode ->
			{
				final NamedNodeMap attrs = itemNode.getAttributes();
				int id = parseInteger(attrs, "id");
				int count = parseInteger(attrs, "count");
				int chance = parseInteger(attrs, "chance");
				int minLvl = parseInteger(attrs, "minLvl", 1);
				
				items.add(new EventItem(id, count, chance, minLvl));
			});
			
			_events.add(new EventsInfo(eventName, items));
		}));
	}
	
	public EventsInfo getEventsData(String eventName)
	{
		return _events.stream().filter(event -> event.eventName().equals(eventName)).findFirst().orElse(null);
	}
	
	public static EventsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventsData INSTANCE = new EventsData();
	}
}