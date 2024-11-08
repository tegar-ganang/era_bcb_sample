package net.sourceforge.dawnlite;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import javax.imageio.ImageIO;
import net.sourceforge.dawnlite.config.Config;
import net.sourceforge.dawnlite.config.Keys;
import net.sourceforge.dawnlite.control.Control;
import net.sourceforge.dawnlite.control.subtitles.MarkupAwareSubtitleSegmenter;
import net.sourceforge.dawnlite.control.subtitles.SRTReader;
import net.sourceforge.dawnlite.control.subtitles.SRTWriter;
import net.sourceforge.dawnlite.control.subtitles.SubtitleSegmenter;
import net.sourceforge.dawnlite.control.subtitles.SubtitleTextDurationEstimator;
import net.sourceforge.dawnlite.control.subtitles.Subtitles;
import net.sourceforge.dawnlite.control.tts.NetworkTTSTool;
import net.sourceforge.dawnlite.control.tts.TextDurationEstimator;
import net.sourceforge.dawnlite.initial.rois.ImageMetadata;
import net.sourceforge.dawnlite.initial.rois.Mini7Input;
import net.sourceforge.dawnlite.script.Script;
import net.sourceforge.dawnlite.script.output.CommandLineTool;
import net.sourceforge.dawnlite.script.output.svg.SimpleSVGWriter;
import net.sourceforge.dawnlite.script.xmlser.XMLOutput;
import net.sourceforge.dawnlite.tools.Tools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * This is the main application for the DawNLITE overall functionality.
 * 
 * This is a snapshot of the current usage instructions:
 * <code>
 * 
Options: (default values in square brackets)<br/>
--audiocodec, -acodec: 	Audio codec to be used [libfaac]<br/>
--audioencoder: 	Software for encoding audio [ffmpeg]<br/>
--audioencoderoptions: 	Additional options for the call to the audio encoder []<br/>
--audiofileformat: 	file format for temporary encoded audio file [mp4]<br/>
--audioinputfile, -audioin: 	Use pre-existing audio input file instead of generating synthetic speech. When used, this overrides the SyncMode setting to RigidAudio, and the text file must be of the supported formats for timed text input (currently supported: SRT). []<br/>
--clean: 	Delete all, none, or only some temp files after successful execution (values: yes, no, raw: delete raw media files, showcase: keep files to be displayed in showcase) [false]<br/>
--configfile, -c: 	Load a config file<br/>
--defaultmotionduration: 	Default duration of a camera motion, in ms [1500]<br/>
--defaultstayduration: 	Default duration of a still display of a ROI, in ms [3000]<br/>
--displayres, -dr: 	Resolution of the output video [480x320]<br/>
--fps: 	Frames per second for output video [25]<br/>
--fileformat, -f: 	Final output file format [m4v]<br/>
--fillmode, -fm: 	Strategy for choosing ROIs to fill long enough intervals (supported values: Furthest, Nearest, Random) [Nearest]<br/>
--help, -h, -usage, -?: 	Show this help message<br/>
--highlightmode, -hilite: 	Mode for highlighting Regions of Interest (Implemented mode(s): FadingRectangles) [None]<br/>
--imagefile, -i: 	Image file, overrides corresponding metadata entry if present<br/>
--imageres, -ir: 	Image resolution, overrides corresponding metadata entry if present<br/>
--initialaudiodelay, -del: 	How long to wait before audio playback starts, in ms [4500]<br/>
--maxduration, -dur: 	Maximum duration of output A/V file in millis [60000]<br/>
--muxer: 	Software for muxing audio and video [MP4Box]<br/>
--muxeroptions: 	Additional options for the call to the muxer encoder []<br/>
--orientation: 	Forces orientation to be either "landscape" or "portrait", or "rotatable" to switch to the orientation that fits the occuring ROIs better. [keep]<br/>
--outfileprefix, -prefix: 	Common prefix for temp and output files (without directories)<br/>
--outputfolder, -folder: 	Folder where output and temp files are written<br/>
--profile: 	Adjust any required parameters for a certain (usually target-device-related) profile. Note that multiple profiles may exist per device. Note also that values defined by a profile for one run cannot be changed for this run. [None]<br/>
--prologstring: 	Not implemented yet<br/>
--prologtextfile, -p: 	Not implemented yet<br/>
--roifile, -roi: 	File containing ROI descriptions<br/>
--relevance: 	Use relevance-based synchronization algorithm (yes or no) [no]<br/>
--renderer: 	Software for rendering animation to video [MP4Client]<br/>
--subtitlemode: 	Subtitle mode, can be overridden by SyncMode (supported values so far: None, Simple) [None]<br/>
--syncmode: 	How text/audio and video are synchronized (supported values so far: RigidAudio, ArrangableAudio) [ArrangableAudio]<br/>
--ttshost: 	Host address of text to speech server [localhost]<br/>
--ttsport: 	Port of text to speech server [59125]<br/>
--ttssystem: 	The text-to-speech system to be used [Mary]<br/>
--targetdevice: 	Perform specific preparations for a target device, e.g., iPhone, iPod. Note that this does not automatically adjust other parameters (in contrast to "Profile")<br/>
--textfile, -t: 	Text file to be used []<br/>
--videocodec, -vcodec: 	Video codec for output [libx264]<br/>
--videoencoder: 	Software for encoding video [ffmpeg]<br/>
--videoencoderoptions: 	Additional options for the call to the video encoder []<br/>
--videofileformat: 	file format for temporary encoded video file [mp4]<br/>
--voice, -v: 	Voice used for text to speech [us1]<br/>
--writeconfigfile, -wcfg: 	Write current config (including command line args) to a file<br/>
 * </code>
 * @author reiterer
 *
 */
public class VCMain {

    String outFilePrefix = "test";

    /**
	 * Array for all files involved, access by the index constants in this class
	 */
    File[] files;

    static final int MINI7 = 0;

    static final int IMAGE = 1;

    static final int TEXT = 2;

    static final int VCS = 3;

    static final int ANIM = 4;

    static final int RAWAUDIO = 5;

    static final int RAWVIDEO = 6;

    static final int ENCODEDVIDEO = 7;

    static final int ENCODEDAUDIO = 8;

    static final int AVFILE = 9;

    static final int OUTFOLDER = 10;

    static final int SUBTITLES = 11;

    static int numFiles = 12;

    String aCodec;

    String aFileFormat;

    String vCodec;

    String vFileFormat;

    String avFileFormat;

    String[] muxerOptions;

    String[] aEncOptions;

    String[] vEncOptions;

    String subtitlesFormat = "srt";

    String vcsFileSuffix = "vcs.xml";

    String animFileSuffix = "vcs.svg";

    boolean preExistingAudio = false;

    Subtitles preSubtitles;

    int frameRate;

    int durationMillis;

    /**
	 * Text to be uttered before the main text. Prolog text is not considered
	 * for matching with image annotations. It only matters for reasons of
	 * delaying main text's utterance (and thus the synchronized VC). Example
	 * usage would be having the name of a painting spoken. Setting a prolog
	 * overrides any set or default value for initial delay. Prolog utterance is
	 * currently not correctly handled. Possible future extension: design markup
	 * language for text that allows marking text as "to be matched", thus
	 * integrating prolog functionality as well as other to-be-ignored text.
	 */
    String prolog = null;

    Dimension imageDimension;

    Dimension displayDimension;

    protected Config config;

    protected ImageMetadata metadata;

    protected String text;

    protected CommandLineTool commandLineTool = new CommandLineTool();

    /**
	 * The system's core component
	 */
    protected Control control;

    protected Script script;

    /**
	 * Useful if image dimensions cannot be determined otherwise 
	 */
    protected BufferedImage image = null;

    protected TextDurationEstimator textDurationEstimator = null;

    String getOutFolderWithSeparator() {
        return files[OUTFOLDER].getAbsolutePath() + File.separator;
    }

    /**
	 * deletes a file if it exists
	 * @param f file to be deleted
	 * @return true if file existed and was deleted, false otherwise
	 */
    static boolean deleteIfExists(File f) {
        if (f == null) return false;
        if (f.exists()) {
            return f.delete();
        }
        return false;
    }

    public VCMain(Config config) {
        this.config = config;
    }

    /**
	 * TODO: clarify where this workflow considers sync modes, tolerable missing inputs etc
	 */
    public void run() {
        prepareFormatParams();
        if (!prepareFiles()) {
            return;
        }
        if (!prepareImageMetadata()) {
            return;
        }
        if (!prepareDimensions()) {
            return;
        }
        if (files[TEXT] != null) {
            prepareText();
            if (!prepareTTSTool()) {
                return;
            }
        }
        if (!prepareControl()) {
            return;
        }
        this.script = control.executeControl();
        if (!config.getBooleanProperty(Keys.Clean)) {
            if (!writeScriptFile()) {
                return;
            }
        }
        durationMillis = script.getDurationMillis();
        if (files[SUBTITLES] != null) {
            if (!writeSubtitleFile()) {
                return;
            }
        }
        if (!writeAnimation()) {
            return;
        }
        if (!writeRawVideo()) {
            return;
        }
        if (files[TEXT] != null) {
            if (!writeRawAudio()) {
                return;
            }
        } else {
            files[RAWAUDIO] = null;
            files[ENCODEDAUDIO] = null;
        }
        if (!encodeAndMux()) {
            return;
        }
        clean();
        System.out.println("-- done! --");
    }

    protected boolean encodeAndMux() {
        commandLineTool.setMuxer(config.getProperty(Keys.Muxer));
        String targetDevice = config.getProperty(Keys.TargetDevice);
        if (targetDevice != null) {
            targetDevice = targetDevice.toLowerCase();
            if (targetDevice.startsWith("ipodnew") || targetDevice.startsWith("iphonenew")) {
                System.out.println("-- encoding and muxing for iPhone/iPod... --");
                return commandLineTool.encodeAndMuxForIPod(files[RAWVIDEO], files[RAWAUDIO], files[AVFILE], vCodec, aCodec, avFileFormat, frameRate, muxerOptions);
            }
        }
        if (!writeEncodedVideo()) {
            return false;
        }
        if (files[RAWAUDIO] != null) {
            if (!writeEncodedAudio()) {
                return false;
            }
        }
        if (!writeMuxedAV()) {
            return false;
        }
        return true;
    }

    protected boolean writeAnimation() {
        System.out.println("-- writing SVG... --");
        boolean svgResult = false;
        SimpleSVGWriter svgWriter = new SimpleSVGWriter();
        OutputStream os = null;
        try {
            os = new FileOutputStream(files[ANIM]);
            String hiliteMode = config.getProperty(Keys.HighlightMode);
            if (hiliteMode != null) {
                hiliteMode = hiliteMode.toLowerCase();
                if (hiliteMode.equals("fadingrectangles")) {
                    svgWriter.setHiliteMode(SimpleSVGWriter.FADING_RECT);
                }
            }
            svgWriter.write(script, os);
            os.close();
            svgResult = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return svgResult;
    }

    protected boolean writeRawVideo() {
        System.out.println("-- generating raw video... --");
        commandLineTool.setAnimRenderer(config.getProperty(Keys.Renderer));
        boolean renderResult = commandLineTool.renderAnimToRawAVI(files[ANIM], frameRate, durationMillis, files[RAWVIDEO]);
        if (!renderResult) {
            System.out.println("failed!");
            return false;
        }
        if (files[RAWVIDEO].canRead()) {
            System.out.println("Raw video file written.");
            return true;
        } else {
            System.out.println("failed! [Renderer terminated without producing output]");
            return false;
        }
    }

    /**
	 * Writes the script to the desired file. 
	 * @return true iff successful
	 */
    protected boolean writeScriptFile() {
        System.out.println("-- generating virtual camera script --");
        XMLOutput xmlOutput;
        xmlOutput = new XMLOutput();
        xmlOutput.setCommentLevel(XMLOutput.COMMENT_LEVEL_MAX);
        OutputStream os;
        try {
            os = new FileOutputStream(files[VCS]);
            xmlOutput.write(script, os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!files[VCS].exists()) {
            System.out.println("failed!");
            return false;
        } else {
            System.out.println("VCS XML file written.");
        }
        return true;
    }

    /**
	 * Writes the generated subtitles to the desired subtitles file.
	 * @return true iff successful
	 */
    protected boolean writeSubtitleFile() {
        System.out.println("-- generating subtitles file --");
        if (this.subtitlesFormat.equals("srt")) {
            SRTWriter writer = new SRTWriter();
            boolean result = writer.write(control.finalizeSubtitles(), files[SUBTITLES]);
            result &= files[SUBTITLES].exists();
            if (false == result) {
                System.out.println("Failed!");
            } else {
                System.out.println("Done.");
            }
            return result;
        } else {
            throw new NotImplementedException();
        }
    }

    /**
	 * This is the last preparation method in this class, constructing the
	 * Virtual Camera Control instance from the configuration data that was preprocessed
	 * by other methods.
	 * @return true iff successful
	 */
    protected boolean prepareControl() {
        control = new Control();
        control.setDefaultMotionDuration(config.getIntProperty(Keys.DefaultMotionDuration));
        control.setDefaultStayDuration(config.getIntProperty(Keys.DefaultStayDuration));
        control.setInitialAudioDelay(config.getIntProperty(Keys.InitialAudioDelay));
        control.setMaxDuration(config.getIntProperty(Keys.MaxDuration));
        control.setTextDurationEstimator(textDurationEstimator);
        control.setVisualData(imageDimension, displayDimension, metadata);
        try {
            control.setFillMode(config.getProperty(Keys.FillMode));
            if (!preExistingAudio) {
                control.setSyncMode(config.getProperty(Keys.SyncMode));
            } else {
                control.setSyncMode(Control.RIGID_AUDIO);
            }
            control.useRelevance(config.getBooleanProperty(Keys.Relevance));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        if (!((prolog == null) || (prolog.isEmpty()))) {
            control.setProlog(prolog);
        }
        control.setText(text);
        if (config.getProperty(Keys.SubtitleMode).toLowerCase().equals("simple")) {
            SubtitleSegmenter subtitleSegmenter = new MarkupAwareSubtitleSegmenter();
            control.setSubtitleSegmenter(subtitleSegmenter);
            if (control.getSyncMode() != Control.SUBTITLES) {
                control.setAdditionalSubtitles(Control.SUBTITLES);
            }
        } else if (config.getProperty(Keys.SubtitleMode).toLowerCase().equals("runningText")) {
            if (control.getSyncMode() != Control.RUNNING_TEXT) {
                control.setAdditionalSubtitles(Control.RUNNING_TEXT);
            }
        }
        String orientationString = config.getProperty(Keys.Orientation).toLowerCase();
        if (orientationString != null) {
            int orientationToSet = 0;
            if (orientationString.equals("landscape")) {
                orientationToSet = Control.LANDSCAPE;
            } else if (orientationString.equals("portrait")) {
                orientationToSet = Control.PORTRAIT;
            } else if (orientationString.equals("rotatable")) {
                orientationToSet = Control.BEST_ORIENTATION;
            }
            control.setOrientation(orientationToSet);
        }
        return true;
    }

    /**
	 * Prepares the TTSTool to be used by Virtual Camera Control according to the config.
	 * Note that if TTS is not going to be used, the prepared object will be
	 * of a class implementing TextDurationEstimator but not its subinterface
	 * TTSTool.
	 * 
	 * @return true iff successful
	 */
    protected boolean prepareTTSTool() {
        if (!preExistingAudio) {
            NetworkTTSTool nttsTool;
            String ttsToolClassName;
            Class<? extends NetworkTTSTool> ttsToolClass;
            Properties ttsToolClasses = new Properties();
            String ttsToolsFile = "res/ttstools.xml";
            try {
                ttsToolClasses.loadFromXML(new FileInputStream(ttsToolsFile));
            } catch (InvalidPropertiesFormatException e) {
                System.out.println("Bad properties format in TTSTools directory file " + ttsToolsFile);
                return false;
            } catch (FileNotFoundException e) {
                System.out.println("TTSTools directory file not found at " + ttsToolsFile);
                return false;
            } catch (IOException e) {
                System.out.println("Error reading TTSTools directory file " + ttsToolsFile);
                return false;
            }
            ttsToolClassName = ttsToolClasses.getProperty(config.getProperty(Keys.TTSSystem).toLowerCase());
            try {
                ttsToolClass = (Class<? extends NetworkTTSTool>) Class.forName(ttsToolClassName);
            } catch (ClassNotFoundException e1) {
                System.out.println("Class " + ttsToolClassName + " not found!");
                return false;
            }
            if (NetworkTTSTool.class.isAssignableFrom(ttsToolClass)) {
                try {
                    nttsTool = (NetworkTTSTool) ttsToolClass.newInstance();
                } catch (InstantiationException e) {
                    System.out.println("Error instantiating " + ttsToolClass);
                    return false;
                } catch (IllegalAccessException e) {
                    System.out.println("Error instantiating " + ttsToolClass);
                    return false;
                }
                nttsTool.configure(config.getProperty(Keys.TTSHost), Integer.parseInt(config.getProperty(Keys.TTSPort)), config.getProperty(Keys.Voice));
                textDurationEstimator = nttsTool;
            } else {
                System.out.println("Unknown text-to-speech system: " + config.getProperty(Keys.TTSSystem));
                return false;
            }
        } else {
            SubtitleTextDurationEstimator est = new SubtitleTextDurationEstimator();
            est.setSubtitles(preSubtitles);
            textDurationEstimator = est;
        }
        return textDurationEstimator.isReady();
    }

    /**
	 * Reads the text that will be used for guiding the virtual camera and stores it in this object.
	 * If pre-existing audio is to be used, instead of a plain text file, the file
	 * is attempted to be interpreted as an SRT file such that the subtitle segments'
	 * timing will be used as the source for text timing. The plain text is then 
	 * reconstructed from the subtitles
	 * @return true iff successful
	 */
    protected boolean prepareText() {
        if (files[TEXT] == null) return false;
        if (!preExistingAudio) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(files[TEXT]);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader bReader = new BufferedReader(reader);
            StringBuffer textBuf = new StringBuffer();
            try {
                while (bReader.ready()) {
                    textBuf.append(bReader.readLine());
                    textBuf.append('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(textBuf);
            text = "" + textBuf;
        } else {
            SRTReader reader;
            reader = new SRTReader();
            try {
                this.preSubtitles = new Subtitles(reader.read(files[TEXT]));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            this.text = preSubtitles.getText();
        }
        prolog = config.getProperty(Keys.PrologString);
        return true;
    }

    /**
	 * Extracts and preprocesses the dimensions of the display and the image from the config.
	 * If config does not provide image dimensions, this method opens the image file to get the dimensions.
	 * Note that this will fail if Java's ImageIO.read method cannot deal with the image's format.
	 * directly from there. 
	 * @return true iff successful
	 */
    protected boolean prepareDimensions() {
        String imageDimensionString = null;
        {
            imageDimensionString = config.getProperty(Keys.ImageRes);
            if (imageDimensionString != null) {
                imageDimension = Tools.parseIntDimension(imageDimensionString);
            } else {
                System.out.println("Determining image dimensions from image file.");
                try {
                    image = ImageIO.read(files[IMAGE]);
                    imageDimension = new Dimension(image.getWidth(), image.getHeight());
                    System.out.println("Image dimensions determined as " + imageDimension);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("failed!");
                    return false;
                }
            }
        }
        displayDimension = Tools.parseIntDimension(config.getProperty(Keys.DisplayRes));
        return true;
    }

    /**
	 * prepares objects for all files to be read or written in the process
	 * @return true iff successful
	 */
    protected boolean prepareFiles() {
        outFilePrefix = config.getProperty(Keys.OutFilePrefix);
        files = new File[numFiles];
        if (config.getProperty(Keys.OutputFolder) == null) {
            config.setProperty(Keys.OutputFolder.toString(), "out");
            files[OUTFOLDER] = new File(config.getProperty(Keys.OutputFolder));
            System.out.println("Using default output folder: " + files[OUTFOLDER].getAbsolutePath());
        } else {
            files[OUTFOLDER] = new File(config.getProperty(Keys.OutputFolder));
        }
        if (files[OUTFOLDER].exists()) {
            if (files[OUTFOLDER].isDirectory()) {
            } else {
                System.out.println("There is a non-directory file with the same name as the given output directory!:" + files[OUTFOLDER].getAbsolutePath());
                return false;
            }
        } else {
            if (!files[OUTFOLDER].mkdirs()) {
                System.out.println("Output directory cannot be created: " + files[OUTFOLDER].getAbsolutePath());
                return false;
            }
        }
        files[MINI7] = new File(config.getProperty(Keys.ROIFile));
        String textFileName = config.getProperty(Keys.TextFile);
        if (textFileName.trim().length() == 0) {
            files[TEXT] = null;
            System.out.println("Not using text.");
        } else {
            files[TEXT] = new File(config.getProperty(Keys.TextFile));
            if (!files[TEXT].exists()) {
                System.out.println("Text file not found: " + files[MINI7].getAbsolutePath());
                return false;
            }
            if (config.getProperty(Keys.AudioInputFile).isEmpty()) {
                files[RAWAUDIO] = new File(getOutFolderWithSeparator() + outFilePrefix + "-rawAudio.wav");
                deleteIfExists(files[RAWAUDIO]);
                files[ENCODEDAUDIO] = new File(getOutFolderWithSeparator() + outFilePrefix + "-aEnc." + aFileFormat);
                deleteIfExists(files[ENCODEDAUDIO]);
            } else {
                this.preExistingAudio = true;
                files[RAWAUDIO] = new File(config.getProperty(Keys.AudioInputFile));
                files[ENCODEDAUDIO] = new File(getOutFolderWithSeparator() + outFilePrefix + "-aEnc." + aFileFormat);
            }
            if (config.getProperty(Keys.SubtitleMode).toLowerCase().equals("simple")) {
                files[SUBTITLES] = new File(getOutFolderWithSeparator() + outFilePrefix + "." + subtitlesFormat);
                deleteIfExists(files[SUBTITLES]);
            } else {
                files[SUBTITLES] = null;
            }
        }
        files[VCS] = new File(getOutFolderWithSeparator() + outFilePrefix + "." + vcsFileSuffix);
        files[ANIM] = new File(getOutFolderWithSeparator() + outFilePrefix + "." + animFileSuffix);
        files[RAWVIDEO] = new File(getOutFolderWithSeparator() + outFilePrefix + "-rawVideo.avi");
        files[ENCODEDVIDEO] = new File(getOutFolderWithSeparator() + outFilePrefix + "-vEnc." + vFileFormat);
        files[AVFILE] = new File(getOutFolderWithSeparator() + outFilePrefix + "." + avFileFormat);
        if (!files[MINI7].exists()) {
            System.out.println("Mini7 file for ROI descriptions not found: " + files[MINI7].getAbsolutePath());
            return false;
        }
        deleteIfExists(files[VCS]);
        deleteIfExists(files[ANIM]);
        deleteIfExists(files[RAWVIDEO]);
        deleteIfExists(files[ENCODEDVIDEO]);
        deleteIfExists(files[AVFILE]);
        return true;
    }

    /**
	 * Prepares the image metadata
	 * @return true iff successful
	 */
    protected boolean prepareImageMetadata() {
        Mini7Input m7i;
        m7i = new Mini7Input();
        metadata = m7i.read(files[MINI7].getAbsolutePath());
        if (metadata == null) return false;
        {
            String imageFileName = config.getProperty(Keys.ImageFile);
            if (imageFileName == null) {
                imageFileName = metadata.getImageURL();
            } else {
                metadata.setImageURL(imageFileName);
            }
            files[IMAGE] = new File(imageFileName);
        }
        if (!files[IMAGE].exists()) {
            System.out.println("Image file does not exist: " + files[IMAGE]);
        }
        return true;
    }

    /**
	 * Splits the value of a config property into space-separated parts. This is meant for use
	 * with command-line arguments that are to be passed to external tools, e.g.,
	 * encoder options. 
	 * @param whichOptions identifies the parameter of which the value is to be returned.
	 * @return the parts of the chosen parameter if any were set for the passed key.
	 */
    String[] parseOptions(Keys whichOptions) {
        String options = config.getProperty(whichOptions);
        if (options != null && !options.isEmpty()) {
            return options.split(" ");
        }
        return null;
    }

    /**
	 * This method extracts parameters related to coding formats from the config and
	 * stores them in fields of this object.
	 */
    protected void prepareFormatParams() {
        frameRate = Integer.parseInt(config.getProperty(Keys.FPS));
        aCodec = config.getProperty(Keys.AudioCodec);
        aFileFormat = config.getProperty(Keys.AudioFileFormat);
        vCodec = config.getProperty(Keys.VideoCodec);
        vFileFormat = config.getProperty(Keys.VideoFileFormat);
        avFileFormat = config.getProperty(Keys.FileFormat);
        aEncOptions = parseOptions(Keys.AudioEncoderOptions);
        vEncOptions = parseOptions(Keys.VideoEncoderOptions);
        muxerOptions = parseOptions(Keys.MuxerOptions);
    }

    protected boolean writeEncodedVideo() {
        System.out.println("-- encoding video... --");
        commandLineTool.setVideoEncoder(config.getProperty(Keys.VideoEncoder));
        commandLineTool.setMuxer(config.getProperty(Keys.Muxer));
        boolean encodeVideoResult = commandLineTool.encodeVideo(files[RAWVIDEO], files[ENCODEDVIDEO], vCodec, vFileFormat);
        if (!encodeVideoResult) {
            System.out.println("failed!");
            return false;
        }
        if (files[ENCODEDVIDEO].canRead()) {
            System.out.println("Encoded video file written.");
            return true;
        } else {
            System.out.println("failed! [Encoder terminated without producing output]");
            return false;
        }
    }

    protected boolean writeEncodedAudio() {
        System.out.println("-- encoding audio... --");
        commandLineTool.setAudioEncoder(config.getProperty(Keys.AudioEncoder));
        boolean audEncResult = commandLineTool.encodeAudio(files[RAWAUDIO], files[ENCODEDAUDIO], aCodec, aFileFormat);
        if (!audEncResult) {
            System.out.println("failed!");
            return false;
        }
        if (files[ENCODEDAUDIO].canRead()) {
            System.out.println("Encoded audio file written.");
            return true;
        } else {
            System.out.println("failed! [Encoder terminated without producing output]");
            return false;
        }
    }

    protected boolean writeRawAudio() {
        if (!preExistingAudio) {
            System.out.println("-- generating audio... --");
            try {
                OutputStream os = new FileOutputStream(files[RAWAUDIO]);
                control.generateWAV(os);
                os.close();
            } catch (FileNotFoundException e) {
                System.out.println("Bad TTS tool configuration!");
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (!files[RAWAUDIO].exists()) {
                System.out.println("failed!");
            } else {
                System.out.println("Raw audio file written.");
                return true;
            }
        } else {
            System.out.println("-- checking for pre-existing audio file --");
            System.out.println(config.getProperty(Keys.AudioInputFile));
            if (files[RAWAUDIO].canRead()) {
                System.out.println("Ok.");
                return true;
            } else {
                if (files[RAWAUDIO].exists()) {
                    System.out.println("Unreadable!");
                } else {
                    System.out.println("Does not exist!");
                }
                return false;
            }
        }
        return false;
    }

    protected boolean writeMuxedAV() {
        System.out.println("-- muxing... --");
        commandLineTool.muxAV(files[ENCODEDVIDEO], files[ENCODEDAUDIO], files[SUBTITLES], files[AVFILE], (prolog == null ? control.getInitialAudioDelay() : 0));
        if (!files[AVFILE].exists()) {
            System.out.println("failed!");
            return false;
        } else {
            System.out.println("AV file written: " + files[AVFILE].getAbsolutePath() + " of size " + files[AVFILE].length());
        }
        return true;
    }

    protected boolean prepareForTargetDevice() {
        String targetDevice = config.getProperty(Keys.TargetDevice);
        if (targetDevice != null) {
            targetDevice = targetDevice.toLowerCase();
            if (targetDevice.startsWith("ipod") || targetDevice.startsWith("iphone")) {
                System.out.println("-- preparing for iPhone/iPod... --");
                return commandLineTool.prepareForIPod(files[AVFILE]);
            }
        }
        return true;
    }

    protected void clean() {
        if (preExistingAudio) {
            files[RAWAUDIO] = null;
        }
        if (config.getBooleanProperty(Keys.Clean)) {
            deleteIfExists(files[SUBTITLES]);
            deleteIfExists(files[VCS]);
            deleteIfExists(files[ANIM]);
            deleteIfExists(files[RAWAUDIO]);
            deleteIfExists(files[RAWVIDEO]);
            deleteIfExists(files[ENCODEDAUDIO]);
            deleteIfExists(files[ENCODEDVIDEO]);
        } else if (config.getProperty(Keys.Clean).toLowerCase().equals("showcase")) {
            deleteIfExists(files[RAWAUDIO]);
            deleteIfExists(files[RAWVIDEO]);
            deleteIfExists(files[ENCODEDAUDIO]);
            deleteIfExists(files[ENCODEDVIDEO]);
        } else if (config.getProperty(Keys.Clean).toLowerCase().equals("raw")) {
            deleteIfExists(files[RAWAUDIO]);
            deleteIfExists(files[RAWVIDEO]);
        }
    }

    /**
	 * Starts the application according to the parameters given (and configuration
	 * values if a config file is referenced in the parameters, and using defaults
	 * that are not overridden by parameters or config)
	 * @param args command-line parameters
	 */
    public static void main(String[] args) {
        Config config = new Config();
        if (!config.processArgs(args)) {
            return;
        }
        VCMain vcMain;
        vcMain = new VCMain(config);
        vcMain.run();
    }

    void copyFile(File inputFile, File outputFile) {
        try {
            FileReader in;
            in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
