package dl.pushlog;

import static dl.pushlog.Functions.sleep;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import org.apache.log4j.Logger;

/**
 * @version $Id: JTail.java,v 1.1 2005/06/04 06:39:07 dirk Exp $
 * @author $Author: dirk $
 */
public class JTail {

    private static Logger log = Logger.getLogger(JTail.class);

    public static interface Callback {

        public void process(byte[] data);
    }

    public static class DefaultCallback implements Callback {

        public void process(byte[] data) {
            System.out.println(new String(data));
        }
    }

    private void doTailChannel(DefaultCallback callback, ReadableByteChannel chan) throws IOException {
        doTailInputStream(callback, Channels.newInputStream(chan));
    }

    private void doTailInputStream(DefaultCallback callback, InputStream is) throws IOException {
        final InputStreamReader isr = new InputStreamReader(is);
        final BufferedReader br = new BufferedReader(isr);
        do {
            String line;
            while (null != (line = br.readLine())) {
                log.debug("line: " + line);
                callback.process(line.getBytes());
            }
            sleep(1023);
        } while (true);
    }

    public void doTailFile(DefaultCallback callback, File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        try {
            long length = file.length();
            final long skip = length - 10 * 100;
            if (0 < skip) {
                inputStream.skip(skip);
            }
            doTailInputStream(callback, inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * @param av
     */
    public static void main(String[] av) {
        final String filename = av[0];
        JTail jtail = new JTail();
        try {
            final File file = new File(filename);
            final FileInputStream fis = new FileInputStream(filename);
            long length = file.length();
            final long skip = length - 5 * 80;
            if (0 < skip) {
                fis.skip(skip);
            }
            final FileChannel chan = fis.getChannel();
            jtail.doTailChannel(new DefaultCallback(), chan);
        } catch (Exception e) {
            System.err.println("failed: " + e.toString());
            log.debug(e.toString(), e);
        }
    }
}
