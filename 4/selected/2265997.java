package com.rbnb.plot;

import java.util.Hashtable;

public class ConfigCubby {

    private Channel[] channels = null;

    private boolean newChannels = false;

    private Channel channel = null;

    private boolean newChannel = false;

    private Hashtable ht = null;

    private boolean newHash = false;

    public synchronized void setChannels(Channel[] ch) {
        while (newChannels) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("ConfigCubby.setChannels: exception");
                e.printStackTrace();
            }
        }
        channels = ch;
        newChannels = true;
        notifyAll();
    }

    public synchronized Channel[] getChannels() {
        while (!newChannels) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("ConfigCubby.getChannels: exception");
                e.printStackTrace();
            }
        }
        newChannels = false;
        notifyAll();
        return channels;
    }

    public synchronized void setChannel(Channel ch) {
        while (newChannel) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("ConfigCubby.setChannels: exception");
                e.printStackTrace();
            }
        }
        channel = ch;
        newChannel = true;
        notifyAll();
    }

    public synchronized Channel getChannel() {
        while (!newChannel) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("ConfigCubby.getChannels: exception");
                e.printStackTrace();
            }
        }
        newChannel = false;
        notifyAll();
        return channel;
    }

    public synchronized void setHash(Hashtable h) {
        while (newHash) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("ConfigCubby.setHash: exception");
                e.printStackTrace();
            }
        }
        ht = h;
        newHash = true;
        notifyAll();
    }

    public synchronized Hashtable getHash() {
        while (!newHash) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("ConfigCubby.getHash: exception");
                e.printStackTrace();
            }
        }
        newHash = false;
        notifyAll();
        return ht;
    }
}
