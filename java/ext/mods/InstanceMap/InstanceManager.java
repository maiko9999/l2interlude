package ext.mods.InstanceMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.model.actor.instance.Door;

public class InstanceManager
{
	private Map<Integer, MapInstance> instances;
	
	protected InstanceManager()
	{
		instances = new ConcurrentHashMap<>();
		instances.put(0, new MapInstance(0));
	}
	
	public void addDoor(int instanceid, Door door)
	{
		if (!instances.containsKey(instanceid) || instanceid == 0)
			return;
		
		instances.get(instanceid).addDoor(door);
	}
	
	public void addDoor(int instanceid, int doorid)
	{
		if (!instances.containsKey(instanceid) || instanceid == 0)
			return;
		
		Door data = DoorData.getInstance().getDoor(doorid);
		
		instances.get(instanceid).addDoor(data);
	}
	
	public void deleteInstance(int id)
	{
		if (id == 0)
		{
			System.out.println("Attempt to delete instance with id 0.");
			return;
		}
		
		instances.remove(id);
	}
	
	public synchronized MapInstance createInstance()
	{
		MapInstance instance = new MapInstance(InstanceIdFactory.getNextAvailable());
		instances.put(instance.getId(), instance);
		return instance;
	}
	
	public MapInstance getInstance(int id)
	{
		return instances.get(id);
	}
	
	public static InstanceManager getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static final class SingletonHolder
	{
		protected static final InstanceManager instance = new InstanceManager();
	}
}
