package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.records.NewbieItem;
import net.sf.l2j.gameserver.model.records.custom.Macros;
import net.sf.l2j.gameserver.skills.L2Skill;

import org.w3c.dom.Document;

/**
 * This class loads and stores {@link PlayerTemplate}s. It also feed their skill trees.
 */
public class PlayerData implements IXmlReader
{
	private final Map<Integer, PlayerTemplate> _templates = new HashMap<>();
	
	private final Map<Integer, List<GeneralSkillNode>> _skills = new HashMap<>();
	
	protected PlayerData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("xml/classes");
		LOGGER.info("Loaded {} player classes templates.", _templates.size());
		
		for (PlayerTemplate template : _templates.values())
			_skills.put(template.getClassId().getId(), List.copyOf(template.getSkills()));
		
		// We add parent skills, if existing.
		for (PlayerTemplate template : _templates.values())
		{
			final ClassId parentClassId = template.getClassId().getParent();
			if (parentClassId != null)
				template.getSkills().addAll(_templates.get(parentClassId.getId()).getSkills());
		}
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "class", classNode ->
		{
			final StatSet set = new StatSet();
			forEach(classNode, "set", setNode -> set.putAll(parseAttributes(setNode)));
			forEach(classNode, "macros", itemsNode ->
			{
				final List<Macros> items = new ArrayList<>();
				forEach(itemsNode, "macro", itemNode -> items.add(new Macros(parseAttributes(itemNode))));
				set.set("macros", items);
			});
			forEach(classNode, "items", itemsNode ->
			{
				final List<NewbieItem> items = new ArrayList<>();
				forEach(itemsNode, "item", itemNode -> items.add(new NewbieItem(parseAttributes(itemNode))));
				set.set("items", items);
			});
			forEach(classNode, "skills", skillsNode ->
			{
				final List<GeneralSkillNode> skills = new ArrayList<>();
				forEach(skillsNode, "skill", skillNode -> skills.add(new GeneralSkillNode(parseAttributes(skillNode))));
				set.set("skills", skills);
			});
			forEach(classNode, "spawns", spawnsNode ->
			{
				final List<Location> locs = new ArrayList<>();
				forEach(spawnsNode, "spawn", spawnNode -> locs.add(new Location(parseAttributes(spawnNode))));
				set.set("spawnLocations", locs);
			});
			_templates.put(set.getInteger("id"), new PlayerTemplate(set));
		}));
	}
	
	public PlayerTemplate getTemplate(ClassId classId)
	{
		return _templates.get(classId.getId());
	}
	
	public PlayerTemplate getTemplate(int classId)
	{
		return _templates.get(classId);
	}
	
	public final String getClassNameById(int classId)
	{
		final PlayerTemplate template = _templates.get(classId);
		return (template != null) ? template.getClassName() : "Invalid class";
	}
	
	public List<L2Skill> getSkillsByClassId(int id)
	{
		final List<GeneralSkillNode> template = _skills.get(id);
		if (template == null)
			return List.of();
		
		return template.stream().map(node -> SkillTable.getInstance().getInfo(node.getId(), node.getValue())).filter(skill -> skill != null).toList();
	}
	
	public static PlayerData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerData INSTANCE = new PlayerData();
	}
}