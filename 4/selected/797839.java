package org.apache.solr.request;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;

/**
 * Writes a ContentStream directly to the output.
 *
 * <p>
 * This writer is a special case that extends and alters the
 * QueryResponseWriter contract.  If SolrQueryResponse contains a
 * ContentStream added with the key {@link #CONTENT}
 * then this writer will output that stream exactly as is (with it's
 * Content-Type).  if no such ContentStream has been added, then a
 * "base" QueryResponseWriter will be used to write the response
 * according to the usual contract.  The name of the "base" writer can
 * be specified as an initialization param for this writer, or it
 * defaults to the "standard" writer.
 * </p>
 * 
 * @version $Id: RawResponseWriter.java 954340 2010-06-14 01:23:34Z hossman $
 * @since solr 1.3
 */
public class RawResponseWriter implements QueryResponseWriter {

    /** 
   * The key that should be used to add a ContentStream to the 
   * SolrQueryResponse if you intend to use this Writer.
   */
    public static final String CONTENT = "content";

    private String _baseWriter = null;

    public void init(NamedList n) {
        if (n != null) {
            Object base = n.get("base");
            if (base != null) {
                _baseWriter = base.toString();
            }
        }
    }

    protected QueryResponseWriter getBaseWriter(SolrQueryRequest request) {
        return request.getCore().getQueryResponseWriter(_baseWriter);
    }

    public String getContentType(SolrQueryRequest request, SolrQueryResponse response) {
        Object obj = response.getValues().get(CONTENT);
        if (obj != null && (obj instanceof ContentStream)) {
            return ((ContentStream) obj).getContentType();
        }
        return getBaseWriter(request).getContentType(request, response);
    }

    public void write(Writer writer, SolrQueryRequest request, SolrQueryResponse response) throws IOException {
        Object obj = response.getValues().get(CONTENT);
        if (obj != null && (obj instanceof ContentStream)) {
            ContentStream content = (ContentStream) obj;
            Reader reader = content.getReader();
            try {
                IOUtils.copy(reader, writer);
            } finally {
                reader.close();
            }
        } else {
            getBaseWriter(request).write(writer, request, response);
        }
    }
}
