package net.sf.l2j.gameserver.model.entity.events.lastman;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

public class LMManager
{
	protected static final Logger _log = Logger.getLogger(LMManager.class.getName());
	
	private LMStartTask _task;
	
	public static final int JOIN_LM_REQ_ID = 100_003;
	
	public Calendar _nextEvent;
	
	private LMManager()
	{
		if (Config.LM_EVENT_ENABLED)
		{
			LMEvent.getInstance().init();
			
			scheduleEventStart();
			_log.info("Last Man Engine: is Started.");
		}
		else
			_log.info("Last Man Engine: Engine is disabled.");
	}
	
	public void scheduleEventStart()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			
			for (String timeOfDay : Config.LM_EVENT_INTERVAL)
			{
				// Creating a Calendar object from the specified interval value
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, Integer.parseInt("00"));
				
				// If the date is in the past, make it the next day (Example: Checking for "1:00", when the time is 23:57.)
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				
				// Check for the test date to be the minimum (smallest in the specified list)
				if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
					nextStartTime = testStartTime;
			}
			
			_nextEvent = testStartTime;
			
			if (nextStartTime != null)
			{
				_task = new LMStartTask(nextStartTime.getTimeInMillis());
				ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			_log.warning("LMEventEngine: Error figuring out a start time. Check LMEventInterval in config file.");
		}
	}
	
	public void startReg()
	{
		if (!LMEvent.getInstance().startParticipation())
		{
			World.announceToOnlinePlayers("Last Man: Event was cancelled.");
			_log.warning("LMEventEngine: Error spawning event npc for participation.");
			
			scheduleEventStart();
		}
		else
		{
			World.announceToOnlinePlayers("Last Man: Joinable in " + Config.LM_NPC_LOC_NAME + "!");
			
			if (Config.EVENT_COMMANDS)
				World.announceToOnlinePlayers("Last Man: Command: .lmjoin / .lmleave / .lminfo");
			
			if (Config.ALLOW_LM_DLG)
			{
				for (Player players : World.getInstance().getPlayers())
				{
					if (OlympiadManager.getInstance().isRegistered(players) || players.isAlikeDead() || players.isTeleporting() || players.isDead() || players.isInObserverMode() || players.isInStoreMode() || LMEvent.getInstance().isStarted() && LMEvent.getInstance().isPlayerParticipant(players.getObjectId()) || players.isInsideZone(ZoneId.CASTLE) || players.isInsideZone(ZoneId.SIEGE) || players.getClassId() == ClassId.BISHOP || players.getClassId() == ClassId.CARDINAL || players.getClassId() == ClassId.SHILLIEN_ELDER || players.getClassId() == ClassId.SHILLIEN_SAINT || players.getClassId() == ClassId.ELVEN_ELDER || players.getClassId() == ClassId.EVAS_SAINT || players.getClassId() == ClassId.PROPHET || players.getClassId() == ClassId.HIEROPHANT)
						continue;
					
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1);
					confirm.addString("Do you wish to Join LM Event?");
					confirm.addTime(45000);
					confirm.addRequesterId(JOIN_LM_REQ_ID);
					players.sendPacket(confirm);
				}
			}
			
			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + (60000L * Config.LM_EVENT_PARTICIPATION_TIME));
			ThreadPool.execute(_task);
		}
	}
	
	public void startEvent()
	{
		if (!LMEvent.getInstance().startFight())
		{
			World.announceToOnlinePlayers("Last Man: Event cancelled due to lack of Participation.");
			scheduleEventStart();
		}
		else
		{
			LMEvent.getInstance().sysMsgToAllParticipants("Teleporting participants in " + Config.LM_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + 60000L * Config.LM_EVENT_RUNNING_TIME);
			ThreadPool.execute(_task);
		}
	}
	
	public void endEvent()
	{
		World.announceToOnlinePlayers(LMEvent.getInstance().calculateRewards());
		LMEvent.getInstance().sysMsgToAllParticipants("Teleporting back to town in " + Config.LM_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		LMEvent.getInstance().stopFight();
		scheduleEventStart();
	}
	
	public void skipDelay()
	{
		if (_task == null)
			return;
		
		if (_task.nextRun.cancel(false))
		{
			_task.setStartTime(System.currentTimeMillis());
			ThreadPool.execute(_task);
		}
	}
	
	class LMStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;
		
		public LMStartTask(long startTime)
		{
			_startTime = startTime;
		}
		
		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}
		
		@Override
		public void run()
		{
			int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);
			
			if (delay > 0)
				announce(delay);
			
			int nextMsg = 0;
			if (delay > 3600)
				nextMsg = delay - 3600;
			else if (delay > 1800)
				nextMsg = delay - 1800;
			else if (delay > 900)
				nextMsg = delay - 900;
			else if (delay > 600)
				nextMsg = delay - 600;
			else if (delay > 300)
				nextMsg = delay - 300;
			else if (delay > 60)
				nextMsg = delay - 60;
			else if (delay > 5)
				nextMsg = delay - 5;
			else if (delay > 0)
				nextMsg = delay;
			else
			{
				if (LMEvent.getInstance().isInactive())
					startReg();
				else if (LMEvent.getInstance().isParticipating())
					startEvent();
				else
					endEvent();
			}
			
			if (delay > 0)
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
		}
		
		private void announce(long time)
		{
			if (time >= 3600 && time % 3600 == 0)
			{
				if (LMEvent.getInstance().isParticipating())
					World.announceToOnlinePlayers("Last Man: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				else if (LMEvent.getInstance().isStarted())
					LMEvent.getInstance().sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
			}
			else if (time >= 60)
			{
				if (LMEvent.getInstance().isParticipating())
					World.announceToOnlinePlayers("Last Man: " + (time / 60) + " minute(s) until registration is closed!");
				else if (LMEvent.getInstance().isStarted())
					LMEvent.getInstance().sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
			}
			else
			{
				if (LMEvent.getInstance().isParticipating())
					World.announceToOnlinePlayers("Last Man: " + time + " second(s) until registration is closed!");
				else if (LMEvent.getInstance().isStarted())
					LMEvent.getInstance().sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
			}
		}
	}
	
	public String getNextTime()
	{
		Calendar currentTime = Calendar.getInstance();
		for (String timeOfDay : Config.LM_EVENT_INTERVAL)
		{
			String[] splitTimeOfDay = timeOfDay.split(":");
			int eventHour = Integer.parseInt(splitTimeOfDay[0]);
			int eventMinute = Integer.parseInt(splitTimeOfDay[1]);
			
			Calendar eventTime = Calendar.getInstance();
			eventTime.setLenient(true);
			eventTime.set(Calendar.HOUR_OF_DAY, eventHour);
			eventTime.set(Calendar.MINUTE, eventMinute);
			eventTime.set(Calendar.SECOND, 0);
			
			if (eventTime.getTimeInMillis() > currentTime.getTimeInMillis())
			{
				// If the event time is in the future, return its formatted representation.
				SimpleDateFormat format = new SimpleDateFormat("HH:mm");
				return format.format(eventTime.getTime());
			}
		}
		
		// If no future event time is found, return the first interval (00:00) for the next day.
		return "00:00";
	}
	
	public static final LMManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LMManager INSTANCE = new LMManager();
	}
}