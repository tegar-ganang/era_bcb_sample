package com.protomatter.syslog;

import java.io.PrintWriter;
import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;
import org.jdom.*;
import com.protomatter.util.*;
import com.protomatter.xml.*;

/**
 *  The default LogPolicy that knows about log levels and channels.
 *
 *  @see LogPolicy
 */
public class SimpleLogPolicy implements LogPolicy {

    private int logMask = Syslog.INHERIT_MASK;

    private HashMap channels = null;

    private boolean allChannels = false;

    private boolean initialized = false;

    private Object VALUE = new Object();

    /**
   *  All channels are listened to by default, and the
   *  default log mask is inherited from Syslog itself.
   */
    public SimpleLogPolicy() {
        super();
        Set channels = new HashSet();
        channels.add(Syslog.ALL_CHANNEL);
        setChannels(channels);
    }

    /**
   *  Set the list of channels to use.  Channel names
   *  are values in the vector.
   */
    public synchronized void setChannels(List channels) {
        Set set = new HashSet();
        Iterator i = channels.iterator();
        while (i.hasNext()) {
            set.add(i.next());
        }
        setChannels(set);
    }

    /**
   *  Set the list of channels to use.  Channel names are
   *  looked for as keys in the hashtable.
   */
    public synchronized void setChannels(Set channelSet) {
        this.channels = new HashMap();
        Iterator i = channelSet.iterator();
        while (i.hasNext()) {
            channels.put(i.next(), VALUE);
        }
        this.allChannels = channelSet.contains(Syslog.ALL_CHANNEL);
    }

    /**
   *  Add the given channel to the list of channels we
   *  are listening to.
   */
    public synchronized void addChannel(String channel) {
        channels.put(channel, VALUE);
        this.allChannels = channels.containsKey(Syslog.ALL_CHANNEL);
    }

    /**
   *  Remove the given channel from the list of channels
   *  we are listening to.
   */
    public synchronized void removeChannel(String channel) {
        channels.remove(channel);
        this.allChannels = channels.containsKey(Syslog.ALL_CHANNEL);
    }

    /**
   *  Remove all channels from the list of channels
   *  we are listening to.
   */
    public synchronized void removeAllChannels() {
        channels = new HashMap();
        this.allChannels = false;
    }

    /**
   *  Get the list of channels this policy listens to.
   */
    public Iterator getChannels() {
        return channels.keySet().iterator();
    }

    /** 
   *  Determine if a log message should be logged given the information.
   *  Only checks to see if the log level is in the mask.
   */
    public boolean shouldLog(SyslogMessage message) {
        boolean inMask = false;
        if (this.logMask == Syslog.INHERIT_MASK) inMask = ((Syslog.currentLogMask & (1 << message.level)) != 0); else inMask = ((this.logMask & (1 << message.level)) != 0);
        if (!inMask) return false;
        if (!allChannels) {
            if (Syslog.ALL_CHANNEL.equals(message.channel)) return true;
            return channels.containsKey(message.channel);
        }
        return true;
    }

    /**
   *  Check if the given level is covered by the given mask.
   */
    protected final boolean inMask(int level, int mask) {
        return ((mask & (1 << level)) != 0);
    }

    /**
   *  Set the mask to at or above the level specified.
   */
    public final void setLogMask(String minLevel) {
        if (minLevel.equals("DEBUG")) this.setLogMask(Syslog.atOrAbove(Syslog.DEBUG)); else if (minLevel.equals("INFO")) this.setLogMask(Syslog.atOrAbove(Syslog.INFO)); else if (minLevel.equals("WARNING")) this.setLogMask(Syslog.atOrAbove(Syslog.WARNING)); else if (minLevel.equals("ERROR")) this.setLogMask(Syslog.atOrAbove(Syslog.ERROR)); else if (minLevel.equals("FATAL")) this.setLogMask(Syslog.atOrAbove(Syslog.FATAL)); else if (minLevel.equals("INHERIT_MASK")) this.setLogMask(Syslog.INHERIT_MASK); else throw new IllegalArgumentException("Invalid syslog level string");
    }

    /**
    * Set the mask for logging of messages.
    * For example, to log all messages of type ERROR or greater,
    * you would call:
    *
    *   setLogMask(Syslog.atOrAbove(Syslog.ERROR));
    */
    public final void setLogMask(int mask) {
        this.logMask = mask;
    }

    /**
   *  Get the mask for logging of messages.
   */
    public final int getLogMask() {
        return this.logMask;
    }

    /**
   *  Configure this policy given the XML element.
   *  The <tt>&lt;Policy&gt;</tt> element should look like this:<P>
   *
   *  <TABLE BORDER=1 CELLPADDING=4 CELLSPACING=0 WIDTH="90%">
   *  <TR><TD>
   *  <PRE><B>
   *
   *  &lt;Policy class="<i>PolicyClassName</i>" &gt;
   *
   *    &lt;logMask&gt;<i>LogMask</i>&lt;/logMask&gt;
   *    &lt;channels&gt;<i>ChannelList</i>&lt;/channels&gt;
   *
   *  &lt;/Policy&gt;
   *  </B></PRE>
   *  </TD></TR></TABLE><P>
   *
   *  This class reads the "<tt>logMask</tt>" and "<tt>channels</tt>"
   *  elements.<P>
   *
   *  <TABLE BORDER=1 CELLPADDING=2 CELLSPACING=0 WIDTH="90%">
   *  <TR CLASS="TableHeadingColor">
   *  <TD COLSPAN=3><B>Element</B></TD>
   *  </TR>
   *  <TR CLASS="TableHeadingColor">
   *  <TD><B>name</B></TD>
   *  <TD><B>value</B></TD>
   *  <TD><B>required</B></TD>
   *  </TR>
   *
   *  <TR CLASS="TableRowColor">
   *  <TD VALIGN=TOP><TT>logMask</TT></TD>
   *  <TD>A log mask string.  Integers are treated as raw masks
   *      and log level names (<TT>DEBUG</TT>, <TT>INFO</TT>,
   *      <tt>WARNING</TT>, <TT>ERROR</TT> and <TT>FATAL</TT>)
   *      are interpreted as at-or-above the given level.
   *  </TD>
   *  <TD VALIGN=TOP>no (default is <tt>INHERIT_MASK</TT>)</TD>
   *  </TR>
   *
   *  <TR CLASS="TableRowColor">
   *  <TD VALIGN=TOP><TT>channels</TT></TD>
   *  <TD>A comma and/or space separated list of channel names.
   *      The constants <TT>DEFAULT_CHANNEL</TT> and <TT>ALL_CHANNEL</TT>
   *      are interpreted as their symbolic values.
   *  </TD>
   *  <TD VALIGN=TOP>no (default is <tt>ALL_CHANNEL</TT>)</TD>
   *  </TR>
   *
   *  </TABLE><P>
   */
    public void configure(Element e) {
        if (e == null) return;
        String tmp = e.getChildTextTrim("logMask", e.getNamespace());
        if (tmp != null) {
            setLogMask(tmp);
        }
        tmp = e.getChildTextTrim("channels", e.getNamespace());
        if (tmp == null) tmp = "";
        removeAllChannels();
        StringTokenizer st = new StringTokenizer(tmp, ", ");
        while (st.hasMoreTokens()) {
            String chan = st.nextToken();
            if (chan.equals("ALL_CHANNEL")) addChannel(Syslog.ALL_CHANNEL); else if (chan.equals("DEFAULT_CHANNEL")) addChannel(Syslog.DEFAULT_CHANNEL); else addChannel(chan);
        }
    }

    /**
   *
   */
    public Element getConfiguration(Element element) {
        Element param = new Element("channels");
        if (allChannels) {
            param.setText("ALL_CHANNEL");
        } else {
            StringBuffer channelList = new StringBuffer();
            Iterator i = channels.keySet().iterator();
            while (i.hasNext()) {
                channelList.append(i.next());
                if (i.hasNext()) channelList.append(", ");
            }
            param.setText(channelList.toString());
        }
        element.addChild(param);
        element.addChild((new Element("logMask")).setText(getLogMaskAsString()));
        return element;
    }

    private String getLogMaskAsString() {
        if (logMask == Syslog.INHERIT_MASK) return "INHERIT_MASK"; else if (logMask == Syslog.atOrAbove(Syslog.DEBUG)) return "DEBUG"; else if (logMask == Syslog.atOrAbove(Syslog.INFO)) return "INFO"; else if (logMask == Syslog.atOrAbove(Syslog.WARNING)) return "WARNING"; else if (logMask == Syslog.atOrAbove(Syslog.ERROR)) return "ERROR"; else if (logMask == Syslog.atOrAbove(Syslog.FATAL)) return "FATAL";
        Vector list = new Vector(5);
        if (inMask(logMask, Syslog.DEBUG)) list.addElement("DEBUG");
        if (inMask(logMask, Syslog.INFO)) list.addElement("INFO");
        if (inMask(logMask, Syslog.WARNING)) list.addElement("WARNING");
        if (inMask(logMask, Syslog.ERROR)) list.addElement("ERROR");
        if (inMask(logMask, Syslog.FATAL)) list.addElement("FATAL");
        StringBuffer b = new StringBuffer(32);
        Enumeration e = list.elements();
        while (e.hasMoreElements()) {
            b.append(e.nextElement());
            if (e.hasMoreElements()) b.append(",");
        }
        return b.toString();
    }
}
