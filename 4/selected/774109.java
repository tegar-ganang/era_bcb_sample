package org.atricore.idbus.capabilities.sso.main.sp.producers;

import oasis.names.tc.saml._2_0.metadata.EntityDescriptorType;
import oasis.names.tc.saml._2_0.metadata.IDPSSODescriptorType;
import oasis.names.tc.saml._2_0.metadata.RoleDescriptorType;
import oasis.names.tc.saml._2_0.protocol.LogoutRequestType;
import oasis.names.tc.saml._2_0.protocol.ResponseType;
import oasis.names.tc.saml._2_0.protocol.StatusResponseType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SSOException;
import org.atricore.idbus.capabilities.sso.main.common.AbstractSSOMediator;
import org.atricore.idbus.capabilities.sso.main.common.producers.SSOProducer;
import org.atricore.idbus.capabilities.sso.main.sp.SPSecurityContext;
import org.atricore.idbus.capabilities.sso.main.sp.SamlR2SPMediator;
import org.atricore.idbus.capabilities.sso.main.sp.plans.SamlR2SloRequestToSamlR2RespPlan;
import org.atricore.idbus.capabilities.sso.support.SAMLR2Constants;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.capabilities.sso.support.core.SSORequestException;
import org.atricore.idbus.capabilities.sso.support.core.SSOResponseException;
import org.atricore.idbus.capabilities.sso.support.core.StatusCode;
import org.atricore.idbus.capabilities.sso.support.core.StatusDetails;
import org.atricore.idbus.capabilities.sso.support.core.encryption.SamlR2Encrypter;
import org.atricore.idbus.capabilities.sso.support.core.signature.SamlR2SignatureException;
import org.atricore.idbus.capabilities.sso.support.core.signature.SamlR2SignatureValidationException;
import org.atricore.idbus.capabilities.sso.support.core.signature.SamlR2Signer;
import org.atricore.idbus.capabilities.sts.main.SecurityTokenEmissionException;
import org.atricore.idbus.common.sso._1_0.protocol.SPInitiatedLogoutRequestType;
import org.atricore.idbus.common.sso._1_0.protocol.SSOResponseType;
import org.atricore.idbus.kernel.main.federation.metadata.*;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.MediationMessageImpl;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationFault;
import org.atricore.idbus.kernel.main.mediation.MediationState;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.IdPChannel;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedLocalProvider;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedProvider;
import org.atricore.idbus.kernel.main.session.SSOSessionManager;
import org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;
import org.atricore.idbus.kernel.planning.*;
import javax.xml.namespace.QName;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: IDPSingleSignOnServiceProducer.java 1246 2009-06-05 20:30:58Z sgonzalez $
 */
public class SingleLogoutProducer extends SSOProducer {

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    private static final Log logger = LogFactory.getLog(SingleLogoutProducer.class);

    public SingleLogoutProducer(AbstractCamelEndpoint<CamelMediationExchange> endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    protected void doProcess(CamelMediationExchange exchange) throws Exception {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        logger.debug("Processing SLO Message : " + in.getMessage().getContent());
        Object content = in.getMessage().getContent();
        AbstractSSOMediator mediator = (AbstractSSOMediator) channel.getIdentityMediator();
        in.getMessage().getState().setAttribute("SAMLR2Signer", mediator.getSigner());
        try {
            if (content instanceof StatusResponseType) {
                StatusResponseType samlResponse = (StatusResponseType) in.getMessage().getContent();
                if (logger.isDebugEnabled()) logger.debug("Received SAML 2.0 SLO Response " + samlResponse.getID());
                doProcessStatusResponse(exchange, samlResponse);
            } else if (content instanceof LogoutRequestType) {
                LogoutRequestType samlSloRequest = (LogoutRequestType) in.getMessage().getContent();
                if (logger.isDebugEnabled()) logger.debug("Received SAML 2.0 SLO Request " + samlSloRequest.getID());
                doProcessLogoutRequest(exchange, samlSloRequest);
            } else {
                throw new SSOException("Unsupported message type " + content);
            }
        } catch (SSORequestException e) {
            throw new IdentityMediationFault(e.getTopLevelStatusCode() != null ? e.getTopLevelStatusCode().getValue() : StatusCode.TOP_RESPONDER.getValue(), e.getSecondLevelStatusCode() != null ? e.getSecondLevelStatusCode().getValue() : null, e.getStatusDtails() != null ? e.getStatusDtails().getValue() : StatusDetails.UNKNOWN_REQUEST.getValue(), e.getErrorDetails() != null ? e.getErrorDetails() : content.getClass().getName(), e);
        } catch (SSOException e) {
            throw new IdentityMediationFault(StatusCode.TOP_RESPONDER.getValue(), null, StatusDetails.UNKNOWN_REQUEST.getValue(), content.getClass().getName(), e);
        }
    }

    protected void doProcessLogoutRequest(CamelMediationExchange exchange, LogoutRequestType sloRequest) throws Exception {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        SPSecurityContext secCtx = (SPSecurityContext) in.getMessage().getState().getLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        validateRequest(sloRequest, in.getMessage().getRawContent());
        CircleOfTrustMemberDescriptor idp = getProvider().getCotManager().lookupMemberByAlias(sloRequest.getIssuer().getValue());
        if (secCtx == null || !idp.getAlias().equals(secCtx.getIdpAlias())) {
            logger.warn("Unexpected SLO Request received from IDP " + sloRequest.getIssuer().getValue());
        } else {
            SSOSessionManager sessionMgr = ((IdPChannel) channel).getSessionManager();
            try {
                sessionMgr.invalidate(secCtx.getSessionIndex());
            } catch (NoSuchSessionException e) {
                logger.debug("Session already invalidated " + secCtx.getSessionIndex());
            }
            secCtx.clear();
            in.getMessage().getState().removeLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        }
        EndpointDescriptor destination = resolveIdPSloEndpoint(idp.getAlias(), new SSOBinding[] { SSOBinding.asEnum(endpoint.getBinding()) }, true);
        ResponseType samlResponse = buildSamlSloResponse(exchange, sloRequest, idp, destination);
        logger.debug("Sending SAML SLO Response to " + destination);
        CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
        out.setMessage(new MediationMessageImpl(samlResponse.getID(), samlResponse, "ResponseType", null, destination, in.getMessage().getState()));
        exchange.setOut(out);
    }

    protected ResponseType buildSamlSloResponse(CamelMediationExchange exchange, LogoutRequestType sloRequest, CircleOfTrustMemberDescriptor sp, EndpointDescriptor spEndpoint) throws Exception {
        IdentityPlan identityPlan = findIdentityPlanOfType(SamlR2SloRequestToSamlR2RespPlan.class);
        IdentityPlanExecutionExchange idPlanExchange = createIdentityPlanExecutionExchange();
        idPlanExchange.setProperty(VAR_DESTINATION_COT_MEMBER, sp);
        idPlanExchange.setProperty(VAR_DESTINATION_ENDPOINT_DESCRIPTOR, spEndpoint);
        idPlanExchange.setProperty(VAR_REQUEST, sloRequest);
        IdentityArtifact<LogoutRequestType> in = new IdentityArtifactImpl<LogoutRequestType>(new QName(SAMLR2Constants.SAML_PROTOCOL_NS, "LogoutRequest"), sloRequest);
        idPlanExchange.setIn(in);
        IdentityArtifact<ResponseType> out = new IdentityArtifactImpl<ResponseType>(new QName(SAMLR2Constants.SAML_PROTOCOL_NS, "Response"), new ResponseType());
        idPlanExchange.setOut(out);
        identityPlan.prepare(idPlanExchange);
        identityPlan.perform(idPlanExchange);
        if (!idPlanExchange.getStatus().equals(IdentityPlanExecutionStatus.SUCCESS)) {
            throw new SecurityTokenEmissionException("Identity plan returned : " + idPlanExchange.getStatus());
        }
        if (idPlanExchange.getOut() == null) throw new SecurityTokenEmissionException("Plan Exchange OUT must not be null!");
        return (ResponseType) idPlanExchange.getOut().getContent();
    }

    protected void doProcessStatusResponse(CamelMediationExchange exchange, StatusResponseType samlResponse) throws Exception {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        SPInitiatedLogoutRequestType ssoLogoutRequest = (SPInitiatedLogoutRequestType) in.getMessage().getState().getLocalVariable("urn:org:atricore:idbus:sso:protocol:SPInitiatedLogoutRequest");
        in.getMessage().getState().removeLocalVariable("urn:org:atricore:idbus:sso:protocol:SPInitiatedLogoutRequest");
        SPSecurityContext secCtx = (SPSecurityContext) in.getMessage().getState().getLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        in.getMessage().getState().removeLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        LogoutRequestType logoutRequest = (LogoutRequestType) in.getMessage().getState().getLocalVariable("urn:oasis:names:tc:SAML:2.0:protocol:LogoutRequest");
        in.getMessage().getState().removeLocalVariable("urn:oasis:names:tc:SAML:2.0:protocol:LogoutRequest");
        validateResponse(logoutRequest, (StatusResponseType) in.getMessage().getContent(), in.getMessage().getRawContent(), in.getMessage().getState());
        SSOResponseType ssoResponse = new SSOResponseType();
        ssoResponse.setID(uuidGenerator.generateId());
        String destinationLocation = ((SamlR2SPMediator) channel.getIdentityMediator()).getSpBindingSLO();
        EndpointDescriptor destination = new EndpointDescriptorImpl("EmbeddedSPAcs", "SingleLogoutService", SSOBinding.SSO_ARTIFACT.getValue(), destinationLocation, null);
        if (ssoLogoutRequest != null) {
            logger.debug("SLO Response in reply to " + ssoLogoutRequest.getID());
            ssoResponse.setInReplayTo(ssoLogoutRequest.getID());
            if (ssoLogoutRequest.getReplyTo() != null) {
                logger.debug("Using requested reply destination : " + ssoLogoutRequest.getReplyTo());
                destination = new EndpointDescriptorImpl("EmbeddedSPAcs", "SingleLogoutService", SSOBinding.SSO_ARTIFACT.getValue(), ssoLogoutRequest.getReplyTo(), null);
            }
        }
        destroySPSecurityContext(exchange, secCtx);
        logger.debug("Sending JOSSO SLO Response to " + destination);
        CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
        out.setMessage(new MediationMessageImpl(ssoResponse.getID(), ssoResponse, "SPLogoutResponse", null, destination, in.getMessage().getState()));
        exchange.setOut(out);
    }

    protected void validateRequest(LogoutRequestType request, String originalRequest) throws SSORequestException, SSOException {
        SamlR2SPMediator mediator = (SamlR2SPMediator) channel.getIdentityMediator();
        SamlR2Signer signer = mediator.getSigner();
        SamlR2Encrypter encrypter = mediator.getEncrypter();
        String idpAlias = null;
        IDPSSODescriptorType idpMd = null;
        try {
            idpAlias = request.getIssuer().getValue();
            MetadataEntry md = getCotManager().findEntityMetadata(idpAlias);
            EntityDescriptorType saml2Md = (EntityDescriptorType) md.getEntry();
            boolean found = false;
            for (RoleDescriptorType roleMd : saml2Md.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
                if (roleMd instanceof IDPSSODescriptorType) {
                    idpMd = (IDPSSODescriptorType) roleMd;
                }
            }
        } catch (CircleOfTrustManagerException e) {
            throw new SSORequestException(request, StatusCode.TOP_RESPONDER, StatusCode.NO_SUPPORTED_IDP, null, request.getIssuer().getValue(), e);
        }
        if (mediator.isValidateRequestsSignature()) {
            if (request.getSignature() == null) throw new SSORequestException(request, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE);
            try {
                if (originalRequest != null) signer.validateDom(idpMd, originalRequest); else signer.validate(idpMd, request);
            } catch (SamlR2SignatureValidationException e) {
                throw new SSORequestException(request, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            } catch (SamlR2SignatureException e) {
                throw new SSORequestException(request, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            }
        }
    }

    protected StatusResponseType validateResponse(LogoutRequestType request, StatusResponseType response, String originalResponse, MediationState state) throws SSOResponseException, SSOException {
        SamlR2SPMediator mediator = (SamlR2SPMediator) channel.getIdentityMediator();
        SamlR2Signer signer = mediator.getSigner();
        SamlR2Encrypter encrypter = mediator.getEncrypter();
        String idpAlias = null;
        IDPSSODescriptorType idpMd = null;
        try {
            idpAlias = response.getIssuer().getValue();
            MetadataEntry md = getCotManager().findEntityMetadata(idpAlias);
            EntityDescriptorType saml2Md = (EntityDescriptorType) md.getEntry();
            boolean found = false;
            for (RoleDescriptorType roleMd : saml2Md.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
                if (roleMd instanceof IDPSSODescriptorType) {
                    idpMd = (IDPSSODescriptorType) roleMd;
                }
            }
        } catch (CircleOfTrustManagerException e) {
            throw new SSOResponseException(response, StatusCode.TOP_RESPONDER, StatusCode.NO_SUPPORTED_IDP, null, response.getIssuer().getValue(), e);
        }
        EndpointDescriptor endpointDesc;
        try {
            endpointDesc = channel.getIdentityMediator().resolveEndpoint(channel, endpoint);
        } catch (IdentityMediationException e1) {
            throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.RESOURCE_NOT_RECOGNIZED, StatusDetails.INTERNAL_ERROR, "Cannot resolve endpoint descriptor", e1);
        }
        if (response.getDestination() != null) {
            String location = endpointDesc.getResponseLocation();
            if (location == null) location = endpointDesc.getLocation();
            if (!response.getDestination().equals(location)) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_DESTINATION);
            }
        } else if (response.getSignature() != null && (!endpointDesc.getBinding().equals(SSOBinding.SAMLR2_LOCAL.getValue()) && !endpointDesc.getBinding().equals(SSOBinding.SAMLR2_ARTIFACT.getValue()))) {
            throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.NO_DESTINATION);
        }
        if (response.getIssueInstant() == null) {
            throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.INVALID_ATTR_NAME_OR_VALUE, StatusDetails.NO_ISSUE_INSTANT);
        } else if (request != null) {
            if (response.getIssueInstant().compare(request.getIssueInstant()) <= 0) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.INVALID_ATTR_NAME_OR_VALUE, StatusDetails.INVALID_ISSUE_INSTANT, response.getIssueInstant().toGregorianCalendar().toString() + " earlier than request issue instant.");
            } else {
                long ttl = mediator.getRequestTimeToLive();
                long res = response.getIssueInstant().toGregorianCalendar().getTime().getTime();
                long req = request.getIssueInstant().toGregorianCalendar().getTime().getTime();
                if (logger.isDebugEnabled()) logger.debug("TTL : " + res + " - " + req + " = " + (res - req));
                if (response.getIssueInstant().toGregorianCalendar().getTime().getTime() - request.getIssueInstant().toGregorianCalendar().getTime().getTime() > ttl) {
                    throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.INVALID_ATTR_NAME_OR_VALUE, StatusDetails.INVALID_ISSUE_INSTANT, response.getIssueInstant().toGregorianCalendar().toString() + " expired after " + ttl + "ms");
                }
            }
        }
        if (response.getVersion() == null) {
            throw new SSOResponseException(response, StatusCode.TOP_VERSION_MISSMATCH, null, StatusDetails.INVALID_VERSION);
        }
        if (!response.getVersion().equals(SAML_VERSION)) {
            throw new SSOResponseException(response, StatusCode.TOP_VERSION_MISSMATCH, null, StatusDetails.UNSUPPORTED_VERSION, response.getVersion());
        }
        if (request != null) {
            if (response.getInResponseTo() == null) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, null, StatusDetails.NO_IN_RESPONSE_TO);
            } else if (!request.getID().equals(response.getInResponseTo())) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, null, StatusDetails.INVALID_RESPONSE_ID, request.getID() + "/ " + response.getInResponseTo());
            }
        }
        if (response.getStatus() != null) {
            if (response.getStatus().getStatusCode() != null) {
                if (StringUtils.isEmpty(response.getStatus().getStatusCode().getValue()) || !isStatusCodeValid(response.getStatus().getStatusCode().getValue())) {
                    throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.INVALID_ATTR_NAME_OR_VALUE, StatusDetails.INVALID_STATUS_CODE, response.getStatus().getStatusCode().getValue());
                }
            } else {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.INVALID_ATTR_NAME_OR_VALUE, StatusDetails.NO_STATUS_CODE);
            }
        } else {
            throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.INVALID_ATTR_NAME_OR_VALUE, StatusDetails.NO_STATUS);
        }
        if (!endpoint.getBinding().equals(SSOBinding.SAMLR2_REDIRECT.getValue())) {
            if (response.getSignature() == null) throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE);
            try {
                if (originalResponse != null) signer.validateDom(idpMd, originalResponse); else signer.validate(idpMd, response, "LogoutResponse");
            } catch (SamlR2SignatureValidationException e) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            } catch (SamlR2SignatureException e) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            }
        } else {
            try {
                signer.validateQueryString(idpMd, state.getTransientVariable("SAMLResponse"), state.getTransientVariable("RelayState"), state.getTransientVariable("SigAlg"), state.getTransientVariable("Signature"), true);
            } catch (SamlR2SignatureValidationException e) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            } catch (SamlR2SignatureException e) {
                throw new SSOResponseException(response, StatusCode.TOP_REQUESTER, StatusCode.REQUEST_DENIED, StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            }
        }
        return response;
    }

    protected void destroySPSecurityContext(CamelMediationExchange exchange, SPSecurityContext secCtx) throws SSOException {
        CircleOfTrustMemberDescriptor idp = getCotManager().lookupMemberByAlias(secCtx.getIdpAlias());
        IdPChannel idpChannel = (IdPChannel) resolveIdpChannel(idp);
        SSOSessionManager ssoSessionManager = idpChannel.getSessionManager();
        secCtx.clear();
        try {
            ssoSessionManager.invalidate(secCtx.getSessionIndex());
            CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
            in.getMessage().getState().removeRemoteVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        } catch (NoSuchSessionException e) {
            logger.debug("SSO Session already invalidated " + secCtx.getSessionIndex());
        } catch (Exception e) {
            throw new SSOException(e);
        }
    }

    /**
     * @return
     */
    protected FederationChannel resolveIdpChannel(CircleOfTrustMemberDescriptor idpDescriptor) {
        FederatedLocalProvider sp = getProvider();
        FederationChannel idpChannel = sp.getChannel();
        for (FederationChannel fChannel : sp.getChannels()) {
            FederatedProvider idp = fChannel.getTargetProvider();
            for (CircleOfTrustMemberDescriptor member : idp.getMembers()) {
                if (member.getAlias().equals(idpDescriptor.getAlias())) {
                    if (logger.isDebugEnabled()) logger.debug("Selected IdP channel " + fChannel.getName() + " for provider " + idp.getName());
                    idpChannel = fChannel;
                    break;
                }
            }
        }
        return idpChannel;
    }
}
