package net.sf.l2j.gameserver.scripting.quest;

import java.text.SimpleDateFormat;

import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHallSiege;
import net.sf.l2j.gameserver.model.residence.clanhall.SiegableHall;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q655_AGrandPlanForTamingWildBeasts extends Quest
{
	public static final String QUEST_NAME = "Q655_AGrandPlanForTamingWildBeasts";
	
	// Misc
	private static final SiegableHall BEAST_FARM = ClanHallManager.getInstance().getSiegableHall(63);
	
	// NPCs
	private static final int MESSENGER = 35627;
	
	// Items
	private static final int CRYSTAL_OF_PURITY = 8084;
	private static final int TRAINER_LICENSE = 8293;
	
	// Misc
	private static final int REQUIRED_CRYSTAL_COUNT = 10;
	private static final int REQUIRED_CLAN_LEVEL = 4;
	
	private static final String PATH_TO_HTML = "html/script/siegablehall/WildBeastReserve/farm_kel_mahum_messenger_1.htm";
	
	public Q655_AGrandPlanForTamingWildBeasts()
	{
		super(655, "A Grand Plan for Taming Wild Beasts");
		
		setItemsIds(CRYSTAL_OF_PURITY, TRAINER_LICENSE);
		
		addQuestStart(MESSENGER);
		addTalkId(MESSENGER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("35627-06.htm"))
		{
			final Clan clan = player.getClan();
			if (clan != null && clan.getLevel() >= REQUIRED_CLAN_LEVEL && clan.getClanHallId() == 0 && player.isClanLeader() && BEAST_FARM.isWaitingBattle())
			{
				st.setState(QuestStatus.STARTED);
				st.setCond(1);
				playSound(player, SOUND_ACCEPT);
			}
		}
		else if (event.equalsIgnoreCase("35627-11.htm"))
		{
			if (BEAST_FARM.isWaitingBattle())
				htmltext = HTMLData.getInstance().getHtm(player, PATH_TO_HTML);
			else
				htmltext = htmltext.replace("%next_siege%", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(BEAST_FARM.getSiegeDate().getTime()));
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case CREATED:
				final Clan clan = player.getClan();
				if (clan == null)
					return htmltext;
				
				if (BEAST_FARM.isWaitingBattle())
				{
					if (player.isClanLeader())
					{
						if (clan.getClanHallId() == 0)
							htmltext = (clan.getLevel() >= REQUIRED_CLAN_LEVEL) ? "35627-01.htm" : "35627-03.htm";
						else
							htmltext = "35627-04.htm";
					}
					else
					{
						if (clan.getClanHallId() == ClanHallSiege.BEAST_FARM && BEAST_FARM.isWaitingBattle())
							htmltext = HTMLData.getInstance().getHtm(player, PATH_TO_HTML);
						else
							htmltext = "35627-05.htm";
					}
				}
				else
					htmltext = getHtmlText(player, "35627-02.htm").replace("%next_siege%", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(BEAST_FARM.getSiegeDate().getTime()));
				break;
			
			case STARTED:
				// Time out ; quest aborts.
				if (!BEAST_FARM.isWaitingBattle())
				{
					htmltext = "35627-07.htm";
					st.exitQuest(true);
				}
				else
				{
					int cond = st.getCond();
					if (cond == 1)
						htmltext = "35627-08.htm";
					else if (cond == 2)
					{
						htmltext = "35627-10.htm";
						st.setCond(3);
						playSound(player, SOUND_MIDDLE);
						takeItems(player, CRYSTAL_OF_PURITY, -1);
						giveItems(player, TRAINER_LICENSE, 1);
					}
					else if (cond == 3)
						htmltext = "35627-09.htm";
				}
				break;
		}
		return htmltext;
	}
	
	/**
	 * Rewards the {@link Player}'s clan leader with a Crystal of Purity after Player tame a wild beast.<br>
	 * <br>
	 * If 10 crystals are gathered, trigger cond 2 for the clan leader.
	 * @param player : The player used as reference.
	 * @param npc : The npc used as reference.
	 */
	public void reward(Player player, Npc npc)
	{
		final QuestState leaderQs = checkClanLeaderCondition(player, npc, 1);
		if (leaderQs == null)
			return;
		
		if (dropItemsAlways(leaderQs.getPlayer(), CRYSTAL_OF_PURITY, 1, REQUIRED_CRYSTAL_COUNT))
			leaderQs.setCond(2);
	}
}