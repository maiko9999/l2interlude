package ext.mods.email.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;

import ext.mods.email.sql.EmailDAO;

public class EmailDeliveryTask
{
	private final Map<Integer, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
	
	public void loadAllPending()
	{
		try (Connection con = ConnectionPool.getConnection();
		     PreparedStatement ps = con.prepareStatement("SELECT email_id, expiration_time FROM player_emails WHERE status='PENDING'"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					int emailId = rs.getInt("email_id");
					long expTime = rs.getLong("expiration_time");
					scheduleExpiration(null, emailId, expTime);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	
	public void scheduleExpiration(Player sender, int emailId, long expirationTime)
	{
		long delay = expirationTime - System.currentTimeMillis();
		if (delay <= 0)
		{
			expireEmail(emailId);
			return;
		}
		
		ScheduledFuture<?> task = ThreadPool.schedule(() ->
		{
			expireEmail(emailId);
			activeTasks.remove(emailId);
		}, delay);
		long minutes = delay / 60000;
		if(sender != null)
		sender.sendMessage("Tarefa agendada: o e-mail será expirado em aproximadamente " + minutes + " minuto(s).");
		activeTasks.put(emailId, task);
	}
	
	public void cancel(int emailId)
	{
		ScheduledFuture<?> task = activeTasks.remove(emailId);
		if (task != null)
			task.cancel(false);
	}
	
	private void expireEmail(int emailId)
	{
		// Consulta se ainda está pendente
		if (!EmailDAO.isPending(emailId))
			return;
		EmailDAO.expireAndReturnToSender(emailId);
	}
	
	public static EmailDeliveryTask getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		private static final EmailDeliveryTask _instance = new EmailDeliveryTask();
	}
	
}
