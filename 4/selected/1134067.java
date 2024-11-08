package protoj.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSet.Filter;
import protoj.core.internal.AntTarget;
import protoj.core.internal.CoreProject;

/**
 * This feature can extract resources on the classpath and place them in the
 * destination directory with the same relative path. En route variable
 * replacements can be applied to ${var} variables inside the resources where a
 * Configuration instance has been supplied with a call to
 * {@link #initReplacements(Configuration)}.
 * <p>
 * Useful when a third-party component can only work with a filing system
 * resource whereas the resource logically belongs in the jar or in the classes
 * directory. The {@link RetrieveFeature} that delegates to ivy is a good
 * example of this since ivy can only work with files on the filing system and
 * not input streams obtained from system resources.
 * 
 * @author Ashley Williams
 * 
 */
public final class ResourceFeature {

    /**
	 * The owning parent.
	 */
    private CoreProject parent;

    /**
	 * The initial resource extraction location.
	 */
    private File workingDir;

    /**
	 * Contains the replacement properties, may be null.
	 */
    private Configuration replacements;

    /**
	 * Creates an instance with the owning parent.
	 * 
	 * @param parent
	 */
    public ResourceFeature(CoreProject parent) {
        this.parent = parent;
        this.workingDir = createWorkingDir(parent);
    }

    /**
	 * If variable replacement is required then this method should be called to
	 * specify the set of properties to be used during creation of the extracted
	 * resource.
	 * 
	 * @param replacements
	 */
    public void initReplacements(Configuration replacements) {
        this.replacements = replacements;
    }

    /**
	 * Returns a reference to the directory used as temp storage.
	 * 
	 * @param parent
	 * @return
	 */
    private File createWorkingDir(CoreProject parent) {
        File targetDir = parent.getLayout().getTargetDir();
        File workingDir = new File(targetDir, "resource-feature");
        return workingDir;
    }

    /**
	 * Extracts the resource on the classpath with the specified name to the
	 * specified directory. If a replacements has been specified then it is used
	 * to replace any ${var} variables in the resource.
	 * 
	 * @param resourceName
	 * @return
	 */
    public File extractToDir(String resourceName, File destDir) {
        File destFile;
        if (replacements == null) {
            destFile = extractResource(resourceName, destDir);
        } else {
            workingDir.mkdirs();
            destFile = new File(destDir, resourceName);
            File workingFile = extractResource(resourceName, workingDir);
            moveAndResolve(workingFile, destFile);
        }
        return destFile;
    }

    /**
	 * Extracts the classpath resource with the specified name to the specified
	 * destination directory.
	 * 
	 * @param resourceName
	 * @param destDir
	 * @return
	 */
    private File extractResource(String resourceName, File destDir) {
        File file = new File(destDir, resourceName);
        InputStream in = getClass().getResourceAsStream(resourceName);
        try {
            FileOutputStream out = FileUtils.openOutputStream(file);
            try {
                IOUtils.copy(in, out);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return file;
    }

    /**
	 * Moves the given src file to the dest file applying the replacements if
	 * there is any. The replacements contains the list of properties to be used
	 * as replacements for variables in the src file.
	 * 
	 * @param src
	 * @param dest
	 */
    @SuppressWarnings("unchecked")
    private void moveAndResolve(File src, File dest) {
        AntTarget target = new AntTarget("resource-feature");
        target.initLogging(parent.getLayout().getLogFile(), Project.MSG_INFO);
        Move move = new Move();
        target.addTask(move);
        move.setTaskName("move-and-resolve");
        move.setFile(src);
        move.setTofile(dest);
        move.setOverwrite(true);
        FilterSet filterSet = move.createFilterSet();
        filterSet.setBeginToken("${");
        filterSet.setEndToken("}");
        Iterator<String> keys = replacements.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = replacements.getList(key).get(0).toString();
            Filter filter = new Filter();
            filterSet.addFilter(filter);
            filter.setToken(key);
            filter.setValue(value);
        }
        target.execute();
    }
}
