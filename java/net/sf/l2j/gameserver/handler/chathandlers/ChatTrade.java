package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatTrade implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.TRADE
	};
	
	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		if (!player.getClient().performAction(FloodProtector.TRADE_CHAT))
			return;
		
		CreatureSay cs = new CreatureSay(player.getObjectId(), type, player.getName(), text);
		if (Config.TRADE_CHAT.equalsIgnoreCase("global") || (Config.TRADE_CHAT.equalsIgnoreCase("gm") && player.isGM()))
		{
			for (Player worldPlayer : World.getInstance().getPlayers())
			{
				if (!worldPlayer.isBlockingAll())
					worldPlayer.sendPacket(cs);
			}
		}
		else if (Config.TRADE_CHAT.equalsIgnoreCase("on"))
			World.broadcastToSameRegion(player, new CreatureSay(player, type, text));
	}
	
	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}