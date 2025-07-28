package net.sf.l2j.gameserver.communitybbs;

import java.util.StringTokenizer;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.custom.AuctionBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.BuffBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.ClassMasterBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.GateKeeperBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.IndexCBManager;
import net.sf.l2j.gameserver.communitybbs.custom.MissionBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.RankingBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.ServiceBBSManager;
import net.sf.l2j.gameserver.communitybbs.custom.ShopBBSManager;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class CustomCommunityBoard
{
	private static final CLogger LOGGER = new CLogger(CustomCommunityBoard.class.getName());
	
	protected CustomCommunityBoard()
	{
		if (!Config.ENABLE_CUSTOM_BBS)
			return;
		
		LOGGER.info("Loaded custom community board.");
	}
	
	public void handleCommands(GameClient client, String command)
	{
		final Player player = client.getPlayer();
		if (player == null)
			return;
		
		if (!Config.ENABLE_CUSTOM_BBS)
		{
			player.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}
		
		if (!player.isGM() && (player.getCast().isCastingNow() || player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || player.getPvpFlag() > 0 || player.getKarma() > 0 || player.isAlikeDead()))
		{
			player.sendMessage("You can't use the Community Board right now.");
			return;
		}
		
		if (command.startsWith("_bbsgetfav_add"))
			ServiceBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbshome"))
			IndexCBManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsgetfav"))
			GateKeeperBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsloc"))
			BuffBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsclan"))
			RankingBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsmemo"))
			ClassMasterBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_maillist_0_1_0_"))
			ShopBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsmultisell;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			ShopBBSManager.getInstance().parseCmd("_maillist_0_1_0_;" + st.nextToken(), player);
			MultisellData.getInstance().separateAndSendCb("" + st.nextToken(), player, false);
		}
		else if (command.startsWith("_bbsexcmultisell;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			ShopBBSManager.getInstance().parseCmd("_maillist_0_1_0_;" + st.nextToken(), player);
			MultisellData.getInstance().separateAndSendCb("" + st.nextToken(), player, true);
		}
		else if (command.startsWith("_friend"))
			AuctionBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_cbmission"))
			MissionBBSManager.getInstance().parseCmd(command, player);
		else
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", player);
	}
	
	public static CustomCommunityBoard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CustomCommunityBoard INSTANCE = new CustomCommunityBoard();
	}
}