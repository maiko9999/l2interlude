package net.sf.l2j.gameserver.model.entity.events.capturetheflag;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.duels.DuelState;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;

public class CTFEventTeleporter implements Runnable
{
	private Player _player = null;
	
	private int[] _coordinates = new int[3];
	
	private boolean _adminRemove = false;
	
	public CTFEventTeleporter(Player player, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_player = player;
		_coordinates = coordinates;
		_adminRemove = adminRemove;
		
		ThreadPool.schedule(this, fastSchedule ? 0 : (CTFEvent.getInstance().isStarted() ? Config.CTF_EVENT_RESPAWN_TELEPORT_DELAY : Config.CTF_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000);
	}
	
	@Override
	public void run()
	{
		if (_player == null)
			return;
		
		Summon summon = _player.getSummon();
		
		if (summon != null)
			summon.unSummon(_player);
		
		if ((Config.CTF_EVENT_EFFECTS_REMOVAL == 0) || ((Config.CTF_EVENT_EFFECTS_REMOVAL == 1) && ((_player.getTeam() == TeamType.NONE) || (_player.isInDuel() && (_player.getDuelState() != DuelState.INTERRUPTED)))))
			_player.stopAllEffectsExceptThoseThatLastThroughDeath();
		
		if (_player.isInDuel())
			_player.setDuelState(DuelState.INTERRUPTED);
		
		_player.doRevive();
		
		_player.teleportTo((_coordinates[0] + Rnd.get(101)) - 50, (_coordinates[1] + Rnd.get(101)) - 50, _coordinates[2], 0);
		
		if (CTFEvent.getInstance().playerIsCarrier(_player))
		{
			CTFEvent.getInstance().removeFlagCarrier(_player);
			CTFEvent.getInstance().sysMsgToAllParticipants("The " + CTFEvent.getInstance().getParticipantEnemyTeam(_player.getObjectId()).getName() + " flag has been returned!");
		}
		
		if (CTFEvent.getInstance().isStarted() && !_adminRemove)
		{
			int teamId = CTFEvent.getInstance().getParticipantTeamId(_player.getObjectId()) + 1;
			switch (teamId)
			{
				case 0:
					_player.setTeam(TeamType.NONE);
					break;
				case 1:
					_player.setTeam(TeamType.BLUE);
					break;
				case 2:
					_player.setTeam(TeamType.RED);
					break;
			}
		}
		else
			_player.setTeam(TeamType.NONE);
		
		_player.getStatus().setCp(_player.getStatus().getMaxCp());
		_player.getStatus().setHp(_player.getStatus().getMaxHp());
		_player.getStatus().setMp(_player.getStatus().getMaxMp());
		
		_player.getStatus().broadcastStatusUpdate();
		_player.broadcastUserInfo();
	}
}