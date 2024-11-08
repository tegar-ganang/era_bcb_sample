package com.wwfish.cms.model;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-11
 * Time: 11:40:22
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseContentDto extends BaseDto implements IAuditDto {

    private Long channelId;

    private String key;

    private Long version;

    private Boolean onLine;

    private Boolean deleteFlag;

    private Boolean topFlag;

    private Boolean hotFlag;

    private Long clicks;

    private Boolean commentFlag;

    private String workflowStatus;

    private String grade;

    private Long sequence;

    private Date lastIndexTime;

    private Long creator;

    private Long auditor;

    private Long publisher;

    private Date publishTime;

    private Date commitTime;

    private Date auditTime;

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getOnLine() {
        return onLine;
    }

    public void setOnLine(Boolean onLine) {
        this.onLine = onLine;
    }

    public Boolean getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public Boolean getTopFlag() {
        return topFlag;
    }

    public void setTopFlag(Boolean topFlag) {
        this.topFlag = topFlag;
    }

    public Boolean getHotFlag() {
        return hotFlag;
    }

    public void setHotFlag(Boolean hotFlag) {
        this.hotFlag = hotFlag;
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }

    public Boolean getCommentFlag() {
        return commentFlag;
    }

    public void setCommentFlag(Boolean commentFlag) {
        this.commentFlag = commentFlag;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public Date getLastIndexTime() {
        return lastIndexTime;
    }

    public void setLastIndexTime(Date lastIndexTime) {
        this.lastIndexTime = lastIndexTime;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public Long getAuditor() {
        return auditor;
    }

    public void setAuditor(Long auditor) {
        this.auditor = auditor;
    }

    public Long getPublisher() {
        return publisher;
    }

    public void setPublisher(Long publisher) {
        this.publisher = publisher;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }
}
