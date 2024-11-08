package csiebug.domain.hibernateImpl;

import java.util.Calendar;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import csiebug.domain.WebservicesChannel;

public class WebservicesChannelImpl implements WebservicesChannel {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String channelId;

    private String serviceKey;

    private Calendar lastUsed;

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setLastUsed(Calendar lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Calendar getLastUsed() {
        return lastUsed;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WebservicesChannelImpl)) {
            return false;
        }
        WebservicesChannelImpl webservicesChannel = (WebservicesChannelImpl) obj;
        return new EqualsBuilder().append(this.userId, webservicesChannel.getUserId()).append(this.channelId, webservicesChannel.getChannelId()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(this.userId).append(this.channelId).toHashCode();
    }
}
