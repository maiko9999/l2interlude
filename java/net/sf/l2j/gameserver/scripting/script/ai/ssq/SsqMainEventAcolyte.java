package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import java.util.List;

import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.PeriodType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class SsqMainEventAcolyte extends DefaultNpc
{
	public static final int FESTIVAL_COUNT = 5;
	
	public SsqMainEventAcolyte()
	{
		super("ai/ssq");
	}
	
	public SsqMainEventAcolyte(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		31127,
		31128,
		31129,
		31130,
		31131,
		31137,
		31138,
		31139,
		31140,
		31141
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 0;
		npc.getSpawn().instantTeleportInMyTerritory(new Location(getNpcIntAIParam(npc, "escape_x"), getNpcIntAIParam(npc, "escape_y"), getNpcIntAIParam(npc, "coord_z")), 100);
		startQuestTimer("3001", npc, null, 1000);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		var party0 = creature.getParty();
		if (party0 != null)
		{
			for (Player partyMember : party0.getMembers())
			{
				if (partyMember.isIn3DRadius(npc, 1000))
					partyMember.teleportTo(getNpcIntAIParam(npc, "SibylPosX"), getNpcIntAIParam(npc, "SibylPosY"), getNpcIntAIParam(npc, "SibylPosZ"), 0);
			}
		}
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3001"))
		{
			if (SevenSignsManager.getInstance().getCurrentPeriod() == PeriodType.COMPETITION)
			{
				int i0 = getMin();
				int i1 = FestivalOfDarknessManager.getInstance().getTimeOfSSQ() - GameTimeTaskManager.getInstance().getCurrentTick();
				
				CabalType cabalType = null;
				if (getNpcIntAIParam(npc, "part_type") == 1)
					cabalType = CabalType.DUSK;
				else if (getNpcIntAIParam(npc, "part_type") == 2)
					cabalType = CabalType.DAWN;
				
				if ((i0 == 58 || i0 == 18 || i0 == 38) && i1 >= 18 * 60) // Announces that the main event will begin.
				{
					if (npc._i_ai0 < 1)
					{
						// Clear past participants, they can no longer register their score if not done so already.
						List<?> participants = FestivalOfDarknessManager.getInstance().getPreviousParticipants(cabalType, getNpcIntAIParam(npc, "RoomIndex"));
						if (participants != null)
							participants.clear();
						
						if (getNpcIntAIParam(npc, "ShoutSysMsg") == 1)
							npc.broadcastNpcShout(NpcStringId.getNpcMessage(1000317));
						
						npc._i_ai0++;
					}
				}
				else if ((i0 == 0 || i0 == 20 || i0 == 40) && i1 >= 18 * 60) // Notifies that a new game is about to start.
				{
					if (npc._i_ai0 < 1)
					{
						if (getNpcIntAIParam(npc, "ShoutSysMsg") == 1)
							npc.broadcastNpcShout(NpcStringId.getNpcMessage(1000318));
						
						npc._i_ai0++;
						
						// Spawn NPCs at the event site.
						createOnePrivateEx(npc, getNpcIntAIParam(npc, "SibylSilhouette"), getNpcIntAIParam(npc, "SibylPosX"), getNpcIntAIParam(npc, "SibylPosY"), getNpcIntAIParam(npc, "SibylPosZ"), 0, 0, false);
					}
				}
				else if (i0 == 13 || i0 == 33 || i0 == 53) // It informs you that the game will end in 5 minutes.
				{
					if (npc._i_ai0 < 1)
					{
						if (getNpcIntAIParam(npc, "ShoutSysMsg") == 1)
							npc.broadcastNpcShout(NpcStringId.getNpcMessage(1000319));
						
						npc._i_ai0++;
					}
				}
				else if (i0 == 16 || i0 == 36 || i0 == 56) // It informs you that the game will end in 2 minutes.
				{
					if (npc._i_ai0 < 1 && getNpcIntAIParam(npc, "ShoutSysMsg") == 1)
					{
						if (i1 >= 20 * 60)
							npc.broadcastNpcShout(NpcStringId.getNpcMessage(1000320));
						else
							npc.broadcastNpcShout(NpcStringId.getNpcMessage(1000453));
						
						npc._i_ai0++;
					}
				}
				else
					npc._i_ai0 = 0;
			}
			startQuestTimer("3001", npc, null, 7000);
		}
		return null;
	}
	
	@Override
	public void onDecayed(Npc npc)
	{
		cancelQuestTimers("3001", npc);
		
		super.onDecayed(npc);
	}
}