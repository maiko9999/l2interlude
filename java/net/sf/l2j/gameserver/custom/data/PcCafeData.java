package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;

/**
 * @author SweeTs
 */
public final class PcCafeData implements IXmlReader
{
	private final Map<String, String> _cafeData = new HashMap<>();
	
	protected PcCafeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/pcCafe.xml");
		LOGGER.info("Loaded {} pcCafe variables.", _cafeData.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "variable", accessNode ->
			{
				final StatSet set = parseAttributes(accessNode);
				_cafeData.put(set.getString("name"), set.getString("value"));
			});
		});
	}
	
	public void reload()
	{
		_cafeData.clear();
		load();
	}
	
	public boolean getCafeBool(final String key, final boolean defaultValue)
	{
		final Object val = _cafeData.get(key);
		
		if (val instanceof Boolean bool)
			return bool;
		
		if (val instanceof String str)
			return Boolean.parseBoolean(str);
		
		if (val instanceof Number num)
			return num.intValue() != 0;
		
		return defaultValue;
	}
	
	public int getCafeInt(final String key, final int defaultValue)
	{
		final Object val = _cafeData.get(key);
		
		if (val instanceof Number num)
			return num.intValue();
		
		if (val instanceof String str)
			return Integer.parseInt(str);
		
		if (val instanceof Boolean bool)
			return bool ? 1 : 0;
		
		return defaultValue;
	}
	
	public static PcCafeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PcCafeData INSTANCE = new PcCafeData();
	}
}