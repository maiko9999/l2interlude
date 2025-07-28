package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.List;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class PlayerInfo implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"info",
		"item"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (player.getTarget() == null || !(player.getTarget() instanceof Player))
		{
			player.sendMessage("Você precisa selecionar um jogador como alvo.");
			return false;
		}
		
		Player targetPlayer = (Player) player.getTarget();
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><title>Player Info</title><body>");
		sb.append("<font color=LEVEL><b>").append(targetPlayer.getName()).append("</b></font><br><br>");
		
		sb.append("<center><table width=300><tr>");
		sb.append("<td><button value=\"Status\" action=\"bypass -h voiced_info\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		sb.append("<td><button value=\"Equipamentos\" action=\"bypass -h voiced_item\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		sb.append("</tr></table></center><br>");
		
		if (command.equals("info"))
		{
			sb.append("<table width=300 bgcolor=000000>");
			sb.append("<tr><td width=120><font color=LEVEL>HP:</font></td><td>").append((int) targetPlayer.getStatus().getHp()).append(" / ").append(targetPlayer.getStatus().getMaxHp()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>MP:</font></td><td>").append((int) targetPlayer.getStatus().getMp()).append(" / ").append(targetPlayer.getStatus().getMaxMp()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>CP:</font></td><td>").append((int) targetPlayer.getStatus().getCp()).append(" / ").append(targetPlayer.getStatus().getMaxCp()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>Level:</font></td><td>").append(targetPlayer.getStatus().getLevel()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>Classe:</font></td><td>").append(targetPlayer.getTemplate().getClassName()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>P. Atk:</font></td><td>").append(targetPlayer.getStatus().getPAtk(null)).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>M. Atk:</font></td><td>").append(targetPlayer.getStatus().getMAtk(null, null)).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>P. Def:</font></td><td>").append(targetPlayer.getStatus().getPDef(null)).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>M. Def:</font></td><td>").append(targetPlayer.getStatus().getMDef(null, null)).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>P. atkSpeed:</font></td><td>").append(targetPlayer.getStatus().getPAtkSpd()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>C. Speed:</font></td><td>").append(targetPlayer.getStatus().getMAtkSpd()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>STR:</font></td><td>").append(targetPlayer.getStatus().getSTR()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>CON:</font></td><td>").append(targetPlayer.getStatus().getCON()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>DEX:</font></td><td>").append(targetPlayer.getStatus().getDEX()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>INT:</font></td><td>").append(targetPlayer.getStatus().getINT()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>MEN:</font></td><td>").append(targetPlayer.getStatus().getMEN()).append("</td></tr>");
			sb.append("<tr><td><font color=LEVEL>WIT:</font></td><td>").append(targetPlayer.getStatus().getWIT()).append("</td></tr>");
			sb.append("</table>");
		}
		
		else if (command.startsWith("item"))
		{
			int page = 1;
			if (command.contains(" "))
			{
				try
				{
					page = Integer.parseInt(command.split(" ")[1]);
				}
				catch (Exception e)
				{
					page = 1;
				}
			}
			
			List<ItemInstance> paperdoll = targetPlayer.getInventory().getPaperdollItems();
			int itemsPerPage = 7;
			int totalItems = paperdoll.size();
			int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
			
			int start = (page - 1) * itemsPerPage;
			int end = Math.min(start + itemsPerPage, totalItems);
			
			sb.append("<table width=300 bgcolor=000000>");
			for (int i = start; i < end; i++)
			{
				ItemInstance item = paperdoll.get(i);
				if (item != null)
				{
					sb.append("<tr>");
					sb.append("<td width=40><img src=\"").append(item.getItem().getIcon()).append("\" width=32 height=32></td>");
					sb.append("<td>").append(item.getItem().getName());
					if (item.getEnchantLevel() > 0)
						sb.append(" +").append(item.getEnchantLevel());
					sb.append("</td></tr>");
				}
			}
			sb.append("</table><br>");
			
			sb.append("<table width=300><tr>");
			if (page > 1)
				sb.append("<td><button value=\"<<\" action=\"bypass -h voiced_item ").append(page - 1).append("\" width=40 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			else
				sb.append("<td width=40></td>");
			
			sb.append("<td align=center><font color=LEVEL>Página ").append(page).append(" / ").append(totalPages).append("</font></td>");
			
			if (page < totalPages)
				sb.append("<td align=right><button value=\">>\" action=\"bypass -h voiced_item ").append(page + 1).append("\" width=40 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			else
				sb.append("<td width=40></td>");
			sb.append("</tr></table>");
		}
		
		sb.append("</body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
