package game.assets;

import game.engine.GameStatisticsRecorder;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * AssetLoader provides a set of utility methods for loading image and sound assets.
 * <P>
 * This particular class is intended to support the AssetManager by providing a
 * common point of acces to image and sound loading functionality. Any new forms
 * of asset loading should be stored within this class.
 *
 * Note: Whilst the class offers functionality to load sound and midi assets it is
 * currently not the preferred way of loading sound or midi assets. In particular,
 * given the lack of a ready means to clone a Clip object, SoundAsset objects store
 * a URL link to the sound asset which is then reloaded whenever a deep clone is
 * requested (i.e. SoundAsset objects have a builtin capability to load sound clips).
 * However, sound clips loaded by this class can also be used to create a
 * corresponding SoundAsset object, although in doing so, the SoundAsset
 * cannot return a deep clone. This may, or may not, be an issue.
 *
 * The overview presented here also applies to MIDI clips, e.g. if there is a need
 * to perform a deep clone then it will be necessary to create the MIDI asset using
 * a URL specified MIDI.
 *
 * @author <A HREF="mailto:P.Hanna@qub.ac.uk">Philip Hanna</A>
 * @version $Revision: 1.1 $ $Date: 2007/08 $
 *
 * @see AssetManager
 */
public class AssetLoader {

    /**
     * A GraphicsConfiguration instance to faciliate the generation of
     * compatible (i.e. managed) images
     */
    private GraphicsConfiguration graphicsConfiguration;

    /**
     * A GameStatisticsRecorder instance can be associated with this class.
     * If such an object is associated (which it is by default within the game
     * engine) then a record will be kept of the amount of loaded image data.
     */
    private GameStatisticsRecorder gameStatisticsRecorder = null;

    /**
     * This constructor will create a new AssetLoader instance
     */
    public AssetLoader() {
        retrieveGraphicsConfiguration();
    }

    /**
     * Retrieve and store the current graphical configuration for ease of access
     * when creating compatible images
     */
    private final void retrieveGraphicsConfiguration() {
        graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Registers the specified GameStatisticsRecorded with this asset loader.
     * The registered instance will be used to maintain a record of the amount
     * of loaded image data.
     *
     * @param  gameStatisticsRecorder GameStatisticsRecorder instance to be registered
     */
    public final void registerGameStatisticsRecorder(GameStatisticsRecorder gameStatisticsRecorder) {
        this.gameStatisticsRecorder = gameStatisticsRecorder;
    }

    /**
     * Returns an array of image segments extracted from the specified source
     * image using the specified array of Rectangle bounds. One image is returned
     * for each bound.
     *
     * @param  imageName URL specified location of the source image
     * @param  imageSegmentRectangles Rectangle array containing the image segments
     *         extracted and returned
     * @return array of BufferdImage instances containing the extract image segments
     */
    public final BufferedImage[] loadImageSegments(URL imageName, Rectangle[] imageSegmentRectangles) {
        BufferedImage baseImage = loadImage(imageName);
        BufferedImage[] imageSegments = new BufferedImage[imageSegmentRectangles.length];
        for (int iIdx = 0; iIdx < imageSegments.length; iIdx++) imageSegments[iIdx] = extractImageSegment(baseImage, imageSegmentRectangles[iIdx].x, imageSegmentRectangles[iIdx].y, imageSegmentRectangles[iIdx].width, imageSegmentRectangles[iIdx].height);
        return imageSegments;
    }

    /**
     * This method attempts to load and return the specified image. The returned
     * image will be compatible (if possible) with the current graphical device.
     *
     * @param  imageName URL specified location of the source image
     * @return BufferedImage instance containing the loaded image
     *
     * @exception NullPointerException thrown if a null image name is specified
     * @exception IllegalArgumentException thrown if the specified image cannot be loaded
     */
    public final BufferedImage loadImage(URL imageName) {
        if (imageName == null) throw new NullPointerException("AssetLoader.loadImage: NULL parameter supplied.");
        BufferedImage image = null;
        try {
            BufferedImage loadedImage = ImageIO.read(imageName);
            image = graphicsConfiguration.createCompatibleImage(loadedImage.getWidth(), loadedImage.getHeight(), loadedImage.getColorModel().getTransparency());
            Graphics2D g2d = image.createGraphics();
            g2d.drawImage(loadedImage, 0, 0, null);
            g2d.dispose();
        } catch (IOException e) {
            throw new IllegalArgumentException("AssetLoader.loadImage: " + imageName.toString() + " cannot be loaded.");
        }
        if (gameStatisticsRecorder != null && image != null) {
            gameStatisticsRecorder.recordLoadedAssetSize(image.getWidth() * image.getHeight() * 4L);
        }
        return image;
    }

    /**
     * Attempt to extract and return a defined region of the specified source image
     *
     * @param  sourceImage image source from which to extract
     * @param  x extraction offset (relative to the source image)
     * @param  y extraction offset (relative to the source image)
     * @param  width of extracted image segment
     * @param  height of extracted image segment
     *
     * @exception NullPointerException if a null source image is specified
     * @exception IllegalArgumentException if an invalid or out of source image
     *            bounds segment has been specified
     */
    public final BufferedImage extractImageSegment(BufferedImage sourceImage, int x, int y, int width, int height) {
        BufferedImage extractedImage = null;
        if (sourceImage == null) throw new NullPointerException("AssetLoader.extractImageSegement: NULL source image specified.");
        if (x < 0 || y < 0 || width <= 0 || height <= 0) throw new IllegalArgumentException("AssetLoader.extractImageSegment: Invalid image segment " + "x =" + x + " y =" + y + " width = " + width + " height = " + height);
        if (x + width > sourceImage.getWidth() || y + height > sourceImage.getHeight()) throw new IllegalArgumentException("AssetLoader.extractImageSegment: " + "Segment cannot be extracted from source image" + "Segment x =" + x + " y =" + y + " width = " + width + " height = " + height + ": Image width = " + sourceImage.getWidth() + " height = " + sourceImage.getHeight());
        extractedImage = graphicsConfiguration.createCompatibleImage(width, height, sourceImage.getColorModel().getTransparency());
        Graphics2D graphics2D = extractedImage.createGraphics();
        graphics2D.drawImage(sourceImage, 0, 0, width, height, x, y, x + width, y + height, null);
        graphics2D.dispose();
        return extractedImage;
    }

    /**
     * Returns a Clip containing the specified audio file
     *
     * @param  clipName URL specified location of the source clip
     * @return loaded clip
     *
     * @exception IllegalArgumentException if the specified sound clip cannot be loaded
     */
    public final Clip loadAudioClip(URL clipName) {
        Clip loadedClip = null;
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(clipName);
            AudioFormat format = inputStream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                inputStream = AudioSystem.getAudioInputStream(pcmFormat, inputStream);
                format = pcmFormat;
            }
            DataLine.Info clipInfo = new DataLine.Info(Clip.class, format);
            if (AudioSystem.isLineSupported(clipInfo) == false) throw new IllegalArgumentException("AssetLoader.loadAudioClip: Clip format for " + clipName + " is not supported.");
            loadedClip = (Clip) AudioSystem.getLine(clipInfo);
            loadedClip.open(inputStream);
            inputStream.close();
            loadedClip.setFramePosition(0);
        } catch (UnsupportedAudioFileException exception) {
            throw new IllegalArgumentException("AssetLoader.loadAudioClip: " + "Unsupported audio file exception generated for " + clipName);
        } catch (LineUnavailableException exception) {
            throw new IllegalArgumentException("AssetLoader.loadAudioClip: " + "Unsupported audio file exception generated for " + clipName);
        } catch (IOException exception) {
            throw new IllegalArgumentException("AssetLoader.loadAudioClip: " + "Could not read from " + clipName);
        }
        return loadedClip;
    }

    /**
     * Returns a Sequence containing the specified loaded MIDI sequence
     *
     * @param  sequenceName URL specified location of the source MIDI clip
     * @return loaded MIDI sequence
     *
     * @exception InvalidMidiDataException if the specified midi file cannot
     *            be successfully loaded.
     * @exception IOException if the specified URL cannot be opened
     */
    public final Sequence loadMidiSequence(URL sequenceName) {
        Sequence sequence = null;
        try {
            sequence = MidiSystem.getSequence(sequenceName);
        } catch (InvalidMidiDataException exception) {
            throw new IllegalArgumentException("AssetLoader.loadMidiSequence: " + "Unreadable or unsupported Midi file format " + sequenceName);
        } catch (IOException exception) {
            throw new IllegalArgumentException("AssetLoader.loadMidiSequence: " + "IO Exception for " + sequenceName);
        }
        return sequence;
    }
}
