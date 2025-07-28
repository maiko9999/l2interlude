package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Link implements IBypassHandler {

	private static final String[] COMMANDS = { "Link" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		final String path = command.substring(5).trim();
		if (path.indexOf("..") != -1)
			return false;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
		html.setFile(player.getLocale(), "html/" + path);
		html.replace("%objectId%", target.getObjectId());
		player.sendPacket(html);
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
