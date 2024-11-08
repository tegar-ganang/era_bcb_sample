package com.volantis.mcs.accessors.xml;

import com.volantis.synergetics.io.IOUtils;
import com.volantis.synergetics.testtools.TestCaseAbstract;
import com.volantis.synergetics.testtools.io.ResourceTemporaryFileCreator;
import com.volantis.synergetics.testtools.io.TemporaryFileExecutor;
import com.volantis.synergetics.testtools.io.TemporaryFileManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Test the {@link ZipArchive} class.
 * 
 * @todo test zip file timestamps as well.
 */
public class ZipArchiveTestCase extends TestCaseAbstract {

    /**
     * The copyright statement.
     */
    private static String mark = "(c) Volantis Systems Ltd 2003.";

    /** 
     * The name of file 1 in the test archive.
     */
    private static final String ARCHIVE_FILE_1 = "existing-1.txt";

    /**
     * The contents of file 1 in the test archive.
     */
    private static final String ARCHIVE_FILE_1_CONTENT = "a small text file\n";

    /**
     * The number of files in the test archive.
     */
    private static final int ARCHIVE_FILE_COUNT = 2;

    /**
     * A temporary file manager for temporary copies of archive.zip extracted 
     * from our jar file.
     */
    private TemporaryFileManager archiveFileManager = new TemporaryFileManager(new ResourceTemporaryFileCreator(ZipArchiveTestCase.class, "archive.zip"));

    public void testArchiveNonExistant() throws Exception {
        new ZipArchive("non existant archive");
    }

    public void testReadNonExistant() throws Exception {
        archiveFileManager.executeWith(new TemporaryFileExecutor() {

            public void execute(File temporaryFile) throws Exception {
                ZipArchive archive = new ZipArchive(temporaryFile.getPath());
                InputStream input = archive.getInputFrom("non existant file");
                if (input != null) {
                    fail("opened non existant file");
                }
            }
        });
    }

    public void testReadNormal() throws Exception {
        archiveFileManager.executeWith(new TemporaryFileExecutor() {

            public void execute(File temporaryFile) throws Exception {
                ZipArchive archive = new ZipArchive(temporaryFile.getPath());
                InputStream input = archive.getInputFrom(ARCHIVE_FILE_1);
                if (input != null) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    IOUtils.copyAndClose(input, output);
                    assertEquals(ARCHIVE_FILE_1 + " contents not correct", ARCHIVE_FILE_1_CONTENT, output.toString());
                } else {
                    fail("cannot open " + ARCHIVE_FILE_1);
                }
            }
        });
    }

    public void testWrite() throws Exception {
        archiveFileManager.executeWith(new TemporaryFileExecutor() {

            public void execute(File temporaryFile) throws Exception {
                String fileValue = "newly written content of file\n";
                ZipArchive archive = new ZipArchive(temporaryFile.getPath());
                OutputStream output = archive.getOutputTo(ARCHIVE_FILE_1);
                if (output != null) {
                    output.write(fileValue.getBytes());
                    output.close();
                    archive.save();
                } else {
                    fail("cannot open " + ARCHIVE_FILE_1);
                }
                ZipFile zip = new ZipFile(temporaryFile.getPath());
                try {
                    assertEquals("zip contains wrong number of files", ARCHIVE_FILE_COUNT, zip.size());
                    String actual = extractFileFromZip(zip, ARCHIVE_FILE_1);
                    assertEquals(ARCHIVE_FILE_1 + " in zip is not updated", fileValue, actual);
                } finally {
                    zip.close();
                }
            }
        });
    }

    public void testCreate() throws Exception {
        archiveFileManager.executeWith(new TemporaryFileExecutor() {

            public void execute(File temporaryFile) throws Exception {
                String fileValue = "contents of newly created file\n";
                ZipArchive archive = new ZipArchive(temporaryFile.getPath());
                String newFileName = "created.txt";
                OutputStream output = archive.getOutputTo(newFileName);
                if (output != null) {
                    output.write(fileValue.getBytes());
                    output.close();
                    archive.save();
                } else {
                    fail("cannot open " + newFileName);
                }
                ZipFile zip = new ZipFile(temporaryFile.getPath());
                try {
                    assertEquals("zip contains wrong number of files", ARCHIVE_FILE_COUNT + 1, zip.size());
                    String actual = extractFileFromZip(zip, newFileName);
                    assertEquals(newFileName + " in zip not created properly", fileValue, actual);
                } finally {
                    zip.close();
                }
            }
        });
    }

    public void testDelete() throws Exception {
        archiveFileManager.executeWith(new TemporaryFileExecutor() {

            public void execute(File temporaryFile) throws Exception {
                ZipArchive archive = new ZipArchive(temporaryFile.getPath());
                boolean deleted = archive.delete(ARCHIVE_FILE_1);
                archive.save();
                assertTrue("cannot delete " + ARCHIVE_FILE_1, deleted);
                ZipFile zip = new ZipFile(temporaryFile.getPath());
                try {
                    assertEquals("zip contains wrong number of files", ARCHIVE_FILE_COUNT - 1, zip.size());
                } finally {
                    zip.close();
                }
            }
        });
    }

    public void testRename() throws Exception {
        archiveFileManager.executeWith(new TemporaryFileExecutor() {

            public void execute(File temporaryFile) throws Exception {
                ZipArchive archive = new ZipArchive(temporaryFile.getPath());
                String newFileName = "created.txt";
                boolean renamed = archive.rename(ARCHIVE_FILE_1, newFileName);
                archive.save();
                assertTrue("cannot rename " + ARCHIVE_FILE_1, renamed);
                ZipFile zip = new ZipFile(temporaryFile.getPath());
                try {
                    assertEquals("zip contains wrong number of files", ARCHIVE_FILE_COUNT, zip.size());
                    String actual = extractFileFromZip(zip, newFileName);
                    assertEquals(newFileName + " in zip not created properly", ARCHIVE_FILE_1_CONTENT, actual);
                } finally {
                    zip.close();
                }
            }
        });
    }

    /**
     * Return the contents of a file inside a zip file as a string.
     * <p>
     * NOTE: this is only guaranteed to work properly for for ASCII file 
     * content, since it ignores encoding issues.
     * 
     * @param zip the zip file to extract a file from.
     * @param fileName the name of the file inside the zip file to extract.
     * @return the file extracted as a string.
     * @throws IOException if there was a read problem with the zip file.
     */
    private String extractFileFromZip(ZipFile zip, String fileName) throws IOException {
        String contents = null;
        ZipEntry entry = zip.getEntry(fileName);
        if (entry != null) {
            InputStream input = zip.getInputStream(entry);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            IOUtils.copyAndClose(input, buffer);
            contents = buffer.toString();
        }
        return contents;
    }
}
