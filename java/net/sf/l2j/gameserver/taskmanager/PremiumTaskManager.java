package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;

public class PremiumTaskManager implements Runnable
{
	private final Map<Player, Long> _players = new ConcurrentHashMap<>();
	
	public PremiumTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 10000, 10000);
	}
	
	public void add(Player player)
	{
		_players.put(player, System.currentTimeMillis());
	}
	
	@Override
	public void run()
	{
		if (_players.isEmpty())
			return;
		
		for (Player player : _players.keySet())
			player.restorePremServiceData(player, player.getAccountName());
	}
	
	public static final PremiumTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PremiumTaskManager INSTANCE = new PremiumTaskManager();
	}
}