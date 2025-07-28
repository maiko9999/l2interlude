package ext.mods.FarmEventRandom;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.model.zone.type.RandomZone;

import ext.mods.FarmEventRandom.holder.DropHolder;
import ext.mods.FarmEventRandom.holder.MessagesHolder;
import ext.mods.FarmEventRandom.holder.RamdomConfig;
import ext.mods.FarmEventRandom.holder.RandomSpawnHolder;

public class RandomManager
{
	private static final CLogger LOGGER = new CLogger(RandomManager.class.getName());
	private static final List<RandomZone> activeZones = Collections.synchronizedList(new ArrayList<>());
	private static final List<Spawn> activeSpawns = Collections.synchronizedList(new ArrayList<>());
	private static final List<Npc> oldMonsters = Collections.synchronizedList(new ArrayList<>());
	
	private static ScheduledFuture<?> eventChecker;
	private static ScheduledFuture<?> screenMessageTask;
	private static ScheduledFuture<?> zoneSwitchTask;
	
	private static boolean isRunning;
	private static String lastEventTime;
	private static long timeRemaining;
	
	private static final RamdomConfig config = RandomData.getInstance().getConfig();
	private static final MessagesHolder messages = RandomData.getInstance().getMessages(config.getName());
	private static final int COUNTDOWN_INTERVAL = 1000;
	
	public void start()
	{
		if (eventChecker == null || eventChecker.isCancelled())
		{
			eventChecker = ThreadPool.scheduleAtFixedRate(this::checkStartEvent, 1000, 1000);
		}
	}
	
	private void checkStartEvent()
	{
		if (!config.isEnabled() || !config.isTimeToRun())
			return;
		
		String currentTime = LocalTime.now().withSecond(0).withNano(0).toString();
		if (currentTime.equals(lastEventTime) || isRunning)
			return;
		
		lastEventTime = currentTime;
		isRunning = true;
		
		Map<String, String> data = new HashMap<>();
		
		if (config.getPrepareMinutes() < 1)
			data.put("time", config.getPrepareMinutes() * 60 + " segundos");
		else
			data.put("time", config.getPrepareMinutes() + " minutos");
		
		for (String msg : messages.getOnPrepare())
		{
			String formatted = formatMessage(msg, data);
			World.announceToOnlinePlayers(config.getName() + ": " + formatted, true);
		}
		
		cleanPreviousMonsters();
		startZoneCleaner();
		ThreadPool.schedule(this::activateZonesAndSpawn, config.getPrepareMinutes() * 60L * 1000L);
	}
	
	private static ScheduledFuture<?> zoneCleanerTask;
	
	private void startZoneCleaner()
	{
		if (zoneCleanerTask != null && !zoneCleanerTask.isDone())
		{
			zoneCleanerTask.cancel(true);
		}
		
		zoneCleanerTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			for (RandomZone zone : activeZones)
			{
				for (Attackable npc : zone.getKnownTypeInside(Attackable.class))
				{
					if (npc == null || npc.isDead() || npc.isDecayed())
						continue;
					
					// Se não for monstro do evento, e não está nos antigos (pré-evento)
					if (!activeSpawns.stream().anyMatch(spawn -> spawn.getNpc() == npc) && !oldMonsters.contains(npc))
					{
						npc.getSpawn().setRespawnDelay(0);
						npc.deleteMe();
					}
				}
			}
		}, 1000, 5000); // Primeiro delay: 1s, depois executa a cada 5 segundos
	}
	
	private void activateZonesAndSpawn()
	{
		
		List<RandomZone> allZones = new ArrayList<>(ZoneManager.getInstance().getAllZones(RandomZone.class));
		
		if (allZones.isEmpty())
		{
			LOGGER.warn("{} Nenhuma zona do tipo RandomZone encontrada.", config.getName());
			return;
		}
		
		Collections.shuffle(allZones);
		for (int i = 0; i < Math.min(config.getZoneValue(), allZones.size()); i++)
		{
			RandomZone zone = allZones.get(i);
			if (!activeZones.contains(zone))
			{
				zone.setActive(true);
				activeZones.add(zone);
				
				for (String msg : messages.getOnZone())
				{
					String formatted = msg.replace("%zone%", zone.getName());
					World.announceToOnlinePlayers(config.getName() + ": " + formatted, true);
				}
			}
		}
		
		if (activeZones.isEmpty())
			return;
		
		spawnEventMonsters();
		startCountdown();
		scheduleZoneSwitch();
	}
	
	private void spawnEventMonsters()
	{
		for (RandomZone zone : activeZones)
		{
			List<RandomSpawnHolder> spawns = RandomData.getInstance().getSpawns(config.getName(), zone.getId());
			for (RandomSpawnHolder holder : spawns)
			{
				for (int i = 0; i < holder.getCount(); i++)
				{
					try
					{
						NpcTemplate template = NpcData.getInstance().getTemplate(holder.getNpcId());
						if (template == null)
							continue;
						
						int x = holder.getX();
						int y = holder.getY();
						int z = holder.getZ();
						
						if (holder.getCount() > 1)
						{
							int radius = holder.getRange();
							double angle = Rnd.nextDouble() * 2 * Math.PI;
							x += (int) (Math.cos(angle) * Rnd.get(0, radius));
							y += (int) (Math.sin(angle) * Rnd.get(0, radius));
						}
						
						Spawn spawn = new Spawn(template);
						spawn.setLoc(x, y, z, Rnd.get(65535));
						spawn.setRespawnDelay(holder.getRespawnDelay());
						SpawnManager.getInstance().addSpawn(spawn);
						spawn.doSpawn(false);
						activeSpawns.add(spawn);
					}
					catch (Exception e)
					{
						LOGGER.warn("Erro ao spawnar NPC: {}", e.getMessage(), e);
					}
				}
			}
		}
		int intervalHours = config.getInterval();
		String durationText = intervalHours + (intervalHours == 1 ? " hora" : " hora(s)");
		
		Map<String, String> data = new HashMap<>();
		data.put("time", durationText);
		
		for (String msg : messages.getOnStart())
		{
			String formatted = formatMessage(msg, data);
			World.announceToOnlinePlayers(config.getName() + ": " + formatted, true);
		}
		
		for (Npc npc : oldMonsters)
		{
			if (npc != null && !npc.isDead() && !npc.isDecayed())
			{
				npc.getSpawn().setRespawnDelay(0);
				npc.deleteMe();
			}
		}
		
	}
	
	private void startCountdown()
	{
		timeRemaining = config.getInterval() * 60L * 60L * 1000L;
		
		if (screenMessageTask != null && !screenMessageTask.isCancelled())
			screenMessageTask.cancel(true);
		
		screenMessageTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			if (timeRemaining <= 0)
			{
				screenMessageTask.cancel(true);
				return;
			}
			
			if (timeRemaining % 300000 == 0 || (timeRemaining <= 60000 && timeRemaining % 10000 == 0))
			{
				long seconds = timeRemaining / 1000 % 60;
				long minutes = timeRemaining / (1000 * 60) % 60;
				long hours = timeRemaining / (1000 * 60 * 60);
				if (!messages.getOnAuto().isEmpty())
				{
					String template = messages.getOnAuto().get(0);
					String message = template.replace("%dh", hours + "h").replace("%dm", minutes + "m").replace("%ds", seconds + "s");
					World.announceToOnlinePlayers(config.getName() + ": " + message, true);
				}
			}
			timeRemaining -= COUNTDOWN_INTERVAL;
		}, 1000, 1000);
	}
	
	private void scheduleZoneSwitch()
	{
		if (zoneSwitchTask != null && !zoneSwitchTask.isCancelled())
			zoneSwitchTask.cancel(true);
		
		zoneSwitchTask = ThreadPool.schedule(() ->
		{
			stopZones();
			isRunning = false;
		}, config.getInterval() * 60L * 60L * 1000L);
	}
	
	private void cleanPreviousMonsters()
	{
		for (RandomZone zone : activeZones)
		{
			for (Npc npc : zone.getKnownTypeInside(Attackable.class))
			{
				if (npc != null)
					oldMonsters.add(npc);
			}
		}
		
	}
	
	private void stopZones()
	{
		Map<String, String> data = new HashMap<>();
		data.put("time", String.valueOf(Math.round(config.getInterval() / 60.0)));
		data.put("next_time", getNextEventTimeFormatted());
		
		for (String msg : messages.getOnEnd())
		{
			String formatted = formatMessage(msg, data);
			World.announceToOnlinePlayers(config.getName() + ": " + formatted, true);
		}
		
		if (zoneCleanerTask != null && !zoneCleanerTask.isDone())
		{
			zoneCleanerTask.cancel(true);
		}
		
		for (RandomZone zone : activeZones)
		{
			zone.setActive(false);
		}
		activeZones.clear();
		
		for (Spawn spawn : activeSpawns)
		{
			if (spawn != null)
			{
				spawn.setRespawnDelay(0);
				spawn.getNpc().deleteMe();
				SpawnManager.getInstance().deleteSpawn(spawn);
			}
		}
		activeSpawns.clear();
		
		for (Npc npc : oldMonsters)
		{
			if (npc != null && !npc.isDead() && !npc.isDecayed())
			{
				
				try
				{
					NpcTemplate template = NpcData.getInstance().getTemplate(npc.getNpcId());
					if (template == null)
						continue;
					
					int x = npc.getX();
					int y = npc.getY();
					int z = npc.getZ();
					
					int radius = 75;
					double angle = Rnd.nextDouble() * 2 * Math.PI;
					x += (int) (Math.cos(angle) * Rnd.get(0, radius));
					y += (int) (Math.sin(angle) * Rnd.get(0, radius));
					
					Spawn spawn = new Spawn(template);
					spawn.setLoc(x, y, z, Rnd.get(65535));
					spawn.setRespawnDelay(npc.getSpawn().getRespawnDelay());
					spawn.doSpawn(false);
					SpawnManager.getInstance().addSpawn(spawn);
				}
				catch (Exception e)
				{
					LOGGER.warn("Erro ao spawnar NPC: {}", e.getMessage(), e);
				}
				
			}
		}
		oldMonsters.clear();
	}
	
	public void onKill(Player player, Attackable monster)
	{
		if (player != null && monster != null)
		{
			if (timeRemaining > 0)
			{
				for (RandomZone zone : activeZones)
				{
					List<RandomSpawnHolder> spawns = RandomData.getInstance().getSpawns(config.getName(), zone.getId());
					
					if (spawns == null || spawns.isEmpty())
					{
						LOGGER.warn("No spawns found for zone ID: " + zone.getId());
						continue;
					}
					
					for (RandomSpawnHolder spawn : spawns)
					{
						if (spawn.getNpcId() == monster.getNpcId())
						{
							if (monster.isInsideZone(ZoneId.RANDOM))
							{
								for (DropHolder drop : spawn.getDrops())
								{
									if (Rnd.get(100) < drop.getChance())
									{
										int totalAmount = drop.getCount();
										
										if (player.isInParty())
										{
											List<Player> members = player.getParty().getMembers();
											int size = members.size();
											
											int baseAmount = totalAmount / size;
											int remainder = totalAmount % size;
											
											for (Player member : members)
											{
												int amount = baseAmount;
												if (remainder > 0)
												{
													amount++;
													remainder--;
												}
												
												if (amount > 0)
													member.addItem(drop.getItemId(), amount, true);
											}
										}
										else
										{
											player.addItem(drop.getItemId(), totalAmount, true);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
	}
	
	public static String formatMessage(String template, Map<String, String> values)
	{
		for (Map.Entry<String, String> entry : values.entrySet())
		{
			template = template.replace("%" + entry.getKey() + "%", entry.getValue());
		}
		return template;
	}
	
	private String getNextEventTimeFormatted()
	{
		LocalDateTime now = LocalDateTime.now();
		LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);
		int currentDay = now.getDayOfWeek().getValue() % 7;
		
		for (int i = 0; i < 7; i++)
		{
			int dayToCheck = (currentDay + i) % 7;
			
			if (config.getActiveDays().contains(dayToCheck))
			{
				List<LocalTime> sortedTimes = config.getActiveTimes().stream().sorted().collect(Collectors.toList());
				
				for (LocalTime time : sortedTimes)
				{
					if (i > 0 || time.isAfter(currentTime))
					{
						LocalDateTime nextEvent = now.plusDays(i).with(time);
						return nextEvent.format(DateTimeFormatter.ofPattern("EEE HH:mm"));
					}
				}
			}
		}
		return "unknown";
	}
	
	public static RandomManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RandomManager _instance = new RandomManager();
	}
	
}
