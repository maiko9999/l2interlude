package net.sf.l2j.gameserver.model.holder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceName
{
	private static final Map<Integer, String> _classNames = new HashMap<>();
	
	static
	{
		_classNames.put(88, "Duelist");
		_classNames.put(89, "Dreadnought");
		_classNames.put(90, "Phoenix Knight");
		_classNames.put(91, "Hell Knight");
		_classNames.put(92, "Sagittarius");
		_classNames.put(93, "Adventurer");
		_classNames.put(94, "Archmage");
		_classNames.put(95, "Soultaker");
		_classNames.put(96, "Arcana Lord");
		_classNames.put(97, "Cardinal");
		_classNames.put(98, "Hierophant");
		_classNames.put(99, "Eva Templar");
		_classNames.put(100, "Sword Muse");
		_classNames.put(101, "Wind Rider");
		_classNames.put(102, "Moonlight Sentinel");
		_classNames.put(103, "Mystic Muse");
		_classNames.put(104, "Elemental Master");
		_classNames.put(105, "Eva Saint");
		_classNames.put(106, "Shillien Templar");
		_classNames.put(107, "Spectral Dancer");
		_classNames.put(108, "Ghost Hunter");
		_classNames.put(109, "Ghost Sentinel");
		_classNames.put(110, "Storm Screamer");
		_classNames.put(111, "Spectral Master");
		_classNames.put(112, "Shillien Saint");
		_classNames.put(113, "Titan");
		_classNames.put(114, "Grand Khauatari");
		_classNames.put(115, "Dominator");
		_classNames.put(116, "Doomcryer");
		_classNames.put(117, "Fortune Seeker");
		_classNames.put(118, "Maestro");
	}
	
	public static String getName(int classId)
	{
		return _classNames.getOrDefault(classId, "Unknown (" + classId + ")");
	}
	
	public static List<Integer> getClassIdList()
	{
		return new ArrayList<>(_classNames.keySet());
	}
	
	public static Map<Integer, String> getClassMap()
	{
		return _classNames;
	}
}