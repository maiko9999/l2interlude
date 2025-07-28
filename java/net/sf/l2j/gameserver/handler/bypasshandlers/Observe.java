package net.sf.l2j.gameserver.handler.bypasshandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.ObserverGroupData;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.ObserverLocation;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class Observe implements IBypassHandler
{
	private static final String[] COMMANDS = { "observe" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		final ObserverLocation loc = ObserverGroupData.getInstance().getObserverLocation(Integer.parseInt(st.nextToken()));
		if (loc == null)
			return false;
		
		final boolean hasSummon = player.getSummon() != null;
		
		if (loc.getCastleId() > 0)
		{
			// Summon check. Siege observe type got an appropriate message.
			if (hasSummon)
			{
				player.sendPacket(SystemMessageId.NO_OBSERVE_WITH_PET);
				return false;
			}
			
			// Active siege must exist.
			final Castle castle = CastleManager.getInstance().getCastleById(loc.getCastleId());
			if (castle == null || !castle.getSiege().isInProgress())
			{
				player.sendPacket(SystemMessageId.ONLY_VIEW_SIEGE);
				return false;
			}
		}
		// Summon check for regular observe. No message on retail.
		else if (hasSummon)
			return false;
		
		// Can't observe if under attack stance.
		if (player.isInCombat())
		{
			player.sendPacket(SystemMessageId.CANNOT_OBSERVE_IN_COMBAT);
			return false;
		}
		
		// Olympiad registration check. No message on retail.
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
			return false;
		
		player.enterObserverMode(loc);
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}