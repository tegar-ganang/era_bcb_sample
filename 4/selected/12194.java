package org.soda.dpws.transport.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.activation.DataHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.soda.dpws.DPWSException;
import org.soda.dpws.attachments.Attachments;
import org.soda.dpws.attachments.JavaMailAttachments;
import org.soda.dpws.attachments.SimpleAttachment;
import org.soda.dpws.attachments.StreamedAttachments;
import org.soda.dpws.cache.UnreachableDeviceException;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.soap.SoapConstants;
import org.soda.dpws.transport.AbstractMessageSender;
import org.soda.dpws.transport.Channel;
import org.soda.dpws.util.OutMessageDataSource;
import org.soda.dpws.util.STAXUtils;
import eu.more.core.internal.msoa.MSOAinfo;
import eu.more.core.internal.msoa.MappedMessage;
import eu.more.core.internal.msoa.TypeConversation;

/**
 * Sends a http message via commons http client. To customize the HttpClient
 * parameters, set the property <code>HTTP_CLIENT_PARAMS</code> on the
 * DPWSContextImpl for your invocation.
 * 
 */
public class CommonsHttpMessageSender extends AbstractMessageSender {

    private Log log = LogFactory.getLog(CommonsHttpMessageSender.class);

    private static final ThreadLocal<HttpState> httpState = new ThreadLocal<HttpState>();

    protected PostMethod postMethod;

    protected HttpClient client;

    protected HttpState state;

    /**
   * 
   */
    public static final String HTTP_CLIENT_PARAMS = "httpClient.params";

    /**
   * 
   */
    public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; XFire Client +http://xfire.codehaus.org)";

    /**
   * 
   */
    public static final String HTTP_PROXY_HOST = "http.proxyHost";

    /**
   * 
   */
    public static final String HTTP_PROXY_PORT = "http.proxyPort";

    /**
   * 
   */
    public static final String HTTP_STATE = "httpClient.httpstate";

    /**
   * 
   */
    public static final String HTTP_CLIENT = "httpClient";

    /**
   * @param message
   * @param context
   */
    public CommonsHttpMessageSender(OutMessage message, DPWSContextImpl context) {
        super(message, context);
    }

    public void open() throws IOException, DPWSException {
        client = (HttpClient) ((HttpChannel) getMessage().getChannel()).getProperty(HTTP_CLIENT);
        if (client == null) {
            client = new HttpClient();
            ((HttpChannel) getMessage().getChannel()).setProperty(HTTP_CLIENT, client);
        }
        DPWSContextImpl context = getMessageContext();
        HttpClientParams params = (HttpClientParams) context.getContextualProperty(HTTP_CLIENT_PARAMS);
        if (params == null) {
            params = client.getParams();
            client.getParams().setParameter("http.useragent", USER_AGENT);
            client.getParams().setBooleanParameter("http.protocol.expect-continue", true);
            client.getParams().setVersion(HttpVersion.HTTP_1_1);
        } else {
            client.setParams(params);
        }
        String proxyHost = (String) context.getContextualProperty(HTTP_PROXY_HOST);
        if (proxyHost != null) {
            String portS = (String) context.getContextualProperty(HTTP_PROXY_PORT);
            int port = 80;
            if (portS != null) port = Integer.parseInt(portS);
            client.getHostConfiguration().setProxy(proxyHost, port);
        }
        state = (HttpState) context.getContextualProperty(HTTP_STATE);
        if (state == null) state = getHttpState();
        postMethod = new PostMethod(getUri());
        String username = (String) context.getContextualProperty(Channel.USERNAME);
        if (username != null) {
            String password = (String) context.getContextualProperty(Channel.PASSWORD);
            client.getParams().setAuthenticationPreemptive(true);
            state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        }
        if (getSoapAction() != null) {
            postMethod.setRequestHeader("SOAPAction", getQuotedSoapAction());
        }
        OutMessage message = getMessage();
        boolean mtomEnabled = Boolean.valueOf((String) context.getContextualProperty(SoapConstants.MTOM_ENABLED)).booleanValue();
        Attachments atts = message.getAttachments();
        if (mtomEnabled || atts != null) {
            if (atts == null) {
                atts = new JavaMailAttachments();
                message.setAttachments(atts);
            }
            OutMessageDataSource source = new OutMessageDataSource(context, message);
            DataHandler soapHandler = new DataHandler(source);
            atts.setSoapContentType(HttpChannel.getSoapMimeType(message));
            atts.setSoapMessage(new SimpleAttachment(source.getName(), soapHandler));
            postMethod.setRequestHeader("Content-Type", atts.getContentType());
        } else {
            postMethod.setRequestHeader("Content-Type", HttpChannel.getSoapMimeType(getMessage()));
        }
    }

    public void send() throws DPWSException, IOException {
        RequestEntity requestEntity;
        OutMessage message = getMessage();
        DPWSContextImpl context = getMessageContext();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Attachments atts = message.getAttachments();
        MSOAinfo msoainfo = (MSOAinfo) context.getProperty("msoa");
        if (atts != null) {
            atts.write(bos);
        } else if (msoainfo != null) {
            if ((msoainfo.messagemap.containsKey(context.getExchange().getOperation().getOutputMessage().getName().getLocalPart())) && (msoainfo.usemsoa)) {
                log.debug("Sending Request as MSOA Message");
                MappedMessage mm = msoainfo.messagemap.get(context.getExchange().getOperation().getOutputMessage().getName().getLocalPart());
                byte[] b = TypeConversation.toByta(mm.number);
                log.debug("Found Message {" + mm.name + "} in MSOA Map! ... Sending Message Number " + mm.number);
                requestEntity = new ByteArrayRequestEntity(TypeConversation.toByta(mm.number));
                postMethod.setRequestEntity(requestEntity);
                Header h = postMethod.getRequestHeader("Content-Type");
                h.setValue("application/msoa; charset=UTF-8");
                postMethod.setRequestHeader(h);
                try {
                    client.executeMethod(null, postMethod, state);
                } catch (org.apache.commons.httpclient.HttpException e) {
                    throw new UnreachableDeviceException(e);
                } catch (IOException e) {
                    throw new UnreachableDeviceException(e);
                }
                return;
            } else HttpChannel.writeWithoutAttachments(context, message, bos);
            log.debug("SOAP Message send: " + bos.toString());
        } else HttpChannel.writeWithoutAttachments(context, message, bos);
        log.info("Sending message -> data : " + bos.toString());
        requestEntity = new ByteArrayRequestEntity(bos.toByteArray());
        postMethod.setRequestEntity(requestEntity);
        try {
            client.executeMethod(null, postMethod, state);
        } catch (org.apache.commons.httpclient.HttpException e) {
            throw new UnreachableDeviceException(e);
        } catch (IOException e) {
            throw new UnreachableDeviceException(e);
        }
    }

    public boolean hasResponse() {
        if (postMethod == null) return false;
        Header header = postMethod.getResponseHeader("Content-Type");
        if (header == null) return false;
        String ct = header.getValue();
        return ct != null && ct.length() > 0;
    }

    private HttpState getHttpState() {
        HttpState state = httpState.get();
        if (null == state) {
            state = new HttpState();
            httpState.set(state);
        }
        return state;
    }

    public InMessage getInMessage() throws DPWSException {
        try {
            String ct = postMethod.getResponseHeader("Content-Type").getValue();
            InputStream in = postMethod.getResponseBodyAsStream();
            if (ct.toLowerCase().indexOf("multipart/related") != -1) {
                Attachments atts = new StreamedAttachments(in, ct);
                InputStream msgIs = atts.getSoapMessage().getDataHandler().getInputStream();
                InMessage msg = new InMessage(STAXUtils.createXMLStreamReader(msgIs, getEncoding(), getMessageContext()), getUri());
                msg.setAttachments(atts);
                return msg;
            }
            if (ct.toLowerCase().startsWith("application/msoa")) {
                InMessage msg = new InMessage(STAXUtils.createXMLStreamReader(in, getEncoding(), getMessageContext()), getUri());
                msg.setProperty(new String("msoa.usage"), new Boolean(true));
                msg.setProperty(new String("inputstream"), postMethod.getResponseBody());
                return msg;
            }
            return new InMessage(STAXUtils.createXMLStreamReader(in, getEncoding(), getMessageContext()), getUri());
        } catch (IOException ioe) {
            throw new DPWSException("Could not get incoming message", ioe);
        }
    }

    public void close() throws DPWSException {
        if (postMethod != null) {
            postMethod.abort();
            postMethod.releaseConnection();
        }
    }
}
