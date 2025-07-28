package net.sf.l2j.gameserver.handler;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

public interface IBypassHandler
{
	final CLogger LOGGER = new CLogger(IBypassHandler.class.getName());
	
	boolean useBypass(String command, Player player, Creature creature);
	
	String[] getBypassList();
}