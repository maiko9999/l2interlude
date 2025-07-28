package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.List;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import ext.mods.fakeplayer.FakePlayer;
import ext.mods.fakeplayer.FakePlayerManager;
import ext.mods.fakeplayer.FakePlayerTaskManager;

public class AdminCommand implements IAdminCommandHandler
{
	private final String fakesFolder = "fakeplayers/";
	private static final int PAGE_SIZE = 10;
	private static final int MAX_VISIBLE_PAGES = 3;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_fakes",
		"admin_spawnrandom",
		"admin_spawnrandomgrupe",
		"admin_deletefake",
		"admin_findfake"
	
	};
	
	private void showFakeDashboard(Player admin, int page)
	{
		List<FakePlayer> allFakes = FakePlayerManager.getInstance().getFakePlayers();
		int total = allFakes.size();
		
		if (total == 0)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(admin.getLocale(), "html/admin/" + fakesFolder + "index.htm");
			html.replace("%fakecount%", "0");
			html.replace("%taskcount%", FakePlayerTaskManager.getInstance().getTaskCount());
			html.replace("%list%", "");
			html.replace("%pages%", "");
			admin.sendPacket(html);
			return;
		}
		
		int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
		page = Math.max(1, Math.min(page, totalPages));
		int start = (page - 1) * PAGE_SIZE;
		int end = Math.min(start + PAGE_SIZE, total);
		
		// Lista formatada
		StringBuilder list = new StringBuilder("<table width=300>");
		for (int i = start; i < end; i++)
		{
			FakePlayer fake = allFakes.get(i);
			list.append("<tr><td align=left width=300><font color=\"LEVEL\">" + fake.getName() + "</font></td></tr>");
		}
		list.append("</table>");
		
		StringBuilder pages = new StringBuilder();
		pages.append("<table width=150><tr>");
		
		int half = MAX_VISIBLE_PAGES / 2;
		int startPage = Math.max(1, page - half);
		int endPage = Math.min(totalPages, startPage + MAX_VISIBLE_PAGES - 1);
		if (endPage - startPage < MAX_VISIBLE_PAGES - 1)
			startPage = Math.max(1, endPage - MAX_VISIBLE_PAGES + 1);
		
		// Primeira página
		if (startPage > 1)
		{
			pages.append("<td><button value=\"1\" action=\"bypass -h admin_fakes 1\" width=32 height=16 back=\"sek.cbui94\" fore=\"sek.cbui92\"></button></td>");
		}
		
		// Botões de página
		for (int i = startPage; i <= endPage; i++)
		{
			if (i == page)
			{
				// Página atual em destaque (sem botão clicável)
				pages.append("<td><button value=\"" + i + "\" action=\"\" width=32 height=16 back=\"sek.cbui92\" fore=\"sek.cbui94\"></button></td>");
			}
			else
			{
				pages.append("<td><button value=\"" + i + "\" action=\"bypass -h admin_fakes " + i + "\" width=32 height=16 back=\"sek.cbui94\" fore=\"sek.cbui92\"></button></td>");
			}
		}
		
		// Última página
		if (endPage < totalPages)
		{
			pages.append("<td><button value=\"" + totalPages + "\" action=\"bypass -h admin_fakes " + totalPages + "\" width=32 height=16 back=\"sek.cbui94\" fore=\"sek.cbui92\"></button></td>");
		}
		
		pages.append("</tr></table>");
		
		// HTML final
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(admin.getLocale(), "html/admin/" + fakesFolder + "index.htm");
		html.replace("%fakecount%", String.valueOf(total));
		html.replace("%taskcount%", FakePlayerTaskManager.getInstance().getTaskCount());
		html.replace("%list%", list.toString());
		html.replace("%pages%", pages.toString());
		admin.sendPacket(html);
	}
	
	@Override
	public void useAdminCommand(String command, Player admin)
	{
		if (command.startsWith("admin_fakes"))
		{
			try
			{
				String[] parts = command.split(" ");
				
				if (parts.length < 2)
				{
					showFakeDashboard(admin, 1);
				}
				else
				{
					int page = Integer.parseInt(parts[1]);
					showFakeDashboard(admin, page);
				}
				
			}
			catch (Exception e)
			{
				showFakeDashboard(admin, 1);
			}
			
		}
		
		if (command.startsWith("admin_deletefake"))
		{
			if (admin.getTarget() != null && admin.getTarget() instanceof FakePlayer)
			{
				FakePlayer fakePlayer = (FakePlayer) admin.getTarget();
				fakePlayer.despawnPlayer();
			}
			showFakeDashboard(admin, 1);
		}
		
		if (command.startsWith("admin_spawnrandom"))
		{
			FakePlayerManager.getInstance().spawnPlayer(admin.getX(), admin.getY(), admin.getZ());
			
			showFakeDashboard(admin, 1);
		}
		
		if (command.startsWith("admin_spawnrandomgrupe"))
		{
			FakePlayerManager.getInstance().spawnPlayerGroup(admin.getX(), admin.getY(), admin.getZ(), 8);
			showFakeDashboard(admin, 1);
		}
		
		if (command.startsWith("admin_findfake"))
		{
			String[] params = command.split(" ");
			if (params.length < 2)
			{
				admin.sendMessage("Uso correto: //admin_findfake <nome>");
			}
			else
			{
				String fakeName = params[1];
				FakePlayer targetFake = null;
				
				for (FakePlayer fake : FakePlayerManager.getInstance().getFakePlayers())
				{
					if (fake.getName().equalsIgnoreCase(fakeName))
					{
						targetFake = fake;
						break;
					}
				}
				
				if (targetFake != null)
				{
					admin.teleportTo(targetFake.getX(), targetFake.getY(), targetFake.getZ(), 75);
					admin.sendMessage("Teletransportado para FakePlayer: " + fakeName);
				}
				else
				{
					admin.sendMessage("FakePlayer " + fakeName + " não encontrado.");
				}
			}
			showFakeDashboard(admin, 1);
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}
