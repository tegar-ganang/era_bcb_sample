package edu.unibi.agbi.biodwh.download.uncompress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import edu.unibi.agbi.biodwh.project.logic.queue.UncompressQueue;

/**
 * @author Benjamin Kormeier
 * @version 3.1 16.11.2010
 */
public class FileUncompress implements Runnable {

    private final int BUFFER = 2048;

    public static final String SUFFIX_Z = new String(".z");

    public static final String SUFFIX_GZ = new String(".gz");

    public static final String SUFFIX_TAR_GZ = new String(".tar.gz");

    public static final String SUFFIX_TAR = new String(".tar");

    public static final String SUFFIX_ZIP = new String(".zip");

    private File dir = new File(System.getProperty("user.dir"));

    private File file = null;

    private String projectName = null;

    private String parserID = null;

    private long file_size = 0;

    private long read_position = 0;

    private boolean abort;

    public FileUncompress(String projectName, String parserID, File targeteDir, File file) {
        this.projectName = projectName;
        this.parserID = parserID;
        this.dir = targeteDir;
        this.file = file;
    }

    private void unzip(File filename) throws ZipException, IOException {
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(filename)));
        ZipEntry entry = null;
        boolean first_entry = true;
        while ((entry = in.getNextEntry()) != null) {
            if (first_entry) {
                if (!entry.isDirectory()) {
                    File subdir = new File(dir + File.separator + filename.getName().substring(0, filename.getName().length() - SUFFIX_ZIP.length()));
                    if (!subdir.exists()) {
                        subdir.mkdir();
                        dir = subdir;
                    }
                }
                first_entry = false;
            }
            if (entry.isDirectory()) {
                FileUtils.forceMkdir(new File(dir + File.separator + entry.getName()));
            } else {
                File outfile = new File(dir + File.separator + entry.getName());
                File outdir = new File(outfile.getAbsolutePath().substring(0, outfile.getAbsolutePath().length() - outfile.getName().length()));
                if (!outdir.exists()) FileUtils.forceMkdir(outdir);
                FileOutputStream fo = new FileOutputStream(outfile);
                BufferedOutputStream bos = new BufferedOutputStream(fo, BUFFER);
                int read;
                byte data[] = new byte[BUFFER];
                while ((read = in.read(data, 0, BUFFER)) != -1) {
                    read_position++;
                    bos.write(data, 0, read);
                }
                bos.flush();
                bos.close();
            }
        }
        in.close();
    }

    private void unGzip(File filename) throws IOException {
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(filename));
        String outfile = filename.getAbsolutePath();
        outfile = outfile.substring(0, outfile.length() - SUFFIX_GZ.length());
        FileOutputStream out = new FileOutputStream(outfile);
        byte[] buf = new byte[BUFFER];
        int len;
        while ((len = in.read(buf)) > 0 && !abort) {
            read_position += len;
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void unZip(File filename) throws IOException {
        UncompressInputStream in = new UncompressInputStream(new FileInputStream(filename));
        String outfile = filename.getAbsolutePath();
        outfile = outfile.substring(0, outfile.length() - SUFFIX_Z.length());
        FileOutputStream out = new FileOutputStream(outfile);
        byte[] buf = new byte[BUFFER];
        int len;
        while ((len = in.read(buf)) > 0 && !abort) {
            read_position += len;
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void unTar(File filename) throws IOException {
        TarInputStream in = new TarInputStream(new BufferedInputStream(new FileInputStream(filename)));
        TarEntry entry = null;
        boolean first_entry = true;
        BufferedOutputStream out = null;
        while ((entry = in.getNextEntry()) != null && !abort) {
            if (first_entry) {
                if (!entry.isDirectory()) {
                    File subdir = new File(dir + File.separator + filename.getName().substring(0, filename.getName().length() - SUFFIX_TAR.length()));
                    if (!subdir.exists()) {
                        subdir.mkdir();
                        dir = subdir;
                    }
                }
                first_entry = false;
            }
            if (entry.isDirectory()) {
                File subdir = new File(dir + File.separator + entry.getName());
                if (!subdir.exists()) subdir.mkdir();
            } else {
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(dir + File.separator + entry.getName());
                out = new BufferedOutputStream(fos, BUFFER);
                while ((count = in.read(data, 0, BUFFER)) != -1) {
                    read_position += data.length;
                    out.write(data, 0, count);
                }
            }
            out.flush();
            out.close();
        }
        in.close();
    }

    private void unTarGzip(File filename) throws IOException {
        file_size = filename.length();
        unGzip(filename);
        String tarfile = filename.getAbsolutePath();
        tarfile = tarfile.substring(0, tarfile.length() - SUFFIX_GZ.length());
        File tar = new File(tarfile);
        read_position = 0;
        file_size = tar.length();
        unTar(tar);
        new File(tarfile).delete();
    }

    private synchronized int calculateProgress(long pos) {
        double result = ((double) pos / (double) (file_size));
        return (int) (result * 100);
    }

    /**
	 * @return the projectName
	 */
    public synchronized String getProjectName() {
        return projectName;
    }

    /**
	 * @param projectName the projectName to set
	 */
    public synchronized void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
	 * @return the parserID
	 */
    public synchronized String getParserID() {
        return parserID;
    }

    /**
	 * @param parserID the parserID to set
	 */
    public synchronized void setParserID(String parserID) {
        this.parserID = parserID;
    }

    public String getFileName() {
        return file.getName();
    }

    public synchronized int getProgress() {
        return calculateProgress(read_position);
    }

    public boolean abort() {
        abort = true;
        return abort;
    }

    public void run() {
        String filename = file.getName().toLowerCase();
        try {
            if (filename.endsWith(SUFFIX_TAR_GZ)) {
                unTarGzip(file);
            } else if (filename.toLowerCase().endsWith(SUFFIX_GZ)) {
                file_size = file.length();
                unGzip(file);
            } else if (filename.endsWith(SUFFIX_ZIP)) {
                file_size = file.length();
                unzip(file);
            } else if (filename.toLowerCase().endsWith(SUFFIX_Z)) {
                file_size = file.length();
                unZip(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            UncompressQueue.uncompressFailed(projectName, parserID);
        }
        UncompressQueue.removeActiveUncompress(this);
    }
}
