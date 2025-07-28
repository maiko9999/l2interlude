package ext.mods.FarmEventRandom.holder;

public class DropHolder
{
	private final int _itemId;
	private final int _count;
	private final int _chance;

	public DropHolder(int itemId, int count, int chance)
	{
		_itemId = itemId;
		_count = count;
		_chance = chance;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getCount()
	{
		return _count;
	}

	public int getChance()
	{
		return _chance;
	}
}
