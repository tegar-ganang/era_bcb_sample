package UserInterface;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;
import Control.*;

/**
 * This class manage the whole char area, it draws new lines, shows the
 * background and reset all lines
 * 
 * @author kurmt1@bfh.ch, woodr1@bfh.ch, eichs2@bfh.ch
 * @version 1.0
 */
class LCPDChart extends JPanel implements ComponentListener {

    /**
	 * 
	 */
    private final int CHANNEL_NUMBER = 2;

    private static final long serialVersionUID = 1L;

    private BufferedImage BackgroundImage;

    private Graphics2D Background;

    private BufferedImage[] Chart;

    private Graphics2D[] ChartLine;

    private Color LineBackgroundColor;

    private Color LineColor[];

    private Color GridColor;

    private GraphicsConfiguration gfxConf;

    private LCPDData Data;

    private LCPDConverterSettings ConverterSettings;

    private boolean ComponentsInitialized = false;

    private LCPDGraphicSettings GraphicSettings;

    private ArrayList<Float>[] locData;

    private boolean Resized = false;

    private int currentX = 0;

    private long currentY = 0, lastY = 0;

    private int[] PixelCounter;

    private ArrayList<Float> tempSampleBuffer;

    private int[] currentP;

    /**
	 * paint the component, this means if every thing is initialized, also
	 * drawing the lines and everything else.
	 * 
	 * @param Graphics g
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
    public void paintComponent(Graphics g) {
        long Time;
        super.paintComponent(g);
        if (ComponentsInitialized) {
            Graphics2D g2 = (Graphics2D) g;
            setGridColor(GridColor);
            DrawLines();
            g2.drawImage(BackgroundImage, null, 0, 0);
            g2.drawImage(Chart[0], null, 0, 0);
            g2.drawImage(Chart[1], null, 0, 0);
        }
    }

    /**
	 * Draw all channel lines
	 */
    private void DrawLines() {
        int ResX = Chart[0].getWidth();
        int ResY = Chart[0].getHeight();
        int MiddleY = ResY / 2;
        double dx, dy;
        int MaxSize;
        int MaximumIncrement;
        if (ConverterSettings.isRun()) {
            MaximumIncrement = (int) (ConverterSettings.getSampleRate() * ConverterSettings.getDisplayTime());
            for (int i = 0; i < CHANNEL_NUMBER; i++) {
                if (Data.getNewData(i) || Resized) {
                    Resized = false;
                    if (ConverterSettings.getTriggerMode() == LCPDConverterSettings.LCPDTriggerMode.Normal || ConverterSettings.getTriggerMode() == LCPDConverterSettings.LCPDTriggerMode.SingleSlope) {
                        if (Data.getNewData(i)) {
                            locData[i].clear();
                            locData[i].addAll(Data.getSamples(i));
                        }
                        ChartLine[i].setBackground(LineBackgroundColor);
                        ChartLine[i].clearRect(0, 0, ResX, ResY);
                        PixelCounter[i] = 0;
                        currentP[i] = 0;
                    } else {
                        if (Data.getNewData(i)) {
                            MaxSize = (int) (ConverterSettings.getDisplayTime() * ConverterSettings.getSampleRate());
                            tempSampleBuffer.clear();
                            tempSampleBuffer.addAll(Data.getSamples(i));
                            for (int t = 0; t < tempSampleBuffer.size(); t++, currentP[i]++) {
                                if (currentP[i] >= MaxSize) {
                                    currentP[i] = 0;
                                }
                                if (currentP[i] < locData[i].size()) locData[i].set(currentP[i], (float) tempSampleBuffer.get(t)); else locData[i].add((float) tempSampleBuffer.get(t));
                            }
                        }
                        ChartLine[i].setBackground(LineBackgroundColor);
                        ChartLine[i].clearRect(0, 0, ResX, ResY);
                        PixelCounter[i] = 0;
                    }
                    if ((ConverterSettings.getSampleRate() != 0) && (ConverterSettings.getMaxVoltage(i) != 0) && GraphicSettings.getDisplayChannel(i) && locData[i].size() > 0) {
                        dx = (double) ResX / (MaximumIncrement - 2);
                        dy = (double) ResY / (2 * ConverterSettings.getMaxVoltage(i));
                        lastY = Math.round((MiddleY - (GraphicSettings.getOffsetVoltage(i) + locData[i].get(PixelCounter[i])) * dy));
                        currentX = (int) (PixelCounter[i] * dx);
                        System.out.println("locData[" + i + "]:" + locData[i].get(-i + 2));
                        PixelCounter[i]++;
                        ChartLine[i].setColor(GraphicSettings.getChannelColor(i));
                        for (; PixelCounter[i] <= MaximumIncrement && PixelCounter[i] < locData[i].size(); PixelCounter[i]++) {
                            currentY = Math.round(MiddleY - (GraphicSettings.getOffsetVoltage(i) + locData[i].get(PixelCounter[i])) * dy);
                            ChartLine[i].drawLine(currentX, (int) lastY, (int) (currentX + dx), (int) (currentY));
                            lastY = currentY;
                            currentX = (int) (PixelCounter[i] * dx);
                        }
                    }
                }
            }
        }
    }

    /**
	 * Constructor for the lcpd Chart
	 * 
	 * @param LCPDData Reference to Data
	 * 
	 * @param LCPDConverterSetting Reference to the Converter settings
	 * 
	 * @param LCPDGraphicSettings Reference to the Display settings
	 */
    public LCPDChart(LCPDData pData, LCPDConverterSettings pConverterSettings, LCPDGraphicSettings pGraphicSettings) {
        Data = pData;
        ConverterSettings = pConverterSettings;
        GraphicSettings = pGraphicSettings;
        locData = new ArrayList[2];
        tempSampleBuffer = new ArrayList<Float>();
        currentP = new int[2];
        PixelCounter = new int[2];
        for (int i = 0; i < CHANNEL_NUMBER; i++) {
            locData[i] = new ArrayList<Float>();
            currentP[i] = 0;
            PixelCounter[i] = 0;
        }
        LineColor = new Color[CHANNEL_NUMBER];
        Chart = new BufferedImage[CHANNEL_NUMBER];
        ChartLine = new Graphics2D[CHANNEL_NUMBER];
        GridColor = null;
        LineBackgroundColor = new Color((float) 1.0, (float) 1.0, (float) 1.0, (float) 0.0);
        gfxConf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        int width = getSize().width, height = getSize().height;
        if (width < 10) width = 100;
        if (height < 10) height = 100;
        setBackground(new Color(0, 0, 0));
        addComponentListener(this);
    }

    /**
	 * Create the Chart Layers
	 */
    private void createCharts() {
        for (int i = 0; i < CHANNEL_NUMBER; i++) {
            Chart[i] = gfxConf.createCompatibleImage(getSize().width, getSize().height, Transparency.BITMASK);
            ChartLine[i] = Chart[i].createGraphics();
        }
    }

    /**
	 * sets the Background Color new
	 * 
	 * @param Color the background color
	 * 
	 * @see javax.swing.JComponent#setBackground(java.awt.Color)
	 */
    public void setBackground(Color value) {
        super.setBackground(value);
        if (Background != null) {
            Background.setBackground(value);
            Background.clearRect(0, 0, getSize().width, getSize().height);
            setGridColor(GridColor);
        }
    }

    /**
	 * sets the Grid Color
	 * 
	 * @param Color grid color
	 */
    public void setGridColor(Color value) {
        GridColor = value;
        int width = getSize().width, height = getSize().height;
        float dx = width / 10, dy = height / 10;
        if (Background != null) {
            Background.setColor(GridColor);
            for (int i = 1; i < 10; i++) {
                if (i == 5) {
                    Background.drawLine((int) dx * i, 0, (int) dx * i, height);
                    Background.drawLine((int) dx * i + 1, 0, (int) dx * i + 1, height);
                    Background.drawLine((int) dx * i - 1, 0, (int) dx * i - 1, height);
                    Background.drawLine(0, (int) dy * i, width, (int) dy * i);
                    Background.drawLine(0, (int) dy * i + 1, width, (int) dy * i + 1);
                    Background.drawLine(0, (int) dy * i - 1, width, (int) dy * i - 1);
                } else {
                    Background.drawLine((int) dx * i, 0, (int) dx * i, height);
                    Background.drawLine(0, (int) dy * i, width, (int) dy * i);
                }
            }
        }
    }

    /**
	 * sets the color of the different channels
	 * 
	 * @param int Channel number (0=A, 1=B)
	 * 
	 * @param Color Color of the line
	 */
    public void setLineColor(int Channel, Color value) {
        if (LineColor.length > Channel) {
            LineColor[Channel] = value;
        }
    }

    /**
	 * sets an new reference to the display data
	 */
    public void setDisplayData(LCPDData pData) {
        Data = pData;
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    /**
	 * Handles the component resizing.
	 * 
	 * @param ComponentEvent e
	 * 
	 * @seejava.awt.event.ComponentListener#componentResized(java.awt.event.
	 * ComponentEvent)
	 */
    public void componentResized(ComponentEvent e) {
        ComponentsInitialized = true;
        BackgroundImage = gfxConf.createCompatibleImage(getSize().width, getSize().height);
        Background = BackgroundImage.createGraphics();
        Chart[0] = gfxConf.createCompatibleImage(getSize().width, getSize().height, Transparency.BITMASK);
        ChartLine[0] = Chart[0].createGraphics();
        Chart[1] = gfxConf.createCompatibleImage(getSize().width, getSize().height, Transparency.BITMASK);
        ChartLine[1] = Chart[1].createGraphics();
        ChartLine[0].setBackground(LineBackgroundColor);
        ChartLine[1].setBackground(LineBackgroundColor);
        setLineColor(0, new Color(0xFF, 0, 0));
        setLineColor(1, new Color(0, 0xFF, 0));
        setGridColor(GridColor);
        Resized = true;
        createCharts();
        repaint();
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }
}
