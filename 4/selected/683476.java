package org.spark.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spark.util.io.MultiInputStreamWrapper;

public class HttpUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    public static final int DEFAULT_TIMEOUT = 3 * 60 * 1000;

    public static final String DEFAULT_USERAGENT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; SLCC1; .NET CLR 2.0.50727; .NET CLR 3.0.04506; InfoPath.1; .NET CLR 1.1.4322; CIBA)";

    public static String decodeURL(String url) {
        if (url == null) return null;
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return url;
        }
    }

    public static String encodeURL(String url) {
        if (url == null) return null;
        return URI.create(url).toASCIIString();
    }

    public static String getResponseAsString(String _url) throws IOException {
        return getResponseAsString(_url, null, null, null, null, DEFAULT_TIMEOUT);
    }

    public static String getResponseAsString(String _url, Map<String, String> _headers, Map<String, String> _params) throws IOException {
        return getResponseAsString(_url, null, _headers, _params, null, DEFAULT_TIMEOUT);
    }

    public static String getResponseAsString(String _url, Map<String, String> _headers, Object _stringOrStream) throws IOException {
        return getResponseAsString(_url, _stringOrStream, _headers, null, null, DEFAULT_TIMEOUT);
    }

    public static InputStream getResponseAsStream(String _url) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        getResponseAsStream(_url, ostream);
        return new ByteArrayInputStream(ostream.toByteArray());
    }

    public static void getResponseAsStream(String _url, OutputStream _stream) throws IOException {
        getResponseAsStream(_url, null, _stream, null, null, null, DEFAULT_TIMEOUT);
    }

    public static InputStream getResponseAsStream(String _url, Map<String, String> _headers, Map<String, String> _params) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        getResponseAsStream(_url, ostream, _headers, _params);
        return new ByteArrayInputStream(ostream.toByteArray());
    }

    public static void getResponseAsStream(String _url, OutputStream _stream, Map<String, String> _headers, Map<String, String> _params) throws IOException {
        getResponseAsStream(_url, null, _stream, _headers, _params, null, DEFAULT_TIMEOUT);
    }

    public static InputStream getResponseAsStream(String _url, Object _stringOrStream, Map<String, String> _headers) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        getResponseAsStream(_url, _stringOrStream, ostream, _headers);
        return new ByteArrayInputStream(ostream.toByteArray());
    }

    public static void getResponseAsStream(String _url, Object _stringOrStream, OutputStream _stream, Map<String, String> _headers) throws IOException {
        getResponseAsStream(_url, _stringOrStream, _stream, _headers, null, null, DEFAULT_TIMEOUT);
    }

    public static InputStream getResponseAsStream(String _url, Object _stringOrStream, Map<String, String> _headers, Map<String, String> _params, String _contentType, int _timeout) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        getResponseAsStream(_url, _stringOrStream, ostream, _headers, _params, _contentType, _timeout);
        return new ByteArrayInputStream(ostream.toByteArray());
    }

    public static void getResponseAsStream(String _url, Object _stringOrStream, OutputStream _stream, Map<String, String> _headers, Map<String, String> _params, String _contentType, int _timeout) throws IOException {
        if (_url == null || _url.length() <= 0) throw new IllegalArgumentException("Url can not be null.");
        String temp = _url.toLowerCase();
        if (!temp.startsWith("http://") && !temp.startsWith("https://")) _url = "http://" + _url;
        _url = encodeURL(_url);
        HttpMethod method = null;
        if (_stringOrStream == null && (_params == null || _params.size() <= 0)) method = new GetMethod(_url); else method = new PostMethod(_url);
        HttpMethodParams methodParams = ((HttpMethodBase) method).getParams();
        if (methodParams == null) {
            methodParams = new HttpMethodParams();
            ((HttpMethodBase) method).setParams(methodParams);
        }
        if (_timeout < 0) methodParams.setSoTimeout(0); else methodParams.setSoTimeout(_timeout);
        if (_contentType != null && _contentType.length() > 0) {
            if (_headers == null) _headers = new HashMap<String, String>();
            _headers.put("Content-Type", _contentType);
        }
        if (_headers == null || !_headers.containsKey("User-Agent")) {
            if (_headers == null) _headers = new HashMap<String, String>();
            _headers.put("User-Agent", DEFAULT_USERAGENT);
        }
        if (_headers != null) {
            Iterator<Map.Entry<String, String>> iter = _headers.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                method.setRequestHeader((String) entry.getKey(), (String) entry.getValue());
            }
        }
        if (method instanceof PostMethod && (_params != null && _params.size() > 0)) {
            Iterator<Map.Entry<String, String>> iter = _params.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                ((PostMethod) method).addParameter((String) entry.getKey(), (String) entry.getValue());
            }
        }
        if (method instanceof EntityEnclosingMethod && _stringOrStream != null) {
            if (_stringOrStream instanceof InputStream) {
                RequestEntity entity = new InputStreamRequestEntity((InputStream) _stringOrStream);
                ((EntityEnclosingMethod) method).setRequestEntity(entity);
            } else {
                RequestEntity entity = new StringRequestEntity(_stringOrStream.toString(), _contentType, null);
                ((EntityEnclosingMethod) method).setRequestEntity(entity);
            }
        }
        HttpClient httpClient = new HttpClient(new org.apache.commons.httpclient.SimpleHttpConnectionManager());
        httpClient.getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        InputStream instream = null;
        try {
            int status = httpClient.executeMethod(method);
            if (status != HttpStatus.SC_OK) {
                LOG.warn("Http Satus:" + status + ",Url:" + _url);
                if (status >= 500 && status < 600) throw new IOException("Remote service<" + _url + "> respose a error, status:" + status);
            }
            instream = method.getResponseBodyAsStream();
            IOUtils.copy(instream, _stream);
        } catch (IOException err) {
            LOG.error("Failed to access " + _url, err);
            throw err;
        } finally {
            IOUtils.closeQuietly(instream);
            if (method != null) method.releaseConnection();
        }
    }

    public static String getResponseAsString(String _url, Object _stringOrStream, Map<String, String> _headers, Map<String, String> _params, String _contentType, int _timeout) throws IOException {
        if (_url == null || _url.length() <= 0) throw new IllegalArgumentException("Url can not be null.");
        String temp = _url.toLowerCase();
        if (!temp.startsWith("http://") && !temp.startsWith("https://")) _url = "http://" + _url;
        _url = encodeURL(_url);
        HttpMethod method = null;
        if (_stringOrStream == null && (_params == null || _params.size() <= 0)) method = new GetMethod(_url); else method = new PostMethod(_url);
        HttpMethodParams methodParams = ((HttpMethodBase) method).getParams();
        if (methodParams == null) {
            methodParams = new HttpMethodParams();
            ((HttpMethodBase) method).setParams(methodParams);
        }
        if (_timeout < 0) methodParams.setSoTimeout(0); else methodParams.setSoTimeout(_timeout);
        if (_contentType != null && _contentType.length() > 0) {
            if (_headers == null) _headers = new HashMap<String, String>();
            _headers.put("Content-Type", _contentType);
        }
        if (_headers == null || !_headers.containsKey("User-Agent")) {
            if (_headers == null) _headers = new HashMap<String, String>();
            _headers.put("User-Agent", DEFAULT_USERAGENT);
        }
        if (_headers != null) {
            Iterator<Map.Entry<String, String>> iter = _headers.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                method.setRequestHeader((String) entry.getKey(), (String) entry.getValue());
            }
        }
        if (method instanceof PostMethod && (_params != null && _params.size() > 0)) {
            Iterator<Map.Entry<String, String>> iter = _params.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                ((PostMethod) method).addParameter((String) entry.getKey(), (String) entry.getValue());
            }
        }
        if (method instanceof EntityEnclosingMethod && _stringOrStream != null) {
            if (_stringOrStream instanceof InputStream) {
                RequestEntity entity = new InputStreamRequestEntity((InputStream) _stringOrStream);
                ((EntityEnclosingMethod) method).setRequestEntity(entity);
            } else {
                RequestEntity entity = new StringRequestEntity(_stringOrStream.toString(), _contentType, null);
                ((EntityEnclosingMethod) method).setRequestEntity(entity);
            }
        }
        if (method.getRequestHeader("Accept-Charset") == null) method.setRequestHeader("Accept-Charset", "GBK");
        HttpClient httpClient = new HttpClient(new org.apache.commons.httpclient.SimpleHttpConnectionManager());
        httpClient.getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        InputStream mergedStream = null;
        try {
            int status = httpClient.executeMethod(method);
            if (status != HttpStatus.SC_OK) {
                LOG.warn("Http Satus:" + status + ",Url:" + _url);
                if (status >= 500 && status < 600) throw new IOException(method.getResponseBodyAsString()); else return null;
            }
            String charset = getContentCharset(method.getResponseHeader("Content-Type"));
            if (charset != null) return method.getResponseBodyAsString(); else {
                LOG.info("Remote server does not return the charset, url:" + _url);
                InputStream instream = method.getResponseBodyAsStream();
                List<byte[]> bufferList = new ArrayList<byte[]>();
                List<Integer> bufferSizeList = new ArrayList<Integer>();
                boolean endFlag = false;
                String encoding = null;
                int bufferSize = 1024;
                while (true) {
                    byte[] buffer = new byte[bufferSize];
                    int readCount = instream.read(buffer);
                    if (readCount < 0) {
                        endFlag = true;
                        break;
                    }
                    bufferSizeList.add(readCount);
                    bufferList.add(buffer);
                    encoding = EncodingUtils.detectEncoding(buffer);
                    System.out.println("Auto detect as:" + encoding);
                    if (!encoding.equals("ASCII")) break;
                }
                InputStream[] instreams = null;
                if (endFlag) instreams = new InputStream[bufferList.size()]; else instreams = new InputStream[bufferList.size() + 1];
                int counter = 0;
                for (byte[] buffer : bufferList) {
                    instreams[counter] = new ByteArrayInputStream(buffer, 0, bufferSizeList.get(counter));
                    counter++;
                }
                if (!endFlag) instreams[counter] = instream;
                mergedStream = new MultiInputStreamWrapper(instreams);
                String respStr = IOUtils.toString(mergedStream, encoding);
                return respStr;
            }
        } catch (IOException err) {
            LOG.error("Failed to access " + _url, err);
            throw err;
        } finally {
            IOUtils.closeQuietly(mergedStream);
            if (method != null) method.releaseConnection();
        }
    }

    static String getContentCharset(Header header) {
        String charset = null;
        if (header != null) {
            HeaderElement values[] = header.getElements();
            if (values.length == 1) {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) charset = param.getValue();
            }
        }
        return charset;
    }

    static void printHeaders(Header[] headers) {
        if (headers == null) return;
        for (Header header : headers) System.out.println(header.toString());
    }
}
