package net.sf.l2j.gameserver.handler.bypasshandlers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.data.DropCalc;
import net.sf.l2j.gameserver.data.manager.DropSkipManager;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.SkipData;
import net.sf.l2j.gameserver.enums.DropType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class DropListUI
{
	private static final DecimalFormat PERCENT = new DecimalFormat("##.##");
	private static final int ITEMS_PER_LIST = 7;
	
	// Ícones inspirados nos do buffer
	private static final String checkon = "RECEIVE";
	private static final String checkoff = "SKIP";

	public static void sendNpcDrop(Player player, int npcId, int page)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
			return;
		
		if (template.getDropData().isEmpty())
		{
			player.sendMessage("This target has no drop information.");
			return;
		}
		
		// 1. Preparar lista de drop filtrada (sem HERB) e ordenar com Adena primeiro
		final List<DropData> dropList = new ArrayList<>();
		for (DropCategory category : template.getDropData())
		{
			if (category.getDropType() == DropType.HERB)
				continue;

			for (DropData drop : category.getAllDrops())
			{
				
				if (SkipData.getInstance().isSkipped(drop.getItemId()))
					continue;
				
				
				drop.setCategoryType(category.getDropType());
				dropList.add(drop);
			}
		}
		
		// Ordenar: Adena primeiro, depois restantes
		dropList.sort((a, b) ->
		{
			if (a.getItemId() == 57)
				return -1;
			if (b.getItemId() == 57)
				return 1;
			return 0;
		});
		
		final StringBuilder sb = new StringBuilder();
		int pageIndex = 1, i = 0, shown = 0;
		boolean hasMore = false;
		
		for (DropData drop : dropList)
		{
			final int itemId = drop.getItemId();
			final Item item = ItemData.getInstance().getTemplate(itemId);
			if (item == null)
				continue;
			
			// Paginação
			if (pageIndex != page)
			{
				i++;
				if (i == ITEMS_PER_LIST)
				{
					pageIndex++;
					i = 0;
				}
				continue;
			}
			
			if (shown == ITEMS_PER_LIST)
			{
				hasMore = true;
				break;
			}
			
			final boolean isSweep = drop.getCategoryType() == DropType.SPOIL;

			final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
			final boolean isRaid = npcTemplate.isType("RaidBoss");
			final boolean isGrand = npcTemplate.isType("GrandBoss");

			final Npc npc = (player.getTarget() instanceof Npc) ? (Npc) player.getTarget() : null;

			if (npc == null || npc.getNpcId() != npcId)
			{
			    player.sendMessage("You must target the correct NPC to view its drop list.");
			    return;
			}
			
			final double chance = DropCalc.getInstance().calcDropChance(player, npc , drop, drop.getCategoryType(), isRaid, isGrand);
			final double safeChance = (chance > 0 && chance < 0.01) ? 0.01 : chance;
			final double normChance = Math.min(99.999, safeChance);
			final String percent = PERCENT.format(normChance);

			
			String itemName = item.getName();
			if (itemName.startsWith("Recipe: "))
				itemName = "R: " + itemName.substring(8);
			if (itemName.length() >= 40)
				itemName = itemName.substring(0, 37) + "...";
			
			final boolean skipped = DropSkipManager.getInstance().isSkipped(player.getObjectId(), itemId);
			
			// Se está ignorado (no banco), mostrar desmarcado
			final String STATUS = skipped ? checkoff : checkon;
			// Geração do botão
			
			final String icon = item.getIcon() != null ? item.getIcon() : "icon.noimage";
			
			sb.append("<table width=280 bgcolor=000000><tr>");
			sb.append("<td width=44 height=41 align=center>");
			sb.append("<table bgcolor=" + (isSweep ? "FF00FF" : "FFFFFF") + " cellpadding=6 cellspacing=\"-5\"><tr><td>");
			sb.append("<button width=32 height=32 back=" + icon + " fore=" + icon + ">");
			sb.append("</td></tr></table></td>");
			
			sb.append("<td width=240>");
			sb.append(isSweep ? "<font color=ff00ff>" + itemName + "</font>" : itemName);
			sb.append("<br1><font color=B09878>" + (isSweep ? "Spoil" : "Drop") + " Chance : " + percent + "%</font>");
			sb.append("</td>");
			
			sb.append("<td width=20>");
			sb.append("<button value=" + STATUS + " action=\"bypass -h droplist " + npcId + " " + page + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>");
			sb.append("</td></tr></table><img src=L2UI.SquareGray width=280 height=1>");
			shown++;
			
		}
		
		// Espaço final
		sb.append("<img height=").append(294 - (shown * 42)).append(">");
		sb.append("<img height=8><img src=L2UI.SquareGray width=280 height=1>");
		
		// Navegação
		sb.append("<table width=280 bgcolor=000000><tr>");
		sb.append("<td align=center width=70>");
		if (page > 1)
			sb.append("<button value=\"< PREV\" action=\"bypass -h droplist " + npcId + " " + (page - 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>");
		sb.append("</td><td align=center width=140>Page ").append(page).append("</td><td align=center width=70>");
		if (hasMore)
			sb.append("<button value=\"NEXT >\" action=\"bypass -h droplist " + npcId + " " + (page + 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>");
		sb.append("</td></tr></table><img src=L2UI.SquareGray width=280 height=1>");
		
		// Enviar
		final NpcHtmlMessage html = new NpcHtmlMessage(200);
		html.setHtml("<html><title>Droplist : " + template.getName() + "</title><body><img height=14><font color=B09878>* NOTE : Uncheck to ignore specific drop.</font><img src=L2UI.SquareGray width=280 height=1>" + sb + "</body></html>");
		player.sendPacket(html);
	}
}
