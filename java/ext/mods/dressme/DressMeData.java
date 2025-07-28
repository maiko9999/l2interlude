package ext.mods.dressme;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;

import ext.mods.dressme.holder.DressMeEffectHolder;
import ext.mods.dressme.holder.DressMeHolder;

public class DressMeData implements IXmlReader
{
	private final List<DressMeHolder> _entries = new ArrayList<>();
	
	public DressMeData()
	{
		load();
	}
	
	public void reload()
	{
		_entries.clear();
		load();
	}
	
	@Override
	public void load()
	{
		
		parseDataFile("custom/mods/DressMeData.xml");
		LOGGER.info("Loaded {" + _entries.size() + "} DressMe entries.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "dressMeList", listNode -> forEach(listNode, "dress", dressNode ->
		{
			final StatSet attrs = parseAttributes(dressNode);
			
			final DressMeHolder holder = new DressMeHolder(attrs);
			
			forEach(dressNode, "visualSet", setNode -> holder.setVisualSet(parseAttributes(setNode)));
			forEach(dressNode, "visualWep", wepNode -> holder.setWeaponSet(parseAttributes(wepNode)));
			forEach(dressNode, "visualEffect", fxNode -> holder.setEffect(new DressMeEffectHolder(parseAttributes(fxNode))));
			
			_entries.add(holder);
		}));
	}
	
	public List<DressMeHolder> getEntries()
	{
		return _entries;
	}
	
	public DressMeHolder getBySkillId(int skillId)
	{
		return _entries.stream().filter(d -> d.getSkillId() == skillId).findFirst().orElse(null);
	}
	
	public static DressMeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DressMeData INSTANCE = new DressMeData();
	}
}