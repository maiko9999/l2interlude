package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Player;

public interface IVoicedCommandHandler
{
	default boolean useVoicedCommand(String command, Player player)
	{
		return useVoicedCommand(command, player, "");
	}
	
	boolean useVoicedCommand(String command, Player player, String target);
	
	public String[] getVoicedCommandList();
}
