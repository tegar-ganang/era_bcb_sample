package com.tcs.hrr.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Channel entity.
 * 
 * @author MyEclipse Persistence Tools
 */
public class Channel implements java.io.Serializable {

    private Integer channelId;

    private String channelName;

    private String channelDesc;

    private String createBy;

    private Date createDate;

    private String updateBy;

    private Date updateDate;

    private Set channelSources = new HashSet(0);

    /** default constructor */
    public Channel() {
    }

    /** full constructor */
    public Channel(String channelName, String channelDesc, String createBy, Date createDate, String updateBy, Date updateDate, Set channelSources) {
        this.channelName = channelName;
        this.channelDesc = channelDesc;
        this.createBy = createBy;
        this.createDate = createDate;
        this.updateBy = updateBy;
        this.updateDate = updateDate;
        this.channelSources = channelSources;
    }

    public Integer getChannelId() {
        return this.channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelDesc() {
        return this.channelDesc;
    }

    public void setChannelDesc(String channelDesc) {
        this.channelDesc = channelDesc;
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

    public Set getChannelSources() {
        return this.channelSources;
    }

    public void setChannelSources(Set channelSources) {
        this.channelSources = channelSources;
    }
}
