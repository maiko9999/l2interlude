package net.sf.l2j.loginserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.crypt.NewCrypt;
import net.sf.l2j.loginserver.data.manager.GameServerManager;
import net.sf.l2j.loginserver.data.manager.IpBanManager;
import net.sf.l2j.loginserver.data.sql.AccountTable;
import net.sf.l2j.loginserver.model.GameServerInfo;
import net.sf.l2j.loginserver.network.SessionKey;
import net.sf.l2j.loginserver.network.gameserverpackets.BlowFishKey;
import net.sf.l2j.loginserver.network.gameserverpackets.ChangeAccessLevel;
import net.sf.l2j.loginserver.network.gameserverpackets.GameServerAuth;
import net.sf.l2j.loginserver.network.gameserverpackets.PlayerAuthRequest;
import net.sf.l2j.loginserver.network.gameserverpackets.PlayerInGame;
import net.sf.l2j.loginserver.network.gameserverpackets.PlayerLogout;
import net.sf.l2j.loginserver.network.gameserverpackets.ServerStatus;
import net.sf.l2j.loginserver.network.loginserverpackets.AuthResponse;
import net.sf.l2j.loginserver.network.loginserverpackets.InitLS;
import net.sf.l2j.loginserver.network.loginserverpackets.KickPlayer;
import net.sf.l2j.loginserver.network.loginserverpackets.LoginServerFail;
import net.sf.l2j.loginserver.network.loginserverpackets.PlayerAuthResponse;
import net.sf.l2j.loginserver.network.serverpackets.ServerBasePacket;

public class GameServerThread extends Thread
{
	private static final CLogger LOGGER = new CLogger(GameServerThread.class.getName());
	
	private final Set<String> _accountsOnGameServer = new HashSet<>();
	
	private final Socket _connection;
	private final String _connectionIp;
	
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	
	private InputStream _in;
	private OutputStream _out;
	
	private NewCrypt _blowfish;
	
	private GameServerInfo _gsi;
	
	public GameServerThread(Socket con)
	{
		_connection = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		
		try
		{
			_in = _connection.getInputStream();
			_out = new BufferedOutputStream(_connection.getOutputStream());
		}
		catch (IOException e)
		{
			LOGGER.debug("Couldn't process gameserver input stream.", e);
		}
		
		final KeyPair pair = GameServerManager.getInstance().getKeyPair();
		
		_privateKey = (RSAPrivateKey) pair.getPrivate();
		_publicKey = (RSAPublicKey) pair.getPublic();
		
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
		
		start();
	}
	
	@Override
	public void run()
	{
		// Ensure no further processing for this connection if server is considered as banned.
		if (IpBanManager.getInstance().isBannedAddress(_connection.getInetAddress()))
		{
			LOGGER.info("Banned gameserver with IP {} tried to register.", _connection.getInetAddress().getHostAddress());
			forceClose(LoginServerFail.REASON_IP_BANNED);
			return;
		}
		
		try
		{
			sendPacket(new InitLS(_publicKey.getModulus().toByteArray()));
			
			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			for (;;)
			{
				lengthLo = _in.read();
				lengthHi = _in.read();
				length = lengthHi * 256 + lengthLo;
				
				if (lengthHi < 0 || length < 2 || _connection.isClosed())
					break;
				
				byte[] data = new byte[length - 2];
				
				int receivedBytes = 0;
				int newBytes = 0;
				while (newBytes != -1 && receivedBytes < length - 2)
				{
					newBytes = _in.read(data, 0, length - 2);
					receivedBytes = receivedBytes + newBytes;
				}
				
				if (receivedBytes != length - 2)
					break;
				
				// Decrypt if we have a key.
				_blowfish.decrypt(data, 0, data.length);
				
				checksumOk = NewCrypt.verifyChecksum(data);
				if (!checksumOk)
					return;
				
				int packetType = data[0] & 0xff;
				switch (packetType)
				{
					case 00:
						onReceiveBlowfishKey(data);
						break;
					
					case 01:
						onGameServerAuth(data);
						break;
					
					case 02:
						onReceivePlayerInGame(data);
						break;
					
					case 03:
						onReceivePlayerLogOut(data);
						break;
					
					case 04:
						onReceiveChangeAccessLevel(data);
						break;
					
					case 05:
						onReceivePlayerAuthRequest(data);
						break;
					
					case 06:
						onReceiveServerStatus(data);
						break;
					
					default:
						LOGGER.warn("Unknown opcode ({}) from gameserver, closing connection.", Integer.toHexString(packetType).toUpperCase());
						forceClose(LoginServerFail.NOT_AUTHED);
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.debug("Couldn't process packet.", e);
		}
		finally
		{
			if (isAuthed())
			{
				_gsi.setDown();
				LOGGER.info("GameServer [{}] {} is now set as disconnected.", getServerId(), GameServerManager.getInstance().getServerNames().get(getServerId()));
			}
			LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}
	
	private void onReceiveBlowfishKey(byte[] data)
	{
		final BlowFishKey bfk = new BlowFishKey(data, _privateKey);
		
		_blowfish = new NewCrypt(bfk.getKey());
	}
	
	private void onGameServerAuth(byte[] data)
	{
		handleRegProcess(new GameServerAuth(data));
		
		if (isAuthed())
			sendPacket(new AuthResponse(_gsi.getId()));
	}
	
	private void onReceivePlayerInGame(byte[] data)
	{
		if (isAuthed())
		{
			final PlayerInGame pig = new PlayerInGame(data);
			
			for (String account : pig.getAccounts())
				_accountsOnGameServer.add(account);
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}
	
	private void onReceivePlayerLogOut(byte[] data)
	{
		if (isAuthed())
		{
			final PlayerLogout plo = new PlayerLogout(data);
			
			_accountsOnGameServer.remove(plo.getAccount());
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}
	
	private void onReceiveChangeAccessLevel(byte[] data)
	{
		if (isAuthed())
		{
			final ChangeAccessLevel cal = new ChangeAccessLevel(data);
			
			AccountTable.getInstance().setAccountAccessLevel(cal.getAccount(), cal.getLevel());
			LOGGER.info("Changed {} access level to {}.", cal.getAccount(), cal.getLevel());
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}
	
	private void onReceivePlayerAuthRequest(byte[] data)
	{
		if (isAuthed())
		{
			final PlayerAuthRequest par = new PlayerAuthRequest(data);
			final SessionKey key = LoginController.getInstance().getKeyForAccount(par.getAccount());
			
			if (Config.PROXY)
			{
				var authedClient = LoginController.getInstance().getAuthedClient(par.getAccount());
				var ipAddress = authedClient != null ? authedClient.getConnection().getInetAddress().getHostAddress() : "";
				
				if (key != null && key.equals(par.getKey()))
				{
					LoginController.getInstance().removeAuthedLoginClient(par.getAccount());
					sendPacket(new PlayerAuthResponse(par.getAccount(), true, ipAddress));
				}
				else
					sendPacket(new PlayerAuthResponse(par.getAccount(), false, ipAddress));
			}
			else
			{
				if (key != null && key.equals(par.getKey()))
				{
					LoginController.getInstance().removeAuthedLoginClient(par.getAccount());
					sendPacket(new PlayerAuthResponse(par.getAccount(), true));
				}
				else
					sendPacket(new PlayerAuthResponse(par.getAccount(), false));
			}
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}
	
	private void onReceiveServerStatus(byte[] data)
	{
		if (isAuthed())
			new ServerStatus(data, getServerId()); // will do the actions by itself
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}
	
	private void handleRegProcess(GameServerAuth gameServerAuth)
	{
		final int id = gameServerAuth.getDesiredID();
		final byte[] hexId = gameServerAuth.getHexID();
		
		GameServerInfo gsi = GameServerManager.getInstance().getRegisteredGameServers().get(id);
		// is there a gameserver registered with this id?
		if (gsi != null)
		{
			// does the hex id match?
			if (Arrays.equals(gsi.getHexId(), hexId))
			{
				// check to see if this GS is already connected
				synchronized (gsi)
				{
					if (gsi.isAuthed())
						forceClose(LoginServerFail.REASON_ALREADY_LOGGED_IN);
					else
						attachGameServerInfo(gsi, gameServerAuth);
				}
			}
			else
			{
				// there is already a server registered with the desired id and different hex id
				// try to register this one with an alternative id
				if (Config.ACCEPT_NEW_GAMESERVER && gameServerAuth.acceptAlternateID())
				{
					gsi = new GameServerInfo(id, hexId, this);
					if (GameServerManager.getInstance().registerWithFirstAvailableId(gsi))
					{
						attachGameServerInfo(gsi, gameServerAuth);
						GameServerManager.getInstance().registerServerOnDB(gsi);
					}
					else
						forceClose(LoginServerFail.REASON_NO_FREE_ID);
				}
				// server id is already taken, and we cant get a new one for you
				else
					forceClose(LoginServerFail.REASON_WRONG_HEXID);
			}
		}
		else
		{
			// can we register on this id?
			if (Config.ACCEPT_NEW_GAMESERVER)
			{
				gsi = new GameServerInfo(id, hexId, this);
				if (GameServerManager.getInstance().register(id, gsi))
				{
					attachGameServerInfo(gsi, gameServerAuth);
					GameServerManager.getInstance().registerServerOnDB(gsi);
				}
				// some one took this ID meanwhile
				else
					forceClose(LoginServerFail.REASON_ID_RESERVED);
			}
			else
				forceClose(LoginServerFail.REASON_WRONG_HEXID);
		}
	}
	
	/**
	 * Attachs a GameServerInfo to this Thread
	 * <li>Updates the GameServerInfo values based on GameServerAuth packet</li>
	 * <li><b>Sets the GameServerInfo as Authed</b></li>
	 * @param gsi The GameServerInfo to be attached.
	 * @param gameServerAuth The server info.
	 */
	private void attachGameServerInfo(GameServerInfo gsi, GameServerAuth gameServerAuth)
	{
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPort(gameServerAuth.getPort());
		
		if (!gameServerAuth.getHostName().equals("*"))
		{
			try
			{
				_gsi.setHostName(InetAddress.getByName(gameServerAuth.getHostName()).getHostAddress());
			}
			catch (UnknownHostException e)
			{
				LOGGER.error("Couldn't resolve hostname '{}'.", e, gameServerAuth.getHostName());
				_gsi.setHostName(_connectionIp);
			}
		}
		else
			_gsi.setHostName(_connectionIp);
		
		gsi.setMaxPlayers(gameServerAuth.getMaxPlayers());
		gsi.setAuthed(true);
		
		LOGGER.info("Hooked [{}] {} gameserver on: {}.", getServerId(), GameServerManager.getInstance().getServerNames().get(getServerId()), _gsi.getHostName());
	}
	
	private void forceClose(int reason)
	{
		sendPacket(new LoginServerFail(reason));
		
		try
		{
			_connection.close();
		}
		catch (IOException e)
		{
			LOGGER.debug("Failed disconnecting banned server, server is already disconnected.", e);
		}
	}
	
	private void sendPacket(ServerBasePacket sl)
	{
		try
		{
			byte[] data = sl.getContent();
			NewCrypt.appendChecksum(data);
			data = _blowfish.crypt(data);
			
			int len = data.length + 2;
			synchronized (_out)
			{
				_out.write(len & 0xff);
				_out.write(len >> 8 & 0xff);
				_out.write(data);
				_out.flush();
			}
		}
		catch (IOException e)
		{
			LOGGER.error("Exception while sending packet {}.", sl.getClass().getSimpleName());
		}
	}
	
	public boolean hasAccountOnGameServer(String account)
	{
		return _accountsOnGameServer.contains(account);
	}
	
	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}
	
	public void kickPlayer(String account)
	{
		sendPacket(new KickPlayer(account));
	}
	
	public boolean isAuthed()
	{
		return _gsi != null && _gsi.isAuthed();
	}
	
	public GameServerInfo getGameServerInfo()
	{
		return _gsi;
	}
	
	public void setGameServerInfo(GameServerInfo gsi)
	{
		_gsi = gsi;
	}
	
	private int getServerId()
	{
		return (_gsi == null) ? -1 : _gsi.getId();
	}
	
	public String getConnectionIp()
	{
		return _connectionIp;
	}
}