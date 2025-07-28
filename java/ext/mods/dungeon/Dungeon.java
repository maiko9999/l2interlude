package ext.mods.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.L2Skill;

import ext.mods.InstanceMap.InstanceManager;
import ext.mods.InstanceMap.MapInstance;
import ext.mods.dungeon.holder.SpawnTemplate;
import ext.mods.dungeon.holder.StageTemplate;

public class Dungeon
{
	
	private final DungeonTemplate template;
	private final List<Player> players;
	private final Map<Monster, SpawnTemplate> mobToTemplate = new ConcurrentHashMap<>();
	private ScheduledFuture<?> dungeonCancelTask;
	private ScheduledFuture<?> nextTask;
	private ScheduledFuture<?> timerTask;
	
	private long stageBeginTime;
	private final MapInstance instance;
	private StageTemplate currentStage;
	private List<SpawnTemplate> currentSpawns;
	private int currentStageIndex = 0;
	
	private ScheduledFuture<?> monitorTask;
	
	public Dungeon(DungeonTemplate template, List<Player> players)
	{
		this.template = template;
		this.players = players;
		this.instance = InstanceManager.getInstance().createInstance();
		beginTeleport();
		
	}
	
	private void beginTeleport()
	{
		if (!getNextStage())
		{
			broadcastScreenMessage("Failed to load dungeon stage!", 5);
			cancelDungeon();
			return;
		}
		
		broadcastScreenMessage("You will be teleported in a few seconds!", 5);
		
		L2Skill skill = SkillTable.getInstance().getInfo(1050, 1);
		
		for (Player player : players)
		{
			player.setIsParalyzed(true);
			player.broadcastPacketInRadius(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), 9000, 5000), 1500);
			player.setInstanceMap(instance, true);
			player.getCast().doCast(skill, player, null);
			player.setDungeon(this);
			
			ThreadPool.schedule(() -> player.setIsParalyzed(false), 10000);
		}
		
		nextTask = ThreadPool.schedule(this::teleportPlayers, 10000);
	}
	
	private void teleportPlayers()
	{
		teleToStage();
		broadcastScreenMessage("Stage " + currentStage.order + " begins in 10 seconds!", 5);
		nextTask = ThreadPool.schedule(this::beginStage, 10 * 1000);
	}
	
	private void beginStage()
	{
		for (SpawnTemplate spawn : currentSpawns)
		{
			for (int i = 0; i < spawn.count; i++)
			{
				NpcTemplate template = NpcData.getInstance().getTemplate(spawn.npcId);
				try
				{
					Spawn spawns = new Spawn(template);
					
					int x = spawn.x;
					int y = spawn.y;
					int z = spawn.z;
					
					if (spawn.count > 1)
					{
						final int radius = spawn.range;
						final double angle = Rnd.nextDouble() * 2 * Math.PI;
						x += (int) (Math.cos(angle) * Rnd.get(0, radius));
						y += (int) (Math.sin(angle) * Rnd.get(0, radius));
					}
					
					Location loc = new Location(x, y, z);
					
					spawns.setLoc(loc.getX(), loc.getY(), loc.getZ(), 0);
					
					spawns.doSpawn(false);
					((Monster) spawns.getNpc()).setDungeon(this);
					spawns.getNpc().setInstanceMap(instance, false);
					spawns.getNpc().setTitle(spawn.title);
					SpawnManager.getInstance().addSpawn(spawns);
					
					spawns.getNpc().broadcastPacket(new NpcInfo(spawns.getNpc(), null));
					
					mobToTemplate.put(((Monster) spawns.getNpc()), spawn);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		broadcastScreenMessage("You have " + currentStage.timeLimit + " minutes to finish stage " + currentStage.order + "!", 5);
		stageBeginTime = System.currentTimeMillis();
		updatePlayerStage(currentStage.order);
		timerTask = ThreadPool.scheduleAtFixedRate(this::broadcastTimer, 5 * 1000, 1000);
		dungeonCancelTask = ThreadPool.schedule(this::cancelDungeon, 1000 * 60 * currentStage.timeLimit);
		monitorTask = ThreadPool.scheduleAtFixedRate(this::monitorDungeon, 5000, 5000);
		
	}
	
	private void monitorDungeon()
	{
		
		boolean allDead = true;
		
		for (Player player : players)
		{
			if (player == null)
				continue;
			
			if (!player.isDead())
				allDead = false;
		}
		
		if (allDead)
		{
			cancelDungeon();
		}
	}
	
	private boolean advanceToNextStage()
	{
		if (!getNextStage())
			return false;
		
		if (currentStage != null)
		{
			ThreadPool.schedule(this::teleToStage, 5 * 1000);
			return true;
		}
		return false;
	}
	
	private void teleToStage()
	{
		if (currentStage.teleport)
		{
			for (Player player : players)
			{
				player.teleportTo(currentStage.x, currentStage.y, currentStage.z, 0);
			}
		}
	}
	
	public void cancelDungeon()
	{
		for (Attackable mob : mobToTemplate.keySet())
		{
			deleteMob(mob);
		}
		if (timerTask != null)
			timerTask.cancel(true);
		
		broadcastScreenMessage("You have failed to complete the dungeon. You will be teleported back in 5 seconds.", 5);
		ThreadPool.schedule(this::teleToTown, 5 * 1000);
		
	}
	
	private static void deleteMob(Attackable mob)
	{
		ASpawn spawn = mob.getSpawn();
		spawn.setRespawnDelay(0);
		spawn.getNpc().deleteMe();
		SpawnManager.getInstance().deleteSpawn((Spawn) spawn);
		
	}
	
	private void teleToTown()
	{
		
		for (Player player : players)
		{
			
			if (player.isDead())
				player.doRevive();
			
			player.teleportTo(81664, 149056, -3472, 15);
			
			player.broadcastCharInfo();
			player.broadcastUserInfo();
			player.setDungeon(null);
			player.setInstanceMap(InstanceManager.getInstance().getInstance(0), true);
			
		}
		
		cleanupDungeon();
	}
	
	private void cleanupDungeon()
	{
		InstanceManager.getInstance().deleteInstance(instance.getId());
		DungeonManager.getInstance().removeDungeon(this);
		
		cancelScheduledTasks();
	}
	
	private void completeDungeon()
	{
		ThreadPool.schedule(this::teleToTown, 5 * 1000);
		broadcastScreenMessage("You have completed the dungeon!", 5);
		
	}
	
	private void cancelScheduledTasks()
	{
		if (dungeonCancelTask != null)
			dungeonCancelTask.cancel(true);
		
		if (monitorTask != null)
			monitorTask.cancel(true);
		
		if (timerTask != null)
			timerTask.cancel(true);
		
		if (nextTask != null)
			nextTask.cancel(true);
		
	}
	
	private void broadcastTimer()
	{
		int secondsLeft = (int) ((stageBeginTime + (1000 * 60 * currentStage.timeLimit)) - System.currentTimeMillis()) / 1000;
		int minutes = secondsLeft / 60;
		int seconds = secondsLeft % 60;
		
		ExShowScreenMessage packet = new ExShowScreenMessage(String.format("%02d:%02d", minutes, seconds), 1010, SMPOS.BOTTOM_RIGHT, false);
		for (Player player : players)
		{
			player.sendPacket(packet);
		}
	}
	
	private void broadcastScreenMessage(String msg, int seconds)
	{
		ExShowScreenMessage packet = new ExShowScreenMessage(msg, seconds * 1000, SMPOS.TOP_CENTER, false);
		for (Player player : players)
		{
			player.sendPacket(packet);
		}
	}
	
	private boolean getNextStage()
	{
		if (template.stages.isEmpty())
			return false;
		
		if (currentStageIndex >= template.stages.size())
		{
			currentStage = null;
			return false;
		}
		
		currentStage = template.stages.get(currentStageIndex);
		
		List<SpawnTemplate> stageSpawns = template.spawns.get(currentStage.order);
		if (stageSpawns == null || stageSpawns.isEmpty())
			return false;
		
		currentSpawns = stageSpawns;
		
		stageBeginTime = System.currentTimeMillis();
		currentStageIndex++;
		return true;
	}
	
	public synchronized void onMobKill(Attackable attackable)
	{
		
		SpawnTemplate spawnTemplate = mobToTemplate.remove(attackable);
		if (spawnTemplate == null)
			return;
		
		List<DropData> drops = parseDrops(spawnTemplate.drops);
		
		for (Player player : players)
		{
			if (drops != null && !drops.isEmpty())
			{
				for (DropData drop : drops)
				{
					if (Rnd.get(1000000) < drop.chance)
					{
						int totalAmount = Rnd.get(drop.min, drop.max);
						
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
									member.addItem(drop.itemId, amount, true);
							}
						}
						else
						{
							player.addItem(drop.itemId, totalAmount, true);
						}
					}
				}
			}
		}
		
		deleteMob(attackable);
		
		if (mobToTemplate.isEmpty())
		{
			cancelScheduledTasks();
			
			if (advanceToNextStage())
			{
				broadcastScreenMessage("You have completed stage " + (currentStage.order - 1) + "! Next stage begins in 10 seconds.", 5);
				nextTask = ThreadPool.schedule(this::beginStage, 10 * 1000);
			}
			else
			{
				completeDungeon();
			}
		}
	}
	
	private List<DropData> parseDrops(String dropString)
	{
		List<DropData> drops = new ArrayList<>();
		if (dropString == null || dropString.isEmpty())
			return drops;
		
		String[] entries = dropString.split(";");
		for (String entry : entries)
		{
			String[] parts = entry.split("-");
			if (parts.length < 3)
				continue;
			
			int itemId = Integer.parseInt(parts[0]);
			int min = Integer.parseInt(parts[1]);
			int max = Integer.parseInt(parts[2]);
			int chance = parts.length > 3 ? Integer.parseInt(parts[3]) : 1000000;
			
			drops.add(new DropData(itemId, min, max, chance));
		}
		return drops;
	}
	
	public class DropData
	{
		public final int itemId;
		public final int min;
		public final int max;
		public final int chance;
		
		public DropData(int itemId, int min, int max, int chance)
		{
			this.itemId = itemId;
			this.min = min;
			this.max = max;
			this.chance = chance;
		}
	}
	
	public void updatePlayerStage(int stage)
	{
		for (Player player : players)
		{
			DungeonManager.getInstance().updateStage(template.id, player.getObjectId(), stage);
		}
	}
	
	public List<Player> getPlayers()
	{
		return players;
	}
	
}
