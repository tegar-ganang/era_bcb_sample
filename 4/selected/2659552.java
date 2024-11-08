package editing.split;

import java.io.File;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.control.QualityControl;
import javax.media.Format;
import javax.media.format.*;
import javax.media.datasink.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * A sample program to split an input media file with multiplexed
 * audio and video tracks into files of individual elementary track.
 */
public class Split {

    SplitDataSource splitDS[];

    Object fileSync = new Object();

    boolean allDone = false;

    static String audioExt = null;

    static String videoExt = null;

    /**
     * Main program
     */
    public static void main(String[] args) {
        String inputURL = null;
        if (args.length == 0) prUsage();
        int i = 0;
        while (i < args.length) {
            if (args[i].equals("-a")) {
                i++;
                if (i >= args.length) prUsage();
                audioExt = args[i];
            } else if (args[i].equals("-v")) {
                i++;
                if (i >= args.length) prUsage();
                videoExt = args[i];
            } else {
                inputURL = args[i];
            }
            i++;
        }
        if (inputURL == null) {
            System.err.println("No input url specified.");
            prUsage();
        }
        if (audioExt == null) {
            audioExt = ".wav";
        }
        if (videoExt == null) {
            videoExt = ".mov";
        }
        MediaLocator iml;
        if ((iml = createMediaLocator(inputURL)) == null) {
            System.err.println("Cannot build media locator from: " + inputURL);
            System.exit(0);
        }
        Split split = new Split();
        if (!split.doIt(iml, audioExt, videoExt)) {
            System.err.println("Failed to split the input");
        }
        System.exit(0);
    }

    /**
     * Splits the tracks from a multiplexed input.
     */
    public boolean doIt(MediaLocator inML, String audExt, String vidExt) {
        Processor p;
        try {
            System.err.println("- Create processor for: " + inML);
            p = Manager.createProcessor(inML);
        } catch (Exception e) {
            System.err.println("Yikes!  Cannot create a processor from the given url: " + e);
            return false;
        }
        System.err.println("- Configure the processor for: " + inML);
        if (!waitForState(p, Processor.Configured)) {
            System.err.println("Failed to configure the processor.");
            return false;
        }
        if (FileTypeDescriptor.MPEG.equals(fileExtToCD(inML.getRemainder()).getEncoding())) {
            transcodeMPEGToRaw(p);
        }
        System.err.println("- Realize the processor for: " + inML);
        if (!waitForState(p, Controller.Realized)) {
            System.err.println("Failed to realize the processor.");
            return false;
        }
        setJPEGQuality(p, 0.5f);
        PushBufferDataSource pbds = (PushBufferDataSource) p.getDataOutput();
        PushBufferStream pbs[] = pbds.getStreams();
        splitDS = new SplitDataSource[pbs.length];
        allDone = false;
        boolean atLeastOne = false;
        for (int i = 0; i < pbs.length; i++) {
            splitDS[i] = new SplitDataSource(p, i);
            if ((new FileWriter()).write(splitDS[i])) atLeastOne = true;
        }
        if (!atLeastOne) {
            System.err.println("Failed to split any of the tracks.");
            System.exit(1);
        }
        System.err.println("- Start splitting...");
        waitForFileDone();
        System.err.println("  ...done splitting.");
        return true;
    }

    /**
     * Callback from the FileWriter when a DataSource is done.
     */
    void doneFile() {
        synchronized (fileSync) {
            for (int i = 0; i < splitDS.length; i++) {
                if (!splitDS[i].done) {
                    return;
                }
            }
            allDone = true;
            fileSync.notify();
        }
    }

    void waitForFileDone() {
        System.err.print("  ");
        synchronized (fileSync) {
            while (!allDone) {
                try {
                    fileSync.wait(1000);
                    System.err.print(".");
                } catch (Exception e) {
                }
            }
        }
        System.err.println("");
    }

    /**
     * Transcode the MPEG audio to linear and video to JPEG so
     * we can do the splitting.
     */
    void transcodeMPEGToRaw(Processor p) {
        TrackControl tc[] = p.getTrackControls();
        AudioFormat afmt;
        for (int i = 0; i < tc.length; i++) {
            if (tc[i].getFormat() instanceof VideoFormat) tc[i].setFormat(new VideoFormat(VideoFormat.JPEG)); else if (tc[i].getFormat() instanceof AudioFormat) {
                afmt = (AudioFormat) tc[i].getFormat();
                tc[i].setFormat(new AudioFormat(AudioFormat.LINEAR, afmt.getSampleRate(), afmt.getSampleSizeInBits(), afmt.getChannels()));
            }
        }
    }

    /**
     * Setting the encoding quality to the specified value on the JPEG encoder.
     * 0.5 is a good default.
     */
    void setJPEGQuality(Player p, float val) {
        Control cs[] = p.getControls();
        QualityControl qc = null;
        VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
                Object owner = ((Owned) cs[i]).getOwner();
                if (owner instanceof Codec) {
                    Format fmts[] = ((Codec) owner).getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++) {
                        if (fmts[j].matches(jpegFmt)) {
                            qc = (QualityControl) cs[i];
                            qc.setQuality(val);
                            System.err.println("- Set quality to " + val + " on " + qc);
                            break;
                        }
                    }
                }
                if (qc != null) break;
            }
        }
    }

    /**
     * Utility class to block until a certain state had reached.
     */
    public class StateWaiter implements ControllerListener {

        Processor p;

        boolean error = false;

        StateWaiter(Processor p) {
            this.p = p;
            p.addControllerListener(this);
        }

        public synchronized boolean waitForState(int state) {
            switch(state) {
                case Processor.Configured:
                    p.configure();
                    break;
                case Processor.Realized:
                    p.realize();
                    break;
                case Processor.Prefetched:
                    p.prefetch();
                    break;
                case Processor.Started:
                    p.start();
                    break;
            }
            while (p.getState() < state && !error) {
                try {
                    wait(1000);
                } catch (Exception e) {
                }
            }
            return !(error);
        }

        public void controllerUpdate(ControllerEvent ce) {
            if (ce instanceof ControllerErrorEvent) {
                error = true;
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Block until the given processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    boolean waitForState(Processor p, int state) {
        return (new StateWaiter(p)).waitForState(state);
    }

    /**
     * Convert a file name to a content type.  The extension is parsed
     * to determine the content type.
     */
    ContentDescriptor fileExtToCD(String name) {
        String ext;
        int p;
        if ((p = name.lastIndexOf('.')) < 0) return null;
        ext = (name.substring(p + 1)).toLowerCase();
        String type;
        if (ext.equals("mp3")) {
            type = FileTypeDescriptor.MPEG_AUDIO;
        } else {
            if ((type = com.sun.media.MimeManager.getMimeType(ext)) == null) return null;
            type = ContentDescriptor.mimeTypeToPackageName(type);
        }
        return new FileTypeDescriptor(type);
    }

    /**
     * Create a media locator from the given string.
     */
    static MediaLocator createMediaLocator(String url) {
        MediaLocator ml;
        if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null) return ml;
        if (url.startsWith(File.separator)) {
            if ((ml = new MediaLocator("file:" + url)) != null) return ml;
        } else {
            String file = "file:" + System.getProperty("user.dir") + File.separator + url;
            if ((ml = new MediaLocator(file)) != null) return ml;
        }
        return null;
    }

    static void prUsage() {
        System.err.println("Usage: java Split <input> -a <audio ext> -v <video ext> ...");
        System.err.println("     <input>: output URL or file name");
        System.err.println("     <audio ext>: audio file extension. e.g .wav");
        System.err.println("     <video ext>: video file extension. e.g .mov");
        System.exit(0);
    }

    /**
     * The custom DataSource to split input.
     */
    class SplitDataSource extends PushBufferDataSource {

        Processor p;

        PushBufferDataSource ds;

        PushBufferStream pbs[];

        SplitStream streams[];

        int idx;

        boolean done = false;

        public SplitDataSource(Processor p, int idx) {
            this.p = p;
            this.ds = (PushBufferDataSource) p.getDataOutput();
            this.idx = idx;
            pbs = ds.getStreams();
            streams = new SplitStream[1];
            streams[0] = new SplitStream(pbs[idx]);
        }

        public void connect() throws java.io.IOException {
        }

        public PushBufferStream[] getStreams() {
            return streams;
        }

        public Format getStreamFormat() {
            return pbs[idx].getFormat();
        }

        public void start() throws java.io.IOException {
            p.start();
            ds.start();
        }

        public void stop() throws java.io.IOException {
        }

        public Object getControl(String name) {
            return null;
        }

        public Object[] getControls() {
            return new Control[0];
        }

        public Time getDuration() {
            return ds.getDuration();
        }

        public void disconnect() {
        }

        public String getContentType() {
            return ContentDescriptor.RAW;
        }

        public MediaLocator getLocator() {
            return ds.getLocator();
        }

        public void setLocator(MediaLocator ml) {
            System.err.println("Not interested in a media locator");
        }
    }

    /**
     * Utility Source stream for the SplitDataSource.
     */
    class SplitStream implements PushBufferStream, BufferTransferHandler {

        PushBufferStream pbs;

        BufferTransferHandler bth;

        Format format;

        public SplitStream(PushBufferStream pbs) {
            this.pbs = pbs;
            pbs.setTransferHandler(this);
        }

        public void read(Buffer buf) {
        }

        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        public boolean endOfStream() {
            return pbs.endOfStream();
        }

        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        public Format getFormat() {
            return pbs.getFormat();
        }

        public void setTransferHandler(BufferTransferHandler bth) {
            this.bth = bth;
        }

        public Object getControl(String name) {
            return null;
        }

        public Object[] getControls() {
            return new Control[0];
        }

        public synchronized void transferData(PushBufferStream pbs) {
            if (bth != null) bth.transferData(pbs);
        }
    }

    /**
     * Given a DataSource, creates a DataSink and generate a file.
     */
    class FileWriter implements ControllerListener, DataSinkListener {

        Processor p;

        SplitDataSource ds;

        DataSink dsink;

        boolean write(SplitDataSource ds) {
            this.ds = ds;
            try {
                p = Manager.createProcessor(ds);
            } catch (Exception e) {
                System.err.println("Failed to create a processor to concatenate the inputs.");
                return false;
            }
            p.addControllerListener(this);
            if (!waitForState(p, Processor.Configured)) {
                System.err.println("Failed to configure the processor.");
                return false;
            }
            String ext, suffix;
            if (ds.getStreamFormat() instanceof AudioFormat) {
                ext = audioExt;
                suffix = "-aud";
            } else {
                ext = videoExt;
                suffix = "-vid";
            }
            ContentDescriptor cd;
            if ((cd = fileExtToCD(ext)) == null) {
                System.err.println("Couldn't figure out from the file extension the type of output needed: " + ext);
                return false;
            }
            System.err.println("- Set output content descriptor to: " + cd);
            if ((p.setContentDescriptor(cd)) == null) {
                System.err.println("Failed to set the output content descriptor on the processor.");
                return false;
            }
            if (!waitForState(p, Controller.Prefetched)) {
                System.err.println("Failed to realize the processor.");
                return false;
            }
            String name = "file:" + System.getProperty("user.dir") + File.separator + "split" + suffix + ds.idx + ext;
            MediaLocator oml;
            if ((oml = createMediaLocator(name)) == null) {
                System.err.println("Cannot build media locator from: " + name);
                System.exit(0);
            }
            if ((dsink = createDataSink(p, oml)) == null) {
                System.err.println("Failed to create a DataSink for the given output MediaLocator: " + oml);
                return false;
            }
            dsink.addDataSinkListener(this);
            try {
                p.start();
                dsink.start();
            } catch (IOException e) {
                System.err.println("IO error during concatenation");
                return false;
            }
            return true;
        }

        /**
	 * Controller Listener.
	 */
        public void controllerUpdate(ControllerEvent evt) {
            if (evt instanceof ControllerErrorEvent) {
                System.err.println("Failed to split the file.");
                System.exit(-1);
            } else if (evt instanceof EndOfMediaEvent) {
                evt.getSourceController().close();
            }
        }

        /**
	 * Event handler for the file writer.
	 */
        public void dataSinkUpdate(DataSinkEvent evt) {
            if (evt instanceof EndOfStreamEvent || evt instanceof DataSinkErrorEvent) {
                try {
                    dsink.close();
                } catch (Exception e) {
                }
                p.removeControllerListener(this);
                ds.done = true;
                doneFile();
            }
        }

        /**
	 * Create the DataSink.
	 */
        DataSink createDataSink(Processor p, MediaLocator outML) {
            DataSource ds;
            if ((ds = p.getDataOutput()) == null) {
                System.err.println("Something is really wrong: the processor does not have an output DataSource");
                return null;
            }
            DataSink dsink;
            try {
                System.err.println("- Create DataSink for: " + outML);
                dsink = Manager.createDataSink(ds, outML);
                dsink.open();
            } catch (Exception e) {
                System.err.println("Cannot create the DataSink: " + e);
                return null;
            }
            return dsink;
        }
    }
}
