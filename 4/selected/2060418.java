package org.xebra.client.com;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xebra.client.events.DownloadEvent;
import org.xebra.client.events.EventDispatcher;
import org.xebra.client.util.ApplicationProperties;
import org.xebra.dcm.util.ErrorMessages;

/**
 * This is a service used to make HTTP POST requests to the HxDIV Server. All of
 * the methods of this service retrieve the HTTP responses as a
 * <code>Document</code> representing an XML file, which can then be parsed as
 * needed by other classes. None of these methods are thread-safe, and any
 * threaded requests to these methods should consider the server's response
 * time.
 * 
 * <p>
 * For an overview on the Hypertext Transfer Protocol (HTTP) please see the
 * documentation available on the <a target="_blank"
 * href="http://www.w3.org/Protocols/">World Wide Web Consortium's (W3C) web
 * site</a>.
 * </p>
 * 
 * <p>
 * Developers should note that this is an entirely static class and cannot
 * be instantiated. All methods are static.
 * </p>
 * 
 * @see org.w3c.dom.Document
 * 
 * @author Rafael Chargel
 * @version $Revision: 1.7 $
 */
public final class HTTPRequestService {

    private static XebraConnectionRegistry reg = XebraConnectionRegistry.getSingletonInstance();

    /**
	 * Cannot be instantiated.
	 */
    private HTTPRequestService() {
        super();
    }

    /**
	 * Gets the study data from the Xebra Server.
	 * 
	 * @return Returns the study data as a <code>Document</code> object.
	 * 
	 * @throws IOException
	 *             is thrown if there are any errors communicating with the
	 *             HxDIV server.
	 */
    public static Document fetchStudyData(String studyUid, URL codebase) throws IOException {
        ApplicationProperties ap = ApplicationProperties.getSingletonInstance();
        URL url = null;
        try {
            url = new URL(codebase, "studyRequest?studyUID=" + studyUid);
            if (url == null) {
                throw new IOException(ErrorMessages.URL_ERROR + "The URL is null");
            }
        } catch (MalformedURLException mExc) {
            IOException ioExc = new IOException(ErrorMessages.URL_ERROR + mExc.getMessage());
            ioExc.initCause(mExc);
            throw ioExc;
        }
        if (ap.isAnonymized()) {
            return fetchDocument(studyUid, url, new String[] { "studyUID", "anon" }, new String[] { studyUid, "true" }, 1, true);
        }
        return fetchDocument(studyUid, url, new String[] { "studyUID" }, new String[] { studyUid }, 1, true);
    }

    /**
	 * This method does the actual work of fetching the requested XML file via
	 * an HTTP POST.
	 * 
	 * @param studyUid
	 * 			  The UID of the associated study
	 * @param url
	 *            The URL pointing to the XML File requested.
	 * @param paramNames
	 *            The name of the parameter being sent via HTTP POST.
	 * @param paramValues
	 *            The valud of the parameter being sent via HTTP POST.
	 * @param attempt
	 *            The current attempt number.
	 * 
	 * @return Returns a <code>Document</code> representing the requested XML
	 *         file.
	 * 
	 * @throws IOException
	 *             is thrown if there are any errors communicating to the
	 *             server.
	 */
    private static Document fetchDocument(String studyUid, URL url, String[] paramNames, String[] paramValues, int attempt) throws IOException {
        return fetchDocument(studyUid, url, paramNames, paramValues, attempt, false);
    }

    /**
	 * This method does the actual work of fetching the requested XML file via
	 * an HTTP POST.
	 * 
	 * @param studyUid
	 * 			  The uid of the associated study
	 * @param url
	 *            The URL pointing to the XML File requested.
	 * @param paramNames
	 *            The name of the parameter being sent via HTTP POST.
	 * @param paramValues
	 *            The valud of the parameter being sent via HTTP POST.
	 * @param attempt
	 *            The current attempt number.
	 * @param isZipped
	 *            Determines whether the input stream will be compressed.
	 * 
	 * @return Returns a <code>Document</code> representing the requested XML
	 *         file.
	 * 
	 * @throws IOException
	 *             is thrown if there are any errors communicating to the
	 *             server.
	 */
    private static Document fetchDocument(String studyUid, URL url, String[] paramNames, String[] paramValues, int attempt, boolean isZipped) throws IOException {
        if (paramNames == null || paramNames.length == 0 || paramValues == null || paramValues.length == 0) {
            if (paramNames != null || paramValues != null) {
                throw new IOException(ErrorMessages.NULL_ARGUMENT_ERROR + "The parameter names and values cannot be null or empty");
            }
        }
        if (paramNames.length != paramValues.length) {
            throw new IOException(ErrorMessages.INVALID_ARGUMENT_ERROR + "The parameter names and values arrays must be the same length.");
        }
        ApplicationProperties ap = ApplicationProperties.getSingletonInstance();
        HttpURLConnection conn = null;
        BufferedWriter out = null;
        try {
            conn = (HttpURLConnection) reg.retrieve(studyUid).getURLConnection(url);
            if (conn == null) {
                throw new IOException(ErrorMessages.CONNECTION_ERROR + "The connection is null.");
            }
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        } catch (IOException ioExc) {
            if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exc) {
                }
                return fetchDocument(studyUid, url, paramNames, paramValues, ++attempt);
            }
            throw ioExc;
        }
        try {
            out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()), ap.getDefaultDataBuffer());
            if (out == null) {
                throw new IOException(ErrorMessages.IO_STREAM_ERROR + "output stream is null");
            }
        } catch (IOException ioExc) {
            if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exc) {
                }
                return fetchDocument(studyUid, url, paramNames, paramValues, ++attempt);
            }
            throw ioExc;
        }
        StringBuffer content = null;
        try {
            content = new StringBuffer();
            for (int i = 0; i < paramNames.length; i++) {
                content.append(paramNames[i]);
                content.append("=");
                content.append(URLEncoder.encode(paramValues[i], "UTF-8"));
                if (i < (paramNames.length - 1)) {
                    content.append("&");
                }
            }
        } catch (Throwable t) {
            if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exc) {
                }
                return fetchDocument(studyUid, url, paramNames, paramNames, ++attempt);
            }
            throw new IOException(t.getMessage());
        }
        try {
            if (content != null && content.length() > 0) {
                out.write(content.toString());
                out.flush();
            }
        } catch (IOException ioExc) {
            if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exc) {
                }
                return fetchDocument(studyUid, url, paramNames, paramNames, ++attempt);
            }
            throw ioExc;
        } finally {
            try {
                out.close();
            } catch (IOException ioExc) {
                if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException exc) {
                    }
                    return fetchDocument(studyUid, url, paramNames, paramNames, ++attempt);
                }
                throw ioExc;
            }
        }
        byte[] data = null;
        try {
            if (isZipped) {
                data = getZippedData(conn);
            } else {
                data = getData(conn);
            }
        } catch (IOException ioExc) {
            if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                return fetchDocument(studyUid, url, paramNames, paramValues, ++attempt);
            }
            throw ioExc;
        }
        Document doc = null;
        try {
            doc = buildDocument(new ByteArrayInputStream(data));
        } catch (IOException ioExc) {
            if (attempt < ap.getAllowableNumberOfReloadAttempts() || ap.getAllowableNumberOfReloadAttempts() < 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exc) {
                }
                return fetchDocument(studyUid, url, paramNames, paramValues, ++attempt);
            }
            throw ioExc;
        }
        return doc;
    }

    /**
	 * Reads the data from the input stream.
	 * 
	 * @param conn The URL Connection.
	 * 
	 * @return Returns a byte array of data.
	 * 
	 * @throws IOException Thrown if there are any errors reading from the stream.
	 */
    private static byte[] getZippedData(HttpURLConnection conn) throws IOException {
        int contentLength = conn.getContentLength();
        ApplicationProperties ap = ApplicationProperties.getSingletonInstance();
        CountingZipInputStream in = new CountingZipInputStream(new BufferedInputStream(conn.getInputStream(), ap.getDefaultDataBuffer()));
        if (in == null) {
            throw new IOException(ErrorMessages.IO_STREAM_ERROR + "(INPUT STREAM)");
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int read = -1;
        byte[] buffer = new byte[40960];
        while ((read = in.read(buffer)) != -1) {
            bout.write(buffer, 0, read);
            if (contentLength > 0) {
                EventDispatcher.getSingletonInstance().fireDownloadEvent(DownloadEvent.DOWNLOADING, (int) in.getBytesRead(), contentLength);
            }
        }
        in.close();
        return bout.toByteArray();
    }

    /**
	 * Reads the data from the input stream.
	 * 
	 * @param conn The URL Connection.
	 * 
	 * @return Returns a byte array of data.
	 * 
	 * @throws IOException Thrown if there are any errors reading from the stream.
	 */
    private static byte[] getData(HttpURLConnection conn) throws IOException {
        int contentLength = conn.getContentLength();
        ApplicationProperties ap = ApplicationProperties.getSingletonInstance();
        BufferedInputStream in = new BufferedInputStream(conn.getInputStream(), ap.getDefaultDataBuffer());
        if (in == null) {
            throw new IOException(ErrorMessages.IO_STREAM_ERROR + "(INPUT STREAM)");
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int total = 0;
        int read = -1;
        byte[] buffer = new byte[40960];
        while ((read = in.read(buffer)) != -1) {
            total += read;
            bout.write(buffer, 0, read);
            if (contentLength > 0) {
                EventDispatcher.getSingletonInstance().fireDownloadEvent(DownloadEvent.DOWNLOADING, total, contentLength);
            }
        }
        in.close();
        return bout.toByteArray();
    }

    /**
	 * Builds a <code>Document</code> object from a
	 * <code>BufferedInputStream</code>. <br />
	 * <b>Note:</b> this does not close the used input stream.
	 * 
	 * @param stream
	 *            The <code>InputStream</code> to build the
	 *            <code>Document</code> from.
	 * 
	 * @return Returns a <code>Document</code> representing an XML file. Never
	 *         returns null.
	 * 
	 * @exception IOException
	 *                is thrown if an error is encountered trying to build the
	 *                document from the input stream.
	 */
    private static Document buildDocument(InputStream stream) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            javax.xml.transform.Source src = new StreamSource(stream);
            Result result = new DOMResult(doc);
            trans.transform(src, result);
            if (doc != null) return doc;
            throw new Exception(ErrorMessages.PARSE_DOC_ERROR);
        } catch (Throwable t) {
            IOException exc = new IOException(ErrorMessages.BUILD_DOC_ERROR);
            exc.initCause(t);
            throw exc;
        }
    }

    private static class CountingZipInputStream extends GZIPInputStream {

        CountingZipInputStream(InputStream in) throws IOException {
            super(in);
        }

        public long getBytesRead() {
            return inf.getBytesRead();
        }
    }
}
