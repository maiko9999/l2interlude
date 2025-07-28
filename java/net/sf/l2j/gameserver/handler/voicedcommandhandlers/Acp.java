package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.taskmanager.AutoPotionTaskManager;

public class Acp implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"acp",
		"setCp",
		"setHp",
		"setMp",
		"acpEnabled",
		"acpDisabled"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.AUTO_POTIONS_ENABLED)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		if (player.getStatus().getLevel() < Config.AUTO_POTION_MIN_LEVEL)
		{
			player.sendMessage(player.getSysString(10_188, Config.AUTO_POTION_MIN_LEVEL));
			return false;
		}
		
		switch (command)
		{
			case "acp":
				sendAcpHtml(player, "");
				break;
			
			case "acpEnabled":
				AutoPotionTaskManager.getInstance().add(player);
				player.sendMessage(player.getSysString(10_189));
				break;
			
			case "acpDisabled":
				AutoPotionTaskManager.getInstance().remove(player);
				player.sendMessage(player.getSysString(10_190));
				break;
			
			default:
				if (command.startsWith("setCp") && Config.AUTO_CP_ENABLED)
					setAutoPotionValue(command, player, "Cp");
				else if (command.startsWith("setHp") && Config.AUTO_HP_ENABLED)
					setAutoPotionValue(command, player, "Hp");
				else if (command.startsWith("setMp") && Config.AUTO_MP_ENABLED)
					setAutoPotionValue(command, player, "Mp");
				break;
		}
		return true;
	}
	
	private void sendAcpHtml(Player player, String message)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/acp.htm");
		htm.replace("%valueCp%", player.isAcpCp());
		htm.replace("%valueHp%", player.isAcpHp());
		htm.replace("%valueMp%", player.isAcpMp());
		htm.replace("%msg%", message);
		player.sendPacket(htm);
	}
	
	private void setAutoPotionValue(String command, Player player, String type)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/acp.htm");
		
		String valueStr = command.length() > 6 ? command.substring(6).trim() : "";
		String message = "<font color=\"FF0000\">" + player.getSysString(10_191) + "</font>";
		if (!valueStr.isEmpty())
		{
			try
			{
				int value = Integer.parseInt(valueStr);
				if (value < 0 || value > 100)
					message = "<font color=\"FF0000\">" + player.getSysString(10_192) + "</font>";
				else
				{
					switch (type)
					{
						case "Cp":
							player.setAcpCp(value);
							break;
						
						case "Hp":
							player.setAcpHp(value);
							break;
						
						case "Mp":
							player.setAcpMp(value);
							break;
					}
					message = "<font color=\"00FF00\">"+ player.getSysString(10_193) +"</font>";
				}
			}
			catch (NumberFormatException e)
			{
				// ignore invalid input
			}
		}
		
		htm.replace("%msg%", message);
		htm.replace("%valueCp%", player.isAcpCp());
		htm.replace("%valueHp%", player.isAcpHp());
		htm.replace("%valueMp%", player.isAcpMp());
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}