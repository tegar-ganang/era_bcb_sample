package org.relayirc.swingui;

import org.relayirc.chatengine.*;
import org.relayirc.swingutil.*;
import org.relayirc.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * An MDI client frame that holds a ChannelPanel.
 * @author David M. Johnson
 * @version $Revision: 1.4 $
 *
 * <p>The contents of this file are subject to the Mozilla Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/</p>
 * Original Code: Relay-JFC Chat Client <br>
 * Initial Developer: David M. Johnson <br>
 * Contributor(s): No contributors to this file <br>
 * Copyright (C) 1997-2000 by David M. Johnson <br>
 * All Rights Reserved.
 */
public class ChannelFrame extends JInternalFrame implements MDIClientFrame {

    private Channel _channel = null;

    private ChannelPanel _chatPanel = null;

    public ChannelFrame(Channel chan) {
        super("[" + chan.getName() + "] " + chan.getTopic(), true, true, true, true);
        _channel = chan;
        setFrameIcon(IconManager.getIcon("Users"));
        _chatPanel = new ChannelPanel(_channel, this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(_chatPanel, BorderLayout.CENTER);
        addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameClosed(InternalFrameEvent e) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ((ChannelPanel) getChannelPanel()).part();
                    }
                });
            }
        });
        _channel.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("Topic")) {
                    setTitle("[" + _channel.getName() + "] " + evt.getNewValue());
                }
            }
        });
        validate();
    }

    public ChannelPanel getChannelPanel() {
        return _chatPanel;
    }

    public MDIClientPanel getClientPanel() {
        return _chatPanel;
    }

    public void setClientPanel(MDIClientPanel clientPanel) {
    }

    public JInternalFrame getFrame() {
        return this;
    }
}
