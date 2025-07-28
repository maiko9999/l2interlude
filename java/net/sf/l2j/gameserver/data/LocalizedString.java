package net.sf.l2j.gameserver.data;

import java.util.Locale;

public record LocalizedString(String key, AbstractLocaleData source)
{
	public String get(Locale locale)
	{
		return source.get(locale, key);
	}
}