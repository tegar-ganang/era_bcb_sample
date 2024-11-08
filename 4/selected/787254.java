package com.creawor.hz_market.t_village_signal;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.SQLException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class t_village_signal implements Serializable {

    private int id = 0;

    public int getId() {
        return id;
    }

    public void setId(int parm) {
        id = parm;
    }

    private String village_code;

    public String getVillage_code() {
        return village_code;
    }

    public void setVillage_code(String parm) {
        village_code = parm;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String parm) {
        name = parm;
    }

    private String signal;

    public String getSignal() {
        return signal;
    }

    public void setSignal(String parm) {
        signal = parm;
    }

    private String town;

    public String getTown() {
        return town;
    }

    public void setTown(String parm) {
        town = parm;
    }

    private String village_xz;

    public String getVillage_xz() {
        return village_xz;
    }

    public void setVillage_xz(String parm) {
        village_xz = parm;
    }

    private String village_zr;

    public String getVillage_zr() {
        return village_zr;
    }

    public void setVillage_zr(String parm) {
        village_zr = parm;
    }

    private String county;

    public String getCounty() {
        return county;
    }

    public void setCounty(String parm) {
        county = parm;
    }

    private String channel_code;

    private String is_covered;

    public String getChannel_code() {
        return channel_code;
    }

    public void setChannel_code(String channel_code) {
        this.channel_code = channel_code;
    }

    public String getIs_covered() {
        return is_covered;
    }

    public void setIs_covered(String is_covered) {
        this.is_covered = is_covered;
    }

    private java.util.Date insert_day;

    public java.util.Date getInsert_day() {
        return insert_day;
    }

    public void setInsert_day(java.util.Date insert_day) {
        this.insert_day = insert_day;
    }

    private String opentype;

    public String getOpentype() {
        return opentype;
    }

    public void setOpentype(String opentype) {
        this.opentype = opentype;
    }

    private String village;

    private String sound_quality;

    private int cover_num = 0;

    private String rival_info;

    private String covered_name;

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getSound_quality() {
        return sound_quality;
    }

    public void setSound_quality(String sound_quality) {
        this.sound_quality = sound_quality;
    }

    public int getCover_num() {
        return cover_num;
    }

    public void setCover_num(int cover_num) {
        this.cover_num = cover_num;
    }

    public String getRival_info() {
        return rival_info;
    }

    public void setRival_info(String rival_info) {
        this.rival_info = rival_info;
    }

    public String getCovered_name() {
        return covered_name;
    }

    public void setCovered_name(String covered_name) {
        this.covered_name = covered_name;
    }

    private double x = 0;

    public double getX() {
        return x;
    }

    public void setX(double parm) {
        x = parm;
    }

    private double y = 0;

    public double getY() {
        return y;
    }

    public void setY(double parm) {
        y = parm;
    }

    private String iscreate_net;

    private String iseveryone;

    public String getIscreate_net() {
        return iscreate_net;
    }

    public void setIscreate_net(String iscreate_net) {
        this.iscreate_net = iscreate_net;
    }

    public String getIseveryone() {
        return iseveryone;
    }

    public void setIseveryone(String iseveryone) {
        this.iseveryone = iseveryone;
    }
}
