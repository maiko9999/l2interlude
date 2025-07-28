package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;

public class TeleportRequest implements IBypassHandler
{
	private static final String[] COMMANDS = { "teleport_request" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{		
		if (target instanceof Npc npc)
			npc.showTeleportWindow(player, TeleportType.STANDARD);
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}