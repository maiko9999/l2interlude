package net.sf.l2j.gameserver.custom.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.model.balance.BalanceGeneration;
import net.sf.l2j.gameserver.model.holder.BalanceHolder;
import net.sf.l2j.gameserver.model.holder.BalanceName;

public class BalanceData
{
	private final Map<String, BalanceHolder> _modifiers = new HashMap<>();
	public final Map<String, Double> _vulnModifiers = new HashMap<>();
	
	public void init()
	{
		System.out.println("Loaded Balance.");
		load();
	}
	
	public void load()
	{
		_modifiers.clear();
		
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("SELECT * FROM balance_classes");
				ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					int classAtk = rs.getInt("class_id_attacker");
					int classTgt = rs.getInt("class_id_target");
					
					double pAtk = rs.getDouble("p_atk_mod");
					double mAtk = rs.getDouble("m_atk_mod");
					double pDef = rs.getDouble("p_def_mod");
					double mDef = rs.getDouble("m_def_mod");
					
					String key = buildKey(classAtk, classTgt);
					_modifiers.put(key, new BalanceHolder(pAtk, mAtk, pDef, mDef));
				}
				
			}
			
		}
		catch (Exception e)
		{
			System.out.println("Error loading balance modifiers: " + e);
		}
		
		try (Connection con1 = ConnectionPool.getConnection())
		{
			BalanceGeneration.generate(con1, _modifiers);
		}
		catch (Exception e)
		{
			System.out.println("Error loading insert balance modifiers or vulnerabilities: " + e);
		}
		
		loadVulnerabilities();
		
	}
	
	private void loadVulnerabilities()
	{
		_vulnModifiers.clear();
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT skill_type, multiplier FROM balance_vulnerability");
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				String type = rs.getString("skill_type").toUpperCase();
				double multiplier = rs.getDouble("multiplier");
				_vulnModifiers.put(type, multiplier);
			}
		}
		catch (Exception e)
		{
			System.out.println("Error loading balance_vulnerability table:" + e);
		}
	}
	
	public static String buildKey(int atk, int tgt)
	{
		return atk + ":" + tgt;
	}
	
	public double getSkillTypeMultiplier(String skillType)
	{
		return _vulnModifiers.getOrDefault(skillType.toUpperCase(), 1.0);
	}
	
	public BalanceHolder getModifier(int classAtk, int classTgt)
	{
		String key = buildKey(classAtk, classTgt);
		return _modifiers.getOrDefault(key, new BalanceHolder(1.0, 1.0, 1.0, 1.0)); // P.Atk, P.Def, M.Atk, M.Def
	}
	
	public Map<String, BalanceHolder> getModifierMap()
	{
		return _modifiers;
	}
	
	public void updateModifier(int classAtk, int classTgt, BalanceHolder mod)
	{
		String sql = "INSERT INTO balance_classes (class_id_attacker, class_id_target, p_atk_mod, m_atk_mod, p_def_mod, m_def_mod) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE p_atk_mod = VALUES(p_atk_mod), m_atk_mod = VALUES(m_atk_mod), p_def_mod = VALUES(p_def_mod), m_def_mod = VALUES(m_def_mod) ";
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, classAtk);
			ps.setInt(2, classTgt);
			ps.setDouble(3, mod._pAtkMod);
			ps.setDouble(4, mod._mAtkMod);
			ps.setDouble(5, mod._pDefMod);
			ps.setDouble(6, mod._mDefMod);
			ps.executeUpdate();
			
			String key = classAtk + ":" + classTgt;
			_modifiers.put(key, mod);
			
			String atkName = BalanceName.getName(classAtk).toLowerCase();
			String tgtName = BalanceName.getName(classTgt).toLowerCase();
			
			System.out.println("Loaded updating: " + atkName + " Vs " + tgtName + ".");
		}
		catch (Exception e)
		{
			System.out.println("Error updating modifier: " + e);
		}
	}
	
	public void saveVulnerability(String type, double multiplier)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO balance_vulnerability (skill_type, multiplier) VALUES (?, ?)"))
		{
			ps.setString(1, type);
			ps.setDouble(2, multiplier);
			ps.executeUpdate();
			
			_vulnModifiers.put(type, multiplier);
			System.out.println("Loaded updated vulnerability: " + type + " = " + multiplier);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static BalanceData getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static final class SingletonHolder
	{
		protected static final BalanceData instance = new BalanceData();
	}
}
