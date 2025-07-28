package net.sf.l2j.loginserver.data.xml;

import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.HashMap;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.loginserver.data.manager.ProxyManager;
import net.sf.l2j.loginserver.model.L2Proxy;
import net.sf.l2j.loginserver.model.L2ProxyInfo;

import org.w3c.dom.Document;

public class ProxyDataLoader implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(ProxyDataLoader.class.getName());
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/proxy.xml");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		var proxyInfoList = new HashMap<Integer, L2ProxyInfo>();
		var proxies = new HashMap<Integer, L2Proxy>();
		
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "gameserver", gameserverNode ->
			{
				final var gameserverSet = parseAttributes(gameserverNode);
				var serverId = gameserverSet.getInteger("serverId");
				var hidesGameserver = gameserverSet.getBool("hide");
				var fallbackToGameserver = gameserverSet.getBool("fallbackToGameserver");
				var proxyInfo = new L2ProxyInfo(serverId, hidesGameserver, fallbackToGameserver);
				proxyInfoList.put(serverId, proxyInfo);
				
				forEach(gameserverNode, "proxy", proxyNode ->
				{
					final var proxySet = parseAttributes(proxyNode);
					try
					{
						final var proxy = new L2Proxy(serverId, proxySet.getInteger("proxyServerId"), proxySet.getString("proxyHost"), proxySet.getInteger("proxyPort"), proxySet.getString("apiKey"), proxySet.getInteger("apiPort"), proxySet.getBool("validateHealth"));
						proxies.put(proxySet.getInteger("proxyServerId"), proxy);
					}
					catch (UnknownHostException ex)
					{
						LOGGER.warn("Failed to process proxy due to badly formatted proxy host", ex);
					}
				});
			});
		});
		
		ProxyManager.getInstance().initialise(proxyInfoList, proxies);
	}
	
	public static ProxyDataLoader getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ProxyDataLoader INSTANCE = new ProxyDataLoader();
	}
}