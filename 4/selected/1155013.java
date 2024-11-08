package com.creawor.hz_market.t_channel;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.SQLException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class t_channel implements Serializable {

    private int id = 0;

    public int getId() {
        return id;
    }

    public void setId(int parm) {
        id = parm;
    }

    private java.util.Date insert_day;

    private String service_hall_code;

    public String getService_hall_code() {
        return service_hall_code;
    }

    public void setService_hall_code(String parm) {
        service_hall_code = parm;
    }

    private String service_hall_name;

    public String getService_hall_name() {
        return service_hall_name;
    }

    public void setService_hall_name(String parm) {
        service_hall_name = parm;
    }

    private String channel_type;

    public String getChannel_type() {
        return channel_type;
    }

    public void setChannel_type(String parm) {
        channel_type = parm;
    }

    private String star_level;

    public String getStar_level() {
        return star_level;
    }

    public void setStar_level(String parm) {
        star_level = parm;
    }

    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String parm) {
        address = parm;
    }

    private String company;

    public String getCompany() {
        return company;
    }

    public void setCompany(String parm) {
        company = parm;
    }

    private String contact_man;

    public String getContact_man() {
        return contact_man;
    }

    public void setContact_man(String parm) {
        contact_man = parm;
    }

    private String contact_tel;

    public String getContact_tel() {
        return contact_tel;
    }

    public void setContact_tel(String parm) {
        contact_tel = parm;
    }

    private String contact_mobile;

    public String getContact_mobile() {
        return contact_mobile;
    }

    public void setContact_mobile(String parm) {
        contact_mobile = parm;
    }

    private double rent = 0;

    public double getRent() {
        return rent;
    }

    public void setRent(double parm) {
        rent = parm;
    }

    private String town;

    public String getTown() {
        return town;
    }

    public void setTown(String parm) {
        town = parm;
    }

    private String towncode;

    public String getTowncode() {
        return towncode;
    }

    public void setTowncode(String parm) {
        towncode = parm;
    }

    private String county;

    public String getCounty() {
        return county;
    }

    public void setCounty(String parm) {
        county = parm;
    }

    private String county_code;

    public String getCounty_code() {
        return county_code;
    }

    public void setCounty_code(String parm) {
        county_code = parm;
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

    private int zoom = 0;

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int parm) {
        zoom = parm;
    }

    private String village_xz;

    private String village_zr;

    public String getVillage_xz() {
        return village_xz;
    }

    public void setVillage_xz(String village_xz) {
        this.village_xz = village_xz;
    }

    public String getVillage_zr() {
        return village_zr;
    }

    public void setVillage_zr(String village_zr) {
        this.village_zr = village_zr;
    }

    private String parent;

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    private String jindu;

    public String getJindu() {
        return jindu;
    }

    public void setJindu(String strparm) {
        jindu = strparm;
    }

    private String main_type;

    public String getMain_type() {
        return main_type;
    }

    public void setMain_type(String main_type) {
        this.main_type = main_type;
    }

    public java.util.Date getInsert_day() {
        return insert_day;
    }

    public void setInsert_day(java.util.Date insert_day) {
        this.insert_day = insert_day;
    }

    private String iscomplete;

    public String getIscomplete() {
        return iscomplete;
    }

    public void setIscomplete(String iscomplete) {
        this.iscomplete = iscomplete;
    }
}
