package ext.mods.email.items;

import java.sql.Connection;
import java.sql.PreparedStatement;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class EmailStorage
{
	
	public static void saveEmail(int emailId, int senderId, int targetId, ItemInstance item, boolean isPaid, int paymentItemId, int paymentItemCount, long expirationTime)
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			PreparedStatement ps = con.prepareStatement("INSERT INTO player_emails (email_id, sender_id, target_id, item_object_id, item_id, count, enchant_level, is_augmented, augment_id, is_paid, payment_item_id, payment_item_count, expiration_time, created_time) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			ps.setInt(1, emailId);
			ps.setInt(2, senderId);
			ps.setInt(3, targetId);
			ps.setInt(4, item.getObjectId());
			ps.setInt(5, item.getItemId());
			ps.setInt(6, item.getCount());
			ps.setInt(7, item.getEnchantLevel());
			ps.setBoolean(8, item.isAugmented());
			ps.setInt(9, item.isAugmented() ? item.getAugmentation().getId() : 0);
			ps.setBoolean(10, isPaid);
			ps.setObject(11, isPaid ? paymentItemId : null);
			ps.setObject(12, isPaid ? paymentItemCount : null);
			ps.setLong(13, expirationTime);
			ps.setLong(14, System.currentTimeMillis());
			
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}