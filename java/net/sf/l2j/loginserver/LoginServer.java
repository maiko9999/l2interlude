package net.sf.l2j.loginserver;

import java.awt.GraphicsEnvironment;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.LogManager;

import net.sf.l2j.commons.gui.InterfaceLS;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.mmocore.SelectorConfig;
import net.sf.l2j.commons.mmocore.SelectorThread;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.data.manager.GameServerManager;
import net.sf.l2j.loginserver.data.manager.IpBanManager;
import net.sf.l2j.loginserver.data.sql.AccountTable;
import net.sf.l2j.loginserver.data.xml.ProxyDataLoader;
import net.sf.l2j.loginserver.network.LoginClient;
import net.sf.l2j.loginserver.network.LoginPacketHandler;

public class LoginServer
{
	private static final CLogger LOGGER = new CLogger(LoginServer.class.getName());
	
	public static final int PROTOCOL_REV = 0x0102;
	
	private static LoginServer _loginServer;
	
	private GameServerListener _gameServerListener;
	private SelectorThread<LoginClient> _selectorThread;
	
	public static void main(String[] args) throws Exception
	{
		_loginServer = new LoginServer();
	}
	
	/*
	 * Create directories for logs
	 */
	static void createDirectories()
	{
		try
		{
			createDirectory("log");
			createDirectory("log/console");
			createDirectory("log/error");
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to create directories.", e);
		}
	}
	
	/**
	 * Create a directory relative to the {@link net.sf.l2j.Config#BASE_PATH base path}. Create last directory in the {@code path}.
	 * @param path the path to create
	 */
	static void createDirectory(String path)
	{
		var p = Config.BASE_PATH;
		
		if (Config.DEV_MODE)
			p = p.resolve("dev").resolve("login");
		
		p.resolve(path).toFile().mkdir();
	}
	
	public LoginServer() throws Exception
	{
		if (Config.DEV_MODE)
			Config.BASE_PATH.resolve("dev").resolve("login").toFile().mkdirs();
		
		// Create log folder
		createDirectories();
		
		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(Config.CONFIG_PATH.resolve("logging.properties").toFile()))
		{
			LogManager.getLogManager().updateConfiguration(is, (key) ->
			{
				return (oldValue, newValue) ->
				{
					if (key.endsWith(".pattern"))
					{
						if (Config.DEV_MODE)
							return Config.BASE_PATH.resolve("dev").resolve("login").resolve(newValue).toString();
						else
							return Config.BASE_PATH.resolve(newValue).toString();
					}
					return newValue;
				};
			});
		}
		// GUI
		if (!GraphicsEnvironment.isHeadless())
		{
			System.out.println("Login: Running in Interface GUI.");
			new InterfaceLS();
		}
		StringUtil.printSection("Config");
		Config.loadLoginServer();
		
		StringUtil.printSection("Poolers");
		ConnectionPool.init();
		
		AccountTable.getInstance();
		
		StringUtil.printSection("LoginController");
		LoginController.getInstance();
		
		StringUtil.printSection("GameServerManager");
		GameServerManager.getInstance();
		
		if (Config.PROXY)
		{
			StringUtil.printSection("Proxy");
			ProxyDataLoader.getInstance().load();
		}
		
		StringUtil.printSection("Ban List");
		IpBanManager.getInstance();
		
		StringUtil.printSection("IP, Ports & Socket infos");
		InetAddress bindAddress = null;
		if (!Config.LOGINSERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.LOGINSERVER_HOSTNAME);
			}
			catch (UnknownHostException uhe)
			{
				LOGGER.error("The LoginServer bind address is invalid, using all available IPs.", uhe);
			}
		}
		
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		
		final LoginPacketHandler lph = new LoginPacketHandler();
		final SelectorHelper sh = new SelectorHelper();
		try
		{
			_selectorThread = new SelectorThread<>(sc, sh, lph, sh, sh);
		}
		catch (IOException ioe)
		{
			LOGGER.error("Failed to open selector.", ioe);
			
			System.exit(1);
		}
		
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			
			LOGGER.info("Listening for gameservers on {}:{}.", Config.GAMESERVER_LOGIN_HOSTNAME, Config.GAMESERVER_LOGIN_PORT);
		}
		catch (IOException ioe)
		{
			LOGGER.error("Failed to start the gameserver listener.", ioe);
			
			System.exit(1);
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.LOGINSERVER_PORT);
		}
		catch (IOException ioe)
		{
			LOGGER.error("Failed to open server socket.", ioe);
			
			System.exit(1);
		}
		_selectorThread.start();
		LOGGER.info("Loginserver ready on {}:{}.", (bindAddress == null) ? "*" : bindAddress.getHostAddress(), Config.LOGINSERVER_PORT);
		
		StringUtil.printSection("Waiting for gameserver answer");
	}
	
	public static LoginServer getInstance()
	{
		return _loginServer;
	}
	
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	public void shutdown(boolean restart)
	{
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
}