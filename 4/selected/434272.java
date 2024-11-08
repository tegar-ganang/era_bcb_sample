package froxy.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import froxy.client.Helper;
import froxy.crypto.Decrypt;
import froxy.urlfetch.HttpContentCreator;

public class ProxyHandler extends AbstractHandler {

    private static ThreadLocal<Boolean> encript = new ThreadLocal<Boolean>();

    private static Log logger = LogFactory.getLog(ProxyHandler.class);

    public static boolean isEncript() {
        if (encript.get() == null) {
            encript.set(Helper.ENCRIPT_ENABLEED);
        }
        return encript.get();
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        encript.set(Helper.ENCRIPT_ENABLEED);
        if (request.getRequestURL().toString().equalsIgnoreCase("http://localhost:" + Helper.getFroxyPort() + "/pac") || request.getRequestURL().toString().equalsIgnoreCase(Helper.getPACAddress())) {
            createPACContent(response);
            baseRequest.setHandled(true);
            return;
        }
        boolean gziped = false;
        HttpContentCreator hcc = new HttpContentCreator();
        HttpResponse respons = hcc.getResponseDirect(request);
        HttpEntity myEntity = respons.getEntity();
        if (isEncript() && myEntity != null) {
            Header ceheader = myEntity.getContentEncoding();
            if (ceheader != null) {
                HeaderElement[] codecs = ceheader.getElements();
                for (int i = 0; i < codecs.length; i++) {
                    if (codecs[i].getName().equalsIgnoreCase(Helper.GZIP)) {
                        gziped = true;
                    }
                }
            }
        }
        response.setStatus(respons.getStatusLine().getStatusCode());
        Header[] header = respons.getAllHeaders();
        for (int i = 0; i < header.length; i++) {
            if (isEncript() && header[i].getName().equalsIgnoreCase(Helper.Content_Encoding) && gziped) {
                continue;
            }
            if (header[i].getName().equalsIgnoreCase(Helper.Content_Length)) {
                continue;
            }
            response.addHeader(header[i].getName(), header[i].getValue());
        }
        if (myEntity != null) {
            InputStream is = myEntity.getContent();
            if (encript.get()) {
                if (gziped) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    writeToOutputStream(bos, is, null);
                    response.getOutputStream().write(unGZip(bos.toByteArray()));
                    bos.close();
                } else {
                    Decrypt xor = createDecryptor();
                    writeToOutputStream(response.getOutputStream(), is, xor);
                }
            } else {
                writeToOutputStream(response.getOutputStream(), is, null);
            }
            is.close();
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }
        baseRequest.setHandled(true);
    }

    private void writeToOutputStream(OutputStream out, InputStream is, Decrypt xor) throws IOException {
        int readByte;
        byte[] buffer = new byte[Helper.BUFFER_SIZE];
        while ((readByte = is.read(buffer)) > 0) {
            if (xor != null) {
                buffer = xor.decrypt(buffer, readByte);
            }
            out.write(buffer, 0, readByte);
        }
    }

    private void createPACContent(HttpServletResponse response) throws IOException {
        response.getWriter().write(String.format(Helper.PAC_FUNCTION_HEADER, Helper.PAC_FUNCTION_DIRECT));
        String[] blockedWebsites = Helper.getProxyMask();
        if (blockedWebsites != null) {
            for (int i = 0; i < blockedWebsites.length; i++) {
                response.getWriter().write(String.format(Helper.PAC_FUNCTION_CONTENT_PROXY, new Object[] { "http://*" + blockedWebsites[i] + "*", Helper.getLocalIP() + ":" + Helper.getFroxyPort() }));
            }
        }
        String[] directAccessSites = Helper.getDirectAccess();
        if (directAccessSites != null) {
            for (int i = 0; i < directAccessSites.length; i++) {
                response.getWriter().write(String.format(Helper.PAC_FUNCTION_CONTENT_DIRECT, new Object[] { "http://*" + directAccessSites[i] + "*" }));
            }
        }
        response.getWriter().write(String.format(Helper.PAC_FUNCTION_FOOTER, Helper.getDefaultProxy()));
        response.getWriter().flush();
        response.getWriter().close();
    }

    private byte[] unGZip(byte[] data) throws IOException {
        byte[] result = null;
        Decrypt xor = createDecryptor();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        GZIPInputStream gzip = new GZIPInputStream(bis);
        byte[] buf = new byte[Helper.BUFFER_SIZE];
        int num = -1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((num = gzip.read(buf, 0, buf.length)) > 0) {
            if (xor != null) {
                buf = xor.decrypt(buf, num);
            }
            baos.write(buf, 0, num);
        }
        result = baos.toByteArray();
        baos.flush();
        baos.close();
        gzip.close();
        bis.close();
        return result;
    }

    private Decrypt createDecryptor() {
        if (Helper.getDecryptClass() != null) {
            Decrypt xor = null;
            try {
                xor = (Decrypt) Class.forName(Helper.getDecryptClass()).newInstance();
            } catch (InstantiationException e) {
                logger.error(e.getMessage(), e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(), e);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage(), e);
                e.printStackTrace();
            }
            return xor;
        } else {
            return null;
        }
    }
}
