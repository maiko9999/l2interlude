package net.sf.l2j.gameserver.model.manor;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.geometry.Territory;

/**
 * This class represents the manor area in a world.<br>
 * <br>
 * Manor area consists of a polygon defines by nodes.<br>
 * Manor area has a particular castle assigned to it.
 */
public class ManorArea extends Territory
{
	private final int _castleId;
	
	public ManorArea(StatSet set)
	{
		super(set);
		
		_castleId = set.getInteger("castleId");
	}
	
	/**
	 * @return The manor area castle id.
	 */
	public final int getCastleId()
	{
		return _castleId;
	}
}