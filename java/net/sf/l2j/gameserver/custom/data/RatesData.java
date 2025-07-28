package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.records.custom.RatesHolder;

import org.w3c.dom.Document;

public class RatesData implements IXmlReader
{
	private final Map<Integer, RatesHolder> _ratess = new HashMap<>();
	
	public RatesData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/rates.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ratess.size() + " rates.");
		
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> {
			forEach(listNode, "rate", greetNode -> {
				StatSet set = new StatSet(parseAttributes(greetNode));
				RatesHolder holder = new RatesHolder(set);
				_ratess.put(holder.getLevel(), holder);
			});
		});
	}
	
	public RatesHolder getRates(int lvl)
	{
		return _ratess.get(lvl);
	}
	
	public static RatesData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RatesData _instance = new RatesData();
	}
}
