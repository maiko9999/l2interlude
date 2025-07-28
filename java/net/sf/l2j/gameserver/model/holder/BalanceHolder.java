package net.sf.l2j.gameserver.model.holder;

public class BalanceHolder
{
	public double _pAtkMod;
	public double _mAtkMod;
	public double _pDefMod;
	public double _mDefMod;
	
	public BalanceHolder(double pAtk, double mAtk, double pDef, double mDef)
	{
		_pAtkMod = pAtk;
		_mAtkMod = mAtk;
		_pDefMod = pDef;
		_mDefMod = mDef;
	}
}