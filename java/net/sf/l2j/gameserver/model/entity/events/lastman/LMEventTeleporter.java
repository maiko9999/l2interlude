package net.sf.l2j.gameserver.model.entity.events.lastman;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.duels.DuelState;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;

public class LMEventTeleporter implements Runnable
{
	private Player _player;
	
	private int[] _coordinates = new int[3];
	
	private boolean _adminRemove = false;
	
	public LMEventTeleporter(Player player, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_player = player;
		_coordinates = coordinates;
		_adminRemove = adminRemove;
		
		loadTeleport(fastSchedule);
	}
	
	public LMEventTeleporter(Player player, boolean fastSchedule, boolean adminRemove)
	{
		_player = player;
		_coordinates = Config.LM_EVENT_PLAYER_COORDINATES.get(Rnd.get(Config.LM_EVENT_PLAYER_COORDINATES.size()));
		_adminRemove = adminRemove;
		
		loadTeleport(fastSchedule);
	}
	
	private void loadTeleport(boolean fastSchedule)
	{
		ThreadPool.schedule(this, fastSchedule ? 0 : (LMEvent.getInstance().isStarted() ? Config.LM_EVENT_RESPAWN_TELEPORT_DELAY : Config.LM_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000);
	}
	
	@Override
	public void run()
	{
		if (_player == null)
			return;
		
		Summon summon = _player.getSummon();
		
		if (summon != null)
			summon.unSummon(_player);
		
		if (Config.LM_EVENT_EFFECTS_REMOVAL == 0 || (Config.LM_EVENT_EFFECTS_REMOVAL == 1 && (_player.getTeam() == TeamType.NONE || (_player.isInDuel() && _player.getDuelState() != DuelState.INTERRUPTED))))
			_player.stopAllEffectsExceptThoseThatLastThroughDeath();
		
		if (_player.isInDuel())
			_player.setDuelState(DuelState.INTERRUPTED);
		
		_player.doRevive();
		
		_player.teleportTo(_coordinates[0] + Rnd.get(101) - 50, _coordinates[1] + Rnd.get(101) - 50, _coordinates[2], 0);
		
		if (LMEvent.getInstance().isStarted() && !_adminRemove)
			_player.setTeam(TeamType.RED);
		else
			_player.setTeam(TeamType.NONE);
		
		_player.getStatus().setCp(_player.getStatus().getMaxCp());
		_player.getStatus().setHp(_player.getStatus().getMaxHp());
		_player.getStatus().setMp(_player.getStatus().getMaxMp());
		
		_player.getStatus().broadcastStatusUpdate();
		_player.broadcastTitleInfo();
		_player.broadcastUserInfo();
	}
}