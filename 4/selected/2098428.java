package org.trebor.freesand;

import static java.awt.Color.DARK_GRAY;
import static java.awt.event.InputEvent.CTRL_MASK;
import static java.awt.event.InputEvent.META_MASK;
import static java.awt.event.InputEvent.SHIFT_MASK;
import static java.awt.event.KeyEvent.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.trebor.freesand.World.Element.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageProducer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import org.trebor.freesand.World.Element;

/**
 * Game provides the freesand graphical user interface functionality.
 * World simulation occurs in {@link World}, where element behavior is
 * handled.
 */
@SuppressWarnings("serial")
public class Game extends JFrame {

    /** default world width */
    public static final int WORLD_WIDTH = 300;

    /** default world height */
    public static final int WORLD_HEIGHT = 300;

    /** frequency to paint graphics */
    public static final long WORLD_PAINT_MS = 50;

    /** period of time to test each element */
    public static final long TEST_PERIOD = 1000;

    /** delay between when user stops resizing screen and when
     * resize computations occur */
    public static final int RESIZE_DELAY = 300;

    /** minimum brush size */
    public static final double MIN_BRUSH_SIZE = 0.01;

    /** maximum brush size */
    public static final double MAX_BRUSH_SIZE = 3 + MIN_BRUSH_SIZE;

    /** brush size change step */
    public static final double BRUSH_STEP = 0.10;

    /** free sand image file extention */
    public static final String FILE_EXTENSION = "png";

    /** default period of time to show screen messages */
    public static final long MESSAGE_DISPLAY_TIME = 500;

    /** color to paint on background to display element test results */
    public static final Color TEST_BACKGROUND_COLOR = new Color(128, 128, 128);

    /** simulation world image */
    protected World world;

    /** buffer for manual double buffering */
    protected BufferedImage frameBuffer;

    /** should the paint brush be antialiased */
    protected boolean antiAliasBrush = false;

    /** statistics panel */
    protected JPanel statsPanel;

    /** world simulation panel */
    protected JPanel worldPanel;

    /** the menu bar */
    protected JMenuBar menuBar;

    /** file chooser object */
    protected JFileChooser fileChooser;

    /** toggle display of statistics panel */
    protected JCheckBoxMenuItem statsToggleCbmi;

    /** toggle full screen mode */
    protected JCheckBoxMenuItem fullScreenCbmi;

    /** graphics for frameBuffer */
    protected Graphics2D bufferGr;

    /** graphics for world simulation */
    protected Graphics2D worldGr;

    /** a handy dandy random number generater */
    protected Random rnd = new Random();

    /** stack to store paused state on */
    protected Stack<Boolean> pausedStack = new Stack<Boolean>();

    /** the cut and paste clipboard */
    protected Clipboard clipboard;

    /** computed frame rate */
    protected double frameRate;

    /** low pass filter rate for frame rate */
    protected double rateFilter = 0.95;

    /** percent of the time spent updateing the world simulation
     * @see #paintPercent
     */
    protected double updatePercent;

    /** percent of the time spent painting the world image
     * @see #updatePercent
     */
    protected double paintPercent;

    /** current brush color */
    protected Color brushColor = WATER_EL.getColor();

    /** current brush element */
    protected Element brushElement = WATER_EL;

    /** current brush shape */
    protected Shape brushShape = circle;

    /** current brush name */
    protected String brushName = "Circle";

    /** default scale to increase brush by */
    protected float paintScale = 60;

    /** current brush size */
    protected double brushSize = 1 + MIN_BRUSH_SIZE;

    /** angle to rotate brush shape by */
    protected double brushAngle = 0;

    /** world width */
    protected int width;

    /** world height */
    protected int height;

    /** this frame */
    protected JFrame frame = null;

    /** background color of frame */
    protected Color backGround = new Color(64, 64, 64);

    /** force window to be repainted at next oportunity */
    protected boolean forcePaint = false;

    /** request that the animation thread pause @see paused */
    private boolean pauseRequest = false;

    /** paused state of animation thread @see #pauseRequest */
    private boolean paused = pauseRequest;

    /** request that simulation take 1 step */
    private boolean takeStep = false;

    /** display message @see #messageDisplayTime */
    private String message = "";

    /** time remaining to display message @see #message */
    private long messageDisplayTime = 0;

    /** triangle shape */
    public static Shape triangle = createRegularPoly(3);

    /** square shape */
    public static Shape square = normalize(new Rectangle2D.Float(0, 0, 1, 1));

    /** rectangle shape */
    public static Shape rectangle = normalize(new Rectangle2D.Float(0f, 0f, 1f, .25f));

    /** diamond shape */
    public static Shape diamond = createRegularPoly(4);

    /** pyramid shape */
    public static Shape pyramid = createPyrmidShape();

    /** pentagon shape */
    public static Shape pentagon = createRegularPoly(5);

    /** hexagon shape */
    public static Shape hexagon = createRegularPoly(6);

    /** cirlce shape */
    public static Shape circle = normalize(new Ellipse2D.Float(0, 0, 1, 1));

    /** heart shape */
    public static Shape heart = createHeartShape();

    /** star shape */
    public static Shape star = createStar(5);

    /** cat shape */
    public static Shape cat = createCatShape();

    /** dog shape */
    public static Shape dog = createDogShape();

    /** fish shape */
    public static Shape fish = createFishShape();

    javax.swing.filechooser.FileFilter fileFilter = new javax.swing.filechooser.FileFilter() {

        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String extension = getExtension(f);
            return (extension != null && extension.equals(FILE_EXTENSION));
        }

        public String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');
            if (i > 0 && i < s.length() - 1) ext = s.substring(i + 1).toLowerCase();
            return ext;
        }

        public String getDescription() {
            return "Image Files (.png)";
        }
    };

    BrushSelectionAction[] basicBrushes = { new BrushSelectionAction("Square", square, getKeyStroke(VK_S, 0)), new BrushSelectionAction("Circle", circle, getKeyStroke(VK_C, 0)), new BrushSelectionAction("Pyramid", pyramid, getKeyStroke(VK_P, 0)), new BrushSelectionAction("Rectangle", rectangle, getKeyStroke(VK_R, 0)) };

    BrushSelectionAction[] moreBrushes = { new BrushSelectionAction("Triangle", triangle, null), new BrushSelectionAction("Diamond", diamond, null), new BrushSelectionAction("Pentagon", pentagon, null), new BrushSelectionAction("Hexagon", hexagon, null) };

    BrushSelectionAction[] funBrushes = { new BrushSelectionAction("Heart", heart, null), new BrushSelectionAction("Star", star, null), new BrushSelectionAction("Cat", cat, null), new BrushSelectionAction("Dog", dog, null), new BrushSelectionAction("Fish", fish, null) };

    ElementSelectionAction[] elementActions = { new ElementSelectionAction(AIR_EL, getKeyStroke(VK_1, 0)), new ElementSelectionAction(WATER_EL, getKeyStroke(VK_2, 0)), new ElementSelectionAction(FIRE1_EL, getKeyStroke(VK_3, 0)), new ElementSelectionAction(EARTH_EL, getKeyStroke(VK_4, 0)), new ElementSelectionAction(SAND_EL, getKeyStroke(VK_5, 0)), new ElementSelectionAction(PLANT_EL, getKeyStroke(VK_6, 0)), new ElementSelectionAction(OIL_EL, getKeyStroke(VK_7, 0)), new ElementSelectionAction(ROCK_EL, getKeyStroke(VK_8, 0)), new ElementSelectionAction(AIR_SOURCE_EL, getKeyStroke(VK_1, SHIFT_MASK)), new ElementSelectionAction(WATER_SOURCE_EL, getKeyStroke(VK_2, SHIFT_MASK)), new ElementSelectionAction(FIRE_SOURCE_EL, getKeyStroke(VK_3, SHIFT_MASK)), new ElementSelectionAction(SAND_SOURCE_EL, getKeyStroke(VK_5, SHIFT_MASK)), new ElementSelectionAction(OIL_SOURCE_EL, getKeyStroke(VK_7, SHIFT_MASK)) };

    SandAction actionOpen = new SandAction("Open", getKeyStroke(VK_O, META_MASK), "load from file") {

        public void actionPerformed(ActionEvent e) {
            readWorld();
        }
    };

    SandAction actionSave = new SandAction("Save", getKeyStroke(VK_S, META_MASK), "save to file") {

        public void actionPerformed(ActionEvent e) {
            writeWorld();
        }
    };

    SandAction actionPause = new SandAction("Pause", getKeyStroke(VK_SPACE, 0), "togglel pause of the simulation") {

        public void actionPerformed(ActionEvent e) {
            togglePause();
        }
    };

    SandAction actionStep = new SandAction("Step", getKeyStroke(VK_SPACE, SHIFT_MASK), "cause simulation to take single step") {

        public void actionPerformed(ActionEvent e) {
            if (!isPaused()) pause();
            takeStep = true;
        }
    };

    SandAction actionPerformanceTest = new SandAction("Performance Tests", getKeyStroke(VK_T, CTRL_MASK), "test performance of elements") {

        public void actionPerformed(ActionEvent e) {
            new Thread() {

                public void run() {
                    String OverwriteOption = "Run Tests";
                    String CancelOption = "Cancel";
                    Object[] possibleValues = { OverwriteOption, CancelOption };
                    int n = JOptionPane.showOptionDialog(frame, "Element performance tests will overwrite your current work.", "Run Element Tests?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, possibleValues, OverwriteOption);
                    if (n == 0) testElements();
                }
            }.start();
        }
    };

    SandAction actionExit = new SandAction("Exit Program", getKeyStroke(VK_Q, META_MASK), "quit this program") {

        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    };

    SandAction actionCut = new SandAction("Cut", getKeyStroke(VK_X, META_MASK), "cut screen contents") {

        public void actionPerformed(ActionEvent e) {
            cut();
        }
    };

    SandAction actionCopy = new SandAction("Copy", getKeyStroke(VK_C, META_MASK), "copy screen contents") {

        public void actionPerformed(ActionEvent e) {
            copy();
        }
    };

    SandAction actionPaste = new SandAction("Paste", getKeyStroke(VK_V, META_MASK), "copy screen contents") {

        public void actionPerformed(ActionEvent e) {
            paste();
        }
    };

    SandAction actionFill = new SandAction("Fill Screen", getKeyStroke(VK_BACK_SPACE, SHIFT_MASK), "fill entire screen with currently selected element") {

        public void actionPerformed(ActionEvent e) {
            fillWorld(brushElement);
        }
    };

    SandAction actionToggleStatsPanel = new SandAction("Statistics", getKeyStroke(VK_A, META_MASK), "toggle visibilty of statistics menu") {

        public void actionPerformed(ActionEvent e) {
            toggleStatsPanel();
        }
    };

    SandAction actionFullScreen = new SandAction("Full Screen", getKeyStroke(VK_F, META_MASK), "toggle full screen mode") {

        public void actionPerformed(ActionEvent e) {
            toggleFullScreen();
        }
    };

    SandAction actionEscapeFullScreen = new SandAction("Escape Full Screen", getKeyStroke(VK_ESCAPE, 0), "return to windowed mode") {

        public void actionPerformed(ActionEvent e) {
            if (fullScreenCbmi.isSelected()) fullScreenCbmi.doClick();
        }
    };

    SandAction actionRotateLeft = new SandAction("Rotate Left", getKeyStroke(VK_PERIOD, SHIFT_MASK), "rotate paint cursor left") {

        public void actionPerformed(ActionEvent e) {
            brushAngle = (brushAngle + 45) % 360;
            setPaintCursor();
        }
    };

    SandAction actionRotateRight = new SandAction("Rotate Right", getKeyStroke(VK_COMMA, SHIFT_MASK), "rotate paint cursor right") {

        public void actionPerformed(ActionEvent e) {
            brushAngle = (brushAngle - 45) % 360;
            setPaintCursor();
        }
    };

    SandAction actionGrowBrush = new SandAction("Grow Brush", getKeyStroke(VK_PERIOD, 0), "increase brush size") {

        public void actionPerformed(ActionEvent e) {
            brushSize = min(brushSize + BRUSH_STEP, MAX_BRUSH_SIZE);
            setPaintCursor();
        }
    };

    SandAction actionShrinkBrush = new SandAction("Shrink Brush", getKeyStroke(VK_COMMA, 0), "decrease brush size") {

        public void actionPerformed(ActionEvent e) {
            brushSize = max(brushSize - BRUSH_STEP, MIN_BRUSH_SIZE);
            setPaintCursor();
        }
    };

    SandAction actionAbout = new SandAction("About", null, "information about the FreeSand program") {

        public void actionPerformed(ActionEvent e) {
        }
    };

    Thread animation = new Thread() {

        public void run() {
            long start = 0;
            long update = 0;
            long end = 0;
            double total = 0;
            long statsSum = 0;
            long worldSum = 0;
            while (true) {
                paused = pauseRequest;
                start = System.currentTimeMillis();
                if (!paused || takeStep) {
                    world.update();
                    if (takeStep) {
                        takeStep = false;
                        forcePaint = true;
                    }
                } else {
                    try {
                        sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                update = System.currentTimeMillis();
                worldSum += total;
                if (forcePaint || ((message != null || !paused) && worldSum >= WORLD_PAINT_MS)) {
                    world.paint(bufferGr);
                    if (messageDisplayTime > 0 && message != null) paintMessage(bufferGr, message); else message = null;
                    worldPanel.repaint();
                    worldSum = 0;
                    forcePaint = false;
                }
                end = System.currentTimeMillis();
                total = (float) (end - start);
                if (messageDisplayTime > 0) messageDisplayTime -= total;
                if (total > 0 && !paused) {
                    frameRate = rateFilter * frameRate + (1 - rateFilter) * 1000 / total;
                    updatePercent = rateFilter * updatePercent + (1 - rateFilter) * ((update - start) / total);
                    paintPercent = rateFilter * paintPercent + (1 - rateFilter) * ((end - update) / total);
                    if ((statsSum += total) >= 1000) {
                        statsSum = 0;
                        statsPanel.repaint();
                    }
                }
            }
        }
    };

    /**
     * Main entry point into program.  It creates and starts a
     * freesand game. It also sets the mac style menus.
     *
     * @param  args currently ignored
     */
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        new Game(true);
    }

    /**
     * Construct a freesand game with option to build gui
     * components at construction time.
     *
     * @param build build and start game at construction time
     * @see #buildGame()
     */
    public Game(boolean build) {
        if (build) buildGame();
    }

    /**
     * Construct gui elements, display and start game.
     */
    protected void buildGame() {
        constructFrame();
        displayFrame();
        constructWorld();
        animation.start();
        setPaintCursor();
    }

    /**
     * Put together elements of the gui frame.
     */
    protected void constructFrame() {
        frame = this;
        try {
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(fileFilter);
        menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.add(actionOpen);
        menu.add(actionSave);
        menu.addSeparator();
        menu.add(actionPause);
        menu.add(actionStep);
        menu.add(actionPerformanceTest);
        menu.addSeparator();
        menu.add(actionExit);
        menuBar.add(menu);
        menu = new JMenu("Edit");
        menu.add(actionCut);
        menu.add(actionCopy);
        menu.add(actionPaste);
        menu.addSeparator();
        menu.add(actionFill);
        menuBar.add(menu);
        menu = new JMenu("View");
        menu.add(statsToggleCbmi = new JCheckBoxMenuItem(actionToggleStatsPanel));
        menu.add(fullScreenCbmi = new JCheckBoxMenuItem(actionFullScreen));
        menu.add(actionEscapeFullScreen);
        menuBar.add(menu);
        GraphicsDevice gv = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        fullScreenCbmi.setEnabled(gv.isFullScreenSupported());
        menu = new JMenu("Brush");
        for (BrushSelectionAction bb : basicBrushes) menu.add(bb);
        menu.addSeparator();
        JMenu mBrushes = new JMenu("More Brushes");
        for (BrushSelectionAction ab : moreBrushes) mBrushes.add(ab);
        menu.add(mBrushes);
        mBrushes = new JMenu("Fun Brushes");
        for (BrushSelectionAction fb : funBrushes) mBrushes.add(fb);
        menu.add(mBrushes);
        menu.addSeparator();
        menu.add(actionRotateLeft);
        menu.add(actionRotateRight);
        menu.addSeparator();
        menu.add(actionGrowBrush);
        menu.add(actionShrinkBrush);
        menuBar.add(menu);
        setJMenuBar(menuBar);
        menu = new JMenu("Elements");
        for (ElementSelectionAction ea : elementActions) menu.add(ea);
        menuBar.add(menu);
        JToolBar toolBar = new JToolBar("Elements");
        for (ElementSelectionAction ea : elementActions) toolBar.add(ea);
        setBackground(backGround);
        worldPanel = new JPanel() {

            public void paint(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(frameBuffer, 0, 0, null);
            }
        };
        worldPanel.setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));
        worldPanel.setLayout(null);
        add(worldPanel);
        addComponentListener(new ComponentAdapter() {

            Thread resizeThread = null;

            public void componentResized(ComponentEvent e) {
                (resizeThread = new Thread() {

                    public void run() {
                        try {
                            sleep(RESIZE_DELAY);
                            if (resizeThread == this) resize();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        MouseInputAdapter mia = new MouseInputAdapter() {

            public void mouseClicked(MouseEvent e) {
                paint(e);
            }

            public void mouseDragged(MouseEvent e) {
                paint(e);
            }

            public void paint(MouseEvent e) {
                Element source = null;
                if (e.isShiftDown() && (source = Element.lookup(brushColor).lookupSourceOrOutput()) != null) worldGr.setColor(source.getColor()); else worldGr.setColor(brushColor);
                paintBrushShape(brushShape, worldGr, e.getX(), e.getY());
                forcePaint = true;
            }
        };
        worldPanel.addMouseListener(mia);
        worldPanel.addMouseMotionListener(mia);
        statsPanel = new JPanel() {

            public void paint(Graphics graphics) {
                Graphics2D gr = (Graphics2D) graphics;
                gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gr.setColor(new Color(205, 205, 205));
                gr.fill(gr.getClipBounds());
                gr.setColor(Color.GRAY);
                gr.setFont(Font.decode("Courier"));
                gr.drawString("fps:   " + round(frameRate * 100) / 100.0, 5, 15);
                gr.drawString("sim:   " + round(updatePercent * 100) + "%", 5, 30);
                gr.drawString("paint: " + round(paintPercent * 100) + "%", 5, 45);
                gr.drawString("width:  " + width, 100, 15);
                gr.drawString("height: " + height, 100, 30);
                gr.drawString("pixels: " + (width * height), 100, 45);
                gr.drawString("brush: " + brushName, 195, 15);
                gr.drawString("elmnt: " + brushElement, 195, 30);
            }
        };
        statsPanel.setPreferredSize(new Dimension(300, 55));
        statsPanel.setMinimumSize(new Dimension(150, 55));
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
    }

    /**
     * Toggle display of statistics panel.
     */
    public void toggleStatsPanel() {
        if (statsToggleCbmi.isSelected()) add(statsPanel); else remove(statsPanel);
        worldPanel.setPreferredSize(new Dimension(worldPanel.getWidth(), worldPanel.getHeight()));
        invalidate();
        statsPanel.invalidate();
        pack();
    }

    /**
     * Toggle full screen mode.
     */
    public void toggleFullScreen() {
        GraphicsDevice gv = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        if (fullScreenCbmi.isSelected()) {
            if (gv.isFullScreenSupported()) {
                setVisible(false);
                dispose();
                setUndecorated(true);
                pack();
                gv.setFullScreenWindow(this);
                setVisible(true);
            } else {
                showMessage("Not Supported");
                fullScreenCbmi.setSelected(false);
            }
        } else {
            setVisible(false);
            dispose();
            setUndecorated(false);
            pack();
            gv.setFullScreenWindow(null);
            setVisible(true);
        }
    }

    /**
     * Pack and display frame. Also record size of world.
     */
    protected void displayFrame() {
        pack();
        setVisible(true);
        ((Graphics2D) worldPanel.getGraphics()).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        width = worldPanel.getWidth();
        height = worldPanel.getHeight();
    }

    /**
     * Construct simulaiton world.
     */
    protected void constructWorld() {
        world = (world == null) ? new World(width, height) : new World(width, height, world);
        world.initialize();
        frameBuffer = new BufferedImage(world.getWidth(), world.getHeight(), BufferedImage.TYPE_INT_ARGB);
        bufferGr = (Graphics2D) frameBuffer.getGraphics();
        worldGr = (Graphics2D) world.getGraphics();
    }

    /**
     * Resize the world to match the current world panel dimentions.
     */
    public void resize() {
        pushPaused(true);
        width = worldPanel.getWidth();
        height = worldPanel.getHeight();
        constructWorld();
        popPaused();
        forcePaint = true;
    }

    /**
     * Copy world frame image to copy/paste buffer.
     */
    public void copy() {
        showMessage("Copying");
        World copy = new World(world);
        clipboard.setContents(copy, null);
        forcePaint = true;
    }

    /**
     * Cut world frame image to copy/paste buffer.
     */
    public void cut() {
        showMessage("Cutting");
        World copy = new World(world);
        clipboard.setContents(copy, null);
        fillWorld(AIR_EL);
        forcePaint = true;
    }

    /**
     * Paste image in copy/paste buffer to world frame.
     */
    public void paste() {
        pushPaused(true);
        showMessage("Pasting");
        try {
            Transferable content = clipboard.getContents(null);
            DataFlavor flavor = content.getTransferDataFlavors()[0];
            if (content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                setWorldImage((BufferedImage) content.getTransferData(DataFlavor.imageFlavor));
            } else if (flavor.isMimeTypeEqual("image/x-pict")) {
                InputStream is = (InputStream) content.getTransferData(flavor);
                Image image = (Image) getImageFromPictStream(is);
                if (image != null) setWorldImage(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        popPaused();
        forcePaint = true;
    }

    /**
     * Convert pixels in provided image to nearest {@link Element} color.
     *
     * @param  image image to convert
     * @return The modified image passed to this function. No new image is created.
     */
    public static BufferedImage convertToElements(BufferedImage image) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; ++i) pixels[i] = nearest(new Color(pixels[i])).getValue();
        return image;
    }

    /**
     * Read pict type image from input stream.  This is used to
     * read images out of the copy/paste buffer.
     *
     * @param  is inputstream from wich the image will be read.
     * @return The image read from the input stream.
     */
    @SuppressWarnings("unchecked")
    protected Image getImageFromPictStream(InputStream is) {
        try {
            java.lang.Object[] nullObjects = null;
            java.lang.Class<?>[] nullClasses = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] header = new byte[512];
            byte[] buf = new byte[4096];
            int retval = 0;
            int size = 0;
            Method m;
            baos.write(header, 0, 512);
            while ((retval = is.read(buf, 0, 4096)) > 0) baos.write(buf, 0, retval);
            baos.close();
            size = baos.size();
            if (size <= 0) return null;
            byte[] imgBytes = baos.toByteArray();
            Class c = Class.forName("quicktime.QTSession");
            m = c.getMethod("isInitialized", nullClasses);
            Boolean b = (Boolean) m.invoke(nullObjects, nullObjects);
            if (b.booleanValue() == false) {
                m = (Method) c.getMethod("open", nullClasses).invoke(null, nullObjects);
            }
            c = Class.forName("quicktime.util.QTHandle");
            Constructor con = c.getConstructor(new Class[] { imgBytes.getClass() });
            Object handle = con.newInstance(new Object[] { imgBytes });
            String s = new String("PICT");
            c = Class.forName("quicktime.util.QTUtils");
            m = c.getMethod("toOSType", new Class[] { s.getClass() });
            Integer type = (Integer) m.invoke(nullObjects, new Object[] { s });
            c = Class.forName("quicktime.std.image.GraphicsImporter");
            con = c.getConstructor(new Class[] { Integer.TYPE });
            Object importer = con.newInstance(new Object[] { type });
            m = c.getMethod("setDataHandle", new Class[] { Class.forName("quicktime.util." + "QTHandleRef") });
            m.invoke(importer, new Object[] { handle });
            m = c.getMethod("getNaturalBounds", nullClasses);
            Object rect = m.invoke(importer, nullObjects);
            c = Class.forName("quicktime.app.view.GraphicsImporterDrawer");
            con = c.getConstructor(new Class[] { importer.getClass() });
            Object iDrawer = con.newInstance(new Object[] { importer });
            m = rect.getClass().getMethod("getWidth", nullClasses);
            Integer width = (Integer) m.invoke(rect, nullObjects);
            m = rect.getClass().getMethod("getHeight", nullClasses);
            Integer height = (Integer) m.invoke(rect, nullObjects);
            Dimension d = new Dimension(width.intValue(), height.intValue());
            c = Class.forName("quicktime.app.view.QTImageProducer");
            con = c.getConstructor(new Class[] { iDrawer.getClass(), d.getClass() });
            Object producer = con.newInstance(new Object[] { iDrawer, d });
            if (producer instanceof ImageProducer) return (Toolkit.getDefaultToolkit().createImage((ImageProducer) producer));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write current world image to disk.
     */
    public void writeWorld() {
        try {
            pushPaused(true);
            showMessage("Saving");
            if (fileChooser.showSaveDialog(this) == APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.toString().toLowerCase().endsWith("." + FILE_EXTENSION)) {
                    file = new File(file + "." + FILE_EXTENSION);
                }
                if (file.exists()) {
                    String OverwriteOption = "Overwrite";
                    String CancelOption = "Cancel";
                    Object[] possibleValues = { OverwriteOption, CancelOption };
                    int n = JOptionPane.showOptionDialog(this, file.getName() + " already exists in this directory.  Should it be overwritten?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, possibleValues, OverwriteOption);
                    if (n == 0) ImageIO.write(world, "png", file);
                } else ImageIO.write(world, "png", file);
            }
            popPaused();
            forcePaint = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read an image from the disk.
     */
    public void readWorld() {
        try {
            pushPaused(true);
            showMessage("Loading");
            if (fileChooser.showOpenDialog(this) == APPROVE_OPTION) setWorldImage(ImageIO.read(fileChooser.getSelectedFile()));
            popPaused();
            forcePaint = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the world image to the provided image.
     *
     * @param  rawImage image to set as the new world image
     */
    protected void setWorldImage(Image rawImage) {
        pushPaused(true);
        BufferedImage image = new BufferedImage(rawImage.getWidth(null), rawImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawImage(rawImage, 0, 0, null);
        image = convertToElements(image);
        World newWorld = new World(image);
        if (newWorld.width != worldPanel.getWidth() || newWorld.height != worldPanel.getHeight()) {
            String ResizeOption = "Resize";
            String CancelOption = "Cancel";
            Object[] possibleValues = { ResizeOption, CancelOption };
            int n = JOptionPane.showOptionDialog(this, "Resize to " + newWorld.width + "x" + newWorld.height + " to fit new image?", "Resize?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, possibleValues, ResizeOption);
            if (n == 0) {
                worldPanel.setPreferredSize(new Dimension(newWorld.width, newWorld.height));
                invalidate();
                worldPanel.invalidate();
                pack();
                width = newWorld.width;
                height = newWorld.height;
            }
        }
        world = newWorld;
        worldGr = (Graphics2D) world.getGraphics();
        constructWorld();
        popPaused();
    }

    /**
     * Execute performance tests on all {@link Element}s.  Destroys
     * current world image.
     */
    public void testElements() {
        try {
            showMessage("Testing");
            double tmpRateFilter = rateFilter;
            rateFilter = 0;
            class Result implements Comparable<Object> {

                Element element;

                double fps;

                public Result(Element element, double fps) {
                    this.element = element;
                    this.fps = fps;
                }

                public int compareTo(Object o) {
                    Result other = (Result) o;
                    if (other.fps < fps) return -1;
                    return other.fps > fps ? 1 : 0;
                }
            }
            ;
            Vector<Result> results = new Vector<Result>();
            unpause();
            for (int i = 0; i < elementActions.length; ++i) {
                fillWorld(elementActions[i].element);
                double maxFps = 0;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < TEST_PERIOD) {
                    Thread.sleep(7);
                    if (frameRate > maxFps) maxFps = frameRate;
                }
                results.add(new Result(elementActions[i].element, maxFps));
            }
            pause();
            Thread.sleep(2 * WORLD_PAINT_MS);
            fillWorld(TEST_BACKGROUND_COLOR);
            Collections.sort(results);
            double fastest = 0;
            for (Result result : results) if (result.fps > fastest) fastest = result.fps;
            double scale = (width - 40) / fastest;
            worldGr.setFont(worldGr.getFont().deriveFont(10f));
            int i = 0;
            showMessage("Results");
            for (Result result : results) {
                Element el = result.element;
                double fps = result.fps;
                worldGr.setColor(el.getColor());
                worldGr.fillRect(20, 20 + 20 * i, (int) (scale * fps), 10);
                worldGr.setColor(computeMatchingColor(el.getColor()));
                worldGr.drawString(el + ": " + round(100 * fps) / 100d + " fps", 25, 29 + 20 * i++);
                forcePaint = true;
            }
            rateFilter = tmpRateFilter;
            forcePaint = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Push current paused state onto a stack and request new
     * paused state.  This function does not return until pause
     * state is synchronizePause.
     *
     * @param  pauseRequest new requested pause state
     * @see    #popPaused()
     * @see    #synchronizePause()
     */
    public void pushPaused(boolean pauseRequest) {
        pausedStack.push(this.pauseRequest);
        this.pauseRequest = pauseRequest;
        synchronizePause();
    }

    /**
     * Pop paused state from stack.  This function does not return
     * until pause state is synchronized.
     *
     * @return Returns the resulting paused state.
     * @see    #pushPaused(boolean pauseRequest)
     * @see    #synchronizePause()
     */
    public boolean popPaused() {
        this.pauseRequest = pausedStack.pop();
        synchronizePause();
        return isPaused();
    }

    /**
     * Pause simulation.  This function does not return until pause
     * state is synchronized.
     *
     * @see    #unpause()
     * @see    #synchronizePause()
     */
    public void pause() {
        pauseRequest = true;
        synchronizePause();
        showMessage("Paused");
    }

    /**
     * Resume paused simulation.  This function does not return
     * until pause state is synchronized.
     *
     * @see    #pause()
     * @see    #synchronizePause()
     */
    public void unpause() {
        pauseRequest = false;
        synchronizePause();
    }

    /**
     * Test paused state of simulation.  This function does not
     * return until pause state is synchronized.
     *
     * @return The paused state of the simulation.
     * @see    #pause()
     * @see    #unpause()
     * @see    #synchronizePause()
     */
    public boolean isPaused() {
        synchronizePause();
        return paused;
    }

    /**
     * Toggle paused state of simulation.  This function does not
     * return until pause state is synchronized.
     *
     * @return The paused state of the simulation.
     * @see    #synchronizePause()
     */
    public boolean togglePause() {
        pauseRequest = !pauseRequest;
        synchronizePause();
        if (paused) showMessage("Paused");
        return paused;
    }

    /**
     * Wait until the animation thread has recognized the requested
     * paused state.  This is done so that the the world is not
     * treated as though it is paused even if it's in the middle
     * updating the world.
     *
     * @see    #pause()
     * @see    #unpause()
     * @see    #isPaused()
     * @see    #pushPaused(boolean pauseRequest)
     * @see    #popPaused()
     */
    protected void synchronizePause() {
        try {
            while (paused != pauseRequest) Thread.sleep(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fill entire world image with a provided element.  This
     * function must pause or the animation thread may cause one
     * pixel to be missed.
     *
     * @param  element element to fill image with
     */
    protected void fillWorld(Element element) {
        fillWorld(element.getColor());
    }

    /**
     * Fill entire world image with a provided color.  This
     * function must pause or the animation thread may cause one
     * pixel to be missed.
     *
     * @param  color color to fill image with
     */
    protected void fillWorld(Color color) {
        pushPaused(true);
        world.fill(color);
        popPaused();
    }

    /**
     * Set current paint cursor from currently selected brush.
     */
    protected void setPaintCursor() {
        BufferedImage cursorImage = createShapeImage(transformBrush(brushShape, 0, 0), computeMatchingColor(brushColor), antiAliasBrush);
        Toolkit tk = Toolkit.getDefaultToolkit();
        Cursor cursor = tk.createCustomCursor(cursorImage, new Point(cursorImage.getWidth() / 2, cursorImage.getHeight() / 2), "");
        worldPanel.setCursor(cursor);
    }

    /**
     * Create a BufferedImage from a given Shape.  The returned
     * image has the shape painted into the image filled with the
     * provided color, and optionally is antialiased.
     *
     * @param  shape shape to create image with
     * @param  color color to fill shape when it's drawn into image
     * @param  antialias whether or not to antialias the painted shape
     * @return A BufferedImage the size of the provided shape.
     */
    public static BufferedImage createShapeImage(Shape shape, Color color, boolean antialias) {
        Rectangle2D bounds = shape.getBounds();
        int size = max((int) (bounds.getWidth()), (int) (bounds.getHeight()));
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) image.getGraphics();
        if (antialias) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate((size - bounds.getWidth()) / 2 - bounds.getX(), (size - bounds.getHeight()) / 2 - bounds.getY());
        g.setColor(color);
        g.fill(shape);
        return image;
    }

    /**
     * Set the cursor for a given container and all its descendents.
     * @deprecated This function should no loger be needed as
     * cursors are being handled in a better way.
     *
     * @param  container container to change cursor of
     * @param  cursor new cursor for the container
     */
    public static void setCursor(Container container, Cursor cursor) {
        for (Component c : container.getComponents()) {
            c.setCursor(cursor);
            if (c instanceof Container) setCursor((Container) c, cursor);
        }
    }

    /**
     * SandAction is derived from AbstractAction and provides a
     * standard class from which to subclass Game actions.
     */
    public abstract class SandAction extends AbstractAction {

        /**
         * Create a SandAction with a given name, shortcut key
         * and description.
         *
         * @param  name name of action
         * @param  key shortcut key to trigger action
         */
        public SandAction(String name, KeyStroke key, String description) {
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, description);
            putValue(ACCELERATOR_KEY, key);
        }

        /**
         * Called when the given action is to be executed.  This
         * function must be implemented by the subclass.
         *
         * @param  e action event
         */
        public abstract void actionPerformed(ActionEvent e);

        public boolean updateEnabledState() {
            return isEnabled();
        }
    }

    /**
     * BrushAction is derived from SandActionAction and is used
     * to select different brushes.
     */
    protected class BrushSelectionAction extends SandAction {

        Shape brush;

        public BrushSelectionAction(String name, Shape brush, KeyStroke key) {
            super(name, key, "Select " + name.toLowerCase() + " brush");
            this.brush = brush;
            putValue(SMALL_ICON, new ImageIcon(createShapeImage(transformBrush(brush, 0, 0), DARK_GRAY, true)));
        }

        public void actionPerformed(ActionEvent e) {
            showMessage(getValue(NAME) + " Brush");
            brushShape = brush;
            brushName = getValue(NAME).toString();
            setPaintCursor();
            forcePaint = true;
        }
    }

    class ElementSelectionAction extends SandAction {

        Element element;

        public ElementSelectionAction(Element element, KeyStroke key) {
            super(null, key, "Select " + element + " element");
            this.element = element;
            BufferedImage elementImage = new BufferedImage(70, 25, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = elementImage.getGraphics();
            g.setClip(0, 0, elementImage.getWidth(), elementImage.getHeight());
            paint(g);
            putValue(SMALL_ICON, new ImageIcon(elementImage));
        }

        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 0));
            Rectangle2D bounds = g.getClipBounds();
            g.fill(bounds);
            g.setColor(element.getColor());
            g.fillRoundRect(0, 0, (int) bounds.getWidth() - 1, (int) bounds.getHeight() - 1, 20, 20);
            g.setFont(g.getFont().deriveFont(10f));
            g.setColor(computeMatchingColor(element.getColor()));
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D sBounds = fm.getStringBounds(element.toString(), g);
            g.drawString(element.toString(), (int) ((bounds.getWidth() - sBounds.getWidth()) / 2), (int) ((bounds.getHeight() + sBounds.getHeight() / 2) / 2));
        }

        public void actionPerformed(ActionEvent e) {
            showMessage(element.toString());
            brushColor = element.getColor();
            brushElement = element;
            setPaintCursor();
            forcePaint = true;
        }
    }

    /**
     * Show a message on the screen for the default amount of time.
     *
     * @param  message message to show on the screen
     * @see    #MESSAGE_DISPLAY_TIME
     */
    public void showMessage(String message) {
        showMessage(message, MESSAGE_DISPLAY_TIME);
    }

    /**
     * Show a message on the screen for the specifed number of miliseconds.
     *
     * @param  message message to show on the screen
     * @param  messageDisplayTime time in miliseconds to display message
     */
    public void showMessage(String message, long messageDisplayTime) {
        this.message = message;
        this.messageDisplayTime = messageDisplayTime;
        forcePaint = true;
    }

    /**
     * Paint provided message onto the provided graphics object as
     * big as possible.  The function does not know how to handle
     * carriage returns.
     *
     * @param  g graphic on which to draw message
     * @param  message message to draw
     */
    public void paintMessage(Graphics2D g, String message) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle2D sBounds = g.getFontMetrics().getStringBounds(message, g);
        double increase = (worldPanel.getWidth() / sBounds.getWidth()) * 0.9d;
        Font font = g.getFont();
        g.setFont(font.deriveFont(font.getSize() * (float) increase));
        g.setColor(new Color(255, 255, 255, 128));
        sBounds = g.getFontMetrics().getStringBounds(message, g);
        g.drawString(message, (int) ((worldPanel.getWidth() - sBounds.getWidth()) / 2), (int) ((worldPanel.getHeight() + sBounds.getHeight() / 2) / 2));
        g.setFont(font);
    }

    /**
     * Given a color, compute a close color which will be visible
     * if overlayed on the original color.
     *
     * @param  color color to find match to
     * @return The new matched color.
     */
    Color computeMatchingColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), new float[3]);
        return hsb[2] < 0.2 ? Color.getHSBColor(hsb[0], hsb[1], hsb[2] < .1 ? 0.5f : hsb[2] * 2f) : Color.getHSBColor(hsb[0], hsb[1], hsb[2] / 2);
    }

    /**
     * Create normalized pyramid shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createPyrmidShape() {
        Area gp = new Area();
        gp.add(new Area(createRegularPoly(4)));
        gp.subtract(new Area(translate(square, 0, 0.5)));
        return normalize(gp);
    }

    /**
     * Create normalized heart shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createHeartShape() {
        GeneralPath gp = new GeneralPath();
        gp.append(translate(circle, 0.5, 0), false);
        gp.append(translate(circle, 0, 0.5), false);
        gp.append(square, false);
        return normalize(rotate(gp, 225));
    }

    /**
     * Create normalized cat shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createCatShape() {
        Area cat = new Area(circle);
        Area wisker = new Area(new Rectangle2D.Double(0, -.01, .3, .02));
        Area leftWiskers = new Area();
        leftWiskers.add(rotate(wisker, -20));
        leftWiskers.add(rotate(wisker, 20));
        leftWiskers.add(rotate(wisker, 20));
        Area rightWiskers = new Area();
        rightWiskers.add(rotate(wisker, 180));
        rightWiskers.add(rotate(wisker, -20));
        rightWiskers.add(rotate(wisker, -20));
        Area ear = new Area(translate(scale(triangle, .5, .5), 0.0, -0.6));
        translate(ear, .07, 0);
        cat.add(ear);
        rotate(cat, 60);
        translate(ear, -.14, 0);
        cat.add(ear);
        rotate(cat, -30);
        Area eye = new Area(scale(circle, 0.18, 0.18));
        eye.subtract(new Area(scale(circle, .06, .12)));
        translate(eye, -.15, -.1);
        cat.subtract(eye);
        translate(eye, .3, 0);
        cat.subtract(eye);
        cat.subtract(translate(leftWiskers, .08, .14));
        cat.subtract(translate(rightWiskers, -.08, .14));
        Area nose = new Area(createRegularPoly(3));
        rotate(nose, 180);
        scale(nose, .15, .15);
        translate(nose, 0, .1);
        cat.subtract(nose);
        scale(cat, 1.0, 0.85);
        return normalize(cat);
    }

    /**
     * Create normalized dog shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createDogShape() {
        Area dog = new Area(circle);
        Area ear = new Area(scale(circle, .4, .7));
        rotate(ear, 20);
        translate(ear, -.5, -.2);
        dog.subtract(ear);
        scale(ear, -1, 1);
        dog.subtract(ear);
        scale(ear, -1, 1);
        translate(ear, -.05, 0);
        dog.add(ear);
        scale(ear, -1, 1);
        dog.add(ear);
        scale(ear, -1, 1);
        Area eye = new Area(scale(circle, 0.18, 0.18));
        eye.subtract(new Area(scale(circle, .12, .12)));
        translate(eye, -.15, -.1);
        dog.subtract(eye);
        translate(eye, .3, 0);
        dog.subtract(eye);
        Area snout = new Area(circle);
        scale(snout, .30, .30);
        translate(snout, 0, .2);
        dog.subtract(snout);
        Area nose = new Area(createRegularPoly(3));
        rotate(nose, 180);
        scale(nose, .20, .20);
        translate(nose, 0, .2);
        dog.add(nose);
        scale(dog, 0.90, 1.0);
        return normalize(dog);
    }

    /**
     * Create normalized fish shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createFishShape() {
        Area fish = new Area();
        Area body = new Area(new Arc2D.Double(0.0, 0, 1.0, 1.0, 30, 120, Arc2D.CHORD));
        Rectangle2D bounds = body.getBounds2D();
        translate(body, -(bounds.getX() + bounds.getWidth() / 2), -bounds.getHeight());
        fish.add(body);
        scale(body, 1, -1);
        fish.add(body);
        Area eye = new Area(scale(circle, .13, .13));
        eye.subtract(new Area(scale(circle, .08, .08)));
        translate(eye, -.15, -.08);
        fish.subtract(eye);
        Area tail = new Area(normalize(rotate(triangle, 30)));
        scale(tail, .50, .50);
        translate(tail, .4, 0);
        fish.add(tail);
        return normalize(fish);
    }

    /**
     * Create normalized regular polygon shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createRegularPoly(int edges) {
        double radius = 1000;
        double theta = 0.75 * (2 * Math.PI);
        double dTheta = (2 * Math.PI) / edges;
        Polygon p = new Polygon();
        for (int edge = 0; edge < edges; ++edge) {
            p.addPoint((int) (Math.cos(theta) * radius), (int) (Math.sin(theta) * radius));
            theta += dTheta;
        }
        return normalize(p);
    }

    /**
     * Create normalized star shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */
    public static Shape createStar(int points) {
        double radius = 1000;
        double theta = 0.75 * (2 * Math.PI);
        double dTheta = (4 * Math.PI) / points;
        Polygon p = new Polygon();
        for (int point = 0; point < points; ++point) {
            p.addPoint((int) (Math.cos(theta) * radius), (int) (Math.sin(theta) * radius));
            theta += dTheta;
        }
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        gp.append(p, true);
        return normalize(gp);
    }

    /**
     * Create a transformed version of the provided shape with a
     * new width & length <= 1 and centered at the origin (0, 0).
     *
     * @param  shape source shape to tranform, it is not changed
     * @return The newly normalized shape.
     */
    public static Shape normalize(Shape shape) {
        Rectangle2D bounds = shape.getBounds2D();
        shape = translate(shape, -(bounds.getX() + bounds.getWidth() / 2), -(bounds.getY() + bounds.getHeight() / 2));
        bounds = shape.getBounds2D();
        double scale = bounds.getWidth() > bounds.getHeight() ? 1.0 / bounds.getWidth() : 1.0 / bounds.getHeight();
        return scale(shape, scale, scale);
    }

    /**
     * Rotate a provided shape the number of degrees specified.
     *
     * @param  shape source shape to rotate, remains unchanged
     * @param  degrees to rotate shape
     * @return The new instance of the rotated shape.
     */
    public static Shape rotate(Shape shape, double degrees) {
        return AffineTransform.getRotateInstance(degrees / 180 * Math.PI).createTransformedShape(shape);
    }

    /**
     * Rotate a provided shape around the point specifed, the
     * number of degrees specified.
     *
     * @param  shape source shape to rotate, remains unchanged
     * @param  degrees to rotate shape
     * @param  x x location of point of rotation
     * @param  y y location of point of rotation
     * @return The new instance of the rotated shape.
     */
    public static Shape rotate(Shape shape, double degrees, double x, double y) {
        return AffineTransform.getRotateInstance(degrees / 180 * Math.PI, x, y).createTransformedShape(shape);
    }

    /**
     * Translate a provided shape by the amounts specifed.
     *
     * @param  shape source shape to translate, remains unchanged
     * @param  x x location to translate shape to
     * @param  y y location to translate shape to
     * @return The new instance of the translated shape.
     */
    public static Shape translate(Shape shape, double x, double y) {
        return AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
    }

    /**
     * Scale a provided shape by the amounts specifed.
     *
     * @param  shape source shape to translate, remains unchanged
     * @param  x scale shape in x dimention by this amount
     * @param  y scale shape in y dimention by this amount
     * @return The new instance of the scaled shape.
     */
    public static Shape scale(Shape shape, double x, double y) {
        return AffineTransform.getScaleInstance(x, y).createTransformedShape(shape);
    }

    /**
     * Rotate a provided area the number of degrees specified.
     *
     * @param  area source area to rotate, remains unchanged
     * @param  degrees to rotate area
     * @return The new instance of the rotated area.
     */
    public static Area rotate(Area area, double degrees) {
        area.transform(AffineTransform.getRotateInstance(degrees / 180 * Math.PI));
        return area;
    }

    /**
     * Translate a provided area by the amounts specifed.
     *
     * @param  area source area to translate, remains unchanged
     * @param  x x location to translate area to
     * @param  y y location to translate area to
     * @return The new instance of the translated area.
     */
    public static Area translate(Area area, double x, double y) {
        area.transform(AffineTransform.getTranslateInstance(x, y));
        return area;
    }

    /**
     * Scale a provided area by the amounts specifed.
     *
     * @param  area source area to translate, remains unchanged
     * @param  x scale area in x dimention by this amount
     * @param  y scale area in y dimention by this amount
     * @return The new instance of the scaled area.
     */
    public static Area scale(Area area, double x, double y) {
        area.transform(AffineTransform.getScaleInstance(x, y));
        return area;
    }

    /**
     * Perform all standard transforms to brush before it is
     * painted to the screen.
     *
     * @param  brush original brush shape
     * @param  x x location translate brush to
     * @param  y y location translate brush to
     * @return The new instance of the tranfromed brush shape.
     */
    public Shape transformBrush(Shape brush, double x, double y) {
        brush = rotate(brush, brushAngle);
        brush = scale(brush, paintScale * brushSize, paintScale * brushSize);
        brush = translate(brush, x, y);
        return brush;
    }

    /**
     * Paint brush onto provided graphics.
     *
     * @param  brush brush shape
     * @param  x x location translate brush to
     * @param  y y location translate brush to
     * @return The new instance of the tranfromed brush shape.
     */
    public Shape paintBrushShape(Shape brush, Graphics2D g, double x, double y) {
        brush = transformBrush(brush, x, y);
        g.fill(brush);
        return brush;
    }
}
