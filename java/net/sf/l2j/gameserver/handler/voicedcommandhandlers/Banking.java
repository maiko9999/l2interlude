package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

public class Banking implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"bank",
		"withdraw",
		"deposit"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.ENABLE_COMMAND_GOLDBAR)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		if (command.equalsIgnoreCase("bank"))
			player.sendMessage(player.getSysString(10_194, Config.BANKING_SYSTEM_ADENA, Config.BANKING_SYSTEM_GOLDBARS, Config.BANKING_SYSTEM_GOLDBARS, Config.BANKING_SYSTEM_ADENA));
		else if (command.equalsIgnoreCase("deposit"))
		{
			if (player.getAdena() >= Config.BANKING_SYSTEM_ADENA)
			{
				player.getInventory().reduceAdena(Config.BANKING_SYSTEM_ADENA);
				player.getInventory().addItem(3470, Config.BANKING_SYSTEM_GOLDBARS);
				player.sendPacket(new ItemList(player, true));
				player.sendMessage(player.getSysString(10_195, Config.BANKING_SYSTEM_GOLDBARS, Config.BANKING_SYSTEM_ADENA));
			}
			else
				player.sendMessage(player.getSysString(10_196, Config.BANKING_SYSTEM_ADENA));
		}
		else if (command.equalsIgnoreCase("withdraw"))
		{
			long a = player.getAdena();
			long b = Config.BANKING_SYSTEM_ADENA;
			
			if (a + b > Integer.MAX_VALUE)
			{
				player.sendMessage(player.getSysString(10_197));
				return false;
			}
			
			if (player.getInventory().getItemCount(3470, 0) >= Config.BANKING_SYSTEM_GOLDBARS)
			{
				player.getInventory().destroyItemByItemId(3470, Config.BANKING_SYSTEM_GOLDBARS);
				player.getInventory().addAdena(Config.BANKING_SYSTEM_ADENA);
				player.sendPacket(new ItemList(player, true));
				player.sendMessage(player.getSysString(10_198, Config.BANKING_SYSTEM_ADENA, Config.BANKING_SYSTEM_GOLDBARS));
			}
			else
				player.sendMessage(player.getSysString(10_199, Config.BANKING_SYSTEM_ADENA));
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}