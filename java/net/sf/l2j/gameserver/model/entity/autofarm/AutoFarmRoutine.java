package net.sf.l2j.gameserver.model.entity.autofarm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.entity.autofarm.AutoFarmManager.AutoFarmType;
import net.sf.l2j.gameserver.model.entity.autofarm.zone.AutoFarmArea;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.extractable.ExtractableProductItem;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillCreateItem;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

public class AutoFarmRoutine
{
	private final AutoFarmProfile _autoFarmProfile;
	private final Map<String, Integer> _cachedMessages = new HashMap<>();
	private boolean _noAttackSkillItems;
	private int _skillAttackFailCount;
	
	public AutoFarmRoutine(AutoFarmProfile autoFarmProfile)
	{
		_autoFarmProfile = autoFarmProfile;
	}
	
	public synchronized void start()
	{
		if (!_autoFarmProfile.isEnabled())
			return;
		
		if (!Config.AUTOFARM_ENABLED)
		{
			stop("System cannot be used now");
			return;
		}
		
		final Player player = _autoFarmProfile.getPlayer();
		if (player.isDead())
		{
			stop("Your character is dead");
			return;
		}
		
		if (_skillAttackFailCount >= 20)
		{
			stop("Your character cannot attack anymore");
			return;
		}
		
		if (_autoFarmProfile.getEndTime() != 0 && _autoFarmProfile.getFinalEndTime() < System.currentTimeMillis())
		{
			// If still attacking, it will stop as soon as the monster is killed
			if (!isPlayerAttacking(player))
				stop("Scheduled duration has concluded");
			
			return;
		}
		
		run(player);
	}
	
	public synchronized void stop(String msg)
	{
		_skillAttackFailCount = 0;
		_cachedMessages.clear();
		AutoFarmManager.getInstance().stopPlayer(_autoFarmProfile, msg);
		onEnd();
	}
	
	private synchronized void run(Player player)
	{
		final AutoFarmArea area = _autoFarmProfile.getSelectedArea();
		
		// Remove invalid targets
		if (!(player.getTarget() instanceof Monster monster) || monster.isDead() || (area.getType() == AutoFarmType.ZONA && !monster.isInsideZone(ZoneId.AUTO_FARM)))
			player.setTarget(null);
		
		ZoneBuilder.getInstance().clearAllPreview(player);
		
		// Nothing to do
		if (player.isSleeping() || player.isStunned() || player.isImmobilized())
		{
			sendAdminMessage(player, "Immobilized");
			return;
		}
		
		// High priority because many actions depend on them
		if (_autoFarmProfile.useAutoPotion())
		{
			testHpPotions(player);
			testMpPotions(player);
		}
		
		// Before starting to attack, we prepare
		if (testHealSkill(player) || testBuffSkill(player))
		{
			sendAdminMessage(player, "Healing");
			return;
		}
		
		if (player.getAI().getCurrentIntention().getType() == IntentionType.PICK_UP && player.getMove().getTask() != null)
		{
			sendAdminMessage(player, "Picking Herbs");
			return;
		}
		
		// Not inside the zone. Possible scenarios:
		// Character may be on the way to the area
		// Attacking a monster from outside the area
		// The player forced the character's movement
		if (area.getType() == AutoFarmType.ZONA && !isPlayerAttacking(player))
		{
			// If they went out to attack we allow it, may have done it because it's the shortest path
			// But if they went out and have no intention to attack, then they should return
			// It's not interesting to always force a return because the new destination may not be interesting, so it would depend on further analysis
			if (!player.isInsideZone(ZoneId.AUTO_FARM))
			{
				if (area.getFarmZone().tryGoBackInside())
					sendMessage(player, "returning to the interior of the zone");
				else
				{
					// Couldn't do anything
					stop("Character outside the zone");
				}
				
				return;
			}
			
			if (area.isOwnerNearEdge(50) && area.getFarmZone().tryGoBackInside())
				sendMessage(player, "returning to the interior of the zone");
		}
		else if (area.getType() == AutoFarmType.ROTA)
		{
			// First we must reach the path of the route
			if (!area.getRouteZone().reachedFirstNode())
			{
				area.getRouteZone().moveToNextPoint();
				return;
			}
			
			// Now we can go back
			if (!area.isOwnerNearEdge(50))
			{
				area.getRouteZone().moveToNextPoint();
				return;
			}
		}
		
		// If we don't pick the herb first, it will disappear
		if (_autoFarmProfile.pickHerbs() && tryPickUpHerbs(player))
			return;
		
		Monster currentTarget = (Monster) player.getTarget();
		
		if (_autoFarmProfile.attackSummon())
		{
			if (player.getSummon() != null && currentTarget != null)
			{
				player.getSummon().setTarget(currentTarget);
				player.getSummon().getAI().tryToAttack(currentTarget);
			}
		}
		else if (player.getSummon() != null)
			player.getSummon().getAttack().stop();
		
		if (!isPlayerAttacking(player) && (player.getAI().getCurrentIntention().getType() != IntentionType.ATTACK || !GeoEngine.getInstance().canMoveAround(player.getX(), player.getY(), player.getZ())))
		{
			if (currentTarget != null && !currentTarget.isDead() && !player.isMageClass())
			{
				if ((player.distance3D(currentTarget) > _autoFarmProfile.getAttackRange() && GeoEngine.getInstance().canSeeTarget(player, currentTarget) && GeoEngine.getInstance().canMoveToTarget(player, currentTarget)))
				{
					if (player.getMove().maybeMoveToLocation(currentTarget.getPosition().clone(), 50, true, false))
						sendAdminMessage(player, "Following target: " + currentTarget.getName());
				}
				else
					sendAdminMessage(player, "Target is in range: " + currentTarget.getName());
				
				sendAdminMessage(player, "Continuing to attack: " + currentTarget.getName());
				player.getAI().tryToAttack(currentTarget);
				return;
			}
			
			for (Monster monster : getTarget())
			{
				if (monster.getAI().getAggroList().getHate(player) > 0 || player.distance3D(monster) < (_autoFarmProfile.getAttackRange() + (player.isMageClass() ? 0 : 150)) && GeoEngine.getInstance().canSeeTarget(player, monster) && GeoEngine.getInstance().canMoveToTarget(player, monster))
				{
					sendAdminMessage(player, "New target: " + monster.getName());
					player.setTarget(monster);
					
					if (player.isMageClass())
						break;
				}
				
				if (!area.isMovementAllowed())
					continue;
				
				// We haven't found a new target and we are already on route
				if (player.getMove().getTask() != null)
					continue;
				
				if (player.getMove().maybeMoveToLocation(new Location(monster.getX(), monster.getY(), monster.getZ()), 0, true, false))
				{
					sendAdminMessage(player, "Needs movement");
					break;
				}
			}
		}
		
		// We need to check if the target is visible again because it may have moved
		if (player.getTarget() != null && player.distance3D(player.getTarget().getPosition()) <= _autoFarmProfile.getAttackRange() && GeoEngine.getInstance().canSeeTarget(player, player.getTarget()))
		{
			final Monster target = player.getTarget().getMonster();
			final L2Skill skill = getCastSkill(player);
			
			if (skill != null) // && not casting?
			{
				player.getAI().tryToCast(target, skill);
				sendAdminMessage(player, "Casting");
			}
			else if (!player.isMageClass() || (player.isMageClass() && _autoFarmProfile.getAttackSkills().isEmpty()))
			{
				player.getAI().tryToAttack(target);
				sendAdminMessage(player, "Attacking");
			}
			else if (_noAttackSkillItems)
			{
				_skillAttackFailCount++;
				sendAdminMessage(player, "Skill cast fail " + _skillAttackFailCount);
			}
			
			_cachedMessages.clear();
		}
		else if (player.getMove().getTask() == null && !player.isRooted() && !isPlayerAttacking(player))
		{
			if (area.getType() == AutoFarmType.ROTA)
			{
				area.getRouteZone().moveToNextPoint();
				return;
			}
			
			if (player.getTarget() == null && player.getAI().getCurrentIntention().getType() == IntentionType.IDLE && player.getAI().getNextIntention().getType() == IntentionType.IDLE)
				trySendMessage(player, "waiting for new monsters to spawn.");
		}
	}
	
	private synchronized void onEnd()
	{
		if (_autoFarmProfile.getMacro() == null)
			return;
		
		final Player player = _autoFarmProfile.getPlayer();
		if (player.isInCombat())
		{
			AttackStanceTaskManager.getInstance().remove(player);
			player.broadcastPacket(new AutoAttackStop(player.getObjectId()));
		}
		
		switch (_autoFarmProfile.getMacro())
		{
			case ESCAPE:
				player.getAI().tryToCast(player, 2099, 1);
				break;
			
			case LOGOUT:
				player.logout(true);
				break;
			
			case ITEM:
				final ItemInstance item = player.getInventory().getItemByItemId(_autoFarmProfile.getMacroAdditionalId());
				if (item == null)
				{
					player.sendMessage("Could not execute the macro. The item is not available.");
					break;
				}
				
				final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
				handler.useItem(player, item, false);
				break;
			
			case SKILL:
				final L2Skill skill = player.getSkill(_autoFarmProfile.getMacroAdditionalId());
				if (skill == null || player.isSkillDisabled(skill))
				{
					player.sendMessage("Could not execute the macro. The skill is not available.");
					break;
				}
				
				player.getAI().tryToCast(player, skill);
		}
	}
	
	/*
	 * We use canSeeTarget instead of canMoveToTarget because pathfinding is not used for attacking
	 */
	private List<Monster> getTarget()
	{
		final Player player = _autoFarmProfile.getPlayer();
		final List<Monster> targets = new ArrayList<>();
		
		for (Monster monster : _autoFarmProfile.getSelectedArea().getMonsters())
		{
			if (monster.isDead())
				continue;
			
			if (!GeoEngine.getInstance().canSeeTarget(player, monster))
				continue;
			
			if (monster.isRaidRelated() && !_autoFarmProfile.attackRaid())
				continue;
			
			if (!_autoFarmProfile.getTargets().isEmpty() && !_autoFarmProfile.getTargets().contains(monster.getName()))
				continue;
			
			targets.add(monster);
		}
		
		targets.sort(Comparator.comparingDouble(t -> player.distance3D(t)));
		return targets;
	}
	
	private L2Skill getCastSkill(Player player)
	{
		// Chance of debuff
		if (Rnd.get(100) <= Config.AUTOFARM_DEBUFF_CHANCE || !player.getTarget().getMonster().getAI().getAggroList().contains(player))
		{
			final L2Skill debuff = getAttackSkill(player, true);
			if (debuff != null)
				return debuff;
		}
		
		// Fighter has physical attack
		if (!player.isMageClass() && Rnd.nextBoolean())
			return null;
		
		return getAttackSkill(player, false);
	}
	
	private L2Skill getAttackSkill(Player player, boolean debuff)
	{
		final List<L2Skill> skills = _autoFarmProfile.getAttackSkills(debuff);
		Collections.shuffle(skills);
		
		for (int i = 0; i < skills.size(); i++)
		{
			final L2Skill skill = Rnd.get(skills);
			if (player.isSkillDisabled(skill))
				continue;
			
			if (player.distance3D(player.getTarget()) > (skill.getCastRange() > 0 ? skill.getCastRange() : skill.getSkillRadius()))
				continue;
			
			// This Critical Blow skill from the Adventurer class is like a buff
			if (skill.getId() == 409 && player.getFirstEffect(skill) != null)
				continue;
			
			if (skill.getItemConsumeId() != 0)
			{
				// We save the state to check if the player can continue farming
				final ItemInstance consumable = player.getInventory().getItemByItemId(skill.getItemConsumeId());
				_noAttackSkillItems = consumable == null || consumable.getCount() < skill.getItemConsume();
				
				if (_noAttackSkillItems)
					continue;
			}
			
			if (!debuff)
				return skill;
			
			if (player.getTarget().getMonster().getFirstEffect(skill) == null)
				return skill;
		}
		
		return null;
	}
	
	/*
	 * If necessary, returns a skill to heal HP or MP
	 */
	private boolean testHealSkill(Player player)
	{
		// Life has priority
		if (player.getStatus().getHpRatio() < Config.AUTOFARM_HP_HEAL_RATE)
		{
			for (L2Skill skill : _autoFarmProfile.getHpHealSkills())
			{
				if (!player.isSkillDisabled(skill))
				{
					player.getAI().tryToCast(player, skill);
					return true;
				}
			}
		}
		
		if (player.getStatus().getMpRatio() < Config.AUTOFARM_MP_HEAL_RATE)
		{
			for (L2Skill skill : _autoFarmProfile.getMpHealSkills())
			{
				if (!player.isSkillDisabled(skill))
				{
					player.getAI().tryToCast(player, skill);
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean testBuffSkill(Player player)
	{
		if (player.getCast().isCastingNow())
			return false;
		
		// The order of the player's skills can create priority in this case
		for (L2Skill skill : _autoFarmProfile.getBuffSkills())
		{
			if (skill.hasEffects() && player.getFirstEffect(skill) != null || player.isSkillDisabled(skill))
				continue;
			
			if (skill.getItemConsumeId() != 0)
			{
				final ItemInstance consumable = player.getInventory().getItemByItemId(skill.getItemConsumeId());
				if (consumable == null || consumable.getCount() < skill.getItemConsume())
					continue;
			}
			
			if (skill.getSkillType() == SkillType.CREATE_ITEM || skill.getSkillType() == SkillType.EXTRACTABLE)
			{
				if (testCreateItemSkill(player, skill))
					return true;
				
				continue;
			}
			else if (skill.getSkillType() == SkillType.NEGATE)
			{
				if (testNegateSkill(player, skill))
					return true;
				
				continue;
			}
			
			player.getAI().tryToCast(player, skill);
			return true;
		}
		
		return false;
	}
	
	private static boolean testNegateSkill(Player player, L2Skill skill)
	{
		boolean doCast = false;
		for (AbstractEffect effect : player.getAllEffects())
		{
			if (ArraysUtil.contains(skill.getNegateId(), effect.getSkill().getId()))
				doCast = true;
			else
			{
				// Disablers.java
				for (SkillType skillType : skill.getNegateStats())
				{
					final L2Skill effectSkill = effect.getSkill();
					if (effectSkill.getEffectType() != null && effectSkill.getEffectAbnormalLvl() >= 0)
					{
						if (effectSkill.getEffectType() == skillType && effectSkill.getEffectAbnormalLvl() <= skill.getNegateLvl())
							doCast = true;
					}
					else if (effectSkill.getSkillType() == skillType && effectSkill.getAbnormalLvl() <= skill.getNegateLvl())
						doCast = true;
				}
			}
		}
		
		if (doCast)
			player.getAI().tryToCast(player, skill);
		
		return doCast;
	}
	
	private static boolean testCreateItemSkill(Player player, L2Skill skill)
	{
		boolean doCast = false;
		if (skill.getSkillType() == SkillType.CREATE_ITEM)
		{
			// Summon CP Potion
			if (skill.getId() == 1324)
				doCast = true;
			else
			{
				final L2SkillCreateItem createSkill = (L2SkillCreateItem) skill;
				final int createItemCount = createSkill._createItemCount;
				
				for (int createItemId : createSkill._createItemId)
				{
					if (player.getInventory().getItemCount(createItemId) < createItemCount)
						doCast = true;
				}
			}
		}
		else if (skill.getSkillType() == SkillType.EXTRACTABLE) // only the Quiver of Arrow
		{
			final ExtractableProductItem extractable = skill.getExtractableSkill().getProductItems().iterator().next();
			if (player.getInventory().getItemCount(extractable.getItems().iterator().next().getId()) < 200)
				doCast = true;
		}
		
		if (doCast)
			player.getAI().tryToCast(player, skill);
		
		return doCast;
	}
	
	private static void testHpPotions(Player player)
	{
		if (player.getStatus().getHpRatio() > Config.AUTOFARM_HP_HEAL_RATE)
			return;
		
		testAutoPotions(player, Config.AUTOFARM_HP_POTIONS);
	}
	
	private static void testMpPotions(Player player)
	{
		if (player.getStatus().getMpRatio() > Config.AUTOFARM_MP_HEAL_RATE)
			return;
		
		testAutoPotions(player, Config.AUTOFARM_MP_POTIONS);
	}
	
	private static void testAutoPotions(Player player, int[] ids)
	{
		for (int i : ids)
		{
			final ItemInstance potion = player.getInventory().getItemByItemId(i);
			if (potion == null)
				continue;
			
			if (player.isItemDisabled(potion))
				continue;
			
			boolean useItem = true;
			for (IntIntHolder holder : potion.getEtcItem().getSkills())
			{
				// This check will be done in the handler, but we anticipate it to avoid sending failure messages
				if (player.isSkillDisabled(holder.getSkill()))
				{
					useItem = false;
					break;
				}
				
				// Effects with the same stack
				if (holder.getSkill().hasEffects())
				{
					for (AbstractEffect ae : player.getAllEffects())
					{
						for (EffectTemplate effect : holder.getSkill().getEffectTemplates())
						{
							if (ae.getTemplate().getStackType().equals(effect.getStackType()))
							{
								useItem = false;
								break;
							}
						}
					}
				}
			}
			
			if (!useItem)
				continue;
			
			final IItemHandler handler = ItemHandler.getInstance().getHandler(potion.getEtcItem());
			handler.useItem(player, potion, false);
			break; // only one at a time
		}
	}
	
	private boolean tryPickUpHerbs(Player player)
	{
		if (player.getStatus().getHpRatio() > Config.AUTOFARM_HP_HEAL_RATE && player.getStatus().getMpRatio() > Config.AUTOFARM_MP_HEAL_RATE)
			return false;
		
		for (ItemInstance herb : player.getKnownTypeInRadius(ItemInstance.class, _autoFarmProfile.getFinalRadius() + 100, i -> i.getOwnerId() == player.getObjectId() && i.getName().contains("Herb of Mana") || i.getName().contains("Herb of Life")))
		{
			if (!GeoEngine.getInstance().canSeeTarget(player, herb) || player.distance3D(herb) > 100)
				continue;
			
			if (herb.getName().contains("Herb of Life") && player.getStatus().getHpRatio() > Config.AUTOFARM_HP_HEAL_RATE)
				continue;
			
			if (herb.getName().contains("Herb of Mana") && player.getStatus().getMpRatio() > Config.AUTOFARM_MP_HEAL_RATE)
				continue;
			
			if (herb.getName().contains("Herb of Recovery") && (player.getStatus().getHpRatio() > Config.AUTOFARM_HP_HEAL_RATE || player.getStatus().getMpRatio() > Config.AUTOFARM_MP_HEAL_RATE))
				continue;
			
			player.getAI().tryToPickUp(herb.getObjectId(), false);
			return true;
		}
		
		return false;
	}
	
	private static boolean isPlayerAttacking(Player player)
	{
		return player.getAttack().isAttackingNow() || player.getCast().isCastingNow();
	}
	
	private static void sendAdminMessage(Player player, String msg)
	{
		if (!player.isGM())
			return;
		
		player.sendMessage("AutoFarmLog: " + msg);
	}
	
	private static void sendMessage(Player player, String msg)
	{
		if (!Config.AUTOFARM_SEND_LOG_MESSAGES)
			return;
		
		player.sendMessage(String.format("AutoFarm (%s): %s", new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()), msg));
	}
	
	private void trySendMessage(Player player, String msg)
	{
		if (!Config.AUTOFARM_SEND_LOG_MESSAGES)
			return;
		
		final int count = _cachedMessages.merge(msg, 1, Integer::sum);
		if (count >= 30)
		{
			_cachedMessages.remove(msg);
			return;
		}
		
		if (count == 1 || count >= 30)
			player.sendMessage(String.format("AutoFarm (%s): %s", new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()), msg));
	}
}