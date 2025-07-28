package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.enums.items.CrystalType;

import org.w3c.dom.Document;

public class EquipGradeRestrictionData implements IXmlReader
{
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private final Map<CrystalType, RestrictionInfo> restrictions = new EnumMap<>(CrystalType.class);
	
	protected EquipGradeRestrictionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		restrictions.clear();
		parseDataFile("custom/mods/equip_grade_restrictions.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + restrictions.size() + " grade restrictions.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "restrictions", listNode ->
		{
			forEach(listNode, "grade", node ->
			{
				StatSet set = new StatSet(parseAttributes(node));
				
				try
				{
					CrystalType grade = CrystalType.valueOf(set.getString("name"));
					boolean enabled = set.getBool("enabled", false);
					String dateStr = set.getString("availableFrom", null);
					LocalDateTime date = (dateStr != null) ? LocalDateTime.parse(dateStr, FORMATTER) : null;
					String message = set.getString("message", "");
					
					restrictions.put(grade, new RestrictionInfo(enabled, date, message));
					
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.warn(getClass().getSimpleName() + ": Invalid crystal grade '" + set.getString("name") + "' in " + path.getFileName());
				}
			});
		});
	}
	
	public boolean isEquipAllowed(CrystalType grade)
	{
		RestrictionInfo info = restrictions.get(grade);
		if (info == null || !info.enabled)
			return true;
		
		return info.availableFrom == null || LocalDateTime.now().isAfter(info.availableFrom);
	}
	
	public String getBlockMessage(CrystalType grade)
	{
		final RestrictionInfo info = restrictions.get(grade);
		if (info == null || !info.enabled || info.availableFrom == null)
			return "";
		
		final String raw = (info.message == null || info.message.isEmpty()) ? "You cannot yet equip items of the grade {grade}. Available at {date}." : info.message;
		
		return raw.replace("{grade}", grade.name()).replace("{date}", info.availableFrom.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
	}
	
	public static EquipGradeRestrictionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final EquipGradeRestrictionData INSTANCE = new EquipGradeRestrictionData();
	}
	
	private static class RestrictionInfo
	{
		final boolean enabled;
		final LocalDateTime availableFrom;
		final String message;
		
		public RestrictionInfo(boolean enabled, LocalDateTime availableFrom, String message)
		{
			this.enabled = enabled;
			this.availableFrom = availableFrom;
			this.message = message;
		}
	}
	
}