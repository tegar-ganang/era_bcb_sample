package com.google.code.wifimaps.entidade;

import java.math.BigDecimal;
import java.util.Date;

public class WifiNetwork {

    private Long wifiId;

    private String wep;

    private String macAddress;

    private String ssid;

    private String Type;

    private Integer rssi;

    private Integer channel;

    private Integer maxRssi;

    private Integer minRssi;

    private Integer hdop;

    private BigDecimal latPosition;

    private BigDecimal lonPosition;

    private Date lastSeen;

    public Long getWifiId() {
        return wifiId;
    }

    public void setWifiId(Long wifiId) {
        this.wifiId = wifiId;
    }

    public String getWep() {
        return wep;
    }

    public void setWep(String wep) {
        this.wep = wep;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getMaxRssi() {
        return maxRssi;
    }

    public void setMaxRssi(Integer maxRssi) {
        this.maxRssi = maxRssi;
    }

    public Integer getMinRssi() {
        return minRssi;
    }

    public void setMinRssi(Integer minRssi) {
        this.minRssi = minRssi;
    }

    public Integer getHdop() {
        return hdop;
    }

    public void setHdop(Integer hdop) {
        this.hdop = hdop;
    }

    public BigDecimal getLatPosition() {
        return latPosition;
    }

    public void setLatPosition(BigDecimal latPosition) {
        this.latPosition = latPosition;
    }

    public BigDecimal getLonPosition() {
        return lonPosition;
    }

    public void setLonPosition(BigDecimal lonPosition) {
        this.lonPosition = lonPosition;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
}
