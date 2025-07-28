package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTManager;

public class AdminTvTEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_tvt_add",
		"admin_tvt_remove",
		"admin_tvt_advance"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_tvt_add"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			add(player, plr);
		}
		else if (command.startsWith("admin_tvt_remove"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			remove(player, plr);
		}
		else if (command.startsWith("admin_tvt_advance"))
			TvTManager.getInstance().skipDelay();
		
		return;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void add(Player activeChar, Player player)
	{
		if (TvTEvent.getInstance().isPlayerParticipant(player.getObjectId()))
		{
			activeChar.sendMessage(player.getSysString(10_090));
			return;
		}
		
		if (!TvTEvent.getInstance().addParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_091));
			return;
		}
		
		if (TvTEvent.getInstance().isStarted())
			new TvTEventTeleporter(player, TvTEvent.getInstance().getParticipantTeamCoordinates(player.getObjectId()), true, false);
	}
	
	private void remove(Player activeChar, Player player)
	{
		if (!TvTEvent.getInstance().removeParticipant(player.getObjectId()))
		{
			activeChar.sendMessage(player.getSysString(10_092));
			return;
		}
		
		new TvTEventTeleporter(player, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}