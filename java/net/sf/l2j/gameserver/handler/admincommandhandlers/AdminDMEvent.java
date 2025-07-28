package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMManager;

public class AdminDMEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_dm_add",
		"admin_dm_remove",
		"admin_dm_advance"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_dm_add"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			add(player, plr);
		}
		else if (command.startsWith("admin_dm_remove"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			remove(player, plr);
		}
		else if (command.startsWith("admin_dm_advance"))
			DMManager.getInstance().skipDelay();
		
		return;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void add(Player activeChar, Player player)
	{
		if (DMEvent.getInstance().isPlayerParticipant(player.getObjectId()))
		{
			activeChar.sendMessage(player.getSysString(10_090));
			return;
		}
		
		if (!DMEvent.getInstance().addParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_091));
			return;
		}
		
		if (DMEvent.getInstance().isStarted())
			new DMEventTeleporter(player, true, false);
	}
	
	private void remove(Player activeChar, Player player)
	{
		if (!DMEvent.getInstance().removeParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_092));
			return;
		}
		
		new DMEventTeleporter(player, Config.DM_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}