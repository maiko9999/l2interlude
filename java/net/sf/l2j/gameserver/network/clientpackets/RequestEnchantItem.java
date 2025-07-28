package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.custom.data.EnchantData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.ArmorSetData;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.actors.MissionType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.player.MissionList;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.records.custom.EnchantScroll;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SkillList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public final class RequestEnchantItem extends L2GameClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null || _objectId == 0)
			return;
		
		if (!player.isOnline() || getClient().isDetached())
		{
			player.setActiveEnchantItem(null);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isOperating())
		{
			player.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		ItemInstance scroll = player.getActiveEnchantItem();
		
		if (item == null || scroll == null)
		{
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		// template for scroll
		final EnchantScroll enchant = EnchantData.getInstance().getEnchantScroll(scroll);
		if (enchant == null)
			return;
		
		// first validation check
		if (!isEnchantable(item) || !enchant.isValid(item) || item.getOwnerId() != player.getObjectId())
		{
			player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		// attempting to destroy scroll
		scroll = player.getInventory().destroyItem(scroll.getObjectId(), 1);
		if (scroll == null)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.cancelActiveTrade();
			player.sendPacket(SystemMessageId.TRADE_ATTEMPT_FAILED);
			return;
		}
		
		final MissionList mission = player.getMissions();
		synchronized (item)
		{
			double chance = enchant.getChance(item);
			
			// last validation check
			if (item.getOwnerId() != player.getObjectId() || !isEnchantable(item) || chance < 0)
			{
				player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
				player.setActiveEnchantItem(null);
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			// success
			if (Rnd.get(100) < enchant.getChance(item))
			{
				player.sendPacket(item.getEnchantLevel() == 0 ? SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED).addItemName(item.getItemId()) : SystemMessage.getSystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
				
				int newEnchant = item.getEnchantLevel() + enchant.enchantAmount();
				item.setEnchantLevel(newEnchant, player);
				
				// If item is equipped, verify the skill obtention (+4 duals, +6 armorset).
				if (item.isEquipped())
				{
					final Item it = item.getItem();
					
					// Add skill bestowed by +4 duals.
					if (it instanceof Weapon weapon && item.getEnchantLevel() == 4)
					{
						final L2Skill enchant4Skill = weapon.getEnchant4Skill();
						if (enchant4Skill != null)
						{
							player.addSkill(enchant4Skill, false);
							player.sendPacket(new SkillList(player));
						}
					}
					// Add skill bestowed by +6 armorset.
					else if (it instanceof Armor && item.getEnchantLevel() == 6)
					{
						// Checks if player is wearing a chest item
						final int chestId = player.getInventory().getItemIdFrom(Paperdoll.CHEST);
						if (chestId != 0)
						{
							final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestId);
							if (armorSet != null && armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
							{
								final int skillId = armorSet.getEnchant6skillId();
								if (skillId > 0)
								{
									final L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
									if (skill != null)
									{
										player.addSkill(skill, false);
										player.sendPacket(new SkillList(player));
									}
								}
							}
						}
					}
				}
				
				player.sendPacket(EnchantResult.SUCCESS);
				
				final MissionType type = item.isWeapon() ? MissionType.ENCHANT_WEAPON : MissionType.ENCHANT_OTHER;
				if (mission.getMission(type).getValue() < item.getEnchantLevel())
					mission.set(type, item.getEnchantLevel(), false, false);
				
				mission.update(MissionType.ENCHANT_SUCCESS);
				
				player.sendPacket(new ItemList(player, false));
				if (enchant.announceTheEnchant(item) && enchant.message())
					World.announceToOnlinePlayers(player.getSysString(10_060, player.getName(), item.getEnchantLevel(), item.getName()));
			}
			else
			{
				// Drop passive skills from items.
				if (item.isEquipped() && (enchant.cristalize() || enchant.returnVal() != -1))
				{
					final Item it = item.getItem();
					
					// Remove skill bestowed by +4 duals.
					if (it instanceof Weapon weapon && item.getEnchantLevel() >= 4)
					{
						final L2Skill enchant4Skill = weapon.getEnchant4Skill();
						if (enchant4Skill != null)
						{
							player.removeSkill(enchant4Skill.getId(), false);
							player.sendPacket(new SkillList(player));
						}
					}
					// Add skill bestowed by +6 armorset.
					else if (it instanceof Armor && item.getEnchantLevel() >= 6)
					{
						// Checks if player is wearing a chest item
						final int chestId = player.getInventory().getItemIdFrom(Paperdoll.CHEST);
						if (chestId != 0)
						{
							final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestId);
							if (armorSet != null && armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
							{
								final int skillId = armorSet.getEnchant6skillId();
								if (skillId > 0)
								{
									player.removeSkill(skillId, false);
									player.sendPacket(new SkillList(player));
								}
							}
						}
					}
				}
				
				if (!enchant.cristalize())
				{
					
					if (enchant.returnOnFail() != -1)
					{
						player.sendMessage("Failed in Enchant. The enchant value of the item became " + enchant.returnOnFail() + ".");
						item.setEnchantLevel(enchant.returnOnFail(), player);
					}
					
					player.sendPacket(EnchantResult.UNSUCCESS);
				}
				else
				{
					// enchant failed, destroy item
					int crystalId = item.getItem().getCrystalItemId();
					int count = item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2;
					if (count < 1)
						count = 1;
					
					final ItemInstance destroyItem = player.getInventory().destroyItem(item);
					if (destroyItem == null)
					{
						player.setActiveEnchantItem(null);
						player.sendPacket(EnchantResult.CANCELLED);
						return;
					}
					
					if (crystalId != 0)
					{
						player.getInventory().addItem(crystalId, count);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(crystalId).addItemNumber(count));
					}
					
					// Messages.
					if (item.getEnchantLevel() > 0)
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_S2_EVAPORATED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
					else
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_EVAPORATED).addItemName(item.getItemId()));
					
					player.sendPacket((crystalId == 0) ? EnchantResult.UNK_RESULT_4 : EnchantResult.UNK_RESULT_1);
				}
				mission.update(MissionType.ENCHANT_FAILED);
			}
			
			player.broadcastUserInfo();
			player.setActiveEnchantItem(null);
		}
	}
	
	protected static final boolean isEnchantable(ItemInstance item)
	{
		// Hero, shadow, EtcItem and fishing rods can't be enchanted.
		if (item.isHeroItem() || item.isShadowItem() || item.isEtcItem() || item.getItem().getItemType() == WeaponType.FISHINGROD || !item.isEnchantable())
			return false;
		
		// only equipped items or in inventory can be enchanted.
		if (item.getLocation() != ItemLocation.INVENTORY && item.getLocation() != ItemLocation.PAPERDOLL)
			return false;
		
		// Traveler weapons can't be enchanted.
		if (item.isWeapon())
			return !item.getWeaponItem().isTravelerWeapon();
		
		return true;
	}
}