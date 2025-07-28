package ext.mods.tour.holder;

import net.sf.l2j.commons.data.StatSet;

public class TourPrizeHolder
{
	private final int _position;
	private final String _reward;
	
	public TourPrizeHolder(StatSet set)
	{
		_position = set.getInteger("position");
		_reward = set.getString("reward");
	}
	
	public int getPosition()
	{
		return _position;
	}
	
	public String getReward()
	{
		return _reward;
	}
}
