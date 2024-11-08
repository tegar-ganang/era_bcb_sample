package net.sourceforge.coberclipse.builders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.sourceforge.coberclipse.CoberclipseLog;
import net.sourceforge.coberclipse.CoberclipsePlugin;
import net.sourceforge.coberclipse.CoverageTestListener;
import net.sourceforge.coberclipse.util.FileUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;

/**
 * The class instrumentor is implemented as a builder. It generates
 * the classes and instruments them. An TestRunListener is attached to
 * it so that it's notified once the test is ended. 
 * 
 * @author Guang Zhou
 *
 */
public class ClassInstrumentor extends IncrementalProjectBuilder {

    public static String DATA_FILE = "cobertura.ser";

    public static String ORIGINAL_DATA_FILE = "original_cobertura.ser";

    private static String INSTRUMENTED_PATH = "instrumented-classes";

    public static final String BUILDER_ID = CoberclipsePlugin.PLUGIN_ID + ".classInstrumentor";

    public static final String REPORTING_PATH = "reporting";

    private static final TestRunListener listener = new CoverageTestListener();

    /**
     * Add the testRunListener.
     */
    public ClassInstrumentor() {
        JUnitCore.removeTestRunListener(listener);
        JUnitCore.addTestRunListener(listener);
    }

    /**
     * When called by Eclipse, this builder should perform an audit as
     * necessary. If the build kind is <code>INCREMENTAL_BUILD</code> or
     * <code>AUTO_BUILD</code>, the <code>getDelta</code> method can be
     * used during the invocation of this method to obtain information about
     * what changes have occurred since the last invocation of this method.
     * After completing a build, this builder may return a list of projects for
     * which it requires a resource delta the next time it is run.
     * 
     * @param kind
     *                the kind of build being requested. Valid values are
     *                <ul>
     *                <li><code>FULL_BUILD</code>- indicates a full build.</li>
     *                <li><code>INCREMENTAL_BUILD</code>- indicates an
     *                incremental build. </li>
     *                <li><code>AUTO_BUILD</code>- indicates an
     *                automatically triggered incremental build (autobuilding
     *                on).</li>
     *                </ul>
     * @param args
     *                a table of builder-specific arguments keyed by argument
     *                name (key type: <code>String</code>, value type:
     *                <code>String</code>); <code>null</code> is equivalent
     *                to an empty map
     * @param monitor
     *                a progress monitor, or <code>null</code> if progress
     *                reporting and cancellation are not desired
     * @return the list of projects for which this builder would like deltas the
     *         next time it is run or <code>null</code> if none
     * @exception CoreException
     *                    if this build fails.
     * @see IProject#build(int, String, Map, IProgressMonitor)
     */
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        IProject p = getProject();
        File dataFile = new File(p.getLocation().toString() + File.separator + ORIGINAL_DATA_FILE);
        if (dataFile.exists()) {
            dataFile.delete();
        }
        IFolder instrumentedDir = p.getFolder(INSTRUMENTED_PATH);
        if (instrumentedDir == null || !instrumentedDir.exists()) {
            instrumentedDir.create(true, true, null);
        }
        System.setProperty("net.sourceforge.cobertura.datafile", dataFile.getAbsolutePath());
        System.setProperty("net.sourceforge.cobertura.instrumentedDir", instrumentedDir.getLocation().toString());
        if (kind == FULL_BUILD) {
            fullBuild();
        } else if (kind == AUTO_BUILD) {
            fullBuild();
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) fullBuild(); else incrementalBuild(delta);
        }
        File destdataFile = new File(p.getLocation().toString() + File.separator + DATA_FILE);
        try {
            FileUtils.copyFile(dataFile, destdataFile);
        } catch (IOException e) {
            CoberclipseLog.error(e);
        }
        return null;
    }

    protected void fullBuild() throws CoreException {
        try {
            getProject().accept(new ResourceVisitor(), IResource.NONE);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return;
    }

    protected void incrementalBuild(IResourceDelta delta) throws CoreException {
        delta.accept(new DeltaResourceVisitor());
    }

    /**
     * Add this builder to the specified project if possible. Do nothing if the
     * builder has already been added.
     * 
     * @param project
     *                the project (not <code>null</code>)
     */
    public static void addBuilderToProject(IProject project) {
        if (!project.isOpen()) return;
        IProjectDescription description;
        try {
            description = project.getDescription();
        } catch (CoreException e) {
            CoberclipseLog.error(e);
            return;
        }
        ICommand[] cmds = description.getBuildSpec();
        for (int j = 0; j < cmds.length; j++) if (cmds[j].getBuilderName().equals(BUILDER_ID)) return;
        ICommand newCmd = description.newCommand();
        newCmd.setBuilderName(BUILDER_ID);
        List<ICommand> newCmds = new ArrayList<ICommand>();
        newCmds.addAll(Arrays.asList(cmds));
        newCmds.add(newCmd);
        description.setBuildSpec((ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
        try {
            project.setDescription(description, null);
        } catch (Throwable e) {
            CoberclipseLog.error(e);
        }
    }

    /**
     * Remove this builder from the specified project if possible. Do nothing if
     * the builder has already been removed.
     * 
     * @param project
     *                the project (not <code>null</code>)
     */
    public static void removeBuilderFromProject(IProject project) {
        if (!project.isOpen()) return;
        IProjectDescription description;
        try {
            description = project.getDescription();
        } catch (CoreException e) {
            CoberclipseLog.error(e);
            return;
        }
        int index = -1;
        ICommand[] cmds = description.getBuildSpec();
        for (int j = 0; j < cmds.length; j++) {
            if (cmds[j].getBuilderName().equals(BUILDER_ID)) {
                index = j;
                break;
            }
        }
        if (index == -1) return;
        List<ICommand> newCmds = new ArrayList<ICommand>();
        newCmds.addAll(Arrays.asList(cmds));
        newCmds.remove(index);
        description.setBuildSpec((ICommand[]) newCmds.toArray(new ICommand[newCmds.size()]));
        try {
            project.setDescription(description, null);
        } catch (CoreException e) {
            CoberclipseLog.error(e);
        }
    }
}
