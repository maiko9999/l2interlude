package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;

import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeleportMode;
import net.sf.l2j.gameserver.enums.boats.BoatDock;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Boat;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.player.BoatInfo;
import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocationInVehicle;

public class MoveBackwardToLocation extends L2GameClientPacket
{
	private static final Point2D CENTER_BOAT = new Point2D(0, -100);
	
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _originX;
	private int _originY;
	private int _originZ;
	private int _moveMovement;
	
	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		
		try
		{
			_moveMovement = readD(); // is 0 if cursor keys are used 1 if mouse is used
		}
		catch (BufferUnderflowException e)
		{
			if (Config.L2WALKER_PROTECTION)
			{
				final Player player = getClient().getPlayer();
				if (player != null)
					player.logout(false);
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final BoatInfo info = player.getBoatInfo();
		
		// If Player can't be controlled, forget it.
		if (player.isOutOfControl() || player.getCast().isCastingNow())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// If Player can't move, forget it.
		if (player.getStatus().getMoveSpeed() == 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.CANT_MOVE_TOO_ENCUMBERED);
			return;
		}
		
		// Cancel enchant over movement.
		player.cancelActiveEnchant();
		
		// Correct targetZ from floor level to head level.
		_targetZ += player.getCollisionHeight();
		
		// If under teleport mode, teleport instead of tryToMove.
		switch (player.getTeleportMode())
		{
			case ONE_TIME:
				player.setTeleportMode(TeleportMode.NONE);
			case FULL_TIME:
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.teleportTo(_targetX, _targetY, _targetZ, 0);
				return;
		}
		
		// Generate a Location based on target coords.
		final Location targetLoc = new Location(_targetX, _targetY, _targetZ);
		
		// If we target past 9900 distance, forget it.
		if (!targetLoc.isIn3DRadius(_originX, _originY, _originZ, 9900))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (AutoFarmManager.getInstance().isPlayerAddingLoc(player.getObjectId()))
		{
			AutoFarmManager.getInstance().addAreaNode(player, targetLoc);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (AutoFarmManager.getInstance().isRouteFarmActive(player.getObjectId()))
		{
			//player.sendMessage("Movimento desativado pelo AutoFarm."); // TODO
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final boolean isInBoat = info.isInBoat();
		
		// If out of Boat, register a move Intention.
		if (!isInBoat)
		{
			if (player.tryToPassBoatEntrance(targetLoc, _moveMovement == 0))
				return;
			
			info.setCanBoard(false);
			
			if (_moveMovement == 0)
			{
				int vectorX = _targetX - player.getX();
				int vectorY = _targetY - player.getY();
				
				// Calculate the magnitude of the vector
				double magnitude = Math.sqrt(vectorX * vectorX + vectorY * vectorY);
				
				// Normalize the vector (unit vector)
				double unitVectorX = vectorX / magnitude;
				double unitVectorY = vectorY / magnitude;
				
				// Scale the unit vector by the desired distance
				int newVectorX = (int) (unitVectorX * 16);
				int newVectorY = (int) (unitVectorY * 16);
				
				// Calculate the new location
				int newX = player.getX() + newVectorX;
				int newY = player.getY() + newVectorY;
				
				final Location checkLoc = new Location(newX, newY, player.getZ());
				
				if (!GeoEngine.getInstance().canMove(player.getX(), player.getY(), player.getZ(), checkLoc.getX(), checkLoc.getY(), checkLoc.getZ(), null))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (!player.getMove().maybeMoveToLocation(GeoEngine.getInstance().getValidLocation(player, targetLoc), 0, false, false))
					player.sendPacket(ActionFailed.STATIC_PACKET);
				
				return;
			}
			
			player.getAI().tryToMoveTo(targetLoc, null);
		}
		// Player is on the boat, we don't want to schedule a real movement until he gets out of it otherwise GeoEngine will be confused.
		else
		{
			// We want to set the real player heading though so it can be used during actual departure.
			player.getPosition().setHeading(MathUtil.calculateHeadingFrom(_originX, _originY, _targetX, _targetY));
			
			final Boat boat = info.getBoat();
			if (boat == null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final BoatDock dock = boat.getDock();
			if (dock == null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final boolean isMoving = boat.isMoving();
			
			final Point2D targetPoint = new Point2D(_targetX, _targetY);
			final Point2D originPoint = new Point2D(_originX, _originY);
			
			// Check if there is an intersection point with the boat entrance.
			Point2D boardingPoint = dock.getBoardingPoint(originPoint, targetPoint, isInBoat);
			
			// If not, check if there is an intersection point with the boat exit.
			if (boardingPoint == null)
				boardingPoint = BoatDock.getBoardingPoint(dock.getBoatExit(), originPoint, targetPoint, isInBoat);
			
			// No intersection point found, if the boat is docked do nothing.
			if (boardingPoint == null && !isMoving)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final Location pos = info.getBoatPosition();
			final int oX = pos.getX();
			final int oY = pos.getY();
			final int z = pos.getZ();
			
			info.setBoatMovement(true);
			
			final double distToBorder = isMoving ? 400 : originPoint.distance2D(boardingPoint);
			
			if (boardingPoint != null && distToBorder < 90)
			{
				// Just sending a client move packet so player will try to move towards exit.
				player.broadcastPacket(new MoveToLocation(player, new Location(boardingPoint.getX(), boardingPoint.getY(), -3624)));
				info.setBoatMovement(false);
				info.setCanBoard(false);
				return;
			}
			
			final SpawnLocation boatPos = info.getBoatPosition();
			final Point2D currentPoint = new Point2D(boatPos.getX(), boatPos.getY());
			
			final double distToCenter = CENTER_BOAT.distance2D(currentPoint);
			
			if (distToCenter > 350)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!isMoving && distToBorder > 200 && distToCenter > 250)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (isMoving && distToCenter < 250)
			{
				player.broadcastPacket(new MoveToLocation(player, new Location(_targetX, _targetY, _targetZ)));
				info.setBoatMovement(false);
				info.setCanBoard(false);
				return;
			}
			
			if (boardingPoint != null)
			{
				boardingPoint = dock.convertWorldToBoatCoordinates(boardingPoint.getX(), boardingPoint.getY());
				
				final int tX = boardingPoint.getX();
				final int tY = boardingPoint.getY();
				
				player.broadcastPacket(new MoveToLocationInVehicle(player, boat, tX, tY, z, oX, oY, z));
				info.setBoatMovement(false);
				info.setCanBoard(false);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		sendPacket(ActionFailed.STATIC_PACKET);
	}
}