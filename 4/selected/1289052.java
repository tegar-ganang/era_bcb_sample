package com.jaxws.json.jquery.doc.provider;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import com.jaxws.json.codec.JSONCodec;
import com.jaxws.json.codec.decode.JSONReader;
import com.jaxws.json.codec.doc.AbstractHttpMetadataProvider;
import com.jaxws.json.codec.doc.HttpMetadataProvider;
import com.jaxws.json.codec.encode.WSJSONWriter;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

/**
 * @author Sundaramurthi Saminathan
 * @since JSONWebservice codec version 0.7
 * @version 1.0
 * 
 * JQuery JSON service end point document provider.
 */
public class MetaDataModelServer extends AbstractHttpMetadataProvider implements HttpMetadataProvider {

    private static final String[] queries = new String[] { "jsonmodel" };

    /**
	 * Map holder which keeps end point documents.
	 */
    private static final Map<QName, String> endPointDocuments = Collections.synchronizedMap(new LinkedHashMap<QName, String>());

    /**
	 * Request received codec instance holder
	 */
    private JSONCodec codec;

    /**
	 * "jsonmodel" query handled.
	 */
    public String[] getHandlingQueries() {
        return queries;
    }

    /**
	 * Handler flag, If query string is jsonmodel , its handled by model server.
	 */
    public boolean canHandle(String queryString) {
        return queryString != null && queryString.startsWith(queries[0]);
    }

    /**
	 * end point codec set holder.
	 */
    public void setJSONCodec(JSONCodec codec) {
        this.codec = codec;
    }

    /**
	 * Meta data model content provider.
	 * @see HttpMetadataProvider.getContentType
	 */
    public String getContentType() {
        return "application/json; charset=\"utf-8\"";
    }

    public void process() {
        JSONReader reader = new JSONReader();
        @SuppressWarnings("unchecked") Map<String, Object> doc = (Map<String, Object>) reader.read(WSJSONWriter.writeMetadata(getMetadataModelMap(this.codec.getEndpoint(), true), this.codec.getCustomSerializer()));
        StringBuffer buffer = new StringBuffer();
        getJQTree(doc, buffer, 0);
        endPointDocuments.put(this.codec.getEndpoint().getServiceName(), buffer.toString());
    }

    @SuppressWarnings("unchecked")
    private void getJQTree(Map<String, Object> docNonsorted, StringBuffer buffer, int level) {
        Map<String, Object> doc = new TreeMap<String, Object>(docNonsorted);
        buffer.append('[');
        ++level;
        int index = 0;
        String address = (String) doc.remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        for (String key : doc.keySet()) {
            if (index > 0) buffer.append(",");
            String text = key;
            if (level == 3) {
                text = "<a href=\\\"" + (address != null ? address : "") + "?form" + key + "\\\" target=\\\"content\\\">" + key + "</a>";
            }
            buffer.append("{\"text\": \"" + text + "\",\"classes\":\"level" + level + " index" + index + "\"");
            buffer.append(",\"expanded\": " + (level < 2) + "");
            Object value = doc.get(key);
            if (value != null && value instanceof Map) {
                buffer.append(",\"children\":");
                getJQTree((Map<String, Object>) value, buffer, level);
            }
            buffer.append("}");
            index++;
        }
        buffer.append(']');
    }

    /**
	 * Output responder.
	 */
    public void doResponse(WSHTTPConnection ouStream) throws IOException {
        QName serviceName = this.codec.getEndpoint().getServiceName();
        if (!endPointDocuments.containsKey(serviceName)) process();
        String portDocuments = endPointDocuments.get(this.codec.getEndpoint().getServiceName());
        if (portDocuments != null) {
            doResponse(ouStream, portDocuments);
        } else {
            ouStream.getOutput().write(String.format("Unable to find default document for %s", this.codec.getEndpoint().getPortName()).getBytes());
        }
    }

    public int compareTo(HttpMetadataProvider o) {
        if (o.equals(this)) {
            return 0;
        } else {
            return Integer.MIN_VALUE;
        }
    }
}
