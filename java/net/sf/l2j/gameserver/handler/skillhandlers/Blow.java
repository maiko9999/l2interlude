package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.skills.ShieldDefense;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Blow implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.BLOW
	};
	
	@Override
	public void useSkill(Creature creature, L2Skill skill, WorldObject[] targets, ItemInstance item)
	{
		if (creature.isAlikeDead())
			return;
		
		final boolean ss = creature.isChargedShot(ShotType.SOULSHOT);
		
		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature targetCreature))
				continue;
			
			if (targetCreature.isAlikeDead())
				continue;
			
			if (Formulas.calcBlowRate(creature, targetCreature, skill))
			{
				// Calculate skill evasion.
				if (Formulas.calcPhysicalSkillEvasion(targetCreature, skill))
				{
					if (creature instanceof Player player)
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(targetCreature));
					
					if (targetCreature instanceof Player targetPlayer)
						targetPlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(creature));
					
					continue;
				}
				
				final boolean isCrit = skill.getBaseCritRate() > 0 && Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(creature));
				final ShieldDefense sDef = Formulas.calcShldUse(creature, targetCreature, skill, isCrit);
				final byte reflect = Formulas.calcSkillReflect(targetCreature, skill);
				
				if (skill.hasEffects())
				{
					if (reflect == Formulas.SKILL_REFLECT_SUCCEED)
					{
						creature.stopSkillEffects(skill.getId());
						
						skill.getEffects(targetCreature, creature);
					}
					else
					{
						targetCreature.stopSkillEffects(skill.getId());
						
						if (Formulas.calcSkillSuccess(creature, targetCreature, skill, sDef, true))
							skill.getEffects(creature, targetCreature, sDef, false);
						else
							creature.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(targetCreature).addSkillName(skill));
					}
				}
				
				double damage = (int) Formulas.calcBlowDamage(creature, targetCreature, skill, sDef, ss);
				creature.sendPacket(new PlaySound("skillsound.critical_hit_02"));
				
				if (isCrit)
					damage *= 2;
				
				// Skill counter.
				if ((reflect & Formulas.SKILL_COUNTER) != 0)
				{
					if (targetCreature instanceof Player targetPlayer)
						targetPlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(creature));
					
					if (creature instanceof Player player)
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(targetCreature));
					
					// Calculate the counter percent.
					final double counteredPercent = targetCreature.getStatus().calcStat(Stats.COUNTER_SKILL_PHYSICAL, 0, targetCreature, null) / 100.;
					
					damage *= counteredPercent;
					
					// Reduce caster HPs.
					creature.reduceCurrentHp(damage, targetCreature, skill);
					
					// Send damage message.
					targetCreature.sendDamageMessage(creature, (int) damage, false, true, false);
				}
				else
				{
					// Manage cast break of the target (calculating rate, sending message...)
					Formulas.calcCastBreak(targetCreature, damage);
					
					// Reduce target HPs.
					targetCreature.reduceCurrentHp(damage, creature, skill);
					
					// Send damage message.
					creature.sendDamageMessage(targetCreature, (int) damage, false, true, false);
				}
				
				creature.setChargedShot(ShotType.SOULSHOT, skill.isStaticReuse());
			}
			
			// Possibility of a lethal strike.
			Formulas.calcLethalHit(creature, targetCreature, skill);
			
			if (skill.hasSelfEffects())
			{
				final AbstractEffect effect = creature.getFirstEffect(skill.getId());
				if (effect != null && effect.isSelfEffect())
					effect.exit();
				
				skill.getEffectsSelf(creature);
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}