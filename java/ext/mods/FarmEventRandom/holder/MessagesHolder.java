package ext.mods.FarmEventRandom.holder;

import java.util.ArrayList;
import java.util.List;

public class MessagesHolder
{
	private final List<String> _onPrepare = new ArrayList<>();
	private final List<String> _onStart = new ArrayList<>();
	
	private final List<String> _onZone = new ArrayList<>();
	
	private final List<String> _onEnd = new ArrayList<>();
	private final List<String> _onAuto = new ArrayList<>();
	
	public void addOnPrepare(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onPrepare.add(msg);
	}
	
	public void addOnStart(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onStart.add(msg);
	}
	
	public void addOnEnd(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onEnd.add(msg);
	}
	
	public void addOnAuto(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onAuto.add(msg);
	}
	
	public void addOnZone(String msg)
	{
		if (msg != null && !msg.isEmpty())
			_onZone.add(msg);
	}
	
	public List<String> getOnPrepare()
	{
		return _onPrepare;
	}
	
	public List<String> getOnStart()
	{
		return _onStart;
	}
	
	public List<String> getOnEnd()
	{
		return _onEnd;
	}
	
	public List<String> getOnAuto()
	{
		return _onAuto;
	}
	
	public List<String> getOnZone()
	{
		return _onZone;
	}
}