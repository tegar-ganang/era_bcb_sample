package net.sf.joafip.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net.sf.joafip.AbstractDeleteFileTestCase;
import net.sf.joafip.entity.EnumFilePersistenceCloseAction;
import net.sf.joafip.export_import.Container;

/**
 * test use a storage from release 2.2.2b*
 * 
 * @author luc peuvrier
 * 
 */
public class TestUseRuntime222 extends AbstractDeleteFileTestCase {

    protected FilePersistence filePersistence;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        copyFile("runtime222/all.flag", path);
        copyFile("runtime222/backup.flag", path);
        copyFile("runtime222/data.flag", path);
        copyFile("runtime222/store.bak", path);
        copyFile("runtime222/store.data", path);
        filePersistence = new FilePersistence(path, false, false);
    }

    private void copyFile(final String sourceFileName, final File path) throws IOException {
        final File source = new File(sourceFileName);
        final File destination = new File(path, source.getName());
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = new FileInputStream(source).getChannel();
            dstChannel = new FileOutputStream(destination).getChannel();
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
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            filePersistence.close();
        } catch (Throwable throwable) {
        }
        super.tearDown();
    }

    public void testUse222() throws FilePersistenceException, FilePersistenceClassNotFoundException, FilePersistenceInvalidClassException, FilePersistenceDataCorruptedException, FilePersistenceNotSerializableException {
        final DataAccessSession dataAccessSession = filePersistence.createDataAccessSession();
        dataAccessSession.open();
        Container container = (Container) dataAccessSession.getObject("container");
        assertTrue("bad state", container.checkState());
        container.work();
        dataAccessSession.close(EnumFilePersistenceCloseAction.SAVE);
        dataAccessSession.open();
        container = (Container) dataAccessSession.getObject("container");
        container.work();
        dataAccessSession.close(EnumFilePersistenceCloseAction.SAVE);
    }
}
