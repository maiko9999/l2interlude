package net.sf.l2j.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.OperateType;
import net.sf.l2j.gameserver.model.SellBuffHolder;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.records.ManufactureItem;
import net.sf.l2j.gameserver.model.trade.TradeItem;
import net.sf.l2j.gameserver.network.GameClient;

public final class OfflineTradersTable
{
	private static final CLogger LOGGER = new CLogger(OfflineTradersTable.class.getName());
	
	private static final String SAVE_OFFLINE_STATUS = "INSERT INTO character_offline_trade (`charId`,`time`,`type`,`title`) VALUES (?,?,?,?)";
	private static final String SAVE_ITEMS = "INSERT INTO character_offline_trade_items (`charId`,`item`,`count`,`price`,`enchant`) VALUES (?,?,?,?,?)";
	private static final String CLEAR_OFFLINE_TABLE = "DELETE FROM character_offline_trade";
	private static final String CLEAR_OFFLINE_TABLE_ITEMS = "DELETE FROM character_offline_trade_items";
	private static final String LOAD_OFFLINE_STATUS = "SELECT * FROM character_offline_trade";
	private static final String LOAD_OFFLINE_ITEMS = "SELECT * FROM character_offline_trade_items WHERE charId = ?";
	
	public void store()
	{
		if (!Config.RESTORE_OFFLINERS || (!Config.OFFLINE_TRADE_ENABLE && !Config.OFFLINE_CRAFT_ENABLE))
			return;
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement offline = con.prepareStatement(SAVE_OFFLINE_STATUS);
			PreparedStatement item = con.prepareStatement(SAVE_ITEMS))
		{
			try (Statement stm = con.createStatement())
			{
				stm.execute(CLEAR_OFFLINE_TABLE);
				stm.execute(CLEAR_OFFLINE_TABLE_ITEMS);
			}
			
			for (Player player : World.getInstance().getPlayers())
			{
				try
				{
					if (player.getOperateType() != OperateType.NONE && (player.getClient() == null || player.getClient().isDetached()))
					{
						offline.setInt(1, player.getObjectId());
						offline.setLong(2, player.getOfflineStartTime());
						offline.setInt(3, player.isSellingBuffs() ? OperateType.SELL_BUFFS.getId() : player.getOperateType().getId());
						
						switch (player.getOperateType())
						{
							case BUY:
								if (!Config.OFFLINE_TRADE_ENABLE)
									continue;
								
								offline.setString(4, player.getBuyList().getTitle());
								for (final TradeItem i : player.getBuyList())
								{
									item.setInt(1, player.getObjectId());
									item.setInt(2, i.getItem().getItemId());
									item.setLong(3, i.getQuantity());
									item.setLong(4, i.getPrice());
									item.setLong(5, i.getEnchant());
									item.addBatch();
								}
								break;
							
							case SELL:
							case PACKAGE_SELL:
								if (!Config.OFFLINE_TRADE_ENABLE)
									continue;
								
								offline.setString(4, player.getSellList().getTitle());
								player.getSellList().updateItems(false);
								if (player.isSellingBuffs())
								{
									for (SellBuffHolder holder : player.getSellingBuffs())
									{
										item.setInt(1, player.getObjectId());
										item.setInt(2, holder.getSkillId());
										item.setLong(3, holder.getSkillLvl());
										item.setLong(4, holder.getPrice());
										item.setLong(5, 0);
										item.addBatch();
									}
								}
								else
								{
									for (final TradeItem i : player.getSellList())
									{
										item.setInt(1, player.getObjectId());
										item.setInt(2, i.getObjectId());
										item.setLong(3, i.getQuantity());
										item.setLong(4, i.getPrice());
										item.setLong(5, i.getEnchant());
										item.addBatch();
									}
								}
								break;
							
							case MANUFACTURE:
								if (!Config.OFFLINE_CRAFT_ENABLE)
									continue;
								
								offline.setString(4, player.getManufactureList().getStoreName());
								for (final ManufactureItem i : player.getManufactureList())
								{
									item.setInt(1, player.getObjectId());
									item.setInt(2, i.recipeId());
									item.setLong(3, 0L);
									item.setLong(4, i.cost());
									item.setLong(5, 0L);
									item.addBatch();
								}
								break;
						}
						
						item.executeBatch();
						offline.execute();
					}
				}
				catch (Exception e)
				{
					LOGGER.error("Error while saving offline: " + player.getObjectId() + " ", e);
				}
			}
			
			LOGGER.info("Offline stored.");
		}
		catch (Exception e)
		{
			LOGGER.error("Error while saving offline: ", e);
		}
	}
	
	public void saveOfflineTraders(Player player)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(CLEAR_OFFLINE_TABLE);
			PreparedStatement ps2 = con.prepareStatement(CLEAR_OFFLINE_TABLE_ITEMS))
		{
			ps.execute();
			ps2.execute();
			
			try (PreparedStatement ps3 = con.prepareStatement(SAVE_OFFLINE_STATUS);
				PreparedStatement ps4 = con.prepareStatement(SAVE_ITEMS))
			{
				ps3.setInt(1, player.getObjectId());
				ps3.setLong(2, player.getOfflineStartTime());
				ps3.setInt(3, player.isSellingBuffs() ? OperateType.SELL_BUFFS.getId() : player.getOperateType().getId());
				
				switch (player.getOperateType())
				{
					case BUY:
						ps3.setString(4, player.getBuyList().getTitle());
						for (final TradeItem i : player.getBuyList())
						{
							ps4.setInt(1, player.getObjectId());
							ps4.setInt(2, i.getItem().getItemId());
							ps4.setLong(3, i.getQuantity());
							ps4.setLong(4, i.getPrice());
							ps4.setLong(5, i.getEnchant());
							ps4.addBatch();
						}
						break;
					
					case SELL:
					case PACKAGE_SELL:
						ps3.setString(4, player.getSellList().getTitle());
						player.getSellList().updateItems(false);
						if (player.isSellingBuffs())
						{
							for (SellBuffHolder holder : player.getSellingBuffs())
							{
								ps4.setInt(1, player.getObjectId());
								ps4.setInt(2, holder.getSkillId());
								ps4.setLong(3, holder.getSkillLvl());
								ps4.setLong(4, holder.getPrice());
								ps4.setLong(5, 0);
								ps4.addBatch();
							}
						}
						else
						{
							for (final TradeItem i : player.getSellList())
							{
								ps4.setInt(1, player.getObjectId());
								ps4.setInt(2, i.getObjectId());
								ps4.setLong(3, i.getQuantity());
								ps4.setLong(4, i.getPrice());
								ps4.setLong(5, i.getEnchant());
								ps4.addBatch();
							}
						}
						break;
					
					case MANUFACTURE:
						ps3.setString(4, player.getManufactureList().getStoreName());
						for (final ManufactureItem i : player.getManufactureList())
						{
							ps4.setInt(1, player.getObjectId());
							ps4.setInt(2, i.recipeId());
							ps4.setLong(3, 0);
							ps4.setLong(4, i.cost());
							ps4.setLong(5, 0);
							ps4.addBatch();
						}
						break;
				}
				
				ps4.executeBatch();
				ps3.execute();
			}
			catch (Exception e)
			{
				LOGGER.error("error while saving offline traders.", e);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("error while clear table offline traders.", e);
		}
	}
	
	public void restore()
	{
		if (!Config.RESTORE_OFFLINERS || (!Config.OFFLINE_TRADE_ENABLE && !Config.OFFLINE_CRAFT_ENABLE))
			return;
		
		int count = 0;
		
		try (Connection con = ConnectionPool.getConnection();
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(LOAD_OFFLINE_STATUS))
		{
			
			while (rs.next())
			{
				final long time = rs.getLong("time");
				if (Config.OFFLINE_MAX_DAYS > 0 && isExpired(time))
					continue;
				
				final OperateType oType = getType(rs.getInt("type"));
				boolean isSellBuff = false;
				if (oType == OperateType.SELL_BUFFS)
					isSellBuff = true;
				
				final OperateType type = isSellBuff ? OperateType.PACKAGE_SELL : oType;
				if (type == null || type == OperateType.NONE)
					continue;
				
				final Player player = Player.restore(rs.getInt("charId"), true);
				if (player == null)
					continue;
				
				final GameClient client = new GameClient(null);
				client.spawnOffline(player);
				player.setOfflineStartTime(time);
				player.sitDown();
				
				if (isSellBuff)
					player.setSellingBuffs(true);
				
				final String title = rs.getString("title");
				
				try (PreparedStatement ps = con.prepareStatement(LOAD_OFFLINE_ITEMS))
				{
					ps.setInt(1, player.getObjectId());
					try (ResultSet item = ps.executeQuery())
					{
						switch (type)
						{
							case BUY:
								while (item.next())
									player.getBuyList().addItemByItemId(item.getInt(2), item.getInt(3), item.getInt(4), item.getInt(5));
								
								player.getBuyList().setTitle(title);
								break;
							case SELL:
							case PACKAGE_SELL:
								if (player.isSellingBuffs())
								{
									while (item.next())
										player.getSellingBuffs().add(new SellBuffHolder(item.getInt("item"), item.getInt("count"), item.getInt("price")));
								}
								else
								{
									while (item.next())
										player.getSellList().addItem(item.getInt(2), item.getInt(3), item.getInt(4));
								}
								
								player.getSellList().setTitle(title);
								player.getSellList().setPackaged(type == OperateType.PACKAGE_SELL);
								break;
							case MANUFACTURE:
								while (item.next())
									player.getManufactureList().add(new ManufactureItem(item.getInt(2), item.getInt(4)));
								
								player.getManufactureList().setStoreName(title);
								break;
						}
					}
					
					if (Config.OFFLINE_SLEEP_EFFECT)
					{
						player.startAbnormalEffect(Integer.decode("0x80"));
						player.broadcastUserInfo();
					}
					
					player.setOperateType(type);
					player.restoreEffects();
					player.broadcastUserInfo();
					player.broadcastTitleInfo();
					
					count++;
				}
				catch (Exception e)
				{
					
					LOGGER.warn("Error loading offline {}({}).", e, player.getName(), player.getObjectId());
					player.logout(true);
				}
			}
			
			LOGGER.info("Loaded " + count + " offline.");
			
			try (Statement stm2 = con.createStatement())
			{
				stm2.execute(CLEAR_OFFLINE_TABLE);
				stm2.execute(CLEAR_OFFLINE_TABLE_ITEMS);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Error while loading offline: ", e);
		}
	}
	
	protected OperateType getType(int id)
	{
		for (final OperateType type : OperateType.values())
			if (type.getId() == id)
				return type;
			
		LOGGER.warn("Wrong OperateType id '{}' not found.", id);
		return null;
	}
	
	protected boolean isExpired(long time)
	{
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.add(Calendar.DAY_OF_YEAR, Config.OFFLINE_MAX_DAYS);
		return (cal.getTimeInMillis() <= System.currentTimeMillis());
	}
	
	public static boolean offlineMode(final Player player)
	{
		if (player.isInOlympiadMode() /*|| player.isFestivalParticipant()*/ || player.isInJail() || player.getBoatInfo().getBoat() != null)
			return false;
		
		if (Config.OFFLINE_MODE_IN_PEACE_ZONE && !player.isInsideZone(ZoneId.PEACE))
			return false;
		
		switch (player.getOperateType())
		{
			case SELL:
			case PACKAGE_SELL:
			case BUY:
				return Config.OFFLINE_TRADE_ENABLE;
			case MANUFACTURE:
				return Config.OFFLINE_CRAFT_ENABLE;
		}
		
		return false;
	}
	
	public static final OfflineTradersTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		protected static final OfflineTradersTable INSTANCE = new OfflineTradersTable();
	}
}