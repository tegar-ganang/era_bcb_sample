package ingenias.module;

import ingenias.editor.GUIResources;
import ingenias.editor.DiagramPaneInitialization;
import ingenias.editor.IDEState;
import ingenias.editor.IDEUpdater;
import ingenias.editor.ProgressListener;
import ingenias.editor.actions.HistoryManager;
import ingenias.editor.actions.LoadFileSwingTask;
import ingenias.editor.persistence.TextAreaOutputStream;
import ingenias.genproject.ProjectGenerator;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class IAFProjectCreatorSwingTask extends SwingWorker<Void, Void> implements ProgressListener {

    private File newSpec;

    private String projectName = "";

    private IDEUpdater updater;

    private IDEState ids;

    private GUIResources resources;

    private IDEState nids;

    private String directory;

    private SpecificationTemplateKind stk;

    public IAFProjectCreatorSwingTask(String projectName, SpecificationTemplateKind stk, IDEUpdater updater, IDEState ids, final GUIResources resources) {
        this.projectName = projectName;
        this.directory = ids.prefs.getWorkspacePath() + "/" + projectName;
        this.updater = updater;
        this.ids = ids;
        this.stk = stk;
        this.resources = resources;
        final SwingWorker sw = this;
        this.addPropertyChangeListener(new PropertyChangeListener() {

            /**
			 * Invoked when task's progress property changes.
			 */
            public void propertyChange(PropertyChangeEvent evt) {
                if (!sw.isDone()) {
                    int progress = sw.getProgress();
                    resources.getProgressBar().setValue(progress);
                    resources.getProgressBar().setString("Creating ..." + ((progress)) + "%");
                }
            }
        });
        this.resources.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        resources.addProgressListener(this);
        resources.getProgressBar().setVisible(true);
        resources.getProgressBar().invalidate();
        resources.getProgressBar().validate();
        resources.getProgressBar().repaint();
        resources.getProgressBar().setString("Creating ...");
        resources.getProgressBar().setStringPainted(true);
        resources.getMainFrame().setEnabled(false);
    }

    @Override
    public Void doInBackground() {
        try {
            new File(directory + "/lib").mkdir();
            new File(directory + "/spec").mkdir();
            new File(directory + "/src").mkdir();
            new File(directory + "/gensrc").mkdir();
            new File(directory + "/permsrc").mkdir();
            new File(directory + "/bin").mkdir();
            new File(directory + "/jade").mkdir();
            new File(directory + "/config").mkdir();
            this.setProgress(0);
            switch(stk) {
                case NoTemplate:
                    String specContent = ProjectGenerator.getDefaultProjectDefinitionWorkspace(projectName);
                    newSpec = new File(directory + "/spec/specification.xml");
                    FileOutputStream fos = new FileOutputStream(newSpec);
                    fos.write(specContent.getBytes());
                    fos.close();
                    break;
                case HelloWorld:
                    newSpec = copyResourceFromTo("examples/exampleHelloWorld.idk", directory + "/spec/specification.xml");
                    break;
                case GUIAgent:
                    newSpec = copyResourceFromTo("examples/exampleGUIAgent.idk", directory + "/spec/specification.xml");
                    break;
                case Interaction:
                    newSpec = copyResourceFromTo("examples/exampleInteraction.idk", directory + "/spec/specification.xml");
                    break;
            }
            replace("{projectLocation}", "{workspace}/" + projectName, newSpec);
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("generate.xml", directory + "/generate.xml");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/modiaf.jar", directory + "/lib/modiaf.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("config/Properties.prop", directory + "/config/Properties.prop");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/Base64.jar", directory + "/lib/Base64.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/commons-codec-1.3.jar", directory + "/lib/commons-codec-1.3.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/ingeniaseditor.jar", directory + "/lib/ingeniaseditor.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jdom.jar", directory + "/lib/jdom.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/swixml.jar", directory + "/lib/swixml.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/xercesImpl.jar", directory + "/lib/xercesImpl.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/http.jar", directory + "/lib/http.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/iiop.jar", directory + "/lib/iiop.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jade.jar", directory + "/lib/jade.jar");
            resources.getProgressBar().setValue(getProgress() + 1);
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/junit.jar", directory + "/lib/junit.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jadeTools.jar", directory + "/lib/jadeTools.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jgraph.jar", directory + "/lib/jgraph.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/xstream-1.2.jar", directory + "/lib/xstream-1.2.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/gnujaxp.jar", directory + "/lib/gnujaxp.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/iText-2.1.5.jar", directory + "/lib/iText-2.1.5.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jcommon-1.0.16.jar", directory + "/lib/jcommon-1.0.16.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jfreechart-1.0.13-experimental.jar", directory + "/lib/jfreechart-1.0.13-experimental.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/jfreechart-1.0.13.jar", directory + "/lib/jfreechart-1.0.13.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/swtgraphics2d.jar", directory + "/lib/swtgraphics2d.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/ant-contrib-1.0b3.jar", directory + "/lib/ant-contrib-1.0b3.jar");
            this.setProgress(getProgress() + 2);
            copyResourceFromTo("lib/AntelopeTasks_3.5.1.jar", directory + "/lib/AntelopeTasks_3.5.1.jar");
            this.setProgress(getProgress() + 2);
            resources.getProgressBar().setString("Generating sources with the IAF ..." + ((getProgress())) + "%");
            File buildFile = new File(directory + "/generate.xml");
            Project p = new Project();
            p.setUserProperty("ant.file", buildFile.getAbsolutePath());
            p.init();
            ProjectHelper helper = ProjectHelper.getProjectHelper();
            p.addReference("ant.projectHelper", helper);
            p.setProperty("specfile", directory + "/spec/specification.xml");
            p.setProperty("mainP", directory);
            helper.parse(p, buildFile);
            DefaultLogger consoleLogger = new DefaultLogger();
            consoleLogger.setErrorPrintStream(new PrintStream(System.err));
            consoleLogger.setOutputPrintStream(new PrintStream(System.out));
            consoleLogger.setMessageOutputLevel(Project.MSG_DEBUG);
            p.addBuildListener(consoleLogger);
            p.executeTarget(p.getDefaultTarget());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void replace(String string, String string2, File newSpec) {
        try {
            String content = readString(new FileInputStream(newSpec));
            content = content.replace(string, string2);
            FileOutputStream fos = new FileOutputStream(newSpec);
            fos.write(content.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void done() {
        resources.getProgressBar().setVisible(false);
        resources.getProgressBar().invalidate();
        resources.getProgressBar().validate();
        resources.getProgressBar().repaint();
        resources.removeProgressListener(this);
        resources.getProgressBar().setValue(resources.getProgressBar().getMaximum());
        this.resources.getMainFrame().setCursor(null);
        resources.getMainFrame().setEnabled(true);
        int decision = JOptionPane.showConfirmDialog(resources.getMainFrame(), "Project created successfully. Do you want to open it?", "Project created", JOptionPane.YES_NO_OPTION);
        if (decision == JOptionPane.YES_OPTION) {
            Runnable launchLoadAction = new Runnable() {

                public void run() {
                    int result = JOptionPane.OK_OPTION;
                    if (ids.isChanged()) {
                        result = JOptionPane.showConfirmDialog(resources.getMainFrame(), "You will loose current data. Do you want to continue (y/n)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (result == JOptionPane.OK_OPTION) {
                            new LoadFileSwingTask(newSpec, updater, ids, resources).execute();
                        }
                    } else new LoadFileSwingTask(newSpec, updater, ids, resources).execute();
                }
            };
            new Thread(launchLoadAction).start();
        }
    }

    public void setCurrentProgress(int progress) {
        setProgress(progress);
    }

    private File copyResourceFromTo(String from, String to) throws FileNotFoundException, IOException {
        InputStream streamToModiaf = this.getClass().getClassLoader().getResourceAsStream(from);
        if (streamToModiaf == null) throw new FileNotFoundException(from + " resource not found");
        File destination = new File(to);
        if (destination.isDirectory() && !destination.exists()) destination.mkdirs();
        FileOutputStream target = new FileOutputStream(destination);
        byte[] bytes = new byte[8000];
        int read = 0;
        do {
            read = streamToModiaf.read(bytes);
            if (read > 0) {
                target.write(bytes, 0, read);
            }
        } while (read != -1);
        target.close();
        streamToModiaf.close();
        return destination;
    }

    public static String readString(FileInputStream streamToModiaf) throws IOException {
        byte[] bytes = new byte[8000];
        StringBuffer sb = new StringBuffer();
        int read = 0;
        do {
            read = streamToModiaf.read(bytes);
            if (read > 0) {
                for (int k = 0; k < read; k++) {
                    sb.append((char) bytes[k]);
                }
            }
        } while (read != -1);
        streamToModiaf.close();
        return sb.toString();
    }
}
