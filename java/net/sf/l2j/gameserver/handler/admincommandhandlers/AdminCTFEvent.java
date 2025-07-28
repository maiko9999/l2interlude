package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFManager;

public class AdminCTFEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ctf_add",
		"admin_ctf_remove",
		"admin_ctf_advance"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.startsWith("admin_ctf_add"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			add(player, plr);
		}
		else if (command.startsWith("admin_ctf_remove"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			remove(player, plr);
		}
		else if (command.startsWith("admin_ctf_advance"))
			CTFManager.getInstance().skipDelay();
		
		return;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void add(Player activeChar, Player player)
	{
		if (CTFEvent.getInstance().isPlayerParticipant(player.getObjectId()))
		{
			activeChar.sendMessage(player.getSysString(10_090));
			return;
		}
		
		if (!CTFEvent.getInstance().addParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_091));
			return;
		}
		
		if (CTFEvent.getInstance().isStarted())
			new CTFEventTeleporter(player, CTFEvent.getInstance().getParticipantTeamCoordinates(player.getObjectId()), true, false);
	}
	
	private void remove(Player activeChar, Player player)
	{
		if (!CTFEvent.getInstance().removeParticipant(player.getObjectId()))
		{
			activeChar.sendMessage(player.getSysString(10_092));
			return;
		}
		
		new CTFEventTeleporter(player, Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}