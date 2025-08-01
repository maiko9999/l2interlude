package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.custom.data.BossHpAnnounceData;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.manager.RaidPointManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This class manages all {@link GrandBoss}es.<br>
 * <br>
 * Those npcs inherit from {@link Monster}. Since a script is generally associated to it, {@link GrandBoss#returnHome} returns false to avoid misbehavior. No random walking is allowed.
 */
public final class GrandBoss extends Monster
{
	public GrandBoss(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setRaidRelated();
	}
	
	@Override
	public int getSeeRange()
	{
		return getTemplate().getAggroRange();
	}
	
	@Override
	public boolean isRaidBoss()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		setNoRndWalk(true);
		super.onSpawn();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		final Player player = killer.getActingPlayer();
		if (player != null)
		{
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
			broadcastPacket(new PlaySound("systemmsg_e.1209"));
			
			if (Config.ANNOUNCE_DIE_GRANDBOSS)
			{
				if (player.getClan() != null)
					World.announceToOnlinePlayers(player.getSysString(10_235, getName(), getStatus().getLevel(), player.getName(), player.getClan().getName()));
				else
					World.announceToOnlinePlayers(player.getSysString(10_236, getName(), getStatus().getLevel(), player.getName()));
			}
			
			final Party party = player.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					RaidPointManager.getInstance().addPoints(member, getNpcId(), (getStatus().getLevel() / 2) + Rnd.get(-5, 5));
					if (member.isNoble())
						HeroManager.getInstance().setRBkilled(member.getObjectId(), getNpcId());
				}
			}
			else
			{
				RaidPointManager.getInstance().addPoints(player, getNpcId(), (getStatus().getLevel() / 2) + Rnd.get(-5, 5));
				if (player.isNoble())
					HeroManager.getInstance().setRBkilled(player.getObjectId(), getNpcId());
			}
		}
		
		return true;
	}
	
	private int _lastAnnouncedHpPercent = 100;
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
		
		if (!BossHpAnnounceData.getInstance().isAnnounceEnabledFor(getNpcId()) || getStatus().getMaxHp() <= 0)
			return;
		
		int currentPercent = (int) ((getStatus().getHp() / getStatus().getMaxHp()) * 100);
		
		if (currentPercent > _lastAnnouncedHpPercent)
			_lastAnnouncedHpPercent = 100;
		
		for (BossHpAnnounceData.HpThreshold threshold : BossHpAnnounceData.getInstance().getThresholds(getNpcId()))
		{
			if (currentPercent <= threshold.percent && _lastAnnouncedHpPercent > threshold.percent)
			{
				_lastAnnouncedHpPercent = threshold.percent;
				String msg = threshold.message.replace("%boss%", getName()).replace("%hp%", String.valueOf(threshold.percent));
				World.announceToOnlinePlayers(msg, true);
				break;
			}
		}
	}
	
	@Override
	public boolean returnHome()
	{
		return false;
	}
}