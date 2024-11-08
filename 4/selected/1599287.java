package idv.cloudee.proxy2.impl.connect;

import com.cirnoworks.http.utils.Header;
import com.cirnoworks.http.utils.HTTPUtils;
import com.cirnoworks.http.utils.HeaderRequest;
import com.cirnoworks.http.utils.exception.BadRequestException;
import idv.cloudee.proxy2.framework.ProxyServer;
import idv.cloudee.proxy2.framework.ProxySession;
import idv.cloudee.proxy2.intf.RequestDealer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Cloudee
 */
public class ConnectDealer implements RequestDealer {

    private boolean running;

    private final Object runningLock = new Object();

    private Socket s;

    private String proxyAddress;

    private int proxyPort;

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean dealRequest(HeaderRequest reqHeaderParam, Socket conn, ProxySession session) throws BadRequestException {
        try {
            String url = reqHeaderParam.getUrl();
            String method = reqHeaderParam.getMethod();
            String protocol = reqHeaderParam.getProtocal();
            Header param = new Header();
            HTTPUtils.readHeader(conn.getInputStream(), param);
            int port = 80;
            String sport = "80";
            int idx = url.indexOf(":");
            if (idx > 0) {
                sport = url.substring(idx + 1);
                url = url.substring(0, idx);
                port = Integer.parseInt(sport);
            }
            if (proxyAddress == null) {
                s = new Socket(url, port);
                conn.getOutputStream().write("HTTP/1.0 200 Connection established".getBytes("ISO8859-1"));
                conn.getOutputStream().write(HTTPUtils.CRLF);
                conn.getOutputStream().write(HTTPUtils.CRLF);
            } else {
                s = new Socket(proxyAddress, proxyPort);
                s.getOutputStream().write(method.getBytes("ISO8859-1"));
                s.getOutputStream().write(' ');
                s.getOutputStream().write(url.getBytes("ISO8859-1"));
                s.getOutputStream().write(':');
                s.getOutputStream().write(sport.getBytes("ISO8859-1"));
                s.getOutputStream().write(' ');
                s.getOutputStream().write(protocol.getBytes("ISO8859-1"));
                s.getOutputStream().write(HTTPUtils.CRLF);
                HTTPUtils.sendHead(s.getOutputStream(), param);
                String feedBack = HTTPUtils.readLine(s.getInputStream());
                conn.getOutputStream().write(feedBack.getBytes("ISO8859-1"));
                conn.getOutputStream().write(HTTPUtils.CRLF);
                Header responseParam = new Header();
                HTTPUtils.readHeader(s.getInputStream(), responseParam);
                HTTPUtils.sendHead(conn.getOutputStream(), responseParam);
            }
            InputStream is = conn.getInputStream();
            OutputStream os = s.getOutputStream();
            InputStream rspis = s.getInputStream();
            OutputStream rspos = conn.getOutputStream();
            Pipe req = new Pipe(is, os);
            Pipe rsp = new Pipe(rspis, rspos);
            running = true;
            synchronized (runningLock) {
                ProxyServer.submitThread(rsp);
                ProxyServer.submitThread(req);
                while (running) {
                    runningLock.wait();
                }
                req.close();
                rsp.close();
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        return false;
    }

    class Pipe implements Runnable {

        InputStream is;

        OutputStream os;

        public Pipe(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        public void close() {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            byte[] buf = new byte[4096];
            int read;
            try {
                while (true) {
                    read = is.read(buf);
                    if (read < 0) {
                        break;
                    }
                    os.write(buf, 0, read);
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
                synchronized (runningLock) {
                    running = false;
                    runningLock.notifyAll();
                }
            }
        }
    }
}
