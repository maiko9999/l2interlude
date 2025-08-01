package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.CoupleManager;
import net.sf.l2j.gameserver.data.manager.RelationManager;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.MissionType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class WeddingManagerNpc extends Folk
{
	public WeddingManagerNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onInteract(Player player)
	{
		// Married people got access to another menu
		if (player.getCoupleId() > 0)
			sendHtmlMessage(player, "html/mods/wedding/start2.htm");
		// "Under marriage acceptance" people go to this one
		else if (player.isUnderMarryRequest())
			sendHtmlMessage(player, "html/mods/wedding/waitforpartner.htm");
		// And normal players go here :)
		else
			sendHtmlMessage(player, "html/mods/wedding/start.htm");
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("AskWedding"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				final Player partner = World.getInstance().getPlayer(st.nextToken());
				if (partner == null)
				{
					sendHtmlMessage(player, "html/mods/wedding/notfound.htm");
					return;
				}
				
				// check conditions
				if (!weddingConditions(player, partner))
					return;
				
				// block the wedding manager until an answer is given.
				player.setUnderMarryRequest(true);
				partner.setUnderMarryRequest(true);
				
				// memorize the requesterId for future use, and send a popup to the target
				partner.setRequesterId(player.getObjectId());
				partner.sendPacket(new ConfirmDlg(1983).addString(player.getName() + " asked you to marry. Do you want to start a new relationship ?"));
			}
			else
				sendHtmlMessage(player, "html/mods/wedding/notfound.htm");
		}
		else if (command.startsWith("Divorce"))
			CoupleManager.getInstance().deleteCouple(player.getCoupleId());
		else if (command.startsWith("GoToLove"))
		{
			// Find the partner using the couple id.
			final int partnerId = CoupleManager.getInstance().getPartnerId(player.getCoupleId(), player.getObjectId());
			if (partnerId == 0)
			{
				player.sendMessage(player.getSysString(10_061));
				return;
			}
			
			final Player partner = World.getInstance().getPlayer(partnerId);
			if (partner == null)
			{
				player.sendMessage(player.getSysString(10_062));
				return;
			}
			
			// Simple checks to avoid exploits
			if (partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || partner.isInJail() || partner.isInOlympiadMode() || partner.isInDuel() || partner.isFestivalParticipant() || partner.isInObserverMode())
			{
				player.sendMessage(player.getSysString(10_063));
				return;
			}
			
			if (partner.getClan() != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()) != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()).getSiege().isInProgress())
			{
				player.sendMessage(player.getSysString(10_064));
				return;
			}
			
			// If all checks are successfully passed, teleport the player to the partner
			player.teleportTo(partner.getX(), partner.getY(), partner.getZ(), 20);
		}
	}
	
	private boolean weddingConditions(Player requester, Player partner)
	{
		// Check if player target himself
		if (partner.getObjectId() == requester.getObjectId())
		{
			sendHtmlMessage(requester, "html/mods/wedding/error_wrongtarget.htm");
			return false;
		}
		
		// Sex check
		if (!Config.WEDDING_SAMESEX && partner.getAppearance().getSex() == requester.getAppearance().getSex())
		{
			sendHtmlMessage(requester, "html/mods/wedding/error_sex.htm");
			return false;
		}
		
		// Check if player has the target on friendlist
		if (!RelationManager.getInstance().areFriends(requester.getObjectId(), partner.getObjectId()))
		{
			sendHtmlMessage(requester, "html/mods/wedding/error_friendlist.htm");
			return false;
		}
		
		// Target mustn't be already married
		if (partner.getCoupleId() > 0)
		{
			sendHtmlMessage(requester, "html/mods/wedding/error_alreadymarried.htm");
			return false;
		}
		
		// Check for Formal Wear
		if (Config.WEDDING_FORMALWEAR && (!requester.isWearingFormalWear() || !partner.isWearingFormalWear()))
		{
			sendHtmlMessage(requester, "html/mods/wedding/error_noformal.htm");
			return false;
		}
		
		// Check and reduce wedding price
		if (requester.getAdena() < Config.WEDDING_PRICE || partner.getAdena() < Config.WEDDING_PRICE)
		{
			sendHtmlMessage(requester, "html/mods/wedding/error_adena.htm");
			return false;
		}
		
		return true;
	}
	
	public static void justMarried(Player requester, Player partner)
	{
		// Unlock the wedding manager for both users, and set them as married.
		requester.setUnderMarryRequest(false);
		partner.setUnderMarryRequest(false);
		
		// Reduce Adena amount according to configs.
		requester.reduceAdena(Config.WEDDING_PRICE, true);
		partner.reduceAdena(Config.WEDDING_PRICE, true);
		
		// Messages to the couple.
		requester.sendMessage(requester.getSysString(10_065, partner.getName()));
		partner.sendMessage(partner.getSysString(10_066, requester.getName()));
		
		// Wedding march.
		requester.broadcastPacket(new MagicSkillUse(requester, requester, 2230, 1, 1, 0));
		partner.broadcastPacket(new MagicSkillUse(partner, partner, 2230, 1, 1, 0));
		
		// Fireworks
		requester.broadcastPacket(new MagicSkillUse(requester, requester, 2025, 1, 1, 0));
		partner.broadcastPacket(new MagicSkillUse(partner, partner, 2025, 1, 1, 0));
		
		requester.getMissions().update(MissionType.MARRIED);
		partner.getMissions().update(MissionType.MARRIED);
		
		World.announceToOnlinePlayers(requester.getSysString(10_067, requester.getName(), partner.getName()));
	}
	
	private void sendHtmlMessage(Player player, String file)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getLocale(), file);
		html.replace("%objectId%", getObjectId());
		html.replace("%adenaCost%", StringUtil.formatNumber(Config.WEDDING_PRICE));
		html.replace("%needOrNot%", Config.WEDDING_FORMALWEAR ? player.getSysString(10_068) : player.getSysString(10_069));
		player.sendPacket(html);
	}
}