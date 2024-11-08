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
 * 频道与栏目关联 数据类
 * <p> SiteChannelsPartReal 数据类 <br> 
 * <p>   <br>
 * @author admin,
 * @version 1.0. 2010-07-08
 *
 */
public class SiteChannelsPartReal extends BaseEntity {

    private java.lang.Integer id;

    /**频道ID*/
    private java.lang.Integer channelsId;

    /**栏目ID*/
    private java.lang.Integer partId;

    public SiteChannelsPartReal() {
    }

    public SiteChannelsPartReal(java.lang.Integer id) {
        this.id = id;
    }

    public void setId(java.lang.Integer value) {
        this.id = value;
    }

    public java.lang.Integer getId() {
        return this.id;
    }

    public void setChannelsId(java.lang.Integer value) {
        this.channelsId = value;
    }

    public java.lang.Integer getChannelsId() {
        return this.channelsId;
    }

    public void setPartId(java.lang.Integer value) {
        this.partId = value;
    }

    public java.lang.Integer getPartId() {
        return this.partId;
    }

    public String toString() {
        return new ToStringBuilder(this).append("Id", getId()).append("ChannelsId", getChannelsId()).append("PartId", getPartId()).toString();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getId()).append(getChannelsId()).append(getPartId()).toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof SiteChannelsPartReal == false) return false;
        if (this == obj) return true;
        SiteChannelsPartReal other = (SiteChannelsPartReal) obj;
        return new EqualsBuilder().append(getId(), other.getId()).append(getChannelsId(), other.getChannelsId()).append(getPartId(), other.getPartId()).isEquals();
    }
}
