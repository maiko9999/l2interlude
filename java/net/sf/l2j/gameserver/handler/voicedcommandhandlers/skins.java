package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import ext.mods.dressme.DressMeData;
import ext.mods.dressme.holder.DressMeHolder;
import ext.mods.dressme.holder.DressMeVisualType;

public class skins implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"skin"
	};
	private static final int ITEMS_PER_PAGE = 5;
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // skip 'skin'
		
		String type = "ARMOR";
		int page = 1;
		
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			
			if ("type".equalsIgnoreCase(token) && st.hasMoreTokens())
			{
				type = st.nextToken().toUpperCase();
			}
			else if ("page".equalsIgnoreCase(token) && st.hasMoreTokens())
			{
				try
				{
					page = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException e)
				{
					page = 1;
				}
			}
			else if ("apply".equalsIgnoreCase(token) && st.hasMoreTokens())
			{
				
				int skillId = Integer.parseInt(st.nextToken());
				
				final long cooldown = 900;
				
				long currentTime = System.currentTimeMillis();
				if (currentTime - player.getLastDressMeSummonTime() < cooldown)
				{
					player.sendMessage("You need to wait before summoning another DressMe.");
					return true;
				}
				
				DressMeHolder skin = DressMeData.getInstance().getBySkillId(skillId);
				if (skin != null)
				{
					if (player.getPremiumService() <= 0)
					{
						player.sendMessage("Este comando é exclusivo para jogadores VIP.");
						
						return true;
					}
					
					if (skin.isVip() || player.getPremiumService() > 0)
					{
						player.applyDressMe(skin, true);
						player.sendMessage("Skin aplicada: " + skin.getName());
						player.setLastDressMeSummonTime(currentTime);
					}
					
					
				}
				
			}
			else if ("preview".equalsIgnoreCase(token) && st.hasMoreTokens())
			{
				int skillId = Integer.parseInt(st.nextToken());
				final long cooldown = 1000 * 60 * 1;
				
				long currentTime = System.currentTimeMillis();
				if (currentTime - player.getLastDressMeSummonTime() < cooldown)
				{
					player.sendMessage("You need to wait before summoning another DressMe.");
					return true;
				}
				
				DressMeHolder skin = DressMeData.getInstance().getBySkillId(skillId);
				if (skin != null)
				{
					player.applyDressMe(skin, false);
					player.sendMessage("Visual temporário: " + skin.getName());
					ThreadPool.schedule(() ->
					{
						player.removeDressMeArmor();
						player.removeDressMeWeapon();
					}, 1L * 60L * 1000L);
					player.setLastDressMeSummonTime(currentTime);
				}
				
			}
			else if ("clean".equalsIgnoreCase(token))
			{
				player.removeDressMeArmor();
				player.removeDressMeWeapon();
				
			}
		}
		
	
		showSkins(player, type, page);
		return true;
		
	}
	
	private void showSkins(Player player, String type, int page)
	{
		List<DressMeHolder> all = DressMeData.getInstance().getEntries().stream().filter(e -> e.getType().toString().equalsIgnoreCase(type)).collect(Collectors.toList());
		
		int totalPages = (int) Math.ceil((double) all.size() / ITEMS_PER_PAGE);
		page = Math.max(1, Math.min(page, totalPages));
		
		int start = (page - 1) * ITEMS_PER_PAGE;
		int end = Math.min(start + ITEMS_PER_PAGE, all.size());
		
		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Skin</title><body><center>");
		sb.append("<br><font color=LEVEL>Visualizações - ").append(type).append("</font>");
		
		sb.append("<table><tr>");
		sb.append("<td><button value=\"ARMOR\" action=\"bypass -h voiced_skin type ARMOR\" width=60 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		sb.append("<td><button value=\"WEAPON\" action=\"bypass -h voiced_skin type WEAPON\" width=60 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		sb.append("</tr></table><br>");
		

		for (int i = start; i < end; i++)
		{
			DressMeHolder skin = all.get(i);
			int iconId = (skin.getType() == DressMeVisualType.WEAPON ? skin.getRightHandId() : skin.getChestId());
			
			sb.append("<table width=300><tr>");
			sb.append("<td width=32><img src=\"icon." + getIconName(iconId) + "\" width=32 height=32></td>");
			sb.append("<td><font color=\"LEVEL\">" + skin.getName() + "</font>");
			sb.append("<table><tr>");
			sb.append("<td><button value=\"Aplicar\" action=\"bypass -h voiced_skin apply " + skin.getSkillId() + "\" width=60 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			sb.append("<td><button value=\"Testar\" action=\"bypass -h voiced_skin preview " + skin.getSkillId() + "\" width=60 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			sb.append("<td><button value=\"Limpa\" action=\"bypass -h voiced_skin clean" + "\" width=60 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			sb.append("</tr></table>");
			sb.append("</td></tr></table>");
		}
		
	
		sb.append("<table><tr>");
		if (page > 1)
			sb.append("<td><button value=\"<<\" action=\"bypass -h voiced_skin type " + type + " page " + (page - 1) + "\" width=40 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		else
			sb.append("<td width=40></td>");
		
		sb.append("<td width=200 align=center>Page ").append(page).append(" / ").append(totalPages).append("</td>");
		
		if (page < totalPages)
			sb.append("<td><button value=\">>\" action=\"bypass -h voiced_skin type " + type + " page " + (page + 1) + "\" width=40 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		else
			sb.append("<td width=40></td>");
		
		sb.append("</tr></table>");
		
		sb.append("</center></body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
		
	}
	
	private String getIconName(int itemId)
	{
		return ItemData.getInstance().getTemplate(itemId).getIcon();
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
