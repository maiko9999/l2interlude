package ext.mods.InstanceMap;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;

public class MapInstance
{
	private int _id;
	private List<Door> _doors;
	
	public MapInstance(int id)
	{
		_id = id;
		_doors = new ArrayList<>();
	}
	
	public void open_doors()
	{
		for (Door door : _doors)
			door.openMe();
	}
	
	public void close_doors()
	{
		for (Door door : _doors)
			door.closeMe();
	}
	
	public void addDoor(Door door)
	{
		_doors.add(door);
	}
	
	public List<Door> get_doors()
	{
		return _doors;
	}
	
	public void checkProximityAndOpen_doors(List<Player> players)
	{
		if (_doors == null || players == null)
		{
			return;
		}
		for (Door door : _doors)
		{
			if (door == null || door.getInstanceMap() == null)
			{
				continue;
			}
			boolean shouldOpen = false;
			for (Player player : players)
			{
				if (player == null || player.getInstanceMap() == null)
				{
					continue;
				}
				if (door.getInstanceMap().getId() == player.getInstanceMap().getId() && door.isIn3DRadius(player, 400))
				{
					shouldOpen = true;
					break;
				}
			}
			if (shouldOpen && !door.isOpened())
			{
				door.openMe();
			}
		}
	}
	
	public void checkProximityAndClose_doors(List<Player> players)
	{
		if (_doors == null || players == null)
		{
			return;
		}
		for (Door door : _doors)
		{
			if (door == null || door.getInstanceMap() == null)
			{
				continue;
			}
			boolean shouldClose = true;
			for (Player player : players)
			{
				if (player == null || player.getInstanceMap() == null)
				{
					continue;
				}
				if (door.getInstanceMap().getId() == player.getInstanceMap().getId() && door.isIn3DRadius(player, 400))
				{
					shouldClose = false;
					break;
				}
			}
			if (shouldClose && door.isOpened())
			{
				door.closeMe();
			}
		}
	}
	
	public void checkDeleted(List<Player> players)
	{
		if (_doors == null || players == null)
		{
			return;
		}
		for (Door door : _doors)
		{
			if (door == null || door.getInstanceMap() == null)
			{
				continue;
			}
			boolean shouldClose = true;
			for (Player player : players)
			{
				if (player == null || player.getInstanceMap() == null)
				{
					continue;
				}
				if (door.getInstanceMap().getId() == player.getInstanceMap().getId())
				{
					shouldClose = false;
					break;
				}
			}
			if (shouldClose)
			{
				door.deleteMe();
			}
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
}