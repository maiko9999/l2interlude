package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.AbstractLocaleData;

import org.w3c.dom.Document;

public final class SysString extends AbstractLocaleData implements IXmlReader
{
	private final Map<Locale, Map<String, String>> _data = new HashMap<>();
	
	private Locale _activeLocale;
	
	public void reload()
	{
		LOGGER.info("SysString has been cleared ({} entries).", _data.size());
		
		for (var locale : Config.LOCALES)
		{
			_data.get(locale).clear();
			load();
		}
	}
	
	@Override
	public void load()
	{
		for (var locale : Config.LOCALES)
		{
			_activeLocale = locale;
			_data.put(locale, new ConcurrentHashMap<>());
			parseFile(resolve(locale, "sysstring.xml").toString());
			locale = null;
		}
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", root -> forEach(root, "string", node ->
		{
			_data.get(_activeLocale).put(parseString(node.getAttributes(), "key"), node.getTextContent());
		}));
	}
	
	@Override
	public String get(Locale locale, String key)
	{
		var result = _data.get(locale).get(key);
		if (result == null)
		{
			result = _data.get(Config.DEFAULT_LOCALE).get(key);
			if (result == null)
				return "missing sysstring: " + key;
		}
		return result;
	}
	
	public static SysString getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SysString INSTANCE = new SysString();
	}
}