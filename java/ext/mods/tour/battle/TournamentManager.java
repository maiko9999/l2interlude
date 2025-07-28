package ext.mods.tour.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

import ext.mods.tour.holder.TourHolder;

public class TournamentManager
{
	private Map<Integer, BattleInstance> _battles = new ConcurrentHashMap<>();
	
	public List<Player> selectPlayers(int count)
	{
		
		List<Player> allPlayers = World.getInstance().getPlayers().stream().filter(this::isEligible).collect(Collectors.toList());
		Collections.shuffle(allPlayers);
		
		if (count == 2)
		{
			
			List<Player> soloPlayers = allPlayers.stream().filter(player -> !player.isInParty()).limit(2).collect(Collectors.toList());
			return soloPlayers.size() == 2 ? soloPlayers : Collections.emptyList();
		}
		else
		{
			List<Player> selectedPlayers = new ArrayList<>();
			
			for (Player player : allPlayers)
			{
				
				if (!player.isInParty() || !player.getParty().isLeader(player))
				{
					continue;
				}
				
				List<Player> partyMembers = player.getParty().getMembers();
				
				if (partyMembers.size() != (count / 2))
				{
					continue;
				}
				
				if (!partyMembers.stream().allMatch(this::isEligible))
				{
					continue;
				}
				
				selectedPlayers.addAll(partyMembers);
				
				if (selectedPlayers.size() == count)
				{
					break;
				}
			}
			
			return selectedPlayers.size() == count ? selectedPlayers : Collections.emptyList();
		}
	}
	
	private boolean isEligible(Player player)
	{
		if (player == null || player.isInOlympiadMode() || player.isInJail() || player.isDead())
			return false;
		
		if (!player.isInTournament())
			return false;
		
		if (getBattleOf(player) != null)
			return false;
		
		return true;
	}
	
	private static AtomicInteger battleIdCounter = new AtomicInteger(1);
	
	public void startBattle(List<Player> players, TourHolder holder)
	{
		
		int battleId = battleIdCounter.getAndIncrement();
		
		BattleInstance battle = new BattleInstance(battleId, holder, players.toArray(new Player[0]));
		_battles.put(battleId, battle);
		battle.start();
	}
	
	public void onPlayerDeath(Player player)
	{
		BattleInstance battle = getBattleOf(player);
		if (battle != null)
			battle.onPlayerDeath(player);
	}
	
	public void endAllBattles()
	{
		_battles.values().forEach(BattleInstance::forceEnd);
		_battles.clear();
		battleIdCounter.getAndDecrement();
		
	}
	
	private BattleInstance getBattleOf(Player player)
	{
		for (BattleInstance b : _battles.values())
		{
			if (b.contains(player))
				return b;
		}
		return null;
	}
	
	public void removeBattle(BattleInstance battle)
	{
		_battles.remove(battle.getId());
	}
	
	public static TournamentManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		private static final TournamentManager _instance = new TournamentManager();
	}
}