package net.sf.l2j.commons.gui;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Window.Type;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class InteraceAbout
{
	private JFrame frmAbout;

	private static final String URL_LUCERA = "https://rusacis.com";
	private static final String FORUM = "https://www.xpzone.info";
	private static final String EMAIL = "juliopradrol2j@gmail.com";

	public InteraceAbout()
	{
		initialize();
		frmAbout.setVisible(true);
	}

	private void initialize()
	{
		frmAbout = new JFrame();
		frmAbout.setResizable(false);
		frmAbout.setTitle("Sobre o Painel");
		frmAbout.setBounds(100, 100, 370, 270);
		frmAbout.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frmAbout.setType(Type.UTILITY);
		frmAbout.getContentPane().setLayout(null);

		JLabel lblTitle = new JLabel("Control Panel");
		lblTitle.setFont(new Font("Tahoma", Font.BOLD, 24));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setBounds(10, 10, 334, 40);
		frmAbout.getContentPane().add(lblTitle);

		JLabel lblAuthor = new JLabel("Desenvolvido por Julio Prado");
		lblAuthor.setHorizontalAlignment(SwingConstants.CENTER);
		lblAuthor.setBounds(10, 50, 334, 20);
		frmAbout.getContentPane().add(lblAuthor);

		JLabel lblBase = new JLabel("RUSACIS 3.8 IL PTS");
		lblBase.setHorizontalAlignment(SwingConstants.CENTER);
		lblBase.setBounds(10, 70, 334, 20);
		frmAbout.getContentPane().add(lblBase);

		JLabel lblYear = new JLabel("© 2025 - " + Calendar.getInstance().get(Calendar.YEAR));
		lblYear.setHorizontalAlignment(SwingConstants.CENTER);
		lblYear.setBounds(10, 90, 334, 20);
		frmAbout.getContentPane().add(lblYear);

		JLabel lblForum = createLinkLabel("Fórum: XPZone", FORUM);
		lblForum.setBounds(10, 120, 334, 20);
		frmAbout.getContentPane().add(lblForum);

		JLabel lblEmail = new JLabel("Contato: " + EMAIL);
		lblEmail.setHorizontalAlignment(SwingConstants.CENTER);
		lblEmail.setBounds(10, 145, 334, 20);
		frmAbout.getContentPane().add(lblEmail);

		JLabel lblPaypal = createLinkLabel("RUSACIS", URL_LUCERA);
		lblPaypal.setBounds(10, 170, 334, 20);
		frmAbout.getContentPane().add(lblPaypal);

		JLabel lblThanks = new JLabel("Obrigado por usar o painel ETX!");
		lblThanks.setHorizontalAlignment(SwingConstants.CENTER);
		lblThanks.setFont(new Font("Tahoma", Font.ITALIC, 12));
		lblThanks.setBounds(10, 200, 334, 20);
		frmAbout.getContentPane().add(lblThanks);

		frmAbout.setLocationRelativeTo(null);
	}

	private static JLabel createLinkLabel(String text, String url)
	{
		JLabel label = new JLabel("<html><font color='#f0c93d'><u>" + text + "</u></font></html>");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (Desktop.isDesktopSupported())
				{
					try
					{
						Desktop.getDesktop().browse(new URI(url));
					}
					catch (IOException | URISyntaxException ex)
					{
						ex.printStackTrace();
					}
				}
			}
		});
		return label;
	}
}
