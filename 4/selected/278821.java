package com.business.entity.cms;

import java.util.Date;
import javax.persistence.Entity;
import com.business.entity.IdEntity;

@Entity
public abstract class Article extends IdEntity {

    private Long channelID;

    private Long classID;

    private Long typeID;

    private String url;

    private String relativeUrl;

    private String title;

    private String titleImage;

    private String summary;

    private String source;

    private String content;

    private Integer review;

    private Integer priority;

    private Byte status;

    private boolean stickyTopic;

    private boolean prohibitComment;

    private boolean publish;

    private Date commitTime;

    private Long commitPersonID;

    private Date lastModifiedTime;

    private Long lastModifiedPersonID;

    public Long getCommitPersonID() {
        return commitPersonID;
    }

    public void setCommitPersonID(Long commitPersonID) {
        this.commitPersonID = commitPersonID;
    }

    public Long getLastModifiedPersonID() {
        return lastModifiedPersonID;
    }

    public void setLastModifiedPersonID(Long lastModifiedPersonID) {
        this.lastModifiedPersonID = lastModifiedPersonID;
    }

    public Long getChannelID() {
        return channelID;
    }

    public void setChannelID(Long channelID) {
        this.channelID = channelID;
    }

    public Long getClassID() {
        return classID;
    }

    public void setClassID(Long classID) {
        this.classID = classID;
    }

    public Long getTypeID() {
        return typeID;
    }

    public void setTypeID(Long typeID) {
        this.typeID = typeID;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRelativeUrl() {
        return relativeUrl;
    }

    public void setRelativeUrl(String relativeUrl) {
        this.relativeUrl = relativeUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleImage() {
        return titleImage;
    }

    public void setTitleImage(String titleImage) {
        this.titleImage = titleImage;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getReview() {
        return review;
    }

    public void setReview(Integer review) {
        this.review = review;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public boolean isStickyTopic() {
        return stickyTopic;
    }

    public void setStickyTopic(boolean stickyTopic) {
        this.stickyTopic = stickyTopic;
    }

    public boolean isProhibitComment() {
        return prohibitComment;
    }

    public void setProhibitComment(boolean prohibitComment) {
        this.prohibitComment = prohibitComment;
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
