package net.sf.l2j.gameserver.model.entity.autofarm.zone;

import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager.AutoFarmType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

public class AutoFarmZone extends AutoFarmArea
{
	private boolean _isBuilt;
	
	public AutoFarmZone(int zoneId, String name, int ownerId)
	{
		super(zoneId, name, ownerId, AutoFarmType.ZONA);
	}
	
	public AutoFarmZone(String name, int ownerId)
	{
		super(name, ownerId, AutoFarmType.ZONA);
	}

	@Override
	public void onEnter(Creature character)
	{
		if (character.getActingPlayer() != null)
		{
			// Only monsters and the player that created them.
			if (character.getObjectId() != getOwnerId())
				return;
			
			if (!getProfile().isEnabled())
				character.sendMessage("You have entered an AutoFarm zone.");
		}
		else
			getMonsterHistory().add(character.getName());
		
		character.setInsideZone(ZoneId.AUTO_FARM, true);
	}

	@Override
	public void onExit(Creature character)
	{
		if (character.getActingPlayer() != null && !getProfile().isEnabled())
			character.sendMessage("You have exited the AutoFarm zone.");
		
		character.setInsideZone(ZoneId.AUTO_FARM, false);
	}
	
	@Override
	public void visualizeZone(ExServerPrimitive debug)
	{
		getZone().visualizeZone("ZONA " + getName(), debug);
	}
	
	@Override
	public AutoFarmZone getFarmZone()
	{
		return this;
	}
	
	public boolean isBuilt()
	{
		return _isBuilt;
	}
	
	@Override
	public List<Monster> getMonsters()
	{
		return getKnownTypeInside(Monster.class);
	}
	
	public Location findValidLocation()
	{
		Location center = getZoneZ().findPointInCenter();
		int attempt = 0;
		int radius = 100; // Initial distance to move away.

		while (attempt < 10)
		{
			// Maximum of 10 attempts.
			if (getOwner().getMove().maybeMoveToLocation(center, 0, true, true))
			{
				return center; // Found a valid location.
			}

			// Increase the distance from the center and try again.
			double angle = Math.random() * 2 * Math.PI; // Generate a random angle.
			int newX = (int) (center.getX() + radius * Math.cos(angle));
			int newY = (int) (center.getY() + radius * Math.sin(angle));

			// Ensure that the new point is still within the Z limits.
			int newZ = center.getZ();

			center = new Location(newX, newY, newZ);
			radius += 100; // Increase the radius for the next attempt.
			attempt++;
		}

		return null; // If a valid location is not found after 10 attempts.
	}
	
	public boolean tryGoBackInside()
	{
		final Location way = getWayIn();
		if (way != null)
		{
			getOwner().getAI().tryToMoveTo(way, null);
			return true;
		}
		
		return false;
	}
	
	private Location getWayIn()
	{
		// While the player is on the way to the center or the monster, they will enter the zone.
		final Player owner = getOwner();
		final Location center = getZoneZ().findPointInCenter();
		
		if (GeoEngine.getInstance().canMoveToTarget(owner, center))
			return center;
		
		final List<Monster> monsters = getMonsters().stream().filter(m -> GeoEngine.getInstance().canMoveToTarget(owner, m)).toList();
		if (!monsters.isEmpty())
			return Rnd.get(monsters).getPosition().clone();
		
		// Random location within the zone.
		for (int i = 0; i < 10; i++)
		{
			final Location loc = getZoneZ().getRandomPoint();
			if (loc != null && GeoEngine.getInstance().canMoveToTarget(owner, loc))
				return loc;
		}
		
		// The player will have to move manually.
		return null;
	}
	
	public void addToWorld()
	{
		if (_isBuilt)
			return;

		updateWorldRegions();
		_isBuilt = true;
	}
	
	public void removeFromWorld()
	{
		if (!_isBuilt)
			return;

		getCreatures().forEach(c -> removeCreature(c));
		updateWorldRegions();
		_isBuilt = false;
	}
	
	private void updateWorldRegions()
	{
		final WorldRegion[][] regions = World.getInstance().getWorldRegions();
		for (int x = 0; x < regions.length; x++)
		{
			final int xLoc = World.getRegionX(x);
			final int xLoc2 = World.getRegionX(x + 1);
			for (int y = 0; y < regions[x].length; y++)
			{
				if (getZone().intersectsRectangle(xLoc, xLoc2, World.getRegionY(y), World.getRegionY(y + 1)))
				{
					if (_isBuilt)
						regions[x][y].removeZone(this);
					else
					{
						// From ZoneManager revalidated creatures in the areas.
						for (WorldObject object : regions[x][y].getObjects())
						{
							if (object instanceof Creature)
								revalidateInZone(((Creature) object));
						}
						
						regions[x][y].addZone(this);
					}
				}
			}
		}
	}
}