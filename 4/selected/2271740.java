package net.sf.xtvdclient.xtvd;

import net.sf.xtvdclient.xtvd.datatypes.Xtvd;
import net.sf.xtvdclient.xtvd.parser.Parser;
import net.sf.xtvdclient.xtvd.parser.ParserFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * This class builds a SOAP request that targets the
 * TMS XTVD webservice and fetches the resulting
 * XML document.
 *
 * The following code sample shows ways of using this class:
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
 * @author Rakesh Vidyadharan 2<sup><small>nd</small></sup> February, 2004
 *
 *         Copyright 2004, Tribune Media Services
 *
 *         $Id: SOAPRequest.java,v 1.7 2004/03/19 22:02:47 rakesh Exp $
 * @since ddclient version 1.2
 */
public class SOAPRequest {

    /**
   * The system specific end of line character.
   */
    public static final String END_OF_LINE = System.getProperty("line.separator");

    /**
   * The URI for the TMS XTVD web service.
   *
   * {@value}
   */
    public static final String WEBSERVICE_URI = "http://datadirect.webservices.zap2it.com/tvlistings/xtvdService";

    /**
   * The XML header to use to wrap the request.
   */
    public static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8'?>\n";

    /**
   * The SOAP Envelope to use to wrap the request.
   */
    public static final String SOAP_ENVELOPE = "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:SOAP-ENC='http://schemas.xmlsoap.org/soap/encoding/'>\n";

    /**
   * The closing SOAP Envelope to use to wrap the request.
   *
   * {@value}
   */
    public static final String CLOSE_SOAP_ENVELOPE = "</SOAP-ENV:Envelope>";

    /**
   * The SOAP Body to use to wrap the request.
   */
    public static final String SOAP_BODY = "<SOAP-ENV:Body>\n<tms:download xmlns:tms='urn:TMSWebServices'>\n";

    /**
   * The closing SOAP Body to use to wrap the request.
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
   * The userName to use to authenticate the request
   * with the webservice.
   */
    private String userName;

    /**
   * The password to use along with the {@link #userName}
   * to authenticate the request with the webservice.
   */
    private transient String password;

    /**
   * The URI for the TMS XTVD web service.  If no URI is specified,
   * the URI defaults to the value of {@link #WEBSERVICE_URI}.
   */
    private URL webserviceURI;

    /**
   * The HttpURLConnection used to connect to the
   * webservice.
   */
    private HttpURLConnection httpConnection = null;

    /**
   * The formatter used to convert the date-time values to the ISO
   * format expected by the XTVD web service.  The formatter will
   * convert the date-time value to UTC
   * while converting to the string representation.
   */
    private SimpleDateFormat sdf;

    /**
   * A Writer that is used to write log messages to.
   * By default this is set to System.err.  Please use
   * {@link #setLog(Writer)} if you wish to specify another log
   * stream.
   */
    private Writer log;

    /**
   * Default constructor.  Initialises the {@link #webserviceURI} with
   * the value in {@link #WEBSERVICE_URI}, and the {@link #sdf}
   * class fields.
   *
   * <b>Note:</b> You must set the authentication credentials for
   * the class using the {@link #setUserName(String)} and {@link
   * #setPassword(String)} mutator methods if you use this form
   * of the constructor.
   *
   * @throws DataDirectException If a MalformedURLException is
   *                             caught while creating a new URL.
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
   * @param userName The userName to use for accessing the webservices.
   * @param password The password associated with the user.
   * @throws DataDirectException If a MalformedURLException is
   *                             caught while creating a new URL.
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
   * @param userName The userName to use for accessing the webservices.
   * @param password The password associated with the user.
   * @param uri The URI for the webservice.
   * @throws DataDirectException If a MalformedURLException is
   *                             caught while creating a new URL with the uri parameter.
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
   * @param start The date-time from which data is requested.
   * @param end The date-time till which data is requested.
   * @param file The fully qualified name of the file to which
   *             the XTVD document is to be written.
   * @throws DataDirectException If errors are encountered while
   *                             interacting with the web service or while writing to the
   *                             specified file.
   * @see #getData(Calendar, Calendar, Writer)
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
   * Connect to the webservice and download the XTVD document
   * using the specified parameters.  Parse the XTVD response
   * and write it to the specified Writer.
   *
   * @param start The date-time from which data is requested.
   * @param end The date-time till which  data is requested.
   * @param writer The Writer to which the XML data is to be written.
   *
   * @throws DataDirectException If errors are encountered while
   *                             interacting with the web service.
   * @see #sendDownloadRequest(Calendar, Calendar)
   * @see #readSOAPResponse(Writer)
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
   * Connect to the webservice and download the XTVD document
   * using the specified parameters.  Parse the XTVD document and
   * populate the instance variables in the specified {@link Xtvd}
   * object.
   *
   * @param start The date-time from which data is requested.
   * @param end The date-time till which data is requested.
   * @param xtvd The Xtvd to which the XML data is to be written.
   * @throws DataDirectException If errors are encountered while interacting with the web service.
   * @see #sendDownloadRequest(Calendar, Calendar)
   * @see #readSOAPResponse(Xtvd)
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
   * Connect to the webservice, fetch the HTTP Digest
   * headers, and send a SOAP request with the appropriate
   * authentication credentials.
   *
   * @param start The date-time from which data is requested.
   * @param end The date-time till which data is requested.
   * @throws DataDirectException If errors are encountered while
   *                             interacting with the web service.
   * @see #sendSOAPRequest(Calendar, Calendar)
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
        Authenticator.setDefault(new XtvdAuthenticator(userName, password));
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
            httpConnection.setRequestProperty("Accept-Encoding", "gzip");
            httpConnection.setRequestProperty("SOAPAction", "urn:TMSWebServices:xtvdWebService#download");
            httpConnection.setRequestMethod(HTTP_METHOD);
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setUseCaches(false);
            sendSOAPRequest(start, end);
        } catch (IOException ioex) {
            throw new DataDirectException("IO error while communicating with the webservice.", ioex);
        }
    }

    /**
   * Send the SOAP request using the values set in the class fields
   * start and end for the time interval.
   * This method invokes {@link #getDownloadRequest(Calendar,
   * Calendar)} to get the SOAP request envelope.
   *
   * @param start The date-time from which data is requested.
   * @param end The date-time till which data is requested.
   * @throws DataDirectException If errors are encountered while
   *                             sending the request to the server.
   * @see #getDownloadRequest(Calendar, Calendar)
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
   * start and end calendar objects.
   *
   * @param start Start on this date
   * @param end End on this date
   *
   * @return The entire SOAP request envelope.
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
   * Read the response to the SOAP request from the server.
   * Write the XTVD XML data to the specified writer
   * after stripping out the SOAP envelope.  If a
   * SOAPFault is caught, then the fault message will be written to
   * the {@link #log} writer, and a {@link DataDirectException}
   * thrown to indicate the fault condition.
   *
   * @param writer The Writer to which the XML data is to be written.
   * @throws DataDirectException If errors are encountered while
   *                             reading or parsing the response from the server.
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
   * Read the response to the SOAP request from the server.
   * Write the XTVD XML data to the specified {@link Xtvd}
   * class after stripping out the SOAP envelope.  If a
   * SOAPFault is caught, then the fault message will be written to
   * the {@link #log} writer, and a {@link DataDirectException}
   * thrown to indicate the fault condition.
   *
   * @param xtvd The Xtvd to which the XML data is to be written.
   * @throws DataDirectException If errors are encountered while
   *                             reading or parsing the response from the server.
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
   * Process the HTTP error returned by the XTVD
   * webservice
   *
   * @throws DataDirectException An exception with information about
   *                             the cause of the HTTP error.
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
   * the server.  Check the Content-encoding header to
   * see if the response was compressed using gzip or
   * not, and build the appropriate reader.
   *
   * @return The appropriate instance of BufferedReader.
   * @throws IOException If errors are encountered while fetching the
   *                     reader from {@link #httpConnection}.
   * @since ddclient version 1.3.2
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
   * @return The value/reference of/to userName.
   */
    public final String getUserName() {
        return userName;
    }

    /**
   * Set {@link #userName}.
   *
   * @param userName The value to set.
   */
    public final void setUserName(String userName) {
        this.userName = userName;
    }

    /**
   * Set {@link #password}.
   *
   * @param password The value to set.
   */
    public final void setPassword(String password) {
        this.password = password;
    }

    /**
   * Returns {@link #webserviceURI}.
   *
   * @return The string representation of {@link #webserviceURI}
   */
    public final String getWebserviceURI() {
        return webserviceURI.toString();
    }

    /**
   * Set {@link #webserviceURI}.
   *
   * @param uri The value to set.
   * @throws DataDirectException If errors are encountered while
   *                             creating a new URL instance.
   */
    public final void setWebserviceURI(String uri) throws DataDirectException {
        try {
            webserviceURI = new URL(uri);
        } catch (MalformedURLException mfuex) {
            throw new DataDirectException("Invalid URI " + uri + " specified.", mfuex);
        }
    }

    /**
   * Returns {@link #log}.
   *
   * @return The value/reference of/to log.
   */
    public final Writer getLog() {
        return log;
    }

    /**
   * Set {@link #log}.
   *
   * @param log The value to set.
   */
    public final void setLog(Writer log) {
        this.log = log;
    }
}
