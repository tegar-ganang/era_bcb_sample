package org.gvsig.tools.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import org.gvsig.tools.backup.exceptions.BackupException;

/**
 * <p>Performs a backup of a file, into another file at the same path (directory), with the file extension 
 *  changed to <i>.bak</i>.</p>
 *
 * @author Jose Ignacio Yarza (jiyarza@opensistemas.com)
 * @author Pablo Piqueras Bartolom� (pablo.piqueras@iver.es)
 */
public class DefaultBackupGenerator implements BackupGenerator {

    public void backup(File source) throws BackupException {
        try {
            int index = source.getAbsolutePath().lastIndexOf(".");
            if (index == -1) return;
            File dest = new File(source.getAbsolutePath().substring(0, index) + ".bak");
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dstChannel = new FileOutputStream(dest).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (Exception ex) {
            throw new BackupException(ex.getMessage(), ex, source);
        }
    }
}
