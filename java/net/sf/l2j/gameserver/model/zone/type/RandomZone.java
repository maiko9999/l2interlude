package net.sf.l2j.gameserver.model.zone.type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.subtype.SpawnZoneType;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

public class RandomZone extends SpawnZoneType
{
	private int _id;
	private String _name;
	private boolean _active = false;
	private boolean _flagzone = false;
	private Set<Integer> _restrictedClasses = new HashSet<>();
	private Set<Integer> _restrictedItems = new HashSet<>();

	private Location _returnLocation = new Location(0, 0, 0);
	
	public RandomZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("id"))
			_id = Integer.parseInt(value);
		else if (name.equals("name"))
			_name = value;
		else if (name.equals("flagzone"))
			_flagzone = Boolean.parseBoolean(value);
		else if (name.equals("restrictedClasses"))
		{
			for (String className : value.split(","))
			{
				int classId = getClassIdFromName(className.trim());
				if (classId > 0)
					_restrictedClasses.add(classId);
			}
		}
		else if (name.equals("restrictedItems"))
		{
			for (String itemIdStr : value.split(","))
				_restrictedItems.add(Integer.parseInt(itemIdStr.trim()));
		}
		
		else if (name.equals("returnLocation"))
		{
			String[] coords = value.split(",");
			if (coords.length == 3)
				_returnLocation = new Location(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
		}
		
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (_active)
			character.setInsideZone(ZoneId.RANDOM, true);
		
		if (_flagzone)
		{
			if (character instanceof Player)
			{
				Player player = (Player) character;
				
				if (player.getPvpFlag() == 0)
				{
					player.updatePvPFlag(1);
				}
				PvpFlagTaskManager.getInstance().add(player, Config.PVP_NORMAL_TIME);
			}
		}
		
		if (character instanceof Player)
		{
			Player player = (Player) character;
			
			// 1. Restringir CLASSES
			if (_restrictedClasses.contains(player.getClassId().getId()))
			{
				player.sendMessage("Your class is restricted in this event zone.");
				player.teleToLocation(_returnLocation);
				return;
			}

			
			// 3. Remover ITENS proibidos
			for (int itemId : _restrictedItems)
			{
				if (player.getInventory().getItemByItemId(itemId) != null)
				{
					ItemInstance item = player.getInventory().getItemByItemId(itemId);
					
					player.getInventory().unequipItemInSlot(player.getInventory().getSlotFromItem(item));
					player.sendMessage("Some restricted items have been unequipped.");
				}
			}
		}
		
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (_active)
			character.setInsideZone(ZoneId.RANDOM, false);
		
		if (_flagzone)
		{
			if (character instanceof Player)
			{
				Player player = (Player) character;
				
				PvpFlagTaskManager.getInstance().add(player, Config.PVP_NORMAL_TIME);
			}
			
		}
	}
	
	public void setActive(boolean val)
	{
		_active = val;
		
		if (!val)
		{
			forEachCreatureInside(c -> c.setInsideZone(ZoneId.RANDOM, false));
		}
		
	}
	
	public void forEachCreatureInside(Consumer<Creature> action)
	{
		if (_creatures == null || _creatures.isEmpty())
			return;
		
		_creatures.stream().filter(c -> c != null && c.isInsideZone(ZoneId.RANDOM)).forEach(action);
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public boolean isFlegZone()
	{
		return _flagzone;
	}
	
	public Set<Integer> getRestrictedClasses()
	{
		return Collections.unmodifiableSet(_restrictedClasses);
	}
	
	public Set<Integer> getRestrictedItems()
	{
		return Collections.unmodifiableSet(_restrictedItems);
	}
	
	public Location getReturnLocation()
	{
		return _returnLocation;
	}
	
	private int getClassIdFromName(String name)
	{
		for (ClassId cid : ClassId.VALUES)
		{
			if (cid.name().equalsIgnoreCase(name))
				return cid.getId();
		}
		return -1;
	}
	
	@Override
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
}
