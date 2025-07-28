package net.sf.l2j.gameserver.network.loginserverpackets;

public class AuthResponse extends LoginServerBasePacket
{
	private final int _serverId;
	private final String _serverName;
	private final String _realIpAddress;
	
	public AuthResponse(byte[] decrypt)
	{
		super(decrypt);
		
		_serverId = readC();
		_serverName = readS();
		_realIpAddress = readS();
	}
	
	public int getServerId()
	{
		return _serverId;
	}
	
	public String getServerName()
	{
		return _serverName;
	}
	
	public String getRealIpAddress()
	{
		return _realIpAddress;
	}
}