package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.custom.data.AuctionCurrencies;
import net.sf.l2j.gameserver.custom.data.CapsuleBoxData;
import net.sf.l2j.gameserver.custom.data.DonateData;
import net.sf.l2j.gameserver.custom.data.EnchantData;
import net.sf.l2j.gameserver.custom.data.MissionData;
import net.sf.l2j.gameserver.custom.data.PcCafeData;
import net.sf.l2j.gameserver.custom.data.PolymorphData;
import net.sf.l2j.gameserver.custom.data.PvPData;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.CrestCache;
import net.sf.l2j.gameserver.data.manager.BuyListManager;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.data.xml.BoatData;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.InstantTeleportData;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.ObserverGroupData;
import net.sf.l2j.gameserver.data.xml.RestartPointData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.data.xml.SysString;
import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

import ext.mods.dressme.DressMeData;

public class AdminReload implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_reload"
	};
	
	@Override
	public void useAdminCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		try
		{
			do
			{
				String type = st.nextToken();
				if (type.startsWith("admin"))
				{
					AdminData.getInstance().reload();
					player.sendMessage("Admin data has been reloaded.");
				}
				else if (type.startsWith("announcement"))
				{
					AnnouncementData.getInstance().reload();
					player.sendMessage("The content of announcements.xml has been reloaded.");
				}
				else if (type.startsWith("boat"))
				{
					BoatData.getInstance().reload();
					player.sendMessage("Boat have been reloaded.");
				}
				else if (type.startsWith("buylist"))
				{
					BuyListManager.getInstance().reload();
					player.sendMessage("Buylists have been reloaded.");
				}
				else if (type.startsWith("config"))
				{
					Config.loadGameServer();
					player.sendMessage("Configs files have been reloaded.");
				}
				else if (type.startsWith("crest"))
				{
					CrestCache.getInstance().reload();
					player.sendMessage("Crests have been reloaded.");
				}
				else if (type.startsWith("cw"))
				{
					CursedWeaponManager.getInstance().reload();
					player.sendMessage("Cursed weapons have been reloaded.");
				}
				else if (type.startsWith("door"))
				{
					DoorData.getInstance().reload();
					player.sendMessage("Doors instance has been reloaded.");
				}
				else if (type.startsWith("htm"))
				{
					HTMLData.getInstance().reload();
					player.sendMessage("The HTM cache has been reloaded.");
				}
				else if (type.startsWith("item"))
				{
					ItemData.getInstance().reload();
					player.sendMessage("Items' templates have been reloaded.");
				}
				else if (type.equals("multisell"))
				{
					MultisellData.getInstance().reload();
					player.sendMessage("The multisell instance has been reloaded.");
				}
				else if (type.equals("npc"))
				{
					NpcData.getInstance().reload();
					ScriptData.getInstance().reload();
					player.sendMessage("NPCs templates and Scripts have been reloaded.");
				}
				else if (type.startsWith("npcwalker"))
				{
					WalkerRouteData.getInstance().reload();
					player.sendMessage("Walking routes have been reloaded.");
				}
				else if (type.equals("script"))
				{
					ScriptData.getInstance().reload();
					player.sendMessage("Scripts have been reloaded.");
				}
				else if (type.startsWith("skill"))
				{
					SkillTable.getInstance().reload();
					player.sendMessage("Skills' XMLs have been reloaded.");
				}
				else if (type.startsWith("teleport"))
				{
					InstantTeleportData.getInstance().reload();
					TeleportData.getInstance().reload();
					player.sendMessage("Teleport locations have been reloaded.");
				}
				else if (type.startsWith("zone"))
				{
					ZoneManager.getInstance().reload();
					player.sendMessage("Zones have been reloaded.");
				}
				else if (type.startsWith("spawnlist"))
				{
					// make sure all spawns are deleted
					SpawnManager.getInstance().despawn();
					World.getInstance().deleteVisibleNpcSpawns();
					
					// now respawn all
					NpcData.getInstance().reload();
					ScriptData.getInstance().reload();
					SpawnManager.getInstance().reload();
				}
				else if (type.startsWith("capsule"))
				{
					CapsuleBoxData.getInstance().reload();
					player.sendMessage("Capsule Box have been reloaded.");
				}
				else if (type.startsWith("sysstring"))
				{
					SysString.getInstance().reload();
					player.sendMessage("SysString have been reloaded.");
				}
				else if (type.startsWith("enchant"))
				{
					EnchantData.getInstance().reload();
					player.sendMessage("Enchant have been reloaded.");
				}
				else if (type.startsWith("donate"))
				{
					DonateData.getInstance().reload();
					player.sendMessage("Donate have been reloaded.");
				}
				else if (type.startsWith("skins"))
				{
					DressMeData.getInstance().reload();
					player.sendMessage("Skins have been reloaded.");
				}
				else if (type.startsWith("mission"))
				{
					MissionData.getInstance().reload();
					player.sendMessage("Mission have been reloaded.");
				}
				else if (type.startsWith("auction"))
				{
					AuctionCurrencies.getInstance().reload();
					player.sendMessage("Auction have been reloaded.");
				}
				else if (type.startsWith("pccafe"))
				{
					PcCafeData.getInstance().reload();
					player.sendMessage("PcCafe have been reloaded.");
				}
				else if (type.startsWith("restart"))
				{
					RestartPointData.getInstance().reload();
					player.sendMessage("RestartPointData have been reloaded.");
				}
				else if (type.startsWith("observer"))
				{
					ObserverGroupData.getInstance().reload();
					player.sendMessage("ObserverGroupData have been reloaded.");
				}
				else if (type.startsWith("poly"))
				{
					PolymorphData.getInstance().reload();
					player.sendMessage("Polymorph templates have been reloaded.");
				}
				else if (type.startsWith("pvpdata"))
				{
					PvPData.getInstance().reload();
					player.sendMessage("PvPData have been reloaded.");
				}
				else
					sendUsage(player);
			}
			while (st.hasMoreTokens());
		}
		catch (Exception e)
		{
			sendUsage(player);
		}
	}
	
	public void sendUsage(Player player)
	{
		player.sendMessage("Usage : //reload <admin|announcement|buylist|config>");
		player.sendMessage("Usage : //reload <crest|cw|door|htm|item|multisell|npc>");
		player.sendMessage("Usage : //reload <npcwalker|script|skill|teleport|zone>");
		player.sendMessage("Usage : //reload <spawnlist|sysstring|capsule|donate|mission>");
		player.sendMessage("Usage : //reload <skins|enchant|auction|pccafe|restart>");
		player.sendMessage("Usage : //reload <observer|poly|pvpdata>");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}