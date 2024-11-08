package aimee_agent;

import bsh.*;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 *
 * @author steven
 */
public class Main {

    private static Interpreter beanshell = null;

    private static HashMap<String, String> Options = null;

    private static XMPPConnection connection = null;

    private static Chat chat = null;

    private static boolean quit = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Options = new HashMap<String, String>();
        getOpts(args);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        }
        initShell();
        initDesktop();
        if (Options.containsKey("debugConnection")) {
            org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = true;
        }
        initXMPP();
        while (!quit) {
            ConnectionManager.getDefault().send();
            Notifier.sendAlert("Sent Message", "Message Sent");
            try {
                Thread.sleep(60l * 1000l);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                quit = true;
            }
        }
        Notifier.sendAlert("Exiting!", "AIMEE has recieved a signal to quit.");
        System.exit(0);
    }

    public static void Quit() {
        quit = true;
    }

    private static void getOpts(String[] args) {
        for (String line : args) {
            line = line.replaceAll("-", "");
            String[] data = line.split("=");
            Options.put(data[0], data[1]);
        }
    }

    public static synchronized Interpreter getShell() {
        return beanshell;
    }

    private static synchronized void initDesktop() {
        if (Options.containsKey("clearCache")) {
            PreferenceManager.getDefault().reset();
        }
        if (Options.containsKey("desktop")) {
            try {
                beanshell.eval("desktop()");
            } catch (EvalError ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            if (Options.containsKey("webdebug")) {
                try {
                    beanshell.eval("websession = server(" + Options.get("webdebug") + "))");
                    beanshell.eval("setAvailability(true)");
                    System.out.println("Web Server Started On Port 8888");
                } catch (EvalError ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    Notifier.sendException(ex);
                }
            }
        }
    }

    public static synchronized void initShell() {
        if (beanshell == null) {
            beanshell = new Interpreter();
            try {
                beanshell.set("TrayManager", TrayManager.getDefault());
                beanshell.set("Options", Options);
            } catch (EvalError ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                Notifier.sendException(ex);
            }
        } else {
            try {
                beanshell.eval("websession.terminate()");
                beanshell = null;
                initShell();
            } catch (EvalError ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                Notifier.sendException(ex);
            }
        }
    }

    public static void initXMPP() {
        try {
            String XMPPServer = PreferenceManager.getDefault().get("XMPP Server");
            ConnectionConfiguration config = new ConnectionConfiguration(XMPPServer);
            config.setDebuggerEnabled(true);
            connection = new XMPPConnection(XMPPServer);
            connection.connect();
            if (connection.isConnected()) {
                String login = PreferenceManager.getDefault().get("login");
                String password = PreferenceManager.getDefault().get("password");
                connection.login(login, password, java.net.InetAddress.getLocalHost().getHostName());
                if (connection.isAuthenticated()) {
                    chat = connection.getChatManager().createChat(PreferenceManager.getDefault().get("master"), ConnectionManager.getDefault().getChatListener());
                    ConnectionManager.getDefault().registerChat(chat);
                    Notifier.sendAlert("Connected", "AIMEE is now connected to " + chat.getParticipant());
                } else {
                    if (Options.get("showLoginFailure") != null) {
                        System.out.println("Failed to login using " + XMPPServer + "\n" + login + "\n" + password);
                    }
                    Notifier.sendAlert("Login Failure", "Could not log you in, try again later.");
                }
            } else {
                Notifier.sendAlert("Error!", "Could not connect to " + XMPPServer + "\nCheck your settings and try again");
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        } catch (XMPPException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            Notifier.sendException(ex);
        }
    }
}
