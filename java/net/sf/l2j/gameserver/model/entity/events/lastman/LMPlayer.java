package net.sf.l2j.gameserver.model.entity.events.lastman;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;

public class LMPlayer
{
	private Player _player;
	private short _points;
	private short _credits;
	private String _hexCode;
	
	public LMPlayer(Player player, String hexCode)
	{
		_player = player;
		_points = 0;
		_credits = Config.LM_EVENT_PLAYER_CREDITS;
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
	
	public short getCredits()
	{
		return _credits;
	}
	
	public void setCredits(short credits)
	{
		_credits = credits;
	}
	
	public void decreaseCredits()
	{
		--_credits;
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
	
	public String getHexCode()
	{
		return _hexCode;
	}
}