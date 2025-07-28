package net.sf.l2j.gameserver.model.records.custom;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;

public record EnchantScroll(int scrollId, CrystalType grade, boolean isWeapon, boolean cristalize, int returnVal, int[] chance, int[] chanceF, int[] chanceM, boolean message, int[] enchants, int enchantAmount, int returnOnFail)
{
	public EnchantScroll(StatSet set)
	{
		this(set.getInteger("id"), set.getEnum("grade", CrystalType.class, CrystalType.NONE), set.getBool("isWeapon"), set.getBool("crystalize", true), set.getInteger("return", 0), set.getIntegerArray("rate", ArraysUtil.EMPTY_INT_ARRAY), set.getIntegerArray("rateF", ArraysUtil.EMPTY_INT_ARRAY), set.getIntegerArray("rateM", ArraysUtil.EMPTY_INT_ARRAY), set.getBool("message", false), set.getIntegerArray("enchants", ArraysUtil.EMPTY_INT_ARRAY), set.getInteger("enchantAmount", 1), set.getInteger("returnOnFail", -1));
	}
	
	public int getChance(ItemInstance item)
	{
		int level = item.getEnchantLevel();
		
		if (item.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR && level <= 4)
			return 100;
		
		if (chance == ArraysUtil.EMPTY_INT_ARRAY && item.getItem().getType2() == Item.TYPE2_WEAPON && isWeapon && item.isWeapon())
			return ((Weapon) item.getItem()).isMagical() ? level >= chanceM.length ? 0 : chanceM[level] : level >= chanceF.length ? 0 : chanceF[level];
		
		return level >= chance.length ? 0 : chance[level];
	}
	
	public boolean announceTheEnchant(ItemInstance item)
	{
		return item != null && message && ArraysUtil.contains(enchants, item.getEnchantLevel());
	}
	
	public boolean isValid(ItemInstance item)
	{
		if (grade != item.getItem().getCrystalType())
			return false;
		
		if (getChance(item) == 0)
			return false;
		
		switch (item.getItem().getType2())
		{
			case Item.TYPE2_WEAPON:
				return isWeapon;
			case Item.TYPE2_SHIELD_ARMOR:
			case Item.TYPE2_ACCESSORY:
				return !isWeapon;
			default:
				return false;
		}
	}
}