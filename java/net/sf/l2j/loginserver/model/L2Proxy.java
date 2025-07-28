package net.sf.l2j.loginserver.model;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class L2Proxy
{
	private final int _gameserverId;
	private final int _proxyServerId;
	private final InetAddress _proxyAddress;
	private final int _proxyPort;
	private final String _apiKey;
	private final int _apiPort;
	private final boolean _validateHealth;
	private boolean _isHealthy = true;
	private int _maxConnections = -1;
	
	public L2Proxy(int gameserverId, int proxyServerId, String proxyHost, int proxyPort, String apiKey, int apiPort, boolean validateHealth) throws UnknownHostException
	{
		_gameserverId = gameserverId;
		_proxyServerId = proxyServerId;
		_proxyAddress = InetAddress.getByName(proxyHost);
		_proxyPort = proxyPort;
		_apiKey = apiKey;
		_apiPort = apiPort;
		_validateHealth = validateHealth;
	}
	
	public int getGameserverId()
	{
		return _gameserverId;
	}
	
	public int getProxyServerId()
	{
		return _proxyServerId;
	}
	
	public InetAddress getProxyAddress()
	{
		return _proxyAddress;
	}
	
	public int getProxyPort()
	{
		return _proxyPort;
	}
	
	public boolean isHealthy()
	{
		return _isHealthy;
	}
	
	public void setHealthy(boolean healthy)
	{
		_isHealthy = healthy;
	}
	
	public int getMaxConnections()
	{
		return _maxConnections;
	}
	
	public void setMaxConnections(int maxConnections)
	{
		_maxConnections = maxConnections;
	}
	
	public String getApiKey()
	{
		return _apiKey;
	}
	
	public int getApiPort()
	{
		return _apiPort;
	}
	
	public boolean shouldValidateHealth()
	{
		return _validateHealth;
	}
}