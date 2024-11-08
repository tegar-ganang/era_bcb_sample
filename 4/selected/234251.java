package uk.midearth.dvb.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ClientGUI {

    private static JFrame mainFrame = new JFrame("JDVB Client");

    private static JList chanList = new JList();

    private static JScrollPane chanPane = new JScrollPane();

    private static JScrollPane consolePane = new JScrollPane();

    private static JTextArea consoleText = new JTextArea(12, 40);

    private static JTextField statusField = new JTextField("Status ...");

    private static JButton playerButton = new JButton("Start Player Anyway");

    private static boolean consoleClick = false;

    private static ClientRMI client;

    private static void createGUI() {
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container pane = mainFrame.getContentPane();
        JLabel label;
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        label = new JLabel("Channel List");
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        pane.add(label, c);
        c.insets = new Insets(0, 0, 0, 0);
        label = new JLabel("Console");
        c.gridx = 1;
        c.gridy = 0;
        pane.add(label, c);
        chanList.setVisibleRowCount(10);
        chanPane.getViewport().setView(chanList);
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 1;
        pane.add(chanPane, c);
        c.insets = new Insets(0, 0, 0, 0);
        consolePane.getViewport().setView(consoleText);
        consoleText.setEditable(false);
        c.gridx = 1;
        c.gridy = 1;
        pane.add(consolePane, c);
        statusField.setEditable(false);
        statusField.setColumns(15);
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 0, 10);
        pane.add(statusField, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 2;
        pane.add(playerButton, c);
        addMouseListenerChanList();
        addActionListenerPlayerButton();
        playerButton.setEnabled(false);
        mainFrame.pack();
        mainFrame.setVisible(true);
        log("Done creating GUI.");
    }

    public static void log(String str) {
        logNoEnd(str + "\n");
    }

    public static void logNoEnd(String str) {
        JScrollBar sb = consolePane.getVerticalScrollBar();
        consoleText.append(str);
        sb.setValue(sb.getMaximum());
    }

    private static void addActionListenerPlayerButton() {
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                client.startPlayerAnyway();
            }
        };
        playerButton.addActionListener(al);
    }

    private static void addMouseListenerChanList() {
        MouseListener ml = new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (chanList.getSelectedIndex() != -1) {
                    client.changeChannel(chanList.getSelectedIndex());
                }
            }
        };
        chanList.addMouseListener(ml);
    }

    private static void getChannels() {
        log("Retrieving channel data");
        chanList.setListData(client.channels());
        log("Done retrieving channel data");
    }

    public static void main(String[] argv) {
        if (argv.length != 1) {
            System.out.println("ClientGUI usage:");
            System.out.println("  java ClientGUI [server config file]");
            System.exit(1);
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                createGUI();
            }
        });
        String configFilename = argv[0];
        log("Working on " + configFilename);
        client = new ClientRMI(configFilename);
        getChannels();
        statusField.setText("Server playing: " + client.currentServerChannel());
        chanList.setSelectedValue(client.currentServerChannel(), true);
        playerButton.setEnabled(true);
    }
}
