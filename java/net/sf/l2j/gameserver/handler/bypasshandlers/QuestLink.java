package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

import java.util.ArrayList;
import java.util.List;

public class QuestLink implements IBypassHandler {

	private static final String[] COMMANDS = {
		"Quest"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target) {
		String quest = "";
		try
		{
			quest = command.substring(5).trim();
		}
		catch (IndexOutOfBoundsException ioobe)
		{
			// Do nothing.
		}
		
		if (target instanceof Npc npc)
		{
			if (quest.isEmpty())
				showQuestWindowGeneral(player, npc);
			else
				showQuestWindowSingle(player, npc, ScriptData.getInstance().getQuest(quest));
		}
		return true;
	}

	/**
	 * Collect quests in progress and possible quests and show proper quest window to a {@link Player}.
	 * @param player : The player that talk with the Npc.
	 * @param npc : The Npc instance.
	 */
	public static void showQuestWindowGeneral(Player player, Npc npc)
	{
		final List<Quest> quests = new ArrayList<>();
		
		for (Quest quest : npc.getTemplate().getEventQuests(EventHandler.TALKED))
		{
			if (quest == null || !quest.isRealQuest() || quests.contains(quest))
				continue;
			
			final QuestState qs = player.getQuestList().getQuestState(quest.getName());
			if (qs == null || qs.isCreated())
				continue;
			
			quests.add(quest);
		}
		
		for (Quest quest : npc.getTemplate().getEventQuests(EventHandler.QUEST_START))
		{
			if (quest == null || !quest.isRealQuest() || quests.contains(quest))
				continue;
			
			quests.add(quest);
		}
		
		if (quests.isEmpty())
			showQuestWindowSingle(player, npc, null);
		else if (quests.size() == 1)
			showQuestWindowSingle(player, npc, quests.get(0));
		else
			showQuestWindowChoose(player, npc, quests);
	}
	
	/**
	 * Open a quest window on client with the text of this {@link Npc}. Create the {@link QuestState} if not existing.
	 * @param player : The Player that talk with the Npc.
	 * @param npc : The Npc instance.
	 * @param quest : The Quest to check.
	 */
	private static void showQuestWindowSingle(Player player, Npc npc, Quest quest)
	{
		if (quest == null)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player.getLocale(), Quest.getNoQuestMsg());
			player.sendPacket(html);
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (quest.isRealQuest())
		{
			// Check player being overweight.
			if (player.isOverweight())
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}
			
			// Check player has the quest started.
			if (player.getQuestList().getQuestState(quest.getName()) == null)
			{
				// Check available quest slot.
				if (player.getQuestList().getAllQuests(false).size() >= 25)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
					html.setFile(player.getLocale(), Quest.getTooMuchQuestsMsg());
					player.sendPacket(html);
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Create new state.
				if (npc.getTemplate().getEventQuests(EventHandler.QUEST_START).contains(quest))
					quest.newQuestState(player);
			}
		}
		
		player.getQuestList().setLastQuestNpcObjectId(npc.getObjectId());
		quest.notifyTalk(npc, player);
	}
	
	/**
	 * Shows the list of available {@link Quest}s for this {@link Npc}.
	 * @param player : The player that talk with the Npc.
	 * @param npc : The Npc instance.
	 * @param quests : The list containing quests of the Npc.
	 */
	private static void showQuestWindowChoose(Player player, Npc npc, List<Quest> quests)
	{
		final StringBuilder sb = new StringBuilder("<html><body>");
		
		for (final Quest q : quests)
		{
			StringUtil.append(sb, "<a action=\"bypass -h npc_%objectId%_Quest ", q.getName(), "\">[", q.getDescr());
			
			final QuestState qs = player.getQuestList().getQuestState(q.getName());
			if (qs != null && qs.isStarted())
				sb.append(player.getSysString(10_075) + "</a><br>");
			else if (qs != null && qs.isCompleted())
				sb.append(player.getSysString(10_076) + "</a><br>");
			else
				sb.append("]</a><br>");
		}
		
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(sb.toString());
		html.replace("%objectId%", npc.getObjectId());
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
