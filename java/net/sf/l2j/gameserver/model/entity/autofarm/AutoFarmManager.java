package net.sf.l2j.gameserver.model.entity.autofarm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.entity.autofarm.zone.AutoFarmArea;
import net.sf.l2j.gameserver.model.entity.autofarm.zone.AutoFarmRoute;
import net.sf.l2j.gameserver.model.entity.autofarm.zone.AutoFarmZone;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.TownZone;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive.Point;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.L2Skill;
/**
 * TODO:
 * 1) It is necessary to implement parameter saving, so that all settings are preserved after the server restarts.
 * 2) Change all text to player.getSysString().
 * 3) Impl. offline farm. ? (if yes, add config USE_ONLY_FOR_PREMIUM.
 * 4) All setting move to Config (XML).
 */
public class AutoFarmManager
{
	public static final int MAX_ROUTE_LINE_LENGTH = 1500;
	
	private static final CLogger LOGGER = new CLogger(AutoFarmManager.class.getName());
	
	private static final String HTML_PATH = "html/mods/autofarm/";
	private static final String ZONE_NAME_REGEX = "^[A-Za-z0-9]{1,12}$";
	
	private final Map<Integer, AutoFarmProfile> _players = new ConcurrentHashMap<>();
	
	public enum AutoFarmType
	{
		ZONA,
		ROTA,
		OPEN;
	}
	
	public enum AutoFarmMacro
	{
		ESCAPE,
		LOGOUT,
		ITEM,
		SKILL;
	}
	
	public Collection<AutoFarmProfile> getPlayers()
	{
		return _players.values();
	}
	
	public AutoFarmProfile getPlayer(int id)
	{
		return _players.get(id);
	}
	
	public AutoFarmProfile getProfile(Player player)
	{
		return _players.computeIfAbsent(player.getObjectId(), k -> new AutoFarmProfile(player));
	}
	
	public boolean isPlayerAddingLoc(int playerId)
	{
		return _players.containsKey(playerId) && getPlayer(playerId).isAddingLocation();
	}
	
	public boolean isPlayerActive(int playerId)
	{
		return _players.containsKey(playerId) && getPlayer(playerId).isEnabled();
	}
	
	public boolean isRouteFarmActive(int playerId)
	{
		return _players.containsKey(playerId) && getPlayer(playerId).isEnabled() && getPlayer(playerId).getSelectedArea().getType() == AutoFarmType.ROTA;
	}
	
	public void onPlayerLogin(Player player)
	{
		if (getPlayer(player.getObjectId()) != null)
		{
			final AutoFarmProfile autoFarmProfile = getProfile(player);
			autoFarmProfile.updatePlayer(player);
			
			if (!autoFarmProfile.getAreas().isEmpty())
				return;
		}
		
		AutoFarmData.getInstance().restorePlayer(player);
	}
	
	public void handleBypass(Player player, String bypass)
	{
		try
		{
			if (!Config.AUTOFARM_ENABLED)
			{
				showIndexWindow(player, player.getSysString(10_200));
				return;
			}
			
			if (player.isDead())
				return;

			final StringTokenizer st = new StringTokenizer(bypass, " ");
			if (!st.hasMoreTokens())
				return;
			
			final String command = st.nextToken();
			final AutoFarmProfile autoFarmProfile = getProfile(player);

			if (!bypass.startsWith("zb addnode") && !bypass.startsWith("zb lock_new_nodes"))
				autoFarmProfile.setAddingLocation(false);
			
			if (command.equals("index"))
			{
				autoFarmProfile.checkLastClassId();
				showIndexWindow(player, null);
			}
			else if (command.equals("list_areas"))
				handleListAreasBypass(player, autoFarmProfile, st);
			else if (command.equals("skills"))
				handleSkillsBypass(player, autoFarmProfile, st);
			else if (command.equals("targets"))
				handleTargetsBypass(player, autoFarmProfile, st);
			else if (command.equals("macro"))
				handleMacroBypass(player, autoFarmProfile, st);
			else if (command.equals("zb"))
				handleZoneBuilderBypass(player, autoFarmProfile, st);
			else if (command.equals("select_area")) 
				handleSelectAreaBypass(player, autoFarmProfile, Integer.valueOf(st.nextToken()));
			else if (command.equals("timer"))
				handleTimerBypass(player, autoFarmProfile, st);
			else if (command.equals("options"))
				handleAreaOptionsBypass(player, autoFarmProfile, st);
			else if (command.equals("toggle"))
				handleToggleBypass(player, autoFarmProfile, st.nextToken());
			else if (command.equals("clearpreview"))
			{
				if (!autoFarmProfile.isEnabled())
					ZoneBuilder.getInstance().clearAllPreview(player);
			}
			else if (command.equals("help"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setFile(player.getLocale(), HTML_PATH + "help/" + st.nextToken());
				player.sendPacket(html);
			}
			else if (command.equals("change_status"))
			{
				if (autoFarmProfile.isEnabled())
					stopPlayer(player, null);
				else
					startPlayer(player);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to handle AutoFarmManager bypass. Player: {}, Bypass: {}", e, player.getName(), bypass);
		}
	}
	
	private void handleToggleBypass(Player player, AutoFarmProfile autoFarmProfile, String action)
	{
		String msg = null;
		
		if (action.equals("autopotion"))
			autoFarmProfile.toggleAutoPotion();
		else if (action.equals("attackraid"))
			autoFarmProfile.toggleAttackRaid();
		else if (action.equals("attacksummon"))
			autoFarmProfile.toggleAttackSummon();
		else if (action.equals("pickherbs"))
		{
			if (autoFarmProfile.getSelectedArea() != null && !autoFarmProfile.pickHerbs() && !autoFarmProfile.getSelectedArea().isMovementAllowed())
			{
				if (autoFarmProfile.getSelectedArea().getType() == AutoFarmType.ROTA)
					msg = "Option unavailable for routes.";
				else
					msg = "Incompatible with your range and the area radius.";
			}
			
			if (msg == null)
				autoFarmProfile.togglePickHerbs();
		}
		
		showIndexWindow(player, msg);
	}
	
	private void handleTimerBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st)
	{
		if (st.hasMoreTokens())
		{
			final String action = st.nextToken();
			if (action.equals("set"))
			{
				final int value = st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0;
				if (value < 0)
					return;
				
				if (Config.AUTOFARM_MAX_TIMER != 0 && (value == 0 || value > Config.AUTOFARM_MAX_TIMER))
				{
					showTimerWindow(player, autoFarmProfile, "Choose a value between 1 and " + Config.AUTOFARM_MAX_TIMER);
					return;
				}
				
				autoFarmProfile.setEndTime(value > 0 ? TimeUnit.MINUTES.toMillis(value) : value);
			}
		}
		
		showTimerWindow(player, autoFarmProfile, null);
	}
	
	private void handleAreaOptionsBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st)
	{
		String msg = null;
		if (st.hasMoreTokens())
		{
			if (autoFarmProfile.getSelectedArea() == null)
				msg = "Choose your area first.";
			else if (autoFarmProfile.getSelectedArea().getType() == AutoFarmType.ZONA)
				msg = "Your current area is incompatible with this option.";
			else
			{
				final String action = st.nextToken();
				if (action.equals("radius"))
				{
					final int range = Integer.valueOf(st.nextToken());
					if (range >= 100 && range <= autoFarmProfile.getAreaMaxRadius())
						autoFarmProfile.setRadius(range);
				}
				
				if (!autoFarmProfile.isEnabled())
					ZoneBuilder.getInstance().previewCylinder(player, autoFarmProfile.getFinalRadius());
			}
		}
		
		final Set<Integer> options = new LinkedHashSet<>();
		
		// Always display the selected option at the top.
		options.add(autoFarmProfile.getFinalRadius());

		// Add the multiples of 100.
		final int maxRange = autoFarmProfile.getAreaMaxRadius();
		for (int i = 100; i <= (maxRange / 100) * 100; i += 100)
			options.add(i);

		// If the value is not divisible by 100, add the remainder.
		if (maxRange % 100 != 0)
			options.add(maxRange);

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "arealist_options.htm"); 
		html.replace("%current_radius%", autoFarmProfile.getFinalRadius());
		html.replace("%attack_range%", autoFarmProfile.getAttackRange());
		html.replace("%radius%", options.stream().map(String::valueOf).collect(Collectors.joining(";")));
		html.replace("%msg%", msg != null ? msg + "<br>" : "");
		player.sendPacket(html);
	}
	
	private void handleSelectAreaBypass(Player player, AutoFarmProfile autoFarmProfile, int areaId)
	{
		if (autoFarmProfile.getAreaById(areaId) == null)
			return;
		
		if (autoFarmProfile.isEnabled())
		{
			showIndexWindow(player, "Action unavailable at the moment.");
			return;
		}
		
		final AutoFarmArea area = autoFarmProfile.getAreaById(areaId);
		
		if (autoFarmProfile.getSelectedAreaId() == areaId)
		{
			autoFarmProfile.setSelectedAreaId(0);
			ZoneBuilder.getInstance().clearAllPreview(player);
			showAreaListWindow(player, area.getType());
			return;
		}
		
		if (area.getType() != AutoFarmType.OPEN && !area.isFromDb())
		{
			showAreaListWindow(player, area.getType(), "Invalid zone.");
			return;
		}
		
		if (!canSeeArea(player, area))
		{
			showAreaListWindow(player, area.getType(), "Area too far to be used.");
			return;
		}
		
		// Update variables first.
		autoFarmProfile.setSelectedAreaId(areaId);
		
		// We need to do this first so that the list of targets is ready when the player accesses it.
		if (area.getType() == AutoFarmType.ZONA && !area.getFarmZone().isBuilt())
			area.getFarmZone().addToWorld();
		
		if (area.getType() == AutoFarmType.OPEN)
			ZoneBuilder.getInstance().previewCylinder(player, autoFarmProfile.getFinalRadius());
		else
			ZoneBuilder.getInstance().previewFinalArea(autoFarmProfile, areaId);
		
		showAreaListWindow(player, area.getType());
	}
	
	private void handleListAreasBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st) throws IllegalArgumentException
	{
		if (autoFarmProfile.getBuildingAreaId() != 0 && autoFarmProfile.getBuildingArea().isChanged())
		{
			showConfirmChangeWindow(player, autoFarmProfile.getBuildingArea());
			return;
		}
		
		AutoFarmType type = null;
		if (st.hasMoreTokens())
			type = AutoFarmType.valueOf(st.nextToken());
		else if (autoFarmProfile.getSelectedArea() != null)
			type = autoFarmProfile.getSelectedArea().getType();
		else
			type = AutoFarmType.ZONA;
		
		showAreaListWindow(player, type);
	}
	
	private void handleMacroBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st) throws NumberFormatException, IllegalArgumentException, NoSuchElementException
	{
		if (st.hasMoreTokens())
		{
			final String action = st.nextToken();
			if (autoFarmProfile.getEndTime() == 0)
			{
				showMacroWindow(player, autoFarmProfile, "<font color=FFA000>Macros can be used when a duration is specified for AutoFarm.</font>");
			}
			else if (action.equals("set"))
			{
				final AutoFarmMacro macro = AutoFarmMacro.valueOf(st.nextToken());
				if (autoFarmProfile.getMacro() == macro)
					autoFarmProfile.setMacro(null, 0);
				else
					autoFarmProfile.setMacro(macro, Integer.valueOf(st.nextToken()));
				
				showMacroWindow(player, autoFarmProfile, null);
			}
			else if (action.equals("list"))
			{
				final AutoFarmMacro type = AutoFarmMacro.valueOf(st.nextToken());
				if (type == AutoFarmMacro.ITEM)
					showMacroItemWindow(player, Integer.valueOf(st.nextToken()));
				else if (type == AutoFarmMacro.SKILL)
					showMacroSkillWindow(player, Integer.valueOf(st.nextToken()));
			}
		}
		else
			showMacroWindow(player, autoFarmProfile, null);
	}
	
	private void handleZoneBuilderBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st) throws NumberFormatException
	{
		if (!st.hasMoreTokens())
			return;
		
		if (autoFarmProfile.isEnabled())
		{
			showIndexWindow(player, "Action currently unavailable.");
			return;
		}
		
		final String action = st.nextToken();
		if (action.equals("new"))
		{
			createArea(player, autoFarmProfile, st);
		}
		else if (action.equals("addnode"))
		{
			newNodeMode(player, autoFarmProfile, false);
		}
		else if (action.equals("lock_new_nodes"))
		{
			newNodeMode(player, autoFarmProfile, true);
		}
		else if (action.equals("removenode"))
		{
			removeAreaNode(player, autoFarmProfile, Integer.valueOf(st.nextToken()));
		}
		else if (action.equals("clearnodes"))
		{
			if (autoFarmProfile.getBuildingArea() == null || ZoneBuilder.getInstance().getPoints(player).isEmpty() && autoFarmProfile.getBuildingArea().getNodes().isEmpty())
				return;
			
			// Something prevents the preview from being cleared at this moment, probably some concurrency issue
			// This alternative was the simplest to fix
			ThreadPool.execute(() -> ZoneBuilder.getInstance().clearAllPreview(player));

			// The HTML can be displayed before the completion of the task above, so it's important that it shows the empty list
			ZoneBuilder.getInstance().getPoints(player).clear();
			
			autoFarmProfile.getBuildingArea().setIsChanged(true);
			showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), null);
		}
		else if (action.equals("save"))
		{
			saveArea(player, autoFarmProfile);
		}
		else if (action.equals("discart"))
		{
			final AutoFarmArea area = autoFarmProfile.getBuildingArea();
			if (area == null || !area.isChanged())
				return;
			
			area.setIsChanged(false);
			ZoneBuilder.getInstance().clearAllPreview(player);
			showAreaListWindow(player, area.getType());
		}
		else
		{
			final int areaId = Integer.valueOf(st.nextToken());
			if (autoFarmProfile.getAreaById(areaId) == null)
				return;
			
			if (action.equals("delete"))
			{
				deleteArea(player, autoFarmProfile, areaId);
			}
			else if (action.equals("edit"))
			{
				if (autoFarmProfile.getBuildingAreaId() != areaId)
					ZoneBuilder.getInstance().clearAllPreview(player);
				
				autoFarmProfile.setBuildingAreaId(areaId);
				showEditAreaWindow(player, areaId, null);
			}
			else if (action.equals("preview"))
			{
				previewArea(player, autoFarmProfile, areaId, st.hasMoreTokens() ? st.nextToken().equals("FINAL") : false);
			}
		}
	}
	
	private void handleSkillsBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st) throws NumberFormatException, NoSuchElementException
	{
		if (!st.hasMoreTokens())
			return;
		
		final String action = st.nextToken();
		if (action.equals("page"))
		{
			final int page = Integer.valueOf(st.nextToken());
			if (page == 1 && st.hasMoreTokens())
				autoFarmProfile.setCurrentSkillSlot(Integer.valueOf(st.nextToken()));
			
			showSkillsWindow(player, autoFarmProfile, page);
			return;
		}
		
		final Map<Integer, Integer> skills = autoFarmProfile.getSkills();
		if (action.equals("clear"))
			skills.clear();
		else
		{
			final int skillId = Integer.valueOf(st.nextToken());
			
			if (action.equals("add"))
			{
				if (player.getSkill(skillId) == null)
					return;
				
				if (skills.size() == 6)
				{
					showIndexWindow(player, "Maximum number of skills reached.");
					return;
				}
				
				if (skills.get(autoFarmProfile.getCurrentSkillSlot()) != null)
				{
					final List<Integer> slots = new ArrayList<>();
					for (int i = 1; i <= 6; i++)
					{
			            if (!skills.containsKey(i))
			            	slots.add(i);
			        }
					
					autoFarmProfile.setCurrentSkillSlot(slots.get(0));
				}
				
				skills.put(autoFarmProfile.getCurrentSkillSlot(), skillId);
			}
			else if (action.equals("remove"))
			{
				final int slot = skills.entrySet().stream().filter(m -> m.getValue() == skillId).map(Entry::getKey).findFirst().orElse(-1);
				if (slot < 0)
					return;
						
				skills.remove(slot);
			}
		}
		
		showSkillsWindow(player, autoFarmProfile, st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 1);
	}
	
	private void handleTargetsBypass(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st) throws NumberFormatException
	{
		if (!st.hasMoreTokens())
			return;
		
		if (autoFarmProfile.getSelectedAreaId() == 0)
		{
			showIndexWindow(player, "Choose an area first.");
			return;
		}
		
		final String action = st.nextToken();
		if (action.equals("page") && st.hasMoreTokens())
		{
			showTargetsWindow(player, autoFarmProfile, Integer.valueOf(st.nextToken()));
			return;
		}
		
		if (action.equals("clear"))
			autoFarmProfile.getTargets().clear();
		else
		{
			final String targetName = Util.getRemainingTokens(st);
			if (targetName.isEmpty())
				return;
			
			if (action.equals("add"))
				autoFarmProfile.getTargets().add(targetName);
			else if (action.equals("remove"))
				autoFarmProfile.getTargets().remove(targetName);
		}
		
		showTargetsWindow(player, autoFarmProfile, 1);
	}
	
	
	private void showMacroItemWindow(Player player, int page)
	{
		final StringBuilder sb = new StringBuilder();
		final Pagination<ItemInstance> pagination = new Pagination<>(player.getInventory().getItems().stream(), page, 7, i -> i.isEtcItem() && i.getEtcItem().getHandlerName() != null);
		
		for (ItemInstance item : pagination)
		{
			String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/macro_table.htm");
			table = table.replace("%icon%", item.getItem().getIcon());
			table = table.replace("%id%", String.valueOf(item.getItemId()));
			table = table.replace("%name%", item.getName());
			sb.append(table);
		}
		
		if (sb.isEmpty())
			sb.append("<tr><td width=280 align=center><br>No compatible item available</td></tr>");
		if (pagination.totalEntries() > 7)
		{
			pagination.generateSpace(36);
			pagination.generatePages("bypass autofarm macro list ITEM %page%");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "macro_list.htm");
		html.replace("%list%", sb.toString());
		html.replace("%pagination%", pagination.getContent());
		html.replace("%type%", "ITEM");
		player.sendPacket(html);
	}
	
	private void showMacroSkillWindow(Player player, int page)
	{
		final StringBuilder sb = new StringBuilder();
		final Pagination<L2Skill> pagination = new Pagination<>(player.getSkills().values().stream(), page, 7, s -> !s.isPassive() && !s.isSkillTypeOffensive());
		
		for (L2Skill skill : pagination)
		{
			final int skillId = skill.getId();
			String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/macro_table.htm");
			table = table.replace("%icon%", skill.getIcon());
			table = table.replace("%id%", String.valueOf(skillId));
			table = table.replace("%name%", skill.getName());
			sb.append(table);
		}
		
		if (sb.isEmpty())
			sb.append("<tr><td width=280 align=center><br>No compatible skill available</td></tr>");
		
		if (pagination.totalEntries() > 7)
		{
			pagination.generateSpace(36);
			pagination.generatePages("bypass autofarm macro list SKILL %page%");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "macro_list.htm");
		html.replace("%list%", sb.toString());
		html.replace("%pagination%", pagination.getContent());
		html.replace("%type%", "SKILL");
		player.sendPacket(html);
	}
	
	private static void showMacroWindow(Player player, AutoFarmProfile autoFarmProfile, String msg)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "macro.htm");
		
		for (AutoFarmMacro macro : AutoFarmMacro.values())
		{
			if (macro == autoFarmProfile.getMacro())
			{
				if (macro == AutoFarmMacro.ITEM)
				{
					html.replace("%item_bypass%", "set ITEM 0");
					html.replace("%item_name%", "(" + ItemData.getInstance().getTemplate(autoFarmProfile.getMacroAdditionalId()).getName() + ")");
				}
				else if (macro == AutoFarmMacro.SKILL)
				{
					html.replace("%skill_bypass%", "set SKILL 0");
					html.replace("%skill_name%", "(" + player.getSkill(autoFarmProfile.getMacroAdditionalId()).getName() + ")");
				}
			}
			html.replace("%" + macro.name() + "%", macro == autoFarmProfile.getMacro() ? "checked" : "unable");
		}
		
		html.replace("%msg%", msg != null ? msg + "<br>" : "");
		html.replace("%skill_bypass%", "list SKILL 1");
		html.replace("%item_bypass%", "list ITEM 1");
		html.replace("%item_name%", "");
		html.replace("%skill_name%", "");
		player.sendPacket(html);
	}
	
	private void showTargetsWindow(Player player, AutoFarmProfile autoFarmProfile, int page)
	{
		if (autoFarmProfile.getSelectedArea() == null)
		{
			showIndexWindow(player, "Choose an area first.");
			return;
		}
		
		// Due to the way HTML is structured, we cannot use pagination to save the list.
		final StringBuilder sb = new StringBuilder();
		final Pagination<String> pagination = new Pagination<>(autoFarmProfile.getSelectedArea().getMonsterHistory().stream(), page, 6, Util.getMonsterComparator(autoFarmProfile.getTargets()));
		
		for (String monster : pagination)
		{
			String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/targets_table.htm");
			if (autoFarmProfile.getTargets().contains(monster))
			{
				table = table.replace("%action%", "remove");
				table = table.replace("%button_back%", "L2UI_CH3.mapbutton_zoomout2");
				table = table.replace("%button_fore%", "L2UI_CH3.mapbutton_zoomout1");
			}
			else
			{
				table = table.replace("%action%", "add");
				table = table.replace("%button_back%", "L2UI_CH3.mapbutton_zoomin2");
				table = table.replace("%button_fore%", "L2UI_CH3.mapbutton_zoomin1");
			}
			
			table = table.replace("%npc_name%", monster);
			sb.append(table);
		}
		
		if (sb.isEmpty())
			sb.append("<tr><td width=280 align=center>Empty</td></tr>");
		else if (pagination.totalEntries() > 6)
		{
			pagination.generateSpace(36);
			pagination.generatePages("bypass autofarm targets page %page%");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "targets.htm");
		html.replace("%list%", sb.toString());
		html.replace("%pagination%", pagination.getContent());
		html.replace("%target_count%", autoFarmProfile.getTargets().size());
		html.replace("%area%", autoFarmProfile.getSelectedArea().getType().name());
		html.replace("%area_name%", autoFarmProfile.getSelectedArea().getName());
		player.sendPacket(html);
	}
	
	private void showSkillsWindow(Player player, AutoFarmProfile autoFarmProfile, int page)
	{
		final StringBuilder sb = new StringBuilder();
		final Pagination<L2Skill> pagination = new Pagination<>(player.getSkills().values().stream(), page, 7, s -> Util.isSkillCompatible(s), Util.getSkillComparator(autoFarmProfile.getSkills().values()));
		
		for (L2Skill skill : pagination)
		{
			final int skillId = skill.getId();

			String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/skills_table.htm");
			if (autoFarmProfile.getSkills().values().contains(skill.getId()))
			{
				table = table.replace("%action%", "remove");
				table = table.replace("%button_back%", "L2UI_CH3.mapbutton_zoomout2");
				table = table.replace("%button_fore%", "L2UI_CH3.mapbutton_zoomout1");
			}
			else
			{
				table = table.replace("%action%", "add");
				table = table.replace("%button_back%", "L2UI_CH3.mapbutton_zoomin2");
				table = table.replace("%button_fore%", "L2UI_CH3.mapbutton_zoomin1");
			}
			
			final int skillRange = skill.getCastRange() > 0 ? skill.getCastRange() : skill.getSkillRadius();
			table = table.replace("%skill_icon%", skill.getIcon());
			table = table.replace("%skill_id%", String.valueOf(skillId));
			table = table.replace("%skill_name%", skill.getName());
			table = table.replace("%skill_type%", skill.isDebuff() ? "Debuff" : skill.isSkillTypeOffensive() ? "Attack" : "Buff");
			table = table.replace("%skill_range%", skill.isOffensive() && skillRange > 0 ? " - " + skillRange : "");
			sb.append(table);
		}
		
		if (pagination.totalEntries() > 6)
		{
			pagination.generateSpace(36);
			pagination.generatePages("bypass autofarm skills page %page%");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "skills.htm");
		html.replace("%list%", sb.toString());
		html.replace("%msg%", "Selecionadas: " + autoFarmProfile.getSkills().size());
		html.replace("%pagination%", pagination.getContent());
		html.replace("%page%", page);
		player.sendPacket(html);
	}
	
	public void showIndexWindow(Player player)
	{
		handleBypass(player, "index");
	}
	
	private void showIndexWindow(Player player, String msg)
	{
		final AutoFarmProfile autoFarmProfile = getProfile(player);
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "index.htm");
		
		boolean showingSkill = false;
		for (int slot = 0; slot < 6; slot++)
		{
			if (autoFarmProfile.getSkills().containsKey(slot))
			{
				showingSkill = true;
				final int skillId = autoFarmProfile.getSkills().get(slot);
				L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
				
				html.replace("%1_negative_height%" + slot, -2);
				html.replace("%skill_icon%" + slot, skill.getIcon());
				html.replace("%skill_width%" + slot, 31);
				html.replace("%skill_height%" + slot, 32);
				html.replace("%2_negative_height%" + slot, -48);
				html.replace("%bg_icon%" + slot, "L2UI_CH3.refineslot2");
				html.replace("%bg_width%" + slot, 36);
				html.replace("%bg_height%" + slot, 48);
			}
			else
			{
				html.replace("%1_negative_height%" + slot, -12);
				html.replace("%skill_icon%" + slot, "L2UI_CH3.refineslot2");
				html.replace("%skill_width%" + slot, 36);
				html.replace("%skill_height%" + slot, 48);
				html.replace("%2_negative_height%" + slot, showingSkill ? -48 : -42);
				html.replace("%bg_icon%" + slot, "L2UI_CH3.multisell_plusicon");
				html.replace("%bg_width%" + slot, 31);
				html.replace("%bg_height%" + slot, 30);
			}
		}
		
		if (autoFarmProfile.isEnabled())
		{
			html.replace("%remaining_time%", autoFarmProfile.getEndTime() != 0 ? (TimeUnit.MILLISECONDS.toMinutes(autoFarmProfile.getFinalEndTime() - System.currentTimeMillis()) + " min") : "0 min");
			html.replace("%end_time%", autoFarmProfile.getEndTime() != 0 ? new SimpleDateFormat("HH:mm:ss").format(autoFarmProfile.getFinalEndTime()) : "00:00:00");
			html.replace("%start_time%", new SimpleDateFormat("HH:mm:ss").format(autoFarmProfile.getStartTime()));
			html.replace("%status_color%", "00FF00");
			html.replace("%status%", "ON");
		}
		else
		{
			html.replace("%remaining_time%", "0 min");
			html.replace("%end_time%", "00:00:00");
			html.replace("%start_time%", "00:00:00");
			html.replace("%status_color%", "FF0000");
			html.replace("%status%", "OFF");
		}
		
		html.replace("%area%",  autoFarmProfile.getSelectedAreaId() != 0 ? StringUtil.trimAndDress(autoFarmProfile.getSelectedArea().getName(), 10) : "define");
		html.replace("%targets%", !autoFarmProfile.getTargets().isEmpty() ? String.valueOf(autoFarmProfile.getTargets().size()) : "define");
		html.replace("%duration%", autoFarmProfile.getEndTime() != 0 ? TimeUnit.MILLISECONDS.toMinutes(autoFarmProfile.getEndTime()) + " m" : "define");
		html.replace("%macro%", autoFarmProfile.getMacro() != null ? autoFarmProfile.getMacro().toString().toLowerCase() : "define");
		html.replace("%autopotion%", autoFarmProfile.useAutoPotion() ? "checked" : "unable");
		html.replace("%attack_raid%", autoFarmProfile.attackRaid() ? "checked" : "unable");
		html.replace("%attack_summon%", autoFarmProfile.attackSummon() ? "checked" : "unable");
		html.replace("%pickherbs%", autoFarmProfile.pickHerbs() ? "checked" : "unable");
		html.replace("%alert%", msg != null ? HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/index_alert.htm").replace("%msg%", msg) : "");
		html.replace("%fix%", showingSkill ? "<img height=\"-6\">" : "");
		html.replace("%optional%", player.isMageClass() ? "" : "(optional)");
		player.sendPacket(html);
	}
	
	public void showZoneWindow(Player player)
	{
		showEditAreaWindow(player, getProfile(player).getBuildingAreaId(), null);
	}
	
	private void showEditAreaWindow(Player player, int areaId, String msg)
	{
		final AutoFarmArea area = getProfile(player).getAreaById(areaId);
		if (area == null)
			return;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "areaedit.htm");
		
		if (ZoneBuilder.getInstance().getPoints(player).isEmpty() && (area.isChanged() || area.getNodes().isEmpty()))
		{
			String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/areaedit_table.htm");
			table = table.replaceAll("%node%", "0");
			table = table.replace("%x%", "0");
			table = table.replace("%y%", "0");
			table = table.replace("%z%", "0");
			html.replace("%list%", table);
		}
		else
		{
			final StringBuilder sb = new StringBuilder();
			if (ZoneBuilder.getInstance().getPoints(player).isEmpty())
			{
				int indice = 0;
				for (Location loc : area.getNodes())
				{
					indice++;
					String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/areaedit_table.htm");
					table = table.replaceAll("%node%", String.valueOf(indice));
					table = table.replace("%x%", String.valueOf(loc.getX()));
					table = table.replace("%y%", String.valueOf(loc.getY()));
					table = table.replace("%z%", String.valueOf(loc.getZ()));
					sb.append(table);
				}
			}
			else
			{
				for (Entry<Integer, Point> entry : ZoneBuilder.getInstance().getPoints(player).entrySet())
				{
					String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/areaedit_table.htm");
					table = table.replaceAll("%node%", String.valueOf(entry.getKey()));
					table = table.replace("%x%", String.valueOf(entry.getValue().getX()));
					table = table.replace("%y%", String.valueOf(entry.getValue().getY()));
					table = table.replace("%z%", String.valueOf(entry.getValue().getZ()));
					sb.append(table);
				}
			}
			html.replace("%list%", sb.toString());
		}
		
		final AutoFarmProfile profile = getProfile(player);
		html.replace("%checkbox%", profile.getSelectedAreaId() == areaId ? "checked" : "unable");
		html.replace("%name%", area.getName());
		html.replace("%id%", areaId);
		html.replace("%msg%", msg != null ? msg + "<br>" : "");
		html.replace("%type%", area.getType().name());
		html.replace("%lock%", profile.isAddingLocationLocked() ? "locked" : "lock");
		html.replace("%adding%", profile.isAddingLocation() ? "-" : "+");
		player.sendPacket(html);
	}
	
	private void showAreaListWindow(Player player, AutoFarmType type)
	{
		showAreaListWindow(player, type, null);
	}
	
	private void showAreaListWindow(Player player, AutoFarmType type, String msg)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		final AutoFarmProfile autoFarmProfile = getProfile(player);
		
		if (type == AutoFarmType.OPEN)
		{
			html.setFile(player.getLocale(), HTML_PATH + "arealist_open.htm");
			html.replace("%radius%", Math.max(0, autoFarmProfile.getFinalRadius()));
			html.replace("%checkbox%", autoFarmProfile.getSelectedAreaId() == 1 ? "checked" : "unable");
		}
		else
		{
			final List<AutoFarmArea> areas = getProfile(player).getAreas().values().stream().filter(a -> a.getType() == type).sorted(Comparator.comparing(AutoFarmArea::getName)).toList();
			html.setFile(player.getLocale(), HTML_PATH + (type == AutoFarmType.ZONA ? "arealist_zone.htm" : "arealist_route.htm"));
			
			if (areas.isEmpty())
			{
				html.replace("%list%", "empty");
			}
			else
			{
				final StringBuilder sb = new StringBuilder();
				for (AutoFarmArea area : areas)
				{
					String table = HTMLData.getInstance().getHtm(player.getLocale(), HTML_PATH + "parts/arealist_table.htm");
					table = table.replaceAll("%id%", String.valueOf(area.getId()));
					table = table.replace("%name%", area.getName());
					table = table.replaceAll("%checkbox%", autoFarmProfile.getSelectedAreaId() == area.getId() ? "checked" : "unable");
					sb.append(table);
				}
				html.replace("%list%", sb.toString());
			}
		}
		
		html.replace("%msg%", msg != null ? "<font color=FFA000>" + msg + "</font><br>": "");
		player.sendPacket(html);
	}
	
	private void showTimerWindow(Player player, AutoFarmProfile autoFarmProfile, String msg)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + "timer.htm");
		html.replace("%msg%", msg != null ? "<font color=FFA000>" + msg + "</font><br>": "");
		html.replace("%current%", autoFarmProfile.getEndTime() == 0 ? "" : TimeUnit.MILLISECONDS.toMinutes(autoFarmProfile.getEndTime()) + " minute(s)<br>");
		player.sendPacket(html);
	}
	
	private void showConfirmChangeWindow(Player player, AutoFarmArea area)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player.getLocale(), HTML_PATH + (area.isFromDb() ? "confirm_change.htm" : "confirm_save.htm")); 
		html.replace("%type%", area.getType().toString());
		html.replace("%name%", area.getName());
		html.replace("%id%", area.getId());
		player.sendPacket(html);
	}
	
	private boolean canSeeArea(Player player, AutoFarmArea area)
	{
		if (area.getType() == AutoFarmType.OPEN)
			return true;
		
		if (area.getZone() == null && area.isFromDb())
			ZoneBuilder.getInstance().setAutoFarmAreaZone(player, area);
		
		if (area.getNodes().stream().anyMatch(n -> GeoEngine.getInstance().canMoveToTarget(player, n)))
			return true;
		
		return area.isOwnerNearOrInside(500);
	}
	
	private void startPlayer(Player player)
	{
		if (player.denyAiAction() || player.getCast().isCastingNow())
			return;
		
		if (Config.AUTOFARM_DISABLE_TOWN && player.isInsideZone(ZoneId.TOWN))
		{
			showIndexWindow(player, "It is not possible to start in the city");
			return;
		}
		
		final AutoFarmProfile autoFarmProfile = getProfile(player);
		if (autoFarmProfile.getSelectedAreaId() == 0)
		{
			showIndexWindow(player, "It is necessary to select an area.");
			return;
		}
		
		if (Config.AUTOFARM_MAX_TIMER != 0 && autoFarmProfile.getEndTime() == 0)
		{
			showIndexWindow(player, "It is necessary to set a duration.");
			return;
		}
		
		final String playerIp = player.getClient().getConnection().getInetAddress().getHostAddress();
		if (!Config.AUTOFARM_ALLOW_DUALBOX && _players.values().stream().filter(AutoFarmProfile::isEnabled).anyMatch(p -> p.getPlayer().getClient().getConnection().getInetAddress().getHostAddress().equals(playerIp)))
		{
			showIndexWindow(player, "Dualboxing is not allowed");
			return;
		}
		
		if (!canSeeArea(player, autoFarmProfile.getSelectedArea()))
		{
			showIndexWindow(player, "Area too distant to be viewed");
			return;
		}
		
		// The area is nearby, we can already display it, even if the player is not moved automatically.
		ZoneBuilder.getInstance().previewFinalArea(autoFarmProfile, autoFarmProfile.getSelectedAreaId());
		
		// It is outside the zone. Do we try to enter or not start?
		if (autoFarmProfile.getSelectedArea().getType() == AutoFarmType.ZONA && !player.isInsideZone(ZoneId.AUTO_FARM) && !autoFarmProfile.getSelectedArea().getFarmZone().tryGoBackInside())
		{
			showIndexWindow(player, "Move to the inside of the zone.");
			return;
		}
		
		if (player.getTarget() != null)
			player.setTarget(null);
		
		if (Config.AUTOFARM_CHANGE_PLAYER_TITLE)
		{
			autoFarmProfile.setPlayerTitle(player.getTitle());
			player.setTitle("Auto Farming");
			player.broadcastTitleInfo();
		}
		
		if (Config.AUTOFARM_CHANGE_PLAYER_NAME_COLOR)
		{
			final int color = Integer.decode("0x" + Config.AUTOFARM_PLAYER_NAME_COLOR);
			player.getAppearance().setNameColor(color);
			player.getAppearance().setTitleColor(color);
			player.broadcastUserInfo();
		}
		
		autoFarmProfile.setEnabled(true);
		autoFarmProfile.startRoutine();
		showIndexWindow(player, null);
	}
	
	public void stopPlayer(AutoFarmProfile autoFarmProfile, String msg)
	{
		stopPlayer(autoFarmProfile.getPlayer(), msg);
	}
	
	public void stopPlayer(Player player, String msg)
	{
		// This player did not use the system.
		if (getPlayer(player.getObjectId()) == null)
			return;
		
		final AutoFarmProfile autoFarmProfile = getProfile(player);
		
		// Logout
		if (!player.isOnline())
			autoFarmProfile.updatePlayer(null);
		
		if (autoFarmProfile.isEnabled())
		{
			if (autoFarmProfile.getPlayerTitle() != null)
			{
				player.setTitle(autoFarmProfile.getPlayerTitle());
				player.broadcastTitleInfo();
			}
			
			if (Config.AUTOFARM_CHANGE_PLAYER_NAME_COLOR)
			{
				player.getAppearance().setNameColor(player.getAccessLevel().getNameColor());
				player.getAppearance().setTitleColor(player.getAccessLevel().getTitleColor());
				player.broadcastUserInfo();
			}
			
			ZoneBuilder.getInstance().clearAllPreview(player);
			autoFarmProfile.setEnabled(false);
			player.abortAll(false);
		}
		
		showIndexWindow(player, msg);
	}
	
	private void createArea(Player player, AutoFarmProfile autoFarmProfile, StringTokenizer st) throws IllegalArgumentException, NumberFormatException
	{
		if (!st.hasMoreTokens())
			return;
		
		final AutoFarmType type = AutoFarmType.valueOf(st.nextToken());
		final String name = st.hasMoreTokens() ? st.nextToken().toLowerCase() : "";
		
		if (name.isEmpty() || !StringUtil.isValidString(name, ZONE_NAME_REGEX))
		{
			showAreaListWindow(player, type, "Invalid name.");
			return;
		}
		
		if (autoFarmProfile.getAreas().values().stream().anyMatch(a -> a.getName().equals(name)))
		{
			showAreaListWindow(player, type, "Name already in use.");
			return;
		}
		
		if (type == AutoFarmType.ZONA)
		{
			if (!player.isGM() && autoFarmProfile.getAreas().values().stream().filter(a -> a.getType() == AutoFarmType.ZONA).count() >= Config.AUTOFARM_MAX_ZONES)
			{
				showAreaListWindow(player, type, "Zone limit reached.");
				return;
			}
			
			final AutoFarmZone zone = new AutoFarmZone(name, player.getObjectId());
			autoFarmProfile.getAreas().put(zone.getId(), zone);
			autoFarmProfile.setBuildingAreaId(zone.getId());
		}
		else if (type == AutoFarmType.ROTA)
		{
			if (!player.isGM() && autoFarmProfile.getAreas().values().stream().filter(a -> a.getType() == AutoFarmType.ROTA).count() >= Config.AUTOFARM_MAX_ROUTES)
			{
				showAreaListWindow(player, type, "Route limit reached.");
				return;
			}
			
			final AutoFarmRoute route = new AutoFarmRoute(name, player.getObjectId());
			autoFarmProfile.getAreas().put(route.getId(), route);
			autoFarmProfile.setBuildingAreaId(route.getId());
		}
		
		showAreaListWindow(player, type);
		ZoneBuilder.getInstance().clearAllPreview(player);
	}
	
	private void newNodeMode(Player player, AutoFarmProfile autoFarmProfile, boolean lock)
	{
		if (autoFarmProfile.getBuildingArea() == null)
			return;
		
		if (autoFarmProfile.isAddingLocation())
		{
			if (lock)
				autoFarmProfile.toggleAddingLocationLock();
			else
				autoFarmProfile.setAddingLocation(false);
			
			showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), autoFarmProfile.isAddingLocation() ? "Indicate the location with the mouse." : null);
			return;
		}
		
		if (autoFarmProfile.getBuildingArea().getZone() != null && !canSeeArea(player, autoFarmProfile.getBuildingArea()))
		{
			showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), "Area too distant to be viewed.");
			return;
		}
		
		// If we are going to edit the zone, it is necessary to restore it.
		if (ZoneBuilder.getInstance().getPoints(player).isEmpty() && !autoFarmProfile.getBuildingArea().isChanged())
		{
			ZoneBuilder.getInstance().restoreDebugPoints(player, autoFarmProfile.getBuildingArea().getNodes());
			ZoneBuilder.getInstance().preview(player);
		}
		
		if (autoFarmProfile.getBuildingArea().getType() == AutoFarmType.ZONA && ZoneBuilder.getInstance().getPoints(player).size() >= Math.min(Config.AUTOFARM_MAX_ZONE_NODES, 20))
		{
			showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), "Your zone has reached the maximum number of points.");
			return;
		}
		else if (autoFarmProfile.getBuildingArea().getType() == AutoFarmType.ROTA && ZoneBuilder.getInstance().getPoints(player).size() >= Math.min(Config.AUTOFARM_MAX_ROUTE_NODES, 20))
		{
			showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), "Your route has reached the maximum number of points.");
			return;
		}
		
		if (lock)
			autoFarmProfile.toggleAddingLocationLock();
		else
			autoFarmProfile.setAddingLocation(true);
		
		showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), "Indicate the location with the mouse.");
	}
	
	public void addAreaNode(Player player, Location loc)
	{
		final AutoFarmProfile autoFarmProfile = getProfile(player);
		final AutoFarmArea area = autoFarmProfile.getBuildingArea();
		if (area == null)
			return;

		String msg = null;
		if (Config.AUTOFARM_DISABLE_TOWN && ZoneManager.getInstance().getZone(loc.getX(), loc.getY(), loc.getZ(), TownZone.class) != null)
			msg = "Invalid location.";
		else if (area.getType() == AutoFarmType.ZONA && Config.AUTOFARM_MAX_ZONE_AREA != 0 && ZoneBuilder.getInstance().calculateArea(player) > Config.AUTOFARM_MAX_ZONE_AREA)
			msg = "Your zone has reached the maximum size.";
		else if (area.getType() == AutoFarmType.ROTA && Config.AUTOFARM_MAX_ROUTE_PERIMITER != 0 && ZoneBuilder.getInstance().calculatePerimeter(player) > Config.AUTOFARM_MAX_ROUTE_PERIMITER)
			msg = "Your route has reached the maximum length.";
		else
		{
			final Map<Integer, Point> points = ZoneBuilder.getInstance().getPoints(player);
			if (points.size() > 0)
			{
				final Point lastPoint = points.get(points.size());
				if (!GeoEngine.getInstance().canSee(lastPoint.getX(), lastPoint.getY(), lastPoint.getZ() + 20, 0, loc.getX(), loc.getY(), loc.getZ(), 0, null, null))
					msg = "Geographically unavailable point.";
				else if (lastPoint.distance3D(loc) > MAX_ROUTE_LINE_LENGTH)
					msg = "Point too distant.";
			}
		}
		
		if (msg == null)
		{
			area.setIsChanged(true);
			ZoneBuilder.getInstance().addPoint(player, loc);
		}
		
		if (!autoFarmProfile.isAddingLocationLocked())
			autoFarmProfile.setAddingLocation(false);
		
		showEditAreaWindow(player, area.getId(), msg);
	}
	
	private void removeAreaNode(Player player, AutoFarmProfile autoFarmProfile, int nodeId)
	{
		if (autoFarmProfile.getBuildingArea() == null || nodeId < 0)
			return;
		
		if (autoFarmProfile.getBuildingArea().getZone() != null && !canSeeArea(player, autoFarmProfile.getBuildingArea()))
		{
			showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), "Area too distant to be viewed.");
			return;
		}
		
		final Map<Integer, Point> points = ZoneBuilder.getInstance().getPoints(player);
		
		// If we are going to edit the zone, it is necessary to restore it.
		if (points.isEmpty())
			ZoneBuilder.getInstance().restoreDebugPoints(player, autoFarmProfile.getBuildingArea().getNodes());
		
		if (points.isEmpty() || points.size() < nodeId)
			return;

		// Point removed.
		ZoneBuilder.getInstance().removePoint(player, nodeId);
		
		// If after removing a node the area is exactly as it was before, then it has not been changed at all.
		if (Util.isNodeListEquals(points.values(), autoFarmProfile.getBuildingArea().getNodes()))
			autoFarmProfile.getBuildingArea().setIsChanged(false);
		else
			autoFarmProfile.getBuildingArea().setIsChanged(true);
		
		showEditAreaWindow(player, autoFarmProfile.getBuildingAreaId(), null);
	}
	
	private void deleteArea(Player player, AutoFarmProfile autoFarmProfile, int areaId)
	{
		final AutoFarmArea area = autoFarmProfile.getAreaById(areaId);
		if (area.getType() == AutoFarmType.OPEN)
			return;
		
		if (area.isFromDb())
			AutoFarmData.getInstance().deleteArea(player.getObjectId(), areaId);
		
		if (area.getType() == AutoFarmType.ZONA)
			area.getFarmZone().removeFromWorld();
		
		boolean clearPreview = false;
		if (autoFarmProfile.getSelectedAreaId() == areaId)
		{
			clearPreview = true;
			autoFarmProfile.setSelectedAreaId(0);
		}
		
		if (autoFarmProfile.getBuildingAreaId() == areaId)
		{
			clearPreview = true;
			autoFarmProfile.setBuildingAreaId(0);
		}
		
		if (clearPreview)
			ZoneBuilder.getInstance().clearAllPreview(player);
		
		autoFarmProfile.getAreas().remove(areaId);
		autoFarmProfile.getTargets().clear();
		showAreaListWindow(player, area.getType());
	}
	
	private void saveArea(Player player, AutoFarmProfile autoFarmProfile)
	{
		final AutoFarmArea area = autoFarmProfile.getBuildingArea();
		if (area == null)
			return;
		
		if (!area.isChanged())
		{
			showEditAreaWindow(player, area.getId(), null);
			return;
		}
		
		if (ZoneBuilder.getInstance().getPoints(player).isEmpty())
		{
			showEditAreaWindow(player, area.getId(), "Build your zone before saving.");
			return;
		}
		
		if (area.getType() == AutoFarmType.ROTA)
		{
			
			if (ZoneBuilder.getInstance().getPoints(player).size() < 2)
			{
				showEditAreaWindow(player, area.getId(), "A route needs at least 2 points.");
				return;
			}
		}
		else
		{
			if (ZoneBuilder.getInstance().getPoints(player).size() < 3)
			{
				showEditAreaWindow(player, area.getId(), "A zone needs at least 3 points.");
				return;
			}
		}
		
		// When clearing the points from the area, the ZoneBuilder will select those that are in it.
		area.getNodes().clear();
		
		// Create ZoneNPolyZ.
		ZoneBuilder.getInstance().setAutoFarmAreaZone(player, area);
		
		// Display final zone.
		ZoneBuilder.getInstance().previewFinalArea(autoFarmProfile, area.getId());

		// Update variables.
		autoFarmProfile.setSelectedAreaId(area.getId());
		
		if (area.getType() == AutoFarmType.ZONA)
		{
			area.getFarmZone().removeFromWorld();
			area.getFarmZone().addToWorld();
		}
		
		if (!area.isFromDb())
			AutoFarmData.getInstance().insertArea(player.getObjectId(), autoFarmProfile.getBuildingArea());
		
		area.setIsChanged(false);
		AutoFarmData.getInstance().insertNodes(autoFarmProfile.getBuildingArea());
		showEditAreaWindow(player, area.getId(), "Saved!");
	}
	
	private void previewArea(Player player, AutoFarmProfile autoFarmProfile, int areaId, boolean finalView)
	{
		final AutoFarmArea area = autoFarmProfile.getAreaById(areaId);
		if (area.getType() != AutoFarmType.OPEN && area.isChanged() || !area.isFromDb())
			return;
		
		// It is already being viewed.
		if (!ZoneBuilder.getInstance().getPoints(player).isEmpty() && autoFarmProfile.getBuildingAreaId() == areaId)
		{
			ZoneBuilder.getInstance().clearAllPreview(player);
			return;
		}
		
		// We'll check later to ensure that the preview can always be cleared.
		if (!canSeeArea(player, area))
		{
			if (finalView)
				showAreaListWindow(player, area.getType(), "Area too distant to be viewed.");
			else
				showEditAreaWindow(player, areaId, "Area too distant to be viewed.");
			
			return;
		}
		
		if (finalView)
		{
			ZoneBuilder.getInstance().previewFinalArea(autoFarmProfile, areaId);
			showAreaListWindow(player, area.getType());
		}
		else
		{
			ZoneBuilder.getInstance().restoreDebugPoints(player, autoFarmProfile.getBuildingArea().getNodes());
			ZoneBuilder.getInstance().preview(player);
			showEditAreaWindow(player, areaId, null);
		}
			
		autoFarmProfile.setBuildingAreaId(areaId);
	}
	
	public static final AutoFarmManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final AutoFarmManager INSTANCE = new AutoFarmManager();
	}
}