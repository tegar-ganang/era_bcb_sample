package com.creawor.hz_market.t_town_base;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.SQLException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class t_town_base implements Serializable {

    private int id = 0;

    public int getId() {
        return id;
    }

    public void setId(int parm) {
        id = parm;
    }

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String parm) {
        code = parm;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String parm) {
        name = parm;
    }

    private String county;

    public String getCounty() {
        return county;
    }

    public void setCounty(String parm) {
        county = parm;
    }

    private java.util.Date insert_day;

    public java.util.Date getInsert_day() {
        return insert_day;
    }

    public void setInsert_day(java.util.Date parm) {
        insert_day = parm;
    }

    private int population = 0;

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int parm) {
        population = parm;
    }

    private String sec;

    public String getSec() {
        return sec;
    }

    public void setSec(String parm) {
        sec = parm;
    }

    private String alcalde;

    public String getAlcalde() {
        return alcalde;
    }

    public void setAlcalde(String parm) {
        alcalde = parm;
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

    private String xzc_num;

    private String yryc_num;

    private String short_net_num;

    private String channel_num;

    private String manager_num;

    private String d_manager_num;

    private String l_manager_num;

    public String getChannel_num() {
        return channel_num;
    }

    public void setChannel_num(String channel_num) {
        this.channel_num = channel_num;
    }

    public String getD_manager_num() {
        return d_manager_num;
    }

    public void setD_manager_num(String d_manager_num) {
        this.d_manager_num = d_manager_num;
    }

    public String getL_manager_num() {
        return l_manager_num;
    }

    public void setL_manager_num(String l_manager_num) {
        this.l_manager_num = l_manager_num;
    }

    public String getManager_num() {
        return manager_num;
    }

    public void setManager_num(String manager_num) {
        this.manager_num = manager_num;
    }

    public String getShort_net_num() {
        return short_net_num;
    }

    public void setShort_net_num(String short_net_num) {
        this.short_net_num = short_net_num;
    }

    public String getXzc_num() {
        return xzc_num;
    }

    public void setXzc_num(String xzc_num) {
        this.xzc_num = xzc_num;
    }

    public String getYryc_num() {
        return yryc_num;
    }

    public void setYryc_num(String yryc_num) {
        this.yryc_num = yryc_num;
    }
}
