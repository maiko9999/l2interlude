package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;

public class ChatLink implements IBypassHandler {

	private static final String[] COMMANDS = { "Chat" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target) {
		if (target instanceof Npc npc)
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException | NumberFormatException e)
			{
				// Do nothing.
			}
			
			npc.showChatWindow(player, val);
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
