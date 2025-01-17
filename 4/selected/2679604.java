package org.atricore.idbus.capabilities.sso.main.sp.producers;

import oasis.names.tc.saml._2_0.metadata.EndpointType;
import oasis.names.tc.saml._2_0.metadata.EntityDescriptorType;
import oasis.names.tc.saml._2_0.metadata.IDPSSODescriptorType;
import oasis.names.tc.saml._2_0.metadata.RoleDescriptorType;
import oasis.names.tc.saml._2_0.protocol.AuthnRequestType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SSOException;
import org.atricore.idbus.capabilities.sso.main.common.AbstractSSOMediator;
import org.atricore.idbus.capabilities.sso.main.common.producers.SSOProducer;
import org.atricore.idbus.capabilities.sso.main.sp.SPSecurityContext;
import org.atricore.idbus.capabilities.sso.main.sp.SamlR2SPMediator;
import org.atricore.idbus.capabilities.sso.main.sp.plans.SPInitiatedAuthnReqToSamlR2AuthnReqPlan;
import org.atricore.idbus.capabilities.sso.support.SAMLR2Constants;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.capabilities.sts.main.SecurityTokenEmissionException;
import org.atricore.idbus.common.sso._1_0.protocol.*;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustMemberDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptorImpl;
import org.atricore.idbus.kernel.main.federation.metadata.MetadataEntry;
import org.atricore.idbus.kernel.main.mediation.MediationMessageImpl;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannel;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimSet;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimSetImpl;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedLocalProvider;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedProvider;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;
import org.atricore.idbus.kernel.planning.*;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.UsernameTokenType;
import javax.xml.namespace.QName;

/**
 *
 */
public class SPInitiatedSingleSignOnProducer extends SSOProducer {

    private static final Log logger = LogFactory.getLog(SPInitiatedSingleSignOnProducer.class);

    protected UUIDGenerator uuidGenerator = new UUIDGenerator();

    public SPInitiatedSingleSignOnProducer(AbstractCamelEndpoint<CamelMediationExchange> endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    protected void doProcess(CamelMediationExchange exchange) throws SSOException {
        logger.debug("Processing SP Initiated Single Sign-On on HTTP Redirect");
        try {
            CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
            AbstractSSOMediator mediator = (AbstractSSOMediator) channel.getIdentityMediator();
            in.getMessage().getState().setAttribute("SAMLR2Signer", mediator.getSigner());
            SPInitiatedAuthnRequestType ssoAuthnReq = (SPInitiatedAuthnRequestType) in.getMessage().getContent();
            String relayState = in.getMessage().getState().getLocalState().getId();
            SPSecurityContext secCtx = (SPSecurityContext) in.getMessage().getState().getLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
            if (secCtx != null && secCtx.getSessionIndex() != null) {
                if (ssoAuthnReq.isForceAuthn() != null && !ssoAuthnReq.isForceAuthn()) {
                    logger.debug("SSO Session found " + secCtx.getSessionIndex());
                    SPAuthnResponseType ssoResponse = new SPAuthnResponseType();
                    ssoResponse.setID(uuidGenerator.generateId());
                    SPInitiatedAuthnRequestType ssoRequest = (SPInitiatedAuthnRequestType) in.getMessage().getState().getLocalVariable("urn:org:atricore:idbus:sso:protocol:SPInitiatedAuthnRequest");
                    if (ssoRequest != null) {
                        ssoResponse.setInReplayTo(ssoRequest.getID());
                    }
                    SubjectType subjectType = toSubjectType(secCtx.getSubject());
                    ssoResponse.setSessionIndex(secCtx.getSessionIndex());
                    ssoResponse.setSubject(subjectType);
                    String destinationLocation = resolveSpBindingACS();
                    EndpointDescriptor destination = new EndpointDescriptorImpl("EmbeddedSPAcs", "AssertionConsumerService", SSOBinding.SSO_ARTIFACT.getValue(), destinationLocation, null);
                    CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
                    out.setMessage(new MediationMessageImpl(ssoResponse.getID(), ssoResponse, "SPAuthnResposne", relayState, destination, in.getMessage().getState()));
                    exchange.setOut(out);
                    return;
                } else {
                    logger.debug("SSO Session found " + secCtx.getSessionIndex() + ", but SP requested 'forceAuthn'. Destroying security context");
                    destroySPSecurityContext(exchange, secCtx);
                }
            }
            CircleOfTrustMemberDescriptor idp = this.resolveIdp(exchange);
            logger.debug("Using IdP " + idp.getAlias());
            EndpointType idpSsoEndpoint = resolveIdpSsoEndpoint(idp);
            EndpointDescriptor ed = new EndpointDescriptorImpl("IDPSSOEndpoint", "SingleSignOnService", idpSsoEndpoint.getBinding(), idpSsoEndpoint.getLocation(), idpSsoEndpoint.getResponseLocation());
            FederationChannel idpChannel = resolveIdpChannel(idp);
            if (logger.isDebugEnabled()) logger.debug("Using IdP channel " + idpChannel.getName());
            AuthnRequestType authnRequest = buildAuthnRequest(exchange, idp, ed, idpChannel);
            in.getMessage().getState().setLocalVariable("urn:org:atricore:idbus:sso:protocol:SPInitiatedAuthnRequest", ssoAuthnReq);
            in.getMessage().getState().setLocalVariable(SAMLR2Constants.SAML_PROTOCOL_NS + ":AuthnRequest", authnRequest);
            CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
            out.setMessage(new MediationMessageImpl(uuidGenerator.generateId(), authnRequest, "AuthnRequest", relayState, ed, in.getMessage().getState()));
            exchange.setOut(out);
        } catch (Exception e) {
            throw new SSOException(e);
        }
    }

    protected AuthnRequestType buildAuthnRequest(CamelMediationExchange exchange, CircleOfTrustMemberDescriptor idp, EndpointDescriptor ed, FederationChannel idpChannel) throws IdentityPlanningException, SSOException {
        IdentityPlan identityPlan = findIdentityPlanOfType(SPInitiatedAuthnReqToSamlR2AuthnReqPlan.class);
        IdentityPlanExecutionExchange idPlanExchange = createIdentityPlanExecutionExchange();
        idPlanExchange.setProperty(VAR_DESTINATION_COT_MEMBER, idp);
        idPlanExchange.setProperty(VAR_DESTINATION_ENDPOINT_DESCRIPTOR, ed);
        idPlanExchange.setProperty(VAR_COT_MEMBER, idpChannel.getMember());
        idPlanExchange.setProperty(VAR_RESPONSE_CHANNEL, idpChannel);
        SPInitiatedAuthnRequestType ssoAuthnRequest = (SPInitiatedAuthnRequestType) ((CamelMediationMessage) exchange.getIn()).getMessage().getContent();
        IdentityArtifact in = new IdentityArtifactImpl(new QName("urn:org:atricore:idbus:sso:protocol", "SPInitiatedAuthnRequest"), ssoAuthnRequest);
        idPlanExchange.setIn(in);
        IdentityArtifact<AuthnRequestType> out = new IdentityArtifactImpl<AuthnRequestType>(new QName(SAMLR2Constants.SAML_PROTOCOL_NS, "AuthnRequest"), new AuthnRequestType());
        idPlanExchange.setOut(out);
        identityPlan.prepare(idPlanExchange);
        identityPlan.perform(idPlanExchange);
        if (!idPlanExchange.getStatus().equals(IdentityPlanExecutionStatus.SUCCESS)) {
            throw new SecurityTokenEmissionException("Identity plan returned : " + idPlanExchange.getStatus());
        }
        if (idPlanExchange.getOut() == null) throw new SecurityTokenEmissionException("Plan Exchange OUT must not be null!");
        return (AuthnRequestType) idPlanExchange.getOut().getContent();
    }

    protected CircleOfTrustMemberDescriptor resolveIdp(CamelMediationExchange exchange) throws SSOException {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        SPInitiatedAuthnRequestType ssoAuthnReq = (SPInitiatedAuthnRequestType) in.getMessage().getContent();
        String idpAlias = null;
        CircleOfTrustMemberDescriptor idp = null;
        if (ssoAuthnReq.getRequestAttribute() != null) {
            for (int i = 0; i < ssoAuthnReq.getRequestAttribute().size(); i++) {
                RequestAttributeType a = ssoAuthnReq.getRequestAttribute().get(i);
                if (a.getName().equals("atricore_idp_alias")) idpAlias = a.getValue();
            }
        }
        if (idpAlias != null) {
            if (logger.isDebugEnabled()) logger.debug("Using IdP alias from request attribute " + idpAlias);
            idp = getCotManager().lookupMemberByAlias(idpAlias);
            if (idp == null) {
                throw new SSOException("No IDP found in circle of trust for received alias [" + idpAlias + "], verify your setup.");
            }
        }
        if (idp != null) return idp;
        SamlR2SPMediator mediator = (SamlR2SPMediator) channel.getIdentityMediator();
        idpAlias = mediator.getPreferredIdpAlias();
        if (idpAlias != null) {
            if (logger.isDebugEnabled()) logger.debug("Using preferred IdP alias " + idpAlias);
            idp = getCotManager().lookupMemberByAlias(idpAlias);
            if (idp == null) {
                throw new SSOException("No IDP found in circle of trust for preferred alias [" + idpAlias + "], verify your setup.");
            }
        }
        if (idp != null) return idp;
        throw new SSOException("Cannot resolve IDP, try to configure a preferred IdP for this SP");
    }

    /**
     * @return
     */
    protected FederationChannel resolveIdpChannel(CircleOfTrustMemberDescriptor idpDescriptor) {
        BindingChannel bChannel = (BindingChannel) channel;
        FederatedLocalProvider sp = bChannel.getProvider();
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

    protected EndpointType resolveIdpSsoEndpoint(CircleOfTrustMemberDescriptor idp) throws SSOException {
        SamlR2SPMediator mediator = (SamlR2SPMediator) channel.getIdentityMediator();
        SSOBinding preferredBinding = mediator.getPreferredIdpSSOBindingValue();
        MetadataEntry idpMd = idp.getMetadata();
        if (idpMd == null || idpMd.getEntry() == null) throw new SSOException("No metadata descriptor found for IDP " + idp);
        if (idpMd.getEntry() instanceof EntityDescriptorType) {
            EntityDescriptorType md = (EntityDescriptorType) idpMd.getEntry();
            for (RoleDescriptorType role : md.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
                if (role instanceof IDPSSODescriptorType) {
                    IDPSSODescriptorType idpSsoRole = (IDPSSODescriptorType) role;
                    EndpointType defaultEndpoint = null;
                    for (EndpointType idpSsoEndpoint : idpSsoRole.getSingleSignOnService()) {
                        SSOBinding b = SSOBinding.asEnum(idpSsoEndpoint.getBinding());
                        if (b.equals(preferredBinding)) return idpSsoEndpoint;
                        if (b.equals(SSOBinding.SAMLR2_ARTIFACT)) defaultEndpoint = idpSsoEndpoint;
                        if (defaultEndpoint == null) defaultEndpoint = idpSsoEndpoint;
                    }
                    return defaultEndpoint;
                }
            }
        } else {
            throw new SSOException("Unknown metadata descriptor type " + idpMd.getEntry().getClass().getName());
        }
        logger.debug("No IDP Endpoint supporting binding : " + preferredBinding);
        throw new SSOException("IDP does not support preferred binding " + preferredBinding);
    }

    protected String resolveSpBindingACS() {
        return ((SamlR2SPMediator) channel.getIdentityMediator()).getSpBindingACS();
    }
}
