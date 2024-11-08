package net.sf.clairv.index.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transports the indices using a BFS copying approach.
 * TODO: add hard-link copying option.
 * 
 * @author qiuyin
 * 
 */
public class CopyTransporter extends Transporter {

    private static final Log log = LogFactory.getLog(CopyTransporter.class);

    public void transport(File file) throws TransportException {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    transport(file);
                }
            } else if (file.isFile()) {
                try {
                    FileChannel inChannel = new FileInputStream(file).getChannel();
                    FileChannel outChannel = new FileOutputStream(getOption("destination")).getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                } catch (IOException e) {
                    log.error("File transfer failed", e);
                }
            }
        }
    }
}
