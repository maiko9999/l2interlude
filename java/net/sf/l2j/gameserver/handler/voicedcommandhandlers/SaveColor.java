package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class SaveColor implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"savecolor",
		"namecolor",
		"titlecolor"
	
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String args)
	{
		
		if (command.startsWith("namecolor"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			String nameColor = String.valueOf(st.nextToken());
			
			if (player.getPremiumService() <= 0)
			{
				player.sendMessage("Apenas VIPs podem personalizar as cores.");
				return false;
			}
			
			if (nameColor == null || nameColor.isEmpty())
			{
				player.sendMessage("Informe uma cor hexadecimal válida, ex: FF0000");
				return false;
			}
			
			player.getMemos().set("name_color", nameColor);
			
			player.getAppearance().setNameColor(Integer.parseInt(nameColor, 16));
			
			player.broadcastUserInfo();
			player.sendMessage("Cores aplicadas com sucesso!");
			
		}
		
		if (command.startsWith("titlecolor"))
		{
			if (player.getPremiumService() <= 0)
			{
				player.sendMessage("Apenas VIPs podem personalizar as cores.");
				return false;
			}
			
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			String titleColor = String.valueOf(st.nextToken());
			
			if (titleColor == null || titleColor.isEmpty())
			{
				player.sendMessage("Informe uma cor hexadecimal válida, ex: FF0000");
				return false;
			}
			
			player.getMemos().set("title_color", titleColor);
			
			player.getAppearance().setTitleColor(Integer.parseInt(titleColor, 16));
			player.broadcastUserInfo();
			
			player.sendMessage("Cores aplicadas com sucesso!");
			
		}
		
		if (command.startsWith("savecolor"))
		{
			if (player.getPremiumService() <= 0)
			{
				player.sendMessage("Apenas VIPs podem personalizar as cores.");
				return false;
			}
			
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			String titleColor = String.valueOf(st.nextToken());
			String nameColor = String.valueOf(st.nextToken());
			
			if (!nameColor.matches("[0-9A-Fa-f]{6}") || !titleColor.matches("[0-9A-Fa-f]{6}"))
			{
				player.sendMessage("Use códigos hexadecimais válidos, ex: FF0000.");
				return false;
			}
			
			// Salva no Memo
			player.getMemos().set("name_color", nameColor);
			player.getMemos().set("title_color", titleColor);
			
			// Aplica na hora
			player.getAppearance().setNameColor(Integer.parseInt(nameColor, 16));
			player.getAppearance().setTitleColor(Integer.parseInt(titleColor, 16));
			player.broadcastUserInfo();
			
			player.sendMessage("Cores aplicadas com sucesso!");
			
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
