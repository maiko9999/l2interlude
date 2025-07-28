package net.sf.l2j.gameserver.model.records.custom;

import net.sf.l2j.commons.data.StatSet;

public record Macros(int panel, int slot, String name, String action, String acronim, String command)
{
	public Macros(StatSet set)
	{
		this(set.getInteger("panel"), set.getInteger("slot"), set.getString("name"), set.getString("action"), set.getString("acronim"), set.getString("command"));
	}
}