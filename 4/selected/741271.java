package main;

import graphics.ClassData;
import graphics.CompilationErrors;
import graphics.DrawAggregation;
import graphics.DrawInheritance;
import graphics.DrawInterface;
import graphics.GenerateTestCase;
import graphics.JTabbedPaneWithCloseIcons;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.thoughtworks.xstream.XStream;
import parser.JavaCompilerWorker;
import storage.JavaCodeWriter;
import umleditor.ClassDiagramGraph;
import java.io.*;
import java.util.*;

public final class GUI extends JFrame {

    private static final long serialVersionUID = -1069189737105975838L;

    private JSplitPane Window;

    private JTabbedPaneWithCloseIcons Right;

    private Vector<ClassData> Classes;

    private JMenuItem saveUML;

    private JavaParser JP;

    private JFileChooser JFCIm;

    private JFileChooser JFCOp;

    private File[] selectedFiles;

    private int FontSize;

    private Diagram diagram;

    private GenerateTestCase testCase;

    private String fwSaveDir;

    private boolean choseSaveDir;

    /**
	 * Method used to draw the inheritance diagram
	 * Draws it, sets it up in a JScrollPane, puts it to the screen
	 * Updates the GUI
	 */
    private void DrawInheritance() {
        DrawInheritance t1 = new DrawInheritance(JP.getInheritance(), Classes, FontSize);
        JScrollPane JSP = new JScrollPane(t1);
        JSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Right.addTab("Inheritance Diagram", JSP);
        Window.setRightComponent(Right);
        Window.setDividerLocation(200);
    }

    /**
	 * Same thing as the inheritance diagram but with interfaces
	 *
	 */
    private void DrawInt() {
        DrawInterface t1 = new DrawInterface(JP.getInterfaces(), Classes, FontSize);
        JScrollPane JSP = new JScrollPane(t1);
        JSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Right.addTab("Interface Diagram", JSP);
        Window.setRightComponent(Right);
        Window.setDividerLocation(200);
    }

    /**
	 * Same thing as the inheritance diagram but with the aggregation
	 *
	 */
    private void DrawAggregation() {
        DrawAggregation t1 = new DrawAggregation(JP.getAggregation(), Classes, FontSize);
        JScrollPane JSP = new JScrollPane(t1);
        JSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JSP.setSize(600, 600);
        Right.addTab("Aggregation Diagram", JSP);
        Right.setSize(600, 600);
        Window.setRightComponent(Right);
        Window.setDividerLocation(200);
    }

    /**
	 * Imports the files, then runs the parser on those files, then draws the tree
	 * based on the input files
	 * 
	 */
    private void ImportFiles() {
        JFCIm.setDialogTitle("Open File");
        JFCIm.setFileSelectionMode(JFileChooser.FILES_ONLY);
        JFCIm.setCurrentDirectory(new File("."));
        JFCIm.setMultiSelectionEnabled(true);
        int result = JFCIm.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION) return;
        selectedFiles = JFCIm.getSelectedFiles();
        saveUML.setEnabled(true);
        for (File x : selectedFiles) JP.parseSource(x.getPath());
        while (!JP.isReady()) ;
        drawTree();
    }

    /**
	 * the most important method of the class
	 * once the files have been input, they need to be sorted in some fashion so that
	 * all the components can use them
	 * 
	 * the drawTree method sorts all the input data into a ClassData vector, then draws the tree
	 *
	 */
    private void drawTree() {
        Vector<Vector<String>> inherits = JP.getInheritance();
        Vector<String> temp;
        for (int i = 0; i < inherits.size(); i++) {
            temp = inherits.get(i);
            if (temp.size() > 0) for (int j = 0; j < Classes.size(); j++) {
                if (temp.get(0).equals(Classes.get(j).getClassName())) {
                    for (int k = 1; k < temp.size(); k++) Classes.get(j).addInherits(temp.get(k));
                }
            }
        }
        inherits = JP.getAggregation();
        for (int i = 0; i < inherits.size(); i++) {
            temp = inherits.get(i);
            if (temp.size() > 0) for (int j = 0; j < Classes.size(); j++) {
                if (temp.get(0).equals(Classes.get(j).getClassName())) {
                    for (int k = 1; k < temp.size(); k++) Classes.get(j).addAggregates(temp.get(k));
                }
            }
        }
        inherits = JP.getInterfaces();
        for (int i = 0; i < inherits.size(); i++) {
            temp = inherits.get(i);
            if (temp.size() > 0) for (int j = 0; j < Classes.size(); j++) {
                if (temp.get(0).substring(1).equals(Classes.get(j).getClassName())) {
                    Classes.get(j).setInterface(true);
                    for (int k = 1; k < temp.size(); k++) Classes.get(j).addInterface(temp.get(k));
                }
            }
        }
        GlobalVariables.buildTree();
        JScrollPane JSPtemp = new JScrollPane(GlobalVariables.mytree);
        JSPtemp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JSPtemp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        int selection = Right.getSelectedIndex();
        int number = Right.getComponentCount();
        Vector<String> d = new Vector<String>();
        for (int i = 0; i < number; i++) {
            d.add(Right.getComponent(i).getName());
        }
        Right.removeAll();
        for (int i = 0; i < number; i++) {
            if (d.get(i).equals("Aggregation")) {
                DrawAggregation();
                Right.getComponent(i).setName("Aggregation");
            } else if (d.get(i).equals("Inheritance")) {
                DrawInheritance();
                Right.getComponent(i).setName("Inheritance");
            } else {
                DrawInt();
                Right.getComponent(i).setName("Interface");
            }
        }
        Right.setSelectedIndex(selection);
        Window.setLeftComponent(JSPtemp);
        Window.setDividerLocation(200);
    }

    /**
	 * Saves the file
	 * passes the path name to the parser, and then the parser does the rest
	 *
	 */
    private void saveFile() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new XMLFilter());
        jfc.setDialogTitle("Save Project");
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setCurrentDirectory(new File("."));
        jfc.setMultiSelectionEnabled(false);
        int result = jfc.showSaveDialog(this);
        if (result == JFileChooser.CANCEL_OPTION) return;
        String saveFile = jfc.getSelectedFile().getPath();
        if (!saveFile.substring(saveFile.length() - 4, saveFile.length()).equals(".xml")) saveFile += ".xml";
        XStream xstream = new XStream();
        try {
            File f = new File(saveFile);
            if (f.exists()) {
                int res = JOptionPane.showConfirmDialog(null, "File alreay exists! Overwrite?", "Confirm overwrite", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.NO_OPTION) {
                    return;
                }
                f.delete();
                f.createNewFile();
            }
            FileOutputStream fout = new FileOutputStream(f);
            xstream.toXML((ClassDiagramGraph) diagram.getGraph(), fout);
            fout.close();
        } catch (Exception e) {
            jUML.DBG("Gui", "save", "major error", saveFile);
            e.printStackTrace();
        }
    }

    private void openProj() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new XMLFilter());
        jfc.setDialogTitle("Open Project");
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setCurrentDirectory(new File("."));
        jfc.setMultiSelectionEnabled(false);
        int result = jfc.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION) return;
        String projFile = jfc.getSelectedFile().getPath();
        if (!projFile.substring(projFile.length() - 4, projFile.length()).equals(".xml")) projFile += ".xml";
        saveUML.setEnabled(true);
        XStream xstream = new XStream();
        ClassDiagramGraph cdg = new ClassDiagramGraph();
        try {
            FileInputStream fin = new FileInputStream(projFile);
            xstream.fromXML(fin, cdg);
            fin.close();
            fin = null;
        } catch (Exception e) {
            jUML.DBG("GXMLrdr", "major errors everywhere");
            e.printStackTrace();
        }
        if (GlobalVariables.CLASS != null) GlobalVariables.CLASS.clear();
        GlobalVariables.CLASS = cdg.getNodes();
        GlobalVariables.buildTree();
        diagram = new Diagram(Window, cdg);
    }

    /**
	 * Public Constructor
	 * Sets up the GUI, nothing else really
	 * inherited from a jframe, so it will just work correctly no need for anything retarded
	 *
	 */
    public GUI() {
        selectedFiles = null;
        setTitle("jUML_" + jUML.VERSION);
        FontSize = 12;
        GlobalVariables.initTree();
        JP = new JavaParser();
        Right = new JTabbedPaneWithCloseIcons();
        JFCIm = new JFileChooser();
        JFCOp = new JFileChooser();
        Classes = new Vector<ClassData>();
        JFCIm.setFileFilter(new JavaFilter());
        JFCOp.setFileFilter(new XMLFilter());
        Right.setSize(600, 600);
        JScrollPane l = new JScrollPane();
        JScrollPane r = new JScrollPane();
        l.setBackground(Color.WHITE);
        r.setBackground(Color.WHITE);
        l.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        l.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        r.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        r.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Window = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, l, r);
        Window.setDividerLocation(200);
        testCase = new GenerateTestCase();
        fwSaveDir = "";
        choseSaveDir = false;
        add(Window);
        JMenuBar menubar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem newProject = new JMenuItem("New project");
        newProject.setMnemonic(KeyEvent.VK_N);
        JMenuItem fileImp = new JMenuItem("Import source files");
        fileImp.setMnemonic(KeyEvent.VK_I);
        JMenuItem fileOpen = new JMenuItem("Open Project");
        fileOpen.setMnemonic(KeyEvent.VK_O);
        JMenuItem fileExit = new JMenuItem("Exit");
        fileExit.setMnemonic(KeyEvent.VK_X);
        saveUML = new JMenuItem("Save project");
        saveUML.setMnemonic(KeyEvent.VK_S);
        saveUML.setEnabled(false);
        JMenuItem openUML = new JMenuItem("Open project");
        openUML.setMnemonic(KeyEvent.VK_R);
        saveUML.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                saveFile();
            }
        });
        openUML.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                openProj();
            }
        });
        newProject.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (GlobalVariables.CLASS != null) GlobalVariables.CLASS.clear();
                saveUML.setEnabled(true);
                diagram = new Diagram(Window);
            }
        });
        fileExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (saveUML.isEnabled()) {
                    System.exit(0);
                } else {
                    System.exit(0);
                }
            }
        });
        fileImp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ImportFiles();
            }
        });
        file.add(newProject);
        file.add(saveUML);
        file.add(openUML);
        file.add(fileImp);
        file.add(fileExit);
        JMenu diagrams = new JMenu("Diagrams");
        diagrams.setMnemonic(KeyEvent.VK_D);
        JMenuItem InhDiag = new JMenuItem("Inheritance diagram");
        InhDiag.setMnemonic(KeyEvent.VK_H);
        JMenuItem IntDiag = new JMenuItem("Interface diagram");
        IntDiag.setMnemonic(KeyEvent.VK_F);
        JMenuItem AggDiag = new JMenuItem("Aggregation diagram");
        AggDiag.setMnemonic(KeyEvent.VK_A);
        InhDiag.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (Right.getComponentCount() == 3) {
                    if (Right.getComponent(0).getName().equals("Inheritance")) {
                        Right.setSelectedIndex(0);
                    } else if (Right.getComponent(1).getName().equals("Inheritance")) {
                        Right.setSelectedIndex(1);
                    } else {
                        Right.setSelectedIndex(2);
                    }
                } else if (Right.getComponentCount() == 2) {
                    if (Right.getComponent(0).getName().equals("Inheritance")) {
                        Right.setSelectedIndex(0);
                    } else if (Right.getComponent(1).getName().equals("Inheritance")) {
                        Right.setSelectedIndex(1);
                    } else {
                        DrawInheritance();
                        Right.setSelectedIndex(2);
                        Right.getComponent(2).setName("Inheritance");
                    }
                } else if (Right.getComponentCount() == 1) {
                    if (Right.getComponent(0).getName().equals("Inheritance")) Right.setSelectedIndex(0); else {
                        DrawInheritance();
                        Right.setSelectedIndex(1);
                        Right.getComponent(1).setName("Inheritance");
                    }
                } else {
                    DrawInheritance();
                    Right.setSelectedIndex(0);
                    Right.getComponent(0).setName("Inheritance");
                }
            }
        });
        IntDiag.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (Right.getComponentCount() == 3) {
                    if (Right.getComponent(0).getName().equals("Interface")) {
                        Right.setSelectedIndex(0);
                    } else if (Right.getComponent(1).getName().equals("Interface")) {
                        Right.setSelectedIndex(1);
                    } else {
                        Right.setSelectedIndex(2);
                    }
                } else if (Right.getComponentCount() == 2) {
                    if (Right.getComponent(0).getName().equals("Interface")) {
                        Right.setSelectedIndex(0);
                    } else if (Right.getComponent(1).getName().equals("Interface")) {
                        Right.setSelectedIndex(1);
                    } else {
                        DrawInt();
                        Right.setSelectedIndex(2);
                        Right.getComponent(2).setName("Interface");
                    }
                } else if (Right.getComponentCount() == 1) {
                    if (Right.getComponent(0).getName().equals("Interface")) ; else {
                        DrawInt();
                        Right.setSelectedIndex(1);
                        Right.getComponent(1).setName("Interface");
                    }
                } else {
                    DrawInt();
                    Right.setSelectedIndex(0);
                    Right.getComponent(0).setName("Interface");
                }
            }
        });
        AggDiag.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (Right.getComponentCount() == 3) {
                    if (Right.getComponent(0).getName().equals("Aggregation")) {
                        Right.setSelectedIndex(0);
                    } else if (Right.getComponent(1).getName().equals("Aggregation")) {
                        Right.setSelectedIndex(1);
                    } else {
                        Right.setSelectedIndex(2);
                    }
                } else if (Right.getComponentCount() == 2) {
                    if (Right.getComponent(0).getName().equals("Aggregation")) {
                        Right.setSelectedIndex(0);
                    } else if (Right.getComponent(1).getName().equals("Aggregation")) {
                        Right.setSelectedIndex(1);
                    } else {
                        DrawAggregation();
                        Right.setSelectedIndex(2);
                        Right.getComponent(2).setName("Aggregation");
                    }
                } else if (Right.getComponentCount() == 1) {
                    if (Right.getComponent(0).getName().equals("Aggregation")) ; else {
                        DrawAggregation();
                        Right.setSelectedIndex(1);
                        Right.getComponent(1).setName("Aggregation");
                    }
                } else {
                    DrawAggregation();
                    Right.getComponent(0).setName("Aggregation");
                }
            }
        });
        diagrams.add(InhDiag);
        diagrams.add(IntDiag);
        diagrams.add(AggDiag);
        JMenu tools = new JMenu("Tools");
        tools.setMnemonic(KeyEvent.VK_T);
        JMenuItem generateCode = new JMenuItem("Generate Code");
        generateCode.setMnemonic(KeyEvent.VK_G);
        final JMenuItem compileCode = new JMenuItem("Compile Code");
        compileCode.setMnemonic(KeyEvent.VK_C);
        compileCode.setEnabled(false);
        final JMenuItem generateTestCase = new JMenuItem("Generate Test Case");
        generateTestCase.setMnemonic(KeyEvent.VK_T);
        generateTestCase.setEnabled(false);
        final JMenuItem executeCode = new JMenuItem("Execute Code");
        executeCode.setEnabled(false);
        executeCode.setMnemonic(KeyEvent.VK_E);
        generateCode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (!choseSaveDir) {
                    JFrame frame = new JFrame();
                    JFileChooser JFCIm = new JFileChooser();
                    JFCIm.setDialogTitle("Choose save directory");
                    JFCIm.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    JFCIm.setCurrentDirectory(new File("."));
                    JFCIm.setMultiSelectionEnabled(false);
                    int result = JFCIm.showOpenDialog(frame);
                    if (result == JFileChooser.CANCEL_OPTION) return;
                    choseSaveDir = true;
                    fwSaveDir = JFCIm.getSelectedFile().getAbsolutePath() + "/";
                    testCase.setSaveDir(fwSaveDir);
                    generateTestCase.setEnabled(true);
                    compileCode.setEnabled(true);
                }
                GlobalVariables.DISPLAYSRC = true;
                for (umleditor.Node x : GlobalVariables.CLASS) {
                    Thread t = new Thread(new JavaCodeWriter(false, null, fwSaveDir + ((storage.ClassNode) x).getName().getText() + ".java", ((storage.ClassNode) x)));
                    t.start();
                }
            }
        });
        compileCode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Vector<Thread> vT = new Vector<Thread>();
                for (umleditor.Node x : GlobalVariables.CLASS) {
                    Thread t = new Thread(new JavaCompilerWorker(fwSaveDir + ((storage.ClassNode) x).getName().getText() + ".java"));
                    vT.add(t);
                    t.start();
                }
                try {
                    jUML.DBG("CompileCode", "i'm sleepy");
                    Thread.sleep(10);
                    for (int i = 0; i < vT.size(); i++) {
                        if (vT.get(i).isAlive()) {
                            i = -1;
                            Thread.sleep(10);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                jUML.DBG("CompileCode", "checking for errors");
                boolean isErr = false;
                for (umleditor.Node x : GlobalVariables.CLASS) {
                    if (isCompileErr(fwSaveDir, ((storage.ClassNode) x).getName().getText())) {
                        isErr = true;
                        jUML.DBG("CompileCode", "there was an error");
                    }
                }
                if (!isErr) jUML.DBG("CompileCode", "no errors");
            }
        });
        generateTestCase.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Object[] options = { "Save", "Cancel" };
                int a = JOptionPane.showOptionDialog(null, testCase, "Generate Test Case", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, new ImageIcon("./misc/fighter.png"), options, options[0]);
                if (a == JOptionPane.OK_OPTION) {
                    testCase.saveSrc();
                    File f = new File(fwSaveDir + "TestCase.java");
                    if (f.exists()) {
                        a = JOptionPane.showConfirmDialog(null, new JLabel("A test case already exists. Overwrite?"), "Confirm overwrite", JOptionPane.OK_CANCEL_OPTION);
                        if (a == JOptionPane.CANCEL_OPTION) return;
                    }
                    executeCode.setEnabled(true);
                    Thread t = new Thread(new JavaCodeWriter(true, testCase.getSrc(), fwSaveDir + "TestCase.java", null));
                    t.start();
                } else {
                    testCase.clearTextArea();
                }
            }
        });
        executeCode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Thread t = new Thread(new JavaCompilerWorker(fwSaveDir + "TestCase.java"));
                t.start();
                jUML.DBG("Execute", "compiling testcase");
                while (t.isAlive()) ;
                jUML.DBG("Execute", "done compiling testcase");
                if (isCompileErr(fwSaveDir, "TestCase")) return;
                String osType = System.getProperty("os.name");
                String scriptName = null;
                String scriptText = null;
                if (osType.contains("Windows")) {
                    scriptText = "@echo off\n" + "cd " + fwSaveDir + "\n" + "java TestCase\n" + "pause\n" + "exit 0";
                    scriptName = "tmp.bat";
                } else {
                    scriptText = "#! /bin/bash\n\n" + "cd " + fwSaveDir + "\n" + "java TestCase\n" + "read -p \"Press enter to exit\"\n";
                    scriptName = "tmp.sh";
                }
                jUML.DBG("Execute", "creating " + scriptName);
                File f = new File(fwSaveDir + scriptName);
                try {
                    f.createNewFile();
                    FileWriter fout = new FileWriter(f);
                    fout.write(scriptText);
                    fout.flush();
                    fout.close();
                } catch (IOException e) {
                    jUML.DBG("Execute", "error writing " + scriptName);
                    e.printStackTrace();
                    return;
                }
                jUML.DBG("Execute", "done creating " + scriptName);
                f.setExecutable(true);
                jUML.DBG("Execute", "gogogogo");
                String exe = null;
                if (osType.contains("Windows")) {
                    exe = "cmd /c start ";
                } else {
                    exe = "xterm -e ";
                }
                jUML.DBG("ExecCode", exe + fwSaveDir + scriptName);
                try {
                    Runtime.getRuntime().exec(exe + fwSaveDir + scriptName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        tools.add(generateCode);
        tools.add(compileCode);
        tools.add(generateTestCase);
        tools.add(executeCode);
        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem About = new JMenuItem("About");
        About.setMnemonic(KeyEvent.VK_A);
        About.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                JOptionPane.showMessageDialog(null, "jUML: the Java UML diagrammer\n" + "Authors: Josh Haynes and Jeff Prillaman", "About this program...", JOptionPane.PLAIN_MESSAGE, new ImageIcon("./misc/vtux_small.png"));
            }
        });
        help.add(About);
        menubar.add(file);
        menubar.add(diagrams);
        menubar.add(tools);
        menubar.add(help);
        setJMenuBar(menubar);
        setSize(800, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocation(0, 0);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setVisible(true);
    }

    public boolean isCompileErr(String dir, String className) {
        File f = new File(dir + className + ".java.out");
        if (f.length() > 0) {
            jUML.DBG("Execute", "error compiling " + className);
            Object[] options = { "Ok" };
            JOptionPane.showOptionDialog(null, new CompilationErrors(f), "Compilation Errors: " + className + ".java", JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, new ImageIcon("./misc/black_mage.png"), options, options[0]);
            return true;
        }
        return false;
    }
}
