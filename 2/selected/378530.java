package com.hyk.proxy.client.util;

import java.io.Console;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy.Type;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jivesoftware.smack.XMPPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hyk.compress.CompressorFactory;
import com.hyk.compress.preference.DefaultCompressPreference;
import com.hyk.proxy.client.application.gae.config.Config;
import com.hyk.proxy.client.application.gae.config.Config.ConnectionMode;
import com.hyk.proxy.client.application.gae.config.Config.HykProxyServerAuth;
import com.hyk.proxy.client.application.gae.config.Config.ProxyInfo;
import com.hyk.proxy.client.application.gae.config.Config.ProxyType;
import com.hyk.proxy.client.application.gae.config.Config.XmppAccount;
import com.hyk.proxy.client.application.gae.rpc.HttpClientRpcChannel;
import com.hyk.proxy.client.application.gae.rpc.XmppRpcChannel;
import com.hyk.proxy.common.Constants;
import com.hyk.proxy.common.http.header.SetCookieHeaderValue;
import com.hyk.proxy.common.http.message.HttpMessageExhange;
import com.hyk.proxy.common.http.message.HttpRequestExchange;
import com.hyk.proxy.common.http.message.HttpResponseExchange;
import com.hyk.proxy.common.http.message.HttpServerAddress;
import com.hyk.proxy.common.rpc.service.MasterNodeService;
import com.hyk.proxy.common.xmpp.XmppAddress;
import com.hyk.proxy.framework.prefs.Preferences;
import com.hyk.proxy.framework.util.SslCertificateHelper;
import com.hyk.rpc.core.RPC;
import com.hyk.rpc.core.RpcException;
import com.hyk.rpc.core.Rpctimeout;
import com.hyk.rpc.core.address.Address;
import com.hyk.rpc.core.constant.RpcConstants;

/**
 *
 */
public class ClientUtils {

    protected static Logger logger = LoggerFactory.getLogger(ClientUtils.class);

    private static byte[] STDIN_BUFFER = new byte[1024];

    private static Console console = System.console();

    private static final String ContentRangeValueHeader = "bytes";

    private static MasterNodeService master = null;

    private static final String DEFAULT_GOOGLE_PROXY_TYPE = "DefaultGoogleHttpProxyType";

    public static final int DIRECT = 0;

    public static final int OVER_HTTP = 1;

    public static final int OVER_HTTPS = 2;

    public static SSLContext getFakeSSLContext(String host, String port) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(SslCertificateHelper.getClientKeyStore(host), SslCertificateHelper.KS_PASS.toCharArray());
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    public static boolean isIPV6Address(String address) {
        try {
            return InetAddress.getByName(address) instanceof Inet6Address;
        } catch (Throwable e) {
            return false;
        }
    }

    public static HttpResponse buildHttpServletResponse(HttpResponseExchange forwardResponse) throws IOException {
        if (null == forwardResponse) {
            return new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_TIMEOUT);
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(forwardResponse.getResponseCode()));
        List<String[]> headers = forwardResponse.getHeaders();
        for (String[] header : headers) {
            if (header[0].equalsIgnoreCase(HttpHeaders.Names.SET_COOKIE) || header[0].equalsIgnoreCase(HttpHeaders.Names.SET_COOKIE2)) {
                List<SetCookieHeaderValue> cookies = SetCookieHeaderValue.parse(header[1]);
                for (SetCookieHeaderValue cookie : cookies) {
                    response.addHeader(header[0], cookie.toString());
                }
            } else {
                response.addHeader(header[0], header[1]);
            }
        }
        byte[] content = forwardResponse.getBody();
        if (null != content) {
            ChannelBuffer bufer = ChannelBuffers.wrappedBuffer(content);
            response.setContent(bufer);
        }
        return response;
    }

    public static long[] parseContentRange(String value) {
        String left = value.substring(ContentRangeValueHeader.length()).trim();
        String[] split = left.split("/");
        String[] split2 = split[0].split("-");
        long[] ret = new long[3];
        ret[0] = Long.parseLong(split2[0].trim());
        ret[1] = Long.parseLong(split2[1].trim());
        ret[2] = Long.parseLong(split[1].trim());
        return ret;
    }

    public static boolean isCompleteResponse(HttpResponseExchange response) {
        String contentRange = response.getHeaderValue(HttpHeaders.Names.CONTENT_RANGE);
        if (null == contentRange) {
            return true;
        }
        long[] lens = parseContentRange(contentRange);
        if (lens[1] >= (lens[2] - 1)) {
            return true;
        }
        return false;
    }

    public static HttpServerAddress createHttpServerAddress(String appid) {
        return new HttpServerAddress(appid + ".appspot.com", Constants.HTTP_INVOKE_PATH, Config.getInstance().getClient2ServerConnectionMode().equals(ConnectionMode.HTTPS2GAE));
    }

    public static XmppAddress createXmppAddress(String appid) {
        return new XmppAddress(appid + "@appspot.com");
    }

    public static MasterNodeService getMasterNodeService(Config config) throws IOException, RpcException, XMPPException {
        if (null != master) {
            return master;
        }
        RPC rpc = null;
        if (config.getClient2ServerConnectionMode().equals(ConnectionMode.HTTP2GAE)) {
            rpc = ClientUtils.createHttpRPC(Executors.newCachedThreadPool());
            int oldtimeout = rpc.getSessionManager().getSessionTimeout();
            rpc.getSessionManager().setSessionTimeout(20000);
            try {
                master = rpc.getRemoteService(MasterNodeService.class, MasterNodeService.NAME, ClientUtils.createHttpServerAddress(Constants.MASTER_APPID));
            } catch (Rpctimeout e) {
            } finally {
                rpc.getSessionManager().setSessionTimeout(oldtimeout);
            }
        } else {
            if (null != config.getXmppAccounts()) {
                XmppAccount account = config.getXmppAccounts().get(0);
                rpc = ClientUtils.createXmppRPC(account, Executors.newCachedThreadPool());
                master = rpc.getRemoteService(MasterNodeService.class, MasterNodeService.NAME, ClientUtils.createXmppAddress(Constants.MASTER_APPID));
            }
        }
        return master;
    }

    public static RPC createHttpRPC(Executor workerExecutor) throws IOException, RpcException {
        Config config = Config.getInstance();
        DefaultCompressPreference.init(CompressorFactory.getRegistCompressor(config.getCompressor()).compressor);
        Properties initProps = new Properties();
        initProps.setProperty(RpcConstants.SESSIN_TIMEOUT, Integer.toString(config.getRpcTimeOut() * 1000));
        initProps.setProperty(RpcConstants.COMPRESS_PREFER, DefaultCompressPreference.class.getName());
        HttpClientRpcChannel httpCleintRpcchannle = new HttpClientRpcChannel(workerExecutor);
        return new RPC(httpCleintRpcchannle, initProps);
    }

    public static RPC createXmppRPC(XmppAccount account, Executor workerExecutor) throws IOException, RpcException, XMPPException {
        Config config = Config.getInstance();
        DefaultCompressPreference.init(CompressorFactory.getRegistCompressor(config.getCompressor()).compressor);
        Properties initProps = new Properties();
        initProps.setProperty(RpcConstants.SESSIN_TIMEOUT, Integer.toString(config.getRpcTimeOut() * 1000));
        initProps.setProperty(RpcConstants.COMPRESS_PREFER, DefaultCompressPreference.class.getName());
        XmppRpcChannel xmppRpcchannle = new XmppRpcChannel(workerExecutor, account);
        return new RPC(xmppRpcchannle, initProps);
    }

    public static String extractAppId(Address addr) {
        if (addr instanceof HttpServerAddress) {
            String host = ((HttpServerAddress) addr).getHost();
            return host.substring(0, host.indexOf('.'));
        }
        if (addr instanceof XmppAddress) {
            String jid = ((XmppAddress) addr).getJid();
            return jid.substring(0, jid.indexOf('@'));
        }
        return null;
    }

    public static boolean isHttpServerReachable(String appid, boolean viaGoogle) {
        HttpURLConnection conn = null;
        try {
            String host = appid + ".appspot.com";
            String server = "http://" + host;
            URL url = new URL(server);
            Proxy proxy = null;
            if (viaGoogle) {
                proxy = new Proxy(Type.HTTP, new InetSocketAddress(GoogleAvailableService.getInstance().getAvailableHttpService(), 80));
                conn = (HttpURLConnection) (url.openConnection(proxy));
            } else if (GoogleAvailableService.getInstance().getMappingHost(host) != host) {
                proxy = new Proxy(Type.HTTP, new InetSocketAddress(GoogleAvailableService.getInstance().getMappingHost(host), 80));
                conn = (HttpURLConnection) (url.openConnection(proxy));
            } else {
                conn = (HttpURLConnection) (url.openConnection());
            }
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (null != conn) {
                try {
                    conn.disconnect();
                } catch (Exception e2) {
                }
            }
        }
    }

    public static boolean isHttpsServerReachable(String appid) {
        String server = appid + ".appspot.com";
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(server, 443));
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public static String readFromStdin(boolean isEcho) throws IOException {
        if (isEcho) {
            return console.readLine().trim();
        }
        return new String(console.readPassword()).trim();
    }

    public static String httpMessage2String(HttpMessageExhange msg) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\r\n============================================\r\n");
        if (msg instanceof HttpResponseExchange) {
            int resCode = ((HttpResponseExchange) msg).responseCode;
            buffer.append(HttpResponseStatus.valueOf(resCode)).append("\r\n");
        } else {
            HttpRequestExchange req = (HttpRequestExchange) msg;
            buffer.append(req.method).append("  ").append(req.url).append("\r\n");
        }
        List<String[]> hs = msg.getHeaders();
        for (String[] header : hs) {
            buffer.append(header[0]).append(":").append(header[1]).append("\r\n");
        }
        buffer.append("============================================\r\n");
        return buffer.toString();
    }

    public static int selectDefaultGoogleProxy() {
        String value = Preferences.getPreferenceValue(DEFAULT_GOOGLE_PROXY_TYPE);
        int intValue = OVER_HTTPS;
        if (null != value) {
            intValue = Integer.parseInt(value);
        }
        switch(intValue) {
            case OVER_HTTP:
                {
                    if (setDefaultGoogleHttpProxy()) {
                        return OVER_HTTP;
                    }
                    break;
                }
            case OVER_HTTPS:
                {
                    if (setDefaultGoogleHttpsProxy()) {
                        return OVER_HTTPS;
                    }
                    break;
                }
            default:
                break;
        }
        return DIRECT;
    }

    public static boolean setDefaultGoogleHttpsProxy() {
        if (Config.getInstance().getHykProxyClientLocalProxy() != null || !Config.getInstance().getClient2ServerConnectionMode().equals(ConnectionMode.HTTP2GAE)) {
            return false;
        }
        return Config.getInstance().selectDefaultHttpsProxy();
    }

    public static boolean setDefaultGoogleHttpProxy() {
        if (Config.getInstance().getHykProxyClientLocalProxy() != null || !Config.getInstance().getClient2ServerConnectionMode().equals(ConnectionMode.HTTP2GAE)) {
            return false;
        }
        return Config.getInstance().selectDefaultHttpProxy();
    }

    public static void checkRemoteServer() {
        if (Config.getInstance().getHykProxyClientLocalProxy() != null || !Config.getInstance().getClient2ServerConnectionMode().equals(ConnectionMode.HTTP2GAE)) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Start check remote server reachable.");
        }
        List<HykProxyServerAuth> auths = Config.getInstance().getHykProxyServerAuths();
        if (auths.isEmpty()) {
            auths = new LinkedList<Config.HykProxyServerAuth>();
            HykProxyServerAuth master = new HykProxyServerAuth();
            master.appid = Constants.MASTER_APPID;
            auths.add(master);
        }
        for (HykProxyServerAuth auth : auths) {
            if (!ClientUtils.isHttpServerReachable(auth.appid, false)) {
                if (ClientUtils.isHttpServerReachable(auth.appid, true)) {
                    Preferences.setPrefernceValue(DEFAULT_GOOGLE_PROXY_TYPE, OVER_HTTP + "");
                } else {
                    Preferences.setPrefernceValue(DEFAULT_GOOGLE_PROXY_TYPE, OVER_HTTPS + "");
                    logger.error("Can NOT reach remote appengine server:" + auth.appid);
                }
                return;
            } else {
                break;
            }
        }
        Preferences.setPrefernceValue(DEFAULT_GOOGLE_PROXY_TYPE, DIRECT + "");
    }
}
