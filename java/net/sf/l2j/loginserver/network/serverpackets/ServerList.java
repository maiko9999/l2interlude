package net.sf.l2j.loginserver.network.serverpackets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.commons.network.ServerType;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.data.manager.GameServerManager;
import net.sf.l2j.loginserver.data.manager.ProxyManager;
import net.sf.l2j.loginserver.model.Account;
import net.sf.l2j.loginserver.model.GameServerInfo;
import net.sf.l2j.loginserver.model.L2Proxy;
import net.sf.l2j.loginserver.model.ServerData;

public final class ServerList extends L2LoginServerPacket
{
	private final List<ServerData> _servers = new ArrayList<>();
	
	private final int _lastServer;
	
	private static final Comparator<ServerData> SERVER_DATA_COMPARATOR = Comparator.comparingInt(ServerData::serverId);
	
	public ServerList(Account account)
	{
		_lastServer = account.getLastServer();
		
		for (GameServerInfo gsi : GameServerManager.getInstance().getRegisteredGameServers().values())
		{
			final ServerType type = (account.getAccessLevel() < 0 || (gsi.getType() == ServerType.GM_ONLY && account.getAccessLevel() <= 0)) ? ServerType.DOWN : gsi.getType();
			final String hostName = (isLocalIp(account.getClientIp()) && gsi.getGameServerThread() != null) ? gsi.getGameServerThread().getConnectionIp() : gsi.getHostName();
			
			if (Config.PROXY)
			{
				var proxyInfo = ProxyManager.getInstance().getProxyInfoByGameserverId(gsi.getId());
				
				if (proxyInfo == null)
				{
					_servers.add(new ServerData(type, hostName, gsi));
					continue;
				}
				
				var proxiesForGameserver = ProxyManager.getInstance().getProxies().stream().filter(x -> x.getGameserverId() == gsi.getId()).collect(Collectors.toList());
				if (!proxyInfo.hidesGameServer())
					_servers.add(new ServerData(type, hostName, gsi));
				
				if (proxyInfo.fallbackToGameServer() && proxiesForGameserver.stream().noneMatch(L2Proxy::isHealthy))
					_servers.add(new ServerData(type, hostName, gsi));
				
				proxiesForGameserver.forEach(proxy ->
				{
					var newType = proxy.isHealthy() ? ServerType.AUTO : ServerType.DOWN;
					var proxyGsi = gsi.cloneInfo();
					
					proxyGsi.setPort(proxy.getProxyPort());
					proxyGsi.setId(proxy.getProxyServerId());
					
					if (gsi.getType() == ServerType.DOWN)
					{
						proxyGsi.setDown();
						newType = ServerType.DOWN;
					}
					_servers.add(new ServerData(newType, proxy.getProxyAddress().getHostAddress(), proxyGsi));
				});
			}
			else
				_servers.add(new ServerData(type, hostName, gsi));
		}
		
		if (Config.PROXY)
			_servers.sort(SERVER_DATA_COMPARATOR);
	}
	
	@Override
	public void write()
	{
		writeC(0x04);
		writeC(_servers.size());
		writeC(_lastServer);
		
		for (ServerData server : _servers)
		{
			writeC(server.serverId());
			
			try
			{
				final byte[] raw = InetAddress.getByName(server.hostName()).getAddress();
				writeC(raw[0] & 0xff);
				writeC(raw[1] & 0xff);
				writeC(raw[2] & 0xff);
				writeC(raw[3] & 0xff);
			}
			catch (UnknownHostException e)
			{
				e.printStackTrace();
				writeC(127);
				writeC(0);
				writeC(0);
				writeC(1);
			}
			
			writeD(server.port());
			writeC(server.ageLimit());
			writeC(server.isPvp() ? 0x01 : 0x00);
			writeH(server.currentPlayers());
			writeH(server.maxPlayers());
			writeC(server.type() == ServerType.DOWN ? 0x00 : 0x01);
			
			int bits = 0;
			if (server.isTestServer())
				bits |= 0x04;
			
			if (server.isShowingClock())
				bits |= 0x02;
			
			writeD(bits);
			writeC(server.isShowingBrackets() ? 0x01 : 0x00);
		}
	}
	
	public static boolean isLocalIp(InetAddress address)
	{
		return address == null || address.isLinkLocalAddress() || address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isSiteLocalAddress();
	}
}