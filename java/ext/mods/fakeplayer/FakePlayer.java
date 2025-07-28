package ext.mods.fakeplayer;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.AntiFeedManager;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.player.Appearance;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.residence.castle.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TeleportToLocation;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.WaterTaskManager;

import ext.mods.fakeplayer.ai.FakePlayerAI;
import ext.mods.fakeplayer.helper.FakePlayerHelpers;
import ext.mods.playergod.PlayerGodManager;

public class FakePlayer extends Player
{
	private FakePlayerAI _ai;
	private Location _saveLoc;
	
	public FakePlayer(int objectId, PlayerTemplate template, String accountName, Appearance app)
	{
		super(objectId, template, accountName, app);
	}
	
	public Location getFakeSaveLocation()
	{
		return _saveLoc;
	}
	
	public void setFakeSaveLocation(Location loc)
	{
		_saveLoc = loc;
	}
	
	public Location isFakeLocationRandom()
	{
		
		int dx = Rnd.get(-200, 200);
		int dy = Rnd.get(-200, 200);
		
		return new Location(getX() + dx, getY() + dy, getZ());
	}
	
	public Location isFakePostion()
	{
		return new Location(getX(), getY(), getZ());
	}
	
	public void teleportToLocation(int x, int y, int z, int randomOffset)
	{
		getMove().stop();
		abortAll(true);
		
		getAI().tryToIdle();
		if (randomOffset > 0)
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		z += 5;
		broadcastPacket(new TeleportToLocation(this, x, y, z, true));
		decayMe();
		setXYZ(x, y, z);
		onTeleported();
		spawnMe();
		revalidateZone(true);
	}
	
	public boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (skill == null)
			return false;
		
		if (isDead() || isOutOfControl())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isSkillDisabled(skill))
			return false;
		
		SkillType sklType = skill.getSkillType();
		
		if (isFishing() && (sklType != SkillType.PUMPING && sklType != SkillType.REELING && sklType != SkillType.FISHING))
		{
			return false;
		}
		
		if (isInObserverMode())
		{
			getCast().canAbortCast();
			return false;
		}
		
		if (isSitting())
		{
			if (skill.isToggle())
			{
				AbstractEffect effect = getFirstEffect(skill.getId());
				if (effect != null)
				{
					effect.exit();
					return false;
				}
			}
			return false;
		}
		
		if (skill.isToggle())
		{
			AbstractEffect effect = getFirstEffect(skill.getId());
			
			if (effect != null)
			{
				if (skill.getId() != 60)
					effect.exit();
				
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		if (isFakeDeath())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		WorldObject target = null;
		SkillTargetType sklTargetType = skill.getTargetType();
		
		if (sklTargetType == SkillTargetType.GROUND)
		{
			LOGGER.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (sklTargetType)
		{
			// Target the player if skill type is AURA, PARTY, CLAN or SELF
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case AURA_UNDEAD:
			case PARTY:
			case ALLY:
			case CLAN:
			case GROUND:
			case SELF:
			case CORPSE_ALLY:
			case AREA_SUMMON:
				target = this;
				break;
			case OWNER_PET:
			case SUMMON:
				target = getSummon();
				break;
			default:
				target = getTarget();
				break;
		}
		
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (target instanceof Door)
		{
			if (!((Door) target).isAttackableBy(this) || (((Door) target).isUnlockable() && skill.getSkillType() != SkillType.UNLOCK))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		if (isInDuel())
		{
			if (target instanceof Playable)
			{
				// Get Player
				Player cha = target.getActingPlayer();
				if (cha.getDuelId() != getDuelId())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		if (skill.isSiegeSummonSkill())
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(this);
			if (siege == null || !siege.checkSide(getClan(), SiegeSide.ATTACKER) || (isInsideZone(ZoneId.CASTLE)))
			{
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_CALL_PET_FROM_THIS_LOCATION));
				return false;
			}
			
			if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) == CabalType.DAWN && SevenSignsManager.getInstance().getPlayerCabal(getObjectId()) == CabalType.DUSK)
			{
				sendPacket(SystemMessageId.SEAL_OF_STRIFE_FORBIDS_SUMMONING);
				return false;
			}
		}
		
		if (!skill.checkCondition(this, (Creature) target, false))
		{
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (skill.isOffensive())
		{
			if (isInsideZone(ZoneId.PEACE))
			{
				// If Creature or target is in a peace zone, send a system message TARGET_IN_PEACEZONE ActionFailed
				sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (isInOlympiadMode() && !isOlympiadStart())
			{
				// if Player is in Olympia and the match isn't already start, send ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			if (!target.isAttackableBy(this) && !getAccessLevel().canGiveDamage())
			{
				// If target is not attackable, send ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (!target.isAttackableBy(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case AURA:
					case FRONT_AURA:
					case BEHIND_AURA:
					case AURA_UNDEAD:
					case CLAN:
					case ALLY:
					case PARTY:
					case SELF:
					case GROUND:
					case CORPSE_ALLY:
					case AREA_SUMMON:
						break;
					default: // Send ActionFailed to the Player
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
				}
			}
			
			if (dontMove)
			{
				
				if (skill.getCastRange() > 0 && !isIn3DRadius(target, (int) (skill.getCastRange() + getCollisionRadius())))
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.TARGET_TOO_FAR);
					
					// Send ActionFailed to the Player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			
		}
		
		if (!skill.isOffensive() && target instanceof Monster && !forceUse)
		{
			switch (sklTargetType)
			{
				case OWNER_PET:
				case SUMMON:
				case AURA:
				case FRONT_AURA:
				case BEHIND_AURA:
				case AURA_UNDEAD:
				case CLAN:
				case SELF:
				case CORPSE_ALLY:
				case PARTY:
				case ALLY:
				case CORPSE_MOB:
				case AREA_CORPSE_MOB:
				case GROUND:
					break;
				default:
				{
					switch (sklType)
					{
						case BEAST_FEED:
						case DELUXE_KEY_UNLOCK:
						case UNLOCK:
							break;
						default:
							sendPacket(ActionFailed.STATIC_PACKET);
							return false;
					}
					break;
				}
			}
		}
		
		if (sklType == SkillType.SPOIL)
		{
			if (!(target instanceof Monster))
			{
				// Send ActionFailed to the Player
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		if (sklType == SkillType.SWEEP && target instanceof Attackable)
		{
			if (((Attackable) target).isDead())
			{
				return false;
			}
		}
		
		if (sklType == SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof Monster))
			{
				// Send ActionFailed to the Player
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		switch (sklTargetType)
		{
			case PARTY:
			case ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case AURA_UNDEAD:
			case GROUND:
			case SELF:
			case CORPSE_ALLY:
			case AREA_SUMMON:
				break;
			default:
				if (!getAccessLevel().canGiveDamage())
				{
					// Send a System Message to the Player
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					
					// Send ActionFailed to the Player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
		}
		
		if (sklTargetType == SkillTargetType.HOLY)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// finally, after passing all conditions
		return true;
	}
	
	public FakePlayerAI getAi()
	{
		return _ai;
	}
	
	public void setAi(FakePlayerAI _fakeAi)
	{
		_ai = _fakeAi;
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the Player
		if (!super.doDie(killer))
			return false;
		
		if (isMounted())
			stopFeed();
		
		// Clean player charges on death.
		clearCharges();
		
		synchronized (this)
		{
			if (isFakeDeath())
				stopFakeDeath(true);
		}
		
		if (killer != null)
		{
			final Player pk = killer.getActingPlayer();
			
			if (pk != null)
			{
				CTFEvent.getInstance().onKill(killer, this);
				DMEvent.getInstance().onKill(killer, this);
				LMEvent.getInstance().onKill(killer, this);
				TvTEvent.getInstance().onKill(killer, this);
				
				PlayerGodManager.getInstance().onKill(pk);
				
			}
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
		}
		
		// Stop casting for any player that may be casting a force buff on this Player.
		forEachKnownType(Creature.class, creature -> creature.getFusionSkill() != null && creature.getFusionSkill().getTarget() == this, creature -> creature.getCast().stop());
		
		if (getFusionSkill() != null)
			getCast().stop();
		
		// Calculate death penalty buff.
		calculateDeathPenaltyBuffLevel(killer);
		
		if (isPhoenixBlessed())
			reviveRequest(this, null, false);
		
		WaterTaskManager.getInstance().remove(this);
		
		// Disable beastshots.
		disableBeastShots();
		
		AntiFeedManager.getInstance().setLastDeathTime(getObjectId());
		
		return true;
	}
	
	public void assignDefaultAI()
	{
		try
		{
			setAi(FakePlayerHelpers.getAIbyClassId(getClassId()).getConstructor(FakePlayer.class).newInstance(this));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void forceAutoAttack(Creature creature)
	{
		if (getTarget() == null || isInsideZone(ZoneId.PEACE) || isConfused())
			return;
		
		Creature target = (Creature) getTarget();
		
		if (isInOlympiadMode() && target instanceof Playable)
		{
			Player playerTarget = target.getActingPlayer();
			if (playerTarget == null || (playerTarget.isInOlympiadMode() && (!isOlympiadStart() || getOlympiadGameId() != playerTarget.getOlympiadGameId())))
				return;
		}
		
		if (!target.isAttackableBy(creature) && !getAccessLevel().canGiveDamage())
			return;
		
		if (!GeoEngine.getInstance().canSeeTarget(this, target))
		{
			return;
		}
		
		if (!GeoEngine.getInstance().canMoveToTarget(getX(), getY(), getZ(), target.getX(), target.getY(), target.getZ()))
		{
			return;
		}
		
		if (isArcher() && MathUtil.calculateDistance(this, target, false) < getStatus().getPhysicalAttackRange() * 0.5)
		{
			int dx = getX() - target.getX();
			int dy = getY() - target.getY();
			double length = Math.sqrt(dx * dx + dy * dy);
			
			if (length > 0)
			{
				int kiteX = getX() + (int) (dx / length * 300);
				int kiteY = getY() + (int) (dy / length * 300);
				int kiteZ = GeoEngine.getInstance().getHeight(kiteX, kiteY, getZ());
				
				if (GeoEngine.getInstance().canMoveToTarget(getX(), getY(), getZ(), kiteX, kiteY, kiteZ))
				{
					getAI().tryToMoveTo(new Location(kiteX, kiteY, kiteZ), null);
					return;
				}
			}
		}
		
		if (isDagger() && MathUtil.calculateDistance(this, target, false) < getStatus().getPhysicalAttackRange() + 50 && Rnd.get(100) < 40)
		{
			double angle = Rnd.get(0, 360);
			double radians = Math.toRadians(angle);
			int distance = 100;
			
			int moveX = target.getX() + (int) (Math.cos(radians) * distance);
			int moveY = target.getY() + (int) (Math.sin(radians) * distance);
			int moveZ = GeoEngine.getInstance().getHeight(moveX, moveY, target.getZ());
			
			if (GeoEngine.getInstance().canMoveToTarget(getX(), getY(), getZ(), moveX, moveY, moveZ))
			{
				getAI().tryToMoveTo(new Location(moveX, moveY, moveZ), null);
				
			}
		}
		
		getAI().tryToAttack(target);
	}
	
	public boolean isArcher()
	{
		return getActiveWeaponItem() != null && getAttackType() == WeaponType.BOW;
	}
	
	public boolean isDagger()
	{
		return getActiveWeaponItem() != null && getAttackType() == WeaponType.DAGGER;
	}
	
	public synchronized void despawnPlayer()
	{
		try
		{
			FakePlayerNames.releaseName(getName(), getAppearance().getSex());
			setOnlineStatus(false, true);
			abortAll(true);
			stopAllToggles();
			getInventory().destroyAllItems();
			
			for (AbstractEffect effect : getAllEffects())
			{
				if (effect.getSkill().isToggle())
				{
					effect.exit();
					continue;
				}
				
				switch (effect.getEffectType())
				{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
						effect.exit();
						break;
					default:
						break;
				}
			}
			
			decayMe();
			
			// If the Player has Pet, unsummon it
			if (getSummon() != null)
				getSummon().unSummon(this);
			
			// deals with sudden exit in the middle of transaction
			if (getActiveRequester() != null)
			{
				setActiveRequester(null);
				cancelActiveTrade();
			}
			
			World.getInstance().removePlayer(this);
			
		}
		catch (Exception e)
		{
			System.out.println("Exception on deleteMe()" + e.getMessage() + e);
		}
	}
	
	public void heal()
	{
		getStatus().setMaxCpHpMp();
	}
}
