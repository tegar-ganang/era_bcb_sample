package wsmg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import org.xmlpull.v1.builder.XmlBuilderException;
import org.xmlpull.v1.builder.XmlElement;
import wsmg.measure.profile.RunTimeStatistics;
import xsul.http_server.HttpServerException;
import xsul.http_server.HttpServerRequest;
import xsul.http_server.HttpServerResponse;
import xsul.processor.DynamicInfosetProcessorException;
import xsul.processor.soap_over_http.SoapHttpDynamicInfosetProcessor;
import xsul.soap.SoapUtil;
import xsul.soap11_util.Soap11Util;
import xsul.soap12_util.Soap12Util;

/**
 * @author Chathura Herath (cherath@cs.indiana.edu)
 */
public class MessengerSOAPServer extends SoapHttpDynamicInfosetProcessor {

    public MessengerSOAPServer(int port) {
        super();
        this.setServerPort(port);
        this.setSupportedSoapFragrances(new SoapUtil[] { Soap12Util.getInstance(), Soap11Util.getInstance() });
    }

    public void service(HttpServerRequest req, HttpServerResponse res) throws HttpServerException {
        String method = req.getMethod();
        final String responseEnc = "UTF-8";
        res.setContentType("text/html; charset=\"" + responseEnc + "\"");
        if ("GET".equalsIgnoreCase(method)) {
            Writer out = new PrintWriter(res.getOutputStream());
            try {
                out.write("<html><head><title>Delivery thread of WS-Messenger</title></head>");
                out.write("<body bgcolor='white'><h1>Delivery thread of WS-Messenger is running</h1>");
                out.write(RunTimeStatistics.getHtmlString());
                out.write("</body>");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("post".equalsIgnoreCase(method)) {
        }
    }

    private void doGet(String path, String query, HttpServerRequest req, HttpServerResponse res) {
        String method = req.getMethod();
        final String responseEnc = "UTF-8";
        res.setContentType("text/html; charset=\"" + responseEnc + "\"");
        if ("GET".equals(method)) {
            Writer out = new PrintWriter(res.getOutputStream());
            try {
                out.write("<form name=\"input\" action=\"shutdown\" method=\"get\">" + "Shutdown Messenger:" + "<input type=\"ShutDown\" value=\"Submit\">" + "</form>");
                out.write(RunTimeStatistics.getHtmlString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public XmlElement processMessage(XmlElement arg0) throws DynamicInfosetProcessorException {
        return null;
    }
}
