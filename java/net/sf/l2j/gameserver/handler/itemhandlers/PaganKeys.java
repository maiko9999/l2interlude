package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class PaganKeys implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player player))
			return;
		
		final WorldObject target = player.getTarget();
		
		if (!(target instanceof Door targetDoor))
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!player.isIn3DRadius(targetDoor, Npc.INTERACTION_DISTANCE))
		{
			player.sendPacket(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// FIXME: new FloodProtector.
		if (!player.getClient().performAction(FloodProtector.ROLL_DICE))
			return;
		
		if (!playable.destroyItem(item.getObjectId(), 1, true))
			return;
		
		final int doorId = targetDoor.getDoorId();
		
		switch (item.getItemId())
		{
			case 8056:
				if (doorId == 23150004 || doorId == 23150003)
				{
					DoorData.getInstance().getDoor(23150003).openMe();
					DoorData.getInstance().getDoor(23150004).openMe();
				}
				else
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(8056));
				break;
			
			case 8273:
				switch (doorId)
				{
					case 19160002, 19160003, 19160004, 19160005, 19160006, 19160007, 19160008, 19160009:
						DoorData.getInstance().getDoor(doorId).openMe();
						break;
					
					default:
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(8273));
						break;
				}
				break;
			
			case 8274:
				switch (doorId)
				{
					case 19160010, 19160011:
						DoorData.getInstance().getDoor(doorId).openMe();
						break;
					
					default:
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(8275));
						break;
				}
				break;
			
			case 8275:
				switch (doorId)
				{
					case 19160012, 19160013:
						DoorData.getInstance().getDoor(doorId).openMe();
						break;
					
					default:
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(8275));
						break;
				}
				break;
		}
	}
}