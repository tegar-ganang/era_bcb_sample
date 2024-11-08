package org.happycomp.radiog.encoding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.internal.runtime.Activator;
import org.happycomp.radio.downloader.DownloadingItem;
import org.happycomp.radio.io.IOUtils;

/**
 * Thread, ktery 'procesuje' vstupni soubor a vytvari wav nebo mp3.
 * @author pavels
 *
 */
public class EncodeProcessThread extends Thread {

    public static final Logger LOGGER = Logger.getLogger(EncodeProcessThread.class.getName());

    private File inputFile;

    private File[] previousFiles = new File[0];

    private File exportedWavFile;

    private File exportedMP3File;

    private File randomFolder;

    private boolean deleteOnExit = false;

    private EncodeMonitor encodeMonitor;

    public EncodeProcessThread(File inputFile, File[] previousInputFiles, File exportedWavFile, File exportedMP3File, boolean deleteOnExit, EncodeMonitor encodeMonitor, File randomFolder) throws IOException {
        super();
        this.inputFile = inputFile;
        this.exportedWavFile = exportedWavFile;
        this.exportedMP3File = exportedMP3File;
        if (!this.exportedWavFile.exists()) {
            this.exportedWavFile.createNewFile();
        }
        this.deleteOnExit = deleteOnExit;
        this.encodeMonitor = encodeMonitor;
        this.randomFolder = randomFolder;
        this.previousFiles = previousInputFiles;
    }

    @Override
    public void run() {
        try {
            File[] inputFiles = new File[this.previousFiles != null ? this.previousFiles.length + 1 : 1];
            File copiedInput = new File(this.randomFolder, this.inputFile.getName());
            IOUtils.copyFile(this.inputFile, copiedInput);
            inputFiles[inputFiles.length - 1] = copiedInput;
            if (previousFiles != null) {
                for (int i = 0; i < this.previousFiles.length; i++) {
                    File prev = this.previousFiles[i];
                    File copiedPrev = new File(this.randomFolder, prev.getName());
                    IOUtils.copyFile(prev, copiedPrev);
                    inputFiles[i] = copiedPrev;
                }
            }
            org.happycomp.radiog.Activator activator = org.happycomp.radiog.Activator.getDefault();
            if (this.exportedMP3File != null) {
                EncodingUtils.encodeToWavAndThenMP3(inputFiles, this.exportedWavFile, this.exportedMP3File, this.deleteOnExit, this.randomFolder, activator.getCommandsMap());
            } else {
                EncodingUtils.encodeToWav(inputFiles, this.exportedWavFile, randomFolder, activator.getCommandsMap());
            }
            if (encodeMonitor != null) {
                encodeMonitor.setEncodingFinished(true);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
