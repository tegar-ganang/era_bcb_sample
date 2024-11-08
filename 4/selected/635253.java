package org.nees.rbnb;

import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import com.rbnb.sapi.*;

/**
 * @author Terry E Weymouth
 *
 */
public class ChannelList extends RBNBBase {

    Sink sink = null;

    ChannelMap sMap;

    boolean connected = false;

    public static void main(String[] args) {
        ChannelList cl = new ChannelList();
        if (cl.parseArgs(args)) cl.exec();
    }

    protected String getCVSVersionString() {
        return "  CVS information... \n" + "  $Revision: 36 $\n" + "  $Date: 2008-04-15 20:12:19 -0400 (Tue, 15 Apr 2008) $\n" + "  $RCSfile: ChannelList.java,v $ \n";
    }

    private void exec() {
        connect();
        ChannelTree t = getChannelTree("/...");
        Iterator i = t.rootIterator();
        int level = 0;
        if (!i.hasNext()) {
            printlnAtLevel(level, "[empty root list]");
        }
        ChannelTree.Node node = (ChannelTree.Node) i.next();
        if (!node.getType().toString().equals("Server")) {
            printlnAtLevel(level, "[unexpected non-Server node type = " + node.getType() + "]");
            return;
        }
        String serverName = node.getName();
        printServerNode(level, serverName);
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) {
        if (!setBaseArgs(cmd)) return false;
        return true;
    }

    public void connect() {
        try {
            sink = new Sink();
            sink.OpenRBNBConnection(getServer(), "ChannelListRequest");
            connected = true;
            System.out.println("ChannelList: Connection made to server = " + getServer() + " requesting channel list.");
        } catch (SAPIException se) {
            se.printStackTrace();
        }
    }

    private void printServerNode(int level, String serverName) {
        printlnAtLevel(level, "[Printing list for Server = " + serverName + "]");
        String pattern = serverName + "/...";
        if (level == 0) pattern = "/" + pattern;
        printlnAtLevel(level, "[Pattern = " + pattern + "]");
        ChannelTree tr = getChannelTree(pattern);
        if (tr == null) {
            printlnAtLevel(level, "[no tree]");
            return;
        }
        Iterator i = tr.rootIterator();
        if (!i.hasNext()) {
            printlnAtLevel(level, "[empty root list]");
            return;
        }
        ChannelTree.Node node = (ChannelTree.Node) i.next();
        if (!node.getType().toString().equals("Server")) {
            printlnAtLevel(level, "[unexpected non-Server node type = " + node.getType() + "]");
            return;
        }
        if (!node.getName().equals(serverName)) {
            printlnAtLevel(level, "[unexpected node name = " + node.getName() + "]");
            return;
        }
        printlnAtLevel(level, node.getName() + " -- " + node.getFullName() + " (" + node.getType() + ")");
        printChildren(level + 1, node);
    }

    private void printChildren(int level, ChannelTree.Node node) {
        List l = node.getChildren();
        Iterator i = l.iterator();
        while (i.hasNext()) {
            node = (ChannelTree.Node) i.next();
            printlnAtLevel(level, node.getName() + " -- " + node.getFullName() + " (" + node.getType() + ")");
            if (node.getType().toString().equals("Server")) printServerNode(level + 1, node.getName()); else {
                printChildren(level + 1, node);
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
        for (int i = 0; i < level; i++) System.out.print("+");
        System.out.println(line);
    }
}
