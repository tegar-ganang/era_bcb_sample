package freedbimporter.gui;

import freedbimporter.util.EncodingDeterminator;
import freedbimporter.util.RawTextReader;
import freedbimporter.util.RawTextLogger;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Thread to merge FreeDB-data.
 * <p>
 * @version      3.1 by 16.12.2010
 * @author       Copyright 2004 <a href="MAILTO:freedb2mysql@freedb2mysql.de">Christian Kruggel</a> - freedbimporter and all it&acute;s parts are free software and destributed under <a href="http://www.gnu.org/licenses/gpl-2.0.txt" target="_blank">GNU General Public License</a>
 */
public class CharMerger extends SuspendableThread {

    public static final int KILO_BYTE_FACTOR = 1024;

    static final String NEW_FILE_NAME = File.separator + "00000000";

    long writtenFileBytes, readFileBytes;

    int targetKiloBytes, writtenFilesCount, readFilesCount;

    EncodingDeterminator det;

    MergerConfigurator skin;

    File source, target;

    boolean sort;

    public CharMerger(MergerConfigurator gui, File sourceDir, EncodingDeterminator sourceEncodingDeterminator, File targetDir, int fileSizeKB, boolean sort) {
        this.skin = gui;
        this.targetKiloBytes = fileSizeKB * KILO_BYTE_FACTOR;
        this.source = sourceDir.getAbsoluteFile();
        this.det = sourceEncodingDeterminator;
        this.target = targetDir.getAbsoluteFile();
        this.sort = sort;
    }

    public void run() {
        writtenFileBytes = 0;
        writtenFilesCount = 0;
        readFileBytes = 0;
        readFilesCount = 0;
        long duration = 0;
        long startTime = System.currentTimeMillis();
        try {
            File[] files = source.listFiles();
            if (sort) Arrays.sort(files);
            for (int i = 0; run && (i < files.length); i++) {
                pause();
                if (files[i].isDirectory()) copyDir(files[i], new File(target.getCanonicalPath() + File.separator + files[i].getName())); else {
                    File dest = new File(target.getCanonicalPath() + File.separator + files[i].getName());
                    dest.delete();
                    copyFile(files[i], dest);
                }
            }
        } catch (IOException i) {
            MainGUI.LOGGER.error(i);
        }
        duration = System.currentTimeMillis() - startTime;
        StringBuilder report = new StringBuilder(512);
        report.append(freedbimporter.data.adaption.langanno.CharProfile.getTextualDuration(duration));
        report.append(System.getProperty("line.separator"));
        report.append(readFilesCount);
        report.append(" files read (");
        report.append(readFileBytes / KILO_BYTE_FACTOR);
        report.append(" kb) from ");
        report.append(source.getAbsolutePath());
        report.append(System.getProperty("line.separator"));
        report.append(writtenFilesCount);
        report.append(" files written (");
        report.append(writtenFileBytes / KILO_BYTE_FACTOR);
        report.append(" kb) to ");
        report.append(target.getAbsolutePath());
        report.append(System.getProperty("line.separator"));
        report.append(System.getProperty("line.separator"));
        report.append(det.getReport());
        skin.report(getClass().getSimpleName(), report.toString());
    }

    public long stateCheckSum() {
        return readFileBytes + readFilesCount + writtenFileBytes + writtenFilesCount;
    }

    void copyFile(File sourceFile, File targetFile) throws IOException {
        RawTextReader reader = null;
        RawTextLogger writer = null;
        try {
            String sourceEncoding = det.getEncoding(sourceFile);
            reader = new RawTextReader(sourceFile, sourceEncoding);
            writer = new RawTextLogger(targetFile, true, true);
            while (run && reader.hasMoreElements()) {
                pause();
                writer.println(reader.nextElement());
            }
            writer.close();
            reader.close();
        } catch (IOException i) {
            MainGUI.LOGGER.error(i);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException i) {
                MainGUI.LOGGER.error(i);
            }
        }
    }

    void copyDir(File sourceDir, File targetDir) throws IOException {
        targetDir.mkdirs();
        File[] files = sourceDir.listFiles();
        if (sort) Arrays.sort(files);
        int i = 0;
        long innerBytesCount = 0;
        File newTargetFileToWrite = new File(targetDir.getCanonicalPath() + NEW_FILE_NAME);
        for (; run && (i < files.length); i++) {
            pause();
            if (innerBytesCount >= targetKiloBytes) {
                StringBuilder s = new StringBuilder(8);
                s.append(files[i - 1].getName());
                while (s.length() < 8) s.insert(0, '0');
                writtenFileBytes = writtenFileBytes + newTargetFileToWrite.length();
                writtenFilesCount++;
                newTargetFileToWrite.renameTo(new File(targetDir.getCanonicalPath() + File.separator + s));
                newTargetFileToWrite = new File(targetDir.getCanonicalPath() + NEW_FILE_NAME);
                skin.setTitle(writtenFilesCount + " files written");
                skin.toggleTux();
                innerBytesCount = 0;
            }
            innerBytesCount = innerBytesCount + files[i].length();
            copyFile(files[i], newTargetFileToWrite);
            readFileBytes = readFileBytes + files[i].length();
            readFilesCount++;
        }
        writtenFileBytes = writtenFileBytes + newTargetFileToWrite.length();
        writtenFilesCount++;
        if (files.length > 0) {
            StringBuilder s = new StringBuilder(8);
            s.append(files[i - 1].getName());
            while (s.length() < 8) s.insert(0, '0');
            newTargetFileToWrite.renameTo(new File(targetDir.getCanonicalPath() + File.separator + s));
        }
    }
}
