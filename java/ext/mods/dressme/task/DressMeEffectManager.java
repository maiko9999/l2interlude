package ext.mods.dressme.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.L2Skill;

import ext.mods.dressme.holder.DressMeHolder;

public class DressMeEffectManager
{
	
	private final Map<Integer, ActiveEffect> _activeEffects = new ConcurrentHashMap<>();
	
	private static class ActiveEffect
	{
		private final int _skillId;
		private final ScheduledFuture<?> _task;
		
		public ActiveEffect(int skillId, ScheduledFuture<?> task)
		{
			_skillId = skillId;
			_task = task;
		}
	}
	
	private DressMeEffectManager()
	{
	}
	
	public void startEffect(Player player, DressMeHolder skin)
	{
		if (player == null || skin.getEffect() == null)
			return;
		
		final int playerId = player.getObjectId();
		final int skillId = skin.getEffect().getSkillId();
		final int skillLevel = skin.getEffect().getLevel();
		final int interval = skin.getEffect().getInterval(); // em segundos
		
		stopEffect(player); // Cancela qualquer anterior
		
		applySkill(player, skillId, skillLevel); // Aplica imediatamente
		
		ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() ->
		{
			
			Player checkOnplayer = World.getInstance().getPlayer(player.getObjectId());
			
			if (checkOnplayer != null)
			{
				applySkill(player, skillId, skillLevel);
			}
			else
			{
				stopEffect(player);
			}
			
		}, interval * 1000L, interval * 1000L);
		
		_activeEffects.put(playerId, new ActiveEffect(skillId, task));
	}
	
	public void stopEffect(Player player)
	{
		if (player == null)
			return;
		
		ActiveEffect effect = _activeEffects.remove(player.getObjectId());
		if (effect != null)
		{
			effect._task.cancel(false);
			player.stopSkillEffects(effect._skillId); // Remove o efeito visual
		}
	}
	
	private static void applySkill(Player player, int skillId, int skillLevel)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if (skill != null)
		{
			player.broadcastPacketInRadius(new MagicSkillUse(player, player, skill.getId(), 1, 0, 0, false), skill.getSkillRadius());
			
		}
	}
	
	public static DressMeEffectManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final DressMeEffectManager INSTANCE = new DressMeEffectManager();
	}
}