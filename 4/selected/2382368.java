package com.cbsgmbh.xi.ad.edifact.module.crypt.test;

import com.cbsgmbh.xi.af.edifact.module.crypt.configuration.ConfigurationSettings;

public class ConfigurationTestImpl implements ConfigurationSettings {

    private String certificateForEncryption;

    private String certificateForSignature;

    private String channelId;

    private boolean cryptMethodEnc;

    private boolean cryptMethodSig;

    private String cryptalgSym;

    private boolean mdnRequired;

    private String messageType;

    private String micAlgMdn;

    private String micAlgReq;

    private String viewCertificateForEncryption;

    private String viewCertificateForSignature;

    private String xiPartyFrom;

    private String xiPartyTo;

    public String getCertificateForEncryption() {
        return certificateForEncryption;
    }

    public void setCertificateForEncryption(String certificateForEncryption) {
        this.certificateForEncryption = certificateForEncryption;
    }

    public String getCertificateForSignature() {
        return certificateForSignature;
    }

    public void setCertificateForSignature(String certificateForSignature) {
        this.certificateForSignature = certificateForSignature;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getCryptalgSym() {
        return cryptalgSym;
    }

    public void setCryptalgSym(String cryptalgSym) {
        this.cryptalgSym = cryptalgSym;
    }

    public boolean isCryptMethodEnc() {
        return cryptMethodEnc;
    }

    public void setCryptMethodEnc(boolean cryptMethodEnc) {
        this.cryptMethodEnc = cryptMethodEnc;
    }

    public boolean isCryptMethodSig() {
        return cryptMethodSig;
    }

    public void setCryptMethodSig(boolean cryptMethodSig) {
        this.cryptMethodSig = cryptMethodSig;
    }

    public boolean isMdnRequired() {
        return mdnRequired;
    }

    public void setMdnRequired(boolean mdnRequired) {
        this.mdnRequired = mdnRequired;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMicAlgMdn() {
        return micAlgMdn;
    }

    public void setMicAlgMdn(String micAlgMdn) {
        this.micAlgMdn = micAlgMdn;
    }

    public String getMicAlgReq() {
        return micAlgReq;
    }

    public void setMicAlgReq(String micAlgReq) {
        this.micAlgReq = micAlgReq;
    }

    public String getViewCertificateForEncryption() {
        return viewCertificateForEncryption;
    }

    public void setViewCertificateForEncryption(String viewCertificateForEncryption) {
        this.viewCertificateForEncryption = viewCertificateForEncryption;
    }

    public String getViewCertificateForSignature() {
        return viewCertificateForSignature;
    }

    public void setViewCertificateForSignature(String viewCertificateForSignature) {
        this.viewCertificateForSignature = viewCertificateForSignature;
    }

    public String getXiPartyFrom() {
        return xiPartyFrom;
    }

    public void setXiPartyFrom(String xiPartyFrom) {
        this.xiPartyFrom = xiPartyFrom;
    }

    public String getXiPartyTo() {
        return xiPartyTo;
    }

    public void setXiPartyTo(String xiPartyTo) {
        this.xiPartyTo = xiPartyTo;
    }

    public boolean getCryptMethodEnc() {
        return false;
    }

    public boolean getCryptMethodSig() {
        return false;
    }

    public boolean getMdnRequired() {
        return mdnRequired;
    }

    public boolean isMessageDirectionInbound() {
        return true;
    }

    public boolean isMessageDirectionOutbound() {
        return false;
    }
}
