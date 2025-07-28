package net.sf.l2j.gameserver.model.entity.autofarm;

import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager.AutoFarmType;
import net.sf.l2j.gameserver.model.entity.autofarm.zone.AutoFarmArea;

public class AutoFarmTask implements Runnable
{
	private int _runTick;
	
	public AutoFarmTask()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		_runTick++;
		
		// Routine
		AutoFarmManager.getInstance().getPlayers().parallelStream().filter(AutoFarmProfile::isEnabled).forEach(AutoFarmProfile::startRoutine);
		
		// Remove unused zones from the world
		if (_runTick >= 60)
		{
			for (AutoFarmProfile autoFarmProfile : AutoFarmManager.getInstance().getPlayers())
			{
				if (autoFarmProfile.isEnabled())
					continue;
				
				if (System.currentTimeMillis() > autoFarmProfile.getLastActiveTime() + TimeUnit.MINUTES.toMillis(10))
				{
					final List<AutoFarmArea> areas = autoFarmProfile.getAreas().values().stream().filter(a -> a.getId() != autoFarmProfile.getSelectedAreaId() && a.isFromDb() && a.getType() == AutoFarmType.ZONA && a.getFarmZone().isBuilt()).toList();
					areas.forEach(a -> a.getFarmZone().removeFromWorld());
				}
			}
			
			_runTick = 0;
		}
	}
	
	public static final AutoFarmTask getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final AutoFarmTask INSTANCE = new AutoFarmTask();
	}
}