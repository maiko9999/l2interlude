package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.DropType;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * A container used by monster drops.
 */
public class DropData
{
	public static final int MAX_CHANCE = 1000000;

	private final int _itemId;
	private final int _minDrop;
	private final int _maxDrop;
	private final double _chance;

	// 🔧 Adicionado: Tipo da categoria (Drop, Spoil, Herb etc.)
	private DropType _categoryType;

	public DropData(int itemId, int minDrop, int maxDrop, double chance)
	{
		_itemId = itemId;
		_minDrop = minDrop;
		_maxDrop = maxDrop;
		_chance = chance;
	}

	@Override
	public String toString()
	{
		return "DropData =[ItemID: " + _itemId + " Min: " + _minDrop + " Max: " + _maxDrop + " Chance: " + _chance + "%]";
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getMinDrop()
	{
		return _minDrop;
	}

	public int getMaxDrop()
	{
		return _maxDrop;
	}

	public double getChance()
	{
		return _chance;
	}


	public void setCategoryType(DropType type)
	{
		_categoryType = type;
	}

	
	public DropType getCategoryType()
	{
		return _categoryType;
	}

	public IntIntHolder calculateDrop(double ratio)
	{
		int count;
		if (ratio <= 1)
		{
			count = Rnd.get(_minDrop, _maxDrop);
		}
		else
		{
			ratio *= 100;
			int multiplier = (int) (ratio / 100);
			int bonus = (int) (ratio % 100);

			count = Rnd.get(_minDrop * multiplier, _maxDrop * multiplier);
			if (Rnd.get(100) < bonus)
				count += Rnd.get(_minDrop, _maxDrop);
		}

		return new IntIntHolder(_itemId, count);
	}
}
