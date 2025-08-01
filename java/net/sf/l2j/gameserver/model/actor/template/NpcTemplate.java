package net.sf.l2j.gameserver.model.actor.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.NpcRace;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.memo.NpcMemo;
import net.sf.l2j.gameserver.model.records.PrivateData;
import net.sf.l2j.gameserver.model.residence.Residence;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.residence.clanhall.SiegableHall;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

public class NpcTemplate extends CreatureTemplate
{
	private final int _npcId;
	private final int _idTemplate;
	
	private final String _name;
	private final String _title;
	private final String _alias;
	
	private final boolean _usingServerSideName;
	private final boolean _usingServerSideTitle;
	
	private final String _type;
	private final byte _level;
	private final double _hitTimeFactor;
	private final int _rHand;
	private final int _lHand;
	private final double _exp;
	private final double _sp;
	
	private final int _baseAttackRange;
	private final int[] _baseDamageRange;
	private final int _baseRandomDamage;
	
	private final NpcRace _race;
	
	private final int _ssCount;
	private final int _spsCount;
	
	private final boolean _isUndying;
	private final boolean _canBeAttacked;
	private final int _corpseTime;
	private final boolean _isNoSleepMode;
	private final int _aggroRange;
	private final boolean _aggro;
	private final boolean _canMove;
	private final boolean _isSeedable;
	private final boolean _canSeeThrough;
	
	private final boolean _cantBeChampionMonster;
	
	private final NpcMemo _aiParams;
	private final List<DropCategory> _categories;
	private final List<PrivateData> _privateData;
	
	private final List<L2Skill> _passives;
	private final Map<NpcSkillType, L2Skill> _skills;
	
	private final Map<EventHandler, List<Quest>> _questEvents = new EnumMap<>(EventHandler.class);
	
	private String[] _clans;
	private int _clanRange;
	private int[] _ignoredIds;
	
	private List<ClassId> _teachInfo;
	
	private Residence _residence;
	
	public NpcTemplate(StatSet set)
	{
		super(set);
		
		_npcId = set.getInteger("id");
		_idTemplate = set.getInteger("idTemplate", _npcId);
		
		_name = set.getString("name");
		_title = set.getString("title", "");
		_alias = set.getString("alias", "");
		
		_usingServerSideName = set.getBool("usingServerSideName", false);
		_usingServerSideTitle = set.getBool("usingServerSideTitle", false);
		
		_type = set.getString("type");
		_level = set.getByte("level", (byte) 1);
		_hitTimeFactor = set.getDouble("hitTimeFactor", 0.);
		_rHand = set.getInteger("rHand", 0);
		_lHand = set.getInteger("lHand", 0);
		_exp = set.getDouble("exp", 0.);
		_sp = set.getDouble("sp", 0.);
		
		_baseAttackRange = set.getInteger("baseAttackRange", 0);
		_baseDamageRange = set.getIntegerArray("baseDamageRange");
		_baseRandomDamage = set.getInteger("baseRandomDamage", 0);
		
		_race = set.getEnum("race", NpcRace.class, NpcRace.DUMMY);
		
		_ssCount = set.getInteger("ssCount", 0);
		_spsCount = set.getInteger("spsCount", 0);
		
		_isUndying = set.getBool("undying", false);
		_canBeAttacked = set.getBool("canBeAttacked", true);
		_corpseTime = set.getInteger("corpseTime", 7);
		_isNoSleepMode = set.getBool("noSleepMode", false);
		_aggroRange = set.getInteger("aggroRange", 0);
		_aggro = set.getBool("aggro", false);
		_canMove = set.getBool("canMove", true);
		_isSeedable = set.getBool("seedable", false);
		_canSeeThrough = set.getBool("canSeeThrough", false);
		
		_cantBeChampionMonster = (_title.equalsIgnoreCase("Quest Monster") || isType("Chest") || !set.getBool("canBeChamp", true)) ? true : false;
		
		_aiParams = (set.containsKey("aiParams")) ? new NpcMemo(set.getMap("aiParams")) : NpcMemo.DUMMY_SET;
		_categories = set.getList("drops");
		_privateData = set.getList("privates");
		
		_passives = set.getList("passives");
		_skills = set.getMap("skills");
		
		if (set.containsKey("clan"))
		{
			_clans = set.getStringArray("clan");
			_clanRange = set.getInteger("clanRange");
			
			if (set.containsKey("ignoredIds"))
				_ignoredIds = set.getIntegerArray("ignoredIds");
		}
		
		if (set.containsKey("teachTo"))
		{
			final int[] classIds = set.getIntegerArray("teachTo");
			
			_teachInfo = new ArrayList<>(classIds.length);
			for (int classId : classIds)
				_teachInfo.add(ClassId.VALUES[classId]);
		}
		
		if (Config.NPC_STAT_MULTIPLIERS)
		{
			switch (_type)
			{
				case "Monster":
					_baseHpMax *= Config.MONSTER_HP_MULTIPLIER;
					_baseMpMax *= Config.MONSTER_MP_MULTIPLIER;
					_basePAtk *= Config.MONSTER_PATK_MULTIPLIER;
					_baseMAtk *= Config.MONSTER_MATK_MULTIPLIER;
					_basePDef *= Config.MONSTER_PDEF_MULTIPLIER;
					_baseMDef *= Config.MONSTER_MDEF_MULTIPLIER;
					break;
				case "RaidBoss":
					_baseHpMax *= Config.RAIDBOSS_HP_MULTIPLIER;
					_baseMpMax *= Config.RAIDBOSS_MP_MULTIPLIER;
					_basePAtk *= Config.RAIDBOSS_PATK_MULTIPLIER;
					_baseMAtk *= Config.RAIDBOSS_MATK_MULTIPLIER;
					_basePDef *= Config.RAIDBOSS_PDEF_MULTIPLIER;
					_baseMDef *= Config.RAIDBOSS_MDEF_MULTIPLIER;
					break;
				case "GrandBoss":
					_baseHpMax *= Config.GRANDBOSS_HP_MULTIPLIER;
					_baseMpMax *= Config.GRANDBOSS_MP_MULTIPLIER;
					_basePAtk *= Config.GRANDBOSS_PATK_MULTIPLIER;
					_baseMAtk *= Config.GRANDBOSS_MATK_MULTIPLIER;
					_basePDef *= Config.GRANDBOSS_PDEF_MULTIPLIER;
					_baseMDef *= Config.GRANDBOSS_MDEF_MULTIPLIER;
					break;
			}
		}
		
		// Set the Castle if existing.
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getNpcs().contains(_npcId))
			{
				_residence = castle;
				break;
			}
		}
		
		if (_residence == null)
		{
			// Set the ClanHall if existing.
			for (ClanHall ch : ClanHallManager.getInstance().getClanHalls().values())
			{
				if (ch.getNpcs().contains(_npcId))
				{
					_residence = (ch instanceof SiegableHall sh) ? sh : ch;
					break;
				}
			}
		}
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public int getIdTemplate()
	{
		return _idTemplate;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public String getAlias()
	{
		return _alias;
	}
	
	public boolean isUsingServerSideName()
	{
		return _usingServerSideName;
	}
	
	public boolean isUsingServerSideTitle()
	{
		return _usingServerSideTitle;
	}
	
	public String getType()
	{
		return _type;
	}
	
	/**
	 * @param type : the type to check.
	 * @return true if the instance type written as {@link String} is the same as this {@link NpcTemplate}, or false otherwise.
	 */
	public boolean isType(String type)
	{
		return _type.equalsIgnoreCase(type);
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public double getHitTimeFactor()
	{
		return _hitTimeFactor;
	}
	
	public int getRightHand()
	{
		return _rHand;
	}
	
	public int getLeftHand()
	{
		return _lHand;
	}
	
	public double getRewardExp()
	{
		return _exp;
	}
	
	public double getRewardSp()
	{
		return _sp;
	}
	
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	public int[] getBaseDamageRange()
	{
		return _baseDamageRange;
	}
	
	public int getBaseRandomDamage()
	{
		return _baseRandomDamage;
	}
	
	public NpcRace getRace()
	{
		return _race;
	}
	
	public String[] getClans()
	{
		return _clans;
	}
	
	public int getClanRange()
	{
		return _clanRange;
	}
	
	public int[] getIgnoredIds()
	{
		return _ignoredIds;
	}
	
	public int getSsCount()
	{
		return _ssCount;
	}
	
	public int getSpsCount()
	{
		return _spsCount;
	}
	
	public boolean isUndying()
	{
		return _isUndying;
	}
	
	public boolean canBeAttacked()
	{
		return _canBeAttacked;
	}
	
	public int getCorpseTime()
	{
		return _corpseTime;
	}
	
	public boolean isNoSleepMode()
	{
		return _isNoSleepMode;
	}
	
	public int getAggroRange()
	{
		return _aggroRange;
	}
	
	public boolean getAggro()
	{
		return _aggro;
	}
	
	public boolean canMove()
	{
		return _canMove;
	}
	
	public boolean isSeedable()
	{
		return _isSeedable;
	}
	
	public boolean canSeeThrough()
	{
		return _canSeeThrough;
	}
	
	public boolean cantBeChampion()
	{
		return _cantBeChampionMonster;
	}
	
	public Residence getResidence()
	{
		return _residence;
	}
	
	public NpcMemo getAiParams()
	{
		return _aiParams;
	}
	
	/**
	 * @return the {@link List} of all {@link DropCategory}s of this {@link NpcTemplate}.
	 */
	public List<DropCategory> getDropData()
	{
		return _categories;
	}
	
	/**
	 * Add a {@link DropCategory} to drop list.
	 * @param category : The {@link DropCategory} to be added.
	 */
	public void addDropData(DropCategory category)
	{
		_categories.add(category);
	}
	
	/**
	 * @return the {@link List} of all {@link PrivateData}.
	 */
	public List<PrivateData> getPrivateData()
	{
		return _privateData;
	}
	
	/**
	 * @return the {@link List} holding the passive {@link L2Skill}s.
	 */
	public List<L2Skill> getPassives()
	{
		return _passives;
	}
	
	/**
	 * @return the {@link Map} holding the active {@link L2Skill}s.
	 */
	public Map<NpcSkillType, L2Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * @param types : The {@link NpcSkillType}s to test.
	 * @return the {@link List} of {@link L2Skill}s based on given {@link NpcSkillType}s.
	 */
	public List<L2Skill> getSkills(NpcSkillType... types)
	{
		return _skills.entrySet().stream().filter(s -> ArraysUtil.contains(types, s.getKey())).map(Map.Entry::getValue).toList();
	}
	
	/**
	 * @param type : The {@link NpcSkillType} to test.
	 * @return the {@link L2Skill} based on a given {@link NpcSkillType}.
	 */
	public L2Skill getSkill(NpcSkillType type)
	{
		return _skills.get(type);
	}
	
	/**
	 * @param classId : The ClassId to check.
	 * @return true if _teachInfo exists and if it contains the {@link ClassId} set as parameter, or false otherwise.
	 */
	public boolean canTeach(ClassId classId)
	{
		return _teachInfo != null && _teachInfo.contains((classId.getLevel() == 3) ? classId.getParent() : classId);
	}
	
	/**
	 * @return the {@link Map} of {@link Quest}s {@link List} categorized by {@link EventHandler}.
	 */
	public Map<EventHandler, List<Quest>> getEventQuests()
	{
		return _questEvents;
	}
	
	/**
	 * @param type : The ScriptEventType to refer.
	 * @return the {@link List} of {@link Quest}s associated to a {@link EventHandler}.
	 */
	public List<Quest> getEventQuests(EventHandler type)
	{
		return _questEvents.getOrDefault(type, Collections.emptyList());
	}
	
	/**
	 * Add a {@link Quest} to the given {@link EventHandler} {@link List}.<br>
	 * <br>
	 * Create the category if it's not existing.
	 * @param type : The ScriptEventType to test.
	 * @param quest : The Quest to add.
	 */
	public void addQuestEvent(EventHandler type, Quest quest)
	{
		List<Quest> list = _questEvents.get(type);
		if (list == null)
		{
			list = new ArrayList<>(5);
			list.add(quest);
			
			_questEvents.put(type, list);
		}
		else
		{
			list.remove(quest);
			
			if (type.isMultipleRegistrationAllowed() || list.isEmpty())
				list.add(quest);
		}
	}
}