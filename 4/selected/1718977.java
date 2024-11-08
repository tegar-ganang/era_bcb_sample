package com.monad.homerun.pkg.hms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.util.TimeUtil;
import com.monad.homerun.uiutl.RmiAppBase;
import com.monad.homerun.base.User;
import com.monad.homerun.rmictrl.MessageCtrl;
import com.monad.homerun.message.Message;
import com.monad.homerun.message.Track;

/**
 * Composer provides a way for users to read, compose, 
 * acknowledge and post HomeRun messages
 */
public class Composer extends RmiAppBase implements ActionListener {

    private static final long serialVersionUID = 3060402914797481392L;

    private static final int compWidth = 600;

    private static final int compHeight = 400;

    private MessageCtrl msgCtrl = null;

    private String readBox = null;

    private int numMsgs = 0;

    private Message readMsg = null;

    private String[] readers = null;

    private JComboBox boxCB = new JComboBox();

    private JComboBox recipCB = new JComboBox();

    private JButton prevButton = new JButton();

    private JButton nextButton = new JButton();

    private int numUp = 0;

    private int numDn = 0;

    private JTextField senderFld = new JTextField();

    private JTextField timeFld = new JTextField();

    private JTextArea readArea = new JTextArea(5, 16);

    private JTextArea writeArea = new JTextArea(5, 16);

    private JButton sendButton = new JButton("Send");

    private JButton ackButton = new JButton("Ack");

    private JButton clearButton = new JButton("Clear");

    private JButton copyButton = new JButton("Copy");

    private JButton infoButton = new JButton("Info");

    public Composer() {
        appName = "Composer";
        appIcon = "email.png";
        appWidth = compWidth;
        appHeight = compHeight;
    }

    public void initUIComponents(Container cp) {
        cp.setLayout(new BorderLayout());
        Box aboutBox = new Box(BoxLayout.X_AXIS);
        aboutBox.add(new JLabel("Box:"));
        aboutBox.add(boxCB);
        aboutBox.add(new JLabel("From:"));
        senderFld.setEditable(false);
        senderFld.setBackground(Color.WHITE);
        aboutBox.add(senderFld);
        aboutBox.add(new JLabel("At:"));
        timeFld.setEditable(false);
        timeFld.setBackground(Color.WHITE);
        aboutBox.add(timeFld);
        Box readCmdBox = new Box(BoxLayout.Y_AXIS);
        prevButton.addActionListener(this);
        readCmdBox.add(prevButton);
        nextButton.addActionListener(this);
        readCmdBox.add(nextButton);
        ackButton.addActionListener(this);
        readCmdBox.add(ackButton);
        infoButton.addActionListener(this);
        readCmdBox.add(infoButton);
        JPanel readPanel = new JPanel(new BorderLayout());
        readPanel.add(aboutBox, BorderLayout.NORTH);
        readPanel.add(new JScrollPane(readArea), BorderLayout.CENTER);
        readPanel.add(readCmdBox, BorderLayout.EAST);
        JPanel sendPanel = new JPanel();
        sendPanel.add(new JLabel("To:"));
        sendPanel.add(recipCB);
        Box writeCmdBox = new Box(BoxLayout.Y_AXIS);
        sendButton.addActionListener(this);
        writeCmdBox.add(sendButton);
        copyButton.addActionListener(this);
        writeCmdBox.add(copyButton);
        clearButton.addActionListener(this);
        writeCmdBox.add(clearButton);
        JPanel writePanel = new JPanel(new BorderLayout());
        writePanel.add(sendPanel, BorderLayout.NORTH);
        writePanel.add(writeCmdBox, BorderLayout.EAST);
        writePanel.add(new JScrollPane(writeArea), BorderLayout.CENTER);
        cp.add(readPanel, BorderLayout.NORTH);
        cp.add(writePanel, BorderLayout.CENTER);
        cp.add(msgLabel, BorderLayout.SOUTH);
    }

    public void start() {
        super.start();
        try {
            msgCtrl = (MessageCtrl) appReg.getServerControl("message");
            if (msgCtrl == null) {
                setSysMessage("Message system unavailable - exit Informer");
                return;
            }
            prevButton.setIcon(getHRIcon("system", "prev.gif"));
            nextButton.setIcon(getHRIcon("system", "next.gif"));
            readers = msgCtrl.getReaderNames();
            String[] boards = msgCtrl.getMessageBoards(userName);
            for (int i = 0; i < boards.length; i++) {
                boxCB.addItem(boards[i]);
                recipCB.addItem(boards[i]);
            }
            String[] recipients = msgCtrl.getRecipientNames();
            for (int i = 0; i < recipients.length; i++) {
                recipCB.addItem(recipients[i]);
                if (userName.equals(recipients[i])) {
                    boxCB.addItem(userName);
                }
            }
            boxCB.addActionListener(this);
            showBox();
        } catch (RemoteException remExp) {
            setSysMessage("Unable to initialize");
        }
    }

    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        if (source == sendButton) {
            postMessage();
        } else if (source == clearButton) {
            writeArea.setText(null);
        } else if (source == prevButton) {
            scrollMessages("prev");
        } else if (source == nextButton) {
            scrollMessages("next");
        } else if (source == ackButton) {
            ackMessage();
        } else if (source == copyButton) {
            copyMessage();
        } else if (source == infoButton) {
            showMessageTrack();
        } else if (source == boxCB) {
            showBox();
        }
    }

    private void showBox() {
        readBox = (String) boxCB.getSelectedItem();
        clearMessages();
        scrollMessages("init");
    }

    private void scrollMessages(String mode) {
        if ("init".equals(mode)) {
            readMsg = null;
            try {
                numMsgs = msgCtrl.getNumMessages(readBox);
            } catch (RemoteException re) {
                setSysMessage(SERVER_ERR);
                return;
            }
            numUp = 0;
            numDn = (numMsgs > 0) ? numMsgs - 1 : numMsgs;
            prevButton.setText(String.valueOf(numUp));
            nextButton.setText(String.valueOf(numDn));
        } else if ("prev".equals(mode)) {
            if (numUp > 0) {
                prevButton.setText(String.valueOf(--numUp));
                nextButton.setText(String.valueOf(++numDn));
            }
        } else {
            if (numDn > 0) {
                prevButton.setText(String.valueOf(++numUp));
                nextButton.setText(String.valueOf(--numDn));
            }
        }
        prevButton.setEnabled(numUp > 0);
        nextButton.setEnabled(numDn > 0);
        try {
            String msgKey = readMsg != null ? readMsg.getKey() : null;
            if (GlobalProps.DEBUG) {
                System.out.println("MskKey: " + msgKey + " recip: " + readBox);
            }
            readMsg = msgCtrl.getMessage(msgKey, (!"prev".equals(mode)), readBox);
            if (readMsg != null && !User.ANON_USER.equals(userName)) {
                msgCtrl.ackMessage(readMsg.getKey(), userName, Track.SEEN);
            }
        } catch (RemoteException re) {
            setSysMessage(SERVER_ERR);
            readMsg = null;
        }
        if (readMsg != null) {
            senderFld.setText(readMsg.getSender());
            timeFld.setText(TimeUtil.windowFormat(readMsg.getTimeStamp()));
            if (userName.equals(readBox)) {
                String msgSender = readMsg.getSender();
                if (!User.ANON_USER.equals(msgSender)) {
                    recipCB.setSelectedItem(msgSender);
                }
            } else {
                recipCB.setSelectedItem(readBox);
            }
            readArea.setText(readMsg.getText());
        }
        validate();
    }

    private void showMessageTrack() {
        if (readMsg == null) {
            return;
        }
        Track msgTrack = null;
        try {
            msgTrack = msgCtrl.getTrack(readMsg.getKey());
        } catch (RemoteException re) {
            msgTrack = null;
        }
        if (msgTrack != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("<--Reader Status-->");
            sb.append('\n');
            for (int i = 0; i < readers.length; i++) {
                sb.append(readers[i]);
                sb.append(": ");
                String status = msgTrack.getReaderStatus(readers[i]);
                if (status != null) {
                    int split = status.indexOf(":");
                    if (split > 0) {
                        sb.append(status.substring(0, split));
                        sb.append(" ");
                        long time = Long.parseLong(status.substring(split + 1));
                        sb.append(TimeUtil.windowFormat(time));
                    }
                } else {
                    sb.append("Unseen");
                }
                sb.append('\n');
            }
            writeArea.setText(sb.toString());
        }
    }

    private void copyMessage() {
        if (readMsg != null) {
            writeArea.setText(readMsg.getText());
        }
    }

    private void clearMessages() {
        readArea.setText(null);
        senderFld.setText(null);
        timeFld.setText(null);
    }

    private void postMessage() {
        if (writeArea.getText().length() == 0) {
            setSysMessage("No message to send");
            return;
        }
        String recipient = (String) recipCB.getSelectedItem();
        if (recipient == null || recipient.length() == 0) {
            setSysMessage("No recipient for message");
            return;
        }
        Message msg = new Message(userName, recipient, writeArea.getText());
        try {
            if (msgCtrl.postMessage(msg)) {
                setSysMessage("Message transmitted");
                if (recipient.equals(readBox)) {
                    showBox();
                }
            } else {
                setSysMessage("Unable to post message");
            }
        } catch (RemoteException re) {
            setSysMessage("Message failed");
            if (GlobalProps.DEBUG) {
                System.out.println("Caught remote exception in post: " + re.getMessage());
            }
        }
    }

    private void ackMessage() {
        if (readArea.getText().length() == 0 || readMsg == null) {
            setSysMessage("No message to acknowledge");
            return;
        }
        if (User.ANON_USER.equals(userName)) {
            setSysMessage("No anonymous acknowledgement");
            return;
        }
        try {
            if (msgCtrl.ackMessage(readMsg.getKey(), userName, Track.ACKED)) {
                setSysMessage("Acknowledgement transmitted");
            }
        } catch (RemoteException re) {
            setSysMessage("Acknowledgement failed");
            if (GlobalProps.DEBUG) {
                System.out.println("Caught remote exception in ack: " + re.getMessage());
            }
        }
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        new Composer().runAppInFrame(args);
    }
}
