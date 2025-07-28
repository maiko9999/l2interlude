package net.sf.l2j.commons.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sf.l2j.gameserver.custom.data.BalanceData;
import net.sf.l2j.gameserver.model.holder.BalanceHolder;
import net.sf.l2j.gameserver.model.holder.BalanceName;

public class damage extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	
	private final String[] columnNames =
	{
		"Attacker",
		"Target",
		"P.Atk",
		"M.Atk"
	};
	
	private final List<RowData> allData;
	private final List<RowData> data;
	
	public damage()
	{
		allData = new ArrayList<>();
		data = new ArrayList<>();
		for (String key : BalanceData.getInstance().getModifierMap().keySet())
		{
			String[] split = key.split(":");
			int atk = Integer.parseInt(split[0]);
			int tgt = Integer.parseInt(split[1]);
			BalanceHolder mod = BalanceData.getInstance().getModifier(atk, tgt);
			RowData row = new RowData(atk, tgt, mod);
			allData.add(row);
			data.add(row);
		}
	}
	
	@Override
	public int getRowCount()
	{
		return data.size();
	}
	
	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int col)
	{
		return columnNames[col];
	}
	
	@Override
	public boolean isCellEditable(int row, int col)
	{
		return col == 2 || col == 3;
	}
	
	@Override
	public Object getValueAt(int row, int col)
	{
		RowData r = data.get(row);
		return switch (col)
		{
			case 0 -> BalanceName.getName(r._classAtk);
			case 1 -> BalanceName.getName(r._classTgt);
			case 2 -> r._modifier._pAtkMod;
			case 3 -> r._modifier._mAtkMod;
			default -> null;
		};
	}
	
	@Override
	public void setValueAt(Object value, int row, int col)
	{
		RowData r = data.get(row);
		try
		{
			double val = Double.parseDouble(value.toString());
			switch (col)
			{
				case 2 -> r._modifier._pAtkMod = val;
				case 3 -> r._modifier._mAtkMod = val;
			}
			BalanceData.getInstance().updateModifier(r._classAtk, r._classTgt, r._modifier);
			fireTableCellUpdated(row, col);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Invalid value: " + value);
		}
	}
	
	public void filter(String text)
	{
		data.clear();
		if (text == null || text.isEmpty())
		{
			data.addAll(allData);
		}
		else
		{
			String lower = text.toLowerCase();
			for (RowData r : allData)
			{
				String atkName = BalanceName.getName(r._classAtk).toLowerCase();
				if (atkName.startsWith(lower))
				{
					data.add(r);
				}
			}
		}
		fireTableDataChanged();
	}
	
	private static class RowData
	{
		int _classAtk, _classTgt;
		BalanceHolder _modifier;
		
		RowData(int atk, int tgt, BalanceHolder mod)
		{
			_classAtk = atk;
			_classTgt = tgt;
			_modifier = mod;
		}
	}
}
