package geomss.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Frame;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.event.*;
import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.PrintStream;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.DefaultEditorKit;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Length;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.pickfast.PickCanvas;
import bsh.util.JConsole;
import bsh.Interpreter;
import bsh.EvalError;
import net.roydesign.app.QuitJMenuItem;
import net.roydesign.app.AboutJMenuItem;
import net.roydesign.mac.MRJAdapter;
import net.roydesign.ui.FolderDialog;
import jahuwaldt.swing.*;
import jahuwaldt.io.ExtFilenameFilter;
import jahuwaldt.j3d.*;
import jahuwaldt.j3d.image.*;
import geomss.geom.*;
import geomss.geom.reader.*;
import geomss.j3d.J3DGeomGroup;
import geomss.j3d.RenderType;
import static geomss.j3d.ProjectionPolicy.*;
import geomss.ui.InputDialog;
import geomss.ui.DialogItem;
import geomss.GeomSSApp;
import geomss.GeomSSScene;
import geomss.GeomSSUtil;

/**
*  Main window for the GeomSS program.  Most of the program
*  code is based here.
*
*  <p>  Modified by:  Joseph A. Huwaldt   </p>
*
*  @author  Joseph A. Huwaldt   Date: May 2, 2009
*  @version August 8, 2011
**/
public class MainWindow extends JFrame {

    private static final short CANVAS_WIDTH = 640;

    private static final short CANVAS_HEIGHT = 480;

    private static final String WINDOW_MODIFIED = "windowModified";

    private JMenuBar _menuBar;

    private static final int kFileMenu = 0;

    private static final int kSaveItem = 4;

    private static final int kExportAsItem = 7;

    private static final int kEditMenu = 1;

    private static final int kUndoItem = 0;

    private static final int kRedoItem = 1;

    private static final int kCutItem = 3;

    private static final int kCopyItem = 4;

    private static final int kPasteItem = 5;

    private final GeomSS _app;

    private QuitListener _quitListener;

    private PageFormat _pf = null;

    /**
	*  A count of the number of instances of the application window that have been opened.
	**/
    private static int _instanceCount = 0;

    private List<GeomReader> _geomWriters = new ArrayList();

    private GeomSSCanvas3D _canvas;

    private final PNGImageObserver _PNGObserver = new PNGImageObserver();

    private final JPEGImageObserver _JPEGObserver = new JPEGImageObserver();

    private final JConsole _console = new JConsole();

    private final Interpreter _bsh = new Interpreter(_console);

    /**
	*  Constructor for our application window.
	*
	*  @param  name        The name of this window (usually file name of data file or "Untitled").
	*  @param  newData     The data set this window contains.
	*  @param  application A reference to the application that created this window.
	**/
    private MainWindow(String name, GeomElement newData, GeomSS application) throws NoSuchMethodException {
        super(name);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setLocationByPlatform(true);
        _app = application;
        initDisplay(newData);
        _menuBar = createMenuBar();
        this.setJMenuBar(_menuBar);
        _quitListener = new QuitListener() {

            public boolean quit() {
                return !windowShouldClose();
            }
        };
        _app.getGUIApplication().addQuitListener(_quitListener);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                if (windowShouldClose()) {
                    _app.getGUIApplication().removeQuitListener(_quitListener);
                    MainWindow.this.dispose();
                }
            }
        });
        this.pack();
        initializeBeanShell();
        ++_instanceCount;
    }

    /**
	*  Initializes the BeanShell environment for this program.
	**/
    private void initializeBeanShell() {
        new Thread(_bsh).start();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    GeomSSUtil.setInterpreter(_bsh);
                    ResourceBundle resBundle = _app.getResourceBundle();
                    GeomSSBatch.initializeBeanShell(resBundle.getString("appName"), _bsh, new PublicInterface());
                    String path = _app.getPreferences().getLastPath();
                    if (path != null) _bsh.eval("cd(\"" + path + "\");");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
	*  Sets the title for this frame to the specified string.  Also notifies the main
	*  application class so that the "Windows" menu gets updated too.
	*
	*  @param  title The title to be displayed in the frame's border.
	*                A null value is treated as an empty string, "".
	**/
    public void setTitle(String title) {
        super.setTitle(title);
        _app.getGUIApplication().windowTitleChanged(this);
    }

    /**
	*  Layouts the contents of this main application window.
	*
	*  @param  theData  The data in this window.
	**/
    private void initDisplay(GeomElement theData) throws NoSuchMethodException {
        Container cp = this.getContentPane();
        cp.setLayout(new BorderLayout());
        _canvas = new GeomSSCanvas3D(_app.getResourceBundle(), theData, CANVAS_WIDTH, CANVAS_HEIGHT);
        _canvas.addCaptureObserver(_PNGObserver);
        _canvas.addCaptureObserver(_JPEGObserver);
        JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _console, _canvas);
        cp.add(content, BorderLayout.CENTER);
        content.setOneTouchExpandable(true);
        _canvas.setMinimumSize(new Dimension(200, 200));
        _canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
        _console.setPreferredSize(new Dimension(CANVAS_WIDTH, 200));
        _console.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JToolBar toolbar = createToolbar();
        cp.add(toolbar, BorderLayout.NORTH);
    }

    /**
	*  Initializes the menus associated with this window.
	**/
    private JMenuBar createMenuBar() throws NoSuchMethodException {
        ResourceBundle resBundle = _app.getResourceBundle();
        JMenuBar menuBar = new JMenuBar();
        List<String[]> menuStrings = new ArrayList();
        String[] row = new String[3];
        row[0] = resBundle.getString("newItemText");
        row[1] = resBundle.getString("newItemKey");
        row[2] = "handleNew";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("openItemText");
        row[1] = resBundle.getString("openItemKey");
        row[2] = "handleOpen";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("srcScriptItemText");
        row[1] = null;
        row[2] = "handleSrcScript";
        menuStrings.add(row);
        menuStrings.add(new String[3]);
        row = new String[3];
        row[0] = resBundle.getString("closeItemText");
        row[1] = resBundle.getString("closeItemKey");
        row[2] = "handleClose";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("changeCWDItemText");
        row[1] = null;
        row[2] = "handleChangeCWD";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("saveItemText");
        row[1] = resBundle.getString("saveItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("saveAsItemText");
        row[1] = null;
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("exportAsItemText");
        row[1] = null;
        row[2] = null;
        menuStrings.add(row);
        menuStrings.add(new String[3]);
        row = new String[3];
        row[0] = resBundle.getString("pageSetupItemText");
        row[1] = null;
        row[2] = "handlePageSetup";
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("printItemText");
        row[1] = resBundle.getString("printItemKey");
        row[2] = "handlePrint";
        menuStrings.add(row);
        JMenu menu = AppUtilities.buildMenu(this, resBundle.getString("fileMenuText"), menuStrings);
        menuBar.add(menu);
        JMenu exportMenu = buildExportMenu(menu.getItem(kExportAsItem).getText());
        if (exportMenu != null) {
            menu.remove(kExportAsItem);
            menu.add(exportMenu, kExportAsItem);
        }
        MDIApplication guiApp = _app.getGUIApplication();
        QuitJMenuItem quit = guiApp.getQuitJMenuItem();
        quit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                _app.getGUIApplication().handleQuit(e);
            }
        });
        if (!quit.isAutomaticallyPresent()) {
            menu.addSeparator();
            menu.add(quit);
        }
        menuStrings.clear();
        row = new String[3];
        row[0] = resBundle.getString("undoItemText");
        row[1] = resBundle.getString("undoItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("redoItemText");
        row[1] = null;
        row[2] = null;
        menuStrings.add(row);
        menuStrings.add(new String[3]);
        row = new String[3];
        row[0] = resBundle.getString("cutItemText");
        row[1] = resBundle.getString("cutItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("copyItemText");
        row[1] = resBundle.getString("copyItemKey");
        row[2] = null;
        menuStrings.add(row);
        row = new String[3];
        row[0] = resBundle.getString("pasteItemText");
        row[1] = resBundle.getString("pasteItemKey");
        row[2] = null;
        menuStrings.add(row);
        menu = AppUtilities.buildMenu(this, resBundle.getString("editMenuText"), menuStrings);
        TransferActionListener transferActionListener = new TransferActionListener();
        setMenuItemAction(menu.getItem(kCutItem), transferActionListener, new DefaultEditorKit.CutAction(), (String) TransferHandler.getCutAction().getValue(Action.NAME));
        setMenuItemAction(menu.getItem(kCopyItem), transferActionListener, new DefaultEditorKit.CopyAction(), (String) TransferHandler.getCopyAction().getValue(Action.NAME));
        setMenuItemAction(menu.getItem(kPasteItem), transferActionListener, new DefaultEditorKit.PasteAction(), (String) TransferHandler.getPasteAction().getValue(Action.NAME));
        menuBar.add(menu);
        menu = guiApp.newWindowsMenu(resBundle.getString("windowsMenuText"));
        menuBar.add(menu);
        AboutJMenuItem about = guiApp.createAboutMenuItem();
        if (!about.isAutomaticallyPresent()) {
            menu = new JMenu(resBundle.getString("helpMenuText"));
            menuBar.add(menu);
            menu.add(about);
        }
        return menuBar;
    }

    /**
	*  Method that will build an "Export" menu containing a list of all the ModelWriter objects that
	*  are available.
	*
	*  @param  title	The title for the menu that is created.
	*  @returns A JMenu instance containing a list of all ModelWriter objects that can write to
	*           files in various formats.
	**/
    private JMenu buildExportMenu(String title) throws NoSuchMethodException {
        ResourceBundle resBundle = _app.getResourceBundle();
        JMenu menu = new JMenu(title);
        GeomReader[] allReaders = GeomReaderFactory.getAllReaders();
        if (allReaders != null) {
            for (GeomReader reader : allReaders) {
                if (reader.canWriteData()) {
                    _geomWriters.add(reader);
                    JMenuItem menuItem = new JMenuItem(reader.toString() + "...");
                    menuItem.addActionListener(new ExportAsMenuListener(reader));
                    menuItem.setActionCommand(reader.toString());
                    menuItem.setEnabled(true);
                    menu.add(menuItem);
                }
            }
        }
        JMenuItem menuItem = new JMenuItem(resBundle.getString("exportAsPNGItemText"));
        menuItem.addActionListener(AppUtilities.getActionListenerForMethod(this, "handleSaveAsPNG"));
        menuItem.setEnabled(true);
        menu.add(menuItem);
        menuItem = new JMenuItem(resBundle.getString("exportAsJPEGItemText"));
        menuItem.addActionListener(AppUtilities.getActionListenerForMethod(this, "handleSaveAsJPEG"));
        menuItem.setEnabled(true);
        menu.add(menuItem);
        return menu;
    }

    /**
	*  Defines an action listener for our Export As menu items.
	**/
    private class ExportAsMenuListener implements ActionListener {

        private final GeomReader _reader;

        public ExportAsMenuListener(GeomReader reader) {
            _reader = reader;
        }

        public void actionPerformed(ActionEvent evt) {
            handleExportPointGeom(_reader);
        }
    }

    /**
	*  Method that sets the specified action to a menu item while preserving the existing,
	*  item's text, accelerator key and mnemonic.
	**/
    private void setMenuItemAction(JMenuItem item, TransferActionListener transferActionListener, Action action, String actionCommand) {
        String text = item.getText();
        KeyStroke accel = item.getAccelerator();
        int mnemonic = item.getMnemonic();
        item.setAction(action);
        item.setText(text);
        item.setAccelerator(accel);
        item.setMnemonic(mnemonic);
        item.setActionCommand(actionCommand);
        item.addActionListener(transferActionListener);
    }

    /**
	*  Create a tool bar for this window.
	**/
    private JToolBar createToolbar() {
        ResourceBundle resBundle = _app.getResourceBundle();
        JToolBar toolbar = new JToolBar();
        URL url = ClassLoader.getSystemResource(resBundle.getString("centerAndZoomIcon"));
        JButton button = new JButton(new ImageIcon(url));
        button.setToolTipText(resBundle.getString("centerandZoomToolTip"));
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                GeomSSScene scene = _canvas.getScene();
                scene.centerAndZoom();
            }
        });
        toolbar.add(button);
        String[] iconFileNames = { resBundle.getString("rightSideViewIcon"), resBundle.getString("leftSideViewIcon"), resBundle.getString("topViewIcon"), resBundle.getString("bottomViewIcon"), resBundle.getString("frontViewIcon"), resBundle.getString("rearViewIcon"), resBundle.getString("topRightFrontViewIcon"), resBundle.getString("topLeftFrontViewIcon"), resBundle.getString("topRightBackViewIcon"), resBundle.getString("topLeftBackViewIcon") };
        ImageIcon[] icons = new ImageIcon[iconFileNames.length];
        for (int i = 0; i < iconFileNames.length; ++i) {
            url = ClassLoader.getSystemResource(iconFileNames[i]);
            icons[i] = new ImageIcon(url);
        }
        JComboBox viewOptions = new ViewAngleComboBox(icons);
        viewOptions.setMaximumSize(viewOptions.getPreferredSize());
        viewOptions.setToolTipText(resBundle.getString("selectViewToolTip"));
        toolbar.add(viewOptions);
        url = ClassLoader.getSystemResource(resBundle.getString("symmetryUnselectedIcon"));
        JToggleButton togButton = new JToggleButton(new ImageIcon(url));
        url = ClassLoader.getSystemResource(resBundle.getString("symmetrySelectedIcon"));
        togButton.setSelectedIcon(new ImageIcon(url));
        togButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                JToggleButton btn = (JToggleButton) evt.getSource();
                GeomSSScene scene = _canvas.getScene();
                scene.setMirrored(btn.isSelected());
            }
        });
        togButton.setToolTipText(resBundle.getString("symmetrySelectToolTip"));
        toolbar.add(togButton);
        url = ClassLoader.getSystemResource(resBundle.getString("projPolicyUnselectedIcon"));
        togButton = new JToggleButton(new ImageIcon(url));
        url = ClassLoader.getSystemResource(resBundle.getString("projPolicySelectedIcon"));
        togButton.setSelectedIcon(new ImageIcon(url));
        togButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                JToggleButton btn = (JToggleButton) evt.getSource();
                GeomSSScene scene = _canvas.getScene();
                scene.setProjectionPolicy((btn.isSelected() ? PARALLEL_PROJECTION : PERSPECTIVE_PROJECTION));
            }
        });
        togButton.setToolTipText(resBundle.getString("projPolicyToolTip"));
        toolbar.add(togButton);
        String[] renderIconNames = { resBundle.getString("solidWireframeIcon"), resBundle.getString("solidIcon"), resBundle.getString("wireframeIcon"), resBundle.getString("stringsOnlyIcon"), resBundle.getString("pointsOnlyIcon") };
        icons = new ImageIcon[renderIconNames.length];
        for (int i = 0; i < renderIconNames.length; ++i) {
            url = ClassLoader.getSystemResource(renderIconNames[i]);
            icons[i] = new ImageIcon(url);
        }
        JComboBox renderOptions = new JComboBox(icons);
        renderOptions.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                JComboBox cb = (JComboBox) evt.getSource();
                RenderType[] renderOptions = RenderType.values();
                int selected = cb.getSelectedIndex();
                GeomSSScene scene = _canvas.getScene();
                scene.setRenderType(renderOptions[selected]);
            }
        });
        renderOptions.setMaximumSize(renderOptions.getPreferredSize());
        renderOptions.setToolTipText(resBundle.getString("pointGeomRenderingToolTip"));
        toolbar.add(renderOptions);
        return toolbar;
    }

    /**
	*  A JComboBox that deselects the list whenever the orientation in the 3D canvas changes.
	**/
    private class ViewAngleComboBox extends JComboBox {

        private GeomSSCanvas3D.PDViewAngle[] _vAngleOptions = GeomSSCanvas3D.PDViewAngle.values();

        private boolean isSelecting = false;

        public ViewAngleComboBox(Object[] items) {
            super(items);
            this.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    int index = getSelectedIndex();
                    if (index >= 0) {
                        isSelecting = true;
                        _canvas.setView(_vAngleOptions[index]);
                        isSelecting = false;
                    }
                }
            });
            _canvas.addTransformChangeListener(new TransformChangeListener() {

                public void transformChanged(TransformChangeEvent e) {
                    if (!isSelecting) {
                        if (e.getType().equals(TransformChangeEvent.Type.ROTATE)) {
                            setSelectedIndex(-1);
                        }
                    }
                }
            });
        }
    }

    /**
	*  Creates, and initializes, a new application window with the specified content.
	*  Effectively creates a new instance of this program.
	*
	*  @param  name        The name of this window (usually file name of data file or "Untitled").
	*  @param  newData     The data set this window contains.  If null is passed,
	*                      an empty/default/new data set is created.
	*  @param  application A reference to the application that created this window.
	**/
    public static MainWindow newAppWindow(String name, GeomElement newData, GeomSS application) throws NoSuchMethodException {
        if (name == null || name.length() < 1) {
            name = application.getResourceBundle().getString("untitled");
            if (_instanceCount > 0) name = name + _instanceCount;
        }
        MainWindow appFrame = new MainWindow(name, (newData != null ? newData : GeomList.newInstance(name)), application);
        if (newData != null) {
            try {
                appFrame._bsh.set("newData", newData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return appFrame;
    }

    /**
	*  Handle the user choosing the "New..." from the File
	*  menu.  Creates a new, blank, document window.
	*  The event object is ignored.
	**/
    public MainWindow handleNew(ActionEvent event) {
        return (MainWindow) (_app.getGUIApplication().handleNew(event));
    }

    /**
	*  Handle the user choosing "Close" from the File menu.  This implementation
	*  dispatches a "Window Closing" event to this window.
	*  The event object is ignored.
	**/
    public void handleClose(ActionEvent event) {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
	*  Handle the user choosing "Open..." from the File menu.
	*  Lets the user choose a data file and open it.
	*  The event object is ignored.
	**/
    public void handleOpen(ActionEvent event) {
        ResourceBundle resBundle = _app.getResourceBundle();
        try {
            DialogItem item = new DialogItem(resBundle.getString("inputDlgListLabel") + " ", resBundle.getString("inputDlgListExample"));
            List<DialogItem> itemList = new ArrayList();
            itemList.add(item);
            item = new DialogItem(resBundle.getString("inputDlgFileLabel") + " ", new File(_app.getPreferences().getLastPath(), resBundle.getString("inputDlgFileExample")));
            item.setLoadFile(true);
            itemList.add(item);
            InputDialog dialog = new InputDialog(this, resBundle.getString("inputDlgTitle"), "", itemList);
            itemList = dialog.getOutput();
            if (itemList == null) return;
            String listName = (String) itemList.get(0).getElement();
            File theFile = (File) itemList.get(1).getElement();
            if (!theFile.exists()) {
                String msg = resBundle.getString("fileDoesntExistMsg").replace("<FILENAME/>", theFile.getName());
                JOptionPane.showMessageDialog(this, msg, resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            String cwd = (String) _bsh.get("bsh.cwd");
            String pathName;
            if (theFile.getParent().equals(cwd)) pathName = theFile.getName(); else pathName = theFile.getPath();
            if (AppUtilities.isWindows()) pathName = pathName.replace("\\", "/");
            String command = listName + " = readGeomFile(pathToFile(\"" + pathName + "\"));";
            _bsh.eval(command);
            _bsh.println(command);
            command = "draw(" + listName + ");";
            _bsh.eval(command);
            _bsh.println(command);
            command = "centerAndZoom();";
            _bsh.eval(command);
            _bsh.println(command);
        } catch (EvalError e) {
            e.printStackTrace();
        } catch (Exception e) {
            AppUtilities.showException(null, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
            e.printStackTrace();
        }
    }

    /**
	*  Method that reads in the specified file and creates a new window for displaying
	*  it's contents.
	*
	*  @param  app      A reference to the application that created this window.
	*  @param  parent   Parent frame for dialogs (null is fine).
	*  @param  theFile  The file to be loaded and displayed.  If null is passed, this
	*                   method will do nothing.
	**/
    public static void newWindowFromDataFile(GeomSS app, Frame parent, File theFile) throws NoSuchMethodException {
        GeometryList newData = readGeometryData(app.getResourceBundle(), parent, theFile);
        if (newData != null) {
            MainWindow window = newAppWindow(theFile.getName(), newData, app);
            window._canvas.getScene().centerAndZoom();
            window.setVisible(true);
            app.getGUIApplication().addWindow(window);
        }
    }

    /**
	*  Method that reads in a data file and return the resulting data structure.
	*
	*  @param resBundle The main application's resource bundle.
	*  @param parent    The parent frame for dialogs (null is fine).
	*  @param theFile   The file to be read in.  If <code>null</code> is passed, this
	*                   method will do nothing.
	**/
    private static GeometryList readGeometryData(ResourceBundle resBundle, Component parent, File theFile) {
        GeometryList data = null;
        try {
            data = GeomSSBatch.readGeometryData(resBundle, parent, theFile);
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null && e.getCause() != null) msg = e.getCause().getMessage();
            JOptionPane.showMessageDialog(parent, msg, resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            AppUtilities.showException(null, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
            e.printStackTrace();
        }
        return data;
    }

    /**
	*  Handle the user choosing "Source Script..." from the File menu.
	*  Lets the user choose a script file and source it.
	*  The event object is ignored.
	**/
    public void handleSrcScript(ActionEvent event) {
        ResourceBundle resBundle = _app.getResourceBundle();
        try {
            ExtFilenameFilter fnFilter = new ExtFilenameFilter();
            fnFilter.addExtension("bsh");
            String dir = _app.getPreferences().getLastPath();
            File theFile = AppUtilities.selectFile(this, FileDialog.LOAD, resBundle.getString("fileDialogLoad"), dir, null, fnFilter);
            if (theFile == null) return;
            String cwd = (String) _bsh.get("bsh.cwd");
            String command = null;
            if (theFile.getParent().equals(cwd)) command = "source(\"" + theFile.getName() + "\");"; else command = "source(\"" + theFile.getPath() + "\");";
            if (AppUtilities.isWindows()) command = command.replace("\\", "/");
            _bsh.println(command);
            _bsh.eval(command);
        } catch (Exception e) {
            _bsh.error(e);
            e.printStackTrace();
        }
    }

    /**
	*  Handle the user choosing "Save" from the File menu.  This saves
	*  the model to the last file used.  Catches and displays to
	*  the user all exceptions.  The event object is ignored.
	**/
    public void handleSave(ActionEvent event) {
        try {
            doSave();
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null) {
                Throwable cause = e.getCause();
                if (cause != null) msg = cause.getMessage();
            }
            ResourceBundle resBundle = _app.getResourceBundle();
            JOptionPane.showMessageDialog(this, msg, resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            ResourceBundle resBundle = _app.getResourceBundle();
            AppUtilities.showException(this, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
            e.printStackTrace();
        }
    }

    /**
	*  Handle the user choosing "Save" from the File menu.  This saves
	*  the model to the last file used.  All exceptions are thrown.
	*
	*  @return true if the user cancels, false if the model was saved.
	**/
    private boolean doSave() throws IOException {
        return false;
    }

    /**
	*  Handle the user choosing "Save As" from the File menu.  This asks
	*  the user for input and then saves the model to a file.
	*  The event object is ignored.
	**/
    public void handleSaveAs(ActionEvent event) {
        try {
            doSaveAs();
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null) {
                Throwable cause = e.getCause();
                if (cause != null) msg = cause.getMessage();
            }
            ResourceBundle resBundle = _app.getResourceBundle();
            JOptionPane.showMessageDialog(this, msg, resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            ResourceBundle resBundle = _app.getResourceBundle();
            AppUtilities.showException(this, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
            e.printStackTrace();
        }
    }

    /**
	*  Handle the user choosing "Save As" from the File menu.  This asks
	*  the user for input and then saves the model to a file.  This
	*  method throws all exceptions.
	*
	*  @return true if the user cancels, false if the model was saved.
	**/
    private boolean doSaveAs() throws IOException {
        return true;
    }

    /**
	*  Method called when the window is about to be closed to determine if it should be closed.
	*  For example, if there are unsaved changes, the user may choose to ignore them (and close the window),
	*  save them (and close the window), or cancel (and keep the window open).
	*  Returns true if the window should be allowed to close, or false if closing should
	*  be canceled.
	**/
    private boolean windowShouldClose() {
        return true;
    }

    /**
	*  Handle the user choosing "Save As PNG..." from the File menu.  This asks
	*  the user for input and then saves the image to a file.
	*  The event object is ignored.
	**/
    public void handleSaveAsPNG(ActionEvent event) {
        String extension = "png";
        String dir = _app.getPreferences().getLastPath();
        String fileName = this.getTitle() + "." + extension;
        ExtFilenameFilter fnFilter = new ExtFilenameFilter(extension);
        File theFile = selectFile4Save(extension, fnFilter, dir, fileName);
        if (canWriteFile(theFile)) {
            _PNGObserver.setFilename(theFile.getPath());
            _PNGObserver.setCaptureNextFrame();
            _canvas.getView().repaint();
        }
    }

    /**
	*  Handle the user choosing "Save As JPEG..." from the File menu.  This asks
	*  the user for input and then saves the image to a file.
	*  The event object is ignored.
	**/
    public void handleSaveAsJPEG(ActionEvent event) {
        String extension = "jpeg";
        String dir = _app.getPreferences().getLastPath();
        String fileName = this.getTitle() + "." + extension;
        ExtFilenameFilter fnFilter = new ExtFilenameFilter(extension);
        fnFilter.addExtension("jpg");
        File theFile = selectFile4Save(extension, fnFilter, dir, fileName);
        if (canWriteFile(theFile)) {
            _JPEGObserver.setFilename(theFile.getPath());
            _JPEGObserver.setCaptureNextFrame();
            _canvas.getView().repaint();
        }
    }

    /**
	*  Asks the user to select a file for saving.
	*
	*  @param fileType  The file extension for the file being saved (example: "png" for a PNG file).
	*  @param fnFilter  The file name filter to use.
	*  @param dir       The name of the directory path to prompt the user with (the default path).
	*  @param fileName  The name of the file to prompt the user with (the default file).
	*  @return A refernece to the selected file or <code>null</code> for no file selected
	*          (user canceled or error).  Exceptions are handled in this method.
	**/
    private File selectFile4Save(String extension, ExtFilenameFilter fnFilter, String dir, String fileName) {
        ResourceBundle resBundle = _app.getResourceBundle();
        try {
            String msg = resBundle.getString("fileSaveDialog").replace("<TYPE/>", extension.toUpperCase());
            File theFile = AppUtilities.selectFile4Save(this, msg, dir, fileName, fnFilter, extension, resBundle.getString("fileExists1"), resBundle.getString("fileExists2"));
            if (theFile != null) {
                if (theFile.exists() && !theFile.canWrite()) throw new IOException(resBundle.getString("canNotWrite2File"));
                return theFile;
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null) {
                Throwable cause = e.getCause();
                if (cause != null) msg = cause.getMessage();
            }
            JOptionPane.showMessageDialog(this, msg, resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            AppUtilities.showException(this, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
        }
        return null;
    }

    /**
	*  Displays a message to the user if a file exists but can not be written to.
	*
	*  @param theFile  The file to test.
	*  @return <code>true</code> if the program can write to the file or <code>false</code> if
	*          theFile is <code>null</code> or the file exists but can not be written to.
	**/
    public boolean canWriteFile(File theFile) {
        if (theFile == null) return false;
        if (!theFile.exists() || theFile.canWrite()) return true;
        ResourceBundle resBundle = _app.getResourceBundle();
        JOptionPane.showMessageDialog(this, resBundle.getString("canNotWrite2File"), resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
	*  Handle the user choosing to export to a GeomReader.
	*  Lets the user choose a data file and open it.
	**/
    public void handleExportPointGeom(GeomReader reader) {
        ResourceBundle resBundle = _app.getResourceBundle();
        try {
            DialogItem item = new DialogItem(resBundle.getString("exptDlgListLabel") + " ", resBundle.getString("exptDlgListExample"));
            List<DialogItem> itemList = new ArrayList();
            itemList.add(item);
            item = new DialogItem(reader.toString() + " " + resBundle.getString("exptDlgSaveLabel") + " ", new File(_app.getPreferences().getLastPath(), resBundle.getString("inputDlgFileExample")));
            item.setLoadFile(false);
            item.setFileExtension(reader.getExtension());
            itemList.add(item);
            InputDialog dialog = new InputDialog(this, resBundle.getString("inputDlgTitle"), "", itemList);
            itemList = dialog.getOutput();
            if (itemList == null) return;
            String listName = (String) itemList.get(0).getElement();
            File theFile = (File) itemList.get(1).getElement();
            if (canWriteFile(theFile)) {
                String cwd = (String) _bsh.get("bsh.cwd");
                String writerName = reader.getClass().toString();
                writerName = writerName.substring(writerName.lastIndexOf(".") + 1);
                String pathName;
                if (theFile.getParent().equals(cwd)) pathName = theFile.getName(); else pathName = theFile.getPath();
                if (AppUtilities.isWindows()) pathName = pathName.replace("\\", "/");
                String command = "writeGeomFile(" + listName + ", pathToFile(\"" + pathName + "\"), new " + writerName + "());";
                _bsh.eval(command);
                _bsh.println(command);
            }
        } catch (EvalError e) {
            e.printStackTrace();
            AppUtilities.showException(this, resBundle.getString("evalErrTitle"), resBundle.getString("unexpectedMsg"), e);
        } catch (Exception e) {
            e.printStackTrace();
            AppUtilities.showException(null, resBundle.getString("unexpectedTitle"), resBundle.getString("unexpectedMsg"), e);
        }
    }

    /**
	*  Handle the user choosing "Change Working Directory..." from the File menu.  Displays a
	*  File selection dialog allowing the user to select the working directory.
	**/
    public void handleChangeCWD(ActionEvent event) {
        ResourceBundle resBundle = _app.getResourceBundle();
        FolderDialog fd = new FolderDialog(this, resBundle.getString("selectNewCWDText"));
        fd.setDirectory(_app.getPreferences().getLastPath());
        fd.show();
        if (fd.getFile() != null) {
            String newDir = fd.getDirectory();
            if (AppUtilities.isWindows()) {
                newDir = newDir.replace("\\", "/");
            }
            try {
                String command = "cd(\"" + newDir + "\");";
                _bsh.eval(command);
                _bsh.println(command);
                _app.getPreferences().setLastPath(newDir);
            } catch (EvalError e) {
                String msg = e.getMessage();
                JOptionPane.showMessageDialog(this, msg, resBundle.getString("evalErrTitle"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
	*  Handle the user choosing "Page Setup..." from the File menu.  Displays a
	*  Page Setup dialog allowing the user to change the page settings.
	*  The event object is ignored.
	**/
    public void handlePageSetup(ActionEvent event) {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (_pf == null) _pf = job.defaultPage();
        _pf = job.pageDialog(_pf);
    }

    /**
	*  Handle the user choosing "Print" from the File menu.
	*  Prints the currently displayed plot.
	*  The event object is ignored.
	**/
    public void handlePrint(ActionEvent event) {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (_pf == null) _pf = job.defaultPage();
        job.setPrintable(_canvas, _pf);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, e);
            }
        }
    }

    /**
	*  A class that serves as the public interface (in BeanShell) for this
	*  application.
	**/
    private class PublicInterface implements GeomSSApp {

        /**
		*  Return a reference to the 3D scene.
		**/
        public GeomSSScene getScene() {
            return _canvas.getScene();
        }

        /**
		*  Return the parent Frame for this application.
		**/
        public Frame getParentFrame() {
            return MainWindow.this;
        }

        /**
		*  Quit or exit the application after properly saving preferences, etc.
		**/
        public void quit() {
            try {
                String cwd = (String) _bsh.get("bsh.cwd");
                _app.getPreferences().setLastPath(cwd);
            } catch (Exception e) {
                e.printStackTrace();
            }
            _app.getGUIApplication().handleQuit(null);
        }

        /**
		*  Read in a geometry file and return a GeometryList instance.  All exceptions
		*  are handled by this method.
		*
		*  @param theFile   The file to be read in.  If <code>null</code> is passed, this
		*                   method will do nothing.
		*  @return A GeometryList object containing the geometry read in from the file or
		*          <code>null</code> if the user cancels the read at any point or if an
		*          exception of any kind is thrown.
		**/
        public GeometryList readGeomFile(File theFile) {
            return readGeometryData(_app.getResourceBundle(), MainWindow.this, theFile);
        }

        /**
		*  Write out geometry to the specified file using the specified
		*  GeometryList instance (some GeomReader classes have specific requirements for
		*  the contents of this list).  All excpetions are handled by this method.
		*
		*  @param geometry The geometry object to be written out.
		*  @param theFile  The file to be written to.
		*  @param writer   The GeomReader to use to write out the file.
		**/
        public void writeGeomFile(GeometryList geometry, File theFile, GeomReader writer) {
            if (canWriteFile(theFile)) {
                try {
                    writer.write(theFile, geometry);
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (msg == null && e.getCause() != null) msg = e.getCause().getMessage();
                    ResourceBundle resBundle = _app.getResourceBundle();
                    JOptionPane.showMessageDialog(MainWindow.this, msg, resBundle.getString("ioErrorTitle"), JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }

        /**
		*  Returns a list of all known GeomReader objects that are capable of writing to
		*  a file.
		**/
        public List<GeomReader> getAllGeomWriters() {
            return _geomWriters;
        }

        /**
		*  Save a copy of the current 3D view as a PNG file.
		**/
        public void saveAsPNG() {
            handleSaveAsPNG(null);
        }

        /**
		*  Save a copy of the current 3D view as a PNG file.
		*
		*  @param file  The file to be saved.
		**/
        public void saveAsPNG(File file) {
            if (canWriteFile(file)) {
                _PNGObserver.setFilename(file.getPath());
                _PNGObserver.setCaptureNextFrame();
                _canvas.getView().repaint();
            }
        }

        /**
		*  Save a copy of the current 3D view as a JPEG file.
		**/
        public void saveAsJPEG() {
            handleSaveAsJPEG(null);
        }

        /**
		*  Save a copy of the current 3D view as a JPEG file.
		*
		*  @param file  The file to be saved.
		**/
        public void saveAsJPEG(File file) {
            if (canWriteFile(file)) {
                _JPEGObserver.setFilename(file.getPath());
                _JPEGObserver.setCaptureNextFrame();
                _canvas.getView().repaint();
            }
        }

        /**
		*  Print the current 3D view.  The user will be
		*  asked to supply information on the print settings.
		**/
        public void print() {
            handlePrint(null);
        }
    }
}
