package ext.mods.email;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import ext.mods.util.Tokenizer;

public class PlayerEmailManager
{
	private static Logger LOGGER = Logger.getLogger(PlayerEmailManager.class.getName());
	
	public void handlerHtm(Player player, Tokenizer tokenizer)
	{
		String filename = tokenizer.getToken(2);
		navi(player, filename);
	}
	
	public void navi(Player player, String filename)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/menu/" + filename);
		
		StringBuilder sb = new StringBuilder();
		String targetName = player.getSelectedEmailTarget();
		
		sb.append("<table width=300 cellpadding=2 cellspacing=2 align=center bgcolor=000000>");
		
		sb.append("<tr>");
		sb.append("<td width=60 align=right><font color=LEVEL>Jogador:</font></td>");
		
		if (targetName != null)
		{
			sb.append("<td width=120 align=left><font color=00FF00>" + targetName + "</font></td>");
			sb.append("<td width=80 align=center>");
			sb.append("<button value=\"Limpar\" action=\"bypass -h voiced_email clean\" width=55 height=15 back=sek.cbui94 fore=sek.cbui92>");
			sb.append("</td>");
			
		}
		else
		{
			sb.append("<td width=120 align=left><edit var=\"targetName\" width=150></td>");
			sb.append("<td width=80 align=center>");
			sb.append("<button value=\"Buscar\" action=\"bypass -h voiced_email search $targetName\" width=55 height=15 back=sek.cbui94 fore=sek.cbui92><br>");
			sb.append("</td>");
		}
		sb.append("</tr>");
		sb.append("</table><br>");
		
		htm.replace("%playerinfo%", sb.toString());
		htm.replace("%targetName%", targetName != null ? targetName : "invalido");
		player.sendPacket(htm);
	}
	
	public void searchPlayer(Player player, String targetName)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><title>Email Search</title><body><center>");
		sb.append("<font color=LEVEL>Resultado da Busca</font>");
		
		sb.append("<table width=300 border=1 cellspacing=0 cellpadding=3 bgcolor=000000>");
		sb.append("<tr>");
		sb.append("<td width=60 align=center><font color=LEVEL>Status</font></td>");
		sb.append("<td width=110 align=center><font color=LEVEL>Nome</font></td>");
		sb.append("<td width=50 align=center><font color=LEVEL>Ação</font></td>");
		sb.append("</tr>");
		sb.append("</table>");
		

		sb.append("<table width=300 cellspacing=0 cellpadding=3 bgcolor=000000>");
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name, online FROM characters " + "WHERE LOWER(char_name) LIKE ? AND char_name != ? AND accesslevel = 0 " + "ORDER BY online DESC, char_name LIMIT 15"))
		{
			ps.setString(1, "%" + targetName.toLowerCase() + "%");
			ps.setString(2, player.getName());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					String name = rs.getString("char_name");
					
					String displayName = name.length() > 12 ? name.substring(0, 10) + "..." : name;
					boolean isOnline = rs.getInt("online") == 1;
					
					sb.append("<tr>");
					sb.append("<td width=60 align=center><font color=").append(isOnline ? "00FF00" : "FF0000").append(">").append(isOnline ? "Online" : "Offline").append("</font></td>");
					sb.append("<td width=110 align=center>").append(displayName).append("</td>");
					sb.append("<td width=50 align=center>");
					sb.append("<button value=\"OK\" action=\"bypass -h voiced_email target ").append(name).append("\" width=40 height=21 back=sek.cbui94 fore=sek.cbui92>");
					sb.append("</td></tr>");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("searchPlayer DB error: " + e.getMessage());
		}
		
		sb.append("</table><br>");
		sb.append("<button value=\"Voltar\" action=\"bypass -h voiced_email htm email_main.htm\" width=80 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\">");
		sb.append("</center></body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	public static PlayerEmailManager getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerEmailManager instance = new PlayerEmailManager();
	}
}