package com.jason.oa.entity.wltg;

public class TbUser implements java.io.Serializable {

    private Long id;

    private String name;

    private String pwd;

    private String channels;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return this.pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getChannels() {
        return this.channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }
}
