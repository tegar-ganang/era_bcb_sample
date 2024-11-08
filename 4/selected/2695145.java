package com.wwg.cms.bo.entity;

import com.wwg.cms.bo.*;

/**
 *create auto on 2010-8-31 16:43:18
 *create by wwl
 * channel
 * 栏目表维护 
 * @hibernate.class table="channel" *  mutable="true" 
 *  proxy="com.wwg.cms.bo.entity.ChannelEntity" 
 */
public class ChannelEntity implements Channel {

    private Long id;

    private String bzId;

    private String shortName;

    private String remark;

    private Long parentId;

    private String channelUsetemplate;

    private String documentViewtemplate;

    private String target;

    private Boolean status;

    private String workflow;

    private Long indexType;

    private Long orderNum;

    private String firstFilterfield;

    private String secondFilterfield;

    private Boolean isshowAlldoc;

    private String name;

    private java.util.Date createdTime;

    private java.util.Date updatedTime;

    /**
	 *编号
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
	 *栏目标志符
 	 *
	 * @hibernate.property column="bz_id"
	 * @return String 
	 */
    public String getBzId() {
        return bzId;
    }

    public void setBzId(String bzId) {
        this.bzId = bzId;
    }

    /**
	 *栏目简称
 	 *
	 * @hibernate.property column="short_name"
	 * @return String 
	 */
    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
	 *栏目描述
 	 *
	 * @hibernate.property column="remark"
	 * @return String 
	 */
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
	 *栏目父节点
 	 *
	 * @hibernate.property column="parent_id"
	 * @return Long 
	 */
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
	 *本级栏目使用模板
 	 *
	 * @hibernate.property column="channel_usetemplate"
	 * @return String 
	 */
    public String getChannelUsetemplate() {
        return channelUsetemplate;
    }

    public void setChannelUsetemplate(String channelUsetemplate) {
        this.channelUsetemplate = channelUsetemplate;
    }

    /**
	 *本级栏目预览文章的模板
 	 *
	 * @hibernate.property column="document_viewtemplate"
	 * @return String 
	 */
    public String getDocumentViewtemplate() {
        return documentViewtemplate;
    }

    public void setDocumentViewtemplate(String documentViewtemplate) {
        this.documentViewtemplate = documentViewtemplate;
    }

    /**
	 *栏目目标窗口
 	 *
	 * @hibernate.property column="target"
	 * @return String 
	 */
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
	 *栏目是否在线
 	 *
	 * @hibernate.property column="status"
	 * @return Boolean 
	 */
    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    /**
	 *栏目审批文章的方式
 	 *
	 * @hibernate.property column="workflow"
	 * @return String 
	 */
    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    /**
	 *栏目的索引方式
 	 *
	 * @hibernate.property column="index_type"
	 * @return Long 
	 */
    public Long getIndexType() {
        return indexType;
    }

    public void setIndexType(Long indexType) {
        this.indexType = indexType;
    }

    /**
	 *栏目的排序方式
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
	 *
 	 *
	 * @hibernate.property column="first_filterfield"
	 * @return String 
	 */
    public String getFirstFilterfield() {
        return firstFilterfield;
    }

    public void setFirstFilterfield(String firstFilterfield) {
        this.firstFilterfield = firstFilterfield;
    }

    /**
	 *
 	 *
	 * @hibernate.property column="second_filterfield"
	 * @return String 
	 */
    public String getSecondFilterfield() {
        return secondFilterfield;
    }

    public void setSecondFilterfield(String secondFilterfield) {
        this.secondFilterfield = secondFilterfield;
    }

    /**
	 *是否显示此栏目下级栏目的所有文章(0，显示此栏目和下级栏目的所有文章;1,显示当前栏目下的文章)
 	 *
	 * @hibernate.property column="isshow_alldoc"
	 * @return Boolean 
	 */
    public Boolean getIsshowAlldoc() {
        return isshowAlldoc;
    }

    public void setIsshowAlldoc(Boolean isshowAlldoc) {
        this.isshowAlldoc = isshowAlldoc;
    }

    /**
	 *栏目名称
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
	 *修改时间
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelEntity that = (ChannelEntity) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? ChannelEntity.class.hashCode() + id.hashCode() : ChannelEntity.class.hashCode();
    }
}
