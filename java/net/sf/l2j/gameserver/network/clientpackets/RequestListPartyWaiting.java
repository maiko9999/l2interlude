package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.data.manager.PartyMatchRoomManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.PartyMatchRoom;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyRoomMember;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchDetail;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchList;

public final class RequestListPartyWaiting extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _auto;
	private int _bbs;
	private int _lvl;
	
	@Override
	protected void readImpl()
	{
		_auto = readD();
		_bbs = readD();
		_lvl = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (player.isCursedWeaponEquipped())
			return;
		
		if (player.isInPartyMatchRoom())
		{
			final PartyMatchRoom room = PartyMatchRoomManager.getInstance().getRoom(player.getPartyRoom());
			if (room == null)
				return;
			
			player.sendPacket(new PartyMatchDetail(room));
			player.sendPacket(new ExPartyRoomMember(room, 2));
			player.broadcastUserInfo();
		}
		else
		{
			if (player.getParty() != null && !player.getParty().isLeader(player))
			{
				player.sendPacket(SystemMessageId.CANT_VIEW_PARTY_ROOMS);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Add to waiting list.
			PartyMatchRoomManager.getInstance().addWaitingPlayer(player);
			
			// Send Room list.
			player.sendPacket(new PartyMatchList(player, _bbs, _lvl));
		}
	}
}