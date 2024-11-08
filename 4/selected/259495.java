package com.intridea.io.vfs.provider.s3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.intridea.io.vfs.TestEnvirounment;

@Test(groups = { "storage" })
public class S3ProviderTest {

    private FileSystemManager fsManager;

    private String fileName, dirName, bucketName;

    private FileObject file, dir;

    private FileSystemOptions opts;

    @BeforeClass
    public void setUp() throws FileNotFoundException, IOException {
        Properties config = TestEnvirounment.getInstance().getConfig();
        fsManager = VFS.getManager();
        Random r = new Random();
        fileName = "vfs-file" + r.nextInt(1000);
        dirName = "vfs-dir" + r.nextInt(1000);
        bucketName = config.getProperty("s3.testBucket", "vfs-s3-tests");
    }

    @Test
    public void createFileOk() throws FileSystemException {
        file = fsManager.resolveFile("s3://vfs-s3-tests/test-place/" + fileName, opts);
        file.createFile();
        Assert.assertTrue(file.exists());
    }

    @Test(expectedExceptions = { FileSystemException.class })
    public void createFileFailed() throws FileSystemException {
        FileObject tmpFile = fsManager.resolveFile("s3://../new-mpoint/vfs-bad-file");
        tmpFile.createFile();
    }

    /**
	 * Create folder on already existed file
	 * @throws FileSystemException
	 */
    @Test(expectedExceptions = { FileSystemException.class }, dependsOnMethods = { "createFileOk" })
    public void createFileFailed2() throws FileSystemException {
        FileObject tmpFile = fsManager.resolveFile("s3://" + bucketName + "/test-place/" + fileName);
        tmpFile.createFolder();
    }

    @Test
    public void createDirOk() throws FileSystemException {
        dir = fsManager.resolveFile("s3://" + bucketName + "/test-place/" + dirName);
        dir.createFolder();
        Assert.assertTrue(dir.exists());
    }

    @Test(expectedExceptions = { FileSystemException.class })
    public void createDirFailed() throws FileSystemException {
        FileObject tmpFile = fsManager.resolveFile("s3://../new-mpoint/vfs-bad-dir");
        tmpFile.createFolder();
    }

    /**
	 * Create file on already existed folder
	 * @throws FileSystemException
	 */
    @Test(expectedExceptions = { FileSystemException.class }, dependsOnMethods = { "createFileOk" })
    public void createDirFailed2() throws FileSystemException {
        FileObject tmpFile = fsManager.resolveFile("s3://" + bucketName + "/test-place/" + dirName);
        tmpFile.createFile();
    }

    @Test
    public void exists() throws FileNotFoundException, IOException {
        FileObject existedDir = fsManager.resolveFile("s3://" + bucketName + "/test-place");
        Assert.assertTrue(existedDir.exists());
        FileObject nonExistedDir = fsManager.resolveFile(existedDir, "path/to/non/existed/dir");
        Assert.assertFalse(nonExistedDir.exists());
        FileObject existedFile = fsManager.resolveFile("s3://" + bucketName + "/jonny.zip");
        Assert.assertTrue(existedFile.exists());
        FileObject nonExistedFile = fsManager.resolveFile("s3://" + bucketName + "/ne/bыlo/i/net");
        Assert.assertFalse(nonExistedFile.exists());
    }

    @Test(dependsOnMethods = { "createFileOk" })
    public void upload() throws FileNotFoundException, IOException {
        FileObject dest = fsManager.resolveFile("s3://" + bucketName + "/test-place/backup.zip");
        if (dest.exists()) {
            dest.delete();
        }
        FileObject src = fsManager.resolveFile(new java.io.File("tests/files/backup.zip").getAbsolutePath());
        dest.copyFrom(src, Selectors.SELECT_SELF);
        Assert.assertTrue(dest.exists() && dest.getType().equals(FileType.FILE));
    }

    @Test(dependsOnMethods = { "getSize" })
    public void download() throws IOException {
        FileObject typica = fsManager.resolveFile("s3://" + bucketName + "/jonny.zip");
        File localCache = File.createTempFile("vfs.", ".s3-test");
        FileOutputStream out = new FileOutputStream(localCache);
        IOUtils.copy(typica.getContent().getInputStream(), out);
        Assert.assertEquals(localCache.length(), typica.getContent().getSize());
        localCache.delete();
    }

    @Test(dependsOnMethods = { "createFileOk", "createDirOk" })
    public void listChildren() throws FileSystemException {
        FileObject baseDir = fsManager.resolveFile(dir, "list-children-test");
        baseDir.createFolder();
        for (int i = 0; i < 5; i++) {
            FileObject tmpFile = fsManager.resolveFile(baseDir, i + ".tmp");
            tmpFile.createFile();
        }
        FileObject[] children = baseDir.getChildren();
        Assert.assertEquals(children.length, 5);
    }

    @Test(dependsOnMethods = { "createDirOk" })
    public void findFiles() throws FileSystemException {
        FileObject baseDir = fsManager.resolveFile(dir, "find-tests");
        baseDir.createFolder();
        fsManager.resolveFile(baseDir, "child-file.tmp").createFile();
        fsManager.resolveFile(baseDir, "child-file2.tmp").createFile();
        fsManager.resolveFile(baseDir, "child-dir").createFolder();
        fsManager.resolveFile(baseDir, "child-dir/descendant.tmp").createFile();
        fsManager.resolveFile(baseDir, "child-dir/descendant2.tmp").createFile();
        fsManager.resolveFile(baseDir, "child-dir/descendant-dir").createFolder();
        FileObject[] files;
        files = baseDir.findFiles(Selectors.SELECT_CHILDREN);
        Assert.assertEquals(files.length, 3);
        files = baseDir.findFiles(Selectors.SELECT_FOLDERS);
        Assert.assertEquals(files.length, 3);
        files = baseDir.findFiles(Selectors.SELECT_FILES);
        Assert.assertEquals(files.length, 4);
        files = baseDir.findFiles(Selectors.EXCLUDE_SELF);
        Assert.assertEquals(files.length, 6);
    }

    @Test(dependsOnMethods = { "createFileOk", "createDirOk" })
    public void getType() throws FileSystemException {
        FileObject imagine = fsManager.resolveFile(dir, "imagine-there-is-no-countries");
        Assert.assertEquals(imagine.getType(), FileType.IMAGINARY);
        Assert.assertEquals(dir.getType(), FileType.FOLDER);
        Assert.assertEquals(file.getType(), FileType.FILE);
    }

    @Test
    public void getContentType() throws FileSystemException {
        FileObject backup = fsManager.resolveFile("s3://" + bucketName + "/jonny.zip");
        Assert.assertEquals(backup.getContent().getContentInfo().getContentType(), "application/zip");
    }

    @Test
    public void getSize() throws FileSystemException {
        FileObject backup = fsManager.resolveFile("s3://" + bucketName + "/jonny.zip");
        Assert.assertEquals(backup.getContent().getSize(), 652292);
    }

    @Test(dependsOnMethods = { "findFiles" })
    public void delete() throws FileSystemException {
        FileObject testsDir = fsManager.resolveFile(dir, "find-tests");
        testsDir.delete(Selectors.EXCLUDE_SELF);
        FileObject[] files = testsDir.findFiles(Selectors.SELECT_ALL);
        Assert.assertEquals(files.length, 1);
    }

    @AfterClass
    public void tearDown() throws FileSystemException {
        FileObject vfsTestDir = fsManager.resolveFile(dir, "..");
        vfsTestDir.delete(Selectors.SELECT_ALL);
    }
}
