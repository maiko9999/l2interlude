package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import org.w3c.dom.Document;

public class PolymorphData implements IXmlReader
{
	private final Map<Integer, Polymorph> _fakePcs = new HashMap<>();
	
	public PolymorphData()
	{
		load();
	}
	
	public void reload()
	{
		_fakePcs.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/polymorph.xml");
		LOGGER.info("Loaded {} polymorph templates.", _fakePcs.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "npc", node ->
		{
			final StatSet set = parseAttributes(node);
			forEach(node, "appearance", setNode -> set.putAll(parseAttributes(setNode)));
			forEach(node, "items", setNode -> set.putAll(parseAttributes(setNode)));
			forEach(node, "clan", setNode -> set.putAll(parseAttributes(setNode)));
			_fakePcs.put(set.getInteger("id"), new Polymorph(set));
		}));
	}
	
	public record Polymorph(String name, String title, int nameColor, int titleColor, double radius, double height, int race, int sex, int classId, int hairStyle, int hairColor, int face, byte hero, int enchant, int rightHand, int leftHand, int chest, int legs, int gloves, int feet, int hair, int hair2, int clanId, int clanCrest, int allyId, int allyCrest, int pledge)
	{
		public Polymorph(StatSet set)
		{
			this(set.getString("name", null), set.getString("title", null), Integer.decode("0x" + set.getString("nameColor", "FFFFFF")), Integer.decode("0x" + set.getString("titleColor", "FFFF77")), set.getDouble("radius", 0), set.getDouble("height", 0), set.getInteger("race", 0), set.getInteger("sex", 0), set.getInteger("classId", 0), set.getInteger("hairStyle", 0), set.getInteger("hairColor", 0), set.getInteger("face", 0), set.getByte("hero", (byte) -1), set.getInteger("enchant", 0), set.getInteger("rightHand", 0), set.getInteger("leftHand", 0), set.getInteger("chest", 0), set.getInteger("legs", 0), set.getInteger("gloves", 0), set.getInteger("feet", 0), set.getInteger("hair", 0), set.getInteger("hair2", 0), set.getInteger("clanId", 0), set.getInteger("clanCrest", 0), set.getInteger("allyId", 0), set.getInteger("allyCrest", 0), set.getInteger("pledge", 0));
		}
	}
	
	public Polymorph getFakePc(int npcId)
	{
		return _fakePcs.get(npcId);
	}
	
	public static PolymorphData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PolymorphData INSTANCE = new PolymorphData();
	}
}