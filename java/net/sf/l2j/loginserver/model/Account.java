package net.sf.l2j.loginserver.model;

import java.net.InetAddress;

import net.sf.l2j.commons.network.ServerType;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.data.manager.GameServerManager;
import net.sf.l2j.loginserver.data.manager.ProxyManager;
import net.sf.l2j.loginserver.data.sql.AccountTable;

public final class Account
{
	private final String _login;
	private final String _password;
	
	private final int _accessLevel;
	private final int _lastServer;
	
	private InetAddress _clientIp;
	
	public Account(final String login, final String password, final int accessLevel, final int lastServer)
	{
		_login = login.toLowerCase();
		_password = password;
		_accessLevel = accessLevel;
		_lastServer = lastServer;
	}
	
	public String getLogin()
	{
		return _login;
	}
	
	public String getPassword()
	{
		return _password;
	}
	
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public int getLastServer()
	{
		return _lastServer;
	}
	
	public InetAddress getClientIp()
	{
		return _clientIp;
	}
	
	public void setClientIp(InetAddress addr)
	{
		_clientIp = addr;
	}
	
	public final boolean isLoginPossible(int serverId)
	{
		GameServerInfo gsi = GameServerManager.getInstance().getRegisteredGameServers().get(serverId);
		
		if (!Config.PROXY)
		{
			L2Proxy proxy = ProxyManager.getInstance().getProxyById(serverId);
			
			if (gsi == null && proxy != null)
				gsi = GameServerManager.getInstance().getRegisteredGameServers().get(proxy.getGameserverId());
		}
		
		if (gsi == null || !gsi.isAuthed())
			return false;
		
		final ServerType type = gsi.getType();
		
		// DOWN status doesn't allow anyone to logon.
		if (type == ServerType.DOWN)
			return false;
		
		// GM_ONLY status or full server only allows superior access levels accounts to login. Otherwise, any positive access level account can login.
		final boolean canLogin = (type == ServerType.GM_ONLY || gsi.getCurrentPlayerCount() >= gsi.getMaxPlayers()) ? _accessLevel > 0 : _accessLevel >= 0;
		if (canLogin && _lastServer != serverId)
			AccountTable.getInstance().setAccountLastServer(_login, serverId);
		
		return canLogin;
	}
}