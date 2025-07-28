package net.sf.l2j.gameserver.taskmanager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;

/**
 * Handle all {@link Npc} AI tasks.
 */
public final class AiTaskManager implements Runnable
{
	private final Set<Npc> _npcs = ConcurrentHashMap.newKeySet();
	
	private static final int RETURN_HOME_RAIDBOSS_RADIUS = Config.RETURN_HOME_RAIDBOSS_RADIUS;
	private static final int RETURN_HOME_MONSTER_RADIUS = Config.RETURN_HOME_MONSTER_RADIUS;
	
	private static final Set<Integer> EXCLUDED_RAIDBOSS_IDS = Set.of(29095);
	private static final Set<Integer> EXCLUDED_MONSTER_IDS = Set.of(29016, 29008, 29004);
	
	protected AiTaskManager()
	{
		// Run task each second.
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
		
		// Run task every 10 seconds.
		ThreadPool.scheduleAtFixedRate(this::animationTask, 10000, 10000);
	}
	
	@Override
	public final void run()
	{
		_npcs.forEach(npc -> processNpc(npc));
	}
	
	private void processNpc(Npc npc)
	{
		npc.getAI().runAI();
		
		if (npc instanceof GrandBoss)
			return;
		else if (npc instanceof RaidBoss raidBoss)
			monsterReturn(raidBoss, Config.RETURN_HOME_RAIDBOSS, RETURN_HOME_RAIDBOSS_RADIUS, EXCLUDED_RAIDBOSS_IDS);
		else if (npc instanceof Monster monster)
			monsterReturn(monster, Config.RETURN_HOME_MONSTER, RETURN_HOME_MONSTER_RADIUS, EXCLUDED_MONSTER_IDS);
	}
	
	private void monsterReturn(Monster monster, boolean returnHome, int radius, Set<Integer> excludedNpcIds)
	{
		if (!returnHome || isNpcIdExcluded(monster.getNpcId(), excludedNpcIds))
			return;
		
		if (!monster.isIn3DRadius(monster.getSpawnLocation(), radius))
		{
			System.out.println("Returning monster: " + monster.getNpcId());
			monster.teleportTo(monster.getSpawnLocation(), 0);
			monster.removeAllAttackDesire();
			monster.getStatus().setHpMp(monster.getStatus().getMaxHp(), monster.getStatus().getMaxMp());
			teleportMinions(monster);
		}
	}
	
	private boolean isNpcIdExcluded(int npcId, Set<Integer> excludedNpcIds)
	{
	    return excludedNpcIds.contains(npcId);
	}
	
	private void teleportMinions(Monster monster)
	{
		monster.getMinions().forEach(minion ->
		{
			if (!minion.isDead())
			{
				minion.teleportToMaster();
				minion.removeAllAttackDesire();
				minion.getStatus().setHpMp(minion.getStatus().getMaxHp(), minion.getStatus().getMaxMp());
			}
		});
	}
	
	protected final void animationTask()
	{
		_npcs.stream().filter(npc -> npc instanceof Folk).forEach(folk ->
		{
			int moveAroundSocial = folk.getTemplate().getAiParams().getInteger("MoveAroundSocial", 0);
			int moveAroundSocial1 = folk.getTemplate().getAiParams().getInteger("MoveAroundSocial1", 0);
			
			if (moveAroundSocial > 0 || moveAroundSocial1 > 0)
			{
				if (folk.getStatus().getHpRatio() > 0.4 && !folk.isDead() && folk.getAI().getCurrentIntention().getType() != IntentionType.ATTACK)
				{
					if (Rnd.get(100) < Config.NPC_ANIMATION)
					{
						if (moveAroundSocial > 0)
							folk.getAI().addSocialDesire(3, (moveAroundSocial * 1000) / 30, 50);
						else if (moveAroundSocial1 > 0)
							folk.getAI().addSocialDesire(3, (moveAroundSocial1 * 1000) / 30, 50);
					}
				}
			}
		});
	}
	
	/**
	 * Add the {@link Npc} set as parameter to the {@link AiTaskManager}.
	 * @param npc : The {@link Npc} to add.
	 */
	public final void add(Npc npc)
	{
		npc.setAISleeping(false);
		
		_npcs.add(npc);
	}
	
	/**
	 * Remove the {@link Npc} set as parameter from the {@link AiTaskManager}.
	 * @param npc : The {@link Npc} to remove.
	 */
	public final void remove(Npc npc)
	{
		npc.setAISleeping(true);
		
		_npcs.remove(npc);
	}
	
	public static final AiTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		protected static final AiTaskManager INSTANCE = new AiTaskManager();
	}
}