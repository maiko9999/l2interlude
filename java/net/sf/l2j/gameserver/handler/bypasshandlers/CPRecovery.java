package net.sf.l2j.gameserver.handler.bypasshandlers;

import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class CPRecovery implements IBypassHandler
{
	private static final String[] COMMANDS = { "CPRecovery" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (target instanceof Npc npc)
		{
			if (npc.getNpcId() != 31225 && npc.getNpcId() != 31226)
				return false;
			
			// Consume 100 Adena.
			if (player.reduceAdena(100, true))
			{
				npc.getAI().addCastDesireHold(player, FrequentSkill.ARENA_CP_RECOVERY.getSkill(), 1000000);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED).addCharName(player));
			}
		}
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}