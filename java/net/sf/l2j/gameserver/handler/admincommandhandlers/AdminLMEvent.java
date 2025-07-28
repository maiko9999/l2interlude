package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEventTeleporter;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMManager;

public class AdminLMEvent implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_lm_add",
		"admin_lm_remove",
		"admin_lm_advance"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.equals("admin_lm_add"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			add(player, plr);
		}
		else if (command.equals("admin_lm_remove"))
		{
			WorldObject target = player.getTarget();
			
			if (!(target instanceof Player plr))
			{
				player.sendMessage(player.getSysString(10_089));
				return;
			}
			
			remove(player, plr);
		}
		else if (command.equals("admin_lm_advance"))
			LMManager.getInstance().skipDelay();
		
		return;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void add(Player activeChar, Player player)
	{
		if (LMEvent.getInstance().isPlayerParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_090));
			return;
		}
		
		if (!LMEvent.getInstance().addParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_091));
			return;
		}
		
		if (LMEvent.getInstance().isStarted())
			new LMEventTeleporter(player, true, false);
	}
	
	private static void remove(Player activeChar, Player player)
	{
		if (!LMEvent.getInstance().removeParticipant(player))
		{
			activeChar.sendMessage(player.getSysString(10_092));
			return;
		}
		
		new LMEventTeleporter(player, Config.LM_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
}