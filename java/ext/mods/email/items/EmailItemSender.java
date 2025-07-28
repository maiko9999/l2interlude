package ext.mods.email.items;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

import ext.mods.email.PlayerEmailManager;
import ext.mods.email.task.EmailDeliveryTask;
import ext.mods.util.Tokenizer;

public class EmailItemSender
{
	
	public static void processSendCommand(Player sender, Tokenizer tokenizer)
	{
		String targetName = sender.getSelectedEmailTarget();
		String paymentOption = tokenizer.getAsString(3);
		
		String paymentItemName = tokenizer.getAsString(4);
		int paymentAmount = tokenizer.getAsInteger(5, 0);
		String durationOption = tokenizer.getAsString(4);
		
		if (targetName == null || paymentOption == null || durationOption == null)
		{
			sender.sendMessage("Você deve selecionar um jogador antes de enviar.");
			PlayerEmailManager.getInstance().navi(sender, "email_main.htm");
			return;
		}
		
		Integer targetObjectId = getTargetObjectId(targetName);
		if (targetObjectId == null)
		{
			sender.sendMessage("Jogador '" + targetName + "' não encontrado.");
			PlayerEmailManager.getInstance().navi(sender, "email_main.htm");
			return;
		}
		
		boolean isPaid = paymentOption.equalsIgnoreCase("sim");
		int paymentItemId = parsePaymentItemId(paymentItemName);
		
		if (isPaid)
		{
			if (paymentItemId == -1)
			{
				sender.sendMessage("Você marcou como pago, mas não selecionou um item válido de pagamento.");
				PlayerEmailManager.getInstance().navi(sender, "email_main.htm");
				return;
			}
			if (paymentAmount <= 0)
			{
				sender.sendMessage("Você marcou como pago, mas não especificou uma quantidade válida.");
				PlayerEmailManager.getInstance().navi(sender, "email_main.htm");
				return;
			}
		}
		
		long expirationTime = calculateExpiration(durationOption);
		
		Map<Integer, Long> selectedItems = sender.getTempSelectedItems();
		if (selectedItems.isEmpty())
		{
			sender.sendMessage("Nenhum item selecionado para envio.");
			PlayerEmailManager.getInstance().navi(sender, "email_main.htm");
			return;
		}
		
		int emailId = IdFactory.getInstance().getNextId();
		
		int sentCount = 0;
		for (Map.Entry<Integer, Long> entry : selectedItems.entrySet())
		{
			int objectId = entry.getKey();
			long quantity = entry.getValue();
			
			ItemInstance item = sender.getInventory().getItemByObjectId(objectId);
			if (item == null || quantity < 1 || quantity > item.getCount())
			{
				continue;
			}
			
			ItemInstance removedItem = sender.getInventory().destroyItem(objectId, (int) quantity);
			if (removedItem != null)
			{
				removedItem.setCount((int) quantity);
				
				EmailStorage.saveEmail(emailId, sender.getObjectId(), targetObjectId, removedItem, isPaid, paymentItemId, paymentAmount, expirationTime);
				sentCount++;
			}
		}
		
		sender.clearTempSelectedItems();
		sender.setSelectedEmailTarget(null);
		
		if (sentCount > 0)
		{
			sender.sendMessage("Email enviado com sucesso para " + targetName + ".");
			EmailDeliveryTask.getInstance().scheduleExpiration(sender, emailId, expirationTime);
		}
		else
		{
			sender.sendMessage("Nenhum Email pôde ser enviado.");
		}
	}
	
	private static Integer getTargetObjectId(String targetName)
	{
		Player target = World.getInstance().getPlayer(targetName);
		if (target != null)
		{
			return target.getObjectId();
		}
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT obj_Id FROM characters WHERE char_name = ?"))
		{
			
			ps.setString(1, targetName);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("obj_Id");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private static int parsePaymentItemId(String itemName)
	{
		if (itemName == null)
		{
			return -1;
		}
		
		switch (itemName.trim().toLowerCase())
		{
			case "adena":
				return 57;
			case "goldbar":
				return 3470;
			case "ticketdonate":
				return 9999;
			case "nenhum":
				return -1;
			default:
				return -1;
		}
	}
	
	private static long calculateExpiration(String durationStr)
	{
		long currentTime = System.currentTimeMillis();
		switch (durationStr.toLowerCase())
		{
			case "5minutos":
				return currentTime + (5 * 60 * 1000);
			case "30minutos":
				return currentTime + (30 * 60 * 1000);
			case "2horas":
				return currentTime + (2 * 60 * 60 * 1000);
			default:
				return currentTime + (5 * 60 * 1000); // padrão
		}
	}
}
