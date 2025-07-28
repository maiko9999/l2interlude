package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import ext.mods.tour.TourData;
import ext.mods.tour.TournamentEvent;
import ext.mods.tour.holder.TourConfig;
import ext.mods.util.Tokenizer;

public class VoicedTour implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"tour",
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		
		if (command.startsWith("tour"))
		{
			final Tokenizer tokenizer = new Tokenizer(command);
			
			if (tokenizer.size() == 1)
			{
				navi(player);
				return true;
			}
			
			final String param = tokenizer.getToken(1);
			
			if (param == null)
			{
				navi(player);
				return true;
			}
			
			switch (param.toLowerCase())
			{
				case "navi":
					
					navi(player);
					break;
				case "register":
					registerToTournament(player);
					break;
				case "unregister":
					unregisterToTournament(player);
					break;
				
			}
			
		}
		
		return true;
	}
	
	public static void navi(Player player)
	{
		String content = """
			<html>
			<title>Battle Manager</title>
			<body>
			<center>
			<font color="LEVEL">Tournament Manager</font>
			<br>
			<img src="L2UI.SquareGray" width=270 height=1><br><br>
			
			<table width=270>
			<tr>
			<td width=150 align=left>Registration:</td>
			<td width=60 align=center><font color="LEVEL">%registed%</font></td>
			<td align=right><button value="Register" action="bypass -h voiced_tour register" width=65 height=16 back="L2UI_ch3.smallbutton2_over" fore="L2UI_ch3.smallbutton2"></td>
			</tr>
			
			<tr>
			<td width=150 align=left>unRegistration:</td>
			<td width=60 align=center><font color="LEVEL">%registed%</font></td>
			<td align=right><button value="Cancel" action="bypass -h voiced_tour unregister" width=65 height=16 back="L2UI_ch3.smallbutton2_over" fore="L2UI_ch3.smallbutton2"></td>
			</tr>
			
			<tr>
			<td width=150 align=left>Ranking:</td>
			<td width=60></td>
			<td align=right><button value="See Ranking" action="bypass -h voiced_tournamentrank" width=65 height=16 back="L2UI_ch3.smallbutton2_over" fore="L2UI_ch3.smallbutton2"></td>
			</tr>
			
			<tr>
			<td width=150 align=left>Reward:</td>
			<td width=60></td>
			<td align=right><button value="See Reward" action="bypass -h voiced_tournamentlistreward" width=65 height=16 back="L2UI_ch3.smallbutton2_over" fore="L2UI_ch3.smallbutton2"></td>
			</tr>
			</table>
			
			<br>
			<img src="L2UI.SquareGray" width=270 height=1><br>
			
			<font color="LEVEL">Tournament Event Info</font>
			<br>
			<img src="L2UI.SquareGray" width=270 height=1><br><br>
			
			<table width=270>
			<tr><td align=left>Is Running:</td><td align=right>%isRunning%</td></tr>
			<tr><td align=left>Registration Open:</td><td align=right>%isRegistering%</td></tr>
			<tr><td align=left>Duration:</td><td align=right>%eventDuration% minutes</td></tr>
			<tr><td align=left>Preparation Time:</td><td align=right>%preparationTime% minutes</td></tr>
			<tr><td align=left>Next Times:</td><td align=right>%nextTimes%</td></tr>
			</table>
			
			<br>
			<img src="L2UI.SquareGray" width=270 height=1><br>
			</center>
			</body>
			</html>
			""";
		
		// Substituições
		content = content.replace("%registed%", player.isInTournament() ? "ON" : "OFF");
		
		TourConfig config = TourData.getInstance().getConfig();
		content = content.replace("%isRunning%", TournamentEvent.isRunning() ? "<font color=LEVEL>Yes</font>" : "No");
		content = content.replace("%isRegistering%", TournamentEvent.isRunningRegister() ? "<font color=LEVEL>Yes</font>" : "No");
		content = content.replace("%eventDuration%", String.valueOf(config.getDuration()));
		content = content.replace("%preparationTime%", String.valueOf(config.getPreparation()));
		content = content.replace("%nextTimes%", String.join(", ", config.getTimes()));
		
		// Envio do HTML ao player
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(content);
		player.sendPacket(html);
	}
	
	private void registerToTournament(Player player)
	{
		if (TournamentEvent.isRunningRegister())
		{
			
			if (player.getParty() != null)
			{
				if (player.getParty() != null && !player.getParty().isLeader(player))
				{
					player.sendMessage("Only the Party Leader can register the party for the Tournament.");
					return;
				}
				
				for (Player member : player.getParty().getMembers())
				{
					if (!member.isInTournament())
					{
						member.setInTournament(true);
						member.sendMessage("You have been registered for the Tournament by your Party Leader!");
					}
					else
					{
						member.sendMessage("You are already registered for the Tournament.");
					}
				}
			}
			else
			{
				
				if (!player.isInTournament())
				{
					player.setInTournament(true);
					player.sendMessage("You have registered for the Tournament!");
				}
				else
				{
					player.sendMessage("You are already registered for the Tournament.");
				}
			}
		}
	}
	
	private void unregisterToTournament(Player player)
	{
		if (TournamentEvent.isRunningRegister())
		{
			if (player.getParty() != null)
			{
				
				if (!player.getParty().isLeader(player))
				{
					player.sendMessage("Only the Party Leader can unregister the party from the Tournament.");
					return;
				}
				
				for (Player member : player.getParty().getMembers())
				{
					if (member.isInTournament())
					{
						member.setInTournament(false);
						member.sendMessage("Your Party Leader has unregistered you from the Tournament.");
					}
					else
					{
						member.sendMessage("You were not registered for the Tournament.");
					}
				}
			}
			else
			{
				
				if (player.isInTournament())
				{
					player.setInTournament(false);
					player.sendMessage("You have unregistered from the Tournament.");
				}
				else
				{
					player.sendMessage("You were not registered for the Tournament.");
				}
			}
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
