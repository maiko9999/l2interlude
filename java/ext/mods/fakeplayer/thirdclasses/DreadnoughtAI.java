package ext.mods.fakeplayer.thirdclasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.l2j.gameserver.enums.items.ShotType;

import ext.mods.fakeplayer.FakePlayer;
import ext.mods.fakeplayer.ai.CombatAI;
import ext.mods.fakeplayer.ai.addon.IConsumableSpender;
import ext.mods.fakeplayer.ai.addon.IPotionUser;
import ext.mods.fakeplayer.helper.FakePlayerHelpers;
import ext.mods.fakeplayer.model.HealingSpell;
import ext.mods.fakeplayer.model.OffensiveSpell;
import ext.mods.fakeplayer.model.SpellUsageCondition;
import ext.mods.fakeplayer.model.SupportSpell;

public class DreadnoughtAI extends CombatAI implements IConsumableSpender, IPotionUser
{
	private final int CPPotion = 5592;
	private final int HPPotion = 1540;
	private final int MPPotion = 728;
	
	public DreadnoughtAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void thinkAndAct()
	{
		super.thinkAndAct();
		setBusyThinking(true);
		applyDefaultBuffs();
		handleConsumable(_fakePlayer, CPPotion);
		handleConsumable(_fakePlayer, HPPotion);
		handleConsumable(_fakePlayer, MPPotion);
		handlePotions(_fakePlayer, CPPotion, HPPotion, MPPotion);
		handleShots();
		tryTargetNearestCreatureByTypeInRadius(FakePlayerHelpers.getTargetClass(), FakePlayerHelpers.getTargetRange());
		tryAttackingUsingFighterOffensiveSkill();
		setBusyThinking(false);
	}
	
	@Override
	protected ShotType getShotType()
	{
		return ShotType.SOULSHOT;
	}
	
	@Override
	protected int[][] getBuffs()
	{
		return FakePlayerHelpers.getFighterBuffs();
	}
	
	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		return Collections.emptyList();
	}
	
	@Override
	protected List<HealingSpell> getHealingSpells()
	{
		return Collections.emptyList();
	}
	
	@Override
	protected List<SupportSpell> getSelfSupportSpells()
	{
		List<SupportSpell> _selfSupportSpells = new ArrayList<>();
		_selfSupportSpells.add(new SupportSpell(440, SpellUsageCondition.MISSINGCP, 1000, 1));
		return _selfSupportSpells;
	}
}
