package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.event.Events;

public class AdminEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_event_start",
		"admin_event_stop"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		String eventName = "";
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		if (st.hasMoreTokens())
			eventName = st.nextToken();
		
		if (command.startsWith("admin_event_start"))
		{
			sendFile(player, "events/default.htm");
			try
			{
				if (eventName != null)
				{
					Events event = (Events) ScriptData.getInstance().getQuest(eventName);
					if (event != null)
					{
						if (event.eventStart(1))
						{
							player.sendMessage(player.getSysString(10_116, eventName));
							return;
						}
						
						player.sendMessage(player.getSysString(10_117, eventName));
						return;
					}
				}
			}
			catch (Exception e)
			{
				player.sendMessage(player.getSysString(10_120, ": //event_start <eventname>"));
				sendFile(player, "events/default.htm");
			}
		}
		else if (command.startsWith("admin_event_stop"))
		{
			try
			{
				sendFile(player, "events/default.htm");
				if (eventName != null)
				{
					Events event = (Events) ScriptData.getInstance().getQuest(eventName);
					if (event != null)
					{
						if (event.eventStop())
						{
							player.sendMessage(player.getSysString(10_118, eventName));
							return;
						}
						player.sendMessage(player.getSysString(10_119, eventName));
						return;
					}
				}
			}
			catch (Exception e)
			{
				player.sendMessage(player.getSysString(10_120, ": //event_start <eventname>"));
				sendFile(player, "events/default.htm");
			}
		}
		
		return;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}