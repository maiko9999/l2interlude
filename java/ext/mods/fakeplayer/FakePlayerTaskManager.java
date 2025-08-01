package ext.mods.fakeplayer;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.pool.ThreadPool;

import ext.mods.fakeplayer.task.AITask;
import ext.mods.fakeplayer.task.AITaskRunner;

public class FakePlayerTaskManager
{
	private final int aiTaskRunnerInterval = 700;
	private final int _playerCountPerTask = 2000;
	private List<AITask> _aiTasks;
	
	public void initialise()
	{
		ThreadPool.scheduleAtFixedRate(new AITaskRunner(), aiTaskRunnerInterval, aiTaskRunnerInterval);
		_aiTasks = new ArrayList<>();
	}
	
	public void adjustTaskSize()
	{
		int fakePlayerCount = FakePlayerManager.getInstance().getFakePlayersCount();
		int tasksNeeded = calculateTasksNeeded(fakePlayerCount);
		_aiTasks.clear();
		
		for (int i = 0; i < tasksNeeded; i++)
		{
			int from = i * _playerCountPerTask;
			int to = (i + 1) * _playerCountPerTask;
			_aiTasks.add(new AITask(from, to));
		}
	}
	
	private int calculateTasksNeeded(int fakePlayerCount)
	{
		return fakePlayerCount == 0 ? 0 : fakePlayerCount > 0 && fakePlayerCount < _playerCountPerTask ? 1 : (fakePlayerCount + _playerCountPerTask) / _playerCountPerTask;
	}
	
	public int getPlayerCountPerTask()
	{
		return _playerCountPerTask;
	}
	
	public int getTaskCount()
	{
		return _aiTasks.size();
	}
	
	public List<AITask> getAITasks()
	{
		return _aiTasks;
	}
	
	public static FakePlayerTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		private static final FakePlayerTaskManager _instance = new FakePlayerTaskManager();
	}
}
