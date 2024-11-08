package net.sf.zip.internal.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;
import junit.framework.TestCase;
import net.sf.zip.internal.Util;
import net.sf.zip.internal.ZipPlugin;
import net.sf.zip.internal.model.ArchiveFile;
import net.sf.zip.internal.model.IArchive;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class ZipFileTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testZipFile() throws Exception {
        ArchiveFile archive = new ArchiveFile(new ZipFile(ZipPlugin.getFileInPlugin(new Path("testresources/test.zip"))));
        IArchive[] children = archive.getChildren();
        assertEquals(1, children.length);
    }

    public void testAddFiles() throws Exception {
        File original = ZipPlugin.getFileInPlugin(new Path("testresources/test.zip"));
        File copy = new File(original.getParentFile(), "1test.zip");
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(original);
            out = new FileOutputStream(copy);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } finally {
            Util.close(in);
            Util.close(out);
        }
        ArchiveFile archive = new ArchiveFile(ZipPlugin.createArchive(copy.getPath()));
        archive.addFiles(new String[] { ZipPlugin.getFileInPlugin(new Path("testresources/add.txt")).getPath() }, new NullProgressMonitor());
        IArchive[] children = archive.getChildren();
        boolean found = false;
        for (IArchive child : children) {
            if (child.getLabel(IArchive.NAME).equals("add.txt")) found = true;
        }
        assertTrue(found);
        copy.delete();
    }
}
