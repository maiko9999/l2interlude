package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.mmocore.SendablePacket;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.GameClient;

public abstract class L2GameServerPacket extends SendablePacket<GameClient>
{
	protected static final CLogger LOGGER = new CLogger(L2GameServerPacket.class.getName());
	
	protected abstract void writeImpl();
	
	@Override
	protected void write()
	{
		if (Config.PACKET_HANDLER_DEBUG && !Config.SERVER_PACKETS.contains(getClass().getSimpleName()))
			LOGGER.info(getType());
		
		try
		{
			writeImpl();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed writing {} for {}. ", e, getType(), getClient().toString());
		}
	}
	
	public void runImpl()
	{
	}
	
	public String getType()
	{
		if (getClient().getPlayer() != null)
			return "[" + getClient().getPlayer().getName() + "] " + "[S] " + getClass().getSimpleName();
		
		return "[S] " + getClass().getSimpleName();
	}
}