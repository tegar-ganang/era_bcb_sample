package org.jivesoftware.smackx.debugger;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The EnhancedDebugger is a debugger that allows to debug sent, received and interpreted messages
 * but also provides the ability to send ad-hoc messages composed by the user.<p>
 * <p/>
 * A new EnhancedDebugger will be created for each connection to debug. All the EnhancedDebuggers
 * will be shown in the same debug window provided by the class EnhancedDebuggerWindow.
 *
 * @author Gaston Dombiak
 */
public class EnhancedDebugger implements SmackDebugger {

    private static final String NEWLINE = "\n";

    private static ImageIcon packetReceivedIcon;

    private static ImageIcon packetSentIcon;

    private static ImageIcon presencePacketIcon;

    private static ImageIcon iqPacketIcon;

    private static ImageIcon messagePacketIcon;

    private static ImageIcon unknownPacketTypeIcon;

    {
        URL url;
        url = Thread.currentThread().getContextClassLoader().getResource("images/nav_left_blue.png");
        if (url != null) {
            packetReceivedIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/nav_right_red.png");
        if (url != null) {
            packetSentIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/photo_portrait.png");
        if (url != null) {
            presencePacketIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/question_and_answer.png");
        if (url != null) {
            iqPacketIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/message.png");
        if (url != null) {
            messagePacketIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/unknown.png");
        if (url != null) {
            unknownPacketTypeIcon = new ImageIcon(url);
        }
    }

    private DefaultTableModel messagesTable = null;

    private JTextArea messageTextArea = null;

    private JFormattedTextField userField = null;

    private JFormattedTextField statusField = null;

    private XMPPConnection connection = null;

    private PacketListener packetReaderListener = null;

    private PacketListener packetWriterListener = null;

    private ConnectionListener connListener = null;

    private Writer writer;

    private Reader reader;

    private ReaderListener readerListener;

    private WriterListener writerListener;

    private Date creationTime = new Date();

    private DefaultTableModel statisticsTable = null;

    private int sentPackets = 0;

    private int receivedPackets = 0;

    private int sentIQPackets = 0;

    private int receivedIQPackets = 0;

    private int sentMessagePackets = 0;

    private int receivedMessagePackets = 0;

    private int sentPresencePackets = 0;

    private int receivedPresencePackets = 0;

    private int sentOtherPackets = 0;

    private int receivedOtherPackets = 0;

    JTabbedPane tabbedPane;

    public EnhancedDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        this.connection = connection;
        this.writer = writer;
        this.reader = reader;
        createDebug();
        EnhancedDebuggerWindow.addDebugger(this);
    }

    /**
     * Creates the debug process, which is a GUI window that displays XML traffic.
     */
    private void createDebug() {
        tabbedPane = new JTabbedPane();
        addBasicPanels();
        addAdhocPacketPanel();
        addInformationPanel();
        packetReaderListener = new PacketListener() {

            SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss aaa");

            public void processPacket(final Packet packet) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        addReadPacketToTable(dateFormatter, packet);
                    }
                });
            }
        };
        packetWriterListener = new PacketListener() {

            SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss aaa");

            public void processPacket(final Packet packet) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        addSentPacketToTable(dateFormatter, packet);
                    }
                });
            }
        };
        connListener = new ConnectionListener() {

            public void connectionClosed() {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        statusField.setValue("Closed");
                        EnhancedDebuggerWindow.connectionClosed(EnhancedDebugger.this);
                    }
                });
            }

            public void connectionClosedOnError(final Exception e) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        statusField.setValue("Closed due to an exception");
                        EnhancedDebuggerWindow.connectionClosedOnError(EnhancedDebugger.this, e);
                    }
                });
            }

            public void reconnectingIn(final int seconds) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        statusField.setValue("Attempt to reconnect in " + seconds + " seconds");
                    }
                });
            }

            public void reconnectionSuccessful() {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        statusField.setValue("Reconnection stablished");
                        EnhancedDebuggerWindow.connectionEstablished(EnhancedDebugger.this);
                    }
                });
            }

            public void reconnectionFailed(Exception e) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        statusField.setValue("Reconnection failed");
                    }
                });
            }
        };
    }

    private void addBasicPanels() {
        JSplitPane allPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        allPane.setOneTouchExpandable(true);
        messagesTable = new DefaultTableModel(new Object[] { "Hide", "Timestamp", "", "", "Message", "Id", "Type", "To", "From" }, 0) {

            public boolean isCellEditable(int rowIndex, int mColIndex) {
                return false;
            }

            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 2 || columnIndex == 3) {
                    return Icon.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        JTable table = new JTable(messagesTable);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getTableHeader().getColumnModel().getColumn(0).setMaxWidth(0);
        table.getTableHeader().getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(30);
        table.getColumnModel().getColumn(3).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(30);
        table.getColumnModel().getColumn(5).setMaxWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(55);
        table.getColumnModel().getColumn(6).setMaxWidth(200);
        table.getColumnModel().getColumn(6).setPreferredWidth(50);
        table.getColumnModel().getColumn(7).setMaxWidth(300);
        table.getColumnModel().getColumn(7).setPreferredWidth(90);
        table.getColumnModel().getColumn(8).setMaxWidth(300);
        table.getColumnModel().getColumn(8).setPreferredWidth(90);
        SelectionListener selectionListener = new SelectionListener(table);
        table.getSelectionModel().addListSelectionListener(selectionListener);
        table.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
        allPane.setTopComponent(new JScrollPane(table));
        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(messageTextArea.getText()), null);
            }
        });
        menu.add(menuItem1);
        messageTextArea.addMouseListener(new PopupListener(menu));
        allPane.setBottomComponent(new JScrollPane(messageTextArea));
        allPane.setDividerLocation(150);
        tabbedPane.add("All Packets", allPane);
        tabbedPane.setToolTipTextAt(0, "Sent and received packets processed by Smack");
        final JTextArea sentText = new JTextArea();
        sentText.setWrapStyleWord(true);
        sentText.setLineWrap(true);
        sentText.setEditable(false);
        sentText.setForeground(new Color(112, 3, 3));
        tabbedPane.add("Raw Sent Packets", new JScrollPane(sentText));
        tabbedPane.setToolTipTextAt(1, "Raw text of the sent packets");
        menu = new JPopupMenu();
        menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(sentText.getText()), null);
            }
        });
        JMenuItem menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sentText.setText("");
            }
        });
        sentText.addMouseListener(new PopupListener(menu));
        menu.add(menuItem1);
        menu.add(menuItem2);
        final JTextArea receivedText = new JTextArea();
        receivedText.setWrapStyleWord(true);
        receivedText.setLineWrap(true);
        receivedText.setEditable(false);
        receivedText.setForeground(new Color(6, 76, 133));
        tabbedPane.add("Raw Received Packets", new JScrollPane(receivedText));
        tabbedPane.setToolTipTextAt(2, "Raw text of the received packets before Smack process them");
        menu = new JPopupMenu();
        menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(receivedText.getText()), null);
            }
        });
        menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                receivedText.setText("");
            }
        });
        receivedText.addMouseListener(new PopupListener(menu));
        menu.add(menuItem1);
        menu.add(menuItem2);
        ObservableReader debugReader = new ObservableReader(reader);
        readerListener = new ReaderListener() {

            public void read(final String str) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (EnhancedDebuggerWindow.PERSISTED_DEBUGGER && !EnhancedDebuggerWindow.getInstance().isVisible()) {
                            return;
                        }
                        int index = str.lastIndexOf(">");
                        if (index != -1) {
                            if (receivedText.getLineCount() >= EnhancedDebuggerWindow.MAX_TABLE_ROWS) {
                                try {
                                    receivedText.replaceRange("", 0, receivedText.getLineEndOffset(0));
                                } catch (BadLocationException e) {
                                    e.printStackTrace();
                                }
                            }
                            receivedText.append(str.substring(0, index + 1));
                            receivedText.append(NEWLINE);
                            if (str.length() > index) {
                                receivedText.append(str.substring(index + 1));
                            }
                        } else {
                            receivedText.append(str);
                        }
                    }
                });
            }
        };
        debugReader.addReaderListener(readerListener);
        ObservableWriter debugWriter = new ObservableWriter(writer);
        writerListener = new WriterListener() {

            public void write(final String str) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (EnhancedDebuggerWindow.PERSISTED_DEBUGGER && !EnhancedDebuggerWindow.getInstance().isVisible()) {
                            return;
                        }
                        if (sentText.getLineCount() >= EnhancedDebuggerWindow.MAX_TABLE_ROWS) {
                            try {
                                sentText.replaceRange("", 0, sentText.getLineEndOffset(0));
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                        }
                        sentText.append(str);
                        if (str.endsWith(">")) {
                            sentText.append(NEWLINE);
                        }
                    }
                });
            }
        };
        debugWriter.addWriterListener(writerListener);
        reader = debugReader;
        writer = debugWriter;
    }

    private void addAdhocPacketPanel() {
        final JTextArea adhocMessages = new JTextArea();
        adhocMessages.setEditable(true);
        adhocMessages.setForeground(new Color(1, 94, 35));
        tabbedPane.add("Ad-hoc message", new JScrollPane(adhocMessages));
        tabbedPane.setToolTipTextAt(3, "Panel that allows you to send adhoc packets");
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Message");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText("<message to=\"\" id=\"" + StringUtils.randomString(5) + "-X\"><body></body></message>");
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("IQ Get");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText("<iq type=\"get\" to=\"\" id=\"" + StringUtils.randomString(5) + "-X\"><query xmlns=\"\"></query></iq>");
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("IQ Set");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText("<iq type=\"set\" to=\"\" id=\"" + StringUtils.randomString(5) + "-X\"><query xmlns=\"\"></query></iq>");
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Presence");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText("<presence to=\"\" id=\"" + StringUtils.randomString(5) + "-X\"/>");
            }
        });
        menu.add(menuItem);
        menu.addSeparator();
        menuItem = new JMenuItem("Send");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!"".equals(adhocMessages.getText())) {
                    AdHocPacket packetToSend = new AdHocPacket(adhocMessages.getText());
                    connection.sendPacket(packetToSend);
                }
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Clear");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText(null);
            }
        });
        menu.add(menuItem);
        adhocMessages.addMouseListener(new PopupListener(menu));
    }

    private void addInformationPanel() {
        JPanel informationPanel = new JPanel();
        informationPanel.setLayout(new BorderLayout());
        JPanel connPanel = new JPanel();
        connPanel.setLayout(new GridBagLayout());
        connPanel.setBorder(BorderFactory.createTitledBorder("Connection information"));
        JLabel label = new JLabel("Host: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        JFormattedTextField field = new JFormattedTextField(connection.getServiceName());
        field.setMinimumSize(new java.awt.Dimension(150, 20));
        field.setMaximumSize(new java.awt.Dimension(150, 20));
        field.setEditable(false);
        field.setBorder(null);
        connPanel.add(field, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        label = new JLabel("Port: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        field = new JFormattedTextField(connection.getPort());
        field.setMinimumSize(new java.awt.Dimension(150, 20));
        field.setMaximumSize(new java.awt.Dimension(150, 20));
        field.setEditable(false);
        field.setBorder(null);
        connPanel.add(field, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        label = new JLabel("User: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        userField = new JFormattedTextField();
        userField.setMinimumSize(new java.awt.Dimension(150, 20));
        userField.setMaximumSize(new java.awt.Dimension(150, 20));
        userField.setEditable(false);
        userField.setBorder(null);
        connPanel.add(userField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        label = new JLabel("Creation time: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        field = new JFormattedTextField(new SimpleDateFormat("yyyy.MM.dd hh:mm:ss aaa"));
        field.setMinimumSize(new java.awt.Dimension(150, 20));
        field.setMaximumSize(new java.awt.Dimension(150, 20));
        field.setValue(creationTime);
        field.setEditable(false);
        field.setBorder(null);
        connPanel.add(field, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        label = new JLabel("Status: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(label, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        statusField = new JFormattedTextField();
        statusField.setMinimumSize(new java.awt.Dimension(150, 20));
        statusField.setMaximumSize(new java.awt.Dimension(150, 20));
        statusField.setValue("Active");
        statusField.setEditable(false);
        statusField.setBorder(null);
        connPanel.add(statusField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        informationPanel.add(connPanel, BorderLayout.NORTH);
        JPanel packetsPanel = new JPanel();
        packetsPanel.setLayout(new GridLayout(1, 1));
        packetsPanel.setBorder(BorderFactory.createTitledBorder("Transmitted Packets"));
        statisticsTable = new DefaultTableModel(new Object[][] { { "IQ", 0, 0 }, { "Message", 0, 0 }, { "Presence", 0, 0 }, { "Other", 0, 0 }, { "Total", 0, 0 } }, new Object[] { "Type", "Received", "Sent" }) {

            public boolean isCellEditable(int rowIndex, int mColIndex) {
                return false;
            }
        };
        JTable table = new JTable(statisticsTable);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        packetsPanel.add(new JScrollPane(table));
        informationPanel.add(packetsPanel, BorderLayout.CENTER);
        tabbedPane.add("Information", new JScrollPane(informationPanel));
        tabbedPane.setToolTipTextAt(4, "Information and statistics about the debugged connection");
    }

    public Reader newConnectionReader(Reader newReader) {
        ((ObservableReader) reader).removeReaderListener(readerListener);
        ObservableReader debugReader = new ObservableReader(newReader);
        debugReader.addReaderListener(readerListener);
        reader = debugReader;
        return reader;
    }

    public Writer newConnectionWriter(Writer newWriter) {
        ((ObservableWriter) writer).removeWriterListener(writerListener);
        ObservableWriter debugWriter = new ObservableWriter(newWriter);
        debugWriter.addWriterListener(writerListener);
        writer = debugWriter;
        return writer;
    }

    public void userHasLogged(final String user) {
        final EnhancedDebugger debugger = this;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                userField.setText(user);
                EnhancedDebuggerWindow.userHasLogged(debugger, user);
                connection.addConnectionListener(connListener);
            }
        });
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public PacketListener getReaderListener() {
        return packetReaderListener;
    }

    public PacketListener getWriterListener() {
        return packetWriterListener;
    }

    /**
     * Updates the statistics table
     */
    private void updateStatistics() {
        statisticsTable.setValueAt(new Integer(receivedIQPackets), 0, 1);
        statisticsTable.setValueAt(new Integer(sentIQPackets), 0, 2);
        statisticsTable.setValueAt(new Integer(receivedMessagePackets), 1, 1);
        statisticsTable.setValueAt(new Integer(sentMessagePackets), 1, 2);
        statisticsTable.setValueAt(new Integer(receivedPresencePackets), 2, 1);
        statisticsTable.setValueAt(new Integer(sentPresencePackets), 2, 2);
        statisticsTable.setValueAt(new Integer(receivedOtherPackets), 3, 1);
        statisticsTable.setValueAt(new Integer(sentOtherPackets), 3, 2);
        statisticsTable.setValueAt(new Integer(receivedPackets), 4, 1);
        statisticsTable.setValueAt(new Integer(sentPackets), 4, 2);
    }

    /**
     * Adds the received packet detail to the messages table.
     *
     * @param dateFormatter the SimpleDateFormat to use to format Dates
     * @param packet        the read packet to add to the table
     */
    private void addReadPacketToTable(final SimpleDateFormat dateFormatter, final Packet packet) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                String messageType;
                String from = packet.getFrom();
                String type = "";
                Icon packetTypeIcon;
                receivedPackets++;
                if (packet instanceof IQ) {
                    packetTypeIcon = iqPacketIcon;
                    messageType = "IQ Received (class=" + packet.getClass().getName() + ")";
                    type = ((IQ) packet).getType().toString();
                    receivedIQPackets++;
                } else if (packet instanceof Message) {
                    packetTypeIcon = messagePacketIcon;
                    messageType = "Message Received";
                    type = ((Message) packet).getType().toString();
                    receivedMessagePackets++;
                } else if (packet instanceof Presence) {
                    packetTypeIcon = presencePacketIcon;
                    messageType = "Presence Received";
                    type = ((Presence) packet).getType().toString();
                    receivedPresencePackets++;
                } else {
                    packetTypeIcon = unknownPacketTypeIcon;
                    messageType = packet.getClass().getName() + " Received";
                    receivedOtherPackets++;
                }
                if (EnhancedDebuggerWindow.MAX_TABLE_ROWS > 0 && messagesTable.getRowCount() >= EnhancedDebuggerWindow.MAX_TABLE_ROWS) {
                    messagesTable.removeRow(0);
                }
                messagesTable.addRow(new Object[] { formatXML(packet.toXML()), dateFormatter.format(new Date()), packetReceivedIcon, packetTypeIcon, messageType, packet.getPacketID(), type, "", from });
                updateStatistics();
            }
        });
    }

    /**
     * Adds the sent packet detail to the messages table.
     *
     * @param dateFormatter the SimpleDateFormat to use to format Dates
     * @param packet        the sent packet to add to the table
     */
    private void addSentPacketToTable(final SimpleDateFormat dateFormatter, final Packet packet) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                String messageType;
                String to = packet.getTo();
                String type = "";
                Icon packetTypeIcon;
                sentPackets++;
                if (packet instanceof IQ) {
                    packetTypeIcon = iqPacketIcon;
                    messageType = "IQ Sent (class=" + packet.getClass().getName() + ")";
                    type = ((IQ) packet).getType().toString();
                    sentIQPackets++;
                } else if (packet instanceof Message) {
                    packetTypeIcon = messagePacketIcon;
                    messageType = "Message Sent";
                    type = ((Message) packet).getType().toString();
                    sentMessagePackets++;
                } else if (packet instanceof Presence) {
                    packetTypeIcon = presencePacketIcon;
                    messageType = "Presence Sent";
                    type = ((Presence) packet).getType().toString();
                    sentPresencePackets++;
                } else {
                    packetTypeIcon = unknownPacketTypeIcon;
                    messageType = packet.getClass().getName() + " Sent";
                    sentOtherPackets++;
                }
                if (EnhancedDebuggerWindow.MAX_TABLE_ROWS > 0 && messagesTable.getRowCount() >= EnhancedDebuggerWindow.MAX_TABLE_ROWS) {
                    messagesTable.removeRow(0);
                }
                messagesTable.addRow(new Object[] { formatXML(packet.toXML()), dateFormatter.format(new Date()), packetSentIcon, packetTypeIcon, messageType, packet.getPacketID(), type, to, "" });
                updateStatistics();
            }
        });
    }

    private String formatXML(String str) {
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            try {
                tFactory.setAttribute("indent-number", 2);
            } catch (IllegalArgumentException e) {
            }
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamSource source = new StreamSource(new StringReader(str));
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);
            return sw.toString();
        } catch (TransformerConfigurationException tce) {
            System.out.println("\n** Transformer Factory error");
            System.out.println("   " + tce.getMessage());
            Throwable x = tce;
            if (tce.getException() != null) x = tce.getException();
            x.printStackTrace();
        } catch (TransformerException te) {
            System.out.println("\n** Transformation error");
            System.out.println("   " + te.getMessage());
            Throwable x = te;
            if (te.getException() != null) x = te.getException();
            x.printStackTrace();
        }
        return str;
    }

    /**
     * Returns true if the debugger's connection with the server is up and running.
     *
     * @return true if the connection with the server is active.
     */
    boolean isConnectionActive() {
        return connection.isConnected();
    }

    /**
     * Stops debugging the connection. Removes any listener on the connection.
     */
    void cancel() {
        connection.removeConnectionListener(connListener);
        connection.removePacketListener(packetReaderListener);
        connection.removePacketWriterListener(packetWriterListener);
        ((ObservableReader) reader).removeReaderListener(readerListener);
        ((ObservableWriter) writer).removeWriterListener(writerListener);
        messagesTable = null;
    }

    /**
     * An ad-hoc packet is like any regular packet but with the exception that it's intention is
     * to be used only <b>to send packets</b>.<p>
     * <p/>
     * The whole text to send must be passed to the constructor. This implies that the client of
     * this class is responsible for sending a valid text to the constructor.
     */
    private class AdHocPacket extends Packet {

        private String text;

        /**
         * Create a new AdHocPacket with the text to send. The passed text must be a valid text to
         * send to the server, no validation will be done on the passed text.
         *
         * @param text the whole text of the packet to send
         */
        public AdHocPacket(String text) {
            this.text = text;
        }

        public String toXML() {
            return text;
        }
    }

    /**
     * Listens for debug window popup dialog events.
     */
    private class PopupListener extends MouseAdapter {

        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class SelectionListener implements ListSelectionListener {

        JTable table;

        SelectionListener(JTable table) {
            this.table = table;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (table.getSelectedRow() == -1) {
                messageTextArea.setText(null);
            } else {
                messageTextArea.setText((String) table.getModel().getValueAt(table.getSelectedRow(), 0));
                messageTextArea.setCaretPosition(0);
            }
        }
    }
}
