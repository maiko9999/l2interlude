package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.records.custom.EnchantScroll;

import org.w3c.dom.Document;

public class EnchantData implements IXmlReader
{
	private final Map<Integer, EnchantScroll> _data = new HashMap<>();
	
	public EnchantData()
	{
		load();
	}
	
	public void reload()
	{
		_data.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/enchants.xml");
		LOGGER.info("Loaded {} enchant scroll data.", _data.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "scroll", scrollNode ->
		{
			final StatSet set = parseAttributes(scrollNode);
			forEach(scrollNode, "settings", settingsNode -> set.putAll(parseAttributes(settingsNode)));
			forEach(scrollNode, "chances", chancesNode -> set.putAll(parseAttributes(chancesNode)));
			forEach(scrollNode, "announce", announceNode -> set.putAll(parseAttributes(announceNode)));
			_data.put(set.getInteger("id"), new EnchantScroll(set));
		}));
	}
	
	public EnchantScroll getEnchantScroll(ItemInstance item)
	{
		return _data.get(item.getItemId());
	}
	
	public static EnchantData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantData INSTANCE = new EnchantData();
	}
}