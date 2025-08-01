package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.LV3Monster;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class LV3SongDance extends LV3Monster
{
	public LV3SongDance()
	{
		super("ai/individual/Monster/LV3Monster");
	}
	
	public LV3SongDance(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		27269,
		27270,
		27272,
		27288
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 0;
		npc._i_ai1 = 0;
		npc._i_ai2 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			if (damage == 0)
				damage = 1;
			
			npc.getAI().addAttackDesire(attacker, ((1.000000 * damage) / (npc.getStatus().getLevel() + 7)) * 100);
		}
		
		if (Rnd.get(100) < 33 && npc._i_ai0 == 0)
			npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.DEBUFF1), 1000000);
		
		if (Rnd.get(100) < 33 && npc._i_ai1 == 0)
			npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.DEBUFF2), 1000000);
		
		if (Rnd.get(100) < 33 && npc._i_ai2 == 0)
			npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.DEBUFF3), 1000000);
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		if (skill == getNpcSkillByType(npc, NpcSkillType.DEBUFF1))
			npc._i_ai0 = 1;
		
		if (skill == getNpcSkillByType(npc, NpcSkillType.DEBUFF2))
			npc._i_ai1 = 1;
		
		if (skill == getNpcSkillByType(npc, NpcSkillType.DEBUFF3))
			npc._i_ai2 = 1;
	}
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (caster != null)
		{
			if (caster.getObjectId() == npc._param2)
			{
				if (skill.getEffectId() == getNpcSkillByType(npc, NpcSkillType.DEBUFF1).getEffectId())
					npc._i_ai0 = 0;
				
				if (skill.getEffectId() == getNpcSkillByType(npc, NpcSkillType.DEBUFF2).getEffectId())
					npc._i_ai1 = 0;
				
				if (skill.getEffectId() == getNpcSkillByType(npc, NpcSkillType.DEBUFF3).getEffectId())
					npc._i_ai2 = 0;
			}
		}
		
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}
}