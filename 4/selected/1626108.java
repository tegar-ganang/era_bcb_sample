package com.vgkk.hula.jsp.controller;

import java.io.*;
import java.util.*;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;

/**
 * A class for use with multipart form data which can handle uploading a single file as well
 * the usual parameter name/value pairs. ??Cannot handle multiple selections from a
 * selection box - see standard initParamHash - should be easy enough to implement if
 * necessary
 * ?? Also does not handle <text area> input
 *
 */
public class MultiPartParser {

    public static final String HEADER_CONTENT_DISPOSITION = "Content-disposition";

    /**
	 * If the incoming request <code>request</code> is a MIME multi-part message,
	 * saves the <code>noFiles</code> first embedded <b>files</b> in temporary
	 * files. The message is read from <code>ServletRequest.getInputStream()</code>.
	 * <p>
	 * Are considered as parameters definitions, every body part of the MIME message
	 * which does not contain an attached file. The &quot;name&quot; of the
	 * body part is the key of the parameter and the body itself is its value.
	 * This is the way the parameters of a POST request are sent.
	 * <p>
	 * Body parts which don't even have a &quot;name&quot; are ignored. Attached
	 * files with empty filenames are ignored.
	 * <p>
	 * <p>
	 * Here is an example where we will get the pair <i>(&quot;field1&quot;/
	 * &quot;Joe Blow&quot;) as a parameter of every attached files.<br>
	 * <xmp>
	 * --------AaB03x
	 *	content-disposition: form-data; name="field1"
	 *
	 *    Joe Blow
	 * --------AaB03x
	 *   content-disposition: form-data; name="myfile"; filename="file1.txt"
	 *   Content-Type: text/plain
	 *
	 *    ... contents of file1.txt ...
	 * --------AaB03x--
	 * </xmp>
	 *
	 * @param request       a HTTP request which is a MIME multipart message
	 * @param parameters    a receptacle for posted parameters of the request
	 *
	 * @return  an array of {@link Attachment attachments}.
	 *
	 * @throws MessagingException  if the message is ill-formatted
	 *
	 */
    static List<Attachment> parseParametersAndAttachments(HttpServletRequest request, Map parameters) throws IOException, MessagingException {
        DataSource ds = new HttpRequestDataSource(request);
        MimeMultipart mmp = new MimeMultipart(ds);
        int handledBodyCount = mmp.getCount();
        List<Attachment> attachments = new ArrayList<Attachment>();
        for (int i = 0; i < handledBodyCount; i++) {
            BodyPart bodyPart = mmp.getBodyPart(i);
            String filename = bodyPart.getFileName();
            if ((filename != null) && (filename.length() > 0)) {
                File temporaryFile = File.createTempFile("svupload", null);
                String[] disposition = bodyPart.getHeader(HEADER_CONTENT_DISPOSITION);
                String name = null;
                if (disposition.length >= 1) {
                    name = getParameterValue(disposition[0], "name");
                    String temp = getParameterValue(disposition[0], "filename");
                    if (temp != null) {
                        filename = temp;
                    }
                }
                filename = new String(filename.getBytes("ISO-8859-1"), "UTF8");
                Attachment attachment = new Attachment(name, filename, temporaryFile);
                for (Enumeration e = bodyPart.getAllHeaders(); e.hasMoreElements(); ) {
                    Header header = (Header) e.nextElement();
                    String headerName = header.getName();
                    String headerValue = header.getValue();
                    attachment.addHeader(headerName, headerValue);
                }
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    int readBytes;
                    byte[] buffer;
                    is = bodyPart.getInputStream();
                    fos = new FileOutputStream(temporaryFile);
                    buffer = new byte[1024];
                    while ((readBytes = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, readBytes);
                    }
                    attachments.add(attachment);
                } catch (IOException ioe) {
                    if (fos != null) {
                        fos.close();
                        temporaryFile.delete();
                        fos = null;
                    }
                    throw ioe;
                } finally {
                    if (fos != null) {
                        fos.close();
                        fos = null;
                    }
                }
            } else if (filename == null) {
                final String NAME_STRING = " name=";
                for (Enumeration e = bodyPart.getAllHeaders(); e.hasMoreElements(); ) {
                    Header header = (Header) e.nextElement();
                    String headerValue = header.getValue();
                    int nameIndex = headerValue.toLowerCase().indexOf(NAME_STRING);
                    if (nameIndex != -1) {
                        int quoteIndex = headerValue.lastIndexOf('\"');
                        if (quoteIndex > nameIndex) {
                            String name = headerValue.substring(nameIndex + NAME_STRING.length() + 1, quoteIndex);
                            ByteArrayOutputStream value = new ByteArrayOutputStream();
                            InputStream inputStream = bodyPart.getInputStream();
                            try {
                                int b;
                                while ((b = inputStream.read()) != -1) {
                                    value.write(b);
                                }
                            } finally {
                                try {
                                    inputStream.close();
                                } catch (IOException ee) {
                                }
                            }
                            parameters.put(name, value.toString("UTF-8").trim());
                        } else {
                            throw new MessagingException("MIME multi-part message format error");
                        }
                    }
                }
            }
        }
        return attachments;
    }

    /**
	 * Returns the value of the parameter <code>parameter</code> declared in
	 * the header value <code>headerValue</code> (typically <code>Content-Type
	 * </code> value). If a parameter is just set (no associated value), this
	 * method returns an empty string.
	 * <p>
	 * An example of header value :<br>
	 * <xmp>
	 * form-data; name="somename"; filename="somefilename"
	 * </xmp>
	 *
	 * @return  <code>null</code> if parameter isn't a contained in <code>
	 *  headerValue</code>.
	 *
	 */
    static String getParameterValue(String headerValue, String parameter) throws MessagingException {
        String r_parameterValue = null;
        final char parameterSeparator = ';';
        final char parameterValueEnclosingChar = '\"';
        final char parameterNameValueSeparator = '=';
        int parameterStartIndex = 0;
        while (parameterStartIndex < headerValue.length()) {
            int parameterEndIndex;
            String parameterDeclaration;
            int valueStartIndex;
            String curParamName = null;
            String curParamValue = null;
            parameterEndIndex = headerValue.indexOf(parameterSeparator, parameterStartIndex);
            parameterEndIndex = (parameterEndIndex != -1) ? parameterEndIndex : headerValue.length();
            parameterDeclaration = headerValue.substring(parameterStartIndex, parameterEndIndex);
            valueStartIndex = parameterDeclaration.indexOf(parameterNameValueSeparator, 0);
            if (valueStartIndex == -1) {
                curParamName = parameterDeclaration.trim();
            } else {
                curParamName = parameterDeclaration.substring(0, valueStartIndex).trim();
                valueStartIndex = parameterDeclaration.indexOf(parameterValueEnclosingChar, valueStartIndex + 1) + 1;
                if (valueStartIndex != -1) {
                    int valueEndIndex = parameterDeclaration.indexOf(parameterValueEnclosingChar, valueStartIndex);
                    if (valueEndIndex != -1) {
                        curParamValue = parameterDeclaration.substring(valueStartIndex, valueEndIndex);
                    } else {
                        throw new MessagingException("header value lacks an enclosing quote for a parameter");
                    }
                } else {
                    throw new MessagingException("header value lacks an enclosing quote for a parameter");
                }
            }
            if (curParamName.length() == 0) {
                throw new MessagingException("empty parameter name");
            }
            if (parameter.equals(curParamName)) {
                r_parameterValue = curParamValue;
                break;
            }
            parameterStartIndex = parameterEndIndex + 1;
        }
        return r_parameterValue;
    }

    /**
	 * A simple <code>DataSource</code> on a <code>ServletRequest</code>.
	 */
    private static class HttpRequestDataSource implements DataSource {

        public static final String HEADER_CONTENT_TYPE = "Content-Type";

        private HttpServletRequest mRequest;

        public HttpRequestDataSource(HttpServletRequest request) {
            this.mRequest = request;
        }

        public InputStream getInputStream() throws IOException {
            return mRequest.getInputStream();
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("unwritable data source");
        }

        public String getContentType() {
            return mRequest.getHeader(HEADER_CONTENT_TYPE);
        }

        public String getName() {
            return mRequest.getServerName();
        }
    }
}
