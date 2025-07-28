package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;

public class AdminOlympiadPoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_addolypoints",
		"admin_removeolypoints",
		"admin_setolypoints",
		"admin_getolypoints"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final Player targetPlayer = getTargetPlayer(player, true);
		
		if (command.startsWith("admin_addolypoints"))
		{
			String val = command.substring(19);
			
			if (targetPlayer != null)
			{
				if (player.isNoble())
				{
					StatSet stat = Olympiad.getInstance().getNobleStats(player.getObjectId());
					if (stat == null)
					{
						player.sendMessage("Oops! This player hasn't played on Olympiad yet!");
						return;
					}
					
					int oldpoints = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					int points = oldpoints + Integer.parseInt(val);
					if (points > 100)
					{
						player.sendMessage("You can't set more than 100 or less than 0 Olympiad points!");
						return;
					}
					
					stat.set("olympiad_points", points);
					player.sendMessage("Player " + player.getName() + " now has " + points + " Olympiad points.");
				}
				else
				{
					player.sendMessage("Oops! This player is not noblesse!");
					return;
				}
			}
			else
			{
				player.sendMessage("Usage: target a player and write the amount of points you would like to add.");
				player.sendMessage("Example: //addolypoints 10");
				player.sendMessage("However, keep in mind that you can't have less than 0 or more than 100 points.");
			}
		}
		else if (command.startsWith("admin_removeolypoints"))
		{
			String val = command.substring(22);
			
			if (targetPlayer != null)
			{
				if (player.isNoble())
				{
					StatSet stat = Olympiad.getInstance().getNobleStats(player.getObjectId());
					if (stat == null)
					{
						player.sendMessage("Oops! This player hasn't played on Olympiad yet!");
						return;
					}
					
					int oldpoints = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					int points = oldpoints - Integer.parseInt(val);
					if (points < 0)
						points = 0;
					
					stat.set("olympiad_points", points);
					player.sendMessage("Player " + player.getName() + " now has " + points + " Olympiad points.");
				}
				else
				{
					player.sendMessage("Oops! This player is not noblesse!");
					return;
				}
			}
			else
			{
				player.sendMessage("Usage: target a player and write the amount of points you would like to remove.");
				player.sendMessage("Example: //removeolypoints 10");
				player.sendMessage("However, keep in mind that you can't have less than 0 or more than 100 points.");
			}
		}
		else if (command.startsWith("admin_setolypoints"))
		{
			String val = command.substring(19);
			if (targetPlayer != null)
			{
				if (player.isNoble())
				{
					StatSet stat = Olympiad.getInstance().getNobleStats(player.getObjectId());
					if (stat == null)
					{
						player.sendMessage("Oops! This player hasn't played on Olympiad yet!");
						return;
					}
					
					if (Integer.parseInt(val) < 1 && Integer.parseInt(val) > 100)
					{
						player.sendMessage("You can't set more than 100 or less than 0 Olympiad points! or lower then 0");
						return;
					}
					
					stat.set("olympiad_points", Integer.parseInt(val));
					player.sendMessage("Player " + player.getName() + " now has " + Integer.parseInt(val) + " Olympiad points.");
				}
				else
				{
					player.sendMessage("Oops! This player is not noblesse!");
					return;
				}
			}
			else
			{
				player.sendMessage("Usage: target a player and write the amount of points you would like to set.");
				player.sendMessage("Example: //setolypoints 10");
				player.sendMessage("However, keep in mind that you can't have less than 0 or more than 100 points.");
			}
		}
		else if (command.startsWith("admin_getolypoints"))
		{
			if (targetPlayer != null)
			{
				if (player.isNoble())
				{
					final StatSet set = Olympiad.getInstance().getNobleStats(player.getObjectId());
					player.sendMessage(">=========>>" + player.getName() + "<<=========");
					if (set == null)
					{
						player.sendMessage("   Match(s):  " + 0);
						player.sendMessage("   Win(s):    " + 0);
						player.sendMessage("   Defeat(s): " + 0);
						player.sendMessage("   Point(s):  " + 0);
					}
					else
					{
						player.sendMessage("   Match(s):  " + set.getInteger(Olympiad.COMP_DONE));
						player.sendMessage("   Win(s):    " + set.getInteger(Olympiad.COMP_WON));
						player.sendMessage("   Defeat(s): " + set.getInteger(Olympiad.COMP_LOST));
						player.sendMessage("   Point(s):  " + set.getInteger(Olympiad.POINTS));
						player.sendMessage(">=========>>" + player.getName() + "<<=========");
					}
				}
				else
				{
					player.sendMessage("Oops! This player is not noblesse!");
					return;
				}
			}
			else
				player.sendMessage("You must target a player to use the command.");
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}