package de.z8bn.ircg;

import java.util.*;

/**
 *
 * @author Administrator
 */
public class FservInf {

    public LinkedList<FFile> files = new LinkedList<FFile>();

    public LinkedList<FFile> rootfiles = new LinkedList<FFile>();

    public int maxqueue, maxsend, curqueue, cursend;

    public Transfer[] myqueues;

    public Transfer[] mysends;

    public Trigger trigger;

    public String toString() {
        StringBuffer bf = new StringBuffer();
        try {
            bf.append("---------\n");
            bf.append("Fserv ").append(trigger.getTrigger()).append(" on ").append(trigger.getUser()).append("@").append(trigger.getChannel()).append("\n");
            bf.append("Queues: (").append(curqueue).append("/").append(maxqueue).append("), Sends: (").append(cursend).append("/").append(maxsend).append(")\n");
            bf.append("Files on Server: ").append(files.size()).append("\n");
            bf.append("# own files in queue: ").append(myqueues.length).append(" / in send: ").append(mysends.length).append("\n");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return bf.toString();
    }
}
