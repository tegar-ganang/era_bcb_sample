package net.hypotenubel.jaicwain.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.*;
import net.hypotenubel.irc.net.*;
import net.hypotenubel.jaicwain.App;
import net.hypotenubel.jaicwain.gui.*;
import net.hypotenubel.jaicwain.gui.swing.*;
import net.hypotenubel.jaicwain.local.*;
import net.hypotenubel.jaicwain.session.SessionStatus;

/**
 * When invoked, this action shows the server information dialog.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: ShowServerInformationAction.java 151 2006-10-04 22:26:50Z captainnuss $
 */
public class ShowServerInformationAction extends AbstractAction implements GUIEventListener, IRCConnectionListener, LocalizationEventListener {

    /**
     * Currently active session.
     */
    private AbstractIRCSession currentSession = null;

    /**
     * Creates a new {@code ShowServerInformationAction} object and
     * initializes it.
     */
    public ShowServerInformationAction() {
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
        ServerInformationDialog dialog = new ServerInformationDialog(currentSession);
        dialog.setOwner(frame);
        dialog.showModal();
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
        putValue(NAME, App.localization.localize("app", "showserverinformationaction.name", "Server Information..."));
        putValue(SHORT_DESCRIPTION, App.localization.localize("app", "showserverinformationaction." + "description", "Shows a dialog containing " + "information about the current " + "IRC server."));
    }
}
