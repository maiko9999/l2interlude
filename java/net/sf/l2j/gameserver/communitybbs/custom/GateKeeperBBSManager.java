package net.sf.l2j.gameserver.communitybbs.custom;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class GateKeeperBBSManager extends BaseBBSManager
{
	@Override
	public void parseCmd(String command, Player player)
	{
		if (command.equals("_bbsgetfav"))
			showPage(0, player);
		else if (command.startsWith("_bbsgetfav;page"))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				showPage(Integer.parseInt(args[1]), player);
		}
		else if (command.startsWith("_bbsgetfav;go"))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				teleport(player, Integer.parseInt(args[1]));
			
			showPage(0, player);
			
			// Close CB after teleport
			player.sendPacket(new ShowBoard());
		}
		
	}
	
	private void showPage(int page, Player player)
	{
		String content;
		if (page > 0)
			content = HTMLData.getInstance().getHtm(player.getLocale(), String.format(CB_PATH + getFolder() + "50010-%d.htm", page));
		else
			content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "50010.htm");
		
		separateAndSend(content, player);
	}
	
	/**
	 * Teleport the {@link Player} into the {@link Npc}'s {@link TeleportLocation}s {@link List} index.<br>
	 * <br>
	 * @param player : The {@link Player} to test.
	 * @param index : The {@link TeleportLocation} index information to retrieve from this {@link Npc}'s instant teleports {@link List}.
	 */
	protected void teleport(Player player, int index)
	{
		final List<TeleportLocation> teleports = TeleportData.getInstance().getTeleports(50010);
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
		
		if (Config.FREE_TELEPORT || player.getStatus().getLevel() <= Config.LVL_FREE_TELEPORT || teleport.getPriceCount() == 0 || player.destroyItemByItemId(teleport.getPriceId(), teleport.getPriceCount(), true))
			player.teleportTo(teleport, 20);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/gk/";
	}
	
	public static GateKeeperBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GateKeeperBBSManager INSTANCE = new GateKeeperBBSManager();
	}
}
