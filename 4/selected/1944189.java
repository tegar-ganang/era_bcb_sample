package net.sf.joafip.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net.sf.joafip.AbstractDeleteFileTestCase;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.StorableAccess;
import net.sf.joafip.TestException;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
@StorableAccess
public abstract class AbstractCopyRuntime extends AbstractDeleteFileTestCase {

    public AbstractCopyRuntime() throws TestException {
        super();
    }

    public AbstractCopyRuntime(final String name) throws TestException {
        super(name);
    }

    protected void copyFile(final String sourceFileName, final File path) throws IOException {
        final File source = new File(sourceFileName);
        final File destination = new File(path, source.getName());
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(source);
            srcChannel = fileInputStream.getChannel();
            fileOutputStream = new FileOutputStream(destination);
            dstChannel = fileOutputStream.getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            try {
                if (dstChannel != null) {
                    dstChannel.close();
                }
            } catch (Exception exception) {
            }
            try {
                if (srcChannel != null) {
                    srcChannel.close();
                }
            } catch (Exception exception) {
            }
            try {
                fileInputStream.close();
            } catch (Exception exception) {
            }
            try {
                fileOutputStream.close();
            } catch (Exception exception) {
            }
        }
    }
}
