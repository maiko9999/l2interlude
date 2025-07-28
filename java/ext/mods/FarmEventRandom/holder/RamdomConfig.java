package ext.mods.FarmEventRandom.holder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.StatSet;

public class RamdomConfig
{
	private final boolean _enabled;
	private final String _eventName;
	private final int _preparation;
	private final int _interval;
	private final int _zoneValue;
	private final Set<Integer> _activeDays;
	private final List<LocalTime> _activeTimes;
	
	public RamdomConfig(StatSet set)
	{
		_enabled = set.getBool("enable", false);
		_eventName = set.getString("name", "[Unnamed Event]");
		_preparation = set.getInteger("prepareMinutes", 5);
		_interval = set.getInteger("intervalHours", 6);
		_zoneValue = set.getInteger("select", 1);
		

		String days = set.getString("days", "0,1,2,3,4,5,6");
		_activeDays = Arrays.stream(days.split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toSet());
		
		String times = set.getString("times", "");
		_activeTimes = Arrays.stream(times.split(";")).map(String::trim).filter(s -> !s.isEmpty()).map(time -> LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))).collect(Collectors.toList());
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public String getName()
	{
		return _eventName;
	}
	
	public int getInterval()
	{
		return _interval;
	}
	
	public int getPrepareMinutes()
	{
		return _preparation;
	}
	
	public int getZoneValue()
	{
		return _zoneValue;
	}
	
	public Set<Integer> getActiveDays()
	{
		return _activeDays;
	}
	
	public List<LocalTime> getActiveTimes()
	{
		return _activeTimes;
	}
	
	public boolean isTimeToRun()
	{
		LocalDateTime now = LocalDateTime.now();
		int currentDay = now.getDayOfWeek().getValue() % 7;
		LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);
		
		if (!_activeDays.contains(currentDay))
			return false;
		
		return _activeTimes.stream().anyMatch(time -> time.equals(currentTime));
	}
}
