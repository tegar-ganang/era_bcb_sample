package ru.pit.tvlist.persistence.domain;

import java.util.Date;

/**
 * 
 * @author Peter Salnikov (netman@yandex.ru)
 * 
 * @hibernate.class 
 *     lazy="false" 
 *     table="TV_BROADCAST"
 * 
 * @hibernate.cache 
 *     usage="read-write"
 */
public class Broadcast implements IDomainObject {

    private Long id;

    private Date date;

    private String name;

    private String descr;

    private String picName;

    private Long type;

    private String extId;

    private Channel channel;

    /**
     * Internal ID
     * 
     * @hibernate.id 
     *     column="id" 
     *     generator-class="identity"
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * External ID
     * 
     * @hibernate.property 
     *     column="externalId" 
     *     non-null="true"
     */
    public String getExtId() {
        return extId;
    }

    public void setExtId(String ext_id) {
        this.extId = ext_id;
    }

    /**
     * Broadcast date/time
     *  
     * @hibernate.property 
     *     column="date" 
     *     non-null="true"
     */
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Broadcast description
     *  
     * @hibernate.property 
     *     column="descr" 
     *     non-null="true"
     */
    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    /**
     * Broadcast name
     *  
     * @hibernate.property 
     *     column="name" 
     *     non-null="true"
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Broadcast picture name
     *  
     * @hibernate.property 
     *     column="picName" 
     *     non-null="true"
     */
    public String getPicName() {
        return picName;
    }

    public void setPicName(String pic) {
        this.picName = pic;
    }

    /**
     * Broadcast type
     * 
     * TODO implement RefLists
     *  
     * @hibernate.property 
     *     column="type" 
     *     non-null="true"
     */
    public Long getType() {
        return type;
    }

    public void setType(Long type) {
        this.type = type;
    }

    /**
     * Broadcast channel
     * 
     * @hibernate.many-to-one 
     *     column="channelId"
     *     not-null="true"
     */
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
