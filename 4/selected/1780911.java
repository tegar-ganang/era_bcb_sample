package com.markpiper.tvtray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.markpiper.tvtray.gui.ProgressListener;

/**
 * 
 * 
 * The ChannelManager is the data model for the application. The CM is
 * responsible for fetching the data from XML feeds, and parsing it into Channel
 * and Programme objects. It includes methods that allow the GUI to fetch
 * Channel and Programme data from the model.
 * 
 * The manager will run as a separate thread in the background when fetching
 * data.
 * 
 * @author Mark Piper
 */
public final class ChannelManager implements Runnable, Cloneable, NowAndNextModel, Serializable {

    private Vector channels;

    private String baseURL;

    private boolean hideNotOnAir;

    private transient boolean updating = false;

    private transient Vector cListeners;

    private transient Vector pListeners;

    private int FETCH_DELAY = 2000;

    /**
     * Constructor for the Channel Manager 
     *  
     */
    public ChannelManager() {
        channels = new Vector();
    }

    /**
     * Adds a channel based on the XML TV feed at the specified URL 
     * 
     * @param URL -
     *            the url of the XML TV feed
     */
    public void addChannel(String URL, String alias) {
        try {
            Channel ch = new Channel(URL, alias);
            channels.add(ch);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, "The URL provided is not valid", "TVTray Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Adds the specified channel to the ChannelManager
     * 
     * @param ch
     */
    public void addChannel(Channel ch) {
        channels.add(ch);
    }

    /**
     * Returns a Channel object corresponding to the given name
     * 
     * @param name the name of the channel to return
     * @return Channel
     */
    public Channel getChannel(String name) {
        for (Iterator i = channels.iterator(); i.hasNext(); ) {
            Channel c = (Channel) i.next();
            if (name.equals(c.getName())) return c;
        }
        return null;
    }

    /**
     * Returns a vector of programmes from all channels that are on at the
     * specified time
     * 
     * @param t -
     *            the time to consider, as a Calendar
     * @return The programmes on at time t
     */
    public Vector getAllByTime(Calendar t) {
        Vector out = new Vector();
        for (Iterator i = channels.iterator(); i.hasNext(); ) {
            Channel ch = (Channel) i.next();
            Programme currentProg = null;
            for (Iterator ip = ch.getProgrammes().iterator(); ip.hasNext(); ) {
                Programme p = (Programme) ip.next();
                if (p.isOnAt(t)) {
                    currentProg = p;
                    break;
                }
            }
            out.add(currentProg);
        }
        return out;
    }

    /**
     * Used to request that the ChannelManager should check it's channels and
     * do any necessary updating
     *
     */
    public void update() {
        if (updating == false) {
            updating = true;
        } else {
            return;
        }
        Thread th = new Thread(RuntimeHandler.getInstance(), this);
        th.start();
    }

    /**
     * Adds a ChannelManagerListener to this object
     *  
     * @param l the ChannelManagerListener to add
     */
    public void addChannelListener(ChannelManagerListener l) {
        if (cListeners == null) cListeners = new Vector();
        cListeners.add(l);
    }

    public void addProgressListener(ProgressListener p) {
        if (pListeners == null) pListeners = new Vector();
        pListeners.add(p);
    }

    /** 
     * Swaps the order of two Channels (at indices i and j) in the Channel Manager
     * 
     * @param i	first index
     * @param j	second index
     */
    public void changeOrder(int i, int j) {
        Channel tmp = (Channel) channels.elementAt(i);
        channels.setElementAt(channels.elementAt(j), i);
        channels.setElementAt(tmp, j);
    }

    /**
     * Persists the baseURL and channel aliases to a props file
     *
     */
    public void writeProperties() {
        Properties channelInfo = new Properties();
        try {
            channelInfo.load(new FileInputStream(new File("channels.txt")));
            channelInfo.setProperty("baseurl", getBaseURL());
            channelInfo.setProperty("hideNOA", (isHidingNotOnAir() == true ? "true" : "false"));
            for (Iterator i = channels.iterator(); i.hasNext(); ) {
                Channel ch = (Channel) i.next();
                channelInfo.setProperty("channel." + ch.getName(), ch.getAlias());
            }
            channelInfo.store(new FileOutputStream(new File("channels.txt")), "TVTray Saved");
        } catch (IOException e) {
        }
    }

    public boolean isHidingNotOnAir() {
        return hideNotOnAir;
    }

    public void hideNotOnAir(boolean noa) {
        hideNotOnAir = noa;
    }

    /**
     * Run method to start a Thread to fetch channels
     *  
     */
    public void run() {
        updateChannels(getChannelsForUpdate(false));
        updating = false;
    }

    /**
     * Returns a vector of all Channels in the ChannelManager
     * 
     * @return Vector of Channel objects
     */
    public Vector getChannels() {
        return channels;
    }

    /**
     * Method called by Preferences window to confirm updates to 
     * channel model
     *
     */
    protected void setChannels(Vector newChannels) {
        channels = newChannels;
    }

    public String getBaseURL() {
        return baseURL;
    }

    protected void setBaseURL(String newBase) {
        baseURL = newBase;
    }

    /**
     * Return the programme that is on at the specified time on the given
     * channel
     * 
     * @param channelName
     *            the name of the channel
     * @param t 
     *            the time to consider
     * @return The programme that is on channelName at the specified time
     */
    public Programme getNowProgramme(String channelName, Calendar t) {
        Channel chan = getChannel(channelName);
        Programme prog = chan.getByTime(t);
        return prog;
    }

    /** 
     * Returns the number of channels currently managed by the Channel Manager
     * 
     * @return the number of channels available
     */
    public int getNumberOfChannels() {
        return channels.size();
    }

    public int getNumberOfActiveChannels() {
        int count = 0;
        for (Iterator i = channels.iterator(); i.hasNext(); ) {
            Channel ch = (Channel) i.next();
            if (ch.isActive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Fires the channelsLoaded event
     * @param ok
     */
    private void fireChannelsLoaded(boolean ok) {
        if (cListeners != null) {
            for (Iterator i = cListeners.iterator(); i.hasNext(); ) {
                ChannelManagerListener chl = (ChannelManagerListener) i.next();
                chl.channelsLoaded(ok);
            }
        }
    }

    private void fireFetchingChannels() {
        if (cListeners != null) {
            for (Iterator i = cListeners.iterator(); i.hasNext(); ) {
                ChannelManagerListener chl = (ChannelManagerListener) i.next();
                chl.fetchingChannels();
            }
        }
    }

    private void fireFetchProgress(int progress) {
        if (pListeners != null) {
            for (Iterator i = pListeners.iterator(); i.hasNext(); ) {
                ProgressListener chl = (ProgressListener) i.next();
                chl.fetchingProgress(progress);
            }
        }
    }

    private void fireErrorOccurred(Channel c, Exception e) {
        if (pListeners != null) {
            for (Iterator i = pListeners.iterator(); i.hasNext(); ) {
                ProgressListener chl = (ProgressListener) i.next();
                chl.errorOccurred(c, e);
            }
        }
    }

    /**
     * Returns a vector of Channels which require update - either because they are not yet present in the 
     * ChannelManager, or because the listings are out of date
     * 
     * @param force Indicates whether channels should be loaded regardless of the last update date
     */
    private Vector getChannelsForUpdate(boolean force) {
        Vector updateChannels = new Vector();
        Properties channelInfo = new Properties();
        try {
            channelInfo.load(new FileInputStream(new File("channels.txt")));
            baseURL = channelInfo.getProperty("baseurl");
            hideNotOnAir = (channelInfo.getProperty("hideNOA").equals("true") ? true : false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot load channels list", "TVTray Error", JOptionPane.ERROR_MESSAGE);
            fireChannelsLoaded(false);
            return null;
        }
        ArrayList channelNames = new ArrayList();
        for (Enumeration en = channelInfo.propertyNames(); en.hasMoreElements(); ) {
            String ch = (String) en.nextElement();
            if (ch.length() > 8 && ch.substring(0, 8).equals("channel.")) {
                channelNames.add(ch.substring(ch.indexOf(".") + 1, ch.length()));
            }
        }
        for (Iterator i = channelNames.iterator(); i.hasNext(); ) {
            String chanName = (String) i.next();
            Channel chan = getChannel(chanName);
            if (chan == null) {
                String channelAlias = channelInfo.getProperty("channel." + chanName);
                try {
                    Channel ch = new Channel(baseURL + chanName + ".xml", channelAlias);
                    addChannel(ch);
                    updateChannels.add(ch);
                } catch (MalformedURLException e) {
                    JOptionPane.showMessageDialog(null, "The specified URL for " + channelAlias + "(" + baseURL + chanName + ".xml) is not valid", "TVTray Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (force || (chan.isActive() && chan.requiresUpdate())) {
                    updateChannels.add(chan);
                }
            }
        }
        return updateChannels;
    }

    public void updateChannels(Vector updateChannels) {
        boolean loaded = false;
        int progress = 0;
        if (updateChannels.size() != 0) {
            fireFetchingChannels();
            for (Iterator i = updateChannels.iterator(); i.hasNext(); ) {
                Channel ch = (Channel) i.next();
                try {
                    ch.update(false);
                    loaded = true;
                } catch (Exception e) {
                    fireErrorOccurred(ch, e);
                } finally {
                    try {
                        Thread.sleep(FETCH_DELAY);
                    } catch (InterruptedException e) {
                    }
                }
                progress++;
                double pc = ((double) progress / updateChannels.size()) * 100;
                fireFetchProgress((int) pc);
            }
        } else {
            loaded = true;
        }
        fireChannelsLoaded(loaded);
    }

    protected Channel getChannelByOrder(int num) {
        return (Channel) channels.elementAt(num);
    }

    public Object clone() {
        Object cl = null;
        Channel chclone = null;
        try {
            cl = super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        ((ChannelManager) cl).setChannels(new Vector());
        for (Iterator i = channels.iterator(); i.hasNext(); ) {
            Channel ch = (Channel) i.next();
            try {
                chclone = (Channel) ch.clone();
            } catch (CloneNotSupportedException e) {
            }
            ((ChannelManager) cl).getChannels().add(chclone);
        }
        return cl;
    }
}
