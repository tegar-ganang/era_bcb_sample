package org.nees.rbnb;

import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import com.rbnb.sapi.*;

/**
 * @author Terry E Weymouth
 *
 */
public class TextChannelList extends RBNBBase {

    Sink sink = null;

    ChannelMap sMap;

    boolean connected = false;

    boolean includeHidden = false;

    boolean sourceOnly = false;

    public static void main(String[] args) {
        TextChannelList cl = new TextChannelList();
        if (cl.parseArgs(args)) cl.exec(); else {
            System.out.println("Parse of argument list failed.");
            System.exit(1);
        }
    }

    protected String getCVSVersionString() {
        return "  CVS information... \n" + "  $Revision: 36 $\n" + "  $Date: 2008-04-15 20:12:19 -0400 (Tue, 15 Apr 2008) $\n" + "  $RCSfile: TextChannelList.java,v $ \n";
    }

    private void exec() {
        connect();
        if (!connected) return;
        ChannelTree t = getChannelTree("/...");
        Iterator i = t.rootIterator();
        int level = 0;
        if (!i.hasNext()) {
            printlnAtLevel(level, "[empty root list]");
            System.exit(1);
        }
        ChannelTree.Node node = (ChannelTree.Node) i.next();
        if (!node.getType().toString().equals("Server")) {
            printlnAtLevel(level, "[unexpected non-Server node type = " + node.getType() + "]");
            System.exit(1);
        }
        String serverName = node.getName();
        printServerNode(level, serverName);
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        opt.addOption("I", false, "Include hidden channels.");
        opt.addOption("S", false, "Include source channels only.");
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) {
        if (!setBaseArgs(cmd)) return false;
        if (cmd.hasOption('I')) {
            includeHidden = true;
        }
        return true;
    }

    public void connect() {
        try {
            sink = new Sink();
            sink.OpenRBNBConnection(getServer(), "ChannelListRequest");
            connected = true;
        } catch (SAPIException se) {
            System.out.println("Can not connect to server: " + getServer());
            System.exit(1);
        }
    }

    private void printServerNode(int level, String serverName) {
        String pattern = serverName + "/...";
        if (level == 0) pattern = "/" + pattern;
        ChannelTree tr = getChannelTree(pattern);
        if (tr == null) {
            printlnAtLevel(level, "[no tree]");
            System.exit(1);
        }
        Iterator i = tr.rootIterator();
        if (!i.hasNext()) {
            printlnAtLevel(level, "[empty root list]");
            System.exit(1);
        }
        ChannelTree.Node node = (ChannelTree.Node) i.next();
        if (!node.getType().toString().equals("Server")) {
            printlnAtLevel(level, "[unexpected non-Server node type = " + node.getType() + "]");
            System.exit(1);
        }
        if (!node.getName().equals(serverName)) {
            printlnAtLevel(level, "[unexpected node name = " + node.getName() + "]");
            System.exit(1);
        }
        boolean process = ((!node.getName().startsWith("_")) || includeHidden) || (node.getType().toString().equals("Source") && sourceOnly);
        if (process) {
            if (node.getType().toString().equals("Channel")) printlnAtLevel(level, node.getFullName());
            printChildren(level + 1, node);
        }
    }

    private void printChildren(int level, ChannelTree.Node node) {
        List l = node.getChildren();
        Iterator i = l.iterator();
        while (i.hasNext()) {
            node = (ChannelTree.Node) i.next();
            boolean process = ((!node.getName().startsWith("_")) || includeHidden) || (node.getType().toString().equals("Source") && sourceOnly);
            if (node.getType().toString().equals("Channel")) printlnAtLevel(level, node.getFullName());
            if (node.getType().toString().equals("Server")) printServerNode(level + 1, node.getName()); else {
                if (process) printChildren(level + 1, node);
            }
        }
    }

    private ChannelTree getChannelTree(String pattern) {
        ChannelTree tr = null;
        try {
            sMap = new ChannelMap();
            if ((pattern != null) && (pattern.length() > 0)) sMap.Add(pattern);
            sink.RequestRegistration(sMap);
            sMap = sink.Fetch(-1, sMap);
            tr = ChannelTree.createFromChannelMap(sMap);
        } catch (SAPIException se) {
            se.printStackTrace();
        }
        return tr;
    }

    private void printlnAtLevel(int level, String line) {
        System.out.println(line);
    }
}
