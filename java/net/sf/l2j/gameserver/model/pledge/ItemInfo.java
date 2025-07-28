package net.sf.l2j.gameserver.model.pledge;

public class ItemInfo
{
	public final int _count;
	public final int _enchant;
	
	public ItemInfo(int count, int enchant)
	{
		_count = count;
		_enchant = enchant;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
}