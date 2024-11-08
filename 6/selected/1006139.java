package com.thegreatchina.im.msn.backend;

import java.io.IOException;
import java.net.URLEncoder;
import org.apache.log4j.Logger;
import com.thegreatchina.im.IMException;
import com.thegreatchina.im.LoginFailException;
import com.thegreatchina.im.msn.Profile;
import com.thegreatchina.im.msn.MemberList;
import com.thegreatchina.im.msn.Contact.Status;
import com.thegreatchina.im.msn.backend.cmd.ClientCommand;
import com.thegreatchina.im.msn.backend.cmd.server.RNG;
import com.thegreatchina.im.msn.backend.cmd.server.XFRSB;

public class Server {

    public static final String MSN_SERVER = "messenger.hotmail.com";

    public static final int MSN_SERVER_PORT = 1863;

    public static final String NEXUS_SERVER = "nexus.passport.com";

    public static final int HTTPS_SERVER_PORT = 443;

    private static Logger logger = Logger.getLogger(Server.class);

    private Server() {
    }

    public static NotificationSession createSession(String userId, String pwd, MemberList cache, Status status) throws IMException {
        logger.debug("Server.createSession(.." + userId);
        Server server = new Server();
        SocketConnection notification = server.login(userId, pwd);
        if (notification != null) {
            NotificationSession session = new NotificationSession(notification, userId, cache, status);
            return session;
        }
        return null;
    }

    public static SwitchboardSession getInvited(RNG rng, Profile profile) throws IMException {
        logger.debug("Server.getInvited(RNG..");
        SocketConnection switchboard = new SocketConnection();
        try {
            switchboard.connect(rng.getSwitchboard(), rng.getPort());
        } catch (IOException e) {
            logger.error(e);
            throw new IMException(e);
        }
        SwitchboardSession session = new SwitchboardSession(switchboard, profile, rng.getSessionId());
        try {
            int trId = switchboard.getTrId();
            String cmd = "ANS " + trId + " " + profile.getAccountName() + " " + rng.getAuthentication() + " " + rng.getSessionId();
            switchboard.send(cmd);
            String response = switchboard.readLine();
            String endline = "ANS " + trId + " OK";
            while (!response.equals(endline)) {
                if ((response.length() < 4) || (!"IRO".equals(response.substring(0, 3)))) {
                    switchboard.close();
                    logger.error("Server.getInvited-" + response);
                    return null;
                }
                int start = response.indexOf(" ");
                start = response.indexOf(" ", start + 1);
                start = response.indexOf(" ", start + 1);
                int end = response.indexOf(" ", start + 1);
                start = end + 1;
                end = response.indexOf(" ", start);
                String account = response.substring(start, end);
                session.addContact(account);
                response = switchboard.readLine();
            }
        } catch (IOException e) {
            switchboard.close();
            logger.error(e);
            throw new IMException(e);
        }
        return session;
    }

    /**
	 * only create a swithboard session. nobody is invited.
	 * @param profile
	 * @param xfrsb
	 * @return
	 * @throws IMException
	 */
    public static SwitchboardSession initializeInvitation(Profile profile, XFRSB xfrsb) throws IMException {
        logger.debug("Server.initializeInvitation(Profile..");
        SocketConnection switchboard = new SocketConnection();
        try {
            switchboard.connect(xfrsb.getSwitchboard(), xfrsb.getPort());
        } catch (IOException e) {
            throw new IMException(e);
        }
        try {
            int trId = switchboard.getTrId();
            String cmd = "USR " + trId + " " + profile.getAccountName() + " " + xfrsb.getAuthentication();
            switchboard.send(cmd);
            String response = switchboard.readLine();
            String okline = "USR " + trId + " OK " + profile.getAccountName();
            if (response.indexOf(okline) != 0) {
                switchboard.close();
                logger.error("return null,response:" + response);
                return null;
            }
        } catch (IOException e) {
            switchboard.close();
            logger.error(e);
            throw new IMException(e);
        }
        SwitchboardSession session = new SwitchboardSession(switchboard, profile, null);
        return session;
    }

    private SocketConnection login(String userId, String pwd) throws IMException {
        logger.debug("Server.login(String..");
        SocketConnection notification = new SocketConnection();
        String response = null;
        String usrResponse = null;
        SocketConnection msnServer = new SocketConnection();
        String xfr = null;
        try {
            msnServer.connect(MSN_SERVER, MSN_SERVER_PORT);
        } catch (IOException e) {
            throw new IMException(e);
        }
        try {
            xfr = initializeServer(msnServer, userId);
        } catch (IOException e) {
            throw new IMException(e);
        } finally {
            msnServer.close();
        }
        String notificationServer = null;
        int notificationServerPort = -1;
        try {
            String tag = " NS ";
            int start = xfr.indexOf(tag);
            start += tag.length();
            int end = xfr.indexOf(":", start);
            notificationServer = xfr.substring(start, end);
            start = end + 1;
            end = xfr.indexOf(" ", start);
            String portString = xfr.substring(start, end);
            notificationServerPort = Integer.parseInt(portString);
        } catch (StringIndexOutOfBoundsException e) {
            logger.error(xfr);
            throw new IMException(e);
        }
        try {
            notification.connect(notificationServer, notificationServerPort);
        } catch (IOException e) {
            throw new IMException(e);
        }
        try {
            response = initializeServer(notification, userId);
            usrResponse = response.substring(12);
        } catch (IOException e) {
            throw new IMException(e);
        }
        String ticket = getTicket(usrResponse, userId, pwd);
        if (ticket == null) {
            notification.close();
            logger.error("ticket == null");
            return null;
        }
        try {
            String command = "USR " + notification.getTrId() + " TWN S " + ticket;
            notification.send(command);
            response = notification.readLine();
        } catch (IOException e) {
            notification.close();
            throw new IMException(e);
        }
        if ((response != null) && (response.indexOf("Unauthorized") >= 0)) {
            logger.error("unauthorized:" + response);
            return null;
        } else {
            return notification;
        }
    }

    private String getTicket(String usrResponse, String userId, String password) throws IMException {
        logger.debug("Server.getTicket(String..");
        String[] nexusResponse = null;
        String[] tick = null;
        SSLConnection nexus = new SSLConnection();
        SSLConnection login = new SSLConnection();
        SSLConnection redirect = new SSLConnection();
        try {
            nexus.connect(NEXUS_SERVER);
        } catch (IOException e) {
            throw new IMException(e);
        }
        try {
            String get = "GET /rdr/pprdr.asp HTTP/1.0" + ClientCommand.ENTER;
            nexus.send(get);
            nexusResponse = nexus.read();
        } catch (IOException e) {
            throw new IMException(e);
        } finally {
            nexus.close();
        }
        String loginServerString = null;
        String loginPath = null;
        String theString = null;
        String tag = null;
        int start = -1;
        int end = -1;
        try {
            tag = "DALogin=";
            theString = search(nexusResponse, tag);
            start = theString.indexOf(tag);
            end = theString.indexOf(",", start);
            String loginUrlString = theString.substring(start + tag.length(), end);
            end = loginUrlString.indexOf("/");
            loginServerString = loginUrlString.substring(0, end);
            loginPath = loginUrlString.substring(end);
        } catch (StringIndexOutOfBoundsException e) {
            logger.error(e);
            logger.warn(nexusResponse);
            throw new IMException(e);
        }
        try {
            login.connect(loginServerString);
        } catch (IOException e) {
            throw new IMException(e);
        }
        try {
            String get1 = "GET " + loginPath + " HTTP/1.1" + ClientCommand.ENTER;
            String get2 = "Authorization: Passport1.4 OrgVerb=GET,OrgURL=http%3A%2F%2Fmessenger%2Emsn%2Ecom," + "sign-in=" + URLEncoder.encode(userId, "utf-8") + ",pwd=" + password + "," + usrResponse + ClientCommand.ENTER;
            String get3 = "Host: " + loginServerString + ClientCommand.ENTER;
            String cmd3 = get1 + get2 + get3;
            login.send(cmd3);
            tick = login.read();
        } catch (IOException e) {
            throw new IMException(e);
        } finally {
            login.close();
        }
        if (tick[0].indexOf("Bad Request") >= 0) {
            return null;
        }
        String locationTag = "Location: https://";
        theString = search(tick, locationTag);
        if (theString != null) {
            start = theString.indexOf(locationTag);
        } else {
            start = -1;
        }
        if (start >= 0) {
            start += locationTag.length();
            end = theString.indexOf("/", start);
            String redirectServer = theString.substring(start, end);
            start = end;
            end = theString.indexOf(" ", start);
            String redirectPath = (end > 0) ? theString.substring(start, end) : theString.substring(start);
            try {
                redirect.connect(redirectServer);
            } catch (IOException e) {
                throw new IMException(e);
            }
            try {
                String get1 = "GET " + redirectPath + " HTTP/1.1" + ClientCommand.ENTER;
                String get2 = "Authorization: Passport1.4 OrgVerb=GET,OrgURL=http%3A%2F%2Fmessenger%2Emsn%2Ecom," + "sign-in=" + URLEncoder.encode(userId, "utf-8") + ",pwd=" + password + "," + usrResponse + ClientCommand.ENTER;
                String get3 = "Host: " + redirectServer + ClientCommand.ENTER;
                String cmd3 = get1 + get2 + get3;
                redirect.send(cmd3);
                tick = redirect.read();
                if (search(tick, "Bad Request") != null) {
                    logger.error("bad request:" + tick);
                    return null;
                }
            } catch (IOException e) {
                throw new IMException(e);
            } finally {
                redirect.close();
            }
        }
        String ticket = null;
        try {
            tag = "from-PP='";
            theString = search(tick, tag);
            start = theString.indexOf(tag);
            end = theString.indexOf("',", start);
            ticket = theString.substring(start + tag.length(), end);
        } catch (Exception e) {
            throw new LoginFailException(e);
        }
        return ticket;
    }

    private String initializeServer(SocketConnection con, String userId) throws IOException {
        logger.debug("Server.initializeServer(SocketConnection..");
        String command = "VER 1 MSNP8 CVR0";
        con.send(command);
        con.readLine();
        command = "CVR " + con.getTrId() + " 0x0409 win 4.10 i386 MSNMSGR 5.0.0544 MSMSGS " + userId;
        con.send(command);
        con.readLine();
        command = "USR " + con.getTrId() + " TWN I " + userId;
        con.send(command);
        String response = con.readLine();
        return response;
    }

    private String search(String[] list, String pattern) {
        for (String str : list) {
            if (str.indexOf(pattern) >= 0) {
                return str;
            }
        }
        return null;
    }
}
