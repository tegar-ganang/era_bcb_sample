package de.schwarzrot.vdr.data.domain;

import java.beans.PropertyChangeEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import de.schwarzrot.data.Entity;
import de.schwarzrot.data.VdrEntity;
import de.schwarzrot.data.meta.SortInfo;
import de.schwarzrot.data.support.AbstractEntity;

/**
 * class to hold all informations about a VDR timer
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 * 
 */
public class Timer extends AbstractEntity implements VdrEntity {

    public static final String FLD_CHANNEL_ID = "channelID";

    public static final String FLD_NAME = "name";

    public static final String FLD_BEGIN = "begin";

    public static final String FLD_END = "end";

    public static final String FLD_PRE_START = "preStart";

    public static final String FLD_POST_END = "postEnd";

    public static final String FLD_LIFE_TIME = "lifeTime";

    public static final String FLD_PRIORITY = "priority";

    public static final String FLD_ACTIVE = "active";

    public static final String FLD_VPS = "vps";

    public static final String FLD_XINFO = "xInfo";

    private static final int ACTIVE_STATUS = 1;

    private static DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static int postEnd = 40;

    private static int preStart = 5;

    private static final long serialVersionUID = 713L;

    private static DateFormat timeFormat = new SimpleDateFormat("HHmm");

    private static final String VDR_SEPARATOR = ":";

    private static final int VPS_STATUS = 4;

    private boolean active;

    private boolean delivered;

    private Date begin;

    private Date end;

    private EpgEvent event;

    private int lifeTime;

    private String name;

    private String path;

    private String pathPrefix;

    private int priority;

    private int sequence;

    private int status;

    private boolean vps;

    private Date vpsBegin;

    private String xInfo;

    public Timer() {
        setActive(true);
        setVps(false);
        setVps(true);
        setVps(false);
        setDelivered(false);
        setPriority(50);
        setLifeTime(99);
    }

    public Timer(EpgEvent event) {
        this();
        setEvent(event);
    }

    @Override
    public int compareTo(Entity other) throws IllegalArgumentException {
        int rv = 0;
        if (other instanceof Timer) {
            if (this.begin != null && ((Timer) other).begin != null) rv = this.begin.compareTo(((Timer) other).begin); else rv = super.compareTo(other);
        } else rv = super.compareTo(other);
        return rv;
    }

    public final Date getBegin() {
        return begin;
    }

    public final String getChannelId() {
        String rv = null;
        if (event != null && event.getChannel() != null) rv = event.getChannel().getShortId();
        return rv;
    }

    @Override
    public List<SortInfo> getDefaultOrder() {
        List<SortInfo> order = new ArrayList<SortInfo>();
        order.add(new SortInfo("begin", false));
        order.add(new SortInfo("active"));
        return order;
    }

    public final Date getEnd() {
        return end;
    }

    public final EpgEvent getEvent() {
        return event;
    }

    public final int getLifeTime() {
        return lifeTime;
    }

    @Override
    public Map<String, String> getMappings() {
        Map<String, String> mappings = super.getMappings();
        mappings.put("event", "eventid");
        mappings.put("priority", "prio");
        mappings.put("lifeTime", "life");
        mappings.put("begin", "tmbegin");
        mappings.put("sequence", "weight");
        mappings.put("path", "recpath");
        mappings.put("end", "tmend");
        return mappings;
    }

    public final String getName() {
        return name;
    }

    /**
     * @return the path
     */
    public final String getPath() {
        return path;
    }

    public final String getPathPrefix() {
        return pathPrefix;
    }

    public final int getPostEnd() {
        return postEnd;
    }

    public final int getPreStart() {
        return preStart;
    }

    public final int getPriority() {
        return priority;
    }

    public final int getSequence() {
        return sequence;
    }

    public final int getStatus() {
        return status;
    }

    @Override
    public List<String> getUniqColumnNames() {
        List<String> rv = new ArrayList<String>();
        rv.add("event");
        return rv;
    }

    public final Date getVpsBegin() {
        return vpsBegin;
    }

    public final String getXInfo() {
        return xInfo;
    }

    public final boolean isActive() {
        return active;
    }

    public final boolean isDelivered() {
        return delivered;
    }

    public final boolean isVps() {
        return vps;
    }

    public final void setActive(boolean active) {
        Boolean ov = this.active;
        this.active = active;
        if (active) this.status |= ACTIVE_STATUS; else this.status &= ~ACTIVE_STATUS;
        if (!ov.equals(active)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "active", ov, active);
        this.firePropertyChange(pce);
    }

    public final void setBegin(Date begin) {
        Date ov = this.begin;
        this.begin = begin;
        if (ov != null) {
            if (begin == null || !ov.equals(begin)) setDirty(true);
        } else if (ov != begin) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "begin", ov, begin);
        this.firePropertyChange(pce);
    }

    public final void setChannelId(String channelId) {
    }

    public final void setDelivered(boolean delivered) {
        Boolean ov = this.delivered;
        this.delivered = delivered;
        if (!ov.equals(delivered)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "delivered", ov, delivered);
        firePropertyChange(pce);
    }

    public final void setEnd(Date end) {
        Date ov = this.end;
        this.end = end;
        if (ov != null) {
            if (end == null || !ov.equals(end)) setDirty(true);
        } else if (ov != end) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "end", ov, end);
        this.firePropertyChange(pce);
    }

    public final void setEvent(EpgEvent event) {
        EpgEvent ov = this.event;
        this.event = event;
        if (event != null) {
            if (event.getTitle() != null) setName(event.getTitle());
            if (event.getVpsBegin() != null) {
                setVpsBegin(event.getVpsBegin());
                setVps(true);
            }
            if (event.getBegin() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(event.getBegin());
                setBegin(event.getBegin());
                cal.add(Calendar.MINUTE, event.getDuration());
                setEnd(cal.getTime());
            }
            setSequence(event.getEpgId());
            if (xInfo == null) createXInfo();
        }
        if (ov != null) {
            if (event == null || ov.compareTo(event) != 0) setDirty(true);
        } else if (ov != event) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "event", ov, event);
        this.firePropertyChange(pce);
    }

    public final void setLifeTime(int lifeTime) {
        Integer ov = this.lifeTime;
        this.lifeTime = lifeTime;
        if (ov.compareTo(lifeTime) != 0) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "lifeTime", ov, lifeTime);
        this.firePropertyChange(pce);
    }

    public final void setName(String name) {
        String ov = this.name;
        this.name = name;
        if (ov != null) {
            if (name == null || ov.compareTo(name) != 0) setDirty(true);
        } else if (ov != name) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "name", ov, name);
        this.firePropertyChange(pce);
    }

    /**
     * @param path
     *            the path to set
     */
    public final void setPath(String path) {
        String ov = this.path;
        this.path = path;
        if (ov != null) {
            if (path == null || ov.compareTo(path) != 0) setDirty(true);
        } else if (ov != path) setDirty(true);
        if (this.path != null) {
            int idx = this.path.indexOf("~");
            if (idx >= 0) {
                setPathPrefix(this.path.substring(0, idx));
                setName(this.path.substring(idx + 1));
            }
        }
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "path", ov, path);
        firePropertyChange(pce);
    }

    public final void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public final void setPostEnd(int postEnd) {
        Integer ov = Timer.postEnd;
        Timer.postEnd = postEnd;
        if (!ov.equals(postEnd)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "postEnd", ov, postEnd);
        this.firePropertyChange(pce);
    }

    public final void setPreStart(int preStart) {
        Integer ov = Timer.preStart;
        Timer.preStart = preStart;
        if (!ov.equals(preStart)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "preStart", ov, preStart);
        this.firePropertyChange(pce);
    }

    public final void setPriority(int priority) {
        Integer ov = this.priority;
        this.priority = priority;
        if (!ov.equals(priority)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "priority", ov, priority);
        this.firePropertyChange(pce);
    }

    public final void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public final void setStatus(int status) {
        this.status = status;
        active = (status & ACTIVE_STATUS) != 0;
        vps = (status & VPS_STATUS) != 0;
    }

    public final void setVps(boolean vps) {
        Boolean ov = this.vps;
        this.vps = vps;
        if (vps) this.status |= VPS_STATUS; else this.status &= ~VPS_STATUS;
        if (!ov.equals(vps)) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "vps", ov, vps);
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

    public final void setXInfo(String info) {
        String ov = this.xInfo;
        xInfo = info;
        if (ov != null) {
            if (info == null || ov.compareTo(info) != 0) setDirty(true);
        } else if (ov != info) setDirty(true);
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "xInfo", ov, info);
        this.firePropertyChange(pce);
    }

    @Override
    public String toString() {
        StringBuilder rv = new StringBuilder();
        Calendar c = Calendar.getInstance();
        c.setTime(begin);
        c.add(Calendar.MINUTE, (-1) * preStart);
        Date nBegin = c.getTime();
        c.setTime(end);
        c.add(Calendar.MINUTE, postEnd);
        Date nEnd = c.getTime();
        rv.append(status);
        rv.append(VDR_SEPARATOR);
        rv.append(getChannelId());
        rv.append(VDR_SEPARATOR);
        rv.append(dayFormat.format(nBegin));
        rv.append(VDR_SEPARATOR);
        rv.append(timeFormat.format(nBegin));
        rv.append(VDR_SEPARATOR);
        rv.append(timeFormat.format(nEnd));
        rv.append(VDR_SEPARATOR);
        rv.append(String.format("%02d", priority));
        rv.append(VDR_SEPARATOR);
        rv.append(String.format("%02d", lifeTime));
        rv.append(VDR_SEPARATOR);
        if (pathPrefix != null && pathPrefix.length() > 1) {
            rv.append(pathPrefix.replace("/", "~"));
            rv.append("~");
        }
        if (name != null) rv.append(name.replace("\n", "|").replace(VDR_SEPARATOR, "~").replace(" ", "_")); else rv.append("<no name?>");
        if (event != null) {
            rv.append(VDR_SEPARATOR);
            rv.append(createXInfo());
        }
        return rv.toString();
    }

    protected String createXInfo() {
        assert event != null : "event may not be null!";
        StringBuilder sb = new StringBuilder("<vdrassistant>");
        if (event.getChannel() != null) sb.append("<channelid>").append(event.getChannel().getShortId()).append("</channelid>");
        if (event.getBegin() != null) sb.append("<start>").append(event.getBegin().getTime()).append("</start>");
        sb.append("<eventid>").append(event.getEpgId()).append("</eventid>");
        sb.append("<title>").append(event.getTitle()).append("</title>");
        sb.append("</vdrassistant>");
        return sb.toString();
    }

    protected void parseXInfo() {
    }

    public static Date createDate(String day, String time) {
        String[] parts = day.split("-");
        int hours = Integer.valueOf(time.substring(0, 2));
        int minutes = Integer.valueOf(time.substring(2));
        Calendar c = Calendar.getInstance();
        c.set(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]) - 1, Integer.valueOf(parts[2]), hours, minutes, 0);
        return c.getTime();
    }

    public static Date createDate(String day, String time0, String time1) {
        String[] parts = day.split("-");
        int h0 = Integer.valueOf(time0.substring(0, 2));
        int m0 = Integer.valueOf(time0.substring(2));
        int h1 = Integer.valueOf(time1.substring(0, 2));
        int m1 = Integer.valueOf(time1.substring(2));
        long tm0 = h0 * 3600 + m0 * 60;
        long tm1 = h1 * 3600 + m1 * 60;
        Calendar c = Calendar.getInstance();
        if (tm0 > tm1) c.set(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]) - 1, Integer.valueOf(parts[2]) + 1, h1, m1, 0); else c.set(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]) - 1, Integer.valueOf(parts[2]), h1, m1, 0);
        return c.getTime();
    }

    public static Timer valueOf(String line, Map<ChannelInfo, Map<String, EpgEvent>> events) {
        Timer rv = null;
        if (line != null && line.length() > 0) {
            String[] parts = line.split(":");
            if (parts.length > 7) {
                ChannelInfo chn = ChannelInfo.valueOf(parts[1]);
                EpgEvent evt = new EpgEvent();
                Date begin = createDate(parts[2], parts[3]);
                Date end = createDate(parts[2], parts[3], parts[4]);
                rv = new Timer();
                rv.setStatus(Integer.valueOf(parts[0]));
                evt.setChannel(chn);
                evt.setBegin(begin);
                evt.setDuration((int) (end.getTime() - begin.getTime()) / 1000);
                evt.setTitle(parts[7]);
                String chnId = chn.getShortId();
                EpgEvent tmp = null;
                if (events.containsKey(chnId)) {
                    Map<String, EpgEvent> innerMap = events.get(chnId);
                    if (innerMap.containsKey(evt.toString())) tmp = innerMap.get(evt.toString());
                }
                if (tmp != null) rv.setEvent(tmp); else rv.setEvent(evt);
                rv.setPriority(Integer.valueOf(parts[5]));
                rv.setLifeTime(Integer.valueOf(parts[6]));
                if (parts.length > 8) rv.setXInfo(parts[8]);
            }
        }
        return rv;
    }
}
