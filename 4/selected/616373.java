package com.volantis.mps.bms.impl;

import com.volantis.mps.bms.Address;
import com.volantis.mps.bms.Recipient;
import com.volantis.mps.bms.RecipientType;

public class DefaultRecipient implements Recipient {

    private Address address;

    private String channel;

    private String deviceName;

    private RecipientType type = RecipientType.TO;

    private String failureReason;

    public DefaultRecipient() {
    }

    public DefaultRecipient(Address address, String deviceName) {
        setAddress(address);
        setDeviceName(deviceName);
    }

    public void setAddress(Address address) {
        if (null == address) {
            throw new IllegalArgumentException("address cannot be null");
        }
        this.address = address;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return this.channel;
    }

    public void setDeviceName(String name) {
        if (null == name) {
            throw new IllegalArgumentException("device name cannot be null");
        }
        this.deviceName = name;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setRecipientType(RecipientType type) {
        this.type = type;
    }

    public RecipientType getRecipientType() {
        return this.type;
    }

    public void setFailureReason(String reason) {
        this.failureReason = reason;
    }

    public String getFailureReason() {
        return this.failureReason;
    }

    public String toString() {
        return "[recipient: address=" + (null != this.address ? this.address.getValue() : "null") + ", device=" + this.deviceName + ", type=" + (null != this.type ? this.type.name() : "null") + ", channel=" + this.channel + ", failureReason=" + this.failureReason + "]";
    }
}
