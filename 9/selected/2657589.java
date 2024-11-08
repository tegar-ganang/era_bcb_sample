package vademecum.visualiser.umatrix;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.GridUtils;
import vademecum.data.HeightMatrixUtils;
import vademecum.data.IBestMatch;
import vademecum.data.IDataGrid;
import vademecum.data.IHeightMatrix;
import vademecum.data.Retina;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.panel.XplorePanel;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.visualiser.umatrix.colors.ColorMapChooser;
import vademecum.visualiser.umatrix.contour.CalcContour;
import vademecum.visualiser.umatrix.contour.ContourPointListMatrix;
import vademecum.visualiser.umatrix.contour.Line3DList;
import vademecum.visualiser.umatrix.features.FeatureUMatrix_DataExtraction;
import vademecum.visualiser.umatrix.features.FeatureUMatrix_VisualisingOptions;

public class UMatrix2D extends VGraphics implements Runnable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 6300872084518765281L;

    private static Log log = LogFactory.getLog(UMatrix2D.class);

    private BufferedImage image;

    private Vector<IBestMatch> bmList;

    private IHeightMatrix matrix;

    private ContourPointListMatrix cplm;

    private Line3DList lines;

    private ColorMapChooser colorMapChooser = new ColorMapChooser();

    private boolean drawContour = true;

    private int contourSteps = 10;

    private boolean showClusterSurface = true;

    private boolean showBestMatches = true;

    private float matrixMax;

    private Vector<Vector<Vector<Double>>> lineVec = new Vector<Vector<Vector<Double>>>();

    private Vector<Point2D> bmPointList = new Vector<Point2D>();

    private ArrayList<Integer> clusterInfo;

    private ArrayList<Color> colors = new ArrayList<Color>();

    Vector<Vector<Integer>> clusterSurface;

    private float transparency = (float) 0.3;

    public String matrixType = "U";

    public UMatrix2D() {
        super();
    }

    public String getMatrixType() {
        return matrixType;
    }

    public void setMatrixType(String mtype) {
        this.matrixType = mtype;
    }

    public void init(IHeightMatrix matrix, Vector<IBestMatch> bmList, ArrayList<Integer> clusterInfo, Vector<Vector<Integer>> clusterSurface) {
        this.clusterSurface = clusterSurface;
        this.colorMapChooser.setColorMap("earthcolor");
        colors.add(Color.BLACK);
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
        this.matrix = matrix;
        this.matrixMax = HeightMatrixUtils.getMax(this.matrix).floatValue();
        this.setContourSteps(this.contourSteps);
        buildImage();
    }

    public void buildImage() {
        image = new BufferedImage(matrix.getNumCols(), matrix.getNumRows(), BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < this.matrix.getNumRows(); i++) {
            for (int j = 0; j < this.matrix.getNumCols(); j++) {
                float colorVal = (float) (this.matrix.getHeight(i, j)) / this.matrixMax;
                if (colorVal > 1) {
                    colorVal = 1;
                }
                int colIndex = Math.round((float) (this.colorMapChooser.numOfValues() - 1) * colorVal);
                Vector<Float> rgbColor = this.colorMapChooser.getFloatRGBValues(colIndex);
                int cluster = clusterSurface.get(i).get(j);
                Color clusterCol = colors.get(cluster % colors.size());
                float[] col = clusterCol.getRGBColorComponents(null);
                if ((cluster != 0) && this.isShowClusterSurface()) {
                    for (int index = 0; index < col.length; index++) {
                        col[index] = rgbColor.get(index) + (col[index] - rgbColor.get(index)) * this.transparency;
                    }
                } else {
                    for (int index = 0; index < col.length; index++) {
                        col[index] = rgbColor.get(index);
                    }
                }
                image.setRGB(j, i, new Color(col[0], col[1], col[2]).getRGB());
            }
        }
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
        for (int i = 0; i < this.bmList.size(); i++) {
            int offsetX = Math.round(Math.round((double) this.getWidth() / (double) this.matrix.getNumCols() / 2));
            int offsetY = Math.round(Math.round((double) this.getHeight() / (double) this.matrix.getNumRows() / 2));
            int x = offsetX + Math.round(Math.round(((double) this.getWidth() / (double) this.matrix.getNumCols()) * (double) this.bmList.get(i).getColumn()));
            int y = offsetY + Math.round(Math.round(((double) this.getHeight() / (double) this.matrix.getNumRows()) * (double) this.bmList.get(i).getRow()));
            if (this.showBestMatches) {
                g2.setColor(this.colors.get(this.clusterInfo.get(i) % colors.size()));
                g2.fillOval(x - 2, y - 2, 4, 4);
                g2.setColor(Color.WHITE);
                g2.drawOval(x - 2, y - 2, 4, 4);
                if (this.clusterInfo.get(i) > 0) {
                    g2.drawString(Integer.toString(this.clusterInfo.get(i)), x + 1, y + 1);
                }
            }
            this.bmPointList.add(new Point2D.Double(x, y));
        }
    }

    @Override
    public void initInteractions() {
        super.initInteractions();
        FeatureUMatrix_DataExtraction feat1 = new FeatureUMatrix_DataExtraction();
        feat1.setPlot(this);
        feat1.setVGraphics(this);
        feat1.initPopupMenu();
        this.addMouseListener(feat1);
        this.addMouseMotionListener(feat1);
        interactionList.add(feat1);
        FeatureUMatrix_VisualisingOptions feat2 = new FeatureUMatrix_VisualisingOptions();
        feat2.setPlot(this);
        feat2.setVGraphics(this);
        feat2.initPopupMenu();
        this.addMouseListener(feat2);
        this.addMouseMotionListener(feat2);
        interactionList.add(feat2);
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

    public boolean isDrawContour() {
        return drawContour;
    }

    public void setDrawContour(boolean drawContour) {
        this.drawContour = drawContour;
    }

    public boolean isShowClusterSurface() {
        return showClusterSurface;
    }

    public void setShowClusterSurface(boolean showClusterSurface) {
        this.showClusterSurface = showClusterSurface;
    }

    public void setContourSteps(int value) {
        this.contourSteps = value;
        log.debug("ContourSteps: " + this.contourSteps);
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

    public void setTransparency(float f) {
        this.transparency = f;
    }

    public float getTransparency() {
        return this.transparency;
    }

    public boolean isShowBestMatches() {
        return showBestMatches;
    }

    public void setShowBestMatches(boolean showBestMatches) {
        this.showBestMatches = showBestMatches;
    }

    public Vector<String> getColorMaps() {
        return this.colorMapChooser.getColorMapNames();
    }

    public void changeColorMap(String mapName) {
        this.colorMapChooser.setColorMap(mapName);
    }

    @Override
    public Properties getProperties() {
        Properties sp = super.getProperties();
        Properties p = new Properties();
        String prefix = "VGraphics" + this.getID() + "->";
        p.setProperty(prefix + "MatrixType", this.getMatrixType());
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
        String prefix = "VGraphics" + this.getID() + "->";
        String mType = p.getProperty(prefix + "MatrixType");
        this.setMatrixType(mType);
        new Thread(this).start();
    }

    public void run() {
        try {
            ExperimentNode eNode = this.getXplorePanel().getSourceNode();
            Retina retina = (Retina) eNode.getMethod().getOutput(Retina.class);
            Retina grid = retina;
            IHeightMatrix heightMatrix = null;
            ((VisualizerFrame) this.getXplorePanel().getGraphicalViewer()).showWaitPanel("Reconstructing. Please Wait ...");
            if (getMatrixType().equals("U")) {
                heightMatrix = new vademecum.data.UMatrix(grid);
            } else {
                heightMatrix = new vademecum.data.PMatrix(grid, grid.getInputVectors());
                heightMatrix.calculateHeights();
            }
            log.debug("NumOfClusterCols: " + GridUtils.getClusterColumnPos(grid.getWeightVectors()));
            int clusterCol;
            if (GridUtils.getClusterColumnPos(grid.getWeightVectors()) != null) {
                clusterCol = GridUtils.getClusterColumnPos(grid.getWeightVectors()).size() - 1;
            } else {
                clusterCol = 0;
            }
            init(heightMatrix, grid.getBMList(), GridUtils.getClusterColumn(grid.getWeightVectors(), clusterCol), grid.getClusterSurface());
            ((VisualizerFrame) this.getXplorePanel().getGraphicalViewer()).hideWaitPanel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
