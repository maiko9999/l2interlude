package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.World;

public class AskJoinAlly extends L2GameServerPacket
{
	private final String _requestorName;
	private final int _requestorObjId;
	
	public AskJoinAlly(int requestorObjId, String requestorName)
	{
		_requestorName = requestorName;
		_requestorObjId = requestorObjId;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xa8);
		writeD(_requestorObjId);
		writeS(_requestorName);
		writeS(null);
		writeS(World.getInstance().getPlayer(_requestorObjId).getName());
	}
}