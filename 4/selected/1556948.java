package com.wwfish.cms.model;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-7-30
 * Time: 14:21:53
 * To change this template use File | Settings | File Templates.
 */
public class ChannelDto extends BaseDto {

    private String shortName;

    private String description;

    private Long parentId;

    private String channelTemplate;

    private String contentTemplate;

    private String url;

    private Boolean status;

    private String workflow;

    private String indexType;

    private String firstFilterField;

    private String name;

    private String variety;

    private ChannelDto parentExt;

    private List<ChannelDto> children;

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getChannelTemplate() {
        return channelTemplate;
    }

    public void setChannelTemplate(String channelTemplate) {
        this.channelTemplate = channelTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public void setContentTemplate(String contentTemplate) {
        this.contentTemplate = contentTemplate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getFirstFilterField() {
        return firstFilterField;
    }

    public void setFirstFilterField(String firstFilterField) {
        this.firstFilterField = firstFilterField;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public ChannelDto getParentExt() {
        return parentExt;
    }

    public void setParentExt(ChannelDto parentExt) {
        this.parentExt = parentExt;
    }

    public List<ChannelDto> getChildren() {
        return children;
    }

    public void setChildren(List<ChannelDto> children) {
        this.children = children;
    }
}
