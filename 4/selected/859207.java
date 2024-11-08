package org.moxy.oak.plugin;

import org.moxy.irc.*;
import org.moxy.oak.*;
import org.moxy.oak.irc.*;
import org.moxy.oak.security.*;

public class PluginWrapper {

    private Object plugin = null;

    private String room = null;

    private IRCConnection server = null;

    private int myIdentifier;

    static int sidentifier = 1;

    static int cidentifier = 1;

    /**
     * Makes a channel plugin wrapper.
     * @since 1.0
     * @param plugin the plugin to wrap.
     * @param server the server the plugin was loaded on.
     * @param room the channel the plugin was loaded on.
     */
    public PluginWrapper(Object plugin, IRCConnection server, String room) {
        this.plugin = plugin;
        this.server = server;
        this.room = room;
        if (room == null) myIdentifier = sidentifier++; else myIdentifier = cidentifier++;
    }

    /**
       * Makes a server plugin wrapper
       * @since 1.0
       * @param plugin the plugin to wrap
       * @param server the server the plugin was loaded on.
       */
    public PluginWrapper(OakPlugin plugin, IRCConnection server) {
        this(plugin, server, null);
    }

    /**
       * Returns the plugin.
       * @since 1.0
       * @return The plugin.
       */
    public Object getPlugin() {
        return plugin;
    }

    /**
       * Returns teh server identifier.
       * @since 1.0
       * @return The server identifier.
       */
    public IRCConnection getServer() {
        return server;
    }

    /**
       * Returns the channel.
       * @since 1.0
       * return the channel or null if it's a server plugin
       */
    public String getChannel() {
        return room;
    }

    public int getIdentifier() {
        return myIdentifier;
    }
}
