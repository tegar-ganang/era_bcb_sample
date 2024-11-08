package com.creawor.hz_market.t_town_base;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class t_town_base_Form extends ActionForm {

    public t_town_base_Form() {
        super();
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String strparm) {
        id = strparm;
    }

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String strparm) {
        code = strparm;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String strparm) {
        name = strparm;
    }

    private String county;

    public String getCounty() {
        return county;
    }

    public void setCounty(String strparm) {
        county = strparm;
    }

    private String insert_day;

    public String getInsert_day() {
        return insert_day;
    }

    public void setInsert_day(String strparm) {
        insert_day = strparm;
    }

    private String population;

    public String getPopulation() {
        return population;
    }

    public void setPopulation(String strparm) {
        population = strparm;
    }

    private String sec;

    public String getSec() {
        return sec;
    }

    public void setSec(String strparm) {
        sec = strparm;
    }

    private String alcalde;

    public String getAlcalde() {
        return alcalde;
    }

    public void setAlcalde(String strparm) {
        alcalde = strparm;
    }

    private String x;

    public String getX() {
        return x;
    }

    public void setX(String strparm) {
        x = strparm;
    }

    private String y;

    public String getY() {
        return y;
    }

    public void setY(String strparm) {
        y = strparm;
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
