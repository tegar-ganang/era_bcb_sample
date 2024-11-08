package de.renier.vdr.channel.editor;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import de.renier.vdr.channel.ChannelElement;
import de.renier.vdr.channel.editor.actions.ActionManager;
import de.renier.vdr.channel.editor.actions.CloseAction;
import de.renier.vdr.channel.editor.actions.OpenAction;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * ChannelEditor
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ChannelEditor extends JFrame {

    private static final long serialVersionUID = -5724780387927715325L;

    private javax.swing.JPanel jContentPane = null;

    private boolean modified = false;

    private File channelFile = null;

    private JSplitPane jSplitPane = null;

    private ChannelListingPanel channelListingPanel = null;

    private JMenuBar jJMenuBar = null;

    private JMenu jMenu = null;

    private JMenu jMenu1 = null;

    private JToolBar jToolBar = null;

    public static ChannelEditor application = null;

    public static ChannelElement nothingSelectedChannel = new ChannelElement(Messages.getString("ChannelEditor.0"));

    private JSplitPane jSplitPane1 = null;

    private ChannelPropertyPanel channelPropertyPanel = null;

    private JTabbedPane jTabbedPane = null;

    private ChannelParkingPanel channelParkingPanel = null;

    private ChannelDeletedPanel channelDeletedPanel = null;

    private JMenu jMenu2 = null;

    private JMenu jMenu3 = null;

    /**
   * This method initializes jSplitPane
   * 
   * @return javax.swing.JSplitPane
   */
    private JSplitPane getJSplitPane() {
        if (jSplitPane == null) {
            jSplitPane = new JSplitPane();
            jSplitPane.setLeftComponent(getChannelListingPanel());
            jSplitPane.setDividerSize(2);
            jSplitPane.setDividerLocation(300);
            jSplitPane.setEnabled(true);
            jSplitPane.setRightComponent(getJSplitPane1());
        }
        return jSplitPane;
    }

    /**
   * This method initializes channelListingPanel
   * 
   * @return de.renier.vdr.channel.editor.ChannelListingPanel
   */
    public ChannelListingPanel getChannelListingPanel() {
        if (channelListingPanel == null) {
            channelListingPanel = new ChannelListingPanel();
        }
        return channelListingPanel;
    }

    /**
   * This method initializes jJMenuBar
   * 
   * @return javax.swing.JMenuBar
   */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getJMenu());
            jJMenuBar.add(getJMenu3());
            jJMenuBar.add(getJMenu1());
        }
        return jJMenuBar;
    }

    /**
   * This method initializes jMenu
   * 
   * @return javax.swing.JMenu
   */
    private JMenu getJMenu() {
        if (jMenu == null) {
            jMenu = new JMenu();
            jMenu.setText(Messages.getString("ChannelEditor.1"));
            jMenu.setMnemonic(KeyEvent.VK_F);
            jMenu.add(ActionManager.getInstance().getOpenAction()).setMnemonic(KeyEvent.VK_O);
            jMenu.add(ActionManager.getInstance().getSaveAction()).setMnemonic(KeyEvent.VK_S);
            jMenu.add(ActionManager.getInstance().getSaveAsAction());
            jMenu.addSeparator();
            jMenu.add(ActionManager.getInstance().getPreferencesAction());
            jMenu.addSeparator();
            jMenu.add(getJMenu2());
            jMenu.addSeparator();
            jMenu.add(ActionManager.getInstance().getCloseAction()).setMnemonic(KeyEvent.VK_X);
        }
        return jMenu;
    }

    /**
   * This method initializes jMenu1
   * 
   * @return javax.swing.JMenu
   */
    private JMenu getJMenu1() {
        if (jMenu1 == null) {
            jMenu1 = new JMenu();
            jMenu1.setText(Messages.getString("ChannelEditor.2"));
            jMenu1.setMnemonic(KeyEvent.VK_I);
            jMenu1.add(ActionManager.getInstance().getStatisticAction()).setMnemonic(KeyEvent.VK_S);
            jMenu1.add(ActionManager.getInstance().getAboutAction()).setMnemonic(KeyEvent.VK_A);
        }
        return jMenu1;
    }

    /**
   * This method initializes jToolBar
   * 
   * @return javax.swing.JToolBar
   */
    private JToolBar getJToolBar() {
        if (jToolBar == null) {
            jToolBar = new JToolBar();
            jToolBar.add(ActionManager.getInstance().getCloseAction()).setToolTipText(Messages.getString("ChannelEditor.3"));
            jToolBar.add(ActionManager.getInstance().getOpenAction()).setToolTipText(Messages.getString("ChannelEditor.4"));
            jToolBar.add(ActionManager.getInstance().getSaveAction()).setToolTipText(Messages.getString("ChannelEditor.5"));
            jToolBar.add(ActionManager.getInstance().getSaveAsAction()).setToolTipText(Messages.getString("ChannelEditor.6"));
            jToolBar.addSeparator();
            jToolBar.add(ActionManager.getInstance().getPreferencesAction()).setToolTipText(Messages.getString("ChannelEditor.7"));
            jToolBar.addSeparator();
            jToolBar.add(ActionManager.getInstance().getSearchAction()).setToolTipText(Messages.getString("ChannelEditor.8"));
            jToolBar.addSeparator();
            jToolBar.add(ActionManager.getInstance().getParkAction()).setToolTipText(Messages.getString("ChannelEditor.9"));
            jToolBar.add(ActionManager.getInstance().getUnparkAction()).setToolTipText(Messages.getString("ChannelEditor.10"));
            jToolBar.add(ActionManager.getInstance().getDeleteChannelAction()).setToolTipText(Messages.getString("ChannelEditor.11"));
            jToolBar.addSeparator();
            jToolBar.add(ActionManager.getInstance().getCreateChannelAction()).setToolTipText(Messages.getString("ChannelEditor.12"));
            jToolBar.add(ActionManager.getInstance().getCreateCategoryAction()).setToolTipText(Messages.getString("ChannelEditor.13"));
            jToolBar.add(ActionManager.getInstance().getMultiRenameAction()).setToolTipText(Messages.getString("ChannelEditor.14"));
            jToolBar.addSeparator();
            jToolBar.add(ActionManager.getInstance().getStatisticAction()).setToolTipText(Messages.getString("ChannelEditor.15"));
            jToolBar.add(ActionManager.getInstance().getAboutAction()).setToolTipText(Messages.getString("ChannelEditor.16"));
        }
        return jToolBar;
    }

    /**
   * This method initializes jSplitPane1
   * 
   * @return javax.swing.JSplitPane
   */
    private JSplitPane getJSplitPane1() {
        if (jSplitPane1 == null) {
            jSplitPane1 = new JSplitPane();
            jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
            jSplitPane1.setDividerLocation(230);
            jSplitPane1.setDividerSize(2);
            jSplitPane1.setTopComponent(getChannelPropertyPanel());
            jSplitPane1.setBottomComponent(getJTabbedPane());
        }
        return jSplitPane1;
    }

    /**
   * This method initializes channelPropertyPanel
   * 
   * @return de.renier.vdr.channel.editor.ChannelPropertyPanel
   */
    public ChannelPropertyPanel getChannelPropertyPanel() {
        if (channelPropertyPanel == null) {
            channelPropertyPanel = new ChannelPropertyPanel();
            channelPropertyPanel.setPreferredSize(new java.awt.Dimension(440, 200));
        }
        return channelPropertyPanel;
    }

    /**
   * This method initializes jTabbedPane
   * 
   * @return javax.swing.JTabbedPane
   */
    private JTabbedPane getJTabbedPane() {
        if (jTabbedPane == null) {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.addTab(Messages.getString("ChannelEditor.17"), new ImageIcon(getClass().getResource("/org/javalobby/icons/16x16/GreenFlag.gif")), getChannelParkingPanel(), null);
            jTabbedPane.addTab(Messages.getString("ChannelEditor.19"), new ImageIcon(getClass().getResource("/org/javalobby/icons/16x16/RedFlag.gif")), getChannelDeletedPanel(), null);
        }
        return jTabbedPane;
    }

    /**
   * This method initializes channelParkingPanel
   * 
   * @return de.renier.vdr.channel.editor.ChannelParkingPanel
   */
    public ChannelParkingPanel getChannelParkingPanel() {
        if (channelParkingPanel == null) {
            channelParkingPanel = new ChannelParkingPanel();
        }
        return channelParkingPanel;
    }

    /**
   * This method initializes channelDeletedPanel
   * 
   * @return de.renier.vdr.channel.editor.ChannelDeletedPanel
   */
    public ChannelDeletedPanel getChannelDeletedPanel() {
        if (channelDeletedPanel == null) {
            channelDeletedPanel = new ChannelDeletedPanel();
        }
        return channelDeletedPanel;
    }

    /**
   * This method initializes jMenu2
   * 
   * @return javax.swing.JMenu
   */
    private JMenu getJMenu2() {
        if (jMenu2 == null) {
            jMenu2 = new JMenu();
            jMenu2.setText(Messages.getString("ChannelEditor.21"));
            jMenu2.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/Copy.gif")));
            refreshLastOpenedFiles();
        }
        return jMenu2;
    }

    public void refreshLastOpenedFiles() {
        jMenu2.removeAll();
        List files = Utils.getLastOpenedFiles();
        Iterator it = files.iterator();
        while (it.hasNext()) {
            File file = (File) it.next();
            jMenu2.add(new JMenuItem(new OpenAction(file)));
        }
    }

    /**
   * This method initializes jMenu3
   * 
   * @return javax.swing.JMenu
   */
    private JMenu getJMenu3() {
        if (jMenu3 == null) {
            jMenu3 = new JMenu();
            jMenu3.setText(Messages.getString("ChannelEditor.23"));
            jMenu3.add(ActionManager.getInstance().getCreateChannelAction()).setMnemonic(KeyEvent.VK_C);
            jMenu3.add(ActionManager.getInstance().getCreateCategoryAction()).setMnemonic(KeyEvent.VK_K);
            jMenu3.addSeparator();
            jMenu3.add(ActionManager.getInstance().getImportAliasAction()).setMnemonic(KeyEvent.VK_I);
            jMenu3.add(ActionManager.getInstance().getExportAliasAction()).setMnemonic(KeyEvent.VK_E);
        }
        return jMenu3;
    }

    /**
   * Main start
   * 
   * @param args
   */
    public static void main(String[] args) throws Exception {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                application = new ChannelEditor();
                application.setTitle(Messages.getString("ChannelEditor.24"));
            }
        });
    }

    /**
   * This is the default constructor
   */
    public ChannelEditor() {
        super();
        initialize();
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        this.setJMenuBar(getJJMenuBar());
        this.setSize(800, 600);
        this.setContentPane(getJContentPane());
        this.setTitle(Messages.getString("ChannelEditor.25"));
        Dimension frameDim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension ownDim = this.getSize();
        this.setLocation(((int) (frameDim.getWidth() - ownDim.getWidth()) / 2), ((int) (frameDim.getHeight() - ownDim.getHeight()) / 2));
        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/org/javalobby/icons/20x20/List.gif")));
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent e) {
                new CloseAction().actionPerformed(new ActionEvent(this, 0, "close"));
            }
        });
    }

    /**
   * This method initializes jContentPane
   * 
   * @return javax.swing.JPanel
   */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            jContentPane.add(getJSplitPane(), java.awt.BorderLayout.CENTER);
            jContentPane.add(getJToolBar(), java.awt.BorderLayout.NORTH);
        }
        return jContentPane;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
        ActionManager.getInstance().getSaveAction().setEnabled(modified);
    }

    public File getChannelFile() {
        return channelFile;
    }

    public void setChannelFile(File channelFile) {
        this.channelFile = channelFile;
    }
}
