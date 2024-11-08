package com.wwg.cms.bo.entity;

import com.wwg.cms.bo.*;

/**
 *create auto on 2010-8-31 16:43:18
 *create by wwl
 * document
 * 文章表
 * @hibernate.class table="document" *  mutable="true" 
 *  proxy="com.wwg.cms.bo.entity.DocumentEntity" 
 */
public class DocumentEntity implements Document {

    private Long id;

    private String keyword;

    private String content;

    private String docRefer;

    private String brief;

    private String author;

    private java.util.Date publicationTime;

    private Long lastVersion;

    private Boolean documentStatus;

    private Boolean isDelete;

    private Boolean isOntop;

    private Boolean isHot;

    private Long clicks;

    private Boolean isCommend;

    private Long workflowStatus;

    private Long specialId;

    private String imageUrl;

    private String downloadUrl;

    private String shortTitle;

    private Long grade;

    private Long orderNum;

    private java.util.Date lastindexTime;

    private String createAuthor;

    private String auditAuthor;

    private String publishAuthor;

    private String name;

    private java.util.Date createdTime;

    private java.util.Date updatedTime;

    private Long createAuthorid;

    private Long auditAuthorid;

    private Long channelId;

    private Long templateId;

    private Long publishAuthorid;

    /**
	 *文章编号
 	 *
	 * @hibernate.id  column="id"
	 *  unsaved-value="null"
 	 *  generator-class="native"
	 * @return Long 
	 */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
	 *关键词
 	 *
	 * @hibernate.property column="keyword"
	 * @return String 
	 */
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
	 *文章内容
 	 *
	 * @hibernate.property column="content"
	 * @return String 
	 */
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
	 *文章来源
 	 *
	 * @hibernate.property column="doc_refer"
	 * @return String 
	 */
    public String getDocRefer() {
        return docRefer;
    }

    public void setDocRefer(String docRefer) {
        this.docRefer = docRefer;
    }

    /**
	 *文章简介
 	 *
	 * @hibernate.property column="brief"
	 * @return String 
	 */
    public String getBrief() {
        return brief;
    }

    public void setBrief(String brief) {
        this.brief = brief;
    }

    /**
	 *文章作者
 	 *
	 * @hibernate.property column="author"
	 * @return String 
	 */
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
	 *文章发布日期
 	 *
	 * @hibernate.property column="publication_time"
	 * @return java.util.Date 
	 */
    public java.util.Date getPublicationTime() {
        return publicationTime;
    }

    public void setPublicationTime(java.util.Date publicationTime) {
        this.publicationTime = publicationTime;
    }

    /**
	 *最新版本号
 	 *
	 * @hibernate.property column="last_version"
	 * @return Long 
	 */
    public Long getLastVersion() {
        return lastVersion;
    }

    public void setLastVersion(Long lastVersion) {
        this.lastVersion = lastVersion;
    }

    /**
	 *文章是否在线的状态
 	 *
	 * @hibernate.property column="document_status"
	 * @return Boolean 
	 */
    public Boolean getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(Boolean documentStatus) {
        this.documentStatus = documentStatus;
    }

    /**
	 *文章是否回收 (0，允许回收,1,不允许回收)
 	 *
	 * @hibernate.property column="is_delete"
	 * @return Boolean 
	 */
    public Boolean getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Boolean isDelete) {
        this.isDelete = isDelete;
    }

    /**
	 *文章是否置顶（0,置顶；1，不指定）
 	 *
	 * @hibernate.property column="is_ontop"
	 * @return Boolean 
	 */
    public Boolean getIsOntop() {
        return isOntop;
    }

    public void setIsOntop(Boolean isOntop) {
        this.isOntop = isOntop;
    }

    /**
	 *文章是否热门 （0,热门;1,普通）
 	 *
	 * @hibernate.property column="is_hot"
	 * @return Boolean 
	 */
    public Boolean getIsHot() {
        return isHot;
    }

    public void setIsHot(Boolean isHot) {
        this.isHot = isHot;
    }

    /**
	 *文章点击统计
 	 *
	 * @hibernate.property column="clicks"
	 * @return Long 
	 */
    public Long getClicks() {
        return clicks;
    }

    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }

    /**
	 *文章是否评论
 	 *
	 * @hibernate.property column="is_commend"
	 * @return Boolean 
	 */
    public Boolean getIsCommend() {
        return isCommend;
    }

    public void setIsCommend(Boolean isCommend) {
        this.isCommend = isCommend;
    }

    /**
	 *文章审批状态
 	 *
	 * @hibernate.property column="workflow_status"
	 * @return Long 
	 */
    public Long getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(Long workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    /**
	 *文章所属专题
 	 *
	 * @hibernate.property column="special_id"
	 * @return Long 
	 */
    public Long getSpecialId() {
        return specialId;
    }

    public void setSpecialId(Long specialId) {
        this.specialId = specialId;
    }

    /**
	 *上传图片url
 	 *
	 * @hibernate.property column="image_url"
	 * @return String 
	 */
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
	 *上传附件url
 	 *
	 * @hibernate.property column="download_url"
	 * @return String 
	 */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
	 *文章标题简称
 	 *
	 * @hibernate.property column="short_title"
	 * @return String 
	 */
    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    /**
	 *文章等级
 	 *
	 * @hibernate.property column="grade"
	 * @return Long 
	 */
    public Long getGrade() {
        return grade;
    }

    public void setGrade(Long grade) {
        this.grade = grade;
    }

    /**
	 *文章序号
 	 *
	 * @hibernate.property column="order_num"
	 * @return Long 
	 */
    public Long getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Long orderNum) {
        this.orderNum = orderNum;
    }

    /**
	 *文章标题
 	 *
	 * @hibernate.property column="lastindex_time"
	 * @return java.util.Date 
	 */
    public java.util.Date getLastindexTime() {
        return lastindexTime;
    }

    public void setLastindexTime(java.util.Date lastindexTime) {
        this.lastindexTime = lastindexTime;
    }

    /**
	 *创建人
 	 *
	 * @hibernate.property column="create_author"
	 * @return String 
	 */
    public String getCreateAuthor() {
        return createAuthor;
    }

    public void setCreateAuthor(String createAuthor) {
        this.createAuthor = createAuthor;
    }

    /**
	 *审核人
 	 *
	 * @hibernate.property column="audit_author"
	 * @return String 
	 */
    public String getAuditAuthor() {
        return auditAuthor;
    }

    public void setAuditAuthor(String auditAuthor) {
        this.auditAuthor = auditAuthor;
    }

    /**
	 *发布人
 	 *
	 * @hibernate.property column="publish_author"
	 * @return String 
	 */
    public String getPublishAuthor() {
        return publishAuthor;
    }

    public void setPublishAuthor(String publishAuthor) {
        this.publishAuthor = publishAuthor;
    }

    /**
	 *文章标题
 	 *
	 * @hibernate.property column="name"
	 * @return String 
	 */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
	 *创建时间
 	 *
	 * @hibernate.property column="created_time"
	 * @return java.util.Date 
	 */
    public java.util.Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(java.util.Date createdTime) {
        this.createdTime = createdTime;
    }

    /**
	 *更新时间
 	 *
	 * @hibernate.property column="updated_time"
	 * @return java.util.Date 
	 */
    public java.util.Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(java.util.Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    /**
	 *创建人id
 	 *
	 * @hibernate.property column="create_authorid"
	 * @return Long 
	 */
    public Long getCreateAuthorid() {
        return createAuthorid;
    }

    public void setCreateAuthorid(Long createAuthorid) {
        this.createAuthorid = createAuthorid;
    }

    /**
	 *审核人id
 	 *
	 * @hibernate.property column="audit_authorid"
	 * @return Long 
	 */
    public Long getAuditAuthorid() {
        return auditAuthorid;
    }

    public void setAuditAuthorid(Long auditAuthorid) {
        this.auditAuthorid = auditAuthorid;
    }

    /**
	 *编号
 	 *
	 * @hibernate.property column="channel_id"
	 * @return Long 
	 */
    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    /**
	 *编号
 	 *
	 * @hibernate.property column="template_id"
	 * @return Long 
	 */
    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    /**
	 *发布人id
 	 *
	 * @hibernate.property column="publish_authorid"
	 * @return Long 
	 */
    public Long getPublishAuthorid() {
        return publishAuthorid;
    }

    public void setPublishAuthorid(Long publishAuthorid) {
        this.publishAuthorid = publishAuthorid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentEntity that = (DocumentEntity) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? DocumentEntity.class.hashCode() + id.hashCode() : DocumentEntity.class.hashCode();
    }
}
