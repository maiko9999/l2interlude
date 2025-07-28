package ext.mods.dungeon;

import java.util.List;
import java.util.Map;

import ext.mods.dungeon.enums.DungeonType;
import ext.mods.dungeon.holder.SpawnTemplate;
import ext.mods.dungeon.holder.StageTemplate;

public class DungeonTemplate
{
	public final int id;
	public final String name;

	public final DungeonType type;

	public final boolean sharedInstance;
	public final long cooldown;
	public final List<StageTemplate> stages;
	public final Map<Integer, List<SpawnTemplate>> spawns;
	
	public DungeonTemplate(int id, String name, DungeonType type, boolean sharedInstance, long cooldown, List<StageTemplate> stages, Map<Integer, List<SpawnTemplate>> spawns)
	{
		this.id = id;
		this.name = name;
		this.type = type;
		this.sharedInstance = sharedInstance;
		this.cooldown = cooldown;
		this.stages = stages;
		this.spawns = spawns;
	}
}
