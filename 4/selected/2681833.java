package org.rjam.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.rjam.alert.AlertAdminPanel;
import org.rjam.alert.AlertGroup;
import org.rjam.alert.businesshours.ApplicationScedulePanel;
import org.rjam.gui.admin.ApplicationAdminDialog;
import org.rjam.gui.admin.JamSystemTray;
import org.rjam.gui.analysis.AbstractReportPane;
import org.rjam.gui.analysis.ResponseTimeAnalysisPane;
import org.rjam.gui.api.IDataManager;
import org.rjam.gui.api.IReport;
import org.rjam.gui.base.Constants;
import org.rjam.gui.base.Logger;
import org.rjam.gui.beans.EntityMap;
import org.rjam.gui.beans.Favorite;
import org.rjam.gui.beans.ReportDescription;
import org.rjam.gui.thread.ThreadSelectPanel;
import org.rjam.report.ExportDialog;
import org.rjam.report.ExportManager;
import org.rjam.report.ReportDescDialog;
import org.rjam.report.xml.Transformer;
import org.rjam.sql.BaseJdbcConnection;
import org.rjam.sql.SqlDataManager;
import org.rjam.xml.Parser;
import org.rjam.xml.Token;

public class AnalysisApplicatioin extends ApplicationFrame implements ActionListener, TreeSelectionListener, Constants {

    public static final String VERSION = "2.11";

    public static final String VERSION_DATE = "Date: 2011/09/23";

    private static final String PROP_ENABLE_ADMIN = "Admin";

    private static final String PROP_ENABLE_ALERTS = "Alerts";

    private static String versionString = null;

    private static String versionDate = null;

    private static Logger logger = Logger.getLogger(AnalysisApplicatioin.class);

    private static int windowCount = 0;

    private static Preferences pref = Preferences.systemNodeForPackage(AnalysisApplicatioin.class);

    private Favorite fav = Favorite.getFavorites();

    private Favorite curFav = null;

    private JdbcConfigPanel configPanel;

    private SelectPanel selectPanel;

    private JTabbedPane jtabbedpane;

    private Map<String, AbstractReportPane> reportMap = new HashMap<String, AbstractReportPane>();

    private static JamSystemTray _systemTray = null;

    private File pdfFile;

    private File dataFile;

    private ReportDescription[] availible;

    private boolean showStat;

    private JCheckBoxMenuItem usePreparedMenuItem;

    private boolean hideOnClose;

    protected File alertsFile;

    protected boolean initComplete;

    private JMenu favMenu;

    private JMenuItem updateFavItem;

    private JCheckBoxMenuItem useRolledMenuItem;

    protected File calendarFile;

    public static File selectFile(Component parent, File file, String approveButtonText, boolean forSave) {
        File ret = null;
        final JFileChooser fc = new JFileChooser();
        if (file != null) {
            fc.setSelectedFile(file);
        }
        if (fc.showDialog(parent, approveButtonText) == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            if (forSave && file.exists()) {
                int value = JOptionPane.showConfirmDialog(parent, file + " already exists.\n" + "Do you want to over write it?", "Please Confirm", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.YES_OPTION) {
                    ret = file;
                }
            } else {
                ret = file;
            }
        }
        return ret;
    }

    public static void setPreference(String name, String val) {
        pref.put(name, val);
    }

    public static String getPreference(String name, String def) {
        return pref.get(name, def);
    }

    public static String getVersionDate() {
        if (versionDate == null) {
            versionDate = VERSION_DATE.substring(6, VERSION_DATE.length() - 1).trim();
        }
        return versionDate;
    }

    public static String getVersion() {
        if (versionString == null) {
            synchronized (AnalysisApplicatioin.class) {
                if (versionString == null) {
                    versionString = VERSION;
                }
            }
        }
        return versionString;
    }

    private static int getWindowCount() {
        return windowCount;
    }

    private static int decWindowCount() {
        synchronized (AnalysisApplicatioin.class) {
            return --windowCount;
        }
    }

    private static int incWindowCount() {
        synchronized (AnalysisApplicatioin.class) {
            return ++windowCount;
        }
    }

    public static JamSystemTray getSystemTray() {
        return _systemTray;
    }

    public AnalysisApplicatioin(String title, final boolean visible) {
        super(title);
        URL res = getClass().getResource(IMAGE_TITLE);
        setIconImage(new javax.swing.ImageIcon(res).getImage());
        setHideOnClose(!visible);
        Thread initer = new Thread(new Runnable() {

            public void run() {
                final SplashDialog sp = new SplashDialog();
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        sp.setVisible(visible);
                    }
                });
                setJMenuBar(createMenuBar());
                setContentPane(createContent());
                setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                pack();
                RefineryUtilities.centerFrameOnScreen(AnalysisApplicatioin.this);
                setVisible(visible);
                sp.dispose();
                AnalysisApplicatioin.this.initComplete = true;
            }
        });
        initer.start();
    }

    private static final long serialVersionUID = 1L;

    private static final String PROP_SHOW_CONFIG = "ShowConfig";

    private static final String DEFAULT_SHOW_CONFIG = "true";

    private static final String PROP_TITLE = "Title";

    public static final String FAVORITES = "Favorites";

    public static final String PRODUCTION = "Production";

    public static final String PROP_LOOK_AND_FEEL = "LookAndFeel";

    @Override
    public void setVisible(boolean arg0) {
        super.setVisible(arg0);
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
        super.windowOpened(arg0);
        incWindowCount();
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        if (getWindowCount() == 1) {
            attemptExit();
        } else {
            dispose();
        }
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
        super.windowClosed(arg0);
        decWindowCount();
    }

    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        if (s.equals("EXIT")) {
            attemptExit();
        }
    }

    private void attemptExit() {
        if (hideOnClose) {
            setVisible(false);
        } else {
            String s = "Confirm";
            String s1 = "Are you sure you want to exit?";
            int i = JOptionPane.showConfirmDialog(this, s1, s, 0, 3);
            if (i == 0) {
                dispose();
                System.exit(0);
            }
        }
    }

    public boolean isHideOnClose() {
        return hideOnClose;
    }

    public void setHideOnClose(boolean hideOnClose) {
        this.hideOnClose = hideOnClose;
    }

    public void valueChanged(TreeSelectionEvent e) {
    }

    public void addReport(ReportDescription reportDesc) {
        AbstractReportPane analysis;
        try {
            analysis = (AbstractReportPane) reportDesc.getImpl();
        } catch (Throwable e) {
            logger.error("Can't create report", e);
            analysis = new ResponseTimeAnalysisPane();
        }
        String name = analysis.getName();
        if (name != null && name.length() > 0 && !isActive(analysis)) {
            this.reportMap.put(name, analysis);
            jtabbedpane.add(name, analysis);
            getSelectPanel().addReportPane(analysis);
            analysis.setShowStats(this.showStat);
        }
    }

    public void removeReport(String name) {
        AbstractReportPane ana = reportMap.remove(name);
        if (ana != null) {
            jtabbedpane.remove(ana);
            getSelectPanel().removeReportPane(ana);
        }
    }

    private ReportDescription[] getAvailableReports() {
        if (this.availible == null) {
            this.availible = ReportDescription.getAvailible();
            Thread initer = new Thread(new Runnable() {

                public void run() {
                    for (int idx = 0; idx < availible.length; idx++) {
                        try {
                            IReport report = availible[idx].getImpl();
                            ISelector sel = getSelectPanel();
                            report.setSelector(sel);
                            report.setDataManager(new SqlDataManager(getSelectPanel().getJdbcConfig(), report.getQueryType()));
                            logger.debug("Report inited = " + report.getName());
                        } catch (Throwable e) {
                            logger.error("Can't initialize report idx=" + idx + " name = " + availible[idx].getName(), e);
                        }
                    }
                }
            });
            initer.start();
        }
        return this.availible;
    }

    private JComponent createContent() {
        JPanel ret = new JPanel(new BorderLayout());
        jtabbedpane = new JTabbedPane();
        boolean showConfig = System.getProperty(PROP_SHOW_CONFIG, DEFAULT_SHOW_CONFIG).toLowerCase().startsWith("t");
        if (showConfig) {
            jtabbedpane.add("Configuration", getConfigPanel());
        } else {
            getConfigPanel().setVisible(false);
        }
        jtabbedpane.add("Selection", getSelectPanel());
        jtabbedpane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        ret.add(jtabbedpane);
        selectPanel.setJdbcConfig(getConfigPanel());
        ReportDescription[] rep = getAvailableReports();
        for (int idx = 0; idx < rep.length; idx++) {
            if (rep[idx].isInclude()) {
                addReport(rep[idx]);
            }
        }
        if (windowCount == 0) {
            String title = System.getProperty(PROP_TITLE);
            if (title == null) {
                title = "RJam Analysis (" + configPanel.getUrl() + ")";
            }
            setTitle(title);
        }
        return ret;
    }

    private JdbcConfigPanel getConfigPanel() {
        if (configPanel == null) {
            configPanel = new JdbcConfigPanel();
            configPanel.addPropertyChangeListener(JdbcConfigPanel.EVENT_CONNECTION_CLOSED, new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent arg0) {
                    Object obj = arg0.getNewValue();
                    boolean b = obj.toString().startsWith("t");
                    usePreparedMenuItem.setSelected(b);
                }
            });
        }
        return configPanel;
    }

    private JMenuItem createNewMenu() {
        JMenuItem ret = new JMenuItem("New Window", 120);
        ret.setActionCommand("NEW_WINDOW");
        ret.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                AnalysisApplicatioin mt = new AnalysisApplicatioin(getTitle() + "-" + getWindowCount(), true);
                mt.pack();
                RefineryUtilities.centerFrameOnScreen(mt);
                mt.setVisible(true);
                mt.setVisible(true);
            }
        });
        return ret;
    }

    private JMenu createDataMenu() {
        JMenu ret = new JMenu("Data", true);
        JMenuItem item = new JMenuItem("Clear");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (Iterator<String> it = reportMap.keySet().iterator(); it.hasNext(); ) {
                    String key = (String) it.next();
                    ((AbstractReportPane) reportMap.get(key)).clear();
                }
            }
        });
        ret.add(item);
        return ret;
    }

    private JMenuItem createExportToCsvMenuItem() {
        JMenuItem ret = new JMenuItem("Export to CSV");
        ret.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ReportDescription[] rep = getAvailableReports();
                List<ReportDescription> descList = new ArrayList<ReportDescription>();
                for (int idx = 0; idx < rep.length; idx++) {
                    if (rep[idx].isInclude()) {
                        try {
                            IReport tr = rep[idx].getImpl();
                            if (tr.getRowCount() > 0) {
                                descList.add(rep[idx]);
                            }
                        } catch (InstantiationException e1) {
                        } catch (IllegalAccessException e1) {
                        }
                    }
                }
                rep = (ReportDescription[]) descList.toArray(new ReportDescription[descList.size()]);
                if (rep.length == 0) {
                    return;
                }
                ReportDescDialog rd = new ReportDescDialog(jtabbedpane);
                RefineryUtilities.centerFrameOnScreen(rd);
                rep = rd.show(rep);
                int cnt = 0;
                for (int idx = 0; idx < rep.length; idx++) {
                    if (rep[idx].isInclude()) {
                        cnt++;
                    }
                }
                if (rd.isCancel() || cnt == 0) {
                    return;
                }
                boolean combine = cnt == 1;
                if (cnt > 1) {
                    int value = JOptionPane.showConfirmDialog(jtabbedpane, "Do you want to save each report in a seperate file?", "Please Confirm", JOptionPane.YES_NO_OPTION);
                    combine = !(value == JOptionPane.YES_OPTION);
                }
                final JFileChooser fc = new JFileChooser();
                if (dataFile != null) {
                    if (dataFile.isFile()) {
                        fc.setSelectedFile(dataFile);
                    }
                }
                if (!combine && cnt > 1) {
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (dataFile != null) {
                        if (dataFile.isFile()) {
                            fc.setSelectedFile(dataFile.getParentFile());
                        } else {
                            fc.setSelectedFile(dataFile);
                        }
                    }
                }
                if (fc.showDialog(jtabbedpane, "Save Data") == JFileChooser.APPROVE_OPTION) {
                    dataFile = fc.getSelectedFile();
                    if (combine || cnt == 1) {
                        if (dataFile.isFile()) {
                            if (dataFile.exists()) {
                                int value = JOptionPane.showConfirmDialog(jtabbedpane, dataFile + " already exists.\n" + "Do you want to over write it?", "Please Confirm", JOptionPane.YES_NO_OPTION);
                                if (value != JOptionPane.YES_OPTION) {
                                    return;
                                }
                            }
                        }
                    } else {
                        if (!dataFile.exists()) {
                            int value = JOptionPane.showConfirmDialog(jtabbedpane, dataFile + " does not exists.\n" + "Do you want create it now?", "", JOptionPane.YES_NO_OPTION);
                            if (value != JOptionPane.YES_OPTION) {
                                return;
                            } else {
                                dataFile.mkdirs();
                            }
                        }
                    }
                    int value = JOptionPane.showConfirmDialog(jtabbedpane, "Format date as GMT?", "", JOptionPane.YES_NO_OPTION);
                    boolean gmt = value == JOptionPane.YES_OPTION;
                    if (cnt == 1 || combine) {
                        String data = getAnalysisData(gmt, rep);
                        OutputStream out = null;
                        try {
                            dataFile.getParentFile().mkdirs();
                            out = new FileOutputStream(dataFile);
                            out.write(data.getBytes());
                            out.flush();
                        } catch (Exception e1) {
                            JOptionPane.showMessageDialog(jtabbedpane, e1.toString(), "An error occurred saving the data.", JOptionPane.ERROR_MESSAGE);
                        } finally {
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (Exception ex) {
                                }
                            }
                        }
                    } else {
                        saveDataToDir(dataFile, rep, gmt);
                    }
                }
            }

            private void saveDataToDir(File dir, ReportDescription[] rep, boolean gmt) {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                boolean saveAll = false;
                for (int idx = 0; idx < rep.length; idx++) {
                    if (!rep[idx].isInclude()) {
                        continue;
                    }
                    OutputStream out = null;
                    AbstractReportPane report = null;
                    try {
                        report = (AbstractReportPane) rep[idx].getImpl();
                        File dataFile = new File(dir, report.getName() + ".csv");
                        if (!saveAll && dataFile.exists()) {
                            Object[] options = { "Yes", "No", "Yes to All" };
                            int value = JOptionPane.showOptionDialog(jtabbedpane, dataFile + " already exists.\nDo you want to over write it?", "Please Confirm", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                            saveAll = value == JOptionPane.CANCEL_OPTION;
                            if (value == JOptionPane.NO_OPTION) {
                                continue;
                            }
                        }
                        String data = report.getData(gmt);
                        out = new FileOutputStream(dataFile);
                        out.write(data.getBytes());
                        out.flush();
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(jtabbedpane, e1.toString(), "An error occurred saving the data.", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
            }

            private String getAnalysisData(boolean gmt, ReportDescription[] rep) {
                StringBuffer ret = new StringBuffer();
                for (int idx = 0; idx < rep.length; idx++) {
                    if (!rep[idx].isInclude()) {
                        continue;
                    }
                    AbstractReportPane report = null;
                    try {
                        report = (AbstractReportPane) rep[idx].getImpl();
                        String data = report.getData(gmt);
                        if (ret.length() > 0) {
                            ret.append("\n");
                            ret.append("\n");
                        }
                        ret.append(data);
                    } catch (Exception e) {
                        logger.error("Can't create impl", e);
                    }
                }
                return ret.toString();
            }
        });
        return ret;
    }

    private JMenu createMiscMenu() {
        JMenu ret = new JMenu("Misc", true);
        ret.setMnemonic('M');
        JMenuItem item = new JMenuItem("Thread Analysis");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SelectPanel panel = getSelectPanel();
                String[] apps = panel.getSelectedApps();
                if (apps == null || apps.length != 1) {
                    JOptionPane.showMessageDialog(AnalysisApplicatioin.this, "Please select an application AND a server.", "Select Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String[] svrs = panel.getSelectedServers();
                if (svrs == null || svrs.length == 0) {
                    JOptionPane.showMessageDialog(AnalysisApplicatioin.this, "Please select an application AND a server.", "Select Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                final ThreadSelectPanel tsp = new ThreadSelectPanel(panel.getJdbcConfig(), apps[0], svrs[0], panel.getEndDate());
                JFrame frame = new JFrame("Thread Analysis");
                frame.setIconImage(new javax.swing.ImageIcon(getClass().getResource(Constants.IMAGE_TITLE)).getImage());
                frame.addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {
                    }

                    public void windowClosed(WindowEvent e) {
                        tsp.close();
                    }

                    public void windowClosing(WindowEvent e) {
                    }

                    public void windowDeactivated(WindowEvent e) {
                    }

                    public void windowDeiconified(WindowEvent e) {
                    }

                    public void windowIconified(WindowEvent e) {
                    }

                    public void windowOpened(WindowEvent e) {
                    }
                });
                frame.getContentPane().add(tsp);
                frame.pack();
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Dimension screenSize = toolkit.getScreenSize();
                int x = (screenSize.width - frame.getWidth()) / 2;
                int y = (screenSize.height - frame.getHeight()) / 2;
                frame.setLocation(x, y);
                frame.setVisible(true);
            }
        });
        ret.add(item);
        ret.add(createLogMenu());
        ret.add(new javax.swing.JPopupMenu.Separator());
        item = new JMenuItem("Toggle Debug Info");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                AnalysisApplicatioin.this.setShowStats();
            }
        });
        ret.add(item);
        item = new JMenuItem("Check Current Application Version");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ApplicationVersionFrame frame = new ApplicationVersionFrame();
                frame.setVisible(true);
            }
        });
        ret.add(item);
        if (isAlertEnabled()) {
            ret.add(createManageAlertsMenuItem());
            ret.add(createHideConsoleMenuItem());
        }
        usePreparedMenuItem = new JCheckBoxMenuItem("Use Prepared Statement");
        usePreparedMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                getConfigPanel().setUsePrepared(usePreparedMenuItem.isSelected());
            }
        });
        ret.add(usePreparedMenuItem);
        useRolledMenuItem = new JCheckBoxMenuItem("Use Aggregate Data");
        useRolledMenuItem.setSelected(BaseJdbcConnection.isUseRolled());
        useRolledMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                BaseJdbcConnection.setUseRolled(useRolledMenuItem.isSelected());
            }
        });
        ret.add(useRolledMenuItem);
        return ret;
    }

    private JMenuItem createHideConsoleMenuItem() {
        JMenuItem ret = new JMenuItem("Hide Console");
        ret.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (_systemTray == null) {
                    _systemTray = new JamSystemTray(AnalysisApplicatioin.this);
                    _systemTray.start();
                    setHideOnClose(true);
                }
                AnalysisApplicatioin.this.setVisible(false);
            }
        });
        return ret;
    }

    private JMenuItem createManageAlertsMenuItem() {
        JMenuItem ret = new JMenuItem("Personal Alerts");
        ret.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ReportDescription[] desc = getAvailableReports();
                IReport[] reports = new IReport[desc.length];
                boolean auto = getSelectPanel().getMonitoringPanel().isMonitoring();
                for (int idx = 0; idx < desc.length; idx++) {
                    try {
                        reports[idx] = desc[idx].getImpl();
                        if (!auto || reports[idx].getSelector() == null) {
                            ISelector sel = getSelectPanel();
                            reports[idx].setSelector(sel);
                            reports[idx].setDataManager(new SqlDataManager(getSelectPanel().getJdbcConfig(), reports[idx].getQueryType()));
                        }
                    } catch (Exception e1) {
                        logger.error("Error creting alerts panel", e1);
                    }
                }
                AlertAdminPanel panel = new AlertAdminPanel(reports);
                final JFrame frame = new JFrame("Personal Alerts");
                frame.setIconImage(new javax.swing.ImageIcon(getClass().getResource(Constants.IMAGE_TITLE)).getImage());
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(panel);
                frame.setSize(1200, 800);
                RefineryUtilities.centerFrameOnScreen(frame);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        frame.setVisible(true);
                    }
                });
            }
        });
        return ret;
    }

    protected void setShowStats() {
        this.showStat = !showStat;
        for (Iterator<String> it = reportMap.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            AbstractReportPane rpt = reportMap.get(key);
            rpt.setShowStats(this.showStat);
        }
    }

    private JMenuItem createExportToPdfMenuItem() {
        JMenuItem ret = new JMenuItem("Export PDF");
        ret.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ExportDialog ed = new ExportDialog(jtabbedpane);
                RefineryUtilities.centerDialogInParent(ed);
                ed.setVisible(true);
                if (ed.isCancel()) {
                    return;
                }
                File file = selectFile(jtabbedpane, pdfFile, "Select Output File", true);
                if (file == null) {
                    return;
                }
                pdfFile = file;
                final ExportManager em = new ExportManager(ed.getReportName(), ed.getReportDesc());
                em.setOutputFormat(ed.getOutputFormat());
                em.setOutputFile(pdfFile);
                ProgressDialog monitor = new ProgressDialog(jtabbedpane);
                em.setMonitor(monitor);
                ReportDescription[] rep = getAvailableReports();
                List<ReportDescription> descList = new ArrayList<ReportDescription>();
                for (int idx = 0; idx < rep.length; idx++) {
                    if (rep[idx].isInclude()) {
                        descList.add(rep[idx]);
                    }
                }
                ReportDescDialog rd = new ReportDescDialog(jtabbedpane);
                RefineryUtilities.centerFrameOnScreen(rd);
                rep = rd.show((ReportDescription[]) descList.toArray(new ReportDescription[descList.size()]));
                if (rd.isCancel()) {
                    return;
                }
                try {
                    final List<IReport> list2 = new ArrayList<IReport>();
                    for (int idx = 0; idx < rep.length; idx++) {
                        if (rep[idx].isInclude()) {
                            list2.add(rep[idx].getImpl());
                        }
                    }
                    em.setReports(list2);
                    em.start();
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(jtabbedpane, e1.toString(), "An error occurred exporting reports.", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return ret;
    }

    private JMenuItem createManageReportsMenuItem() {
        JMenuItem ret = new JMenuItem("Select Reports");
        ret.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ReportDescDialog ed = new ReportDescDialog(jtabbedpane);
                RefineryUtilities.centerFrameOnScreen(ed);
                ReportDescription[] rep = ed.show(getAvailableReports());
                if (ed.isCancel()) {
                    return;
                }
                AnalysisApplicatioin.this.availible = rep;
                for (int idx = 0; idx < rep.length; idx++) {
                    if (rep[idx].isInclude()) {
                        addReport(rep[idx]);
                    } else {
                        removeReport(rep[idx].getName());
                    }
                }
            }
        });
        return ret;
    }

    private JMenu createReportMenu() {
        JMenu ret = new JMenu("Reports", true);
        ret.setMnemonic('R');
        try {
            Class.forName("org.apache.fop.Version");
            ret.add(createExportToPdfMenuItem());
        } catch (Exception e) {
        }
        ret.add(createExportToCsvMenuItem());
        ret.add(createManageReportsMenuItem());
        return ret;
    }

    public boolean isActive(AbstractReportPane ana) {
        boolean ret = reportMap.get(ana.getName()) != null;
        return ret;
    }

    private JMenu createHelpMenu() {
        JMenu ret = new JMenu("Help", true);
        ret.setMnemonic('H');
        JMenuItem item = new JMenuItem("About");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                AboutDialog ad = new AboutDialog(jtabbedpane);
                RefineryUtilities.centerFrameOnScreen(ad);
                ad.setVisible(true);
            }
        });
        ret.add(item);
        return ret;
    }

    private JMenu createTestMenu() {
        JMenu ret = new JMenu("Test", true);
        ret.setMnemonic('T');
        JMenuItem item = new JMenuItem("Out of Memory");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                long free = Runtime.getRuntime().freeMemory();
                List<byte[]> list = new ArrayList<byte[]>();
                while (free > 0) {
                    list.add(new byte[1024 * 1024]);
                }
            }
        });
        ret.add(item);
        item = new JMenuItem("Null Pointer");
        item.addActionListener(new ActionListener() {

            @SuppressWarnings("null")
            public void actionPerformed(ActionEvent e) {
                String tmp = null;
                tmp.toString();
            }
        });
        ret.add(item);
        item = new JMenuItem("Index out of range");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                byte[] ary = new byte[0];
                if (ary[1] == ' ') {
                }
            }
        });
        ret.add(item);
        return ret;
    }

    private JMenu createFavoriteMenu() {
        final JMenu ret = new JMenu(FAVORITES, true);
        ret.setMnemonic('F');
        populatFavoriteMenu(ret);
        return ret;
    }

    private void populatFavoriteMenu(final JMenu ret) {
        ret.removeAll();
        JMenuItem item = new JMenuItem("Add");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FavoriteDialog di = new FavoriteDialog();
                File file = di.showDialog();
                if (file != null) {
                    saveAlerts(file);
                    fav = Favorite.getFavorites();
                    curFav = new Favorite(file);
                    populatFavoriteMenu(ret);
                }
            }
        });
        ret.add(item);
        if (fav.getKids().size() > 0) {
            item = new JMenuItem("Manage");
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    FavoriteManageDialog di = new FavoriteManageDialog();
                    di.setVisible(true);
                    fav = Favorite.getFavorites();
                    populatFavoriteMenu(ret);
                }
            });
            ret.add(item);
        }
        if (curFav != null) {
            updateFavItem = new JMenuItem("Update " + curFav.getName());
            updateFavItem.setEnabled(true);
        } else {
            updateFavItem = new JMenuItem("...");
            updateFavItem.setEnabled(false);
        }
        updateFavItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (curFav != null) {
                    saveAlerts(curFav.getFile());
                }
            }
        });
        ret.add(updateFavItem);
        ret.add(new JSeparator());
        buidFavoritMenu(ret, fav);
    }

    private void buidFavoritMenu(JMenu ret, Favorite fav) {
        List<Favorite> kids = fav.getKids();
        if (kids != null) {
            for (int idx = 0, sz = kids.size(); idx < sz; idx++) {
                final Favorite child = kids.get(idx);
                if (child.isGroup()) {
                    JMenu menu = new JMenu(child.getName());
                    ret.add(menu);
                    buidFavoritMenu(menu, child);
                } else {
                    JMenuItem item = new JMenuItem(child.getName());
                    item.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent arg0) {
                            File file = child.getFile();
                            loadAlerts(file, false);
                            curFav = child;
                            updateFavItem.setText("Update " + curFav.getName());
                            updateFavItem.setEnabled(true);
                        }
                    });
                    ret.add(item);
                }
            }
        }
    }

    public Token getAlertXml() {
        ReportDescription[] desc = getAvailableReports();
        Token tok = Transformer.toXml(getSelectPanel(), desc);
        tok.setDebug(true);
        return tok;
    }

    public void saveAlerts(File file) {
        Token tok = getAlertXml();
        try {
            PrintStream out = new PrintStream(new FileOutputStream(file));
            tok.setDebug(true);
            out.println(tok.toString());
            out.close();
        } catch (FileNotFoundException e) {
            logger.error("Error saving alerts to " + file, e);
            JOptionPane.showMessageDialog(jtabbedpane, "Error saving alerts", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadAlerts(File file, boolean startQuery) {
        while (!initComplete) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        try {
            Parser p = new Parser(file);
            Token tok = p.parse();
            SelectPanel sp = getSelectPanel();
            sp.stopQuery();
            sp.setIgnoreEvents(true);
            Date start = sp.getStartDate();
            Date end = sp.getEndDate();
            Transformer.fromXml(sp, getAvailableReports(), tok);
            sp.setStartDate(start);
            sp.setEndDate(end);
            sp.setIgnoreEvents(false);
            sp.populateForSource();
            ReportDescription[] rep = getAvailableReports();
            for (int idx = 0; idx < rep.length; idx++) {
                if (rep[idx].isInclude()) {
                    addReport(rep[idx]);
                } else {
                    removeReport(rep[idx].getName());
                }
            }
            if (startQuery) {
                sp.startQuery(true);
            }
        } catch (Exception e) {
            logger.error("Error loading alerts.", e);
            throw new RuntimeException("Error loading alerts.", e);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar jmenubar = new JMenuBar();
        JMenu jmenu = new JMenu("File", true);
        jmenu.setMnemonic('F');
        jmenu.add(createNewMenu());
        jmenu.add(createDataMenu());
        boolean showConfig = System.getProperty(PROP_SHOW_CONFIG, DEFAULT_SHOW_CONFIG).toLowerCase().startsWith("t");
        if (showConfig) {
            jmenu.add(new javax.swing.JPopupMenu.Separator());
            JMenuItem jmenuitem = new JMenuItem("Open Configuration", 120);
            jmenuitem.setActionCommand("OPEN");
            jmenuitem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    getConfigPanel().loadButtonActionPerformed();
                }
            });
            jmenu.add(jmenuitem);
            jmenuitem = new JMenuItem("Save Configuration", 120);
            jmenuitem.setActionCommand("SAVE");
            jmenuitem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    getConfigPanel().saveButtonActionPerformed();
                }
            });
            jmenu.add(jmenuitem);
            jmenu.add(new javax.swing.JPopupMenu.Separator());
        }
        jmenu.add(new javax.swing.JPopupMenu.Separator());
        JMenuItem jmenuitem = new JMenuItem("Exit", 120);
        jmenuitem.setActionCommand("EXIT");
        jmenuitem.addActionListener(this);
        jmenu.add(jmenuitem);
        jmenubar.add(jmenu);
        if (isAdmin()) {
            jmenubar.add(createAdminMenu());
            jmenubar.add(createTestMenu());
            jmenubar.add(createAlertMenu());
        }
        jmenubar.add(createMiscMenu());
        jmenubar.add(createReportMenu());
        favMenu = createFavoriteMenu();
        jmenubar.add(favMenu);
        jmenubar.add(createHelpMenu());
        return jmenubar;
    }

    public static boolean isAdmin() {
        return System.getProperty(PROP_ENABLE_ADMIN, "true").startsWith("t");
    }

    public static boolean isAlertEnabled() {
        return System.getProperty(PROP_ENABLE_ALERTS, "true").startsWith("t");
    }

    /**
	 * @param args
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String cls = System.getProperty(PROP_LOOK_AND_FEEL);
            if (cls == null) {
                cls = getPreference(PROP_LOOK_AND_FEEL, null);
            }
            if (cls != null) {
                UIManager.setLookAndFeel(cls);
            }
        } catch (Exception e) {
        }
        if (args.length % 2 == 0) {
            for (int idx = 0; idx < args.length; idx++) {
                System.setProperty(args[idx++], args[idx]);
            }
        }
        @SuppressWarnings("unused") AnalysisApplicatioin test = new AnalysisApplicatioin("Application Monitor", true);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionDialog());
    }

    public SelectPanel getSelectPanel() {
        if (selectPanel == null) {
            selectPanel = new SelectPanel();
            selectPanel.addPropertyChangeListener(DateAndClockPanel.PROP_MILITAY_CHANGED, new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent arg0) {
                    ReportDescription[] rpt = getAvailableReports();
                    for (int idx = 0; idx < rpt.length; idx++) {
                        try {
                            ((AbstractReportPane) rpt[idx].getImpl()).setMilitaryTime(((Boolean) arg0.getNewValue()).booleanValue());
                        } catch (Exception e) {
                        }
                    }
                }
            });
        }
        return selectPanel;
    }

    private JMenu createAlertMenu() {
        JMenu ret = new JMenu("Alerts", true);
        ret.setMnemonic('A');
        final JCheckBoxMenuItem cb = new JCheckBoxMenuItem("Always Evaluate Alerts", ExecuteQueryThread.isDebug());
        cb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ExecuteQueryThread.setDebug(cb.isSelected());
                ReportDescription[] rpts = getAvailableReports();
                for (ReportDescription r : rpts) {
                    List<AlertGroup> lst;
                    try {
                        lst = r.getImpl().getAlertGroups();
                        if (lst != null) {
                            for (AlertGroup g : lst) {
                                g.reset();
                            }
                        }
                    } catch (InstantiationException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        ret.add(cb);
        JMenuItem item = new JMenuItem("Reset Alerts");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ReportDescription[] rpts = getAvailableReports();
                for (ReportDescription r : rpts) {
                    List<AlertGroup> lst;
                    try {
                        lst = r.getImpl().getAlertGroups();
                        if (lst != null) {
                            for (AlertGroup g : lst) {
                                g.reset();
                            }
                        }
                    } catch (Exception e1) {
                    }
                }
            }
        });
        ret.add(item);
        item = new JMenuItem("Manage Alerts");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ReportDescription[] desc = getAvailableReports();
                IReport[] reports = new IReport[desc.length];
                boolean auto = getSelectPanel().getMonitoringPanel().isMonitoring();
                for (int idx = 0; idx < desc.length; idx++) {
                    try {
                        reports[idx] = desc[idx].getImpl();
                        if (!auto || reports[idx].getSelector() == null) {
                            ISelector sel = getSelectPanel();
                            reports[idx].setSelector(sel);
                            reports[idx].setDataManager(new SqlDataManager(getSelectPanel().getJdbcConfig(), reports[idx].getQueryType()));
                        }
                    } catch (Exception e1) {
                        logger.error("Error creting alerts panel", e1);
                    }
                }
                AlertAdminPanel.showPanel(AnalysisApplicatioin.this, reports);
            }
        });
        ret.add(item);
        item = new JMenuItem("Hide Console");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (_systemTray == null) {
                    _systemTray = new JamSystemTray(AnalysisApplicatioin.this);
                    _systemTray.start();
                    setHideOnClose(true);
                }
                AnalysisApplicatioin.this.setVisible(false);
            }
        });
        ret.add(item);
        item = new JMenuItem("Manage Schedules");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ApplicationScedulePanel.showPanel(EntityMap.getEntity(EntityMap.APPLICATION).getAllValues());
            }
        });
        ret.add(item);
        return ret;
    }

    private JMenu createAdminMenu() {
        JMenu ret = new JMenu("Admin", true);
        ret.setMnemonic('A');
        JMenuItem item = new JMenuItem("Manage Applications");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ApplicationAdminDialog ad;
                try {
                    ad = new ApplicationAdminDialog(getSelectPanel().getDataManager());
                    ad.setVisible(true);
                } catch (SQLException e1) {
                    JOptionPane.showMessageDialog(jtabbedpane, e1.toString(), "An error occurred.", JOptionPane.ERROR_MESSAGE);
                    logger.error("Admin Colnsole", e1);
                }
            }
        });
        ret.add(item);
        item = new JMenuItem("Field Administration");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    IDataManager mgr = getSelectPanel().getDataManager();
                    EntityMap map = mgr.getEntityMap(SERVER);
                    String svrs[] = map.getAllValues();
                    if (svrs.length == 0) {
                        map.populateAll();
                    }
                    svrs = map.getAllValues();
                    System.out.println("svr.len = " + svrs.length);
                    for (int idx = 0; idx < svrs.length; idx++) {
                        System.out.println(svrs[idx]);
                    }
                } catch (Throwable e1) {
                    JOptionPane.showMessageDialog(jtabbedpane, e1.toString(), "An error occurred.", JOptionPane.ERROR_MESSAGE);
                    logger.error("Admin Colnsole", e1);
                }
            }
        });
        ret.add(item);
        return ret;
    }

    private JMenu createLogMenu() {
        JMenu ret = new JMenu("Logging", true);
        JMenuItem item = new JMenuItem("None");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Logger.setDefaultLevel(Logger.LEVEL_NONE);
            }
        });
        ret.add(item);
        item = new JMenuItem("Error");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Logger.setDefaultLevel(Logger.LEVEL_ERROR);
            }
        });
        ret.add(item);
        item = new JMenuItem("Info");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Logger.setDefaultLevel(Logger.LEVEL_INFO);
            }
        });
        ret.add(item);
        item = new JMenuItem("Warn");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Logger.setDefaultLevel(Logger.LEVEL_WARN);
            }
        });
        ret.add(item);
        item = new JMenuItem("Debug");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Logger.setDefaultLevel(Logger.LEVEL_DEBUG);
            }
        });
        ret.add(item);
        return ret;
    }

    public static String getFavoriteType() {
        String ret = FAVORITES;
        String title = System.getProperty(PROP_TITLE, PRODUCTION);
        if (!title.equals(PRODUCTION)) {
            ret += "_" + title;
        }
        return ret;
    }
}
