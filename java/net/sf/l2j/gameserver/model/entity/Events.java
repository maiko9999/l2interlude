package net.sf.l2j.gameserver.model.entity;

import java.util.List;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public class Events
{
	protected static final CLogger LOGGER = new CLogger(Events.class.getName());
	
	protected static void closeDoors(List<Integer> doors)
	{
		for (int doorId : doors)
		{
			Door door = DoorData.getInstance().getDoor(doorId);
			
			if (door != null)
				door.closeMe();
		}
	}
	
	protected static void openDoors(List<Integer> doors)
	{
		for (int doorId : doors)
		{
			Door door = DoorData.getInstance().getDoor(doorId);
			
			if (door != null)
				door.openMe();
		}
	}
	
	public static void spawnProtection(Player player)
	{
		player.startAbnormalEffect(AbnormalEffect.HOLD_2);
		player.setIsParalyzed(true);
		player.sendMessage(player.getSysString(10_175));
		
		ThreadPool.schedule(() ->
		{
			player.setIsParalyzed(false);
			player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
			player.sendPacket(new ExShowScreenMessage("FIGHT!", 3000));
			player.sendMessage(player.getSysString(10_011));
		}, 15000);
	}
}