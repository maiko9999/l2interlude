package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.records.custom.CapsuleBoxItem;
import net.sf.l2j.gameserver.model.records.custom.Item;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class CapsuleBoxData implements IXmlReader
{
	private final Map<Integer, CapsuleBoxItem> _capsuleBoxItems = new HashMap<>();
	
	public CapsuleBoxData()
	{
		load();
	}
	
	public void reload()
	{
		_capsuleBoxItems.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/capsuleBox.xml");
		LOGGER.info("CapsuleBoxData: Loaded " + _capsuleBoxItems.size() + " items.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "items", itemsNode ->
		{
			final NamedNodeMap attrs = itemsNode.getAttributes();
			
			int id = parseInteger(attrs, "id");
			int playerLevel = parseInteger(attrs, "playerLevel");
			
			final CapsuleBoxItem capsuleBoxItem = new CapsuleBoxItem(id, playerLevel);
			
			forEach(itemsNode, "item", itemNode ->
			{
				final NamedNodeMap itemAttrs = itemNode.getAttributes();
				int itemId = parseInteger(itemAttrs, "itemId");
				int min = parseInteger(itemAttrs, "min");
				int max = parseInteger(itemAttrs, "max");
				int enchantLevel = parseInteger(itemAttrs, "enchantLevel", 0);
				int chance = parseInteger(itemAttrs, "chance");
				
				capsuleBoxItem.addItem(new Item(itemId, min, max, enchantLevel, chance));
			});
			
			_capsuleBoxItems.put(id, capsuleBoxItem);
		}));
	}
	
	public CapsuleBoxItem getCapsuleBoxItemById(int id)
	{
		return _capsuleBoxItems.get(id);
	}
	
	public static CapsuleBoxData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CapsuleBoxData INSTANCE = new CapsuleBoxData();
	}
}