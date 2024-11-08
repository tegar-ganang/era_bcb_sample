package com.cbsgmbh.xi.af.as2.helpers;

import java.io.IOException;
import com.cbsgmbh.xi.af.as2.crypt.CryptManagerException;
import com.cbsgmbh.xi.af.as2.crypt.CryptManagerLocal;
import com.cbsgmbh.xi.af.as2.crypt.util.CryptUtil;
import com.cbsgmbh.xi.af.as2.util.ModuleExceptionEEDM;
import com.cbsgmbh.xi.af.http.util.HTTPUtil;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.lib.mp.processor.ModuleProcessorException;
import com.sap.aii.af.lib.mp.processor.ModuleProcessorLocal;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.aii.af.service.administration.impl.AdminManagerImpl;
import com.sap.aii.af.service.administration.monitoring.ActivationState;
import com.sap.aii.af.service.administration.monitoring.ChannelActivationStatus;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.Channel;

public class XiChannelSapImpl implements XiChannel {

    private String channelId;

    private Channel sapChannel;

    private ModuleProcessorLocal moduleProcessor;

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(XiChannelSapImpl.class.getName(), TracerCategories.APP_LISTENER);

    public XiChannelSapImpl(String channelId, Channel sapChannel, ModuleProcessorLocal moduleProcessor) {
        this.channelId = channelId;
        this.sapChannel = sapChannel;
        this.moduleProcessor = moduleProcessor;
    }

    public String getId() {
        return this.channelId;
    }

    protected boolean getValueAsBoolean(final String s) throws CPAException {
        return this.sapChannel.getValueAsBoolean(s);
    }

    protected String getValueAsString(final String s) throws CPAException {
        return this.sapChannel.getValueAsString(s);
    }

    public Channel getChannel() {
        return this.sapChannel;
    }

    public void processMessage(final XiMessage xiMessage, final Tracer tracer) throws ModuleProcessorException, ModuleException {
        final Message sapMessage = ((XiMessageSapImpl) xiMessage).getSapMessage();
        tracer.info("XI Message created - next step sending of XI message {0} with message key {1}", new Object[] { sapMessage.getMessageId(), sapMessage.getMessageKey() });
        final ModuleData inputModuleData = new ModuleData();
        tracer.info("Instantiated ModuleData.");
        inputModuleData.setPrincipalData(sapMessage);
        tracer.info("Set PrincipalData.");
        @SuppressWarnings("unused") final ModuleData outputModuleData = this.moduleProcessor.process(this.getId(), inputModuleData);
        tracer.info("XI Message sent to AF.");
    }

    public void verifyIsActivated() throws CPAException, ModuleExceptionEEDM {
        final Tracer tracer = baseTracer.entering("verifyIsActivated()");
        final String adapterStatus = this.getValueAsString("adapterStatus");
        if (adapterStatus.equalsIgnoreCase("inactive")) {
            tracer.info("Channel is inactive. Message may not be processed");
            throw new ModuleExceptionEEDM(HTTPUtil.INACTIVE_ADAPTER_ERROR, ModuleExceptionEEDM.UNSPECIFIED_ERROR);
        }
        tracer.info("Channel state: {0}", adapterStatus);
        tracer.leaving();
    }

    public void verifyIsStartedInChannelMonitoring() throws ModuleExceptionEEDM {
        final Tracer tracer = baseTracer.entering("verifyIsActivatedInChannelMonitoring()");
        ChannelActivationStatus channelActivationStatus = null;
        try {
            channelActivationStatus = AdminManagerImpl.getInstance().getChannelActivationStatus(this.getId());
        } catch (Exception e) {
            tracer.error("Exception occured during channel activation status determination");
            throw new ModuleExceptionEEDM(HTTPUtil.INACTIVE_ADAPTER_ERROR, ModuleExceptionEEDM.UNSPECIFIED_ERROR);
        }
        if (channelActivationStatus.getActivationState() == ActivationState.STOPPED) {
            tracer.error("Channel has been stopped. Message may not be processed.");
            throw new ModuleExceptionEEDM(HTTPUtil.INACTIVE_ADAPTER_ERROR, ModuleExceptionEEDM.UNSPECIFIED_ERROR);
        }
        tracer.info("Channel activation status: {0}", channelActivationStatus);
        tracer.leaving();
    }

    public String getCertificateForMdnSignature() throws CPAException {
        return getValueAsString(HTTPUtil.MDN_RCV_PRIVKEY_CERTID);
    }

    public String getViewCertificateForMdnSignature() throws CPAException {
        return getValueAsString(HTTPUtil.MDN_RCV_PRIVKEY_CERTID_VIEW);
    }

    public boolean getCryptConfigEncryptionEnabled() throws CPAException {
        return getValueAsBoolean(CryptUtil.CRYPT_METHOD_ENC);
    }

    public boolean getCryptConfigSignatureEnabled() throws CPAException {
        return getValueAsBoolean(CryptUtil.CRYPT_METHOD_SIG);
    }

    public boolean getIsMdnRequired() throws CPAException {
        return getValueAsBoolean(CryptUtil.CRYPT_AS2_MDN);
    }

    public String getCryptMicInRequest() throws CPAException {
        return getValueAsString(CryptUtil.CRYPT_ALG_MIC_IN_REQ);
    }

    public String getCryptMicInMDN() throws CPAException {
        return getValueAsString(CryptUtil.CRYPT_ALG_MIC_IN_MDN);
    }

    /**
     * Internal helper method to validate retrieved data against CPACache
     * 
     * @param channelID Channel Id as String
     * @return Channel channel
     * @exception CPAException, ModuleExceptionEEDM, IOException
     * @throws StatusMessageException 
     */
    public void validateAgainstConfiguration(final SMimeMessageBody mimeBodyPart, final RequestData requestData) throws CPAException, IOException, StatusMessageException {
        final Tracer tracer = baseTracer.entering("validateAgainstConfiguration(final SMimeMessageBody mimeBodyPart, final RequestData requestData)");
        final String SIGN = "pkcs7-signature";
        final boolean channelEncryptionEnabled = getCryptConfigEncryptionEnabled();
        tracer.info("cxiChannel.getCryptConfigEncoding: {0}", channelEncryptionEnabled);
        final boolean channelSignatureEnabled = getCryptConfigSignatureEnabled();
        tracer.info("xiChannel.getCryptConfigSignature(): {0}", channelSignatureEnabled);
        final String micAlgReq = getCryptMicInRequest();
        tracer.info("micAlgReq: {0}", micAlgReq);
        final String micAlgMdn = getCryptMicInMDN();
        tracer.info("micAlgMdn: {0}", micAlgMdn);
        validateChannelMicAlgorithm(micAlgReq);
        tracer.info("micAlgReq validation successful");
        validateChannelMicAlgorithmMdn(micAlgMdn);
        tracer.info("micAlgMdn validation successful");
        final CryptManagerLocal cmLocal = createCryptManager();
        final boolean isRequestEncrypted = checkIsRequestEncrypted(mimeBodyPart, cmLocal);
        compareEncryptionSettings(isRequestEncrypted, channelEncryptionEnabled);
        tracer.info("cryptConfigEnc validated against channel information successfully");
        if (!channelEncryptionEnabled) {
            final boolean isRequestSigned = checkIsRequestSigned(mimeBodyPart, cmLocal);
            compareSignatureSettings(isRequestSigned, channelSignatureEnabled);
            final String contentTypeReq = requestData.getMessageContentType();
            tracer.info("contentTypeReq: {0}", contentTypeReq);
            final boolean micAlgSha1Req = contentTypeReq.toLowerCase().indexOf(CryptUtil.MICALG_SHA1) != -1;
            final boolean micAlgMd5Req = contentTypeReq.toLowerCase().indexOf(CryptUtil.MICALG_MD5) != -1;
            tracer.info("micAlgSha1Req: {0}, micAlgMd5Req: {1}", new Object[] { Boolean.valueOf(micAlgSha1Req), Boolean.valueOf(micAlgMd5Req) });
            checkCryptSettingsMatch(micAlgSha1Req, micAlgMd5Req, channelSignatureEnabled, micAlgReq);
            tracer.info("micAlg of incoming request successfully validated against channel information.");
            tracer.info("cryptConfigSig validated against channel information successfully");
        } else {
            tracer.info("Request message is encrypted. Validation of signature not possible before decryption.");
        }
        final String dispNotToReq = requestData.getDispositionNotificationTo();
        tracer.info("Value for dispositionNotificationTo is: {0}", dispNotToReq);
        boolean mdnRequiredReq = false;
        if (dispNotToReq != null) {
            mdnRequiredReq = true;
        }
        tracer.info("mdnRequiredReq: {0}", mdnRequiredReq);
        final boolean channelMdnRequired = getIsMdnRequired();
        compareMdnSettings(mdnRequiredReq, channelMdnRequired);
        tracer.info("mdnRequired validated against channel information successfully");
        if (getIsMdnRequired()) {
            checkMdnCryptSettings(requestData, SIGN, channelSignatureEnabled, micAlgMdn);
        }
        tracer.info("Validation against CPA cache completed successfully");
        tracer.leaving();
    }

    private void checkMdnCryptSettings(final RequestData requestData, final String SIGN, final boolean channelSignatureEnabled, final String micAlgMdn) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("checkMdnCryptSettings(final RequestData requestData, final String SIGN, final boolean channelSignatureEnabled, final String micAlgMdn)");
        final boolean signMdn = channelSignatureEnabled;
        tracer.info("Signature mode for MDN set: {0}", signMdn);
        final String mdnOptionsReq = requestData.getDispositionNotificationOptions();
        tracer.info("Value for MDN options is: {0}", mdnOptionsReq);
        checkResponseMdnCryptSettings(signMdn, mdnOptionsReq);
        tracer.info("MDN options of request successfully validated against channel information");
        boolean signMdnReq = false;
        if ((mdnOptionsReq != null) && (mdnOptionsReq.toLowerCase().indexOf(SIGN) != -1)) {
            signMdnReq = true;
        }
        if (signMdnReq != signMdn) {
            tracer.error("signMdnReq: {0}, signMdn: {1}", new Object[] { Boolean.valueOf(signMdnReq), Boolean.valueOf(signMdn) });
            tracer.error("Error during validation against CPACache data - error message: {0}", HTTPUtil.EDIFACT_ERROR_INVALID_SIGN_OPTIONS);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_SIGN_OPTIONS);
        }
        tracer.info("signMdn successfully validated against channel information");
        tracer.info("this.signMdn: {0}", signMdn);
        if (signMdn) {
            checkMdnMicSettings(micAlgMdn, mdnOptionsReq);
        }
        tracer.leaving();
    }

    private void checkMdnMicSettings(final String micAlgMdn, final String mdnOptionsReq) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("checkMdnMicSettings(final String micAlgMdn, final String mdnOptionsReq)");
        boolean micAlgSha1Sign = false, micAlgMd5Sign = false;
        if (mdnOptionsReq.toLowerCase().indexOf(CryptUtil.MICALG_SHA1) != -1) {
            micAlgSha1Sign = true;
        } else if (mdnOptionsReq.toLowerCase().indexOf(CryptUtil.MICALG_MD5) != -1) {
            micAlgMd5Sign = true;
        }
        if (((micAlgMdn.equalsIgnoreCase(CryptUtil.MICALG_SHA1)) && (!micAlgSha1Sign)) || ((micAlgMdn.equalsIgnoreCase(CryptUtil.MICALG_MD5)) && (!micAlgMd5Sign))) {
            tracer.info("micAlgMdn: {0}, micAlgSha1Sign: {1}, micAlgMd5Sign: {2}", new Object[] { micAlgMdn, Boolean.valueOf(micAlgSha1Sign), Boolean.valueOf(micAlgMd5Sign) });
            tracer.info("Error during validation against CPACache data - error message: {0}", HTTPUtil.EDIFACT_ERROR_INVALID_DISP_NOT_OPTIONS_MIC);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_DISP_NOT_OPTIONS_MIC, HTTPUtil.MDN_DISP_F_UNSUPPORTED_MIC);
        }
        tracer.info("micAlg of outgoing MDN successfully validated against channel information.");
        tracer.leaving();
    }

    private void checkResponseMdnCryptSettings(final boolean signMdn, final String mdnOptionsReq) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("checkResponseMdnCryptSettings(final boolean signMdn, final String mdnOptionsReq)");
        if (((signMdn) && (mdnOptionsReq == null || mdnOptionsReq.trim().equals(""))) || ((!signMdn) && (mdnOptionsReq != null && !mdnOptionsReq.trim().equals("")))) {
            tracer.error("signMdn: {0}, mdnOptionsReq: {1}", new Object[] { Boolean.valueOf(signMdn), mdnOptionsReq });
            tracer.error("Error during validation against CPACache data - error message: {0}", new Object[] { HTTPUtil.EDIFACT_ERROR_INVALID_DISP_NOT_OPTIONS_SIGN });
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_DISP_NOT_OPTIONS_SIGN);
        }
        tracer.leaving();
    }

    private void compareMdnSettings(final boolean mdnRequiredReq, final boolean channelMdnRequired) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("compareMdnSettings(final boolean mdnRequiredReq, final boolean channelMdnRequired)");
        if (mdnRequiredReq != channelMdnRequired) {
            tracer.error("mdnRequiredReq: {0}, mdnRequired: {1}", new Object[] { Boolean.valueOf(mdnRequiredReq), Boolean.valueOf(channelMdnRequired) });
            tracer.error("Error during validation against CPACache data - error message: {0}", new Object[] { HTTPUtil.EDIFACT_ERROR_INVALID_DISP_NOT_TO });
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_DISP_NOT_TO, HTTPUtil.MDN_DISP_E_UNEXP_PROCESSING_ERROR);
        }
        tracer.leaving();
    }

    private void checkCryptSettingsMatch(final boolean micAlgSha1Req, final boolean micAlgMd5Req, final boolean channelSignatureEnabled, final String micAlgReq) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("checkCryptSettingsMatch(final boolean micAlgSha1Req, final boolean micAlgMd5Req, final boolean channelSignatureEnabled, final String micAlgReq)");
        if ((!channelSignatureEnabled && (micAlgSha1Req || micAlgMd5Req)) || (channelSignatureEnabled && !micAlgSha1Req && !micAlgMd5Req) || (channelSignatureEnabled && (micAlgReq.equalsIgnoreCase(CryptUtil.MICALG_SHA1)) && (!micAlgSha1Req)) || (channelSignatureEnabled && (micAlgReq.equalsIgnoreCase(CryptUtil.MICALG_MD5)) && (!micAlgMd5Req))) {
            tracer.error("Error during validation against CPACache data - error message: {0}", HTTPUtil.EDIFACT_ERROR_INVALID_CONTENT_TYPE_MIC);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_CONTENT_TYPE_MIC, HTTPUtil.MDN_DISP_F_UNSUPPORTED_MIC);
        }
        tracer.leaving();
    }

    private void compareSignatureSettings(final boolean cryptMethodSigReq, final boolean channelSignatureEnabled) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("compareSignatureSettings(final boolean cryptMethodSigReq, final boolean channelSignatureEnabled)");
        if (cryptMethodSigReq != channelSignatureEnabled) {
            tracer.error("cryptMethodSigReq: {0}, cryptConfigSig: {1}", new Object[] { Boolean.valueOf(cryptMethodSigReq), Boolean.valueOf(channelSignatureEnabled) });
            tracer.error("Error during validation against CPACache data - error message: {0}", HTTPUtil.EDIFACT_ERROR_INVALID_SIGN_MODE);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_SIGN_MODE, HTTPUtil.MDN_DISP_E_INTEGRITY_CHECK_FAILED);
        }
        tracer.leaving();
    }

    private boolean checkIsRequestSigned(final SMimeMessageBody mimeBodyPart, final CryptManagerLocal cmLocal) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("checkIsRequestSigned(final SMimeMessageBody mimeBodyPart, final CryptManagerLocal cmLocal)");
        try {
            tracer.leaving();
            return cmLocal.isSigned(mimeBodyPart.getBodyPart());
        } catch (javax.mail.MessagingException me) {
            tracer.error("Error during parsing AS2 request message - error message: {0}", HTTPUtil.EDIFACT_ERROR_PARSING_SIGN_MODE);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_PARSING_SIGN_MODE, HTTPUtil.MDN_DISP_E_INTEGRITY_CHECK_FAILED);
        }
    }

    private void compareEncryptionSettings(final boolean cryptMethodEncReq, final boolean channelEncryptionEnabled) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("compareEncryptionSettings(final boolean cryptMethodEncReq, final boolean channelEncryptionEnabled)");
        if (cryptMethodEncReq != channelEncryptionEnabled) {
            tracer.error("cryptMethodEncReq: {0}, cryptConfigEnc: {1}", new Object[] { Boolean.valueOf(cryptMethodEncReq), Boolean.valueOf(channelEncryptionEnabled) });
            tracer.error("Error during validation against CPACache data - error message: {0}", HTTPUtil.EDIFACT_ERROR_INVALID_ENC_MODE);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_ENC_MODE, HTTPUtil.MDN_DISP_E_DECRYPTION_FAILED);
        }
        tracer.leaving();
    }

    private boolean checkIsRequestEncrypted(final SMimeMessageBody mimeBodyPart, final CryptManagerLocal cmLocal) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("checkIsRequestEncrypted(final SMimeMessageBody mimeBodyPart, final CryptManagerLocal cmLocal)");
        try {
            tracer.leaving();
            return cmLocal.isEncrypted(mimeBodyPart.getBodyPart());
        } catch (javax.mail.MessagingException me) {
            tracer.error("Error during parsing AS2 request message - error message: {0}", HTTPUtil.EDIFACT_ERROR_PARSING_ENC_MODE);
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_PARSING_ENC_MODE, HTTPUtil.MDN_DISP_E_DECRYPTION_FAILED);
        }
    }

    private CryptManagerLocal createCryptManager() throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("createCryptManager()");
        try {
            tracer.leaving();
            return CryptUtil.getCryptManagerLocal();
        } catch (CryptManagerException cme) {
            tracer.error("Error during initializing CryptManagerLocal: {0}", cme.getMessage());
            throw new StatusMessageException("Error during initializing CryptManagerLocal: " + cme.getMessage());
        }
    }

    private void validateChannelMicAlgorithmMdn(final String micAlgMdn) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("validateChannelMicAlgorithmMdn(final String micAlgMdn)");
        if ((!micAlgMdn.equalsIgnoreCase(CryptUtil.MICALG_SHA1))) {
            tracer.error("Error during validation of CPACache data - error message: {0}: micAlgMdn = {1}", new Object[] { HTTPUtil.EDIFACT_ERROR_INVALID_CHANNEL_DATA, micAlgMdn });
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_CHANNEL_DATA);
        }
    }

    private void validateChannelMicAlgorithm(final String micAlgReq) throws StatusMessageException {
        final Tracer tracer = baseTracer.entering("validateChannelMicAlgorithm(final String micAlgReq)");
        if ((!micAlgReq.equalsIgnoreCase(CryptUtil.MICALG_SHA1)) && (!micAlgReq.equalsIgnoreCase(CryptUtil.MICALG_MD5))) {
            tracer.error("Error during validation of CPACache data - error message: {0}: micAlgReq = {1}", new Object[] { HTTPUtil.EDIFACT_ERROR_INVALID_CHANNEL_DATA, micAlgReq });
            throw new StatusMessageException(HTTPUtil.EDIFACT_ERROR_INVALID_CHANNEL_DATA);
        }
    }
}
