package com.cbsgmbh.xi.af.as2.module.crypt.configuration;

public abstract class ConfigurationAbstractImpl implements ConfigurationSettings {

    protected String messageType = null;

    protected String channelId = null;

    protected String xiPartyFrom = null;

    protected String xiPartyTo = null;

    protected boolean mdnRequired = false;

    protected boolean encUsed = false;

    protected boolean signatureUsed = false;

    protected String algorithmRequest = null;

    protected String algorithmMdn = null;

    protected String symmetricCryptAlgorithm = null;

    protected String certificateForEncryption = null;

    protected String viewCertificateForEncryption = null;

    protected String certificateForSignature = null;

    protected String viewCertificateForSignature = null;

    /**
     * Getter for channel Id
     * @return String
     */
    public String getChannelId() {
        return this.channelId;
    }

    /**
     * Getter for message type
     * @return String
     */
    public String getMessageType() {
        return this.messageType;
    }

    /**
     * Getter for XI party from
     * @return String
     */
    public String getXiPartyFrom() {
        return this.xiPartyFrom;
    }

    /**
     * Getter for XI party to
     * @return String
     */
    public String getXiPartyTo() {
        return this.xiPartyTo;
    }

    /**
     * Getter for cryptalgSym
     * The value must be one of CryptUtil.CRYPTALG_SYM_*
     * @return String
     */
    public String getCryptalgSym() {
        return this.symmetricCryptAlgorithm;
    }

    /**
     * Getter for encryption method
     * @return boolean
     */
    public boolean getCryptMethodEnc() {
        return this.encUsed;
    }

    /**
     * Getter for signature method
     * @return boolean
     */
    public boolean getCryptMethodSig() {
        return this.signatureUsed;
    }

    /**
     * Getter for MDN usage
     * @return boolean
     */
    public boolean getMdnRequired() {
        return this.mdnRequired;
    }

    /**
     * Getter for mic algorithm of MDN
     * @return String
     */
    public String getMicAlgMdn() {
        return this.algorithmMdn;
    }

    /**
     * Getter for mic algorithm of request
     * @return String
     */
    public String getMicAlgReq() {
        return this.algorithmRequest;
    }

    /**
     * Getter for the certificate for encryption view
     * @return String
     */
    public String getViewCertificateForEncryption() {
        return this.viewCertificateForEncryption;
    }

    /**
     * Getter for the certificate for signature view
     * @return String
     */
    public String getViewCertificateForSignature() {
        return this.viewCertificateForSignature;
    }

    /**
     * Getter for the certificate for encryption
     * @return String
     */
    public String getCertificateForEncryption() {
        return this.certificateForEncryption;
    }

    /**
     * Getter for the certificate for signature
     * @return String
     */
    public String getCertificateForSignature() {
        return this.certificateForSignature;
    }
}
