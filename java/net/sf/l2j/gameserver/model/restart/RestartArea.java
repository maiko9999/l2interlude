package net.sf.l2j.gameserver.model.restart;

import java.util.EnumMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.geometry.Territory;

import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * A zone used as restart point when dead or scrolling out.<br>
 * <br>
 * It priors and overrides behavior from {@link RestartPoint}, and enforce the restart point based on {@link ClassRace}.
 */
public class RestartArea extends Territory
{
	private final EnumMap<ClassRace, String> _classRestrictions;
	
	@SuppressWarnings("unchecked")
	public RestartArea(StatSet set)
	{
		super(set);
		
		_classRestrictions = set.getObject("classRestrictions", EnumMap.class);
	}
	
	public String getClassRestriction(Player player)
	{
		return _classRestrictions.get(player.getTemplate().getRace());
	}
}