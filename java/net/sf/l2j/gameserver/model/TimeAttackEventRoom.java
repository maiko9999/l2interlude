package net.sf.l2j.gameserver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.group.Party;

public class TimeAttackEventRoom
{
	private final Map<RoomIndex, Party> eventRoomMap = new ConcurrentHashMap<>();
	
	public boolean clear(int index, int partType)
	{
		return eventRoomMap.remove(new RoomIndex(index, partType)) != null;
	}
	
	public synchronized boolean addParty(int index, int partType, Party party)
	{
		if (party == null)
			return false;
		
		if (party.getMembersCount() == 0)
			return false;
		
		final RoomIndex rindex = new RoomIndex(index, partType);
		final Party oldParty = eventRoomMap.get(rindex);
		if (oldParty != null && oldParty.getMembersCount() > 0)
			return false;
		
		eventRoomMap.put(rindex, party);
		return true;
	}
	
	public synchronized Party getParty(int index, int partType)
	{
		final RoomIndex rindex = new RoomIndex(index, partType);
		final Party party = eventRoomMap.get(rindex);
		if (party != null)
		{
			if (party.getMembersCount() > 0)
				return party;
			else
				eventRoomMap.remove(rindex);
		}
		return null;
	}
	
	record RoomIndex(int index, int partType)
	{
	}
	
	public static TimeAttackEventRoom getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private final static TimeAttackEventRoom INSTANCE = new TimeAttackEventRoom();
	}
}