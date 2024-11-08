package org.mars_sim.msp.ui.swing;

import org.apache.commons.io.IOUtils;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.swing.sound.AudioPlayer;
import org.mars_sim.msp.ui.swing.tool.ToolWindow;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static class for saving/loading user interface configuration data.
 */
public class UIConfig {

    private static String CLASS_NAME = "org.mars_sim.msp.ui.standard.UIConfig";

    private static Logger logger = Logger.getLogger(CLASS_NAME);

    public static final UIConfig INSTANCE = new UIConfig();

    public static final String TOOL = "tool";

    public static final String UNIT = "unit";

    private static final String DIRECTORY = System.getProperty("user.home") + File.separator + ".mars-sim" + File.separator + "saved";

    private static final String FILE_NAME = "ui_settings.xml";

    private static final String FILE_NAME_DTD = "ui_settings.dtd";

    private static final String UI = "ui";

    private static final String USE_DEFAULT = "use-default";

    private static final String LOOK_AND_FEEL = "look-and-feel";

    private static final String SHOW_UNIT_BAR = "show-unit-bar";

    private static final String SHOW_TOOL_BAR = "show-tool-bar";

    private static final String MAIN_WINDOW = "main-window";

    private static final String LOCATION_X = "location-x";

    private static final String LOCATION_Y = "location-y";

    private static final String WIDTH = "width";

    private static final String HEIGHT = "height";

    private static final String VOLUME = "volume";

    private static final String SOUND = "sound";

    private static final String MUTE = "mute";

    private static final String INTERNAL_WINDOWS = "internal-windows";

    private static final String WINDOW = "window";

    private static final String TYPE = "type";

    private static final String NAME = "name";

    private static final String DISPLAY = "display";

    private static final String Z_ORDER = "z-order";

    private Document configDoc;

    /** 
     * Private singleton constructor.
     */
    private UIConfig() {
    }

    /**
     * Loads and parses the XML save file.
     */
    public void parseFile() {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(new File(DIRECTORY, FILE_NAME));
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
            SAXBuilder saxBuilder = new SAXBuilder(true);
            configDoc = saxBuilder.build(reader);
        } catch (Exception e) {
            if (!(e instanceof FileNotFoundException)) logger.log(Level.SEVERE, "parseFile()", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Creates an XML document for the UI configuration and saves it to a file.
     * 
     * @param window the main window.
     */
    public void saveFile(MainWindow window) {
        FileOutputStream stream = null;
        try {
            Document outputDoc = new Document();
            DocType dtd = new DocType(UI, DIRECTORY + File.separator + FILE_NAME_DTD);
            Element uiElement = new Element(UI);
            outputDoc.setDocType(dtd);
            outputDoc.addContent(uiElement);
            outputDoc.setRootElement(uiElement);
            uiElement.setAttribute(USE_DEFAULT, "false");
            uiElement.setAttribute(SHOW_TOOL_BAR, Boolean.toString(window.getToolToolBar().isVisible()));
            uiElement.setAttribute(SHOW_UNIT_BAR, Boolean.toString(window.getUnitToolBar().isVisible()));
            String currentLFClassName = UIManager.getLookAndFeel().getClass().getName();
            String systemLFClassName = UIManager.getSystemLookAndFeelClassName();
            if (currentLFClassName.equals(systemLFClassName)) uiElement.setAttribute(LOOK_AND_FEEL, "native"); else uiElement.setAttribute(LOOK_AND_FEEL, "default");
            Element mainWindowElement = new Element(MAIN_WINDOW);
            uiElement.addContent(mainWindowElement);
            mainWindowElement.setAttribute(LOCATION_X, Integer.toString(window.getFrame().getX()));
            mainWindowElement.setAttribute(LOCATION_Y, Integer.toString(window.getFrame().getY()));
            mainWindowElement.setAttribute(WIDTH, Integer.toString(window.getFrame().getWidth()));
            mainWindowElement.setAttribute(HEIGHT, Integer.toString(window.getFrame().getHeight()));
            Element volumeElement = new Element(VOLUME);
            uiElement.addContent(volumeElement);
            AudioPlayer player = window.getDesktop().getSoundPlayer();
            volumeElement.setAttribute(SOUND, Float.toString(player.getVolume()));
            volumeElement.setAttribute(MUTE, Boolean.toString(player.isMute()));
            Element internalWindowsElement = new Element(INTERNAL_WINDOWS);
            uiElement.addContent(internalWindowsElement);
            MainDesktopPane desktop = window.getDesktop();
            JInternalFrame[] windows = desktop.getAllFrames();
            for (JInternalFrame window1 : windows) {
                Element windowElement = new Element(WINDOW);
                internalWindowsElement.addContent(windowElement);
                windowElement.setAttribute(Z_ORDER, Integer.toString(desktop.getComponentZOrder(window1)));
                windowElement.setAttribute(LOCATION_X, Integer.toString(window1.getX()));
                windowElement.setAttribute(LOCATION_Y, Integer.toString(window1.getY()));
                windowElement.setAttribute(WIDTH, Integer.toString(window1.getWidth()));
                windowElement.setAttribute(HEIGHT, Integer.toString(window1.getHeight()));
                windowElement.setAttribute(DISPLAY, Boolean.toString(!window1.isClosed()));
                if (window1 instanceof ToolWindow) {
                    windowElement.setAttribute(TYPE, TOOL);
                    windowElement.setAttribute(NAME, ((ToolWindow) window1).getToolName());
                } else if (window1 instanceof UnitWindow) {
                    windowElement.setAttribute(TYPE, UNIT);
                    windowElement.setAttribute(NAME, ((UnitWindow) window1).getUnit().getName());
                } else {
                    windowElement.setAttribute(TYPE, "other");
                    windowElement.setAttribute(NAME, "other");
                }
            }
            Unit[] toolBarUnits = window.getUnitToolBar().getUnitsInToolBar();
            for (Unit toolBarUnit : toolBarUnits) {
                UnitWindow unitWindow = desktop.findUnitWindow(toolBarUnit);
                if ((unitWindow == null) || unitWindow.isIcon()) {
                    Element windowElement = new Element(WINDOW);
                    internalWindowsElement.addContent(windowElement);
                    windowElement.setAttribute(TYPE, UNIT);
                    windowElement.setAttribute(NAME, toolBarUnit.getName());
                    windowElement.setAttribute(DISPLAY, "false");
                }
            }
            File configFile = new File(DIRECTORY, FILE_NAME);
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            InputStream in = getClass().getResourceAsStream("/dtd/ui_settings.dtd");
            IOUtils.copy(in, new FileOutputStream(new File(DIRECTORY, "ui_settings.dtd")));
            XMLOutputter fmt = new XMLOutputter();
            fmt.setFormat(Format.getPrettyFormat());
            stream = new FileOutputStream(configFile);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
            fmt.output(outputDoc, writer);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Checks if UI should use default configuration.
     * 
     * @return true if default.
     */
    public boolean useUIDefault() {
        try {
            Element root = configDoc.getRootElement();
            return Boolean.parseBoolean(root.getAttributeValue(USE_DEFAULT));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Checks if UI should use native or default look & feel.
     * 
     * @return true if native.
     */
    public boolean useNativeLookAndFeel() {
        try {
            Element root = configDoc.getRootElement();
            String lookAndFeel = root.getAttributeValue(LOOK_AND_FEEL);
            return (lookAndFeel.equals("native"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if UI should show the Tool bar.
     * 
     * @return true if default.
     */
    public boolean showToolBar() {
        try {
            Element root = configDoc.getRootElement();
            return Boolean.parseBoolean(root.getAttributeValue(SHOW_TOOL_BAR));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Checks if UI should show the Unit bar.
     * 
     * @return true if default.
     */
    public boolean showUnitBar() {
        try {
            Element root = configDoc.getRootElement();
            return Boolean.parseBoolean(root.getAttributeValue(SHOW_UNIT_BAR));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Gets the screen location of the main window origin.
     * 
     * @return location.
     */
    public Point getMainWindowLocation() {
        try {
            Element root = configDoc.getRootElement();
            Element mainWindow = root.getChild(MAIN_WINDOW);
            int x = Integer.parseInt(mainWindow.getAttributeValue(LOCATION_X));
            int y = Integer.parseInt(mainWindow.getAttributeValue(LOCATION_Y));
            return new Point(x, y);
        } catch (Exception e) {
            return new Point(0, 0);
        }
    }

    /**
     * Gets the size of the main window.
     * 
     * @return size.
     */
    public Dimension getMainWindowDimension() {
        try {
            Element root = configDoc.getRootElement();
            Element mainWindow = root.getChild(MAIN_WINDOW);
            int width = Integer.parseInt(mainWindow.getAttributeValue(WIDTH));
            int height = Integer.parseInt(mainWindow.getAttributeValue(HEIGHT));
            return new Dimension(width, height);
        } catch (Exception e) {
            return new Dimension(300, 300);
        }
    }

    /**
     * Gets the sound volume level.
     * 
     * @return volume (0 (silent) to 1 (loud)).
     */
    public float getVolume() {
        try {
            Element root = configDoc.getRootElement();
            Element volume = root.getChild(VOLUME);
            return Float.parseFloat(volume.getAttributeValue(SOUND));
        } catch (Exception e) {
            return 50F;
        }
    }

    /**
     * Checks if sound volume is set to mute.
     * 
     * @return true if mute.
     */
    public boolean isMute() {
        try {
            Element root = configDoc.getRootElement();
            Element volume = root.getChild(VOLUME);
            return Boolean.parseBoolean(volume.getAttributeValue(MUTE));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if an internal window is displayed.
     * 
     * @param windowName the window name.
     * @return true if displayed.
     */
    @SuppressWarnings("unchecked")
    public boolean isInternalWindowDisplayed(String windowName) {
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            boolean result = false;
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    String name = internalWindow.getAttributeValue(NAME);
                    if (name.equals(windowName)) {
                        result = Boolean.parseBoolean(internalWindow.getAttributeValue(DISPLAY));
                        break;
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the origin location of an internal window on the desktop.
     * 
     * @param windowName the window name.
     * @return location.
     */
    @SuppressWarnings("unchecked")
    public Point getInternalWindowLocation(String windowName) {
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            Point result = new Point(0, 0);
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    String name = internalWindow.getAttributeValue(NAME);
                    if (name.equals(windowName)) {
                        int locationX = Integer.parseInt(internalWindow.getAttributeValue(LOCATION_X));
                        int locationY = Integer.parseInt(internalWindow.getAttributeValue(LOCATION_Y));
                        result.setLocation(locationX, locationY);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return new Point(0, 0);
        }
    }

    /**
     * Gets the z order of an internal window on the desktop.
     * 
     * @param windowName the window name.
     * @return z order (lower number represents higher up)
     */
    @SuppressWarnings("unchecked")
    public int getInternalWindowZOrder(String windowName) {
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            int result = -1;
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    String name = internalWindow.getAttributeValue(NAME);
                    if (name.equals(windowName)) result = Integer.parseInt(internalWindow.getAttributeValue(Z_ORDER));
                }
            }
            return result;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Gets the size of an internal window.
     * 
     * @param windowName the window name.
     * @return size.
     */
    @SuppressWarnings("unchecked")
    public Dimension getInternalWindowDimension(String windowName) {
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            Dimension result = new Dimension(0, 0);
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    String name = internalWindow.getAttributeValue(NAME);
                    if (name.equals(windowName)) {
                        int width = Integer.parseInt(internalWindow.getAttributeValue(WIDTH));
                        int height = Integer.parseInt(internalWindow.getAttributeValue(HEIGHT));
                        result = new Dimension(width, height);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return new Dimension(0, 0);
        }
    }

    /**
     * Gets the internal window type.
     * 
     * @param windowName the window name.
     * @return "unit" or "tool".
     */
    @SuppressWarnings("unchecked")
    public String getInternalWindowType(String windowName) {
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            String result = "";
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    String name = internalWindow.getAttributeValue(NAME);
                    if (name.equals(windowName)) result = internalWindow.getAttributeValue(TYPE);
                }
            }
            return result;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks if internal window is configured.
     * 
     * @param windowName the window name.
     * @return true if configured.
     */
    @SuppressWarnings("unchecked")
    public boolean isInternalWindowConfigured(String windowName) {
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            boolean result = false;
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    String name = internalWindow.getAttributeValue(NAME);
                    if (name.equals(windowName)) result = true;
                }
            }
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets all of the internal window names.
     * 
     * @return list of window names.
     */
    @SuppressWarnings("unchecked")
    public List<String> getInternalWindowNames() {
        List<String> result = new ArrayList<String>();
        try {
            Element root = configDoc.getRootElement();
            Element internalWindows = root.getChild(INTERNAL_WINDOWS);
            List<Object> internalWindowNodes = internalWindows.getChildren();
            for (Object element : internalWindowNodes) {
                if (element instanceof Element) {
                    Element internalWindow = (Element) element;
                    result.add(internalWindow.getAttributeValue(NAME));
                }
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }
}
