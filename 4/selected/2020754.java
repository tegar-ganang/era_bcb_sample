package com.adpython.domain;

import java.util.Date;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Article {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Persistent
    private String title;

    @Persistent(serialized = "true", defaultFetchGroup = "true")
    private Text content;

    @Persistent
    private Long channelId;

    @Persistent
    private Long createUserId;

    @Persistent
    private Date createTime;

    @Persistent
    private Date updateTime;

    @Persistent
    private boolean isDeleted;

    @Persistent
    private boolean ifShow;

    @Persistent
    private Integer rank;

    public Article(String title, Text content, Long channelId) {
        this.title = title;
        this.content = content;
        this.channelId = channelId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setContent(Text content) {
        this.content = content;
    }

    public Text getContent() {
        return this.content;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getChannelId() {
        return this.channelId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Long getCreateUserId() {
        return this.createUserId;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getUpdateTime() {
        return this.updateTime;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean getIsDeleted() {
        return this.isDeleted;
    }

    public void setIfShow(boolean ifShow) {
        this.ifShow = ifShow;
    }

    public boolean getIfShow() {
        return this.ifShow;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getRank() {
        return rank;
    }
}
