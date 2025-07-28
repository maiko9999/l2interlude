package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;

public class GeoZoneCheck
{
    public static boolean isCollisionFree(int x, int y)
    {
        for (Creature creature : World.getInstance().getAroundCharacters(x, y, 150, 200))
        {
            if (creature != null && creature.isInsideZone(ZoneId.COLISION))
            {
                return true;
            }
        }
        return false;
    }
}
