package org.atricore.idbus.capabilities.sso.main.sp.producers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SSOException;
import org.atricore.idbus.capabilities.sso.main.common.producers.SSOProducer;
import org.atricore.idbus.capabilities.sso.main.sp.SPSecurityContext;
import org.atricore.idbus.capabilities.sso.main.sp.SamlR2SPMediator;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.capabilities.sso.support.metadata.SSOService;
import org.atricore.idbus.common.sso._1_0.protocol.IDPSessionHeartBeatRequestType;
import org.atricore.idbus.common.sso._1_0.protocol.IDPSessionHeartBeatResponseType;
import org.atricore.idbus.common.sso._1_0.protocol.SPSessionHeartBeatRequestType;
import org.atricore.idbus.common.sso._1_0.protocol.SPSessionHeartBeatResponseType;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrust;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustMemberDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptorImpl;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.MediationMessageImpl;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannel;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.IdPChannel;
import org.atricore.idbus.kernel.main.mediation.channel.SPChannel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedLocalProvider;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedProvider;
import org.atricore.idbus.kernel.main.mediation.provider.Provider;
import org.atricore.idbus.kernel.main.session.SSOSessionManager;
import org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException;
import org.atricore.idbus.kernel.main.session.exceptions.SSOSessionException;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;
import org.atricore.idbus.kernel.planning.IdentityPlanningException;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class SessionHeartBeatProducer extends SSOProducer {

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    private static final Log logger = LogFactory.getLog(SessionHeartBeatProducer.class);

    public SessionHeartBeatProducer(AbstractCamelEndpoint<CamelMediationExchange> endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    protected void doProcess(CamelMediationExchange exchange) throws Exception {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        logger.debug("Processing Session Heart-Beat Message : " + in.getMessage().getContent());
        if (in.getMessage().getContent() instanceof SPSessionHeartBeatRequestType) {
            doProcessSPSessionHeartBeat(exchange, (SPSessionHeartBeatRequestType) in.getMessage().getContent());
        } else {
            throw new SSOException("Unsupported message type " + in.getMessage().getContent());
        }
    }

    protected void doProcessSPSessionHeartBeat(CamelMediationExchange exchange, SPSessionHeartBeatRequestType request) throws SSOException, IdentityPlanningException, SSOSessionException {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
        SPSecurityContext secCtx = (SPSecurityContext) in.getMessage().getState().getLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        SPSessionHeartBeatResponseType response = new SPSessionHeartBeatResponseType();
        response.setID(uuidGenerator.generateId());
        response.setInReplayTo(request.getID());
        response.setSsoSessionId(request.getSsoSessionId());
        if (secCtx == null || secCtx.getSessionIndex() == null) {
            if (logger.isDebugEnabled()) logger.debug("No Security Context found for " + getProvider().getName().toUpperCase() + "_SECURITY_CTX: " + secCtx);
            response.setValid(false);
        } else {
            if (logger.isDebugEnabled()) logger.debug("Security Context found " + secCtx);
            try {
                updateSPSecurityContext(secCtx, exchange);
                SamlR2SPMediator mediator = (SamlR2SPMediator) channel.getIdentityMediator();
                if (logger.isTraceEnabled()) logger.trace("Checking IDP Session heart beat for:" + secCtx + ".  Configured interval: " + mediator.getIdpSessionHeartBeatInterval());
                long now = System.currentTimeMillis();
                if (secCtx.getLastIdPSessionHeartBeat() == null || secCtx.getLastIdPSessionHeartBeat() + mediator.getIdpSessionHeartBeatInterval() * 1000L < now) {
                    IDPSessionHeartBeatResponseType idpResp = performIdPSessionHeartBeat(exchange, secCtx);
                    secCtx.setLastIdPSessionHeartBeat(now);
                    if (idpResp != null) {
                        logger.debug("IDP HeartBeat sent, response: " + idpResp.getID() + " isValid:" + idpResp.isValid());
                        response.setValid(idpResp.isValid());
                    } else {
                        logger.debug("IDP HeartBeat not send");
                        response.setValid(true);
                    }
                } else {
                    if (logger.isTraceEnabled()) logger.debug("IDP Session heart beat not necessary");
                    response.setValid(true);
                }
            } catch (NoSuchSessionException e) {
                if (logger.isDebugEnabled()) logger.debug("SP Session not found or invalid : " + secCtx.getSessionIndex());
                response.setValid(false);
            }
        }
        EndpointDescriptor destination = new EndpointDescriptorImpl("SPSessionHeartBeatService", "SPSessionHeartBeatService", endpoint.getBinding(), null, null);
        out.setMessage(new MediationMessageImpl(uuidGenerator.generateId(), response, "SPSessionHeartBeatResponse", null, destination, in.getMessage().getState()));
    }

    protected IDPSessionHeartBeatResponseType performIdPSessionHeartBeat(CamelMediationExchange exchange, SPSecurityContext secCtx) throws SSOException {
        try {
            if (logger.isDebugEnabled()) logger.debug("Triggering IDP Session heart beat");
            CircleOfTrustMemberDescriptor idp = resolveIdp(secCtx.getIdpAlias());
            logger.debug("Using IdP " + idp.getAlias());
            SPChannel spChannel = resolveSpChannel(idp);
            EndpointDescriptor ed = spChannel != null ? resolveIdpHeartBeatEndpoint(spChannel) : null;
            if (ed == null) {
                logger.debug("No HeartBeat endpoint found for " + idp.getAlias() + ". Heart Beat ignored.");
                return null;
            }
            IDPSessionHeartBeatRequestType req = buildIDPSessionHeartBeatRequest(secCtx);
            if (logger.isDebugEnabled()) logger.debug("Sending IDPSessionHeartBeatRequest " + req.getID() + " to IDP " + idp.getAlias() + " using endpoint " + ed.getLocation());
            IDPSessionHeartBeatResponseType res = (IDPSessionHeartBeatResponseType) channel.getIdentityMediator().sendMessage(req, ed, spChannel);
            if (res.getInReplayTo() == null || !res.getInReplayTo().equals(req.getID())) throw new SSOException("Received response is not expected, invalid 'inReplayTo' attribute " + res.getInReplayTo() + ", expected " + req.getID());
            return res;
        } catch (IdentityMediationException e) {
            throw new SSOException(e);
        } catch (IdentityPlanningException e) {
            throw new SSOException(e);
        }
    }

    protected CircleOfTrustMemberDescriptor resolveIdp(String idpAlias) {
        return getCotManager().lookupMemberByAlias(idpAlias);
    }

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

    protected SPChannel resolveSpChannel(CircleOfTrustMemberDescriptor idp) throws SSOException {
        CircleOfTrust cot = getProvider().getChannel().getCircleOfTrust();
        SPChannel spChannel = null;
        for (Provider p : cot.getProviders()) {
            if (p instanceof FederatedLocalProvider) {
                FederatedLocalProvider lp = (FederatedLocalProvider) p;
                if (lp.getChannel() == null || lp.getChannel().getMember() == null) continue;
                if (lp.getChannel().getMember().getAlias().equals(idp.getAlias())) {
                    spChannel = (SPChannel) lp.getChannel();
                    break;
                }
                for (FederationChannel c : lp.getChannels()) {
                    if (c.getMember().getAlias().equals(idp.getAlias())) {
                        spChannel = (SPChannel) c;
                        break;
                    }
                }
                if (spChannel != null) break;
            }
        }
        if (spChannel == null) {
            logger.debug("No SP Channel defined in local providers for " + idp.getAlias());
        }
        return spChannel;
    }

    protected EndpointDescriptor resolveIdpHeartBeatEndpoint(SPChannel spChannel) throws SSOException {
        IdentityMediationEndpoint endpoint = null;
        for (IdentityMediationEndpoint ep : spChannel.getEndpoints()) {
            if (ep.getType().equals(SSOService.IDPSessionHeartBeatService.toString())) {
                if (ep.getBinding().equals(SSOBinding.SSO_LOCAL.getValue())) {
                    endpoint = ep;
                    break;
                } else if (ep.getBinding().equals(SSOBinding.SSO_SOAP.getValue())) {
                    endpoint = ep;
                }
            }
        }
        if (endpoint == null) {
            logger.warn("No IDP Endpoint supporting service/binding " + SSOService.IDPSessionHeartBeatService + " in channel " + spChannel.getName());
            return null;
        }
        try {
            return channel.getIdentityMediator().resolveEndpoint(spChannel, endpoint);
        } catch (IdentityMediationException e) {
            logger.error("Cannot resolve endpoint " + endpoint.getName() + ". " + e.getMessage(), e);
            return null;
        }
    }

    protected IDPSessionHeartBeatRequestType buildIDPSessionHeartBeatRequest(SPSecurityContext secCtx) throws SSOException, IdentityPlanningException {
        IDPSessionHeartBeatRequestType request = new IDPSessionHeartBeatRequestType();
        request.setID(uuidGenerator.generateId());
        request.setSsoSessionId(secCtx.getIdpSsoSession());
        return request;
    }

    protected SPSecurityContext updateSPSecurityContext(SPSecurityContext secCtx, CamelMediationExchange exchange) throws SSOException, SSOSessionException {
        if (logger.isDebugEnabled()) logger.debug("Updating SP Security Context for " + secCtx.getSessionIndex());
        IdPChannel idPChannel = (IdPChannel) getProvider().getChannel();
        SSOSessionManager ssoSessionManager = idPChannel.getSessionManager();
        ssoSessionManager.accessSession(secCtx.getSessionIndex());
        if (logger.isDebugEnabled()) logger.debug("Updated SP security context " + secCtx);
        return secCtx;
    }
}
