package com.google.gwt.dev.shell;

/**
 * A class to encapsulate data needed by {@code JavaObject}.
 */
public class SessionData {

    private final BrowserChannelClient browserChannel;

    private final HtmlUnitSessionHandler sessionHandler;

    public SessionData(HtmlUnitSessionHandler sessionHandler, BrowserChannelClient browserChannel) {
        this.sessionHandler = sessionHandler;
        this.browserChannel = browserChannel;
    }

    public BrowserChannelClient getChannel() {
        return browserChannel;
    }

    public HtmlUnitSessionHandler getSessionHandler() {
        return sessionHandler;
    }
}
