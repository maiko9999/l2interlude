package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class EventManager extends Npc
{
	private static final String ctfhtmlPath = "html/mods/events/ctf/";
	private static final String TvthtmlPath = "html/mods/events/tvt/";
	private static final String dmhtmlPath = "html/mods/events/dm/";
	private static final String lmhtmlPath = "html/mods/events/lm/";
	
	public EventManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		CTFEvent.getInstance().onBypass(command, player);
		TvTEvent.getInstance().onBypass(command, player);
		DMEvent.getInstance().onBypass(command, player);
		LMEvent.getInstance().onBypass(command, player);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		if (player == null)
			return;
		
		if (TvTEvent.getInstance().isParticipating())
		{
			final boolean isParticipant = TvTEvent.getInstance().isPlayerParticipant(player.getObjectId());
			final String htmContent;
			
			if (!isParticipant)
				htmContent = HTMLData.getInstance().getHtm(player, TvthtmlPath + "Participation.htm");
			else
				htmContent = HTMLData.getInstance().getHtm(player, TvthtmlPath + "RemoveParticipation.htm");
			
			if (htmContent != null)
			{
				int[] teamsPlayerCounts = TvTEvent.getInstance().getTeamsPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1]));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", TvTEvent.getInstance().getParticipationFee());
				
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (TvTEvent.getInstance().isStarting() || TvTEvent.getInstance().isStarted())
		{
			final String htmContent = HTMLData.getInstance().getHtm(player, TvthtmlPath + "Status.htm");
			
			if (htmContent != null)
			{
				int[] teamsPlayerCounts = TvTEvent.getInstance().getTeamsPlayerCounts();
				int[] teamsPointsCounts = TvTEvent.getInstance().getTeamsPoints();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1])); // <---- array index from 0 to 1 thx DaRkRaGe
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (DMEvent.getInstance().isParticipating())
		{
			final boolean isParticipant = DMEvent.getInstance().isPlayerParticipant(player.getObjectId());
			final String htmContent;
			
			if (!isParticipant)
				htmContent = HTMLData.getInstance().getHtm(player, dmhtmlPath + "Participation.htm");
			else
				htmContent = HTMLData.getInstance().getHtm(player, dmhtmlPath + "RemoveParticipation.htm");
			
			if (htmContent != null)
			{
				int PlayerCounts = DMEvent.getInstance().getPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(PlayerCounts));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", DMEvent.getInstance().getParticipationFee());
				
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (DMEvent.getInstance().isStarting() || DMEvent.getInstance().isStarted())
		{
			final String htmContent = HTMLData.getInstance().getHtm(player, dmhtmlPath + "Status.htm");
			
			if (htmContent != null)
			{
				String[] firstPositions = DMEvent.getInstance().getFirstPosition(Config.DM_REWARD_FIRST_PLAYERS);
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				String htmltext = "";
				if (firstPositions != null)
				{
					for (int i = 0; i < firstPositions.length; i++)
					{
						String[] row = firstPositions[i].split("\\,");
						htmltext += "<tr><td></td><td>" + row[0] + "</td><td align=\"center\">" + row[1] + "</td></tr>";
					}
				}
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%positions%", htmltext);
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (LMEvent.getInstance().isParticipating())
		{
			final boolean isParticipant = LMEvent.getInstance().isPlayerParticipant(player.getObjectId());
			final String htmContent;
			
			if (!isParticipant)
				htmContent = HTMLData.getInstance().getHtm(player, lmhtmlPath + "Participation.htm");
			else
				htmContent = HTMLData.getInstance().getHtm(player, lmhtmlPath + "RemoveParticipation.htm");
			
			if (htmContent != null)
			{
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(LMEvent.getInstance().getPlayerCounts()));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", LMEvent.getInstance().getParticipationFee());
				
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (LMEvent.getInstance().isStarting() || LMEvent.getInstance().isStarted())
		{
			final String htmContent = HTMLData.getInstance().getHtm(player, lmhtmlPath + "Status.htm");
			
			if (htmContent != null)
			{
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				String htmltext = "";
				htmltext = String.valueOf(LMEvent.getInstance().getPlayerCounts());
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%countplayer%", htmltext);
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (CTFEvent.getInstance().isParticipating())
		{
			final boolean isParticipant = CTFEvent.getInstance().isPlayerParticipant(player.getObjectId());
			final String htmContent;
			
			if (!isParticipant)
				htmContent = HTMLData.getInstance().getHtm(player, ctfhtmlPath + "Participation.htm");
			else
				htmContent = HTMLData.getInstance().getHtm(player, ctfhtmlPath + "RemoveParticipation.htm");
			
			if (htmContent != null)
			{
				int[] teamsPlayerCounts = CTFEvent.getInstance().getTeamsPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.CTF_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.CTF_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1]));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", CTFEvent.getInstance().getParticipationFee());
				
				player.sendPacket(npcHtmlMessage);
			}
		}
		else if (CTFEvent.getInstance().isStarting() || CTFEvent.getInstance().isStarted())
		{
			final String htmContent = HTMLData.getInstance().getHtm(player, ctfhtmlPath + "Status.htm");
			
			if (htmContent != null)
			{
				int[] teamsPlayerCounts = CTFEvent.getInstance().getTeamsPlayerCounts();
				int[] teamsPointsCounts = CTFEvent.getInstance().getTeamsPoints();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%team1name%", Config.CTF_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.CTF_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1])); // <---- array index from 0 to 1 thx DaRkRaGe
				player.sendPacket(npcHtmlMessage);
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