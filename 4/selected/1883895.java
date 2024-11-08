package eric;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import rene.gui.Global;
import rene.util.FileName;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlTag;
import rene.util.xml.XmlTagPI;
import rene.util.xml.XmlTree;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.Count;
import rene.zirkel.macro.Macro;
import rene.zirkel.macro.MacroItem;
import rene.zirkel.objects.PointObject;
import eric.controls.SliderSnap;

/**
 * 
 * @author erichake
 */
public class JMacrosTools {

    public static Vector librarymacros = new Vector();

    public static Vector builtinmacros = new Vector();

    public static String MacrosLibraryFileName = "";

    public static String MacrosBackupLibraryFileName = "";

    public static JZirkelFrame CurrentJZF = null;

    public static ArrayList AllJZFs = new ArrayList();

    public static ArrayList AllJZFs2 = new ArrayList();

    public static ArrayList StartupFiles = new ArrayList();

    public static boolean isStartup = true;

    public static boolean isNewVersion = false;

    public static boolean busy = false;

    public JMacrosTools() {
    }

    public static boolean isJZFnumTooBig() {
        if (AllJZFs.size() > 10) {
            JOptionPane.showMessageDialog(null, JGlobals.Loc("alert.toomuchwins"));
            return true;
        }
        return false;
    }

    public static void NewJZirkelWindow(final boolean with3D, final int w, final int h) {
        int xloc = 0, yloc = 0;
        final JZirkelFrame oldframe = CurrentJZF;
        if (!(oldframe == null)) {
            updateLibraryFromTree();
            final Point pt = oldframe.getLocation();
            xloc = pt.x + 20;
            yloc = pt.y + 20;
        }
        initProperties();
        JGlobalPreferences.setLocalPreferences();
        rene.zirkel.construction.Count.resetAll();
        CurrentJZF = new eric.JZirkelFrame(with3D, xloc, yloc, w, h);
        if (AllJZFs.size() == 0) {
            LoadDefaultMacrosAtStartup();
        }
        AllJZFs2.add(CurrentJZF);
        AllJZFs.add(CurrentJZF);
        CurrentJZF.ZContent.macros.myJML.initMacrosTree();
    }

    public static String shortFileName(String s) {
        s = s.replace(System.getProperty("file.separator"), "@sep@");
        final String fn[] = s.split("@sep@");
        return fn[fn.length - 1];
    }

    public static void setWindowTitle(final JZirkelFrame jzf) {
        final String s1 = (AllJZFs2.size() < 2) ? "" : "[" + (AllJZFs2.indexOf(jzf) + 1) + "] ";
        final String s2 = (jzf.ZF.Filename.equals("")) ? Zirkel.name("program.name") : shortFileName(jzf.ZF.Filename);
        jzf.SetTitle(s1 + s2);
        jzf.GeneralMenuBar.InitWindowsMenu();
    }

    public static void RefreshDisplay() {
        for (int i = 0; i < AllJZFs.size(); i++) {
            final JZirkelFrame jzf = (JZirkelFrame) AllJZFs.get(i);
            setWindowTitle(jzf);
            if (!jzf.equals(CurrentJZF)) {
                jzf.ZF.ZC.setFrozen(false);
                jzf.ZF.ZC.setMouseAllowed(false);
                jzf.JPM.MainPalette.setVisible(false);
                jzf.ResizeAll();
                jzf.ZF.ZC.removeMouseMotionListener(jzf.ZF.ZC);
                jzf.JPR.setLocalPreferences();
                jzf.ZF.ZC.setFrozen(true);
                jzf.pack();
            }
        }
        CurrentJZF.ZF.ZC.setFrozen(true);
        CurrentJZF.ResizeAll();
        CurrentJZF.JPM.MainPalette.FollowWindow();
        CurrentJZF.JPM.MainPalette.setVisible(true);
        CurrentJZF.ZF.ZC.addMouseMotionListener(CurrentJZF.ZF.ZC);
        CurrentJZF.JPM.MainPalette.paintImmediately();
        CurrentJZF.JPR.setLocalPreferences();
        CurrentJZF.JPM.setGoodProperties(CurrentJZF.JPM.geomSelectedIcon());
        CurrentJZF.ZF.ZC.setFrozen(false);
        CurrentJZF.pack();
        final JZirkelFrame myjzf = CurrentJZF;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                myjzf.ZF.ZC.setMouseAllowed(true);
            }
        });
        if (JGlobals.JPB != null) {
            JGlobals.JPB.clearme();
        }
    }

    public static void FirstRun() {
        if (StartupFiles.size() > 0) {
            OpenStartupFiles();
        } else {
            NewWindow();
        }
        isStartup = false;
    }

    public static boolean isStartup() {
        return isStartup;
    }

    public static void NewWindow() {
        if (isJZFnumTooBig()) {
            return;
        }
        NewJZirkelWindow(false, 800, 600);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                RefreshDisplay();
                addListeners();
            }
        });
    }

    public static void OpenStartupFiles() {
        busy = true;
        for (int i = 0; i < StartupFiles.size(); i++) {
            final String filename = (String) StartupFiles.get(i);
            if ((filename.endsWith(".mcr"))) {
                OpenMacro(filename);
            } else {
                OpenFile(filename, null, false);
            }
        }
        StartupFiles.clear();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                RefreshDisplay();
                busy = false;
                addListeners();
            }
        });
    }

    public static void New3DWindow() {
        if (isJZFnumTooBig()) {
            return;
        }
        final InputStream o = JMacrosTools.class.getResourceAsStream("/base3D.zir");
        final String Filename = "base3D.zir";
        OpenFile(Filename, o, true);
        CurrentJZF.ZF.setTitle(Zirkel.name("program.name"));
        CurrentJZF.ZF.Filename = "";
        RefreshDisplay();
        final JZirkelFrame myJZF = CurrentJZF;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                myJZF.ZF.ZC.JCM.fix3Dcomments();
            }
        });
    }

    public static void OpenFile() {
        if (isJZFnumTooBig()) {
            return;
        }
        final String filename = getOpenFile();
        if (!filename.equals("")) {
            removeListeners();
            OpenFile(filename, null, false);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    RefreshDisplay();
                    addListeners();
                }
            });
        }
    }

    public static Dimension FindWindowSize(final String filename) {
        final Dimension d = new Dimension(800, 600);
        InputStream o = null;
        try {
            o = new FileInputStream(filename);
            if (FileName.extension(filename).endsWith("z")) {
                o = new GZIPInputStream(o);
            }
            final XmlReader xml = new XmlReader();
            xml.init(o);
            XmlTree tree = xml.scan();
            o.close();
            Enumeration e = tree.getContent();
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                if (tree.getTag() instanceof XmlTagPI) {
                    continue;
                }
                if (!tree.getTag().name().equals("CaR")) {
                    throw new ConstructionException("CaR tag not found");
                } else {
                    break;
                }
            }
            e = tree.getContent();
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                if (tree.getTag().name().equals("Construction")) {
                    break;
                }
            }
            e = tree.getContent();
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                if (tree.getTag().name().equals("Windowdim")) {
                    break;
                }
            }
            final XmlTag tag = tree.getTag();
            if ((tag.hasParam("w")) && (tag.hasParam("h"))) {
                final int w = Integer.parseInt(tag.getValue("w"));
                final int h = Integer.parseInt(tag.getValue("h"));
                d.width = w;
                d.height = h;
            }
        } catch (final Exception ex) {
        }
        return d;
    }

    public static boolean isAlreadyOpened(final String filename) {
        for (int i = 0; i < AllJZFs.size(); i++) {
            final JZirkelFrame jzf = (JZirkelFrame) AllJZFs.get(i);
            if (jzf.ZF.Filename.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    public static void OpenFile(final String filename, final InputStream in, final boolean with3D) {
        if (isJZFnumTooBig()) {
            return;
        }
        if (filename.equals("")) {
            return;
        }
        if (isAlreadyOpened(filename)) {
            return;
        }
        final Dimension d = FindWindowSize(filename);
        if ((filename.endsWith(".zir")) || (filename.endsWith(".zirz"))) {
            int m = 1;
            try {
                if (!CurrentJZF.busy) {
                    m = CurrentJZF.ZF.ZC.getConstruction().V.size();
                }
            } catch (final Exception e) {
            }
            if (m > 0) {
                NewJZirkelWindow(with3D, d.width, d.height);
            }
            CurrentJZF.is3D = with3D;
            CurrentJZF.busy = true;
            CurrentJZF.ZF.setinfo("save");
            CurrentJZF.ZF.ZC.getConstruction().BackgroundFile = null;
            CurrentJZF.ZF.Background = "";
            CurrentJZF.ZF.dograb(false);
            CurrentJZF.ZF.doload(filename, in);
            CurrentJZF.JPM.fix3Dpalette();
            CurrentJZF.JPR.getLocalPreferences();
            rene.zirkel.construction.Count.resetAll();
            CurrentJZF.ZContent.macros.myJML.initMacrosTree();
            CurrentJZF.JPM.setSelected("grid", CurrentJZF.ZF.ZC.showGrid());
            CurrentJZF.JPM.setSelected("hidden", false);
            Global.setParameter("grid.colorindex", CurrentJZF.ZF.ZC.GridColor);
            Global.setParameter("grid.thickness", CurrentJZF.ZF.ZC.GridThickness);
            Global.setParameter("grid.labels", CurrentJZF.ZF.ZC.GridLabels);
            Global.setParameter("grid.axesonly", CurrentJZF.ZF.ZC.AxesOnly);
            CurrentJZF.JPM.setSelected("acolor" + Global.getParameter("grid.colorindex", 0), true);
            CurrentJZF.JPM.setSelected("athickness" + Global.getParameter("grid.thickness", 0), true);
            CurrentJZF.JPM.setSelected("numgrid", Global.getParameter("grid.labels", false));
            CurrentJZF.JPM.setSelected("dottedgrid", Global.getParameter("grid.axesonly", false));
            CurrentJZF.JPM.setSelected("partial", false);
            CurrentJZF.JPM.setSelected("plines", false);
            CurrentJZF.JPM.setSelected("showvalue", false);
            if (CurrentJZF.ZF.ZC.getConstruction().BackgroundFile == null) {
                CurrentJZF.JPM.setSelected("background", false);
            } else {
                CurrentJZF.JPM.setSelectedWithoutClic("background", true);
            }
            CurrentJZF.ZChanges.CLength = 0;
            CurrentJZF.toFront();
            CurrentJZF.JPM.MainPalette.FollowWindow();
            final JZirkelFrame myjzf = CurrentJZF;
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    myjzf.ZF.ZC.JCM.readXmlTags();
                }
            });
        }
    }

    public static void OpenMacro(final String name) {
        if (AllJZFs.size() == 0) {
            NewJZirkelWindow(false, 800, 600);
        }
        updateLibraryFromTree();
        saveLibraryToDisk();
        CurrentJZF.ZF.setinfo("macro");
        if (name.equals("")) {
            CurrentJZF.ZF.loadMacros();
        } else {
            InputStream o;
            try {
                o = new FileInputStream(name);
                if (ZirkelFrame.isCompressed(name)) {
                    o = new GZIPInputStream(o);
                }
                CurrentJZF.ZF.ZC.load(o, false, true);
                o.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        final JZirkelFrame myjzf = CurrentJZF;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                myjzf.ZContent.macros.myJML.initMacrosTree();
            }
        });
    }

    public static void setCurrentJZF(final JZirkelFrame jzf) {
        if ((CurrentJZF == null) || (!(CurrentJZF.equals(jzf)))) {
            updateLibraryFromTree();
            CurrentJZF = jzf;
            rene.zirkel.construction.Count.resetAll();
            PointObject.setPointLabel(CurrentJZF.PointLabel);
            final JZirkelFrame myjzf = CurrentJZF;
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    myjzf.ZContent.macros.myJML.initMacrosTree();
                }
            });
        }
    }

    public static void setDefaultMacros() {
        if (!(CurrentJZF == null)) {
            int i = 0;
            final Vector F = new Vector();
            final Vector V = CurrentJZF.ZF.ZC.getMacros();
            for (i = 0; i < V.size(); i++) {
                final MacroItem mi = (MacroItem) V.get(i);
                if (!(mi.M.isProtected())) {
                    F.add(V.get(i));
                }
            }
            V.clear();
            for (i = 0; i < builtinmacros.size(); i++) {
                V.add(builtinmacros.get(i));
            }
            for (i = 0; i < librarymacros.size(); i++) {
                V.add(librarymacros.get(i));
            }
            for (i = 0; i < F.size(); i++) {
                V.add(F.get(i));
            }
        }
    }

    public static void getDefaultMacros() {
        if ((!(CurrentJZF == null))) {
            librarymacros.clear();
            final Vector V = CurrentJZF.ZF.ZC.getMacros();
            for (int i = 0; i < V.size(); i++) {
                final MacroItem mi = (MacroItem) V.get(i);
                if (mi.M.isProtected()) {
                    if (!(mi.M.Name.startsWith("@builtin@"))) {
                        librarymacros.add(V.get(i));
                    }
                }
            }
        }
    }

    public static void removeListeners() {
        for (int i = 0; i < AllJZFs.size(); i++) {
            JZirkelFrame f = (JZirkelFrame) AllJZFs.get(i);
            f.removeListeners();
        }
    }

    public static void addListeners() {
        removeListeners();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                for (int i = AllJZFs.size() - 1; i > -1; i--) {
                    JZirkelFrame f = (JZirkelFrame) AllJZFs.get(i);
                    f.addListeners();
                }
            }
        });
    }

    public static void activate(JZirkelFrame jzf) {
        if (busy) {
            return;
        }
        if ((AllJZFs.contains(jzf)) && (jzf != CurrentJZF)) {
            AllJZFs.remove(jzf);
            AllJZFs.add(jzf);
            jzf.toFront();
            setCurrentJZF(jzf);
            RefreshDisplay();
        }
    }

    public static void activateLastJZF() {
        for (int i = AllJZFs.size() - 1; i > -1; i--) {
            JZirkelFrame f = (JZirkelFrame) AllJZFs.get(i);
            if (f.getState() != JFrame.ICONIFIED) {
                activate(f);
                return;
            }
        }
    }

    /**
     * Find the front most window and activate it
     * @return
     */
    public static boolean isBackWindow() {
        final Frame jfs[] = Frame.getFrames();
        for (final Frame jf : jfs) {
            if ((jf.isActive()) && (jf instanceof JZirkelFrame)) {
                final JZirkelFrame jzf = (JZirkelFrame) jf;
                if (!jzf.equals(CurrentJZF)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isBelongToCurrentJZF(ZirkelCanvas zc) {
        if (CurrentJZF == null) {
            return true;
        } else {
            return CurrentJZF.ZF.ZC.equals(zc);
        }
    }

    public static boolean disposeCurrentJZF() {
        CurrentJZF.setExtendedState(Frame.NORMAL);
        if (AllJZFs.size() == 1) {
            if (!CurrentJZF.restricted) {
                updateLibraryFromTree();
                saveLibraryToDisk();
                JGlobalPreferences.savePreferences();
            }
            if (CurrentJZF.ZF.close()) {
                Global.saveProperties("CaR Properties");
                Global.exit(0);
            } else {
                return false;
            }
        } else {
            if (CurrentJZF.ZF.close()) {
                AllJZFs2.remove(CurrentJZF);
                AllJZFs.remove(CurrentJZF);
                final JPalette jp = CurrentJZF.JPM.MainPalette;
                final ZirkelFrame curZF = CurrentJZF.ZF;
                JZirkelFrame curJZF = CurrentJZF;
                curJZF.dispose();
                curZF.dispose();
                jp.dispose();
                activateLastJZF();
            } else {
                return false;
            }
        }
        return true;
    }

    public static void disposeAllJZFs() {
        if (!CurrentJZF.restricted) {
            updateLibraryFromTree();
            saveLibraryToDisk();
            JGlobalPreferences.savePreferences();
        }
        while (AllJZFs.size() > 1) {
            CurrentJZF = (JZirkelFrame) AllJZFs.get(AllJZFs.size() - 1);
            CurrentJZF.setExtendedState(Frame.NORMAL);
            if (CurrentJZF.ZF.close()) {
                AllJZFs2.remove(CurrentJZF);
                AllJZFs.remove(CurrentJZF);
                final JPalette jp = CurrentJZF.JPM.MainPalette;
                final ZirkelFrame curZF = CurrentJZF.ZF;
                final JZirkelFrame curJZF = CurrentJZF;
                curJZF.dispose();
                curZF.dispose();
                jp.dispose();
                activateLastJZF();
            } else {
                return;
            }
        }
        CurrentJZF = (JZirkelFrame) AllJZFs.get(0);
        CurrentJZF.setExtendedState(Frame.NORMAL);
        if (CurrentJZF.ZF.close()) {
            Global.saveProperties("CaR Properties");
            Global.exit(0);
        } else {
            return;
        }
    }

    /**
     * This function will copy files or directories from one location to
     * another. note that the source and the destination must be mutually
     * exclusive. This function can not be used to copy a directory to a sub
     * directory of itself. The function will also have problems if the
     * destination files already exist.
     *
     * @param src
     *            -- A File object that represents the source for the copy
     * @param dest
     *            -- A File object that represnts the destination for the copy.
     * @throws IOException
     *             if unable to copy.
     */
    public static void copyFiles(final File src, final File dest) throws IOException {
        if (!src.exists()) {
            throw new IOException("copyFiles: Can not find source: " + src.getAbsolutePath() + ".");
        } else if (!src.canRead()) {
            throw new IOException("copyFiles: No right to source: " + src.getAbsolutePath() + ".");
        }
        if (src.isDirectory()) {
            if (!dest.exists()) {
                if (!dest.mkdirs()) {
                    throw new IOException("copyFiles: Could not create direcotry: " + dest.getAbsolutePath() + ".");
                }
            }
            final String list[] = src.list();
            for (final String element : list) {
                final File dest1 = new File(dest, element);
                final File src1 = new File(src, element);
                copyFiles(src1, dest1);
            }
        } else {
            FileInputStream fin = null;
            FileOutputStream fout = null;
            final byte[] buffer = new byte[4096];
            int bytesRead;
            try {
                fin = new FileInputStream(src);
                fout = new FileOutputStream(dest);
                while ((bytesRead = fin.read(buffer)) >= 0) {
                    fout.write(buffer, 0, bytesRead);
                }
            } catch (final IOException e) {
                final IOException wrapper = new IOException("copyFiles: Unable to copy file: " + src.getAbsolutePath() + "to" + dest.getAbsolutePath() + ".");
                wrapper.initCause(e);
                wrapper.setStackTrace(e.getStackTrace());
                throw wrapper;
            } finally {
                if (fin != null) {
                    fin.close();
                }
                if (fout != null) {
                    fin.close();
                }
            }
        }
    }

    public static void copyFile(final String inFile, final String outFile) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(inFile).getChannel();
            out = new FileOutputStream(outFile).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (final Exception e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (final Exception e) {
                }
            }
        }
    }

    public static String getHomeDirectory() {
        final String name = "carmetal_config";
        final String SP = System.getProperty("file.separator");
        return FileSystemView.getFileSystemView().getDefaultDirectory() + SP + name + SP;
    }

    public static void createLocalDirectory() {
        final String mypath = eric.JGlobals.AppPath();
        if ((isNewVersion) || (!(new File(getHomeDirectory() + "docs").exists()))) {
            try {
                copyFiles(new File(mypath + "docs"), new File(getHomeDirectory() + "docs"));
            } catch (final IOException ex) {
                System.out.println("bug : createLocalDirectory()");
            }
        }
        String Filename = "library.mcr";
        if (new File(mypath + Zirkel.name("language", "") + "library.mcr").exists()) {
            Filename = Zirkel.name("language", "") + "library.mcr";
        } else if (new File(getHomeDirectory() + Zirkel.name("language", "") + "library.mcr").exists()) {
            Filename = Zirkel.name("language", "") + "library.mcr";
        }
        MacrosLibraryFileName = getHomeDirectory() + Filename;
        if (new File(MacrosLibraryFileName).exists()) {
            if (isNewVersion) {
                MacrosBackupLibraryFileName = getHomeDirectory() + "library_backup.mcr";
                copyFile(MacrosLibraryFileName, MacrosBackupLibraryFileName);
                copyFile(mypath + Filename, MacrosLibraryFileName);
            }
        } else {
            new File(getHomeDirectory()).mkdirs();
            copyFile(mypath + Filename, MacrosLibraryFileName);
        }
    }

    /**
     *
     */
    public static void LoadDefaultMacrosAtStartup() {
        try {
            final InputStream o = JMacrosTools.class.getResourceAsStream("/builtin.mcr");
            LoadMacros(o, builtinmacros);
            o.close();
        } catch (final Exception e) {
        }
        if (new File(MacrosLibraryFileName).exists()) {
            try {
                final InputStream o = new FileInputStream(MacrosLibraryFileName);
                LoadMacros(o, librarymacros);
                o.close();
                if (!MacrosBackupLibraryFileName.equals("")) {
                    final InputStream o2 = new FileInputStream(MacrosBackupLibraryFileName);
                    LoadMacros(o2, librarymacros);
                    o2.close();
                    final File f = new File(MacrosBackupLibraryFileName);
                    f.delete();
                }
                return;
            } catch (final Exception e) {
            }
        }
        try {
            final InputStream o = JMacrosTools.class.getResourceAsStream("/default.mcr");
            LoadMacros(o, librarymacros);
            o.close();
            return;
        } catch (final Exception e) {
        }
    }

    private static void saveLibraryToDisk() {
        if (!CurrentJZF.restricted) {
            CurrentJZF.ZF.dosave(MacrosLibraryFileName, false, true, true, librarymacros);
        }
    }

    public static void updateLibraryFromTree() {
        if (!CurrentJZF.restricted) {
            librarymacros.removeAllElements();
            parseupdate(CurrentJZF.ZContent.macros.myJML.MacroTreeTopNode, librarymacros);
        }
    }

    private static void parseupdate(final JDefaultMutableTreeNode node, final Vector V) {
        if (node.isLeaf()) {
            final String myname = (String) node.getUserObject();
            if (!(myname.startsWith("-- "))) {
                if (node.m.isProtected()) {
                    final MacroItem mi = new MacroItem(node.m, null);
                    V.add(mi);
                }
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                parseupdate((JDefaultMutableTreeNode) node.getChildAt(i), V);
            }
        }
    }

    private static void LoadMacros(final InputStream in, final Vector Macros) throws Exception {
        Macro m;
        try {
            final XmlReader xml = new XmlReader();
            xml.init(in);
            XmlTree tree = xml.scan();
            if (tree == null) {
                throw new ConstructionException("XML file not recognized");
            }
            Enumeration e = tree.getContent();
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                if (tree.getTag() instanceof XmlTagPI) {
                    continue;
                }
                if (!tree.getTag().name().equals("CaR")) {
                    throw new ConstructionException("CaR tag not found");
                } else {
                    break;
                }
            }
            e = tree.getContent();
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                final XmlTag tag = tree.getTag();
                if (tag.name().equals("Macro")) {
                    try {
                        Count.setAllAlternate(true);
                        m = new Macro(null, tree);
                        int i = 0;
                        for (i = 0; i < Macros.size(); i++) {
                            if (((MacroItem) Macros.elementAt(i)).M.getName().equals(m.getName())) {
                                break;
                            }
                        }
                        if (i >= Macros.size()) {
                            m.setProtected(true);
                            final MacroItem mi = new MacroItem(m, null);
                            Macros.addElement(mi);
                        }
                    } catch (final ConstructionException ex) {
                        Count.setAllAlternate(false);
                        throw ex;
                    }
                    Count.setAllAlternate(false);
                } else {
                    throw new ConstructionException("Construction not found");
                }
            }
        } catch (final Exception e) {
            throw e;
        }
    }

    public static void initObjectsProperties() {
        Global.setParameter("options.segment.color", Global.getParameter("options.segment.color", 1));
        Global.setParameter("options.segment.colortype", Global.getParameter("options.segment.colortype", 0));
        Global.setParameter("options.segment.shownames", Global.getParameter("options.segment.shownames", false));
        Global.setParameter("options.segment.showvalues", Global.getParameter("options.segment.showvalues", false));
        Global.setParameter("options.segment.large", Global.getParameter("options.segment.large", false));
        Global.setParameter("options.segment.bold", Global.getParameter("options.segment.bold", false));
        Global.setParameter("options.line.color", Global.getParameter("options.line.color", 3));
        Global.setParameter("options.line.colortype", Global.getParameter("options.line.colortype", 0));
        Global.setParameter("options.line.shownames", Global.getParameter("options.line.shownames", false));
        Global.setParameter("options.line.showvalues", false);
        Global.setParameter("options.line.large", Global.getParameter("options.line.large", false));
        Global.setParameter("options.line.bold", Global.getParameter("options.line.bold", false));
        Global.setParameter("options.point.color", Global.getParameter("options.point.color", 2));
        Global.setParameter("options.point.colortype", Global.getParameter("options.point.colortype", 0));
        Global.setParameter("options.point.shownames", Global.getParameter("options.point.shownames", false));
        Global.setParameter("options.point.large", Global.getParameter("options.point.large", false));
        Global.setParameter("options.point.bold", Global.getParameter("options.point.bold", false));
        Global.setParameter("options.point.showvalues", Global.getParameter("options.point.showvalues", false));
        Global.setParameter("options.circle.color", Global.getParameter("options.circle.color", 4));
        Global.setParameter("options.circle.colortype", Global.getParameter("options.circle.colortype", 0));
        Global.setParameter("options.circle.shownames", Global.getParameter("options.circle.shownames", false));
        Global.setParameter("options.circle.showvalues", Global.getParameter("options.circle.showvalues", false));
        Global.setParameter("options.circle.filled", Global.getParameter("options.circle.filled", false));
        Global.setParameter("options.circle.solid", Global.getParameter("options.circle.solid", false));
        Global.setParameter("options.circle.large", Global.getParameter("options.circle.large", false));
        Global.setParameter("options.circle.bold", Global.getParameter("options.circle.bold", false));
        Global.setParameter("options.angle.color", Global.getParameter("options.angle.color", 1));
        Global.setParameter("options.angle.colortype", Global.getParameter("options.angle.colortype", 0));
        Global.setParameter("options.angle.shownames", Global.getParameter("options.angle.shownames", false));
        Global.setParameter("options.angle.showvalues", Global.getParameter("options.angle.showvalues", true));
        Global.setParameter("options.angle.filled", Global.getParameter("options.angle.filled", true));
        Global.setParameter("options.angle.solid", Global.getParameter("options.angle.solid", false));
        Global.setParameter("options.angle.large", Global.getParameter("options.angle.large", false));
        Global.setParameter("options.angle.bold", Global.getParameter("options.angle.bold", false));
        Global.setParameter("options.angle.obtuse", Global.getParameter("options.angle.obtuse", false));
        Global.setParameter("options.area.color", Global.getParameter("options.area.color", 1));
        Global.setParameter("options.area.colortype", Global.getParameter("options.area.colortype", 2));
        Global.setParameter("options.area.shownames", Global.getParameter("options.area.shownames", false));
        Global.setParameter("options.area.showvalues", Global.getParameter("options.area.showvalues", false));
        Global.setParameter("options.area.filled", Global.getParameter("options.area.filled", true));
        Global.setParameter("options.area.solid", Global.getParameter("options.area.solid", false));
        Global.setParameter("options.text.color", Global.getParameter("options.text.color", 1));
        Global.setParameter("options.text.colortype", Global.getParameter("options.text.colortype", 1));
        Global.setParameter("options.text.shownames", Global.getParameter("options.text.shownames", true));
        Global.setParameter("options.text.showvalues", Global.getParameter("options.text.showvalues", true));
        Global.setParameter("options.locus.color", Global.getParameter("options.locus.color", 1));
        Global.setParameter("options.locus.colortype", Global.getParameter("options.locus.colortype", 0));
        Global.setParameter("options.locus.shownames", Global.getParameter("options.locus.shownames", false));
        Global.setParameter("options.locus.showvalues", Global.getParameter("options.locus.showvalues", false));
    }

    public static void initProperties() {
        if (!Global.getParameter("program.version", "").equals(Zirkel.name("program.version"))) {
            Global.setParameter("program.newversion", true);
            Global.setParameter("program.version", Zirkel.name("program.version"));
            Global.setParameter("icons", ZirkelFrame.DefaultIcons);
            isNewVersion = true;
        }
        Global.setParameter("iconpath", "/rene/zirkel/newicons/");
        Global.setParameter("icontype", "png");
        if (Global.getParameter("options.smallicons", false)) {
            Global.setParameter("iconsize", 24);
        } else {
            Global.setParameter("iconsize", 32);
        }
        Global.setParameter("save.includemacros", true);
        Global.setParameter("load.clearmacros", false);
        Global.setParameter("options.backups", false);
        Global.setParameter("options.visual", true);
        Global.setParameter("options.filedialog", false);
        Global.setParameter("options.restricted", true);
        Global.setParameter("options.smallicons", false);
        Global.setParameter("options.indicate", true);
        Global.setParameter("restricted", false);
        Global.setParameter("showgrid", false);
        Global.setParameter("simplegraphics", false);
        Global.setParameter("quality", true);
        Global.setParameter("export.jar", "CaRMetal.jar");
        Global.setParameter("iconpath", "/eric/icons/palette/");
        Global.Background = Global.getParameter("colorbackground", new Color(231, 238, 255));
        Global.setParameter("background.tile", Global.getParameter("background.tile", false));
        if (!Global.haveParameter("options.germanpoints") && Locale.getDefault().getLanguage().equals("de")) {
            Global.setParameter("options.germanpoints", true);
        }
        SliderSnap.init();
        initObjectsProperties();
    }

    public static String getOpenFile() {
        String name = "";
        final JFileChooser jfc = new JFileChooser(JGlobals.getLastFilePath());
        jfc.setDialogType(javax.swing.JFileChooser.OPEN_DIALOG);
        jfc.setApproveButtonText("Ouvrir la figure");
        jfc.setAcceptAllFileFilterUsed(false);
        final JFileFilter ffilter = new JFileFilter(CurrentJZF.Strs.getString("filedialog.filefilter"), ".zir");
        jfc.addChoosableFileFilter(ffilter);
        final JFileFilter fcfilter = new JFileFilter(CurrentJZF.Strs.getString("filedialog.compressedfilefilter"), ".zirz");
        jfc.addChoosableFileFilter(fcfilter);
        jfc.setFileFilter(ffilter);
        jfc.setAccessory(new ZirkelCanvasFileChooserPreview(jfc));
        final int rep = jfc.showOpenDialog(null);
        if (rep == JFileChooser.APPROVE_OPTION) {
            name = jfc.getSelectedFile().getAbsolutePath();
            JGlobals.setLastFilePath(name);
        } else {
            name = "";
        }
        return name;
    }
}
