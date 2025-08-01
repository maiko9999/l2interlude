package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.sql.Date;
import java.text.SimpleDateFormat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.model.residence.clanhall.SiegableHall;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class SiegeInfo implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"siege"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("siege") && Config.SIEGE_INFO)
			showSiegeHtm(player);
		else
			player.sendMessage(player.getSysString(10_200));
		
		return true;
	}
	
	private void showSiegeHtm(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/menu/siege.htm");
		StringBuilder content = new StringBuilder();
		StringBuilder content2 = new StringBuilder();
		
		content.append("<table width=280><tr><td align=center width=70>Castle</td><td align=center width=140>Siege date</td><td align=center width=70>Skill/Reward</td></tr></table>");
		int n = 0;
		content.append("<table width=280>");
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			Date siegeDate = new Date(castle.getSiege().getSiegeDate().getTimeInMillis());
			String formattedDate = dateFormat.format(siegeDate);
			
			StringBuilder html = new StringBuilder();
			html.append("<tr><td>");
			html.append(n % 2 == 0 ? "<table width=280 height=22 bgcolor=000000><tr>" : "<table width=280 height=22><tr>");
			html.append("<td width=100>").append(castle.getName()).append("</td>");
			html.append("<td width=140 align=center>").append(formattedDate).append("</td>");
			html.append("<td width=70 align=center>");
			
			if (castle.getSkillsMember().size() >= 1 || castle.getSkillsLeader().size() >= 1)
				html.append("<font color=00FF00>YES</font>");
			else
				html.append("<font color=FF0000>N/A</font>");
			
			if (castle.getItemsMember().size() >= 1 || castle.getItemsLeader().size() >= 1)
				html.append(" / <font color=00FF00>YES</font>");
			else
				html.append(" / <font color=FF0000>N/A</font>");
			
			html.append("</td></tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
			html.append("</td></tr>");
			
			content.append(html.toString());
			n++;
		}
		content.append("</table>");
		
		content2.append("<table width=300><tr><td align=center width=150>Clan Hall</td><td align=center width=140>Siege date</td></tr></table>");
		content2.append("<table width=300>");
		
		for (SiegableHall siegableHall : ClanHallManager.getInstance().getSiegableHalls())
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			Date siegeDate = new Date(siegableHall.getSiegeDate().getTimeInMillis());
			String formattedDate = dateFormat.format(siegeDate);
			
			StringBuilder html = new StringBuilder();
			html.append("<tr><td>");
			html.append(n % 2 == 0 ? "<table width=320 height=22 bgcolor=000000><tr>" : "<table width=320 height=22><tr>");
			html.append("<td width=255>").append(siegableHall.getName()).append("</td>");
			html.append("<td width=140 align=center>").append(formattedDate).append("</td>");
			html.append("<td width=70 align=center>");
			
			html.append("</td></tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
			html.append("</td></tr>");
			
			content2.append(html.toString());
			n++;
		}
		content2.append("</table>");
		
		htm.replace("%castle_list%", content.toString());
		htm.replace("%ch_list%", content2.toString());
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}