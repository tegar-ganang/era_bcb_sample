package edu.rice.cs.util;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import edu.rice.cs.drjava.DrJavaTestCase;
import edu.rice.cs.plt.concurrent.JVMBuilder;
import edu.rice.cs.util.FileOps;

/** Test cases for {@link FileOps}.
  * @version $Id: FileOpsTest.java 5175 2010-01-20 08:46:32Z mgricken $
  */
@SuppressWarnings("deprecation")
public class FileOpsTest extends DrJavaTestCase {

    private static final Log _log = new Log("FileOpsTest.txt", false);

    public static final String TEXT = "hi, dude.";

    public static final String PREFIX = "prefix";

    public static final String SUFFIX = ".suffix";

    public void testCreateTempDirectory() throws IOException {
        File dir = FileOps.createTempDirectory(PREFIX);
        try {
            assertTrue("createTempDirectory result is a directory", dir.isDirectory());
            assertTrue("temp directory has correct prefix", dir.getName().startsWith(PREFIX));
        } finally {
            assertTrue("delete directory", dir.delete());
        }
    }

    public void testReadAndWriteTempFile() throws IOException {
        File file = FileOps.writeStringToNewTempFile(PREFIX, SUFFIX, TEXT);
        try {
            assertTrue("temp file has correct prefix", file.getName().startsWith(PREFIX));
            assertTrue("temp file has correct suffix", file.getName().endsWith(SUFFIX));
            String read = FileOps.readFileAsString(file);
            assertEquals("contents after read", TEXT, read);
        } finally {
            assertTrue("delete file", file.delete());
        }
    }

    public void testRecursiveDirectoryDelete() throws IOException {
        final File baseDir = FileOps.createTempDirectory(PREFIX);
        File parentDir = baseDir;
        boolean ret;
        for (int i = 0; i < 5; i++) {
            File subdir = new File(parentDir, "subdir" + i);
            ret = subdir.mkdir();
            assertTrue("create directory " + subdir, ret);
            for (int j = 0; j < 2; j++) {
                File file = new File(parentDir, "file" + i + "-" + j);
                FileOps.writeStringToFile(file, "Some text for file " + file.getAbsolutePath());
                assertTrue(file + " exists", file.exists());
            }
            parentDir = subdir;
        }
        ret = FileOps.deleteDirectory(baseDir);
        assertTrue("delete directory result", ret);
        assertEquals("directory exists after deleting it", false, baseDir.exists());
    }

    /** This method checks that backups are made correctly, that when a save fails,
   * no data is lost, and that when a save is attempted on a write-protected file,
   * the save fails (bug #782963).
   */
    public void testSaveFile() throws IOException {
        File writeTo = File.createTempFile("fileops", ".test").getCanonicalFile();
        writeTo.deleteOnExit();
        File backup = new File(writeTo.getPath() + "~");
        FileOps.saveFile(new FileOps.DefaultFileSaver(writeTo) {

            public void saveTo(OutputStream os) throws IOException {
                String output = "version 1";
                os.write(output.getBytes());
            }

            public boolean shouldBackup() {
                return false;
            }
        });
        assertEquals("save w/o backup", "version 1", FileOps.readFileAsString(writeTo));
        assertEquals("save w/o backup did not backup", false, backup.exists());
        FileOps.saveFile(new FileOps.DefaultFileSaver(writeTo) {

            public void saveTo(OutputStream os) throws IOException {
                String output = "version 2";
                os.write(output.getBytes());
            }
        });
        assertEquals("save2 w backup", "version 2", FileOps.readFileAsString(writeTo));
        assertEquals("save2 w backup did backup", "version 1", FileOps.readFileAsString(backup));
        FileOps.saveFile(new FileOps.DefaultFileSaver(writeTo) {

            public void saveTo(OutputStream os) throws IOException {
                String output = "version 3";
                os.write(output.getBytes());
            }
        });
        assertEquals("save3 w backup on", "version 3", FileOps.readFileAsString(writeTo));
        assertEquals("save3 w backup on did not backup", "version 1", FileOps.readFileAsString(backup));
        try {
            FileOps.saveFile(new FileOps.DefaultFileSaver(writeTo) {

                public void saveTo(OutputStream os) throws IOException {
                    String output = "version 4";
                    os.write(output.getBytes());
                    throw new IOException();
                }
            });
            fail("IOException not propagated");
        } catch (IOException ioe) {
        }
        assertEquals("failed save4 w/o backup", "version 3", FileOps.readFileAsString(writeTo));
        assertEquals("failed save4 w/o backup check original backup", "version 1", FileOps.readFileAsString(backup));
        try {
            FileOps.saveFile(new FileOps.DefaultFileSaver(writeTo) {

                public boolean shouldBackup() {
                    return true;
                }

                public void saveTo(OutputStream os) throws IOException {
                    String output = "version 5";
                    os.write(output.getBytes());
                    throw new IOException();
                }
            });
            fail("IOException not propagated spot 2");
        } catch (IOException ioe) {
        }
        assertEquals("failed save5 w backup", "version 3", FileOps.readFileAsString(writeTo));
        try {
            FileOps.readFileAsString(backup);
            fail("The backup file should no longer exist.");
        } catch (FileNotFoundException e) {
        }
        writeTo.setReadOnly();
        try {
            FileOps.saveFile(new FileOps.DefaultFileSaver(writeTo) {

                public boolean shouldBackup() {
                    return true;
                }

                public void saveTo(OutputStream os) throws IOException {
                    String output = "version 6";
                    os.write(output.getBytes());
                }
            });
            fail("The file to be saved was read-only!");
        } catch (IOException ioe) {
        }
        assertEquals("failed save6 w backup", "version 3", FileOps.readFileAsString(writeTo));
        try {
            FileOps.readFileAsString(backup);
            fail("The backup file should no longer exist.");
        } catch (FileNotFoundException e) {
        }
    }

    /** This tests that packageExplore correctly runs through and returns
   * non-empty packages
   */
    public void testPackageExplore() throws IOException {
        File rootDir = FileOps.createTempDirectory("fileOpsTest");
        File subDir0 = new File(rootDir, "sub0");
        subDir0.mkdir();
        File subDir1 = new File(rootDir, "sub1");
        subDir1.mkdir();
        File subsubDir0 = new File(subDir0, "subsub0");
        subsubDir0.mkdir();
        File javasubsub = new File(subsubDir0, "aclass.java");
        FileOps.writeStringToFile(javasubsub, "contents of this file are unimportant");
        File javasub1 = new File(subDir1, "myclass.java");
        FileOps.writeStringToFile(javasub1, "this file is pretty much empty");
        File javaroot = new File(rootDir, "someclass.java");
        FileOps.writeStringToFile(javaroot, "i can write anything i want here");
        LinkedList<String> packages = FileOps.packageExplore("hello", rootDir);
        assertEquals("package count a", 3, packages.size());
        assertTrue("packages contents a0", packages.contains("hello.sub0.subsub0"));
        assertTrue("packages contents a1", packages.contains("hello.sub1"));
        assertTrue("packages contents a2", packages.contains("hello"));
        packages = FileOps.packageExplore("", rootDir);
        assertEquals("package count b", 2, packages.size());
        assertTrue("packages contents b0", packages.contains("sub0.subsub0"));
        assertTrue("packages contents b1", packages.contains("sub1"));
        assertTrue("deleting temp directory", FileOps.deleteDirectory(rootDir));
    }

    /** Tests that non-empty directories can be deleted on exit. */
    public void testDeleteDirectoryOnExit() throws IOException, InterruptedException {
        File tempDir = FileOps.createTempDirectory("DrJavaTestTempDir");
        assertTrue("tempDir exists", tempDir.exists());
        File dir1 = new File(tempDir, "dir1");
        dir1.mkdir();
        assertTrue("dir1 exists", dir1.exists());
        assertTrue("dir1 is directory", dir1.isDirectory());
        File file1 = new File(dir1, "file1");
        file1.createNewFile();
        assertTrue("file1 exists", file1.exists());
        File dir2 = new File(dir1, "dir2");
        dir2.mkdir();
        assertTrue("dir2 exists", dir2.exists());
        assertTrue("dir2 is directory", dir1.isDirectory());
        File file2 = new File(dir2, "file2");
        file2.createNewFile();
        assertTrue("file2 exists", file2.exists());
        edu.rice.cs.plt.io.IOUtil.deleteOnExitRecursively(tempDir);
        Process process = JVMBuilder.DEFAULT.start(FileOpsTest.class.getName(), dir1.getAbsolutePath());
        int status = process.waitFor();
        assertEquals("Delete on exit test exited with an error!", 0, status);
        assertTrue("dir1 should be deleted", !dir1.exists());
        assertTrue("file1 should be deleted", !file1.exists());
        assertTrue("dir2 should be deleted", !dir2.exists());
        assertTrue("file2 should be deleted", !file2.exists());
    }

    public void testSplitFile() {
        String[] parts = new String[] { "", "home", "username", "dir" };
        StringBuilder path1 = new StringBuilder();
        for (String s : parts) {
            path1.append(s);
            path1.append(File.separator);
        }
        File f = new File(path1.toString());
        String[] res = FileOps.splitFile(f);
        assertTrue("Inconsitent results. Expected " + java.util.Arrays.asList(parts).toString() + ", but found " + java.util.Arrays.asList(res).toString(), java.util.Arrays.equals(parts, res));
    }

    private String fixPathFormat(String s) {
        return s.replace('\\', '/');
    }

    public void testMakeRelativeTo() throws IOException, SecurityException {
        File base, abs;
        base = new File("src/test1/test2/file.txt");
        abs = new File("built/test1/test2/file.txt");
        assertEquals("Wrong Relative Path 1", "../../../built/test1/test2/file.txt", fixPathFormat(FileOps.stringMakeRelativeTo(abs, base)));
        base = new File("file.txt");
        abs = new File("built/test1/test2/file.txt");
        assertEquals("Wrong Relative Path 2", "built/test1/test2/file.txt", fixPathFormat(FileOps.stringMakeRelativeTo(abs, base)));
        base = new File("built/test1/test2test/file.txt");
        abs = new File("built/test1/test2/file.txt");
        assertEquals("Wrong Relative Path 3", "../test2/file.txt", fixPathFormat(FileOps.stringMakeRelativeTo(abs, base)));
        base = new File("file.txt");
        abs = new File("test.txt");
        assertEquals("Wrong Relative Path 4", "test.txt", fixPathFormat(FileOps.stringMakeRelativeTo(abs, base)));
    }

    /** Main method called by testDeleteDirectoryOnExit.  Runs in new JVM so the files can be deleted.  Exits with status
    * 1 if wrong number of arguments are passed.  Exits with status 2 if file doesn't exist.
    * @param args should contain the file name of the directory to delete on exit
    */
    public static void main(String[] args) {
        if (args.length != 1) System.exit(1);
        File dir = new File(args[0]);
        if (!dir.exists()) System.exit(2);
        FileOps.deleteDirectoryOnExit(dir);
        System.exit(0);
    }

    public void testConvertToAbsolutePathEntries() {
        String ud = System.getProperty("user.dir");
        String f = System.getProperty("file.separator");
        String p = System.getProperty("path.separator");
        String expected, actual, input;
        input = "." + p + "drjava" + p + p + f + "home" + f + "foo" + f + "junit.jar";
        expected = ud + f + "." + p + ud + f + "drjava" + p + ud + p + (new File(f + "home" + f + "foo" + f + "junit.jar")).getAbsolutePath();
        actual = FileOps.convertToAbsolutePathEntries(input);
        assertEquals("testConvertToAbsolutePathEntries for several paths failed, input = '" + input + "', expected = '" + expected + "', actual = '" + actual + "'", expected, actual);
        input = "";
        expected = ud;
        actual = FileOps.convertToAbsolutePathEntries(input);
        assertEquals("testConvertToAbsolutePathEntries for empty path failed, input = '" + input + "', expected = '" + expected + "', actual = '" + actual + "'", expected, actual);
        input = p + p + p + ".";
        expected = ud + p + ud + p + ud + p + ud + f + ".";
        actual = FileOps.convertToAbsolutePathEntries(input);
        assertEquals("testConvertToAbsolutePathEntries for several empty paths failed, input = '" + input + "', expected = '" + expected + "', actual = '" + actual + "'", expected, actual);
        input = p + p;
        expected = ud + p + ud + p + ud;
        actual = FileOps.convertToAbsolutePathEntries(input);
        assertEquals("testConvertToAbsolutePathEntries for trailing empty paths failed, input = '" + input + "', expected = '" + expected + "', actual = '" + actual + "'", expected, actual);
    }

    /** Tests getFilesInDir. */
    public void testGetFiles() throws IOException {
        File dir1 = FileOps.createTempDirectory("DrJavaTestTempDir");
        assertTrue("dir1 exists", dir1.exists());
        File file1a = File.createTempFile("DrJavaTest-", ".temp", dir1).getCanonicalFile();
        assertTrue("file1a exists", file1a.exists());
        File file1b = File.createTempFile("DrJava-", ".temp", dir1).getCanonicalFile();
        assertTrue("file1b exists", file1b.exists());
        File dir2 = FileOps.createTempDirectory("DrJavaTestDir-", dir1).getCanonicalFile();
        assertTrue("dir2 exists", dir2.exists());
        File file2 = File.createTempFile("DrJavaTest-", ".temp", dir2).getCanonicalFile();
        assertTrue("file2 exists", file2.exists());
        FileFilter ff = new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName();
                return name.startsWith("DrJavaTest");
            }
        };
        Set<File> res1 = new TreeSet<File>(Arrays.asList(new File[] { file1a }));
        Set<File> res2 = new TreeSet<File>(Arrays.asList(new File[] { file1a, file2 }));
        Set<File> nrfiles = new TreeSet<File>();
        for (File f : FileOps.getFilesInDir(dir1, false, ff)) {
            nrfiles.add(f.getCanonicalFile());
        }
        Set<File> rfiles = new TreeSet<File>();
        for (File f : FileOps.getFilesInDir(dir1, true, ff)) {
            rfiles.add(f.getCanonicalFile());
        }
        assertEquals("non-recursive FilesInDir test", res1, nrfiles);
        assertEquals("recursive FileInDir test", res2, rfiles);
        edu.rice.cs.plt.io.IOUtil.deleteRecursively(dir1);
    }

    /** Tests for getShortFile. This test creates a file and writes to it using a long file name, then
    * checks if the short file name is the same (canonical) file and that it contains teh same data. */
    public void testGetShortFile() throws IOException {
        File dir1 = FileOps.createTempDirectory("DrJavaTestTempDir");
        File dir2 = new File(dir1, "Documents and Settings" + File.separator + "User Name" + File.separator + "My Documents");
        assertTrue("Couldn't create temp directory", dir2.mkdirs());
        File longF = new File(dir2, "rt.concjunit.txt");
        PrintWriter pw = new PrintWriter(new FileWriter(longF));
        String s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        File shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        BufferedReader br = new BufferedReader(new FileReader(shortF));
        String line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        longF = new File(dir2, "prefix and space rt.concjunit.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        longF = new File(dir2, "short.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        File dir3 = new File(dir2, "Another Long Directory");
        assertTrue("Couldn't create temp directory", dir3.mkdirs());
        longF = new File(dir3, "rt.concjunit.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        longF = new File(dir3, "prefix and space rt.concjunit.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        longF = new File(dir3, "short.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        File dir4 = new File(dir2, "shortdir");
        assertTrue("Couldn't create temp directory", dir4.mkdirs());
        longF = new File(dir4, "rt.concjunit.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        longF = new File(dir4, "prefix and space rt.concjunit.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        longF = new File(dir4, "short.txt");
        pw = new PrintWriter(new FileWriter(longF));
        s = new java.util.Date().toString() + " " + System.currentTimeMillis();
        pw.println(s);
        pw.close();
        shortF = FileOps.getShortFile(longF);
        assertTrue("Short file name doesn't exist", shortF.exists());
        br = new BufferedReader(new FileReader(shortF));
        line = br.readLine();
        br.close();
        assertEquals("File contents are not the same", s, line);
        assertEquals("Files are not the same", longF.getCanonicalFile(), shortF.getCanonicalFile());
        FileOps.deleteDirectory(dir1);
    }
}
