package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatShout implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.SHOUT
	};
	
	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		if (!player.getClient().performAction(FloodProtector.GLOBAL_CHAT))
			return;
		
		final CreatureSay cs = new CreatureSay(player, type, text);
		if (Config.GLOBAL_CHAT.equalsIgnoreCase("global") || (Config.GLOBAL_CHAT.equalsIgnoreCase("gm") && player.isGM()))
		{
			for (Player players : World.getInstance().getPlayers())
			{
				if (!players.isBlockingAll())
					players.sendPacket(cs);
			}
		}
		else if (Config.GLOBAL_CHAT.equalsIgnoreCase("on"))
			World.broadcastToSameRegion(player, new CreatureSay(player, type, text));
	}
	
	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}