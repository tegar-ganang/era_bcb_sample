package com.volantis.mps.servlet;

import com.volantis.mcs.context.MarinerContextException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.xml.sax.SAXException;

/**
 * A simple extension to the <code>MessageStoreServlet</code> to allow it
 * to be tested with creating an entire and valid MCS context.
 */
public class TestMSS extends MessageStoreServlet {

    protected void processXML(HttpServletRequest request, HttpServletResponse response, InputStream msgStream) throws IOException, MarinerContextException, SAXException {
        OutputStream os = response.getOutputStream();
        byte[] buffer = new byte[1028];
        int read = msgStream.read(buffer);
        while (read != -1) {
            os.write(buffer, 0, read);
            read = msgStream.read(buffer);
        }
        os.flush();
    }
}
