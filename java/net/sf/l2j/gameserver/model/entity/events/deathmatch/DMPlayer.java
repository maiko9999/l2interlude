package net.sf.l2j.gameserver.model.entity.events.deathmatch;

import net.sf.l2j.gameserver.model.actor.Player;

public class DMPlayer
{
	private Player _player;
	private short _points;
	private short _death;
	private String _hexCode;
	
	public DMPlayer(Player player, String hexCode)
	{
		_player = player;
		_points = 0;
		_death = 0;
		_hexCode = hexCode;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public void setPlayer(Player player)
	{
		_player = player;
	}
	
	public short getPoints()
	{
		return _points;
	}
	
	public void setPoints(short points)
	{
		_points = points;
	}
	
	public void increasePoints()
	{
		++_points;
	}
	
	public short getDeath()
	{
		return _death;
	}
	
	public void setDeath(short death)
	{
		_death = death;
	}
	
	public void increaseDeath()
	{
		++_death;
	}
	
	public String getHexCode()
	{
		return _hexCode;
	}
}