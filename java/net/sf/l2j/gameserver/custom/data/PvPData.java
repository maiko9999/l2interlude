package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

import org.w3c.dom.Document;

public class PvPData implements IXmlReader
{
	private final List<ColorSystem> _color = new ArrayList<>();
	private final List<RewardSystem> _reward = new ArrayList<>();
	private boolean _enabled;
	
	public PvPData()
	{
		load();
	}
	
	public void reload()
	{
		_color.clear();
		_reward.clear();
		load();
	}
	
	@Override
	public void load()
	{
		if (_enabled)
		{
			parseDataFile("custom/mods/pvpSystem.xml");
			LOGGER.info("Loaded {} PvP Colors templates.", _color.size());
			LOGGER.info("Loaded {} PvP Rewards templates.", _reward.size());
		}
		else
			LOGGER.warn("PvP System is disabled.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			final StatSet set = parseAttributes(listNode);
			_enabled = set.getBool("enabled", true);
			
			if (_enabled)
			{
				forEach(listNode, "color", colorNode ->
				{
					final StatSet colorSet = parseAttributes(colorNode);
					_color.add(new ColorSystem(colorSet));
				});
				
				forEach(listNode, "reward", rewardNode ->
				{
					final StatSet rewardSet = parseAttributes(rewardNode);
					_reward.add(new RewardSystem(rewardSet));
				});
			}
		});
	}
	
	public List<ColorSystem> getColor()
	{
		return _enabled ? _color : Collections.emptyList();
	}
	
	public List<RewardSystem> getReward()
	{
		return _enabled ? _reward : Collections.emptyList();
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public record RewardSystem(List<IntIntHolder> reward)
	{
		public RewardSystem(StatSet set)
		{
			this(set.getIntIntHolderList("rewards"));
		}
	}
	
	public record ColorSystem(int pvpAmount, int nameColor, int titleColor)
	{
		public ColorSystem(StatSet set)
		{
			this(set.getInteger("pvpAmount"), Integer.decode("0x" + set.getString("nameColor", "FFFFFF")), Integer.decode("0x" + set.getString("titleColor", "FFFF77")));
		}
	}
	
	public static PvPData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PvPData _instance = new PvPData();
	}
}