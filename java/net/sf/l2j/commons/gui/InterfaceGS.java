package net.sf.l2j.commons.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;

import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.model.World;

public class InterfaceGS
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
	
	static final String[] abortOptions =
	{
		"Abort",
		"Cancel"
	};
	
	public InterfaceGS()
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
		
		// Initialize menu items.
		final JMenuBar menuBar = new JMenuBar();
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		
		final JMenu mnActions = new JMenu("Game");
		mnActions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnActions);
		
		final JMenuItem mntmShutdowna = new JMenuItem("Shutdown");
		mntmShutdowna.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmShutdowna.addActionListener(arg0 ->
		{
			if (JOptionPane.showOptionDialog(null, "Shutdown GameServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, shutdownOptions, shutdownOptions[1]) == 0)
			{
				final Object answer = JOptionPane.showInputDialog(null, "Shutdown delay in seconds", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "600");
				if (answer != null)
				{
					final String input = ((String) answer).trim();
					if (isDigit(input))
					{
						final int delay = Integer.parseInt(input);
						if (delay > 0)
						{
							Shutdown.getInstance().startShutdown(null, delay, false);
						}
					}
				}
			}
		});
		mnActions.add(mntmShutdowna);
		
		final JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmRestart.addActionListener(arg0 ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart GameServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, restartOptions, restartOptions[1]) == 0)
			{
				final Object answer = JOptionPane.showInputDialog(null, "Restart delay in seconds", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "600");
				if (answer != null)
				{
					final String input = ((String) answer).trim();
					if (isDigit(input))
					{
						final int delay = Integer.parseInt(input);
						if (delay > 0)
						{
							Shutdown.getInstance().startShutdown(null, delay, true);
						}
					}
				}
			}
		});
		mnActions.add(mntmRestart);
		
		final JMenuItem mntmAbort = new JMenuItem("Abort");
		mntmAbort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbort.addActionListener(arg0 ->
		{
			if (JOptionPane.showOptionDialog(null, "Abort server shutdown?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, abortOptions, abortOptions[1]) == 0)
			{
				Shutdown.getInstance().abort(null);
			}
		});
		mnActions.add(mntmAbort);
		
		final JMenu mnBalance = new JMenu("Balance");
		mnBalance.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnBalance);
		
		final JMenuItem viewModifiers = new JMenuItem("Damage");
		viewModifiers.addActionListener(e ->
		{
			JFrame balanceFrame = new JFrame("Information Balance");
			damage model = new damage();
			JTable table = new JTable(model);
			JScrollPane scroll = new JScrollPane(table);
			
			DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer()
			{
				private static final long serialVersionUID = 1L;
				
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					
					if (!isSelected)
					{
						c.setBackground(UIManager.getColor("Table.background"));
						c.setForeground(UIManager.getColor("Table.foreground"));
					}
					else
					{
						c.setBackground(table.getSelectionBackground());
						c.setForeground(table.getSelectionForeground());
					}
					
					if (column >= 2 && column <= 5 && value instanceof Number)
					{
						double val = ((Number) value).doubleValue();
						if (val != 1.0)
						{
							if (!isSelected)
							{
								c.setForeground(val > 1.0 ? new Color(100, 220, 100) : new Color(220, 100, 100));
							}
						}
					}
					
					return c;
				}
			};
			
			for (int i = 2; i <= 3; i++)
			{
				table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
			}
			
			JPanel topPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Filtrar classe: ");
			JTextField filterField = new JTextField();
			topPanel.add(label, BorderLayout.WEST);
			topPanel.add(filterField, BorderLayout.CENTER);
			
			filterField.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(javax.swing.event.DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
				
				@Override
				public void removeUpdate(javax.swing.event.DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
				
				@Override
				public void changedUpdate(javax.swing.event.DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
			});
			
			JButton helpButton = new JButton("❓");
			helpButton.setToolTipText("Clique para entender o sistema de balanceamento");
			
			helpButton.addActionListener(e2 ->
			{
				String message = "➤ PAINEL DE BALANCEAMENTO DE DANO ENTRE CLASSES\n\n" + "▸ Este painel permite controlar o dano físico (P.Atk) e mágico (M.Atk) entre classes PvP.\n" + "▸ Cada linha representa o modificador de dano de uma classe (Attacker) contra outra (Target).\n\n" + "✦ P.Atk → Controla o dano físico causado.\n" + "✦ M.Atk → Controla o dano mágico causado.\n\n" + "✔ Valor 1.0: Dano normal, sem alteração.\n" + "✔ Valor acima de 1.0: Aumenta o dano (ex: 1.2 = 20% a mais).\n" + "✔ Valor abaixo de 1.0: Reduz o dano (ex: 0.8 = 20% a menos).\n\n" + "▸ Os valores modificam diretamente o cálculo do dano final entre as classes.\n" + "▸ Os valores coloridos indicam modificações:\n" + "   - Verde: aumento de dano.\n" + "   - Vermelho: redução de dano.\n\n" + "▸ Use o filtro acima para buscar uma classe específica e editar seus modificadores.";
				
				JOptionPane.showMessageDialog(balanceFrame, message, "Ajuda - Sistema de Balanceamento", JOptionPane.INFORMATION_MESSAGE);
			});
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(topPanel, BorderLayout.NORTH);
			mainPanel.add(scroll, BorderLayout.CENTER);
			topPanel.add(helpButton, BorderLayout.EAST);
			
			// Ícones da janela
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "16x16.png").getImage());
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "32x32.png").getImage());
			
			balanceFrame.setIconImages(icons);
			balanceFrame.setContentPane(mainPanel);
			balanceFrame.setSize(600, 400);
			balanceFrame.setLocationRelativeTo(null);
			balanceFrame.setVisible(true);
		});
		mnBalance.add(viewModifiers);
		
		// defence
		
		final JMenuItem viewDefence = new JMenuItem("Defence");
		viewDefence.addActionListener(e ->
		{
			JFrame balanceFrame = new JFrame("Information Balance");
			defence model = new defence();
			JTable table = new JTable(model);
			JScrollPane scroll = new JScrollPane(table);
			
			DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer()
			{
				private static final long serialVersionUID = 1L;
				
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					
					if (!isSelected)
					{
						c.setBackground(UIManager.getColor("Table.background"));
						c.setForeground(UIManager.getColor("Table.foreground"));
					}
					else
					{
						c.setBackground(table.getSelectionBackground());
						c.setForeground(table.getSelectionForeground());
					}
					if (column >= 2 && column <= 5 && value instanceof Number)
					{
						double val = ((Number) value).doubleValue();
						if (val != 1.0)
						{
							if (!isSelected)
							{
								
								c.setForeground(val > 1.0 ? new Color(100, 220, 100) : new Color(220, 100, 100));
							}
						}
					}
					
					return c;
				}
			};
			
			for (int i = 2; i <= 3; i++)
			{
				table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
			}
			
			JPanel topPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Filtrar classe: ");
			JTextField filterField = new JTextField();
			topPanel.add(label, BorderLayout.WEST);
			topPanel.add(filterField, BorderLayout.CENTER);
			
			filterField.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(javax.swing.event.DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
				
				@Override
				public void removeUpdate(javax.swing.event.DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
				
				@Override
				public void changedUpdate(javax.swing.event.DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
			});
			
			JButton helpButton = new JButton("❓");
			helpButton.setToolTipText("Clique para entender o sistema de defesa entre classes");
			
			helpButton.addActionListener(e2 ->
			{
				String message = "➤ PAINEL DE BALANCEAMENTO DE DEFESA ENTRE CLASSES\n\n" + "▸ Este painel permite controlar a defesa física (P.Def) e mágica (M.Def) que uma classe recebe contra outra.\n" + "▸ Cada linha representa o modificador de defesa que a classe Target (alvo) terá ao enfrentar a classe Attacker (atacante).\n\n" + "✦ P.Def → Controla a defesa física que será aplicada contra ataques físicos.\n" + "✦ M.Def → Controla a defesa mágica que será aplicada contra ataques mágicos.\n\n" + "✔ Valor 1.0: Defesa normal, sem alteração.\n" + "✔ Valor acima de 1.0: Aumenta a defesa (ex: 1.2 = 20% a mais de resistência).\n" + "✔ Valor abaixo de 1.0: Reduz a defesa (ex: 0.8 = 20% mais vulnerável).\n\n" + "▸ Os valores modificam diretamente a resistência que o alvo terá ao ser atacado por aquela classe.\n" + "▸ Os valores coloridos indicam modificações:\n" + "   - Verde: aumento de defesa.\n" + "   - Vermelho: redução de defesa.\n\n" + "▸ Use o campo de filtro para buscar uma classe específica e ajustar suas vulnerabilidades defensivas.";
				
				JOptionPane.showMessageDialog(balanceFrame, message, "Ajuda - Sistema de Defesa", JOptionPane.INFORMATION_MESSAGE);
			});
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(topPanel, BorderLayout.NORTH);
			mainPanel.add(scroll, BorderLayout.CENTER);
			topPanel.add(helpButton, BorderLayout.EAST);
			// Ícones da janela
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "16x16.png").getImage());
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "32x32.png").getImage());
			
			balanceFrame.setIconImages(icons);
			balanceFrame.setContentPane(mainPanel);
			balanceFrame.setSize(600, 400);
			balanceFrame.setLocationRelativeTo(null);
			balanceFrame.setVisible(true);
		});
		mnBalance.add(viewDefence);
		
		// vulnerability
		
		final JMenuItem viewvulnerability = new JMenuItem("Vulnerability");
		viewvulnerability.addActionListener(e ->
		{
			JFrame balanceFrame = new JFrame("Information Balance");
			vulnerabilityefence model = new vulnerabilityefence();
			JTable table = new JTable(model);
			JScrollPane scroll = new JScrollPane(table);
			
			DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer()
			{
				private static final long serialVersionUID = 1L;
				
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					
					if (!isSelected)
					{
						c.setBackground(UIManager.getColor("Table.background"));
						c.setForeground(UIManager.getColor("Table.foreground"));
					}
					else
					{
						c.setBackground(table.getSelectionBackground());
						c.setForeground(table.getSelectionForeground());
					}
					if (column >= 2 && column <= 5 && value instanceof Number)
					{
						double val = ((Number) value).doubleValue();
						if (val != 1.0)
						{
							if (!isSelected)
							{
								
								c.setForeground(val > 1.0 ? new Color(100, 220, 100) : new Color(220, 100, 100));
							}
						}
					}
					
					return c;
				}
			};
			
			for (int i = 1; i <= 1; i++)
			{
				table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
			}
			
			JPanel topPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Filtrar Type: ");
			JTextField filterField = new JTextField();
			topPanel.add(label, BorderLayout.WEST);
			topPanel.add(filterField, BorderLayout.CENTER);
			
			filterField.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
				
				@Override
				public void removeUpdate(DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
				
				@Override
				public void changedUpdate(DocumentEvent e)
				{
					model.filter(filterField.getText());
				}
			});
			
			JButton helpButton = new JButton("❓");
			helpButton.setToolTipText("Clique para entender o sistema de vulnerabilidade por tipo de skill");
			
			helpButton.addActionListener(e2 ->
			{
				String message = "➤ PAINEL DE VULNERABILIDADE POR TIPO DE SKILL\n\n" + "▸ Esta aba permite controlar o quanto as classes são vulneráveis a efeitos de habilidades (skills) específicas.\n\n" + "✦ Skill Type → Define o tipo de efeito da skill (ex: PARALYZE, SLEEP, STUN etc).\n" + "✦ Multiplier → Define o modificador de chance que o tipo de efeito terá ao ser aplicado na vítima.\n\n" + "✔ Valor 1.0: Chance normal da skill.\n" + "✔ Valor acima de 1.0: Aumenta a chance de aplicação (ex: 1.2 = 20% mais fácil de aplicar).\n" + "✔ Valor abaixo de 1.0: Reduz a chance de aplicação (ex: 0.8 = 20% mais difícil de aplicar).\n\n" + "▸ Esses valores atuam sobre a **resistência global** de todas as classes a cada tipo de efeito.\n" + "▸ Por exemplo, se STUN estiver com 0.8, todas as classes do jogo terão 20% mais resistência contra stun.\n\n" + "▸ Use esse painel para balancear o impacto de efeitos de controle no PvP/PvE, reduzindo abusos ou fortalecendo tipos pouco usados.";
				
				JOptionPane.showMessageDialog(balanceFrame, message, "Ajuda - Vulnerabilidades de Skills", JOptionPane.INFORMATION_MESSAGE);
			});
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(topPanel, BorderLayout.NORTH);
			mainPanel.add(scroll, BorderLayout.CENTER);
			
			topPanel.add(helpButton, BorderLayout.EAST);
			
			// Ícones da janela
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "16x16.png").getImage());
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "32x32.png").getImage());
			
			balanceFrame.setIconImages(icons);
			balanceFrame.setContentPane(mainPanel);
			balanceFrame.setSize(600, 400);
			balanceFrame.setLocationRelativeTo(null);
			balanceFrame.setVisible(true);
		});
		mnBalance.add(viewvulnerability);
		
		final JMenu mnAnnounce = new JMenu("Announce");
		mnAnnounce.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnAnnounce);
		
		final JMenuItem mntmNormal = new JMenuItem("Normal");
		mntmNormal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmNormal.addActionListener(arg0 ->
		{
			final Object input = JOptionPane.showInputDialog(null, "Announce message", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "");
			if (input != null)
			{
				final String message = ((String) input).trim();
				if (!message.isEmpty())
				{
					World.announceToOnlinePlayers(message, false);
				}
			}
		});
		mnAnnounce.add(mntmNormal);
		
		final JMenuItem mntmCritical = new JMenuItem("Critical");
		mntmCritical.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmCritical.addActionListener(arg0 ->
		{
			final Object input = JOptionPane.showInputDialog(null, "Critical announce message", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "");
			if (input != null)
			{
				final String message = ((String) input).trim();
				if (!message.isEmpty())
				{
					World.announceToOnlinePlayers(message, true);
				}
			}
		});
		mnAnnounce.add(mntmCritical);
		
		final JMenu mnFont = new JMenu("Font");
		mnFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnFont);
		
		final String[] fonts =
		{
			"10",
			"13",
			"16",
			"21",
			"27",
			"33"
		};
		for (String font : fonts)
		{
			final JMenuItem mntmFont = new JMenuItem(font);
			mntmFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			mntmFont.addActionListener(arg0 -> console.setFont(new Font("Monospaced", Font.PLAIN, Integer.parseInt(font))));
			mnFont.add(mntmFont);
		}
		
		final JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnHelp);
		
		final JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbout.addActionListener(arg0 -> new InteraceAbout());
		mnHelp.add(mntmAbout);
		
		// Set Panels.
		final JPanel systemPanel = new InterfaceGSInfo();
		final JScrollPane scrollPanel = new JScrollPane(console);
		scrollPanel.setBounds(0, 0, 300, 160);
		final JLayeredPane layeredPanel = new JLayeredPane();
		layeredPanel.add(scrollPanel, 0, 0);
		layeredPanel.add(systemPanel, 1, 0);
		
		// Set frame.
		final JFrame frame = new JFrame("Game Panel - Limited Edition");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown server immediately?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, shutdownOptions, shutdownOptions[1]) == 0)
				{
					Shutdown.getInstance().startShutdown(null, 1, false);
				}
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent ev)
			{
				scrollPanel.setSize(frame.getContentPane().getSize());
				systemPanel.setLocation(frame.getContentPane().getWidth() - systemPanel.getWidth() - 34, systemPanel.getY());
			}
		});
		frame.setJMenuBar(menuBar);
		frame.setIconImages(loadIcons());
		frame.add(layeredPanel, BorderLayout.CENTER);
		frame.getContentPane().setPreferredSize(new Dimension(1000, 600));
		frame.pack();
		frame.setLocationRelativeTo(null);
		
		redirectSystemStreams();
		
		new SplashScreenLS(".." + File.separator + "images" + File.separator + "splashscreen.png", frame);
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
	
	public static boolean isDigit(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		for (char c : text.toCharArray())
		{
			if (!Character.isDigit(c))
			{
				return false;
			}
		}
		return true;
	}
}