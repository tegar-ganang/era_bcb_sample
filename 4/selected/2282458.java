package org.opendte.node;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
import org.opendte.controller.*;

public class TestCaseManager {

    public static final String TIMEFORMAT = "yyyy-MM-dd_HH-mm-ss";

    private File clTestcaseDirectory;

    private Vector testCases;

    public TestCaseManager(File testcaseDirectory) {
        clTestcaseDirectory = testcaseDirectory;
    }

    public File getTestCaseDir(TestSpecification spec) {
        SimpleDateFormat formatter = new SimpleDateFormat(TIMEFORMAT);
        Date specDate = spec.getCreationDate();
        String dateString = formatter.format(specDate);
        File testCaseDir = new File(clTestcaseDirectory, spec.getName() + "-" + dateString);
        return testCaseDir;
    }

    public void installTestCase(TestSpecification spec, byte[] tCaseBytes) throws IOException {
        File testCaseDir = getTestCaseDir(spec);
        if (!testCaseDir.exists()) testCaseDir.mkdirs();
        System.out.println("Installing test case " + spec.getName() + " in directory " + testCaseDir);
        UDPLogger.trace("Installing test case " + spec.getName() + " in directory " + testCaseDir);
        if (tCaseBytes == null) return;
        unzipTestCase(tCaseBytes, testCaseDir);
        File specFile = new File(testCaseDir, ".spec");
        FileOutputStream fout = new FileOutputStream(specFile);
        ObjectOutputStream oout = new ObjectOutputStream(fout);
        oout.writeObject(spec);
        oout.flush();
        fout.flush();
        fout.close();
    }

    public boolean isInstalled(TestSpecification spec) {
        File testCaseDir = getTestCaseDir(spec);
        try {
            File specFile = new File(testCaseDir, ".spec");
            if (!specFile.exists()) return false;
            FileInputStream fin = new FileInputStream(specFile);
            ObjectInputStream oin = new ObjectInputStream(fin);
            TestSpecification specFromFile = (TestSpecification) oin.readObject();
            if (spec.equals(specFromFile)) {
                UDPLogger.trace("Test case " + spec.getName() + "@" + spec.getCreationDate() + " is installed");
                return true;
            } else {
                UDPLogger.trace("Test case " + spec.getName() + "@" + spec.getCreationDate() + " is not installed");
                return false;
            }
        } catch (Exception ex) {
            UDPLogger.trace("Test case " + spec.getName() + " is not installed, exception was " + ex.getMessage());
            return false;
        }
    }

    public IFTestCase createTestCase(TestSpecification spec) throws Exception {
        UDPLogger.trace("Creating test case " + spec.getName());
        File tcDir = getTestCaseDir(spec);
        UDPLogger.trace("Testcase dir: " + tcDir.getAbsolutePath());
        String classpath = tcDir.getAbsolutePath();
        classpath = classpath.replace('\\', '/');
        classpath = "file:///" + classpath + "/";
        System.out.println("Using classpath: " + classpath);
        URL classpaths[] = { new URL(classpath) };
        URLClassLoader ucl = new URLClassLoader(classpaths);
        Thread.currentThread().setContextClassLoader(ucl);
        return (IFTestCase) Class.forName(spec.getMainClassName(), true, ucl).newInstance();
    }

    public void unzipTestCase(byte[] testCaseBytes, File testCaseDir) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(testCaseBytes);
        ZipInputStream zin = new ZipInputStream(bin);
        ZipEntry zEntry;
        while ((zEntry = zin.getNextEntry()) != null) {
            String zEntryName = zEntry.getName();
            if (zEntryName.startsWith("/")) zEntryName = zEntryName.substring(1); else if (zEntryName.startsWith("./")) zEntryName = zEntryName.substring(2);
            zEntryName = testCaseDir + File.separator + zEntryName;
            File zipFile = new File(zEntryName);
            if (zEntry.isDirectory()) {
                UDPLogger.trace("next entry: " + zEntryName + " (directory)");
                if (!zipFile.exists()) zipFile.mkdirs();
            } else {
                UDPLogger.trace("next zip entry: " + zEntry);
                UDPLogger.trace("storing entry to file " + zipFile);
                File zipFileParent = zipFile.getParentFile();
                if (zipFileParent != null) {
                    if (!zipFileParent.exists()) zipFileParent.mkdirs();
                }
                if (zipFile.exists()) zipFile.delete();
                zipFile.createNewFile();
                FileOutputStream fout = new FileOutputStream(zipFile);
                while (true) {
                    byte buf[] = new byte[4096];
                    int read = zin.read(buf, 0, 4096);
                    if (read > -1) fout.write(buf, 0, read); else break;
                }
                fout.flush();
                fout.close();
            }
        }
    }
}
