package com.tms.webservices.applications.xtvd;

import com.tms.webservices.applications.datatypes.AbstractDataType;
import com.tms.webservices.applications.datatypes.Date;

/**
 * A <code>bean</code> that represents a map record.  A map element
 * is a child of the <code>lineups/lineup</code> element.
 *
 * @author Rakesh Vidyadharan 19<sup><small>th</small></sup> February, 2004
 *
 * <p>Copyright 2004, Tribune Media Services</p>
 *
 * $Id: Map.java,v 1.1.1.1 2005/07/19 04:28:20 shawndr Exp $
 */
public class Map extends AbstractDataType {

    /**
   * A variable that represents the <code>station</code> value 
   * associated with a map.
   */
    private int station = 0;

    /**
   * A variable that represents the <code>channel</code> value 
   * associated with a map.
   *
   * @since ddclient version 1.3.1 (datatype changed from <code>int</code>
   * to <code>String</code>)
   */
    private String channel;

    /**
   * A variable that represents the <code>channelMinor</code> value 
   * associated with a map.
   */
    private int channelMinor = 0;

    /**
   * A variable that represents the <code>from</code> value 
   * associated with a map.
   */
    private Date from = null;

    /**
   * A variable that represents the <code>to</code> value 
   * associated with a map.
   */
    private Date to = null;

    /**
   * Default constructor.  Not particularly useful, except if you
   * wish to create an instance of the class, that will be re-used
   * to associate with different map records.
   */
    public Map() {
        super();
    }

    /**
   * Create a new instance of the class with the specified values for
   * {@link #station} and {@link #channel}.
   *
   * @param int station - The {@link #station} value to set.
   * @param String channel - The {@link #channel} value to set.
   */
    public Map(int station, String channel) {
        this();
        setStation(station);
        setChannel(channel);
    }

    /**
   * Create a new instance of the class with the specified values for
   * {@link #station}, {@link #channel} and {@link #channelMinor}.
   *
   * @param int station - The {@link #station} value to set.
   * @param String channel - The {@link #channel} value to set.
   * @param int channelMinor - The {@link #channelMinor} value to set.
   */
    public Map(int station, String channel, int channelMinor) {
        this();
        setStation(station);
        setChannel(channel);
        setChannelMinor(channelMinor);
    }

    /**
   * Create a new instace of the class and initialise the instance
   * variables with the specified values.
   *
   * @param int station - The {@link #station} value to set.
   * @param String channel - The {@link #channel} value to set.
   * @param int channelMinor - The {@link #channelMinor} value to set.
   * @param Date from - The {@link #from} value to set.
   * @param Date to - The {@link #to} value to set.
   */
    public Map(int station, String channel, int channelMinor, Date from, Date to) {
        this(station, channel);
        setChannelMinor(channelMinor);
        setFrom(from);
        setTo(to);
    }

    /**
   * Over-ridden implementation.  Return an <code>XML representation
   * </code> of the class fields in the same format as in the original
   * <code>XTVD document</code>.
   *
   * @return String - The XML representation of the map record.
   */
    public String toString() {
        StringBuffer buffer = new StringBuffer(64);
        buffer.append("<map station='").append(station);
        buffer.append("' channel='").append(channel);
        if (channelMinor != 0) {
            buffer.append("' channelMinor='").append(channelMinor);
        }
        if (from != null) {
            buffer.append("' from='").append(from.toString());
        }
        if (to != null) {
            buffer.append("' to='").append(to.toString());
        }
        buffer.append("'/>");
        return buffer.toString();
    }

    /**
   * Returns {@link #station}.
   *
   * @return int - The value/reference of/to station.
   */
    public final int getStation() {
        return station;
    }

    /**
   * Set {@link #station}.
   *
   * @param int station - The value to set.
   */
    public final void setStation(int station) {
        this.station = station;
    }

    /**
   * Returns {@link #channel}.
   *
   * @return String - The value/reference of/to channel.
   */
    public final String getChannel() {
        return channel;
    }

    /**
   * Set {@link #channel}.
   *
   * @param String channel - The value to set.
   */
    public final void setChannel(String channel) {
        this.channel = channel;
    }

    /**
   * Returns {@link #channelMinor}.
   *
   * @return int - The value/reference of/to channelMinor.
   */
    public final int getChannelMinor() {
        return channelMinor;
    }

    /**
   * Set {@link #channelMinor}.
   *
   * @param int channelMinor - The value to set.
   */
    public final void setChannelMinor(int channelMinor) {
        this.channelMinor = channelMinor;
    }

    /**
   * Returns {@link #from}.
   *
   * @return Date - The value/reference of/to from.
   */
    public final Date getFrom() {
        return from;
    }

    /**
   * Set {@link #from}.
   *
   * @param Date from - The value to set.
   */
    public final void setFrom(Date from) {
        this.from = from;
    }

    /**
   * Returns {@link #to}.
   *
   * @return Date - The value/reference of/to to.
   */
    public final Date getTo() {
        return to;
    }

    /**
   * Set {@link #to}.
   *
   * @param Date to - The value to set.
   */
    public final void setTo(Date to) {
        this.to = to;
    }
}
