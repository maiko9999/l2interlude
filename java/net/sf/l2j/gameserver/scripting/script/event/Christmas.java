package net.sf.l2j.gameserver.scripting.script.event;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.custom.data.EventsData;
import net.sf.l2j.gameserver.data.manager.EventsDropManager;
import net.sf.l2j.gameserver.data.manager.EventsDropManager.RuleType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.records.custom.EventsInfo;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Christmas extends Events
{
	private static boolean ACTIVE = false;
	
	private List<Npc> _npclist;
	
	private final int SANTATRAINEE = 31863;
	private final int SANTATRAINEE2 = 31864;
	
	private final int CHRISTMAS_TREE = 13006;
	
	private static final SpawnLocation[] _santacoords =
	{
		new SpawnLocation(81921, 148921, -3467, 16384),
		new SpawnLocation(146405, 28360, -2269, 49648),
		new SpawnLocation(19319, 144919, -3103, 31135),
		new SpawnLocation(-82805, 149890, -3129, 16384),
		new SpawnLocation(-12347, 122549, -3104, 16384),
		new SpawnLocation(110642, 220165, -3655, 61898),
		new SpawnLocation(116619, 75463, -2721, 20881),
		new SpawnLocation(85513, 16014, -3668, 23681),
		new SpawnLocation(81999, 53793, -1496, 61621),
		new SpawnLocation(148159, -55484, -2734, 44315),
		new SpawnLocation(44185, -48502, -797, 27479),
		new SpawnLocation(86899, -143229, -1293, 8192)
	};
	
	private static final SpawnLocation[] _treecoords =
	{
		new SpawnLocation(81961, 148921, -3467, 0),
		new SpawnLocation(146445, 28360, -2269, 0),
		new SpawnLocation(19319, 144959, -3103, 0),
		new SpawnLocation(-82845, 149890, -3129, 0),
		new SpawnLocation(-12387, 122549, -3104, 0),
		new SpawnLocation(110602, 220165, -3655, 0),
		new SpawnLocation(116659, 75463, -2721, 0),
		new SpawnLocation(85553, 16014, -3668, 0),
		new SpawnLocation(82011, 53764, -1488, 0),
		new SpawnLocation(148199, -55484, -2734, 0),
		new SpawnLocation(44185, -48542, -797, 0),
		new SpawnLocation(86859, -143229, -1293, 0)
	};
	
	private void addDrop()
	{
		final EventsInfo event = EventsData.getInstance().getEventsData(getName());
		
		EventsDropManager.getInstance().addChristmasRule(getName(), RuleType.ALL_NPC, event.items());
	}
	
	private void removeDrop()
	{
		EventsDropManager.getInstance().removeChristmasRules(getName());
	}
	
	public Christmas()
	{
		addQuestStart(SANTATRAINEE, SANTATRAINEE2);
		addFirstTalkId(SANTATRAINEE, SANTATRAINEE2);
		addTalkId(SANTATRAINEE, SANTATRAINEE2);
	}
	
	@Override
	public boolean eventStart(int priority)
	{
		if (ACTIVE)
			return false;
		
		ACTIVE = true;
		
		_npclist = new ArrayList<>();
		
		for (SpawnLocation loc : _santacoords)
			recordSpawn(SANTATRAINEE, loc);
		
		for (SpawnLocation locs : _treecoords)
			recordSpawn(CHRISTMAS_TREE, locs);
		
		eventStatusStart(priority);
		addDrop();
		
		World.announceToOnlinePlayers(10_159, getName());
		return true;
	}
	
	@Override
	public boolean eventStop()
	{
		if (!ACTIVE)
			return false;
		
		ACTIVE = false;
		
		if (!_npclist.isEmpty())
			_npclist.forEach(npc -> npc.deleteMe());
		
		_npclist.clear();
		
		eventStatusStop();
		removeDrop();
		
		World.announceToOnlinePlayers(10_160, getName());
		return true;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		if (event.equalsIgnoreCase("0"))
		{
			if ((player.getInventory().getItemCount(5556) >= 4) && (player.getInventory().getItemCount(5557) >= 4) && (player.getInventory().getItemCount(5558) >= 10) && (player.getInventory().getItemCount(5559) >= 1))
			{
				takeItems(player, 5556, 4);
				takeItems(player, 5557, 4);
				takeItems(player, 5558, 10);
				takeItems(player, 5559, 1);
				giveItems(player, 5560, 1);
			}
			else
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));
			
			return null;
		}
		else if (event.equalsIgnoreCase("1"))
		{
			if (player.getInventory().getItemCount(5560) >= 10)
			{
				takeItems(player, 5560, 10);
				giveItems(player, 5561, 1);
			}
			else
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));
			
			return null;
		}
		else if (event.equalsIgnoreCase("2"))
		{
			if (player.getInventory().getItemCount(5561) >= 10)
			{
				takeItems(player, 5561, 10);
				giveItems(player, 7836, 1);
			}
			else
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));
			
			return null;
		}
		else if (event.equalsIgnoreCase("3"))
		{
			if (player.getInventory().getItemCount(5561) >= 20)
			{
				takeItems(player, 5561, 20);
				giveItems(player, 8936, 1);
			}
			else
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));
			
			return null;
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getNpcId() + ".htm";
	}
	
	private void recordSpawn(int npcId, SpawnLocation loc)
	{
		Npc npc = addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), false, 0, false);
		if (npc != null)
			_npclist.add(npc);
	}
}