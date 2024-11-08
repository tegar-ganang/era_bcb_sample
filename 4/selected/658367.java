package net.hypotenubel.jaicwain.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.*;
import net.hypotenubel.jaicwain.App;
import net.hypotenubel.jaicwain.gui.*;
import net.hypotenubel.jaicwain.gui.swing.*;
import net.hypotenubel.jaicwain.local.*;
import net.hypotenubel.jaicwain.session.irc.*;

/**
 * When invoked, this action parts from the current channel.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: PartAction.java 155 2006-10-08 22:11:13Z captainnuss $
 */
public class PartAction extends AbstractAction implements GUIEventListener, LocalizationEventListener {

    /**
     * Currently active channel.
     */
    private JaicWainIRCChannel currentChannel = null;

    /**
     * Creates a new {@code PartAction} object and initializes it.
     */
    public PartAction() {
        App.gui.addGUIEventListener(this);
        App.localization.addLocalizationEventListener(this);
        languageChanged();
        updateState();
    }

    /**
     * Updates this action's state.
     */
    private void updateState() {
        if (App.gui.getFocussedMainFrame() == null) {
            currentChannel = null;
            setEnabled(false);
            return;
        }
        JaicWainIRCChannel chan = null;
        Component c = App.gui.getFocussedMainFrame().getTabbedChannelContainer().getTabbedPane().getSelectedComponent();
        if (c instanceof IRCChatPanel) {
            chan = ((IRCChatPanel) c).getChannel();
        } else {
            currentChannel = null;
            setEnabled(false);
            return;
        }
        currentChannel = chan;
        setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (currentChannel != null) {
            currentChannel.execute("/part");
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

    public void languageChanged() {
        putValue(NAME, App.localization.localize("app", "partaction.name", "Part"));
        putValue(SHORT_DESCRIPTION, App.localization.localize("app", "partaction.description", "Leaves the current channel."));
    }
}
