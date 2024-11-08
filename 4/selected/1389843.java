package photospace.vfs;

import java.io.*;
import org.apache.commons.io.*;
import org.apache.commons.logging.*;
import junit.framework.*;
import photospace.meta.*;

public class BrowserTest extends TestCase {

    private static final Log log = LogFactory.getLog(BrowserTest.class);

    FileSystem filesystem;

    FileSystemBrowser browser;

    public void setUp() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "BrowserTest");
        File subdir = new File(root, "subdir");
        subdir.mkdirs();
        File photo = new File(System.getProperty("project.root"), "build/test/exif-nordf.jpg");
        FileUtils.copyFileToDirectory(photo, root);
        FileUtils.copyFileToDirectory(photo, subdir);
        filesystem = new FileSystemImpl();
        filesystem.setRoot(root);
        PersisterImpl persister = new PersisterImpl();
        persister.setFilesystem(filesystem);
        persister.setTranslator(new Translator());
        browser = new FileSystemBrowser();
        browser.setFilesystem(filesystem);
        browser.setPersister(persister);
    }

    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(filesystem.getRoot());
    }

    public void testBrowse() throws Exception {
        FolderMeta dir = browser.browse("/", 1);
        assertFalse(dir.getFiles().length == 0);
        assertFalse(dir.getPhotos().length == 0);
        assertFalse(dir.getFolders().length == 0);
    }

    public void testBrowseShallow() throws Exception {
        FolderMeta dir = browser.browse("/", 0);
        assertTrue(dir.getFiles().length == 0);
    }

    public void testBrowseDeep() throws Exception {
        FolderMeta dir = browser.browse("/", -1);
        assertFalse(dir.getFiles().length == 0);
    }
}
