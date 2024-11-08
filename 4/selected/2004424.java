package com.duroty.application.home.utils;

import java.io.Serializable;

/**
 * @author Jordi Marquès
 * @version 1.0
*/
public class FeedObj implements Serializable {

    /**
         *
         */
    private static final long serialVersionUID = 4508199681014805871L;

    /**
    * DOCUMENT ME!
    */
    private int idint;

    /**
     * DOCUMENT ME!
     */
    private String name;

    /**
     * DOCUMENT ME!
     */
    private String value;

    /**
     * DOCUMENT ME!
     */
    private int channel;

    /**
     *
     */
    public FeedObj() {
        super();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getChannel() {
        return channel;
    }

    /**
     * DOCUMENT ME!
     *
     * @param channel DOCUMENT ME!
     */
    public void setChannel(int channel) {
        this.channel = channel;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getIdint() {
        return idint;
    }

    /**
     * DOCUMENT ME!
     *
     * @param idint DOCUMENT ME!
     */
    public void setIdint(int idint) {
        this.idint = idint;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getName() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getValue() {
        return value;
    }

    /**
     * DOCUMENT ME!
     *
     * @param value DOCUMENT ME!
     */
    public void setValue(String value) {
        this.value = value;
    }
}
