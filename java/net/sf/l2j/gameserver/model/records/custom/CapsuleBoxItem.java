package net.sf.l2j.gameserver.model.records.custom;

import java.util.ArrayList;
import java.util.List;

public record CapsuleBoxItem(int id, int playerLevel, List<Item> items)
{
	public CapsuleBoxItem(int id, int playerLevel)
	{
		this(id, playerLevel, new ArrayList<>());
	}
	
	public void addItem(Item item)
	{
		items.add(item);
	}
}