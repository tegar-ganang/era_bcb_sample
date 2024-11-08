package org.atricore.idbus.capabilities.sso.main.binding;

import oasis.names.tc.saml._2_0.metadata.*;
import oasis.names.tc.saml._2_0.protocol.ArtifactResolveType;
import oasis.names.tc.saml._2_0.protocol.ArtifactResponseType;
import oasis.names.tc.saml._2_0.protocol.RequestAbstractType;
import oasis.names.tc.saml._2_0.protocol.StatusResponseType;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SSOException;
import org.atricore.idbus.capabilities.sso.main.binding.plans.SamlR2ArtifactToSamlR2ArtifactResolvePlan;
import org.atricore.idbus.capabilities.sso.support.SAMLR11Constants;
import org.atricore.idbus.capabilities.sso.support.SAMLR2Constants;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.kernel.main.federation.metadata.*;
import org.atricore.idbus.kernel.main.mediation.*;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannel;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelMediator;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.AbstractMediationHttpBinding;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedLocalProvider;
import org.atricore.idbus.kernel.planning.*;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import static org.atricore.idbus.capabilities.sso.main.common.plans.SSOPlanningConstants.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class SamlR11HttpArtifactBinding extends AbstractMediationHttpBinding {

    private static final Log logger = LogFactory.getLog(SamlR2HttpArtifactBinding.class);

    private String artifactParameterName = "SAMLart";

    private SamlArtifactEncoder artifactEncoder;

    public SamlR11HttpArtifactBinding(Channel channel) {
        super(SSOBinding.SAMLR11_ARTIFACT.getValue(), channel);
        artifactEncoder = new SamlR11ArtifactEncoderImpl();
    }

    public MediationMessage createMessage(CamelMediationMessage message) {
        Exchange exchange = message.getExchange().getExchange();
        logger.debug("Create Message Body from exchange " + exchange.getClass().getName());
        Message httpMsg = exchange.getIn();
        if (httpMsg.getHeader("http.requestMethod") == null) {
            throw new IllegalArgumentException("Unknown message, no valid HTTP Method header found!");
        }
        try {
            MediationState state = createMediationState(exchange);
            String samlArtStr = state.getTransientVariable(artifactParameterName);
            String relayState = state.getTransientVariable("RelayState");
            if (samlArtStr == null || "".equals(samlArtStr)) throw new IllegalArgumentException("'" + artifactParameterName + "' parameter not found!");
            SamlArtifact samlArtifact = getEncoder().decode(samlArtStr);
            String sourceId = samlArtifact.getSourceID();
            CircleOfTrustMemberDescriptor resolverMemberDescr = this.getProvider().getCotManager().loolkupMemberById(sourceId);
            if (resolverMemberDescr == null) {
                logger.warn("Unkonw SAML Artifact SourceID [" + sourceId + "]");
                throw new SSOException("Unkonw SAML Artifact SourceID [" + sourceId + "]");
            }
            FederationChannel fChannel = (FederationChannel) channel;
            CircleOfTrustMemberDescriptor memberDescr = fChannel.getMember();
            MetadataEntry md = resolverMemberDescr.getMetadata();
            EntityDescriptorType samlMd = (EntityDescriptorType) md.getEntry();
            EndpointDescriptor samlResolverEd = resolveArtifactResolveEndpoint(samlMd, samlArtifact.getEndpointIndex());
            ArtifactResolveType req = buildArtifactResolve(memberDescr, resolverMemberDescr, samlArtStr, samlResolverEd, fChannel);
            ArtifactResponseType res = (ArtifactResponseType) this.channel.getIdentityMediator().sendMessage(req, samlResolverEd, channel);
            java.lang.Object msgValue = res.getAny();
            if (logger.isTraceEnabled()) logger.trace("Received SAML Message : " + msgValue);
            if (msgValue instanceof JAXBElement) {
                msgValue = ((JAXBElement) msgValue).getValue();
            }
            if (msgValue instanceof RequestAbstractType) {
                RequestAbstractType samlRequest = (RequestAbstractType) msgValue;
                logger.debug("Received SAML Request " + samlRequest.getID());
                return new MediationMessageImpl<RequestAbstractType>(httpMsg.getMessageId(), samlRequest, null, null, relayState, null, state);
            } else if (msgValue instanceof StatusResponseType) {
                StatusResponseType samlResponse = (StatusResponseType) msgValue;
                logger.debug("Received SAML Response " + samlResponse.getID());
                return new MediationMessageImpl<StatusResponseType>(httpMsg.getMessageId(), samlResponse, null, null, relayState, null, state);
            } else {
                throw new RuntimeException("Unknown SAML 2.0 Type : " + msgValue);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void copyMessageToExchange(CamelMediationMessage samlOut, Exchange exchange) {
        try {
            MediationMessage out = samlOut.getMessage();
            EndpointDescriptor ed = out.getDestination();
            assert ed != null : "Mediation Response MUST Provide a destination";
            if (out.getContent() == null) {
                throw new NullPointerException("Cannot Create form with null content for action " + ed.getLocation());
            }
            if (logger.isDebugEnabled()) logger.debug("Creating HTML Artifact to " + ed.getLocation());
            String msgName = null;
            String destAlias = null;
            java.lang.Object msgValue = out.getContent();
            String element = out.getContentType();
            boolean isResponse = false;
            if (out.getContent() instanceof RequestAbstractType) {
                msgName = "SAMLRequest";
            } else if (out.getContent() instanceof StatusResponseType) {
                msgName = "SAMLResponse";
                isResponse = true;
            }
            if (msgValue == null) {
                throw new NullPointerException("Cannot send null content to " + ed.getLocation());
            }
            MessageQueueManager aqm = getArtifactQueueManager();
            Artifact artifact = aqm.pushMessage(msgValue);
            CircleOfTrustMemberDescriptor cotMember = getCotMember(destAlias);
            SamlArtifact samlArtifact = new SamlArtifact(SAMLR11Constants.SAML_ARTIFACT_TYPE, 0, cotMember.getId(), artifact.getContent());
            if (logger.isTraceEnabled()) logger.trace("Created SAML Artifact " + samlArtifact);
            String samlArtifactEnc = getEncoder().encode(samlArtifact);
            String qryString = "?" + artifactParameterName + "=" + samlArtifactEnc;
            if (out.getRelayState() != null) {
                qryString += "&relayState=" + out.getRelayState();
            }
            Message httpOut = exchange.getOut();
            Message httpIn = exchange.getIn();
            String redirLocation = this.buildHttpTargetLocation(httpIn, ed, isResponse) + qryString;
            copyBackState(out.getState(), exchange);
            httpOut.getHeaders().put("Cache-Control", "no-cache, no-store");
            httpOut.getHeaders().put("Pragma", "no-cache");
            httpOut.getHeaders().put("http.responseCode", 302);
            httpOut.getHeaders().put("Content-Type", "text/html");
            httpOut.getHeaders().put("Location", redirLocation);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected MessageQueueManager getArtifactQueueManager() {
        AbstractCamelMediator mediator = (AbstractCamelMediator) getChannel().getIdentityMediator();
        return mediator.getArtifactQueueManager();
    }

    public String getArtifactParameterName() {
        return artifactParameterName;
    }

    public void setArtifactParameterName(String artifactParameterName) {
        this.artifactParameterName = artifactParameterName;
    }

    public SamlArtifactEncoder getEncoder() {
        return getArtifactEncoder();
    }

    public SamlArtifactEncoder getArtifactEncoder() {
        return artifactEncoder;
    }

    public void setArtifactEncoder(SamlArtifactEncoder artifactEncoder) {
        this.artifactEncoder = artifactEncoder;
    }

    protected EndpointDescriptor resolveArtifactResolveEndpoint(EntityDescriptorType samlMd, int edIdx) throws CircleOfTrustManagerException {
        EndpointType samlEndpoint = null;
        for (RoleDescriptorType roleDescriptor : samlMd.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
            if (roleDescriptor instanceof SSODescriptorType) {
                SSODescriptorType ssoDescriptor = (SSODescriptorType) roleDescriptor;
                EndpointType soapSamlEndpoint = null;
                EndpointType defaultSamlEndpoint = null;
                for (IndexedEndpointType samlIdxEndpoint : ssoDescriptor.getArtifactResolutionService()) {
                    if (samlIdxEndpoint.isIsDefault() != null && samlIdxEndpoint.isIsDefault() && (samlIdxEndpoint.getBinding().equals(SSOBinding.SAMLR11_SOAP.getValue()))) {
                        defaultSamlEndpoint = samlIdxEndpoint;
                    }
                    if (edIdx > 0) {
                        if (edIdx == samlIdxEndpoint.getIndex()) {
                            samlEndpoint = samlIdxEndpoint;
                            break;
                        }
                    } else {
                        if (samlIdxEndpoint.getBinding().equals(SSOBinding.SAMLR11_SOAP.getValue())) {
                            soapSamlEndpoint = samlIdxEndpoint;
                        }
                    }
                }
                if (samlEndpoint == null) samlEndpoint = soapSamlEndpoint;
                if (samlEndpoint == null) samlEndpoint = defaultSamlEndpoint;
                if (samlEndpoint != null) break;
            }
            if (samlEndpoint != null) break;
        }
        if (samlEndpoint == null) throw new RuntimeException("Cannot resovle SAML 2.0 ArtifactResolutionService for entity " + samlMd.getEntityID());
        return new EndpointDescriptorImpl("ArtifactResolutionService", "ArtifactResolveType", samlEndpoint.getBinding(), samlEndpoint.getLocation(), samlEndpoint.getResponseLocation());
    }

    protected ArtifactResolveType buildArtifactResolve(CircleOfTrustMemberDescriptor member, CircleOfTrustMemberDescriptor destMember, String samlArtEnc, EndpointDescriptor ed, FederationChannel channel) throws IdentityPlanningException, SSOException {
        IdentityPlan identityPlan = findIdentityPlanOfType(SamlR2ArtifactToSamlR2ArtifactResolvePlan.class);
        IdentityPlanExecutionExchange idPlanExchange = new IdentityPlanExecutionExchangeImpl();
        IdentityMediationEndpoint idEndpoint = null;
        idPlanExchange.setProperty(VAR_COT, this.getProvider().getCotManager().getCot());
        idPlanExchange.setProperty(VAR_COT_MEMBER, member);
        idPlanExchange.setProperty(VAR_CHANNEL, this.channel);
        idPlanExchange.setProperty(VAR_ENDPOINT, idEndpoint);
        idPlanExchange.setProperty(VAR_DESTINATION_COT_MEMBER, destMember);
        idPlanExchange.setProperty(VAR_DESTINATION_ENDPOINT_DESCRIPTOR, ed);
        idPlanExchange.setProperty(VAR_COT_MEMBER, channel.getMember());
        idPlanExchange.setProperty(VAR_RESPONSE_CHANNEL, channel);
        IdentityArtifact in = new IdentityArtifactImpl(new QName(SAMLR2Constants.SAML_PROTOCOL_NS, "SAMLart"), samlArtEnc);
        idPlanExchange.setIn(in);
        IdentityArtifact<ArtifactResolveType> out = new IdentityArtifactImpl<ArtifactResolveType>(new QName(SAMLR2Constants.SAML_PROTOCOL_NS, "ArtifactResolve"), new ArtifactResolveType());
        idPlanExchange.setOut(out);
        identityPlan.prepare(idPlanExchange);
        identityPlan.perform(idPlanExchange);
        if (!idPlanExchange.getStatus().equals(IdentityPlanExecutionStatus.SUCCESS)) {
            throw new SSOException("Identity plan returned : " + idPlanExchange.getStatus());
        }
        if (idPlanExchange.getOut() == null) throw new SSOException("Plan Exchange OUT must not be null!");
        return (ArtifactResolveType) idPlanExchange.getOut().getContent();
    }

    protected IdentityPlan findIdentityPlanOfType(Class planClass) throws SSOException {
        for (IdentityMediationEndpoint e : channel.getEndpoints()) {
            if (e.getIdentityPlans() == null) continue;
            for (IdentityPlan p : e.getIdentityPlans()) {
                if (planClass.isInstance(p)) return p;
            }
        }
        logger.warn("No identity plan of class " + planClass.getName() + " was found for binding " + SamlR2HttpArtifactBinding.class.getSimpleName());
        return null;
    }

    protected CircleOfTrustMemberDescriptor getCotMember(String destAlias) throws SSOException {
        FederationChannel fc = getFederationChannel(destAlias);
        return fc.getMember();
    }

    /**
     * Gest the FederationChannel that is used to send a message using HTTP-Artifact binding.
     */
    protected FederationChannel getFederationChannel(String destAlias) throws SSOException {
        if (channel instanceof FederationChannel) {
            return (FederationChannel) channel;
        }
        FederatedLocalProvider provider = getFederatedProvider();
        FederationChannel fChannel = provider.getChannel();
        for (FederationChannel f : provider.getChannels()) {
            if (f.getMember().getAlias().equals(destAlias)) fChannel = f;
        }
        return fChannel;
    }

    /**
     * Returns the FederatedLocalProvider used to send a SAML Message using HTTP-Artifact Binding
     * @return
     * @throws org.atricore.idbus.capabilities.sso.main.SSOException
     */
    protected FederatedLocalProvider getFederatedProvider() throws SSOException {
        if (channel instanceof FederationChannel) {
            FederationChannel fChannel = (FederationChannel) channel;
            return fChannel.getProvider();
        }
        if (channel instanceof BindingChannel) {
            BindingChannel bChannel = (BindingChannel) channel;
            FederatedLocalProvider provider = bChannel.getProvider();
            return provider;
        }
        throw new SSOException("Unsupported channel type : " + channel);
    }
}
