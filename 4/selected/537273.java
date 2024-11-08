package org.moxy.oak.plugin;

import java.net.*;
import java.util.*;
import java.io.*;
import org.moxy.oak.*;
import org.moxy.irc.*;
import org.moxy.oak.irc.*;
import org.moxy.oak.security.*;

public class PartyLine2 implements DCCChatPlugin {

    Oak bot;

    PartyLine2Server server;

    Properties dccProps = new Properties();

    public String getDescription() {
        return "Join the party line.";
    }

    public void init(Oak b, DCCChatManager manager) {
        bot = b;
        server = new PartyLine2Server(bot);
        String s;
        s = System.getProperty("user.home");
        if (!s.endsWith(System.getProperty("file.separator"))) {
            s = s + System.getProperty("file.separator");
        }
        s = s + ".OakTJB";
        File f = new File(s);
        if (!f.exists()) {
            f.mkdirs();
        }
        f = new File(s, "PartyLine2.props");
        try {
            dccProps.load(new FileInputStream(f));
        } catch (Exception e) {
        }
    }

    public void newConnection(String fullNick, IRCConnection origin, Socket s) {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
            DataOutputStream output = new DataOutputStream(s.getOutputStream());
            String nick;
            output.writeBytes("User name:\n");
            String userName = input.readLine();
            output.writeBytes("Password:\n");
            String password = input.readLine();
            while (true) {
                output.writeBytes("PartyLine nick:\n");
                nick = input.readLine();
                if (server.isValidNick(nick)) {
                    break;
                }
                output.writeBytes("Nick already in use.\n");
            }
            server.addUser(new PartyLine2User(fullNick, nick, s, input, output, server));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void main(String arg[]) throws Exception {
        Properties props = new Properties();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        s = System.getProperty("user.home");
        if (!s.endsWith(System.getProperty("file.separator"))) {
            s = s + System.getProperty("file.separator");
        }
        s = s + ".OakTJB";
        File f = new File(s);
        if (!f.exists()) {
            f.mkdirs();
        }
        f = new File(s, "PartyLine2.props");
        try {
            props.load(new FileInputStream(f));
        } catch (Exception e) {
        }
        while (true) {
            System.out.println("Enter username:");
            String uname = in.readLine();
            System.out.println("Enter password:");
            String password = in.readLine();
            props.put(uname, password);
            System.out.println("Owner/oP/Regular? o/p/r");
            String rep = in.readLine().toLowerCase();
            if (rep.startsWith("o")) {
                props.put(uname + ".level", "o");
            } else if (rep.startsWith("p")) {
                props.put(uname + ".level", "p");
            } else {
                props.put(uname + ".level", "r");
            }
            System.out.println("Another y/n");
            if (!in.readLine().equals("y")) {
                break;
            }
        }
        props.store(new FileOutputStream(f), "DCC properties");
    }
}
