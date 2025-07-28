package ext.mods.FarmEventRandom.holder;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

public class RandomSpawnHolder
{
	private final int _npcId;
	private final IntIntHolder _countAndRange;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _respawnDelay;
	private final List<DropHolder> _drops;
	
	public RandomSpawnHolder(StatSet set)
	{
		_npcId = set.getInteger("npcId");
		_countAndRange = set.getIntIntHolder("countAndRange");
		_x = set.getInteger("x");
		_y = set.getInteger("y");
		_z = set.getInteger("z");
		_respawnDelay = set.getInteger("respawnDelay", 60);
		
		_drops = new ArrayList<>();
		String dropsStr = set.getString("drops", "");
		if (!dropsStr.isEmpty())
		{
			for (String part : dropsStr.split(";"))
			{
				String[] vals = part.split("-");
				if (vals.length >= 3)
				{
					int itemId = Integer.parseInt(vals[0]);
					int count = Integer.parseInt(vals[1]);
					int chance = Integer.parseInt(vals[2]);
					_drops.add(new DropHolder(itemId, count, chance));
				}
			}
		}
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public int getCount()
	{
		return _countAndRange.getId();
	}
	
	public int getRange()
	{
		return _countAndRange.getValue();
	}
	
	public int getX()
	{
		return _x;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	public List<DropHolder> getDrops()
	{
		return _drops;
	}
}
