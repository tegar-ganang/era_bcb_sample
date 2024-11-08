package oqube.bytes.utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import junit.framework.TestCase;

public class TemporaryFSTest extends TestCase {

    public void testCtorWithCreateDirectory() {
        File tmp = new File("toto" + new Date().getTime());
        if (tmp.exists()) throw new IllegalStateException("Wannabe temp file already exists");
        TemporaryFS fs = new TemporaryFS(tmp);
        assertTrue("File not created", fs.root().exists());
        assertTrue("Not a directory", fs.root().isDirectory());
        assertTrue("Cannot write or read", fs.root().canWrite() && fs.root().canRead());
        tmp.delete();
    }

    public void testCtorWithDirectoryExists() {
        File tmp = new File("toto" + new Date().getTime());
        tmp.mkdir();
        TemporaryFS fs = new TemporaryFS(tmp);
        assertEquals("Wrong root directory", tmp, fs.root());
        tmp.delete();
    }

    public void testCtorWithFileExists() throws IOException {
        File tmp = new File("toto" + new Date().getTime());
        tmp.createNewFile();
        try {
            TemporaryFS fs = new TemporaryFS(tmp);
            fail("Should have thrown exception");
        } catch (Exception e) {
        } finally {
            tmp.delete();
        }
    }

    public void testCtorWithString() {
        File tmp = new File("toto" + new Date().getTime());
        if (tmp.exists()) throw new IllegalStateException("Wannabe temp file already exists");
        TemporaryFS fs = new TemporaryFS(tmp.getPath());
        assertTrue("File not created", fs.root().exists());
        assertTrue("Not a directory", fs.root().isDirectory());
        assertTrue("Cannot write or read", fs.root().canWrite() && fs.root().canRead());
        tmp.delete();
    }

    public void testCleanWithSubdirs() throws IOException {
        File tmp = new File("toto" + new Date().getTime());
        if (tmp.exists()) throw new IllegalStateException("Wannabe temp file already exists");
        TemporaryFS fs = new TemporaryFS(tmp);
        File d1 = new File(fs.root(), "tete");
        d1.mkdir();
        File f = new File(d1, "tutut");
        f.createNewFile();
        File d2 = new File(d1, "sub");
        d2.mkdir();
        f = new File(d2, "toto");
        f.createNewFile();
        fs.clean();
        assertTrue("Directory not wiped out", !tmp.exists());
    }
}
