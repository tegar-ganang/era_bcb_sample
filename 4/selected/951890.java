package net.assimilator.examples.sca.web;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * Sends data using HTTP post to open connection.
 *
 * @version $Id: PostWriter.java 150 2007-05-22 03:32:09Z khartig $
 */
public class PostWriter implements Serializable {

    protected static final String BOUNDARY = "---------------------------7d159c1302d0y0";

    protected static final byte[] CRLF = { 0x0d, 0x0A };

    protected static int fudge = -20;

    protected static String encoding = "iso-8859-1";

    private static final long serialVersionUID = -8893190758977894461L;

    /**
     * Send POST data from Entry to the open connection.
     *
     * @param sampler    Description of parameter
     * @param connection Description of Parameter
     * @throws IOException Description of Exception
     */
    public void sendPostData(URLConnection connection, HTTPSampler sampler) throws IOException {
        String filename = sampler.getFilename();
        if ((filename != null) && (filename.trim().length() > 0)) {
            OutputStream out = connection.getOutputStream();
            writeln(out, "--" + BOUNDARY);
            Iterator args = sampler.getArguments().iterator();
            while (args.hasNext()) {
                Argument arg = (Argument) args.next();
                writeFormMultipartStyle(out, arg.getName(), (String) arg.getValue());
                writeln(out, "--" + BOUNDARY);
            }
            writeFileToURL(out, filename, sampler.getFileField(), getFileStream(filename), sampler.getMimetype());
            writeln(out, "--" + BOUNDARY + "--");
            out.flush();
            out.close();
        } else {
            String postData = sampler.getQueryString();
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print(postData);
            out.flush();
        }
    }

    /**
     * Sets the HTTP header values for the request
     *
     * @param connection parameter description
     * @param sampler    parameter description
     * @throws IOException description
     */
    public void setHeaders(URLConnection connection, HTTPSampler sampler) throws IOException {
        ((HttpURLConnection) connection).setRequestMethod("POST");
        String filename = sampler.getFileField();
        if ((filename != null) && (filename.trim().length() > 0)) {
            connection.setRequestProperty("Content-type", "multipart/form-data; boundary=" + BOUNDARY);
            connection.setDoOutput(true);
            connection.setDoInput(true);
        } else {
            String postData = sampler.getQueryString();
            connection.setRequestProperty("Content-length", "" + postData.length());
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
        }
    }

    private InputStream getFileStream(String filename) throws IOException {
        return new BufferedInputStream(new FileInputStream(filename));
    }

    /**
     * Writes out the contents of a file in correct multipart format.
     *
     * @param out       file output stream.
     * @param filename  filename.
     * @param fieldname filedname.
     * @param in        data input stream.
     * @param mimetype  file mimetype.
     * @throws IOException thrown on read/write errors
     */
    private void writeFileToURL(OutputStream out, String filename, String fieldname, InputStream in, String mimetype) throws IOException {
        writeln(out, "Content-Disposition: form-data; name=\"" + encode(fieldname) + "\"; filename=\"" + encode(filename) + "\"");
        writeln(out, "Content-Type: " + mimetype);
        out.write(CRLF);
        byte[] buf = new byte[1024 * 100];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
        out.write(CRLF);
        in.close();
    }

    /**
     * Writes form data in multipart format.
     *
     * @param out   output stream
     * @param name  form data name.
     * @param value data value.
     * @throws java.io.IOException on output error.
     */
    private void writeFormMultipartStyle(OutputStream out, String name, String value) throws IOException {
        writeln(out, "Content-Disposition: form-data; name=\"" + name + "\"");
        out.write(CRLF);
        writeln(out, value);
    }

    private String encode(String value) {
        StringBuffer newValue = new StringBuffer();
        char[] chars = value.toCharArray();
        for (char character : chars) {
            if (character == '\\') {
                newValue.append("\\\\");
            } else {
                newValue.append(character);
            }
        }
        return newValue.toString();
    }

    private void writeln(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(encoding));
        out.write(CRLF);
    }
}
