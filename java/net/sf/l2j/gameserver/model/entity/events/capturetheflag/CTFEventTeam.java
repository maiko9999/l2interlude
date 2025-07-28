package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.actor.Player;

public class CTFEventTeam
{
	private final String _name;
	
	private int[] _coordinates = new int[3];
	
	private short _points;
	
	private final Map<Integer, Player> _participatedPlayers = new ConcurrentHashMap<>();
	
	public CTFEventTeam(String name, int[] coordinates)
	{
		_name = name;
		_coordinates = coordinates;
		_points = 0;
	}
	
	public boolean addPlayer(Player player)
	{
		if (player == null)
			return false;
		
		_participatedPlayers.put(player.getObjectId(), player);
		return true;
	}
	
	public void removePlayer(int objectId)
	{
		_participatedPlayers.remove(objectId);
	}
	
	public void increasePoints()
	{
		++_points;
	}
	
	public void cleanMe()
	{
		_participatedPlayers.clear();
		_points = 0;
	}
	
	public boolean containsPlayer(int objectId)
	{
		return _participatedPlayers.containsKey(objectId);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int[] getCoordinates()
	{
		return _coordinates;
	}
	
	public short getPoints()
	{
		return _points;
	}
	
	public Map<Integer, Player> getParticipatedPlayers()
	{
		return _participatedPlayers;
	}
	
	public int getParticipatedPlayerCount()
	{
		return _participatedPlayers.size();
	}
}