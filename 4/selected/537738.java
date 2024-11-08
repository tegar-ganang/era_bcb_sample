package com.elibera.m.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Image;
import com.elibera.m.app.MLE;
import com.elibera.m.app.AppSettings;
import com.elibera.m.display.ProgressCanvas;
import com.elibera.m.events.Closeable;
import com.elibera.m.events.HelperEvent;
import com.elibera.m.events.HelperThread;
import com.elibera.m.events.ProgressBarThread;
import com.elibera.m.events.RecordingElement;
import com.elibera.m.events.RootThread;
import com.elibera.m.rms.HelperRMSStoreMLibera;
import com.elibera.m.rms.RMS;
import com.elibera.m.utils.HelperApp;
import com.elibera.m.utils.HelperStd;
import com.elibera.m.utils.MyByte;
import com.elibera.m.utils.MyInteger;
import com.elibera.m.xml.PageSuite;
import com.elibera.m.fileio.HelperFileIO;
import com.elibera.m.utils.HelperLocation;

/**
 * EIne statische Klasse zur Durchführung divereser HTTP Aufgaben
 * @author meisi
 * @date 17.09.2005
 *	@time 12:11:13
 * -------------------------------------------------------------------------------------------------------
 * Copyright eLibera Meisenberger & Lazaridis OEG 2005, alle Rechte vorbehalten 
 * Verwendung nur mit Erlaubnis von eLibera Meisenberger & Lazaridis OEG
 * Copright eLibera Meisenberger & Lazaridis OEG 2005, all rights reserved
 * -------------------------------------------------------------------------------------------------------
 */
public class HelperHTTP {

    public static String URL_QUERY_BINARY = "bin";

    public static String URL_QUERY_XML_PAGE = "xml";

    public static String URL_QUERY_CONTENT = "content";

    public static int BUFFER_SIZE = 5000;

    /**
     * if the response length exceeds this limit the whole response will be cached 
     * int the file system to save heap size memory
     */
    public static int RESPONSE_LENGTH_FOR_FILE_IO_TEMP_FILE = 0;

    public static MLE midlet;

    public static final String VALUE_USER_AGENT = "mle";

    public static String VALUE_LANGUAGE = "";

    private static volatile boolean lockRedirectServer = false;

    /**
     * holt sich einen lock für getHTTPRessource
     *
     */
    private static void getLockRedirectServer(RequestTask req) throws Exception {
        int count = 0;
        Thread.sleep(1);
        while (!doLock()) {
            count++;
            Thread.sleep(100);
            if (count % 10 == 0) checkIfAlive(req);
        }
    }

    private static synchronized boolean doLock() {
        if (lockRedirectServer) return false;
        lockRedirectServer = true;
        return true;
    }

    public static boolean isRedirectServerInUse() {
        return lockRedirectServer;
    }

    /**
	 * führt eine HTTP Aktion entsprechend des RequestTask aus
	 * sollte es ein Event mit einer Progress-Bar sein, so muss diese bereits am Bildschirm aktiviert worden sein
	 * @param req der befüllte Request-Task
	 * @return
	 */
    public static RequestTask getHTTPRessource(RequestTask req) {
        System.out.println("getHTTPRessource:");
        if (req.mlpSession >= 0) midlet.httpMLPServerLastAccess = req.mlpSession;
        req.cachePos = HelperApp.checkForHTTPURLInCache(req.url, req.mlpSession >= 0 ? 'm' : 'h');
        if (req.cachePos >= 0) {
            System.out.println("check fast cache:" + midlet.appBinaryCacheTime[req.cachePos] + "," + HelperRMSStoreMLibera.getAppData(18) * 1000 + "," + System.currentTimeMillis());
            if (midlet.appBinaryCacheTime[req.cachePos] + HelperRMSStoreMLibera.getAppData(18) * 1000 > System.currentTimeMillis()) {
                req.response = midlet.appBinaryCache[req.cachePos];
                req.receivedDataContentType = midlet.appBinaryCacheCT[req.cachePos];
                System.out.println("cache fast used!!!" + req.response.length);
                return req;
            }
        }
        ProgressCanvas pb = null;
        if (req.isProgressBarTask) pb = req.getGauge();
        CloseServerTimeoutTask timeout = null;
        Server s = null;
        Closeable cIn = null, cOut = null;
        try {
            AppSettings.informHTTPConnectionRequest(req);
            if (HelperServer.URL_GATEWAY == null) return doPlainHttpRequest(req, pb);
            getLockRedirectServer(req);
            checkIfAlive(req);
            checkIfAlive(req);
            System.out.println("Loading URL:" + req.url);
            MyInteger to = new MyInteger(0);
            if (HelperServer.GATEWAY_ERROR_NR <= 2) {
                if (pb != null) pb.setValueRePaint(0);
                HelperServer.startRedirectServer(pb, to);
            }
            while (!HelperServer.redirectServerStarted && !HelperServer.BL && HelperServer.GATEWAY_ERROR == null) {
                if (to.i > 30000) {
                    HelperServer.closeGatewayServer();
                    throw new Exception("could not open Connection to Gateway Server! timeout!");
                }
                Thread.sleep(100);
                checkIfAlive(req);
                to.i += 100;
            }
            if (HelperServer.BL) {
                HelperApp.setErrorAlert(HelperServer.BLOCKED_MSG, null, req, req.connectionThread.previousScreen);
                req.connectionThread.stopThread();
                req.error = new StringBuffer(HelperServer.BLOCKED_MSG);
                return req;
            }
            if (HelperServer.GATEWAY_ERROR != null) {
                return doPlainHttpRequest(req, pb);
            }
            s = HelperServer.getRedirectServer();
            checkIfAlive(req);
            if (pb != null) pb.setMode(0);
            String response = null;
            cIn = Closeable.create(s.is, req.connectionThread);
            cOut = Closeable.create(s.os, req.connectionThread);
            try {
                response = doRequest(req, pb, s);
                if (HelperStd.isEmpty(response)) throw new IOException("Did the server close the connection?");
            } catch (IOException ioo) {
                if (req.error.length() > 0) throw ioo;
                HelperServer.closeGatewayServer();
                req.error.append(ioo.getMessage());
                lockRedirectServer = false;
                return getHTTPRessource(req);
            }
            System.out.println("resp:" + response);
            int c = 0;
            while (response.compareTo("1") == 0) {
                if (pb != null) {
                    pb.setText(HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_INFO_TEXT_WAITING_FOR_RESPONSE) + (c > 0 ? "#" + c : ""));
                    pb.setValueRePaint(0);
                }
                c++;
                response = HelperServer.readLine(s.is);
                System.out.println("resp2:" + response);
            }
            if (response.compareTo("cached") == 0) {
                req.response = midlet.appBinaryCache[req.cachePos];
                req.receivedDataContentType = midlet.appBinaryCacheCT[req.cachePos];
                System.out.println("cache used!!!" + req.response.length);
                return req;
            }
            if (response.compareTo("ad2") == 0) {
                MLE.midlet.pg.loadAdd(s);
                response = HelperServer.readLine(s.is);
            }
            if (response.compareTo("error") == 0) {
                String msg = HelperServer.readLine(s.is);
                System.out.println(response + ":" + msg + ",");
                if (msg != null && msg.compareTo("url") == 0) {
                    req.error = new StringBuffer(HelperApp.translateWord(HelperApp.TEXT_ERROR_HTTP_CONNECTION_INVALID_URL) + req.url);
                    if (req.showError) HelperThread.setError(req.connectionThread, req.error.toString(), null);
                } else {
                    req.error = new StringBuffer(HelperApp.translateWord(HelperApp.TEXT_ERROR_HTTP_CONNECTION_ERROR_URL_REQUEST) + msg + ";\nURL:" + req.url);
                    if (req.showError) HelperThread.setError(req.connectionThread, req.error.toString(), null);
                }
                req.connectionThread.stopThread();
                return req;
            } else if (response.compareTo("mediaerror") == 0) {
                byte[] msg = new byte[HelperStd.parseInt(HelperServer.readLine(s.is), 0)];
                s.is.read(msg);
                req.error = new StringBuffer(HelperApp.translateWord(HelperApp.TEXT_ERROR_HTTP_CONNECTION_ERROR_MEDIA_CONVERTING) + new String(msg));
                if (req.showError) HelperThread.setError(req.connectionThread, req.error.toString(), null);
                req.connectionThread.stopThread();
                return req;
            } else if (response.compareTo("converting") == 0) {
                byte[] msg = new byte[HelperStd.parseInt(HelperServer.readLine(s.is), 0)];
                System.out.println("conv:" + msg.length);
                s.is.read(msg);
                String url = new String(msg);
                System.out.println("converting:" + url);
                req.error = new StringBuffer(url);
                req.receivedDataContentType = "conv";
                if (req.showError) HelperApp.setInfoAlert(HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_INFO_TEXT_MEDIA_CONVERSION), HelperApp.translateCoreWord(HelperApp.WORD_CONVERTING), req.connectionThread.previousScreen);
                req.connectionThread.previousScreen = midlet.alerts.infoAlert;
                cIn.remove();
                if (cOut != null) cOut.remove();
                req.connectionThread.stopThread();
                return req;
            }
            while (response.compareTo("ok") != 0) response = HelperServer.readLine(s.is);
            req.receivedDataContentType = HelperServer.readLine(s.is);
            boolean cacheResponse = false;
            if (HelperServer.readLine(s.is).compareTo("1") == 0) cacheResponse = true;
            int len = HelperStd.parseInt(HelperServer.readLine(s.is), 0);
            String txt = null;
            if (pb != null) {
                pb.setMode(3);
                txt = HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_MSG_GET_REQUEST_DOWNLOADING_DATA) + HelperStd.formatBytes(len);
                pb.setText(txt);
                pb.doRepaint();
            }
            if (req.receivedDataContentType.indexOf("mlepre") > 0 && req.downloadThread != null) {
                req.preparsedStoreId = RMS.savePreparsedPageSuite(s.is, len, req.receivedDataContentType, HelperEvent.getServerID(null, req.url, req.typeLong), req.url, req.typeLong, req.connectionThread, pb, false);
                return req;
            }
            req.responseLength = len;
            if (len == 0) req.response = new byte[0]; else {
                if (pb != null) pb.setMaxValue(len);
                int actual = 0;
                int bytesread = 0;
                OutputStream out = null;
                StringBuffer fileName = new StringBuffer();
                if (len > RESPONSE_LENGTH_FOR_FILE_IO_TEMP_FILE) req.fconn = HelperFileIO.writeTmpFile(midlet.dc.ps, fileName);
                System.out.println(req.fconn + "," + fileName);
                if (req.fconn != null) {
                    out = req.fconn.openOutputStream();
                    req.fconnFileName = fileName.toString();
                }
                System.out.println("downloading:" + out + "," + req.responseLength);
                if (out != null) req.responseTmpFile = true; else req.response = new byte[len];
                if (pb != null && len > 10000) {
                    if (out != null) pb.setText(txt + " [stream]"); else pb.setText(txt + " [memory]");
                    pb.doRepaint();
                }
                while ((bytesread < len) && (actual != -1)) {
                    int toread = BUFFER_SIZE;
                    if (toread >= len - bytesread) toread = len - bytesread;
                    if (out == null) actual = s.is.read(req.response, bytesread, toread); else {
                        byte[] b = new byte[toread];
                        actual = s.is.read(b);
                        out.write(b, 0, actual);
                    }
                    if (actual > 0) bytesread += actual;
                    if (pb != null) {
                        pb.setValueRePaint(bytesread);
                    }
                }
                System.out.println("finished");
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
            if (pb != null) pb.setTitel(HelperApp.translateCoreWord(HelperApp.WORD_LOADING));
            if (cacheResponse && req.response != null) {
                System.out.println("caching page:" + req.response + ";" + req.url + "," + req.receivedDataContentType);
                HelperApp.cacheAppBinary(req.url, req.response, req.receivedDataContentType);
                System.out.println("response was cached!!!" + req.response + "," + req.url);
            }
            checkIfAlive(req);
            System.out.println("HTTP-Returned content:" + req.receivedDataContentType);
            if (req.receivedDataContentType.indexOf("text") == 0) {
                if (req.response != null) System.out.println(new String(req.response)); else if (req.fconnFileName != null) System.out.println(new String(HelperFileIO.getFileData(req.fconnFileName, null, pb)));
            }
            System.out.println("-------------------");
        } catch (java.io.InterruptedIOException io) {
            HelperStd.log(io, "Interrupted POST, GET:" + req.url);
            HelperServer.closeGatewayServer();
            if (req.connectionThread == null || !req.connectionThread.isRunning()) return null;
            if (req != null) {
                req.error.append(io.getMessage());
                if (req.showError) HelperThread.setError(req.connectionThread, HelperApp.translateWord(HelperApp.TEXT_ERROR_HTTP_CONNECTION_INTERRUPTED), io);
            }
        } catch (Exception e) {
            HelperStd.log(e, "Verbindung POST, GET:" + req.url);
            if (req.connectionThread == null || !req.connectionThread.isRunning()) return null;
            HelperServer.closeGatewayServer();
            if (req != null) {
                req.error.append(e.getMessage());
                if (req.showError) HelperThread.setError(req.connectionThread, HelperApp.translateWord(HelperApp.TEXT_ERROR_HTTP_CONNECTION) + req.url, e);
            }
        } finally {
            if (timeout != null) timeout.doStop();
            lockRedirectServer = false;
            try {
                if (req != null && req.response != null && !HelperServer.isProxyConnectionUrl()) HelperRMSStoreMLibera.incDataBytesDownload(req.response.length);
            } catch (Exception e) {
            }
            if (cIn != null) cIn.remove();
            if (cOut != null) cOut.remove();
        }
        return req;
    }

    private static String doRequest(RequestTask req, ProgressCanvas pb, Server s) throws Exception {
        if (req.cachePos >= 0 && req.postDataLength <= 0) {
            System.out.println("cache-GET-request");
            writeEncoded("cache", s);
        } else writeEncoded("url", s);
        writeEncoded(req.url, s);
        if (req.mlpSession >= 0) {
            writeEncoded(HelperRMSStoreMLibera.getUsername(midlet.httpMLPServerNames[req.mlpSession]), s);
            String pass = HelperRMSStoreMLibera.getPassword(midlet.httpMLPServerNames[req.mlpSession]);
            int passMode = HelperRMSStoreMLibera.appData[0];
            if (passMode == 2 || passMode == 3) {
                if (midlet.httpMLPCachedPass == null) {
                    midlet.httpMLPCachedPass = new String[midlet.httpMLPServerNames.length];
                    midlet.httpMLPCachedPass[req.mlpSession] = HelperStd.getMD5Hash((pass + HelperServer.redirectServerKey).getBytes());
                }
                pass = midlet.httpMLPCachedPass[req.mlpSession];
            }
            writeEncoded(pass, s);
        } else {
            writeEncoded("", s);
            writeEncoded("", s);
        }
        writeEncoded((HelperApp.rt.totalMemory() - HelperApp.rt.freeMemory()) + "", s);
        writeEncoded(HelperRMSStoreMLibera.getFreeSpaceOfRootStore() + "", s);
        System.out.println("REFERER:" + req.referer);
        writeEncoded(req.referer, s);
        String[] toWrite = null;
        if (HelperLocation.newLocationValue) toWrite = HelperStd.incArray(toWrite, "l" + HelperLocation.locationValue);
        System.out.println("LOCATION:" + HelperLocation.newLocationValue + "," + HelperLocation.locationValue);
        if (toWrite != null) {
            writeEncoded(toWrite.length + "", s);
            for (int i = 0; i < toWrite.length; i++) {
                writeEncoded(toWrite[i], s);
            }
        } else writeEncoded("0", s);
        writeEncoded("" + req.postParamNames.length, s);
        writeEncoded("" + req.postFileNames.length, s);
        if (req.postDataLength > 0) {
            if (req.isProgressBarTask) {
                HelperThread.setProgressText(((ProgressBarThread) req.connectionThread), HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_MSG_POST_REQUEST));
                pb.setMaxValue(req.postDataLength);
            }
            int dataSend = 0;
            System.out.println("post:" + req.postDataLength);
            for (int i = 0; i < req.postParamNames.length; i++) {
                writeEncoded(req.postParamNames[i], s);
                writeEncoded(req.postParamValues[i], s);
                dataSend += req.postParamNames[i].length() + req.postParamValues[i].length();
                s.os.flush();
                if (req.isProgressBarTask) pb.setValueRePaint(dataSend);
            }
            req.postParamNames = null;
            req.postParamValues = null;
            for (int i = 0; i < req.postFileNames.length; i++) {
                System.out.println(req.postFileNames[i] + "," + req.postFileContentTypes[i] + "," + req.postFileData[i] + "," + req.postFileDataBinary[i]);
                writeEncoded(req.postFileParamName[i], s);
                writeEncoded(req.postFileNames[i], s);
                writeEncoded(req.postFileContentTypes[i], s);
                RecordingElement el = req.postFileData[i];
                long dataSize = 0;
                if (el != null) dataSize = req.postFileData[i].getDataSize(); else dataSize = req.postFileDataBinary[i].length;
                writeEncoded(dataSize + "", s);
                s.os.write(("b" + dataSize + "\n").getBytes());
                dataSend += req.postFileNames[i].length() + req.postFileContentTypes[i].length();
                try {
                    if (el != null) {
                        el.writeDataToStream(s.os, pb);
                        req.postFileData[i] = null;
                    } else {
                        s.os.write(req.postFileDataBinary[i]);
                        req.postFileDataBinary[i] = null;
                    }
                    s.os.flush();
                } catch (Exception ee) {
                    throw new Exception(ee + ":" + ee.getMessage());
                }
                dataSend += dataSize;
                if (req.isProgressBarTask) pb.setValueRePaint(dataSend);
            }
        }
        HelperApp.runJavaGarbageCollector();
        s.os.write("-1\n".getBytes());
        s.os.flush();
        if (pb != null) {
            pb.setText(HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_INFO_TEXT_WAITING_FOR_RESPONSE));
            pb.setValueRePaint(0);
        }
        CloseServerTimeoutTask timeout = new CloseServerTimeoutTask(s);
        String response = null;
        try {
            timeout.start();
            response = HelperServer.readLine(s.is);
        } finally {
            if (timeout != null) timeout.doStop();
            timeout = null;
        }
        return response;
    }

    private static void writeEncoded(String v, Server s) throws IOException {
        HelperServer.writeEncoded(v, s.os);
    }

    /**
	 * schmeißt eine Exception wenn der Thread abgebrochen wurde
	 * @param req
	 * @throws Exception
	 */
    public static void checkIfAlive(RequestTask req) throws Exception {
        if (!req.isThreadAlive()) throw new Exception(HelperApp.translateWord(HelperApp.TEXT_ERROR_USER_ABORTED_CURRENT_ACTION));
    }

    /**
	 * gibt einen RequestTaks für einen HTTP Zugrif auf den MLP Server zurück, Merke es wird nur eine URL gesetzt
	 * @param midlet
	 * @param params
	 * @param values
	 * @return der RequestTask
	 */
    public static RequestTask getMLPlattformRequestTask(String param, String value, ProgressBarThread conThread, int mlpServerPos, PageSuite lastOpenPs) {
        String u = AppSettings.getMLPURL(mlpServerPos);
        int mlpSession = mlpServerPos;
        String mlpurl = null;
        if (value.indexOf("http") == 0) {
            value = checkHttpReplaceUrl(value);
            mlpSession = AppSettings.isMLPServer(value);
            if (mlpSession < 0) mlpSession = 0; else mlpurl = value;
        }
        if (mlpSession >= 0 && mlpurl == null) {
            StringBuffer url = new StringBuffer(u);
            if (midlet.httpMLPServerTypes[mlpSession]) {
                url.append(param);
                if (value.charAt(0) != '/') url.append('/');
                url.append(value);
            } else {
                boolean fr = false;
                if (u.indexOf('?') > 0) fr = true;
                url.append(fr ? '&' : '?');
                url.append(urlEncode(param));
                url.append('=');
                fr = false;
                if ((value.indexOf('?') >= 0 || value.indexOf('&') >= 0) && value.indexOf('=') > 0) fr = true;
                if (fr && value.indexOf('?') >= 0) value = value.replace('?', '&');
                if (fr) url.append(value); else url.append(urlEncode(value));
            }
            mlpurl = url.toString();
            mlpurl = checkHttpReplaceUrl(mlpurl);
        }
        RequestTask req = new RequestTask(mlpurl, "m|" + midlet.httpMLPServerNames[mlpServerPos], conThread, mlpSession);
        setReferer(req, lastOpenPs);
        return req;
    }

    /**
	 * gibt einen Request Task für eine Binary vom MLP zurück
	 * @param binaryID
	 * @param midlet
	 * @param progressBarTask
	 * @param conThread
	 * @return
	 */
    public static RequestTask getMLPlattformBinaryRequestTask(String binaryID, ProgressBarThread conThread, int mlpServerPos, PageSuite lastOpenPs) {
        return getMLPlattformRequestTask(URL_QUERY_BINARY, binaryID, conThread, mlpServerPos, lastOpenPs);
    }

    /**
	 * gibt einen Request Task für eine PageSuite vom MLP zurück
	 * @param contentID
	 * @param midlet
	 * @param progressBarTask
	 * @param conThread
	 * @return
	 */
    public static RequestTask getMLPlattformContentRequestTask(String contentID, ProgressBarThread conThread, int mlpServerPos, PageSuite lastOpenPs) {
        return getMLPlattformRequestTask(URL_QUERY_CONTENT, contentID, conThread, mlpServerPos, lastOpenPs);
    }

    /**
	 * gibt einen RequestTask für eine XML Seite am MLP zurück
	 * @param xmlID
	 * @param midlet
	 * @param progressBarTask
	 * @param conThread
	 * @return
	 */
    public static RequestTask getMLPlattformXMLPageRequestTask(String xmlID, ProgressBarThread conThread, int mlpServerPos, PageSuite lastOpenPs) {
        return getMLPlattformRequestTask(URL_QUERY_XML_PAGE, xmlID, conThread, mlpServerPos, lastOpenPs);
    }

    /**
	 * gibt einen Request Task für eine beliebige HTTP URL zurück
	 * @param midlet
	 * @param url
	 * @param progressBarTask
	 * @param conThread
	 * @return
	 */
    public static RequestTask getAnyHTTPRequestTask(String url, String typeLong, RootThread conThread, boolean isActiveProgressBarThread, PageSuite lastOpenPs) throws Exception {
        url = url.trim();
        url = checkHttpReplaceUrl(url);
        int mlpSession = AppSettings.isMLPServer(url);
        RequestTask req = new RequestTask(url, typeLong, null, mlpSession);
        req.typeLong = typeLong;
        req.connectionThread = conThread;
        if (isActiveProgressBarThread) req.isProgressBarTask = true;
        setReferer(req, lastOpenPs);
        return req;
    }

    public static String[] HTTP_REPLACE_TOKEN_START = new String[0];

    public static String[] HTTP_REPLACE_VALUE = new String[0];

    public static String checkHttpReplaceUrl(String url) {
        System.out.println("checkHttpReplaceUrl:" + url);
        for (int i = 0; i < HTTP_REPLACE_TOKEN_START.length; i++) {
            if (url.indexOf(HTTP_REPLACE_TOKEN_START[i]) == 0 && i < HTTP_REPLACE_VALUE.length) return HTTP_REPLACE_VALUE[i] + url.substring(HTTP_REPLACE_TOKEN_START[i].length());
        }
        return url;
    }

    /**
	 * sets the referer of the request task 
	 * @param req
	 * @param lastOpenPs
	 */
    public static void setReferer(RequestTask req, PageSuite lastOpenPs) {
        req.referer = "";
        if (lastOpenPs == null || HelperStd.isEmpty(lastOpenPs.typeLong) || HelperStd.isEmpty(lastOpenPs.url)) return;
        String t = lastOpenPs.typeLong;
        if (t.length() == 1 && t.charAt(0) == 'm') {
            t += "|" + midlet.httpMLPServerNames[midlet.httpMLPServerLastAccess];
        }
        req.referer = t + "|" + lastOpenPs.url;
        if (lastOpenPs.curPage > 0) req.referer += "#" + lastOpenPs.curPage;
    }

    /**
	 * loads a HTTP ressource 
	 * @param url if url starts with http than the link type "h" is used, otherwise the link type "m" is used
	 * @param mlpServerPos
	 * @param thread
	 * @return the byte[] data or null
	 * @throws Exception
	 */
    public static MyByte loadRessource(String url, int mlpServerPos, ProgressBarThread thread, PageSuite lastOpenPs, StringBuffer contentType) throws Exception {
        RequestTask req = null;
        if (url.indexOf("http://") == 0 || url.charAt(0) == '{') req = HelperHTTP.getAnyHTTPRequestTask(url, "h", thread, true, lastOpenPs); else req = HelperHTTP.getMLPlattformBinaryRequestTask(url, thread, mlpServerPos, lastOpenPs);
        req = HelperHTTP.getHTTPRessource(req);
        if (req.receivedDataContentType != null) contentType.append(req.receivedDataContentType);
        if (!thread.isRunning()) {
            return null;
        }
        return new MyByte(req);
    }

    /**
	 * fügt der URL die Parameter hinzu
	 * @param url --> die bestehende URL, kann bereits Parameter enthalten
	 * @param param --> Array von Parametern
	 * @param values --> Array der zugehörigen Werte
	 * @return --> die neue URL
	 */
    public static String addParamsToURL(String url, String[] param, String[] values) {
        if (url.indexOf('?') > 0) url += "&"; else url += "?";
        String add = "";
        for (int i = 0; i < param.length; i++) {
            url += add + urlEncode(param[i]) + "=" + urlEncode(values[i]);
            add = "&";
        }
        return url;
    }

    /**
     * ersetzt illigale Zeichen für eine URL in dem String
     * zB Space --> %20
     */
    public static String urlEncode(String s) {
        if (s == null) return "";
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char a = s.charAt(i);
            if (a < 48 || (a > 57 && a < 65) || (a > 90 && a < 97) || a > 122) ret.append("%" + Integer.toHexString(a)); else ret.append(a);
        }
        return ret.toString();
    }

    public static String HEADER_USERNAME = "MLIBERA_USERNAME";

    public static String HEADER_PASSWORD = "MLIBERA_PASSWORD";

    private static String DEFBOUNDARY = "--AaB03x";

    private static String NL = "\r\n";

    private static void writeUsAscii(OutputStream os, String text) throws Exception {
        os.write(text.getBytes("US-ASCII"));
    }

    /**
     * eine einfache Methode um den InputStream zu capturen
     * req.is enthält den InputStream
     * */
    public static HttpConnection doPlainHttpRequestInternal(RequestTask req, ProgressCanvas pb) throws Exception {
        String BOUNDARY = DEFBOUNDARY + System.currentTimeMillis();
        System.out.println("doPlainHttpRequest:" + req.url);
        HttpConnection hc = null;
        Thread.sleep(10);
        if (req.postDataLength > 0) {
            hc = (HttpConnection) Connector.open(req.url, Connector.READ_WRITE);
            hc.setRequestMethod(HttpConnection.POST);
            hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        } else {
            hc = (HttpConnection) Connector.open(req.url, Connector.READ_WRITE);
            hc.setRequestMethod(HttpConnection.GET);
        }
        if (req.connectionThread != null) req.c = Closeable.create(hc, req.connectionThread);
        hc.setRequestProperty("Accept-Language", VALUE_LANGUAGE);
        hc.setRequestProperty("Accept", "*/*");
        hc.setRequestProperty("User-Agent", VALUE_USER_AGENT);
        hc.setRequestProperty("MLIBERA_DEVICE", midlet.deviceName);
        hc.setRequestProperty("MLIBERA_VERSION", midlet.version);
        hc.setRequestProperty("MLIBERA_PLATFORM", VALUE_USER_AGENT);
        if (req.mlpSession >= 0) {
            System.out.println("use cookie:" + MLE.midlet.httpMLPSession[req.mlpSession]);
            System.out.println(HelperRMSStoreMLibera.getUsername(midlet.httpMLPServerNames[req.mlpSession]));
            System.out.println(HelperRMSStoreMLibera.getPassword(midlet.httpMLPServerNames[req.mlpSession]));
            hc.setRequestProperty("Cookie", "" + MLE.midlet.httpMLPSession[req.mlpSession]);
            if (MLE.midlet.httpMLPSession[req.mlpSession] == null) {
                hc.setRequestProperty(HEADER_USERNAME, "" + HelperRMSStoreMLibera.getUsername(midlet.httpMLPServerNames[req.mlpSession]));
                hc.setRequestProperty(HEADER_PASSWORD, "" + HelperRMSStoreMLibera.getPassword(midlet.httpMLPServerNames[req.mlpSession]));
            }
        }
        if (req.postDataLength > 0) {
            if (req.isProgressBarTask) pb.setText(HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_MSG_POST_REQUEST));
            OutputStream os = hc.openOutputStream();
            req.os = os;
            for (int i = 0; i < req.postParamNames.length; i++) {
                byte[] d = req.postParamValues[i].getBytes("UTF-8");
                writeUsAscii(os, "--" + BOUNDARY + NL);
                writeUsAscii(os, "Content-disposition: form-data; name=\"" + req.postParamNames[i] + "\"" + NL);
                writeUsAscii(os, "Content-Type: text/plain; charset=UTF-8" + NL);
                writeUsAscii(os, "Content-Length: " + d.length + NL);
                writeUsAscii(os, "Content-Transfer-Encoding: 8bit" + NL + NL);
                os.write(d);
                writeUsAscii(os, NL);
            }
            req.postParamValues = null;
            req.postParamNames = null;
            pb.setMaxValue(req.postFileNames.length);
            for (int i = 0; i < req.postFileNames.length; i++) {
                writeUsAscii(os, "--" + BOUNDARY + NL);
                writeUsAscii(os, "Content-Disposition: attachment; filename=\"" + req.postFileNames[i] + "\"" + NL);
                writeUsAscii(os, "Content-Type: " + req.postFileContentTypes[i] + "; name=\"" + req.postFileParamName[i] + "\"" + NL);
                long dataSize = 0;
                RecordingElement el = req.postFileData[i];
                if (el != null) dataSize = req.postFileData[i].getDataSize(); else dataSize = req.postFileDataBinary[i].length;
                writeUsAscii(os, "Content-Length: " + dataSize + NL);
                writeUsAscii(os, "Content-Transfer-Encoding: binary" + NL + NL);
                if (el != null) {
                    el.writeDataToStream(os, null);
                    req.postFileData[i] = null;
                } else {
                    os.write(req.postFileDataBinary[i]);
                    req.postFileDataBinary[i] = null;
                }
                writeUsAscii(os, NL);
                pb.setValueRePaint(i + 1);
            }
            os.write(("--" + BOUNDARY + "--" + NL + NL).getBytes());
            os.close();
            req.postDataLength = -1;
        }
        int code = hc.getResponseCode();
        System.out.println("response-code:" + code);
        if ((code >= 300 && code <= 303) || code == 307) {
            req.cachePos++;
            if (req.cachePos > 5) throw new Exception("too many redirects!");
            String url = hc.getHeaderField("Location");
            System.out.println("Location:" + url);
            if (url != null) {
                req.url = url;
                return doPlainHttpRequestInternal(req, pb);
            }
        }
        req.cachePos = -1;
        HelperApp.runJavaGarbageCollector();
        InputStream is = hc.openInputStream();
        req.receivedDataContentType = hc.getHeaderField("Content-Type");
        System.out.println("content-type:" + req.receivedDataContentType);
        if (req.mlpSession >= 0) {
            StringBuffer cookies = new StringBuffer();
            for (int i = 0; i < 200; i++) {
                String n = hc.getHeaderFieldKey(i);
                if (n == null) break;
                if (n.toLowerCase().compareTo("set-cookie") != 0) continue;
                n = hc.getHeaderField(i);
                System.out.println("new cookie:" + n);
                String[] cks = HelperStd.split(n.replace(',', ';'), ';');
                for (int c = 0; c < cks.length; c++) {
                    cks[c] = cks[c].trim();
                    String ckslower = cks[c].toLowerCase();
                    if (ckslower.indexOf('=') <= 0) continue;
                    if (ckslower.indexOf("path=") == 0 || ckslower.indexOf("expires=") == 0) continue;
                    if (cookies.length() > 0) cookies.append("; ");
                    cookies.append(cks[c]);
                }
                int p = n.indexOf("; ");
                if (p > 0) n = n.substring(0, p);
            }
            System.out.println("new cookies:" + cookies);
            if (cookies.length() > 0) midlet.httpMLPSession[req.mlpSession] = cookies.toString();
        }
        req.is = is;
        return hc;
    }

    /**
     * eine Workaround methode, falls der redirect server tot ist
     * @param req
     * @return
     * @throws IOException
     */
    public static RequestTask doPlainHttpRequest(RequestTask req, ProgressCanvas pb) throws Exception {
        boolean post = req.postDataLength > 0;
        HttpConnection hc = doPlainHttpRequestInternal(req, pb);
        InputStream is = req.is;
        int len = (int) hc.getLength();
        if (req.isProgressBarTask) pb.setText(HelperApp.translateWord(HelperApp.TEXT_PROGRESS_BAR_HTTP_MSG_GET_REQUEST_DOWNLOADING_DATA) + len + " Bytes");
        System.out.println("content-length:" + len);
        if (len <= 0) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) {
                bout.write(ch);
                byte[] buffer = new byte[BUFFER_SIZE];
                int read = is.read(buffer);
                if (read > 0) bout.write(buffer, 0, read);
            }
            req.response = bout.toByteArray();
        } else {
            if (req.isProgressBarTask) pb.setMaxValue(len);
            req.response = new byte[len];
            int bytesread = 0, actual = 0;
            while ((bytesread < len) && (actual != -1)) {
                int toread = BUFFER_SIZE;
                if (toread >= len - bytesread) toread = len - bytesread;
                actual = is.read(req.response, bytesread, toread);
                if (actual > 0) bytesread += actual;
                if (req.isProgressBarTask) pb.setValueRePaint(bytesread);
            }
        }
        HelperRMSStoreMLibera.incDataBytesDownload(req.response.length);
        System.out.println("HTTP-Returned content:" + req.receivedDataContentType);
        if (req.receivedDataContentType.indexOf("text") == 0) {
            if (req.response != null) System.out.println(new String(req.response)); else if (req.fconnFileName != null) System.out.println(new String(HelperFileIO.getFileData(req.fconnFileName, null, pb)));
        }
        System.out.println("-------------------");
        if (!post) {
            String cc = hc.getHeaderField("Cache-Control");
            if (cc == null || cc.indexOf("no-cache") < 0) HelperApp.cacheAppBinary(req.url, req.response, req.receivedDataContentType);
        }
        if (req.c != null) req.c.remove();
        req.c = null;
        return req;
    }
}
