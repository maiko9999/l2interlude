package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class TerritoryStatus implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"TerritoryStatus"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (target instanceof Npc npc)
		{
			final Castle castle = npc.getCastle();
			if (castle == null)
				return false;
			
			final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
			
			final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
			if (clan != null)
			{
				html.setFile(player.getLocale(), "html/territorystatus.htm");
				html.replace("%clanName%", clan.getName());
				html.replace("%clanLeaderName%", clan.getLeaderName());
				html.replace("%taxPercent%", castle.getCurrentTaxPercent());
			}
			else
				html.setFile(player.getLocale(), "html/territorynoclan.htm");
			
			html.replace("%territory%", (castle.getId() > 6) ? NpcStringId.ID_1001100.getMessage() : NpcStringId.ID_1001000.getMessage());
			html.replace("%townName%", castle.getTownName());
			html.replace("%objectId%", target.getObjectId());
			
			player.sendPacket(html);
		}
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
