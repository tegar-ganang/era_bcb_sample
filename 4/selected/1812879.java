package de.nava.informa.impl.hibernate;

import de.nava.informa.core.ChannelGroupIF;
import de.nava.informa.core.ChannelIF;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Hibernate implementation of the ChannelGroupIF interface.
 *
 * @author Niko Schmuck (niko@nava.de)
 */
public class ChannelGroup implements ChannelGroupIF {

    private static final long serialVersionUID = -4572648595088013842L;

    private long id = -1;

    private String title;

    private ChannelGroupIF parent;

    private Set<ChannelIF> channels;

    private Collection<ChannelGroupIF> children;

    public ChannelGroup() {
        this("Unnamed channel group");
    }

    public ChannelGroup(String title) {
        this(null, title);
    }

    public ChannelGroup(ChannelGroupIF parent, String title) {
        this.title = title;
        this.channels = Collections.synchronizedSet(new HashSet<ChannelIF>());
        this.parent = parent;
        this.children = new ArrayList<ChannelGroupIF>();
    }

    /**
   * @return integer representation of identity.
   */
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
   * @return title.
   */
    public String getTitle() {
        return title;
    }

    public void setTitle(String aTitle) {
        this.title = aTitle;
    }

    /**
   * @return channels.
   */
    public Set<ChannelIF> getChannels() {
        return channels;
    }

    public void setChannels(Set<ChannelIF> aChannels) {
        this.channels = Collections.synchronizedSet(aChannels);
    }

    public void add(ChannelIF channel) {
        channels.add(channel);
    }

    public void remove(ChannelIF channel) {
        channels.remove(channel);
    }

    public Collection<ChannelIF> getAll() {
        return getChannels();
    }

    public ChannelIF getById(long channelId) {
        Iterator it = getChannels().iterator();
        while (it.hasNext()) {
            ChannelIF channel = (ChannelIF) it.next();
            if (channel.getId() == channelId) {
                return channel;
            }
        }
        return null;
    }

    /**
   * @return parent group.
   */
    public ChannelGroupIF getParent() {
        return parent;
    }

    public void setParent(ChannelGroupIF group) {
        this.parent = group;
    }

    /**
   * @return children.
   */
    public Collection<ChannelGroupIF> getChildren() {
        return children;
    }

    public void setChildren(Collection<ChannelGroupIF> aChildren) {
        this.children = aChildren;
    }

    public void addChild(ChannelGroupIF child) {
        getChildren().add(child);
        child.setParent(this);
    }

    public void removeChild(ChannelGroupIF child) {
        getChildren().remove(child);
    }

    /**
   * Returns a string representation of the object.
   *
   * @return  a string representation of the object.
   */
    public String toString() {
        return "[Hibernate ChannelGroup \"" + getTitle() + "\"(id=" + id + ")]";
    }
}
