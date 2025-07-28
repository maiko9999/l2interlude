package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.PrivilegeType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ClanWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class SendWarehouseWithdrawList extends L2GameClientPacket
{
	private static final Logger ITEM_LOG = Logger.getLogger("item");
	
	private static final int BATCH_LENGTH = 8; // length of one item
	
	private IntIntHolder[] _items = null;
	
	@Override
	protected void readImpl()
	{
		final int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new IntIntHolder[count];
		for (int i = 0; i < count; i++)
		{
			int objId = readD();
			int cnt = readD();
			
			if (objId < 1 || cnt < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new IntIntHolder(objId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}
		
		player.cancelActiveEnchant();
		
		final ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;
		
		final Folk folk = player.getCurrentFolk();
		if (folk == null || !folk.isWarehouse() || !player.getAI().canDoInteract(folk))
			return;
		
		if (!(warehouse instanceof PcWarehouse) && !player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0)
			return;
		
		if (Config.MEMBERS_CAN_WITHDRAW_FROM_CLANWH)
		{
			if (warehouse instanceof ClanWarehouse && !player.hasClanPrivileges(PrivilegeType.SP_WAREHOUSE_SEARCH))
				return;
		}
		else if (warehouse instanceof ClanWarehouse && !player.isClanLeader())
			return;
		
		int weight = 0;
		int slots = 0;
		
		for (IntIntHolder i : _items)
		{
			// Calculate needed slots
			ItemInstance item = warehouse.getItemByObjectId(i.getId());
			if (item == null || item.getCount() < i.getValue())
				return;
			
			if (item.getItemId() == 57 && (Integer.MAX_VALUE - player.getInventory().getAdena() - i.getValue()) < 0)
				return;
			
			weight += i.getValue() * item.getItem().getWeight();
			
			if (!item.isStackable())
				slots += i.getValue();
			else if (player.getInventory().getItemByItemId(item.getItemId()) == null)
				slots++;
		}
		
		// Item Max Limit Check
		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
		
		// Weight limit Check
		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}
		
		// Proceed to the transfer
		for (IntIntHolder i : _items)
		{
			ItemInstance oldItem = warehouse.getItemByObjectId(i.getId());
			if (oldItem == null || oldItem.getCount() < i.getValue())
				continue;
			
			// Only log worthy items.
			if (Config.LOG_ITEMS)
			{
				final LogRecord logRecord = new LogRecord(Level.INFO, "WITHDRAW_WAREHOUSE");
				logRecord.setLoggerName("item");
				logRecord.setParameters(new Object[]
				{
					player,
					oldItem
				});
				ITEM_LOG.log(logRecord);
			}
			
			warehouse.transferItem(i.getId(), i.getValue(), player);
		}
	}
}