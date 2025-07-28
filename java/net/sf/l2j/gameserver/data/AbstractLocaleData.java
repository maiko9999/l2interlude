package net.sf.l2j.gameserver.data;


import java.nio.file.Path;
import java.util.Locale;

public abstract class AbstractLocaleData extends AbstractData
{
	public final static Path BASE_LOCALE_PATH;
	
	protected Path resolve(Locale locale, String file)
	{
		return resolve(BASE_LOCALE_PATH).resolve(locale.toString()).resolve(file);
	}
	
	protected Path resolve(Locale locale, Path file)
	{
		return resolve(BASE_LOCALE_PATH).resolve(locale.toString()).resolve(file);
	}
	
	public LocalizedString getLocalizedString(String key)
	{
		return new LocalizedString(key, this);
	}
	
	public abstract String get(Locale locale, String key);
	
	static
	{
		BASE_LOCALE_PATH = Path.of(System.getProperty("net.sf.l2j.gameserver.data.AbstractLocaleData.BASE_LOCALE_PATH", "locale"));
	}
}