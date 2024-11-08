package cruise.umple.util;

import java.io.*;

public class Input {

    private InputStream stream;

    private BufferedReader reader;

    public Input(InputStream aStream) {
        stream = aStream;
        reader = new BufferedReader(new InputStreamReader(aStream));
    }

    public boolean setStream(InputStream aStream) {
        boolean wasSet = false;
        stream = aStream;
        wasSet = true;
        return wasSet;
    }

    public InputStream getStream() {
        return stream;
    }

    public void delete() {
    }

    public String readUmpleFile(String[] args, PrintStream writer) {
        if (args.length > 0) {
            return args[0];
        } else {
            writer.println("Please specify the file to compile:");
            return readLine();
        }
    }

    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            return "";
        }
    }
}
