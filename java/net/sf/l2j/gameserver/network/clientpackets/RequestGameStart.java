package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Locale;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.AntiFeedManager;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.model.CharSelectSlot;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.GameClient.GameClientState;
import net.sf.l2j.gameserver.network.serverpackets.CharSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SSQInfo;

public class RequestGameStart extends L2GameClientPacket
{
	private int _slot;
	
	@Override
	protected void readImpl()
	{
		_slot = readD();
		readH(); // Not used.
		readD(); // Not used.
		readD(); // Not used.
		readD(); // Not used.
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (!client.performAction(FloodProtector.CHARACTER_SELECT))
			return;
		
		if ((Config.DUALBOX_CHECK_MAX_PLAYERS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddClient(AntiFeedManager.GAME_ID, client, Config.DUALBOX_CHECK_MAX_PLAYERS_PER_IP))
		{
			final NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setFile(Locale.forLanguageTag("en-US"), "html/mods/IPRestriction.htm");
			msg.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(client, Config.DUALBOX_CHECK_MAX_PLAYERS_PER_IP)));
			client.sendPacket(msg);
			return;
		}
		
		// we should always be able to acquire the lock but if we cant lock then nothing should be done (ie repeated packet)
		if (client.getActiveCharLock().tryLock())
		{
			try
			{
				// should always be null but if not then this is repeated packet and nothing should be done here
				if (client.getPlayer() == null)
				{
					final CharSelectSlot info = client.getCharSelectSlot(_slot);
					if (info == null || info.getAccessLevel() < 0)
						return;
					
					// Load up character from disk
					final Player player = client.loadCharFromDisk(_slot);
					if (player == null)
						return;
					
					player.setClient(client);
					client.setPlayer(player);
					player.setOnlineStatus(true, true);
					player.getStatus().setHpMp(info.getCurrentHp(), info.getCurrentMp());
					player.getStatus().setCp(info.getMaxHp());
					
					sendPacket(SSQInfo.sendSky());
					
					client.setState(GameClientState.ENTERING);
					
					sendPacket(new CharSelected(player, client.getSessionId().playOkID1));
				}
			}
			finally
			{
				client.getActiveCharLock().unlock();
			}
		}
	}
}