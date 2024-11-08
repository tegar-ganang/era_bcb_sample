package at.arcsmed.mpower.communicator.soap.call.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import at.arcsmed.mpower.communicator.service.call.CallResponse;
import at.arcsmed.mpower.communicator.soap.call.client.soap.ActiveCallEndedMessage;
import at.arcsmed.mpower.communicator.soap.call.client.soap.ISIPCallClient;
import at.arcsmed.mpower.communicator.soap.call.client.soap.IncomingCallCanceledMessage;
import at.arcsmed.mpower.communicator.soap.call.client.soap.IncomingCallMessage;
import at.arcsmed.mpower.communicator.soap.call.client.soap.OutgoingCallAcceptedMessage;
import at.arcsmed.mpower.communicator.soap.call.client.soap.OutgoingCallRejectedMessage;
import at.arcsmed.mpower.communicator.soap.call.client.soap.SIPCallClient_Service;
import at.arcsmed.mpower.communicator.soap.call.client.soap.Status;

/**
 * CallClient is the implementation of the CallResponse.<br>
 * Is responsible to pass each request e.g. showIncomingCall(...) to the real
 * remote end (a soap webservice).
 * 
 * @author msi
 * @version 1.0.1
 */
public class CallClient implements CallResponse, CallClientUrlChangeListener {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private ISIPCallClient port;

    private Properties properties = null;

    public CallClient() {
        log.info("Init service soap call client");
        initWebServiceClient();
    }

    /**
	 * Initializes the webservice.<br>
	 * Creates the new port instance and overwrites the remote URL where the SOAP
	 * request will be sent.
	 */
    @SuppressWarnings("unchecked")
    private void initWebServiceClient() {
        log.debug("Initialize SIPCallManagement web service client");
        String sipCallManagerEndpoint = "http://localhost:8080/MPOWER-SIPCallManagement/SIPCallManagement";
        SIPCallClient_Service service = new SIPCallClient_Service();
        port = service.getISIPCallClient();
    }

    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    public void showIncomingCall(String accountSipId, String sipId) {
        log.info("Calling SIPCallClient.incomingCall from sip id:" + sipId);
        Map<String, Object> reqContext = ((javax.xml.ws.BindingProvider) port).getRequestContext();
        String oldCallClientUrl = (String) reqContext.get(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        log.warn("REMOTE END:" + oldCallClientUrl);
        if (port == null) initWebServiceClient();
        try {
            IncomingCallMessage message = new IncomingCallMessage();
            message.setAccountSipId(accountSipId);
            message.setCalleeSipId(sipId);
            port.incomingCall(message);
        } catch (Exception ex) {
            log.error("Error calling SIPCallClient.incomingCall for sip id:" + sipId, ex);
            ex.printStackTrace();
        }
        log.info("return value: void");
    }

    public void showActiveCallEnded(String accountSipId, String sipId) {
        log.info("Calling SIPCallClient.activeCallEnded for sip id:" + sipId);
        if (port == null) initWebServiceClient();
        try {
            ActiveCallEndedMessage message = new ActiveCallEndedMessage();
            message.setAccountSipId(accountSipId);
            message.setCalleeSipId(sipId);
            port.activeCallEnded(message);
        } catch (Exception ex) {
            log.error("Error calling SIPCallClient.activeCallEnded for sip id:" + sipId, ex);
            ex.printStackTrace();
        }
        log.info("return value: void");
    }

    public void showIncomingCallCanceled(String accountSipId, String sipId) {
        log.info("Calling SIPCallClient.incomingCallCanceled from sip id:" + sipId);
        if (port == null) initWebServiceClient();
        try {
            IncomingCallCanceledMessage message = new IncomingCallCanceledMessage();
            message.setAccountSipId(accountSipId);
            message.setCalleeSipId(sipId);
            port.incomingCallCanceled(message);
        } catch (Exception ex) {
            log.error("Error calling SIPCallClient.incomingCallCanceled for sip id:" + sipId, ex);
            ex.printStackTrace();
        }
        log.info("return value: void");
    }

    public void showOutgoingCallAccepted(String accountSipId, String sipId) {
        log.info("Calling SIPCallClient.outgoingCallAccepted for sip id:" + sipId);
        if (port == null) initWebServiceClient();
        try {
            OutgoingCallAcceptedMessage message = new OutgoingCallAcceptedMessage();
            message.setAccountSipId(accountSipId);
            message.setCalleeSipId(sipId);
            port.outgoingCallAccepted(message);
        } catch (Exception ex) {
            log.error("Error calling SIPCallClient.outgoingCallAccepted for sip id:" + sipId, ex);
            ex.printStackTrace();
        }
        log.info("return value: void");
    }

    public void showOutgoingCallRejected(String accountSipId, String sipId) {
        log.info("Calling SIPCallClient.outgoingCallRejected for sip id:" + sipId);
        if (port == null) initWebServiceClient();
        try {
            OutgoingCallRejectedMessage message = new OutgoingCallRejectedMessage();
            message.setAccountSipId(accountSipId);
            message.setCalleeSipId(sipId);
            port.outgoingCallRejected(message);
        } catch (Exception ex) {
            log.error("Error calling SIPCallClient.outgoingCallRejected for sip id:" + sipId, ex);
            ex.printStackTrace();
        }
        log.info("return value: void");
    }

    public boolean setNewUrl(String callClientUrl) {
        if (port != null) {
            Map<String, Object> reqContext = ((javax.xml.ws.BindingProvider) port).getRequestContext();
            String oldCallClientUrl = (String) reqContext.get(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            log.debug("Setting Url for ISIPCallManagement from " + oldCallClientUrl + " to " + callClientUrl);
            reqContext.put(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY, callClientUrl);
            return true;
        } else {
            log.warn("Can't set CallClientURL (The web gui which will control the mpower commmunicator) !");
            return false;
        }
    }
}
