package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.ADENA_ID;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class SendWarehouseDepositList extends L2GameClientPacket
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
		
		final boolean isPrivate = warehouse instanceof PcWarehouse;
		
		final Folk folk = player.getCurrentFolk();
		if (folk == null || !folk.isWarehouse() || !player.getAI().canDoInteract(folk))
			return;
		
		if (!isPrivate && !player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0)
			return;
		
		// Freight price from config or normal price per item slot (30)
		final int fee = _items.length * 30;
		int currentAdena = player.getAdena();
		int slots = 0;
		
		for (IntIntHolder i : _items)
		{
			ItemInstance item = player.checkItemManipulation(i.getId(), i.getValue());
			if (item == null)
				return;
			
			if (item.getItemId() == ADENA_ID)
			{
				if (isPrivate)
				{
					if (Integer.MAX_VALUE - player.getWarehouse().getAdena() - i.getValue() < 0)
					{
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
						return;
					}
				}
				else if (player.getClan() == null || Integer.MAX_VALUE - player.getClan().getWarehouse().getAdena() - i.getValue() < 0)
				{
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
					return;
				}
			}
			
			// Calculate needed adena and slots
			if (item.getItemId() == ADENA_ID)
				currentAdena -= i.getValue();
			
			if (!item.isStackable())
				slots += i.getValue();
			else if (warehouse.getItemByItemId(item.getItemId()) == null)
				slots++;
		}
		
		// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}
		
		// Check if enough adena and charge the fee
		if (currentAdena < fee || !player.reduceAdena(fee, false))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}
		
		// get current tradelist if any
		if (player.getActiveTradeList() != null)
			return;
		
		// Proceed to the transfer
		for (IntIntHolder i : _items)
		{
			// Check validity of requested item
			ItemInstance oldItem = player.checkItemManipulation(i.getId(), i.getValue());
			if (oldItem == null)
				return;
			
			if (!oldItem.isDepositable(isPrivate) || !oldItem.isAvailable(player, true, isPrivate, false))
				continue;
			
			// Only log worthy items.
			if (Config.LOG_ITEMS)
			{
				final LogRecord logRecord = new LogRecord(Level.INFO, "DEPOSIT_WEREHOUSE");
				logRecord.setLoggerName("item");
				logRecord.setParameters(new Object[]
				{
					player,
					oldItem
				});
				ITEM_LOG.log(logRecord);
			}
			
			player.getInventory().transferItem(i.getId(), i.getValue(), warehouse);
		}
	}
}