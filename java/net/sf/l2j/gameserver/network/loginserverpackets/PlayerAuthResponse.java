package net.sf.l2j.gameserver.network.loginserverpackets;

public class PlayerAuthResponse extends LoginServerBasePacket
{
	private final String _account;
	private final boolean _authed;
	private final String _realIpAddress;
	
	public PlayerAuthResponse(byte[] decrypt)
	{
		super(decrypt);
		
		_account = readS();
		_authed = readC() != 0;
		_realIpAddress = readS();
	}
	
	public String getAccount()
	{
		return _account;
	}
	
	public boolean isAuthed()
	{
		return _authed;
	}
	
	public String getRealIpAddress()
	{
		return _realIpAddress;
	}
}