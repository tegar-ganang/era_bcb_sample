package aurora.service.ws;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import uncertain.composite.CompositeLoader;
import uncertain.composite.CompositeMap;
import uncertain.composite.XMLOutputter;
import uncertain.exception.BuiltinExceptionFactory;
import uncertain.exception.ConfigurationFileException;
import uncertain.logging.LoggingContext;
import uncertain.ocm.IObjectRegistry;
import uncertain.proc.AbstractEntry;
import uncertain.proc.ProcedureRunner;

public class WebServiceInvoker extends AbstractEntry {

    IObjectRegistry mRegistry;

    String url;

    String inputPath;

    String returnPath;

    boolean raiseExceptionOnError = true;

    public static final String WS_INVOKER_ERROR_CODE = "aurora.service.ws.invoker_error";

    public WebServiceInvoker(IObjectRegistry registry) {
        this.mRegistry = registry;
    }

    @Override
    public void run(ProcedureRunner runner) throws Exception {
        if (url == null) {
            throw BuiltinExceptionFactory.createAttributeMissing(this, "url");
        }
        if (inputPath == null) {
            throw BuiltinExceptionFactory.createAttributeMissing(this, "inputPath");
        }
        CompositeMap context = runner.getContext();
        Object inputObject = context.getObject(inputPath);
        if (inputObject == null) throw BuiltinExceptionFactory.createDataFromXPathIsNull(this, inputPath);
        if (!(inputObject instanceof CompositeMap)) throw BuiltinExceptionFactory.createInstanceTypeWrongException(inputPath, CompositeMap.class, inputObject.getClass());
        URI uri = new URI(url);
        URL url = uri.toURL();
        PrintWriter out = null;
        BufferedReader br = null;
        CompositeMap soapBody = createSOAPBody();
        soapBody.addChild((CompositeMap) inputObject);
        String content = XMLOutputter.defaultInstance().toXML(soapBody.getRoot(), true);
        LoggingContext.getLogger(context, this.getClass().getCanonicalName()).config("request:\r\n" + content);
        HttpURLConnection httpUrlConnection = null;
        try {
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("SOAPAction", "urn:anonOutInOp");
            httpUrlConnection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            httpUrlConnection.connect();
            OutputStream os = httpUrlConnection.getOutputStream();
            out = new PrintWriter(os);
            out.println("<?xml version='1.0' encoding='UTF-8'?>");
            out.println(new String(content.getBytes("UTF-8")));
            out.flush();
            out.close();
            String soapResponse = null;
            CompositeMap soap = null;
            CompositeLoader cl = new CompositeLoader();
            if (HttpURLConnection.HTTP_OK == httpUrlConnection.getResponseCode()) {
                soap = cl.loadFromStream(httpUrlConnection.getInputStream());
                soapResponse = soap.toXML();
                LoggingContext.getLogger(context, this.getClass().getCanonicalName()).config("correct response:" + soapResponse);
            } else {
                soap = cl.loadFromStream(httpUrlConnection.getErrorStream());
                soapResponse = soap.toXML();
                LoggingContext.getLogger(context, this.getClass().getCanonicalName()).config("error response:" + soapResponse);
                if (raiseExceptionOnError) {
                    throw new ConfigurationFileException(WS_INVOKER_ERROR_CODE, new Object[] { url, soapResponse }, this);
                }
            }
            httpUrlConnection.disconnect();
            CompositeMap result = (CompositeMap) soap.getChild(SOAPServiceInterpreter.BODY.getLocalName()).getChilds().get(0);
            if (returnPath != null) runner.getContext().putObject(returnPath, result, true);
        } catch (Exception e) {
            LoggingContext.getLogger(context, this.getClass().getCanonicalName()).log(Level.SEVERE, "", e);
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                out.close();
            }
            if (br != null) {
                br.close();
            }
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    public String inputStream2String(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    public boolean isRaiseExceptionOnError() {
        return raiseExceptionOnError;
    }

    public void setRaiseExceptionOnError(boolean raiseExceptionOnError) {
        this.raiseExceptionOnError = raiseExceptionOnError;
    }

    public boolean getRaiseExceptionOnError() {
        return raiseExceptionOnError;
    }

    private CompositeMap createSOAPBody() {
        CompositeMap env = new CompositeMap(SOAPServiceInterpreter.ENVELOPE.getPrefix(), SOAPServiceInterpreter.ENVELOPE.getNameSpace(), SOAPServiceInterpreter.ENVELOPE.getLocalName());
        CompositeMap body = new CompositeMap(SOAPServiceInterpreter.BODY.getPrefix(), SOAPServiceInterpreter.BODY.getNameSpace(), SOAPServiceInterpreter.BODY.getLocalName());
        env.addChild(body);
        return body;
    }
}
