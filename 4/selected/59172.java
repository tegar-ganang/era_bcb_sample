package com.museum4j.modelo;

import java.util.*;
import java.io.Serializable;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class RSS implements Serializable {

    private Logger logger = Logger.getLogger(this.getClass());

    private String version = "2.0";

    private RSSChannel channel;

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String v) {
        this.version = v;
    }

    public RSSChannel getChannel() {
        return this.channel;
    }

    public void setChannel(RSSChannel c) {
        this.channel = c;
    }
}
