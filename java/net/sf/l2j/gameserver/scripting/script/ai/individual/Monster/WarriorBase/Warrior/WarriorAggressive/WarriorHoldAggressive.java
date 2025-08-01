package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorAggressive;

import net.sf.l2j.gameserver.model.actor.Npc;

public class WarriorHoldAggressive extends WarriorAggressive
{
	public WarriorHoldAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorAggressive");
	}
	
	public WarriorHoldAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21661,
		21684,
		21707,
		21730,
		21753,
		21776
	};
	
	@Override
	public void onNoDesire(Npc npc)
	{
		if (npc.distance2D(npc.getSpawnLocation()) > 0)
			npc.getAI().addMoveToDesire(npc.getSpawnLocation(), 30);
	}
}