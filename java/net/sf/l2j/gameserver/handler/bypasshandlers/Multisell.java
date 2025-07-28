package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;

public class Multisell implements IBypassHandler {

	private static final String[] COMMANDS = {
		"multisell",
		"exc_multisell"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target) {
		try {
			if (target instanceof Npc npc)
			{
				if (command.toLowerCase().startsWith(COMMANDS[0])) // multisell
				{
					MultisellData.getInstance().separateAndSend(command.substring(9).trim(), player, npc, false);
					return true;
				}
				else if (command.toLowerCase().startsWith(COMMANDS[1])) // exc_multisell
				{
					MultisellData.getInstance().separateAndSend(command.substring(13).trim(), player, npc, true);
					return true;
				}
			}
			return false;
		}
		catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
