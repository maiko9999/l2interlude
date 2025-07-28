package ext.mods.tour.holder;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

public class TourHolder
{
	private final int count;
	private final int x;
	private final int y;
	private final int z;
	private final int _duration;
	private final List<IntIntHolder> _rewards;
	
	public TourHolder(StatSet set)
	{
		count = set.getInteger("count", 1);
		x = set.getInteger("x");
		y = set.getInteger("y");
		z = set.getInteger("z");
		_duration = set.getInteger("duration", 10);
		_rewards = parseRewards(set.getString("reward", "57-1;"));
		
	}
	
	private List<IntIntHolder> parseRewards(String rewardString)
	{
		List<IntIntHolder> list = new ArrayList<>();
		
		if (rewardString.isEmpty())
			return list;
		
		String[] rewardEntries = rewardString.split(";");
		for (String entry : rewardEntries)
		{
			String[] parts = entry.split("-");
			if (parts.length == 2)
			{
				int itemId = Integer.parseInt(parts[0]);
				int amount = Integer.parseInt(parts[1]);
				list.add(new IntIntHolder(itemId, amount));
			}
		}
		return list;
	}
	
	public int getCount()
	{
		return count;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public int getDuration()
	{
		return _duration;
	}
	
	public List<IntIntHolder> getRewards()
	{
		return _rewards;
	}
}
