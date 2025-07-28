package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.communitybbs.CustomCommunityBoard;

public final class RequestShowBoard extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		readD(); // Not used for security reason.
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.ENABLE_CUSTOM_BBS)
			CustomCommunityBoard.getInstance().handleCommands(getClient(), Config.BBS_DEFAULT);
		
		if (Config.ENABLE_COMMUNITY_BOARD)
			CommunityBoard.getInstance().handleCommands(getClient(), Config.BBS_DEFAULT);
	}
}