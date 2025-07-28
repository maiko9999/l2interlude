package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * This class manages all attackable festival NPCs, spawned during the Festival of Darkness.
 */
public class FestivalMonster extends Monster
{
	private Set<Npc> _minions;
	
	public FestivalMonster(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean isAggressive()
	{
		return true;
	}
	
	@Override
	public Set<Npc> getMinions()
	{
		if (_minions == null)
			_minions = ConcurrentHashMap.newKeySet();
		
		return _minions;
	}
}