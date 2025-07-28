package net.sf.l2j.gameserver.scripting.script.ai.ssq;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager.L2DarknessFestival;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.TimeAttackEventRoom;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class SsqMainEventSibyl extends DefaultNpc
{
	public SsqMainEventSibyl()
	{
		super("ai/ssq");
		addTALKED(_npcIds);
		addMENU_SELECTED(_npcIds);
	}
	
	public SsqMainEventSibyl(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		31132,
		31133,
		31134,
		31135,
		31136,
		31142,
		31143,
		31144,
		31145,
		31146
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = GameTimeTaskManager.getInstance().getCurrentTick(); // Record the current time.
		npc._i_ai1 = 0; // When the first battle starts, value: 1, when the second battle starts, value: 2
		npc._i_ai3 = GameTimeTaskManager.getInstance().getCurrentTick(); // Variable that records the recent ?? that spawned additional monsters.
		
		npc._i_quest0 = 0; // Remember where the unit's archers are.
		npc._i_quest1 = 1; // Number of archers that can appear in a unit at one time.
		
		npc._i_quest2 = 0; // 4 fast monsters in the second battle (top, bottom).
		npc._i_quest3 = 0; // 4 fast monsters in the second battle (left, right).
		npc._i_quest4 = 1; // Maximum number of fast monsters that can currently appear.
		npc._i_ai2 = 0; // 4 slow monsters in the second battle.
		npc._i_ai4 = 1; // Maximum number of slow monsters that can currently appear.
		
		npc._param1 = 0; // Number of spawns due to additional monster requests.
		npc._flag = 0;
		
		startQuestTimer("3001", npc, null, 3000);
		
		npc.broadcastPacket(new PlaySound(1, "B06_S01", npc));
		startQuestTimer("3023", npc, null, 120 * 1000);
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3001"))
		{
			// Send a congratulatory message at 17 minutes and 50 seconds.
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0 > 60 * 17 + 45 && npc._i_ai1 == 2)
			{
				npc._i_ai1 = 3;
				npc.broadcastPacketInRadius(new NpcSay(npc, SayType.SHOUT, NpcStringId.ID_1000380.getMessage()), 1500);
			}
			
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0 > 60 * 10 && npc._i_ai1 == 1)
			{
				for (int i0 = 0; i0 < 10; i0++)
				{
					int i1 = Rnd.get(4);
					int i2 = npc.getX();
					int i3 = npc.getY();
					if (i1 == 0)
					{
						i2 = i2 + 200 + 50 - Rnd.get(100);
						i3 = i3 + 200 + 50 - Rnd.get(100);
					}
					else if (i1 == 1)
					{
						i2 = i2 + 200 + 50 - Rnd.get(100);
						i3 = i3 - 200 + 50 - Rnd.get(100);
					}
					else if (i1 == 2)
					{
						i2 = i2 - 200 + 50 - Rnd.get(100);
						i3 = i3 + 200 + 50 - Rnd.get(100);
					}
					else
					{
						i2 = i2 - 200 + 50 - Rnd.get(100);
						i3 = i3 - 200 + 50 - Rnd.get(100);
					}
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_present_name"), i2, i3, getNpcIntAIParam(npc, "coord_z"), 0, 0, false);
				}
				npc._i_ai1 = 2;
			}
			
			// It automatically disappears after 18 minutes.
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0 > 60 * 17 + 50)
			{
				npc.getMaster().getSpawn().instantTeleportInMyTerritory(getNpcIntAIParam(npc, "escape_x"), getNpcIntAIParam(npc, "escape_y"), getNpcIntAIParam(npc, "coord_z"), 100);
				
				FestivalOfDarknessManager.FestivalManager.getFestivalInstance().values().forEach(L2DarknessFestival::festivalEnd);
				
				TimeAttackEventRoom.getInstance().clear(getNpcIntAIParam(npc, "room_index"), getNpcIntAIParam(npc, "part_type"));
				npc.deleteMe();
			}
			
			// 1st battle.
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0 > 10 && npc._i_ai1 == 0)
			{
				npc._i_ai1 = 1;
				
				// Create primary monsters at once.
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_1x"), getNpcIntAIParam(npc, "battle1_1y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0001, npc._i_ai0, 0);
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_2x"), getNpcIntAIParam(npc, "battle1_2y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0010, npc._i_ai0, 0);
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_3x"), getNpcIntAIParam(npc, "battle1_3y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0100, npc._i_ai0, 0);
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_4x"), getNpcIntAIParam(npc, "battle1_4y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x1000, npc._i_ai0, 0);
			}
			
			// 2nd battle.
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0 > 60 * 5)
			{
				// Slow monster spawn
				int i0 = GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0;
				if (i0 > 15 * 60)
					npc._i_ai4 = 4;
				else if (i0 > 10 * 60)
					npc._i_ai4 = 3;
				else if (i0 > 8 * 60)
					npc._i_ai4 = 4;
				else if (i0 > 6 * 60)
					npc._i_ai4 = 2;
				
				i0 = 0;
				if ((npc._i_ai2 & 0x0001) != 0)
					i0 = i0 + 1;
				if ((npc._i_ai2 & 0x0010) != 0)
					i0 = i0 + 1;
				if ((npc._i_ai2 & 0x0100) != 0)
					i0 = i0 + 1;
				if ((npc._i_ai2 & 0x1000) != 0)
					i0 = i0 + 1;
				
				if (i0 < npc._i_ai4)
				{
					int i2 = Rnd.get(4) + 1;
					
					if (i2 == 1)
						i2 = 0x0001;
					else if (i2 == 2)
						i2 = 0x0010;
					else if (i2 == 3)
						i2 = 0x0100;
					else if (i2 == 4)
						i2 = 0x1000;
					
					i0 = 0;
					for (int i1 = 1; i1 < 5; i1++)
					{
						// Search for a location where monsters can be spawned.
						if (i0 == 0 && (i2 & npc._i_ai2) == 0)
						{
							i0 = i2;
							npc._i_ai2 = npc._i_ai2 + i0;
						}
						else
						{
							// Move the beat.
							if (i2 == 0x0001)
								i2 = 0x0010;
							else if (i2 == 0x0010)
								i2 = 0x0100;
							else if (i2 == 0x0100)
								i2 = 0x1000;
							else if (i2 == 0x1000)
								i2 = 0x0001;
						}
					}
					
					// Puncture.
					if (i2 == 0)
						npc.broadcastNpcSay("It is a puncture!!");
					
					int i1 = 3000 + (60 * 18 - (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0) * 10);
					
					if (i0 == 0x0001)
						startQuestTimer("3010", npc, null, i1);
					else if (i0 == 0x0010)
						startQuestTimer("3011", npc, null, i1);
					else if (i0 == 0x0100)
						startQuestTimer("3012", npc, null, i1);
					else if (i0 == 0x1000)
						startQuestTimer("3013", npc, null, i1);
					else
						npc.broadcastNpcSay("It is a puncture in the archer phone!!");
				}
				
				i0 = GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0;
				if (i0 > 16 * 60)
					npc._i_quest4 = 4;
				else if (i0 > 13 * 60)
					npc._i_quest4 = 3;
				else if (i0 > 10 * 60)
					npc._i_quest4 = 2;
				else if (i0 > 8 * 60)
					npc._i_quest4 = 3;
				else if (i0 > 6 * 60)
					npc._i_quest4 = 2;
				
				// Fast Monster 1 (up, down)
				i0 = 0;
				if ((npc._i_quest2 & 0x0001) != 0)
					i0 = i0 + 1;
				if ((npc._i_quest2 & 0x0010) != 0)
					i0 = i0 + 1;
				if ((npc._i_quest2 & 0x0100) != 0)
					i0 = i0 + 1;
				if ((npc._i_quest2 & 0x1000) != 0)
					i0 = i0 + 1;
				
				if (i0 < npc._i_quest4)
				{
					int i2 = Rnd.get(4) + 1;
					
					if (i2 == 1)
						i2 = 0x0001;
					else if (i2 == 2)
						i2 = 0x0010;
					else if (i2 == 3)
						i2 = 0x0100;
					else if (i2 == 4)
						i2 = 0x1000;
					
					i0 = 0;
					for (int i1 = 1; i1 < 5; i1++)
					{
						// Search for a location where monsters can be spawned.
						if (i0 == 0 && (i2 & npc._i_quest2) == 0)
						{
							i0 = i2;
							npc._i_quest2 = npc._i_quest2 + i0;
						}
						else
						{
							// move the beat
							if (i2 == 0x0001)
								i2 = 0x0010;
							else if (i2 == 0x0010)
								i2 = 0x0100;
							else if (i2 == 0x0100)
								i2 = 0x1000;
							else if (i2 == 0x1000)
								i2 = 0x0001;
						}
					}
					
					if (i2 == 0)
						npc.broadcastNpcSay("It is a puncture!!");
					
					int i1 = 3000 + (60 * 18 - (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0)) * 10;
					if (i0 == 0x0001)
						startQuestTimer("3014", npc, null, i1);
					else if (i0 == 0x0010)
						startQuestTimer("3015", npc, null, i1);
					else if (i0 == 0x0100)
						startQuestTimer("3016", npc, null, i1);
					else if (i0 == 0x1000)
						startQuestTimer("3017", npc, null, i1);
					else
						npc.broadcastNpcSay("It is a puncture in the archer phone!!");
				}
				// Fast Monster 2 (left, right)
				i0 = 0;
				if ((npc._i_quest3 & 0x0001) != 0)
					i0 = i0 + 1;
				if ((npc._i_quest3 & 0x0010) != 0)
					i0 = i0 + 1;
				if ((npc._i_quest3 & 0x0100) != 0)
					i0 = i0 + 1;
				if ((npc._i_quest3 & 0x1000) != 0)
					i0 = i0 + 1;
				if (i0 < npc._i_quest4)
				{
					int i2 = Rnd.get(4) + 1;
					
					if (i2 == 1)
						i2 = 0x0001;
					else if (i2 == 2)
						i2 = 0x0010;
					else if (i2 == 3)
						i2 = 0x0100;
					else if (i2 == 4)
						i2 = 0x1000;
					
					i0 = 0;
					for (int i1 = 1; i1 < 5; ++i1)
					{
						// Search for a location where monsters can be spawned.
						if (i0 == 0 && (i2 & npc._i_quest3) == 0)
						{
							i0 = i2;
							npc._i_quest3 = npc._i_quest3 + i0;
						}
						else
						{
							// move the beat
							if (i2 == 0x0001)
								i2 = 0x0010;
							else if (i2 == 0x0010)
								i2 = 0x0100;
							else if (i2 == 0x0100)
								i2 = 0x1000;
							else if (i2 == 0x1000)
								i2 = 0x0001;
						}
					}
					
					// puncture
					if (i2 == 0)
						npc.broadcastNpcSay("It is a puncture!!");
					
					int i1 = 3000 + (60 * 18 - (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0)) * 10;
					if (i0 == 0x0001)
						startQuestTimer("3018", npc, null, i1);
					else if (i0 == 0x0010)
						startQuestTimer("3019", npc, null, i1);
					else if (i0 == 0x0100)
						startQuestTimer("3020", npc, null, i1);
					else if (i0 == 0x1000)
						startQuestTimer("3021", npc, null, i1);
					else
						npc.broadcastNpcSay("It is a puncture in the archer phone!!");
				}
				
				// Bomb Monster Appears.
				if (npc._flag == 0)
				{
					if (Rnd.get(100) < 10)
					{
						createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_bomb_man_name"), npc.getX() + 250 + Rnd.get(100), npc.getY() + 250 + Rnd.get(100), npc.getZ(), 0, 0, false, npc.getX(), npc._i_ai0, 0);
						npc._flag = GameTimeTaskManager.getInstance().getCurrentTick();
					}
				}
				else if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._flag > 260 && Rnd.get(100) < 30)
				{
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_bomb_man_name"), npc.getX() - 250 - Rnd.get(100), npc.getY() - 250 - Rnd.get(100), npc.getZ(), 0, 0, false, npc.getX(), npc._i_ai0, 0);
					npc._flag = GameTimeTaskManager.getInstance().getCurrentTick();
				}
			}
			
			// Archer who holds the magic circle in check
			int i0 = GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0;
			if (i0 > 14 * 60)
				npc._i_quest1 = 4;
			else if (i0 > 12 * 60)
				npc._i_quest1 = 3;
			else if (i0 > 10 * 60)
				npc._i_quest1 = 2;
			else if (i0 > 8 * 60)
				npc._i_quest1 = 3;
			else if (i0 > 6 * 60)
				npc._i_quest1 = 2;
			
			i0 = 0;
			if ((npc._i_quest0 & 0x0001) != 0)
				i0 = i0 + 1;
			if ((npc._i_quest0 & 0x0010) != 0)
				i0 = i0 + 1;
			if ((npc._i_quest0 & 0x0100) != 0)
				i0 = i0 + 1;
			if ((npc._i_quest0 & 0x1000) != 0)
				i0 = i0 + 1;
			
			if (i0 < npc._i_quest1)
			{
				int i2 = Rnd.get(4) + 1;
				
				if (i2 == 1)
					i2 = 0x0001;
				else if (i2 == 2)
					i2 = 0x0010;
				else if (i2 == 3)
					i2 = 0x0100;
				else if (i2 == 4)
					i2 = 0x1000;
				
				i0 = 0;
				for (int i1 = 1; i1 < 5; i1++)
				{
					// Search for a location where monsters can be spawned.
					if (i0 == 0 && (i2 & npc._i_quest0) == 0)
					{
						i0 = i2;
						npc._i_quest0 = npc._i_quest0 + i0;
					}
					else
					{
						// Move the beat.
						if (i2 == 0x0001)
							i2 = 0x0010;
						else if (i2 == 0x0010)
							i2 = 0x0100;
						else if (i2 == 0x0100)
							i2 = 0x1000;
						else if (i2 == 0x1000)
							i2 = 0x0001;
					}
				}
				
				// Puncture.
				if (i2 == 0)
					npc.broadcastNpcSay("It is a puncture!!");
				
				int i1 = 3000 + (60 * 18 - (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0)) * 10;
				
				if (i0 == 0x0001)
					startQuestTimer("3006", npc, null, i1);
				else if (i0 == 0x0010)
					startQuestTimer("3007", npc, null, i1);
				else if (i0 == 0x0100)
					startQuestTimer("3008", npc, null, i1);
				else if (i0 == 0x1000)
					startQuestTimer("3009", npc, null, i1);
				else
					npc.broadcastNpcSay("It is a puncture in the archer phone!!");
			}
			
			startQuestTimer("3001", npc, null, 3000);
		}
		
		// Recruit monsters needed for the first battle.
		if (name.equalsIgnoreCase("3002"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_1x"), getNpcIntAIParam(npc, "battle1_1y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0001, npc._i_ai0, 0);
		
		if (name.equalsIgnoreCase("3003"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_2x"), getNpcIntAIParam(npc, "battle1_2y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0010, npc._i_ai0, 0);
		
		if (name.equalsIgnoreCase("3004"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_3x"), getNpcIntAIParam(npc, "battle1_3y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0100, npc._i_ai0, 0);
		
		if (name.equalsIgnoreCase("3005"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_npc_name"), getNpcIntAIParam(npc, "battle1_4x"), getNpcIntAIParam(npc, "battle1_4y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x1000, npc._i_ai0, 0);
		
		// Spawn a unit of archers.
		if (name.equalsIgnoreCase("3006"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_sniper_name"), getNpcIntAIParam(npc, "stair_1x"), getNpcIntAIParam(npc, "stair_1y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0001, npc._i_ai0, 0);
		
		if (name.equalsIgnoreCase("3007"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_sniper_name"), getNpcIntAIParam(npc, "stair_2x"), getNpcIntAIParam(npc, "stair_2y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0010, npc._i_ai0, 0);
		
		if (name.equalsIgnoreCase("3008"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_sniper_name"), getNpcIntAIParam(npc, "stair_3x"), getNpcIntAIParam(npc, "stair_3y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0100, npc._i_ai0, 0);
		
		if (name.equalsIgnoreCase("3009"))
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_sniper_name"), getNpcIntAIParam(npc, "stair_4x"), getNpcIntAIParam(npc, "stair_4y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x1000, npc._i_ai0, 0);
			
		// Calculate ??? when the enhanced version monster will appear
		// i0 = 1 if an enhanced version monster is spawned, otherwise 0
		int i0 = (int) (((1.0 * GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0) / (60 * 20) * 100));
		
		if (i0 >= Rnd.get(100))
			i0 = 1;
		else
			i0 = 0;
		
		// slow monster spawn.
		if (name.equalsIgnoreCase("3010") || (name.equalsIgnoreCase("3011") || (name.equalsIgnoreCase("3012") || (name.equalsIgnoreCase("3013")))))
		{
			if (i0 == 1)
				i0 = getNpcIntAIParam(npc, "battle_slow_type_name_ex");
			else
				i0 = getNpcIntAIParam(npc, "battle_slow_type_name");
			
			if (name.equalsIgnoreCase("3010"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle1_1x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle1_1y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x0001);
			
			if (name.equalsIgnoreCase("3011"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_2x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_2y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x0010);
			
			if (name.equalsIgnoreCase("3012"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_3x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_3y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x0100);
			
			if (name.equalsIgnoreCase("3013"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_4x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_4y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x1000);
		}
		
		// Fast monster spawn.
		if (name.equalsIgnoreCase("3014") || (name.equalsIgnoreCase("3015") || (name.equalsIgnoreCase("3016") || (name.equalsIgnoreCase("3017") || (name.equalsIgnoreCase("3018") || (name.equalsIgnoreCase("3019") || (name.equalsIgnoreCase("3020") || (name.equalsIgnoreCase("3021")))))))))
		{
			if (i0 == 1)
				i0 = getNpcIntAIParam(npc, "battle_fast_type_name_ex");
			else
				i0 = getNpcIntAIParam(npc, "battle_fast_type_name");
			
			// Fast monster 1 spawn.
			if (name.equalsIgnoreCase("3014"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_1x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_1y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x0001);
			
			if (name.equalsIgnoreCase("3015"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_2x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_2y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x0010);
			
			if (name.equalsIgnoreCase("3016"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_3x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_3y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x0100);
			
			if (name.equalsIgnoreCase("3017"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_4x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_4y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x1000);
			
			// Fast monster 2 spawn.
			if (name.equalsIgnoreCase("3018"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_1x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_1y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x10000001);
			
			if (name.equalsIgnoreCase("3019"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_2x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_2y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x10000010);
			
			if (name.equalsIgnoreCase("3020"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_3x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_3y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x10000100);
			
			if (name.equalsIgnoreCase("3021"))
				createOnePrivateEx(npc, i0, getNpcIntAIParam(npc, "battle2_4x") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "battle2_4y") - 80 + Rnd.get(160), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, npc.getX(), npc._i_ai0, 0x10001000);
		}
		
		if (name.equalsIgnoreCase("3022"))
		{
			npc.getMaster().getSpawn().instantTeleportInMyTerritory(new Location(getNpcIntAIParam(npc, "escape_x"), getNpcIntAIParam(npc, "escape_y"), getNpcIntAIParam(npc, "coord_z")), 100);
			npc.deleteMe();
			
			FestivalOfDarknessManager.FestivalManager.getFestivalInstance().values().forEach(L2DarknessFestival::festivalEnd);
			TimeAttackEventRoom.getInstance().clear(getNpcIntAIParam(npc, "room_index"), getNpcIntAIParam(npc, "part_type"));
		}
		
		if (name.equalsIgnoreCase("3023"))
		{
			npc.broadcastPacket(new PlaySound(1, "B06_S01", npc));
			startQuestTimer("3024", npc, null, 180 * 1000);
		}
		
		if (name.equalsIgnoreCase("3024"))
		{
			npc.broadcastPacket(new PlaySound(1, "B07_S01", npc));
			startQuestTimer("3025", npc, null, 122 * 1000);
		}
		
		if (name.equalsIgnoreCase("3025"))
		{
			npc.broadcastPacket(new PlaySound(1, "B07_S01", npc));
			startQuestTimer("3026", npc, null, 122 * 1000);
		}
		
		if (name.equalsIgnoreCase("3026"))
		{
			npc.broadcastPacket(new PlaySound(1, "B07_S01", npc));
			startQuestTimer("3027", npc, null, 122 * 1000);
		}
		
		if (name.equalsIgnoreCase("3027"))
		{
			npc.broadcastPacket(new PlaySound(1, "B07_S01", npc));
			startQuestTimer("3028", npc, null, 180 * 1000);
		}
		
		if (name.equalsIgnoreCase("3028"))
		{
			npc.broadcastPacket(new PlaySound(1, "B06_F", npc));
			startQuestTimer("3029", npc, null, 120 * 1000);
		}
		
		if (name.equalsIgnoreCase("3029"))
			npc.broadcastPacket(new PlaySound(1, "B06_F", npc));
		
		broadcastScriptEventEx(npc, 10007, npc.getY(), npc.getObjectId(), 1000);
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onScriptEvent(Npc npc, int eventId, int arg1, int arg2)
	{
		// Request for one-shot command.
		if (eventId == 10012)
		{
			if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._param2 > 90 + Rnd.get(60))
			{
				final Creature c0 = (Creature) World.getInstance().getObject(arg1);
				if (c0 instanceof Player)
					((Npc) c0).sendScriptEvent(10013, 0, 0); // Permit one-point shooting.
					
				npc._param2 = GameTimeTaskManager.getInstance().getCurrentTick();
			}
		}
		
		// If the party leader dies in the first battle (within 4 minutes and 30 seconds of birth).
		if (eventId == 10005 && GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai0 < 270)
		{
			if (arg1 == 0x0001)
				startQuestTimer("3002", npc, null, 8000 + Rnd.get(8) * 1000);
			else if (arg1 == 0x0010)
				startQuestTimer("3003", npc, null, 8000 + Rnd.get(8) * 1000);
			else if (arg1 == 0x0100)
				startQuestTimer("3004", npc, null, 8000 + Rnd.get(8) * 1000);
			else if (arg1 == 0x1000)
				startQuestTimer("3005", npc, null, 8000 + Rnd.get(8) * 1000);
		}
		
		// When the party leader asks for help in the first battle.
		if (eventId == 10004)
		{
			int i0 = Rnd.get(2) + 1;
			if (arg1 == 0x0001)
			{
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_11x"), getNpcIntAIParam(npc, "battle1_11y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0001, npc._i_ai0, 0);
				if (i0 == 2)
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_12x"), getNpcIntAIParam(npc, "battle1_12y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0001, npc._i_ai0, 0);
			}
			
			if (arg1 == 0x0010)
			{
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_21x"), getNpcIntAIParam(npc, "battle1_21y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0010, npc._i_ai0, 0);
				if (i0 == 2)
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_22x"), getNpcIntAIParam(npc, "battle1_22y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0010, npc._i_ai0, 0);
			}
			
			if (arg1 == 0x0100)
			{
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_31x"), getNpcIntAIParam(npc, "battle1_31y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0100, npc._i_ai0, 0);
				if (i0 == 2)
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_32x"), getNpcIntAIParam(npc, "battle1_32y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x0100, npc._i_ai0, 0);
			}
			
			if (arg1 == 0x1000)
			{
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_41x"), getNpcIntAIParam(npc, "battle1_41y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x1000, npc._i_ai0, 0);
				if (i0 == 2)
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle1_archer_name"), getNpcIntAIParam(npc, "battle1_42x"), getNpcIntAIParam(npc, "battle1_42y"), getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0x1000, npc._i_ai0, 0);
			}
		}
		
		// If the archer spawning on the platform dies.
		if (eventId == 10006)
			npc._i_quest0 = npc._i_quest0 - arg1;
		
		// When a slow monster dies.
		if (eventId == 10008)
			npc._i_ai2 = npc._i_ai2 - arg1;
		
		// When fast monster 1 dies.
		if (eventId == 10009)
			npc._i_quest2 = npc._i_quest2 - arg1;
		
		// When Fast Monster 2 dies.
		if (eventId == 10010)
			npc._i_quest3 = npc._i_quest3 - arg1;
		
		if (eventId == 10011)
			npc._param1 = npc._param1 - 1;
	}
	
	@Override
	public void ON_TALKED(Npc npc, Player talker)
	{
		showHTML(npc, talker, "html/seven_signs/festival/festival_witch.htm");
	}
	
	@Override
	public void ON_MENU_SELECTED(Npc npc, Player talker, int ask, int reply)
	{
		if (ask == 505)
		{
			if (reply == 1)
			{
				var party = talker.getParty();
				if (party == null)
					talker.teleportTo(getNpcIntAIParam(npc, "escape_x"), getNpcIntAIParam(npc, "escape_y"), getNpcIntAIParam(npc, "coord_z"), 0);
				else if (party.getLeader() != talker)
					showHTML(npc, talker, "html/seven_signs/festival/ssq_main_event_sibyl_q0505_04.htm");
				else
					showHTML(npc, talker, "html/seven_signs/festival/ssq_main_event_sibyl_q0505_01.htm");
			}
			else if (reply == 2)
			{
				var party = talker.getParty();
				if (party != null && party.getLeader() != talker)
					showHTML(npc, talker, "html/seven_signs/festival/ssq_main_event_sibyl_q0505_04.htm");
				else
					showHTML(npc, talker, "html/seven_signs/festival/ssq_main_event_sibyl_q0505_02.htm");
			}
			else if (reply == 3)
			{
				startQuestTimer("3022", npc, null, 7000);
				broadcastScriptEvent(npc, 10015, 0, 1500);
				showHTML(npc, talker, "html/seven_signs/festival/ssq_main_event_sibyl_q0505_05.htm");
			}
			else if (reply == 4)
			{
				// Additional requests are accepted with a minimum delay of 10 seconds.
				if (GameTimeTaskManager.getInstance().getCurrentTick() - npc._i_ai3 < 10 || npc._param1 >= 5)
					showHTML(npc, talker, "html/seven_signs/festival/ssq_main_event_sibyl_q0505_03.htm"); // I'll leave a message to encourage them to do their best in the current situation.
				else
				{
					// Currently, it spawns within a radius of approximately 500 grids around itself (NPC).
					int i0 = npc.getX() - 500 + Rnd.get(1000);
					int i1 = npc.getY() - 500 + Rnd.get(1000);
					if (Rnd.get(100) < 40)
						createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_supporter_name"), i0, i1, getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0, npc._i_ai0, 0);
					else
						createOnePrivateEx(npc, getNpcIntAIParam(npc, "battle_supporter_name_ex"), i0, i1, getNpcIntAIParam(npc, "coord_z"), 32768, 0, false, 0, npc._i_ai0, 0);
					
					npc._param1++;
					npc._i_ai3 = GameTimeTaskManager.getInstance().getCurrentTick();
				}
			}
		}
	}
	
	public void showHTML(Npc npc, Player talker, String content)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HTMLData.getInstance().getHtm(talker, content));
		talker.sendPacket(html);
		talker.sendPacket(ActionFailed.STATIC_PACKET);
	}
}