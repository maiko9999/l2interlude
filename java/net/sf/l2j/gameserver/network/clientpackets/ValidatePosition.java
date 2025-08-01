package net.sf.l2j.gameserver.network.clientpackets;

import java.awt.Color;

import net.sf.l2j.gameserver.enums.TeleportMode;
import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.player.BoatInfo;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;

public class ValidatePosition extends L2GameClientPacket
{
	private int _x;
	private int _y;
	private int _z;
	@SuppressWarnings("unused")
	private int _heading;
	private int _boatId;
	
	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_boatId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null || player.isTeleporting() || player.isInObserverMode())
			return;
		
		// Disable validation for CameraMode.
		if (player.getTeleportMode() == TeleportMode.CAMERA_MODE)
		{
			// Retrieve the current WorldRegion passed by the client location, and set it. It allows knownlist to be properly refreshed.
			final WorldRegion region = World.getInstance().getRegion(_x, _y);
			if (region != null)
				player.setRegion(region);
			
			player.setXYZ(_x, _y, _z);
			return;
		}
		
		// Disable validation during fall to avoid "jumping".
		if (player.isFalling(_z))
			return;
		
		final SpawnLocation realPos = player.getPosition().clone();
		if (GeoEngine.getInstance().hasGeo(realPos.getX(), realPos.getY()) && (_z < realPos.getZ()) && (Math.abs(realPos.getZ() - _z) > 800))
		{
			player.sendPacket(new ValidateLocation(player));
			return;
		}
		
		float actualSpeed = 0;
		double dist = 0;
		
		final BoatInfo info = player.getBoatInfo();
		// Send back position if client<>server desync is too big. For boats, send back if the desync is bigger than 500.
		if (info.isInBoat())
		{
			final SpawnLocation pos = info.getBoatPosition();
			actualSpeed = 500;
			dist = pos.distance2D(_x, _y);
			
			if (dist > actualSpeed)
				sendPacket(new GetOnVehicle(player.getObjectId(), _boatId, pos));
		}
		// Swimming should almost never receive position from server, but in rare cases where desync is too high we should send it.
		// Send back position if client<>server desync is too big. For swimming, send back if the desync is bigger than speed * 4 or 250.
		else if (player.isInWater())
		{
			actualSpeed = player.getStatus().getMoveSpeed();
			dist = (player.getMove().getMoveType() == MoveType.GROUND) ? player.getPosition().distance2D(_x, _y) : player.getPosition().distance3D(_x, _y, _z);
			if (dist > Math.max(250, actualSpeed * 4))
				sendPacket(new ValidateLocation(player));
		}
		// For regular movement, send back if the desync is bigger than actual speed.
		else if (!player.temporaryFixPagan())
		{
			actualSpeed = player.getStatus().getMoveSpeed();
			dist = (player.getMove().getMoveType() == MoveType.GROUND) ? player.getPosition().distance2D(_x, _y) : player.getPosition().distance3D(_x, _y, _z);
			
			if (dist > actualSpeed && !info.isBoatMovement())
				sendPacket(new ValidateLocation(player));
		}
		
		// Draw a debug of this packet if activated.
		if (player.getMove().isDebugMove())
		{
			final String desc = "speed=" + actualSpeed + " desync=" + dist;
			
			// Draw debug packet to all players.
			player.forEachKnownGM(p ->
			{
				// Get debug packet.
				final ExServerPrimitive debug = p.getDebugPacket("MOVE" + player.getObjectId());
				debug.addPoint(desc, Color.GREEN, true, _x, _y, _z);
				debug.sendTo(p);
			});
		}
	}
}