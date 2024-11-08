package gui.client;

import engine.clients.ClientEventsListenerIF;
import engine.clients.client.ClientQueue;
import gui.inputpanel.InputPanelListenerIF;
import gui.settings.SettingsListenerIF;
import gui.outputpanel.MessageParameters;
import gui.outputpanel.ViewersPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.di.viewer.AbstractViewer.OffsetPolicy;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import streamlogger.StreamLogger;
import util.AppGlobals;
import util.Globals;

public class pnlClientGUI extends javax.swing.JPanel {

    private javax.swing.Timer oneSecondTimer = null;

    private boolean connected = false;

    private Timer timer = new Timer();

    private String clientAddress = "";

    private Integer clientPort = 0;

    private static Logger log = Logger.getLogger(pnlClientGUI.class.getName());

    private StreamLogger sl = null;

    public pnlClientGUI() {
        initComponents();
        if (!java.beans.Beans.isDesignTime()) {
            pnlInput.setDirectionShow(false);
            pnlInput.initList(AppGlobals.getInstance().getAppSettingsDirectory() + "list.client.txt");
            pnlReceivedData.setType(ViewersPanel.Type.input);
            pnlReceivedData.setTitle("Received data");
            pnlSentData.setType(ViewersPanel.Type.output);
            pnlSentData.setTitle("Sent data");
            pnlSystemLog.setTitle("System log");
            cmdDisconnect.setSelected(true);
            Globals.getInstance().getDlgOptions().addListener(new SettingsListenerIF() {

                public void ServerFileListDeleted() {
                }

                public void ClientFileListDeleted() {
                    pnlInput.refreshList();
                }

                public void ServerSentWindowDataBufferSize(int size) {
                }

                public void ServerReceivedWindowDataBufferSize(int size) {
                }

                public void ClientSentWindowDataBufferSize(int size) {
                    pnlSentData.setMaxKBytes(size);
                }

                public void ClientReceivedWindowDataBufferSize(int size) {
                    pnlReceivedData.setMaxKBytes(size);
                }

                public void SendingDataChunkSize(int size) {
                    pnlInput.setPacketSize(size);
                }

                public void ServerLoggingSettingsChanged(String path, int size, int filesCount, boolean isLoggingEnabled) {
                }

                public void ClientLoggingSettingsChanged(String path, int size, int filesCount, boolean isLoggingEnabled) {
                    sl = new StreamLogger(path, "client.log", size, filesCount);
                    sl.setEnableLogging(isLoggingEnabled);
                }
            });
            oneSecondTimer = new javax.swing.Timer(1000, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    fireClientUpTimeTicker();
                }
            });
            Globals.getInstance().getClient().addListener(new ClientEventsListenerIF() {

                public void DataArrived(ChannelHandlerContext ctx, final MessageEvent e) {
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            outputDataArrived((BigEndianHeapChannelBuffer) e.getMessage());
                        }
                    });
                }

                public void Connected(ChannelHandlerContext ctx, ChannelStateEvent e) {
                    clientPort = Integer.parseInt(pnlServerPort.getPort());
                    clientAddress = pnlServerAddress.getIPAddress();
                    oneSecondTimer.start();
                    pnlSystemLog.addLog("Connected to server: " + clientAddress + ":" + clientPort, new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.boldGreen));
                    lockFields(true);
                    pnlInput.lockButtons(false);
                    fireClientConnect();
                    connected = true;
                }

                public void Disconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
                    connected = false;
                    Globals.getInstance().getClient().shutdown();
                    oneSecondTimer.stop();
                    pnlSystemLog.addLog("Disconnected.", new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.boldOrange));
                    lockFields(false);
                    pnlInput.lockButtons(true);
                    cmdDisconnect.setSelected(true);
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            if (Globals.getConfig().getProperty("Client.clear_prompt").toString().equalsIgnoreCase("true")) {
                                int ret = JOptionPane.showConfirmDialog(null, "Would you like to clear CLIENT output windows?", "Confirmation", JOptionPane.YES_NO_OPTION);
                                if (ret == 0) {
                                    pnlSystemLog.clearAll();
                                    pnlReceivedData.clearAll();
                                    pnlSentData.clearAll();
                                }
                            }
                        }
                    });
                    fireClientDisconnect();
                }

                public void ExceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
                    pnlSystemLog.addLog("Error: " + e.getCause().getMessage(), new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error, e.getChannel().getId().toString()));
                }

                public void DataSentFromQueue(final ChannelBuffer bufferedMessage) {
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            outputDataSent(bufferedMessage);
                        }
                    });
                }
            });
            pnlInput.addEventsListener(new InputPanelListenerIF() {

                public void SendChannelBuffer(ChannelBuffer bufferedMessage) {
                    if (pnlInput.isAddCR()) {
                        bufferedMessage.writeByte((byte) 0xd);
                    }
                    if (pnlInput.isAddLF()) {
                        bufferedMessage.writeByte((byte) 0xa);
                    }
                    sendDataFromClientOut(bufferedMessage);
                }

                public void ExceptionCaught(String msg) {
                    pnlSystemLog.addLog(msg, new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
                }
            });
            pnlInput.lockButtons(true);
            Globals.getInstance().getDlgOptions().fireAll();
        }
    }

    private void outputDataArrived(BigEndianHeapChannelBuffer msg) {
        mirrorBinaryData(msg, pnlReceivedData);
    }

    private void outputDataSent(ChannelBuffer msg) {
        mirrorBinaryData(msg, pnlSentData);
    }

    public void sendDataFromClientOut(ChannelBuffer buffer) {
        ClientQueue.getInstance().put(buffer);
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

    class checkConnectionTask extends TimerTask {

        public void run() {
            if (!connected) {
                cmdDisconnect.setSelected(true);
                lockFields(false);
            }
            timer.purge();
        }
    }

    private void lockFields(boolean lock) {
        pnlServerAddress.setEnabled(!lock);
        pnlServerPort.setEnabled(!lock);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        buttonGroup1 = new javax.swing.ButtonGroup();
        pnlClientToolBar = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        pnlServerAddress = new gui.addressfields.pnlIp();
        jLabel5 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        pnlServerPort = new gui.addressfields.pnlPort();
        cmdConnect = new javax.swing.JToggleButton();
        cmdDisconnect = new javax.swing.JToggleButton();
        splitServerSystem = new javax.swing.JSplitPane();
        splitServerClient = new javax.swing.JSplitPane();
        splitInputOutput = new javax.swing.JSplitPane();
        pnlInput = new gui.inputpanel.pnlInput();
        pnlSentData = new gui.outputpanel.ViewersPanel();
        pnlReceivedData = new gui.outputpanel.ViewersPanel();
        pnlSystemLog = new gui.outputpanel.LogPanel();
        setLayout(new java.awt.BorderLayout());
        pnlClientToolBar.setPreferredSize(new java.awt.Dimension(818, 50));
        pnlClientToolBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 0, 0, new java.awt.Color(153, 153, 153)), "Client", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel1.setPreferredSize(new java.awt.Dimension(600, 50));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));
        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel3.setForeground(new java.awt.Color(199, 7, 7));
        jLabel3.setText("*");
        jPanel1.add(jLabel3);
        jLabel2.setText("Server");
        jPanel1.add(jLabel2);
        jPanel1.add(pnlServerAddress);
        jLabel5.setText("       ");
        jPanel1.add(jLabel5);
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setForeground(new java.awt.Color(199, 7, 7));
        jLabel1.setText("*");
        jPanel1.add(jLabel1);
        jLabel6.setText("Port");
        jPanel1.add(jLabel6);
        jPanel1.add(pnlServerPort);
        buttonGroup1.add(cmdConnect);
        cmdConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/connect1.png")));
        cmdConnect.setText("Connect");
        cmdConnect.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                cmdConnectMousePressed(evt);
            }
        });
        jPanel1.add(cmdConnect);
        buttonGroup1.add(cmdDisconnect);
        cmdDisconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/disconnect1.png")));
        cmdDisconnect.setText("Disconnect");
        cmdDisconnect.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                cmdDisconnectMousePressed(evt);
            }
        });
        jPanel1.add(cmdDisconnect);
        pnlClientToolBar.add(jPanel1);
        add(pnlClientToolBar, java.awt.BorderLayout.PAGE_START);
        splitServerSystem.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitServerSystem.setDividerSize(8);
        splitServerSystem.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitServerSystem.setOneTouchExpandable(true);
        splitServerClient.setDividerSize(16);
        splitServerClient.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitServerClient.setMinimumSize(new java.awt.Dimension(688, 400));
        splitServerClient.setOneTouchExpandable(true);
        splitInputOutput.setDividerSize(16);
        splitInputOutput.setOneTouchExpandable(true);
        pnlInput.setMinimumSize(new java.awt.Dimension(240, 140));
        pnlInput.setPreferredSize(new java.awt.Dimension(100, 140));
        splitInputOutput.setLeftComponent(pnlInput);
        splitInputOutput.setRightComponent(pnlSentData);
        splitServerClient.setRightComponent(splitInputOutput);
        splitServerClient.setLeftComponent(pnlReceivedData);
        splitServerSystem.setTopComponent(splitServerClient);
        splitServerSystem.setRightComponent(pnlSystemLog);
        add(splitServerSystem, java.awt.BorderLayout.CENTER);
    }

    private void cmdDisconnectMousePressed(java.awt.event.MouseEvent evt) {
        if (cmdDisconnect.isSelected() == false) {
            byte[] bye = new byte[5];
            bye[0] = 0x3c;
            bye[1] = 0x62;
            bye[2] = 0x79;
            bye[3] = 0x65;
            bye[4] = 0x3e;
            ClientQueue.getInstance().put(bye);
        }
    }

    private void cmdConnectMousePressed(java.awt.event.MouseEvent evt) {
        if (!(pnlServerAddress.isRightFormat() && pnlServerPort.isRightFormat())) {
            JOptionPane.showMessageDialog(null, "Please check server address and port fields.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            if (cmdConnect.isSelected() == false) {
                cmdConnect.setSelected(true);
                lockFields(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    log.error(ex.getMessage());
                }
                try {
                    clientPort = Integer.parseInt(pnlServerPort.getPort());
                    clientAddress = pnlServerAddress.getIPAddress();
                    Globals.getInstance().getClient().setHost(clientAddress);
                    Globals.getInstance().getClient().setPort(clientPort);
                    Globals.getInstance().getClient().start();
                } catch (NumberFormatException ex) {
                    log.error(ex.getMessage());
                    pnlSystemLog.addLog("NumberFormatException: " + ex.getMessage(), new MessageParameters(MessageParameters.messageType.system, MessageParameters.textStyle.error));
                    timer.schedule(new checkConnectionTask(), 500);
                }
                timer.schedule(new checkConnectionTask(), 2 * 1000);
            }
        }
    }

    private javax.swing.ButtonGroup buttonGroup1;

    private javax.swing.JToggleButton cmdConnect;

    private javax.swing.JToggleButton cmdDisconnect;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel pnlClientToolBar;

    private gui.inputpanel.pnlInput pnlInput;

    private gui.outputpanel.ViewersPanel pnlReceivedData;

    private gui.outputpanel.ViewersPanel pnlSentData;

    private gui.addressfields.pnlIp pnlServerAddress;

    private gui.addressfields.pnlPort pnlServerPort;

    private gui.outputpanel.LogPanel pnlSystemLog;

    private javax.swing.JSplitPane splitInputOutput;

    private javax.swing.JSplitPane splitServerClient;

    private javax.swing.JSplitPane splitServerSystem;

    protected javax.swing.event.EventListenerList CustomEventsListenerList = new javax.swing.event.EventListenerList();

    public void addListener(ClientGUIListenerIF listener) {
        CustomEventsListenerList.add(ClientGUIListenerIF.class, listener);
    }

    public void removeListener(ClientGUIListenerIF listener) {
        CustomEventsListenerList.remove(ClientGUIListenerIF.class, listener);
    }

    public void fireClientUpTimeTicker() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientGUIListenerIF.class) {
                ((ClientGUIListenerIF) listeners[i + 1]).ClientUpTimeTicker();
            }
        }
    }

    public void fireClientDisconnect() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientGUIListenerIF.class) {
                ((ClientGUIListenerIF) listeners[i + 1]).ClientDisconnect();
            }
        }
    }

    public void fireClientConnect() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientGUIListenerIF.class) {
                ((ClientGUIListenerIF) listeners[i + 1]).ClientConnect(clientPort, clientAddress);
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

    public Integer getSplitInputOutput() {
        return splitInputOutput.getDividerLocation();
    }

    public void setSplitInputOutput(Integer SplitSplitInputOutput) {
        splitInputOutput.setDividerLocation(SplitSplitInputOutput);
    }

    public String getConnectionPort() {
        return pnlServerPort.getPort();
    }

    public void setConnectionPort(String ConnectionPort) {
        pnlServerPort.setPort(ConnectionPort);
    }

    public String getServerAddress() {
        return pnlServerAddress.getIPAddress();
    }

    public void setServerAddress(String ServerAddress) {
        pnlServerAddress.setIPAddress(ServerAddress);
    }

    public Integer getSplitServerClientDividerLocation() {
        return splitServerClient.getDividerLocation();
    }

    public void setSplitServerClientDividerLocation(Integer SplitServerClientDividerLocation) {
        splitServerClient.setDividerLocation(SplitServerClientDividerLocation);
    }

    public Integer getSplitServerSystemDividerLocation() {
        return splitServerSystem.getDividerLocation();
    }

    public void setSplitServerSystemDividerLocation(Integer SplitServerSystemDividerLocation) {
        splitServerSystem.setDividerLocation(SplitServerSystemDividerLocation);
    }

    public String getTextToSend() {
        return pnlInput.getFreeTextToSend();
    }

    public void setTextToSend(String TextToSend) {
        pnlInput.setFreeTextToSend(TextToSend);
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
}
