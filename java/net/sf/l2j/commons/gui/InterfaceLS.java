package net.sf.l2j.commons.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import net.sf.l2j.loginserver.LoginServer;

public class InterfaceLS
{
	private JTextArea console;
	private static final String[] shutdownOptions =
	{
		"Shutdown",
		"Cancel"
	};
	private static final String[] restartOptions =
	{
		"Restart",
		"Cancel"
	};
	
	public InterfaceLS()
	{
		applyTheme();
		initialize();
	}
	
	private void initialize()
	{
		// Initialize console.
		console = new JTextArea();
		console.setEditable(false);
		console.setLineWrap(true);
		console.setWrapStyleWord(true);
		console.setDropMode(DropMode.INSERT);
		console.setFont(new Font("Monospaced", Font.PLAIN, 13));
		console.getDocument().addDocumentListener(new InterfaceLimit(500));
		
		JScrollPane scrollPane = new JScrollPane(console);
		
		JMenuBar menuBar = createMenuBar();
		
		JFrame frame = new JFrame("Login Panel - Limited Edition");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setSize(800, 500);
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());
		frame.setJMenuBar(menuBar);
		frame.add(scrollPane, BorderLayout.CENTER);
		frame.setIconImages(loadIcons());
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, shutdownOptions, shutdownOptions[1]) == 0)
				{
					LoginServer.getInstance().shutdown(false);
				}
			}
		});
		
		redirectSystemStreams();
		
		new SplashScreenLS(".." + File.separator + "images" + File.separator + "splashscreen.png", frame);
	}
	
	private JMenuBar createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		
		JMenu loginMenu = new JMenu("Login");
		JMenuItem shutdown = new JMenuItem("Shutdown");
		shutdown.addActionListener(e ->
		{
			if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, shutdownOptions, shutdownOptions[1]) == 0)
			{
				LoginServer.getInstance().shutdown(false);
			}
		});
		JMenuItem restart = new JMenuItem("Restart");
		restart.addActionListener(e ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart LoginServer?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, restartOptions, restartOptions[1]) == 0)
			{
				LoginServer.getInstance().shutdown(true);
			}
		});
		loginMenu.add(shutdown);
		loginMenu.add(restart);
		
		JMenu fontMenu = new JMenu("Font");
		for (String size : new String[]
		{
			"10",
			"13",
			"16",
			"20",
			"24"
		})
		{
			JMenuItem item = new JMenuItem(size);
			item.addActionListener(e -> console.setFont(new Font("Consolas", Font.PLAIN, Integer.parseInt(size))));
			fontMenu.add(item);
		}
		
		JMenu helpMenu = new JMenu("Help");
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(e -> new InteraceAbout());
		helpMenu.add(about);
		
		menuBar.add(loginMenu);
		menuBar.add(fontMenu);
		menuBar.add(helpMenu);
		
		return menuBar;
	}
	
	private List<Image> loadIcons()
	{
		List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "32x32.png").getImage());
		return icons;
	}
	
	private void updateConsole(String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			console.append(text);
			console.setCaretPosition(console.getText().length());
		});
	}
	
	private void redirectSystemStreams()
	{
		OutputStream out = new OutputStream()
		{
			@Override
			public void write(int b)
			{
				updateConsole(String.valueOf((char) b));
			}
			
			@Override
			public void write(byte[] b, int off, int len)
			{
				updateConsole(new String(b, off, len));
			}
			
			@Override
			public void write(byte[] b)
			{
				write(b, 0, b.length);
			}
		};
		
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
	
	private void applyTheme()
	{
		UIManager.put("control", new Color(45, 60, 90));
		UIManager.put("info", new Color(45, 60, 90));
		UIManager.put("nimbusBase", new Color(15, 45, 80));
		UIManager.put("nimbusBlueGrey", new Color(60, 90, 120));
		UIManager.put("nimbusLightBackground", new Color(28, 40, 58));
		UIManager.put("text", Color.WHITE);
		try
		{
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}