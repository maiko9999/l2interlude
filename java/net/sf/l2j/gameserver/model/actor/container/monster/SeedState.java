package net.sf.l2j.gameserver.model.actor.container.monster;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.manor.Seed;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * A container holding all related informations of a {@link Monster} seed state.<br>
 * <br>
 * A seed occurs when a {@link Player} uses a seed over a Monster.
 */
public class SeedState
{
	private final Monster _owner;
	
	private int _seederId;
	private Seed _seed;
	private boolean _isHarvested;
	
	public SeedState(Monster owner)
	{
		_owner = owner;
	}
	
	public boolean isSeeded()
	{
		return _seederId != 0;
	}
	
	public Seed getSeed()
	{
		return _seed;
	}
	
	/**
	 * Set the seed parameters.
	 * @param player : The {@link Player} seeding the monster.
	 * @param seed : The {@link Seed} used to seed the monster.
	 */
	public void setSeeded(Player player, Seed seed)
	{
		_seederId = player.getObjectId();
		_seed = seed;
	}
	
	public boolean isHarvested()
	{
		return _isHarvested;
	}
	
	public void setHarvested()
	{
		_isHarvested = true;
	}
	
	/**
	 * @param player : The {@link Player} to test.
	 * @return True if the {@link Player} set as parameter can harvest, or false otherwise.
	 */
	public boolean isAllowedToHarvest(Player player)
	{
		if (player == null)
			return false;
		
		// Check player being seeder himself.
		if (player.getObjectId() == _seederId)
			return true;
		
		// Check party.
		final Player seeder = World.getInstance().getPlayer(_seederId);
		if (seeder == null || seeder.getParty() == null)
			return false;
		
		// Check player being member of seeder party.
		return seeder.getParty().containsPlayer(player);
	}
	
	/**
	 * @return The {@link IntIntHolder} containing seed crop info.
	 */
	public IntIntHolder getHarvestedCrop()
	{
		// Get base count and apply strong-type multiplier.
		int count = 1;
		for (L2Skill skill : _owner.getTemplate().getPassives())
		{
			if (skill.getId() == 4408)
			{
				switch (skill.getLevel())
				{
					case 10: // Strong type x2
						count *= 2;
						break;
					case 11: // Strong type x3
						count *= 3;
						break;
					case 12: // Strong type x4
						count *= 4;
						break;
					case 13: // Strong type x5
						count *= 5;
						break;
					case 14: // Strong type x6
						count *= 6;
						break;
					case 15: // Strong type x7
						count *= 7;
						break;
					case 16: // Strong type x8
						count *= 8;
						break;
					case 17: // Strong type x9
						count *= 9;
						break;
				}
			}
		}
		
		// Calculate monster and seed level modifier.
		final int diff = _owner.getStatus().getLevel() - _seed.getLevel() - 5;
		if (diff > 0)
			count += diff;
		
		// Calculate reward.
		return new IntIntHolder(_seed.getCropId(), count * Config.RATE_DROP_MANOR);
	}
	
	/**
	 * Clear all seed related variables.
	 */
	public void clear()
	{
		_seederId = 0;
		_seed = null;
		_isHarvested = false;
	}
}