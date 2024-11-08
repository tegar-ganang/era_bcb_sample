package com.tcs.hrr.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * ChannelSource entity.
 * 
 * @author MyEclipse Persistence Tools
 */
public class ChannelSource implements java.io.Serializable {

    private Integer channelSourceId;

    private Channel channel;

    private String channelSourceName;

    private String channelSourceDesc;

    private String createBy;

    private Date createDate;

    private String updateBy;

    private Date updateDate;

    private Set candidates = new HashSet(0);

    /** default constructor */
    public ChannelSource() {
    }

    /** minimal constructor */
    public ChannelSource(Channel channel) {
        this.channel = channel;
    }

    /** full constructor */
    public ChannelSource(Channel channel, String channelSourceName, String channelSourceDesc, String createBy, Date createDate, String updateBy, Date updateDate, Set candidates) {
        this.channel = channel;
        this.channelSourceName = channelSourceName;
        this.channelSourceDesc = channelSourceDesc;
        this.createBy = createBy;
        this.createDate = createDate;
        this.updateBy = updateBy;
        this.updateDate = updateDate;
        this.candidates = candidates;
    }

    public Integer getChannelSourceId() {
        return this.channelSourceId;
    }

    public void setChannelSourceId(Integer channelSourceId) {
        this.channelSourceId = channelSourceId;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getChannelSourceName() {
        return this.channelSourceName;
    }

    public void setChannelSourceName(String channelSourceName) {
        this.channelSourceName = channelSourceName;
    }

    public String getChannelSourceDesc() {
        return this.channelSourceDesc;
    }

    public void setChannelSourceDesc(String channelSourceDesc) {
        this.channelSourceDesc = channelSourceDesc;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateDate() {
        return this.updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Set getCandidates() {
        return this.candidates;
    }

    public void setCandidates(Set candidates) {
        this.candidates = candidates;
    }
}
