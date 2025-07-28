
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class CTFFlag extends Folk
{
	private static final String flagsPath = "html/mods/events/ctf/flags/";
	
	public CTFFlag(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		if (player == null)
			return;
		
		if (CTFEvent.getInstance().isStarting() || CTFEvent.getInstance().isStarted())
		{
			final String team = CTFEvent.getInstance().getParticipantTeam(player.getObjectId()).getName();
			final String enemyteam = CTFEvent.getInstance().getParticipantEnemyTeam(player.getObjectId()).getName();
			
			if (getTitle() == team)
			{
				if (CTFEvent.getInstance().getEnemyCarrier(player) != null)
				{
					final String htmContent = HTMLData.getInstance().getHtm(player, flagsPath + "flag_friendly_missing.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%enemyteam%", enemyteam);
					npcHtmlMessage.replace("%team%", team);
					npcHtmlMessage.replace("%player%", player.getName());
					player.sendPacket(npcHtmlMessage);
				}
				else if (player == CTFEvent.getInstance().getTeamCarrier(player))
				{
					if (Config.CTF_EVENT_CAPTURE_SKILL > 0)
						player.broadcastPacket(new MagicSkillUse(player, Config.CTF_EVENT_CAPTURE_SKILL, 1, 1, 1));
					
					CTFEvent.getInstance().removeFlagCarrier(player);
					CTFEvent.getInstance().getParticipantTeam(player.getObjectId()).increasePoints();
					CTFEvent.getInstance().broadcastScreenMessage(player.getName() + " scored for the " + team + " team!", 7);
				}
				else
				{
					final String htmContent = HTMLData.getInstance().getHtm(player, flagsPath + "flag_friendly.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%enemyteam%", enemyteam);
					npcHtmlMessage.replace("%team%", team);
					npcHtmlMessage.replace("%player%", player.getName());
					player.sendPacket(npcHtmlMessage);
				}
			}
			else
			{
				if (CTFEvent.getInstance().playerIsCarrier(player))
				{
					final String htmContent = HTMLData.getInstance().getHtm(player, flagsPath + "flag_enemy.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%enemyteam%", enemyteam);
					npcHtmlMessage.replace("%team%", team);
					npcHtmlMessage.replace("%player%", player.getName());
					player.sendPacket(npcHtmlMessage);
				}
				else if (CTFEvent.getInstance().getTeamCarrier(player) != null)
				{
					final String htmContent = HTMLData.getInstance().getHtm(player, flagsPath + "flag_enemy_missing.htm");
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%enemyteam%", enemyteam);
					npcHtmlMessage.replace("%player%", CTFEvent.getInstance().getTeamCarrier(player).getName());
					player.sendPacket(npcHtmlMessage);
				}
				else
				{
					if (Config.CTF_EVENT_CAPTURE_SKILL > 0)
						player.broadcastPacket(new MagicSkillUse(player, Config.CTF_EVENT_CAPTURE_SKILL, 1, 1, 1));
					
					CTFEvent.getInstance().setCarrierUnequippedWeapons(player, player.getInventory().getItemFrom(Paperdoll.RHAND), player.getInventory().getItemFrom(Paperdoll.LHAND));
					player.getInventory().equipItem(ItemInstance.create(CTFEvent.getInstance().getEnemyTeamFlagId(player), 1));
					player.getInventory().blockAllItems();
					player.broadcastUserInfo();
					CTFEvent.getInstance().setTeamCarrier(player);
					CTFEvent.getInstance().broadcastScreenMessage(player.getName() + " has taken the " + enemyteam + " flag team!", 5);
				}
			}
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public boolean isInvul()
	{
		return true;
	}
}