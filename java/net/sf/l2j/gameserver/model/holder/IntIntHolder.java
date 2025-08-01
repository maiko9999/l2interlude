package net.sf.l2j.gameserver.model.holder;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * A generic int/int container.
 */
public class IntIntHolder
{
	private int _id;
	private int _value;
	
	public IntIntHolder(int id, int value)
	{
		_id = id;
		_value = value;
	}
	
	@Override
	public String toString()
	{
		return "IntIntHolder [id=" + _id + " value=" + _value + "]";
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getValue()
	{
		return _value;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public void setValue(int value)
	{
		_value = value;
	}
	
	/**
	 * @return The {@link L2Skill} associated to the id/value stored on this {@link IntIntHolder}.
	 */
	public final L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_id, _value);
	}
	
	/**
	 * If this {@link IntIntHolder} is used as itemId/quantity, calculates the expected total weight size of this {@link Item}.
	 * @return The weight of the potential {@link Item} weight multiplied by count, or 0 if the {@link Item} doesn't exist.
	 */
	public final int getWeight()
	{
		final Item item = ItemData.getInstance().getTemplate(_id);
		if (item == null)
			return 0;
		
		return item.getWeight() * _value;
	}
	
	/**
	 * @param id : The int to test as id.
	 * @param value : The int to test as value.
	 * @return True if both values equal, false otherwise.
	 */
	public final boolean equals(int id, int value)
	{
		return _id == id && _value == value;
	}
}