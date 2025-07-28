package net.sf.l2j.loginserver.network.loginserverpackets;

import net.sf.l2j.loginserver.network.serverpackets.ServerBasePacket;

public class PlayerAuthResponse extends ServerBasePacket
{
	public PlayerAuthResponse(String account, boolean response)
	{
		writeC(0x03);
		writeS(account);
		writeC(response ? 1 : 0);
	}
	
	public PlayerAuthResponse(String account, boolean response, String realIp)
	{
		writeC(0x03);
		writeS(account);
		writeC(response ? 1 : 0);
		writeS(realIp);
	}
	
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}