package net.sf.l2j.gameserver.model.itemcontainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ArmorSetListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.BowRodListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.OnEquipListener;
import net.sf.l2j.gameserver.model.trade.BuyProcessItem;
import net.sf.l2j.gameserver.model.trade.SellProcessItem;
import net.sf.l2j.gameserver.model.trade.TradeItem;
import net.sf.l2j.gameserver.model.trade.TradeList;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.taskmanager.ShadowItemTaskManager;

public class PcInventory extends Inventory
{
	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;
	
	private ItemInstance _adena;
	private ItemInstance _ancientAdena;
	
	private int[] _blockItems = null;
	private int _blockMode = -1;
	
	public PcInventory(Player owner)
	{
		super(owner);
		
		addPaperdollListener(ArmorSetListener.getInstance());
		addPaperdollListener(BowRodListener.getInstance());
		addPaperdollListener(ItemPassiveSkillsListener.getInstance());
		addPaperdollListener(ShadowItemTaskManager.getInstance());
	}
	
	@Override
	public Player getOwner()
	{
		return (Player) _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	@Override
	public void equipItem(ItemInstance item)
	{
		if (getOwner().isOperating())
			return;
		
		// Check if player wears formal wear.
		if (getOwner().isWearingFormalWear())
		{
			switch (item.getItem().getBodyPart())
			{
				case Item.SLOT_LEGS, Item.SLOT_FEET, Item.SLOT_GLOVES, Item.SLOT_HEAD:
					unequipItemInBodySlotAndRecord(Item.SLOT_ALLDRESS);
					break;
			}
		}
		
		super.equipItem(item);
	}
	
	@Override
	public void equipPetItem(ItemInstance item)
	{
		// Can't equip item if you are in shop mod.
		if (getOwner().isOperating())
			return;
		
		super.equipPetItem(item);
	}
	
	@Override
	public boolean updateWeight()
	{
		if (!super.updateWeight())
			return false;
		
		// Send StatusUpdate packet.
		final StatusUpdate su = new StatusUpdate(getOwner());
		su.addAttribute(StatusType.CUR_LOAD, _totalWeight);
		getOwner().sendPacket(su);
		
		// Test weight penalty.
		getOwner().refreshWeightPenalty();
		return true;
	}
	
	public ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public int getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public int getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	public List<ItemInstance> getUniqueItems(boolean checkEnchantAndAugment, boolean allowAdena, boolean allowAncientAdena, boolean allowStoreBuy, boolean allowNewbieWeapons)
	{
		final List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (!allowAdena && item.getItemId() == ADENA_ID)
				continue;
			
			if (!allowAncientAdena && item.getItemId() == ANCIENT_ADENA_ID)
				continue;
			
			if (!item.isStackable() && list.stream().anyMatch(i -> i.getItemId() == item.getItemId() && (!checkEnchantAndAugment || (i.getEnchantLevel() == item.getEnchantLevel() && Objects.equals(i.getAugmentation(), item.getAugmentation())))))
				continue;
			
			if (item.isAvailable(getOwner(), allowAdena, (checkEnchantAndAugment && item.isAugmented()) || (allowNewbieWeapons && item.isWeapon() && (item.getWeaponItem().isApprenticeWeapon() || item.getWeaponItem().isTravelerWeapon())) || item.isSellable(), allowStoreBuy))
				list.add(item);
		}
		
		return list;
	}
	
	/**
	 * @param itemId
	 * @return
	 * @see net.sf.l2j.gameserver.model.itemcontainer.PcInventory#getAllItemsByItemId(int, boolean)
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId)
	{
		return getAllItemsByItemId(itemId, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId : ID of item
	 * @param includeEquipped : include equipped items
	 * @return ItemInstance[] : matching items from inventory
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if (item.getItemId() == itemId && (includeEquipped || !item.isEquipped()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * @param itemId
	 * @param enchantment
	 * @return
	 * @see net.sf.l2j.gameserver.model.itemcontainer.PcInventory#getAllItemsByItemId(int, int, boolean)
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId : ID of item
	 * @param enchantment : enchant level of item
	 * @param includeEquipped : include equipped items
	 * @return ItemInstance[] : matching items from inventory
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if ((item.getItemId() == itemId) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowNonTradeable
	 * @param allowStoreBuy
	 * @return ItemInstance : items in inventory
	 */
	public ItemInstance[] getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean allowStoreBuy)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), allowAdena, allowNonTradeable, allowStoreBuy))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * @return a List of all sellable items.
	 */
	public List<ItemInstance> getSellableItems()
	{
		return _items.stream().filter(i -> !i.isEquipped() && i.isSellable() && (getOwner().getSummon() == null || i.getObjectId() != getOwner().getSummon().getControlItemId())).toList();
	}
	
	/**
	 * @return A {@link List} of {@link TradeItem}s based on {@link ItemInstance}s available for transaction from this {@link PcInventory}, adjusted by the sellable {@link TradeList}.
	 */
	public List<TradeItem> getItemsToSell()
	{
		final TradeList tradeList = getOwner().getSellList();
		if (tradeList == null)
			return Collections.emptyList();
		
		final List<TradeItem> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item.isAvailable(getOwner(), false, false, false))
			{
				int count = 0;
				
				// Don't bother trying to compute anything if tested SellList is empty. Simply return available count.
				if (tradeList.isEmpty())
					count = item.getCount();
				// If the item is stackable, check by itemId. If it's found, compare amounts and use the difference, otherwise use item count.
				else if (item.isStackable())
				{
					final TradeItem tradeItem = tradeList.stream().filter(ti -> ti.getItem().getItemId() == item.getItemId()).findFirst().orElse(null);
					count = (tradeItem != null) ? (item.getCount() - tradeItem.getCount()) : item.getCount();
				}
				// If the item is unique, check if already in sell list using its objectId.
				else if (tradeList.stream().noneMatch(ti -> ti.getObjectId() == item.getObjectId()))
					count = item.getCount();
				
				// If some amount was found, we add an entry.
				if (count > 0)
					list.add(new TradeItem(item, count, item.getReferencePrice()));
			}
		}
		return list;
	}
	
	/**
	 * Adjust the {@link TradeItem} set as parameter according its status.
	 * @param item : The {@link TradeItem} to adjust.
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		// For all ItemInstance with same item id.
		for (ItemInstance adjItem : getItemsByItemId(item.getItem().getItemId()))
		{
			// If enchant level is different, bypass.
			if (adjItem.getEnchantLevel() != item.getEnchant())
				continue;
			
			// If item isn't equipable, or equipable but not equiped it is a success.
			if (!adjItem.isEquipable() || !adjItem.isEquipped())
			{
				item.setObjectId(adjItem.getObjectId());
				item.setEnchant(adjItem.getEnchantLevel());
				item.setCount(Math.min(adjItem.getCount(), item.getQuantity()));
				return;
			}
		}
		// None item matched conditions ; return as invalid count.
		item.setCount(0);
	}
	
	/**
	 * Add Adena to this {@link PcInventory}.
	 * @param count : The quantity of Adena to add.
	 */
	public void addAdena(int count)
	{
		if (count > 0)
			addItem(ADENA_ID, count);
	}
	
	/**
	 * Remove Adena from this {@link PcInventory}.
	 * @param count : The quantity of Adena to remove.
	 * @return True if the operation is successful, or false otherwise.
	 */
	public boolean reduceAdena(int count)
	{
		if (count > 0)
			return destroyItemByItemId(ADENA_ID, count) != null;
		
		return false;
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param count : int Quantity of adena to be added
	 */
	public void addAncientAdena(int count)
	{
		if (count > 0)
			addItem(ANCIENT_ADENA_ID, count);
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param count : int Quantity of adena to be removed
	 * @return true if successful.
	 */
	public boolean reduceAncientAdena(int count)
	{
		if (count > 0)
			return destroyItemByItemId(ANCIENT_ADENA_ID, count) != null;
		
		return false;
	}
	
	@Override
	public ItemInstance addItem(ItemInstance item)
	{
		item = super.addItem(item);
		if (item == null)
			return null;
		
		if (item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		else if (item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		
		return item;
	}
	
	@Override
	public ItemInstance addItem(int itemId, int count)
	{
		ItemInstance item = super.addItem(itemId, count);
		if (item == null)
			return null;
		
		if (item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		else if (item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		
		return item;
	}
	
	@Override
	public ItemInstance transferItem(int objectId, int count, ItemContainer target)
	{
		ItemInstance item = super.transferItem(objectId, count, target);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	@Override
	public ItemInstance destroyItem(ItemInstance item)
	{
		return destroyItem(item, item.getCount());
	}
	
	@Override
	public ItemInstance destroyItem(ItemInstance item, int count)
	{
		item = super.destroyItem(item, count);
		
		if (_adena != null && _adena.getCount() <= 0)
			_adena = null;
		
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
			_ancientAdena = null;
		
		return item;
	}
	
	@Override
	public ItemInstance destroyItem(int objectId, int count)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
			return null;
		
		return destroyItem(item, count);
	}
	
	@Override
	public ItemInstance destroyItemByItemId(int itemId, int count)
	{
		ItemInstance item = getItemByItemId(itemId);
		if (item == null)
			return null;
		
		return destroyItem(item, count);
	}
	
	@Override
	public ItemInstance dropItem(ItemInstance item)
	{
		item = super.dropItem(item);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	@Override
	public ItemInstance dropItem(int objectId, int count)
	{
		ItemInstance item = super.dropItem(objectId, count);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	@Override
	protected boolean removeItem(ItemInstance item, boolean isDrop)
	{
		if (!super.removeItem(item, isDrop))
			return false;
		
		// Delete all existing shortcuts refering to this object id.
		getOwner().getShortcutList().deleteShortcuts(item.getObjectId(), ShortcutType.ITEM);
		
		// Removes active Enchant Scroll
		if (item.equals(getOwner().getActiveEnchantItem()))
			getOwner().setActiveEnchantItem(null);
		
		if (item.getItemId() == ADENA_ID)
			_adena = null;
		else if (item.getItemId() == ANCIENT_ADENA_ID)
			_ancientAdena = null;
		
		return true;
	}
	
	@Override
	public void restore()
	{
		super.restore();
		
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	@Override
	public ItemInstance unequipItemInBodySlot(int slot)
	{
		final ItemInstance old = super.unequipItemInBodySlot(slot);
		if (old != null)
			getOwner().refreshExpertisePenalty();
		
		return old;
	}
	
	public boolean validateCapacity(ItemInstance item)
	{
		int slots = 0;
		if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != EtcItemType.HERB)
			slots++;
		
		if (item.getItemId() == ADENA_ID && (Integer.MAX_VALUE - _owner.getInventory().getAdena() - item.getCount()) < 0)
			return false;
		
		return validateCapacity(slots);
	}
	
	public boolean validateCapacityByItemId(IntIntHolder holder)
	{
		return validateCapacityByItemId(holder.getId(), holder.getValue());
	}
	
	public boolean validateCapacityByItemId(int itemId, int itemCount)
	{
		return validateCapacity(calculateUsedSlots(itemId, itemCount));
	}
	
	public boolean validateCapacityByItemIds(List<IntIntHolder> holders)
	{
		int slots = 0;
		for (IntIntHolder holder : holders)
			slots += calculateUsedSlots(holder.getId(), holder.getValue());
		
		return validateCapacity(slots);
	}
	
	/**
	 * @param tradeList : The {@link TradeList} to test.
	 * @return True if the {@link TradeList} set as parameter can pass a {@link #validateCapacity(int)} check.
	 */
	public boolean validateTradeListCapacity(TradeList tradeList)
	{
		int slots = 0;
		for (TradeItem tradeItem : tradeList)
		{
			slots += calculateUsedSlots(tradeItem.getItem(), tradeItem.getCount());
			if (tradeItem.getItem().getItemId() == ADENA_ID && (Integer.MAX_VALUE - _owner.getInventory().getAdena() - tradeItem.getCount()) < 0)
				return false;
		}
		
		return validateCapacity(slots);
	}
	
	/**
	 * @param template : The {@link Item} to test.
	 * @param itemCount : The {@link Item} count to add.
	 * @return The number of used slots for a given {@link Item}.
	 */
	private int calculateUsedSlots(Item template, int itemCount)
	{
		final ItemInstance item = getItemByItemId(template.getItemId());
		if (item != null)
			return (item.isStackable()) ? 0 : itemCount;
		
		return (template.isStackable()) ? 1 : itemCount;
	}
	
	/**
	 * @param itemId : The {@link Item} id to test.
	 * @param itemCount : The {@link Item} count to add.
	 * @return The number of used slots for a given {@link Item} id.
	 */
	private int calculateUsedSlots(int itemId, int itemCount)
	{
		final ItemInstance item = getItemByItemId(itemId);
		if (item != null)
			return (item.isStackable()) ? 0 : itemCount;
		
		final Item template = ItemData.getInstance().getTemplate(itemId);
		return (template.isStackable()) ? 1 : itemCount;
	}
	
	@Override
	public boolean validateCapacity(int slotCount)
	{
		if (slotCount == 0)
			return true;
		
		return (_items.size() + slotCount <= getOwner().getStatus().getInventoryLimit());
	}
	
	@Override
	public boolean validateWeight(int weight)
	{
		return _totalWeight + weight <= _owner.getWeightLimit();
	}
	
	/**
	 * @param tradeList : The {@link TradeList} to test.
	 * @return True if the {@link TradeList} set as parameter can pass a {@link #validateWeight(int)} check.
	 */
	public boolean validateTradeListWeight(TradeList tradeList)
	{
		int weight = 0;
		for (TradeItem tradeItem : tradeList)
			weight += tradeItem.getItem().getWeight() * tradeItem.getCount();
		
		return validateWeight(weight);
	}
	
	/**
	 * @param tradeList : The {@link TradeList} to test.
	 * @return True if each item count from the {@link TradeList} set as parameter can pass an Integer.MAX_VALUE check, or false otherwise.
	 */
	public boolean validateTradeListCount(TradeList tradeList)
	{
		for (TradeItem tradeItem : tradeList)
		{
			long count = tradeItem.getCount();
			
			if (tradeItem.getItem().isStackable())
			{
				final ItemInstance item = getItemByItemId(tradeItem.getItem().getItemId());
				if (item != null)
					count += item.getCount();
			}
			
			if (count > Integer.MAX_VALUE)
				return false;
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	/**
	 * @param itemsToCheck : The {@link BuyProcessItem} array to test.
	 * @return True if the {@link BuyProcessItem} array set as parameter successfully pass inventory checks, false otherwise.
	 */
	public boolean canPassBuyProcess(BuyProcessItem[] itemsToCheck)
	{
		for (BuyProcessItem itemToCheck : itemsToCheck)
		{
			if (itemToCheck.getCount() < 1 || itemToCheck.getPrice() < 0)
				return false;
			
			final ItemInstance item = getItemByItemId(itemToCheck.getItemId());
			if (item == null || item.getEnchantLevel() != itemToCheck.getEnchant())
				return false;
		}
		return true;
	}
	
	/**
	 * @param itemsToCheck : The {@link SellProcessItem} array to test.
	 * @return True if the {@link SellProcessItem} array set as parameter successfully pass inventory checks, false otherwise.
	 */
	public boolean canPassSellProcess(SellProcessItem[] itemsToCheck)
	{
		for (SellProcessItem itemToCheck : itemsToCheck)
		{
			if (itemToCheck.getCount() < 1 || itemToCheck.getPrice() < 0)
				return false;
			
			final ItemInstance item = getItemByObjectId(itemToCheck.getObjectId());
			if (item == null || item.getCount() < itemToCheck.getCount())
				return false;
		}
		return true;
	}
	
	/**
	 * Re-notify to paperdoll listeners every equipped item
	 */
	public void reloadEquippedItems()
	{
		for (ItemInstance item : getPaperdollItems())
		{
			final Paperdoll slot = Paperdoll.getEnumById(item.getLocationSlot());
			
			for (OnEquipListener listener : _paperdollListeners)
			{
				listener.onUnequip(slot, item, getOwner());
				listener.onEquip(slot, item, getOwner());
			}
		}
	}
	
	/**
	 * Set inventory block for specified IDs<br>
	 * array reference is used for {@link PcInventory#_blockItems}
	 * @param items array of Ids to block/allow
	 * @param mode blocking mode {@link PcInventory#_blockMode}
	 */
	public void setInventoryBlock(int[] items, int mode)
	{
		_blockMode = mode;
		_blockItems = items;
		
		_owner.sendPacket(new ItemList((Player) _owner, false));
	}
	
	/**
	 * Unblock blocked itemIds
	 */
	public void unblock()
	{
		_blockMode = -1;
		_blockItems = null;
		
		_owner.sendPacket(new ItemList((Player) _owner, false));
	}
	
	public void blockAllItems()
	{
		setInventoryBlock(new int[]
		{
			(ItemData.getInstance().getArraySize() + 2)
		}, 1);
	}
	
	/**
	 * Check if player can use item by itemid
	 * @param itemId int
	 * @return true if can use
	 */
	public boolean canManipulateWithItemId(int itemId)
	{
		if ((_blockMode == 0 && ArraysUtil.contains(_blockItems, itemId)) || (_blockMode == 1 && !ArraysUtil.contains(_blockItems, itemId)))
			return false;
		return true;
	}
}