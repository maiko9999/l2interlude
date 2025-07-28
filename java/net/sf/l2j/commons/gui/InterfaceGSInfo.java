package net.sf.l2j.commons.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.sf.l2j.gameserver.model.World;

public class InterfaceGSInfo extends JPanel
{
	private static final long serialVersionUID = -1;
	
	protected static final Logger LOGGER = Logger.getLogger(InterfaceGSInfo.class.getName());
	
	protected static final long START_TIME = System.currentTimeMillis();
	
	public InterfaceGSInfo()
	{
		
		setBackground(new Color(18, 30, 49));
		setBorder(new LineBorder(new Color(50, 50, 50), 1, false));
		
		setBounds(300, 100, 270, 160);
		setOpaque(true);
		setLayout(null);
		
		final JLabel lblElapsedTime = new JLabel("Elapsed time");
		lblElapsedTime.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblElapsedTime.setBounds(10, 77, 264, 17);
		add(lblElapsedTime);
		
		final JLabel lblBuildDate = new JLabel("Build date");
		lblBuildDate.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblBuildDate.setBounds(10, 113, 264, 17);
		add(lblBuildDate);
		
		final JLabel lblProtocol = new JLabel("Protocol");
		lblProtocol.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblProtocol.setBounds(10, 5, 264, 17);
		add(lblProtocol);
		
		final JLabel lblConnected = new JLabel("Connected");
		lblConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblConnected.setBounds(10, 23, 264, 17);
		add(lblConnected);
		
		final JLabel lblMaxConnected = new JLabel("Max connected");
		lblMaxConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblMaxConnected.setBounds(10, 41, 264, 17);
		add(lblMaxConnected);
		
		final JLabel lblOfflineShops = new JLabel("Offline trade");
		lblOfflineShops.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblOfflineShops.setBounds(10, 59, 264, 17);
		add(lblOfflineShops);
		
		final JLabel lblJavaVersion = new JLabel("Build JDK");
		lblJavaVersion.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblJavaVersion.setBounds(10, 95, 264, 17);
		add(lblJavaVersion);
		
		final JLabel lblMemoryUsage = new JLabel("CPU");
		lblMemoryUsage.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblMemoryUsage.setBounds(10, 131, 264, 17);
		add(lblMemoryUsage);
		
		// Set initial values.
		lblElapsedTime.setText("Elapsed: 0 sec");
		lblBuildDate.setText("Build date: Unavailable");
		lblProtocol.setText("Protocol: 0");
		lblConnected.setText("Connected: 0");
		lblMaxConnected.setText("Max connected: 0");
		lblOfflineShops.setText("Offline trade: 0");
		lblJavaVersion.setText("Java version: " + System.getProperty("java.version"));
		lblMemoryUsage.setText("Used: 0 MB / 0 MB");
		
		try
		{
			
			lblBuildDate.setText("Build date: " + "2025-" + Calendar.getInstance().get(Calendar.YEAR));
			
		}
		catch (Exception e)
		{
			// Handled above.
		}
		
		// Initial update task.
		new Timer().schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				lblProtocol.setText(730 + " Protocols: " + 746);
			}
		}, 4500);
		
		// Repeating elapsed time task.
		new Timer().scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				final int playerCount = World.getInstance().getPlayers().size();
				if (World.getInstance().MAX_CONNECTED_COUNT < playerCount)
				{
					World.getInstance().MAX_CONNECTED_COUNT = playerCount;
					if (playerCount > 1)
					{
						System.out.println("[GameServer] New record for online players: " + playerCount);
					}
				}
				lblElapsedTime.setText("Elapsed: " + getDurationBreakdown(System.currentTimeMillis() - START_TIME));
				lblConnected.setText("Connected: " + playerCount);
				lblMaxConnected.setText("Max connected: " + World.getInstance().MAX_CONNECTED_COUNT);
				lblOfflineShops.setText("Offline trade: " + World.getInstance().OFFLINE_TRADE_COUNT);
				// Update memory usage
				Runtime runtime = Runtime.getRuntime();
				long totalMemory = runtime.totalMemory();
				long freeMemory = runtime.freeMemory();
				long usedMemory = totalMemory - freeMemory;
				
				lblMemoryUsage.setText("" + formatMemorySize(usedMemory) + " / " + formatMemorySize(totalMemory) + "");
				
			}
		}, 1000, 1000);
	}
	
	static String formatMemorySize(long bytes)
	{
		if (bytes < 1024)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		char pre = "KMGTPE".charAt(exp - 1);
		double size = bytes / Math.pow(1024, exp);
		return String.format(exp == 1 ? "%.0f %sB" : "%.2f %sB", size, pre);
	}
	
	static String getDurationBreakdown(long millis)
	{
		long remaining = millis;
		final long days = TimeUnit.MILLISECONDS.toDays(remaining);
		remaining -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(remaining);
		remaining -= TimeUnit.HOURS.toMillis(hours);
		final long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
		remaining -= TimeUnit.MINUTES.toMillis(minutes);
		final long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
		return (days + "d " + hours + "h " + minutes + "m " + seconds + "s");
	}
}