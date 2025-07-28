package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.manager.PcCafeManager;
import net.sf.l2j.gameserver.enums.PcCafeConsumeType;

public class ExPCCafePointInfo extends L2GameServerPacket
{
	private final int _score;
	private final int _modify;
	private final int _remainingTime;
	private PcCafeConsumeType _pointType;
	private final int _periodType;
	
	public ExPCCafePointInfo(int score, int modify, PcCafeConsumeType pointType)
	{
		_score = score;
		_modify = modify;
		_remainingTime = 0;
		_pointType = pointType;
		_periodType = PcCafeManager.getInstance().enableEvent(); // get point time
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_score);
		writeD(_modify);
		writeC(_periodType);
		writeD(_remainingTime);
		writeC(_pointType.ordinal());
	}
}