package com.creawor.hz_market.t_village_signal;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class t_village_signal_Form extends ActionForm {

    public t_village_signal_Form() {
        super();
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String strparm) {
        id = strparm;
    }

    private String village_code;

    public String getVillage_code() {
        return village_code;
    }

    public void setVillage_code(String strparm) {
        village_code = strparm;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String strparm) {
        name = strparm;
    }

    private String signal;

    public String getSignal() {
        return signal;
    }

    public void setSignal(String strparm) {
        signal = strparm;
    }

    private String town;

    public String getTown() {
        return town;
    }

    public void setTown(String strparm) {
        town = strparm;
    }

    private String village_xz;

    public String getVillage_xz() {
        return village_xz;
    }

    public void setVillage_xz(String strparm) {
        village_xz = strparm;
    }

    private String village_zr;

    public String getVillage_zr() {
        return village_zr;
    }

    public void setVillage_zr(String strparm) {
        village_zr = strparm;
    }

    private String county;

    public String getCounty() {
        return county;
    }

    public void setCounty(String strparm) {
        county = strparm;
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

    private String village;

    private String sound_quality;

    private String cover_num;

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

    public String getCover_num() {
        return cover_num;
    }

    public void setCover_num(String cover_num) {
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
