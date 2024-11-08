package com.tirsen.hanoi.engine;

import java.util.*;

/**
 *
 *
 * <!-- $Id: DefaultQueue.java,v 1.1.1.1 2002/07/06 16:34:59 tirsen Exp $ -->
 * <!-- $Author: tirsen $ -->
 *
 * @author Jon Tirs&eacute;n (tirsen@users.sourceforge.net)
 * @version $Revision: 1.1.1.1 $
 */
public class DefaultQueue implements Queue {

    private List entries = new LinkedList();

    public static class Entry {

        private String instanceID;

        private String channelName;

        private Object message;

        public Entry(String instanceID, String channelName, Object message) {
            this.instanceID = instanceID;
            this.channelName = channelName;
            this.message = message;
        }

        public String getInstanceID() {
            return instanceID;
        }

        public String getChannelName() {
            return channelName;
        }

        public Object getMessage() {
            return message;
        }
    }

    public void enqueue(String instanceID, String channelName, Object message) {
        entries.add(new Entry(instanceID, channelName, message));
    }

    public Entry dequeue() {
        Entry entry;
        if (entries.isEmpty()) {
            entry = null;
        } else {
            entry = (Entry) entries.get(0);
            entries.remove(0);
        }
        return entry;
    }

    public Object peek(String instanceID, String channelName) {
        for (Iterator iterator = entries.iterator(); iterator.hasNext(); ) {
            Entry entry = (Entry) iterator.next();
            if (entry.getInstanceID() == null ? instanceID == null : entry.getInstanceID().equals(instanceID) && entry.getChannelName().equals(channelName)) {
                return entry.getMessage();
            }
        }
        return null;
    }

    public Object dequeue(String instanceID, String channelName) {
        Object message = peek(instanceID, channelName);
        if (message != null) {
            remove(message);
        }
        return message;
    }

    public void remove(Object message) {
        for (ListIterator iterator = entries.listIterator(); iterator.hasNext(); ) {
            Entry entry = (Entry) iterator.next();
            if (entry.getMessage() == message) {
                iterator.remove();
                break;
            }
        }
    }
}
