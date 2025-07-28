package net.sf.l2j.gameserver.communitybbs.custom;

import java.util.List;

import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.SellList;

public class ShopBBSManager extends BaseBBSManager
{
	public static final int BBS_SELL_LIST_ID = 9999;
	
	@Override
	public void parseCmd(String command, Player player)
	{
		showPage("index", player);
		
		if (command.equals("_maillist_0_1_0_"))
			showPage("index", player);
		else if (command.startsWith("_maillist_0_1_0_;page"))
		{
			showPage("index", player);
			
			String[] args = command.split(" ");
			if (args.length > 1)
				showPage(args[1], player);
		}
		else if (command.startsWith("_maillist_0_1_0_;crafter"))
		{
			showPage("crafter", player);
			
			String content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "crafter.htm");
			content = content.replace("%name%", player.getName());
			separateAndSend(content, player);
		}
		else if (command.startsWith("_maillist_0_1_0_;sell"))
		{
			final List<ItemInstance> items = player.getInventory().getSellableItems();
			player.sendPacket(new SellList(BBS_SELL_LIST_ID, player.getAdena(), items));
		}
		else if (command.startsWith("_maillist_0_1_0_;augment"))
		{
			
			final int cmdChoice = Integer.parseInt(command.substring(25).trim());
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
		}
	}
	
	private void showPage(String page, Player player)
	{
		String content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + page + ".htm");
		content = content.replace("%name%", player.getName());
		separateAndSend(content, player);
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/shop/";
	}
	
	public static ShopBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ShopBBSManager INSTANCE = new ShopBBSManager();
	}
}