package org.apache.http.nio.entity;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ReadableByteChannel;
import org.apache.http.HttpEntity;
import org.apache.http.entity.FileEntity;

/**
 * An entity whose content is retrieved from from a file. In addition to the standard 
 * {@link HttpEntity} interface this class also implements NIO specific 
 * {@link HttpNIOEntity}.
 *
 * @deprecated Use {@link NFileEntity}
 * 
 * @version $Revision: 744570 $
 * 
 * @since 4.0
 */
@Deprecated
public class FileNIOEntity extends FileEntity implements HttpNIOEntity {

    public FileNIOEntity(final File file, final String contentType) {
        super(file, contentType);
    }

    public ReadableByteChannel getChannel() throws IOException {
        RandomAccessFile rafile = new RandomAccessFile(this.file, "r");
        return rafile.getChannel();
    }
}
