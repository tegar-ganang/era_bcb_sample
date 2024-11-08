package ubersoldat.net;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import ubersoldat.data.Constants;
import ubersoldat.io.SoldatInputStream;

public class FileExchangeHelper {

    private static String endOfLine = "\r\n";

    public static String readRequest(SoldatInputStream in) throws IOException {
        String filename = in.readLine();
        return filename;
    }

    public static void writeRequest(BufferedOutputStream out, String filename) throws IOException {
        String data = filename + endOfLine;
        out.write(data.getBytes(Constants.DefaultCharset));
        out.flush();
    }

    public static byte[] readRequestResponse(SoldatInputStream in) throws IOException {
        in.readLine();
        in.readLine();
        in.skip(4L);
        byte[] buf = new byte[1000];
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        int read;
        while ((read = in.read(buf)) != -1) {
            tmp.write(buf, 0, read);
        }
        ByteArrayOutputStream tmp2 = new ByteArrayOutputStream();
        tmp2.write(tmp.toByteArray(), 0, tmp.size() - ("ENDFILES" + endOfLine).length());
        return tmp2.toByteArray();
    }

    public static void writeRequestResponse(BufferedOutputStream out, String filename, byte[] content) throws IOException {
        DataOutputStream dout = new DataOutputStream(out);
        String data = "STARTFILES" + endOfLine;
        data += filename + endOfLine;
        dout.write(data.getBytes(Constants.DefaultCharset));
        dout.writeInt(content.length);
        dout.write(content);
        data = "ENDFILES" + endOfLine;
        dout.write(data.getBytes(Constants.DefaultCharset));
        dout.flush();
    }
}
