package ala.infosource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * Simple HTTP methods that wrap Commons HTTPClient API
 * 
 * @author Dave Martin
 */
public class WebUtils {

    public static InputStream getUrlContent(String url) throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod gm = new GetMethod(url);
        httpClient.executeMethod(gm);
        return gm.getResponseBodyAsStream();
    }

    public static String getFormSubmitResultAsString(String url, Map<String, String> parameterMap) throws Exception {
        String content = null;
        HttpClient httpClient = new HttpClient();
        PostMethod pm = new PostMethod(url);
        Set parameterNameSet = parameterMap.keySet();
        Iterator parameterNameIterator = parameterNameSet.iterator();
        while (parameterNameIterator.hasNext()) {
            String parameterName = (String) parameterNameIterator.next();
            String parameterValue = parameterMap.get(parameterName);
            pm.addParameter(parameterName, parameterValue);
        }
        httpClient.executeMethod(pm);
        content = pm.getResponseBodyAsString();
        return content;
    }

    public static String getUrlContentAsString(String url) throws Exception {
        HttpClient httpClient = new HttpClient();
        GetMethod gm = new GetMethod(url);
        gm.setFollowRedirects(true);
        httpClient.executeMethod(gm);
        String content = gm.getResponseBodyAsString();
        return content;
    }

    public static Response getUrlContentAsBytes(String url) throws Exception {
        return getUrlContentAsBytes(url, true);
    }

    public static Response getUrlContentAsBytes(String url, boolean followRedirect) throws Exception {
        Response response = new Response();
        HttpClientParams httpParams = new HttpClientParams();
        HttpClient httpClient = new HttpClient(httpParams);
        GetMethod gm = new GetMethod(url);
        gm.setFollowRedirects(followRedirect);
        httpClient.getParams().setParameter("http.protocol.allow-circular-redirects", true);
        httpClient.executeMethod(gm);
        response.setResponseUrl(gm.getURI().toString());
        InputStream input = gm.getResponseBodyAsStream();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] buff = new byte[1000];
        int read = 0;
        while ((read = input.read(buff)) > 0) {
            bOut.write(buff, 0, read);
        }
        Header hdr = gm.getResponseHeader("Content-Type");
        if (hdr != null) {
            response.setContentType(hdr.getValue());
        }
        response.setResponseAsBytes(bOut.toByteArray());
        return response;
    }
}
