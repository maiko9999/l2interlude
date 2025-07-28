package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.util.LinTime;

import net.sf.l2j.gameserver.data.manager.CastleManorManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class AdminTest implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_test",
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (!st.hasMoreTokens())
		{
			player.sendMessage("Usage : //test setquest || ssq_change || manor_change || dt_set(add, reset, print)");
			return;
		}
		
		switch (st.nextToken())
		{
			case "setquest":
				try
				{
					WorldObject targetObject = getTarget(WorldObject.class, player, true);
					
					if (targetObject == null)
					{
						player.sendPacket(SystemMessageId.INVALID_TARGET);
						return;
					}
					
					if (targetObject instanceof Player targetPlayer)
					{
						if (st.hasMoreTokens())
						{
							final int questId = Integer.parseInt(st.nextToken());
							if (st.hasMoreTokens())
							{
								final int cond = Integer.parseInt(st.nextToken());
								final Quest quest = ScriptData.getInstance().getQuest(questId);
								
								if (quest == null)
								{
									player.sendMessage("Quest with id: " + questId + " not found");
									return;
								}
								
								final QuestState qs = quest.getQuestState(targetPlayer, cond == 0 || cond == 1);
								
								if (qs == null)
								{
									player.sendMessage("Cannot initialize new quest state with cond " + cond + " for player " + targetPlayer.getName() + ". To initialize new quest state, use cond 0.");
									return;
								}
								
								if (cond > 0)
								{
									qs.setState(QuestStatus.STARTED);
									qs.setCond(cond);
									player.sendMessage(targetPlayer.getName() + "'s " + quest.getName() + " quest condition set to " + cond);
								}
								else
									player.sendMessage(targetPlayer.getName() + "'s " + quest.getName() + " quest has been created. To start it, use //test setquest " + questId + " 1");
							}
							else
								player.sendMessage("Invalid command format. Use //test setquest <questId> <cond>");
						}
						else
							player.sendMessage("Invalid command format. Use //test setquest <questId> <cond>");
					}
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("Invalid command format. Use //test setquest <questId> <cond>");
				}
				break;
			
			case "ssq_change":
				SevenSignsManager.getInstance().changePeriod();
				break;
			
			case "manor_change":
				CastleManorManager.getInstance().changeMode();
				break;
			
			case "dt_set":
			{
				final long time = parseTime(st.nextToken());
				LinTime.setDeltaTime(time);
				player.sendMessage("+ set dt " + time);
				break;
			}
			
			case "dt_add":
			{
				final long time = parseTime(st.nextToken());
				LinTime.addDeltaTime(time);
				player.sendMessage("+ add dt " + time);
				break;
			}
			
			case "dt_reset":
				LinTime.resetDeltaTime();
				player.sendMessage("+ reset dt ");
				break;
			
			case "dt_print":
				final String name = st.hasMoreTokens() ? st.nextToken() : "";
				final TimeUnit tu = switch (name)
				{
					case "min" -> TimeUnit.MINUTES;
					case "hour" -> TimeUnit.HOURS;
					case "day" -> TimeUnit.DAYS;
					case "time" -> TimeUnit.NANOSECONDS;
					case "sec" -> TimeUnit.SECONDS;
					default -> TimeUnit.MILLISECONDS;
				};
				
				if (tu == TimeUnit.NANOSECONDS)
				{
					Date date = new Date(LinTime.currentTimeMillis());
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					player.sendMessage("+ dt: " + sdf.format(date));
				}
				else
					player.sendMessage("+ dt: %s %s".formatted(tu.convert(LinTime.deltaTime(), TimeUnit.MILLISECONDS), name));
				
				break;
				
			default:
				player.sendMessage("Usage : //test setquest || ssq_change || manor_change || dt_set(add, reset, print)");
				break;
		}
	}
	
	public static long parseTime(String input)
	{
		if (input.endsWith("sec"))
			return TimeUnit.SECONDS.toMillis(Long.parseLong(input.substring(0, input.length() - 3)));
		
		if (input.endsWith("min"))
			return TimeUnit.MINUTES.toMillis(Long.parseLong(input.substring(0, input.length() - 3)));
		
		if (input.endsWith("hour"))
			return TimeUnit.HOURS.toMillis(Long.parseLong(input.substring(0, input.length() - 4)));
		
		if (input.endsWith("day"))
			return TimeUnit.DAYS.toMillis(Long.parseLong(input.substring(0, input.length() - 3)));
		
		return Long.parseLong(input);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}