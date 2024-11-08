package net.urlgrey.mythpodcaster.domain;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author scottkidder
 *
 */
@Embeddable
public class RecordedProgramPK implements Serializable {

    private static final long serialVersionUID = -2379768958766467182L;

    @Column(name = "chanid", updatable = false, unique = false, insertable = false, nullable = false)
    private int channelId;

    @Column(name = "starttime", updatable = false, unique = false, insertable = false, nullable = false)
    private Timestamp startTime;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "RecordedProgramPK [channelId=" + channelId + ", startTime=" + startTime + "]";
    }
}
