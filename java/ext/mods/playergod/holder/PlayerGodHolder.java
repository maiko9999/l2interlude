package ext.mods.playergod.holder;

import net.sf.l2j.commons.data.StatSet;

public class PlayerGodHolder
{
	private final boolean enabled;
	
	private final int killsRequired;
	private final int timeWindow;
	private final String killAnnouncement;
	
	private final int heroAuraDuration;
	private final boolean auraOnly;
	
	private final boolean loginAnnouncementEnabled;
	private final String loginMessage;
	
	public PlayerGodHolder(StatSet set)
	{
		enabled = set.getBool("enabled", true);
		
		killsRequired = set.getInteger("killsRequired", 10);
		timeWindow = set.getInteger("timeWindow", 300);
		killAnnouncement = set.getString("killAnnouncement", "");
		
		heroAuraDuration = set.getInteger("heroAuraDuration", 600);
		auraOnly = set.getBool("auraOnly", true);
		
		loginAnnouncementEnabled = set.getBool("loginAnnouncementEnabled", true);
		loginMessage = set.getString("loginMessage", "%player_name% retornou como um verdadeiro DEUS da guerra!");
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public int getKillsRequired()
	{
		return killsRequired;
	}
	
	public int getTimeWindow()
	{
		return timeWindow;
	}
	
	public String getKillAnnouncement()
	{
		return killAnnouncement;
	}
	
	public int getHeroAuraDuration()
	{
		return heroAuraDuration;
	}
	
	public boolean isAuraOnly()
	{
		return auraOnly;
	}
	
	public boolean isLoginAnnouncementEnabled()
	{
		return loginAnnouncementEnabled;
	}
	
	public String getLoginMessage()
	{
		return loginMessage;
	}
}
