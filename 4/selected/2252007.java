package edu.upmc.opi.caBIG.caTIES.gate.authentication;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java16.SwingWorker;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import org.apache.log4j.Logger;
import org.ietf.jgss.GSSCredential;
import edu.upmc.opi.caBIG.caTIES.client.vr.authentication.Authenticator;
import edu.upmc.opi.caBIG.caTIES.client.vr.desktop.actions.AboutAction;
import edu.upmc.opi.caBIG.caTIES.client.vr.utils.UIUtilities;
import edu.upmc.opi.caBIG.caTIES.common.App;
import edu.upmc.opi.caBIG.caTIES.common.CaTIES_CertificateUtils;
import edu.upmc.opi.caBIG.caTIES.middletier.CaTIES_UserImpl;
import edu.upmc.opi.caBIG.caTIES.security.CaTIES_SecurityManager;
import edu.upmc.opi.caBIG.caTIES.security.CertificateCopier;

/**
 * Dialog to login to the system.
 * 
 * @author Girish Chavan
 */
public class LoginDialog extends JDialog {

    public class CtrmConfigLocation {

        public String name;

        public String url;
    }

    private static final String CERTIFICATE_NAME = "nciCA.1";

    /**
	 * The Constant LOGIN_ERROR1.
	 */
    public static final String LOGIN_ERROR1 = "Invalid Username or Password";

    private static Logger logger = Logger.getLogger(LoginDialog.class);

    JButton initCancelButton;

    /**
	 * The card panel.
	 */
    JPanel cardPanel;

    /**
	 * The card layout.
	 */
    CardLayout cardLayout;

    /**
	 * The instrlabel.
	 */
    JLabel instrlabel = new JLabel("Enter your username and password");

    /**
	 * The initlabel.
	 */
    JLabel initlabel = new JLabel("Initializing...");

    /**
	 * The login button.
	 */
    JButton loginButton = new JButton("Login");

    /**
	 * Field m_succeeded.
	 */
    private boolean m_succeeded = false;

    /**
	 * Field m_loginNameBox.
	 */
    private JTextField m_loginNameBox = new JTextField();

    /**
	 * Field m_passwordBox.
	 */
    private JPasswordField m_passwordBox = new JPasswordField();

    /**
	 * The self.
	 */
    JDialog self;

    /**
	 * The login listener.
	 */
    private LoginListener loginListener;

    /**
	 * Inits the app objects.
	 */
    @SuppressWarnings("unchecked")
    private void initAppObjects() {
        SwingWorker worker = new SwingWorker() {

            public Object doInBackground() {
                UIUtilities.setGlobalWaitCursor(self);
                Thread.currentThread().setName("LoginDialog:initAppObjects");
                if (!CertificateCopier.initCertificates()) return "noCertificates";
                return "done";
            }

            public void finished() {
                try {
                    if (this.get() == "noCertificates") {
                        initCancelButton.setText("Exit");
                        initlabel.setText("No root certificate found in user directory.");
                        return;
                    }
                    cardLayout.show(cardPanel, "LOGIN");
                    UIUtilities.setGlobalNoWaitCursor(self);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.start();
    }

    protected boolean doGlobusCertificatesExist() {
        String path = System.getProperty("user.home") + File.separator + ".globus" + File.separator + "certificates" + File.separator + CERTIFICATE_NAME;
        File f = new File(path);
        if (!f.exists()) {
            logger.info("No root certificate found in user.home. Copying certificate to user.home");
            try {
                path = System.getProperty("user.home");
                f = new File(path);
                if (!f.exists()) {
                    logger.error("Could not find user.home directory:" + path);
                    return false;
                }
                path += File.separator + ".globus";
                f = new File(path);
                if (!f.exists()) f.mkdir();
                path += File.separator + "certificates";
                f = new File(path);
                if (!f.exists()) f.mkdir();
                path += File.separator + CERTIFICATE_NAME;
                f = new File(path);
                f.createNewFile();
                InputStream in = this.getClass().getResourceAsStream("/certificates/" + CERTIFICATE_NAME);
                if (in == null) {
                    logger.error("Could not find certificate file to be copied in the classpath");
                    return false;
                }
                copyInputStream(in, new BufferedOutputStream(new FileOutputStream(f)));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failed to copy certificate to user.home");
                return false;
            }
        }
        return true;
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[512];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    /**
	 * Constructor.
	 * 
	 * @param parent
	 *            Frame
	 * @param desktop
	 *            the desktop
	 */
    public LoginDialog(Frame desktop) {
        super(desktop, "caTIES Login (" + (String) App.getAppObject("Version") + ")", true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        self = this;
        JLabel upmcbanner = new JLabel();
        ImageIcon ico = null;
        java.net.URL icourl = this.getClass().getResource("/images/catiesbanner.png");
        if (icourl != null) {
            ico = new ImageIcon(icourl);
        }
        upmcbanner.setIcon(ico);
        upmcbanner.setPreferredSize(new Dimension(ico.getIconWidth() - 3, ico.getIconHeight()));
        JLabel verlabel = new JLabel((String) App.getAppObject("Version"));
        JPanel verpanel = new JPanel(new BorderLayout());
        verpanel.add(verlabel, BorderLayout.EAST);
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(buildloginpanel(), "LOGIN");
        JPanel vercardpanel = new JPanel(new BorderLayout());
        vercardpanel.add(verpanel, BorderLayout.NORTH);
        vercardpanel.add(cardPanel, BorderLayout.CENTER);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(upmcbanner, BorderLayout.NORTH);
        mainPanel.add(vercardpanel, BorderLayout.CENTER);
        mainPanel.setBackground(Aesthetics.readOnlyBackgroundColor);
        upmcbanner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 215, 215)));
        this.getContentPane().add(mainPanel);
        this.pack();
        setResizable(false);
        setLocationRelativeTo(desktop);
        verlabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        verpanel.setOpaque(false);
        vercardpanel.setOpaque(false);
        cardPanel.setOpaque(false);
        initAppObjects();
    }

    /**
	 * Authenticate.
	 * 
	 * @param password
	 *            the password
	 * @param username
	 *            the username
	 * 
	 * @return the object
	 */
    private Object authenticate(String username, String password) {
        try {
            Authenticator auth = new Authenticator();
            GSSCredential cred = auth.authenticate(username, password);
            String distinguishedName = Authenticator.getDistinguishedName(cred);
            if (distinguishedName == null) {
                return "Login Failed: " + LOGIN_ERROR1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Login Failed: " + e.getMessage();
        }
        return "Logged In";
    }

    /**
	 * The Class LoginListener.
	 */
    class LoginListener implements ActionListener {

        /**
		 * Action performed.
		 * 
		 * @param evt
		 *            the evt
		 */
        public void actionPerformed(ActionEvent evt) {
            instrlabel.setText("Logging In...");
            m_loginNameBox.setEnabled(false);
            m_passwordBox.setEnabled(false);
            loginButton.setEnabled(false);
            SwingWorker worker = new SwingWorker() {

                public Object doInBackground() {
                    Thread.currentThread().setName("LoginDialog:LoginButtonListener");
                    Object status = authenticate(m_loginNameBox.getText(), new String(m_passwordBox.getPassword()));
                    return status;
                }

                public void finished() {
                    String status;
                    try {
                        status = (String) get();
                        if (status.length() > 65) {
                            instrlabel.setToolTipText(status);
                            status = status.substring(0, 62) + "...";
                        }
                        instrlabel.setText(status);
                        m_loginNameBox.setEnabled(true);
                        m_passwordBox.setEnabled(true);
                        loginButton.setEnabled(true);
                        if (status.equals("Logged In")) {
                            m_succeeded = true;
                            dispose();
                            return;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            };
            worker.start();
        }
    }

    /**
	 * Buildloginpanel.
	 * 
	 * @return the J panel
	 */
    private JPanel buildloginpanel() {
        loginListener = new LoginListener();
        loginButton.addActionListener(loginListener);
        ActionListener lst = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        };
        JButton cancelButton = new JButton("Exit");
        cancelButton.addActionListener(lst);
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new AboutAction((Frame) App.getAppObject("Desktop")));
        JPanel bpanel = new JPanel();
        bpanel.setLayout(new BoxLayout(bpanel, BoxLayout.X_AXIS));
        bpanel.add(Box.createHorizontalGlue());
        bpanel.add(aboutButton);
        bpanel.add(Box.createHorizontalStrut(5));
        bpanel.add(loginButton);
        bpanel.add(Box.createHorizontalStrut(5));
        bpanel.add(cancelButton);
        JLabel namelabel = new JLabel("UserImplname");
        JLabel passlabel = new JLabel("Password");
        JPanel lpanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.insets = new Insets(10, 10, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        lpanel.add(instrlabel, c);
        c.gridwidth = 1;
        c.gridy = 1;
        c.insets = new Insets(10, 30, 0, 15);
        lpanel.add(namelabel, c);
        c.gridy = 2;
        c.insets = new Insets(5, 30, 0, 15);
        lpanel.add(passlabel, c);
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(10, 0, 0, 0);
        lpanel.add(m_loginNameBox, c);
        c.gridy = 2;
        c.insets = new Insets(5, 0, 0, 0);
        lpanel.add(m_passwordBox, c);
        c.gridy = 3;
        c.insets = new Insets(20, 0, 10, 0);
        c.anchor = GridBagConstraints.EAST;
        lpanel.add(bpanel, c);
        lpanel.setOpaque(false);
        bpanel.setOpaque(false);
        m_loginNameBox.setBorder(new LineBorder(Color.black));
        m_passwordBox.setBorder(new LineBorder(Color.black));
        instrlabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        m_loginNameBox.setPreferredSize(new Dimension(295, 20));
        m_passwordBox.setPreferredSize(new Dimension(295, 20));
        aboutButton.setPreferredSize(new Dimension(80, 30));
        loginButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setPreferredSize(new Dimension(80, 30));
        return lpanel;
    }

    /**
	 * Method succeeded.
	 * 
	 * @return boolean
	 */
    public boolean succeeded() {
        return m_succeeded;
    }

    public boolean loadCTRMLocations() {
        return false;
    }

    public JComboBox getCtrmBox() {
        return null;
    }

    public Object testAuthenticate(String userName, String password) {
        return null;
    }

    public CaTIES_UserImpl getUserImpl() {
        return null;
    }
}
