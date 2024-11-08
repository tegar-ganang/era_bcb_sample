package assays.com;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.HashMap;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public final class RequestParser {

    private HashMap userParameters;

    private UserHttpSession sessObj;

    private ServletInputStream sis;

    private FileOutputStream fos;

    private int head = 0;

    private int tail = 0;

    private int bufSize = 32768;

    private byte buffer[] = new byte[32768];

    private byte bValue[] = new byte[16334];

    private int startOfBlock = -1;

    private int del_l;

    public RequestParser(UserHttpSession tmpHttpSessObj) {
        sessObj = tmpHttpSessObj;
        userParameters = new HashMap();
    }

    public String getParameter(String name) {
        String value = (String) userParameters.get(name);
        return value;
    }

    public void setParameter(String name, String value) {
        userParameters.put(name, value);
    }

    private int readByte() throws IOException {
        if (head >= tail) {
            head = 0;
            tail = sis.read(buffer, head, bufSize);
            if (tail < 1) return -1;
        }
        return buffer[head++];
    }

    private int readByteOfFile() throws IOException {
        if (head == tail) {
            fos.write(buffer, startOfBlock, tail - startOfBlock - del_l);
            if (del_l > 0) System.arraycopy(buffer, tail - del_l, buffer, 0, del_l);
            head = del_l;
            startOfBlock = 0;
            tail = sis.read(buffer, head, bufSize - head);
            if (tail < 1) return -1;
            tail += head;
        }
        return head++;
    }

    private int readFirstByteOfFile(String name) throws IOException {
        File tempFile = (name.equals("datafile")) ? sessObj.newDataTempFile() : (name.equals("protocolfile")) ? sessObj.newProtocolTempFile() : (name.equals("parameterfile")) ? sessObj.newParameterTempFile() : sessObj.newZipTempFile();
        fos = new FileOutputStream(tempFile);
        startOfBlock = head;
        if (head == tail) {
            head = 0;
            startOfBlock = 0;
            tail = sis.read(buffer, 0, bufSize);
            if (tail < 1) return -1;
        }
        startOfBlock = head;
        return head++;
    }

    private void writeLastByteOfFile() throws IOException {
        if (head - del_l > startOfBlock) fos.write(buffer, startOfBlock, head - startOfBlock - del_l);
        startOfBlock = -1;
        fos.flush();
        fos.close();
    }

    public boolean parseRequest(HttpServletRequest request) throws IOException, ServletException {
        sis = request.getInputStream();
        StringWriter sw = new StringWriter();
        sw.write("\r\n");
        int i = readByte();
        for (; i != -1 && i != '\r'; i = readByte()) sw.write(i);
        String delimiter = sw.toString();
        int dellength = delimiter.length();
        readByte();
        while (true) {
            StringWriter h = new StringWriter();
            int[] temp = new int[4];
            temp[0] = readByte();
            temp[1] = readByte();
            temp[2] = readByte();
            h.write(temp[0]);
            h.write(temp[1]);
            h.write(temp[2]);
            for (temp[3] = readByte(); temp[3] != -1; temp[3] = readByte()) {
                if (temp[0] == '\r' && temp[1] == '\n' && temp[2] == '\r' && temp[3] == '\n') break;
                h.write(temp[3]);
                temp[0] = temp[1];
                temp[1] = temp[2];
                temp[2] = temp[3];
            }
            String header = h.toString();
            int startName = header.indexOf("name=\"");
            int endName = header.indexOf("\"", startName + 6);
            if (startName == -1 || endName == -1) break;
            String name = header.substring(startName + 6, endName);
            String value = "";
            startName = header.indexOf("filename=\"", endName + 1);
            if (startName > 0) {
                endName = header.indexOf("\"", startName + 10);
                value = header.substring(startName + 10, endName);
                int slash = value.lastIndexOf('\\');
                if (slash != -1) value = value.substring(slash + 1);
            }
            del_l = 0;
            if (!value.equals("")) {
                int ind = readFirstByteOfFile(name);
                while (ind != -1) {
                    if (buffer[ind] == delimiter.charAt(del_l)) {
                        if (++del_l == dellength) break;
                    } else if (del_l > 0) {
                        if (buffer[ind] == '\r') del_l = 1; else del_l = 0;
                    }
                    ind = readByteOfFile();
                }
                writeLastByteOfFile();
            } else {
                int b = readByte();
                int ib = 0;
                while (b != -1) {
                    if (b == delimiter.charAt(del_l)) {
                        if (++del_l == dellength) break;
                    } else if (del_l == 0) bValue[ib++] = (byte) b; else {
                        for (int ii = 0; ii < del_l; ii++) {
                            bValue[ib++] = (byte) delimiter.charAt(ii);
                        }
                        if (b == '\r') del_l = 1; else {
                            del_l = 0;
                            bValue[ib++] = (byte) b;
                        }
                    }
                    b = readByte();
                }
                value = new String(bValue, 0, ib, request.getCharacterEncoding());
            }
            if (!userParameters.containsKey(name)) userParameters.put(name, value);
            if (readByte() == '-' && readByte() == '-') break;
        }
        return true;
    }
}
