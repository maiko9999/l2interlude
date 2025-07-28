package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.custom.ShopBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Fisherman;
import net.sf.l2j.gameserver.model.actor.instance.MercenaryManagerNpc;
import net.sf.l2j.gameserver.model.actor.instance.Merchant;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public final class RequestSellItem extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private int _listId;
	private IntIntHolder[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		
		int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new IntIntHolder[count];
		for (int i = 0; i < count; i++)
		{
			int objectId = readD();
			int itemId = readD();
			int cnt = readD();
			
			if (objectId < 1 || itemId < 1 || cnt < 1)
			{
				_items = null;
				return;
			}
			
			_items[i] = new IntIntHolder(objectId, cnt);
		}
	}
	
	public void sellBbs(Player player)
	{
		long totalPrice = 0;
		
		// Proceed the sell
		for (IntIntHolder i : _items)
		{
			ItemInstance item = player.checkItemManipulation(i.getId(), i.getValue());
			if (item == null || (!item.isSellable()))
				continue;
			
			int price = item.getReferencePrice() / 2;
			totalPrice += price * i.getValue();
			
			if ((Integer.MAX_VALUE / i.getValue()) < price || totalPrice > Integer.MAX_VALUE)
				return;
			
			if ((Integer.MAX_VALUE - player.getInventory().getAdena() - totalPrice) < 0)
			{
				player.sendPacket(SystemMessageId.SELL_ATTEMPT_FAILED);
				return;
			}
			
			player.getInventory().destroyItem(i.getId(), i.getValue());
		}
		
		player.addAdena((int) totalPrice, true);
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (_listId == ShopBBSManager.BBS_SELL_LIST_ID)
		{
			sellBbs(player);
			return;
		}
		
		final Npc merchant = (player.getTarget() instanceof Merchant || player.getTarget() instanceof MercenaryManagerNpc) ? (Npc) player.getTarget() : null;
		if (merchant == null || !player.getAI().canDoInteract(merchant))
			return;
		
		if (_listId > 1000000 && merchant.getTemplate().getNpcId() != _listId - 1000000)
			return;
		
		long totalPrice = 0;
		
		// Proceed the sell
		for (IntIntHolder i : _items)
		{
			ItemInstance item = player.checkItemManipulation(i.getId(), i.getValue());
			if (item == null || (!item.isSellable()))
				continue;
			
			int price = item.getReferencePrice() / 2;
			totalPrice += price * i.getValue();
			
			if ((Integer.MAX_VALUE / i.getValue()) < price || totalPrice > Integer.MAX_VALUE)
				return;
			
			if ((Integer.MAX_VALUE - player.getInventory().getAdena() - totalPrice) < 0)
			{
				player.sendPacket(SystemMessageId.SELL_ATTEMPT_FAILED);
				return;
			}
			
			player.getInventory().destroyItem(i.getId(), i.getValue());
		}
		
		player.addAdena((int) totalPrice, false);
		
		// Send the htm, if existing.
		String htmlFolder = "";
		if (merchant instanceof Fisherman)
			htmlFolder = "fisherman";
		else if (merchant instanceof Merchant)
			htmlFolder = "merchant";
		
		if (!htmlFolder.isEmpty())
		{
			if (HTMLData.getInstance().exists(player, "html/" + htmlFolder + "/" + merchant.getNpcId() + "-sold.htm"))
			{
				final String content = HTMLData.getInstance().getHtm(player, "html/" + htmlFolder + "/" + merchant.getNpcId() + "-sold.htm");
				final NpcHtmlMessage html = new NpcHtmlMessage(merchant.getObjectId());
				html.setHtml(content);
				html.replace("%objectId%", merchant.getObjectId());
				player.sendPacket(html);
			}
		}
	}
}