package com.markpiper.tvtray;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import com.markpiper.tvtray.xml.ChannelXMLHandler;
import com.markpiper.tvtray.xml.ConnectionManager;

/**
 * The Channel class represents a TV channel, with a number of programmes within
 * it. Channels are individually managed by the ChannelManager.
 * 
 * @author Mark Piper
 * 
 */
public class Channel implements Iterable, Serializable, Cloneable {

    private URL channelURL;

    private String channelName;

    private String channelAlias;

    private Vector programmes;

    private Date lastUpdate;

    private boolean isActive;

    public Channel(String channelURL, String channelAlias) throws MalformedURLException {
        programmes = new Vector();
        this.channelURL = new URL(channelURL);
        this.channelAlias = channelAlias;
        isActive = true;
    }

    public void addProgramme(Programme prog) {
        programmes.add(prog);
    }

    public Vector getProgrammes() {
        return programmes;
    }

    public Programme getProgramme(int index) {
        return (Programme) programmes.elementAt(index);
    }

    public int getNumberOfProgrammes() {
        return programmes.size();
    }

    public String getName() {
        return channelName;
    }

    public void setName(String s) {
        channelName = s;
    }

    public URL getURL() {
        return channelURL;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date update) {
        lastUpdate = update;
    }

    public String getAlias() {
        return channelAlias;
    }

    public void setAlias(String alias) {
        channelAlias = alias;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
	 * Returns the programme that is on at the specified time
	 * 
	 * @param t -
	 *            the time to consider
	 * @return The programme on at the specified time
	 */
    public Programme getByTime(Calendar t) {
        for (Iterator i = programmes.iterator(); i.hasNext(); ) {
            Programme p = (Programme) i.next();
            if (p.isOnAt(t)) return p;
        }
        return null;
    }

    /**
	 * Returns the programme that is n programmes after the programme on at the
	 * specified time
	 * 
	 * @param t -
	 *            the time to consider
	 * @param n -
	 *            the number of programmes ahead
	 * @return The programme that is the nth after the programme on at the time
	 *         t
	 */
    public Programme getNext(Calendar t, int n) {
        Object[] progArray = programmes.toArray();
        if (getStartTime().after(t)) {
            return (Programme) progArray[0];
        }
        for (int i = 0; i < progArray.length; i++) {
            if (i + n >= progArray.length) {
                return null;
            }
            if (((Programme) progArray[i]).isOnAt(t)) {
                return (Programme) progArray[i + n];
            }
        }
        return null;
    }

    public String toString() {
        return channelName + " " + channelURL;
    }

    /**
	 * Returns a calendar representing the time/date of the first programme on
	 * this channel
	 * 
	 * @return the start time of the channel, as a calendar
	 */
    public Calendar getStartTime() {
        return ((Programme) programmes.get(0)).getStartCalendar();
    }

    /**
	 * Returns a calendar representing the time/date of the last programme on
	 * this channel
	 * 
	 * @return the end time of the channel, as a calendar
	 */
    public Calendar getEndTime() {
        return ((Programme) programmes.get(0)).getEndCalendar();
    }

    public Iterator iterator() {
        return programmes.iterator();
    }

    /**
	 * Clears out all programmes from the channel. Normally used in readiness to
	 * reload an xml file
	 * 
	 */
    public void clearProgrammes() {
        programmes.clear();
    }

    public boolean requiresUpdate() {
        Date today = new Date();
        DateFormat df = DateFormat.getDateInstance();
        boolean requiresUpdate = (lastUpdate != null ? !df.format(lastUpdate).equals(df.format(today)) : true);
        return requiresUpdate;
    }

    public void update(boolean force) throws IOException, SAXException, ParserConfigurationException {
        clearProgrammes();
        getProgrammeData();
        lastUpdate = new Date();
    }

    private void getProgrammeData() throws IOException, SAXException, ParserConfigurationException {
        InputStream is = ConnectionManager.getInputStream(channelURL.toString());
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser saxp = factory.newSAXParser();
        ChannelXMLHandler handler = new ChannelXMLHandler(this);
        saxp.parse(is, handler);
    }

    protected Object clone() throws CloneNotSupportedException {
        Object cl = null;
        try {
            cl = super.clone();
        } catch (CloneNotSupportedException e) {
            cl = null;
        }
        return cl;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}
