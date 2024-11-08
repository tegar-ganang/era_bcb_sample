package org.jgetfile.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import javolution.lang.TextBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jgetfile.crawler.link.LinkManager;
import org.jgetfile.crawler.manager.ObjectManager;
import org.jgetfile.crawler.util.JGetFileUtils;
import org.jgetfile.performance.PerformanceMonitor;

/**
 * 
 * @author Samuel Mendenhall
 *
 */
public class HttpFile extends RemoteFile {

    private static Logger logger = Logger.getLogger(HttpFile.class.getName());

    private static FileDownloadManager fileDownloadManager = FileDownloadManager.getInstance();

    private static LinkManager linkManager = LinkManager.getInstance();

    public HttpFile(String address) {
        super(address);
    }

    @Override
    public String getRelativeRemotePath(String address) {
        address = StringUtils.substringAfter(address, "http://");
        address = StringUtils.substringAfter(address, "/");
        address = "/" + address;
        return address;
    }

    @SuppressWarnings("static-access")
    @Override
    public void run() {
        SocketChannel channel = null;
        WritableByteChannel fileChannel = null;
        try {
            super.localFile = new File(localFileName);
            URL url = new URL(address);
            String host = url.getHost();
            int port = url.getPort();
            if (port == -1) {
                port = 80;
            }
            fileChannel = new FileOutputStream(localFileName).getChannel();
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetEncoder encoder = charset.newEncoder();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            channel = SocketChannel.open();
            channel.connect(socketAddress);
            TextBuilder relativeRemotePath = new TextBuilder(getRelativeRemotePath(address));
            String request = "GET " + relativeRemotePath + " HTTP/1.0\r\nHost: " + host + "\r\nuser-agent: " + linkManager.getUserAgent() + "\r\n\r\n";
            logger.debug(request);
            channel.write(encoder.encode(CharBuffer.wrap(request)));
            int numRead = 0;
            boolean hasHTTPHeader = false;
            boolean HTTPHeaderRemoved = false;
            boolean first = true;
            while ((numRead = channel.read(buffer)) != -1) {
                buffer.flip();
                if (first) {
                    if (numRead > 4) {
                        byte[] bts = { buffer.get(), buffer.get(), buffer.get(), buffer.get() };
                        numRead -= 4;
                        String probableHTTP = JGetFileUtils.toHex(bts);
                        hasHTTPHeader = probableHTTP.equals("0x48545450");
                    }
                }
                if (!HTTPHeaderRemoved) {
                    if (hasHTTPHeader) {
                        for (int i = 0; i < numRead; i++) {
                            if (JGetFileUtils.toHex(buffer.get()).equals("0x0d") && JGetFileUtils.toHex(buffer.get()).equals("0x0a") && JGetFileUtils.toHex(buffer.get()).equals("0x0d") && JGetFileUtils.toHex(buffer.get()).equals("0x0a")) {
                                HTTPHeaderRemoved = true;
                                break;
                            }
                        }
                    }
                }
                fileChannel.write(buffer);
                buffer.compact();
                first = false;
            }
            if (localFile.length() > 0) {
                PerformanceMonitor.incNumberDownloadedFiles();
            }
        } catch (Exception e) {
            if (ObjectManager.isVeryVerbose()) {
                logger.error(e.getMessage());
                logger.debug(e.getStackTrace());
            }
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                }
            }
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (fileDownloadManager.getDeleteFilesLessThan() != -1 && localFile.length() < fileDownloadManager.getDeleteFilesLessThan()) {
            localFile.delete();
        }
    }
}
