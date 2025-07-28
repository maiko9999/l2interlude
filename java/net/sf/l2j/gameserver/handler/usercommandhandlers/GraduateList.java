package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.util.Set;

import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class GraduateList implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		110
	};
	
	@Override
	public void useUserCommand(int id, Player player)
	{
		final Clan clan = player.getClan();
		if (clan == null)
			return;
		
		// Don't show anything if no graduates.
		final Set<Integer> graduates = clan.getGraduates();
		if (graduates.isEmpty())
			return;
		
		player.sendPacket(SystemMessageId.ACADEMY_LIST_HEADER);
		
		// Iterate the week's graduates.
		for (int objectId : graduates)
		{
			final String playerName = PlayerInfoTable.getInstance().getPlayerName(objectId);
			if (playerName != null)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.GRADUATES_S1).addString(playerName));
		}
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}