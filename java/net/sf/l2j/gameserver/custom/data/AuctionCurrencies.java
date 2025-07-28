package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class AuctionCurrencies implements IXmlReader
{
	private final Map<String, Integer> _currencyMap = new HashMap<>();
	
	public AuctionCurrencies()
	{
		load();
	}
	
	public void reload()
	{
		_currencyMap.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/auctionCurrencies.xml");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "currency", itemsNode ->
		{
			final NamedNodeMap attrs = itemsNode.getAttributes();
			
			String name = parseString(attrs, "name");
			int id = parseInteger(attrs, "id");
			
			_currencyMap.put(name, id);
		}));
	}
	
	public int getCurrencyId(String name)
	{
		return _currencyMap.getOrDefault(name, -1);
	}
	
	public String getCurrencyName(int currencyId)
	{
		return _currencyMap.entrySet().stream().filter(entry -> entry.getValue().equals(currencyId)).map(Map.Entry::getKey).findFirst().orElse("Unknown Currency");
	}
	
	public Set<String> getCurrencyNames()
	{
		return _currencyMap.keySet();
	}
	
	public static AuctionCurrencies getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AuctionCurrencies INSTANCE = new AuctionCurrencies();
	}
}