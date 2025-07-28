package net.sf.l2j.gameserver.handler.bypasshandlers;

import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

public class Teleport implements IBypassHandler
{
	private static final String[] COMMANDS = { "teleport" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (target instanceof Npc npc)
				teleport(player, npc, Integer.parseInt(st.nextToken()));
		}
		catch (Exception e)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		return true;
	}
	
	/**
	 * Teleport the {@link Player} into the {@link Npc}'s {@link TeleportLocation}s {@link List} index.<br>
	 * <br>
	 * Following checks are done : {@link Npc#isTeleportAllowed(Player)}, castle siege, price.
	 * @param player : The {@link Player} to test.
	 * @param npc : The {@link Npc} from which the teleport locations are retrieved.
	 * @param index : The {@link TeleportLocation} index information to retrieve from this {@link Npc}'s instant teleports {@link List}.
	 */
	public void teleport(Player player, Npc npc, int index)
	{
		if (!npc.isTeleportAllowed(player))
			return;
		
		final List<TeleportLocation> teleports = TeleportData.getInstance().getTeleports(npc.getNpcId());
		if (teleports == null || index > teleports.size())
			return;
		
		final TeleportLocation teleport = teleports.get(index);
		if (teleport == null)
			return;
		
		if (teleport.getCastleId() > 0)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(teleport.getCastleId());
			if (castle != null && castle.getSiege().isInProgress())
			{
				player.sendPacket(SystemMessageId.CANNOT_PORT_VILLAGE_IN_SIEGE);
				return;
			}
		}
		
		if (Config.FREE_TELEPORT && player.getStatus().getLevel() <= Config.LVL_FREE_TELEPORT || teleport.getPriceCount() == 0 || player.destroyItemByItemId(teleport.getPriceId(), teleport.getCalculatedPriceCount(player), true))
			player.teleportTo(teleport, 20);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}