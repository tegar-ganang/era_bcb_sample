package com.cirnoworks.http.utils;

import com.cirnoworks.http.utils.exception.BadRequestException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Cloudee
 */
public class HTTPUtils {

    public static final byte[] CRLF = { '\r', '\n' };

    public static final Charset ISO8859_1 = Charset.forName("ISO8859_1");

    public static Socket connRemote(String target, int port, String firstLine) throws IOException {
        Socket s = new Socket(target, port);
        OutputStream os = s.getOutputStream();
        os.write(firstLine.getBytes(ISO8859_1));
        os.write(CRLF);
        return s;
    }

    public static void sendHead(OutputStream os, Header param) throws IOException {
        String key;
        Set<Entry<String, ArrayList<String>>> entries = param.entrySet();
        for (Entry<String, ArrayList<String>> entry : entries) {
            key = entry.getKey();
            for (String value : entry.getValue()) {
                os.write(key.getBytes(ISO8859_1));
                os.write(':');
                os.write(value.getBytes(ISO8859_1));
                os.write(CRLF);
            }
        }
        os.write(CRLF);
    }

    public static boolean deliveryEntity(Header param, InputStream is, OutputStream os, boolean keepalive) {
        return deliveryEntity(param, is, os, DummyOutputStream.getInstance(), keepalive);
    }

    /**
     * ����Post��Ϣ
     * @param param httpͷ��Ϣ
     * @param is ��ȡPOST��Ϣ��Դ
     * @param os ��POST��Ϣ���õ�Ŀ��
     * @param oss ���ڱ�����Ϣ�������
     * @return �������Ƿ񱣳����ӣ�������Connection=closed��
     */
    public static boolean deliveryEntity(Header param, InputStream is, OutputStream os, OutputStream oss, boolean keepalive) {
        boolean ret = keepalive;
        try {
            String te = param.getLastValue("Transfer-Encoding");
            byte[] buf = new byte[4096];
            if (te == null || te.toLowerCase().equals("identity")) {
                String sSize = param.getLastValue("Content-Length");
                if (sSize == null) {
                    if (ret) {
                        return true;
                    } else {
                        ret = false;
                        int size;
                        try {
                            while ((size = is.read(buf)) >= 0) {
                                os.write(buf, 0, size);
                                oss.write(buf, 0, size);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    int size = Integer.parseInt(sSize);
                    int read;
                    try {
                        while (size > 0) {
                            if (size > 4096) {
                                read = is.read(buf);
                            } else {
                                read = is.read(buf, 0, size);
                            }
                            if (read < 0) {
                                throw new IOException("Unexpected stream close.");
                            }
                            size -= read;
                            os.write(buf, 0, read);
                            oss.write(buf, 0, read);
                        }
                    } catch (IOException e) {
                        ret = false;
                    }
                }
            } else {
                System.err.println(Thread.currentThread().hashCode() + "Condiction 3");
                try {
                    while (true) {
                        String sLength = readLine(is);
                        if (sLength == null) {
                            return false;
                        }
                        os.write(sLength.getBytes(ISO8859_1));
                        os.write(CRLF);
                        if (sLength.length() == 0) {
                            return true;
                        }
                        int idx = sLength.indexOf(" ");
                        if (idx >= 0) {
                            sLength = sLength.substring(idx);
                        }
                        int length = Integer.parseInt(sLength, 16);
                        if (length == 0) {
                            break;
                        } else {
                            length += 2;
                            int read;
                            while (length > 0) {
                                if (length > 4096) {
                                    read = is.read(buf);
                                } else {
                                    read = is.read(buf, 0, length);
                                }
                                length -= read;
                                os.write(buf, 0, read);
                                oss.write(buf, 0, read);
                            }
                        }
                    }
                    String line;
                    while (true) {
                        line = HTTPUtils.readLine(is);
                        if (line == null) {
                            ret = false;
                            break;
                        }
                        os.write(line.getBytes(ISO8859_1));
                        os.write(HTTPUtils.CRLF);
                        if (line.length() == 0) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ret = false;
                }
            }
        } finally {
            try {
                os.flush();
            } catch (IOException ex) {
            }
        }
        return ret;
    }

    public static String readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        int i;
        try {
            while (true) {
                i = is.read();
                if (i == '\n') {
                    break;
                }
                if (i < 0 && baos.size() == 0) {
                    return null;
                }
                if (i == '\r') {
                    continue;
                }
                baos.write(i);
            }
        } catch (SocketException se) {
            se.printStackTrace();
            return null;
        }
        return baos.toString("ISO8859_1");
    }

    /**
     * ��ȡHTTP����ͷ
     * @param is ��Դ��
     * @param storage ��Ų����Map
     * @throws IOException ������ȡ�������������
     */
    public static void readHeader(InputStream is, Header storage) throws IOException {
        String line;
        while (true) {
            line = HTTPUtils.readLine(is);
            if (line == null) {
                throw new IOException();
            }
            if (line.equals("")) {
                break;
            }
            int idx = line.indexOf(":");
            if (idx <= 0 || idx >= line.length() - 1) {
                throw new IOException("Bad header line [" + line + "]");
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            storage.add(key, value);
        }
    }

    public static void readFirstLineRequest(InputStream is, HeaderRequest storage) throws BadRequestException {
        String line;
        try {
            line = HTTPUtils.readLine(is);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        if (line == null) {
            throw new BadRequestException("Connection lost.");
        }
        String[] param = line.split(" ");
        if (param.length != 3) {
            throw new BadRequestException("Request-Line too many/less SPs." + param.length + " " + line);
        }
        storage.setMethod(param[0]);
        storage.setUrl(param[1]);
        storage.setProtocal(param[2]);
    }

    public static void readHeaderResponse(InputStream is, HeaderResponse storage) throws IOException {
        String line = HTTPUtils.readLine(is);
        if (line == null) {
            throw new IOException("");
        }
        String[] split = line.split(" ", 3);
        if (split.length != 3) {
            throw new IOException(line);
        }
        if (!split[0].startsWith("HTTP")) {
            throw new IOException(line);
        }
        try {
            Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            throw new IOException(line);
        }
        storage.setProtocal(split[0]);
        storage.setCode(split[1]);
        storage.setReason(split[2]);
        readHeader(is, storage);
    }

    public static void copy(InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = src.read(buffer)) >= 0) {
            dest.write(buffer, 0, read);
        }
    }
}
