package org.crypthing.things.crlmanager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.crypthing.things.config.Bundle;

/**
 * CRL get facility
 * @author yorickflannagan
 * @version 2.0
 * 
 */
public abstract class CRLGetter implements Serializable {

    private static final long serialVersionUID = -5015811478654030882L;

    private Object params;

    /**
   * Sets CRLGetter parameters.
   * @param params - any parameter that instances need
   */
    public void setParameters(Object params) {
        this.params = params;
    }

    /**
   * Gets CRLGetter parameters
   * @return - current parameters.
   */
    public Object getParameters() {
        return params;
    }

    /**
   * Gets a CRL from its CDP.
   * @param cdp - the CDP URL.
   * @return - the CRL itself.
   * @throws CRLHttpException - if an error occurrs.
   */
    public X509CRL getCRL(URL cdp) {
        byte[] buffer = receive(cdp);
        return generateCRL(new ByteArrayInputStream(buffer));
    }

    /**
   * Creates a custom context to Http connection. 
   * @return - the Apache Commons HttpClient
   */
    protected abstract HttpClient getClientConnection();

    private static final int MAX_READ = 10 * 1024;

    private byte[] receive(URL cdp) {
        HttpClient client = getClientConnection();
        byte[] retVal = null;
        HttpMethodBase method = new GetMethod(cdp.toExternalForm());
        try {
            client.executeMethod(method);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            InputStream is;
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                is = method.getResponseBodyAsStream();
                int lido;
                byte[] lcrBuffer = new byte[MAX_READ];
                while ((lido = is.read(lcrBuffer)) > 0) bos.write(lcrBuffer, 0, lido);
                retVal = bos.toByteArray();
            } else throw new CRLHttpException(Bundle.getInstance().getResourceString(this, "CRL_HTTP_RETURN_ERROR").replace("[HTTP_CODE]", String.valueOf(method.getStatusCode())).replace("[URL]", cdp.toExternalForm()));
        } catch (HttpException e) {
            throw new CRLHttpException(Bundle.getInstance().getResourceString(this, "CRL_HTTP_ERROR").replace("[URL]", cdp.toExternalForm()));
        } catch (IOException e) {
            throw new CRLHttpException(Bundle.getInstance().getResourceString(this, "CRL_HTTP_ERROR").replace("[URL]", cdp.toExternalForm()));
        }
        return retVal;
    }

    private X509CRL generateCRL(InputStream stream) {
        X509CRL crl = null;
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
            try {
                crl = (X509CRL) cf.generateCRL(stream);
            } catch (CRLException e) {
                throw new CRLHttpException(Bundle.getInstance().getResourceString(this, "CRL_MALFORMED_CRL_ERROR"), e);
            } finally {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        } catch (CertificateException e) {
            throw new CRLHttpException(Bundle.getInstance().getResourceString(this, "CRL_MALFORMED_CRL_ERROR"), e);
        }
        return crl;
    }
}
