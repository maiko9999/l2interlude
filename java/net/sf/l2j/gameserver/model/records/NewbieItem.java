package net.sf.l2j.gameserver.model.records;

import net.sf.l2j.commons.data.StatSet;

public record NewbieItem(int id, int count, boolean isEquipped, int enchant)
{
	public NewbieItem(StatSet set)
	{
		this(set.getInteger("id"), set.getInteger("count"), set.getBool("isEquipped", true), set.getInteger("enchant", 0));
	}
}