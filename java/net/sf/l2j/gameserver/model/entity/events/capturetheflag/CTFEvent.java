package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.AntiFeedManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.EventState;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Events;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class CTFEvent extends Events
{
	private final String htmlPath = "html/mods/events/ctf/";
	
	private CTFEventTeam[] _teams = new CTFEventTeam[2];
	
	private EventState _state = EventState.INACTIVE;
	
	private Spawn _npcSpawn;
	private Npc _lastNpcSpawn;
	
	private Spawn _flag1Spawn;
	private Spawn _flag2Spawn;
	
	private Npc _lastFlag1Spawn;
	private Npc _lastFlag2Spawn;
	
	private Player _team1Carrier;
	private Player _team2Carrier;
	
	private ItemInstance _team1CarrierRHand;
	private ItemInstance _team2CarrierRHand;
	
	private ItemInstance _team1CarrierLHand;
	private ItemInstance _team2CarrierLHand;
	
	private Map<Integer, Boolean> _teleported = new HashMap<>();
	
	private CTFEvent()
	{
	}
	
	public void init()
	{
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.CTF_ID);
		_teams[0] = new CTFEventTeam(Config.CTF_EVENT_TEAM_1_NAME, Config.CTF_EVENT_TEAM_1_COORDINATES);
		_teams[1] = new CTFEventTeam(Config.CTF_EVENT_TEAM_2_NAME, Config.CTF_EVENT_TEAM_2_COORDINATES);
	}
	
	public boolean startParticipation()
	{
		final NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CTF_EVENT_PARTICIPATION_NPC_ID);
		
		if (tmpl == null)
		{
			LOGGER.warn("CTFEvent: EventManager is a NullPointer -> Invalid npc id in configs?");
			return false;
		}
		
		try
		{
			_npcSpawn = new Spawn(tmpl);
			_npcSpawn.setLoc(Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[0], Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[1], Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[2], Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(60000);
			
			SpawnManager.getInstance().addSpawn(_npcSpawn);
			_lastNpcSpawn = _npcSpawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.warn("CTFEventEngine: exception: " + e.getMessage(), e);
			return false;
		}
		
		setState(EventState.PARTICIPATING);
		return true;
	}
	
	private List<Player> sortPlayersByLevel(List<Player> players)
	{
		return players.stream().sorted(Comparator.comparingInt(p -> -p.getStatus().getLevel())).toList();
	}
	
	public boolean startFight()
	{
		// Set state to STARTING
		setState(EventState.STARTING);
		
		// Randomize and balance team distribution
		final Map<Integer, Player> allParticipants = new HashMap<>();
		
		allParticipants.putAll(_teams[0].getParticipatedPlayers());
		allParticipants.putAll(_teams[1].getParticipatedPlayers());
		
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		
		Player player;
		Iterator<Player> iter;
		if (needParticipationFee())
		{
			iter = allParticipants.values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!hasParticipationFee(player))
					iter.remove();
			}
		}
		
		int[] balance =
		{
			0,
			0
		};
		int priority = 0;
		List<Player> allParticipantsSorted = new ArrayList<>(sortPlayersByLevel(new ArrayList<>(allParticipants.values())));
		while (!allParticipantsSorted.isEmpty())
		{
			// Priority team gets one player
			Player highestLevelPlayer = allParticipantsSorted.remove(allParticipantsSorted.size() - 1);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();
			
			// Exiting if no more players
			if (allParticipantsSorted.isEmpty())
				break;
			
			// The other team gets one player
			priority = 1 - priority;
			highestLevelPlayer = allParticipantsSorted.remove(allParticipantsSorted.size() - 1);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();
			
			// Recalculating priority
			priority = balance[0] > balance[1] ? 1 : 0;
		}
		
		// Check for enought participants
		if ((_teams[0].getParticipatedPlayerCount() < Config.CTF_EVENT_MIN_PLAYERS_IN_TEAMS) || (_teams[1].getParticipatedPlayerCount() < Config.CTF_EVENT_MIN_PLAYERS_IN_TEAMS))
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);
			
			for (Player players : allParticipants.values())
				players.addItem(Config.CTF_EVENT_PARTICIPATION_FEE[0], Config.CTF_EVENT_PARTICIPATION_FEE[1], true);
			
			// Cleanup of teams
			_teams[0].cleanMe();
			_teams[1].cleanMe();
			
			// Unspawn the event NPC
			unSpawnNpc();
			AntiFeedManager.getInstance().clear(AntiFeedManager.CTF_ID);
			return false;
		}
		
		if (needParticipationFee())
		{
			iter = _teams[0].getParticipatedPlayers().values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!payParticipationFee(player))
					iter.remove();
			}
			
			iter = _teams[1].getParticipatedPlayers().values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!payParticipationFee(player))
					iter.remove();
			}
		}
		
		// Spawn Flag Quarters
		spawnFirstHeadQuarters();
		spawnSecondHeadQuarters();
		
		// Closes all doors specified in Configs for CTF
		closeDoors(Config.CTF_DOORS_IDS_TO_CLOSE);
		
		// Set state STARTED
		setState(EventState.STARTED);
		
		// Iterate over all teams
		for (CTFEventTeam team : _teams)
		{
			// Iterate over all participated player instances in this team
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				if (playerInstance != null)
					new CTFEventTeleporter(playerInstance, team.getCoordinates(), false, false); // Teleporter implements Runnable and starts itself
			}
		}
		
		return true;
	}
	
	public String calculateRewards()
	{
		if (_teams[0].getPoints() == _teams[1].getPoints())
		{
			// Check if one of the teams have no more players left
			if ((_teams[0].getParticipatedPlayerCount() == 0) || (_teams[1].getParticipatedPlayerCount() == 0))
			{
				// set state to rewarding
				setState(EventState.REWARDING);
				return "CTF Event: Event has ended. No team won due to inactivity!";
			}
			
			sysMsgToAllParticipants("Event has ended, both teams have tied.");
			if (Config.CTF_REWARD_TEAM_TIE)
			{
				rewardTeam(_teams[0]);
				rewardTeam(_teams[1]);
				return "CTF Event: Event has ended with both teams tying.";
			}
			
			return "CTF Event: Event has ended with both teams tying.";
		}
		
		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);
		
		// Get team which has more points
		CTFEventTeam team = _teams[_teams[0].getPoints() > _teams[1].getPoints() ? 0 : 1];
		rewardTeam(team);
		return "CTF Event: Event finish. Team " + team.getName() + " won with " + team.getPoints() + " points.";
	}
	
	private void rewardTeam(CTFEventTeam team)
	{
		// Iterate over all participated player instances of the winning team
		for (Player player : team.getParticipatedPlayers().values())
		{
			if (player == null)
				continue;
			
			SystemMessage systemMessage = null;
			
			// Iterate over all CTF event rewards
			for (IntIntHolder reward : Config.CTF_EVENT_REWARDS)
			{
				final PcInventory inv = player.getInventory();
				
				// Check for stackable item, non stackabe items need to be added one by one
				if (ItemData.getInstance().getTemplate(reward.getId()).isStackable())
				{
					inv.addItem(reward.getId(), reward.getValue());
					
					if (reward.getValue() > 1)
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(reward.getId()).addItemNumber(reward.getValue());
					else
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(reward.getId());
					
					player.sendPacket(systemMessage);
				}
				else
				{
					for (int i = 0; i < reward.getValue(); ++i)
					{
						inv.addItem(reward.getId(), 1);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(reward.getId()));
					}
				}
			}
			
			StatusUpdate statusUpdate = new StatusUpdate(player);
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			
			statusUpdate.addAttribute(StatusType.CUR_LOAD, player.getCurrentWeight());
			npcHtmlMessage.setHtml(HTMLData.getInstance().getHtm(player, htmlPath + "Reward.htm"));
			player.sendPacket(statusUpdate);
			player.sendPacket(npcHtmlMessage);
		}
	}
	
	public void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);
		
		// Unspawn event npc
		unSpawnNpc();
		
		// Opens all doors specified in Configs for CTF
		openDoors(Config.CTF_DOORS_IDS_TO_CLOSE);
		
		// Reset flag carriers
		if (_team1Carrier != null)
			removeFlagCarrier(_team1Carrier);
		
		if (_team2Carrier != null)
			removeFlagCarrier(_team2Carrier);
		
		// Iterate over all teams
		for (CTFEventTeam team : _teams)
		{
			for (Player player : team.getParticipatedPlayers().values())
			{
				// Check for nullpointer
				if (player != null)
					new CTFEventTeleporter(player, Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES, false, false); // Teleport back.
			}
		}
		
		// Cleanup of teams
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		
		_teleported = new HashMap<>();
		
		// Set state INACTIVE
		setState(EventState.INACTIVE);
		AntiFeedManager.getInstance().clear(AntiFeedManager.CTF_ID);
	}
	
	public synchronized boolean addParticipant(Player player)
	{
		if (player == null)
			return false;
		
		byte teamId = 0;
		
		if (_teams[0] == null || _teams[1] == null)
			return false;
		
		// Check to which team the player should be added
		if (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount())
			teamId = (byte) (Rnd.get(2));
		else
			teamId = (byte) (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount() ? 1 : 0);
		
		return _teams[teamId].addPlayer(player);
	}
	
	public boolean removeParticipant(int objectId)
	{
		// Get the teamId of the player
		byte teamId = getParticipantTeamId(objectId);
		
		// Check if the player is participant
		if (teamId != -1)
		{
			// Remove the player from team
			_teams[teamId].removePlayer(objectId);
			return true;
		}
		
		return false;
	}
	
	public boolean needParticipationFee()
	{
		return (Config.CTF_EVENT_PARTICIPATION_FEE[0] != 0) && (Config.CTF_EVENT_PARTICIPATION_FEE[1] != 0);
	}
	
	public boolean hasParticipationFee(Player player)
	{
		return player.getInventory().getItemCount(Config.CTF_EVENT_PARTICIPATION_FEE[0], -1) >= Config.CTF_EVENT_PARTICIPATION_FEE[1];
	}
	
	public boolean payParticipationFee(Player player)
	{
		return player.destroyItemByItemId(Config.CTF_EVENT_PARTICIPATION_FEE[0], Config.CTF_EVENT_PARTICIPATION_FEE[1], true);
	}
	
	public String getParticipationFee()
	{
		int itemId = Config.CTF_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.CTF_EVENT_PARTICIPATION_FEE[1];
		
		if (itemId == 0 || itemNum == 0)
			return "-";
		
		return String.valueOf(itemNum) + " " + ItemData.getInstance().getTemplate(itemId).getName();
	}
	
	public void sysMsgToAllParticipants(String message)
	{
		CreatureSay cs = new CreatureSay(0, SayType.PARTY, "Event Manager", message);
		
		for (Player player : _teams[0].getParticipatedPlayers().values())
		{
			if (player != null)
				player.sendPacket(cs);
		}
		
		for (Player player : _teams[1].getParticipatedPlayers().values())
		{
			if (player != null)
				player.sendPacket(cs);
		}
	}
	
	private void spawnFirstHeadQuarters()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CTF_EVENT_TEAM_1_HEADQUARTERS_ID);
		
		if (tmpl == null)
		{
			LOGGER.warn("CTFEvent: First Head Quater is a NullPointer -> Invalid npc id in configs?");
			return;
		}
		
		try
		{
			_flag1Spawn = new Spawn(tmpl);
			_flag1Spawn.setLoc(Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[0], Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[1], Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[2], Config.CTF_EVENT_TEAM_1_FLAG_COORDINATES[3]);
			_flag1Spawn.setRespawnDelay(60000);
			
			SpawnManager.getInstance().addSpawn(_flag1Spawn);
			
			_lastFlag1Spawn = _flag1Spawn.doSpawn(false);
			_lastFlag1Spawn.setTitle(Config.CTF_EVENT_TEAM_1_NAME);
		}
		catch (Exception e)
		{
			LOGGER.warn("SpawnFirstHeadQuaters: exception: " + e.getMessage(), e);
			return;
		}
	}
	
	private void spawnSecondHeadQuarters()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CTF_EVENT_TEAM_2_HEADQUARTERS_ID);
		
		if (tmpl == null)
		{
			LOGGER.warn("CTFEvent: Second Head Quater is a NullPointer -> Invalid npc id in configs?");
			return;
		}
		
		try
		{
			_flag2Spawn = new Spawn(tmpl);
			_flag2Spawn.setLoc(Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[0], Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[1], Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[2], Config.CTF_EVENT_TEAM_2_FLAG_COORDINATES[3]);
			_flag2Spawn.setRespawnDelay(60000);
			
			SpawnManager.getInstance().addSpawn(_flag2Spawn);
			
			_lastFlag2Spawn = _flag2Spawn.doSpawn(false);
			_lastFlag2Spawn.setTitle(Config.CTF_EVENT_TEAM_2_NAME);
		}
		catch (Exception e)
		{
			LOGGER.warn("SpawnSecondHeadQuaters: exception: " + e.getMessage(), e);
			return;
		}
	}
	
	private void unSpawnNpc()
	{
		// Delete the npc
		_lastNpcSpawn.deleteMe();
		SpawnManager.getInstance().deleteSpawn((Spawn) _lastNpcSpawn.getSpawn());
		_npcSpawn.doDelete();
		
		// Stop respawning of the npc
		_npcSpawn = null;
		_lastNpcSpawn = null;
		
		// Remove flags
		if (_lastFlag1Spawn != null)
		{
			_lastFlag1Spawn.deleteMe();
			_lastFlag2Spawn.deleteMe();
			SpawnManager.getInstance().deleteSpawn((Spawn) _lastFlag1Spawn.getSpawn());
			SpawnManager.getInstance().deleteSpawn((Spawn) _lastFlag2Spawn.getSpawn());
			_flag1Spawn = null;
			_flag2Spawn = null;
			_lastFlag1Spawn = null;
			_lastFlag2Spawn = null;
		}
	}
	
	public void onLogin(Player player)
	{
		if ((player == null) || (!isStarting() && !isStarted()))
			return;
		
		byte teamId = getParticipantTeamId(player.getObjectId());
		if (teamId == -1)
			return;
		
		_teams[teamId].addPlayer(player);
		new CTFEventTeleporter(player, _teams[teamId].getCoordinates(), true, false);
	}
	
	public void onLogout(Player player)
	{
		if ((player != null) && (isStarting() || isStarted() || isParticipating()))
		{
			if (removeParticipant(player.getObjectId()))
				player.teleportTo((Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)) - 50, (Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)) - 50, Config.CTF_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
		}
	}
	
	public synchronized void onBypass(String command, Player player)
	{
		if (player == null || !isParticipating())
			return;
		
		final String htmContent;
		
		if (command.equals("ctf_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = player.getStatus().getLevel();
			
			if (player.isCursedWeaponEquipped())
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (OlympiadManager.getInstance().isRegistered(player))
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "Olympiad.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (player.getKarma() > 0)
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "Karma.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if ((playerLevel < Config.CTF_EVENT_MIN_LVL) || (playerLevel > Config.CTF_EVENT_MAX_LVL))
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(Config.CTF_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(Config.CTF_EVENT_MAX_LVL));
				}
			}
			else if ((_teams[0].getParticipatedPlayerCount() == Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS) && (_teams[1].getParticipatedPlayerCount() == Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS))
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "TeamsFull.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(Config.CTF_EVENT_MAX_PLAYERS_IN_TEAMS));
				}
			}
			else if ((Config.CTF_EVENT_MAX_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.CTF_ID, player, Config.CTF_EVENT_MAX_PARTICIPANTS_PER_IP))
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "IPRestriction.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, Config.CTF_EVENT_MAX_PARTICIPANTS_PER_IP)));
				}
			}
			else if (!payParticipationFee(player))
			{
				htmContent = HTMLData.getInstance().getHtm(player, htmlPath + "ParticipationFee.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%fee%", getParticipationFee());
				}
			}
			else if (addParticipant(player))
				npcHtmlMessage.setHtml(HTMLData.getInstance().getHtm(player, htmlPath + "Registered.htm"));
			else
				return;
			
			player.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("ctf_event_remove_participation"))
		{
			removeParticipant(player.getObjectId());
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			npcHtmlMessage.setHtml(HTMLData.getInstance().getHtm(player, htmlPath + "Unregistered.htm"));
			player.sendPacket(npcHtmlMessage);
		}
	}
	
	public boolean onAction(Player player, int objectId)
	{
		if ((player == null) || !isStarted())
			return true;
		
		if (player.isGM())
			return true;
		
		byte playerTeamId = getParticipantTeamId(player.getObjectId());
		byte targetedPlayerTeamId = getParticipantTeamId(objectId);
		
		if (((playerTeamId != -1) && (targetedPlayerTeamId == -1)) || ((playerTeamId == -1) && (targetedPlayerTeamId != -1)))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if ((playerTeamId != -1) && (targetedPlayerTeamId != -1) && (playerTeamId == targetedPlayerTeamId) && (player.getObjectId() != objectId) && !Config.CTF_EVENT_TARGET_TEAM_MEMBERS_ALLOWED)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		return true;
	}
	
	public boolean onScrollUse(int objectId)
	{
		if (!isStarted())
			return true;
		
		return isPlayerParticipant(objectId) && !Config.CTF_EVENT_SCROLL_ALLOWED;
	}
	
	public boolean onPotionUse(int objectId)
	{
		if (!isStarted())
			return true;
		
		return isPlayerParticipant(objectId) && !Config.CTF_EVENT_POTIONS_ALLOWED;
	}
	
	public boolean onEscapeUse(int objectId)
	{
		if (!isStarted())
			return true;
		
		return isPlayerParticipant(objectId);
	}
	
	public boolean onItemSummon(int objectId)
	{
		if (!isStarted())
			return true;
		
		return isPlayerParticipant(objectId) && !Config.CTF_EVENT_SUMMON_BY_ITEM_ALLOWED;
	}
	
	public void onKill(Creature killer, Player player)
	{
		if (player == null || !isStarted() || getParticipantTeamId(player.getObjectId()) == -1)
			return;
		
		new CTFEventTeleporter(player, _teams[getParticipantTeamId(player.getObjectId())].getCoordinates(), false, false);
		
		Player attackingPlayer = null;
		if (killer instanceof Pet || killer instanceof Summon)
			attackingPlayer = ((Summon) killer).getOwner();
		else if (killer instanceof Player)
			attackingPlayer = (Player) killer;
		
		if (attackingPlayer != null)
		{
			byte killerTeamId = getParticipantTeamId(attackingPlayer.getObjectId());
			byte killedTeamId = getParticipantTeamId(player.getObjectId());
			if (killerTeamId != -1 && killedTeamId != -1 && killerTeamId != killedTeamId)
				sysMsgToAllParticipants(attackingPlayer.getName() + " Hunted Player " + player.getName() + "!");
		}
	}
	
	public void onTeleported(Player player)
	{
		if (!isStarted() || player == null || !isPlayerParticipant(player.getObjectId()))
			return;
		
		if (!_teleported.containsKey(player.getObjectId()) || !_teleported.get(player.getObjectId()))
			spawnProtection(player);
		
		Map<Integer, Integer> buffs = player.isMageClass() ? Config.CTF_EVENT_MAGE_BUFFS : Config.CTF_EVENT_FIGHTER_BUFFS;
		
		if (buffs == null || buffs.isEmpty())
			return;
		
		buffs.forEach((key, value) ->
		{
			L2Skill skill = SkillTable.getInstance().getInfo(key, value);
			if (skill != null)
				skill.getEffects(player, player);
		});
		
		if (player.getParty() != null)
		{
			Party party = player.getParty();
			party.removePartyMember(player, MessageType.LEFT);
		}
		
		player.stopAllEffectsDebuff();
		_teleported.put(player.getObjectId(), true);
	}
	
	private void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}
	
	public boolean isInactive()
	{
		boolean isInactive;
		
		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}
		
		return isInactive;
	}
	
	public boolean isParticipating()
	{
		boolean isParticipating;
		
		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}
		
		return isParticipating;
	}
	
	public boolean isStarting()
	{
		boolean isStarting;
		
		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}
		
		return isStarting;
	}
	
	public boolean isStarted()
	{
		boolean isStarted;
		
		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}
		
		return isStarted;
	}
	
	public byte getParticipantTeamId(int objectId)
	{
		if (_teams[0] == null || _teams[1] == null)
			return -1;
		
		return (byte) (_teams[0].containsPlayer(objectId) ? 0 : (_teams[1].containsPlayer(objectId) ? 1 : -1));
	}
	
	public CTFEventTeam getParticipantTeam(int objectId)
	{
		return (_teams[0].containsPlayer(objectId) ? _teams[0] : (_teams[1].containsPlayer(objectId) ? _teams[1] : null));
	}
	
	public CTFEventTeam getParticipantEnemyTeam(int objectId)
	{
		return (_teams[0].containsPlayer(objectId) ? _teams[1] : (_teams[1].containsPlayer(objectId) ? _teams[0] : null));
	}
	
	public int[] getParticipantTeamCoordinates(int objectId)
	{
		return _teams[0].containsPlayer(objectId) ? _teams[0].getCoordinates() : (_teams[1].containsPlayer(objectId) ? _teams[1].getCoordinates() : null);
	}
	
	public boolean isPlayerParticipant(int objectId)
	{
		if (!isParticipating() && !isStarting() && !isStarted())
			return false;
		
		return _teams[0].containsPlayer(objectId) || _teams[1].containsPlayer(objectId);
	}
	
	public int[] getTeamsPlayerCounts()
	{
		return new int[]
		{
			_teams[0].getParticipatedPlayerCount(),
			_teams[1].getParticipatedPlayerCount()
		};
	}
	
	public int[] getTeamsPoints()
	{
		return new int[]
		{
			_teams[0].getPoints(),
			_teams[1].getPoints()
		};
	}
	
	public void removeFlagCarrier(Player player)
	{
		if (player.getInventory().hasItemIn(Paperdoll.RHAND))
		{
			if (player.getInventory().hasItemIn(Paperdoll.RHAND))
				player.getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_R_HAND);
		}
		else
		{
			player.getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_LR_HAND);
			if (player.getInventory().hasItemIn(Paperdoll.LHAND))
				player.getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_L_HAND);
		}
		
		player.destroyItemByItemId(getEnemyTeamFlagId(player), 1, false);
		
		player.getInventory().unblock();
		
		final ItemInstance carrierRHand = _teams[0].containsPlayer(player.getObjectId()) ? _team1CarrierRHand : _team2CarrierRHand;
		final ItemInstance carrierLHand = _teams[0].containsPlayer(player.getObjectId()) ? _team1CarrierLHand : _team2CarrierLHand;
		if ((carrierRHand != null) && (player.getInventory().getItemByItemId(carrierRHand.getItemId()) != null))
			player.getInventory().equipItem(carrierRHand);
		
		if ((carrierLHand != null) && (player.getInventory().getItemByItemId(carrierLHand.getItemId()) != null))
			player.getInventory().equipItem(carrierLHand);
		
		setCarrierUnequippedWeapons(player, null, null);
		
		if (_teams[0].containsPlayer(player.getObjectId()))
			_team1Carrier = null;
		else
			_team2Carrier = null;
		
		player.broadcastUserInfo();
	}
	
	public void setTeamCarrier(Player player)
	{
		if (_teams[0].containsPlayer(player.getObjectId()))
			_team1Carrier = player;
		else
			_team2Carrier = player;
	}
	
	public Player getTeamCarrier(Player player)
	{
		if (((_teams[0].containsPlayer(player.getObjectId()) == true) && (_team1Carrier != null) && (!_team1Carrier.isOnline() || ((_teams[1].containsPlayer(player.getObjectId()) == true) && (_team2Carrier != null) && (!_team2Carrier.isOnline())))))
		{
			player.destroyItemByItemId(getEnemyTeamFlagId(player), 1, false);
			return null;
		}
		
		return (_teams[0].containsPlayer(player.getObjectId()) ? _team1Carrier : _team2Carrier);
	}
	
	public Player getEnemyCarrier(Player player)
	{
		if (((_teams[0].containsPlayer(player.getObjectId()) == true) && (_team2Carrier != null) && (!_team2Carrier.isOnline() || ((_teams[1].containsPlayer(player.getObjectId()) == true) && (_team1Carrier != null) && (!_team1Carrier.isOnline())))))
		{
			player.destroyItemByItemId(getEnemyTeamFlagId(player), 1, false);
			return null;
		}
		
		return (_teams[0].containsPlayer(player.getObjectId()) ? _team2Carrier : _team1Carrier);
	}
	
	public boolean playerIsCarrier(Player player)
	{
		return ((player == _team1Carrier) || (player == _team2Carrier)) ? true : false;
	}
	
	public int getEnemyTeamFlagId(Player player)
	{
		return (_teams[0].containsPlayer(player.getObjectId()) ? Config.CTF_EVENT_TEAM_2_FLAG : Config.CTF_EVENT_TEAM_1_FLAG);
	}
	
	public void setCarrierUnequippedWeapons(Player player, ItemInstance itemRight, ItemInstance itemLeft)
	{
		if (_teams[0].containsPlayer(player.getObjectId()))
		{
			_team1CarrierRHand = itemRight;
			_team1CarrierLHand = itemLeft;
		}
		else
		{
			_team2CarrierRHand = itemRight;
			_team2CarrierLHand = itemLeft;
		}
	}
	
	public void broadcastScreenMessage(String message, int duration)
	{
		for (CTFEventTeam team : _teams)
		{
			for (Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
					player.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
		}
	}
	
	public static final CTFEvent getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CTFEvent INSTANCE = new CTFEvent();
	}
}