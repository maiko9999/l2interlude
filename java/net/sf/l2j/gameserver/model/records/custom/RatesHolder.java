// RatesHolder.java
package net.sf.l2j.gameserver.model.records.custom;

import net.sf.l2j.commons.data.StatSet;

public class RatesHolder
{
	private final int level;
	private final float xpRate;
	private final float spRate;
	private final float adenaRate;
	private final float dropRate;
	private final double spoilRate;
	public RatesHolder(StatSet set)
	{
		level = set.getInteger("level");
		xpRate = set.getFloat("xpRate", 1f);
		spRate = set.getFloat("spRate", 1f);
		adenaRate = set.getFloat("adenaRate", 1f);
		dropRate = set.getFloat("dropRate", 1f);
		spoilRate = set.getDouble("spoilRate", 1f);
	}

	public int getLevel() { return level; }
	public float getXpRate() { return xpRate; }
	public float getSpRate() { return spRate; }
	public float getAdenaRate() { return adenaRate; }
	public float getDropRate() { return dropRate; }
	public double getSpoilRate() { return spoilRate; }
}