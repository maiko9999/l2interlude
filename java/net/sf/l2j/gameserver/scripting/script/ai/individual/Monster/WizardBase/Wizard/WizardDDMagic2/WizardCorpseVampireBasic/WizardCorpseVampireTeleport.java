package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2.WizardCorpseVampireBasic;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;

public class WizardCorpseVampireTeleport extends WizardCorpseVampireBasic
{
	public WizardCorpseVampireTeleport()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2/WizardCorpseVampireBasic");
	}
	
	public WizardCorpseVampireTeleport(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21588,
		21589
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("3000", npc, null, 10000, 10000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (!(creature instanceof Playable))
		{
			super.onSeeCreature(npc, creature);
			return;
		}
		
		if (npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK && npc.getAI().getLifeTime() > 7)
		{
			if (npc.distance2D(creature) > 200)
			{
				npc.abortAll(false);
				npc.teleportTo(creature.getPosition(), 0);
				npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.TELEPORT_EFFECT), 1000000);
			}
			
			if (npc.isInMyTerritory())
				npc.getAI().addAttackDesire(creature, 200);
		}
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3000"))
			npc.lookNeighbor(500);
		
		return super.onTimer(name, npc, player);
	}
}