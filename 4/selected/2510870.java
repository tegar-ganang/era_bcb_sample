package org.damour.base.client.objects;

import java.io.Serializable;

public class Referral implements Serializable, IHibernateFriendly {

    public Long id;

    public PermissibleObject subject;

    public String url;

    public String referralURL;

    public String ip;

    public Long initialDate = System.currentTimeMillis();

    public Long recentDate = System.currentTimeMillis();

    public Long counter = 0L;

    public Referral() {
    }

    public String getCachePolicy() {
        return "nonstrict-read-write";
    }

    public boolean isLazy() {
        return false;
    }

    public boolean isFieldUnique(String fieldName) {
        return false;
    }

    public boolean isFieldKey(String fieldName) {
        return false;
    }

    public String getFieldType(String fieldName) {
        return null;
    }

    public int getFieldLength(String fieldName) {
        if ("url".equalsIgnoreCase(fieldName)) {
            return 1024;
        } else if ("referralURL".equalsIgnoreCase(fieldName)) {
            return 1024;
        }
        return -1;
    }

    public String getSqlUpdate() {
        return null;
    }

    public boolean isFieldMapped(String fieldName) {
        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferralURL() {
        return referralURL;
    }

    public void setReferralURL(String referralURL) {
        this.referralURL = referralURL;
    }

    public PermissibleObject getSubject() {
        return subject;
    }

    public void setSubject(PermissibleObject subject) {
        this.subject = subject;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getInitialDate() {
        return initialDate;
    }

    public void setInitialDate(Long initialDate) {
        this.initialDate = initialDate;
    }

    public Long getRecentDate() {
        return recentDate;
    }

    public void setRecentDate(Long recentDate) {
        this.recentDate = recentDate;
    }

    public Long getCounter() {
        return counter;
    }

    public void setCounter(Long counter) {
        this.counter = counter;
    }
}
