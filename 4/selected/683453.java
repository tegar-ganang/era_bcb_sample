package com.googlecode.kanzaki.protocol.chatru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Random;
import com.googlecode.kanzaki.core.Core;
import com.googlecode.kanzaki.exception.BadTicketException;
import com.googlecode.kanzaki.exception.NetworkException;
import com.googlecode.kanzaki.exception.ProtocolException;
import com.googlecode.kanzaki.protocol.ConnectedChannelInterface;
import com.googlecode.kanzaki.protocol.HTTPRequest;
import com.googlecode.kanzaki.protocol.Message;

/**
 * ChatRuConnectedChannel.java
 * 
 * Part of kanzaki project. This class implements ConnectedChannelInterface
 * for chat at chat.chat.ru
 * 
 * @author Egor Ermakov 
 * (c) Copyright 2006 Ermakov Egor <knick_@mail.ru>
 * Licence: GPL  
 */
public class ChatRuConnectedChannel implements ConnectedChannelInterface {

    private static final String channelURI = "/pframe";

    private static final String sendMessageURI = "/newmsg_t";

    private static final String ticketField = "ticket";

    private static final String channelNameField = "channel";

    private static final String channelPasswordField = "chpwd";

    private static final String usernameField = "username";

    private static final String reqnumField = "reqnum";

    private static final String messageBodyField = "msgtext";

    private static final String intonationField = "action";

    private static final String endOfHeaders = "<pre>";

    private static final String badTicketPattern = "bad ticket";

    private static final String keepAliveMessage = "<!-- -->";

    private static final String privateMessageNotificationStart = "<SCRIPT LANGUAGE=\"JavaScript\">";

    private static final String privateMessageNotificationEnd = "</SCRIPT>";

    private ChatRuProtocolHandler protocolHandler;

    private BufferedReader serverReader;

    private boolean connected = false;

    private String channelName;

    private String channelPassword;

    private String username;

    public ChatRuConnectedChannel(ChatRuProtocolHandler protocolHandler, String channelName, String channelPassword) throws NetworkException, ProtocolException, BadTicketException {
        String nextLine;
        HTTPRequest request;
        HashMap params;
        this.protocolHandler = protocolHandler;
        this.channelName = channelName;
        this.channelPassword = channelPassword;
        this.username = protocolHandler.getUsername();
        params = new HashMap();
        params.put(usernameField, protocolHandler.getUsername());
        params.put(ticketField, protocolHandler.getTicket());
        params.put(channelNameField, channelName);
        params.put(channelPasswordField, channelPassword);
        request = new HTTPRequest(protocolHandler.getServerAddr(), protocolHandler.getServerPort(), channelURI, params);
        try {
            serverReader = request.doRequest();
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException("Can`t connect to server");
        }
        while (true) {
            try {
                nextLine = serverReader.readLine();
            } catch (IOException e) {
                throw new NetworkException("Exception while reading answer from server: " + e.toString());
            }
            if (nextLine == null) {
                throw new ProtocolException("Premature end of headers");
            }
            if (nextLine.indexOf(badTicketPattern) != -1) {
                throw new BadTicketException();
            }
            if (nextLine.equals(endOfHeaders)) {
                connected = true;
                return;
            }
        }
    }

    public Message getNextMessage() throws NetworkException, ProtocolException, BadTicketException {
        String nextLine;
        boolean waitingEndOfPrivateNotification = false;
        if (connected == false) {
            throw new NetworkException("Not connected");
        }
        while (true) {
            try {
                nextLine = serverReader.readLine();
                if (waitingEndOfPrivateNotification == true) {
                    if (nextLine.indexOf(privateMessageNotificationEnd) != -1) {
                        waitingEndOfPrivateNotification = false;
                    }
                    continue;
                }
            } catch (IOException e) {
                throw new NetworkException("Network exception while reading from server: " + e.toString());
            }
            if (nextLine == null) {
                throw new NetworkException("Disconnected");
            }
            if (nextLine.length() == 0 || nextLine.equals(keepAliveMessage)) {
                continue;
            }
            if (nextLine.equals(badTicketPattern)) {
                throw new BadTicketException();
            }
            if (nextLine.indexOf(privateMessageNotificationStart) != -1) {
                waitingEndOfPrivateNotification = true;
                continue;
            }
            break;
        }
        return parseMessage(nextLine);
    }

    private Message parseMessage(String rawMessage) throws ProtocolException {
        Message result = new Message();
        String serverTimestamp;
        String emotion;
        String temp;
        try {
            rawMessage = new String(rawMessage.getBytes(), ChatRuProtocolHandler.chatServerEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new ProtocolException("System does not support server`s codepage");
        }
        switch(rawMessage.charAt(0)) {
            case 's':
                result.setMessageType(Message.MESSAGE_SYSTEM);
                break;
            case 'u':
                result.setMessageType(Message.MESSAGE_USUAL);
                break;
            default:
                System.out.println("Unkown message type. Raw message: " + rawMessage);
                throw new ProtocolException("Unknown message type of message: " + rawMessage);
        }
        rawMessage = rawMessage.substring(2);
        serverTimestamp = rawMessage.substring(0, rawMessage.indexOf("]") + 1);
        result.setServerTimestamp(serverTimestamp);
        rawMessage = rawMessage.substring(rawMessage.indexOf("]") + 1);
        if (result.getMessageType() == Message.MESSAGE_SYSTEM) {
            result.setBody(rawMessage);
        } else {
            temp = rawMessage.substring(0, rawMessage.indexOf(":"));
            result.setSender(temp);
            rawMessage = rawMessage.substring(rawMessage.indexOf(":") + 1);
            emotion = rawMessage.substring(1, rawMessage.indexOf(")"));
            if (emotion.equals("")) emotion = null;
            result.setEmotion(emotion);
            rawMessage = rawMessage.substring(rawMessage.indexOf(")") + 1);
            result.setBody(rawMessage);
        }
        return result;
    }

    public HashMap sendMessage(Message message) throws NetworkException, ProtocolException, BadTicketException {
        HTTPRequest request;
        HashMap params;
        String action;
        Random generator;
        generator = new Random();
        params = new HashMap();
        params.put(usernameField, protocolHandler.getUsername());
        params.put(ticketField, protocolHandler.getTicket());
        params.put(reqnumField, new Integer(generator.nextInt(10000)));
        params.put(messageBodyField, message.getBody());
        params.put(channelNameField, channelName);
        params.put(channelPasswordField, channelPassword);
        if (message.getEmotion() == null) {
            action = "200";
        } else if (message.getEmotion().equals("���������� �������")) {
            action = "201";
        } else if (message.getEmotion().equals("������")) {
            action = "202";
        } else if (message.getEmotion().equals("���������")) {
            action = "203";
        } else if (message.getEmotion().equals("�����")) {
            action = "204";
        } else if (message.getEmotion().equals("��������")) {
            action = "206";
        } else if (message.getEmotion().equals("��������")) {
            action = "205";
        } else {
            action = "200";
        }
        params.put(intonationField, action);
        request = new HTTPRequest(HTTPRequest.POST, protocolHandler.getServerAddr(), protocolHandler.getServerPort(), sendMessageURI, params);
        try {
            BufferedReader reader = request.doRequest();
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException("Can`t connect to server");
        }
        return null;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getUsername() {
        return username;
    }
}
