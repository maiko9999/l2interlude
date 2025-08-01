package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.sql.OfflineTradersTable;
import net.sf.l2j.gameserver.data.xml.RecipeData;
import net.sf.l2j.gameserver.enums.actors.OperateType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.craft.ManufactureList;
import net.sf.l2j.gameserver.model.records.ManufactureItem;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopMsg;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestRecipeShopListSet extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 8;
	
	private ManufactureItem[] _items;
	
	@Override
	protected void readImpl()
	{
		int count = readD();
		if (count < 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			count = 0;
		
		_items = new ManufactureItem[count];
		
		for (int i = 0; i < count; i++)
		{
			final int recipeId = readD();
			final int cost = readD();
			
			_items[i] = new ManufactureItem(recipeId, cost, RecipeData.getInstance().getRecipeList(recipeId).isDwarven());
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		// Integrity check.
		if (ArraysUtil.isEmpty(_items))
		{
			player.setOperateType(OperateType.MANUFACTURE_MANAGE);
			player.sendPacket(new RecipeShopManageList(player, player.getManufactureList().isDwarven()));
			player.sendPacket(SystemMessageId.NO_RECIPES_REGISTERED);
			return;
		}
		
		// Integrity check.
		if (_items.length > 20)
		{
			player.setOperateType(OperateType.MANUFACTURE_MANAGE);
			player.sendPacket(new RecipeShopManageList(player, player.getManufactureList().isDwarven()));
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(20));
			return;
		}
		
		// Integrity check.
		if (!player.getRecipeBook().canPassManufactureProcess(_items))
		{
			player.setOperateType(OperateType.MANUFACTURE_MANAGE);
			player.sendPacket(new RecipeShopManageList(player, player.getManufactureList().isDwarven()));
			return;
		}
		
		// Check multiple conditions. Message and OperateType reset are sent directly from the method.
		if (!player.canOpenPrivateStore(false))
		{
			player.setOperateType(OperateType.MANUFACTURE_MANAGE);
			player.sendPacket(new RecipeShopManageList(player, player.getManufactureList().isDwarven()));
			return;
		}
		
		// Retrieve and clear the manufacture list.
		final ManufactureList manufactureList = player.getManufactureList();
		manufactureList.clear();
		
		// Feed it with packet informations.
		manufactureList.set(_items);
		
		if (Config.RESTORE_STORE_ITEMS)
			player.saveTradeList();
		player.getMove().stop();
		player.sitDown();
		player.setOperateType(OperateType.MANUFACTURE);
		player.broadcastUserInfo();
		player.broadcastPacket(new RecipeShopMsg(player));
		OfflineTradersTable.getInstance().saveOfflineTraders(player);
	}
}