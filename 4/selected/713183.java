package com.googlecode.kanzaki.protocol.chatru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import com.googlecode.kanzaki.core.Core;
import com.googlecode.kanzaki.exception.BadTicketException;
import com.googlecode.kanzaki.exception.NetworkException;
import com.googlecode.kanzaki.exception.NotAuthenticatedException;
import com.googlecode.kanzaki.exception.ProtocolException;
import com.googlecode.kanzaki.protocol.AuthToken;
import com.googlecode.kanzaki.protocol.ConnectedChannelInterface;
import com.googlecode.kanzaki.protocol.HTTPRequest;
import com.googlecode.kanzaki.protocol.ProtocolHandlerInterface;

public class ChatRuProtocolHandler implements ProtocolHandlerInterface {

    private String serverAddr;

    private int serverPort;

    private boolean authenticated = false;

    private String ticket;

    private String username;

    private String password;

    public static final String chatServerEncoding = "koi8-r";

    private static final int bufferSize = 4086;

    private static final String loginPath = "/";

    private static final String usernameField = "username";

    private static final String passwordField = "passwd";

    private static final String authFailedMark = "�������� ��� ��� ������";

    private static final String ticketMark = "ticket: ";

    public ChatRuProtocolHandler(String serverAddr, int serverPort) {
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    public List getChannelsList() throws NotAuthenticatedException, NetworkException, ProtocolException {
        if (authenticated == false) {
            throw new NotAuthenticatedException();
        }
        return null;
    }

    public ConnectedChannelInterface joinChannel(String channelName) throws NotAuthenticatedException, NetworkException, ProtocolException, BadTicketException {
        if (authenticated == false) {
            throw new NotAuthenticatedException();
        }
        return new ChatRuConnectedChannel(this, channelName, "");
    }

    public int authenticate(AuthToken token) throws NetworkException, ProtocolException {
        BufferedReader serverReader;
        String nextLine;
        HTTPRequest request;
        HashMap params;
        username = token.getLogin();
        password = token.getPassword();
        params = new HashMap();
        params.put(usernameField, username);
        params.put(passwordField, password);
        request = new HTTPRequest(HTTPRequest.POST, serverAddr, serverPort, loginPath, params);
        try {
            serverReader = request.doRequest();
        } catch (Exception e) {
            throw new NetworkException("Can`t connect to server");
        }
        try {
            while ((nextLine = serverReader.readLine()) != null) {
                nextLine = new String(nextLine.getBytes(), "KOI8-R");
                if (nextLine.indexOf(authFailedMark) != -1) {
                    return AUTH_FAILED;
                }
                if (nextLine.indexOf(ticketMark) != -1) {
                    ticket = new String(nextLine.substring(ticketMark.length()));
                    authenticated = true;
                    return AUTH_OK;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException("Network error during reading response from server");
        }
        throw new ProtocolException("Unable to parse response from server to our log in request. Perhaps you have wrong style or server has closed connection too early");
    }

    /**
	 * For ChatRuConnectedChannel class
	 */
    public String getServerAddr() {
        return serverAddr;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getTicket() {
        return ticket;
    }

    public String getUsername() {
        return username;
    }
}
