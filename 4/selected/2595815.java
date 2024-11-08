package co.edu.unal.ungrid.image.dicom.display.wave;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javax.swing.JComponent;
import co.edu.unal.ungrid.image.dicom.core.BinaryInputStream;
import co.edu.unal.ungrid.image.dicom.display.ApplicationFrame;

/**
 * <p>
 * Implements a component that can display an array of tiles, each of which is a
 * 2D graph of ECG values.
 * </p>
 * 
 * 
 */
public class ECGPanel extends JComponent {

    public static final long serialVersionUID = 200609220000001L;

    /**
	 * @uml.property name="samples" multiplicity="(0 -1)" dimension="2"
	 */
    private short[][] samples;

    /**
	 * @uml.property name="numberOfChannels"
	 */
    private int numberOfChannels;

    /**
	 * @uml.property name="nSamplesPerChannel"
	 */
    private int nSamplesPerChannel;

    /**
	 * @uml.property name="nTilesPerColumn"
	 */
    private int nTilesPerColumn;

    /**
	 * @uml.property name="nTilesPerRow"
	 */
    private int nTilesPerRow;

    /**
	 * @uml.property name="samplingIntervalInMilliSeconds"
	 */
    private float samplingIntervalInMilliSeconds;

    /**
	 * @uml.property name="amplitudeScalingFactorInMilliVolts" multiplicity="(0
	 *               -1)" dimension="1"
	 */
    private float[] amplitudeScalingFactorInMilliVolts;

    /**
	 * @uml.property name="channelNames" multiplicity="(0 -1)" dimension="1"
	 */
    private String[] channelNames;

    /**
	 * @uml.property name="widthOfPixelInMilliSeconds"
	 */
    private float widthOfPixelInMilliSeconds;

    /**
	 * @uml.property name="heightOfPixelInMilliVolts"
	 */
    private float heightOfPixelInMilliVolts;

    /**
	 * @uml.property name="timeOffsetInMilliSeconds"
	 */
    private float timeOffsetInMilliSeconds;

    /**
	 * @uml.property name="displaySequence" multiplicity="(0 -1)" dimension="1"
	 */
    private int displaySequence[];

    /**
	 * @uml.property name="width"
	 */
    private int width;

    /**
	 * @uml.property name="height"
	 */
    private int height;

    /**
	 * <p>
	 * Construct a component containing an array of tiles of ECG waveforms.
	 * </p>
	 * 
	 * @param samples
	 *            the ECG data as separate channels
	 * @param numberOfChannels
	 *            the number of channels (leads)
	 * @param nSamplesPerChannel
	 *            the number of samples per channel (same for all channels)
	 * @param channelNames
	 *            the names of each channel with which to annotate them
	 * @param nTilesPerColumn
	 *            the number of tiles to display per column
	 * @param nTilesPerRow
	 *            the number of tiles to display per row (if 1, then
	 *            nTilesPerColumn should == numberOfChannels)
	 * @param samplingIntervalInMilliSeconds
	 *            the sampling interval (duration of each sample) in
	 *            milliseconds
	 * @param amplitudeScalingFactorInMilliVolts
	 *            how many millivolts per unit of sample data (may be different
	 *            for each channel)
	 * @param horizontalPixelsPerMilliSecond
	 *            how may pixels to use to represent one millisecond
	 * @param verticalPixelsPerMilliVolt
	 *            how may pixels to use to represent one millivolt
	 * @param timeOffsetInMilliSeconds
	 *            how much of the sample data to skip, specified in milliseconds
	 *            from the start of the samples
	 * @param displaySequence
	 *            an array of indexes into samples (etc.) sorted into desired
	 *            sequential display order
	 * @param width
	 *            the width of the resulting component (sample data is truncated
	 *            to fit if necessary)
	 * @param height
	 *            the height of the resulting component (sample data is
	 *            truncated to fit if necessary)
	 */
    public ECGPanel(short[][] samples, int numberOfChannels, int nSamplesPerChannel, String[] channelNames, int nTilesPerColumn, int nTilesPerRow, float samplingIntervalInMilliSeconds, float[] amplitudeScalingFactorInMilliVolts, float horizontalPixelsPerMilliSecond, float verticalPixelsPerMilliVolt, float timeOffsetInMilliSeconds, int[] displaySequence, int width, int height) {
        this.samples = samples;
        this.numberOfChannels = numberOfChannels;
        this.nSamplesPerChannel = nSamplesPerChannel;
        this.channelNames = channelNames;
        this.nTilesPerColumn = nTilesPerColumn;
        this.nTilesPerRow = nTilesPerRow;
        this.samplingIntervalInMilliSeconds = samplingIntervalInMilliSeconds;
        this.amplitudeScalingFactorInMilliVolts = amplitudeScalingFactorInMilliVolts;
        this.widthOfPixelInMilliSeconds = 1 / horizontalPixelsPerMilliSecond;
        this.heightOfPixelInMilliVolts = 1 / verticalPixelsPerMilliVolt;
        this.timeOffsetInMilliSeconds = timeOffsetInMilliSeconds;
        this.displaySequence = displaySequence;
        this.width = width;
        this.height = height;
    }

    /**
	 * @param g2d
	 * @param r
	 * @param fillBackgroundFirst
	 */
    private void renderPlotToGraphics2D(Graphics2D g2d, Rectangle r, boolean fillBackgroundFirst) {
        Color backgroundColor = Color.white;
        Color curveColor = Color.blue;
        Color boxColor = Color.black;
        Color gridColor = Color.red;
        Color channelNameColor = Color.black;
        float curveWidth = 1.5f;
        float boxWidth = 2;
        float gridWidth = 1;
        Font channelNameFont = new Font("SansSerif", Font.BOLD, 14);
        int channelNameXOffset = 10;
        int channelNameYOffset = 20;
        g2d.setBackground(backgroundColor);
        g2d.setColor(backgroundColor);
        if (fillBackgroundFirst) {
            g2d.fill(new Rectangle2D.Float(0, 0, r.width, r.height));
        }
        float widthOfTileInPixels = (float) width / nTilesPerRow;
        float heightOfTileInPixels = (float) height / nTilesPerColumn;
        float widthOfTileInMilliSeconds = widthOfPixelInMilliSeconds * widthOfTileInPixels;
        float heightOfTileInMilliVolts = heightOfPixelInMilliVolts * heightOfTileInPixels;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(gridColor);
        float drawingOffsetY = 0;
        for (int row = 0; row < nTilesPerColumn; ++row) {
            float drawingOffsetX = 0;
            for (int col = 0; col < nTilesPerRow; ++col) {
                g2d.setStroke(new BasicStroke(gridWidth));
                for (float time = 0; time < widthOfTileInMilliSeconds; time += 200) {
                    float x = drawingOffsetX + time / widthOfPixelInMilliSeconds;
                    g2d.draw(new Line2D.Float(x, drawingOffsetY, x, drawingOffsetY + heightOfTileInPixels));
                }
                g2d.setStroke(new BasicStroke(gridWidth));
                for (float milliVolts = -heightOfTileInMilliVolts / 2; milliVolts <= heightOfTileInMilliVolts / 2; milliVolts += 0.5) {
                    float y = drawingOffsetY + heightOfTileInPixels / 2 + milliVolts / heightOfTileInMilliVolts * heightOfTileInPixels;
                    g2d.draw(new Line2D.Float(drawingOffsetX, y, drawingOffsetX + widthOfTileInPixels, y));
                }
                drawingOffsetX += widthOfTileInPixels;
            }
            drawingOffsetY += heightOfTileInPixels;
        }
        g2d.setColor(boxColor);
        g2d.setStroke(new BasicStroke(boxWidth));
        drawingOffsetY = 0;
        int channel = 0;
        for (int row = 0; row < nTilesPerColumn; ++row) {
            float drawingOffsetX = 0;
            for (int col = 0; col < nTilesPerRow; ++col) {
                if (row == 0) g2d.draw(new Line2D.Float(drawingOffsetX, drawingOffsetY, drawingOffsetX + widthOfTileInPixels, drawingOffsetY));
                if (col == 0) g2d.draw(new Line2D.Float(drawingOffsetX, drawingOffsetY, drawingOffsetX, drawingOffsetY + heightOfTileInPixels));
                g2d.draw(new Line2D.Float(drawingOffsetX, drawingOffsetY + heightOfTileInPixels, drawingOffsetX + widthOfTileInPixels, drawingOffsetY + heightOfTileInPixels));
                g2d.draw(new Line2D.Float(drawingOffsetX + widthOfTileInPixels, drawingOffsetY, drawingOffsetX + widthOfTileInPixels, drawingOffsetY + heightOfTileInPixels));
                if (channelNames != null && channel < displaySequence.length && displaySequence[channel] < channelNames.length) {
                    String channelName = channelNames[displaySequence[channel]];
                    if (channelName != null) {
                        g2d.setColor(channelNameColor);
                        g2d.setFont(channelNameFont);
                        g2d.drawString(channelName, drawingOffsetX + channelNameXOffset, drawingOffsetY + channelNameYOffset);
                    }
                }
                drawingOffsetX += widthOfTileInPixels;
                ++channel;
            }
            drawingOffsetY += heightOfTileInPixels;
        }
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(curveColor);
        g2d.setStroke(new BasicStroke(curveWidth));
        float interceptY = heightOfTileInPixels / 2;
        float widthOfSampleInPixels = samplingIntervalInMilliSeconds / widthOfPixelInMilliSeconds;
        int timeOffsetInSamples = (int) (timeOffsetInMilliSeconds / samplingIntervalInMilliSeconds);
        int widthOfTileInSamples = (int) (widthOfTileInMilliSeconds / samplingIntervalInMilliSeconds);
        int usableSamples = nSamplesPerChannel - timeOffsetInSamples;
        if (usableSamples <= 0) {
            return;
        } else if (usableSamples > widthOfTileInSamples) {
            usableSamples = widthOfTileInSamples - 1;
        }
        drawingOffsetY = 0;
        channel = 0;
        GeneralPath thePath = new GeneralPath();
        for (int row = 0; row < nTilesPerColumn && channel < numberOfChannels; ++row) {
            float drawingOffsetX = 0;
            for (int col = 0; col < nTilesPerRow && channel < numberOfChannels; ++col) {
                float yOffset = drawingOffsetY + interceptY;
                short[] samplesForThisChannel = samples[displaySequence[channel]];
                int i = timeOffsetInSamples;
                float rescaleY = amplitudeScalingFactorInMilliVolts[displaySequence[channel]] / heightOfPixelInMilliVolts;
                float fromXValue = drawingOffsetX;
                float fromYValue = yOffset - samplesForThisChannel[i] * rescaleY;
                thePath.reset();
                thePath.moveTo(fromXValue, fromYValue);
                ++i;
                for (int j = 1; j < usableSamples; ++j) {
                    float toXValue = fromXValue + widthOfSampleInPixels;
                    float toYValue = yOffset - samplesForThisChannel[i] * rescaleY;
                    i++;
                    if ((int) fromXValue != (int) toXValue || (int) fromYValue != (int) toYValue) {
                        thePath.lineTo(toXValue, toYValue);
                    }
                    fromXValue = toXValue;
                    fromYValue = toYValue;
                }
                g2d.draw(thePath);
                drawingOffsetX += widthOfTileInPixels;
                ++channel;
            }
            drawingOffsetY += heightOfTileInPixels;
        }
    }

    /**
	 * @param r
	 */
    private BufferedImage createAppropriateBufferedImageToDrawInto(Rectangle r) {
        ColorModel colorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
        return new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(r.width, r.height), colorModel.isAlphaPremultiplied(), null);
    }

    /**
	 * @uml.property name="imageOfRenderedPlot"
	 */
    private BufferedImage imageOfRenderedPlot;

    /**
	 * <p>
	 * Draw the data onto the supplied graphic with the specified background.
	 * </p>
	 * 
	 * @param g
	 *            the graphic to draw into
	 */
    public void paintComponent(Graphics g) {
        Rectangle r = new Rectangle(width, height);
        if (imageOfRenderedPlot == null) {
            imageOfRenderedPlot = createAppropriateBufferedImageToDrawInto(r);
            renderPlotToGraphics2D((Graphics2D) (imageOfRenderedPlot.getGraphics()), r, true);
        }
        g.drawImage(imageOfRenderedPlot, 0, 0, this);
    }

    /**
	 * <p>
	 * For testing.
	 * </p>
	 * 
	 * <p>
	 * Display the specified sample values as an array of tiles in a window, and
	 * take a snapshot of it as a JPEG file.
	 * </p>
	 * 
	 * @param arg
	 *            an argument selecting the input type ("RAW", "DICOM" or
	 *            "SCPECG"), followed by either 8 more arguments, the raw data
	 *            filename (2 bytes per signed 16 bit sample interleaved), the
	 *            number of channels, the number of samples per channel, the
	 *            number of tiles per column, the number of tiles per row, the
	 *            sampling interval in milliseconds, the amplitude scaling
	 *            factor in millivolts, and the time offset in milliseconds for
	 *            the left edge of the display or 4 more arguments, the SCPECG
	 *            or DICOM data filename, the number of tiles per column, the
	 *            number of tiles per row, and the time offset in milliseconds
	 *            for the left edge of the display
	 */
    public static void main(String arg[]) {
        try {
            SourceECG sourceECG = null;
            BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(arg[1])), false);
            int nTilesPerColumn = 0;
            int nTilesPerRow = 0;
            float timeOffsetInMilliSeconds = 0;
            if (arg.length == 9 && arg[0].toUpperCase().equals("RAW")) {
                int numberOfChannels = Integer.parseInt(arg[2]);
                int nSamplesPerChannel = Integer.parseInt(arg[3]);
                nTilesPerColumn = Integer.parseInt(arg[4]);
                nTilesPerRow = Integer.parseInt(arg[5]);
                float samplingIntervalInMilliSeconds = Float.parseFloat(arg[6]);
                float amplitudeScalingFactorInMilliVolts = Float.parseFloat(arg[7]);
                timeOffsetInMilliSeconds = Float.parseFloat(arg[8]);
                sourceECG = new RawSourceECG(i, numberOfChannels, nSamplesPerChannel, samplingIntervalInMilliSeconds, amplitudeScalingFactorInMilliVolts, true);
            } else if (arg.length == 5) {
                nTilesPerColumn = Integer.parseInt(arg[2]);
                nTilesPerRow = Integer.parseInt(arg[3]);
                timeOffsetInMilliSeconds = Float.parseFloat(arg[4]);
                if (arg[0].toUpperCase().equals("SCPECG")) {
                    sourceECG = new SCPSourceECG(i, true);
                } else if (arg[0].toUpperCase().equals("DICOM")) {
                    sourceECG = new DicomSourceECG(i);
                }
            }
            float milliMetresPerPixel = (float) (25.4 / 72);
            float horizontalPixelsPerMilliSecond = 25 / (1000 * milliMetresPerPixel);
            float verticalPixelsPerMilliVolt = 10 / (milliMetresPerPixel);
            ECGPanel pg = new ECGPanel(sourceECG.getSamples(), sourceECG.getNumberOfChannels(), sourceECG.getNumberOfSamplesPerChannel(), sourceECG.getChannelNames(), nTilesPerColumn, nTilesPerRow, sourceECG.getSamplingIntervalInMilliSeconds(), sourceECG.getAmplitudeScalingFactorInMilliVolts(), horizontalPixelsPerMilliSecond, verticalPixelsPerMilliVolt, timeOffsetInMilliSeconds, sourceECG.getDisplaySequence(), 800, 400);
            pg.setPreferredSize(new Dimension(800, 400));
            String title = sourceECG.getTitle();
            ApplicationFrame app = new ApplicationFrame(title == null ? "ECG Panel" : title);
            app.getContentPane().add(pg);
            app.pack();
            app.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
