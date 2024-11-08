package net.sourceforge.cridremote;

import org.apache.log4j.Logger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.sourceforge.cridmanager.Settings;
import net.sourceforge.cridmanager.box.BoxManager;
import net.sourceforge.cridmanager.box.IBox;
import net.sourceforge.cridmanager.box.channel.Channel;
import net.sourceforge.cridmanager.services.ServiceProvider;
import net.sourceforge.cridremote.RemoteControl.RCKey;
import test.fp.UnaryFunction;

/**
 * Fenster f�r die Fernsteuerung.
 */
public class RemoteControlView extends javax.swing.JFrame implements IOsdListener, RcListener {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(RemoteControlView.class);

    private javax.swing.JPanel ivjJFrameContentPane = null;

    private JLabel rcPicture = null;

    private RemoteControl remoteControl;

    private JTextArea monitorList = null;

    private JScrollPane jScrollPane = null;

    private JPanel programButtonPanel = null;

    private int lineNo;

    private static final String MESSAGE_FORMAT = "{1,date, HH:mm:ss,SSS}: {2}\n";

    private static final MessageFormat form = new MessageFormat(MESSAGE_FORMAT);

    private JPanel macroPanel = null;

    private JPanel jPanel1 = null;

    private JButton recordButton = null;

    private JButton playButton = null;

    private JComboBox macroList = null;

    private DefaultComboBoxModel macroListModel = null;

    protected boolean inRecording = false;

    private List currentMacro;

    private JPanel infoPanel = null;

    private JLabel infoLabel = null;

    private JScrollPane stationPanel = null;

    private Stack cursorStack = new Stack();

    private JPanel jPanel = null;

    private JCompOsd jCompOsd = null;

    private OsdReaderThread osdReaderThread;

    private JSplitPane jSplitPane = null;

    private JScrollPane jScrollPane2 = null;

    private final IBox box;

    private static final String TITEL = "M740 AV Remote Control";

    private UnaryFunction alertFunction;

    private boolean stayOnTop = false;

    public RemoteControlView(IBox box) {
        this(box, false, null);
    }

    public RemoteControlView(IBox box, boolean isOnTop, UnaryFunction alertFunction) {
        super();
        if (logger.isDebugEnabled()) {
            logger.debug("RemoteControlView(IBox, boolean, UnaryFunction) - start");
        }
        stayOnTop = isOnTop;
        setAlertFunction(alertFunction);
        this.box = box;
        remoteControl = box.getRemoteControl();
        remoteControl.setPrinter(new UnaryFunction() {

            public Object execute(final Object arg) {
                if (logger.isDebugEnabled()) {
                    logger.debug("RemoteControlView$UnaryFunction.execute(Object) - start");
                }
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (logger.isDebugEnabled()) {
                            logger.debug("RemoteControlView$Runnable.run() - start");
                        }
                        Date now = new Date();
                        getMonitorList().append(form.format(new Object[] { new Integer(lineNo), now, arg }));
                        lineNo++;
                        monitorList.setCaretPosition(monitorList.getText().length());
                        if (logger.isDebugEnabled()) {
                            logger.debug("RemoteControlView$Runnable.run() - end");
                        }
                    }
                });
                if (logger.isDebugEnabled()) {
                    logger.debug("RemoteControlView$UnaryFunction.execute(Object) - end");
                }
                return null;
            }
        });
        initialize();
        fill();
        if (logger.isDebugEnabled()) {
            logger.debug("RemoteControlView(IBox, boolean, UnaryFunction) - end");
        }
    }

    /**
	 * @param host
	 */
    private void fill() {
        if (logger.isDebugEnabled()) {
            logger.debug("fill() - start");
        }
        remoteControl.addListener(this);
        remoteControl.initSocket();
        remoteControl.startLogging();
        if (box.isRunning()) initOsd();
        if (logger.isDebugEnabled()) {
            logger.debug("fill() - end");
        }
    }

    /**
	 * @param host
	 */
    private void initOsd() {
        if (logger.isDebugEnabled()) {
            logger.debug("initOsd() - start");
        }
        try {
            osdReaderThread = box.getOsdReader();
            osdReaderThread.addListener(getJCompOsd());
            osdReaderThread.addListener(this);
        } catch (Exception e) {
            logger.error("initOsd()", e);
            getAlertFunction().execute("No Connection to Box: " + e.getMessage());
            e.printStackTrace();
            int defaultCloseOperation = getDefaultCloseOperation();
            if (defaultCloseOperation == JFrame.EXIT_ON_CLOSE) {
                System.exit(1);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("initOsd() - end");
        }
    }

    /**
	 * @return
	 */
    private UnaryFunction getAlertFunction() {
        if (logger.isDebugEnabled()) {
            logger.debug("getAlertFunction() - start");
        }
        if (alertFunction == null) {
            alertFunction = new UnaryFunction() {

                public Object execute(Object arg) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("getAlertFunction$UnaryFunction.execute(Object) - start");
                    }
                    JOptionPane.showMessageDialog(RemoteControlView.this, arg, "Error", JOptionPane.ERROR_MESSAGE);
                    if (logger.isDebugEnabled()) {
                        logger.debug("getAlertFunction$UnaryFunction.execute(Object) - end");
                    }
                    return null;
                }
            };
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getAlertFunction() - end");
        }
        return alertFunction;
    }

    /**
	 * Return the JFrameContentPane property value.
	 * 
	 * @return javax.swing.JPanel
	 */
    private javax.swing.JPanel getJFrameContentPane() {
        if (ivjJFrameContentPane == null) {
            rcPicture = new JLabel();
            ivjJFrameContentPane = new javax.swing.JPanel();
            ivjJFrameContentPane.setName("JFrameContentPane");
            ivjJFrameContentPane.setLayout(new BorderLayout());
            rcPicture.setText("");
            rcPicture.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            rcPicture.setIcon(new ImageIcon(getClass().getResource("/net/sourceforge/cridremote/RemoteControl.gif")));
            rcPicture.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            rcPicture.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
            rcPicture.setVerticalAlignment(javax.swing.SwingConstants.TOP);
            ivjJFrameContentPane.setFocusCycleRoot(true);
            ivjJFrameContentPane.add(rcPicture, java.awt.BorderLayout.WEST);
            ivjJFrameContentPane.add(getJSplitPane(), java.awt.BorderLayout.CENTER);
            rcPicture.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

                public void mouseMoved(java.awt.event.MouseEvent e) {
                    RemoteControl.RCKey key = remoteControl.isKey(e.getPoint());
                    if (key != null) {
                        rcPicture.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        rcPicture.setToolTipText(key.getTooltip());
                    } else {
                        rcPicture.setCursor(Cursor.getDefaultCursor());
                        rcPicture.setToolTipText("");
                    }
                }
            });
            rcPicture.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    lockGui();
                    Point p = e.getPoint();
                    RemoteControl.RCKey key = remoteControl.hitKey(p);
                    if (key != null) {
                        infoLabel.setText(key.getCode());
                        recordKey(key);
                    } else {
                        infoLabel.setText("Key Info");
                    }
                    unlockGui();
                }
            });
        }
        return ivjJFrameContentPane;
    }

    /**
	 * @param key
	 */
    protected void recordKey(RemoteControl.RCKey key) {
        if (logger.isDebugEnabled()) {
            logger.debug("recordKey(RemoteControl.RCKey) - start");
        }
        if (inRecording) {
            currentMacro.add(key);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("recordKey(RemoteControl.RCKey) - end");
        }
    }

    /**
	 * Initialize the class.
	 */
    private void initialize() {
        if (logger.isDebugEnabled()) {
            logger.debug("initialize() - start");
        }
        this.setSize(870, 627);
        this.setLocation(0, 0);
        this.setName("JFrame1");
        this.setzeTitel();
        this.setContentPane(getJFrameContentPane());
        this.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("$java.awt.event.KeyAdapter.keyPressed(java.awt.event.KeyEvent) - start");
                }
                RCKey key = remoteControl.sendKey(e.getKeyCode());
                if (key != null) {
                    infoLabel.setText(key.getCode());
                    recordKey(key);
                } else {
                    infoLabel.setText("Key Info");
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("$java.awt.event.KeyAdapter.keyPressed(java.awt.event.KeyEvent) - end");
                }
            }
        });
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowDeactivated(java.awt.event.WindowEvent e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("$java.awt.event.WindowAdapter.windowDeactivated(java.awt.event.WindowEvent) - start");
                }
                if (isStayOnTop()) {
                    RemoteControlView.this.toFront();
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("$java.awt.event.WindowAdapter.windowDeactivated(java.awt.event.WindowEvent) - end");
                }
            }
        });
        if (logger.isDebugEnabled()) {
            logger.debug("initialize() - end");
        }
    }

    /**
	 * This method initializes jList
	 * 
	 * @return javax.swing.JList
	 */
    private JTextArea getMonitorList() {
        if (monitorList == null) {
            monitorList = new JTextArea();
        }
        return monitorList;
    }

    /**
	 * This method initializes jScrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getMonitorList());
            jScrollPane.setPreferredSize(new java.awt.Dimension(150, 80));
        }
        return jScrollPane;
    }

    /**
	 * This method initializes jPanel1 gridLayout7.setRows(1);
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getProgramButtonPanel() {
        if (programButtonPanel == null) {
            programButtonPanel = new JPanel();
            programButtonPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
            List serviceList = box.getChannelManager().getServicesSortedByProgramming();
            GridLayout gridLayout = new GridLayout();
            gridLayout.setColumns(3);
            gridLayout.setRows((serviceList.size() / 3) + 1);
            programButtonPanel.setLayout(gridLayout);
            for (Iterator iter = serviceList.iterator(); iter.hasNext(); ) {
                final Channel channel = (Channel) iter.next();
                JButton b = new JButton();
                Dimension dimension = new Dimension(59, 42);
                b.setMaximumSize(dimension);
                b.setSize(dimension);
                b.setMinimumSize(dimension);
                b.setPreferredSize(dimension);
                b.setIcon(channel.getLogo());
                b.setBackground(Color.WHITE);
                b.setToolTipText(channel.getName().getDisplayString());
                b.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        lockGui();
                        remoteControl.switchToStation(channel.getPosition());
                        unlockGui();
                    }
                });
                programButtonPanel.add(b);
            }
        }
        return programButtonPanel;
    }

    /**
	 * This method initializes jPanel1
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getMacroPanel() {
        if (macroPanel == null) {
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            macroPanel = new JPanel();
            macroPanel.setLayout(new GridBagLayout());
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.gridy = 0;
            gridBagConstraints2.insets = new java.awt.Insets(2, 2, 2, 2);
            gridBagConstraints2.anchor = java.awt.GridBagConstraints.CENTER;
            gridBagConstraints2.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints2.gridwidth = 2;
            gridBagConstraints5.gridx = 2;
            gridBagConstraints5.gridy = 0;
            gridBagConstraints5.gridwidth = 1;
            gridBagConstraints5.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints5.anchor = java.awt.GridBagConstraints.CENTER;
            gridBagConstraints5.insets = new java.awt.Insets(2, 2, 2, 2);
            gridBagConstraints6.gridx = 0;
            gridBagConstraints6.gridy = 2;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.gridwidth = 3;
            gridBagConstraints6.insets = new java.awt.Insets(2, 2, 2, 2);
            macroPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.SoftBevelBorder.RAISED));
            macroPanel.add(getRecordButton(), gridBagConstraints2);
            macroPanel.add(getMacroList(), gridBagConstraints6);
            macroPanel.add(getPlayButton(), gridBagConstraints5);
        }
        return macroPanel;
    }

    /**
	 * This method initializes jPanel1
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel();
            jPanel1.setLayout(new BorderLayout());
            jPanel1.add(getMacroPanel(), java.awt.BorderLayout.NORTH);
            jPanel1.add(getStationPanel(), java.awt.BorderLayout.CENTER);
            jPanel1.add(getInfoPanel(), java.awt.BorderLayout.SOUTH);
        }
        return jPanel1;
    }

    /**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getRecordButton() {
        if (recordButton == null) {
            recordButton = new JButton();
            recordButton.setText("Record Macro");
            recordButton.setBackground(java.awt.Color.red);
            recordButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (inRecording) {
                        recordButton.setText("Record Macro");
                        inRecording = false;
                        recordButton.setBackground(java.awt.Color.red);
                        String macroName = JOptionPane.showInputDialog("New macro name?");
                        if (macroName != null) {
                            remoteControl.addMacro(macroName, currentMacro);
                            macroList.setSelectedIndex(remoteControl.getMacros().size() - 1);
                            playButton.setEnabled(remoteControl.getMacros().size() > 0);
                        }
                    } else {
                        recordButton.setText("Stop Recording");
                        recordButton.setBackground(java.awt.Color.yellow);
                        inRecording = true;
                        currentMacro = new ArrayList();
                        playButton.setEnabled(false);
                    }
                }
            });
        }
        return recordButton;
    }

    /**
	 * This method initializes jButton1
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getPlayButton() {
        if (playButton == null) {
            playButton = new JButton();
            playButton.setText("Play Macro");
            playButton.setBackground(java.awt.Color.green);
            playButton.setEnabled(macroListModel.getSize() > 0);
            playButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    lockGui();
                    Object[] selectedObjects = macroList.getSelectedObjects();
                    remoteControl.sendMacro((RemoteControl.Macro) selectedObjects[0], true, RemoteControlView.this);
                    unlockGui();
                }
            });
        }
        return playButton;
    }

    /**
	 * This method initializes jComboBox
	 * 
	 * @return javax.swing.JComboBox
	 */
    private JComboBox getMacroList() {
        if (macroList == null) {
            macroList = new JComboBox();
            macroList.setEditable(false);
            macroList.setModel(getMacroListModel());
        }
        return macroList;
    }

    /**
	 * This method initializes macroListModel
	 * 
	 * @return javax.swing.DefaultComboBoxModel
	 */
    private DefaultComboBoxModel getMacroListModel() {
        if (macroListModel == null) {
            macroListModel = new DefaultComboBoxModel(remoteControl.getMacros());
        }
        return macroListModel;
    }

    /**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getInfoPanel() {
        if (infoPanel == null) {
            infoLabel = new JLabel();
            infoPanel = new JPanel();
            infoPanel.setLayout(new BorderLayout());
            infoLabel.setText("JLabel");
            infoLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            infoPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.SoftBevelBorder.RAISED));
            infoPanel.add(infoLabel, java.awt.BorderLayout.NORTH);
            infoPanel.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
        }
        return infoPanel;
    }

    /**
	 * This method initializes jScrollPane1
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getStationPanel() {
        if (stationPanel == null) {
            stationPanel = new JScrollPane();
            stationPanel.setViewportView(getProgramButtonPanel());
            stationPanel.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            stationPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.SoftBevelBorder.RAISED));
        }
        return stationPanel;
    }

    /**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.setLayout(new BorderLayout());
            jPanel.add(getJCompOsd(), java.awt.BorderLayout.CENTER);
        }
        return jPanel;
    }

    /**
	 * This method initializes jCompOsd
	 * 
	 * @return JCompOsd
	 */
    private JCompOsd getJCompOsd() {
        if (jCompOsd == null) {
            jCompOsd = new JCompOsd(alertFunction);
            jCompOsd.setDoubleBuffered(false);
        }
        return jCompOsd;
    }

    /**
	 * This method initializes jSplitPane
	 * 
	 * @return javax.swing.JSplitPane
	 */
    private JSplitPane getJSplitPane() {
        if (jSplitPane == null) {
            jSplitPane = new JSplitPane();
            jSplitPane.setDividerSize(14);
            jSplitPane.setOneTouchExpandable(true);
            jSplitPane.setLeftComponent(getJScrollPane2());
            jSplitPane.setRightComponent(getJPanel1());
            jSplitPane.setDividerLocation((int) jSplitPane.getPreferredSize().getWidth());
        }
        return jSplitPane;
    }

    /**
	 * This method initializes jScrollPane2
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getJScrollPane2() {
        if (jScrollPane2 == null) {
            jScrollPane2 = new JScrollPane();
            jScrollPane2.setViewportView(getJPanel());
            jScrollPane2.setMinimumSize(new java.awt.Dimension(300, 200));
            jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }
        return jScrollPane2;
    }

    public static void main(String[] args) {
        if (logger.isDebugEnabled()) {
            logger.debug("main(String[]) - start");
        }
        Settings.init(args);
        BoxManager boxManager = (BoxManager) ServiceProvider.instance().getService(RemoteControlView.class, BoxManager.class);
        if (boxManager.getBoxCount() > 0) {
            IBox box = null;
            if (args.length > 0) box = boxManager.getBox(args[0]);
            if (box == null) box = boxManager.getBoxes()[0];
            RemoteControlView view = new RemoteControlView(box);
            view.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            view.setVisible(true);
        } else {
        }
        if (logger.isDebugEnabled()) {
            logger.debug("main(String[]) - end");
        }
    }

    /**
	 * 
	 */
    private void lockGui() {
        if (logger.isDebugEnabled()) {
            logger.debug("lockGui() - start");
        }
        cursorStack.push(ivjJFrameContentPane.getCursor());
        ivjJFrameContentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        recordButton.setEnabled(false);
        playButton.setEnabled(false);
        macroList.setEnabled(false);
        rcPicture.setEnabled(false);
        if (logger.isDebugEnabled()) {
            logger.debug("lockGui() - end");
        }
    }

    /**
	 * 
	 */
    private void unlockGui() {
        if (logger.isDebugEnabled()) {
            logger.debug("unlockGui() - start");
        }
        Cursor tempCursor = (Cursor) cursorStack.pop();
        if (cursorStack.empty()) {
            ivjJFrameContentPane.setCursor(tempCursor);
            ivjJFrameContentPane.enableInputMethods(true);
            recordButton.setEnabled(true);
            playButton.setEnabled(true);
            macroList.setEnabled(true);
            rcPicture.setEnabled(true);
        }
        requestFocus();
        if (logger.isDebugEnabled()) {
            logger.debug("unlockGui() - end");
        }
    }

    public void osdModified(OsdEvent event) {
    }

    public void osdReadComplete() {
        if (logger.isDebugEnabled()) {
            logger.debug("osdReadComplete() - OSD: readCompleted");
        }
    }

    public void osdConnectionLost(OsdEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("osdConnectionLost(OsdEvent) - start");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("osdConnectionLost() - Connection zum OSD weg!");
        }
        getAlertFunction().execute("Connection to Osd2Tcp-server: Lost");
        if (logger.isDebugEnabled()) {
            logger.debug("osdConnectionLost(OsdEvent) - end");
        }
    }

    public void rcConnectionLost(EventObject e) {
        if (logger.isDebugEnabled()) {
            logger.debug("rcConnectionLost() - Connection weg!");
        }
        rcPicture.setEnabled(false);
        setzeTitel(": Keine Verbindung.");
    }

    public void rcConnectionEstablished(EventObject e) {
        if (logger.isDebugEnabled()) {
            logger.debug("rcConnectionEstablished() - Connection da!");
        }
        rcPicture.setEnabled(true);
        initOsd();
        setzeTitel();
    }

    public void wrongVersion(ErrorEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("wrongVersion() - Falsche Version des Osd2Tcp-Servers");
        }
        setzeTitel(": " + event.getMessage());
    }

    public void setAlertFunction(UnaryFunction alertFunction) {
        if (logger.isDebugEnabled()) {
            logger.debug("setAlertFunction(UnaryFunction) - start");
        }
        this.alertFunction = alertFunction;
        if (logger.isDebugEnabled()) {
            logger.debug("setAlertFunction(UnaryFunction) - end");
        }
    }

    public void setStayOnTop(boolean stayOnTop) {
        if (logger.isDebugEnabled()) {
            logger.debug("setStayOnTop(boolean) - start");
        }
        this.stayOnTop = stayOnTop;
        if (logger.isDebugEnabled()) {
            logger.debug("setStayOnTop(boolean) - end");
        }
    }

    public boolean isStayOnTop() {
        return stayOnTop;
    }

    private void setzeTitel() {
        if (logger.isDebugEnabled()) {
            logger.debug("setzeTitel() - start");
        }
        setzeTitel("");
        if (logger.isDebugEnabled()) {
            logger.debug("setzeTitel() - end");
        }
    }

    private void setzeTitel(String message) {
        if (logger.isDebugEnabled()) {
            logger.debug("setzeTitel(String) - start");
        }
        setTitle(TITEL + " [" + box.getName() + "] " + message);
        if (logger.isDebugEnabled()) {
            logger.debug("setzeTitel(String) - end");
        }
    }
}
