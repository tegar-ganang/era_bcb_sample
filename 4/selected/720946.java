package com.microfly.job.html;

import java.io.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * HtmlFetcher
 * ��������ָ����HTML���ض�λ��
 * 
 * Description: a new publishing system
 * Copyright (c) 2007
 *
 * @author jialin
 * @version 1.0
 */
public class HtmlFetcher {

    private HttpClient client = new HttpClient();

    public HtmlFetcher() {
    }

    public void Login(String url, NameValuePair[] fields) throws IOException {
        PostMethod method = new PostMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        method.setRequestHeader("Referer", url);
        method.setRequestHeader("ContentType", "application/x-www-form-urlencoded");
        method.addParameters(fields);
        int statusCode = client.executeMethod(method);
        if (statusCode == HttpStatus.SC_OK) {
        }
    }

    public Reader GetHtml(String url) throws IOException {
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        int statusCode = client.executeMethod(method);
        if (statusCode == HttpStatus.SC_OK) {
            InputStream is = method.getResponseBodyAsStream();
            return new InputStreamReader(is, method.getResponseCharSet());
        }
        return null;
    }

    public void GetHtml(String url, File output) throws IOException {
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        int statusCode = client.executeMethod(method);
        if (statusCode == HttpStatus.SC_OK) {
            String encode = method.getResponseCharSet();
            InputStream is = method.getResponseBodyAsStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, encode));
            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(new FileOutputStream(output), encode);
                String line = null;
                while ((line = br.readLine()) != null) out.write(line);
            } finally {
                if (out != null) try {
                    out.close();
                } catch (Exception e1) {
                }
                if (br != null) try {
                    br.close();
                } catch (Exception e1) {
                }
            }
        }
    }

    public void GetBinary(String url, File output) throws IOException {
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        int statusCode = client.executeMethod(method);
        if (statusCode == HttpStatus.SC_OK) {
            InputStream is = method.getResponseBodyAsStream();
            OutputStream out = null;
            try {
                out = new FileOutputStream(output);
                int b;
                while ((b = is.read()) != -1) {
                    out.write(b);
                }
            } finally {
                if (out != null) try {
                    out.close();
                } catch (Exception e1) {
                }
                if (is != null) try {
                    is.close();
                } catch (Exception e1) {
                }
            }
        }
    }
}
