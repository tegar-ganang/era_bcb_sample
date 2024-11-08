package net.urlgrey.mythpodcaster.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author scott
 *
 */
@Entity
@Table(name = "channel")
public class Channel {

    @Id
    @Column(name = "chanid")
    private int channelId;

    @Column(name = "name")
    private String name;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
    public String toString() {
        final String TAB = "    ";
        String retValue = "";
        retValue = "Channel ( " + super.toString() + TAB + "channelId = " + this.channelId + TAB + "name = " + this.name + TAB + " )";
        return retValue;
    }
}
