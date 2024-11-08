package com.endlessloopsoftware.ego.author;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.egonet.io.StudyReader;
import org.egonet.io.StudyWriter;
import org.egonet.util.DirList;
import org.egonet.util.ExtensionFileFilter;
import com.endlessloopsoftware.egonet.Question;
import com.endlessloopsoftware.egonet.Study;

/****
 * Handles IO for the EgoNet program
 * Tracks data files and changes to those files
 */
public class EgoStore {

    private File studyFile = null;

    private boolean studyFileInUse = false;

    private final EgoNet egoNet;

    private static final String[] questionExtensions = { "qst", "qtp" };

    private static FileFilter readQuestionFilter = (FileFilter) new ExtensionFileFilter("Question Files", questionExtensions[0]);

    private static FileFilter writeQuestionFilter = (FileFilter) new ExtensionFileFilter("Question Templates", questionExtensions);

    private static FileFilter studyFilter = new ExtensionFileFilter("Study Files", "ego");

    private static final String FILE_PREF = "FILE_PREF";

    /**
    * Sets parent frame
    * 
    * @param frame
    *            parent
    */
    public EgoStore(EgoNet egoNet) {
        this.egoNet = egoNet;
    }

    /************************************************************************************************************************************************************
    * Returns study file
    * 
    * @return studyFile file containing study overview information
    */
    public File getStudyFile() {
        return (studyFile);
    }

    /************************************************************************************************************************************************************
    * Returns study file
    * 
    * @return studyFile file containing study overview information
    */
    public boolean getStudyInUse() {
        return (studyFileInUse);
    }

    /************************************************************************************************************************************************************
    * Sets baseQuestionFile variable and notifies observers of change to study
    * 
    * @param f
    *            question file
    */
    public void setStudyFile(File f) {
        studyFile = f;
    }

    /************************************************************************************************************************************************************
    * Select a directory in which to store project related files Create subdirectories if needed.
    */
    public void newStudyFiles() {
        Preferences prefs = Preferences.userNodeForPackage(EgoNet.class);
        JFileChooser jNewStudyChooser = new JFileChooser();
        File dirFile, newStudyFile;
        String projectPath = null;
        String projectName = null;
        jNewStudyChooser.addChoosableFileFilter(studyFilter);
        jNewStudyChooser.setDialogTitle("Select Study Path");
        if (getStudyFile() != null) {
            jNewStudyChooser.setCurrentDirectory(getStudyFile().getParentFile());
        } else {
            File directory = new File(prefs.get(FILE_PREF, "."));
            jNewStudyChooser.setCurrentDirectory(directory);
        }
        try {
            if (JFileChooser.APPROVE_OPTION == jNewStudyChooser.showSaveDialog(egoNet.getFrame())) {
                projectPath = jNewStudyChooser.getSelectedFile().getParent();
                projectName = jNewStudyChooser.getSelectedFile().getName();
                if (projectName.indexOf(".") != -1) {
                    projectName = projectName.substring(0, projectName.indexOf("."));
                }
                try {
                    String folder = projectPath.substring(projectPath.lastIndexOf(File.separator) + 1);
                    if (!folder.equals(projectName)) {
                        dirFile = new File(projectPath, projectName);
                        dirFile.mkdir();
                        projectPath = dirFile.getPath();
                    }
                } catch (SecurityException e) {
                    JOptionPane.showMessageDialog(egoNet.getFrame(), "Unable to create study directories.", "New Study Error", JOptionPane.ERROR_MESSAGE);
                    throw new IOException("Cannot create study directory for " + projectPath);
                }
                try {
                    newStudyFile = new File(projectPath, projectName);
                    newStudyFile = ((ExtensionFileFilter) studyFilter).getCorrectFileName(newStudyFile);
                    if (!newStudyFile.createNewFile()) {
                        int confirm = JOptionPane.showConfirmDialog(egoNet.getFrame(), "<HTML><h2>Study already exists at this location.</h2>" + "<p>Shall I overwrite it?</p></html>", "Overwrite Study File", JOptionPane.OK_CANCEL_OPTION);
                        if (confirm != JOptionPane.OK_OPTION) {
                            throw new IOException("Won't overwrite " + newStudyFile.getName());
                        } else {
                            newStudyFile.delete();
                            newStudyFile.createNewFile();
                        }
                    }
                    Study study = new Study();
                    study.setStudyId(System.currentTimeMillis());
                    egoNet.setStudy(study);
                    setStudyFile(newStudyFile);
                    egoNet.getStudy().setStudyName(projectName);
                    StudyWriter sw = new StudyWriter(newStudyFile);
                    sw.setStudy(study);
                    studyFileInUse = false;
                    prefs.put(FILE_PREF, newStudyFile.getParent());
                } catch (java.io.IOException e) {
                    JOptionPane.showMessageDialog(egoNet.getFrame(), "Unable to create study file.", "File Error", JOptionPane.ERROR_MESSAGE);
                    throw new IOException(e);
                }
                try {
                    dirFile = new File(projectPath, "Statistics");
                    dirFile.mkdir();
                    dirFile = new File(projectPath, "Interviews");
                    dirFile.mkdir();
                } catch (SecurityException e) {
                    JOptionPane.showMessageDialog(egoNet.getFrame(), "Unable to create study directories.", "New Study Error", JOptionPane.ERROR_MESSAGE);
                    throw new IOException(e);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(egoNet.getFrame(), "Study not created.");
            setStudyFile(null);
        }
    }

    /************************************************************************************************************************************************************
    * Select a directory in which to store project related files Create subdirectories if needed.
    */
    public void selectStudy() {
        Preferences prefs = Preferences.userNodeForPackage(EgoNet.class);
        JFileChooser jNewStudyChooser = new JFileChooser();
        File f;
        jNewStudyChooser.addChoosableFileFilter(studyFilter);
        jNewStudyChooser.setDialogTitle("Select Study");
        if (getStudyFile() != null) {
            jNewStudyChooser.setCurrentDirectory(getStudyFile().getParentFile());
        } else {
            jNewStudyChooser.setCurrentDirectory(new File(prefs.get(FILE_PREF, ".")));
        }
        if (JFileChooser.APPROVE_OPTION == jNewStudyChooser.showOpenDialog(egoNet.getFrame())) {
            f = jNewStudyChooser.getSelectedFile();
            try {
                if (!f.canRead()) {
                    throw new IOException("Cannot read study file");
                } else {
                    readStudy(f);
                    setStudyFile(f);
                    prefs.put(FILE_PREF, f.getParent());
                }
            } catch (Exception e) {
                setStudyFile(null);
                JOptionPane.showMessageDialog(null, "Unable to read study file.", "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /************************************************************************************************************************************************************
    * Select a question file to use for custom questions
    */
    public void importQuestions() throws Exception {
        JFileChooser jNewStudyChooser = new JFileChooser();
        File newFile;
        jNewStudyChooser.setCurrentDirectory(DirList.getLibraryDirectory());
        jNewStudyChooser.addChoosableFileFilter(readQuestionFilter);
        jNewStudyChooser.setDialogTitle("Select Custom Questions File");
        if (JFileChooser.APPROVE_OPTION == jNewStudyChooser.showOpenDialog(egoNet.getFrame())) {
            newFile = jNewStudyChooser.getSelectedFile();
            try {
                if (!newFile.canRead()) {
                    throw (new IOException("Cannot read file " + newFile));
                }
                List<Question> questions = StudyReader.getQuestions(newFile);
                for (Question q : questions) egoNet.getStudy().addQuestion(q);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Unable to read question file.", "File Error", JOptionPane.ERROR_MESSAGE);
                throw e;
            }
        }
    }

    /************************************************************************************************************************************************************
    * Save study information to a file with a new name
    */
    public void saveStudyFile() {
        File studyFile = getStudyFile();
        try {
            if (!studyFile.exists()) {
                throw new java.io.IOException("File " + studyFile.getName() + " does not exist");
            }
            if (!studyFile.canWrite()) {
                throw new java.io.IOException("File " + studyFile.getName() + " is not writeable");
            }
            StudyWriter sw = new StudyWriter(studyFile);
            sw.setStudy(egoNet.getStudy());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /************************************************************************************************************************************************************
    * Save question information to a file with a new name
    */
    public void exportQuestions() {
        JFileChooser jNewQuestionsChooser = new JFileChooser();
        File newQuestionFile;
        jNewQuestionsChooser.setCurrentDirectory(new File(getStudyFile().getParent(), "/Questions/"));
        jNewQuestionsChooser.addChoosableFileFilter(writeQuestionFilter);
        jNewQuestionsChooser.setDialogTitle("Save Custom Questions As...");
        if (JFileChooser.APPROVE_OPTION == jNewQuestionsChooser.showSaveDialog(egoNet.getFrame())) {
            try {
                newQuestionFile = ((ExtensionFileFilter) writeQuestionFilter).getCorrectFileName(jNewQuestionsChooser.getSelectedFile());
                if (!newQuestionFile.createNewFile()) {
                    int confirm = JOptionPane.showConfirmDialog(egoNet.getFrame(), "<HTML><h2>Question File already exists at this location.</h2>" + "<p>Shall I overwrite it?</p></html>", "Overwrite Questions File", JOptionPane.OK_CANCEL_OPTION);
                    if (confirm != JOptionPane.OK_OPTION) {
                        throw new IOException("Won't overwrite " + newQuestionFile.getName());
                    }
                }
                writeAllQuestions(newQuestionFile);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(egoNet.getFrame(), "Unable to create question file.", "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /************************************************************************************************************************************************************
    * Save study info and questions as a package
    */
    public void saveAsStudyFile() {
        JFileChooser jNewQuestionsChooser = new JFileChooser("Save Study As...");
        File newStudyFile;
        boolean complete = false;
        if (getStudyFile() != null) jNewQuestionsChooser.setCurrentDirectory(getStudyFile().getParentFile());
        jNewQuestionsChooser.addChoosableFileFilter(studyFilter);
        while (!complete) {
            if (JFileChooser.APPROVE_OPTION == jNewQuestionsChooser.showSaveDialog(egoNet.getFrame())) {
                try {
                    int confirm = JOptionPane.OK_OPTION;
                    newStudyFile = ((ExtensionFileFilter) studyFilter).getCorrectFileName(jNewQuestionsChooser.getSelectedFile());
                    if (!newStudyFile.createNewFile()) {
                        if (newStudyFile.canWrite()) {
                            confirm = JOptionPane.showConfirmDialog(egoNet.getFrame(), "<HTML><h3>A Study File already exists at this location.</h3>" + "<p>Shall I overwrite it?</p></html>", "Overwrite Study Package File", JOptionPane.OK_CANCEL_OPTION);
                        } else {
                            confirm = JOptionPane.showConfirmDialog(egoNet.getFrame(), "<HTML><h2>An <b>Active</b> Study File already exists at this location.</h2>" + "<p>If you overwrite it, any interviews created with it will be unreadable!</p>" + "<p>Shall I overwrite it?</p></html>", "Overwrite Study Package File", JOptionPane.OK_CANCEL_OPTION);
                        }
                    }
                    if (confirm == JOptionPane.OK_OPTION) {
                        if (!newStudyFile.canWrite()) {
                            throw (new java.io.IOException());
                        }
                        StudyWriter sw = new StudyWriter(newStudyFile);
                        egoNet.getStudy().setStudyId(System.currentTimeMillis());
                        sw.setStudy(egoNet.getStudy());
                        setStudyFile(newStudyFile);
                        studyFileInUse = false;
                        complete = true;
                        Preferences prefs = Preferences.userNodeForPackage(EgoNet.class);
                        prefs.put(FILE_PREF, newStudyFile.getParent());
                    }
                } catch (java.io.IOException e) {
                    JOptionPane.showMessageDialog(egoNet.getFrame(), "Unable to write to study file. Study not saved.");
                    throw new RuntimeException(e);
                }
            } else {
                complete = true;
            }
        }
    }

    /************************************************************************************************************************************************************
    * Writes all questions to a package file for later use
    * 
    * @param f
    *            File to write data to
    * @throws IOException
    */
    private void writeAllQuestions(File f) throws IOException {
        StudyWriter sw = new StudyWriter(f);
        sw.writeAllQuestionData(egoNet.getStudy().getQuestions());
    }

    /************************************************************************************************************************************************************
    * Reads in study information from an XML like input file Includes files paths and arrays of question orders
    * 
    * @param file
    *            XML file from which to read study
    */
    public void readStudy(File file) {
        if (file != null) {
            try {
                StudyReader sr = new StudyReader(file);
                if (sr.isStudyInUse()) {
                    studyFileInUse = true;
                    JOptionPane.showMessageDialog(egoNet.getFrame(), "This study has already been used for at least one interview.\n" + "You may change the text of questions while still using previously generated interview files. However, \n" + "if you add, delete, reorder, or modify the answer types of any questions you will no longer be able to use \n" + "it to view existing interview files.", "File In Use", JOptionPane.WARNING_MESSAGE);
                }
                Study study = sr.getStudy();
                egoNet.setStudy(study);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(egoNet.getFrame(), "Unable to read this study file", "Study Reading Error", JOptionPane.ERROR_MESSAGE);
                egoNet.setStudy(new Study());
            }
        }
    }
}
