package com.itbs.aimcer.commune;

import com.itbs.aimcer.bean.*;
import com.itbs.aimcer.gui.Main;
import com.itbs.util.GeneralUtils;
import javax.swing.*;
import java.io.File;
import java.util.logging.Logger;

/**
 * Handles basic connection maintenance.
 * Reconnects, refreshing lists  etc.
 * @author Alex Rass
 * Date: Sep 9, 2004
 */
public class GlobalEventHandler implements ConnectionEventListener {

    private static final Logger log = Logger.getLogger(GlobalEventHandler.class.getName());

    private int connectionsInProgress;

    public GlobalEventHandler() {
    }

    public void statusChanged(Connection connection, Contact contact, Status oldStatus) {
        if (contact.getStatus().isOnline() && !contact.getStatus().isAway() && contact instanceof ContactWrapper) {
            ContactWrapper cw = (ContactWrapper) contact;
            if (cw.getPreferences().isNotifyOnConnect() && !oldStatus.isOnline() && (contact.getStatus().isOnline() || contact.getStatus().isAway())) {
                Main.showTooltip(contact.getDisplayName() + " came online.");
            }
        }
    }

    public void statusChanged(Connection connection) {
    }

    /**
     * A previously requested icon has arrived.
     * Icon will be a part of the contact.
     *
     * @param connection connection
     * @param contact    contact
     */
    public void pictureReceived(IconSupport connection, Contact contact) {
    }

    public void typingNotificationReceived(MessageSupport connection, Nameable contact) {
    }

    /**
     * Sent before connection is attempted
     *
     * @param connection in context
     */
    public void connectionInitiated(Connection connection) {
        Main.getStatusBar().setVisible(true);
        connectionsInProgress++;
    }

    private void connectionDone() {
        connectionsInProgress--;
        if (connectionsInProgress <= 0) {
            connectionsInProgress = 0;
            Main.getStatusBar().setVisible(false);
        }
    }

    public void connectionEstablished(final Connection connection) {
        if (ClientProperties.INSTANCE.isNotifyDisconnects()) {
            Main.setTitle(connection.getServiceName() + "- Online");
        }
        connectionDone();
        connection.setAway(ClientProperties.INSTANCE.isIamAway());
        connection.resetDisconnectInfo();
    }

    /**
     * Connection failed on login
     * @param connection that failed
     * @param message to display.
     */
    public void connectionFailed(final Connection connection, final String message) {
        log.info("Connection to " + connection.getServiceName() + " as " + connection.getUser() + " failed " + message + ". " + connection.getDisconnectCount() + " time(s).");
        connectionDone();
        handleDisconnect(connection);
        reconnect(connection, message);
    }

    public void connectionLost(final Connection connection) {
        log.info("Connection to " + connection.getServiceName() + " lost " + connection.getDisconnectCount() + " time(s).");
        if (ClientProperties.INSTANCE.isNotifyDisconnects()) {
            Main.setTitle(connection.getServiceName() + " Offline");
        }
        connectionDone();
        handleDisconnect(connection);
        reconnect(connection, "Lost Connection for " + connection.getUser().getName());
    }

    private synchronized void reconnect(final Connection connection, final String message) {
        if (Main.getFrame().isDisplayable() && !connection.isDisconnectIntentional()) {
            if (connection.getDisconnectCount() < ClientProperties.INSTANCE.getDisconnectCount() && connection.isConnectionValid()) {
                connection.incDisconnectCount();
                new Thread("Reconnect for " + connection.getServiceName()) {

                    public void run() {
                        try {
                            int lastReconnectCoefficient = (ClientProperties.INSTANCE.getDisconnectCount() == connection.getDisconnectCount() - 1) ? 5 : 1;
                            sleep(connection.getDisconnectCount() == 1 ? 5000 : 60 * 1000 * lastReconnectCoefficient);
                            if (Main.getFrame().isDisplayable() && !connection.isDisconnectIntentional() && connection.getDisconnectCount() < ClientProperties.INSTANCE.getDisconnectCount() && !connection.isLoggedIn()) {
                                log.info("Trying to reconnect " + connection.getServiceName() + " attempt no. " + connection.getDisconnectCount());
                                connection.reconnect();
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }.start();
            } else {
                new Thread("Complain") {

                    public void run() {
                        Main.complain(connection.getServiceName() + ": " + message);
                    }
                }.start();
            }
        }
    }

    /**
     * Call this method after you disconnect.
     * @param connection connection reference
     */
    private void handleDisconnect(Connection connection) {
        for (int i = 0; i < connection.getGroupList().size(); i++) {
            Group group = connection.getGroupList().get(i);
            for (int j = 0; j < group.size(); j++) {
                Nameable contact = group.get(j);
                if (contact instanceof Contact) {
                    Contact cw = (Contact) contact;
                    if (connection == cw.getConnection()) cw.getStatus().setOnline(false);
                }
            }
        }
        Main.getPeoplePanel().update();
    }

    public boolean messageReceived(MessageSupport connection, Message message) {
        if (!message.isOutgoing()) {
            if (ClientProperties.INSTANCE.getIpQuery().length() > 0) {
                if (ClientProperties.INSTANCE.getIpQuery().equals(message.getPlainText())) {
                    try {
                        connection.sendMessage(new MessageImpl(message.getContact(), true, true, GeneralUtils.getInterfaces(false)));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return true;
    }

    public boolean emailReceived(MessageSupport connection, Message message) throws Exception {
        Main.showTooltip(message.getText());
        return true;
    }

    /**
     * Gets called when an assynchronous error occurs.
     *
     * @param message   to display
     * @param exception exception for tracing
     */
    public void errorOccured(String message, Exception exception) {
        Main.complain(message, exception);
    }

    /**
     * Other side requested a file transfer.
     * @param connection connection
     * @param contact who initiated msg
     * @param filename proposed name of file
     * @param description of the file
     * @param connectionInfo  your private object used to store protocol specific data
     */
    public void fileReceiveRequested(FileTransferSupport connection, Contact contact, String filename, String description, Object connectionInfo) {
        final JFileChooser chooser = new JFileChooser(ClientProperties.INSTANCE.getLastFolder());
        chooser.setDialogTitle(contact + " is sending you file " + filename);
        chooser.setToolTipText(description);
        String name = filename == null ? "" : GeneralUtils.stripHTML(filename);
        name = GeneralUtils.replace(name, "/", "");
        name = GeneralUtils.replace(name, "\\", "");
        chooser.setSelectedFile(new File(ClientProperties.INSTANCE.getLastFolder(), name));
        int returnVal = chooser.showSaveDialog(null);
        ClientProperties.INSTANCE.setLastFolder(chooser.getCurrentDirectory().getAbsolutePath());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            connection.rejectFileTransfer(connectionInfo);
            return;
        } else if (chooser.getSelectedFile().isDirectory()) {
            JOptionPane.showMessageDialog(Main.getFrame(), "File already exists or is a folder.", "Error:", JOptionPane.ERROR_MESSAGE);
            connection.rejectFileTransfer(connectionInfo);
        } else if (chooser.getSelectedFile().exists()) {
            returnVal = JOptionPane.showConfirmDialog(Main.getFrame(), "File already exists overwrite?.", "Problem:", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (returnVal == JOptionPane.NO_OPTION) {
                fileReceiveRequested(connection, contact, filename, description, connectionInfo);
                return;
            } else if (returnVal == JOptionPane.CANCEL_OPTION) {
                connection.rejectFileTransfer(connectionInfo);
                return;
            }
            if (!chooser.getSelectedFile().delete()) {
                JOptionPane.showMessageDialog(Main.getFrame(), "Failed to delete the file.  Choose a different name please.", "Error:", JOptionPane.ERROR_MESSAGE);
                fileReceiveRequested(connection, contact, filename, description, connectionInfo);
                return;
            }
        }
        connection.acceptFileTransfer(new FileTransferAdapter(Main.getFrame(), description, chooser.getSelectedFile(), contact), connectionInfo);
    }

    public boolean contactRequestReceived(final String user, final MessageSupport connection) {
        String msg = "Following contact wants to add you to his/her list: " + user + " on " + connection.getServiceName();
        log.info(msg);
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(Main.getFrame(), msg, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }
}
