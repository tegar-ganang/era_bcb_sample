package org.orbeon.oxf.processor;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.xml.ForwardingContentHandler;
import org.orbeon.oxf.xml.XPathUtils;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourceServer extends ProcessorImpl {

    public static final String RESOURCE_SERVER_NAMESPACE_URI = "http://www.orbeon.com/oxf/resource-server";

    public static final String MIMETYPES_NAMESPACE_URI = "http://www.orbeon.com/oxf/mime-types";

    public static final String MIMETYPE_INPUT = "mime-types";

    public ResourceServer() {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG, RESOURCE_SERVER_NAMESPACE_URI));
        addInputInfo(new ProcessorInputOutputInfo(MIMETYPE_INPUT, MIMETYPES_NAMESPACE_URI));
    }

    public void start(org.orbeon.oxf.pipeline.api.PipelineContext context) {
        ExternalContext externalContext = (ExternalContext) context.getAttribute(org.orbeon.oxf.pipeline.api.PipelineContext.EXTERNAL_CONTEXT);
        ExternalContext.Response response = externalContext.getResponse();
        MimeTypeConfig mimeTypeConfig = (MimeTypeConfig) readCacheInputAsObject(context, getInputByName(MIMETYPE_INPUT), new CacheableInputReader() {

            public Object read(PipelineContext context, ProcessorInput input) {
                MimeTypesContentHandler ch = new MimeTypesContentHandler();
                readInputAsSAX(context, input, ch);
                return ch.getMimeTypes();
            }
        });
        try {
            Node configNode = readCacheInputAsDOM(context, INPUT_CONFIG);
            String urlString = XPathUtils.selectStringValueNormalize(configNode, "url");
            if (urlString == null) {
                urlString = XPathUtils.selectStringValueNormalize(configNode, "path");
                if (urlString == null) throw new OXFException("Missing configuration.");
                urlString = "oxf:" + urlString;
            }
            URLConnection urlConnection = null;
            InputStream urlConnectionInputStream = null;
            try {
                try {
                    URL newURL = URLFactory.createURL(urlString);
                    urlConnection = newURL.openConnection();
                    urlConnectionInputStream = urlConnection.getInputStream();
                    long lastModified = NetUtils.getLastModified(urlConnection);
                    response.setCaching(lastModified, false, false);
                    if (!response.checkIfModifiedSince(lastModified, false)) {
                        response.setStatus(ExternalContext.SC_NOT_MODIFIED);
                        return;
                    }
                    String contentType = mimeTypeConfig.getMimeType(urlString);
                    if (contentType != null) response.setContentType(contentType);
                    int length = urlConnection.getContentLength();
                    if (length > 0) response.setContentLength(length);
                } catch (IOException e) {
                    response.setStatus(ExternalContext.SC_NOT_FOUND);
                    return;
                }
                NetUtils.copyStream(urlConnectionInputStream, response.getOutputStream());
            } finally {
                if (urlConnection != null && "file".equalsIgnoreCase(urlConnection.getURL().getProtocol())) {
                    if (urlConnectionInputStream != null) urlConnectionInputStream.close();
                }
            }
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }

    private static class MimeTypesContentHandler extends ForwardingContentHandler {

        public static final String MIMETYPE_ELEMENT = "mime-type";

        public static final String NAME_ELEMENT = "name";

        public static final String PATTERN_ELEMENT = "pattern";

        public static final int NAME_STATUS = 1;

        public static final int EXT_STATUS = 2;

        private int status = 0;

        private StringBuffer buff = new StringBuffer();

        private String name;

        private MimeTypeConfig mimeTypeConfig = new MimeTypeConfig();

        public void startElement(String uri, String localname, String qName, Attributes attributes) throws SAXException {
            if (NAME_ELEMENT.equals(localname)) status = NAME_STATUS; else if (PATTERN_ELEMENT.equals(localname)) status = EXT_STATUS;
        }

        public void characters(char[] chars, int start, int length) throws SAXException {
            if (status == NAME_STATUS || status == EXT_STATUS) buff.append(chars, start, length);
        }

        public void endElement(String uri, String localname, String qName) throws SAXException {
            if (NAME_ELEMENT.equals(localname)) {
                name = buff.toString().trim();
            } else if (PATTERN_ELEMENT.equals(localname)) {
                mimeTypeConfig.define(buff.toString().trim(), name);
            } else if (MIMETYPE_ELEMENT.equals(localname)) {
                name = null;
            }
            buff.delete(0, buff.length());
        }

        public MimeTypeConfig getMimeTypes() {
            return mimeTypeConfig;
        }
    }

    private static class PatternToMimeType {

        public String pattern;

        public String mimeType;

        public PatternToMimeType(String pattern, String mimeType) {
            this.pattern = pattern;
            this.mimeType = mimeType;
        }

        public boolean matches(String path) {
            if (pattern.equals("*")) {
                return true;
            } else if (pattern.startsWith("*") && pattern.endsWith("*")) {
                String middle = pattern.substring(1, pattern.length() - 1);
                return path.indexOf(middle) != -1;
            } else if (pattern.startsWith("*")) {
                return path.endsWith(pattern.substring(1));
            } else if (pattern.endsWith("*")) {
                return path.startsWith(pattern.substring(0, pattern.length() - 1));
            } else {
                return path.equals(pattern);
            }
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    private static class MimeTypeConfig {

        private List patternToMimeTypes = new ArrayList();

        public void define(String pattern, String mimeType) {
            patternToMimeTypes.add(new PatternToMimeType(pattern.toLowerCase(), mimeType.toLowerCase()));
        }

        public String getMimeType(String path) {
            path = path.toLowerCase();
            for (Iterator i = patternToMimeTypes.iterator(); i.hasNext(); ) {
                PatternToMimeType patternToMimeType = (PatternToMimeType) i.next();
                if (patternToMimeType.matches(path)) return patternToMimeType.getMimeType();
            }
            return null;
        }
    }
}
