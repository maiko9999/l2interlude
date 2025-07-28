package net.sf.l2j.gameserver.scripting.script.ai.boss.queenant;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class QueenAntPrivateAntLarva extends DefaultNpc
{
	public QueenAntPrivateAntLarva()
	{
		super("ai/boss/queenant");
	}
	
	public QueenAntPrivateAntLarva(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29002 // queen_ant_larva
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("2001", npc, null, 5000, 5000);
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2001"))
		{
			if (!npc.hasMaster() || npc.getMaster().isDead())
			{
				npc.deleteMe();
				cancelQuestTimers(npc);
			}
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!Config.RAID_DISABLE_CURSE && attacker.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
			npc.getAI().addCastDesire(attacker, 4515, 1, 1000000);
	}
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (!Config.RAID_DISABLE_CURSE && caster.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
		{
			npc.getAI().addCastDesire(caster, 4215, 1, 1000000);
			return;
		}
		
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}
}