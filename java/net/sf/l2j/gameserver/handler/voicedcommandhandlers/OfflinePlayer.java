package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.sql.OfflineTradersTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.trade.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

public class OfflinePlayer implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"offline"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.OFFLINE_TRADE_ENABLE)
		{
			player.sendMessage(player.getSysString(10_200));
			return false;
		}
		
		if (player == null)
			return false;
		
		if ((!player.isInStoreMode() && (!player.isCrafting())) || !player.isSitting())
		{
			player.sendMessage(player.getSysString(10_080));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		final TradeList storeListBuy = player.getBuyList();
		if (storeListBuy == null)
		{
			player.sendMessage(player.getSysString(10_081));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		final TradeList storeListSell = player.getSellList();
		if (storeListSell == null)
		{
			player.sendMessage(player.getSysString(10_082));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (AttackStanceTaskManager.getInstance().isInAttackStance(player))
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Dont allow leaving if player is in combat
		if (player.isInCombat() && !player.isGM())
		{
			player.sendMessage(player.getSysString(10_083));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Dont allow leaving if player is teleporting
		if (player.isTeleporting() && !player.isGM())
		{
			player.sendMessage(player.getSysString(10_084));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage(player.getSysString(10_085));
			return false;
		}
		
		// Prevent player from logging out if they are a festival participant nd it is in progress, otherwise notify party members that the player is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (FestivalOfDarknessManager.getInstance().isFestivalInitialized())
			{
				player.sendMessage(player.getSysString(10_086));
				return false;
			}
			
			Party playerParty = player.getParty();
			if (playerParty != null)
				player.getParty().broadcastToPartyMembers(player, SystemMessage.sendString(player.getSysString(10_173, player.getName())));
		}
		
		if (!OfflineTradersTable.offlineMode(player))
		{
			player.sendMessage(player.getSysString(10_087));
			return false;
		}
		
		if (player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE || player.isCrafting() && Config.OFFLINE_CRAFT_ENABLE)
		{
			player.logout(false);
			return true;
		}
		
		OfflineTradersTable.getInstance().saveOfflineTraders(player);
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}