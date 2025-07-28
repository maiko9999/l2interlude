package net.sf.l2j.gameserver.model.balance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.gameserver.custom.data.BalanceData;
import net.sf.l2j.gameserver.model.holder.BalanceHolder;
import net.sf.l2j.gameserver.model.holder.BalanceName;


public class BalanceGeneration
{
	public static void generate(Connection con, Map<String, BalanceHolder> modifiers) throws SQLException
	{
		Set<Integer> classIds = BalanceName.getClassMap().keySet();
		
		for (int classAtk : classIds)
		{
			for (int classTgt : classIds)
			{
				String key = BalanceData.buildKey(classAtk, classTgt);
				
				if (!modifiers.containsKey(key))
				{
					BalanceHolder mod = calculateModifier(classAtk, classTgt);
					
					try (PreparedStatement ps = con.prepareStatement("INSERT INTO balance_classes (class_id_attacker, class_id_target, p_atk_mod, m_atk_mod, p_def_mod, m_def_mod) VALUES (?, ?, ?, ?, ?, ?)"))
					{
						ps.setInt(1, classAtk);
						ps.setInt(2, classTgt);
						ps.setDouble(3, mod._pAtkMod);
						ps.setDouble(4, mod._mAtkMod);
						ps.setDouble(5, mod._pDefMod);
						ps.setDouble(6, mod._mDefMod);
						ps.executeUpdate();
					}
					
					modifiers.put(key, mod);
				}
			}
		}
	}
	
	private static BalanceHolder calculateModifier(int attacker, int target)
	{
		String atkName = BalanceName.getName(attacker).toLowerCase();
		String tgtName = BalanceName.getName(target).toLowerCase();
		
		double pAtkMod = 1.0, mAtkMod = 1.0, pDefMod = 1.0, mDefMod = 1.0;
		
		boolean atkIsWeakClass = atkName.contains("dominator") || atkName.contains("doomcryer") || atkName.contains("hierophant") || atkName.contains("shillien saint") || atkName.contains("eva saint") || atkName.contains("phoenix knight") || atkName.contains("temple knight") || atkName.contains("warcryer") || atkName.contains("overlord");
		
		boolean atkIsTopClass = atkName.contains("ghost hunter") || atkName.contains("adventurer") || atkName.contains("wind rider") || atkName.contains("sagittarius") || atkName.contains("ghost sentinel") || atkName.contains("moonlight") || atkName.contains("archmage") || atkName.contains("soultaker") || atkName.contains("storm screamer");
		
		boolean tgtIsTopClass = tgtName.contains("ghost hunter") || tgtName.contains("adventurer") || tgtName.contains("wind rider") || tgtName.contains("sagittarius") || tgtName.contains("ghost sentinel") || tgtName.contains("moonlight") || tgtName.contains("archmage") || tgtName.contains("soultaker") || tgtName.contains("storm screamer");
		
		if (atkIsWeakClass)
		{
			pAtkMod += 0.3;
			mAtkMod += 0.3;
			pDefMod += 0.1;
			mDefMod += 0.1;
		}
		
		if (atkIsTopClass)
		{
			pAtkMod -= 0.2;
			mAtkMod -= 0.2;
		}
		if (tgtIsTopClass)
		{
			pDefMod -= 0.2;
			mDefMod -= 0.2;
		}
		
		pAtkMod = Math.max(0.5, Math.min(pAtkMod, 1.5));
		mAtkMod = Math.max(0.5, Math.min(mAtkMod, 1.5));
		pDefMod = Math.max(0.5, Math.min(pDefMod, 1.5));
		mDefMod = Math.max(0.5, Math.min(mDefMod, 1.5));
		
		return new BalanceHolder(pAtkMod, mAtkMod, pDefMod, mDefMod);
	}
	
}
