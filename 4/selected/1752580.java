package com.creawor.hz_market.t_channel_sale;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class t_channel_sale_Form extends ActionForm {

    public t_channel_sale_Form() {
        super();
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String strparm) {
        id = strparm;
    }

    private String year;

    private String month;

    private String day;

    private String insertDay;

    public String getInsertDay() {
        if (null == insertDay) {
            if (null != month) {
                if (month.length() < 2) month = "0" + month;
            }
            insertDay = year + "-" + month;
            if (null != day) {
                insertDay = insertDay + "-" + day;
            } else {
                insertDay = insertDay + "-15";
            }
            insertDay = insertDay + " 00:00:00";
        }
        return insertDay;
    }

    public void setInsertDay(String insertDay) {
        this.insertDay = insertDay;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMonth() {
        if (null == month) {
            if (null != this.insert_day && !"".equals(this.insert_day.trim())) {
                month = insert_day.substring(5, 7);
            }
        }
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        if (null == year) {
            if (null != this.insert_day && !"".equals(this.insert_day.trim())) {
                year = insert_day.substring(0, 4);
            }
        }
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    private String the_month;

    public String getThe_month() {
        return the_month;
    }

    public void setThe_month(String strparm) {
        the_month = strparm;
    }

    private String channel_code;

    public String getChannel_code() {
        return channel_code;
    }

    public void setChannel_code(String strparm) {
        channel_code = strparm;
    }

    private String channel_name;

    public String getChannel_name() {
        return channel_name;
    }

    public void setChannel_name(String strparm) {
        channel_name = strparm;
    }

    private String updated_day;

    public String getUpdated_day() {
        return updated_day;
    }

    public void setUpdated_day(String strparm) {
        updated_day = strparm;
    }

    private String recompense;

    public String getRecompense() {
        return recompense;
    }

    public void setRecompense(String strparm) {
        recompense = strparm;
    }

    private String charge_avg;

    public String getCharge_avg() {
        return charge_avg;
    }

    public void setCharge_avg(String strparm) {
        charge_avg = strparm;
    }

    private String card_sale_avg;

    public String getCard_sale_avg() {
        return card_sale_avg;
    }

    public void setCard_sale_avg(String strparm) {
        card_sale_avg = strparm;
    }

    private String card_apply_avg;

    public String getCard_apply_avg() {
        return card_apply_avg;
    }

    public void setCard_apply_avg(String strparm) {
        card_apply_avg = strparm;
    }

    private String insert_day;

    public String getInsert_day() {
        return insert_day;
    }

    public void setInsert_day(String insert_day) {
        this.insert_day = insert_day;
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
