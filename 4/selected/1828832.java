package com.netx.ut.lib.java;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import com.netx.basic.R1.io.Directory;
import com.netx.basic.R1.shared.Constants;
import com.netx.generics.R1.util.UnitTester;
import com.netx.generics.R1.util.Strings;

public class NTInputOutput extends UnitTester {

    public static void main(String[] args) throws Throwable {
        NTInputOutput nt = new NTInputOutput();
        nt.parallelWrite();
        nt.println("done.");
    }

    public void showPaths() throws IOException {
        File file = new File("C:\\WINDOWS\\system32\\..\\explorer.exe");
        println("getAbsoluteFile: " + file.getAbsoluteFile());
        println("getAbsolutePath: " + file.getAbsolutePath());
        println("getCanonicalFile: " + file.getCanonicalFile());
        println("getCanonicalPath: " + file.getCanonicalPath());
        println("getPath: " + file.getPath());
        println("getParentFile: " + file.getParentFile());
        println("getParent: " + file.getParent());
        println("getParentFile.getAbsolutePath: " + file.getParentFile().getAbsolutePath());
        println("getParentFile.getCanonicalPath: " + file.getParentFile().getCanonicalPath());
        println("getName: " + file.getName());
        println("isAbsolute: " + file.isAbsolute());
        println("compareTo: " + file.compareTo(new File("C:\\WINDOWS\\system32\\explorer.exe")));
        println("toURI: " + file.toURI());
        println("toURL: " + file.toURI().toURL());
        println("user.dir: " + System.getProperty("user.dir"));
        println("Current directory:");
        file = new File(".");
        println("getPath: " + file.getPath());
        println("getAbsolutePath: " + file.getAbsolutePath());
        println("getCanonicalPath: " + file.getCanonicalPath());
        println("Slashes:");
        file = new File("C:\\Windows/explorer.exe");
        println("getPath: " + file.getPath());
        println("getAbsolutePath: " + file.getAbsolutePath());
        println("getCanonicalPath: " + file.getCanonicalPath());
        println("Constructors:");
        file = new File(new File("C:\\WINDOWS"), "/explorer.exe");
        println("getAbsolutePath: " + file.getAbsolutePath());
        println("getCanonicalPath: " + file.getCanonicalPath());
        println("getPath: " + file.getPath());
        file = new File(new File("C:\\WINDOWS"), "/");
        println("getAbsolutePath: " + file.getAbsolutePath());
        println("getCanonicalPath: " + file.getCanonicalPath());
        println("getPath: " + file.getPath());
    }

    public void testReadOnly() throws Exception {
        File f = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/read-only-file");
        println("exists: " + f.exists());
        println("canWrite: " + f.canWrite());
        println("calling setReadOnly()...");
        f.setReadOnly();
        println("canWrite: " + f.canWrite());
        new FileOutputStream(f);
    }

    public void testDeleteReadOnly() throws Exception {
        File f = new File(new NTInputOutput().getClass().getResource("read-only-file").getFile());
        println("deleted: " + f.delete());
    }

    public void parallelWrite() throws Exception {
        File f = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/parallel.txt");
        FileWriter w1 = new FileWriter(f);
        FileWriter w2 = new FileWriter(f, true);
        w1.write("1111\r\n");
        w2.write("2222\r\n");
        w1.close();
        w2.close();
    }

    public void readDirectory() throws Exception {
        File f = new File("C:\\WINDOW");
        new FileInputStream(f);
    }

    public void fileLength() {
        File file = new File("C:\\WINDOWS\\explorer.exe");
        println(file.exists());
        println(file.length());
        println(file.lastModified());
    }

    public void deleteOnExit() {
        File dir = new File("C:\\EXP_1");
        if (dir.mkdir() == true) {
            println("Directory created.");
        }
        dir.deleteOnExit();
    }

    public void listRoots() {
        File[] list = File.listRoots();
        println(list.length + " roots:");
        for (int i = 0; i < list.length; i++) {
            println(list[i]);
        }
    }

    public void testListFiles() {
        File dir = new File("C:\\temp");
        File[] list = dir.listFiles();
        println(list.length);
        for (int i = 0; i < list.length; i++) {
            println(list[i]);
        }
    }

    public void serialize() {
        _serialize(new Integer(10));
    }

    private void _serialize(Serializable object) {
    }

    public void testFileConstructors() throws IOException {
        File root = new File("C:/");
        File dir = new File(root, "Shared/Modules");
        println(dir.getCanonicalPath());
    }

    public void testFileNames() throws IOException {
        File file = new File("C:/Shared/Modules/..");
        println(file.getName());
        File file2 = new File(file.getCanonicalPath());
        println(file2.getName());
    }

    public void testExcelFileAccessException() throws Exception {
        java.util.Locale.setDefault(new java.util.Locale("en", "US"));
        File xls = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/" + "exception.xls");
        println(xls.exists());
        new FileOutputStream(xls);
    }

    public void testLockFile() throws Exception {
        File xls = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/" + "lockedFile.xls");
        FileOutputStream out = new FileOutputStream(xls);
        FileChannel channel = out.getChannel();
        FileLock lock = channel.tryLock();
        if (lock == null) {
            println("it was not possible to lock the file.");
            return;
        }
        try {
            println("press ENTER to continue...");
            System.in.read();
        } finally {
            lock.release();
        }
        out.close();
    }

    public void testClosedStream() throws Exception {
        File readOnly = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/read-only-file");
        FileInputStream in = new FileInputStream(readOnly);
        println(in.read());
        in.close();
        println(in.read());
    }

    public void testBufferSize() throws Exception {
        File readOnly = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/read-only-file");
        MyBufferedInputStream in = new MyBufferedInputStream(new FileInputStream(readOnly));
        println(in.getBufferSize());
        in.close();
    }

    private static class MyBufferedInputStream extends BufferedInputStream {

        public MyBufferedInputStream(InputStream in) {
            super(in);
        }

        public int getBufferSize() {
            return buf.length;
        }
    }

    public void testEOFE() throws Exception {
        File readOnly = new File(getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR).getAbsolutePath() + "/read-only-file");
        BufferedReader in = new BufferedReader(new FileReader(readOnly));
        String s = null;
        do {
            s = in.readLine();
            println(s);
        } while (s != null);
        println(in.read());
        in.close();
    }

    public void testMustangChanges() throws Exception {
        Directory dir = getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR);
        File f = new File(dir.getAbsolutePath() + "\\test-file.tmp");
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        println("canExecute: " + f.canExecute());
        println("changing... " + f.setExecutable(false));
        println("canExecute: " + f.canExecute());
        println("canWrite: " + f.canWrite());
        println("changing... " + f.setWritable(false));
        println("canWrite: " + f.canWrite());
        println("canRead: " + f.canRead());
        println("changing... " + f.setReadable(false));
        println("canRead: " + f.canRead());
        println("setReadOnly: " + f.setReadOnly());
        println("canRead: " + f.canRead());
        new java.io.FileOutputStream(f);
    }

    public void testConcurrentWrites() throws IOException {
        Directory dir = getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR);
        File f = new File(dir.getAbsolutePath() + "\\test-file.tmp");
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        FileWriter out1 = new FileWriter(f, true);
        FileWriter out2 = new FileWriter(f, true);
        out1.write("line 1");
        out1.write(Constants.NL);
        out1.write("line 2");
        out1.write(Constants.NL);
        out2.write("line 3");
        out2.write(Constants.NL);
        out1.flush();
        out2.flush();
        BufferedReader in = new BufferedReader(new FileReader(f));
        String s = in.readLine();
        while (s != null) {
            System.out.println(s);
            s = in.readLine();
        }
    }

    public void testConcurrentWrites2() throws IOException {
        Directory dir = getTestResourceLocation().getDirectory(TestResources.JAVA_TEST_DIR);
        File f = new File(dir.getAbsolutePath() + "\\test-file.tmp");
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        FileOutputStream out1 = new FileOutputStream(f, true);
        out1.write("abc".getBytes());
        FileOutputStream out2 = new FileOutputStream(f, true);
        FileChannel fc = out2.getChannel();
        FileLock lock = fc.lock();
        if (lock == null) {
            throw new RuntimeException();
        }
        out1.write("def".getBytes());
        out1.close();
        out2.close();
    }

    public void testStringWriter() throws Exception {
        StringWriter sw = new StringWriter();
        Exception e = new Exception();
        e.printStackTrace(new PrintWriter(sw));
        print("Number of tabs: ");
        println(Strings.countOccurrences(sw.toString(), '\t'));
    }
}
