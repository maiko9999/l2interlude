package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import ext.mods.roulette.RouletteData;
import ext.mods.roulette.holder.RouletteHolder;

public class RouletteVoiced implements IVoicedCommandHandler
{
	private Map<Integer, Long> _nextUse = new HashMap<>();
	private Map<Integer, ScheduledFuture<?>> _activeTasks = new HashMap<>();
	
	private static final String[] _voicedCommands =
	{
		"roulette",
		"spin"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (player == null)
			return false;
		
		if (command.startsWith("roulette"))
		{
			navi(player);
		}
		
		if (command.startsWith("spin"))
		{
			trySpin(player);
		}
		return true;
	}
	
	public static void navi(Player player)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body><center>");
		
		sb.append("<table width=280 bgcolor=000000>").append("<tr><td align=center><font color=\"LEVEL\">Lucky Roulette</font></td></tr>").append("</table><br>");
		
		sb.append("<table width=250><tr><td align=center>").append("<img src=\"sek.cbui183\" width=64 height=64><br1>").append("<font color=\"LEVEL\">Spin and win amazing prizes!</font><br>").append("<font color=\"FFFFFF\">Cost to spin: <font color=\"LEVEL\">10.000 Adena</font></font><br1>").append("<font color=\"999999\">(Can re-spin every 5 seconds)</font>").append("</td></tr></table><br>");
		
		sb.append("<button value=\"Spin Roulette\" action=\"bypass -h voiced_spin\" width=75 height=16 back=\"sek.cbui307\" fore=\"sek.cbui308\">");
		
		sb.append("<br><br><table width=280 bgcolor=000000>").append("<tr><td align=center><font color=\"999999\">Good luck!</font></td></tr>").append("</table>");
		
		sb.append("</center></body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	private void trySpin(Player player)
	{
		ScheduledFuture<?> activeTask = _activeTasks.get(player.getObjectId());
		if (activeTask != null && !activeTask.isDone())
		{
			player.sendMessage("You are already spinning the roulette wheel!");
			return;
		}
		
		if (_nextUse.containsKey(player.getObjectId()) && _nextUse.get(player.getObjectId()) > System.currentTimeMillis())
		{
			player.sendMessage("You must wait before spinning again.");
			return;
		}
		
		boolean canSpin = false;
		RouletteData roleta = RouletteData.getInstance();
		
		if (roleta.isUseAdena())
		{
			canSpin = player.reduceAdena(roleta.getAdenaCost(), true);
			if (!canSpin)
				player.sendMessage("You don't have enough Adena!");
		}
		else
		{
			if (player.getInventory().destroyItemByItemId(roleta.getItemId(), roleta.getItemCount()) != null)
			{
				
				canSpin = true;
			}
			else
			{
				player.sendMessage("You don't have enough required items!");
			}
		}
		
		if (!canSpin)
			return;
		
		_nextUse.put(player.getObjectId(), System.currentTimeMillis() + 5000); // cooldown
		
		startSpin(player);
	}
	
	private void startSpin(Player player)
	{
		ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() ->
		{
			sendRollingHtml(player);
		}, 500, 1000);
		
		_activeTasks.put(player.getObjectId(), task);
		
		ThreadPool.schedule(() ->
		{
			RouletteHolder prize = RouletteData.getInstance().getRandomItem();
			if (prize != null)
			{
				ScheduledFuture<?> activeTask = _activeTasks.remove(player.getObjectId());
				if (activeTask != null)
				{
					activeTask.cancel(false);
				}
				
				ItemInstance item = null;
				
				if (prize.getEnchant() > 0)
				{
					
					item = player.getInventory().addItem(prize.getId(), prize.getCount());
					item.setEnchantLevel(prize.getEnchant(), player);
					
					player.sendPacket(item.getEnchantLevel() == 0 ? SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED).addItemName(item.getItemId()) : SystemMessage.getSystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
					
				}
				else
				{
					player.getInventory().addItem(prize.getId(), prize.getCount());
				}
				
				player.sendPacket(new ItemList(player, false));
				showPrizeHtml(player, prize);
			}
		}, 10000); // 10 segundos
	}
	
	public void sendRollingHtml(Player player)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">Spinning...</font><br1>");
		
		for (int block = 0; block < 8; block++)
		{
			sb.append("<table width=300><tr>");
			
			for (int i = 0; i < 5; i++)
			{
				RouletteHolder randomItem = RouletteData.getInstance().getRandomVisualItem();
				if (randomItem != null)
				{
					sb.append("<td align=center>");
					sb.append("<img src=\"" + getItemIcon(randomItem.getId()) + "\" width=32 height=32>");
					sb.append("</td>");
				}
				else
				{
					
					sb.append("<td align=center>?</td>");
				}
			}
			
			sb.append("</tr></table>");
		}
		
		sb.append("<br><font color=LEVEL>Good luck!</font>");
		sb.append("</center></body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	public void showPrizeHtml(Player player, RouletteHolder prize)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">You won!</font><br>");
		sb.append("<img src=\"" + getItemIcon(prize.getId()) + "\" width=32 height=32><br>");
		sb.append("<font color=\"FFFFFF\">" + getItemNameId(prize.getId()) + "</font><br>");
		sb.append("<font color=\"LEVEL\">Amount:</font> <font color=\"FFFFFF\">" + prize.getCount() + "</font><br>");
		sb.append("<br><button value=\"Spin Again\" action=\"bypass -h voiced_spin\" width=75 height=16 back=\"sek.cbui307\" fore=\"sek.cbui308\">");
		sb.append("</center></body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	public String getItemIcon(int itemId)
	{
		Item item = ItemData.getInstance().getTemplate(itemId);
		return item != null ? item.getIcon() : "Unknown Item";
	}
	
	public String getItemNameId(int itemId)
	{
		Item item = ItemData.getInstance().getTemplate(itemId);
		return item != null ? item.getName() : "Icon.NOIMAGE";
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}