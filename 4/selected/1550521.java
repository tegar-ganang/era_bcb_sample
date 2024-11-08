package com.ununbium.plugin.webUser;

import com.ununbium.LoadGen.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.cookie.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.*;
import org.apache.log4j.Logger;

public class Script extends ScriptTemplate {

    protected Logger logger = Logger.getLogger(Script.class);

    protected static final int OPTIONS = 0;

    protected static final int GET = 1;

    protected static final int HEAD = 2;

    protected static final int POST = 3;

    protected static final int MULTIPARTPOST = 4;

    protected static final int PUT = 5;

    protected static final int DELETE = 6;

    protected static final int TRACE = 7;

    protected Hashtable webCache = null;

    protected Hashtable webPageCache = null;

    protected HttpState httpState = new HttpState();

    protected GetMethod getMethod;

    protected PostMethod postMethod;

    protected int userTimeout = 120000;

    protected int socketTimeout = 180000;

    protected int connectTimeout = 30000;

    protected SimpleHttpConnectionManager httpConMan = null;

    protected HttpClient httpClient = null;

    protected boolean followRedirects = false;

    protected HTMLEditorKit.Parser parser = new ParserGetter().getParser();

    protected HTMLParser parserCallback = new HTMLParser();

    public void initHTTPClient() {
        httpConMan = new SimpleHttpConnectionManager();
        httpClient = new HttpClient(httpConMan);
        if (proxy != null) {
            httpClient.getHostConfiguration().setProxy(proxy.host, proxy.port);
            if (!proxy.user.equals("")) httpClient.getState().setProxyCredentials(proxy.getAuthScope(), proxy.getUsernamePasswordCredentials());
        }
    }

    public Script() {
        super();
        Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
    }

    protected void setFollowRedirects(boolean follow) {
        logMessage("Follow Page Redirects set to: " + follow);
        followRedirects = follow;
    }

    protected void setUserTimeout(int mil) {
        logMessage("Default User Timeout changed from " + (userTimeout / 1000) + " to " + (mil / 1000) + " seconds");
        userTimeout = mil;
    }

    protected void setTimeout(int mil) {
        logMessage("Default Socket Timeout changed from " + (socketTimeout / 1000) + " to " + (mil / 1000) + " seconds");
        socketTimeout = mil;
    }

    protected void setConnectionTimeout(int mil) {
        logMessage("Default Connection Timeout changed from " + (connectTimeout / 1000) + " to " + (mil / 1000) + " seconds");
        connectTimeout = mil;
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(connectTimeout);
    }

    @Override
    public void run() {
        try {
            if (sleepBeforeStart > 0) {
                try {
                    logger.info("Sleep for " + sleepBeforeStart + " seconds, before running");
                    Thread.sleep(sleepBeforeStart * 1000);
                } catch (Exception e) {
                }
            }
            iteration = 0;
            testHarness.updateCurUsers(1);
            while (run) {
                initHTTPClient();
                iteration++;
                if (!test) {
                    logStr = new String("test=" + testid + " script=" + scriptName + " user=" + (id + 1) + " iter=" + iteration + ": ");
                }
                userIterationStatus = new ScriptStatus(scriptName, "_RunTime_", testid, id + 1, iteration);
                userIterationStatus.logs = new Vector<String>();
                userIterationStatus.requests = new Vector<Request>();
                logMessage(userIterationStatus, "Start iteration");
                script();
                stopTransaction(userIterationStatus);
                if (test) {
                    run = false;
                }
            }
            testHarness.updateCurUsers(-1);
        } catch (Exception e) {
            errorMessage(userIterationStatus, "Exception: " + e, e);
            try {
                stopTransaction(userIterationStatus);
            } catch (Exception ex) {
            }
            run = false;
        }
    }

    @Override
    public void script() throws InterruptedException {
        startTransaction("Default Place Holder Transaction");
        errorMessage("Please overload the script() class function in your script!; the prototype for this function is detailed at http://www.webperformancegroup.com/scriptingAPI/");
        stopTransaction();
    }

    private class TimeoutTimer extends TimerTask {

        private HttpMethod method = null;

        private int timeout = 0;

        public void setTimeout(int i) {
            timeout = i;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setMethod(Object i) {
            this.method = (HttpMethod) i;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public void run() {
            errorMessage("Fetch URL Timed Out Due to user timeout of " + (int) timeout / 1000 + " seconds being reached");
            method.abort();
            method = null;
        }
    }

    /**
	* function to handle calls without post params or multipart post params
	*/
    protected void fetchURL(String target, int method, String contentType, String referer) {
        fetchURL(target, method, contentType, referer, null, null);
    }

    /**
	* function to handle calls with post params but not multipart post params
	*/
    protected void fetchURL(String target, int method, String contentType, String referer, NameValuePair[] pParms) {
        fetchURL(target, method, contentType, referer, pParms, null);
    }

    /**
	* function to handle calls without post params but containing multipart post params
	*/
    protected void fetchURL(String target, int method, String contentType, String referer, Part[] pParms) {
        fetchURL(target, method, contentType, referer, null, pParms);
    }

    protected void fetchURL(String target, int method, String contentType, String referer, NameValuePair[] postParams, Part[] multiPostParams) {
        webPageCache = new Hashtable();
        TimeoutTimer timeoutTimer = new TimeoutTimer();
        Timer timer = new Timer();
        boolean multipart = false;
        switch(method) {
            case OPTIONS:
            case HEAD:
            case PUT:
            case DELETE:
            case TRACE:
                errorMessage("HTTP Method unsupported at this time");
                return;
            case GET:
                try {
                    getMethod = new GetMethod(target);
                    getMethod.getParams().setSoTimeout(socketTimeout);
                    getMethod.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
                    getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
                    getMethod.setFollowRedirects(followRedirects);
                    getMethod.setRequestHeader("Referer", referer);
                    getMethod.setRequestHeader("User-Agent", "WPG Load Generator user: " + (id + 1) + " script: " + scriptName);
                    timeoutTimer.setMethod(getMethod);
                    timeoutTimer.setTimeout(userTimeout);
                    timer.schedule(timeoutTimer, userTimeout);
                    this._responseCode = httpClient.executeMethod(getMethod);
                    if (this._responseCode > 399) {
                        errorMessage("GET URL: " + target + " Failed: " + getMethod.getStatusLine());
                    } else if (this._responseCode > 299) {
                        logMessage("GET URL: " + target + " Warning Get Succeeded with: " + getMethod.getStatusLine());
                    }
                    if (contentType.startsWith("text/") && getMethod.getResponseContentLength() > 0) {
                        this._responseText = new StringBuffer(getMethod.getResponseBodyAsString());
                    }
                    logMessage("GET URL: " + target + " Response Code: " + this._responseCode + " " + HttpStatus.getStatusText(this._responseCode) + " Size: " + this._responseText.length());
                    Request req = new Request(target, "GET", _responseCode, _responseText.toString());
                    req.queryString = getMethod.getQueryString();
                    req.respHeaders = getMethod.getResponseHeaders();
                    req.reqHeaders = getMethod.getRequestHeaders();
                    if (status != null) {
                        status.requests.addElement(req);
                    } else {
                        userIterationStatus.requests.addElement(req);
                    }
                    getMethod.releaseConnection();
                    if (contentType.startsWith("text/")) _getResources(target);
                    timer.cancel();
                    if (test) writeRunLog(req);
                } catch (HttpException he) {
                    errorMessage("HTTP Exception in Get " + he);
                } catch (java.io.IOException ioe) {
                    errorMessage("IO Exception in Get " + ioe);
                } catch (java.lang.IllegalStateException e) {
                } catch (Exception e) {
                    errorMessage("Exception in Get " + e, e);
                } finally {
                    if (getMethod != null) {
                        getMethod.releaseConnection();
                        getMethod = null;
                    }
                    timer.cancel();
                }
                break;
            case MULTIPARTPOST:
                multipart = true;
            case POST:
                try {
                    postMethod = new PostMethod(target);
                    postMethod.getParams().setSoTimeout(socketTimeout);
                    postMethod.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
                    postMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
                    postMethod.setRequestHeader("Referer", referer);
                    postMethod.setRequestHeader("User-Agent", "WPG Load Generator user: " + (id + 1) + " script: " + scriptName);
                    if (multipart) {
                        logger.debug("Multipart POST parameters being handled");
                        postMethod.getParams().setParameter("http.protocol.expect-continue", true);
                        postMethod.setRequestEntity(new MultipartRequestEntity(multiPostParams, postMethod.getParams()));
                    } else {
                        postMethod.setRequestBody(postParams);
                    }
                    timeoutTimer.setMethod(postMethod);
                    timeoutTimer.setTimeout(userTimeout);
                    timer.schedule(timeoutTimer, userTimeout * 1000);
                    this._responseCode = httpClient.executeMethod(postMethod);
                    if (this._responseCode > 399) {
                        errorMessage("POST URL: " + target + " Failed: " + getMethod.getStatusLine());
                    } else if (this._responseCode > 299) {
                        logMessage("POST URL: " + target + " Warning Post Succeeded with: " + getMethod.getStatusLine());
                    }
                    if (contentType.equals("text/html") && postMethod.getResponseContentLength() > 0) this._responseText = new StringBuffer(postMethod.getResponseBodyAsString());
                    logMessage("POST URL: " + target + " Response Code: " + this._responseCode + " " + HttpStatus.getStatusText(this._responseCode) + " Size: " + postMethod.getResponseBodyAsString().length());
                    Request req = new Request(target, "POST", _responseCode, _responseText.toString());
                    req.queryString = postMethod.getQueryString();
                    req.respHeaders = postMethod.getResponseHeaders();
                    req.reqHeaders = postMethod.getRequestHeaders();
                    req.postParameters = postMethod.getParameters();
                    if (status != null) {
                        status.requests.addElement(req);
                    } else {
                        userIterationStatus.requests.addElement(req);
                    }
                    if (contentType.startsWith("text/")) _getResources(target);
                    timer.cancel();
                    if (test) writeRunLog(req);
                } catch (HttpException he) {
                    errorMessage("HTTP Exception in Post " + he);
                } catch (java.io.IOException ioe) {
                    errorMessage("IO Exception in Post " + ioe);
                } catch (java.lang.IllegalStateException e) {
                } catch (Exception e) {
                    errorMessage("Exception in Post " + e, e);
                } finally {
                    if (postMethod != null) {
                        postMethod.releaseConnection();
                        postMethod = null;
                    }
                    timer.cancel();
                }
                break;
            default:
                errorMessage("HTTP Method Unknown");
                break;
        }
    }

    protected String _removeComments(String original) {
        Pattern pattern = Pattern.compile("<!--([^(-->)]*)-->", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = pattern.matcher(original);
        return m.replaceAll("");
    }

    protected void _getResources(String top) {
        try {
            URL topURL = new URL(top);
            StringReader webPageStream = new StringReader(this._responseText.toString());
            parser.parse(webPageStream, parserCallback, true);
            _getAllResourcesOfType(HTML.Tag.LINK, HTML.Attribute.HREF, "text/css", topURL.toString());
            _getAllResourcesOfType(HTML.Tag.SCRIPT, HTML.Attribute.SRC, "script/text", topURL.toString());
            _getAllResourcesOfType(HTML.Tag.IMG, HTML.Attribute.SRC, "image/bin", topURL.toString());
        } catch (java.net.MalformedURLException e) {
            errorMessage("Error fetching resources from " + top + " MalformedURLException: " + e);
        } catch (IOException e) {
            errorMessage("Error fetching resources from " + top + " IOException: " + e);
        } finally {
            parserCallback.clear();
        }
    }

    protected void _getAllResourcesOfType(HTML.Tag tag, HTML.Attribute att, String format, String top) {
        String[] objects = parserCallback.getTagAttributesAsArray(tag, att);
        if (objects == null) return;
        logger.trace("_getAllResourcesOfType: count resources: " + objects.length);
        for (int i = 0; i < objects.length; i++) {
            logger.trace("_getAllResourcesOfType: resource[" + i + "]: " + objects[i]);
            if (objects[i] != null) _fetchResource(objects[i], GET, format, top);
        }
    }

    protected boolean _checkCache(String URL) {
        if (webCache == null) {
            return false;
        }
        if (!webCache.containsKey(URL)) {
            return false;
        }
        TimeoutTimer timeoutTimer = new TimeoutTimer();
        Timer timer = new Timer();
        String cacheLastMod = (String) webCache.get(URL);
        HeadMethod head = null;
        try {
            head = new HeadMethod(URL);
            head.getParams().setSoTimeout(socketTimeout);
            head.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
            head.setFollowRedirects(followRedirects);
            timeoutTimer.setMethod(head);
            timeoutTimer.setTimeout(userTimeout);
            timer.schedule(timeoutTimer, userTimeout);
            int responseCode = httpClient.executeMethod(head);
            timer.cancel();
            if (test) {
                Request req = new Request(URL, "HEAD", responseCode, "Image/Binary");
                req.queryString = head.getQueryString();
                req.respHeaders = head.getResponseHeaders();
                req.reqHeaders = head.getRequestHeaders();
                if (status != null) {
                    status.requests.addElement(req);
                } else {
                    userIterationStatus.requests.addElement(req);
                }
            }
            if (responseCode != HttpStatus.SC_OK) return false;
            String lastModified = head.getResponseHeader("last-modified").getValue();
            head.releaseConnection();
            return lastModified.equals(cacheLastMod);
        } catch (HttpException he) {
            errorMessage("HTTP Exception in Get " + he);
        } catch (java.io.IOException ioe) {
            errorMessage("IO Exception in Get " + ioe);
        } catch (Exception e) {
            errorMessage("Exception in Get " + e, e);
        } finally {
            if (head != null) head.releaseConnection();
            timer.cancel();
        }
        return false;
    }

    protected void _updateCache(String URL, String last) {
        if (webCache == null) {
            webCache = new Hashtable();
        }
        if (webCache.containsKey(URL)) webCache.remove(URL);
        webCache.put(URL, last);
        webPageCache.put(URL, last);
        return;
    }

    protected void _fetchResource(String targetUrl, int op, String type, String referer) {
        URL url = null;
        try {
            url = new URL(new URL(referer), targetUrl);
        } catch (java.net.MalformedURLException e) {
            errorMessage("Error fetching resources from " + referer + " Exception: " + e);
        }
        if (webPageCache.containsKey(url.toString())) {
            logMessage("Resource Already Processed: " + url);
            return;
        }
        if (!_checkCache(url.toString())) {
            GetMethod getImageMethod = null;
            try {
                getImageMethod = new GetMethod(url.toString());
                getImageMethod.getParams().setSoTimeout(socketTimeout);
                getImageMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
                getImageMethod.setFollowRedirects(true);
                getImageMethod.setRequestHeader("Referer", referer);
                getImageMethod.setRequestHeader("User-Agent", "WPG Load Generator user: " + (id + 1) + " script: " + scriptName);
                int responseCode = httpClient.executeMethod(getImageMethod);
                if (test) {
                    Request req = new Request(url.toString(), "GET", responseCode, "Image/Binary/Resource");
                    req.queryString = getImageMethod.getQueryString();
                    req.respHeaders = getImageMethod.getResponseHeaders();
                    req.reqHeaders = getImageMethod.getRequestHeaders();
                    try {
                        logger.debug("saving resource: " + logDir + targetUrl);
                        File localFile = new File(logDir + targetUrl);
                        localFile.getParentFile().mkdirs();
                        BufferedInputStream is = new BufferedInputStream(getImageMethod.getResponseBodyAsStream());
                        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(localFile));
                        int i;
                        while ((i = is.read()) != -1) os.write(i);
                        is.close();
                        os.close();
                    } catch (Exception e) {
                        logger.warn("Error saving resource: " + logDir + targetUrl + " Exception: " + e);
                    }
                    if (status != null) {
                        status.requests.addElement(req);
                    } else {
                        userIterationStatus.requests.addElement(req);
                    }
                }
                if (responseCode != HttpStatus.SC_OK) {
                    logMessage("GET Resource: " + url + " Response Code: " + responseCode + " " + HttpStatus.getStatusText(responseCode));
                    if (test) {
                        if (status != null) {
                            status.logs.addElement(logStr + " Response Code: " + responseCode + " " + HttpStatus.getStatusText(responseCode));
                        } else {
                            userIterationStatus.logs.addElement(logStr + " Response Code: " + responseCode + " " + HttpStatus.getStatusText(responseCode));
                        }
                    }
                    return;
                }
                logMessage("GET Resource: " + url + " Response Code: " + responseCode + " " + HttpStatus.getStatusText(responseCode));
                String lastModified = getImageMethod.getResponseHeader("last-modified").getValue();
                _updateCache(url.toString(), lastModified);
                if (getImageMethod != null) getImageMethod.releaseConnection();
            } catch (HttpException he) {
                errorMessage("HTTP Exception in Get " + he);
            } catch (java.io.IOException ioe) {
                errorMessage("IO Exception in Get " + ioe);
            } catch (Exception e) {
                errorMessage("Exception in Get " + e, e);
            } finally {
                if (getMethod != null) getMethod.releaseConnection();
            }
        } else {
            logMessage("Resource Cached: " + url);
            webPageCache.put(url.toString(), "whenever");
        }
        return;
    }

    private int _tranCnt = 0;

    protected void writeRunLog(Request ro) {
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS-");
        _tranCnt++;
        String reference = dateFmt.format(new Date()) + _tranCnt;
        try {
            File temp = new File(logDir + reference + "-info.txt");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(temp)));
            pw.println("Request: " + ro.url);
            pw.println("************* Request Header *****************");
            for (int i = 0; i < ro.reqHeaders.length; i++) pw.print(ro.reqHeaders[i].toString());
            pw.println("************* Response Header *****************");
            for (int i = 0; i < ro.respHeaders.length; i++) pw.print(ro.respHeaders[i].toString());
            pw.close();
            temp = new File(logDir + reference + "-body.html");
            pw = new PrintWriter(new BufferedWriter(new FileWriter(temp)));
            pw.print(ro.respText);
            pw.close();
        } catch (Exception e) {
            logger.error("Exception caught writing run log entry, Exception: " + e);
        }
    }
}
