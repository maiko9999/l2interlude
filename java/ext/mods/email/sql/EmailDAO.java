package ext.mods.email.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.Augmentation;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.skills.L2Skill;

public class EmailDAO
{
	public static boolean isPending(int emailId)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 1 FROM player_emails WHERE email_id=? AND status='PENDING'"))
		{
			ps.setInt(1, emailId);
			try (ResultSet rs = ps.executeQuery())
			{
				return rs.next();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static void expireAndReturnToSender(int emailId)
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE player_emails SET status='EXPIRED' WHERE email_id=? AND status='PENDING'"))
			{
				ps.setInt(1, emailId);
				ps.executeUpdate();
			}
			
			try (PreparedStatement ps = con.prepareStatement("SELECT sender_id, item_object_id, item_id, count, enchant_level, is_augmented, augment_id FROM player_emails WHERE email_id=?"))
			{
				ps.setInt(1, emailId);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						int senderId = rs.getInt("sender_id");
						int oldObjectId = rs.getInt("item_object_id");
						int itemId = rs.getInt("item_id");
						int count = rs.getInt("count");
						int enchant = rs.getInt("enchant_level");
						boolean isAug = rs.getBoolean("is_augmented");
						int augmentId = rs.getInt("augment_id");
						
						int newobjectId = IdFactory.getInstance().getNextId();
						
						// Online
						Player player = World.getInstance().getPlayer(senderId);
						if (player != null)
						{
							
							ItemInstance item = player.addItem(itemId, count, true);
							if (enchant > 0)
								item.setEnchantLevel(enchant, player);
							
							
							
							if (isAug)
							{
								try (PreparedStatement updateAug = con.prepareStatement("UPDATE augmentations SET item_oid=? WHERE item_oid=?"))
								{
									updateAug.setInt(1, newobjectId);
									updateAug.setInt(2, oldObjectId);
									updateAug.executeUpdate();

									int attributes = 0;
									int skillId = 0;
									int skillLevel = 1;

									// CORRIGIDO: buscar o registro atualizado com o novo ID
									try (PreparedStatement sel = con.prepareStatement("SELECT attributes, skill_id, skill_level FROM augmentations WHERE item_oid=?"))
									{
										sel.setInt(1, newobjectId); // aqui estava o erro
										try (ResultSet rs2 = sel.executeQuery())
										{
											if (rs2.next())
											{
												attributes = rs2.getInt("attributes");
												skillId = rs2.getInt("skill_id");
												skillLevel = rs2.getInt("skill_level");
											}
										}
									}

									int fullAugmentId = attributes | (skillId << 16);
									L2Skill skill = (skillId > 0) ? SkillTable.getInstance().getInfo(skillId, skillLevel) : null;
									item.setAugmentation(new Augmentation(fullAugmentId, skill), player);
								}
							}

							
							player.sendMessage("Um item expirado do correio foi devolvido para seu inventário.");
						}
						
						else
						{
							
							try (PreparedStatement check = con.prepareStatement("SELECT count FROM items WHERE object_id = ? AND owner_id = ? AND item_id = ? AND loc = 'INVENTORY'"))
							{
								check.setInt(1, oldObjectId);
								check.setInt(2, senderId);
								check.setInt(3, itemId);
								
								try (ResultSet rsc = check.executeQuery())
								{
									if (rsc.next())
									{
										
										int currentCount = rsc.getInt("count");
										int newCount = currentCount + count;
										
										try (PreparedStatement updateItem = con.prepareStatement("UPDATE items SET count = ? WHERE object_id = ?"))
										{
											updateItem.setInt(1, newCount);
											updateItem.setInt(2, oldObjectId);
											updateItem.executeUpdate();
										}
										
										if (isAug)
										{
											try (PreparedStatement checkAug = con.prepareStatement("SELECT item_oid FROM augmentations WHERE item_oid = ?"))
											{
												checkAug.setInt(1, oldObjectId);
												try (ResultSet rsAug = checkAug.executeQuery())
												{
													if (!rsAug.next())
													{
														// Se não tiver augment, insere
														int attributes = (augmentId & 0xFFFF);
														int skillId = ((augmentId >> 16) & 0xFFFF);
														int skillLevel = 1;
														
														try (PreparedStatement insertAug = con.prepareStatement("INSERT INTO augmentations (item_oid, attributes, skill_id, skill_level) VALUES (?, ?, ?, ?)"))
														{
															insertAug.setInt(1, oldObjectId);
															insertAug.setInt(2, attributes);
															insertAug.setInt(3, skillId);
															insertAug.setInt(4, skillLevel);
															insertAug.executeUpdate();
														}
													}
												}
											}
										}
									}
									else
									{
										
										try (PreparedStatement insertItem = con.prepareStatement("INSERT INTO items (owner_id, object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time) VALUES (?, ?, ?, ?, ?, 'INVENTORY', 0, 0, 0, -1, 0)"))
										{
											insertItem.setInt(1, senderId);
											insertItem.setInt(2, newobjectId);
											insertItem.setInt(3, itemId);
											insertItem.setInt(4, count);
											insertItem.setInt(5, enchant);
											insertItem.executeUpdate();
										}
										
										if (isAug)
										{
											try (PreparedStatement updateAug = con.prepareStatement("UPDATE augmentations SET item_oid=? WHERE item_oid=?"))
											{
												updateAug.setInt(1, newobjectId);
												updateAug.setInt(2, oldObjectId);
												int updated = updateAug.executeUpdate();
												
												if (updated == 0)
												{
													// fallback: inserir augment manualmente
													int attributes = (augmentId & 0xFFFF);
													int skillId = ((augmentId >> 16) & 0xFFFF);
													int skillLevel = 1;
													
													try (PreparedStatement insertAug = con.prepareStatement("INSERT INTO augmentations (item_oid, attributes, skill_id, skill_level) VALUES (?, ?, ?, ?)"))
													{
														insertAug.setInt(1, newobjectId);
														insertAug.setInt(2, attributes);
														insertAug.setInt(3, skillId);
														insertAug.setInt(4, skillLevel);
														insertAug.executeUpdate();
													}
												}
											}
										}
									}
								}
							}
							
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}