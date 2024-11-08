package ch.enterag.utils.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;
import junit.framework.TestCase;

/** Tests Zip64File.
 @author Hartwig Thomas
 */
public class Zip64FileTester extends TestCase {

    /** buffer size for I/O */
    private static final int iBUFFER_SIZE = 8192;

    /** small file size for I/O */
    private static final int iSMALL_SIZE = 16;

    /** number of buffers for more than 4 GB */
    private static final int iLARGE_BUFFERS = 0x0A0000;

    /** number of buffers for more than 65 KB */
    private static final int iMODERATE_BUFFERS = 20;

    /** global file comment */
    private static final String sZIP_COMMENT = "a global ZIP file comment";

    /** zip file produced by pkzipc */
    private String m_sPkZipFile = null;

    /** test zip file */
    private String m_sTestZipFile = null;

    /** temp location with lots of free space which does not need to be backupped */
    private static final String sTEMP_LOCATION = "D:\\Temp";

    /** temp directory */
    private static final String sTEMP_DIRECTORY = sTEMP_LOCATION + "\\Temp";

    /** extract directory */
    private static final String sEXTRACT_DIRECTORY = sTEMP_LOCATION + "\\Extract";

    /**
	 @param name
	 */
    public Zip64FileTester(String name) {
        super(name);
    }

    /** append a file to the ZIP64 file.
	 * @param zf ZIP64 file.
   * @param sFileName name of file entry in ZIP64 file.
	 * @param fileOriginal file to be appended.
   * @param iMethod method to be used (stored or deflated).
	 */
    private void appendFile(Zip64File zf, String sFileName, File fileOriginal, int iMethod) {
        byte[] buffer = new byte[iBUFFER_SIZE];
        try {
            Date dateModified = new Date(fileOriginal.lastModified());
            FileInputStream fis = new FileInputStream(fileOriginal);
            EntryOutputStream eos = zf.openEntryOutputStream(sFileName, iMethod, dateModified);
            for (int iRead = fis.read(buffer); iRead >= 0; iRead = fis.read(buffer)) eos.write(buffer, 0, iRead);
            fis.close();
            eos.close();
        } catch (ZipException ze) {
            System.out.println(ze.getClass().getName() + ": " + ze.getMessage());
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            System.out.println(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /** append a directory to the ZIP64 file.
	 * @param zf ZIP64 file.
	 * @param sDirectory name of the directory file entry in ZIP64 file.
	 */
    private void appendDirectory(Zip64File zf, String sDirectory) {
        try {
            EntryOutputStream eos = zf.openEntryOutputStream(sDirectory, FileEntry.iMETHOD_STORED, null);
            eos.close();
        } catch (ZipException ze) {
            System.out.println(ze.getClass().getName() + ": " + ze.getMessage());
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            System.out.println(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /** add the test files to the ZIP64 file.
	 * @param iMethod method to be used (stored or deflated).
	 */
    private void zipTest(int iMethod) {
        try {
            File fileTemp = new File(sTEMP_DIRECTORY);
            if (!fileTemp.exists()) fileTemp.mkdirs();
            File fileModerateOriginal = new File(fileTemp.getAbsolutePath() + "\\moderate.txt");
            File fileMediumOriginal = new File(fileTemp.getAbsolutePath() + "\\medium.txt");
            File fileLargeOriginal = new File(fileTemp.getAbsolutePath() + "\\large.txt");
            Zip64File zf = new Zip64File(m_sTestZipFile);
            appendFile(zf, "moderate.txt", fileModerateOriginal, iMethod);
            appendFile(zf, "medium.txt", fileMediumOriginal, iMethod);
            appendFile(zf, "large.txt", fileLargeOriginal, iMethod);
            appendDirectory(zf, "many/");
            for (int iSmall = 0; iSmall < 0x00014000; iSmall++) {
                DecimalFormat df = new DecimalFormat("00000");
                String sSmallFile = "small" + df.format(new Long(iSmall)) + ".txt";
                File fileSmallSource = new File(fileTemp.getAbsolutePath() + "\\many\\" + sSmallFile);
                appendFile(zf, "many/" + sSmallFile, fileSmallSource, iMethod);
            }
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /** extract a file entry from the ZIP64 file.
	 * @param sEntryName name of file entry.
	 * @return true, if file entry could be extracted.
	 */
    private boolean extractFile(String sEntryName) {
        boolean bExtracted = false;
        File fileExtract = new File(sEXTRACT_DIRECTORY);
        if (!fileExtract.exists()) fileExtract.mkdirs();
        String[] asProg = new String[8];
        asProg[0] = "pkzipc.exe";
        asProg[1] = "-extract";
        asProg[2] = "-attr=all";
        asProg[3] = "-directories";
        asProg[4] = "-silent=normal";
        asProg[5] = m_sTestZipFile;
        asProg[6] = sEntryName;
        asProg[7] = fileExtract.getAbsolutePath() + "\\";
        try {
            Process procPkZip = Runtime.getRuntime().exec(asProg);
            InputStream isStdOut = procPkZip.getInputStream();
            for (int c = isStdOut.read(); c != -1; c = isStdOut.read()) System.out.print(c);
            isStdOut.close();
            int iExitCode = procPkZip.waitFor();
            if (iExitCode == 0) bExtracted = true;
            procPkZip.destroy();
        } catch (IOException ie) {
            System.out.println(ie.getClass().getName() + ": " + ie.getMessage());
        } catch (InterruptedException ie) {
            System.out.println(ie.getClass().getName() + ": " + ie.getMessage());
        }
        return bExtracted;
    }

    /** check if two buffers are equal.
	 * @param buffer1 first buffer.
   * @param iSize1 size of first buffer. 
	 * @param buffer2 second buffer.
   * @param iSize2 size of second buffer.
   * @return true, if buffers are equal.
	 */
    private boolean equalBuffers(byte[] buffer1, int iSize1, byte[] buffer2, int iSize2) {
        boolean bEqual = true;
        if (iSize1 == iSize2) {
            for (int i = 0; bEqual && (i < iSize1); i++) if (buffer1[i] != buffer2[i]) bEqual = false;
        } else bEqual = false;
        return bEqual;
    }

    /** check if two files are equal.
   * @param file1 first file.
   * @param file2 second file.
   * @return true, if files are equal.
	 */
    private boolean equalFiles(File file1, File file2) {
        boolean bEqual = true;
        byte[] buffer1 = new byte[iBUFFER_SIZE];
        byte[] buffer2 = new byte[iBUFFER_SIZE];
        try {
            FileInputStream fis1 = new FileInputStream(file1);
            FileInputStream fis2 = new FileInputStream(file2);
            int iRead1 = fis1.read(buffer1);
            int iRead2 = fis2.read(buffer2);
            while (bEqual && (iRead1 >= 0) && (iRead2 >= 0)) {
                if (!equalBuffers(buffer1, iRead1, buffer2, iRead2)) bEqual = false; else {
                    iRead1 = fis1.read(buffer1);
                    iRead2 = fis2.read(buffer2);
                }
            }
            fis1.close();
            fis2.close();
        } catch (FileNotFoundException fnfe) {
            bEqual = false;
            System.out.println(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            bEqual = false;
            System.out.println(ie.getClass().getName() + ": " + ie.getMessage());
        }
        return bEqual;
    }

    /** test if two large files are probably equal by randomly comparing 
	 * some portions.
   * @param file1 first file.
   * @param file2 second file.
   * @return true, if files are probably equal.
	 */
    private boolean equalTest(File file1, File file2) {
        boolean bEqual = true;
        long lLength = file1.length();
        if (lLength == file2.length()) {
            if (lLength <= 16 * iBUFFER_SIZE) bEqual = equalFiles(file1, file2); else {
                byte[] buffer1 = new byte[iBUFFER_SIZE];
                byte[] buffer2 = new byte[iBUFFER_SIZE];
                try {
                    RandomAccessFile raf1 = new RandomAccessFile(file1, "r");
                    RandomAccessFile raf2 = new RandomAccessFile(file2, "r");
                    for (int iTest = 0; iTest < 16; iTest++) {
                        long lRandomPosition = (long) Math.floor((lLength - iBUFFER_SIZE) * Math.random());
                        raf1.seek(lRandomPosition);
                        raf2.seek(lRandomPosition);
                        int iRead1 = raf1.read(buffer1);
                        int iRead2 = raf2.read(buffer2);
                        bEqual = equalBuffers(buffer1, iRead1, buffer2, iRead2);
                    }
                    raf1.close();
                    raf2.close();
                } catch (FileNotFoundException fnfe) {
                    bEqual = false;
                    System.out.println(fnfe.getClass().getName() + ": " + fnfe.getMessage());
                } catch (IOException ie) {
                    bEqual = false;
                    System.out.println(ie.getClass().getName() + ": " + ie.getMessage());
                }
            }
        } else bEqual = false;
        return bEqual;
    }

    /** create a large file.
   * @param fileLarge file to be created.
   * @throws FileNotFoundException if folder does not exist.
   * @throws IOException if an I/O error occurred.
   */
    private void createLarge(File fileLarge) throws FileNotFoundException, IOException {
        System.out.println("writing large file");
        FileOutputStream fos = new FileOutputStream(fileLarge);
        byte[] buffer = new byte[iBUFFER_SIZE];
        for (int iBuffer = 0; iBuffer < iLARGE_BUFFERS; iBuffer++) {
            for (int i = 0; i < buffer.length; i++) {
                if (i % 76 == 75) buffer[i] = 0x0A; else buffer[i] = (byte) (32 + (int) Math.floor(96 * Math.random()));
            }
            fos.write(buffer);
        }
        fos.close();
    }

    /** create a medium size file.
   * @param fileMedium file to be created.
   * @throws FileNotFoundException if folder does not exist.
   * @throws IOException if an I/O error occurred.
   */
    private void createMedium(File fileMedium) throws FileNotFoundException, IOException {
        System.out.println("writing medium file");
        byte[][] abufWord = new byte[256][];
        for (int iWord = 0; iWord < abufWord.length; iWord++) {
            int iLength = 2 + (int) Math.floor(3 * Math.random());
            abufWord[iWord] = new byte[iLength];
            for (int i = 0; i < abufWord[iWord].length; i++) abufWord[iWord][i] = (byte) (32 + 96 * (int) Math.floor(Math.random()));
        }
        FileOutputStream fos = new FileOutputStream(fileMedium);
        byte[] buffer = new byte[iBUFFER_SIZE];
        for (int iBuffer = 0; iBuffer < iLARGE_BUFFERS; iBuffer++) {
            for (int iPos = 0; iPos < buffer.length; ) {
                int iWord = (int) Math.floor(256 * Math.random());
                if (Math.floor(iPos / 76) != Math.floor((iPos + abufWord[iWord].length) / 76)) {
                    buffer[iPos] = 0x0A;
                    iPos++;
                }
                for (int i = 0; (i < abufWord[iWord].length) && (iPos < buffer.length); i++) {
                    buffer[iPos] = abufWord[iWord][i];
                    iPos++;
                }
            }
            fos.write(buffer);
        }
        fos.close();
    }

    /** create a moderate size file.
   * @param fileModerate file to be created.
   * @throws FileNotFoundException if folder does not exist.
   * @throws IOException if an I/O error occurred.
   */
    private void createModerate(File fileModerate) throws FileNotFoundException, IOException {
        System.out.println("writing moderate file");
        FileOutputStream fos = new FileOutputStream(fileModerate);
        byte[] buffer = new byte[iBUFFER_SIZE];
        for (int iBuffer = 0; iBuffer < iMODERATE_BUFFERS; iBuffer++) {
            for (int i = 0; i < buffer.length; i++) {
                if (i % 76 == 75) buffer[i] = 0x0A; else buffer[i] = (byte) (32 + (int) Math.floor(96 * Math.random()));
            }
            fos.write(buffer);
        }
        fos.close();
    }

    /** create a small file.
   * @param fileSmall file to be created.
   * @throws FileNotFoundException if folder does not exist.
   * @throws IOException if an I/O error occurred.
   */
    private void createSmall(File fileSmall) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(fileSmall);
        int iLength = (int) Math.floor(iSMALL_SIZE * Math.random());
        byte[] buffer = new byte[iLength];
        for (int i = 0; i < buffer.length; i++) buffer[i] = (byte) (32 + (int) Math.floor(96 * Math.random()));
        fos.write(buffer);
        fos.close();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File fileTemp = new File(sTEMP_DIRECTORY);
        if (!fileTemp.exists()) fileTemp.mkdirs();
        File fileTest = new File(fileTemp.getParentFile().getAbsolutePath() + "\\test.zip");
        if (fileTest.exists()) fileTest.delete();
        m_sTestZipFile = fileTest.getAbsolutePath();
        String sLargeFile = fileTemp.getAbsolutePath() + "\\large.txt";
        File fileLarge = new File(sLargeFile);
        if (!fileLarge.exists()) createLarge(fileLarge);
        String sMediumFile = fileTemp.getAbsolutePath() + "\\medium.txt";
        File fileMedium = new File(sMediumFile);
        if (!fileMedium.exists()) createMedium(fileMedium);
        String sModerateFile = fileTemp.getAbsolutePath() + "\\moderate.txt";
        File fileModerate = new File(sModerateFile);
        if (!fileModerate.exists()) createModerate(fileModerate);
        String sManyFolder = fileTemp.getAbsolutePath() + "\\many\\";
        File fileMany = new File(sManyFolder);
        if (!fileMany.exists()) {
            System.out.println("writing small files");
            fileMany.mkdir();
            for (int i = 0; i < 0x014000; i++) {
                DecimalFormat df = new DecimalFormat("00000");
                String sSmallFile = fileMany.getAbsolutePath() + "\\small" + df.format(new Long(i)) + ".txt";
                File fileSmall = new File(sSmallFile);
                if (!fileSmall.exists()) createSmall(fileSmall);
            }
        }
        File fileZip = new File(fileTemp.getParentFile().getAbsolutePath() + "\\pktest.zip");
        if (!fileZip.exists()) {
            String[] asProg = new String[8];
            asProg[0] = "pkzipc.exe";
            asProg[1] = "-add=all";
            asProg[2] = "-attr=all";
            asProg[3] = "-dir=specify";
            asProg[4] = "-silent=normal";
            asProg[5] = "-header=" + sZIP_COMMENT;
            asProg[6] = fileZip.getAbsolutePath();
            asProg[7] = fileTemp.getAbsolutePath() + "\\*";
            try {
                Process procPkZip = Runtime.getRuntime().exec(asProg);
                InputStream isStdOut = procPkZip.getInputStream();
                for (int c = isStdOut.read(); c != -1; c = isStdOut.read()) System.out.print(c);
                isStdOut.close();
                int iExitCode = procPkZip.waitFor();
                if (iExitCode != 0) fail("pkzipc exit code: " + String.valueOf(iExitCode));
                procPkZip.destroy();
            } catch (IOException ie) {
                fail(ie.getClass().getName() + ": " + ie.getMessage());
            } catch (InterruptedException ie) {
                fail(ie.getClass().getName() + ": " + ie.getMessage());
            }
        }
        m_sPkZipFile = fileZip.getAbsolutePath();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#Zip64File(java.lang.String, boolean)}.
	 */
    public void testZip64FileStringBoolean() {
        try {
            Zip64File zf = new Zip64File(m_sPkZipFile, true);
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#Zip64File(java.io.File, boolean)}.
	 */
    public void testZip64FileFileBoolean() {
        try {
            File fileTest = new File(m_sTestZipFile);
            if (fileTest.exists()) fileTest.delete();
            Zip64File zf = new Zip64File(fileTest, true);
            zf.close();
            fail("Opening non-existent file read-only should not succeed!");
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#Zip64File(java.lang.String)}.
	 */
    public void testZip64FileString() {
        try {
            File fileTest = new File(m_sTestZipFile);
            if (fileTest.exists()) fileTest.delete();
            Zip64File zf = new Zip64File(m_sTestZipFile);
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#Zip64File(java.io.File)}.
	 */
    public void testZip64FileFile() {
        try {
            File fileZip = new File(m_sPkZipFile);
            Zip64File zf = new Zip64File(fileZip);
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#close()}.
	 */
    public void testClose() {
        try {
            File fileTest = new File(m_sTestZipFile);
            if (fileTest.exists()) fileTest.delete();
            Zip64File zf = new Zip64File(m_sTestZipFile);
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#getComment()}.
	 */
    public void testGetComment() {
        try {
            Zip64File zf = new Zip64File(m_sPkZipFile, true);
            String sComment = zf.getComment();
            if (!sZIP_COMMENT.equals(sComment)) fail("Invalid ZIP comment found: " + sComment + "!");
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#setComment(String)}.
	 */
    public void testSetComment() {
        try {
            Zip64File zf = new Zip64File(m_sTestZipFile);
            String sComment = "a new comment";
            zf.setComment(sComment);
            zf.close();
            zf = new Zip64File(m_sTestZipFile, true);
            if (!sComment.equals(zf.getComment())) fail("ZIP comment could not be set!");
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#getFileEntries()}.
	 */
    public void testGetFileEntries() {
        try {
            Zip64File zf = new Zip64File(m_sPkZipFile, true);
            int iFileEntries = zf.getFileEntries();
            if (iFileEntries != 0x00014004) fail("Invalid number of file entries found: " + String.valueOf(iFileEntries) + "!");
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#getFileEntry(java.lang.String)}.
	 */
    public void testGetFileEntry() {
        try {
            Zip64File zf = new Zip64File(m_sPkZipFile, true);
            FileEntry feMedium = zf.getFileEntry("medium.txt");
            if (feMedium != null) {
                long lSize = iBUFFER_SIZE;
                lSize *= iLARGE_BUFFERS;
                if (feMedium.getSize() != lSize) fail("Invalid size for medium.txt: " + String.valueOf(feMedium.getSize()));
            } else fail("file entry medium.txt not found!");
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#getListFileEntries()}.
	 */
    public void testGetListFileEntries() {
        try {
            String sPrefix = "many/small";
            String sSuffix = ".txt";
            int iPrefixEnd = sPrefix.length();
            int iSuffixStart = iPrefixEnd + 5;
            Zip64File zf = new Zip64File(m_sPkZipFile, true);
            List<FileEntry> listFileEntries = zf.getListFileEntries();
            for (Iterator<FileEntry> iterFileEntry = listFileEntries.iterator(); iterFileEntry.hasNext(); ) {
                FileEntry fe = iterFileEntry.next();
                String sName = fe.getName();
                if ((!sName.equals("large.txt")) && (!sName.equals("medium.txt")) && (!sName.equals("moderate.txt")) && (!sName.equals("many/"))) {
                    if ((sName.startsWith(sPrefix)) && (sName.endsWith(sSuffix))) {
                        int i = Integer.parseInt(sName.substring(iPrefixEnd, iSuffixStart));
                        if ((i < 0) || (i >= 0x00014000)) fail("Invalid file number: " + sName);
                    } else fail("Invalid file name: " + sName);
                }
            }
            zf.close();
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#openEntryInputStream(java.lang.String)}.
	 */
    public void testOpenEntryInputStream() {
        File fileExtract = new File(sEXTRACT_DIRECTORY);
        if (!fileExtract.exists()) fileExtract.mkdirs();
        File fileLarge = new File(fileExtract.getAbsolutePath() + "\\large.txt");
        if (fileLarge.exists()) fileLarge.delete();
        File fileMedium = new File(fileExtract.getAbsolutePath() + "\\medium.txt");
        if (fileMedium.exists()) fileMedium.delete();
        File fileSmall = new File(fileExtract.getAbsolutePath() + "\\small.txt");
        if (fileSmall.exists()) fileSmall.delete();
        File fileTemp = new File(sTEMP_DIRECTORY);
        if (!fileTemp.exists()) fileTemp.mkdirs();
        File fileLargeOriginal = new File(fileTemp.getAbsolutePath() + "\\large.txt");
        File fileMediumOriginal = new File(fileTemp.getAbsolutePath() + "\\medium.txt");
        File fileSmallOriginal = new File(fileTemp.getAbsolutePath() + "\\many\\small13854.txt");
        byte[] buffer = new byte[iBUFFER_SIZE];
        try {
            Zip64File zf = new Zip64File(m_sPkZipFile, true);
            EntryInputStream eis = zf.openEntryInputStream("many/small13854.txt");
            if (eis == null) fail("Input stream for small file could not be opened!"); else {
                FileOutputStream fos = new FileOutputStream(fileSmall);
                for (int iRead = eis.read(buffer); iRead >= 0; iRead = eis.read(buffer)) fos.write(buffer, 0, iRead);
                fos.close();
                eis.close();
                if (!equalTest(fileSmall, fileSmallOriginal)) fail("extracted small file is not equal to its original!");
                fileSmall.delete();
                eis = zf.openEntryInputStream("medium.txt");
                if (eis == null) fail("Input stream for medium file could not be opened!"); else {
                    fos = new FileOutputStream(fileMedium);
                    for (int iRead = eis.read(buffer); iRead >= 0; iRead = eis.read(buffer)) fos.write(buffer, 0, iRead);
                    fos.close();
                    eis.close();
                    if (!equalTest(fileMedium, fileMediumOriginal)) fail("extracted medium file is not equal to its original!"); else {
                        fileMedium.delete();
                        eis = zf.openEntryInputStream("large.txt");
                        if (eis == null) fail("Input stream for large file could not be opened!"); else {
                            fos = new FileOutputStream(fileLarge);
                            for (int iRead = eis.read(buffer); iRead >= 0; iRead = eis.read(buffer)) fos.write(buffer, 0, iRead);
                            fos.close();
                            eis.close();
                            if (!equalTest(fileLarge, fileLargeOriginal)) fail("extracted large file is not equal to its original!"); else {
                                fileLarge.delete();
                                zf.close();
                                fileExtract.delete();
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#openEntryOutputStream(java.lang.String, int, java.util.Date)}.
	 */
    public void testOpenEntryOutputStream() {
        System.out.println("Deleting " + m_sPkZipFile);
        File filePk = new File(m_sPkZipFile);
        filePk.delete();
        File fileTest = new File(m_sTestZipFile);
        File fileExtract = new File(sEXTRACT_DIRECTORY);
        if (!fileExtract.exists()) fileExtract.mkdirs();
        File fileModerate = new File(fileExtract.getAbsolutePath() + "\\moderate.txt");
        if (fileModerate.exists()) fileModerate.delete();
        File fileMedium = new File(fileExtract.getAbsolutePath() + "\\medium.txt");
        if (fileMedium.exists()) fileMedium.delete();
        File fileLarge = new File(fileExtract.getAbsolutePath() + "\\large.txt");
        if (fileLarge.exists()) fileLarge.delete();
        File fileSmall = new File(fileExtract.getAbsolutePath() + "\\many\\small12345.txt");
        if (fileSmall.exists()) fileSmall.delete();
        File fileTemp = new File(sTEMP_DIRECTORY);
        if (!fileTemp.exists()) fileTemp.mkdirs();
        File fileModerateOriginal = new File(fileTemp.getAbsolutePath() + "\\moderate.txt");
        File fileMediumOriginal = new File(fileTemp.getAbsolutePath() + "\\medium.txt");
        File fileLargeOriginal = new File(fileTemp.getAbsolutePath() + "\\large.txt");
        File fileSmallOriginal = new File(fileTemp.getAbsolutePath() + "\\many\\small12345.txt");
        zipTest(FileEntry.iMETHOD_STORED);
        if (!extractFile("moderate.txt")) fail("pkzipc extract of uncompressed moderate.txt failed!");
        if (!equalTest(fileModerate, fileModerateOriginal)) fail("extracted compressed moderate file is not equal to its original!");
        fileModerate.delete();
        if (!extractFile("medium.txt")) fail("pkzipc extract of uncompressed medium.txt failed!");
        if (!equalTest(fileMedium, fileMediumOriginal)) fail("extracted compressed medium file is not equal to its original!");
        fileMedium.delete();
        if (!extractFile("large.txt")) fail("pkzipc extract of uncompressed large.txt failed!");
        if (!equalTest(fileLarge, fileLargeOriginal)) fail("extracted compressed large file is not equal to its original!");
        fileLarge.delete();
        if (!extractFile("many/small12345.txt")) fail("pkzipc extract of uncompressed small12345.txt failed!");
        if (!equalTest(fileSmall, fileSmallOriginal)) fail("extracted compressed small file is not equal to its original!");
        fileSmall.delete();
        fileTest.delete();
        zipTest(FileEntry.iMETHOD_DEFLATED);
        if (!extractFile("moderate.txt")) fail("pkzipc extract of compressed moderate.txt failed!");
        if (!equalTest(fileModerate, fileModerateOriginal)) fail("extracted compressed moderate file is not equal to its original!");
        fileModerate.delete();
        if (!extractFile("medium.txt")) fail("pkzipc extract of compressed medium.txt failed!");
        if (!equalTest(fileMedium, fileMediumOriginal)) fail("extracted compressed medium file is not equal to its original!");
        fileMedium.delete();
        if (!extractFile("large.txt")) fail("pkzipc extract of compressed large.txt failed!");
        if (!equalTest(fileLarge, fileLargeOriginal)) fail("extracted compressed large file is not equal to its original!");
        fileLarge.delete();
        if (!extractFile("many/small12345.txt")) fail("pkzipc extract of compressed small12345.txt failed!");
        if (!equalTest(fileSmall, fileSmallOriginal)) fail("extracted compressed small file is not equal to its original!");
        fileSmall.delete();
    }

    /**
	 * Test method for {@link ch.enterag.utils.zip.Zip64File#delete(java.lang.String)}.
	 */
    public void testDelete() {
        File fileExtract = new File(sEXTRACT_DIRECTORY);
        if (!fileExtract.exists()) fileExtract.mkdirs();
        File fileSmall12344 = new File(fileExtract.getAbsolutePath() + "\\many\\small12344.txt");
        if (fileSmall12344.exists()) fileSmall12344.delete();
        File fileSmall12345 = new File(fileExtract.getAbsolutePath() + "\\many\\small12345.txt");
        if (fileSmall12345.exists()) fileSmall12345.delete();
        File fileSmall12346 = new File(fileExtract.getAbsolutePath() + "\\many\\small12346.txt");
        if (fileSmall12346.exists()) fileSmall12346.delete();
        File fileTemp = new File(sTEMP_DIRECTORY);
        if (!fileTemp.exists()) fileTemp.mkdirs();
        File fileSmall12344Original = new File(fileTemp.getAbsolutePath() + "\\many\\small12344.txt");
        File fileSmall12346Original = new File(fileTemp.getAbsolutePath() + "\\many\\small12346.txt");
        zipTest(FileEntry.iMETHOD_DEFLATED);
        try {
            Zip64File zf = new Zip64File(m_sTestZipFile);
            zf.delete("many/small12345.txt");
            zf.close();
            if (!extractFile("many/small12344.txt")) fail("many/small12344.txt could not be extracted!");
            if (!equalTest(fileSmall12344, fileSmall12344Original)) fail("extracted compressed small12344 file is not equal to its original!");
            if (extractFile("many/small12345.txt")) fail("Deleted file could still be extracted!");
            if (!extractFile("many/small12346.txt")) fail("many/small12346.txt could not be extracted!");
            if (!equalTest(fileSmall12346, fileSmall12346Original)) fail("extracted compressed small12346 file is not equal to its original!");
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getClass().getName() + ": " + fnfe.getMessage());
        } catch (IOException ie) {
            fail(ie.getClass().getName() + ": " + ie.getMessage());
        }
    }
}
