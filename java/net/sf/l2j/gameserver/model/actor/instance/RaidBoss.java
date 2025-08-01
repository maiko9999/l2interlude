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
import net.sf.l2j.gameserver.model.group.CommandChannel;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This class manages all classic raid bosses.<br>
 * <br>
 * Raid Bosses (RB) are mobs which are supposed to be defeated by a party of several players. It extends most of {@link Monster} aspects.<br>
 * <br>
 * They automatically teleport if out of their initial spawn area, and can randomly attack a Player from their Hate List once attacked.<br>
 * <br>
 * Their looting rights are affected by {@link CommandChannel}s. The first who attacks got the priority over loots. Those rights are lost if no attack has been done for 900sec.
 */
public class RaidBoss extends Monster
{
	public RaidBoss(int objectId, NpcTemplate template)
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
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
				broadcastPacket(new PlaySound("systemmsg_e.1209"));
				
				if (Config.ANNOUNCE_DIE_RAIDBOSS)
				{
					if (player.getClan() != null)
						World.announceToOnlinePlayers(player.getSysString(10_233, getName(), getStatus().getLevel(), player.getName(), player.getClan().getName()), true);
					else
						World.announceToOnlinePlayers(player.getSysString(10_234, getName(), getStatus().getLevel(), player.getName()), true);
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
		}
		
		// TODO implement SpawnManager or ASpawn notification
		// RaidBossManager.getInstance().onDeath(this);
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
	
}