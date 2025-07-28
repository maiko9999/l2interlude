package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.Set;

import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExUseSharedGroupItem;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;

public class ItemSkills implements IItemHandler
{
	private static final Set<Integer> SCROLL_ITEM_IDS = Set.of(736, 1538, 1829, 1830, 5858, 5859, 6663, 6664, 7117, 7118, 7119, 7120, 7121, 7122, 7123, 7124, 7125, 7126, 7127, 7128, 7129, 7130, 7131, 7132, 7133, 7134, 7135, 7554, 7555, 7556, 7557, 7558, 7559, 7618, 7619, 9156);
	
	private static final int[] HP_POTION_SKILL_IDS =
	{
		2031, // Lesser Healing Potion
		2032, // Healing potion
		2037 // Greater Healing Potion
	};
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof Servitor)
			return;
		
		final boolean isPet = playable instanceof Pet;
		final Player player = playable.getActingPlayer();
		final Creature target = (playable.getTarget() instanceof Creature targetCreature) ? targetCreature : null;
		
		// Pets can only use tradable items.
		if (isPet && !item.isTradable())
		{
			player.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		
		final IntIntHolder[] skills = item.getEtcItem().getSkills();
		if (skills == null)
		{
			LOGGER.warn("{} doesn't have any registered skill for handler.", item.getName());
			return;
		}
		
		for (final IntIntHolder skillInfo : skills)
		{
			if (skillInfo == null)
				continue;
			
			final L2Skill itemSkill = skillInfo.getSkill();
			if (itemSkill == null)
				continue;
			
			if (!itemSkill.checkCondition(playable, target, false))
				return;
			
			// The skill is currently under reuse.
			if (playable.isSkillDisabled(itemSkill))
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(itemSkill));
				return;
			}
			
			if (SCROLL_ITEM_IDS.contains(item.getItemId()))
			{
				if (CTFEvent.getInstance().isStarted() && CTFEvent.getInstance().onScrollUse(playable.getObjectId()) || DMEvent.getInstance().isStarted() && DMEvent.getInstance().onScrollUse(playable.getObjectId()) || LMEvent.getInstance().isStarted() && LMEvent.getInstance().onScrollUse(playable.getObjectId()) || TvTEvent.getInstance().isStarted() && TvTEvent.getInstance().onScrollUse(playable.getObjectId()))
				{
					playable.sendMessage(player.getSysString(10_078));
					playable.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			if (CTFEvent.getInstance().isStarted() && CTFEvent.getInstance().onPotionUse(playable.getObjectId()) || DMEvent.getInstance().isStarted() && DMEvent.getInstance().onPotionUse(playable.getObjectId()) || LMEvent.getInstance().isStarted() && LMEvent.getInstance().onPotionUse(playable.getObjectId()) || TvTEvent.getInstance().isStarted() && TvTEvent.getInstance().onPotionUse(playable.getObjectId()))
			{
				playable.sendMessage(player.getSysString(10_079));
				playable.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Potions and Energy Stones bypass the AI system. The rest does not.
			if (itemSkill.isPotion() || itemSkill.isSimultaneousCast())
			{
				playable.getCast().doInstantCast(itemSkill, item);
				
				int reuseDelay = itemSkill.getReuseDelay();
				if (item.isEtcItem())
				{
					if (item.getEtcItem().getReuseDelay() > reuseDelay)
						reuseDelay = item.getEtcItem().getReuseDelay();
					
					playable.addTimeStamp(itemSkill, reuseDelay);
					if (reuseDelay != 0)
						playable.disableSkill(itemSkill, reuseDelay);
					
					if (!isPet)
					{
						final int group = item.getEtcItem().getSharedReuseGroup();
						if (group >= 0)
							player.sendPacket(new ExUseSharedGroupItem(item.getItemId(), group, reuseDelay, reuseDelay));
					}
				}
				else if (reuseDelay > 0)
				{
					playable.addTimeStamp(itemSkill, reuseDelay);
					playable.disableSkill(itemSkill, reuseDelay);
				}
				
				if (!isPet && item.isHerb() && player.hasServitor())
					player.getSummon().getCast().doInstantCast(itemSkill, item);
			}
			else
				playable.getAI().tryToCast(target, itemSkill, forceUse, false, (item.isEtcItem() ? item.getObjectId() : 0));
			
			// Send message to owner.
			if (isPet)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addSkillName(itemSkill));
			// Buff icon for healing potions.
			else if (ArraysUtil.contains(HP_POTION_SKILL_IDS, skillInfo.getId()) && skillInfo.getId() >= player.getShortBuffTaskSkillId())
			{
				final EffectTemplate template = itemSkill.getEffectTemplates().get(0);
				if (template != null)
					player.shortBuffStatusUpdate(skillInfo.getId(), skillInfo.getValue(), template.getCounter() * template.getPeriod());
			}
		}
	}
}