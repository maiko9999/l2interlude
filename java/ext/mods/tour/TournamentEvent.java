package ext.mods.tour;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

import ext.mods.tour.battle.TournamentManager;
import ext.mods.tour.holder.TourConfig;
import ext.mods.tour.holder.TourHolder;
import ext.mods.tour.ranking.TournamentRankingManager;

public class TournamentEvent
{
	public static Logger LOGGER = Logger.getLogger(TournamentEvent.class.getName());
	private static ScheduledFuture<?> eventChecker;
	private static ScheduledFuture<?> fightRepeater;
	private static boolean isRunning;
	private static boolean isRunningRegister;
	private static String lastEventTime;
	
	public static void start()
	{
		if (eventChecker == null || eventChecker.isCancelled())
			eventChecker = ThreadPool.scheduleAtFixedRate(() -> checkAndStartEvent(), 500, 1000);
	}
	
	private static void checkAndStartEvent()
	{
		LocalDateTime now = LocalDateTime.now();
		int currentDay = now.getDayOfWeek().getValue() % 7; // 0 = domingo
		
		TourConfig config = TourData.getInstance().getConfig();
		
		if (!config.isEnabled() || !config.getDays().contains(currentDay))
			return;
		
		String nowStr = new SimpleDateFormat("HH:mm").format(new Date());
		
		for (String time : config.getTimes())
		{
			if (nowStr.equals(time) && !isRunning && !nowStr.equals(lastEventTime))
			{
				isRunning = true;
				lastEventTime = nowStr;
				World.announceToOnlinePlayers("[TOURNAMENT] Registration is now OPEN! The battles will start in " + config.getPreparation() + " minutes.", true);
				ThreadPool.schedule(() -> startFight(), 1000 * 60 * config.getPreparation());
				
				break;
			}
		}
	}
	
	private static void startFight()
	{
		TourConfig config = TourData.getInstance().getConfig();
		Math();
		isRunningRegister = true;
		World.announceToOnlinePlayers("[TOURNAMENT] The battles have started! The event will end in " + config.getDuration() + " minutes.", true);
		ThreadPool.schedule(() -> endEvent(), 1000 * 60 * config.getDuration());
	}
	
	private static void Math()
	{
		if (fightRepeater == null || fightRepeater.isCancelled())
			fightRepeater = ThreadPool.scheduleAtFixedRate(() -> checkFights(), 500, 1000);
		
	}
	
	private static void checkFights()
	{
		List<TourHolder> battles = TourData.getInstance().getBattleName("battle");
		
		for (TourHolder holder : battles)
		{
			int playersCount = holder.getCount();
			List<Player> selectedPlayers = TournamentManager.getInstance().selectPlayers(playersCount);
			
			if (selectedPlayers.size() < playersCount)
			{
				continue;
			}
			TournamentManager.getInstance().startBattle(selectedPlayers, holder);
		}
	}
	
	private static void endEvent()
	{
		isRunningRegister = false;
		if (fightRepeater != null)
		{
			fightRepeater.cancel(false);
			fightRepeater = null;
		}
		
		TournamentManager.getInstance().endAllBattles();
		
		TourConfig config = TourData.getInstance().getConfig();

		World.announceToOnlinePlayers("[TOURNAMENT] The event has ended! The next tournament will begin at " + String.join(", ", config.getTimes()) + ".", true);
		
		isRunning = false;
		lastEventTime = "";
		TournamentRankingManager.getInstance().rewardTopPlayers();
		TournamentRankingManager.getInstance().clearRankings();
	}
	
	
	public static boolean isRunning()
	{
		return isRunning;
	}
	
	public static boolean isRunningRegister()
	{
		return isRunningRegister;
	}
	
	public static String lastEvent()
	{
		return lastEventTime;
	}
	
	public static void reset()
	{
		if (eventChecker != null)
		{
			eventChecker.cancel(false);
			eventChecker = null;
		}
	}
	
}