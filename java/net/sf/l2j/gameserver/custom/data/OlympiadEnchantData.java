package net.sf.l2j.gameserver.custom.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class OlympiadEnchantData implements IXmlReader
{
	
	private boolean enabled = false;
	private int enchantValue = 6;
	private String enterMessage = "";
	private String exitMessage = "";
	
	private final Map<Integer, Map<Integer, Integer>> enchantBackup = new HashMap<>(); // playerObjId -> (itemObjId -> originalEnchant)
	
	protected OlympiadEnchantData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDataFile("custom/mods/olympiad_enchant_config.xml");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "olympiadEnchant", node ->
		{
			StatSet set = new StatSet(parseAttributes(node));
			enabled = set.getBool("enabled", false);
			enchantValue = set.getInteger("value", 6);
			
			NamedNodeMap attrs = node.getAttributes();
			enterMessage = parseString(attrs, "enterMessage", "Their equipment has been refined to +{enchant} for the Olympiad.");
			exitMessage = parseString(attrs, "exitMessage", "Your equipment has returned to its original refinement.");
		});
	}
	
	public void applyOlympiadEnchant(Player player)
	{
		if (!enabled || player == null)
			return;
		
		Map<Integer, Integer> originalEnchants = new HashMap<>();
		for (ItemInstance item : player.getInventory().getPaperdollItems())
		{
			if (item != null && item.isEquipable())
			{
				originalEnchants.put(item.getObjectId(), item.getEnchantLevel());
				item.setEnchantLevel(enchantValue, player);
			}
		}
		enchantBackup.put(player.getObjectId(), originalEnchants);
		
		player.sendMessage(enterMessage.replace("{enchant}", String.valueOf(enchantValue)));
		player.sendPacket(new ExShowScreenMessage(enterMessage.replace("{enchant}", String.valueOf(enchantValue)), 4000));
		player.broadcastUserInfo();
	}
	
	public void restoreEnchant(Player player)
	{
		if (!enabled || player == null)
			return;
		
		Map<Integer, Integer> backup = enchantBackup.remove(player.getObjectId());
		if (backup == null)
			return;
		
		for (ItemInstance item : player.getInventory().getPaperdollItems())
		{
			if (item != null && item.isEquipable())
			{
				Integer original = backup.get(item.getObjectId());
				if (original != null)
					item.setEnchantLevel(original, player);
			}
		}
		
		player.sendMessage(exitMessage);
		player.sendPacket(new ExShowScreenMessage(exitMessage, 4000));
		player.broadcastUserInfo();
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public static OlympiadEnchantData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final OlympiadEnchantData INSTANCE = new OlympiadEnchantData();
	}
}
