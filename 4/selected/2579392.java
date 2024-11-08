package de.schwarzrot.vdr.data.domain;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import de.schwarzrot.data.Entity;
import de.schwarzrot.data.meta.SortInfo;
import de.schwarzrot.data.support.AbstractEntity;

/**
 * class to hold information about an TV event announced by EPG
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 * 
 */
public class EpgEvent extends AbstractEntity implements PropertyChangeListener {

    private static final long serialVersionUID = 713L;

    private Date begin;

    private ChannelInfo channel;

    private String cryptVariant;

    private String description;

    private int duration;

    private int epgId;

    private String genre;

    private int minAge;

    private String subTitle;

    private String tableId;

    private String title;

    private String videoMode;

    private Date vpsBegin;

    private boolean withAC3;

    @Override
    public int compareTo(Entity other) throws IllegalArgumentException {
        int rv = 0;
        if (other instanceof EpgEvent) {
            EpgEvent e2 = (EpgEvent) other;
            if (getBegin() != null && e2.getBegin() != null) rv = getBegin().compareTo(e2.getBegin());
            if (rv == 0) {
                if (getChannel() != null && e2.getChannel() != null) rv = this.getChannel().compareTo(e2.getChannel()); else rv = duration - e2.getDuration();
            }
            if (rv == 0) rv = super.compareTo(other);
        } else rv = super.compareTo(other);
        return rv;
    }

    public final Date getBegin() {
        return begin;
    }

    public final ChannelInfo getChannel() {
        return channel;
    }

    public final String getCryptVariant() {
        return cryptVariant;
    }

    @Override
    public List<SortInfo> getDefaultOrder() {
        List<SortInfo> order = new ArrayList<SortInfo>();
        order.add(new SortInfo("begin", false));
        order.add(new SortInfo("channel"));
        return order;
    }

    public final String getDescription() {
        return description;
    }

    public final int getDuration() {
        return duration;
    }

    public final int getEpgId() {
        return epgId;
    }

    public final String getGenre() {
        return genre;
    }

    @Override
    public Map<String, String> getMappings() {
        Map<String, String> mappings = super.getMappings();
        mappings.put("begin", "evtbegin");
        mappings.put("cryptVariant", "cryptvar");
        mappings.put("withAC3", "dolby");
        mappings.put("channel", "channelid");
        return mappings;
    }

    public final int getMinAge() {
        return minAge;
    }

    public final String getSubTitle() {
        return subTitle;
    }

    public final String getTableId() {
        return tableId;
    }

    public final String getTitle() {
        return title;
    }

    @Override
    public List<String> getUniqColumnNames() {
        List<String> rv = new ArrayList<String>();
        rv.add("channel");
        rv.add("epgId");
        return rv;
    }

    public final String getVideoMode() {
        return videoMode;
    }

    public final Date getVpsBegin() {
        return vpsBegin;
    }

    public final boolean isWithAC3() {
        return withAC3;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == channel) this.firePropertyChange(evt);
    }

    public final void setBegin(Date begin) {
        Date ov = this.begin;
        this.begin = begin;
        if (ov != null) {
            if (begin == null || ov.equals(begin)) setDirty(true);
        } else if (ov != begin) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "begin", ov, begin);
        this.firePropertyChange(pce);
    }

    public final void setChannel(ChannelInfo channel) {
        ChannelInfo ov = this.channel;
        this.channel = channel;
        if (ov != null) {
            ov.removePropertyChangeListener(this);
            if (!ov.equals(channel)) setDirty(true);
        } else if (ov != channel) setDirty(true);
        if (channel != null) channel.addPropertyChangeListener(this);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "channel", ov, channel);
        this.firePropertyChange(pce);
    }

    public final void setCryptVariant(String cryptVariant) {
        String ov = this.cryptVariant;
        this.cryptVariant = cryptVariant;
        if (ov != null) {
            if (cryptVariant == null || ov.compareTo(cryptVariant) != 0) setDirty(true);
        } else if (ov != cryptVariant) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "cryptVariant", ov, cryptVariant);
        this.firePropertyChange(pce);
    }

    public final void setDescription(String description) {
        String ov = this.description;
        this.description = description;
        if (ov != null) {
            if (description == null || ov.compareTo(description) != 0) setDirty(true);
        } else if (ov != description) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "description", ov, description);
        this.firePropertyChange(pce);
    }

    public final void setDuration(int duration) {
        Integer ov = this.duration;
        this.duration = duration;
        if (!ov.equals(duration)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "duration", ov, duration);
        this.firePropertyChange(pce);
    }

    public final void setEpgId(int epgId) {
        Integer ov = this.epgId;
        this.epgId = epgId;
        if (!ov.equals(epgId)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "epgId", ov, epgId);
        this.firePropertyChange(pce);
    }

    public final void setGenre(String genre) {
        String ov = this.genre;
        this.genre = genre;
        if (ov != null) {
            if (genre == null || ov.compareTo(genre) != 0) setDirty(true);
        } else if (ov != genre) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "genre", ov, genre);
        firePropertyChange(pce);
    }

    public final void setMinAge(int minAge) {
        Integer ov = this.minAge;
        this.minAge = minAge;
        if (ov.intValue() != minAge) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "minAge", ov, new Integer(minAge));
        firePropertyChange(pce);
    }

    public final void setSubTitle(String subTitle) {
        String ov = this.subTitle;
        this.subTitle = subTitle;
        if (ov != null) {
            if (subTitle == null || ov.compareTo(subTitle) != 0) setDirty(true);
        } else if (ov != subTitle) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "subTitle", ov, subTitle);
        this.firePropertyChange(pce);
    }

    public final void setTableId(String tableId) {
        String ov = this.tableId;
        this.tableId = tableId;
        if (ov != null) {
            if (tableId == null || ov.compareTo(tableId) != 0) setDirty(true);
        } else if (ov != tableId) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "tableId", ov, tableId);
        this.firePropertyChange(pce);
    }

    public final void setTitle(String title) {
        String ov = this.title;
        this.title = title;
        if (ov != null) {
            if (title == null || ov.compareTo(title) != 0) setDirty(true);
        } else if (ov != title) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "title", ov, title);
        this.firePropertyChange(pce);
    }

    public final void setVideoMode(String videoMode) {
        String ov = this.videoMode;
        this.videoMode = videoMode;
        if (ov != null) {
            if (videoMode == null || ov.compareTo(videoMode) != 0) setDirty(true);
        } else if (ov != videoMode) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "videoMode", ov, videoMode);
        this.firePropertyChange(pce);
    }

    public final void setVpsBegin(Date vpsBegin) {
        Date ov = this.vpsBegin;
        this.vpsBegin = vpsBegin;
        if (ov != null) {
            if (vpsBegin == null || !ov.equals(vpsBegin)) setDirty(true);
        } else if (ov != vpsBegin) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "vpsBegin", ov, vpsBegin);
        this.firePropertyChange(pce);
    }

    public final void setWithAC3(boolean withAC3) {
        Boolean ov = this.withAC3;
        this.withAC3 = withAC3;
        if (!ov.equals(withAC3)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "withAC3", ov, withAC3);
        this.firePropertyChange(pce);
    }

    @Override
    public String toString() {
        StringBuffer rv = new StringBuffer(channel.getShortId());
        rv.append(": ");
        rv.append(begin);
        rv.append(" - ");
        rv.append(title);
        return rv.toString();
    }

    public static EpgEvent valueOf(String line) {
        EpgEvent rv = null;
        if (line != null && line.startsWith("E ")) {
            String[] parts = line.split(" ");
            if (parts.length > 3) {
                int pos = 0;
                int id = Integer.valueOf(parts[++pos]);
                long date = Long.valueOf(parts[++pos]);
                rv = new EpgEvent();
                rv.setEpgId(id);
                rv.setBegin(new Date(date * 1000l));
                rv.setDuration(Integer.valueOf(parts[++pos]) / 60);
                if (parts.length > pos) rv.setTableId(parts[++pos]);
                if (parts.length > pos) rv.setCryptVariant(parts[++pos]);
            }
        }
        return rv;
    }
}
