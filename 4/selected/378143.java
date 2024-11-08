package com.qasystems.qstudio.java.gui;

import com.qasystems.debug.DebugWriter;
import com.qasystems.international.MessageResource;
import com.qasystems.io.FileReader;
import com.qasystems.io.FileString;
import com.qasystems.qstudio.configuration.Format;
import com.qasystems.qstudio.java.QStudioGlobal;
import com.qasystems.qstudio.java.Version;
import com.qasystems.qstudio.java.classloader.Trier;
import com.qasystems.qstudio.java.cli.CLI;
import com.qasystems.util.Utilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Date;
import java.util.Vector;

/**
 * This class invokes the analyzer on the given set of project members.
 * You should create instances of Analyzer instead of this class,
 * because subclass Analyzer defines the correct edition behaviour.
 */
public abstract class AnalyzerControl implements Runnable, PropertyChangeListener {

    private static final String SEPLINE = "*************************************************";

    private final MessageResource resources = MessageResource.getClientInstance();

    private String QSJAVAHOMEDIR = null;

    private ProjectMember[] MEMBERS = null;

    private boolean ANALYZER_LOOP_CANCELED = false;

    private Date date = null;

    private int successCount = 0;

    private boolean addLogHeader = false;

    private transient Vector listeners = new Vector();

    private String[] sourcePath = null;

    private String[] classPath = null;

    private String[] pmdClassPath = null;

    private boolean annotateSource = false;

    private PrintWriter errorWriter = new PrintWriter(new BufferedOutputStream(System.err));

    private RuleSet rules = null;

    private Thread thread;

    private String outputPath = null;

    private String javaVersion = null;

    /**
   * Minimal constructor.
   * Uses QStudioGlobal to get the installation directory.
   * Does not set error dialog.
   */
    public AnalyzerControl(ProjectMember[] members, RuleSet newRules) {
        MEMBERS = members;
        if (MEMBERS != null) {
            setRules(newRules);
            QSJAVAHOMEDIR = new QStudioGlobal().getQsjavaHomePath();
            setThread(new Thread(this));
        }
    }

    /**
   * This constructor initializes the AnalyzerControl using the given project.
   * ProjectMembers from the project will be analyzed after calling start().
   * @param project
   */
    public AnalyzerControl(Project project) {
        final Vector members = new Vector();
        for (ProjectMember member = project.getFirstMember(); member != null; member = project.getNextMember()) {
            members.add(member);
        }
        MEMBERS = (ProjectMember[]) members.toArray(new ProjectMember[members.size()]);
        setRules(project.getRules());
        QSJAVAHOMEDIR = new QStudioGlobal().getQsjavaHomePath();
        setThread(new Thread(this));
        setProperties(project);
    }

    /**
   * This constructor initializes the AnalyzerControl. Use start() to start
   * the analysis.
   *
   * @param writer the writer
   * @param members the set of project members to be analyzed
   * @param newRules the set of rules to apply
   * @param qsjavaHomeDir the QStudio for Java installation
   *   directory as specified in the .INI file
   */
    public AnalyzerControl(PrintWriter writer, ProjectMember[] members, RuleSet newRules, String qsjavaHomeDir) {
        errorWriter = writer;
        MEMBERS = members;
        if (MEMBERS != null) {
            setRules(newRules);
            QSJAVAHOMEDIR = qsjavaHomeDir;
            setThread(new Thread(this));
        }
    }

    /**
   * Use this method to actually start the analysis session.
   */
    public void start() {
        if (thread != null) {
            if (!Version.isEnterprise()) {
                thread.start();
            }
        }
    }

    /**
   * Start the analyzer and wait for the analyzer thread to finish
   */
    public void runAnalysis() {
        if (thread != null) {
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                new DebugWriter().writeException(e, this);
            }
        }
    }

    /**
   * Implemented for the Enterprise Edition only.
   *
   * @param projectId the projectID or 1 for the local project
   * @param isMileStone if <tt>true</tt> the results are stored as milestone
   */
    public void start(String projectId, boolean isMileStone) {
    }

    /**
   * Never call this method yourself. Use Start() instead.
   */
    public void run() {
        File cfgFile = null;
        setSuccessCount(0);
        fireEvent(new AnalyzerEvent(this, AnalyzerEvent.ANALYZERSTARTED_EVENT_ID, 0, MEMBERS.length));
        setDate(new Date());
        setAddLogHeader(true);
        try {
            cfgFile = File.createTempFile("cfg", null);
            createConfigurationFile(cfgFile);
        } catch (IOException ex) {
            onConfigFileCreationError();
        }
        try {
            for (int i = 0; (i < MEMBERS.length) && !ANALYZER_LOOP_CANCELED; i++) {
                runAnalyzer(MEMBERS[i], i, MEMBERS.length, cfgFile);
            }
        } catch (Exception ex) {
            new DebugWriter().writeException(ex, this);
        }
        try {
            Utilities.discardBooleanResult(cfgFile.delete());
            fireEvent(new AnalyzerEvent(this, AnalyzerEvent.ANALYZERFINISHED_EVENT_ID, getSuccessCount(), MEMBERS.length));
            errorWriter.close();
        } catch (Exception ex) {
            new DebugWriter().writeException(ex, this);
        }
    }

    /**
   * Add analyzer event listener.
   *
   * @param listener the listener
   */
    public synchronized void addAnalyzerEventListener(AnalyzerEventListener listener) {
        listeners.addElement(listener);
    }

    /**
   * Remove analyzer event listener.
   *
   * @param listener the listener
   */
    public synchronized void removeAnalyzerEventListener(AnalyzerEventListener listener) {
        listeners.removeElement(listener);
    }

    protected void onConfigFileCreationError() {
        logError(resources.getString("MESSAGE_043"));
    }

    private void fireEvent(AnalyzerEvent event) {
        for (int i = 0; i < listeners.size(); i++) {
            final AnalyzerEventListener listener = (AnalyzerEventListener) listeners.elementAt(i);
            switch(event.getID()) {
                case AnalyzerEvent.ANALYZERSTARTED_EVENT_ID:
                    {
                        listener.analyzerStarted(event);
                        break;
                    }
                case AnalyzerEvent.ANALYZERFINISHED_EVENT_ID:
                    {
                        listener.analyzerFinished(event);
                        break;
                    }
                case AnalyzerEvent.ANALYZERFILESTARTED_EVENT_ID:
                    {
                        listener.analyzingFileStarted(event);
                        break;
                    }
                case AnalyzerEvent.ANALYZERFILEFINISHED_EVENT_ID:
                    {
                        listener.analyzingFileFinished(event);
                        break;
                    }
                default:
                    errorWriter.println("Illegal event in method com.qasystems.qstudio.Analyze.fireEvent()");
            }
        }
    }

    private String join(String[] paths) {
        StringBuffer result = null;
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                if (result == null) {
                    result = new StringBuffer(paths[i]);
                } else {
                    Utilities.discardResult(result.append(File.pathSeparator).append(paths[i]));
                }
            }
        }
        return ((result == null) ? null : result.toString());
    }

    private String[] getCliOptions(ProjectMember member, File cfgFile, File errFile, File chkFile, File annFile, File mtxFile, File sumFile) {
        final Vector cliOptions = new Vector();
        String[] path = null;
        Utilities.discardBooleanResult(cliOptions.add("-home"));
        Utilities.discardBooleanResult(cliOptions.add(QSJAVAHOMEDIR));
        Utilities.discardBooleanResult(cliOptions.add("-cfg"));
        Utilities.discardBooleanResult(cliOptions.add(cfgFile.getPath()));
        if (javaVersion != null) {
            Utilities.discardBooleanResult(cliOptions.add("-source"));
            Utilities.discardBooleanResult(cliOptions.add(javaVersion));
        }
        if (chkFile != null) {
            Utilities.discardBooleanResult(cliOptions.add("-chk"));
            Utilities.discardBooleanResult(cliOptions.add(chkFile.getPath()));
        }
        Utilities.discardBooleanResult(cliOptions.add("-err"));
        Utilities.discardBooleanResult(cliOptions.add(errFile.getPath()));
        path = getSourcePath();
        if ((path != null) && (path.length > 0)) {
            Utilities.discardBooleanResult(cliOptions.add("-sourcepath"));
            Utilities.discardBooleanResult(cliOptions.add(join(path)));
        }
        path = getClassPath();
        if ((path != null) && (path.length > 0)) {
            Utilities.discardBooleanResult(cliOptions.add("-classpath"));
            Utilities.discardBooleanResult(cliOptions.add(join(path)));
        }
        Utilities.discardBooleanResult(cliOptions.add("-silent"));
        if (isAnnotateSource()) {
            if (annFile != null) {
                Utilities.discardBooleanResult(cliOptions.add("-ann"));
                Utilities.discardBooleanResult(cliOptions.add(annFile.getPath()));
            } else {
                Utilities.discardBooleanResult(cliOptions.add("-ann"));
            }
        }
        if (mtxFile != null) {
            Utilities.discardBooleanResult(cliOptions.add("-mtx"));
            Utilities.discardBooleanResult(cliOptions.add(mtxFile.getPath()));
        } else {
            if (Version.isEnterprise()) {
                Utilities.discardBooleanResult(cliOptions.add("-mtx"));
            }
        }
        if (sumFile != null) {
            Utilities.discardBooleanResult(cliOptions.add("-sum"));
            Utilities.discardBooleanResult(cliOptions.add(sumFile.getPath()));
        } else {
            if (Version.isEnterprise()) {
                Utilities.discardBooleanResult(cliOptions.add("-sum"));
            }
        }
        if (Trier.getDebugLevel() == Trier.NORMAL) {
            cliOptions.add("-time");
        }
        Utilities.discardBooleanResult(cliOptions.add(new FileString(member.getFile()).toString()));
        return ((String[]) cliOptions.toArray(new String[cliOptions.size()]));
    }

    /**
   * For the lite edition, the output file is parsed and written
   * back in serialized form as a kind of scrambling.
   */
    private void runAnalyzer(ProjectMember member, int seqNr, int count, File cfgFile) {
        File errFile = null;
        boolean outputPathOK = true;
        fireEvent(new AnalyzerEvent(this, AnalyzerEvent.ANALYZERFILESTARTED_EVENT_ID, member, seqNr, count));
        try {
            if (!outputPathOK) {
                logError(resources.getString("MESSAGE_044"));
            } else if (member.getFile().exists()) {
                final File chkFile = new File(member.createOutputFileName(outputPath, "chk"));
                errFile = new File(member.createOutputFileName(outputPath, "err"));
                final File annFile = new File(member.createOutputFileName(outputPath, "ann"));
                final File mtxFile = new File(member.createOutputFileName(outputPath, "mtx"));
                final File sumFile = new File(member.createOutputFileName(outputPath, "sum"));
                final CLI cli = new CLI(getCliOptions(member, cfgFile, errFile, chkFile, annFile, mtxFile, sumFile));
                member.setMustAnalyze(true);
                final int errCode = cli.cli();
                processAnalyzerResult(member, errFile, errCode, cli);
                if (errFile.length() == 0) {
                    errFile.delete();
                }
            } else {
                onSourceFileNotFound(member.toString());
            }
        } catch (Exception ex) {
            deleteChkFileOnError(member);
            onAnalyzerThrowedException(member.toString(), ex, errFile);
            new DebugWriter().writeException(ex, this);
            if ((errFile != null) && errFile.exists()) {
                uploadFailure(member);
            }
        }
        fireEvent(new AnalyzerEvent(this, AnalyzerEvent.ANALYZERFILEFINISHED_EVENT_ID, member, seqNr, count));
    }

    private void encryptChkFile(ProjectMember member, File chkFile) throws Exception {
        final java.io.FileReader reader = new java.io.FileReader(chkFile);
        final File encryptedChkFile = new File(member.createOutputFileName(outputPath, "chk"));
        FileOutputStream outfile = null;
        ObjectOutputStream outstream = null;
        Utilities.discardBooleanResult(encryptedChkFile.getParentFile().mkdirs());
        outfile = new FileOutputStream(encryptedChkFile);
        outstream = new ObjectOutputStream(outfile);
        outstream.writeObject(new Format().parse(reader));
        reader.close();
        outfile.close();
        outstream.close();
    }

    protected void onSourceFileNotFound(String sourceFile) {
        final String pattern = resources.getString("MESSAGE_045");
        logError(resources.format(pattern, new Object[] { sourceFile }));
    }

    protected void onAnalyzerThrowedException(String sourceFile, Exception ex, File errFile) {
        final String msg = new String("When analyzing '" + sourceFile + "' an exception occured: " + ex.toString());
        if ((errFile != null) && errFile.canWrite()) {
            try {
                final FileOutputStream out = new FileOutputStream(errFile);
                out.write(msg.getBytes());
                out.close();
            } catch (IOException ex2) {
                new DebugWriter().writeException(ex2, this);
            }
        }
        logError(msg);
    }

    /**
   * Creates a configuration (.cfg) file for the analyzer.
   * The configuration file defines the rules to be checked.
   *
   * @param cfgFile the file to write the configuration to.
   * @exception java.io.IOException passes all exceptions to the caller
   */
    private void createConfigurationFile(File cfgFile) throws IOException {
        final RuleSet ruleSet = getRules();
        ruleSet.setOutputFile(cfgFile);
        ruleSet.store();
    }

    /**
   * Analyzes the results code from an analyzer run and displays the
   * appropriate message in case an error occured.
   *
   * @param member the project member that was analyzed
   * @param errCode error code of the cli
   * @param errFile the file containing errors and/or warnings from the
   *                analyzer
   * @param cli the command line interface
   */
    private void processAnalyzerResult(ProjectMember member, File errFile, int errCode, CLI cli) {
        if (errFile.length() > 0) {
            try {
                logError(new FileReader(errFile).readFileToString());
            } catch (IOException ex) {
                onErrorFileReadError(errFile.toString(), errCode);
            }
            if ((errCode == 0) && (Trier.getDefaultDebugLevel() != Trier.SILENT)) {
                member.setMustAnalyze(false);
                setSuccessCount(getSuccessCount() + 1);
                uploadResults(member, false);
            } else if (cli.generatedWarnings()) {
                member.setMustAnalyze(false);
                setSuccessCount(getSuccessCount() + 1);
                uploadResults(member, true);
            } else {
                deleteChkFileOnError(member);
                uploadFailure(member);
            }
        } else if (errCode != 0) {
            deleteChkFileOnError(member);
            onAnalyzerError(member.toString(), errCode, errFile);
            uploadFailure(member);
        } else {
            member.setMustAnalyze(false);
            setSuccessCount(getSuccessCount() + 1);
            uploadResults(member, false);
        }
    }

    /**
   * Does nothing. See Analyzer class in the enterprise edition.
   * @param member
   * @param incomplete
   */
    protected void uploadResults(ProjectMember member, boolean incomplete) {
    }

    /**
   * Does nothing. See Analyzer class in the enterprise edition.
   * @param member
   */
    protected void uploadFailure(ProjectMember member) {
    }

    protected void onErrorFileReadError(String sourceFile, int errorCode) {
        final String pattern = resources.getString("MESSAGE_046");
        final String messages = resources.getString("MESSAGE_047");
        final String errors = resources.getString("MESSAGE_048");
        logError(resources.format(pattern, new Object[] { sourceFile, ((errorCode == 0) ? messages : errors) }));
    }

    protected void onAnalyzerError(String sourceFile, int errorCode, File errFile) {
        final String msg = "When analyzing '" + sourceFile + "' the analyzer returned error code " + errorCode + ".";
        if (errFile != null) {
            try {
                final FileOutputStream out = new FileOutputStream(errFile);
                out.write(msg.getBytes());
                out.close();
            } catch (IOException ex) {
                new DebugWriter().writeException(ex, this);
            }
        }
        logError(msg);
    }

    protected void logError(String errorMsg) {
        final String header = resources.format(resources.getString("MESSAGE_051"), new Object[] { getDate().toString() });
        if (!ANALYZER_LOOP_CANCELED) {
            if (errorWriter != null) {
                if (mustAddLogHeader()) {
                    errorWriter.println(SEPLINE);
                    errorWriter.println(header);
                    errorWriter.println(SEPLINE);
                    setAddLogHeader(false);
                } else {
                    errorWriter.println("");
                }
                errorWriter.println(errorMsg);
            }
        }
    }

    /**
   * In case an error occurs during analysis, this method removes the .chk file
   * if it has length 0. Leaving the .chk file would indicate that there are no
   * observations to be reported.
   *
   * @param member the file that was analyzed
   */
    protected void deleteChkFileOnError(ProjectMember member) {
        final File chkFile = new File(member.createOutputFileName(outputPath, "chk"));
        if (chkFile.exists() && (chkFile.length() == 0)) {
            Utilities.discardBooleanResult(chkFile.delete());
        }
    }

    /**
   * Returns <i>class_name</i>@<i>object_hashcode</i>.
   *
   * @return the string
   */
    public String toString() {
        return (getClass().getName() + "@" + Integer.toHexString(hashCode()));
    }

    private synchronized void setSuccessCount(int newSuccessCount) {
        successCount = newSuccessCount;
    }

    /**
   * Gets the successCount value.
   *
   * @return the value
   */
    public int getSuccessCount() {
        return successCount;
    }

    private synchronized void setDate(Date newDate) {
        date = newDate;
    }

    private Date getDate() {
        return date;
    }

    /**
   * Set if a log header should be added to the error log.
   * @param newAddLogHeader
   */
    public synchronized void setAddLogHeader(boolean newAddLogHeader) {
        addLogHeader = newAddLogHeader;
    }

    private boolean mustAddLogHeader() {
        return addLogHeader;
    }

    /**
   * Sets the Java source version.  The default is <tt>null</tt>.
   *
   * @param version the Java source version
   */
    public synchronized void setJavaVersion(String version) {
        javaVersion = version;
    }

    /**
   * Gets the Java source version.
   *
   * @return the Java source version or <tt>null</tt> is none is set
   */
    public String getJavaVersion() {
        return (javaVersion);
    }

    /**
   * Sets the class path. The default is <tt>null</tt>.
     /**
   * Sets the source path.  The default is <tt>null</tt>.
   *
   * @param newSourcePath the source path
   */
    public synchronized void setSourcePath(String[] newSourcePath) {
        sourcePath = newSourcePath;
    }

    /**
   * Gets the source path.
   *
   * @return the source path or <tt>null</tt> is none is set
   */
    public String[] getSourcePath() {
        return sourcePath;
    }

    /**
   * Sets the class path. The default is <tt>null</tt>.
   *
   * @param newClassPath the class path
   */
    public synchronized void setClassPath(String[] newClassPath) {
        classPath = newClassPath;
    }

    /**
   * Gets the class path.
   *
   * @return the class path or <tt>null</tt> is none is set
   */
    public String[] getClassPath() {
        return classPath;
    }

    /**
   * Sets whether or not annotated source code is generated. The default is
   * <tt>false</tt>.
   *
   * @param newAnnotateSource <tt>true</tt> means annotated source code will be
   *                          generated
   */
    public synchronized void setAnnotateSource(boolean newAnnotateSource) {
        annotateSource = newAnnotateSource;
    }

    /**
   * Gets whether or not annotated source code is generated.
   *
   * @return <tt>true</tt> if annotated source will be generated
   */
    public boolean isAnnotateSource() {
        return annotateSource;
    }

    /**
   * Sets the writer used for error logging.
   *
   * @param writer the error print writer
   */
    public synchronized void setErrorWriter(PrintWriter writer) {
        errorWriter = writer;
    }

    /**
   * Sets the rules set. Makes a clone because the output file
   * will be changed.
   *
   * @param newRules the rules
   */
    public synchronized void setRules(RuleSet newRules) {
        try {
            rules = (RuleSet) newRules.clone();
        } catch (CloneNotSupportedException exc) {
            rules = newRules;
        }
    }

    /**
   * Gets the rules set.
   *
   * @return the rules
   */
    public RuleSet getRules() {
        return rules;
    }

    private synchronized void setThread(Thread newThread) {
        thread = newThread;
    }

    /**
   * @return the analyzer thread
   */
    public Thread getThread() {
        return thread;
    }

    /**
   * Sets the outputPath value.
   *
   * @param newOutputPath the value
   */
    public synchronized void setOutputPath(File newOutputPath) {
        outputPath = null;
        if (newOutputPath != null) {
            outputPath = newOutputPath.getAbsolutePath();
        }
    }

    /**
   * User selected "cancel" in the progressmonitor dialog.
   */
    public void propertyChange(PropertyChangeEvent event) {
        if (AnalyzerProgressMonitor.PROPERTY_MONITOR_CANCELED.equals(event.getPropertyName())) {
            stop();
        }
    }

    private synchronized void setAnalyzerLoopCanceled(boolean b) {
        ANALYZER_LOOP_CANCELED = b;
    }

    /**
   * Stop the analyzer. The analyzer will continue untill the file
   * currently being analyzed is finished. No new analysis is started.
   */
    public void stop() {
        setAnalyzerLoopCanceled(true);
    }

    /**
   * Sets the paths of the analyzer with the properties of
   * the given project
   *
   * @param project the project whose paths should be taken
   */
    public void setProperties(Project project) {
        if (project != null) {
            final Vector classPath = project.getClassPath();
            final Vector sourcePath = project.getSourcePath();
            setClassPath((String[]) classPath.toArray(new String[classPath.size()]));
            setSourcePath((String[]) sourcePath.toArray(new String[sourcePath.size()]));
            setOutputPath(project.getOutputPath());
            setJavaVersion(project.getJavaVersion());
        }
    }
}
