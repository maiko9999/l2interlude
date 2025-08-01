package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * This class extends {@link Attackable} and manages all {@link Guard}s.<br>
 * <br>
 * A {@link Guard} is used to protect Players from Player Killers (PKs).
 */
public final class Guard extends Attackable
{
	public Guard(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onSpawn()
	{
		setNoRndWalk(true);
		super.onSpawn();
	}
	
	@Override
	public String getHtmlPath(Player player, int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "html/guard/" + filename + ".htm";
	}
	
	@Override
	public void onInteract(Player player)
	{
		switch (getNpcId())
		{
			case 30733, 31032, 31033, 31034, 31035, 31036, 31671, 31672, 31673, 31674: // Guards in start villages & patrols
				return;
		}
		
		getAI().onRandomAnimation(Rnd.get(8));
		
		player.getQuestList().setLastQuestNpcObjectId(getObjectId());
		
		List<Quest> scripts = getTemplate().getEventQuests(EventHandler.FIRST_TALK);
		if (scripts.size() == 1)
			scripts.get(0).notifyFirstTalk(this, player);
		else
			showChatWindow(player);
	}
	
	@Override
	public boolean isGuard()
	{
		return true;
	}
	
	@Override
	public int getDriftRange()
	{
		return 20;
	}
}