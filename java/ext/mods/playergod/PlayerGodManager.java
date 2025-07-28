package ext.mods.playergod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

import ext.mods.playergod.data.PlayerGodData;
import ext.mods.playergod.holder.PlayerGodHolder;

public class PlayerGodManager
{
	private static final String GOD_MEMO_KEY = "PlayerGodTimeLeft";
	private static final PlayerGodManager INSTANCE = new PlayerGodManager();
	
	private final Map<Integer, Integer> playerKills = new ConcurrentHashMap<>();
	private final Map<Integer, Long> playerFirstKillTime = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
	
	public static PlayerGodManager getInstance()
	{
		return INSTANCE;
	}
	
	public void onKill(Player killer)
	{
		final PlayerGodHolder config = PlayerGodData.getInstance().getHolder();
		if (!config.isEnabled())
			return;
		
		final int playerId = killer.getObjectId();
		
		// Início da contagem
		playerFirstKillTime.putIfAbsent(playerId, System.currentTimeMillis());
		long elapsed = (System.currentTimeMillis() - playerFirstKillTime.get(playerId)) / 1000;
		
		// Se passou o tempo limite, reinicia
		if (elapsed > config.getTimeWindow())
		{
			playerFirstKillTime.put(playerId, System.currentTimeMillis());
			playerKills.put(playerId, 1); // nova kill
			return;
		}
		
		// Incrementa kill
		playerKills.merge(playerId, 1, Integer::sum);
		
		if (playerKills.get(playerId) >= config.getKillsRequired())
		{
			applyGodMode(killer, config.getHeroAuraDuration());
			
			// Limpa cache
			playerKills.remove(playerId);
			playerFirstKillTime.remove(playerId);
		}
	}
	
	public void applyGodMode(Player player, int durationSeconds)
	{
		final int playerId = player.getObjectId();
		
		// Evita múltiplas tasks concorrentes
		cancelExistingTask(playerId);
		
		player.setHeroAura(true);
		player.broadcastUserInfo();
		
		player.getMemos().set(GOD_MEMO_KEY, durationSeconds);
		
		// Inicia nova task de contagem regressiva e armazena
		ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() -> reduceGodTime(player), 1000, 1000);
		activeTasks.put(playerId, task);
		
		final PlayerGodHolder config = PlayerGodData.getInstance().getHolder();
		if (!config.isEnabled())
			return;
		
		String timeFormatted = formatTime(config.getTimeWindow());
		
		String announce = config.getKillAnnouncement().replace("%player_name%", player.getName()).replace("%kills%", String.valueOf(config.getKillsRequired())).replace("%time%", timeFormatted);
		
		World.announceToOnlinePlayers(announce, true);
		
	}
	
	private void reduceGodTime(Player player)
	{
		if (player == null || !player.isOnline())
			return;
		
		int playerId = player.getObjectId();
		int timeLeft = player.getMemos().getInteger(GOD_MEMO_KEY, 0);
		
		if (timeLeft <= 1)
		{
			player.getMemos().unset(GOD_MEMO_KEY);
			player.setHeroAura(false);
			player.broadcastUserInfo();
			
			cancelExistingTask(playerId); // remove task finalizada
		}
		else
		{
			player.getMemos().set(GOD_MEMO_KEY, timeLeft - 1);
		}
	}
	
	private void cancelExistingTask(int playerId)
	{
		ScheduledFuture<?> existing = activeTasks.remove(playerId);
		if (existing != null && !existing.isCancelled())
			existing.cancel(true);
	}
	
	public void onEnterWorld(Player player)
	{
		final PlayerGodHolder config = PlayerGodData.getInstance().getHolder();
		if (!config.isEnabled())
			return;
		
		int timeLeft = player.getMemos().getInteger(GOD_MEMO_KEY, 0);
		if (timeLeft > 0)
		{
			final int playerId = player.getObjectId();
			
			// Evita múltiplas tasks duplicadas
			cancelExistingTask(playerId);
			
			player.setHeroAura(true);
			player.broadcastUserInfo();
			
			if (config.isLoginAnnouncementEnabled())
			{
				String msg = config.getLoginMessage().replace("%player_name%", player.getName());
				World.announceToOnlinePlayers(msg, true);
			}
			
			// Inicia nova task
			ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() -> reduceGodTime(player), 1000, 1000);
			activeTasks.put(playerId, task);
		}
	}
	
	private String formatTime(int seconds)
	{
		int minutes = seconds / 60;
		int sec = seconds % 60;
		
		if (minutes > 0 && sec > 0)
			return String.format("%d min %d s", minutes, sec);
		else if (minutes > 0)
			return String.format("%d min", minutes);
		else
			return String.format("%d s", sec);
	}
	
}
