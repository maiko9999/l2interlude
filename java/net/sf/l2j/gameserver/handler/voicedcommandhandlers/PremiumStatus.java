package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.SimpleDateFormat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class PremiumStatus implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"premium"
	};
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.USE_PREMIUM_SERVICE)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		if ("premium".equals(command))
		{
			NpcHtmlMessage htm = new NpcHtmlMessage(0);
			htm.setFile(player.getLocale(), (player.getPremiumService() == 0 ? "html/mods/premium/normal.htm" : "html/mods/premium/premium.htm"));
			
			htm.replace("%rate_xp%", Config.RATE_XP);
			htm.replace("%rate_sp%", Config.RATE_SP);
			htm.replace("%rate_drop%", Config.RATE_DROP_ITEMS);
			htm.replace("%rate_spoil%", Config.RATE_DROP_SPOIL);
			htm.replace("%rate_currency%", Config.RATE_DROP_CURRENCY);
			htm.replace("%current%", String.valueOf(DATE_FORMAT.format(System.currentTimeMillis())));
			htm.replace("%prem_rate_xp%", Config.PREMIUM_RATE_XP);
			htm.replace("%prem_rate_sp%", Config.PREMIUM_RATE_SP);
			htm.replace("%prem_rate_drop%", Config.PREMIUM_RATE_DROP_ITEMS);
			htm.replace("%prem_rate_spoil%", Config.PREMIUM_RATE_DROP_SPOIL);
			htm.replace("%prem_currency%", Config.PREMIUM_RATE_DROP_CURRENCY);
			if (player.getPremiumService() != 0)
				htm.replace("%expires%", String.valueOf(DATE_FORMAT.format(player.getPremServiceData())));
			
			player.sendPacket(htm);
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}