package spaghettiserver;

import java.util.*;

public class ChatSessionManager extends Thread {

    public Vector sessions = new Vector();

    public Server a;

    ChatSessionManager(Server a) {
        this.a = a;
        System.out.println("Chat Session Manager constructed. ");
    }

    public ChatSession getSession(String id) {
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s = (ChatSession) sessions.elementAt(i);
            if (s.name.equals(id)) {
                return s;
            }
        }
        return null;
    }

    public boolean join(ChatConnection c) {
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s = (ChatSession) sessions.elementAt(i);
            if (s.name.equals(c.sessionname) & s.password.equals(c.sessionpassword)) {
                return s.addConnection(c);
            }
        }
        System.out.println("Session not found, creating new session.");
        ChatSession s = new ChatSession(a);
        s.name = c.sessionname;
        s.password = c.sessionpassword;
        sessions.add(s);
        a.currentSessions++;
        if (a.currentSessions > a.maxSessions) a.maxSessions = a.currentSessions;
        return s.addConnection(c);
    }

    public ChatSession getSession(ChatConnection c) {
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s = (ChatSession) sessions.elementAt(i);
            if (s.name.equals(c.roomid)) {
                return s;
            }
        }
        return null;
    }

    public void removeSession(ChatSession s) {
        a.currentSessions--;
        int j = -1;
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s1 = (ChatSession) sessions.elementAt(i);
            if (s1.equals(s)) j = i;
        }
        if (j != -1) sessions.removeElementAt(j);
        pushSessionList();
    }

    public String getChannelList() {
        String ret = "<ret><conferencelist>";
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s1 = (ChatSession) sessions.elementAt(i);
            ret += "<channel><roomid>" + s1.name + "</roomid><topic></topic><usercount>" + s1.getUserCount() + "</usercount>";
            if (s1.priv) ret += "<private/>";
            ret += "</channel>";
        }
        ret += "</conferencelist></ret>";
        return ret;
    }

    void pushSessionList() {
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s = (ChatSession) sessions.elementAt(i);
            s.dispatch(getChannelList());
        }
    }

    public void createSession(String roomid, String password) {
        ChatSession c = new ChatSession(this.a);
        c.name = roomid;
        c.password = password;
        sessions.addElement(c);
        pushSessionList();
    }

    public void run() {
        while (true) {
            try {
                sleep(10000);
                for (int i = 0; i < sessions.size(); i++) {
                    ChatSession s = (ChatSession) sessions.elementAt(i);
                    s.pushUserlist();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
