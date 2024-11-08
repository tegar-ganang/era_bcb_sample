package util.gen;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

/**Class for writing out compressed files. Be sure to close() this to clear the final buffer!*/
public class Gzipper {

    private GZIPOutputStream out;

    private static final byte[] rtn = "\n".getBytes();

    public Gzipper(File gzipFile) throws FileNotFoundException, IOException {
        if (gzipFile.getName().endsWith(".gz") == false) gzipFile = new File(gzipFile + ".gz");
        out = new GZIPOutputStream(new FileOutputStream(gzipFile));
    }

    /**Be sure to call this to clear the final buffer when done!*/
    public void close() throws IOException {
        out.close();
    }

    /**Adds a return onto the line*/
    public void println(String line) throws IOException {
        out.write(line.getBytes());
        out.write(rtn);
    }

    public void print(String line) throws IOException {
        out.write(line.getBytes());
    }

    /**Adds a return onto each line*/
    public void println(ArrayList<String> lines) throws IOException {
        for (String line : lines) {
            out.write(line.getBytes());
            out.write(rtn);
        }
    }

    public void print(File file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[1024];
        int i;
        while ((i = in.read(buf)) >= 0) out.write(buf, 0, i);
        in.close();
    }
}
