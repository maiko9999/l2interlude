package net.sf.l2j.gameserver.network.clientpackets;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.manager.MailBBSManager;
import net.sf.l2j.gameserver.custom.data.EquipGradeRestrictionData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.manager.CoupleManager;
import net.sf.l2j.gameserver.data.manager.DropSkipManager;
import net.sf.l2j.gameserver.data.manager.PcCafeManager;
import net.sf.l2j.gameserver.data.manager.PetitionManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.RestartType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.ClassMaster;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.SubPledge;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.model.residence.castle.Siege;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.residence.clanhall.SiegableHall;
import net.sf.l2j.gameserver.network.GameClient.GameClientState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SkillList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

import ext.mods.playergod.PlayerGodManager;
import ext.mods.tour.TourData;
import ext.mods.tour.TournamentEvent;

public class EnterWorld extends L2GameClientPacket
{
	
	@Override
	protected void readImpl()
	{
		// Do nothing.
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			getClient().closeNow();
			return;
		}
		
		getClient().setState(GameClientState.IN_GAME);
		
		for (ItemInstance item : player.getInventory().getPaperdollItems())
		{
			if (item == null || !item.isEquipable())
				continue;
			
			final CrystalType grade = item.getItem().getCrystalType();
			if (!EquipGradeRestrictionData.getInstance().isEquipAllowed(grade))
			{
				
				player.getInventory().unequipItemInSlot(item.getLocationSlot());
				
				String message = EquipGradeRestrictionData.getInstance().getBlockMessage(grade);
				player.sendMessage("Item " + item.getName() + " was removed: " + message);
			}
		}
		
		final int objectId = player.getObjectId();
		DropSkipManager.getInstance().loadPlayer(objectId);
		if (player.isGM())
		{
			
			if (Config.SUPER_HASTE)
				SkillTable.getInstance().getInfo(7029, 4).getEffects(player, player);
			
			if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel()))
				player.setInvul(true);
			
			if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel()))
				player.getAppearance().setVisible(false);
			
			if (Config.GM_STARTUP_BLOCK_ALL)
				player.setInBlockingAll(true);
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmlist", player.getAccessLevel()))
				AdminData.getInstance().addGm(player, false);
			else
				AdminData.getInstance().addGm(player, true);
		}
		
		// Set dead status if applies
		if (player.getStatus().getHp() < 0.5 && player.isMortal())
			player.setIsDead(true);
		
		if (player.getNameColor() != 0)
			player.getAppearance().setNameColor(player.getNameColor());
		
		if (player.getTitleColor() != 0)
			player.getAppearance().setTitleColor(player.getTitleColor());
		
		player.getMacroList().sendUpdate();
		player.sendPacket(new ExStorageMaxCount(player));
		player.sendPacket(new HennaInfo(player));
		player.updateEffectIcons();
		player.sendPacket(new EtcStatusUpdate(player));
		
		// Clan checks.
		final Clan clan = player.getClan();
		if (clan != null)
		{
			player.sendPacket(new PledgeSkillList(clan));
			
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addCharName(player);
			final PledgeShowMemberListUpdate psmlu = new PledgeShowMemberListUpdate(player);
			
			// Send packets to others members.
			for (Player member : clan.getOnlineMembers())
			{
				if (member == player)
					continue;
				
				member.sendPacket(sm);
				member.sendPacket(psmlu);
			}
			
			// Send a login notification to sponsor or apprentice, if logged.
			if (player.getSponsor() != 0)
			{
				final Player sponsor = World.getInstance().getPlayer(player.getSponsor());
				if (sponsor != null)
					sponsor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addCharName(player));
			}
			else if (player.getApprentice() != 0)
			{
				final Player apprentice = World.getInstance().getPlayer(player.getApprentice());
				if (apprentice != null)
					apprentice.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addCharName(player));
			}
			
			// Add message at connexion if clanHall not paid.
			final ClanHall ch = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (ch != null && !ch.getPaid())
				player.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
			
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Map<Integer, Integer> skills = player.isClanLeader() ? castle.getSkillsLeader() : castle.getSkillsMember();
				skills.forEach((skillId, skillLvl) ->
				{
					if (castle.getId() == player.getClan().getCastleId())
						player.addSkill(SkillTable.getInstance().getInfo(skillId, skillLvl), true);
					else
						player.removeSkill(skillId, true);
				});
				
				final Siege siege = castle.getSiege();
				if (!siege.isInProgress())
					continue;
				
				final SiegeSide type = siege.getSide(clan);
				if (type == SiegeSide.ATTACKER)
					player.setSiegeState((byte) 1);
				else if (type == SiegeSide.DEFENDER || type == SiegeSide.OWNER)
					player.setSiegeState((byte) 2);
			}
			
			for (SiegableHall hall : ClanHallManager.getInstance().getSiegableHalls())
			{
				if (hall.isInSiege() && hall.isRegistered(clan))
					player.setSiegeState((byte) 1);
			}
			
			player.sendPacket(new PledgeShowMemberListUpdate(player));
			player.sendPacket(new PledgeShowMemberListAll(clan, 0));
			
			for (SubPledge sp : clan.getAllSubPledges())
				player.sendPacket(new PledgeShowMemberListAll(clan, sp.getId()));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) != CabalType.NORMAL)
		{
			CabalType cabal = SevenSignsManager.getInstance().getPlayerCabal(objectId);
			if (cabal != CabalType.NORMAL)
			{
				if (cabal == SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE))
					player.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill(), false);
				else
					player.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill(), false);
			}
		}
		else
		{
			player.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill().getId(), false);
			player.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill().getId(), false);
		}
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			player.setSpawnProtection(true);
		
		player.spawnMe();
		
		// Set the location of debug packets.
		player.setEnterWorldLoc(player.getX(), player.getY(), -16000);
		
		// Engage and notify partner.
		for (Entry<Integer, IntIntHolder> coupleEntry : CoupleManager.getInstance().getCouples().entrySet())
		{
			final IntIntHolder couple = coupleEntry.getValue();
			if (couple.getId() == objectId || couple.getValue() == objectId)
			{
				player.setCoupleId(coupleEntry.getKey());
				break;
			}
		}
		
		if (!player.isCursedWeaponEquipped())
		{
			int[] itemIds =
			{
				8190,
				8689
			};
			
			for (int itemId : itemIds)
			{
				if (player.getInventory().getItemByItemId(itemId) != null)
					player.destroyItem(player.getInventory().getItemByItemId(itemId), true);
			}
			
			player.removeSkill(3630, true);
			player.removeSkill(3631, true);
		}
		
		// Announcements, welcome & Seven signs period messages.
		player.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		player.sendPacket(SevenSignsManager.getInstance().getCurrentPeriod().getMessageId());
		AnnouncementData.getInstance().showAnnouncements(player, false);
		
		PcCafeManager.getInstance().onPlayerLogin(player);
		
		CTFEvent.getInstance().onLogin(player);
		DMEvent.getInstance().onLogin(player);
		LMEvent.getInstance().onLogin(player);
		TvTEvent.getInstance().onLogin(player);
		
		// If the Player is a Dark Elf, check for Shadow Sense at night.
		if (player.getRace() == ClassRace.DARK_ELF && player.hasSkill(L2Skill.SKILL_SHADOW_SENSE))
			player.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ? SystemMessageId.NIGHT_S1_EFFECT_APPLIES : SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
		
		// Notify quest for enterworld event, if quest allows it.
		player.getQuestList().getQuests(Quest::isTriggeredOnEnterWorld).forEach(q -> q.onEnterWorld(player));
		
		// Manually calculate and set the weight, since we got no items manipulation to automatically call it. It is shown over ItemList packet.
		player.getInventory().updateWeight();
		
		player.sendPacket(new QuestList(player));
		player.sendPacket(new SkillList(player));
		player.sendPacket(new FriendList(player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ItemList(player, false));
		player.sendPacket(new ShortCutInit(player));
		
		player.checkCondition(player.getStatus().getMaxHp(), player.getStatus().getHp());
		
		// No broadcast needed since the player will already spawn dead to others.
		if (player.isAlikeDead())
			player.sendPacket(new Die(player));
		
		// Unread mails make a popup appears.
		if (Config.ENABLE_COMMUNITY_BOARD && MailBBSManager.getInstance().checkIfUnreadMail(player))
		{
			player.sendPacket(SystemMessageId.NEW_MAIL);
			player.sendPacket(new PlaySound("systemmsg_e.1233"));
			player.sendPacket(ExMailArrived.STATIC_PACKET);
		}
		
		// Clan notice, if active.
		if (Config.ENABLE_COMMUNITY_BOARD && clan != null && clan.isNoticeEnabled())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(player.getLocale(), "html/clan_notice.htm");
			html.replace("%clan_name%", clan.getName());
			html.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replace("action", "").replace("bypass", ""));
			sendPacket(html);
		}
		else if (Config.SERVER_NEWS)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(player.getLocale(), "html/servnews.htm");
			sendPacket(html);
		}
		
		if (player.getPremiumService() == 1)
			onEnterPremium(player);
		
		PetitionManager.getInstance().checkActivePetition(player);
		
		player.onPlayerEnter();
		
		player.broadcastUserInfo();
		
		sendPacket(new SkillCoolTime(player));
		
		// If player logs back in a stadium, port him in nearest town.
		if (Olympiad.getInstance().playerInStadia(player))
			player.teleportTo(RestartType.TOWN);
		
		if (player.getClanJoinExpiryTime() > System.currentTimeMillis())
			player.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		
		// Attacker or spectator logging into a siege zone will be ported at town.
		if (player.isInsideZone(ZoneId.SIEGE) && player.getSiegeState() < 2)
			player.teleportTo(RestartType.TOWN);
		
		if (player.isInsideZone(ZoneId.BOSS) && (System.currentTimeMillis() - player.getLastAccess()) > 300000)
			player.teleportTo(RestartType.TOWN);
		
		ClassMaster.showQuestionMark(player);
		
		// Tutorial
		final QuestState qs = player.getQuestList().getQuestState("Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent("UC", null, player);
		
		PlayerGodManager.getInstance().onEnterWorld(player);
		player.restoreDressMe();
		if (TournamentEvent.isRunning())
		{
			
			String lastStart = TournamentEvent.lastEvent();
			LocalTime startTime = LocalTime.parse(lastStart, DateTimeFormatter.ofPattern("HH:mm"));
			
			LocalTime now = LocalTime.now();
			
			int durationMinutes = TourData.getInstance().getConfig().getDuration();
			
			LocalTime endTime = startTime.plusMinutes(durationMinutes);
			
			Duration remaining = Duration.between(now, endTime);
			long minutes = remaining.toMinutes();
			
			if (minutes > 0)
			{
				player.sendMessage("Tournament is live! Started at " + lastStart + "h. Remaining time: " + minutes + " minute(s).");
			}
			else
			{
				player.sendMessage("Tournament is live! Started ons " + lastStart + "h.");
			}
			
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void onEnterPremium(Player player)
	{
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date dt = new Date(player.getPremServiceData());
		player.sendMessage(player.getSysString(10_046, df.format(dt)));
		
		String color = player.getMemos().get("PlayerColorName");
		String title = player.getMemos().get("PlayerColorTitle");
		
		if (color != null)
		{
			player.setNameColor(Integer.decode("0x" + color));
			player.getAppearance().setTitleColor(Integer.decode("0x" + color));
		}
		if (title != null)
		{
			player.setTitleColor(Integer.decode("0x" + title));
			player.getAppearance().setTitleColor(Integer.decode("0x" + title));
		}
		player.broadcastUserInfo();
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}