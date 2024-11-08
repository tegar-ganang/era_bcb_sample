package gui.server;

import engine.clients.forwarder.ForwarderQueue;
import gui.outputpanel.MessageParameters;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import engine.server.Server;
import gui.outputpanel.ViewersPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.di.viewer.AbstractViewer.OffsetPolicy;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.channel.ChannelFutureListener;
import util.AppGlobals;
import util.Globals;
import streamlogger.StreamLogger;

public class pnlServerGUI extends javax.swing.JPanel {

    protected javax.swing.Timer forwarderTimer = null;

    private static Logger log = Logger.getLogger(pnlServerGUI.class.getName());

    private boolean forwarderConnected = false;

    private final Timer timer = new Timer();

    private final Timer timer1 = new Timer();

    protected String forwarderAddress = "";

    protected Integer forwarderPort = 0;

    protected Integer serverPort = 0;

    protected ListenersCollection listenersCollection = null;

    protected StreamLogger sl = null;

    public pnlServerGUI() {
        initComponents();
        if (!java.beans.Beans.isDesignTime()) {
            pnlInput.setDirectionShow(true);
            pnlInput.initList(AppGlobals.getInstance().getAppSettingsDirectory() + "list.server.txt");
            pnlReceivedData.setType(ViewersPanel.Type.input);
            pnlReceivedData.setTitle("Received data");
            pnlSentData.setType(ViewersPanel.Type.output);
            pnlSentData.setTitle("Sent data");
            pnlSystemLog.setTitle("System log");
            cmdShutdown.setSelected(true);
            cmdDisconnect.setSelected(true);
            forwarderTimer = new javax.swing.Timer(1000, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    fireForwarderUpTimeTicker();
                }
            });
            pnlInput.lockButtons(true);
            listenersCollection = new ListenersCollection(this);
            Globals.getInstance().getDlgOptions().fireAll();
        }
    }

    protected void outputDataArrivedToForwarder(BigEndianHeapChannelBuffer msg) {
        mirrorBinaryData(msg, pnlReceivedData);
    }

    protected void outputDataSentFromForwarder(ChannelBuffer msg) {
        mirrorBinaryData(msg, pnlSentData);
    }

    protected void sendDataFromForwarderToClient(final ChannelBuffer bufferedMessage) {
        Channel channel = Server.getAllChannels().find(pnlClientsBar1.getSelectedClient());
        if (channel != null) {
            ChannelFuture future = channel.write(bufferedMessage);
            future.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        mirrorBinaryData(bufferedMessage, pnlSentData);
                    } else {
                        pnlSystemLog.addLog("Some data tranmission error while data sending to client number: " + future.getChannel().getId() + " occure.", new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
                    }
                }
            });
        } else {
            pnlSystemLog.addLog("No client to send data selected.", new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
        }
    }

    protected void sendDataFromForwarderOut(BigEndianHeapChannelBuffer buffer) {
        Globals.getInstance().getForwarder().getChannel().write(buffer);
    }

    protected void sendDataFromForwarderOutAndLogIt(ChannelBuffer msg) {
        Globals.getInstance().getForwarder().getChannel().write(msg);
        mirrorBinaryData(msg, pnlSentData);
    }

    protected void outputDataArrivedToServerAndForwardedOut(BigEndianHeapChannelBuffer msg, String channelID) {
        mirrorBinaryData(msg, pnlReceivedData);
        mirrorBinaryData(msg, pnlSentData);
    }

    protected void outputDataArrivedToServer(BigEndianHeapChannelBuffer msg, String channelID) {
        mirrorBinaryData(msg, pnlReceivedData);
    }

    protected void outputDataSentFromServer(ChannelBuffer msg, String ClientID) {
        mirrorBinaryData(msg, pnlSentData);
    }

    private void mirrorBinaryData(BigEndianHeapChannelBuffer buffer, ViewersPanel outPanel) {
        byte[] b = new byte[buffer.writerIndex()];
        buffer.getBytes(0, b);
        outPanel.writeBinaryData(b);
        sl.log(b);
        b = null;
    }

    private void mirrorBinaryData(ChannelBuffer buffer, ViewersPanel outPanel) {
        byte[] b = new byte[buffer.writerIndex()];
        buffer.getBytes(0, b);
        outPanel.writeBinaryData(b);
        sl.log(b);
        b = null;
    }

    public void lockServerFields(boolean lock) {
        pnlServerPort.setEnabled(!lock);
        chkForward.setEnabled(!lock);
    }

    public void lockForwarderFields(boolean lock) {
        pnlForwarderPort.setEnabled(!lock);
        pnlForwarderAddress.setEnabled(!lock);
    }

    private void forwarderConnect() {
        if (cmdConnect.isSelected() == false) {
            if (!(pnlForwarderAddress.isRightFormat() && pnlForwarderPort.isRightFormat())) {
                JOptionPane.showMessageDialog(null, "Please check forwarder address and port fields.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                cmdConnect.setSelected(true);
                lockForwarderFields(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    log.error(ex.getMessage());
                }
                try {
                    forwarderPort = Integer.parseInt(pnlForwarderPort.getPort());
                    forwarderAddress = pnlForwarderAddress.getIPAddress();
                    Globals.getInstance().getForwarder().setHost(forwarderAddress);
                    Globals.getInstance().getForwarder().setPort(forwarderPort);
                    Globals.getInstance().getForwarder().start();
                } catch (NumberFormatException ex) {
                    log.error(ex.getMessage());
                    pnlSystemLog.addLog("NumberFormatException: " + ex.getMessage(), new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
                    timer.schedule(new checkForwardingTask(), 500);
                }
                timer.schedule(new checkForwardingTask(), 2 * 1000);
            }
        }
    }

    private void forwarderDisconnect() {
        if (cmdDisconnect.isSelected() == false) {
            byte[] bye = new byte[5];
            bye[0] = 0x3c;
            bye[1] = 0x62;
            bye[2] = 0x79;
            bye[3] = 0x65;
            bye[4] = 0x3e;
            ForwarderQueue.getInstance().put(bye);
        }
    }

    class checkForwardingTask extends TimerTask {

        public void run() {
            if (!isForwarderConnected()) {
                lockForwarderFields(false);
                cmdDisconnect.setSelected(true);
                lockForwarderFields(false);
            }
            timer.purge();
        }
    }

    class rollbackServerButtons extends TimerTask {

        public void run() {
            cmdListen.setSelected(false);
            cmdShutdown.setSelected(true);
            lockServerFields(false);
            timer1.purge();
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        pnlServerToolBar = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        pnlServerPort = new gui.addressfields.pnlPort();
        cmdListen = new javax.swing.JToggleButton();
        cmdShutdown = new javax.swing.JToggleButton();
        chkForward = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        pnlForwarderAddress = new gui.addressfields.pnlIp();
        jLabel6 = new javax.swing.JLabel();
        pnlForwarderPort = new gui.addressfields.pnlPort();
        cmdConnect = new javax.swing.JToggleButton();
        cmdDisconnect = new javax.swing.JToggleButton();
        pnlContent = new javax.swing.JPanel();
        splitClients = new javax.swing.JSplitPane();
        pnlClientsBar1 = new gui.clientsbar.pnlClientsBar();
        splitServerSystem = new javax.swing.JSplitPane();
        splitServerClient = new javax.swing.JSplitPane();
        pnlReceivedData = new gui.outputpanel.ViewersPanel();
        splitInputOutput = new javax.swing.JSplitPane();
        pnlInput = new gui.inputpanel.pnlInput();
        pnlSentData = new gui.outputpanel.ViewersPanel();
        pnlSystemLog = new gui.outputpanel.LogPanel();
        setLayout(new java.awt.BorderLayout());
        pnlServerToolBar.setPreferredSize(new java.awt.Dimension(986, 60));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 0, 0, new java.awt.Color(153, 153, 153)), "Local server", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel1.setMinimumSize(new java.awt.Dimension(493, 50));
        jPanel1.setPreferredSize(new java.awt.Dimension(543, 50));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));
        jLabel10.setText("Listening port");
        jPanel1.add(jLabel10);
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setForeground(new java.awt.Color(199, 7, 7));
        jLabel1.setText("*");
        jPanel1.add(jLabel1);
        jPanel1.add(jLabel2);
        jPanel1.add(pnlServerPort);
        buttonGroup1.add(cmdListen);
        cmdListen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/connect.png")));
        cmdListen.setText("Listen");
        cmdListen.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                cmdListenMousePressed(evt);
            }
        });
        jPanel1.add(cmdListen);
        buttonGroup1.add(cmdShutdown);
        cmdShutdown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/disconnect.png")));
        cmdShutdown.setText("Shutdown");
        cmdShutdown.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                cmdShutdownMousePressed(evt);
            }
        });
        jPanel1.add(cmdShutdown);
        chkForward.setText("Auto forward to target server");
        jPanel1.add(chkForward);
        pnlServerToolBar.add(jPanel1);
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 0, 0, new java.awt.Color(153, 153, 153)), "Target server", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel2.setPreferredSize(new java.awt.Dimension(428, 50));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));
        jPanel2.add(pnlForwarderAddress);
        jLabel6.setText(":");
        jPanel2.add(jLabel6);
        jPanel2.add(pnlForwarderPort);
        buttonGroup2.add(cmdConnect);
        cmdConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/connect1.png")));
        cmdConnect.setText("Connect");
        cmdConnect.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                cmdConnectMousePressed(evt);
            }
        });
        jPanel2.add(cmdConnect);
        buttonGroup2.add(cmdDisconnect);
        cmdDisconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/disconnect1.png")));
        cmdDisconnect.setText("Disconnect");
        cmdDisconnect.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                cmdDisconnectMousePressed(evt);
            }
        });
        jPanel2.add(cmdDisconnect);
        pnlServerToolBar.add(jPanel2);
        add(pnlServerToolBar, java.awt.BorderLayout.PAGE_START);
        pnlContent.setLayout(new java.awt.BorderLayout());
        splitClients.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitClients.setDividerLocation(200);
        splitClients.setDividerSize(16);
        splitClients.setOneTouchExpandable(true);
        pnlClientsBar1.setMinimumSize(new java.awt.Dimension(0, 14));
        splitClients.setLeftComponent(pnlClientsBar1);
        splitServerSystem.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitServerSystem.setDividerSize(8);
        splitServerSystem.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitServerSystem.setOneTouchExpandable(true);
        splitServerClient.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitServerClient.setDividerSize(16);
        splitServerClient.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitServerClient.setMinimumSize(new java.awt.Dimension(692, 400));
        splitServerClient.setOneTouchExpandable(true);
        splitServerClient.setTopComponent(pnlReceivedData);
        splitInputOutput.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitInputOutput.setDividerSize(16);
        splitInputOutput.setOneTouchExpandable(true);
        pnlInput.setMinimumSize(new java.awt.Dimension(240, 140));
        pnlInput.setPreferredSize(new java.awt.Dimension(347, 70));
        splitInputOutput.setLeftComponent(pnlInput);
        pnlSentData.setMinimumSize(new java.awt.Dimension(600, 141));
        splitInputOutput.setRightComponent(pnlSentData);
        splitServerClient.setBottomComponent(splitInputOutput);
        splitServerSystem.setTopComponent(splitServerClient);
        splitServerSystem.setRightComponent(pnlSystemLog);
        splitClients.setRightComponent(splitServerSystem);
        pnlContent.add(splitClients, java.awt.BorderLayout.CENTER);
        add(pnlContent, java.awt.BorderLayout.CENTER);
    }

    private void cmdShutdownMousePressed(java.awt.event.MouseEvent evt) {
        if (cmdShutdown.isSelected() != true) {
            Globals.getInstance().getServer().gracefullyClose();
            if (chkForward.isSelected() == true) {
                forwarderDisconnect();
            }
            pnlClientsBar1.getOneSecondTimer().stop();
            Globals.getInstance().getServerClientSender().stop();
            pnlSystemLog.addLog("Disconnected.", new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.boldOrange));
            pnlClientsBar1.clearAll();
            lockServerFields(false);
            pnlInput.lockButtons(true);
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    if (Globals.getConfig().getProperty("Server.clear_prompt").toString().equalsIgnoreCase("true")) {
                        int ret = JOptionPane.showConfirmDialog(null, "Would you like to clear SERVER output windows?", "Confirmation", JOptionPane.YES_NO_OPTION);
                        if (ret == 0) {
                            pnlSystemLog.clearAll();
                            pnlReceivedData.clearAll();
                            pnlSentData.clearAll();
                        }
                    }
                }
            });
            fireServerShutDown();
        }
    }

    private void cmdListenMousePressed(java.awt.event.MouseEvent evt) {
        if (pnlServerPort.isRightFormat() == false) {
            JOptionPane.showMessageDialog(null, "Please check all mandatory fields.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            if (cmdListen.isSelected() != true) {
                try {
                    serverPort = Integer.parseInt(pnlServerPort.getPort());
                    if (Globals.getInstance().getServer().startListening(serverPort)) {
                        pnlClientsBar1.getOneSecondTimer().start();
                        Globals.getInstance().getServerClientSender().start();
                        pnlSystemLog.addLog("Listening on port: " + serverPort, new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.boldGreen));
                        lockServerFields(true);
                        pnlInput.lockButtons(false);
                        if (chkForward.isSelected() == true) {
                            forwarderConnect();
                        }
                        fireServerListen();
                    } else {
                        pnlSystemLog.addLog("Bind to port: " + serverPort + " failed", new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
                        timer1.schedule(new rollbackServerButtons(), 500);
                    }
                } catch (NumberFormatException ex) {
                    log.error(ex.getMessage());
                    pnlSystemLog.addLog(ex.getMessage(), new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
                    timer1.schedule(new rollbackServerButtons(), 500);
                }
            }
        }
    }

    private void cmdConnectMousePressed(java.awt.event.MouseEvent evt) {
        forwarderConnect();
    }

    private void cmdDisconnectMousePressed(java.awt.event.MouseEvent evt) {
        forwarderDisconnect();
    }

    private javax.swing.ButtonGroup buttonGroup1;

    private javax.swing.ButtonGroup buttonGroup2;

    private javax.swing.JCheckBox chkForward;

    protected javax.swing.JToggleButton cmdConnect;

    protected javax.swing.JToggleButton cmdDisconnect;

    protected javax.swing.JToggleButton cmdListen;

    protected javax.swing.JToggleButton cmdShutdown;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    protected gui.clientsbar.pnlClientsBar pnlClientsBar1;

    private javax.swing.JPanel pnlContent;

    private gui.addressfields.pnlIp pnlForwarderAddress;

    private gui.addressfields.pnlPort pnlForwarderPort;

    protected gui.inputpanel.pnlInput pnlInput;

    protected gui.outputpanel.ViewersPanel pnlReceivedData;

    protected gui.outputpanel.ViewersPanel pnlSentData;

    private gui.addressfields.pnlPort pnlServerPort;

    private javax.swing.JPanel pnlServerToolBar;

    protected gui.outputpanel.LogPanel pnlSystemLog;

    private javax.swing.JSplitPane splitClients;

    protected javax.swing.JSplitPane splitInputOutput;

    protected javax.swing.JSplitPane splitServerClient;

    protected javax.swing.JSplitPane splitServerSystem;

    protected javax.swing.event.EventListenerList CustomEventsListenerList = new javax.swing.event.EventListenerList();

    public void addListener(ServerGUIListenerIF listener) {
        CustomEventsListenerList.add(ServerGUIListenerIF.class, listener);
    }

    public void removeListener(ServerGUIListenerIF listener) {
        CustomEventsListenerList.remove(ServerGUIListenerIF.class, listener);
    }

    public void fireServerUpTimeTicker() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerGUIListenerIF.class) {
                ((ServerGUIListenerIF) listeners[i + 1]).ServerUpTimeTicker();
            }
        }
    }

    public void fireForwarderUpTimeTicker() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerGUIListenerIF.class) {
                ((ServerGUIListenerIF) listeners[i + 1]).ForwarderUpTimeTicker();
            }
        }
    }

    public void fireServerShutDown() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerGUIListenerIF.class) {
                ((ServerGUIListenerIF) listeners[i + 1]).ServerShutDown();
            }
        }
    }

    public void fireServerListen() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerGUIListenerIF.class) {
                ((ServerGUIListenerIF) listeners[i + 1]).ServerListen(serverPort);
            }
        }
    }

    public void fireServerForwardingStart() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerGUIListenerIF.class) {
                ((ServerGUIListenerIF) listeners[i + 1]).ServerForwardingStart(forwarderPort, forwarderAddress);
            }
        }
    }

    public void fireServerForwardingStop() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerGUIListenerIF.class) {
                ((ServerGUIListenerIF) listeners[i + 1]).ServerForwardingStop();
            }
        }
    }

    public OffsetPolicy getSentOffsetPolicy() {
        return pnlSentData.getOffsetPolicy();
    }

    public void setSentOffsetPolicy(OffsetPolicy offsetPolicy) {
        pnlSentData.setOffsetPolicy(offsetPolicy);
    }

    public OffsetPolicy getReceivedOffsetPolicy() {
        return pnlReceivedData.getOffsetPolicy();
    }

    public void setReceivedOffsetPolicy(OffsetPolicy offsetPolicy) {
        pnlReceivedData.setOffsetPolicy(offsetPolicy);
    }

    public Integer getSplitServerClientDividerLocation() {
        return splitServerClient.getDividerLocation();
    }

    public void setSplitServerClientDividerLocation(Integer SplitClientsDividerLocation) {
        splitServerClient.setDividerLocation(SplitClientsDividerLocation);
    }

    public Integer getSplitClientsDividerLocation() {
        return splitClients.getDividerLocation();
    }

    public void setSplitClientsDividerLocation(Integer SplitClientsDividerLocation) {
        splitClients.setDividerLocation(SplitClientsDividerLocation);
    }

    public Integer getSplitServerSystemDividerLocation() {
        return splitServerSystem.getDividerLocation();
    }

    public void setSplitServerSystemDividerLocation(Integer SplitServerSystemDividerLocation) {
        splitServerSystem.setDividerLocation(SplitServerSystemDividerLocation);
    }

    public Integer getSplitInputOutput() {
        return splitInputOutput.getDividerLocation();
    }

    public void setSplitInputOutput(Integer SplitSplitInputOutput) {
        splitInputOutput.setDividerLocation(SplitSplitInputOutput);
    }

    public int getSentDataWindowActiveTab() {
        return pnlSentData.getActiveTab();
    }

    public void setSentDataWindowActiveTab(int ActiveTab) {
        pnlSentData.setActiveTab(ActiveTab);
    }

    public int getReceivedDataWindowActiveTab() {
        return pnlReceivedData.getActiveTab();
    }

    public void setReceivedDataWindowActiveTab(int ActiveTab) {
        pnlReceivedData.setActiveTab(ActiveTab);
    }

    public String getTextToSend() {
        return pnlInput.getFreeTextToSend();
    }

    public void setTextToSend(String TextToSend) {
        pnlInput.setFreeTextToSend(TextToSend);
    }

    public String getForwardingPort() {
        return pnlForwarderPort.getPort();
    }

    public void setForwardingPort(String ForwardingPort) {
        pnlForwarderPort.setPort(ForwardingPort);
    }

    public String getForwardingAddress() {
        return pnlForwarderAddress.getIPAddress();
    }

    public void setForwardingAddress(String ForwardingAddress) {
        pnlForwarderAddress.setIPAddress(ForwardingAddress);
    }

    public boolean isAutoForwarderOn() {
        return chkForward.isSelected();
    }

    public void setAutoForwarderOn(boolean ForwarderOn) {
        chkForward.setSelected(ForwarderOn);
    }

    public boolean isApppendCR() {
        return pnlInput.isAddCR();
    }

    public void setApppendCR(boolean ApppendServerCR) {
        pnlInput.setAddCR(ApppendServerCR);
    }

    public boolean isApppendLF() {
        return pnlInput.isAddLF();
    }

    public void setApppendLF(boolean ApppendServerLF) {
        pnlInput.setAddLF(ApppendServerLF);
    }

    public String getListeningPort() {
        return pnlServerPort.getPort();
    }

    public void setListeningPort(String ListeningPort) {
        pnlServerPort.setPort(ListeningPort);
    }

    public boolean isForwarderConnected() {
        return forwarderConnected;
    }

    public void setForwarderConnected(boolean forwarderConnected) {
        this.forwarderConnected = forwarderConnected;
    }
}
