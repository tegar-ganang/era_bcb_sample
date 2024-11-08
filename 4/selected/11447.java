package com.tms.webservices.applications.xtvd;

import com.tms.webservices.applications.DataDirectException;
import com.withay.http.DigestClient;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * This class builds a <code>SOAP request</code> that targets the
 * <code>TMS XTVD webservice</code> and fetches the resulting
 * <code>XML document</code>.
 *
 * <p>The following code sample shows ways of using this class:</p>
 *
 * <pre>
 *  String userName = "scott";
 *  String password = "tiger";
 *  SOAPRequest soapRequest = new SOAPRequest( userName, password );
 *
 *  int numberOfDays = 3;
 *  Calendar start = Calendar.getInstance();
 *  Calendar end = Calendar.getInstance();
 *  end.add( Calendar.DAY_OF_YEAR, numberOfDays );
 *  StringWriter writer = new StringWriter( 65536 );
 *  soapRequest.getData( start, end, writer );
 *  String data = writer.toString();
 *
 *  start.add( Calendar.DAY_OF_YEAR, numberOfDays );
 *  end.add( Calendar.DAY_OF_YEAR, numberOfDays * 2 );
 *  String fileName = "/tmp/xtvd.xml";
 *  soapRequest.getDataFile( start, end, fileName );
 *
 *  Xtvd xtvd = new Xtvd();
 *  soapRequest.getData( start, end, xtvd );
 * </pre>
 *
 * @since ddclient version 1.2
 * @author Rakesh Vidyadharan 2<sup><small>nd</small></sup> February, 2004
 *
 * <p>Copyright 2004, Tribune Media Services</p>
 *
 * $Id: SOAPRequest.java,v 1.1.1.1 2005/07/19 04:28:21 shawndr Exp $
 */
public class SOAPRequest {

    /**
   * The system specific <code>end of line</code> character.
   */
    public static final String END_OF_LINE = System.getProperty("line.separator");

    /**
   * The URI for the TMS XTVD web service.
   *
   * {@value}
   */
    public static final String WEBSERVICE_URI = "http://datadirect.webservices.zap2it.com/tvlistings/xtvdService";

    /**
   * The <code>XML header</code> to use to wrap the request.
   */
    public static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8'?>\n";

    /**
   * The <code>SOAP Envelope</code> to use to wrap the request.
   */
    public static final String SOAP_ENVELOPE = "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:SOAP-ENC='http://schemas.xmlsoap.org/soap/encoding/'>\n";

    /**
   * The <code>closing SOAP Envelope</code> to use to wrap the request.
   *
   * {@value}
   */
    public static final String CLOSE_SOAP_ENVELOPE = "</SOAP-ENV:Envelope>";

    /**
   * The <code>SOAP Body</code> to use to wrap the request.
   */
    public static final String SOAP_BODY = "<SOAP-ENV:Body>\n<tms:download xmlns:tms='urn:TMSWebServices'>\n";

    /**
   * The <code>closing SOAP Body</code> to use to wrap the request.
   */
    public static final String CLOSE_SOAP_BODY = "</tms:download>\n</SOAP-ENV:Body>\n";

    /**
   * The HTTP request method that is supported by the TMS XTVD web
   * service.
   *
   * {@value}
   */
    private static final String HTTP_METHOD = "POST";

    /**
   * The <code>userName</code> to use to authenticate the request
   * with the <code>webservice</code>.
   */
    private String userName;

    /**
   * The <code>password</code> to use along with the {@link #userName}
   * to authenticate the request with the <code>webservice</code>.
   */
    private transient String password;

    /**
   * The URI for the TMS XTVD web service.  If no URI is specified,
   * the URI defaults to the value of {@link #WEBSERVICE_URI}.
   */
    private URL webserviceURI;

    /**
   * The <code>HttpURLConnection</code> used to connect to the
   * <code>webservice</code>.
   */
    private HttpURLConnection httpConnection = null;

    /**
   * The formatter used to convert the date-time values to the ISO
   * format expected by the XTVD web service.  The formatter will
   * convert the <code>date-time</code> value to <code>UTC</code>
   * while converting to the string representation.
   */
    private SimpleDateFormat sdf;

    /**
   * A <code>Writer</code> that is used to write log messages to.
   * By default this is set to <code>System.err</code>.  Please use
   * {@link #setLog( Writer )} if you wish to specify another log
   * stream.
   */
    private Writer log;

    /**
   * Default constructor.  Initialises the {@link #webserviceURI} with
   * the value in {@link #WEBSERVICE_URI}, and the {@link #sdf}
   * class fields.
   *
   * <p><b>Note:</b> You must set the authentication credentials for
   * the class using the {@link #setUserName( String )} and {@link
   * #setPassword( String )} mutator methods if you use this form
   * of the constructor.</p>
   *
   * @throws DataDirectException - If a MalformedURLException is
   *   caught while creating a new URL.
   */
    public SOAPRequest() throws DataDirectException {
        super();
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        setWebserviceURI(SOAPRequest.WEBSERVICE_URI);
        log = new PrintWriter(System.err);
    }

    /**
   * Create a new instance of the class with the specified values for
   * {@link #userName} and {@link #password}.  Use this constructor
   * if the request is to be made to the default (production)
   * webservice.
   *
   * @param String userName - The userName to use for accessing the
   *   webservices.
   * @param String password - The password associated with the user.
   * @throws DataDirectException - If a MalformedURLException is
   *   caught while creating a new URL.
   */
    public SOAPRequest(String userName, String password) throws DataDirectException {
        this();
        setUserName(userName);
        setPassword(password);
    }

    /**
   * Create a new instance of the class with the specified values for
   * {@link #userName}, {@link #password} and {@link #webserviceURI}.
   *
   * @param String userName - The userName to use for accessing the
   *   webservices.
   * @param String password - The password associated with the user.
   * @param String uri - The <code>URI</code> for the webservice.
   * @throws DataDirectException - If a MalformedURLException is
   *   caught while creating a new URL with the uri parameter.
   */
    public SOAPRequest(String userName, String password, String uri) throws DataDirectException {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        log = new PrintWriter(System.err);
        setUserName(userName);
        setPassword(password);
        setWebserviceURI(uri);
    }

    /**
   * Connect to the webservice and download the XTVD document using the
   * specified parameters and write the document to the specified file.
   *
   * @see #getData( Calendar, Calendar, Writer )
   * @param Calendar start - The <code>date-time</code> from which
   *   data is requested.
   * @param Calendar end - The <code>date-time</code> till which
   *   data is requested.
   * @param String file - The fully qualified name of the file to which
   *   the XTVD document is to be written.
   * @throws DataDirectException - If errors are encountered while
   *   interacting with the web service or while writing to the
   *   specified file.
   */
    public void getDataFile(Calendar start, Calendar end, String file) throws DataDirectException {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            getData(start, end, writer);
            writer.flush();
            writer.close();
        } catch (IOException ioex) {
            throw new DataDirectException("Error while writing to file " + file + ".", ioex);
        }
    }

    /**
   * Connect to the webservice and download the <code>XTVD document</code>
   * using the specified parameters.  Parse the <code>XTVD response</code>
   * and write it to the specified <code>Writer</code>.
   *
   * @see #sendDownloadRequest( Calendar, Calendar )
   * @see #readSOAPResponse( Writer )
   * @param Calendar start - The <code>date-time</code> from which
   *   data is requested.
   * @param Calendar end - The <code>date-time</code> till which
   *   data is requested.
   * @param Writer writer - The <code>Writer</code> to which the
   *   XML data is to be written.
   * @throws DataDirectException - If errors are encountered while
   *   interacting with the web service.
   */
    public void getData(Calendar start, Calendar end, Writer writer) throws DataDirectException {
        try {
            log.write(sdf.format(new Date()));
            log.write("\tOpening connection to ");
            log.write(webserviceURI.toString());
            log.write(SOAPRequest.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        sendDownloadRequest(start, end);
        readSOAPResponse(writer);
        try {
            writer.flush();
        } catch (IOException ioex) {
            try {
                log.write(DataDirectException.getStackTraceString(ioex));
            } catch (IOException ioex1) {
                ioex1.printStackTrace();
            }
        }
        httpConnection.disconnect();
    }

    /**
   * Connect to the webservice and download the <code>XTVD document</code>
   * using the specified parameters.  Parse the XTVD document and
   * populate the instance variables in the specified {@link Xtvd}
   * object.
   *
   * @see #sendDownloadRequest( Calendar, Calendar )
   * @see #readSOAPResponse( Xtvd )
   * @param Calendar start - The <code>date-time</code> from which
   *   data is requested.
   * @param Calendar end - The <code>date-time</code> till which
   *   data is requested.
   * @param Xtvd xtvd - The <code>Xtvd</code> to which the
   *   XML data is to be written.
   * @throws DataDirectException - If errors are encountered while
   *   interacting with the web service.
   *
   * @since ddclient version 1.2
   */
    public void getData(Calendar start, Calendar end, Xtvd xtvd) throws DataDirectException {
        try {
            log.write(sdf.format(new Date()));
            log.write("\tOpening connection to ");
            log.write(webserviceURI.toString());
            log.write(SOAPRequest.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        sendDownloadRequest(start, end);
        readSOAPResponse(xtvd);
        httpConnection.disconnect();
    }

    /**
   * Connect to the webservice, fetch the <code>HTTP Digest</code>
   * headers, and send a <code>SOAP</code> request with the appropriate
   * authentication credentials.
   *
   * @see #sendSOAPRequest( Calendar, Calendar )
   * @param Calendar start - The <code>date-time</code> from which
   *   data is requested.
   * @param Calendar end - The <code>date-time</code> till which
   *   data is requested.
   * @throws DataDirectException - If errors are encountered while
   *   interacting with the web service.
   */
    private void sendDownloadRequest(Calendar start, Calendar end) throws DataDirectException {
        try {
            httpConnection = (HttpURLConnection) webserviceURI.openConnection();
            try {
                log.write(sdf.format(new Date()));
                log.write("\tSending first request for digest nonce");
                log.write(SOAPRequest.END_OF_LINE);
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
            httpConnection.setRequestMethod(HTTP_METHOD);
            httpConnection.connect();
            httpConnection.disconnect();
        } catch (IOException ioex) {
            throw new DataDirectException("Error connecting to webservice URI.", ioex);
        }
        DigestClient clientAuth = null;
        try {
            clientAuth = new DigestClient(httpConnection.getHeaderField("WWW-Authenticate"));
            try {
                log.write(sdf.format(new Date()));
                log.write("\tSetting username to ");
                log.write(userName);
                log.write(SOAPRequest.END_OF_LINE);
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
            clientAuth.setUsername(userName);
            try {
                log.write(sdf.format(new Date()));
                log.write("\tSetting password.");
                log.write(SOAPRequest.END_OF_LINE);
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
            clientAuth.setPassword(password);
        } catch (Throwable t) {
            throw new DataDirectException(t.getMessage(), t);
        }
        try {
            try {
                log.write(sdf.format(new Date()));
                log.write("\tSending authorisation response");
                log.write(SOAPRequest.END_OF_LINE);
                log.flush();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
            httpConnection = (HttpURLConnection) webserviceURI.openConnection();
            httpConnection.setRequestProperty("Authorization", clientAuth.getAuthorization(HTTP_METHOD, webserviceURI.getFile()));
            httpConnection.setRequestProperty("Accept-Encoding", "gzip");
            httpConnection.setRequestProperty("SOAPAction", "urn:TMSWebServices:xtvdWebService#download");
            httpConnection.setRequestMethod(HTTP_METHOD);
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setUseCaches(false);
            sendSOAPRequest(start, end);
        } catch (IOException ioex) {
            throw new DataDirectException("IO error while communicating with the webservice.", ioex);
        } catch (com.withay.http.BadAuthorizationException baex) {
            throw new DataDirectException("Authentication error.", baex);
        }
    }

    /**
   * Send the SOAP request using the values set in the class fields
   * <code>start</code> and <code>end</code> for the time interval.
   * This method invokes {@link #getDownloadRequest( Calendar,
   * Calendar)} to get the SOAP request envelope.
   *
   * @see #getDownloadRequest( Calendar, Calendar )
   * @param Calendar start - The <code>date-time</code> from which
   *   data is requested.
   * @param Calendar end - The <code>date-time</code> till which
   *   data is requested.
   * @throws DataDirectException - If errors are encountered while
   *   sending the request to the server.
   *
   * @since ddclient version 1.2
   */
    private void sendSOAPRequest(Calendar start, Calendar end) throws DataDirectException {
        String request = getDownloadRequest(start, end);
        try {
            PrintWriter writer = new PrintWriter(httpConnection.getOutputStream());
            writer.println(request);
            writer.flush();
            writer.close();
        } catch (IOException ioex) {
            throw new DataDirectException(ioex.getMessage(), ioex);
        }
        try {
            log.write(sdf.format(new Date()));
            log.write("\tSOAP Request");
            log.write(SOAPRequest.END_OF_LINE);
            log.write(request);
            log.write(SOAPRequest.END_OF_LINE);
            log.flush();
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }

    /**
   * Create the SOAP request envelope based upon the values in the
   * <code>start</code> and <code>end</code> calendar objects.
   *
   * @return String - The entire SOAP request envelope.
   */
    private String getDownloadRequest(Calendar start, Calendar end) {
        StringBuffer requestBuffer = new StringBuffer(128);
        requestBuffer.append(SOAPRequest.XML_HEADER);
        requestBuffer.append(SOAPRequest.SOAP_ENVELOPE);
        requestBuffer.append(SOAPRequest.SOAP_BODY);
        requestBuffer.append("<startTime xsi:type='tms:dateTime'>");
        requestBuffer.append(sdf.format(start.getTime()));
        requestBuffer.append("</startTime>\n");
        try {
            log.write(sdf.format(new Date()));
            log.write("\tstartTime ");
            log.write(sdf.format(start.getTime()));
            log.write(SOAPRequest.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        requestBuffer.append("<endTime xsi:type='tms:dateTime'>");
        requestBuffer.append(sdf.format(end.getTime()));
        requestBuffer.append("</endTime>\n");
        try {
            log.write(sdf.format(new Date()));
            log.write("\tendTime ");
            log.write(sdf.format(end.getTime()));
            log.write(SOAPRequest.END_OF_LINE);
            log.flush();
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        requestBuffer.append(SOAPRequest.CLOSE_SOAP_BODY);
        requestBuffer.append(SOAPRequest.CLOSE_SOAP_ENVELOPE);
        return requestBuffer.toString();
    }

    /**
   * Read the response to the <code>SOAP request</code> from the server.
   * Write the <code>XTVD XML data</code> to the specified writer
   * after stripping out the <code>SOAP envelope</code>.  If a
   * SOAPFault is caught, then the fault message will be written to
   * the {@link #log} writer, and a {@link
   * com.tms.webservices.applications.DataDirectException}
   * thrown to indicate the fault condition.
   *
   * @param Writer writer - The <code>Writer</code> to which the XML
   *   data is to be written.
   * @throws DataDirectException - If errors are encountered while
   *   reading or parsing the response from the server.
   */
    private void readSOAPResponse(Writer writer) throws DataDirectException {
        try {
            log.write(sdf.format(new Date()));
            log.write("\tReading response from server");
            log.write(SOAPRequest.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try {
                    BufferedReader reader = getReader();
                    Parser parser = ParserFactory.getWriterParser(reader, writer);
                    parser.setLog(log);
                    parser.parseXTVD();
                    reader.close();
                    log.write(sdf.format(new Date()));
                    log.write("\tFinished reading response from server");
                    log.write(SOAPRequest.END_OF_LINE);
                    log.flush();
                } catch (Throwable t) {
                    throw new DataDirectException(t.getMessage(), t);
                }
            } else {
                processError();
            }
        } catch (IOException ioex) {
            throw new DataDirectException(ioex.getMessage(), ioex);
        }
    }

    /**
   * Read the response to the <code>SOAP request</code> from the server.
   * Write the <code>XTVD XML data</code> to the specified {@link Xtvd}
   * class after stripping out the <code>SOAP envelope</code>.  If a
   * SOAPFault is caught, then the fault message will be written to
   * the {@link #log} writer, and a {@link
   * com.tms.webservices.applications.DataDirectException}
   * thrown to indicate the fault condition.
   *
   * @param Xtvd xtvd - The <code>Xtvd</code> to which the XML
   *   data is to be written.
   * @throws DataDirectException - If errors are encountered while
   *   reading or parsing the response from the server.
   *
   * @since ddclient version 1.2
   */
    private void readSOAPResponse(Xtvd xtvd) throws DataDirectException {
        try {
            log.write(sdf.format(new Date()));
            log.write("\tReading response from server");
            log.write(SOAPRequest.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try {
                    BufferedReader reader = getReader();
                    Parser parser = ParserFactory.getXtvdParser(reader, xtvd);
                    parser.setLog(log);
                    parser.parseXTVD();
                    reader.close();
                    log.write(sdf.format(new Date()));
                    log.write("\tFinished reading response from server");
                    log.write(SOAPRequest.END_OF_LINE);
                    log.flush();
                } catch (Throwable t) {
                    throw new DataDirectException(t.getMessage(), t);
                }
            } else {
                processError();
            }
        } catch (IOException ioex) {
            throw new DataDirectException(ioex.getMessage(), ioex);
        }
    }

    /**
   * Process the <code>HTTP error</code> returned by the <code>XTVD
   * webservice</code>
   *
   * @throws DataDirectException - An exception with information about
   *   the cause of the HTTP error.
   *
   * @since ddclient version 1.2
   */
    private void processError() throws DataDirectException {
        try {
            int responseCode = httpConnection.getResponseCode();
            BufferedReader reader = getReader();
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    log.write(line);
                    log.write(SOAPRequest.END_OF_LINE);
                    log.flush();
                }
                throw new DataDirectException("Authentication failed!");
            } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    log.write(line);
                    log.write(SOAPRequest.END_OF_LINE);
                    log.flush();
                }
                throw new DataDirectException("SOAP Fault caught!");
            } else {
                throw new DataDirectException("HTTP error (" + responseCode + ") encountered.");
            }
        } catch (Throwable t) {
            throw new DataDirectException(t);
        }
    }

    /**
   * Fetch the appropriate reader to use to read the response from
   * the server.  Check the <code>Content-encoding</code> header to
   * see if the response was compressed using <code>gzip</code> or
   * not, and build the appropriate reader.
   *
   * @since ddclient version 1.3.2
   * @return BufferdReader - The appropriate instance of
   *   <code>BufferedReader</code>.
   * @throws IOException If errors are encountered while fetching the
   *   reader from {@link #httpConnection}.
   */
    private BufferedReader getReader() throws IOException {
        BufferedReader reader = null;
        boolean gzipped = false;
        InputStream in = null;
        int responseCode = httpConnection.getResponseCode();
        String value = httpConnection.getHeaderField("Content-encoding");
        if (value == null) {
            value = httpConnection.getHeaderField("Content-Encoding");
        }
        if (value != null && value.equals("gzip")) {
            gzipped = true;
        }
        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            in = httpConnection.getInputStream();
        } else {
            in = httpConnection.getErrorStream();
        }
        if (gzipped) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(in), "UTF-8"));
        } else {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        }
        return reader;
    }

    /**
   * Returns {@link #userName}.
   *
   * @return String - The value/reference of/to userName.
   */
    public final String getUserName() {
        return userName;
    }

    /**
   * Set {@link #userName}.
   *
   * @param String userName - The value to set.
   */
    public final void setUserName(String userName) {
        this.userName = userName;
    }

    /**
  * Set {@link #password}.
  *
  * @param String password - The value to set.
  */
    public final void setPassword(String password) {
        this.password = password;
    }

    /**
   * Returns {@link #webserviceURI}.
   *
   * @return String - The string representation of {@link #webserviceURI}
   */
    public final String getWebserviceURI() {
        return webserviceURI.toString();
    }

    /**
   * Set {@link #webserviceURI}.
   *
   * @param String uri - The value to set.
   * @throws DataDirectException - If errors are encountered while
   *   creating a new URL instance.
   */
    public final void setWebserviceURI(String uri) throws DataDirectException {
        try {
            webserviceURI = new URL(uri);
        } catch (java.net.MalformedURLException mfuex) {
            throw new DataDirectException("Invalid URI " + uri + " specified.", mfuex);
        }
    }

    /**
   * Returns {@link #log}.
   *
   * @return Writer - The value/reference of/to log.
   */
    public final Writer getLog() {
        return log;
    }

    /**
   * Set {@link #log}.
   *
   * @param Writer log - The value to set.
   */
    public final void setLog(Writer log) {
        this.log = log;
    }
}
