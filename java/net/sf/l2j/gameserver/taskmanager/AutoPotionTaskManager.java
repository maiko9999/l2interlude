package net.sf.l2j.gameserver.taskmanager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class AutoPotionTaskManager implements Runnable
{
	private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
	private static boolean _working = false;
	
	protected AutoPotionTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 0, Config.ACP_PERIOD);
	}
	
	@Override
	public void run()
	{
		if (_working)
			return;
		
		_working = true;
		
		if (!PLAYERS.isEmpty())
		{
			for (Player player : PLAYERS)
			{
				if (player == null || player.isAlikeDead() || player.isOnlineInt() != 1 || !Config.AUTO_POTIONS_IN_OLYMPIAD && player.isInOlympiadMode())
				{
					remove(player);
					continue;
				}
				
				if (Config.AUTO_CP_ENABLED)
				{
					final boolean restoreCP = ((player.getStatus().getCp() / player.getStatus().getMaxCp()) * 100) <= player.isAcpCp();
					for (int itemId : Config.AUTO_CP_ITEM_IDS)
					{
						final ItemInstance cpPotion = player.getInventory().getItemByItemId(itemId);
						if (cpPotion != null && cpPotion.getCount() > 0)
						{
							if (restoreCP)
							{
								ItemHandler.getInstance().getHandler(cpPotion.getEtcItem()).useItem(player, cpPotion, false);
								player.sendMessage("Auto potion: Restored CP.");
								break;
							}
						}
					}
				}
				
				if (Config.AUTO_HP_ENABLED)
				{
					final boolean restoreHP = ((player.getStatus().getHp() / player.getStatus().getMaxHp()) * 100) <= player.isAcpHp();
					for (int itemId : Config.AUTO_HP_ITEM_IDS)
					{
						final ItemInstance hpPotion = player.getInventory().getItemByItemId(itemId);
						if (hpPotion != null && hpPotion.getCount() > 0)
						{
							if (restoreHP)
							{
								ItemHandler.getInstance().getHandler(hpPotion.getEtcItem()).useItem(player, hpPotion, false);
								player.sendMessage("Auto potion: Restored HP.");
								break;
							}
						}
					}
				}
				
				if (Config.AUTO_MP_ENABLED)
				{
					final boolean restoreMP = ((player.getStatus().getMp() / player.getStatus().getMaxMp()) * 100) <= player.isAcpMp();
					for (int itemId : Config.AUTO_MP_ITEM_IDS)
					{
						final ItemInstance mpPotion = player.getInventory().getItemByItemId(itemId);
						if (mpPotion != null && mpPotion.getCount() > 0)
						{
							if (restoreMP)
							{
								ItemHandler.getInstance().getHandler(mpPotion.getEtcItem()).useItem(player, mpPotion, false);
								player.sendMessage("Auto potion: Restored MP.");
								break;
							}
						}
					}
				}
			}
		}
		
		_working = false;
	}
	
	public void add(Player player)
	{
		if (!PLAYERS.contains(player))
			PLAYERS.add(player);
	}
	
	public void remove(Player player)
	{
		PLAYERS.remove(player);
	}
	
	public static AutoPotionTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoPotionTaskManager INSTANCE = new AutoPotionTaskManager();
	}
}