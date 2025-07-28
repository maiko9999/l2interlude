package net.sf.l2j.gameserver.taskmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class DelayedItemsManager implements Runnable
{
	private static final CLogger LOGGER = new CLogger(DelayedItemsManager.class.getName());
	
	private static final String SELECT = "SELECT * FROM items_delayed WHERE payment_status = 0";
	
	private DelayedItemsManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 60000L, 60000L);
	}
	
	@Override
	public void run()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(SELECT);
				ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final Player player = World.getInstance().getPlayer(rset.getInt("owner_id"));
					if (player != null && player.isOnline())
					{
						final int itemId = rset.getInt("item_id");
						final int count = rset.getInt("count");
						final int enchant = rset.getInt("enchant_level");
						final Item giveItem = ItemData.getInstance().getTemplate(itemId);
						if (giveItem != null)
						{
							final ItemInstance item = player.addItem(itemId, count, true);
							if (item != null && enchant > 0)
								item.setEnchantLevel(enchant, player);
							
							updateDonation(player.getObjectId(), itemId, count, enchant);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("DelayedItemsManager: " + e);
		}
	}
	
	private static void updateDonation(int objId, int id, long count, int enchant)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM items_delayed WHERE owner_id=? AND item_id=? AND count=? AND enchant_level=?;"))
		{
			statement.setInt(1, objId);
			statement.setInt(2, id);
			statement.setLong(3, count);
			statement.setInt(4, enchant);
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warn("Failed to remove unitpay_payments from database id: " + id);
		}
	}
	
	public static final DelayedItemsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		protected static final DelayedItemsManager INSTANCE = new DelayedItemsManager();
	}
}