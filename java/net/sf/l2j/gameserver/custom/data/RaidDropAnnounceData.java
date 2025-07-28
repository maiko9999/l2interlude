package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.model.item.kind.Item;

import org.w3c.dom.Document;

public class RaidDropAnnounceData implements IXmlReader
{
	private boolean enabled = false;
	private final Map<CrystalType, StatSet> gradeMap = new EnumMap<>(CrystalType.class);
	private final Map<CrystalType, Set<Integer>> gradeItemIdMap = new EnumMap<>(CrystalType.class);
	
	protected RaidDropAnnounceData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/raid_drop_announce.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded raid drop announce configuration.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			enabled = Boolean.parseBoolean(listNode.getAttributes().getNamedItem("enabled").getNodeValue());
			
			forEach(listNode, "grade", node ->
			{
				StatSet set = new StatSet(parseAttributes(node));
				try
				{
					CrystalType grade = CrystalType.valueOf(set.getString("name"));
					gradeMap.put(grade, set);
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.warn(getClass().getSimpleName() + ": Invalid crystal grade '" + set.getString("name") + "' in " + path.getFileName());
				}
				
				String itemIdsStr = set.getString("itemIds", "");
				if (!itemIdsStr.isEmpty())
				{
					Set<Integer> itemIds = new HashSet<>();
					Arrays.stream(itemIdsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).mapToInt(Integer::parseInt).forEach(itemIds::add);
					gradeItemIdMap.put(CrystalType.valueOf(set.getString("name")), itemIds);
				}
				
			});
			
		});
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public boolean shouldAnnounce(Item item)
	{
		if (item == null)
			return false;
		
		CrystalType grade = item.getCrystalType();
		StatSet set = gradeMap.get(grade);
		if (set == null || !set.getBool("announce", false))
			return false;
		
		Set<Integer> itemIds = gradeItemIdMap.get(grade);
		if (itemIds == null || itemIds.isEmpty())
			return true;
			
		return itemIds.contains(item.getItemId());
	}
	
	public String getMessageTemplate(Item item)
	{
		if (item == null)
			return "";

		CrystalType grade = item.getCrystalType();
		StatSet set = gradeMap.get(grade);
		return (set != null) ? set.getString("message", "") : "";
	}

	
	public static RaidDropAnnounceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidDropAnnounceData INSTANCE = new RaidDropAnnounceData();
	}
}