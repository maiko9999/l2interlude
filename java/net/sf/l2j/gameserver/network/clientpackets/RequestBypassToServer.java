package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.util.QueryParser;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.communitybbs.CustomCommunityBoard;
import net.sf.l2j.gameserver.data.manager.BotsPreventionManager;
import net.sf.l2j.gameserver.data.manager.DropSkipManager;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.BypassHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.bypasshandlers.DropListUI;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.QuestState;

import ext.mods.dungeon.DungeonManager;
import ext.mods.util.Tokenizer;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_command.isEmpty())
			return;
		
		if (!getClient().performAction(FloodProtector.SERVER_BYPASS))
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (_command.startsWith("admin_"))
		{
			String command = _command.split(" ")[0];
			
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
			if (ach == null)
			{
				if (player.isGM())
					player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
				
				LOGGER.warn("No handler registered for admin command '{}'.", command);
				return;
			}
			
			if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command.");
				LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
				return;
			}
			
			if (Config.GMAUDIT)
				GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
			
			ach.useAdminCommand(_command, player);
		}
		
		else if (_command.startsWith("dungeon"))
		{
			final Tokenizer tokenizer = new Tokenizer(_command);
			final String param = tokenizer.getToken(1);
			if (param == null)
			{
				player.sendMessage("Invalid command parameter.");
				return;
			}
			switch (param.toLowerCase())
			{
				case "enter":
					DungeonManager.getInstance().handleEnterDungeonId(player, tokenizer);
					break;
				
				default:
					player.sendMessage("Unknown command.");
					break;
			}
		}
		
		else if (_command.startsWith("droplist"))
		{
			StringTokenizer st = new StringTokenizer(_command, " ");
			st.nextToken(); // skip command
			
			int npcId = Integer.parseInt(st.nextToken());
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			
			if (st.hasMoreTokens())
			{
				int itemId = Integer.parseInt(st.nextToken());
				DropSkipManager.getInstance().toggleSkip(player.getObjectId(), itemId);
				
				// 🔧 Recarrega dados atualizados do banco após alteração
				DropSkipManager.getInstance().loadPlayer(player.getObjectId());
			}
			
			DropListUI.sendNpcDrop(player, npcId, page);
		}

		
		else if (_command.startsWith("player_help "))
		{
			final String path = _command.substring(12);
			if (path.indexOf("..") != -1)
				return;
			
			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(player.getLocale(), "html/help/" + cmd[0]);
			if (cmd.length > 1)
			{
				final int itemId = Integer.parseInt(cmd[1]);
				html.setItemId(itemId);
				
				if (itemId == 7064 && cmd[0].equalsIgnoreCase("lidias_diary/7064-16.htm"))
				{
					final QuestState qs = player.getQuestList().getQuestState("Q023_LidiasHeart");
					if (qs != null && qs.getCond() == 5 && qs.getInteger("diary") == 0)
						qs.set("diary", "1");
				}
			}
			html.disableValidation();
			player.sendPacket(html);
		}
		else if (_command.startsWith("npc_"))
		{
			if (!player.validateBypass(_command))
				return;
			
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);
			
			try
			{
				final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
				if (object instanceof Npc npc && endOfId > 0 && player.getAI().canDoInteract(npc))
					npc.onBypassFeedback(player, _command.substring(endOfId + 1));
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			WorldObject object = player.getTarget();
			if (object instanceof Npc targetNpc)
				targetNpc.onBypassFeedback(player, _command);
		}
		else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block") || _command.startsWith("_cbauction") || _command.startsWith("_cbmission"))
		{
			if (Config.ENABLE_CUSTOM_BBS)
				CustomCommunityBoard.getInstance().handleCommands(getClient(), _command);
			
			if (Config.ENABLE_COMMUNITY_BOARD)
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
		}
		else if (_command.startsWith("Quest "))
		{
			if (!player.validateBypass(_command))
				return;
			
			String[] str = _command.substring(6).trim().split(" ", 2);
			if (str.length == 1)
				player.getQuestList().processQuestEvent(str[0], "");
			else
				player.getQuestList().processQuestEvent(str[0], str[1]);
		}
		else if (_command.startsWith("_match"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroFights(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("_diary"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("arenachange")) // change
		{
			final boolean isManager = player.getCurrentFolk() instanceof OlympiadManagerNpc;
			if (!isManager)
			{
				// Without npc, command can be used only in observer mode on arena
				if (!player.isInObserverMode() || player.isInOlympiadMode() || player.getOlympiadGameId() < 0)
					return;
			}
			
			// Olympiad registration check.
			if (OlympiadManager.getInstance().isRegisteredInComp(player))
			{
				player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
				return;
			}
			
			final int arenaId = Integer.parseInt(_command.substring(12).trim());
			player.enterOlympiadObserverMode(arenaId);
		}
		else if (_command.startsWith("report"))
			BotsPreventionManager.getInstance().analyseBypass(_command, player);
		else if (_command.startsWith("QuestGatekeeper"))
		{
			String[] args = _command.substring(16).split(" ");
			
			int loc = Integer.parseInt(args[0]);
			int loc1 = Integer.parseInt(args[1]);
			int loc2 = Integer.parseInt(args[2]);
			int itemid = Integer.parseInt(args[3]);
			int count = Integer.parseInt(args[4]);
			
			if (player.getInventory().getItemByItemId(itemid) == null || player.getInventory().getItemByItemId(itemid).getCount() < count)
			{
				player.sendMessage("Incorrect item count. You need " + count + " " + itemid);
				return;
			}
			
			player.destroyItemByItemId(itemid, count, true);
			player.teleportTo(loc, loc1, loc2, 20);
		}
		else if (_command.startsWith("npcfind_byid"))
		{
			String[] args = _command.substring(13).split(" ");
			final int raidId = Integer.parseInt(args[0]);
			
			// get spawn information of the raid boss
			final ASpawn spawn = SpawnManager.getInstance().getSpawn(raidId);
			if (spawn != null)
				player.getRadarList().addMarker(spawn.getSpawnLocation());
			else
			{
				// spawn information does not exist, try to find living instance
				final Npc raid = World.getInstance().getNpc(raidId);
				if (raid != null)
					player.getRadarList().addMarker(raid.getPosition());
			}
		}
		else if (_command.startsWith("voiced_"))
		{
			String command = _command.split(" ")[0];
			
			IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));
			
			if (ach == null)
			{
				player.sendMessage("The command " + command.substring(7) + " does not exist!");
				LOGGER.warn("No handler registered for command '" + _command + "'");
				return;
			}
			
			ach.useVoicedCommand(_command.substring(7), player, null);
		}
		else if (_command.startsWith("autofarm"))
			AutoFarmManager.getInstance().handleBypass(player, _command.substring(9));
		else if (_command.startsWith("menu_select"))
		{
			final WorldObject target = player.getTarget();
			if (target instanceof Npc npc)
			{
				final var list = npc.getTemplate().getEventQuests(EventHandler.AI_MENU_SELECTED);
				if (!list.isEmpty())
				{
					final Map<String, String> props = QueryParser.parse(_command);
					final int ask = Integer.parseInt(props.getOrDefault("ask", "0"));
					final int reply = Integer.parseInt(props.getOrDefault("reply", "0"));
					list.get(0).notifyMENU_SELECTED(npc, player, ask, reply);
				}
				else
					LOGGER.warn("pc[{}] target[{}] unhandle {}", player.getName(), target.getName(), _command);
			}
			else
				LOGGER.warn("pc[{}] target[{}] menu_select on incorrect target", player.getName(), target);
		}
		
		useBypassHandler(player);
	}
	
	private void useBypassHandler(Player player)
	{
		_command = _command.replace("?", " ");
		final IBypassHandler handler = BypassHandler.getInstance().getHandler(_command);
		if (handler != null)
			handler.useBypass(_command, player, null);
	}
}