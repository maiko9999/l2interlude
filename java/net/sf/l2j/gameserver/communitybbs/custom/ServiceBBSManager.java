package net.sf.l2j.gameserver.communitybbs.custom;

import java.util.StringTokenizer;
import net.sf.l2j.gameserver.communitybbs.manager.BaseBBSManager;
import net.sf.l2j.gameserver.custom.data.DonateData;
import net.sf.l2j.gameserver.custom.data.DonateData.Donate;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.model.actor.Player;

public class ServiceBBSManager extends BaseBBSManager
{
	@Override
	public void parseCmd(String command, Player player)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String action = st.nextToken();
		switch (action)
		{
			case "_bbsgetfav_add" -> {
				showPage("index", player);
			}
			
			case "_bbsgetfav_add;page" -> {
				String page = st.nextToken();
				showPage(page, player);
			}
			
			case "_bbsgetfav_add;nobles" -> {
				showPage("character", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.setNobless(player, service);
			}
			
			case "_bbsgetfav_add;hero" -> {
				showPage("character", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.setHero(player, service);
			}
			
			case "_bbsgetfav_add;setnamecolor" -> {
				showPage("colorname", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				if (!st.hasMoreTokens())
					return;
				
				DonateData.setNameColor(player, service, st.nextToken());
			}
			
			case "_bbsgetfav_add;settitlecolor" -> {
				showPage("colortitle", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				if (!st.hasMoreTokens())
					return;
				
				DonateData.setTitleColor(player, service, st.nextToken());
			}
			
			case "_bbsgetfav_add;setname" -> {
				showPage("name", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				if (!st.hasMoreTokens())
					return;
				
				DonateData.setName(player, service, st.nextToken());
			}
			
			case "_bbsgetfav_add;premium" -> {
				showPage("premium", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.setPremium(player, service);
			}
			
			case "_bbsgetfav_add;gender" -> {
				showPage("character", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.setGender(player, service);
			}
			
			case "_bbsgetfav_add;nullpk" -> {
				showPage("character", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.clearPK(player, service);
			}
			
			case "_bbsgetfav_add;clanlvl" -> {
				showPage("clan", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.setClanLevel(player, service);
			}
			
			case "_bbsgetfav_add;clanskill" -> {
				showPage("clan", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.addClanSkill(player, service);
			}
			case "_bbsgetfav_add;clanrep" -> {
				showPage("clan", player);
				final var service = scanService(st);
				if (!checkService(service, player, command))
					return;
				
				DonateData.addClanRep(player, service);
			}
			
			case "_bbsgetfav_add;multisell" -> {
				String[] args = command.split(" ");
				if (args.length < 2)
					return;
				
				MultisellData.getInstance().separateAndSendCb(args[1], player, false);
			}
		}
	}
	
	private void showPage(String page, Player player)
	{
		String content = HTMLData.getInstance().getHtm(player.getLocale(), CB_PATH + getFolder() + page + ".htm");
		content = content.replace("%name%", player.getName());
		separateAndSend(content, player);
	}
	
	public static Donate scanService(StringTokenizer st)
	{
		return DonateData.getInstance().getDonate(Integer.parseInt(st.nextToken()));
	}
	
	public static boolean checkService(Donate donate, Player pc, String command)
	{
		if (donate != null)
			return true;
		
		LOGGER.info("pc[{}] use missing service[{}]", pc.getName(), command);
		return false;
	}
	
	@Override
	protected String getFolder()
	{
		return "custom/donate/";
	}
	
	public static ServiceBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ServiceBBSManager INSTANCE = new ServiceBBSManager();
	}
}