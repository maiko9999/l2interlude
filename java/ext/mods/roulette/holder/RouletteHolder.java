package ext.mods.roulette.holder;

public class RouletteHolder
{
	private final int _id;
	private final int _count;
	private final int _enchant;
	private final int _chance;
	
	public RouletteHolder(int id, int count, int enchant, int chance)
	{
		_id = id;
		_count = count;
		_enchant = enchant;
		_chance = chance;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public int getChance()
	{
		return _chance;
	}
}
