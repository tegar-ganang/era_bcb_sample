package archlib.gzip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import archlib.ArchiveInflater;
import archlib.ArchiveInputStream;
import archlib.util.CustomLogger;

/**
 * Inflater for GZIP files. Reads the GZIP file and extracts the contents in default/specified location.
 * @author Vaman Kulkarni
 */
public class GZIPInflater implements ArchiveInflater {

    private Logger log = CustomLogger.getLogger(GZIPInflater.class);

    private static final int DATA_BUFFER_SIZE = 1024;

    private File srcFile = null;

    private File outputFile = null;

    private String archiveName = null;

    private String outputLoc = null;

    private GZIPFileInputStream gzipIn = null;

    private void initInflater(String inArch, String outDir) throws IOException {
        File out = new File(inArch);
        if (!out.exists()) {
            throw new FileNotFoundException(inArch + " is not a valid path");
        }
        if (out.length() < 1) {
            throw new IOException("Input file is empty. Please provide a non-empty valid file");
        }
        if (out.isDirectory()) {
            String errMsg = "Cannot GZIP a directory. Please add the directory to an archive (TAR or ZIP) before you GZIP it.";
            log.severe(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        this.srcFile = out;
        if (outDir == null) {
            outDir = srcFile.getAbsolutePath().replaceAll("\\.gz", "");
        } else {
            outDir = outDir + "/" + srcFile.getName().replaceAll("\\.gz", "");
        }
        File arch = new File(outDir);
        if (arch.exists() && !arch.isDirectory()) {
            log.severe((arch.getName() + " is not a directory or archive is already present"));
            throw new IllegalArgumentException(outDir + " is not a directory or archive is already present.");
        }
        arch.getParentFile().mkdirs();
        this.outputFile = arch;
        outputFile.getParentFile().mkdirs();
        this.gzipIn = new GZIPFileInputStream(new BufferedInputStream(new FileInputStream(srcFile)));
    }

    @Override
    public void inflate(String archiveFilePath, String outputDir) throws IOException {
        log.info("inflate >>>");
        initInflater(archiveFilePath, outputDir);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
        log.info("Extracting to " + outputFile.getAbsolutePath());
        int readCount = 0;
        byte buffer[] = new byte[DATA_BUFFER_SIZE];
        log.info("Decompressing GZIP archive....");
        boolean isCorrupt = true;
        while ((readCount = gzipIn.read(buffer)) != -1) {
            isCorrupt = false;
            out.write(buffer, 0, readCount);
        }
        if (isCorrupt) {
            log.severe("Input file is corrupted or may not be in GZIP format");
        }
        out.flush();
        log.info("Finished decompressing GZIP archive....");
        out.close();
        gzipIn.close();
        if (isCorrupt) {
            log.severe("Input file is corrupted or may not be in GZIP format");
            throw new IOException("Input file is corrupted or may not be in GZIP format");
        }
        log.info("<<< inflate");
    }

    @Override
    public void inflate() throws IOException {
        inflate(archiveName, outputLoc);
    }

    @Override
    public void setArchiveFileName(String archiveFilePath) {
        this.archiveName = archiveFilePath;
    }

    @Override
    public String getArchiveFileName() {
        return this.archiveName;
    }

    @Override
    public void setOutputLocation(String outputDir) {
    }

    @Override
    public String getOutputLocation() {
        return null;
    }

    @Override
    public void inflate(String achiveFilePath) throws IOException {
        inflate(achiveFilePath, null);
    }

    @Override
    public String getInflatedResource() {
        return this.outputFile.getAbsolutePath();
    }

    @Override
    public ArchiveInputStream getArchiveInputStream() {
        return gzipIn;
    }
}
