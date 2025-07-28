package net.sf.l2j.gameserver.model.actor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.custom.data.PolymorphData;
import net.sf.l2j.gameserver.custom.data.PolymorphData.Polymorph;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.NpcRace;
import net.sf.l2j.gameserver.enums.actors.NpcTalkCond;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.BypassHandler;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.ai.Desire;
import net.sf.l2j.gameserver.model.actor.ai.type.NpcAI;
import net.sf.l2j.gameserver.model.actor.attack.NpcAttack;
import net.sf.l2j.gameserver.model.actor.cast.NpcCast;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.FriendlyMonster;
import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.status.NpcStatus;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.model.records.PrivateData;
import net.sf.l2j.gameserver.model.residence.Residence;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.residence.clanhall.SiegableHall;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.AiTaskManager;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

/**
 * An instance type extending {@link Creature}, which represents a Non Playable Character (or NPC) in the world.
 */
public class Npc extends Creature
{
	public static final int INTERACTION_DISTANCE = 150;
	
	private ASpawn _spawn;
	private SpawnLocation _spawnLoc;
	private ScheduledFuture<?> _respawnTask;
	
	private Npc _master;
	private Set<Npc> _minions;
	
	private AtomicBoolean _isDecayed = new AtomicBoolean();
	
	private int _leftHandItemId;
	private int _rightHandItemId;
	private int _enchantEffect;
	
	private double _currentCollisionHeight; // used for npc grow effect skills
	private double _currentCollisionRadius; // used for npc grow effect skills
	
	private int _currentSsCount = 0;
	private int _currentSpsCount = 0;
	private int _shotsMask = 0;
	
	private int _scriptValue = 0;
	
	private Residence _residence;
	
	private Creature _lastAttacker;
	
	private boolean _isCoreAiDisabled;
	
	private List<Integer> _observerGroups;
	
	private long _lookNeighborTimeStamp = 0L;
	
	private List<Integer> _followSlots = Arrays.asList(null, null, null, null, null, null, null, null);
	private Location _lastFollowingLoc = null;
	
	private boolean _isAISleeping = true;
	
	private int _clanId;
	
	private boolean _readyForRespawn;
	
	// AI script related values.
	public int _i_ai0;
	public int _i_ai1;
	public int _i_ai2;
	public int _i_ai3;
	public int _i_ai4;
	
	public Creature _c_ai0;
	public Creature _c_ai1;
	public Creature _c_ai2;
	public Creature _c_ai3;
	public Creature _c_ai4;
	
	// Quest related values.
	public int _i_quest0;
	public int _i_quest1;
	public int _i_quest2;
	public int _i_quest3;
	public int _i_quest4;
	
	public Creature _c_quest0;
	public Creature _c_quest1;
	public Creature _c_quest2;
	public Creature _c_quest3;
	public Creature _c_quest4;
	
	// Parameters related values.
	public int _param1;
	public int _param2;
	public int _param3;
	
	public int _flag;
	public int _respawnTime;
	public int _weightPoint = 1;
	
	// Atomic values
	public AtomicInteger _av_quest0 = new AtomicInteger();
	public AtomicInteger _av_quest1 = new AtomicInteger();
	
	public Creature _summoner;

	private Polymorph _fakePc;
	
	public Npc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		for (final L2Skill skill : template.getPassives())
			addStatFuncs(skill.getStatFuncs(this));
		
		getStatus().initializeValues();
		
		// initialize the "current" equipment
		_leftHandItemId = template.getLeftHand();
		_rightHandItemId = template.getRightHand();
		
		// initialize the "current" collisions
		_currentCollisionHeight = template.getCollisionHeight();
		_currentCollisionRadius = template.getCollisionRadius();
		
		_fakePc = PolymorphData.getInstance().getFakePc(template.getNpcId());
		
		// Set the name of the Creature
		setName(template.getName());
		setTitle(template.getTitle());
		
		// Set the mortal state.
		setMortal(!template.isUndying());
		
		// Set the Residence.
		_residence = template.getResidence();
	}
	
	@Override
	public String toString()
	{
		return StringUtil.trimAndDress(getName(), 20) + " [objId=" + getObjectId() + "]";
	}
	
	@Override
	public NpcAI<? extends Npc> getAI()
	{
		return (NpcAI<?>) _ai;
	}
	
	@Override
	public void setAI()
	{
		_ai = new NpcAI<>(this);
	}
	
	@Override
	public NpcCast getCast()
	{
		return (NpcCast) _cast;
	}
	
	@Override
	public void setCast()
	{
		_cast = new NpcCast(this);
	}
	
	@Override
	public NpcStatus<? extends Npc> getStatus()
	{
		return (NpcStatus<?>) _status;
	}
	
	@Override
	public void setStatus()
	{
		_status = new NpcStatus<>(this);
	}
	
	@Override
	public NpcAttack getAttack()
	{
		return (NpcAttack) _attack;
	}
	
	@Override
	public void setAttack()
	{
		_attack = new NpcAttack(this);
	}
	
	@Override
	public final NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	@Override
	public void setWalkOrRun(boolean value)
	{
		if (value == isRunning())
			return;
		
		super.setWalkOrRun(value);
		
		forEachKnownType(Player.class, this::sendInfo);
	}
	
	@Override
	public boolean isUndead()
	{
		return getTemplate().getRace() == NpcRace.UNDEAD;
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		forEachKnownType(Player.class, this::sendInfo);
	}
	
	@Override
	public final void setTitle(String value)
	{
		_title = (value == null) ? "" : value;
	}
	
	@Override
	public void onInteract(Player player)
	{
		if (!player.getClient().performAction(FloodProtector.ACTION))
			return;
		
		getAI().onRandomAnimation(Rnd.get(8));
		
		player.getQuestList().setLastQuestNpcObjectId(getObjectId());
		
		for (var script : getTemplate().getEventQuests(EventHandler.AI_TALKED))
		{
			script.notifyTALKED(this, player);
			return;
		}
		
		List<Quest> scripts = getTemplate().getEventQuests(EventHandler.FIRST_TALK);
		if (scripts.size() == 1)
			scripts.get(0).notifyFirstTalk(this, player);
		else if (_observerGroups != null)
			showObserverWindow(player);
		else
			showChatWindow(player);
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || !getTemplate().canMove();
	}
	
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new NpcInfo(this, player));
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
			_shotsMask |= type.getMask();
		else
			_shotsMask &= ~type.getMask();
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		if (physical)
		{
			// No more ss for this instance or an ss is already charged.
			if (_currentSsCount <= 0 || isChargedShot(ShotType.SOULSHOT))
				return;
			
			// Reduce the amount of ss for this instance.
			_currentSsCount--;
			
			broadcastPacketInRadius(new MagicSkillUse(this, this, 2154, 1, 0, 0), 600);
			setChargedShot(ShotType.SOULSHOT, true);
		}
		
		if (magic)
		{
			// No more sps for this instance or an sps is already charged.
			if (_currentSpsCount <= 0 || isChargedShot(ShotType.SPIRITSHOT))
				return;
			
			// Reduce the amount of sps for this instance.
			_currentSpsCount--;
			
			broadcastPacketInRadius(new MagicSkillUse(this, this, 2061, 1, 0, 0), 600);
			setChargedShot(ShotType.SPIRITSHOT, true);
		}
	}
	
	@Override
	public int getSkillLevel(int skillId)
	{
		final L2Skill skill = getSkill(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	@Override
	public L2Skill getSkill(int skillId)
	{
		return getTemplate().getSkills().values().stream().filter(s -> s.getId() == skillId).findFirst().orElse(null);
	}
	
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		final Item item = ItemData.getInstance().getTemplate(_rightHandItemId);
		return (item instanceof Weapon weapon) ? weapon : null;
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Item getSecondaryWeaponItem()
	{
		return ItemData.getInstance().getTemplate(_leftHandItemId);
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (attacker != null && !isDead())
		{
			// Add aggro.
			getAI().getAggroList().addDamageHate(attacker, damage, 0);
			
			// Refresh last attacker.
			_lastAttacker = attacker;
			
			//setWalkOrRun(true); FIXME need?
			
			for (Quest quest : getTemplate().getEventQuests(EventHandler.ATTACKED))
				quest.onAttacked(this, attacker, (int) damage, skill);
			
			// Party aggro (minion/master).
			if (isMaster() || hasMaster())
			{
				// Retrieve scripts associated to called Attackable and notify the party call.
				for (Quest quest : getTemplate().getEventQuests(EventHandler.PARTY_ATTACKED))
					quest.onPartyAttacked(this, this, attacker, (int) damage);
				
				// If we have a master, we call the event.
				final Npc master = getMaster();
				
				if (master != null && !master.isDead() && this != master)
				{
					// Retrieve scripts associated to called Attackable and notify the party call.
					for (Quest quest : master.getTemplate().getEventQuests(EventHandler.PARTY_ATTACKED))
						quest.onPartyAttacked(this, master, attacker, (int) damage);
				}
				
				// For all minions except me, we call the event.
				for (Npc minion : getMinions())
				{
					if (minion == this || minion.isDead())
						continue;
					
					// Retrieve scripts associated to called Attackable and notify the party call.
					for (Quest quest : minion.getTemplate().getEventQuests(EventHandler.PARTY_ATTACKED))
						quest.onPartyAttacked(this, minion, attacker, (int) damage);
				}
			}
			
			// Social aggro.
			final String[] actorClans = getTemplate().getClans();
			if (actorClans != null && getTemplate().getClanRange() > 0)
			{
				// Retrieve scripts associated to called Attackable and notify the clan call.
				for (Quest quest : getTemplate().getEventQuests(EventHandler.CLAN_ATTACKED))
					quest.onClanAttacked(this, this, attacker, (int) damage, skill);
				
				forEachKnownTypeInRadius(Npc.class, getTemplate().getClanRange(), called ->
				{
					// Called is dead or caller is the same as called.
					if (called.isDead() || called == this)
						return;
					
					// Caller clan doesn't correspond to the called clan.
					if (!ArraysUtil.contains(actorClans, called.getTemplate().getClans()))
						return;
					
					// Called ignores that type of caller id.
					if (ArraysUtil.contains(called.getTemplate().getIgnoredIds(), getNpcId()))
						return;
					
					// Check if the Attackable is in the LoS of the caller.
					if (!GeoEngine.getInstance().canSeeTarget(this, called))
						return;
					
					// Retrieve scripts associated to called Attackable and notify the clan call.
					for (Quest quest : called.getTemplate().getEventQuests(EventHandler.CLAN_ATTACKED))
						quest.onClanAttacked(this, called, attacker, (int) damage, skill);
				});
			}
		}
		
		// Reduce the current HP of the Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		_leftHandItemId = getTemplate().getLeftHand();
		_rightHandItemId = getTemplate().getRightHand();
		
		_enchantEffect = 0;
		
		_currentCollisionHeight = getTemplate().getCollisionHeight();
		_currentCollisionRadius = getTemplate().getCollisionRadius();
		
		getMove().resetGeoPathFailCount();
		getAI().resetLifeTime();
		
		// Remove the AI from AI manager.
		AiTaskManager.getInstance().remove(this);
		
		// Register to the decay manager.
		DecayTaskManager.getInstance().add(this, getTemplate().getCorpseTime());
		
		for (Quest quest : getTemplate().getEventQuests(EventHandler.MY_DYING))
			ThreadPool.schedule(() -> quest.onMyDying(this, killer), 3000);
		
		// Party aggro (minion/master).
		if (isMaster() || hasMaster())
		{
			// Retrieve scripts associated to called Attackable and notify the party call.
			for (Quest quest : getTemplate().getEventQuests(EventHandler.PARTY_DIED))
				quest.onPartyDied(this, this);
			
			// If we have a master, we call the event.
			final Npc master = getMaster();
			
			if (master != null && this != master)
			{
				// Retrieve scripts associated to called Attackable and notify the party call.
				for (Quest quest : master.getTemplate().getEventQuests(EventHandler.PARTY_DIED))
					quest.onPartyDied(this, master);
			}
			
			// For all minions except me, we call the event.
			for (Npc minion : getMinions())
			{
				if (minion == this)
					continue;
				
				// Retrieve scripts associated to called Attackable and notify the party call.
				for (Quest quest : minion.getTemplate().getEventQuests(EventHandler.PARTY_DIED))
					quest.onPartyDied(this, minion);
			}
			
			if (isMaster())
				getMinions().forEach(n -> n.setMaster(null));
		}
		
		// Social aggro.
		final String[] actorClans = getTemplate().getClans();
		if (actorClans != null && getTemplate().getClanRange() > 0)
		{
			forEachKnownTypeInRadius(Npc.class, getTemplate().getClanRange(), called ->
			{
				// Called is dead.
				if (called.isDead())
					return;
				
				// Caller clan doesn't correspond to the called clan.
				if (!ArraysUtil.contains(actorClans, called.getTemplate().getClans()))
					return;
				
				// Called ignores that type of caller id.
				if (ArraysUtil.contains(called.getTemplate().getIgnoredIds(), getNpcId()))
					return;
				
				// Check if the Attackable is in the LoS of the caller.
				if (!GeoEngine.getInstance().canSeeTarget(this, called))
					return;
				
				// Retrieve scripts associated to called Attackable and notify the clan call.
				for (Quest quest : called.getTemplate().getEventQuests(EventHandler.CLAN_DIED))
					quest.onClanDied(this, called, killer);
			});
		}
		
		if (_spawn != null)
			_spawn.onDie(this);
		
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		getAI().cleanupForNextSpawn();
		_followSlots = Arrays.asList(null, null, null, null, null, null, null, null);
		_lastFollowingLoc = null;
		
		// Initialize ss/sps counts.
		_currentSsCount = Config.NPC_SOULSHOT ? getTemplate().getAiParams().getInteger("SoulShot", 0) : 0;
		_currentSpsCount = Config.NPC_SPIRITSHOT ? getTemplate().getAiParams().getInteger("SpiritShot", 0) : 0;
		
		// NPCs should have running stance when spawned.
		setWalkOrRun(true);
		
		// Set the AI task if region is active or if the NPC is under no sleep mode.
		if ((getRegion() != null && getRegion().isActive()) || getTemplate().isNoSleepMode() || !isInMyTerritory())
			AiTaskManager.getInstance().add(this);
		
		for (Quest quest : getTemplate().getEventQuests(EventHandler.CREATED))
			quest.onCreated(this);
		
		if (_spawn != null)
			_spawn.onSpawn(this);
	}
	
	@Override
	public void onDecay()
	{
		if (isDecayed())
			return;
		
		setIsDead(true);
		
		setDecayed(true);
		
		for (Quest quest : getTemplate().getEventQuests(EventHandler.DECAYED))
			quest.onDecayed(this);
		
		// Stop all quest timers related to DefaultNpc
		for (List<Quest> scripts : getTemplate().getEventQuests().values())
		{
			for (Quest script : scripts)
			{
				if (script instanceof DefaultNpc)
					script.cancelQuestTimers(this);
			}
		}
		
		// Remove the Npc from the world when the decay task is launched.
		super.onDecay();
		
		// Respawn it, if possible.
		if (_spawn != null)
			_spawn.onDecay(this);
		
		_followSlots = Arrays.asList(null, null, null, null, null, null, null, null);
		_lastFollowingLoc = null;
	}
	
	@Override
	public void deleteMe()
	{
		// Decay
		onDecay();
		
		// Remove the AI from AI manager.
		AiTaskManager.getInstance().remove(this);
		
		// Register to the decay manager.
		DecayTaskManager.getInstance().cancel(this);
		
		// Stop all running effects.
		stopAllEffects();
		
		super.deleteMe();
	}
	
	@Override
	public double getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	@Override
	public double getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	@Override
	public boolean isAttackingDisabled()
	{
		return super.isAttackingDisabled() || isCoreAiDisabled();
	}
	
	@Override
	public void forceDecay()
	{
		if (isDecayed())
			return;
		
		super.forceDecay();
	}
	
	@Override
	public void onActiveRegion()
	{
		AiTaskManager.getInstance().add(this);
	}
	
	@Override
	public void onInactiveRegion()
	{
 		// Unset AI.
 		if (!getTemplate().isNoSleepMode() && isInMyTerritory())
		{
			// Stop all active skills effects in progress.
			stopAllEffects();
			
			// Set back to peace.
			getAI().setBackToPeace();
			
			// Stop move route task
			getAI().stopRoute();
			
			// Abort all tasks
			abortAll(true);
			
 			AiTaskManager.getInstance().remove(this);
		}
	}
	
	@Override
	public void instantTeleportTo(int x, int y, int z, int randomOffset)
	{
		super.instantTeleportTo(x, y, z, randomOffset);
		
		// Set AI if OOT
		if (!isInMyTerritory())
			AiTaskManager.getInstance().add(this);
	}
	
	@Override
	public boolean teleportTo(int x, int y, int z, int randomOffset)
	{
		if (!super.teleportTo(x, y, z, randomOffset))
			return false;
		
		// Set AI if OOT
		if (!isInMyTerritory())
			AiTaskManager.getInstance().add(this);
		
		return true;
	}
	
	/**
	 * Edit equipped items, which are set back as default upon spawn/decay.
	 * @param rightHandItemId : The item id used as right hand item.
	 * @param leftHandItemId : The item id used as left hand item.
	 */
	public void equipItem(int rightHandItemId, int leftHandItemId)
	{
		_leftHandItemId = leftHandItemId;
		_rightHandItemId = rightHandItemId;
		
		broadcastPacket(new NpcInfo(this, null));
	}
	
	public int getCurrentSsCount()
	{
		return _currentSsCount;
	}
	
	public int getCurrentSpsCount()
	{
		return _currentSpsCount;
	}
	
	/**
	 * @return the {@link ASpawn} associated to this {@link Npc}.
	 */
	public ASpawn getSpawn()
	{
		return _spawn;
	}
	
	/**
	 * Set the {@link ASpawn} of this {@link Npc}.
	 * @param spawn : The ASpawn to set.
	 */
	public void setSpawn(ASpawn spawn)
	{
		_spawn = spawn;
	}
	
	/**
	 * Sets {@link SpawnLocation} of this {@link Npc}. Used mostly for raid bosses teleporting, so return home mechanism works.
	 * @param loc : new spawn location.
	 */
	public final void setSpawnLocation(SpawnLocation loc)
	{
		_spawnLoc = loc;
	}
	
	/**
	 * @return The {@link SpawnLocation} of this {@link Npc}, regardless the type of spawn (e.g. null, {@link Spawn}, {@link MultiSpawn}, etc).
	 */
	public final SpawnLocation getSpawnLocation()
	{
		return _spawnLoc;
	}
	
	public Npc getMaster()
	{
		return _master;
	}
	
	public void setMaster(Npc npc)
	{
		_master = npc;
	}
	
	public boolean isMaster()
	{
		return _minions != null;
	}
	
	public boolean hasMaster()
	{
		return _master != null;
	}
	
	public Set<Npc> getMinions()
	{
		if (_master == null)
		{
			if (_minions == null)
				_minions = ConcurrentHashMap.newKeySet();
			
			return _minions;
		}
		return _master.getMinions();
	}
	
	/**
	 * Teleport this {@link Npc} to its {@link Npc} master.
	 */
	public void teleportToMaster()
	{
		final Npc master = getMaster();
		if (master == null)
			return;
		
		teleportTo(getSpawn().getSpawnLocation(), 0);
	}
	
	/**
	 * @return True, when this {@link Npc} is in its area of free movement/territory.
	 */
	public boolean isInMyTerritory()
	{
		final Npc master = getMaster();
		if (master != null)
			return master.isInMyTerritory();
		
		return _spawn.isInMyTerritory(this);
	}
	
	public void scheduleDBRespawn(long delay)
	{
		if (delay <= 0)
		{
			LOGGER.info("DB Delay <= 0 while scheduling normal respawn of " + getName() + " [" + getNpcId() + "] (" + delay + ")");
			return;
		}
		
		if (_respawnTask != null)
			LOGGER.info("DB Respawn task already exists for " + getName() + " [" + getNpcId() + "]!");
		
		_respawnTask = ThreadPool.schedule(() ->
		{
			if (_spawn != null)
				_spawn.doRespawn(this);
		}, delay);
	}
	
	public void scheduleRespawn(long delay)
	{
		if (delay <= 0)
			return;
		
		if (_respawnTask != null)
			LOGGER.info("Respawn task already exists for " + getName() + " [" + getNpcId() + "]!");
		
		int coprseTime = getTemplate().getCorpseTime() * 1000;
		if (this instanceof Monster monster && (monster.getSpoilState().isSpoiled() || monster.getSeedState().isSeeded()))
			coprseTime *= 2;
		
		_respawnTask = ThreadPool.schedule(() ->
		{
			if (_spawn != null)
				_spawn.doRespawn(this);
		}, delay + coprseTime);
	}
	
	public void cancelRespawn()
	{
		if (_respawnTask != null)
		{
			_respawnTask.cancel(false);
			_respawnTask = null;
		}
	}
	
	public void scheduleDespawn(long delay)
	{
		ThreadPool.schedule(() ->
		{
			if (!isDecayed())
				deleteMe();
		}, delay);
	}
	
	public boolean isDecayed()
	{
		return _isDecayed.get();
	}
	
	public void setDecayed(boolean decayed)
	{
		_isDecayed.set(decayed);
	}
	
	public boolean isReadyForRespawn()
	{
		return _readyForRespawn;
	}
	
	public void setReadyForRespawn(boolean ready)
	{
		_readyForRespawn = ready;
	}
	
	/**
	 * @return The id of this {@link Npc} contained in its {@link NpcTemplate}.
	 */
	public int getNpcId()
	{
		return getTemplate().getNpcId();
	}
	
	/**
	 * @return True if this {@link Npc} is agressive.
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * @return The id of the item in the left hand of this {@link Npc}.
	 */
	public int getLeftHandItemId()
	{
		return _leftHandItemId;
	}
	
	/**
	 * Set the id of the item in the left hand of this {@link Npc}.
	 * @param itemId : The itemId to set.
	 */
	public void setLeftHandItemId(int itemId)
	{
		_leftHandItemId = itemId;
	}
	
	/**
	 * @return The id of the item in the right hand of this {@link Npc}.
	 */
	public int getRightHandItemId()
	{
		return _rightHandItemId;
	}
	
	/**
	 * Set the id of the item in the right hand of this {@link Npc}.
	 * @param itemId : The itemId to set.
	 */
	public void setRightHandItemId(int itemId)
	{
		_rightHandItemId = itemId;
	}
	
	public int getEnchantEffect()
	{
		return _enchantEffect;
	}
	
	public void setEnchantEffect(int enchant)
	{
		_enchantEffect = enchant;
		updateAbnormalEffect();
	}
	
	public void setCollisionHeight(double height)
	{
		_currentCollisionHeight = height;
	}
	
	public void setCollisionRadius(double radius)
	{
		_currentCollisionRadius = radius;
	}
	
	public int getScriptValue()
	{
		return _scriptValue;
	}
	
	public void setScriptValue(int val)
	{
		_scriptValue = val;
	}
	
	public boolean isScriptValue(int val)
	{
		return _scriptValue == val;
	}
	
	/**
	 * @return True if this {@link Npc} can be a warehouse manager, false otherwise.
	 */
	public boolean isWarehouse()
	{
		return false;
	}
	
	/**
	 * @return The {@link Residence} this {@link Npc} belongs to.
	 */
	public final Residence getResidence()
	{
		return _residence;
	}
	
	public final void setResidence(Residence residence)
	{
		_residence = residence;
	}
	
	public final void setResidence(String param)
	{
		if (!StringUtil.isDigit(param))
			return;
		
		final int residenceId = Integer.parseInt(param);
		
		final Castle castle = CastleManager.getInstance().getCastleById(residenceId);
		if (castle != null)
			setResidence(castle);
		else
		{
			final SiegableHall sh = ClanHallManager.getInstance().getSiegableHall(residenceId);
			if (sh != null)
				setResidence(sh);
		}
	}
	
	/**
	 * @return The {@link Castle} this {@link Npc} belongs to.
	 */
	public final Castle getCastle()
	{
		return (_residence instanceof Castle castle) ? castle : null;
	}
	
	/**
	 * @return The {@link ClanHall} this {@link Npc} belongs to.
	 */
	public final ClanHall getClanHall()
	{
		return (_residence instanceof ClanHall ch) ? ch : null;
	}
	
	/**
	 * @return The {@link SiegableHall} this {@link Npc} belongs to.
	 */
	public final SiegableHall getSiegableHall()
	{
		return (_residence instanceof SiegableHall sh) ? sh : null;
	}
	
	/**
	 * @return The last {@link Creature} who attacked this {@link Npc}.
	 */
	public final Creature getLastAttacker()
	{
		return _lastAttacker;
	}
	
	/**
	 * @param player : The {@link Player} used as reference.
	 * @return True if the {@link Player} set as parameter is the clan leader owning this {@link Npc} (being a {@link Castle}, {@link ClanHall} or {@link SiegableHall}).
	 */
	public boolean isLordOwner(Player player)
	{
		// The player isn't a Clan leader, return.
		if (!player.isClanLeader())
			return false;
		
		// Test residence ownership.
		if (_residence != null && _residence.getOwnerId() == player.getClanId())
			return true;
		
		return false;
	}
	
	public int getClanId()
	{
		// Test clanId directly
		if (_clanId != 0)
			return _clanId;
		
		// Test residence ownership.
		if (_residence != null)
			return _residence.getOwnerId();
		
		return 0;
	}
	
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * @return True if this {@link Npc} got its regular AI behavior disabled.
	 */
	public boolean isCoreAiDisabled()
	{
		return _isCoreAiDisabled;
	}
	
	/**
	 * Toggle on/off the regular AI behavior of this {@link Npc}.
	 * @param value : The value to set.
	 */
	public void disableCoreAi(boolean value)
	{
		_isCoreAiDisabled = value;
	}
	
	public List<Integer> getObserverGroups()
	{
		return _observerGroups;
	}
	
	public void setObserverGroups(List<Integer> groups)
	{
		_observerGroups = groups;
	}
	
	public List<Integer> getFollowSlots()
	{
		return _followSlots;
	}
	
	public Location getLastFollowingLoc()
	{
		return _lastFollowingLoc;
	}
	
	public void setLastFollowingLoc(Location value)
	{
		_lastFollowingLoc = value;
	}
	
	public boolean isAISleeping()
	{
		return _isAISleeping;
	}
	
	public void setAISleeping(boolean value)
	{
		_isAISleeping = value;
	}
	
	/**
	 * Open a quest or chat window for a {@link Player} with the text of this {@link Npc} based of the {@link String} set as parameter.
	 * @param player : The {@link Player} to test.
	 * @param command : The {@link String} used as command bypass received from client.
	 */
	public void onBypassFeedback(Player player, String command)
	{
        final IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
        if (handler != null)
            handler.useBypass(command, player, this);
	}
	
	/**
	 * @param player : The {@link Player} to test.
	 * @return True if the teleport is possible, false otherwise.
	 */
	public boolean isTeleportAllowed(Player player)
	{
		return true;
	}
	
	protected NpcTalkCond getNpcTalkCond(Player player)
	{
		return NpcTalkCond.OWNER;
	}

	/**
	 * Generate the complete path to retrieve a htm, based on npcId.
	 * <ul>
	 * <li>if the file exists on the server (page number = 0) : <B>data/html/default/12006.htm</B> (npcId-page number)</li>
	 * <li>if the file exists on the server (page number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page number)</li>
	 * <li>if the file doesn't exist on the server : <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to you")</li>
	 * </ul>
	 * @param player : The player for whom the HTML path is generate,
	 * @param npcId : The id of the Npc whose text must be displayed.
	 * @param val : The number of the page to display.
	 * @return the pathfile of the selected HTML file in function of the npcId and of the page number.
	 */
	public String getHtmlPath(Player player, int npcId, int val)
	{
		String filename;
		if (val == 0)
			filename = "html/default/" + npcId + ".htm";
		else
			filename = "html/default/" + npcId + "-" + val + ".htm";
		
		if (HTMLData.getInstance().exists(player.getLocale(), filename))
			return filename;
		
		return "html/npcdefault.htm";
	}
	
	/**
	 * Broadcast a {@link String} to the knownlist of this {@link Npc}.
	 * @param message : The {@link String} message to send.
	 */
	public void broadcastNpcSay(String message)
	{
		broadcastPacket(new NpcSay(this, SayType.ALL, message));
	}
	
	/**
	 * Broadcast a {@link NpcStringId} to the knownlist of this {@link Npc}.
	 * @param npcStringId : The {@link NpcStringId} to send.
	 */
	public void broadcastNpcSay(NpcStringId npcStringId)
	{
		broadcastNpcSay(npcStringId.getMessage());
	}
	
	/**
	 * Broadcast a {@link NpcStringId} to the knownlist of this {@link Npc}.
	 * @param npcStringId : The {@link NpcStringId} to send.
	 * @param params : Additional parameters for {@link NpcStringId} construction.
	 */
	public void broadcastNpcSay(NpcStringId npcStringId, Object... params)
	{
		broadcastNpcSay(npcStringId.getMessage(params));
	}
	
	/**
	 * Broadcast a {@link String} to the knownlist of this {@link Npc}.
	 * @param message : The {@link String} message to send.
	 */
	public void broadcastNpcShout(String message)
	{
		broadcastPacket(new NpcSay(this, SayType.SHOUT, message));
	}
	
	/**
	 * Broadcast a {@link NpcStringId} to the knownlist of this {@link Npc}.
	 * @param npcStringId : The {@link NpcStringId} to send.
	 */
	public void broadcastNpcShout(NpcStringId npcStringId)
	{
		broadcastNpcShout(npcStringId.getMessage());
	}
	
	/**
	 * Broadcast a {@link NpcStringId} to the knownlist of this {@link Npc}.
	 * @param npcStringId : The {@link NpcStringId} to send.
	 * @param params : Additional parameters for {@link NpcStringId} construction.
	 */
	public void broadcastNpcShout(NpcStringId npcStringId, Object... params)
	{
		broadcastNpcShout(npcStringId.getMessage(params));
	}
	
	/**
	 * Broadcast a {@link String} on screen to the knownlist of this {@link Npc}.
	 * @param time : The time to show the message on screen.
	 * @param message : The {@link String} to send.
	 */
	public void broadcastOnScreen(int time, String message)
	{
		broadcastPacket(new ExShowScreenMessage(message, time));
	}
	
	/**
	 * Broadcast a {@link NpcStringId} on screen to the knownlist of this {@link Npc}.
	 * @param time : The time to show the message on screen.
	 * @param npcStringId : The {@link NpcStringId} to send.
	 */
	public void broadcastOnScreen(int time, NpcStringId npcStringId)
	{
		broadcastOnScreen(time, npcStringId.getMessage());
	}
	
	/**
	 * Broadcast a {@link NpcStringId} on screen to the knownlist of this {@link Npc}.
	 * @param time : The time to show the message on screen.
	 * @param npcStringId : The {@link NpcStringId} to send.
	 * @param params : Additional parameters for {@link NpcStringId} construction.
	 */
	public void broadcastOnScreen(int time, NpcStringId npcStringId, Object... params)
	{
		broadcastOnScreen(time, npcStringId.getMessage(params));
	}
	
	/**
	 * Research the pk chat window HTM related to this {@link Npc}, based on a {@link String} folder.<br>
	 * Send the content to the {@link Player} passed as parameter.
	 * @param player : The {@link Player} to send the HTM.
	 * @param type : The folder to search on.
	 * @return True if such HTM exists, false otherwise.
	 */
	protected boolean showPkDenyChatWindow(Player player, String type)
	{
		final String content = HTMLData.getInstance().getHtm(player, "html/" + type + "/" + getNpcId() + "-pk.htm");
		if (content != null)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setHtml(content);
			
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		return false;
	}
	
	/**
	 * Build and send an HTM to a {@link Player}, based on {@link Npc}'s observer groups.
	 * @param player : The {@link Player} to test.
	 */
	public void showObserverWindow(Player player)
	{
		if (_observerGroups == null)
			return;
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>&$650;<br><br>");
		
		for (int groupId : _observerGroups)
			StringUtil.append(sb, "<a action=\"bypass -h npc_", getObjectId(), "_observe_group ", groupId, "\">&$", groupId, ";</a><br1>");
		
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(sb.toString());
		
		player.sendPacket(html);
	}
	
	/**
	 * Build and send an HTM to a {@link Player}, based on {@link Npc}'s {@link TeleportLocation}s and {@link TeleportType}.
	 * @param player : The {@link Player} to test.
	 * @param type : The {@link TeleportType} to filter.
	 */
	public void showTeleportWindow(Player player, TeleportType type)
	{
		final List<TeleportLocation> teleports = TeleportData.getInstance().getTeleports(getNpcId());
		if (teleports == null)
			return;
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>&$556;<br><br>");
		
		for (int index = 0; index < teleports.size(); index++)
		{
			final TeleportLocation teleport = teleports.get(index);
			if (teleport == null || type != teleport.getType())
				continue;
			
			StringUtil.append(sb, "<a action=\"bypass -h npc_", getObjectId(), "_teleport ", index, "\" msg=\"811;", player.getLocale().getLanguage() == "en" ? teleport.getDescEn(player) : teleport.getDescRu(player), "\">", player.getLocale().getLanguage() == "en" ? teleport.getDescEn(player) : teleport.getDescRu(player));
			
			if (!(Config.FREE_TELEPORT && player.getStatus().getLevel() <= Config.LVL_FREE_TELEPORT))
			{
				final int priceCount = teleport.getCalculatedPriceCount(player);
				if (priceCount > 0)
					StringUtil.append(sb, " - ", priceCount, " &#", teleport.getPriceId(), ";");
			}
			
			sb.append("</a><br1>");
		}
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(sb.toString());
		
		player.sendPacket(html);
	}
	
	/**
	 * Open a chat window on client with the text of the Npc.<br>
	 * Send the content to the {@link Player} passed as parameter.
	 * @param player : The player that talk with the Npc.
	 */
	public void showChatWindow(Player player)
	{
		showChatWindow(player, 0);
	}
	
	/**
	 * Open a chat window on client with the text specified by {@link #getHtmlPath} and val parameter.<br>
	 * Send the content to the {@link Player} passed as parameter.
	 * @param player : The player that talk with the Npc.
	 * @param val : The current htm page to show.
	 */
	public void showChatWindow(Player player, int val)
	{
		showChatWindow(player, getHtmlPath(player, getNpcId(), val));
	}
	
	/**
	 * Open a chat window on client with the text specified by the given file name and path.<br>
	 * Send the content to the {@link Player} passed as parameter.
	 * @param player : The player that talk with the Npc.
	 * @param filename : The filename that contains the text to send.
	 */
	public final void showChatWindow(Player player, String filename)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getLocale(), filename);
		html.replace("%objectId%", getObjectId());
		
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Move this {@link Npc} from its initial {@link Spawn} location using a defined random offset. The {@link Npc} will circle around the initial location.
	 * @param offset : The random offset used.
	 */
	public void moveFromSpawnPointUsingRandomOffset(int offset)
	{
		// No spawn point or offset isn't noticeable ; return instantly.
		if (_spawn == null || offset < 10)
			return;
		
		// Generate a new Location and calculate the destination.
		final Location loc = _spawn.getRandomWalkLocation(this, offset);
		if (loc != null)
		{
			// Move to the position.
			getMove().maybeMoveToLocation(loc, 0, true, false);
		}
	}
	
	/**
	 * @param isPremium
	 * @return the Exp Reward of this L2Npc contained in the L2NpcTemplate (modified by RATE_XP).
	 */
	public int getExpReward(int isPremium)
	{
		if (isPremium == 1)
			return (int) (getTemplate().getRewardExp() * Config.PREMIUM_RATE_XP);
		
		return (int) (getTemplate().getRewardExp() * Config.RATE_XP);
	}
	
	/**
	 * @param isPremium
	 * @return the SP Reward of this L2Npc contained in the L2NpcTemplate (modified by RATE_SP).
	 */
	public int getSpReward(int isPremium)
	{
		if (isPremium == 1)
			return (int) (getTemplate().getRewardSp() * Config.PREMIUM_RATE_SP);
		
		return (int) (getTemplate().getRewardSp() * Config.RATE_SP);
	}
	
	/**
	 * Force this {@link Attackable} to attack a given {@link Creature}.
	 * @param creature : The {@link Creature} to attack.
	 * @param hate : The amount of hate to set.
	 */
	public void forceAttack(Creature creature, int hate)
	{
		getAI().addAttackDesire(creature, hate);
	}
	
	/**
	 * Enforce the call of {@link EventHandler#SEE_ITEM}.
	 * @param radius : The radius.
	 * @param quantity : The quantity of items to check.
	 * @param ids : The ids of {@link ItemInstance}s.
	 */
	public void lookItem(int radius, int quantity, int... ids)
	{
		final List<ItemInstance> items = getKnownTypeInRadius(ItemInstance.class, radius, i -> ids.length < 1 || ArraysUtil.contains(ids, i.getItem().getItemId()));
		if (!items.isEmpty())
		{
			for (Quest quest : getTemplate().getEventQuests(EventHandler.SEE_ITEM))
				quest.onSeeItem(this, quantity, items);
		}
	}
	
	/**
	 * Enforce the call of {@link EventHandler#SEE_CREATURE}.
	 * @param radius in which neighboring creatures are checked for the SEE_CREATURE event.
	 */
	public void lookNeighbor(int radius)
	{
		// lookNeighbor triggers SEE_CREATURE only once every 30s
		if (System.currentTimeMillis() - _lookNeighborTimeStamp < 30000)
			return;
		
		_lookNeighborTimeStamp = System.currentTimeMillis();
		
		final List<Quest> scripts = getTemplate().getEventQuests(EventHandler.SEE_CREATURE);
		if (!scripts.isEmpty())
		{
			forEachKnownTypeInRadius(Creature.class, radius, creature ->
			{
				// Do not trigger event for specific Player conditions.
				final Player player = creature.getActingPlayer();
				if (player != null && (player.isSpawnProtected() || player.isFlying() || !player.getAppearance().isVisible()))
					return;
				
				if (!isRaidBoss() && creature.isSilentMoving() && !getTemplate().canSeeThrough())
					return;
				
				if (Math.abs(creature.getZ() - getZ()) > 500)
					return;
				
				for (Quest quest : scripts)
					quest.onSeeCreature(this, creature);
			});
		}
	}
	
	/**
	 * The range used by default is getTemplate().getAggroRange().
	 * @param target : The targeted {@link Creature}.
	 * @return True if the {@link Creature} used as target is autoattackable, or false otherwise.
	 * @see #canAutoAttack(Creature)
	 */
	public boolean canAutoAttack(Creature target)
	{
		return canAutoAttack(target, getTemplate().getAggroRange(), false);
	}
	
	/**
	 * @param target : The targeted {@link Creature}.
	 * @param range : The range to check.
	 * @param allowPeaceful : If true, peaceful {@link Attackable}s are able to auto-attack.
	 * @return True if the {@link Creature} used as target is autoattackable, or false otherwise.
	 */
	public boolean canAutoAttack(Creature target, int range, boolean allowPeaceful)
	{
		// Check if the target isn't null, a Door or dead.
		if (target == null || target instanceof Door || target.isAlikeDead())
			return false;
		
		final Desire desire = getAI().getDesires().stream().filter(d -> d.getFinalTarget() == target && d.getType() == IntentionType.ATTACK).findFirst().orElse(null);
		if (desire != null && !desire.canMoveToTarget() && getMove().maybeStartOffensiveFollow(target, getStatus().getPhysicalAttackRange()))
			return false;
			
		if (target instanceof Playable)
		{
			// Check if target is in the Aggro range
			if (!isIn3DRadius(target, range))
				return false;
			
			// Check if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode
			if (!isRaidRelated() && !getTemplate().canSeeThrough() && target.isSilentMoving())
				return false;
			
			// Check if the target is a Player
			final Player targetPlayer = target.getActingPlayer();
			if (targetPlayer != null)
			{
				// Check if the target is invisible.
				if (!targetPlayer.getAppearance().isVisible())
					return false;
				
				// Check if player is an allied Varka.
				if (ArraysUtil.contains(getTemplate().getClans(), "varka_silenos_clan") && targetPlayer.isAlliedWithVarka())
					return false;
				
				// Check if player is an allied Ketra.
				if (ArraysUtil.contains(getTemplate().getClans(), "ketra_orc_clan") && targetPlayer.isAlliedWithKetra())
					return false;
				
				// Check for rift rooms to avoid unwanted aggro.
				if (getSpawn().getMemo().getInteger("CreviceOfDiminsion", 0) > 0 && !getSpawn().isInMyTerritory(targetPlayer))
					return false;
				
				// check if the target is within the grace period for JUST getting up from fake death
				if (targetPlayer.isRecentFakeDeath())
					return false;
			}
		}
		
		if (this instanceof Guard)
		{
			// Check if the Playable target has karma.
			if (target instanceof Playable targetPlayable && targetPlayable.getKarma() > 0)
				return GeoEngine.getInstance().canSeeTarget(this, targetPlayable);
			
			// Check if the Monster target is aggressive.
			if (target instanceof Monster targetMonster && Config.GUARD_ATTACK_AGGRO_MOB)
				return targetMonster.getTemplate().getAggro() && GeoEngine.getInstance().canSeeTarget(this, targetMonster);
			
			return false;
		}
		else if (this instanceof FriendlyMonster)
		{
			// Check if the Playable target has karma.
			if (target instanceof Playable targetPlayable && targetPlayable.getKarma() > 0)
				return GeoEngine.getInstance().canSeeTarget(this, targetPlayable);
			
			return false;
		}
		else
		{
			if (target instanceof Attackable targetAttackable && isConfused())
				return GeoEngine.getInstance().canSeeTarget(this, targetAttackable);
			
			if (target instanceof Npc)
				return false;
			
			// Depending on Config, do not allow mobs to attack players in PEACE zones, unless they are already following those players outside.
			if (!Config.MOB_AGGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE))
				return false;
			
			// Check if the actor is Aggressive
			return ((allowPeaceful || getTemplate().getAggro()) && GeoEngine.getInstance().canSeeTarget(this, target));
		}
	}
	
	public List<PrivateData> getPrivateData()
	{
		return (_spawn.getPrivateData() != null && !_spawn.getPrivateData().isEmpty()) ? _spawn.getPrivateData() : getTemplate().getPrivateData();
	}
	
	/**
	 * Stop the ATTACK {@link Desire} associated to the {@link Creature} set as parameter.<br>
	 * <br>
	 * Abort the move stance aswell.
	 * @param target : The {@link Creature} target used to clean hate.
	 */
	public final void removeAttackDesire(Creature target)
	{
		getAI().getAggroList().stopHate(target);
		
		getMove().stop();
	}
	
	/**
	 * Stop all ATTACK {@link Desire}s.<br>
	 * <br>
	 * Abort the move stance aswell.
	 */
	public final void removeAllAttackDesire()
	{
		getAI().getDesires().clear();
		getAI().getAggroList().cleanAllHate();
		
		getMove().stop();
	}
	
	/**
	 * Remove all types of {@link Desire}s, aggro and hate type.<br>
	 * <br>
	 * Abort the move stance aswell.
	 */
	public final void removeAllDesire()
	{
		getAI().getDesires().clear();
		getAI().getAggroList().cleanAllHate();
		getAI().getHateList().cleanAllHate();
		
		getMove().stop();
	}
	
	/**
	 * Trigger {@link EventHandler#SCRIPT_EVENT} scripts for this {@link Npc}.
	 * @param eventId : The id of the event.
	 * @param arg1 : 1st argument of the event.
	 * @param arg2 : 2nd argument of the event.
	 */
	public final void sendScriptEvent(int eventId, int arg1, int arg2)
	{
		for (Quest quest : getTemplate().getEventQuests(EventHandler.SCRIPT_EVENT))
			quest.onScriptEvent(this, eventId, arg1, arg2);
	}
	
	/**
	 * Drop a reward on the ground, to this {@link Monster} feet. It is item protected to the {@link Player} set as parameter.
	 * @param creature : The {@link Creature} used as item protection, if a Player is found.
	 * @param itemId : The item id used as drop.
	 * @param amount : The item amount used as drop.
	 */
	public void dropItem(Creature creature, int itemId, int amount)
	{
		for (int i = 0; i < amount; i++)
		{
			// Create the ItemInstance and add it in the world as a visible object.
			final ItemInstance item = ItemInstance.create(itemId, amount);
			
			// If the tested Creature is either a Playable or Player, set the drop protection.
			if (creature.getActingPlayer() != null)
				item.setDropProtection(creature.getObjectId(), isRaidBoss());
			
			item.dropMe(this);
			
			// If stackable, end loop as entire count is included in 1 instance of item.
			if (item.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				break;
		}
	}
	
	/**
	 * Return a random hero {@link Player} within specified radius.
	 * @param radius : The radius to check in.
	 * @return A Hero {@link Player}, or null if none is around.
	 */
	public Player getNeighborHero(int radius)
	{
		return Rnd.get(getKnownTypeInRadius(Player.class, radius, Player::isHero));
	}
	
	/**
	 * @return The see range of this {@link Npc}, allowing it to check its surroundings.
	 */
	public int getSeeRange()
	{
		return Config.DEFAULT_SEE_RANGE;
	}
	
	public Polymorph getFakePc()
	{
		return _fakePc;
	}
}