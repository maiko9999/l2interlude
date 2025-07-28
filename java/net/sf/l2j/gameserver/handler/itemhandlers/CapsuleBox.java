package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.custom.data.CapsuleBoxData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.records.custom.CapsuleBoxItem;
import net.sf.l2j.gameserver.model.records.custom.Item;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class CapsuleBox implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player player))
			return;
		
		final int itemId = item.getItemId();
		
		CapsuleBoxItem capsuleBoxItem = CapsuleBoxData.getInstance().getCapsuleBoxItemById(itemId);
		ItemInstance boxInst = null;
		
		if (capsuleBoxItem != null)
		{
			if (player.getStatus().getLevel() <= capsuleBoxItem.playerLevel())
			{
				player.sendMessage(player.getSysString(10_077, capsuleBoxItem.playerLevel()));
				return;
			}
			
			for (Item boxItem : capsuleBoxItem.items())
			{
				boxInst = new ItemInstance(IdFactory.getInstance().getNextId(), boxItem.itemId());
				
				if (Rnd.get(100) < boxItem.chance())
				{
					if (!boxInst.isStackable())
					{
						boxInst.setEnchantLevel(boxItem.enchantLevel(), null);
						player.addItem(boxInst, true);
					}
					else
						player.addItem(boxItem.itemId(), getRandomAmount(boxItem.min(), boxItem.max()), true);
				}
				
				player.broadcastPacket(new MagicSkillUse(player, player, 2024, 1, 1, 0));
			}
		}
		else
			player.sendMessage("This Capsule box expired or is invalid!");
		
		playable.destroyItem(item.getObjectId(), 1, true);
	}
	
	private static int getRandomAmount(int min, int max)
	{
		return min + (int) (Math.random() * ((max - min) + 1));
	}
}