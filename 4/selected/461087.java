package net.solosky.maplefetion.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import net.solosky.maplefetion.net.AbstractTransfer;
import net.solosky.maplefetion.net.TransferException;
import net.solosky.maplefetion.net.buffer.ByteArrayWriter;
import net.solosky.maplefetion.net.buffer.ByteWriter;

/**
 *
 * HTTP方式传输信令
 *
 * @author solosky <solosky772@qq.com>
 */
public class HttpTransfer extends AbstractTransfer {

    private String ssic;

    private String pragma;

    private Thread runThead;

    private int requestId;

    private volatile boolean closeFlag;

    private String url;

    private BlockingQueue<BytesEntry> bytesEntryQueue;

    public static final int TYPE_INIT = 0;

    public static final int TYPE_ALIVE = 1;

    public static final int TYPE_DATA = 2;

    public HttpTransfer(String url, String ssic, String pragma) {
        this.ssic = ssic;
        this.url = url;
        this.pragma = pragma;
        this.requestId = 1;
        this.bytesEntryQueue = new LinkedBlockingQueue<BytesEntry>();
    }

    @Override
    protected void sendBytes(byte[] buff, int offset, int len) throws TransferException {
        this.bytesEntryQueue.add(new BytesEntry(buff, offset, len));
    }

    @Override
    public void startTransfer() throws TransferException {
        Runnable r = new Runnable() {

            public void run() {
                try {
                    ByteWriter writer = new ByteArrayWriter();
                    writer.write("SIPP".getBytes());
                    if (!tryExecuteRequest("i", requestId++, writer, 1)) raiseException(new TransferException("Init Http Transfer failed.."));
                    while (!closeFlag) {
                        writer.clear();
                        BytesEntry entry = bytesEntryQueue.poll(5, TimeUnit.SECONDS);
                        if (entry != null) {
                            writer.writeBytes(entry.getBytes(), entry.getOffset(), entry.getLength());
                            while (bytesEntryQueue.size() > 0) {
                                entry = bytesEntryQueue.poll();
                                writer.writeBytes(entry.getBytes(), entry.getOffset(), entry.getLength());
                            }
                        }
                        writer.write("SIPP".getBytes());
                        if (!tryExecuteRequest("s", requestId++, writer, 3)) {
                            closeFlag = true;
                            raiseException(new TransferException());
                        }
                    }
                    writer.clear();
                    writer.write("SIPP".getBytes());
                    tryExecuteRequest("d", requestId++, writer, 1);
                } catch (Throwable e) {
                    raiseException(new TransferException(e));
                }
            }
        };
        this.runThead = new Thread(r);
        this.runThead.setName(this.getTransferName());
        this.runThead.start();
    }

    @Override
    public void stopTransfer() throws TransferException {
        this.closeFlag = true;
    }

    /**
     * 尝试发起请求，如果失败继续尝试，直到超过指定的尝试次数为止
     * @param writer
     * @param maxRetryTimes
     * @return 执行成功返回true或尝试超过指定次数返回false
     */
    private boolean tryExecuteRequest(String s, int i, ByteWriter writer, int maxRetryTimes) {
        int nowRetryTimes = 0;
        while (nowRetryTimes < maxRetryTimes) {
            try {
                this.executeRequest(s, i, writer);
                return true;
            } catch (IOException e) {
                nowRetryTimes++;
            }
        }
        return nowRetryTimes < maxRetryTimes;
    }

    /**
     * 执行一个请求
     * @param s
     * @param i
     * @param writer
     * @throws IOException
     */
    private void executeRequest(String s, int i, ByteWriter writer) throws IOException {
        String turl = this.url + "?";
        turl += "t=" + s;
        turl += "&i=" + Integer.toString(i);
        URL realURL = new URL(turl);
        HttpURLConnection connection = (HttpURLConnection) realURL.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "IIC2.0/PC 3.5.2540");
        connection.setRequestProperty("Pragma", this.pragma);
        connection.setRequestProperty("Content-Type", "application/oct-stream");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Cookie", "ssic=" + this.ssic);
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        out.write(writer.toByteArray(), 0, writer.size());
        out.flush();
        if (connection.getResponseCode() == 200) {
            int contentLength = connection.getContentLength();
            if (contentLength > 4) {
                InputStream in = connection.getInputStream();
                writer.clear();
                while (contentLength > 4) {
                    writer.writeByte(in.read());
                    contentLength--;
                }
                this.bytesRecived(writer.toByteArray(), 0, writer.size());
            }
        } else {
            throw new IOException("Invalid response stateCode=" + connection.getResponseCode());
        }
    }

    private class BytesEntry {

        private byte[] bytes;

        private int offset;

        private int length;

        public BytesEntry(byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }

        public byte[] getBytes() {
            return this.bytes;
        }

        public int getOffset() {
            return this.offset;
        }

        public int getLength() {
            return this.length;
        }
    }

    @Override
    public String getTransferName() {
        return "HttpTransfer - " + this.url;
    }
}
