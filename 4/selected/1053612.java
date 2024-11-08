package cn.mmbook.platform.model.manage;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import java.util.*;
import javacommon.base.*;
import javacommon.util.*;
import cn.org.rapid_framework.util.*;
import cn.org.rapid_framework.web.util.*;
import cn.org.rapid_framework.page.*;
import cn.org.rapid_framework.page.impl.*;
import cn.mmbook.platform.model.manage.*;
import cn.mmbook.platform.dao.manage.*;
import cn.mmbook.platform.service.manage.impl.*;
import cn.mmbook.platform.service.manage.*;

/**
 * <p> SiteChannels 数据类 <br> 
 * <p>   <br>
 * @author admin,
 * @version 1.0. 2010-07-08
 *
 */
public class SiteChannels extends BaseEntity {

    public static final String FORMAT_INSERT_TIME_ = DATE_TIME_FORMAT;

    private java.lang.Integer id;

    private java.lang.String channelsName;

    private java.lang.Integer showType;

    private java.lang.Integer sortValue;

    private java.lang.Integer effective;

    private java.lang.String channelsNotes;

    private java.sql.Date insertTime;

    /**网站ID*/
    private java.lang.String siteId;

    /**LogoUrl*/
    private java.lang.String logoUrl;

    /**频道对应的栏目集合*/
    private java.util.List sitePartList;

    /**前台页面提交的频道关联栏目ID集合*/
    private java.lang.String partIds;

    /**标签里读取的名称*/
    private java.lang.String name;

    private SitePart sitePart;

    public SitePart getSitePart() {
        return sitePart;
    }

    public void setSitePart(SitePart sitePart) {
        this.sitePart = sitePart;
    }

    public SiteChannels() {
    }

    public SiteChannels(java.lang.Integer id) {
        this.id = id;
    }

    public void setId(java.lang.Integer value) {
        this.id = value;
    }

    public java.lang.Integer getId() {
        return this.id;
    }

    public void setChannelsName(java.lang.String value) {
        this.channelsName = value;
    }

    public java.lang.String getChannelsName() {
        return this.channelsName;
    }

    public void setShowType(java.lang.Integer value) {
        this.showType = value;
    }

    public java.lang.Integer getShowType() {
        return this.showType;
    }

    public void setSortValue(java.lang.Integer value) {
        this.sortValue = value;
    }

    public java.lang.Integer getSortValue() {
        return this.sortValue;
    }

    public void setEffective(java.lang.Integer value) {
        this.effective = value;
    }

    public java.lang.Integer getEffective() {
        return this.effective;
    }

    public void setChannelsNotes(java.lang.String value) {
        this.channelsNotes = value;
    }

    public java.lang.String getChannelsNotes() {
        return this.channelsNotes;
    }

    public java.sql.Date getInsertTime() {
        return this.insertTime;
    }

    public void setInsertTimeString(String value) {
        setInsertTime(string2Date(value, FORMAT_INSERT_TIME_, java.sql.Date.class));
    }

    public void setInsertTime(java.sql.Date value) {
        this.insertTime = value;
    }

    public String getInsertTimeString() {
        return date2String(getInsertTime(), FORMAT_INSERT_TIME_);
    }

    public java.lang.String getSiteId() {
        return siteId;
    }

    public void setSiteId(java.lang.String siteId) {
        this.siteId = siteId;
    }

    public java.lang.String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(java.lang.String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public java.util.List getSitePartList() {
        return sitePartList;
    }

    public void setSitePartList(java.util.List sitePartList) {
        this.sitePartList = sitePartList;
    }

    public java.lang.String getPartIds() {
        return partIds;
    }

    public void setPartIds(java.lang.String partIds) {
        this.partIds = partIds;
    }

    public java.lang.String getName() {
        return name;
    }

    public void setName(java.lang.String name) {
        this.name = name;
    }

    /******************************************/
    public String toString() {
        return new ToStringBuilder(this).append("Id", getId()).append("ChannelsName", getChannelsName()).append("ShowType", getShowType()).append("SortValue", getSortValue()).append("Effective", getEffective()).append("ChannelsNotes", getChannelsNotes()).append("InsertTime", getInsertTime()).append("SiteId", getSiteId()).append("LogoUrl", getLogoUrl()).append("PartIds", getPartIds()).toString();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getId()).append(getChannelsName()).append(getShowType()).append(getSortValue()).append(getEffective()).append(getChannelsNotes()).append(getInsertTime()).append(getSiteId()).append(getLogoUrl()).append(getPartIds()).toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof SiteChannels == false) return false;
        if (this == obj) return true;
        SiteChannels other = (SiteChannels) obj;
        return new EqualsBuilder().append(getId(), other.getId()).append(getChannelsName(), other.getChannelsName()).append(getShowType(), other.getShowType()).append(getSortValue(), other.getSortValue()).append(getEffective(), other.getEffective()).append(getChannelsNotes(), other.getChannelsNotes()).append(getInsertTime(), other.getInsertTime()).append(getSiteId(), other.getSiteId()).append(getLogoUrl(), other.getLogoUrl()).append(getPartIds(), other.getPartIds()).isEquals();
    }
}
