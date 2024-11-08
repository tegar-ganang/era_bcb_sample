package org.jdamico.ircivelaclient.observer;

import java.util.Observable;
import java.util.Observer;
import org.jdamico.ircivelaclient.view.ChatPanel;
import org.jdamico.ircivelaclient.view.FrDrw2FS;
import org.jdamico.ircivelaclient.view.HandleApplet;
import org.jdamico.ircivelaclient.view.StaticData;
import jerklib.Channel;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.JoinEvent;
import jerklib.events.MessageEvent;
import jerklib.events.QuitEvent;
import jerklib.events.IRCEvent.Type;

public class IRCResponseHandler implements Observer {

    private ChatPanel chatPanel;

    private FrDrw2FS drawPanel;

    private HandleApplet handleApplet;

    public IRCResponseHandler() {
    }

    public IRCResponseHandler(HandleApplet handleApplet, ChatPanel chatPanel) {
        this.handleApplet = handleApplet;
        this.chatPanel = chatPanel;
    }

    public void update(Observable o, Object arg) {
        if (arg instanceof IRCEvent) {
            IRCEvent event = (IRCEvent) arg;
            if (event.getType() == Type.CONNECT_COMPLETE) {
                System.out.println("CONNECT_COMPLETE");
                System.out.println("chat: " + chatPanel);
                chatPanel.enableControls();
                handleApplet.setLoadingFlag(false);
            } else if (event.getType() == Type.JOIN_COMPLETE) {
                System.out.println("JOIN_COMPLETE");
                JoinCompleteEvent joinCompleteEvent = (JoinCompleteEvent) event;
                Channel currentChannel = joinCompleteEvent.getChannel();
                chatPanel.populateConnectedUsers(currentChannel);
            } else if (event.getType() == Type.CHANNEL_MESSAGE) {
                MessageEvent messageEvent = (MessageEvent) event;
                if (messageEvent.getMessage().startsWith("QUITCHAT")) {
                    chatPanel.removeUserTable(messageEvent.getNick());
                    chatPanel.removeUserHost(messageEvent.getHostName());
                    String tempStr = messageEvent.getNick() + " has left ";
                    StaticData.chatMessage = tempStr;
                    String color = chatPanel.getColor(messageEvent.getNick().trim());
                    if (messageEvent.getNick().equals(StaticData.teacher)) chatPanel.updateMainContentArea(tempStr, color, true); else chatPanel.updateMainContentArea(tempStr, color, false);
                    return;
                }
                String tempStr = messageEvent.getNick() + ": " + messageEvent.getMessage();
                StaticData.chatMessage = tempStr;
                String color = chatPanel.getColor(messageEvent.getNick().trim());
                if (messageEvent.getNick().trim().equals(StaticData.teacher)) chatPanel.updateMainContentArea(tempStr, color, true); else chatPanel.updateMainContentArea(tempStr, color, false);
            } else if (event.getType() == Type.PRIVATE_MESSAGE) {
                System.out.println("PRIVATE_MESSAGE");
                MessageEvent messageEvent = (MessageEvent) event;
                String what = messageEvent.getMessage();
                String whom = messageEvent.getNick().trim();
                handleApplet.updatePvt(whom, whom + ": " + what);
                handleApplet.setTabForegroundColor(messageEvent.getNick().trim(), 2);
            } else if (event.getType() == Type.JOIN) {
                System.out.println("JOIN");
                JoinEvent joinEvent = (JoinEvent) event;
                chatPanel.addUserTable(joinEvent.getNick());
                chatPanel.addUserHost(joinEvent.getHostName());
                System.out.println("host: " + joinEvent.getHostName());
            } else if (event.getType() == Type.QUIT) {
                System.out.println("QUIT");
                QuitEvent quitEvent = (QuitEvent) event;
                chatPanel.removeUserTable(quitEvent.getNick());
                chatPanel.removeUserHost(quitEvent.getHostName());
                String tempStr = quitEvent.getNick() + " has left (" + quitEvent.getQuitMessage() + ")";
                StaticData.chatMessage = tempStr;
                String color = chatPanel.getColor(quitEvent.getNick().trim());
                if (quitEvent.getNick().equals(StaticData.teacher)) chatPanel.updateMainContentArea(tempStr, color, true); else chatPanel.updateMainContentArea(tempStr, color, false);
            } else if (event.getType() == Type.WHOIS_EVENT) {
            }
        }
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setChatPanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
    }

    public HandleApplet getHandleApplet() {
        return handleApplet;
    }

    public void setHandleApplet(HandleApplet handleApplet) {
        this.handleApplet = handleApplet;
    }

    public FrDrw2FS getDrawPanel() {
        return drawPanel;
    }

    public void setDrawPanel(FrDrw2FS drawPanel) {
        this.drawPanel = drawPanel;
    }
}
