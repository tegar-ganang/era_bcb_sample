package vademecum.visualizer.heightsurface;

import ij.ImagePlus;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageObserver;
import java.awt.image.Kernel;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.IDataGrid;
import vademecum.data.Retina;
import vademecum.extensionPoint.IDataNode;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.vgraphics.IPlotable;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.visualizer.PMatrix2D;
import vademecum.visualizer.heightsurface.dialogs.SurfaceDialog;
import vademecum.visualizer.heightsurface.interactions.Surface3DRotate;
import vademecum.visualizer.heightsurface.jRenderer3D.JRenderer3D;
import vademecum.visualizer.heightsurface.jRenderer3D.Text3D;

/**
 * 
 *	-- Adaption Class -- 
 *   3D Surface Renderer from the open ImageJ Project written by Prof. Barthel
 *	
 */
public class VSurface3D extends VGraphics implements IPlotable {

    private int startWidth = 650;

    private int startHeight = 440;

    public double scaleWindow = 1.;

    private boolean isExamplePlot = false;

    private int imageWidth;

    private int imageHeight;

    public double scaleInit = 1;

    public double zRatioInit = 1;

    private ImagePlus image;

    public JRenderer3D jRenderer3D;

    private Image awtImage;

    @Override
    protected void drawCustomPaintings(Graphics2D g2) {
        awtImage = jRenderer3D.getImage();
        if (awtImage != null) {
            g2.drawImage(awtImage, 0, 0, jRenderer3D.getWidth(), jRenderer3D.getHeight(), (ImageObserver) this);
        }
    }

    public void runMe() {
        generateSampleImage();
        create3DRenderer();
    }

    public void setImage(BufferedImage bufferedImage) {
        imageWidth = bufferedImage.getWidth();
        imageHeight = bufferedImage.getHeight();
        Graphics2D g2D = bufferedImage.createGraphics();
        g2D.drawImage(awtImage, 0, 0, null);
        float ninth = 1.0f / 9.0f;
        float[] blurKernel = { ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth };
        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, blurKernel));
        bufferedImage = op.filter(bufferedImage, null);
        bufferedImage = op.filter(bufferedImage, null);
        g2D = bufferedImage.createGraphics();
        g2D.setColor(new Color(0x3300FF));
        Font font = new Font("Sans", Font.BOLD, 60);
        bufferedImage = op.filter(bufferedImage, null);
        bufferedImage = op.filter(bufferedImage, null);
        g2D.dispose();
        image = new ImagePlus("PM Example Plot", bufferedImage);
        isExamplePlot = false;
        create3DRenderer();
    }

    private void generateSampleImage() {
        imageWidth = 256;
        imageHeight = 256;
        int[] pixels = new int[imageWidth * imageHeight];
        for (int y = 0; y < imageHeight; y++) {
            int dy1 = y - 80;
            int dy2 = y - 60;
            for (int x = 0; x < imageWidth; x++) {
                int dx1 = x - 90;
                int dx2 = x - 180;
                double r1 = Math.sqrt(dx1 * dx1 + dy1 * dy1) / 60;
                double r2 = Math.sqrt(dx2 * dx2 + dy2 * dy2) / 100;
                int v1 = (int) (255 * Math.exp(-r2 * r2));
                int v2 = (int) (255 * Math.exp(-r1 * r1));
                pixels[y * imageWidth + x] = 0xFF000000 | ((int) (v2 + v1) << 16) | (int) ((v2) << 8) | (y / 4);
            }
        }
        MemoryImageSource source = new MemoryImageSource(imageWidth, imageHeight, pixels, 0, imageWidth);
        Image awtImage = Toolkit.getDefaultToolkit().createImage(source);
        BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2D = bufferedImage.createGraphics();
        g2D.drawImage(awtImage, 0, 0, null);
        float ninth = 1.0f / 9.0f;
        float[] blurKernel = { ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth };
        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, blurKernel));
        bufferedImage = op.filter(bufferedImage, null);
        bufferedImage = op.filter(bufferedImage, null);
        g2D = bufferedImage.createGraphics();
        g2D.setColor(Color.white);
        Font font = new Font("Sans", Font.BOLD, 60);
        g2D.setFont(font);
        g2D.drawString("ImageJ", 20, 220);
        bufferedImage = op.filter(bufferedImage, null);
        bufferedImage = op.filter(bufferedImage, null);
        g2D.dispose();
        image = new ImagePlus("Example Plot", bufferedImage);
        isExamplePlot = true;
    }

    public void setBestMatchList(ArrayList<Point> bmlist) {
        for (Point pt : bmlist) {
            jRenderer3D.addPoint3D(pt.x, pt.y, 5, 2, Color.green, 2);
        }
    }

    /**
 * Initializes the JRenderer3D. Set Background, the surface plot, plot mode, lightning mode.
 * Adds a coordinate system. Sets scale. Renders and updates the image.
 */
    private void create3DRenderer() {
        int id = 255;
        double wc = (imageWidth - 1) / 2.;
        double hc = (imageHeight - 1) / 2.;
        double dc = id / 2.;
        jRenderer3D = new JRenderer3D(wc, hc, dc);
        jRenderer3D.setBufferSize(startWidth, startHeight);
        jRenderer3D.setBackgroundColor(Color.white);
        scaleInit = 0.6 * Math.max(startWidth, startHeight) / (double) Math.max(imageHeight, imageWidth);
        zRatioInit = 0.6 * startHeight / (256 * scaleInit);
        jRenderer3D.setTransformZAspectRatio(zRatioInit);
        int grid = 128, gridHeight, gridWidth;
        ;
        if (imageHeight > imageWidth) {
            gridHeight = grid;
            gridWidth = grid * imageWidth / imageHeight;
        } else {
            gridWidth = grid;
            gridHeight = grid * imageHeight / imageWidth;
        }
        jRenderer3D.setSurfacePlotGridSize(gridWidth, gridHeight);
        jRenderer3D.setSurfacePlot(image);
        if (isExamplePlot) {
            jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_GRADIENT);
            jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_FILLED);
            jRenderer3D.setTransformRotationXYZ(60, 0, -20);
            scaleInit *= 0.86;
        } else {
            jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_ORIGINAL);
            jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_FILLED);
            jRenderer3D.setTransformRotationXYZ(60, 0, 34);
        }
        jRenderer3D.setTransformPerspective(0.25);
        double smooth = 5.0;
        jRenderer3D.setSurfaceSmoothingFactor(smooth);
        jRenderer3D.setSurfacePlotLight(0.25);
        int maxDistance = Math.max(imageWidth, Math.max(imageHeight, id));
        jRenderer3D.setTransformMaxDistance(maxDistance);
        jRenderer3D.setTransformScale(scaleInit);
        addCoordinateSystem(imageWidth, imageHeight, id);
        renderAndUpdateDisplay();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        renderAndUpdateDisplay();
    }

    /********************************************************************
 * Draws a simple coordinate system and labels it
 * @param iw
 * @param ih
 * @param id
 ********************************************************************/
    private void addCoordinateSystem(int iw, int ih, int id) {
        jRenderer3D.add3DCube(0, 0, 0, iw - 1, ih - 1, id - 1, Color.gray);
        double d = 12 / scaleInit;
        double fontSize = 14 / scaleInit;
        int dz = (int) (d / zRatioInit);
        jRenderer3D.addText3D(new Text3D("0", -d, -d, -dz, Color.black, fontSize));
        jRenderer3D.addText3D(new Text3D("x", iw + d, -d, -dz, Color.orange, fontSize));
        jRenderer3D.addText3D(new Text3D("y", -d, ih + d, -dz, Color.orange, fontSize));
        jRenderer3D.addText3D(new Text3D("Density", -d, -d, id + dz, Color.orange, fontSize));
    }

    /**
 * Renders and updates the 3D image.
 * Image region is repainted.
 *
 */
    public void renderAndUpdateDisplay() {
        jRenderer3D.doRendering();
        repaint();
    }

    /**
	 * Rezises the buffer size of the image. Renders and updates image.
	 *
	 */
    public void resizeBuffer(int width, int height) {
        if (jRenderer3D != null) {
            scaleWindow = Math.min(width, height) / (double) startHeight;
            jRenderer3D.setBufferSize(width, height);
            renderAndUpdateDisplay();
        }
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r);
        resizeBuffer(r.width, r.height);
    }

    @Override
    public void initInteractions() {
        super.initInteractions();
        Surface3DRotate rotate = new Surface3DRotate();
        rotate.setVGraphics(this);
        rotate.setJRenderer3D(this.jRenderer3D);
        rotate.initPopupMenu();
        this.addMouseListener(rotate);
        this.addMouseMotionListener(rotate);
        addInteraction(rotate);
    }

    public Object getDialog(int i) {
        return new SurfaceDialog(this);
    }

    public String getDialogLabel(int i) {
        return "Settings";
    }

    public int getNumberOfDialogs() {
        return 1;
    }

    public String getPlotMenuLabel() {
        return "Surface";
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
        try {
            ExperimentNode eNode = this.getFigurePanel().getSourceNode();
            IDataNode dn = eNode.getMethod();
            Retina retina = (Retina) dn.getOutput(Retina.class);
            IDataGrid grid = retina.getInputVectors();
            vademecum.data.PMatrix matrix = new vademecum.data.PMatrix(retina, grid);
            matrix.calculateHeights();
            PMatrix2D plot = new PMatrix2D(matrix, retina);
            BufferedImage image = plot.getImage();
            this.setImage(image);
            Surface3DRotate rot3d = (Surface3DRotate) this.interactionList.get(1);
            rot3d.setJRenderer3D(this.jRenderer3D);
        } catch (Exception e) {
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).hideWaitPanel();
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).dispose();
            System.err.println("PMatrix 2D : Something went wrong. Please note the following error messages :");
            System.err.println(e);
            e.printStackTrace();
        }
        this.repaint();
    }

    public Object getHelp() {
        return null;
    }
}
