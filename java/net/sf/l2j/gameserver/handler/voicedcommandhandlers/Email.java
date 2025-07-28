package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.List;
import java.util.Map;

import net.sf.l2j.gameserver.data.xml.AugmentationData;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

import ext.mods.email.PlayerEmailManager;
import ext.mods.email.items.EmailItemSelector;
import ext.mods.email.items.EmailItemSender;
import ext.mods.util.Tokenizer;

public class Email implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"email"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.startsWith("email"))
		{
			final Tokenizer tokenizer = new Tokenizer(command);
			final String param = tokenizer.getToken(1);
			if (param == null)
			{
				PlayerEmailManager.getInstance().navi(player, "email_main.htm");
				return true;
			}
			
			switch (param.toLowerCase())
			{
				case "htm":
					PlayerEmailManager.getInstance().handlerHtm(player, tokenizer);
					break;
				
				case "target":
					String targetName = tokenizer.getToken(2);
					player.setSelectedEmailTarget(targetName);
					PlayerEmailManager.getInstance().navi(player, "email_main.htm");
					break;
				
				case "clean":
					player.setSelectedEmailTarget(null);
					PlayerEmailManager.getInstance().navi(player, "email_main.htm");
					break;
				
				case "select_items":
					int page = 1;
					try
					{
						page = tokenizer.getAsInteger(2, 0);
					}
					catch (Exception e)
					{
						// default to page 1
					}
					EmailItemSelector.showAvailableItems(player, page);
					break;
				
				case "search":
					String searchtargetName = tokenizer.getToken(2);
					
					if (searchtargetName == null)
					{
						player.sendMessage("Parâmetros incompletos.");
						PlayerEmailManager.getInstance().navi(player, "email_main.htm");
						return true;
					}
					
					PlayerEmailManager.getInstance().searchPlayer(player, searchtargetName);
					break;
				
				case "finalize_item":
					int objectIdFinal = tokenizer.getAsInteger(2, 0);
					long qtd = tokenizer.getAsLong(3, 0); // parse da quantidade
					ItemInstance finalItem = player.getInventory().getItemByObjectId(objectIdFinal);
					
					if (finalItem == null || qtd < 1 || qtd > finalItem.getCount())
					{
						player.sendMessage("Quantidade inválida.");
						useVoicedCommand("email confirm_select_item " + objectIdFinal, player);
						return true;
					}
					
					player.sendMessage("Selecionado " + qtd + "x " + finalItem.getName());
					player.addTempSelectedItem(objectIdFinal, qtd);
					EmailItemSelector.showAvailableItems(player, 1);
					
					break;
				
				case "confirm_select_item":
					int objId = tokenizer.getAsInteger(2, 0);
					
					ItemInstance selectedItem = player.getInventory().getItemByObjectId(objId);
					if (selectedItem == null)
						return true;
					
					String icon = selectedItem.getItem().getIcon(); // pega o ícone real se tiver
					if (icon == null || icon.isEmpty())
						icon = "icon.noimage"; // fallback padrão
						
					NpcHtmlMessage html = new NpcHtmlMessage(0);
					StringBuilder sb = new StringBuilder();
					
					Map<Integer, Long> selectedIds = player.getTempSelectedItems();
					long selectedAmount = selectedIds.getOrDefault(selectedItem.getObjectId(), 0L);
					boolean isSelected = selectedAmount > 0;
					
					sb.append("<html><title>Email Items</title><body><center>");
					sb.append("<table width=300 border=0 bgcolor=000000 cellpadding=5>");
					sb.append("<tr>");
					sb.append("<td width=40><img src=\"" + icon + "\" width=32 height=32></td>");
					sb.append("<td width=200><font color=LEVEL>" + selectedItem.getName() + "</font><br1>");
					sb.append("Quantidade disponível: " + selectedItem.getCount() + "<br1>");
					
					if (isSelected)
					{
						sb.append("<font color=\"A9F5F2\">Selecionado: " + selectedAmount + "</font>");
					}
					
					sb.append("</td>");
					sb.append("</tr>");
					sb.append("</table>");
					
					if (selectedItem.getAugmentation() != null)
					{
						int augId = selectedItem.getAugmentation().getId();
						
						List<AugmentationData.AugStat> stats = AugmentationData.getInstance().getAugStatsById(augId);
						if (!stats.isEmpty())
						{
							sb.append("<br><table width=280 bgcolor=000000>");
							sb.append("<tr><td><font color=LEVEL>Stats do Augment:</font></td></tr>");
							for (AugmentationData.AugStat stat : stats)
							{
								sb.append("<tr><td><font color=FF9900>" + stat.getStat().getName() + "</font>: <font color=LEVEL>+" + stat.getValue() + "</font></td></tr>");
							}
							sb.append("</table>");
						}
						
						L2Skill skill = selectedItem.getAugmentation().getSkill();
						if (skill != null)
						{
							sb.append("<br><table width=280 bgcolor=000000>");
							sb.append("<tr><td><font color=LEVEL>Skill do Augment:</font></td></tr>");
							
							String name = skill.getName();

							String[] ocultarPalavras =
							{
								"Special Ability:",
								"Special",
								"Item Skill:",
							};
							
							for (String palavra : ocultarPalavras)
							{
								name = name.replace(palavra, "").trim();
							}

							sb.append("<tr><td><font color=00FF99>" + name + "</font> (Lv " + skill.getLevel() + ") - " + (skill.isPassive() ? "Passiva" : "Ativa") + "</td></tr>");
							sb.append("</table>");
						}
					}
					if (!isSelected)
					{
						sb.append("<br><table width=300 cellpadding=3><tr>");
						
						sb.append("<td><font color=LEVEL>Qtd:</font></td>");
						sb.append("<td><edit var=\"qtd\" width=85 type=number></td>");
						
						sb.append("<td><button value=\"Confirmar\" action=\"bypass -h voiced_email finalize_item " + selectedItem.getObjectId() + " $qtd\" width=75 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></td>");
						
						sb.append("</tr></table>");
					}
					
					else
					{
						
						sb.append("<br><table><tr>");
						sb.append("<td><button value=\"Remover\" action=\"bypass -h voiced_email remove_selected_item " + selectedItem.getObjectId() + "\" width=75 height=21 back=\"L2UI_CH3.Btn1_normal\" fore=\"L2UI_CH3.Btn1_normal\"></td>");
						sb.append("</tr></table>");
					}
					
					sb.append("<br><button value=\"Voltar\" action=\"bypass -h voiced_email select_items\" width=55 height=15 back=sek.cbui94 fore=sek.cbui92>");
					
					sb.append("</center></body></html>");
					
					html.setHtml(sb.toString());
					player.sendPacket(html);
					break;
				
				case "send":
					EmailItemSender.processSendCommand(player, tokenizer);
					break;
				case "clear_all_items":
					player.clearTempSelectedItems();
					player.sendMessage("Itens selecionados foram limpos.");
					EmailItemSelector.showAvailableItems(player, 1); // ou a página atual
					break;
				
				case "remove_selected_item":
					if (tokenizer.countTokens() >= 2)
					{
						int objectId = tokenizer.getAsInteger(2, 0);
						player.removeTempSelectedItem(objectId);
						player.sendMessage("Item removido da lista.");
						EmailItemSelector.showAvailableItems(player, 1);
					}
					else
					{
						player.sendMessage("ID do item não especificado.");
					}
					break;
				
				case "close":
					PlayerEmailManager.getInstance().navi(player, "email_main.htm");
					break;
				
				default:
					player.sendMessage("Unknown command.");
					break;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}