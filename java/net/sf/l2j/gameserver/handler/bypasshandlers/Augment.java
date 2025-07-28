package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;

public class Augment implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Augment"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		final int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
		switch (cmdChoice)
		{
			case 1:
				player.sendPacket(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED);
				player.sendPacket(ExShowVariationMakeWindow.STATIC_PACKET);
				break;
			case 2:
				player.sendPacket(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
				player.sendPacket(ExShowVariationCancelWindow.STATIC_PACKET);
				break;
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
