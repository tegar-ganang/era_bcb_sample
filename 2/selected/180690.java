package com.horstmann.violet.framework;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.PropertyVetoException;
import java.beans.Statement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import com.horstmann.violet.ArrowHead;
import com.horstmann.violet.BentStyle;
import com.horstmann.violet.JMatterClassDiagramGraph;
import com.horstmann.violet.JMatterClassNode;
import com.horstmann.violet.LineStyle;
import com.u2d.umlc.deployment.DeploymentInformation;
import com.u2d.umlc.deployment.JMatterProjectControlForm;
import umlc.codeGeneration.GeneratorIF;
import umlc.codeGeneration.umlc.UmlcFromVioletGenerator;
import umlc.codeGeneration.umlc.VioletFromUmlc;

/**
   This desktop frame contains panes that show graphs.
*/
public class EditorFrame extends JFrame {

    protected static EditorFrame mainFrame;

    /**
      Constructs a blank frame with a desktop pane
      but no graph windows.
      @param appClassName the fully qualified app class name.
      It is expected that the resources are appClassName + "Strings"
      and appClassName + "Version" (the latter for version-specific
      resources)
   */
    public EditorFrame(Class appClass) {
        mainFrame = this;
        String appClassName = appClass.getName();
        appResources = ResourceBundle.getBundle(appClassName + "Strings");
        appFactory = new ResourceFactory(appResources);
        versionResources = ResourceBundle.getBundle(appClassName + "Version");
        editorResources = ResourceBundle.getBundle("com.horstmann.violet.framework.EditorStrings");
        ResourceFactory factory = new ResourceFactory(editorResources);
        preferences = PreferencesService.getInstance(appClass);
        this.icon = new ImageIcon(EditorFrame.class.getClassLoader().getResource("uv.png"));
        this.setIconImage((icon).getImage());
        recentFiles = new ArrayList();
        File lastDir = new File(".");
        String recent = preferences.get("recent", "").trim();
        if (recent.length() > 0) {
            recentFiles.addAll(Arrays.asList(recent.split("[|]")));
            lastDir = new File((String) recentFiles.get(0)).getParentFile();
        }
        fileService = FileService.getInstance(lastDir);
        setTitle(appResources.getString("app.name"));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        setBounds(screenWidth / 16, screenHeight / 16, screenWidth * 7 / 8, screenHeight * 7 / 8);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent event) {
                exit();
            }
        });
        desktop = new JDesktopPane();
        setContentPane(desktop);
        defaultExtension = appResources.getString("files.extension");
        umlcExtension = appResources.getString("umlc.extension");
        violetFilter = new ExtensionFilter(appResources.getString("files.name"), new String[] { defaultExtension, umlcExtension });
        exportFilter = new ExtensionFilter(editorResources.getString("files.image.name"), editorResources.getString("files.image.extension"));
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = factory.createMenu("file");
        menuBar.add(fileMenu);
        newMenu = factory.createMenu("file.new");
        fileMenu.add(newMenu);
        JMenuItem fileOpenItem = factory.createMenuItem("file.open", this, "openFile");
        fileMenu.add(fileOpenItem);
        recentFilesMenu = factory.createMenu("file.recent");
        buildRecentFilesMenu();
        fileMenu.add(recentFilesMenu);
        JMenuItem fileSaveItem = factory.createMenuItem("file.save", this, "save");
        fileMenu.add(fileSaveItem);
        JMenuItem fileSaveAsItem = factory.createMenuItem("file.save_as", this, "saveAs");
        fileMenu.add(fileSaveAsItem);
        JMenuItem fileExportItem = factory.createMenuItem("file.export_image", this, "exportImage");
        fileMenu.add(fileExportItem);
        JMenuItem filePrintItem = factory.createMenuItem("file.print", this, "print");
        fileMenu.add(filePrintItem);
        JMenuItem fileExitItem = factory.createMenuItem("file.exit", this, "exit");
        fileMenu.add(fileExitItem);
        if (fileService == null) {
            fileOpenItem.setEnabled(false);
            fileSaveAsItem.setEnabled(false);
            fileExportItem.setEnabled(false);
            filePrintItem.setEnabled(false);
            fileExitItem.setEnabled(false);
        }
        if (fileService == null || fileService.isWebStart()) {
            recentFilesMenu.setEnabled(false);
            fileSaveItem.setEnabled(false);
        }
        JMenu editMenu = factory.createMenu("edit");
        menuBar.add(editMenu);
        editMenu.add(factory.createMenuItem("edit.properties", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                final GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.editSelected();
            }
        }));
        editMenu.add(factory.createMenuItem("edit.delete", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.removeSelected();
            }
        }));
        editMenu.add(factory.createMenuItem("edit.select_next", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.selectNext(1);
            }
        }));
        editMenu.add(factory.createMenuItem("edit.select_previous", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                Graph graph = frame.getGraph();
                GraphPanel panel = frame.getGraphPanel();
                panel.selectNext(-1);
            }
        }));
        JMenu jmatter = factory.createMenu("jmatter");
        menuBar.add(jmatter);
        jmatter.add(factory.createMenuItem("jmatter.baseDir", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                selectJMatterDir();
            }
        }));
        jmatter.add(factory.createMenuItem("jmatter.control", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                Graph graph = frame.getGraph();
                if (!(graph instanceof JMatterClassDiagramGraph)) return;
                String fileName = frame.getFileName();
                if ((fileName == null) || (frame.getGraphPanel().isModified())) {
                    showPleaseSaveDialog();
                    return;
                }
                showJMatterDialog(fileName);
            }
        }));
        JMenu viewMenu = factory.createMenu("view");
        menuBar.add(viewMenu);
        viewMenu.add(factory.createMenuItem("view.zoom_out", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.changeZoom(-1);
            }
        }));
        viewMenu.add(factory.createMenuItem("view.zoom_in", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.changeZoom(1);
            }
        }));
        viewMenu.add(factory.createMenuItem("view.grow_drawing_area", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                Graph g = frame.getGraph();
                Rectangle2D bounds = g.getBounds((Graphics2D) frame.getGraphics());
                bounds.add(frame.getGraphPanel().getBounds());
                g.setMinBounds(new Rectangle2D.Double(0, 0, GROW_SCALE_FACTOR * bounds.getWidth(), GROW_SCALE_FACTOR * bounds.getHeight()));
                frame.getGraphPanel().revalidate();
                frame.repaint();
            }
        }));
        viewMenu.add(factory.createMenuItem("view.clip_drawing_area", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                Graph g = frame.getGraph();
                Rectangle2D bounds = g.getBounds((Graphics2D) frame.getGraphics());
                g.setMinBounds(null);
                frame.getGraphPanel().revalidate();
                frame.repaint();
            }
        }));
        viewMenu.add(factory.createMenuItem("view.smaller_grid", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.changeGridSize(-1);
            }
        }));
        viewMenu.add(factory.createMenuItem("view.larger_grid", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                panel.changeGridSize(1);
            }
        }));
        final JCheckBoxMenuItem hideGridItem;
        viewMenu.add(hideGridItem = (JCheckBoxMenuItem) factory.createCheckBoxMenuItem("view.hide_grid", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) event.getSource();
                panel.setHideGrid(menuItem.isSelected());
            }
        }));
        viewMenu.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                GraphPanel panel = frame.getGraphPanel();
                hideGridItem.setSelected(panel.getHideGrid());
            }

            public void menuDeselected(MenuEvent event) {
            }

            public void menuCanceled(MenuEvent event) {
            }
        });
        JMenu lafMenu = factory.createMenu("view.change_laf");
        viewMenu.add(lafMenu);
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < infos.length; i++) {
            final UIManager.LookAndFeelInfo info = infos[i];
            JMenuItem item = new JMenuItem(info.getName());
            lafMenu.add(item);
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    String laf = info.getClassName();
                    changeLookAndFeel(laf);
                    preferences.put("laf", laf);
                }
            });
        }
        JMenu windowMenu = factory.createMenu("window");
        menuBar.add(windowMenu);
        windowMenu.add(factory.createMenuItem("window.next", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                JInternalFrame[] frames = desktop.getAllFrames();
                for (int i = 0; i < frames.length; i++) {
                    if (frames[i] == desktop.getSelectedFrame()) {
                        i++;
                        if (i == frames.length) i = 0;
                        try {
                            frames[i].toFront();
                            frames[i].setSelected(true);
                        } catch (PropertyVetoException exception) {
                        }
                        return;
                    }
                }
            }
        }));
        windowMenu.add(factory.createMenuItem("window.previous", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                JInternalFrame[] frames = desktop.getAllFrames();
                for (int i = 0; i < frames.length; i++) {
                    if (frames[i] == desktop.getSelectedFrame()) {
                        if (i == 0) i = frames.length;
                        i--;
                        try {
                            frames[i].toFront();
                            frames[i].setSelected(true);
                        } catch (PropertyVetoException exception) {
                        }
                        return;
                    }
                }
            }
        }));
        windowMenu.add(factory.createMenuItem("window.maximize", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                try {
                    frame.setMaximum(true);
                } catch (PropertyVetoException exception) {
                }
            }
        }));
        windowMenu.add(factory.createMenuItem("window.restore", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                try {
                    frame.setMaximum(false);
                } catch (PropertyVetoException exception) {
                }
            }
        }));
        windowMenu.add(factory.createMenuItem("window.close", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
                if (frame == null) return;
                try {
                    frame.setClosed(true);
                } catch (PropertyVetoException exception) {
                }
            }
        }));
        JMenu helpMenu = factory.createMenu("help");
        menuBar.add(helpMenu);
        helpMenu.add(factory.createMenuItem("help.about", this, "showAboutDialog"));
        helpMenu.add(factory.createMenuItem("help.license", new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("license.txt")));
                    JTextArea text = new JTextArea(10, 50);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        text.append(line);
                        text.append("\n");
                    }
                    text.setCaretPosition(0);
                    text.setEditable(false);
                    JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), new JScrollPane(text), null, JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException exception) {
                }
            }
        }));
    }

    public static EditorFrame getMainFrame() {
        return mainFrame;
    }

    private void selectJMatterDir() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showDialog(this, "Select JMatter Directory");
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            File jmatterDir = chooser.getSelectedFile();
            this.preferences.put("jmatter_dir", jmatterDir.getAbsolutePath());
        }
    }

    private void showPleaseSaveDialog() {
        JOptionPane.showMessageDialog(this, "The graph has been modified, please save first.", "Please Save Graph...", JOptionPane.ERROR_MESSAGE);
    }

    private void showJMatterDialog(String umlFile) {
        String jmatter_dir = this.preferences.get("jmatter_dir", "");
        if ("".equals(jmatter_dir)) {
            JOptionPane.showMessageDialog(this, "Please set the JMatter directory.", "Please Find JMatter...", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String projectName = new File(umlFile).getName().replaceAll(".uml", "");
        DeploymentInformation info = new DeploymentInformation();
        info.setJmatterDirectory(jmatter_dir);
        info.setProjectName(projectName);
        info.setUmlFileLocation(umlFile);
        JOptionPane.showMessageDialog(this, new JMatterProjectControlForm(info), "JMatter Project Control", JOptionPane.PLAIN_MESSAGE);
    }

    /**
    * Changes the look and feel
    * @param lafName the name of the new look and feel
    */
    private void changeLookAndFeel(String lafName) {
        try {
            UIManager.setLookAndFeel(lafName);
            SwingUtilities.updateComponentTreeUI(EditorFrame.this);
        } catch (ClassNotFoundException ex) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (UnsupportedLookAndFeelException ex) {
        }
    }

    private void generateCode(JMatterClassDiagramGraph jg) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
        int returnVal = chooser.showDialog(this, "Generate code.");
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            File sourceFile = chooser.getSelectedFile();
            UmlcFromVioletGenerator generator = new UmlcFromVioletGenerator(jg);
            generator.generate(sourceFile);
        } else System.err.println("A file needs to be specified. ");
    }

    /**
      Adds a graph type to the File->New menu.
      @param resourceName the name of the menu item resource
      @param graphClass the class object for the graph
   */
    public void addGraphType(String resourceName, final Class graphClass) {
        newMenu.add(appFactory.createMenuItem(resourceName, new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                try {
                    GraphFrame frame = new GraphFrame((Graph) graphClass.newInstance());
                    frame.setGenerator(generator);
                    addInternalFrame(frame);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }));
    }

    /**
      Reads the command line arguments.
      @param args the command line arguments
   */
    public void readArgs(String[] args) {
        if (args.length == 0) showAboutDialog(); else {
            for (int i = 0; i < args.length; i++) {
                open(args[i]);
            }
        }
        setTitle();
    }

    /**
    * Opens a file with the given name, or switches to the frame if it is already open.
    * @param name the file name
    */
    private void open(String name) {
        JInternalFrame[] frames = desktop.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] instanceof GraphFrame) {
                GraphFrame frame = (GraphFrame) frames[i];
                if (frame.getFileName().equals(name)) {
                    try {
                        frame.toFront();
                        frame.setSelected(true);
                    } catch (PropertyVetoException exception) {
                    }
                    return;
                }
            }
        }
        try {
            Graph graph = read(new FileInputStream(name));
            GraphFrame frame = new GraphFrame(graph);
            frame.setGenerator(this.generator);
            addInternalFrame(frame);
            frame.setFileName(name);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), exception);
        }
    }

    /**
      Creates an internal frame on the desktop.
      @param c the component to display in the internal frame
      @param t the title of the internal frame.
   */
    private void addInternalFrame(final JInternalFrame iframe) {
        iframe.setResizable(true);
        iframe.setClosable(true);
        iframe.setMaximizable(true);
        iframe.setIconifiable(true);
        int frameCount = desktop.getAllFrames().length;
        desktop.add(iframe);
        int emptySpace = FRAME_GAP * Math.max(ESTIMATED_FRAMES, frameCount);
        int width = Math.max(desktop.getWidth() / 2, desktop.getWidth() - emptySpace);
        int height = Math.max(desktop.getHeight() / 2, desktop.getHeight() - emptySpace);
        iframe.reshape(frameCount * FRAME_GAP, frameCount * FRAME_GAP, width, height);
        iframe.show();
        iframe.addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameActivated(InternalFrameEvent event) {
                setTitle();
            }

            public void internalFrameDeactivated(InternalFrameEvent event) {
                setTitle();
            }
        });
        try {
            iframe.setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    /**
      Sets the frame title.
   */
    private void setTitle() {
        String appName = appResources.getString("app.name");
        GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
        if (frame == null) setTitle(appName); else {
            String fileName = frame.getFileName();
            if (fileName == null) setTitle(appName); else setTitle(appName + " - " + fileName);
        }
    }

    /**
    * Adds a file name to the "recent files" list and rebuilds the "recent files" menu. 
    * @param newFile the file name to add
    */
    private void addRecentFile(final String newFile) {
        recentFiles.remove(newFile);
        if (newFile == null || newFile.equals("")) return;
        recentFiles.add(0, newFile);
        buildRecentFilesMenu();
    }

    /**
    * Rebuilds the "recent files" menu.
    */
    private void buildRecentFilesMenu() {
        recentFilesMenu.removeAll();
        for (int i = 0; i < recentFiles.size(); i++) {
            final String file = (String) recentFiles.get(i);
            String name = new File(file).getName();
            if (i < 10) name = i + " " + name; else if (i == 10) name = "0 " + name;
            JMenuItem item = new JMenuItem(name);
            if (i < 10) item.setMnemonic((char) ('0' + i)); else if (i == 10) item.setMnemonic('0');
            recentFilesMenu.add(item);
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    open(file);
                }
            });
        }
    }

    /**
      Asks the user to open a graph file.
   */
    public void openFile() {
        try {
            FileService.Open open = fileService.open(null, null, violetFilter);
            boolean isUml = false;
            if (open.getName().endsWith(".uml")) isUml = true;
            InputStream in = open.getInputStream();
            if (in != null) {
                Graph graph = null;
                if (!isUml) graph = read(in); else {
                    VioletFromUmlc c = new VioletFromUmlc(generator);
                    try {
                        graph = c.generateGraph(open.getInputStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
                GraphFrame frame = new GraphFrame(graph);
                frame.setGenerator(this.generator);
                addInternalFrame(frame);
                frame.setFileName(open.getName());
                addRecentFile(open.getName());
                setTitle();
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), exception);
        }
    }

    /**
    * Open a file from an URL--used by applet
    * @param url the URL 
    */
    public void openURL(URL url) throws IOException {
        InputStream in = url.openStream();
        if (in != null) {
            Graph graph = read(in);
            GraphFrame frame = new GraphFrame(graph);
            frame.setGenerator(this.generator);
            addInternalFrame(frame);
            try {
                frame.setMaximum(true);
            } catch (PropertyVetoException ex) {
            }
        }
    }

    public void save() {
        GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
        if (frame == null) return;
        String fileName = frame.getFileName();
        if (fileName == null) {
            saveAs();
            return;
        }
        try {
            if (frame.getGraph() instanceof JMatterClassDiagramGraph) {
                JMatterClassDiagramGraph jg = (JMatterClassDiagramGraph) frame.getGraph();
                UmlcFromVioletGenerator generator = new UmlcFromVioletGenerator(jg);
                generator.generate(new File(fileName));
            } else {
                saveFile(frame.getGraph(), new FileOutputStream(fileName));
            }
            frame.getGraphPanel().setModified(false);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), exception);
        }
    }

    /**
      Saves the current graph as a new file.
   */
    public void saveAs() {
        GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
        if (frame == null) return;
        Graph graph = frame.getGraph();
        try {
            String extension = defaultExtension;
            if (frame.getGraph() instanceof JMatterClassDiagramGraph) extension = this.umlcExtension;
            FileService.Save save = fileService.save(null, frame.getFileName(), violetFilter, null, extension);
            OutputStream out = save.getOutputStream();
            if (out != null) {
                try {
                    if (frame.getGraph() instanceof JMatterClassDiagramGraph) {
                        JMatterClassDiagramGraph jg = (JMatterClassDiagramGraph) frame.getGraph();
                        UmlcFromVioletGenerator generator = new UmlcFromVioletGenerator(jg);
                        generator.generate(new File(save.getName()));
                    } else saveFile(graph, out);
                } finally {
                    out.close();
                }
                frame.setFileName(save.getName());
                setTitle();
                frame.getGraphPanel().setModified(false);
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), exception);
        }
    }

    /**
      Exports the current graph to an image file.
   */
    public void exportImage() {
        GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
        if (frame == null) return;
        try {
            String imageExtensions = editorResources.getString("files.image.extension");
            FileService.Save save = fileService.save(null, frame.getFileName(), exportFilter, defaultExtension, imageExtensions);
            OutputStream out = save.getOutputStream();
            if (out != null) {
                String format;
                String fileName = save.getName();
                if (fileName == null) {
                    int n = imageExtensions.indexOf("|");
                    if (n < 0) n = imageExtensions.length();
                    format = imageExtensions.substring(1, n);
                } else format = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (!ImageIO.getImageWritersByFormatName(format).hasNext()) {
                    MessageFormat formatter = new MessageFormat(editorResources.getString("error.unsupported_image"));
                    JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), formatter.format(new Object[] { format }));
                    return;
                }
                Graph graph = frame.getGraph();
                try {
                    saveImage(graph, out, format);
                } finally {
                    out.close();
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), exception);
        }
    }

    /**
      Prints the current graph.
   */
    public void print() {
        GraphFrame frame = (GraphFrame) desktop.getSelectedFrame();
        if (frame == null) return;
        PrintDialog dialog = new PrintDialog(frame.getGraph());
        dialog.setVisible(true);
    }

    /**
      Reads a graph file
      @param in the input stream to read
      @return the graph that is read in
   */
    public Graph read(InputStream in) throws IOException {
        VioletFromUmlc c = new VioletFromUmlc(generator);
        try {
            Graph graph = c.generateGraph(in);
            return graph;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            in.close();
        }
    }

    /**
      Saves the current graph in a file. We use long-term
      bean persistence to save the program data. See
      http://java.sun.com/products/jfc/tsc/articles/persistence4/index.html
      for an overview.
      @param out the stream for saving
   */
    private static void saveFile(Graph graph, OutputStream out) {
        XMLEncoder encoder = new XMLEncoder(out);
        encoder.setExceptionListener(new ExceptionListener() {

            public void exceptionThrown(Exception ex) {
                ex.printStackTrace();
            }
        });
        encoder.setPersistenceDelegate(Point2D.Double.class, new DefaultPersistenceDelegate() {

            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                super.initialize(type, oldInstance, newInstance, out);
                Point2D p = (Point2D) oldInstance;
                out.writeStatement(new Statement(oldInstance, "setLocation", new Object[] { new Double(p.getX()), new Double(p.getY()) }));
            }
        });
        encoder.setPersistenceDelegate(BentStyle.class, staticFieldDelegate);
        encoder.setPersistenceDelegate(LineStyle.class, staticFieldDelegate);
        encoder.setPersistenceDelegate(ArrowHead.class, staticFieldDelegate);
        Graph.setPersistenceDelegate(encoder);
        AbstractNode.setPersistenceDelegate(encoder);
        encoder.writeObject(graph);
        encoder.close();
    }

    /**
      Exports a current graph to an image file.
      @param graph the graph
      @param out the output stream
      @param format the image file format
   */
    public static void saveImage(Graph graph, OutputStream out, String format) throws IOException {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Rectangle2D bounds = graph.getBounds((Graphics2D) dummy.getGraphics());
        BufferedImage image = new BufferedImage((int) bounds.getWidth() + 1, (int) bounds.getHeight() + 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        g2.translate(-bounds.getX(), -bounds.getY());
        g2.setColor(Color.WHITE);
        g2.fill(new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth() + 1, bounds.getHeight() + 1));
        g2.setColor(Color.BLACK);
        g2.setBackground(Color.WHITE);
        graph.draw(g2, null);
        ImageIO.write(image, format, out);
    }

    /**
      Displays the About dialog box.
   */
    public void showAboutDialog() {
        MessageFormat formatter = new MessageFormat(editorResources.getString("dialog.about.version"));
        JOptionPane.showMessageDialog(EditorFrame.getMainFrame(), formatter.format(new Object[] { appResources.getString("app.name"), versionResources.getString("version.number"), versionResources.getString("version.date"), appResources.getString("app.copyright"), editorResources.getString("dialog.about.license") }), "About", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource(appResources.getString("app.icon"))));
    }

    /**
      Exits the program if no graphs have been modified
      or if the user agrees to abandon modified graphs.
   */
    public void exit() {
        int modcount = 0;
        JInternalFrame[] frames = desktop.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] instanceof GraphFrame) {
                GraphFrame frame = (GraphFrame) frames[i];
                if (frame.getGraphPanel().isModified()) modcount++;
            }
        }
        if (modcount > 0) {
            int result = JOptionPane.showConfirmDialog(EditorFrame.getMainFrame(), MessageFormat.format(editorResources.getString("dialog.exit.ok"), new Object[] { new Integer(modcount) }), null, JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;
        }
        savePreferences();
        System.exit(0);
    }

    /**
    * Saves the user preferences before exiting.
    */
    public void savePreferences() {
        String recent = "";
        for (int i = 0; i < Math.min(recentFiles.size(), maxRecentFiles); i++) {
            if (recent.length() > 0) recent += "|";
            recent += recentFiles.get(i);
        }
        preferences.put("recent", recent);
    }

    private ResourceFactory appFactory;

    private ResourceBundle appResources;

    private ResourceBundle versionResources;

    private ResourceBundle editorResources;

    private JDesktopPane desktop;

    private FileService fileService;

    private PreferencesService preferences;

    private JMenu newMenu;

    private String defaultExtension;

    private String umlcExtension;

    private ArrayList recentFiles;

    private JMenu recentFilesMenu;

    private int maxRecentFiles = DEFAULT_MAX_RECENT_FILES;

    private ExtensionFilter violetFilter;

    private ExtensionFilter exportFilter;

    private static final int FRAME_GAP = 20;

    private static final int ESTIMATED_FRAMES = 5;

    private static final int DEFAULT_MAX_RECENT_FILES = 5;

    private static final double GROW_SCALE_FACTOR = Math.sqrt(2);

    private static PersistenceDelegate staticFieldDelegate = new DefaultPersistenceDelegate() {

        protected Expression instantiate(Object oldInstance, Encoder out) {
            try {
                Class cl = oldInstance.getClass();
                Field[] fields = cl.getFields();
                for (int i = 0; i < fields.length; i++) {
                    if (Modifier.isStatic(fields[i].getModifiers()) && fields[i].get(null) == oldInstance) {
                        return new Expression(fields[i], "get", new Object[] { null });
                    }
                }
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance == newInstance;
        }
    };

    private static java.util.HashMap beanInfos;

    static {
        beanInfos = new java.util.HashMap();
        Class[] cls = new Class[] { Point2D.Double.class, BentStyle.class, ArrowHead.class, LineStyle.class, Graph.class, AbstractNode.class };
        for (int i = 0; i < cls.length; i++) {
            try {
                beanInfos.put(cls[i], java.beans.Introspector.getBeanInfo(cls[i]));
            } catch (java.beans.IntrospectionException ex) {
            }
        }
    }

    /**
     * Holds value of property generator.
     */
    private GeneratorIF generator;

    /**
     * Getter for property generator.
     * @return Value of property generator.
     */
    public GeneratorIF getGenerator() {
        return this.generator;
    }

    /**
     * Setter for property generator.
     * @param generator New value of property generator.
     */
    public void setGenerator(GeneratorIF generator) {
        this.generator = generator;
    }

    /**
     * Holds value of property icon.
     */
    private ImageIcon icon;

    /**
     * Getter for property icon.
     * @return Value of property icon.
     */
    public ImageIcon getIcon() {
        return this.icon;
    }

    /**
     * Setter for property icon.
     * @param icon New value of property icon.
     */
    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }
}
