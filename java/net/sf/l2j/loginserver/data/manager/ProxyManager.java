package net.sf.l2j.loginserver.data.manager;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.util.PredicateHelpers;

import net.sf.l2j.loginserver.data.xml.ProxyDataLoader;
import net.sf.l2j.loginserver.model.L2Proxy;
import net.sf.l2j.loginserver.model.L2ProxyInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

public class ProxyManager
{
	private final CLogger _logger = new CLogger(ProxyDataLoader.class.getName());
	private ScheduledFuture<?> _statusUpdateTask;
	private Map<Integer, L2ProxyInfo> _proxyInfo = new HashMap<>();
	private Map<Integer, L2Proxy> _proxyServers = new HashMap<>();
	private final HttpClient _httpClient = HttpClient.newHttpClient();
	
	public synchronized void initialise(Map<Integer, L2ProxyInfo> proxyInfoList, Map<Integer, L2Proxy> proxies)
	{
		_proxyInfo = proxyInfoList;
		_proxyServers = proxies;
		_logger.info("Loaded {} proxy servers.", _proxyServers.size());
		
		if (_statusUpdateTask != null)
			_statusUpdateTask.cancel(true);
		
		_statusUpdateTask = ThreadPool.scheduleAtFixedRate(updateProxyHealthTask(), 0, 10000);
	}
	
	public L2Proxy getProxyById(int proxyId)
	{
		return _proxyServers.get(proxyId);
	}
	
	public Collection<L2Proxy> getProxies()
	{
		return _proxyServers.values();
	}
	
	public L2ProxyInfo getProxyInfoByGameserverId(int gameserverId)
	{
		return _proxyInfo.get(gameserverId);
	}
	
	private Runnable updateProxyHealthTask()
	{
		return () ->
		{
			var distinctProxyHosts = _proxyServers.values().stream().filter(L2Proxy::shouldValidateHealth).filter(PredicateHelpers.distinctByKeys(L2Proxy::getProxyAddress, L2Proxy::getProxyPort)).map(x -> new Object()
			{
				public final Integer proxyId = x.getProxyServerId();
				public final String apiHost = x.getProxyAddress().getHostAddress();
				public final Integer apiPort = x.getApiPort();
			}).collect(Collectors.toList());
			
			for (var host : distinctProxyHosts)
			{
				var request = HttpRequest.newBuilder(URI.create(String.format("http://%s:%s/_ping", host.apiHost, host.apiPort))).header("accept", "application/json").timeout(Duration.ofSeconds(5)).build();
				
				try
				{
					var response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());
					if (response.statusCode() == 200 && response.body().equals("pong"))
					{
						_proxyServers.get(host.proxyId).setHealthy(true);
						continue;
					}
					_proxyServers.get(host.proxyId).setHealthy(false);
				}
				catch (IOException | InterruptedException e)
				{
					_proxyServers.get(host.proxyId).setHealthy(false);
				}
			}
		};
	}
	
	public static final ProxyManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ProxyManager INSTANCE = new ProxyManager();
	}
}