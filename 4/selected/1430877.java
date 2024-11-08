package jgroups.jgroups;

import java.io.*;

public class StreamCopier {

    private static int BUFFER_LENGTH = 1024;

    private static int END_OF_FILE = -1;

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_LENGTH];
        int bytes_read;
        while ((bytes_read = in.read(buffer)) != END_OF_FILE) out.write(buffer, 0, bytes_read);
    }

    public static void copyStreamAndClose(InputStream in, OutputStream out) throws IOException {
        copyStream(in, out);
        in.close();
        out.close();
    }
}
