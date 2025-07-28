package net.sf.l2j.gameserver.scripting.script.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.scripting.Quest;

public abstract class Events extends Quest
{
	private static final String UPDATE_STATUS = "SELECT status FROM events_custom_data WHERE event_name = ?";
	private static final String EVENT_INSERT = "REPLACE INTO events_custom_data (event_name, status) VALUES (?,?)";
	private static final String EVENT_DELETE = "UPDATE events_custom_data SET status = ? WHERE event_name = ?";
	
	public Events()
	{
		super(-1, "events");
		
		restoreStatus(0);
	}
	
	public abstract boolean eventStart(int priority);
	
	public abstract boolean eventStop();
	
	public void eventStatusStart(int priority)
	{
		updateStatus(true);
	}
	
	public void eventStatusStop()
	{
		updateStatus(false);
	}
	
	private void restoreStatus(int priority)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_STATUS))
		{
			statement.setString(1, getName());
			try (ResultSet rset = statement.executeQuery())
			{
				int status = 0;
				while (rset.next())
				{
					status = rset.getInt("status");
				}
				
				if (status > 0)
					eventStart(priority);
				else
					eventStop();
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Error: Could not restore custom event data info: " + e);
		}
	}
	
	private void updateStatus(boolean newEvent)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement stmt = con.prepareStatement(newEvent ? EVENT_INSERT : EVENT_DELETE))
		{
			if (newEvent)
			{
				stmt.setString(1, getName());
				stmt.setInt(2, 1);
			}
			else
			{
				stmt.setInt(1, 0);
				stmt.setString(2, getName());
			}
			
			stmt.execute();
		}
		catch (Exception e)
		{
			LOGGER.warn("Error: could not update custom event database!");
		}
	}
}