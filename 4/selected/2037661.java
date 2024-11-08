package net.hypotenubel.jaicwain.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.*;
import net.hypotenubel.irc.IRCMessage;
import net.hypotenubel.irc.msgutils.JoinMessage;
import net.hypotenubel.irc.net.*;
import net.hypotenubel.jaicwain.App;
import net.hypotenubel.jaicwain.gui.*;
import net.hypotenubel.jaicwain.gui.swing.*;
import net.hypotenubel.jaicwain.local.*;
import net.hypotenubel.jaicwain.session.SessionStatus;
import net.hypotenubel.jaicwain.session.irc.*;

/**
 * Displays the join channel dialog and joins the given channel.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: JoinAction.java 120 2006-09-22 10:37:17Z captainnuss $
 */
public class JoinAction extends AbstractAction implements GUIEventListener, IRCConnectionListener, LocalizationEventListener {

    /**
     * Currently active session.
     */
    private AbstractIRCSession currentSession = null;

    /**
     * Creates a new {@code JoinAction} object and initializes it.
     */
    public JoinAction() {
        App.gui.addGUIEventListener(this);
        App.localization.addLocalizationEventListener(this);
        languageChanged();
        updateState();
    }

    /**
     * Removes us from the listener list of the formerly active session and sets
     * our session reference to {@code null}.
     */
    private void resetSession() {
        if (currentSession != null) {
            currentSession.getConnection().removeIRCConnectionListener(this);
        }
        currentSession = null;
    }

    /**
     * Updates this action's state.
     */
    private void updateState() {
        if (App.gui.getFocussedMainFrame() == null) {
            resetSession();
            setEnabled(false);
            return;
        }
        AbstractIRCSession s = null;
        Component c = App.gui.getFocussedMainFrame().getTabbedChannelContainer().getTabbedPane().getSelectedComponent();
        if (c instanceof IRCChatPanel) {
            s = ((IRCChatPanel) c).getChannel().getParentSession();
        } else if (c instanceof IRCSessionPanel) {
            s = ((IRCSessionPanel) c).getSession();
        } else {
            resetSession();
            setEnabled(false);
            return;
        }
        if (s == currentSession) {
            return;
        } else {
            resetSession();
            currentSession = s;
        }
        setEnabled(currentSession.isActive());
        currentSession.getConnection().addIRCConnectionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        MainFrame frame = App.gui.getFocussedMainFrame();
        if (frame == null) {
            return;
        }
        if (currentSession == null) {
            return;
        }
        JoinDialog dialog = new JoinDialog(frame);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (dialog.getResult() == JOptionPane.OK_OPTION) {
            IRCChannel[] chans = dialog.getChannels();
            IRCMessage msg;
            for (int i = 0; i < chans.length; i++) {
                msg = JoinMessage.createMessage("", "", "", chans[i].getName(), chans[i].getKey());
                currentSession.getConnection().send(msg);
            }
        }
    }

    public void mainFrameAdded(MainFrame frame) {
    }

    public void mainFrameRemoving(MainFrame frame) {
    }

    public void mainFrameFocusChanged() {
        updateState();
    }

    public void mainFrameTabFocusChanged(MainFrame frame) {
        updateState();
    }

    public void statusChanged(IRCConnection conn, SessionStatus oldStatus) {
        setEnabled(conn.isActive());
    }

    public void messageSendable(IRCMessageEvent e) {
    }

    public void messageSent(IRCMessageEvent e) {
    }

    public void messageReceived(IRCMessageEvent e) {
    }

    public void languageChanged() {
        putValue(NAME, App.localization.localize("app", "joinaction.name", "Join..."));
        putValue(SHORT_DESCRIPTION, App.localization.localize("app", "joinaction.description", "Displays the Join Channel " + "dialog."));
    }
}
