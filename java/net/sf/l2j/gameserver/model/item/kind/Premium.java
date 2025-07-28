package net.sf.l2j.gameserver.model.item.kind;

public class Premium
{
	private int _charId;
	private int _itemId;
	private long _activationTime;
	
	public static final int MODIFIER_XP = 0;
	public static final int MODIFIER_SP = 1;
	public static final int MODIFIER_PARTY_XP = 2;
	public static final int MODIFIER_PARTY_SP = 3;
	public static final int MODIFIER_DROP_ADENA = 4;
	public static final int MODIFIER_DROP_ITEMS = 5;
	public static final int MODIFIER_SPOIL = 6;
	public static final int MODIFIER_QUEST = 7;
	public static final int MODIFIER_QUEST_ADENA = 8;
	public static final int MODIFIER_DROP_SEAL_STONES = 9;
	
	public int getCharId()
	{
		return _charId;
	}
	
	public void setCharId(int charId)
	{
		_charId = charId;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	public long getActivationTime()
	{
		return _activationTime;
	}
	
	public void setActivationTime(long activationTime)
	{
		_activationTime = activationTime;
	}
}