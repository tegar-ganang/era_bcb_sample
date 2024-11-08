package org.nees.rbnb;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JSplitPane;
import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import com.rbnb.sapi.*;

/**
 * This Applicaiton create an RBNB channel list from the RBNB server specified on the
 * command line. The command line argument -s specifies the server and -p the port.
 * The defaults are localhost and 3333, respectively. The channel list is refreshed
 * automatically. Click on a node in the list to see it's 
 * 
 * @author Terry E Weymouth
 *
 */
public class ChannelListSwing extends JPanel implements TreeSelectionListener {

    private static final String SERVER_NAME = "localhost";

    private static final String SERVER_PORT = "3333";

    private String serverName = SERVER_NAME;

    private String serverPort = SERVER_PORT;

    private String server = serverName + ":" + serverPort;

    private String optionNotes = null;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm:ss.SSS aa");

    private static final TimeZone TZ = TimeZone.getTimeZone("GMT");

    static {
        DATE_FORMAT.setTimeZone(TZ);
    }

    private Sink sink = null;

    private ChannelMap sMap;

    private boolean connected = false;

    private JTree tree;

    private JEditorPane htmlPane;

    private boolean needsRefresh = false;

    private Timer checkRefresh;

    private Thread timerThread;

    private boolean runit = false;

    public static void main(String[] args) {
        final String[] a = args;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ChannelListSwing c = new ChannelListSwing();
                if (c.parseArgs(a)) {
                    c.connect();
                    c.createAndShowGUI();
                    c.setTimer();
                    c.startCheckThread();
                }
            }
        });
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) {
        if (!setBaseArgs(cmd)) return false;
        System.out.println("Starting ChannelList on " + getServer());
        System.out.println("  Use ChannelList -h to see optional parameters");
        return true;
    }

    public void setNameAndPort(String name, String port) {
        serverName = name;
        serverPort = port;
    }

    protected void createAndShowGUI() {
        JFrame frame = new JFrame("Channel List");
        initGraphics();
        setOpaque(true);
        frame.setContentPane(this);
        frame.pack();
        frame.setVisible(true);
    }

    protected void initGraphics() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        DefaultMutableTreeNode top = createNodes();
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        JScrollPane treeView = new JScrollPane(tree);
        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        JScrollPane htmlView = new JScrollPane(htmlPane);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(treeView);
        splitPane.setBottomComponent(htmlView);
        Dimension minimumSize = new Dimension(100, 50);
        htmlView.setMinimumSize(minimumSize);
        treeView.setMinimumSize(minimumSize);
        splitPane.setDividerLocation(100);
        splitPane.setPreferredSize(new Dimension(500, 300));
        add(splitPane);
        JFrame itsFrame = new JFrame("ChannelList");
        itsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        itsFrame.setSize(new Dimension(120, 40));
        itsFrame.getContentPane().add(this, BorderLayout.CENTER);
        itsFrame.pack();
        itsFrame.setVisible(true);
    }

    protected void setTimer() {
        int delay = 100;
        ActionListener taskPerformer = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (needsRefresh) {
                    DefaultMutableTreeNode top = createNodes();
                    DefaultTreeModel model = new DefaultTreeModel(top);
                    tree.setModel(model);
                    invalidate();
                    needsRefresh = false;
                }
            }
        };
        checkRefresh = new Timer(delay, taskPerformer);
        checkRefresh.start();
    }

    private DefaultMutableTreeNode createNodes() {
        DefaultMutableTreeNode top;
        if (!connected) top = new DefaultMutableTreeNode("UNCONNECTED (attempting to connect to " + getServer() + ")"); else {
            top = new DefaultMutableTreeNode("Connected to " + getServer());
            ChannelTree ct = getChannelTree();
            if (ct == null) {
                top = new DefaultMutableTreeNode("No Channel Tree (connection dropped?)");
            } else {
                Iterator i = ct.rootIterator();
                while (i.hasNext()) {
                    top.add(makeNodes((ChannelTree.Node) i.next()));
                }
            }
        }
        return top;
    }

    private DefaultMutableTreeNode makeNodes(ChannelTree.Node node) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeCover(node));
        List l = node.getChildren();
        Iterator i = l.iterator();
        while (i.hasNext()) {
            root.add(makeNodes((ChannelTree.Node) i.next()));
        }
        return root;
    }

    /** Required by TreeSelectionListener interface. */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof NodeCover) {
            NodeCover nc = (NodeCover) nodeInfo;
            ChannelTree.Node ctNode = nc.node;
            displayMetaData(ctNode);
        } else {
            htmlPane.setText(nodeInfo.toString());
        }
    }

    protected void connect() {
        try {
            sink = new Sink();
            sink.OpenRBNBConnection(getServer(), "ChannelListRequest");
            connected = true;
            System.out.println("ChannelList: Connection made to server = " + getServer() + " requesting channel list.");
        } catch (SAPIException se) {
            System.out.println("ChannelList: Cannot connect to " + getServer() + "; exception = " + se);
        }
    }

    private ChannelTree getChannelTree() {
        try {
            sMap = new ChannelMap();
            sink.RequestRegistration();
            sMap = sink.Fetch(-1, sMap);
            return ChannelTree.createFromChannelMap(sMap);
        } catch (SAPIException se) {
            System.out.println("ChannelList: get channel tree failed. Reconnect?");
            System.out.println("Exception = " + se);
            connected = false;
        }
        return null;
    }

    private ChannelTree getDecendentChannelTree(String pattern) {
        try {
            sMap = new ChannelMap();
            sMap.Add(pattern);
            sink.RequestRegistration();
            sMap = sink.Fetch(-1, sMap);
            return ChannelTree.createFromChannelMap(sMap);
        } catch (SAPIException se) {
            System.out.println("ChannelList: get decendent channel tree failed. Reconnect?");
            System.out.println("Exception = " + se);
            connected = false;
        }
        return null;
    }

    private void displayMetaData(ChannelTree.Node node) {
        double start = node.getStart();
        double duration = node.getDuration();
        double end = start + duration;
        long unixTime = (long) (start * 1000.0);
        String startTime = DATE_FORMAT.format(new Date(unixTime));
        unixTime = (long) (end * 1000.0);
        String endTime = DATE_FORMAT.format(new Date(unixTime));
        if (((node.getType()).toString()).equals("Channel")) htmlPane.setText("FullName = " + node.getFullName() + "\n" + "Time (start, duration) = " + start + ", " + duration + "\n" + "[Assuming \"standard\" time:" + "\n" + "    from " + startTime + "\n" + "    to " + endTime + "]\n" + "Type = " + node.getType() + "\n" + "Mime type = " + node.getMime() + "\n" + "Size = " + node.getSize()); else htmlPane.setText("FullName = " + node.getFullName() + "\n" + "Type = " + node.getType() + "\n");
    }

    protected void startCheckThread() {
        Runnable r = new Runnable() {

            public void run() {
                runWork();
            }
        };
        runit = true;
        timerThread = new Thread(r, "Timer");
        timerThread.start();
        System.out.println("Started checker thread.");
    }

    private void stopThread() {
        runit = false;
        timerThread.interrupt();
        System.out.println("Stopped checker thread.");
    }

    private void runWork() {
        int delay = 10000;
        while (runit) {
            if (!connected) {
                delay = 10000;
                connect();
                if (connected) delay = 1000;
            } else if (!sameTree(tree)) {
                System.out.println("Found mismatched tree");
                needsRefresh = true;
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignore) {
            }
        }
        timerThread = null;
    }

    private boolean sameTree(JTree tree) {
        TreeModel m = tree.getModel();
        ChannelTree ct = getChannelTree();
        if (ct == null) return false;
        return sameTreeNodes(ct.rootIterator(), (DefaultMutableTreeNode) m.getRoot());
    }

    private boolean sameTreeNodes(Iterator i, DefaultMutableTreeNode tree) {
        if (tree.isLeaf() && !i.hasNext()) {
            return true;
        }
        if (tree.isLeaf() && i.hasNext()) {
            return false;
        }
        if (!tree.isLeaf() && !i.hasNext()) {
            return false;
        }
        Vector nodeList = new Vector();
        int count = tree.getChildCount();
        for (int k = 0; k < count; k++) {
            nodeList.add(tree.getChildAt(k));
        }
        while (i.hasNext()) {
            ChannelTree.Node n = (ChannelTree.Node) i.next();
            Enumeration e = nodeList.elements();
            DefaultMutableTreeNode found = null;
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode test = (DefaultMutableTreeNode) e.nextElement();
                Object nodeInfo = test.getUserObject();
                NodeCover nc = (NodeCover) nodeInfo;
                ChannelTree.Node ctNode = nc.node;
                if (n.getName().equals(ctNode.getName())) {
                    found = test;
                    break;
                }
            }
            if (found == null) return false;
            nodeList.remove(found);
            if (!sameTreeNodes(n.getChildren().iterator(), found)) return false;
        }
        if (nodeList.size() > 0) {
            return false;
        }
        return true;
    }

    private class NodeCover {

        ChannelTree.Node node;

        NodeCover(ChannelTree.Node node) {
            this.node = node;
        }

        public String toString() {
            return node.getName();
        }
    }

    protected boolean parseArgs(String[] args) throws IllegalArgumentException {
        try {
            CommandLine cmd = (new PosixParser()).parse(setOptions(), args);
            return setArgs(cmd);
        } catch (Exception e) {
            throw new IllegalArgumentException("Argument Exception: " + e);
        }
    }

    protected boolean setBaseArgs(CommandLine cmd) {
        if (cmd.hasOption('h')) {
            printUsage();
            return false;
        }
        if (cmd.hasOption('s')) {
            String a = cmd.getOptionValue('s');
            if (a != null) setServerName(a);
        }
        if (cmd.hasOption('p')) {
            String a = cmd.getOptionValue('p');
            if (a != null) setServerPort(a);
        }
        return true;
    }

    /**
	 * @param name
	 */
    public void setServerName(String name) {
        serverName = name;
    }

    /**
	 * @param port
	 */
    public void setServerPort(String port) {
        serverPort = port;
    }

    public String getServer() {
        server = serverName + ":" + serverPort;
        return server;
    }

    protected void printUsage() {
        HelpFormatter f = new HelpFormatter();
        f.printHelp(this.getClass().getName(), setOptions());
        if (optionNotes != null) {
            System.out.println("Note: " + optionNotes);
        }
    }

    protected Options setBaseOptions(Options opt) {
        opt.addOption("h", false, "Print help");
        opt.addOption("s", true, "Server Hostname *" + SERVER_NAME);
        opt.addOption("p", true, "Server Port Number *" + SERVER_PORT);
        return opt;
    }

    protected void setNotes(String n) {
        optionNotes = n;
    }
}
