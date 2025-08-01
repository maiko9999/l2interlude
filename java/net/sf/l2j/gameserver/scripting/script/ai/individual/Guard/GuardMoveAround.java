package net.sf.l2j.gameserver.scripting.script.ai.individual.Guard;

import net.sf.l2j.gameserver.model.actor.Npc;

public class GuardMoveAround extends Guard
{
	public GuardMoveAround()
	{
		super("ai/individual/Guard");
	}
	
	public GuardMoveAround(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		31844,
		31845,
		31846,
		31847,
		31848,
		31849,
		31850,
		31851,
		31853
	};
	
	@Override
	public void onNoDesire(Npc npc)
	{
		npc.getAI().addWanderDesire(5, 5);
	}
}