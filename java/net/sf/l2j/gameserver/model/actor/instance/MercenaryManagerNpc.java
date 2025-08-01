package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.manager.BuyListManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.enums.PrivilegeType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.actors.NpcTalkCond;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public final class MercenaryManagerNpc extends Folk
{
	public MercenaryManagerNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final NpcTalkCond condition = getNpcTalkCond(player);
		if (condition != NpcTalkCond.OWNER)
			return;
		
		if (command.startsWith("back"))
			showChatWindow(player);
		else if (command.startsWith("how_to"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/mercmanager/mseller005.htm");
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (command.startsWith("hire"))
		{
			// Can't buy new mercenaries if seal validation period isn't reached.
			if (!SevenSignsManager.getInstance().isSealValidationPeriod())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getLocale(), "html/mercmanager/msellerdenial.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
				return;
			}
			
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			final NpcBuyList buyList = BuyListManager.getInstance().getBuyList(Integer.parseInt(getNpcId() + st.nextToken()));
			if (buyList == null || !buyList.isNpcAllowed(getNpcId()))
				return;
			
			player.tempInventoryDisable();
			player.sendPacket(new BuyList(buyList, player.getAdena(), 0));
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/mercmanager/mseller004.htm");
			player.sendPacket(html);
		}
		else if (command.startsWith("merc_limit"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getLocale(), "html/mercmanager/" + ((getCastle().getId() == 5) ? "aden_msellerLimit.htm" : "msellerLimit.htm"));
			html.replace("%castleName%", getCastle().getName());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		switch (getNpcTalkCond(player))
		{
			case NONE:
				html.setFile(player.getLocale(), "html/mercmanager/mseller002.htm");
				break;
			
			case UNDER_SIEGE:
				html.setFile(player.getLocale(), "html/mercmanager/mseller003.htm");
				break;
			
			default:
				// Different output depending about who is currently owning the Seal of Strife.
				switch (SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE))
				{
					case DAWN:
						html.setFile(player.getLocale(), "html/mercmanager/mseller001_dawn.htm");
						break;
					
					case DUSK:
						html.setFile(player.getLocale(), "html/mercmanager/mseller001_dusk.htm");
						break;
					
					default:
						html.setFile(player.getLocale(), "html/mercmanager/mseller001.htm");
						break;
				}
				break;
		}
		
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	protected NpcTalkCond getNpcTalkCond(Player player)
	{
		if (getCastle() != null && player.getClan() != null)
		{
			if (getCastle().getSiege().isInProgress())
				return NpcTalkCond.UNDER_SIEGE;
			
			if (getCastle().getOwnerId() == player.getClanId() && player.hasClanPrivileges(PrivilegeType.CP_MERCENARIES))
				return NpcTalkCond.OWNER;
		}
		return NpcTalkCond.NONE;
	}
}