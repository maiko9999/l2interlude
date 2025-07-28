package net.sf.l2j.gameserver.handler.bypasshandlers;

import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.xml.ObserverGroupData;
import net.sf.l2j.gameserver.handler.IBypassHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.ObserverLocation;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class ObserveGroup implements IBypassHandler
{
	private static final String[] COMMANDS = { "observe_group" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		final List<ObserverLocation> locs = ObserverGroupData.getInstance().getObserverLocations(Integer.parseInt(st.nextToken()));
		if (locs == null)
			return false;
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>&$650;<br><br>");
		
		for (ObserverLocation loc : locs)
		{
			StringUtil.append(sb, "<a action=\"bypass -h npc_", target.getObjectId(), "_observe ", loc.getLocId(), "\">&$", loc.getLocId(), ";");
			
			if (loc.getCost() > 0)
				StringUtil.append(sb, " - ", loc.getCost(), " &#57;");
			
			StringUtil.append(sb, "</a><br1>");
		}
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
		html.setHtml(sb.toString());
		
		player.sendPacket(html);
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
