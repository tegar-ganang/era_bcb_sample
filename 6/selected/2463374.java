package openjirc.plugin;

import java.util.*;
import openjirc.*;
import java.io.*;

public class BasicUI implements IUI {

    UIListener listener = null;

    boolean disconnected = false;

    String currentChannel = null;

    public BasicUI() {
        super();
    }

    public void init() {
        System.out.println("[Basic UI running ...]");
        System.out.println("[Type /connect to connect ...]");
        new Thread() {

            public void run() {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(System.in));
                } catch (Exception es) {
                    es.printStackTrace();
                }
                while (!disconnected) {
                    try {
                        String next = in.readLine();
                        executeLineTyped(next);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void setListener(UIListener l) {
        listener = l;
    }

    public void showOJIRCMessage(String message, int id) {
        System.out.println("OJIRC [" + getTime() + "]");
    }

    public void disconnected() {
        disconnected = true;
        System.out.println("! DISCONNECTED ! ==== ! DISCONNECTED ! ==[" + getTime() + "] == ! DISCONNECTED ! ==== ! DISCONNECTED !");
        System.exit(0);
    }

    public void connecteced() {
    }

    public void loggedIn() {
    }

    public void executeCommand(IRCMessage p) {
        if (OJConstants.isError(p.getCommand())) {
            System.out.println("[ Error : " + getTime() + "]" + p.getJoinedParameter(0));
        } else {
            System.out.println("[" + getShortTime() + "]" + p.getJoinedParameter(0));
        }
    }

    public boolean isCommand(String p) {
        p = p.trim();
        if (p.charAt(0) == '/') return true;
        return false;
    }

    public void executeLineTyped(String line) {
        if (line.trim().equals("")) return;
        if (!isCommand(line)) {
            if (currentChannel == null) {
                System.out.println("[Error] you are not member of any channels ...");
                return;
            }
            line = "/msg " + currentChannel + " " + line;
        }
        if (listener != null) {
            IRCMessage msg = listener.constructMessage(line);
            if (!listener.isValidCommand(msg)) return;
            if (msg.getCommand().equals("CONNECT")) {
                listener.connect();
                listener.login();
            } else if (msg.getCommand().equals("DISCONNECT")) {
                IRCMessage bePolite = new IRCMessage("QUIT", new String[] {});
                listener.sendCommand(bePolite);
                listener.disconnect();
                System.exit(0);
            } else {
                listener.sendCommand(msg);
            }
        }
    }

    public String getTime() {
        return Calendar.getInstance().getTime().toString();
    }

    public String getShortTime() {
        Calendar obj = Calendar.getInstance();
        return obj.get(Calendar.HOUR_OF_DAY) + " " + obj.get(Calendar.MINUTE);
    }
}
