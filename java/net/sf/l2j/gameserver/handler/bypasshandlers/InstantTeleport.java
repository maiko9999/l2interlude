package net.sf.l2j.gameserver.handler.bypasshandlers;

import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.xml.InstantTeleportData;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

public class InstantTeleport implements IBypassHandler
{
	private static final String[] COMMANDS = { "instant_teleport" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (target instanceof Npc npc)
				instantTeleport(player, npc, Integer.parseInt(st.nextToken()));
		}
		catch (Exception e)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		return true;
	}
	
	/**
	 * Teleport the {@link Player} into the {@link Npc}'s instant teleports {@link List} index.<br>
	 * <br>
	 * The only check is {@link Npc#isTeleportAllowed(Player)}.
	 * @param player : The {@link Player} to test.
	 * @param npc : The {@link Npc} from which the teleport locations are retrieved.
	 * @param index : The {@link Location} index information to retrieve from this {@link Npc}'s instant teleports {@link List}.
	 */
	public void instantTeleport(Player player, Npc npc, int index)
	{
		if (!npc.isTeleportAllowed(player))
			return;
		
		final List<Location> teleports = InstantTeleportData.getInstance().getTeleports(npc.getNpcId());
		if (teleports == null || index > teleports.size())
			return;
		
		final Location teleport = teleports.get(index);
		if (teleport == null)
			return;
		
		player.teleportTo(teleport, 20);
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}