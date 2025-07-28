package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.SellBuffsManager;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SellBuffHolder
{
	private final int _skillId;
	private final int _skillLvl;
	private int _price;
	
	public SellBuffHolder(int skillId, int skillLvl, int price)
	{
		_skillId = skillId;
		_skillLvl = skillLvl;
		_price = price;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getSkillLvl()
	{
		return _skillLvl;
	}
	
	public void setPrice(int price)
	{
		_price = price;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_skillId, _skillLvl);
	}
	
	public L2Skill getSkillFrom(Player seller)
	{
		return switch (_skillId)
		{
			case 4699 -> normalize(SellBuffsManager.getInstance().getBuffSkill(seller, 1331, NpcSkillType.BUFF1));
			case 4700 -> normalize(SellBuffsManager.getInstance().getBuffSkill(seller, 1331, NpcSkillType.BUFF2));
			case 4702 -> normalize(SellBuffsManager.getInstance().getBuffSkill(seller, 1332, NpcSkillType.BUFF1));
			case 4703 -> normalize(SellBuffsManager.getInstance().getBuffSkill(seller, 1332, NpcSkillType.BUFF2));
			default -> normalize(seller.getSkill(_skillId));
		};
	}
	
	private L2Skill normalize(L2Skill actual)
	{
		if (actual == null || actual.getLevel() < _skillLvl)
			return null;
		else if (actual.getLevel() == _skillLvl)
			return actual;
		else
			return getSkill();
	}
	
	public int getSkillUse()
	{
		return switch (_skillId)
		{
			case 4699 -> 4700;
			default -> _skillId;
		};
	}
}