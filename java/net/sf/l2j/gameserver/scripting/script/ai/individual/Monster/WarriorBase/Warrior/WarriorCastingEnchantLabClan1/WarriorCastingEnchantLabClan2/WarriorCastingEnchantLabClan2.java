package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastingEnchantLabClan1.WarriorCastingEnchantLabClan2;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastingEnchantLabClan1.WarriorCastingEnchantLabClan1;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorCastingEnchantLabClan2 extends WarriorCastingEnchantLabClan1
{
	public WarriorCastingEnchantLabClan2()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCastingEnchantLabClan1/WarriorCastingEnchantLabClan2");
	}
	
	public WarriorCastingEnchantLabClan2(String descr)
	{
		super(descr);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		final Party party0 = attacker.getParty();
		if (party0 != null)
		{
			if (party0.getMembersCount() >= 8)
			{
				final L2Skill selfBuff2 = getNpcSkillByType(npc, NpcSkillType.SELF_BUFF2);
				if (getAbnormalLevel(npc, selfBuff2) <= 0)
					npc.getAI().addCastDesire(attacker, selfBuff2, 1000000);
			}
			else if (party0.getMembersCount() >= 6)
			{
				final L2Skill selfBuff1 = getNpcSkillByType(npc, NpcSkillType.SELF_BUFF1);
				if (getAbnormalLevel(npc, selfBuff1) <= 0)
					npc.getAI().addCastDesire(attacker, selfBuff1, 1000000);
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
}