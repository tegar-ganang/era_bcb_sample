package com.antlersoft.opentool;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Stack;
import javax.swing.JButton;
import com.borland.jbuilder.node.JBProject;
import com.borland.primetime.vfs.Filesystem;
import com.borland.primetime.vfs.Url;
import com.antlersoft.analyzer.DBClass;
import com.antlersoft.analyzer.IndexAnalyzeDB;
import com.antlersoft.analyzer.query.QueryParser;
import com.antlersoft.classwriter.ClassWriter;
import com.antlersoft.odb.ObjectDBException;
import com.antlersoft.query.environment.AnalyzerQuery;

class ProjectAnalyzer {

    JBProject project;

    IndexAnalyzeDB db;

    AnalyzerQuery qp;

    private String goodPath;

    ProjectAnalyzer(BBQNode node) throws Exception {
        project = (JBProject) node.getProject();
        db = new IndexAnalyzeDB();
        String dbPath = BBQPathsGroup.pathsProperty.getValue(node);
        File analyzerFile;
        if (dbPath.length() == 0) {
            analyzerFile = new File(project.getProjectPath().getFileObject(), "analyzer.pj");
            BBQPathsGroup.pathsProperty.setValue(node, analyzerFile.getPath());
        } else {
            analyzerFile = new File(dbPath);
        }
        String canonicalPath = analyzerFile.getCanonicalPath();
        try {
            db.openDB(analyzerFile);
        } catch (ObjectDBException odb) {
            odb.printStackTrace();
            clearDB(node, canonicalPath);
        }
        qp = new AnalyzerQuery(new QueryParser());
        goodPath = canonicalPath;
        try {
            File environment_file = new File(analyzerFile, "environment.xml");
            if (environment_file.canRead()) qp.readEnvironment(new FileReader(environment_file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void clearDB(BBQNode node, String canonicalPath) throws Exception {
        db.clearDB();
        BBQPathsGroup.updateTimeProperty.setValue(node, "0");
    }

    void updateDB(final BBQNode node, final JButton button) throws Exception {
        long lastModified = Long.parseLong(BBQPathsGroup.updateTimeProperty.getValue(node));
        String classPath = BBQPathsGroup.classPathProperty.getValue(node);
        Url classUrl;
        if (classPath.length() == 0) {
            classUrl = project.getPaths().getOutPath();
        } else {
            classUrl = new Url(classPath);
        }
        button.setEnabled(false);
        (new UpdateDBThread(classUrl, button, lastModified, node)).start();
    }

    void setAnalyzerPath(BBQNode node, String newPath) {
        String testPath = "";
        try {
            File testFile = new File(newPath);
            testPath = testFile.getCanonicalPath();
            db.openDB(testFile);
            BBQPathsGroup.updateTimeProperty.setValue(node, "0");
        } catch (Exception e) {
            try {
                testPath = goodPath;
                newPath = BBQPathsGroup.pathsProperty.getValue(node);
                db.openDB(new File(goodPath));
            } catch (Exception e2) {
                e2.printStackTrace();
                db = null;
            }
        }
        BBQPathsGroup.pathsProperty.setValue(node, newPath);
        goodPath = testPath;
    }

    void saveEnvironment() throws Exception {
        File environment_file = new File(goodPath, "environment.xml");
        qp.writeEnvironment(new FileWriter(environment_file));
    }

    class UpdateDBThread extends Thread {

        private Url classUrl;

        private JButton button;

        private long lastModified;

        private BBQNode node;

        UpdateDBThread(Url url, JButton b, long l, BBQNode n) {
            classUrl = url;
            button = b;
            lastModified = l;
            node = n;
        }

        public void run() {
            Stack directoryStack = new Stack();
            directoryStack.push(classUrl);
            Filesystem system = classUrl.getFilesystem();
            ClassWriter classwriter = new ClassWriter();
            final long startTime = System.currentTimeMillis();
            try {
                while (!directoryStack.isEmpty()) {
                    Url currentDirectory = (Url) directoryStack.pop();
                    Url[] contents = system.getChildren(currentDirectory, null, Filesystem.TYPE_BOTH);
                    for (int i = 0; i < contents.length; i++) {
                        if (system.isDirectory(contents[i])) directoryStack.push(contents[i]); else {
                            if (contents[i].getFileExtension().equals("class") && system.getLastModified(contents[i]) > lastModified) {
                                System.out.println("Analyzing " + contents[i].toString());
                                BufferedInputStream bis = new BufferedInputStream(system.getInputStream(contents[i]));
                                classwriter.readClass(bis);
                                DBClass.addClassToDB(classwriter, db);
                                bis.close();
                            }
                        }
                    }
                }
                db.closeDB();
                db.openDB(new File(goodPath));
                EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        BBQPathsGroup.updateTimeProperty.setValue(node, Long.toString(startTime));
                    }
                });
            } catch (Exception ioe) {
                ioe.printStackTrace();
            } finally {
                EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        button.setEnabled(true);
                    }
                });
            }
        }
    }
}
