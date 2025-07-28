package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.sql.OfflineTradersTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Menu implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"menu",
		"exp",
		"trade",
		"autoloot",
		"offline",
		"buffprotect",
		"lang"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.ENABLE_MENU)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		if (command.equals("menu"))
			showHtm(player);
		else if (command.startsWith("exp"))
		{
			player.setStopExp(!player.getStopExp());
			player.sendMessage(player.getSysString(player.getStopExp() ? 10_000 : 10_001));
		}
		else if (command.startsWith("trade"))
		{
			player.setTradeRefusal(!player.getTradeRefusal());
			player.sendMessage(player.getSysString(player.getTradeRefusal() ? 10_002 : 10_003));
		}
		else if (command.startsWith("autoloot"))
		{
			player.setAutoLoot(!player.getAutoLoot());
			player.sendMessage(player.getSysString(player.getAutoLoot() ? 10_004 : 10_005));
		}
		else if (command.startsWith("buffprotect"))
		{
			player.setBuffProtected(!player.isBuffProtected());
			player.sendMessage(player.getSysString(player.isBuffProtected() ? 10186 : 10187));
		}
		else if (command.startsWith("offline"))
		{
			if (!OfflineTradersTable.offlineMode(player))
			{
				player.sendMessage(player.getSysString(10_006));
				return false;
			}
			
			if ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) || (player.isCrafting() && Config.OFFLINE_CRAFT_ENABLE))
			{
				player.sendMessage(player.getSysString(10_007));
				player.logout(false);
				return true;
			}
			
			OfflineTradersTable.getInstance().saveOfflineTraders(player);
		}
		else if (command.startsWith("lang") && command.length() > 5)
			player.setLocale(Locale.forLanguageTag(command.substring(5).trim()));
		
		showHtm(player);
		return true;
	}
	
	private void showHtm(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/menu/menu.htm");
		
		final String ACTIVATED = "<font color=00FF00>" + player.getSysString(10_008) + "</font>";
		final String DEACTIVATED = "<font color=FF0000>" + player.getSysString(10_009) + "</font>";
		
		TimeZone timeZone = TimeZone.getTimeZone(Config.TIME_ZONE);
		Calendar currentTime = Calendar.getInstance(timeZone);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(Config.DATE_FORMAT, player.getLocale());
		dateFormat.setTimeZone(timeZone);
		String formattedTime = dateFormat.format(currentTime.getTime());
		String lastRestart = dateFormat.format(GameServer.getInstance().getServerStartTime());
		
		htm.replace("%online%", player.isInStoreMode() ? 0 : World.getInstance().getOnlinePlayerCount() * Config.FAKE_ONLINE_AMOUNT);
		htm.replace("%gainexp%", player.getStopExp() ? ACTIVATED : DEACTIVATED);
		htm.replace("%trade%", player.getTradeRefusal() ? ACTIVATED : DEACTIVATED);
		htm.replace("%autoloot%", player.getAutoLoot() ? ACTIVATED : DEACTIVATED);
		htm.replace("%buffprotect%", player.isBuffProtected() ? ACTIVATED : DEACTIVATED);
		htm.replace("%button%", player.getStopExp() ? "value=" + player.getSysString(10_009) + " action=\"bypass voiced_exp\"" : "value=" + player.getSysString(10_008) + " action=\"bypass voiced_exp\"");
		htm.replace("%button1%", player.getTradeRefusal() ? "value=" + player.getSysString(10_009) + " action=\"bypass voiced_trade\"" : "value=" + player.getSysString(10_008) + " action=\"bypass voiced_trade\"");
		htm.replace("%button2%", player.getAutoLoot() ? "value=" + player.getSysString(10_009) + " action=\"bypass voiced_autoloot\"" : "value=" + player.getSysString(10_008) + " action=\"bypass voiced_autoloot\"");
		htm.replace("%button3%", player.isBuffProtected() ? "value=" + player.getSysString(10_009) + " action=\"bypass voiced_buffprotect\"" : "value=" + player.getSysString(10_008) + " action=\"bypass voiced_buffprotect\"");
		htm.replace("%serverTime%", formattedTime);
		htm.replace("%lastRestart%", lastRestart);
		htm.replace("%trader%", World.getInstance().getTraderCount());
		htm.replace("%maxOnline%", World.getInstance().getMaxOnline());
		
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}