package filemanager.workers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public class DecompressionWorker extends SwingWorker<Object, Integer> {

    private final File fileToExtract;

    private final File extractionFile;

    private JProgressBar progressBar;

    public DecompressionWorker(JProgressBar bar, String filePath, String extractionPath) {
        if (bar != null) {
            this.progressBar = bar;
        }
        if (filePath != null) {
            if (!(new File(filePath).exists())) {
                throw new IllegalArgumentException("File does not exist");
            }
            fileToExtract = new File(filePath);
            if (!isRecognizedStream(fileToExtract)) {
                throw new IllegalArgumentException("Unrecognized file type");
            }
        } else {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (extractionPath != null) {
            if ((new File(extractionPath).isDirectory())) {
                extractionFile = new File(extractionPath);
            } else {
                throw new IllegalArgumentException("Extraction Path must be a directory");
            }
        } else {
            throw new IllegalArgumentException("Extraction Path cannot be null");
        }
    }

    @Override
    protected Object doInBackground() throws Exception {
        ArchiveInputStream bufIn = null;
        FileOutputStream fileOut = null;
        try {
            bufIn = DecompressionWorker.guessStream(fileToExtract);
            ArchiveEntry curZip = null;
            int progress = 0;
            while ((curZip = bufIn.getNextEntry()) != null) {
                if (!curZip.isDirectory()) {
                    byte[] content = new byte[(int) curZip.getSize()];
                    fileOut = new FileOutputStream(extractionFile.getAbsolutePath() + File.separator + curZip.getName());
                    for (int i = 0; i < content.length; i++) {
                        fileOut.write(content[i]);
                    }
                    publish(new Integer(progress));
                    progress++;
                }
            }
        } finally {
            if (bufIn != null) {
                bufIn.close();
            }
        }
        return null;
    }

    @Override
    public void process(List<Integer> chunks) {
        if (progressBar != null) {
            int largest = 0;
            for (Integer curInt : chunks) {
                if (curInt.intValue() > largest) {
                    largest = curInt.intValue();
                }
            }
            progressBar.setIndeterminate(false);
            progressBar.setValue(largest);
        }
    }

    public static ArchiveInputStream guessStream(File filePath) throws FileNotFoundException {
        if (filePath != null) {
            String path = filePath.getAbsolutePath().toLowerCase();
            if (path.endsWith(".zip")) {
                return new ZipArchiveInputStream(new FileInputStream(filePath));
            } else if (path.endsWith(".tar")) {
                return new TarArchiveInputStream(new FileInputStream(filePath));
            } else if (path.endsWith(".jar")) {
                return new JarArchiveInputStream(new FileInputStream(filePath));
            }
        }
        return null;
    }

    public static boolean isRecognizedStream(File filePath) {
        if (filePath != null) {
            String path = filePath.getAbsolutePath().toLowerCase();
            if (path.endsWith(".zip")) {
                return true;
            } else if (path.endsWith(".tar")) {
                return true;
            } else if (path.endsWith(".jar")) {
                return true;
            }
        }
        return false;
    }
}
