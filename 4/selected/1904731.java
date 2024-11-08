package fw4ex_client.generate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import fw4ex_client.Activator;
import fw4ex_client.data.ExpectationDirectory;
import fw4ex_client.data.ExpectationFile;
import fw4ex_client.data.interfaces.IExercise;
import fw4ex_client.data.interfaces.IExpectation;
import fw4ex_client.data.interfaces.IQuestion;

public class GenerateProject {

    public static final String STEM_FILE_EXETENSION = ".fws";

    private IExercise exercise;

    private IProject project;

    private IProgressMonitor progressMonitor;

    private ResourceBundle bundle;

    public GenerateProject(IExercise ex) {
        this.exercise = ex;
        bundle = Activator.getDefault().getResourceBundle();
    }

    public void generate() {
        try {
            progressMonitor = new NullProgressMonitor();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            project = root.getProject(exercise.getContent().getNickname());
            if (project.exists()) {
                boolean replace = Activator.getDefault().askQuestion(bundle.getString("Project_Exists_Title"), bundle.getString("Replace_Project") + project.getName());
                if (replace) {
                    project.delete(true, true, progressMonitor);
                    deploy();
                } else {
                    return;
                }
            } else {
                deploy();
            }
        } catch (CoreException e) {
            ResourceBundle bundle = Activator.getDefault().getResourceBundle();
            Activator.getDefault().showMessage(bundle.getString("Project_Creation_Error"));
        }
    }

    private void deploy() {
        try {
            project.create(progressMonitor);
            project.open(progressMonitor);
            exercise.serialize(System.getProperty("java.io.tmpdir") + "tmp_fw4ex_" + project.getName() + "_data.dat");
            QualifiedName exid = new QualifiedName("fw4ex", "exerciseId");
            QualifiedName exlocation = new QualifiedName("fw4ex", "exerciseLocation");
            project.setPersistentProperty(exid, exercise.getId());
            project.setPersistentProperty(exlocation, exercise.getLocation());
            project.setPersistentProperty(new QualifiedName("fw4ex", "nb_submissions"), new Integer(0).toString());
            createFilesIntoProject();
            File d = new File(project.getLocation() + "/" + bundle.getString("Report_Dir"));
            d.mkdir();
        } catch (CoreException e) {
            ResourceBundle bundle = Activator.getDefault().getResourceBundle();
            Activator.getDefault().showMessage(bundle.getString("Project_Creation_Error"));
        }
    }

    private void createFilesIntoProject() {
        ArrayList<IQuestion> quest = exercise.getContent().getContent().getQuestions();
        for (IQuestion q : quest) {
            ArrayList<IExpectation> exp = q.getExpectations();
            for (IExpectation e : exp) {
                if (e instanceof ExpectationDirectory) {
                    File d = new File(project.getLocation() + "/" + e.getBasename());
                    d.mkdir();
                } else if (e instanceof ExpectationFile) {
                    try {
                        FileOutputStream stream = new FileOutputStream(new File(project.getLocation() + "/" + e.getBasename()));
                        OutputStreamWriter writer = new OutputStreamWriter(stream);
                        writer.write(((ExpectationFile) e).getInitialText());
                        writer.flush();
                        writer.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public void copyFilesIntoProject(HashMap<String, String> files) {
        Set<String> filenames = files.keySet();
        for (String key : filenames) {
            String realPath = files.get(key);
            if (key.equals("fw4ex.xml")) {
                try {
                    FileReader in = new FileReader(new File(realPath));
                    FileWriter out = new FileWriter(new File(project.getLocation() + "/" + bundle.getString("Stem") + STEM_FILE_EXETENSION));
                    int c;
                    while ((c = in.read()) != -1) out.write(c);
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    Activator.getDefault().showMessage("File " + key + " not found... Error while moving files to the new project.");
                } catch (IOException ie) {
                    Activator.getDefault().showMessage("Error while moving " + key + " to the new project.");
                }
            } else {
                try {
                    FileReader in = new FileReader(new File(realPath));
                    FileWriter out = new FileWriter(new File(project.getLocation() + "/" + key));
                    int c;
                    while ((c = in.read()) != -1) out.write(c);
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    Activator.getDefault().showMessage("File " + key + " not found... Error while moving files to the new project.");
                } catch (IOException ie) {
                    Activator.getDefault().showMessage("Error while moving " + key + " to the new project.");
                }
            }
        }
    }

    public void refreshView() {
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
        } catch (CoreException e) {
        }
    }
}
