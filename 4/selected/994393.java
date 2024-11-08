package org.qtitools.playr.tce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import org.qtitools.util.PropertiesManager;
import org.qtitools.r2q2.ws.InterOpRouterPortType;
import org.qtitools.r2q2.ws.InterOpRouterService;
import org.qtitools.r2q2.ws.OutcomeValue;
import org.qtitools.r2q2.ws.R2Q2NextStageResponse;
import org.qtitools.r2q2.ws.ResponseValue;

public class R2Q2AssessmentItem {

    static String R2Q2_RENDERED_OUTPUT = "__r2q2_rendered_output__";

    static String R2Q2_RESPONSE_VALUES = "__r2q2_response_values__";

    String guid;

    InterOpRouterPortType port;

    List<org.qtitools.r2q2.ws.WrappedMap> response_list = new ArrayList<org.qtitools.r2q2.ws.WrappedMap>();

    List<OutcomeValue> defaultOutcomes;

    public R2Q2AssessmentItem(URI filename, String basepath, String endpoint, boolean itemFeedback, String img_base_path) throws R2Q2Exception, FileNotFoundException {
        this(filename, basepath, itemFeedback, img_base_path);
        setEndpoint(endpoint);
    }

    public R2Q2AssessmentItem(URI filename, String basepath, boolean itemFeedback, String img_base_path) throws R2Q2Exception, FileNotFoundException {
        InterOpRouterService rs;
        try {
            rs = new InterOpRouterService(new URL(PropertiesManager.getProperty("TestControllerEngine", "R2Q2Router", "http://localhost:8080/r2q2/services/Router?wsdl")), new QName("http://localhost:8080/r2q2/services/InterOpRouter", "InterOpRouterService"));
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            return;
        }
        port = rs.getInterOpRouter();
        File sourceFile = new File(basepath + File.separator + filename.toString());
        if (!sourceFile.canRead()) {
            throw new FileNotFoundException(sourceFile.toString());
        }
        DataHandler dh = new DataHandler(new FileDataSource(sourceFile));
        Map<String, Object> requestContext = ((BindingProvider) port).getRequestContext();
        @SuppressWarnings("unchecked") Map<String, DataHandler> attachments = (Map<String, DataHandler>) requestContext.get(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS);
        if (attachments == null) {
            attachments = new HashMap<String, DataHandler>();
            requestContext.put(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS, attachments);
        }
        attachments.put("text/xml", dh);
        try {
            int opts = R2Q2RenderOpts.RENDERBODY | R2Q2RenderOpts.RENDERTITLE;
            if (itemFeedback) opts |= R2Q2RenderOpts.RENDERFEEDBACK;
            guid = port.newSession(filename.toString(), "r2q2.rendering.xhtml.XHTMLRenderer", opts, img_base_path);
        } catch (Exception e) {
            throw new R2Q2Exception(e);
        }
    }

    public void setEndpoint(String endpoint) {
        ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
    }

    public List<OutcomeValue> getDefaultOutcomes() {
        return defaultOutcomes;
    }

    public byte[] render() throws R2Q2Exception {
        byte[] out = null;
        try {
            defaultOutcomes = port.firstStage(guid);
            out = getRenderAttachment((BindingProvider) port);
        } catch (Exception e) {
            throw new R2Q2Exception(e);
        }
        return out;
    }

    public Map<String, Object> nextStage(Map<String, List<String>> response) throws R2Q2Exception {
        org.qtitools.r2q2.ws.WrappedMap resp = marshallMap(response);
        Map<String, Object> r2q2_response = new HashMap<String, Object>();
        try {
            response_list.add(resp);
            R2Q2NextStageResponse ret = port.nextStage(guid, resp);
            List<ResponseValue> rrv = ret.getResponseVars();
            List<OutcomeValue> rov = ret.getOutcomeVars();
            boolean correct = true;
            for (ResponseValue rv : rrv) {
                if (rv.isR2Q2Correct() == false) {
                    correct = false;
                    break;
                }
            }
            OutcomeValue ov = new OutcomeValue();
            ov.setBaseType("boolean");
            ov.setCardinality("single");
            ov.setIdentifier("R2Q2_IS_RESPONSE_CORRECT");
            ov.getValue().add(Boolean.toString(correct));
            rov.add(ov);
            r2q2_response.put(R2Q2_RESPONSE_VALUES, rov);
            r2q2_response.put(R2Q2_RENDERED_OUTPUT, getRenderAttachment((BindingProvider) port));
        } catch (Exception e) {
            e.printStackTrace();
            throw new R2Q2Exception(e);
        }
        return r2q2_response;
    }

    protected byte[] getRenderAttachment(BindingProvider port) throws IOException {
        Map responseContext = port.getResponseContext();
        Map attachments = (Map) responseContext.get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        String key = (String) attachments.keySet().toArray()[0];
        DataHandler dh = (DataHandler) attachments.get(key);
        ByteArrayInputStream bais = (ByteArrayInputStream) dh.getContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(bais, baos);
        return baos.toByteArray();
    }

    org.qtitools.r2q2.ws.WrappedMap marshallMap(Map<String, List<String>> m) {
        org.qtitools.r2q2.ws.WrappedMap resp = new org.qtitools.r2q2.ws.WrappedMap();
        for (String key : m.keySet()) {
            org.qtitools.r2q2.ws.WrappedMapEntryType mi = new org.qtitools.r2q2.ws.WrappedMapEntryType();
            mi.setKey(key);
            mi.getValue().addAll(m.get(key));
            resp.getEntry().add(mi);
        }
        return resp;
    }

    protected void copy(InputStream _in, OutputStream _out) throws IOException {
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = _in.read(buf)) > 0) _out.write(buf, 0, len);
    }

    public String getQuestionId() {
        return guid;
    }

    /**
	 * Get a item to its last stage by replaying the responses through r2q2
	 * @return
	 * @throws IOException
	 * @throws R2Q2Exception
	 */
    public byte[] replay() throws R2Q2Exception {
        byte[] output = render();
        for (org.qtitools.r2q2.ws.WrappedMap wm : response_list) {
            port.nextStage(guid, wm);
            try {
                output = getRenderAttachment((BindingProvider) port);
            } catch (IOException e) {
                throw new R2Q2Exception(e);
            }
        }
        return output;
    }

    @Override
    protected void finalize() throws Throwable {
        port.terminateSession(guid);
        super.finalize();
    }
}
