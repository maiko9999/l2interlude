package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SellBuffsManager;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class SellBuffCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"sellbuff",
		"sellbuffs"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.SELLBUFF_ENABLED)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		switch (command)
		{
			case "sellbuff":
			case "sellbuffs":
				SellBuffsManager.getInstance().sendSellMenu(player);
				break;
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}