package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.data.xml.RestartPointData;
import net.sf.l2j.gameserver.enums.RestartType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.L2Skill;

public class L2SkillTeleport extends L2Skill
{
	private final String _recallType;
	private final Location _loc;
	
	public L2SkillTeleport(StatSet set)
	{
		super(set);
		
		_recallType = set.getString("recallType", "");
		_loc = set.getLocation("teleCoords", null);
	}
	
	@Override
	public void useSkill(Creature creature, WorldObject[] targets)
	{
		// Check invalid Player states.
		if (creature instanceof Player player && (player.isAfraid() || player.isInOlympiadMode()))
			return;
		
		boolean bsps = creature.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
		
		for (WorldObject target : targets)
		{
			if (!(target instanceof Player targetPlayer))
				continue;
			
			// Check invalid states.
			if (targetPlayer.isFestivalParticipant() || targetPlayer.isInJail() || targetPlayer.isInDuel() || targetPlayer.isFlying())
				continue;
			
			if (targetPlayer != creature)
			{
				if (targetPlayer.isInOlympiadMode())
					continue;
				
				if (targetPlayer.isInsideZone(ZoneId.BOSS))
					continue;
			}
			
			// teleCoords are prioritized over recallType, if existing.
			Location loc = _loc;
			
			// If teleCoords aren't existing, we calculate the regular way using recallType.
			if (loc == null)
			{
				if (_recallType.equalsIgnoreCase("Castle"))
					loc = RestartPointData.getInstance().getLocationToTeleport(targetPlayer, RestartType.CASTLE);
				else if (_recallType.equalsIgnoreCase("ClanHall"))
					loc = RestartPointData.getInstance().getLocationToTeleport(targetPlayer, RestartType.CLAN_HALL);
				else
					loc = RestartPointData.getInstance().getLocationToTeleport(targetPlayer, RestartType.TOWN);
			}
			
			if (loc != null)
			{
				targetPlayer.setIsIn7sDungeon(false);
				targetPlayer.teleportTo(loc, 20);
			}
		}
		
		creature.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
	}
}