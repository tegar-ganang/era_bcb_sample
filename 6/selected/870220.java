package org.efficientia.cimap.control;

import java.util.Vector;
import org.efficientia.cimap.model.Folder;
import org.efficientia.cimap.model.Message;
import org.efficientia.cimap.model.Server;

/**
 * The AppController realizes the
 * application use cases.
 * 
 * @author Ram�n Jim�nez (rjimenezz AT users DOT sourceforge DOT net)
 *
 */
public class AppController {

    /** IMAP server to interact with. */
    protected Server server;

    /** Mailbox root folder. */
    protected Folder inbox;

    /** Cache of folder's paths. */
    protected Vector folderPaths;

    /** Network monitor to notify of events. */
    protected NetworkMonitor monitor;

    /**
	 * Default constructor.
	 */
    public AppController() {
        folderPaths = new Vector();
    }

    /**
	 * Sets a network monitor listener.
	 * 
	 * @param mon Network monitor to be notified of network events
	 */
    public void setMonitor(NetworkMonitor mon) {
        monitor = mon;
    }

    /**
	 * Attempts to connect and logon to server.
	 * 
	 * @param host Server host
	 * @param port Server port
	 * @param userName IMAP account username
	 * @param password IMAP account password 
	 */
    public void connect(final String host, final String port, final String userName, final String password) {
        Thread t = new Thread() {

            public void run() {
                server = new Server(host, port);
                if (monitor != null) monitor.networkActivity(NetworkMonitor.CONNECTING);
                server.connect();
                if (monitor != null) {
                    monitor.networkActivity(NetworkMonitor.CONNECTED);
                    monitor.networkActivity(NetworkMonitor.LOGGING_IN);
                }
                server.login(userName, password);
                if (monitor != null) {
                    monitor.networkActivity(NetworkMonitor.LOGGED_IN);
                    monitor.networkActivity(NetworkMonitor.GETTING_FOLDERS);
                }
                inbox = server.getFolders();
                getSubFolderPaths();
                if (monitor != null) monitor.networkActivity(NetworkMonitor.GOT_FOLDERS);
            }
        };
        t.start();
    }

    /**
	 * Retrieve folder names as an array,
	 * in a depth-first fashion, with each
	 * entry indented to reflect hierarchy.
	 * 
	 * @return Folder names array
	 */
    public String[] getFolderNames() {
        int nFolders = folderPaths.size();
        String[] folderNames = new String[nFolders];
        for (int i = 0; i < nFolders; i++) {
            String eachPath = (String) folderPaths.elementAt(i);
            folderNames[i] = eachPath.substring(eachPath.lastIndexOf('.') + 1);
        }
        return folderNames;
    }

    /**
	 * Retrieves the subjects of all messages
	 * in the folder whose sequence is specified.
	 * 
	 * @param folderSeq Sequence of folder to get messages for
	 * @return Array of subjects belonging to messages in folder 
	 */
    public String[] getMessageSubjects(int folderSeq) {
        String[] messageSubjects = null;
        final Vector subjects = new Vector();
        String folderPath = (String) folderPaths.elementAt(folderSeq);
        int lastPeriod = folderPath.lastIndexOf('.') + 1;
        if (lastPeriod != 0) folderPath = folderPath.substring(0, lastPeriod) + folderPath.substring(lastPeriod).trim();
        final Folder folder = inbox.getSubFolderByPath(folderPath);
        Thread fetch = new Thread() {

            public void run() {
                server.getMessages(folder);
                Vector messages = folder.getMessages();
                for (int i = 0; i < messages.size(); i++) {
                    Message eachMsg = (Message) messages.elementAt(i);
                    subjects.addElement(eachMsg.getHeader("Subject"));
                }
            }
        };
        fetch.start();
        try {
            fetch.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        messageSubjects = new String[subjects.size()];
        for (int i = 0; i < messageSubjects.length; i++) messageSubjects[i] = (String) subjects.elementAt(i);
        return messageSubjects;
    }

    /**
	 * Disconnects the server in anticipation
	 * of application shutdown.
	 */
    public void disconnect() {
        server.logout();
    }

    /**
	 * Convenience method to cache folder paths.
	 */
    protected void getSubFolderPaths() {
        getSubFolderPaths(inbox, folderPaths, "");
    }

    /**
	 * Recursive method to obtain all
	 * folder paths.
	 * 
	 * @param current Current folder being examined
	 * @param paths Vector holding all folder paths
	 * @param indent Indentation level
	 */
    protected void getSubFolderPaths(Folder current, Vector paths, String indent) {
        String path = current.getPath();
        int lastPeriod = path.lastIndexOf('.') + 1;
        if (lastPeriod > 0) path = path.substring(0, lastPeriod) + indent + path.substring(lastPeriod);
        paths.addElement(path);
        indent += "  ";
        Vector subFolders = current.getSubFolders();
        for (int i = 0; i < subFolders.size(); i++) {
            Folder eachSub = (Folder) subFolders.elementAt(i);
            getSubFolderPaths(eachSub, paths, indent);
        }
    }
}
