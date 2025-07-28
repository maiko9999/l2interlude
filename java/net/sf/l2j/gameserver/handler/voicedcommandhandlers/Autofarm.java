package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager;

public class Autofarm implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"autofarm"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.AUTOFARM_ENABLED)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		AutoFarmManager.getInstance().showIndexWindow(player);
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}