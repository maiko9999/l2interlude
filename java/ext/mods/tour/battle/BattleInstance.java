package ext.mods.tour.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadUserInfo;

import ext.mods.InstanceMap.InstanceManager;
import ext.mods.InstanceMap.MapInstance;
import ext.mods.tour.holder.TourHolder;
import ext.mods.tour.ranking.TournamentRankingManager;

public class BattleInstance
{
	private final int _id;
	private final List<Player> _players = new ArrayList<>();
	private final List<Player> _teamOne = new ArrayList<>();
	private final List<Player> _teamTwo = new ArrayList<>();
	private boolean _isActive;
	private ScheduledFuture<?> _battleTimeout;
	private ScheduledFuture<?> _checkPlayers;
	private final TourHolder _holder;
	private final MapInstance instance;
	public int index = 0;
	
	public BattleInstance(int id, TourHolder holder, Player... players)
	{
		_id = id;
		_players.addAll(Arrays.asList(players));
		_holder = holder;
		this.instance = InstanceManager.getInstance().createInstance();
		
		splitTeams();
	}
	
	private void splitTeams()
	{
		int halfSize = _players.size() / 2;
		for (int i = 0; i < _players.size(); i++)
		{
			if (i < halfSize)
				_teamOne.add(_players.get(i));
			else
				_teamTwo.add(_players.get(i));
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public boolean contains(Player player)
	{
		return _players.contains(player);
	}
	
	public void start()
	{
		_isActive = true;
		
		int middleX = _holder.getX();
		int middleY = _holder.getY();
		int z = _holder.getZ();
		int distance = 700;
		
		for (Player player : _teamOne)
		{
			setupPlayer(player, middleX - distance / 2, middleY, z, 32768, _teamTwo);
			index++;
		}
		
		for (Player player : _teamTwo)
		{
			setupPlayer(player, middleX + distance / 2, middleY, z, 49152, _teamOne);
			index++;
		}
		
		showOpponentStatus();
		
		_battleTimeout = ThreadPool.schedule(() -> timeoutBattle(), _holder.getDuration() * 60 * 1000L);
		if (_checkPlayers == null || _checkPlayers.isCancelled())
			_checkPlayers = ThreadPool.scheduleAtFixedRate(() -> checkPlayers(), 500, 1000);
	}
	
	private void setupPlayer(Player player, int x, int y, int z, int heading, List<Player> opponents)
	{
		player.saveTournamentData();
		player.setInstanceMap(instance, true);
		player.setTournamentBattle(this);
		player.setTarget(null);
		player.setInvul(false);
		
		player.getStatus().setCpHpMp(player.getStatus().getCp(), player.getStatus().getHp(), player.getStatus().getMp());
		player.sendMessage("Get ready! The fight has begun!");
		
		if (player.getSummon() != null)
		{
			player.getSummon().setInstanceMap(instance, true);
			player.getSummon().teleportTo(x, y, z, 75);
		}
		player.teleportTo(x, y, z, 75);
		
		player.setTournamentOpponents(new ArrayList<>(opponents));
	}
	
	private void checkPlayers()
	{
		if (!_isActive)
			return;
		
		for (Player player : _players)
		{
			if (player == null || !player.isOnline())
			{
				_isActive = false;
				cancelTasks();
				broadcastMessage("The battle ended in a draw due to disconnection.");
				cleanUp();
				TournamentManager.getInstance().removeBattle(this);
				return;
			}
		}
	}
	
	public void onPlayerDeath(Player dead)
	{
		if (!_isActive)
			return;
		
		if (_teamOne.contains(dead))
			_teamOne.remove(dead);
		else if (_teamTwo.contains(dead))
			_teamTwo.remove(dead);
		
		if (_teamOne.isEmpty())
			winBattle(_teamTwo);
		else if (_teamTwo.isEmpty())
			winBattle(_teamOne);
	}
	
	private void winBattle(List<Player> winners)
	{
		if (!_isActive)
			return;
		
		for (Player winner : winners)
		{
			for (IntIntHolder reward : _holder.getRewards())
			{
				if (reward.getId() > 0 && reward.getValue() > 0)
					winner.addItem(reward.getId(), reward.getValue(), true);
			}
			winner.sendMessage("Your team won the duel!");
			TournamentRankingManager.getInstance().addWin(winner);
		}
		
		for (Player player : _players)
		{
			if (!winners.contains(player))
			{
				TournamentRankingManager.getInstance().addLoss(player);
			}
		}
		
		broadcastMessage("The battle is over!");
		
		_isActive = false;
		cleanUp();
		cancelTasks();
		TournamentManager.getInstance().removeBattle(this);
	}
	
	public void forceEnd()
	{
		if (!_isActive)
			return;
		
		_isActive = false;
		cancelTasks();
		broadcastMessage("The battle is over.");
		
		for (Player player : _players)
		{
			TournamentRankingManager.getInstance().addDraw(player);
		}
		
		cleanUp();
		TournamentManager.getInstance().removeBattle(this);
	}
	
	private void timeoutBattle()
	{
		if (!_isActive)
			return;
		
		_isActive = false;
		cancelTasks();
		broadcastMessage("Time's up! The battle ended in a draw.");
		
		for (Player player : _players)
		{
			TournamentRankingManager.getInstance().addDraw(player);
		}
		
		cleanUp();
		TournamentManager.getInstance().removeBattle(this);
	}
	
	private void cancelTasks()
	{
		if (_battleTimeout != null)
		{
			_battleTimeout.cancel(false);
			_battleTimeout = null;
		}
		
		if (_checkPlayers != null)
		{
			_checkPlayers.cancel(false);
			_checkPlayers = null;
		}
	}
	
	private void cleanUp()
	{
		for (Player player : _players)
		{
			if (player != null)
			{
				player.restoreTournamentData();
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.sendPacket(new ExOlympiadMode(0));
				
				player.setInstanceMap(InstanceManager.getInstance().getInstance(0), true);
				
			}
		}
		
		InstanceManager.getInstance().deleteInstance(instance.getId());
	}
	
	private void showOpponentStatus()
	{
		int side = 1;
		for (Player player : _players)
		{
			player.setOlympiadSide(side);
			player.sendPacket(new ExOlympiadMode(side));
			player.getStatus().broadcastStatusUpdate();
			for (Player opponent : player.getTournamentOpponents())
			{
				player.sendPacket(new ExOlympiadUserInfo(opponent));
				opponent.getStatus().broadcastStatusUpdate();
			}
			side = (side == 1) ? 2 : 1;
		}
	}
	
	private void broadcastMessage(String text)
	{
		for (Player player : _players)
		{
			if (player != null)
				player.sendMessage(text);
		}
	}
}
