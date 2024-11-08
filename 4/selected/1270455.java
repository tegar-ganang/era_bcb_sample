package net.hawk.digiextractor.digic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ProgressMonitor;
import net.hawk.digiextractor.GUI.Configuration;
import net.hawk.digiextractor.GUI.ErrorMessage;
import net.hawk.digiextractor.GUI.Frame1;

/**
 * The class SimpleVideoExtractor.
 * Implements a simple recording extractor. Simply writes the Packetized 
 * Elementary streams to files. Performs no further checks on the data streams.
 * 
 * @author Hawk
 *
 */
public class SimpleVideoExtractor extends VideoExtractor {

    /** The number of unused bytes at the end of data clusters that are
	 * Tagged with the 0x0C flag.
	 */
    private static final int CLUSTER_SLACK = 0x540;

    /** The maximum number of supported elementary streams.*/
    private static final int MAX_STREAMS = 6;

    /** The parent Frame, needed to create dialog-boxes.*/
    private Frame1 parent;

    /** The Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SimpleVideoExtractor.class.getName());

    /** The normal entry-type. */
    private static final int CLUSTER_LABEL1 = 0x00;

    /** The last entry. */
    private static final int END_LABEL = 0x02;

    /** The first entry for this stream. */
    private static final int CLUSTER_LABEL2 = 0x0C;

    /** The last entry for this stream. */
    private static final int CLUSTER_LABEL3 = 0x0D;

    /** An input buffer for use during extraction. */
    private ByteBuffer inputbuff;

    /** A second buffer for use in extraction (byte swapping). */
    private ByteBuffer outputbuff;

    /**
	 * Create a new SimpleVideoExtractor.
	 * @param aufn a collection of Recordings to extract.
	 * @param i the Image containing the recordings.
	 * @param frame the parent Frame.
	 */
    public SimpleVideoExtractor(final List<AbstractFile> aufn, final AbstractPVRFileSystem i, final Frame1 frame) {
        super(aufn, i);
        parent = frame;
        inputbuff = ByteBuffer.allocateDirect(AbstractPVRFileSystem.CLUSTER_SIZE);
        outputbuff = ByteBuffer.allocateDirect(AbstractPVRFileSystem.CLUSTER_SIZE);
        inputbuff.order(ByteOrder.BIG_ENDIAN);
        outputbuff.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public final void run() {
        LOGGER.fine("Extraction Process running...");
        File outputDirectory = Configuration.getInstance().getOutputDirectory();
        long totalCount = 0;
        for (AbstractFile a : getAufnahmen()) {
            if (a.isExtracted()) {
                totalCount += a.getExtractSize();
            }
        }
        ProgressMonitor monitor = new ProgressMonitor(parent, "extrahiere Aufnahmen", "", 0, (int) totalCount);
        setProgress(0);
        monitor.setProgress(getProgress());
        for (AbstractFile current : getAufnahmen()) {
            if (!monitor.isCanceled() && current.isExtracted()) {
                if (current instanceof Recording) {
                    extractSingleRecording(outputDirectory, monitor, (Recording) current);
                }
                if (current instanceof UserFile) {
                    extractSingleUserFile(outputDirectory, monitor, (UserFile) current);
                }
            }
            current.setExtract(false);
        }
        monitor.close();
        LOGGER.fine("Extraction Process complete..");
        setDone(true);
        parent.extractionComplete();
    }

    /**
	 * Extract single user file.
	 *
	 * @param outputDirectory the output directory
	 * @param monitor the monitor
	 * @param current the current
	 */
    private void extractSingleUserFile(final File outputDirectory, final ProgressMonitor monitor, final UserFile current) {
        try {
            FileChannel inputChannel = getImg().getImageChannel();
            List<Long> clusters = current.getClusterList();
            File outputFile = new File(outputDirectory, current.getOutputFile());
            FileOutputStream fos = new FileOutputStream(outputFile);
            FileChannel outputChannel = fos.getChannel();
            for (Long cl : clusters) {
                LOGGER.finer("writing Cluster: " + cl);
                inputbuff.rewind();
                inputChannel.read(inputbuff, (long) cl * AbstractPVRFileSystem.CLUSTER_SIZE + getImg().getOffset());
                inputbuff.rewind();
                if (outputChannel.write(inputbuff) != AbstractPVRFileSystem.CLUSTER_SIZE) {
                    LOGGER.severe("not enough bytes written!");
                }
                monitor.setProgress(progress());
            }
            outputChannel.close();
        } catch (IOException e) {
            ErrorMessage.showExceptionMessage(parent, e);
            LOGGER.log(Level.SEVERE, "Error during Extraction: ", e);
        }
    }

    /**
	 * Extract single recording.
	 *
	 * @param outputDirectory the output directory
	 * @param monitor the monitor
	 * @param current the current
	 */
    private void extractSingleRecording(final File outputDirectory, final ProgressMonitor monitor, final Recording current) {
        parent.setSelectedEnry(current);
        LOGGER.fine("creating Output-file and writing");
        String outputFileName = current.getOutputFile();
        int[] extractMarkers = current.getMarkersToExtract();
        try {
            List<Long> clusterList = current.getClusterList();
            ByteBuffer b = current.getInfoBlock();
            setTotalClusterCount(current.getExtractSize());
            boolean[] lut = new boolean[MAX_STREAMS];
            int[] map = new int[MAX_STREAMS];
            int c = 0;
            for (int j = 0; j < extractMarkers.length; j++) {
                lut[extractMarkers[j]] = true;
                map[extractMarkers[j]] = c++;
                LOGGER.fine("Marker " + j + ", extract: true");
            }
            FileChannel[] outputChannels = new FileChannel[extractMarkers.length];
            for (int j = 0; j < outputChannels.length; j++) {
                String fileExt = "";
                fileExt = current.getMediaportExtensionForStream(extractMarkers[j]);
                File outputFile = new File(outputDirectory, outputFileName + fileExt);
                if (outputFile.createNewFile()) {
                    LOGGER.fine("created new file: " + outputFile.getAbsolutePath());
                } else {
                    LOGGER.fine("file already existed: " + outputFile.getAbsolutePath());
                }
                FileOutputStream fos = new FileOutputStream(outputFile);
                outputChannels[j] = fos.getChannel();
            }
            FileChannel inputChannel = getImg().getImageChannel();
            LOGGER.fine("creating Cluster List, length: " + clusterList.size());
            InfoBlockEntry entry = new InfoBlockEntry();
            Long cl;
            int fileNr;
            do {
                entry.parseData(b);
                switch(entry.getType()) {
                    case CLUSTER_LABEL1:
                    case CLUSTER_LABEL2:
                    case CLUSTER_LABEL3:
                        if ((entry.getStreamID() < lut.length) && (lut[entry.getStreamID()] || current.isHD())) {
                            cl = clusterList.get(entry.getData());
                            if (current.isHD()) {
                                fileNr = 0;
                            } else {
                                fileNr = map[entry.getStreamID()];
                            }
                            LOGGER.finer("writing Cluster: " + cl);
                            inputbuff.rewind();
                            outputbuff.rewind();
                            inputChannel.read(inputbuff, (long) cl * AbstractPVRFileSystem.CLUSTER_SIZE + getImg().getOffset());
                            inputbuff.rewind();
                            if (getImg().getImageVersion().equals(FileSystemVersion.TSD_V1)) {
                                while (inputbuff.remaining() > 0) {
                                    outputbuff.putShort(inputbuff.getShort());
                                }
                                outputbuff.rewind();
                                if (outputChannels[fileNr].write(outputbuff) != AbstractPVRFileSystem.CLUSTER_SIZE) {
                                    LOGGER.severe("not enough bytes written!");
                                }
                            } else {
                                if (outputChannels[fileNr].write(inputbuff) != AbstractPVRFileSystem.CLUSTER_SIZE) {
                                    LOGGER.severe("not enough bytes written!");
                                }
                                if (entry.getType() == CLUSTER_LABEL2) {
                                    long newpos = outputChannels[fileNr].position() - CLUSTER_SLACK;
                                    outputChannels[fileNr].position(newpos);
                                }
                            }
                            monitor.setProgress(progress());
                        }
                        break;
                    default:
                        break;
                }
                while (getpause() && !getCancel()) {
                    pause();
                }
                yield();
            } while ((entry.getType() != END_LABEL) && (b.remaining() >= InfoBlockEntry.MIN_ENTRY_LEN));
            LOGGER.fine("Closing output-Files");
            for (int j = 0; j < outputChannels.length; j++) {
                outputChannels[j].close();
            }
        } catch (Exception ioe) {
            ErrorMessage.showExceptionMessage(parent, ioe);
            LOGGER.log(Level.SEVERE, "Error during Extraction: ", ioe);
        }
    }
}
