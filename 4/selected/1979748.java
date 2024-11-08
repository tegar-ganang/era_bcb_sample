package com.google.code.sagetvaddons.sagerss.client;

import java.util.Date;
import com.google.gwt.user.client.rpc.IsSerializable;

public class SageShow implements IsSerializable {

    private String id;

    private int airingId;

    private String title;

    private String subtitle;

    private String channel;

    private String description;

    private Date start;

    private Date end;

    private String year;

    private String category;

    private String subcategory;

    public SageShow() {
    }

    public SageShow(String id) {
        this.id = id;
    }

    public int getAiringId() {
        return airingId;
    }

    public void setAiringId(int id) {
        airingId = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }

    public Date getStart() {
        return (Date) start.clone();
    }

    public Date getEnd() {
        return (Date) end.clone();
    }

    public String getYear() {
        return year;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStart(Date start) {
        this.start = (Date) start.clone();
    }

    public void setEnd(Date end) {
        this.end = (Date) end.clone();
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof SageShow) {
            SageShow s = (SageShow) o;
            return s.getId().equals(getId());
        }
        return false;
    }
}
