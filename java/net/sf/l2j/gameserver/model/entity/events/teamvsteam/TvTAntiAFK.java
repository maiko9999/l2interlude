package net.sf.l2j.gameserver.model.entity.events.teamvsteam;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public class TvTAntiAFK
{
	private final ConcurrentHashMap<String, PlayerInfo> _player = new ConcurrentHashMap<>();
	
	private TvTAntiAFK()
	{
		ThreadPool.scheduleAtFixedRate(() -> checkPlayers(), 20000, 20000);
	}
	
	private void checkPlayers()
	{
		if (TvTEvent.getInstance().isStarted())
			Arrays.stream(TvTEvent.getInstance()._teams).flatMap(team -> team.getParticipatedPlayers().values().stream()).filter(player -> player != null && player.isOnline() && !player.isDead() && !player.isImmobilized() && !player.isParalyzed()).forEach(player -> addTvTSpawnInfo(player, player.getName(), player.getX(), player.getY(), player.getZ()));
		else
			_player.clear();
	}
	
	private void addTvTSpawnInfo(Player player, String name, int x, int y, int z)
	{
		_player.compute(name, (key, playerInfo) ->
		{
			if (playerInfo == null)
				return new PlayerInfo(x, y, z, 1);
			else
			{
				if (playerInfo.isSameLocation(x, y, z) && !player.getAttack().isAttackingNow() && !player.getCast().isCastingNow())
				{
					if (playerInfo.incrementAndGetSameLoc() >= 4)
					{
						TvTEvent.getInstance().onLogout(player);
						kickedMsg(player);
						return null;
					}
				}
				else
					return new PlayerInfo(x, y, z, 1);
				return playerInfo;
			}
		});
	}
	
	private void kickedMsg(Player player)
	{
		player.sendPacket(new ExShowScreenMessage("You're kicked out of the TvT by staying afk!", 6000));
	}
	
	private static class PlayerInfo
	{
		private int _x, _y, _z, _sameLoc;
		
		public PlayerInfo(int x, int y, int z, int sameLoc)
		{
			_x = x;
			_y = y;
			_z = z;
			_sameLoc = sameLoc;
		}
		
		public boolean isSameLocation(int x, int y, int z)
		{
			return _x == x && _y == y && _z == z;
		}
		
		public int incrementAndGetSameLoc()
		{
			return ++_sameLoc;
		}
	}
	
	public static final TvTAntiAFK getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TvTAntiAFK INSTANCE = new TvTAntiAFK();
	}
}