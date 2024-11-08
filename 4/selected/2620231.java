package dl.pushlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @version $Id: FileChannelOpener.java,v 1.1 2005/06/08 16:02:50 dirk Exp $
 * @author $Author: dirk $
 */
public class FileChannelOpener implements ChannelOpener {

    public ReadableByteChannel openChannel(ChannelConfig config) throws IOException {
        File file = config.getFile();
        FileInputStream inputStream = new FileInputStream(file);
        long length = file.length();
        final long skip = length;
        if (0 < skip) {
            inputStream.skip(skip);
        }
        return inputStream.getChannel();
    }
}
