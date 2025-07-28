package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.model.actor.Npc;

public class RhamphoDecoPrimeval extends DefaultNpc
{
	public RhamphoDecoPrimeval()
	{
		super("ai/individual");
	}
	
	public RhamphoDecoPrimeval(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		32108
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		super.onCreated(npc);
		
		npc.setIsFlying(true);
		
		if (getNpcIntAIParam(npc, "FreewayID") > -1 && getNpcIntAIParam(npc, "mobile_type") == 1)
			npc.getAI().addMoveRouteDesire(getNpcStringAIParam(npc, "SuperPointName"), 2000);
		else
			LOGGER.error("super point name is null");
		
		npc.getMove().addMoveType(MoveType.FLY);
	}
}