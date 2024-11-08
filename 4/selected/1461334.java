package com.creawor.hz_market.t_channel_sale;

import java.io.Serializable;
import java.util.Date;

public class t_channel_sale implements Serializable {

    private int id = 0;

    public int getId() {
        return id;
    }

    public void setId(int parm) {
        id = parm;
    }

    private String the_month;

    public String getThe_month() {
        return the_month;
    }

    public void setThe_month(String parm) {
        the_month = parm;
    }

    private String channel_code;

    public String getChannel_code() {
        return channel_code;
    }

    public void setChannel_code(String parm) {
        channel_code = parm;
    }

    private String channel_name;

    public String getChannel_name() {
        return channel_name;
    }

    public void setChannel_name(String parm) {
        channel_name = parm;
    }

    private java.util.Date updated_day;

    public java.util.Date getUpdated_day() {
        return updated_day;
    }

    public void setUpdated_day(java.util.Date parm) {
        updated_day = parm;
    }

    private double recompense = 0;

    public double getRecompense() {
        return recompense;
    }

    public void setRecompense(double parm) {
        recompense = parm;
    }

    private float charge_avg = 0;

    public float getCharge_avg() {
        return charge_avg;
    }

    public void setCharge_avg(float parm) {
        charge_avg = parm;
    }

    private float card_sale_avg = 0;

    public float getCard_sale_avg() {
        return card_sale_avg;
    }

    public void setCard_sale_avg(float parm) {
        card_sale_avg = parm;
    }

    private float card_apply_avg = 0;

    public float getCard_apply_avg() {
        return card_apply_avg;
    }

    public void setCard_apply_avg(float parm) {
        card_apply_avg = parm;
    }

    private java.util.Date insert_day;

    public java.util.Date getInsert_day() {
        return insert_day;
    }

    public void setInsert_day(java.util.Date insert_day) {
        this.insert_day = insert_day;
    }

    public void setInsertDay(String dayStr) {
        Date date = com.creawor.km.util.DateUtil.parse(dayStr, null);
        this.insert_day = date;
    }

    private String opentype;

    public String getOpentype() {
        return opentype;
    }

    public void setOpentype(String opentype) {
        this.opentype = opentype;
    }

    private String company;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}
