package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @autor BAN - L2JDEV
 */
public class VoicedTopEnchantCmd implements IVoicedCommandHandler
{
	private final static int PAGE_LIMIT_TOP_ENCHANT_RANKING = 30;
	
	private static final String[] VOICED_COMMANDS =
	{
		"topenchant"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.startsWith("topenchant"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int page = 1;
			if (st.hasMoreTokens())
			{
				page = Integer.parseInt(st.nextToken());
			}
			htmlTopRankingEnchant(player, page);
		}
		
		return true;
	}
	
	private static void htmlTopRankingEnchant(Player player, int page)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body>");
		sb.append("<br><br>");
		sb.append("<table width=285 bgcolor=000000 cellspacing=0 cellpadding=5>");
		sb.append("<tr><td colspan=2 align=center><font color=LEVEL><b>Top Enchant Ranking</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=320 height=1><br>");
		
		int offset = (page - 1) * PAGE_LIMIT_TOP_ENCHANT_RANKING;
		
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("SELECT cha.char_name, it.enchant_level, it.item_id FROM characters cha INNER JOIN items it ON cha.obj_Id = it.owner_id WHERE it.enchant_level > 1 AND cha.accesslevel = 0 ORDER BY it.enchant_level DESC LIMIT ?, ?"))
			{
				ps.setInt(1, offset);
				ps.setInt(2, PAGE_LIMIT_TOP_ENCHANT_RANKING);
				
				try (ResultSet rs = ps.executeQuery())
				{
					int index = 1 + offset;
					while (rs.next())
					{
						final Item item = ItemData.getInstance().getTemplate(rs.getInt("it.item_id"));
						if (item instanceof Weapon)
						{
							String itemName = item.getName();
							if (itemName.length() > 20)
							{
								itemName = itemName.substring(0, 20) + "...";
							}
							
							final Player databasePlayer = World.getInstance().getPlayer(player.getName());
							final String status = "L2UI_CH3.msnicon" + ((databasePlayer != null && databasePlayer.isOnline()) ? "4" : "1");
							
							sb.append("<table width=285 bgcolor=" + ((index % 2 == 0) ? "000000" : "000000") + " cellspacing=0 cellpadding=4>");
							sb.append("<tr>");
							sb.append("<td width=16><img src=\"" + status + "\" width=16 height=16></td>");
							sb.append("<td align=left>");
							sb.append("<font color=LEVEL><b>" + rs.getString("cha.char_name") + "</b></font><br1>");
							sb.append("<font color=B09878>" + itemName + " +<b>" + rs.getInt("it.enchant_level") + "</b></font>");
							sb.append("</td>");
							sb.append("</tr>");
							sb.append("</table>");
							sb.append("<img src=\"L2UI.SquareGray\" width=320 height=1>");
							index++;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		sb.append("<br>");
		sb.append("<table width=285 cellpadding=3>");
		sb.append("<tr>");
		sb.append("<td align=left>");
		if (page > 1) {
		    sb.append("<a action=\"bypass -h voiced_topenchant ").append(page - 1).append("\"><font color=99BBFF>&lt;&lt; Anterior</font></a>");
		} else {
		    sb.append("<font color=000000>&lt;&lt; Anterior</font>");
		}
		sb.append("</td>");

		sb.append("<td align=center><font color=LEVEL> Página ").append(page).append(" </font></td>");

		sb.append("<td align=right>");
		sb.append("<a action=\"bypass -h voiced_topenchant ").append(page + 1).append("\"><font color=99BBFF>Próxima &gt;&gt;</font></a>");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");

		sb.append("</body></html>");
		
		htm.setHtml(sb.toString());
		htm.replace("%info%", sb.toString());
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
