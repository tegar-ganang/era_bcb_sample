package net.sfjinyan.server;

import java.io.IOException;
import java.util.*;
import nanoxml.XMLElement;
import nanoxml.XMLParseException;
import net.sfjinyan.common.Message;
import net.sfjinyan.common.Utils;
import net.sfjinyan.server.chessgame.OnlineChessGame;
import net.sfjinyan.server.chessgame.OnlineChessGame.GameResult;
import net.sfjinyan.server.db.ServerUser;
import org.apache.mina.common.*;

/**
 * The class for handling commands from a client.
 */
class CommandsHandler extends IoHandlerAdapter {

    /**
     * The map of all connected clients, where the key is the username.
     */
    private static Map<String, ClientConnection> m = Collections.synchronizedMap(new HashMap<String, ClientConnection>());

    private static List<IoSession> preliminaryList = Collections.synchronizedList(new ArrayList<IoSession>());

    @Override
    public void exceptionCaught(IoSession session, Throwable t) throws Exception {
        if (t.getMessage() != null && !t.getMessage().equals("Connection reset by peer") && !(t instanceof XMLParseException) && !t.getMessage().equals("There is no such channel")) {
            t.printStackTrace();
        }
        session.close();
    }

    @Override
    public void messageReceived(IoSession session, Object msg) throws Exception {
        XMLElement el = new XMLElement();
        try {
            el.parseString((String) msg);
            if (!isLoggedIn(session)) {
                parseLogin(el, session);
            } else parse(el, findConnectionForSession(session));
        } catch (XMLParseException e) {
            session.write(Message.Reply.toXMLElement(false, e.getMessage()));
        }
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        preliminaryList.add(session);
    }

    @Override
    public void sessionClosed(IoSession session) {
        try {
            ClientConnection conn = findConnectionForSession(session);
            if (conn.isInGameState()) {
                conn.getCurrentGame().finishGame(OnlineChessGame.GameResult.ABORT, "");
                m.remove(findConnectionForSession(session).getUser().getUsername());
            }
        } catch (NullPointerException ex) {
        }
    }

    private ClientConnection findConnectionForSession(IoSession s) {
        for (ClientConnection c : m.values()) {
            if (c.getSession() == s) return c;
        }
        throw new NullPointerException("Can't find connection for a given session");
    }

    private boolean isLoggedIn(IoSession session) {
        for (IoSession c : preliminaryList) {
            if (c == session) return false;
        }
        return true;
    }

    /**
     * Parses XML element, until the user is authorized.
     * @param str XML element for parsing
     * @return Whether the user is authorized
     * @throws java.io.IOException
     */
    private void parseLogin(XMLElement element, IoSession cn) {
        ServerUser obj = JinyanServer.ib.checkAuthorization(element.getStringAttribute("username"), element.getStringAttribute("password"));
        if (obj != null) {
            ClientConnection cc;
            try {
                cc = new ClientConnection(cn);
                cc.setUser(obj);
                m.put(obj.getUsername(), cc);
                preliminaryList.remove(cn);
                cn.write(Message.Reply.toXMLElement(true, "Connected"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else cn.write(Message.Reply.toXMLElement(false));
    }

    public static boolean isConnected(String username) {
        return m.containsKey(username);
    }

    public static ClientConnection getConnectionOf(String username) {
        if (!m.containsKey(username)) return null;
        return m.get(username);
    }

    /**
     * Parses and executes the command of the client.
     * @param str XML element in a String representation
     */
    void parse(XMLElement element, ClientConnection issuer) {
        if (issuer.hasOffer()) {
            issuer.getOffer().replyToOfferor(element.toString());
            issuer.setOffer(null);
        }
        if (issuer.getFlagger() != null) {
            if (element.getName().equals("clkcheck")) {
                issuer.getFlagger().send(element);
                issuer.setFlagger(null);
            }
        }
        if (issuer.isInGameState() && element.getName().equals("move")) {
            issuer.doMove(element);
        } else if (element.getName().equals("request")) {
            if (element.getAttribute("type").equals("MatchRequest")) {
                ClientConnection cc = getConnectionOf(element.getStringAttribute("username"));
                try {
                    if (cc.isInGameState()) {
                        issuer.send(Message.Reply.toXMLElement(false, cc.getUser().getUsername() + " is already playing a game."));
                        return;
                    } else if (cc == issuer) {
                        issuer.send(Message.Reply.toXMLElement(false, "You cannot match yourself."));
                        return;
                    }
                    cc.setOffer(new Offer(issuer, cc, element, Offer.Type.MatchOffer));
                    element.setAttribute("username", issuer.getUser().getUsername());
                    cc.send(element);
                    issuer.send(Message.Info.toXMLElement("Match request sent"));
                } catch (NullPointerException ex) {
                    issuer.send(Message.Reply.toXMLElement(false, element.getStringAttribute("username") + " isn't logged in!"));
                }
            } else if (issuer.isInGameState() && isGameRelatedRequest((String) element.getAttribute("type"))) {
                parseGameRelatedRequest(element, issuer);
            } else {
                try {
                    issuer.send(execRequest(element, issuer));
                } catch (Exception ex) {
                    issuer.send(Message.Reply.toXMLElement(false, ex.getMessage()));
                    ex.printStackTrace();
                }
            }
        } else if (element.getName().equals("privtell")) {
            String receiver = element.getStringAttribute("username");
            try {
                ClientConnection cc = getConnectionOf(receiver);
                element.setAttribute("username", issuer.getUser().getUsername());
                cc.send(element);
                issuer.send(Message.Reply.toXMLElement(true, "Sent to " + receiver));
            } catch (NullPointerException e) {
                issuer.send(Message.Reply.toXMLElement(false, receiver + " isn't logged in."));
            }
        } else if (element.getName().equals("chantell")) {
            Channel cn = JinyanServer.getInstance().getChannel(element.getIntAttribute("channnumber"));
            element.setAttribute("username", issuer.getUser().getUsername());
            cn.send(element);
        } else if (element.getName().equals("whisper")) {
            OnlineChessGame game = null;
            int n = element.getIntAttribute("gamenumber");
            if (n == 0) {
                if (issuer.isInGameState()) {
                    game = issuer.getCurrentGame();
                } else game = issuer.primaryGame;
            } else if (issuer.isObservingGame(n)) game = issuer.observingGames.get(n);
            if (game != null) {
                element.setAttribute("username", issuer.getUser().getUsername());
                game.whisper(element);
            } else {
                issuer.tellDenied();
            }
        } else if (element.getName().equals("kibitz")) {
            OnlineChessGame game = null;
            int n = element.getIntAttribute("channnumber");
            if (n == 0) {
                if (issuer.isInGameState()) {
                    game = issuer.getCurrentGame();
                } else game = issuer.primaryGame;
            } else if (issuer.isObservingGame(n)) game = issuer.observingGames.get(n);
            if (game != null) {
                element.setAttribute("username", issuer.getUser().getUsername());
                game.kibitz(element);
            } else {
                issuer.tellDenied();
            }
        }
    }

    private boolean isGameRelatedRequest(String attribute) {
        if (attribute.equals("Resignation") || attribute.equals("DrawRequest") || attribute.equals("Flag") || attribute.equals("AbortRequest") || attribute.equals("AdjournRequest")) return true; else return false;
    }

    private void parseGameRelatedRequest(XMLElement element, ClientConnection issuer) {
        OnlineChessGame game = issuer.getCurrentGame();
        if (element.getAttribute("type").equals("Resignation")) {
            game.resign(issuer);
        } else if (element.getAttribute("type").equals("DrawRequest")) {
            if (game.isFiftyMoveRule()) game.finishGame(GameResult.FIFTYMOVERULE, "1/2-1/2"); else {
                ClientConnection opponent = game.getOpponentOf(issuer);
                opponent.send(element);
                opponent.setOffer(new Offer(issuer, opponent, element, Offer.Type.DrawOffer));
                issuer.send(Message.Info.toXMLElement("Draw request sent"));
            }
        } else if (element.getAttribute("type").equals("AbortRequest")) {
            ClientConnection opponent = game.getOpponentOf(issuer);
            opponent.send(element);
            opponent.setOffer(new Offer(issuer, opponent, element, Offer.Type.AbortOffer));
            issuer.send(Message.Info.toXMLElement("Abort request sent"));
        } else if (element.getAttribute("type").equals("AdjournRequest")) {
            ClientConnection opponent = game.getOpponentOf(issuer);
            if (!(JinyanServer.ib.isMaxCount(issuer.getUser().getUsername()) || JinyanServer.ib.isMaxCount(opponent.getUser().getUsername()))) {
                opponent.send(element);
                opponent.setOffer(new Offer(issuer, opponent, element, Offer.Type.AdjournmentOffer));
                issuer.send(Message.Info.toXMLElement("Adjourn request sent"));
            } else issuer.tellDenied();
        } else if (element.getAttribute("type").equals("Flag")) {
            if (issuer.getCurrentGame().getClockValueFor(issuer) <= 0) {
                XMLElement elm = new XMLElement();
                elm.parseString("<flagping/>");
                ClientConnection usr = issuer.getCurrentGame().getOpponentOf(issuer);
                usr.send(elm);
                usr.setFlagger(issuer);
                issuer.tellAccepted();
            }
        }
    }

    /**
     * Method for the request, which requires immediate response
     * @param element XML element of the request
     * @return Whether the request is sucessfull
     */
    private XMLElement execRequest(XMLElement element, ClientConnection issuer) {
        if (element.getAttribute("type").equals("NotesModif")) {
            issuer.getUser().setNotes(element.getContent());
            JinyanServer.ib.updateUser(issuer.getUser());
            return Message.Reply.toXMLElement(true);
        } else if (element.getAttribute("type").equals("Finger")) {
            ClientConnection cc = JinyanServer.getConnectionOf((String) element.getAttribute("username"));
            if (cc != null) {
                issuer.send(Message.Info.toXMLElement((Utils.generateUserProfile(cc.getUser()))));
            } else {
                ServerUser usr = JinyanServer.ib.getUser((String) element.getAttribute("username"));
                if (usr != null) issuer.send(Message.Info.toXMLElement(Utils.generateUserProfile(usr))); else return Message.Reply.toXMLElement(false, "No such user");
            }
            return Message.Reply.toXMLElement(true);
        } else if (element.getAttribute("type").equals("History")) {
            ClientConnection cc = JinyanServer.getConnectionOf((String) element.getAttribute("username"));
            if (cc != null) {
                issuer.send(Message.Info.toXMLElement((Utils.generateUserHistory(cc.getUser()))));
            } else {
                ServerUser usr = JinyanServer.ib.getUser((String) element.getAttribute("username"));
                if (usr != null) issuer.send(Message.Info.toXMLElement(Utils.generateUserHistory(usr))); else return Message.Reply.toXMLElement(false, "No such user");
            }
            return null;
        } else if (element.getAttribute("type").equals("GameInfo")) {
            if (element.getAttribute("gamenumber").equals("*")) {
                StringBuffer result = new StringBuffer();
                for (OnlineChessGame game : JinyanServer.liveGames) {
                    result.append(game.toXML());
                }
                issuer.send(Message.Info.toXMLElement(result.toString()));
                return null;
            } else {
                int n = element.getIntAttribute("gamenumber");
                OnlineChessGame game = null;
                for (OnlineChessGame gm : JinyanServer.liveGames) {
                    if (gm.getId() == n) game = gm;
                }
                if (game == null) return Message.Reply.toXMLElement(false); else {
                    issuer.send(game.toXML());
                    return null;
                }
            }
        } else if (element.getAttribute("type").equals("GameObserve")) {
            if (element.getAttribute("gamenumber") == null) {
                return Message.Reply.toXMLElement(issuer.gameObserve(element.getStringAttribute("username")));
            }
            return Message.Reply.toXMLElement(issuer.gameObserve(element.getIntAttribute("gamenumber")));
        } else if (element.getAttribute("type").equals("GameUnobserve")) {
            int num = element.getIntAttribute("gamenumber");
            OnlineChessGame gm = JinyanServer.liveGames.get(num - 1);
            if (gm == null || !gm.isObserving(issuer)) return Message.Reply.toXMLElement(false);
            gm.removeObserver(issuer);
            return Message.Reply.toXMLElement(true, "You are not observing game " + num + " now");
        } else if (element.getAttribute("type").equals("ChannAdd")) {
            Channel cn = JinyanServer.getInstance().getChannel(element.getIntAttribute("channnumber"));
            cn.addSubscrb(issuer);
            return Message.Reply.toXMLElement(true, "You are now subscribed to channel " + element.getIntAttribute("channnumber"));
        } else if (element.getAttribute("type").equals("ChannRemove")) {
            Channel cn = JinyanServer.getInstance().getChannel(element.getIntAttribute("channnumber"));
            boolean removed = cn.removeSubscrb(issuer);
            return Message.Reply.toXMLElement(removed, "Channel " + element.getIntAttribute("channnumber") + " was removed from your list");
        } else if (element.getAttribute("type").equals("Resume")) {
            if (JinyanServer.ib.hasAdjournedGames(issuer.getUser().getUsername())) {
                return Message.Reply.toXMLElement(JinyanServer.ib.attemptToResumeGame(issuer.getUser().getUsername(), (String) element.getAttribute("opponent")));
            }
            return Message.Reply.toXMLElement(false);
        } else if (element.getAttribute("type").equals("AdjournedList")) {
            XMLElement list = JinyanServer.ib.getAdjournedList(issuer.getUser().getUsername());
            issuer.send(list);
            return null;
        } else return Message.Reply.toXMLElement(false);
    }
}
