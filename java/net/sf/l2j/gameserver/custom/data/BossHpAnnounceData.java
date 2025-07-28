package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class BossHpAnnounceData implements IXmlReader
{
	
	private boolean _enabled = true;
	
	public static class HpThreshold
	{
		public final int percent;
		public final String message;
		
		public HpThreshold(int percent, String message)
		{
			this.percent = percent;
			this.message = message;
		}
	}
	
	private final Map<Integer, List<HpThreshold>> _thresholds = new ConcurrentHashMap<>();
	
	protected BossHpAnnounceData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_thresholds.clear();
		parseDataFile("custom/mods/bossHpAnnounce.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _thresholds.size() + " boss HP announce configs.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			Node enabledNode = listNode.getAttributes().getNamedItem("enabled");
			if (enabledNode != null)
				_enabled = Boolean.parseBoolean(enabledNode.getNodeValue());
			
			forEach(listNode, "boss", bossNode ->
			{
				final StatSet set = new StatSet(parseAttributes(bossNode));
				final int npcId = set.getInteger("npcId");
				final List<HpThreshold> list = new ArrayList<>();
				
				forEach(bossNode, "hp", hpNode ->
				{
					final StatSet hpSet = new StatSet(parseAttributes(hpNode));
					final int percent = hpSet.getInteger("percent");
					final String message = hpSet.getString("message", "%boss% reached %hp%% of life!");
					
					if (percent > 0 && percent <= 100)
						list.add(new HpThreshold(percent, message));
				});
				
				list.sort((a, b) -> Integer.compare(b.percent, a.percent)); // decrescente
				_thresholds.put(npcId, list);
			});
		});
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public boolean isAnnounceEnabledFor(int npcId)
	{
		return _enabled && _thresholds.containsKey(npcId);
	}
	
	public List<HpThreshold> getThresholds(int npcId)
	{
		return _thresholds.getOrDefault(npcId, Collections.emptyList());
	}
	
	public static BossHpAnnounceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final BossHpAnnounceData INSTANCE = new BossHpAnnounceData();
	}
}
