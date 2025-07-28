package net.sf.l2j.gameserver.model.records;

public record SellBuffData(int id, int time, boolean applyOnPets)
{
	public int getSkillId()
	{
		return id;
	}
}