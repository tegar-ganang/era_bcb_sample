package org.eclipse.emf.ant.taskdefs;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.emf.ant.util.Util;

/**
 * <p>
 * Removes the version of all the subdirectories of a given directory.  See
 * {@link Util#removeVersion(File)} for further details.
 * This task may be useful when generating code for which the defined model (a
 * Rose file for example) depends on directory names to find its dependencies.
 * </p>
 * <p>
 * If this task is executed by a Eclipse driver with the <b>org.eclipse.emf.ant</b> 
 * plugin, it is neither necessary to use Ant's task <tt>TaskDef</tt> to declare this 
 * task in a script nor to change the Ant's runtime classpath.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * &lt;emf.util.RemoveVersion parentDir=&quot;c:\eclipse\plugins&quot;/&gt;
 * </pre>
 * 
 * @since 2.1.0
 */
public class RemoveVersionTask extends Task {

    public static void assertTrue(String message, boolean expression) throws BuildException {
        if (!expression) {
            throw new BuildException(message);
        }
    }

    private File parentDir;

    public void setParentDir(File dir) {
        parentDir = dir;
    }

    protected void checkAttributes() throws BuildException {
        assertTrue("The attribute 'parentDir' must indicate a valid directory.", parentDir != null && parentDir.isDirectory());
        assertTrue("You must have read and write access to " + parentDir.getAbsolutePath() + ".", parentDir.canRead() && parentDir.canWrite());
    }

    @Override
    public void execute() throws BuildException {
        checkAttributes();
        Util.removeVersion(parentDir);
    }
}
