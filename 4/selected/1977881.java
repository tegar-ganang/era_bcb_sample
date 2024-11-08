package rtjdds.rtps.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import test.latency.PingPublisher;

public class NamedPipe extends File {

    private NamedPipe(String name) {
        super(name);
    }

    public FileChannel getInputChannel() throws FileNotFoundException {
        FileInputStream is = new FileInputStream(this);
        return is.getChannel();
    }

    public FileChannel getOutputChannel() throws FileNotFoundException {
        FileOutputStream os = new FileOutputStream(this);
        return os.getChannel();
    }

    public static NamedPipe createPipe(String name) throws IOException {
        Runtime.getRuntime().exec("mkfifo " + name);
        NamedPipe pipe = new NamedPipe(name);
        if (pipe.exists()) return new NamedPipe(name); else throw new IOException("Errors while creating the pipe");
    }
}
