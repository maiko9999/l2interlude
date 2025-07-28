package net.sf.l2j.gameserver.communitybbs.custom;

import java.text.SimpleDateFormat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;

public class IndexCBManager extends BaseBBSManager
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	protected IndexCBManager()
	{
	}
	
	@Override
	public void parseCmd(String command, Player player)
	{
		if (command.startsWith("_bbshome"))
		{
			String content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + "index.htm");
			content = content.replace("%name%", String.valueOf(player.getName()));
			content = content.replace("%accountName%", player.getAccountName());
			content = content.replace("%class%", player.getTemplate().getClassName());
			content = content.replace("%lvl%", String.valueOf(player.getStatus().getLevel()));
			content = content.replace("%players%", String.valueOf(World.getInstance().getPlayers().size()));
			content = content.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
			content = content.replace("%pkkills%", String.valueOf(player.getPkKills()));
			
			final Clan clan = ClanTable.getInstance().getClan(player.getClanId());
			
			if (clan != null)
				content = content.replaceAll("%clan%", clan.getName());
			else
				content = content.replaceAll("%clan%", "<font color=FF0000>No</font>");
			
			if (player.getPremServiceData() == 1)
			{
				content = content.replace("%rate_xp%", String.valueOf(Config.PREMIUM_RATE_XP));
				content = content.replace("%rate_sp%", String.valueOf(Config.PREMIUM_RATE_SP));
				content = content.replace("%rate_adena%", String.valueOf(Config.PREMIUM_RATE_DROP_CURRENCY));
				content = content.replace("%rate_seal%", String.valueOf(Config.PREMIUM_RATE_DROP_SEAL_STONE));
				content = content.replace("%rate_items%", String.valueOf(Config.PREMIUM_RATE_DROP_ITEMS));
				content = content.replace("%rate_spoil%", String.valueOf(Config.PREMIUM_RATE_DROP_SPOIL));
				content = content.replace("%rate_quest%", String.valueOf(Config.PREMIUM_RATE_QUEST_DROP));
				content = content.replace("%rate_raid%", String.valueOf(Config.PREMIUM_RATE_DROP_ITEMS_BY_RAID));
			}
			else
			{
				content = content.replace("%rate_xp%", String.valueOf(Config.RATE_XP));
				content = content.replace("%rate_sp%", String.valueOf(Config.RATE_SP));
				content = content.replace("%rate_adena%", String.valueOf(Config.RATE_DROP_CURRENCY));
				content = content.replace("%rate_seal%", String.valueOf(Config.RATE_DROP_SEAL_STONE));
				content = content.replace("%rate_items%", String.valueOf(Config.RATE_DROP_ITEMS));
				content = content.replace("%rate_spoil%", String.valueOf(Config.RATE_DROP_SPOIL));
				content = content.replace("%rate_quest%", String.valueOf(Config.RATE_QUEST_DROP));
				content = content.replace("%rate_raid%", String.valueOf(Config.RATE_DROP_ITEMS_BY_RAID));
			}
			
			content = content.replace("%premium%", player.getPremiumService() == 1 ? "<font color=00FF00>ON</font>" : "<font color=FF0000>OFF</font>");
			content = content.replace("%premiumEnd%", player.getPremiumService() == 1 ? DATE_FORMAT.format(player.getPremServiceData()) : "");
			
			separateAndSend(content, player);
		}
		else
			super.parseCmd(command, player);
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/";
	}
	
	public static IndexCBManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final IndexCBManager INSTANCE = new IndexCBManager();
	}
}