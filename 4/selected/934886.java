package de.schwarzrot.vdr.data.domain;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import de.schwarzrot.data.NamedEntity;
import de.schwarzrot.data.meta.SortInfo;
import de.schwarzrot.data.support.AbstractEntity;

/**
 * class to hold informations about a VDR TV channel
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 * 
 */
public class ChannelInfo extends AbstractEntity implements NamedEntity<String> {

    protected static final String UNKNOWN_BOUQUET = "unknown";

    protected static final String EPG_SEPARATOR = "-";

    protected static final String CONF_SEPARATOR = ":";

    private static final String PERSISTENCE_NAME = "channel";

    private static final long serialVersionUID = 713L;

    public final String getAudioIds() {
        return audioIds;
    }

    public final String getBouquet() {
        return bouquet;
    }

    public final String getCaId() {
        return caId;
    }

    public final String getChannelName() {
        return channelName;
    }

    @Override
    public List<SortInfo> getDefaultOrder() {
        List<SortInfo> order = new ArrayList<SortInfo>();
        order.add(new SortInfo("favorite"));
        order.add(new SortInfo("weight"));
        return order;
    }

    public final String getEpgInfo() {
        return epgInfo;
    }

    public final long getFrequency() {
        return frequency;
    }

    @Override
    public Map<String, String> getMappings() {
        Map<String, String> mappings = super.getMappings();
        mappings.put("channelName", "name");
        mappings.put("symbolRate", "symrate");
        mappings.put("videoIds", "vidids");
        mappings.put("audioIds", "audids");
        mappings.put("serviceId", "srvid");
        mappings.put("radioId", "radid");
        mappings.put("frequency", "freq");
        return mappings;
    }

    @Override
    public final String getName() {
        return getChannelName() != null ? getChannelName() : getShortId();
    }

    public final int getNetId() {
        return netId;
    }

    public final String getParameter() {
        return parameter;
    }

    @Override
    public String getPersistenceName() {
        return PERSISTENCE_NAME;
    }

    public final int getRadioId() {
        return radioId;
    }

    public final int getServiceId() {
        return serviceId;
    }

    public final String getShortId() {
        String rv = null;
        if (source != null) {
            StringBuilder sb = new StringBuilder(source.replace(":", "|"));
            sb.append(EPG_SEPARATOR);
            if (netId == 0 && tsId == 0) {
                sb.append(netId);
                sb.append(EPG_SEPARATOR);
                sb.append(frequency / 1000);
            } else {
                sb.append(netId);
                sb.append(EPG_SEPARATOR);
                sb.append(tsId);
            }
            sb.append(EPG_SEPARATOR);
            sb.append(serviceId);
            if (radioId != 0) {
                sb.append(EPG_SEPARATOR);
                sb.append(radioId);
            }
            rv = sb.toString();
        }
        return rv;
    }

    public final String getSource() {
        return source;
    }

    public final int getSymbolRate() {
        return symbolRate;
    }

    public final int getTsId() {
        return tsId;
    }

    public final String getTtId() {
        return ttId;
    }

    @Override
    public List<String> getUniqColumnNames() {
        List<String> rv = new ArrayList<String>();
        rv.add("source");
        rv.add("netId");
        rv.add("tsId");
        rv.add("serviceId");
        rv.add("radioId");
        return rv;
    }

    public final String getVideoIds() {
        return videoIds;
    }

    public final int getWeight() {
        return weight;
    }

    public final boolean isCrypted() {
        return videoIds.equals("1") || !caId.equals("0");
    }

    public final boolean isFavorite() {
        return favorite;
    }

    public final boolean isRadio() {
        return videoIds.equals("0") || videoIds.equals("1");
    }

    public final void setAudioIds(String audioIds) {
        String ov = this.audioIds;
        this.audioIds = audioIds;
        if (ov != null) {
            if (audioIds == null || ov.compareTo(audioIds) != 0) setDirty(true);
        } else if (ov != audioIds) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "audioIds", ov, audioIds);
        this.firePropertyChange(pce);
    }

    public final void setBouquet(String bouquet) {
        String ov = this.bouquet;
        this.bouquet = bouquet;
        if (ov != null) {
            if (bouquet == null || ov.compareTo(bouquet) != 0) setDirty(true);
        } else if (ov != bouquet) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "bouquet", ov, bouquet);
        this.firePropertyChange(pce);
    }

    public final void setCaId(String caId) {
        String ov = this.caId;
        this.caId = caId;
        if (ov != null) {
            if (caId == null || ov.compareTo(caId) != 0) setDirty(true);
        } else if (ov != caId) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "caId", ov, caId);
        this.firePropertyChange(pce);
    }

    public final void setChannelName(String channelName) {
        String ov = this.channelName;
        this.channelName = channelName;
        if (ov != null) {
            if (channelName == null || ov.compareTo(channelName) != 0) setDirty(true);
        } else if (ov != channelName) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "channelName", ov, channelName);
        this.firePropertyChange(pce);
    }

    public final void setEpgInfo(String epgInfo) {
        String ov = this.epgInfo;
        this.epgInfo = epgInfo;
        if (ov != null) {
            if (epgInfo == null || ov.compareTo(epgInfo) != 0) setDirty(true);
        } else if (ov != epgInfo) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "epgInfo", ov, epgInfo);
        this.firePropertyChange(pce);
    }

    public final void setFavorite(boolean favorite) {
        Boolean ov = this.favorite;
        this.favorite = favorite;
        if (!ov.equals(favorite)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "favorite", ov, favorite);
        this.firePropertyChange(pce);
    }

    public final void setFrequency(long frequency) {
        Long ov = this.frequency;
        this.frequency = frequency;
        if (!ov.equals(frequency)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "frequency", ov, frequency);
        this.firePropertyChange(pce);
    }

    @Override
    public final void setName(String notUsed) {
    }

    public final void setNetId(int netId) {
        Integer ov = this.netId;
        this.netId = netId;
        if (!ov.equals(netId)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "netId", ov, netId);
        this.firePropertyChange(pce);
    }

    public final void setParameter(String parameter) {
        String ov = this.parameter;
        this.parameter = parameter;
        if (ov != null) {
            if (parameter == null || ov.compareTo(parameter) != 0) setDirty(true);
        } else if (ov != parameter) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "parameter", ov, parameter);
        this.firePropertyChange(pce);
    }

    public final void setRadioId(int radioId) {
        Integer ov = this.radioId;
        this.radioId = radioId;
        if (!ov.equals(radioId)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "radioId", ov, radioId);
        this.firePropertyChange(pce);
    }

    public final void setServiceId(int serviceId) {
        Integer ov = this.serviceId;
        this.serviceId = serviceId;
        if (!ov.equals(serviceId)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "serviceId", ov, serviceId);
        this.firePropertyChange(pce);
    }

    public final void setSource(String source) {
        String ov = this.source;
        this.source = source;
        if (ov != null) {
            if (source == null || ov.compareTo(source) != 0) setDirty(true);
        } else if (ov != source) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "source", ov, source);
        this.firePropertyChange(pce);
    }

    public final void setSymbolRate(int symbolRate) {
        Integer ov = this.symbolRate;
        this.symbolRate = symbolRate;
        if (!ov.equals(symbolRate)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "symbolRate", ov, symbolRate);
        this.firePropertyChange(pce);
    }

    public final void setTsId(int tsId) {
        Integer ov = this.tsId;
        this.tsId = tsId;
        if (!ov.equals(tsId)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "tsId", ov, tsId);
        this.firePropertyChange(pce);
    }

    public final void setTtId(String ttId) {
        String ov = this.ttId;
        this.ttId = ttId;
        if (ov != null) {
            if (ttId == null || ov.compareTo(ttId) != 0) setDirty(true);
        } else if (ov != ttId) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "ttId", ov, ttId);
        this.firePropertyChange(pce);
    }

    public final void setVideoIds(String videoIds) {
        String ov = this.videoIds;
        this.videoIds = videoIds;
        if (ov != null) {
            if (videoIds == null || ov.compareTo(videoIds) != 0) setDirty(true);
        } else if (ov != videoIds) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "videoIds", ov, videoIds);
        this.firePropertyChange(pce);
    }

    public final void setWeight(int weight) {
        Integer ov = this.weight;
        this.weight = weight;
        if (!ov.equals(weight)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "weight", ov, weight);
        this.firePropertyChange(pce);
    }

    @Override
    public String toString() {
        StringBuilder rv = new StringBuilder(getName().replace(":", "|"));
        if (bouquet != null) {
            rv.append(";");
            rv.append(bouquet);
        }
        rv.append(CONF_SEPARATOR);
        rv.append(frequency);
        rv.append(CONF_SEPARATOR);
        rv.append(parameter);
        rv.append(CONF_SEPARATOR);
        rv.append(source);
        rv.append(CONF_SEPARATOR);
        rv.append(symbolRate);
        rv.append(CONF_SEPARATOR);
        rv.append(videoIds);
        rv.append(CONF_SEPARATOR);
        rv.append(audioIds);
        rv.append(CONF_SEPARATOR);
        rv.append(ttId);
        rv.append(CONF_SEPARATOR);
        rv.append(caId);
        rv.append(CONF_SEPARATOR);
        rv.append(serviceId);
        rv.append(CONF_SEPARATOR);
        rv.append(netId);
        rv.append(CONF_SEPARATOR);
        rv.append(tsId);
        rv.append(CONF_SEPARATOR);
        rv.append(radioId);
        return rv.toString();
    }

    public void updateFrom(ChannelInfo other) {
        if (other.bouquet != null && other.bouquet.length() > 0) setBouquet(other.bouquet);
        if (other.frequency > 0) {
            setFrequency(other.frequency);
            setChannelName(other.channelName);
            setEpgInfo(other.epgInfo);
            setParameter(other.parameter);
            setSymbolRate(other.symbolRate);
            setVideoIds(other.videoIds);
            setAudioIds(other.audioIds);
            setTtId(other.ttId);
            setCaId(other.caId);
        }
    }

    public static ChannelInfo valueOf(String line) {
        ChannelInfo rv = null;
        if (line == null) return null;
        if (line.startsWith("C ")) rv = parseEPG(line); else if (line.contains(CONF_SEPARATOR)) rv = parseLong(line); else rv = parseShort(line);
        return rv;
    }

    protected static ChannelInfo parseEPG(String line) {
        ChannelInfo rv = null;
        String[] parts = line.split(" ", 3);
        if (parts.length > 2) {
            rv = parseShort(parts[1]);
            rv.setChannelName(parts[2]);
        }
        return rv;
    }

    protected static ChannelInfo parseLong(String line) {
        ChannelInfo rv = null;
        String[] parts = line.split(CONF_SEPARATOR);
        if (parts != null && parts.length > 10) {
            rv = new ChannelInfo();
            String[] names = parts[0].replace("|", ":").split(";");
            int pos = 0;
            if (names.length > 1) rv.setBouquet(names[1]); else rv.setBouquet(UNKNOWN_BOUQUET);
            rv.setChannelName(names[0]);
            rv.setFrequency(Integer.valueOf(parts[++pos]));
            rv.setParameter(parts[++pos]);
            rv.setSource(parts[++pos]);
            rv.setSymbolRate(Integer.valueOf(parts[++pos]));
            rv.setVideoIds(parts[++pos]);
            rv.setAudioIds(parts[++pos]);
            if (parts.length > pos) rv.setTtId(parts[++pos]);
            if (parts.length > pos) rv.setCaId(parts[++pos]);
            if (parts.length > pos) rv.setServiceId(Integer.valueOf(parts[++pos]));
            if (parts.length > pos) rv.setNetId(Integer.valueOf(parts[++pos]));
            if (parts.length > pos) rv.setTsId(Integer.valueOf(parts[++pos]));
            if (parts.length > pos) rv.setRadioId(Integer.valueOf(parts[++pos]));
        }
        return rv;
    }

    protected static ChannelInfo parseShort(String line) {
        ChannelInfo rv = null;
        String[] parts = line.split(EPG_SEPARATOR);
        if (parts.length > 3) {
            rv = new ChannelInfo();
            rv.setSource(parts[0]);
            rv.setNetId(Integer.valueOf(parts[1]));
            rv.setTsId(Integer.valueOf(parts[2]));
            rv.setServiceId(Integer.valueOf(parts[3]));
            if (parts.length > 4) rv.setRadioId(Integer.valueOf(parts[4]));
        }
        return rv;
    }

    private String channelName;

    private String bouquet;

    private String source;

    private String parameter;

    private String epgInfo;

    private long frequency;

    private int symbolRate;

    private String videoIds;

    private String audioIds;

    private int weight;

    private int tsId;

    private String ttId;

    private String caId;

    private int netId;

    private int serviceId;

    private int radioId;

    private boolean favorite;
}
