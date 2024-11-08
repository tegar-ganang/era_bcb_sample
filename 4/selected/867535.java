package org.atricore.idbus.capabilities.sso.main.binding;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.capabilities.sso.support.core.util.XmlUtils;
import org.atricore.idbus.common.sso._1_0.protocol.SSORequestAbstractType;
import org.atricore.idbus.common.sso._1_0.protocol.SSOResponseType;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.mediation.*;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelMediator;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.AbstractMediationHttpBinding;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimsRequest;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimsResponse;
import org.atricore.idbus.kernel.main.mediation.policy.PolicyEnforcementRequest;
import org.atricore.idbus.kernel.main.mediation.policy.PolicyEnforcementResponse;
import org.w3._1999.xhtml.Html;
import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class SsoHttpArtifactBinding extends AbstractMediationHttpBinding {

    private static final Log logger = LogFactory.getLog(SsoHttpArtifactBinding.class);

    public static final String SSO_ARTIFACT_ID = "SSOArt";

    public SsoHttpArtifactBinding(Channel channel) {
        super(SSOBinding.SSO_ARTIFACT.getValue(), channel);
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
            String ssoArtifact = state.getTransientVariable(SSO_ARTIFACT_ID);
            if (ssoArtifact == null) {
                throw new IllegalStateException("SSO Artifact (" + SSO_ARTIFACT_ID + ") not found in request");
            }
            String relayState = state.getTransientVariable("RelayState");
            MessageQueueManager aqm = getArtifactQueueManager();
            Object ssoMsg = aqm.pullMessage(new ArtifactImpl(ssoArtifact));
            return new MediationMessageImpl(httpMsg.getMessageId(), ssoMsg, null, relayState, null, state);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void copyMessageToExchange(CamelMediationMessage ssoOut, Exchange exchange) {
        MediationMessage out = ssoOut.getMessage();
        EndpointDescriptor ed = out.getDestination();
        assert ed != null : "Mediation Response MUST Provide a destination";
        if (out.getContent() == null) {
            throw new NullPointerException("Cannot Create HTTP Redirect Artifact with null content for action " + ed.getLocation());
        }
        if (logger.isDebugEnabled()) logger.debug("Creating JOSSO Artifact for location " + ed.getLocation());
        try {
            java.lang.Object msgValue = null;
            boolean isResponse = false;
            if (out.getContent() instanceof ClaimsRequest) {
                msgValue = out.getContent();
            } else if (out.getContent() instanceof ClaimsResponse) {
                msgValue = out.getContent();
                isResponse = true;
            } else if (out.getContent() instanceof SSORequestAbstractType) {
                msgValue = out.getContent();
            } else if (out.getContent() instanceof SSOResponseType) {
                msgValue = out.getContent();
                isResponse = true;
            } else if (out.getContent() instanceof PolicyEnforcementRequest) {
                msgValue = out.getContent();
            } else if (out.getContent() instanceof PolicyEnforcementResponse) {
                msgValue = out.getContent();
                isResponse = true;
            } else if (msgValue != null) {
                msgValue = out.getContent();
                logger.warn("Unknown message content : " + msgValue.getClass().getName());
                if (msgValue.getClass().getSimpleName().contains("esponse")) isResponse = true;
            }
            MessageQueueManager aqm = getArtifactQueueManager();
            Artifact contentArtifact = aqm.pushMessage(msgValue);
            Message httpOut = exchange.getOut();
            Message httpIn = exchange.getIn();
            String artifactLocation = this.buildHttpTargetLocation(httpIn, ed, isResponse);
            artifactLocation += (artifactLocation.contains("?") ? "&" : "?") + SSO_ARTIFACT_ID + "=" + contentArtifact.getContent();
            if (logger.isDebugEnabled()) logger.debug("Redirecting with artifact to " + artifactLocation);
            copyBackState(out.getState(), exchange);
            if (!isEnableAjax()) {
                httpOut.getHeaders().put("Cache-Control", "no-cache, no-store");
                httpOut.getHeaders().put("Pragma", "no-cache");
                httpOut.getHeaders().put("http.responseCode", 302);
                httpOut.getHeaders().put("Content-Type", "text/html");
                httpOut.getHeaders().put("Location", artifactLocation);
            } else {
                Html redir = this.createHtmlArtifactMessage(artifactLocation);
                String marshalledHttpResponseBody = XmlUtils.marshal(redir, "http://www.w3.org/1999/xhtml", "html", new String[] { "org.w3._1999.xhtml" });
                httpOut.getHeaders().put("Cache-Control", "no-cache, no-store");
                httpOut.getHeaders().put("Pragma", "no-cache");
                httpOut.getHeaders().put("http.responseCode", 200);
                httpOut.getHeaders().put("Content-Type", "text/html");
                ByteArrayInputStream baos = new ByteArrayInputStream(marshalledHttpResponseBody.getBytes());
                httpOut.setBody(baos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected MessageQueueManager getArtifactQueueManager() {
        AbstractCamelMediator mediator = (AbstractCamelMediator) getChannel().getIdentityMediator();
        return mediator.getArtifactQueueManager();
    }
}
