package vademecum.visualizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.GridUtils;
import vademecum.data.HeightMatrixUtils;
import vademecum.data.IBestMatch;
import vademecum.data.IDataGrid;
import vademecum.data.PMatrix;
import vademecum.data.Retina;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.visualizer.heightMatrix.colors.ColorMapChooser;
import vademecum.visualizer.heightMatrix.contour.CalcContour;
import vademecum.visualizer.heightMatrix.contour.ContourPointListMatrix;
import vademecum.visualizer.heightMatrix.contour.Line3DList;

public class PMatrix2D extends VGraphics implements Runnable {

    private BufferedImage image;

    private Vector<IBestMatch> bmList;

    private PMatrix matrix;

    private boolean drawContour = false;

    private double matrixMax;

    private Vector<Vector<Vector<Double>>> lineVec = new Vector<Vector<Vector<Double>>>();

    private Vector<Point2D> bmPointList = new Vector<Point2D>();

    private ArrayList<Integer> clusterInfo;

    private ArrayList<Color> colors = new ArrayList<Color>();

    private ColorMapChooser colorMapChooser = new ColorMapChooser();

    private int contourSteps = 10;

    private ContourPointListMatrix cplm;

    private Line3DList lines;

    public PMatrix2D() {
        super();
    }

    public PMatrix2D(PMatrix matrix, Retina retina) {
        super();
        init(matrix, retina);
    }

    public PMatrix2D(PMatrix matrix, Retina retina, boolean drawContour, int contourSteps, ArrayList<Integer> clusterInfo, int clusterCol) {
        super();
        init(matrix, bmList, drawContour, contourSteps, clusterInfo);
    }

    private void init(PMatrix matrix, Retina retina) {
        int clusterCol = 0;
        ArrayList<Integer> clusterInfo = GridUtils.getClusterColumn(retina.getWeightVectors(), clusterCol);
        init(matrix, retina.getBMList(), false, 10, clusterInfo);
    }

    private void init(PMatrix matrix, Vector<IBestMatch> bmList, boolean drawContour, int contourSteps, ArrayList<Integer> clusterInfo) {
        this.matrix = matrix;
        colors.add(Color.WHITE);
        colors.add(Color.BLUE);
        colors.add(Color.CYAN);
        colors.add(Color.GREEN);
        colors.add(Color.MAGENTA);
        colors.add(Color.ORANGE);
        colors.add(Color.PINK);
        colors.add(Color.RED);
        colors.add(Color.YELLOW);
        colors.add(Color.GRAY);
        this.bmList = (Vector<IBestMatch>) bmList.clone();
        if (clusterInfo != null) {
            this.clusterInfo = clusterInfo;
            System.out.println("UMatrix2D: Clusterinfo contains " + this.clusterInfo.size() + " Elements.");
        } else {
            this.clusterInfo = new ArrayList<Integer>();
            for (int i = 0; i < bmList.size(); i++) {
                this.clusterInfo.add(0);
            }
        }
        this.matrixMax = this.matrix.getMax();
        this.matrixMax = HeightMatrixUtils.getMax(this.matrix).floatValue();
        buildImage();
    }

    public ArrayList<Point> getBestmatchCoord() {
        ArrayList<Point> bms = new ArrayList<Point>();
        for (int i = 0; i < this.bmList.size(); i++) {
            int offsetX = Math.round(Math.round((double) this.getWidth() / (double) this.matrix.getNumCols() / 2));
            int offsetY = Math.round(Math.round((double) this.getHeight() / (double) this.matrix.getNumRows() / 2));
            int x = offsetX + Math.round(Math.round(((double) this.getWidth() / (double) this.matrix.getNumCols()) * (double) this.bmList.get(i).getColumn()));
            int y = offsetY + Math.round(Math.round(((double) this.getHeight() / (double) this.matrix.getNumRows()) * (double) this.bmList.get(i).getRow()));
            bms.add(new Point(x, y));
        }
        return bms;
    }

    public void setContourSteps(int value) {
        this.contourSteps = value;
        cplm = CalcContour.calcMatrix(HeightMatrixUtils.toDoubleArray(this.matrix), contourSteps);
        lines = CalcContour.matrix2lines(cplm, HeightMatrixUtils.toDoubleArray(this.matrix));
        lineVec.clear();
        for (int i = 0; i < lines.size(); i++) {
            this.lineVec.add(new Vector<Vector<Double>>());
            for (int j = 0; j < lines.getLine(i).getLine().size(); j++) {
                this.lineVec.get(i).add(new Vector<Double>());
                for (int k = 0; k < lines.getLine(i).getLine().get(j).get().length; k++) {
                    this.lineVec.get(i).get(j).add(lines.getLine(i).getLine().get(j).get()[k]);
                }
            }
        }
    }

    public void buildImage() {
        this.colorMapChooser.setColorMap("paretopmxinv");
        image = new BufferedImage(matrix.getNumCols(), matrix.getNumRows(), BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < matrix.getNumRows(); i++) {
            for (int j = 0; j < matrix.getNumCols(); j++) {
                System.out.println("Matrix Height : " + matrix.getHeight(i, j));
                double colorDVal = matrix.getHeight(i, j) / this.matrixMax;
                System.out.println("colorDVal : " + colorDVal + " (max : " + this.matrixMax + ")");
                float colorVal = (float) colorDVal;
                if (colorVal > 1) {
                    colorVal = 1;
                }
                int colIndex = Math.round((float) (this.colorMapChooser.numOfValues() - 1) * colorVal);
                Vector<Float> rgbColor = this.colorMapChooser.getFloatRGBValues(colIndex);
                image.setRGB(j, i, new Color(colorVal, colorVal, colorVal).getRGB());
            }
        }
    }

    public void drawContour(Graphics2D g2) {
        double sizeFacX = (double) this.getWidth() / (double) this.matrix.getNumCols();
        double sizeFacY = (double) this.getHeight() / (double) this.matrix.getNumRows();
        g2.setColor(Color.BLACK);
        for (int i = 0; i < this.lineVec.size(); i++) {
            g2.drawLine(Math.round(Math.round(this.lineVec.get(i).get(0).get(1) * sizeFacX + (sizeFacX / 2))), Math.round(Math.round(this.lineVec.get(i).get(0).get(0) * sizeFacY + (sizeFacY / 2))), Math.round(Math.round(this.lineVec.get(i).get(1).get(1) * sizeFacX + (sizeFacX / 2))), Math.round(Math.round(this.lineVec.get(i).get(1).get(0) * sizeFacY + (sizeFacY / 2))));
        }
    }

    /**
     * Erzeugt ein int-Array mit den RGB-Werten des Bildes.
     */
    private int[] getPixelsFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        PixelGrabber pixelgrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
        try {
            pixelgrabber.grabPixels();
        } catch (InterruptedException interruptedexception) {
            interruptedexception.printStackTrace();
        }
        return pixels;
    }

    /**
     * Erzeugt aus den RGB-Werten eines Arrays ein Bild.
     */
    private BufferedImage getImageFromPixels(int pixels[], int w, int h) {
        MemoryImageSource memoryimagesource = new MemoryImageSource(w, h, ColorModel.getRGBdefault(), pixels, 0, w);
        Image i = Toolkit.getDefaultToolkit().createImage(memoryimagesource);
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = b.createGraphics();
        g2.drawImage(i, 0, 0, null);
        g2.dispose();
        return b;
    }

    /**
    * Die Methode gauss() berechnet eine weichgezeichnete Variante des
    * Originalbildes.
    *
    * @param
    *
    * @return
    */
    private int[] gauss(int[] pixels, int width, int height) {
        int max = width * height;
        int[] pixelArray = new int[width * height];
        int[][] kernel = { { 1, 4, 7, 4, 1 }, { 4, 16, 26, 16, 4 }, { 7, 26, 41, 26, 7 }, { 4, 16, 26, 16, 4 }, { 1, 4, 7, 4, 1 } };
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = y * width + x;
                int[][] conv = new int[5][5];
                for (int j = -2; j <= 2; j++) {
                    if (i + j - 2 * width > 0) conv[j + 2][0] = pixels[i + j - 2 * width] & 0x000000ff;
                    if (i + j - width > 0) conv[j + 2][1] = pixels[i + j - width] & 0x000000ff;
                    if ((i + j > 0) && (i + j < max)) conv[j + 2][2] = pixels[i + j] & 0x000000ff;
                    if (i + j + width < max) conv[j + 2][3] = pixels[i + j + width] & 0x000000ff;
                    if (i + j + 2 * width < max) conv[j + 2][4] = pixels[i + j + 2 * width] & 0x000000ff;
                }
                int gray = 0;
                for (int k = 0; k < 5; k++) {
                    for (int l = 0; l < 5; l++) {
                        gray += (conv[k][l] * kernel[k][l]);
                    }
                }
                gray /= 273;
                if (gray >= 255) pixelArray[i] = 0xffffffff; else pixelArray[i] = 0xff000000 | gray << 16 | gray << 8 | gray;
            }
        }
        return pixelArray;
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    protected void drawCustomPaintings(Graphics2D g2) {
        g2.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
        if (this.drawContour) {
            double sizeFacX = (double) this.getWidth() / (double) this.matrix.getNumCols();
            double sizeFacY = (double) this.getHeight() / (double) this.matrix.getNumRows();
            g2.setColor(Color.BLACK);
            for (int i = 0; i < this.lineVec.size(); i++) {
                g2.drawLine(Math.round(Math.round(this.lineVec.get(i).get(0).get(1) * sizeFacX + (sizeFacX / 2))), Math.round(Math.round(this.lineVec.get(i).get(0).get(0) * sizeFacY + (sizeFacY / 2))), Math.round(Math.round(this.lineVec.get(i).get(1).get(1) * sizeFacX + (sizeFacX / 2))), Math.round(Math.round(this.lineVec.get(i).get(1).get(0) * sizeFacY + (sizeFacY / 2))));
            }
        }
        this.bmPointList.clear();
        if (bmList != null) {
            for (int i = 0; i < this.bmList.size(); i++) {
                int offsetX = Math.round(Math.round((double) this.getWidth() / (double) this.matrix.getNumCols() / 2));
                int offsetY = Math.round(Math.round((double) this.getHeight() / (double) this.matrix.getNumRows() / 2));
                int x = offsetX + Math.round(Math.round(((double) this.getWidth() / (double) this.matrix.getNumCols()) * (double) this.bmList.get(i).getColumn()));
                int y = offsetY + Math.round(Math.round(((double) this.getHeight() / (double) this.matrix.getNumRows()) * (double) this.bmList.get(i).getRow()));
                if (this.clusterInfo.get(i) < colors.size()) {
                    g2.setColor(this.colors.get(this.clusterInfo.get(i)));
                } else {
                    g2.setColor(Color.DARK_GRAY);
                }
                g2.fillOval(x - 2, y - 2, 4, 4);
                g2.setColor(Color.WHITE);
                g2.drawOval(x - 2, y - 2, 4, 4);
                if (this.clusterInfo.get(i) > 0) {
                    g2.drawString(Integer.toString(this.clusterInfo.get(i)), x, y);
                }
                this.bmPointList.add(new Point2D.Double(x, y));
            }
        }
    }

    @Override
    public void initInteractions() {
        super.initInteractions();
    }

    public Vector<IBestMatch> getBMVector() {
        return this.bmList;
    }

    public Vector<Point2D> getBMOnScreenVector() {
        return this.bmPointList;
    }

    public ArrayList<Point2D.Double> getBMOnScreenArray() {
        ArrayList<Point2D.Double> erg = new ArrayList<Point2D.Double>();
        for (int i = 0; i < bmPointList.size(); i++) {
            erg.add(new Point2D.Double(bmPointList.get(i).getX(), bmPointList.get(i).getY()));
        }
        return erg;
    }

    @Override
    public Properties getProperties() {
        Properties sp = super.getProperties();
        Properties p = new Properties();
        String prefix = "VGraphics" + this.getID() + "_";
        Enumeration keyEn = sp.keys();
        while (keyEn.hasMoreElements()) {
            String key = (String) keyEn.nextElement();
            p.setProperty(key, sp.getProperty(key));
        }
        return p;
    }

    @Override
    public void setProperties(Properties p) {
        super.setProperties(p);
        String prefix = "VGraphics" + this.getID() + "_";
        new Thread(this).start();
    }

    public void run() {
        try {
            ExperimentNode eNode = this.getFigurePanel().getSourceNode();
            Retina retina = (Retina) eNode.getMethod().getOutput(Retina.class);
            IDataGrid grid = retina.getWeightVectors();
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).showWaitPanel("Preparing P-Matrix. Please Wait ...");
            vademecum.data.PMatrix matrix = new vademecum.data.PMatrix(retina, grid);
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).textFlushToWaitPanel("Calculating P-Matrix heights ...");
            matrix.calculateHeights();
            init(matrix, retina);
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).hideWaitPanel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
