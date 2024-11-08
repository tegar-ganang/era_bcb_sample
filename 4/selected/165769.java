package org.qtitools.playr;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.soap.SOAPException;
import org.apache.axis.AxisFault;
import org.apache.axis.attachments.AttachmentPart;
import org.qtitools.util.PropertiesManager;
import r2q2.router.interop.ws.InterOpRouterSoapBindingStub;
import r2q2.router.interop.ws.OutcomeValue;
import r2q2.router.interop.ws.R2Q2NextStageResponse;
import r2q2.router.interop.ws.ResponseValue;
import r2q2.router.interop.ws.WrappedMapEntryType;
import r2q2.router.ws.RouterServiceLocator;

public class R2Q2AssessmentItem {

    static String R2Q2_RENDERED_OUTPUT = "__r2q2_rendered_output__";

    static String R2Q2_RESPONSE_VALUES = "__r2q2_response_values__";

    String guid;

    InterOpRouterSoapBindingStub port;

    List<r2q2.router.interop.ws.WrappedMap> response_list = new ArrayList<r2q2.router.interop.ws.WrappedMap>();

    OutcomeValue[] defaultOutcomes;

    public R2Q2AssessmentItem(URI filename, String basepath, boolean itemFeedback, String img_base_path) throws R2Q2Exception, FileNotFoundException {
        this(filename, basepath, PropertiesManager.getProperty("TestControllerEngine", "R2Q2Router", "http://localhost:8080/r2q2/services/Router?wsdl"), itemFeedback, img_base_path);
    }

    public R2Q2AssessmentItem(URI filename, String basepath, String endpoint, boolean itemFeedback, String img_base_path) throws R2Q2Exception, FileNotFoundException {
        try {
            port = new InterOpRouterSoapBindingStub(new URL(endpoint), new RouterServiceLocator());
        } catch (AxisFault e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        File sourceFile = new File(basepath + File.separator + filename.toString());
        if (!sourceFile.canRead()) {
            throw new FileNotFoundException(sourceFile.toString());
        }
        DataHandler dh = new DataHandler(new FileDataSource(sourceFile));
        port.clearAttachments();
        port.addAttachment(dh);
        try {
            int opts = R2Q2RenderOpts.RENDERBODY | R2Q2RenderOpts.RENDERTITLE;
            if (itemFeedback) opts |= R2Q2RenderOpts.RENDERFEEDBACK;
            guid = port.newSession(filename.toString(), "r2q2.rendering.xhtml.XHTMLRenderer", opts, img_base_path);
        } catch (Exception e) {
            throw new R2Q2Exception(e);
        }
    }

    public List<OutcomeValue> getDefaultOutcomes() {
        return Arrays.asList(defaultOutcomes);
    }

    public byte[] render() throws R2Q2Exception {
        byte[] out = null;
        try {
            defaultOutcomes = port.firstStage(guid);
            out = getRenderAttachment();
        } catch (Exception e) {
            throw new R2Q2Exception(e);
        }
        return out;
    }

    public Map<String, Object> nextStage(Map<String, List<String>> response) throws R2Q2Exception {
        r2q2.router.interop.ws.WrappedMap resp = marshallMap(response);
        Map<String, Object> r2q2_response = new HashMap<String, Object>();
        try {
            response_list.add(resp);
            R2Q2NextStageResponse ret = port.nextStage(guid, resp);
            ResponseValue[] rrv = ret.getResponseVars();
            List<OutcomeValue> rov = new ArrayList<OutcomeValue>();
            rov.addAll(Arrays.asList(ret.getOutcomeVars()));
            boolean correct = true;
            for (ResponseValue rv : rrv) {
                if (rv.isR2Q2_correct() == false) {
                    correct = false;
                    break;
                }
            }
            OutcomeValue ov = new OutcomeValue();
            ov.setBaseType("boolean");
            ov.setCardinality("single");
            ov.setIdentifier("R2Q2_IS_RESPONSE_CORRECT");
            String[] v = { Boolean.toString(correct) };
            ov.setValue(v);
            rov.add(ov);
            r2q2_response.put(R2Q2_RESPONSE_VALUES, rov);
            r2q2_response.put(R2Q2_RENDERED_OUTPUT, getRenderAttachment());
        } catch (Exception e) {
            e.printStackTrace();
            throw new R2Q2Exception(e);
        }
        return r2q2_response;
    }

    protected byte[] getRenderAttachment() throws IOException, SOAPException {
        Object[] attachments = port.getAttachments();
        AttachmentPart ap = (AttachmentPart) attachments[0];
        InputStream is = ap.getDataHandler().getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(is, baos);
        return baos.toByteArray();
    }

    r2q2.router.interop.ws.WrappedMap marshallMap(Map<String, List<String>> m) {
        r2q2.router.interop.ws.WrappedMap resp = new r2q2.router.interop.ws.WrappedMap();
        resp.setEntry(new WrappedMapEntryType[m.size()]);
        int i = 0;
        for (String key : m.keySet()) {
            r2q2.router.interop.ws.WrappedMapEntryType mi = new r2q2.router.interop.ws.WrappedMapEntryType();
            mi.setKey(key);
            String[] v = m.get(key).toArray(new String[m.get(key).size()]);
            mi.setValue(v);
            resp.setEntry(i, mi);
            i++;
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
        for (r2q2.router.interop.ws.WrappedMap wm : response_list) {
            try {
                port.nextStage(guid, wm);
                output = getRenderAttachment();
            } catch (Exception e) {
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
