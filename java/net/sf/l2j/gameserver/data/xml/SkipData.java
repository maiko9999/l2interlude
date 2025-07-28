package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;

public class SkipData implements IXmlReader
{
	private static final List<Integer> _skip = new ArrayList<>();
	private boolean _enabled;
	
	public SkipData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("xml/skippingItems.xml");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			final StatSet set = parseAttributes(listNode);
			_enabled = set.getBool("enabled", false);
			
			if (_enabled)
			{
				forEach(listNode, "item", itemNode ->
				{
					final StatSet itemSet = parseAttributes(itemNode);
					int itemId = itemSet.getInteger("id");
					_skip.add(itemId);
				});
				LOGGER.info("Loaded {} skip list templates.", _skip.size());
			}
			else
				LOGGER.warn("Skipping items list is disabled. No items will be loaded from this list.");
		});
	}
	
	public boolean isSkipped(int itemId)
	{
		return _skip.contains(itemId);
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public static SkipData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkipData INSTANCE = new SkipData();
	}
}