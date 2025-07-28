package net.sf.l2j.gameserver.data;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;

public final class HTMLData extends AbstractLocaleData
{
	private static final CLogger LOGGER = new CLogger(HTMLData.class.getName());
	
	private final Map<Locale, Map<String, String>> _data = new HashMap<>();
	
	@Override
	public void load()
	{
		for (var locale : Config.LOCALES)
		{
			_data.put(locale, new ConcurrentHashMap<>());
			doLoad(locale);
		}
	}
	
	public void reload()
	{
		LOGGER.info("HTMLData has been cleared ({} entries).", _data.size());
		
		for (var locale : Config.LOCALES)
		{
			_data.get(locale).clear();
			doLoad(locale);
		}
	}
	
	private void doLoad(Locale locale)
	{
		final Path localeBasePath = resolve(locale, "");
		try
		{
			Files.walkFileTree(localeBasePath, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					final var fileKey = localeBasePath.relativize(file).toString().replace("\\", "/");
					if (Files.isDirectory(file) || !(fileKey.endsWith(".htm") || fileKey.endsWith(".html")))
						return FileVisitResult.CONTINUE;
					
					ForkJoinPool.commonPool().execute(() ->
					{
						try
						{
							var content = readString(file);
							_data.get(locale).put(fileKey, content);
						}
						catch (DataException e)
						{
							e.printStackTrace();
						}
					});
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public String get(Locale locale, String key)
	{
		return getHtm(locale, key);
	}
	
	public String getHtm(Player player, String file)
	{
		return getHtm(player.getLocale(), file);
	}
	
	public String getHtm(Locale locale, String file)
	{
		var result = _data.get(locale).get(file);
		if (result == null)
		{
			result = _data.get(Config.DEFAULT_LOCALE).get(file);
			if (result == null)
				return "<html><body>Not found file: " + file + "</body></html>";
		}
		return result;
	}
	
	public boolean exists(Player player, String file)
	{
		return exists(player.getLocale(), file);
	}
	
	public boolean exists(Locale locale, String file)
	{
		var path = resolve(locale, file);
		
		if (Files.exists(path) && !Files.isDirectory(path))
			return true;
		
		if (!locale.equals(Config.DEFAULT_LOCALE))
			return exists(Config.DEFAULT_LOCALE, file);
		
		return false;
	}
	
	public static HTMLData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HTMLData INSTANCE = new HTMLData();
	}
}