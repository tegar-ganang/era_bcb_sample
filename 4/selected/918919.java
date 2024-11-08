package org.javasock.jssniff;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.javasock.ARPHandler;
import org.javasock.ICMPHandler;
import org.javasock.IPHandler;
import org.javasock.IPFromToFilter;
import org.javasock.OSIDataLinkDevice;
import org.javasock.TCPHandler;
import org.javasock.TCPPortFilter;
import org.javasock.UDPHandler;
import org.javasock.UDPPortFilter;
import org.javasock.jssniff.handlerdata.ARPData;
import org.javasock.jssniff.handlerdata.DeviceData;
import org.javasock.jssniff.handlerdata.HandlerData;
import org.javasock.jssniff.handlerdata.ICMPData;
import org.javasock.jssniff.handlerdata.IPData;
import org.javasock.jssniff.handlerdata.TCPData;
import org.javasock.jssniff.handlerdata.UDPData;
import org.javasock.jssniff.handlertree.ARPType;
import org.javasock.jssniff.handlertree.Device;
import org.javasock.jssniff.handlertree.ICMPType;
import org.javasock.jssniff.handlertree.IPType;
import org.javasock.jssniff.handlertree.ObjectFactory;
import org.javasock.jssniff.handlertree.TCPType;
import org.javasock.jssniff.handlertree.UDPType;
import org.javasock.windows.AirPcapDevice;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

public class SnifferDialog extends JDialog implements ActionListener {

    private boolean captureIsRunning = false;

    private int handlerID = 0;

    /**
         * The tree model; stores all Handler/Filter data
         * <p>
         *
         * @author John P. Wilson
         *
         * @version 03/16/2007
         */
    private DefaultTreeModel treeModel = null;

    private OSIDataLinkDevice[] dataLinkDevices = null;

    private JList deviceList = null;

    private JButton startCaptureButton = null;

    private JButton stopCaptureButton = null;

    private JProgressBar progressBar = null;

    private JPanel configurationPanel = null;

    private HandlerTreePanel handlerTreePanel = null;

    private JTable packetTable = null;

    private PacketLogTableModel packetTableModel = null;

    private JCheckBox performDNSLookupsCB = null;

    private JCheckBox displayWirelessInLogCB = null;

    private JCheckBox displayEthernetInLogCB = null;

    private JCheckBox displayARPInLogCB = null;

    private JCheckBox displayIPInLogCB = null;

    private JCheckBox displayICMPInLogCB = null;

    private JCheckBox displayTCPInLogCB = null;

    private JCheckBox displayUDPInLogCB = null;

    private JButton saveLogButton = null;

    private JButton clearLogButton = null;

    private JCheckBox showStringDataCheckbox = null;

    private JMenuItem openMenuItem = null;

    private JMenuItem saveMenuItem = null;

    private JMenuItem saveAsMenuItem = null;

    private JMenuItem exitMenuItem = null;

    public JCheckBoxMenuItem dockMenuItem = null;

    private HandlerTreeDialog handlerTreeDialog = null;

    private Vector<Component> componentV = null;

    private Component defaultComponent = null;

    private File currentDir = null;

    private static final boolean DEFAULT_PERFORM_DNS_LOOKUP = false;

    private static final boolean DEFAULT_DISPLAY_WIRELESS = true;

    private static final boolean DEFAULT_DISPLAY_ETHERNET = true;

    private static final boolean DEFAULT_DISPLAY_ARP = true;

    private static final boolean DEFAULT_DISPLAY_IP = true;

    private static final boolean DEFAULT_DISPLAY_ICMP = true;

    private static final boolean DEFAULT_DISPLAY_TCP = true;

    private static final boolean DEFAULT_DISPLAY_UDP = true;

    private static final boolean DEFAULT_SHOW_HEX_DATA = false;

    /**
	* constructor for <code>SnifferDialog</code>
	*/
    public SnifferDialog() {
        super((Frame) null, "JavaSock Sniffer", false);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Device");
        rootNode.setUserObject(new DeviceData());
        treeModel = new DefaultTreeModel(rootNode);
        setResizable(true);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new HandleWindowEvents());
        loadDataLinkDevices();
        addDialogComponents();
        pack();
        int x = 100, y = 100, width = 800, height = 800;
        setBounds(new Rectangle(x, y, width, height));
        defaultComponent = startCaptureButton;
        componentV = new Vector<Component>();
        componentV.add(deviceList);
        componentV.add(startCaptureButton);
        componentV.add(stopCaptureButton);
        componentV.add(performDNSLookupsCB);
        componentV.add(displayWirelessInLogCB);
        componentV.add(displayEthernetInLogCB);
        componentV.add(displayARPInLogCB);
        componentV.add(displayIPInLogCB);
        componentV.add(displayICMPInLogCB);
        componentV.add(displayTCPInLogCB);
        componentV.add(displayUDPInLogCB);
        componentV.add(saveLogButton);
        componentV.add(clearLogButton);
        componentV.add(showStringDataCheckbox);
        setFocusTraversalPolicy(new CustomTraversalPolicy(componentV, defaultComponent));
        defaultComponent.requestFocusInWindow();
        setVisible(true);
    }

    ;

    /**
	* report <code>Exception</code> to user
	*/
    public void reportException(Exception _exc, String _msg) {
        _exc.printStackTrace();
        JOptionPane.showMessageDialog(this, _exc, _msg, JOptionPane.ERROR_MESSAGE);
    }

    ;

    /**
	* load array of available <code>OSIDataLinkDevice</code>s
	*/
    public boolean loadDataLinkDevices() {
        try {
            dataLinkDevices = OSIDataLinkDevice.getDevices();
        } catch (java.io.IOException _exc) {
            reportException(_exc, "Failure loading data link devices");
            dataLinkDevices = null;
        }
        ;
        return true;
    }

    /**
	* add GUI components to <code>SnifferDialog</code>
	*/
    public void addDialogComponents() {
        addJMenu();
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        {
            configurationPanel = new JPanel(new GridBagLayout());
            configurationPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
            GridBagConstraints contentgbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0);
            contentPane.add(configurationPanel, contentgbc);
            {
                JPanel devicesPanel = new JPanel();
                devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.X_AXIS));
                devicesPanel.setBorder(BorderFactory.createTitledBorder("Available OSI Data Link Devices"));
                GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0);
                configurationPanel.add(devicesPanel, gbc);
                devicesPanel.add(Box.createRigidArea(new Dimension(5, 0)));
                JPanel outerBoxForDevicesList = new JPanel();
                outerBoxForDevicesList.setLayout(new BoxLayout(outerBoxForDevicesList, BoxLayout.Y_AXIS));
                outerBoxForDevicesList.add(Box.createRigidArea(new Dimension(0, 5)));
                deviceList = addList(outerBoxForDevicesList, dataLinkDevices, null, 450, 100);
                deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                deviceList.setSelectedIndex(0);
                outerBoxForDevicesList.add(Box.createRigidArea(new Dimension(0, 10)));
                devicesPanel.add(outerBoxForDevicesList);
                devicesPanel.add(Box.createRigidArea(new Dimension(5, 0)));
                Box boxV1 = Box.createVerticalBox();
                devicesPanel.add(boxV1);
                devicesPanel.add(Box.createRigidArea(new Dimension(5, 0)));
                boxV1.add(Box.createVerticalGlue());
                JPanel startStopPanel = new JPanel();
                startStopPanel.setLayout(new BoxLayout(startStopPanel, BoxLayout.X_AXIS));
                startCaptureButton = addButton(startStopPanel, "Start capture");
                stopCaptureButton = addButton(startStopPanel, "Stop capture");
                stopCaptureButton.setEnabled(false);
                boxV1.add(startStopPanel);
                boxV1.add(Box.createRigidArea(new Dimension(0, 5)));
                progressBar = new JProgressBar();
                progressBar.setEnabled(false);
                boxV1.add(progressBar);
                boxV1.add(Box.createVerticalGlue());
            }
            packetTableModel = new PacketLogTableModel(DEFAULT_DISPLAY_WIRELESS, DEFAULT_DISPLAY_ETHERNET, DEFAULT_DISPLAY_ARP, DEFAULT_DISPLAY_IP, DEFAULT_DISPLAY_ICMP, DEFAULT_DISPLAY_TCP, DEFAULT_DISPLAY_UDP, DEFAULT_SHOW_HEX_DATA);
            {
                handlerTreePanel = new HandlerTreePanel(treeModel, 450, 150, packetTableModel);
                handlerTreePanel.setBorder(BorderFactory.createTitledBorder("Handler/Filter Tree"));
                GridBagConstraints gbc = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0);
                configurationPanel.add(handlerTreePanel, gbc);
            }
        }
        {
            JPanel packetPanel = new JPanel();
            packetPanel.setLayout(new BoxLayout(packetPanel, BoxLayout.X_AXIS));
            packetPanel.setBorder(BorderFactory.createTitledBorder("Packet Log (double-click on row for detailed info)"));
            GridBagConstraints contentgbc = new GridBagConstraints(0, 1, 1, 1, 100, 100, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0);
            contentPane.add(packetPanel, contentgbc);
            packetPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            packetTable = addTable(packetPanel, packetTableModel, 650, 175);
            packetTableModel.setPreferredColumnSizes(packetTable);
            packetPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            Box displayOptionsBox = Box.createVerticalBox();
            packetPanel.add(displayOptionsBox);
            packetPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            Box protocolBox = Box.createVerticalBox();
            displayOptionsBox.add(protocolBox);
            protocolBox.setBorder(BorderFactory.createTitledBorder("Display"));
            PListener.setPerformDNSLookup(DEFAULT_PERFORM_DNS_LOOKUP);
            performDNSLookupsCB = addCheckBox(protocolBox, "DNS lookup", DEFAULT_PERFORM_DNS_LOOKUP, this);
            displayWirelessInLogCB = addCheckBox(protocolBox, "802.11", DEFAULT_DISPLAY_WIRELESS, this);
            displayEthernetInLogCB = addCheckBox(protocolBox, "Ethernet", DEFAULT_DISPLAY_ETHERNET, this);
            displayARPInLogCB = addCheckBox(protocolBox, "ARP", DEFAULT_DISPLAY_ARP, this);
            displayIPInLogCB = addCheckBox(protocolBox, "IP", DEFAULT_DISPLAY_IP, this);
            displayICMPInLogCB = addCheckBox(protocolBox, "ICMP", DEFAULT_DISPLAY_ICMP, this);
            displayTCPInLogCB = addCheckBox(protocolBox, "TCP", DEFAULT_DISPLAY_TCP, this);
            displayUDPInLogCB = addCheckBox(protocolBox, "UDP", DEFAULT_DISPLAY_UDP, this);
            displayOptionsBox.add(Box.createRigidArea(new Dimension(0, 5)));
            saveLogButton = addButton(displayOptionsBox, "Save Log");
            displayOptionsBox.add(Box.createRigidArea(new Dimension(0, 5)));
            clearLogButton = addButton(displayOptionsBox, "Clear Log");
            displayOptionsBox.add(Box.createRigidArea(new Dimension(0, 5)));
            showStringDataCheckbox = addCheckBox(displayOptionsBox, "data as text", !DEFAULT_SHOW_HEX_DATA, this);
        }
    }

    ;

    private void addJMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = null;
        menu = new JMenu("File");
        openMenuItem = new JMenuItem("Open Handler/Filter Tree...");
        openMenuItem.addActionListener(this);
        openMenuItem.setEnabled(true);
        menu.add(openMenuItem);
        saveAsMenuItem = new JMenuItem("Save Handler/Filter Tree As...");
        saveAsMenuItem.addActionListener(this);
        saveAsMenuItem.setEnabled(true);
        menu.add(saveAsMenuItem);
        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(this);
        exitMenuItem.setEnabled(true);
        menu.add(exitMenuItem);
        menuBar.add(menu);
        menu = new JMenu("Window");
        dockMenuItem = new JCheckBoxMenuItem("Dock Handler/Filter Tree", true);
        dockMenuItem.addActionListener(this);
        dockMenuItem.setEnabled(true);
        menu.add(dockMenuItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    /**
	* insert button into <code>SnifferDialog</code>
	* @param _container is Container for button
	* @param _name is button label
	*/
    public JButton addButton(Container _container, String _name) {
        JButton button = new JButton(_name);
        button.addActionListener(this);
        _container.add(button);
        return button;
    }

    ;

    /**
	* insert checkbox into <code>OctGenericDialog</code>
	* @param _container is Container for button
	* @param _name is button label
	* @param _selected is whether initially selected
	* @param _listener is the action listener for this JCheckBox
	* 
	* JPW 02/16/2007
	*  - make method static
	*  - add "_listener" argument
	* 
	*/
    public static JCheckBox addCheckBox(Container _container, String _name, boolean _selected, ActionListener _listener) {
        JCheckBox button = new JCheckBox(_name, _selected);
        if (_listener != null) {
            button.addActionListener(_listener);
        }
        _container.add(button);
        return button;
    }

    ;

    /**
	* insert JList into <code>SnifferDialog</code>
	* @param _container is Container for list
	* @param _objList is array of objects to add
	* @param _title is label
	*/
    JList addList(Container _container, Object[] _objList, String _title, int _width, int _height) {
        JList list;
        if (_objList == null) {
            DefaultListModel defaultListModel = new DefaultListModel();
            list = new JList(defaultListModel);
        } else {
            list = new JList(_objList);
        }
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(_width, _height));
        if (_title == null) _container.add(scrollPane); else {
            Box boxV = Box.createVerticalBox();
            _container.add(boxV);
            boxV.setBorder(BorderFactory.createTitledBorder(_title));
            boxV.add(scrollPane);
        }
        return list;
    }

    ;

    /**
	* insert JTable into <code>SnifferDialog</code>
	* @param _model is TableModel for table
	*/
    JTable addTable(Container _container, TableModel _model, int _width, int _height) {
        JTable table = new JTable(_model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        FontCellRenderer dataColRenderer = new FontCellRenderer();
        TableColumn dataCol = table.getColumnModel().getColumn(_model.getColumnCount() - 1);
        dataCol.setCellRenderer(dataColRenderer);
        table.addMouseListener(new TableMouseHandler(this));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(_width, _height));
        _container.add(scrollPane);
        return table;
    }

    ;

    public class TableMouseHandler extends MouseAdapter {

        private JDialog dialog = null;

        public TableMouseHandler(JDialog dialogI) {
            dialog = dialogI;
        }

        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                PacketData pd = packetTableModel.getDataAt(packetTable.getSelectedRow());
                new JPacketInfoDialog(dialog, false, pd.date.toString(), pd);
            }
        }
    }

    public class HandleWindowEvents extends WindowAdapter {

        public void windowClosing(WindowEvent event) {
            exitAction();
        }
    }

    /**
	* get selected <code>OSIDataLinkDevice</code>
	*/
    public OSIDataLinkDevice getSelectedDataLinkDevice() {
        return (OSIDataLinkDevice) deviceList.getSelectedValue();
    }

    ;

    private void startDataCapture() {
        if (captureIsRunning) return;
        OSIDataLinkDevice selectedDataLinkDevice = getSelectedDataLinkDevice();
        if (selectedDataLinkDevice == null) {
            JOptionPane.showMessageDialog(this, "You must select a device from the list of OSI Data Link Devices.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        if (!(rootNode.getUserObject() instanceof DeviceData)) {
            JOptionPane.showMessageDialog(this, "Unknown root node in the handler tree - it isn't a Device", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PListener listener = null;
        int tempHandlerID = 0;
        DeviceData deviceData = (DeviceData) rootNode.getUserObject();
        tempHandlerID = deviceData.getHandlerID();
        if (tempHandlerID == 0) {
            ++handlerID;
            tempHandlerID = handlerID;
            deviceData.setHandlerID(tempHandlerID);
        }
        listener = new PListener(packetTable, tempHandlerID);
        selectedDataLinkDevice.addPacketListener(listener);
        System.err.println("Data link device handler activated");
        packetTableModel.displayPacketsWithID(tempHandlerID, deviceData.getShowPackets());
        if ((deviceData.getHasFilter()) && (selectedDataLinkDevice instanceof AirPcapDevice)) {
            WirelessMACFilter wmf = new WirelessMACFilter(deviceData.getMACAddr1());
            ((AirPcapDevice) selectedDataLinkDevice).addFilter(wmf);
            System.err.println(wmf.toString());
        } else if (deviceData.getHasFilter()) {
            JOptionPane.showMessageDialog(this, "The selected device is not a wireless device.\n" + "No wireless filtering will be performed.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        for (int i = 0; i < rootNode.getChildCount(); ++i) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObject = node.getUserObject();
            if (userObject instanceof ARPData) {
                ARPData arpData = (ARPData) userObject;
                ARPHandler arpHandler = new ARPHandler(selectedDataLinkDevice);
                tempHandlerID = arpData.getHandlerID();
                if (tempHandlerID == 0) {
                    ++handlerID;
                    tempHandlerID = handlerID;
                    arpData.setHandlerID(tempHandlerID);
                }
                listener = new PListener(packetTable, tempHandlerID);
                arpHandler.addPacketListener(listener);
                System.err.println("ARP handler activated");
                packetTableModel.displayPacketsWithID(tempHandlerID, arpData.getShowPackets());
                if ((arpData.getHasFilter()) && ((arpData.getARPOpcode() != HandlerData.NO_FILTER) || (arpData.getARPProtocolType() != HandlerData.NO_FILTER) || (arpData.getARPHardwareType() != HandlerData.NO_FILTER))) {
                    ARPOpcodeProtocolHardwareFilter arpFilter = new ARPOpcodeProtocolHardwareFilter(arpData.getARPOpcode(), arpData.getARPProtocolType(), arpData.getARPHardwareType());
                    arpHandler.addFilter(arpFilter);
                    System.err.println(arpFilter.toString());
                }
            } else if (userObject instanceof IPData) {
                IPData ipData = (IPData) userObject;
                IPHandler ipHandler = new IPHandler(selectedDataLinkDevice);
                tempHandlerID = ipData.getHandlerID();
                if (tempHandlerID == 0) {
                    ++handlerID;
                    tempHandlerID = handlerID;
                    ipData.setHandlerID(tempHandlerID);
                }
                listener = new PListener(packetTable, tempHandlerID);
                ipHandler.addPacketListener(listener);
                System.err.println("IP handler activated");
                packetTableModel.displayPacketsWithID(tempHandlerID, ipData.getShowPackets());
                if (ipData.getHasFilter()) {
                    String fromAddressStr = ipData.getIPFromHost();
                    String toAddressStr = ipData.getIPToHost();
                    InetAddress fromAddress = IPData.getInetAddress(fromAddressStr);
                    InetAddress toAddress = IPData.getInetAddress(toAddressStr);
                    if ((fromAddress != null) || (toAddress != null)) {
                        IPFromToFilter ipFromToFilter = new IPFromToFilter(fromAddress, toAddress);
                        ipHandler.addFilter(ipFromToFilter);
                        System.err.println("IP filter: from " + fromAddressStr + " to " + toAddressStr);
                    }
                }
                if (node.getChildCount() > 0) {
                    for (int j = 0; j < node.getChildCount(); ++j) {
                        DefaultMutableTreeNode subnode = (DefaultMutableTreeNode) node.getChildAt(j);
                        Object subnodeUserObject = subnode.getUserObject();
                        if (subnodeUserObject instanceof ICMPData) {
                            ICMPData icmpData = (ICMPData) subnodeUserObject;
                            ICMPHandler icmpHandler = new ICMPHandler(ipHandler);
                            tempHandlerID = icmpData.getHandlerID();
                            if (tempHandlerID == 0) {
                                ++handlerID;
                                tempHandlerID = handlerID;
                                icmpData.setHandlerID(tempHandlerID);
                            }
                            listener.setICMPHandlerID(tempHandlerID);
                            icmpHandler.addPacketListener(listener);
                            System.err.println("\tICMP handler activated");
                            packetTableModel.displayPacketsWithID(tempHandlerID, icmpData.getShowPackets());
                            if ((icmpData.getHasFilter()) && ((icmpData.getICMPType() != HandlerData.NO_FILTER) || (icmpData.getICMPCode() != HandlerData.NO_FILTER))) {
                                ICMPTypeCodeFilter icmpFilter = new ICMPTypeCodeFilter(icmpData.getICMPType(), icmpData.getICMPCode());
                                icmpHandler.addFilter(icmpFilter);
                                System.err.println("\t" + icmpFilter.toString());
                            }
                        } else if (subnodeUserObject instanceof TCPData) {
                            TCPData tcpData = (TCPData) subnodeUserObject;
                            TCPHandler tcpHandler = new TCPHandler(ipHandler);
                            tempHandlerID = tcpData.getHandlerID();
                            if (tempHandlerID == 0) {
                                ++handlerID;
                                tempHandlerID = handlerID;
                                tcpData.setHandlerID(tempHandlerID);
                            }
                            listener.setTCPHandlerID(tempHandlerID);
                            tcpHandler.addPacketListener(listener);
                            System.err.println("\tTCP handler activated");
                            packetTableModel.displayPacketsWithID(tempHandlerID, tcpData.getShowPackets());
                            if ((tcpData.getHasFilter()) && ((tcpData.getTCPFromPort() != TCPPortFilter.UNSPECIFIED_PORT) || (tcpData.getTCPToPort() != TCPPortFilter.UNSPECIFIED_PORT))) {
                                TCPPortFilter tcpFilter = new TCPPortFilter(tcpData.getTCPFromPort(), tcpData.getTCPToPort());
                                tcpHandler.addFilter(tcpFilter);
                                System.err.println("\tTCP filter: from port " + tcpData.getTCPFromPort() + " to port " + tcpData.getTCPToPort());
                            }
                        } else if (subnodeUserObject instanceof UDPData) {
                            UDPData udpData = (UDPData) subnodeUserObject;
                            UDPHandler udpHandler = new UDPHandler(ipHandler);
                            tempHandlerID = udpData.getHandlerID();
                            if (tempHandlerID == 0) {
                                ++handlerID;
                                tempHandlerID = handlerID;
                                udpData.setHandlerID(tempHandlerID);
                            }
                            listener.setUDPHandlerID(tempHandlerID);
                            udpHandler.addPacketListener(listener);
                            System.err.println("\tUDP handler activated");
                            packetTableModel.displayPacketsWithID(tempHandlerID, udpData.getShowPackets());
                            if ((udpData.getHasFilter()) && ((udpData.getUDPFromPort() != UDPPortFilter.UNSPECIFIED_PORT) || (udpData.getUDPToPort() != UDPPortFilter.UNSPECIFIED_PORT))) {
                                UDPPortFilter udpFilter = new UDPPortFilter(udpData.getUDPFromPort(), udpData.getUDPToPort());
                                udpHandler.addFilter(udpFilter);
                                System.err.println("\tUDP filter: from port " + udpData.getUDPFromPort() + " to port " + udpData.getUDPToPort());
                            }
                        }
                    }
                }
            }
        }
        try {
            selectedDataLinkDevice.startCapture();
        } catch (java.io.IOException _exc) {
            _exc.printStackTrace();
            System.err.println();
            System.err.println("Failed to start capture");
            System.err.println();
            return;
        }
        System.err.println("Capture started");
        captureIsRunning = true;
        progressBar.setEnabled(captureIsRunning);
        progressBar.setIndeterminate(captureIsRunning);
        enableCaptureComponents(captureIsRunning);
    }

    public void stopDataCapture() {
        if (!captureIsRunning) return;
        OSIDataLinkDevice selectedDataLinkDevice = getSelectedDataLinkDevice();
        if (selectedDataLinkDevice == null) return;
        selectedDataLinkDevice.stopCapture();
        System.err.println("Capture stopped");
        captureIsRunning = false;
        refreshDataLinkDevices();
        progressBar.setEnabled(captureIsRunning);
        progressBar.setIndeterminate(captureIsRunning);
        enableCaptureComponents(captureIsRunning);
    }

    public void refreshDataLinkDevices() {
        int oldSelectionIndex = deviceList.getSelectedIndex();
        loadDataLinkDevices();
        deviceList.setListData(dataLinkDevices);
        deviceList.setSelectedIndex(oldSelectionIndex);
    }

    /**
	* enable/disable dialog elements based on whether capture is currently active
	* @param _captureIsOn is whether currently capturing data
	*/
    public void enableCaptureComponents(boolean _captureIsOn) {
        deviceList.setEnabled(!_captureIsOn);
        startCaptureButton.setEnabled(!_captureIsOn);
        stopCaptureButton.setEnabled(_captureIsOn);
        saveLogButton.setEnabled(!_captureIsOn);
        clearLogButton.setEnabled(!_captureIsOn);
    }

    ;

    /**
	* save log data to a text file
	*/
    public void saveLog() {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String filename = chooser.getSelectedFile().getPath();
            try {
                FileWriter writer = new FileWriter(filename);
                PrintWriter out = new PrintWriter(writer);
                packetTableModel.saveLog(out);
                writer.flush();
                writer.close();
            } catch (java.io.IOException _exc) {
                reportException(_exc, "Error saving log");
            }
            ;
        }
    }

    ;

    /**
	* process dialog action events
	* @param _evt is event
	*/
    public void actionPerformed(ActionEvent _evt) {
        Object source = _evt.getSource();
        if (source == null) return;
        if (source == startCaptureButton) {
            if (!captureIsRunning) startDataCapture();
        } else if (source == stopCaptureButton) {
            if (captureIsRunning) stopDataCapture();
        } else if (source == saveLogButton) {
            saveLog();
        } else if (source == clearLogButton) {
            packetTableModel.clearLog();
        } else if (source == showStringDataCheckbox) {
            packetTableModel.setShowHexData(!showStringDataCheckbox.isSelected());
        } else if (source == performDNSLookupsCB) {
            PListener.setPerformDNSLookup(performDNSLookupsCB.isSelected());
        } else if (source == displayWirelessInLogCB) {
            packetTableModel.displayWirelessData(displayWirelessInLogCB.isSelected());
        } else if (source == displayEthernetInLogCB) {
            packetTableModel.displayEthernetData(displayEthernetInLogCB.isSelected());
        } else if (source == displayARPInLogCB) {
            packetTableModel.displayARPData(displayARPInLogCB.isSelected());
        } else if (source == displayIPInLogCB) {
            packetTableModel.displayIPData(displayIPInLogCB.isSelected());
        } else if (source == displayICMPInLogCB) {
            packetTableModel.displayICMPData(displayICMPInLogCB.isSelected());
        } else if (source == displayTCPInLogCB) {
            packetTableModel.displayTCPData(displayTCPInLogCB.isSelected());
        } else if (source == displayUDPInLogCB) {
            packetTableModel.displayUDPData(displayUDPInLogCB.isSelected());
        } else if (source == openMenuItem) {
            readHandlerTreeFile();
        } else if (source == saveMenuItem) {
            System.err.println("User hit Save");
        } else if (source == saveAsMenuItem) {
            writeHandlerTreeFile();
        } else if (source == exitMenuItem) {
            exitAction();
        } else if (source == dockMenuItem) {
            if (dockMenuItem.isSelected()) {
                handlerTreeDialog.setVisible(false);
                GridBagConstraints gbc = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0);
                configurationPanel.add(handlerTreePanel, gbc);
            } else {
                configurationPanel.remove(handlerTreePanel);
                handlerTreeDialog = new HandlerTreeDialog(this, handlerTreePanel);
                handlerTreeDialog.setVisible(true);
            }
            validate();
        }
    }

    ;

    private void readHandlerTreeFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if ((currentDir != null) && (currentDir.exists())) {
            fileChooser.setCurrentDirectory(currentDir);
        }
        int returnVal = fileChooser.showOpenDialog(this);
        currentDir = fileChooser.getCurrentDirectory();
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, new String("Selected file, " + file.getPath() + ", doesn't exist."), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (!file.canRead()) {
            JOptionPane.showMessageDialog(this, new String("Unable to read the selected file, " + file.getPath()), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String fileName = "";
        try {
            fileName = file.getCanonicalPath();
        } catch (IOException ioe) {
            fileName = file.getAbsolutePath();
        }
        System.err.println("Read handler/filter tree from file " + fileName);
        DefaultTreeModel newTreeModel = null;
        DefaultMutableTreeNode newRootNode = null;
        DefaultMutableTreeNode node = null;
        try {
            JAXBContext jc = JAXBContext.newInstance("org.javasock.jssniff.handlertree");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            Device device = (Device) unmarshaller.unmarshal(fileChooser.getSelectedFile());
            newRootNode = new DefaultMutableTreeNode("Device");
            newRootNode.setUserObject(new DeviceData(device));
            newTreeModel = new DefaultTreeModel(newRootNode);
            List handlers = device.getHandlerType();
            if ((handlers != null) && (!handlers.isEmpty())) {
                for (int i = 0; i < handlers.size(); ++i) {
                    if (handlers.get(i) instanceof ARPType) {
                        node = new DefaultMutableTreeNode("ARP");
                        node.setUserObject(new ARPData((ARPType) handlers.get(i)));
                        newTreeModel.insertNodeInto(node, newRootNode, newRootNode.getChildCount());
                    }
                }
                for (int i = 0; i < handlers.size(); ++i) {
                    if (handlers.get(i) instanceof IPType) {
                        DefaultMutableTreeNode ipNode = new DefaultMutableTreeNode("IP");
                        IPType ip = (IPType) handlers.get(i);
                        ipNode.setUserObject(new IPData(ip));
                        newTreeModel.insertNodeInto(ipNode, newRootNode, newRootNode.getChildCount());
                        ICMPType icmp = ip.getICMP();
                        TCPType tcp = ip.getTCP();
                        UDPType udp = ip.getUDP();
                        if (icmp != null) {
                            node = new DefaultMutableTreeNode("ICMP");
                            node.setUserObject(new ICMPData(icmp));
                            newTreeModel.insertNodeInto(node, ipNode, ipNode.getChildCount());
                        }
                        if (tcp != null) {
                            node = new DefaultMutableTreeNode("TCP");
                            node.setUserObject(new TCPData(tcp));
                            newTreeModel.insertNodeInto(node, ipNode, ipNode.getChildCount());
                        }
                        if (udp != null) {
                            node = new DefaultMutableTreeNode("UDP");
                            node.setUserObject(new UDPData(udp));
                            newTreeModel.insertNodeInto(node, ipNode, ipNode.getChildCount());
                        }
                    }
                }
            }
        } catch (JAXBException e) {
            System.err.println("Exception caught trying to read handler tree file:\n" + e);
            JOptionPane.showMessageDialog(this, new String("Error caught reading handler tree file:\n" + e), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        treeModel = newTreeModel;
        this.handlerTreePanel.setTreeModel(treeModel);
    }

    private void writeHandlerTreeFile() {
        JFileChooser fileChooser = new JFileChooser();
        if ((currentDir != null) && (currentDir.exists())) {
            fileChooser.setCurrentDirectory(currentDir);
        }
        int returnVal = fileChooser.showSaveDialog(this);
        currentDir = fileChooser.getCurrentDirectory();
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (file.exists()) {
            int userChoice = JOptionPane.showConfirmDialog(this, "The selected file already exists.  Overwrite it?", "Confirm overwrite", JOptionPane.YES_NO_OPTION);
            if (userChoice == JOptionPane.NO_OPTION) {
                return;
            }
        }
        String fileName = "";
        try {
            fileName = file.getCanonicalPath();
        } catch (IOException ioe) {
            fileName = file.getAbsolutePath();
        }
        System.err.println("Write handler/filter tree to file " + fileName);
        try {
            JAXBContext jc = JAXBContext.newInstance("org.javasock.jssniff.handlertree");
            ObjectFactory factory = new ObjectFactory();
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
            if (!(rootNode.getUserObject() instanceof DeviceData)) {
                throw new Exception("Unknown root node; should be a Device");
            }
            DeviceData deviceData = (DeviceData) rootNode.getUserObject();
            Device device = factory.createDevice();
            deviceData.initializeDevice(device);
            for (int i = 0; i < rootNode.getChildCount(); ++i) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                Object userObject = node.getUserObject();
                if (userObject instanceof ARPData) {
                    ARPData arpData = (ARPData) userObject;
                    ARPType arpType = factory.createARPType();
                    arpData.initializeARPType(arpType);
                    device.getHandlerType().add(arpType);
                } else if (userObject instanceof IPData) {
                    IPData ipData = (IPData) userObject;
                    IPType ipType = factory.createIPType();
                    ipData.initializeIPType(ipType);
                    if (node.getChildCount() > 0) {
                        for (int j = 0; j < node.getChildCount(); ++j) {
                            DefaultMutableTreeNode subnode = (DefaultMutableTreeNode) node.getChildAt(j);
                            Object subnodeUserObject = subnode.getUserObject();
                            if (subnodeUserObject instanceof ICMPData) {
                                ICMPData icmpData = (ICMPData) subnodeUserObject;
                                ICMPType icmpType = factory.createICMPType();
                                icmpData.initializeICMPType(icmpType);
                                ipType.setICMP(icmpType);
                            } else if (subnodeUserObject instanceof TCPData) {
                                TCPData tcpData = (TCPData) subnodeUserObject;
                                TCPType tcpType = factory.createTCPType();
                                tcpData.initializeTCPType(tcpType);
                                ipType.setTCP(tcpType);
                            } else if (subnodeUserObject instanceof UDPData) {
                                UDPData udpData = (UDPData) subnodeUserObject;
                                UDPType udpType = factory.createUDPType();
                                udpData.initializeUDPType(udpType);
                                ipType.setUDP(udpType);
                            }
                        }
                    }
                    device.getHandlerType().add(ipType);
                }
            }
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            FileOutputStream outStream = new FileOutputStream(file);
            marshaller.marshal(device, outStream);
        } catch (PropertyException e) {
            System.err.println("Exception caught trying to write handler tree file:\n" + e);
            JOptionPane.showMessageDialog(this, new String("Error caught writing handler tree file:\n" + e), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (JAXBException e) {
            System.err.println("Exception caught trying to write handler tree file:\n" + e);
            JOptionPane.showMessageDialog(this, new String("Error caught writing handler tree file:\n" + e), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception e) {
            System.err.println("Exception caught trying to write handler tree file:\n" + e);
            JOptionPane.showMessageDialog(this, new String("Error caught writing handler tree file:\n" + e), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void exitAction() {
        if (captureIsRunning) stopDataCapture();
        setVisible(false);
        System.err.println("");
        System.err.println("Exiting JSSniff");
        System.err.println("");
        System.exit(0);
    }

    public JCheckBoxMenuItem getDockMenuItem() {
        return dockMenuItem;
    }

    public class FontCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable tableI, Object valueI, boolean isSelectedI, boolean hasFocusI, int rowI, int columnI) {
            JLabel label = new JLabel();
            Font defaultTableFont = tableI.getFont();
            Font newTableFont = new Font("Monospaced", Font.PLAIN, defaultTableFont.getSize());
            label.setFont(newTableFont);
            label.setText((String) valueI);
            return label;
        }
    }
}
