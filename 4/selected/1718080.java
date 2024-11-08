package com.enjoyxstudy.hip;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jibble.logbot.LogBot;

/**
 * @author onozaty
 */
public class TalkServlet extends HttpServlet {

    /** serialVersionUID */
    private static final long serialVersionUID = 7625101591153702998L;

    /** config */
    private Config config;

    /** irc bot */
    private HipLogBot ircBot;

    /**
     * @param ircBot
     * @param config
     */
    public TalkServlet(HipLogBot ircBot, Config config) {
        super();
        this.ircBot = ircBot;
        this.config = config;
    }

    /**
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    /**
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String message = request.getParameter("message");
        if (message == null || message.equals("")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String prefix = "";
        if (config.isPrefixRemoteAddress()) {
            String remoteAddr = request.getRemoteAddr();
            prefix = ((remoteAddr == null) ? "" : remoteAddr) + ": ";
        }
        String[] messages = message.split("\r\n|\r|\n");
        for (int i = 0; i < messages.length; i++) {
            sendMessage(prefix + messages[i]);
        }
    }

    /**
     * @param message
     */
    private void sendMessage(String message) {
        ircBot.sendMessage(config.getChannel(), message);
        ircBot.append(LogBot.BLACK, "<" + config.getNick() + "> " + message);
    }
}
