package org.relayirc.chatengine;

import org.relayirc.util.*;
import java.util.EventObject;

/**
 * Event fired by a Server. Has either a Channel object, a Server
 * object, a string status message or no value; other fields will be null.
 * @author David M. Johnson.
 * @version $Revision: 1.2 $
 *
 * <p>The contents of this file are subject to the Mozilla Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/</p>
 * Original Code:     Relay IRC Chat Engine<br>
 * Initial Developer: David M. Johnson <br>
 * Contributor(s):    No contributors to this file <br>
 * Copyright (C) 1997-2000 by David M. Johnson <br>
 * All Rights Reserved.
 */
public class ServerEvent extends EventObject {

    private Channel _channel = null;

    private User _user = null;

    private Server _server = null;

    private String _message = null;

    /** Event with no associated value. */
    public ServerEvent(Server src) {
        super(src);
        Debug.println("ServerEvent(" + src + ")");
    }

    /** Event associated with a channel. */
    public ServerEvent(Server src, Channel channel) {
        super(src);
        _channel = channel;
        Debug.println("ServerEvent(" + src + "," + channel + ")");
    }

    /** Event associated with server. */
    public ServerEvent(Server src, Server server) {
        super(src);
        _server = server;
        Debug.println("ServerEvent(" + src + "," + server + ")");
    }

    /** Event associated with a user. */
    public ServerEvent(Server src, User user) {
        super(src);
        _user = user;
        Debug.println("ServerEvent(" + src + "," + user + ")");
    }

    /**
	 * Event associated with status message. Server sends out
    * status messages as events.
    */
    public ServerEvent(Server src, String message) {
        super(src);
        _message = message;
        Debug.println("ServerEvent(" + src + "," + message + ")");
    }

    /** Get associated Channel object, or null if not applicable. */
    public Channel getChannel() {
        return _channel;
    }

    /** Get associated Server object, or null if not applicable. */
    public Server getServer() {
        return _server;
    }

    /** Get associated message, or null if not applicable. */
    public String getMessage() {
        return _message;
    }

    /** Get user associated with this message, or null if not applicable. */
    public User getUser() {
        return _user;
    }
}
