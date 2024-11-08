package protoj.lang;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import protoj.lang.StandardProjectComponentPolicy.StandardProjectComponent;
import protoj.lang.dependency.RetrieveFeature;

/**
 * This feature can extract resources on the classpath and place them in the
 * destination directory with the same relative path. En route the velocity
 * engine intervenes and processes any velocity markup that may be present in
 * the resource.
 * <p>
 * Useful also when a third-party component can only work with a filing system
 * resource whereas the resource logically belongs in the jar or in the classes
 * directory. The {@link RetrieveFeature} that delegates to ivy is a good
 * example of this since ivy can only work with files on the filing system and
 * not input streams obtained from system resources. The ivy file can therefore
 * get hold of project information from the java code via the instances added to
 * the velocity context rather than hardcoding them.
 * <p>
 * Note that the velocity engine is configured with a class resource loader so
 * that resources can be found on the classpath. Additionally it is configured
 * with a file resource loader that points to the project root. Therefore any
 * include directives in resource files should specify paths relative to the
 * project root directory.
 * 
 * @author Ashley Williams
 * 
 */
@StandardProjectComponent
public final class ResourceFeature {

    /**
	 * Velocity implementation.
	 */
    private VelocityEngine velocityEngine;

    /**
	 * The velocity context that is merged with all templates.
	 */
    private VelocityContext context;

    /**
	 * This is the single velocity context that is merged with all templates.
	 * Simply add instances to it, with a call to
	 * <code>getContext().put(String, Object)</code>, in order to make those
	 * instances callable from your resource.
	 * <p>
	 * The {@link StandardProject} instance is added by default with the key
	 * "project". Therefore you can include snippets of text such as
	 * ${project.layout.rootPath} that will be "magically" replaced each time
	 * the extractXXX methods are called. Additionally the
	 * {@link StandardProject#getLayout()} instance is added with the key
	 * "layout". Finally a variable is added called "D" that can be used to
	 * escape the dollar character where it is used for other purposes that may
	 * trip up the velocity parser.
	 * 
	 * @return
	 */
    public VelocityContext getContext() {
        if (context == null) {
            context = new VelocityContext();
            getContext().put("project", getParent());
            getContext().put("layout", getParent().getLayout());
            getContext().put("D", "$");
        }
        return context;
    }

    /**
	 * Extracts the resource on the classpath with the specified name to the
	 * specified directory. If a replacements has been specified then it is used
	 * to replace any ${var} variables in the resource. Be careful not to call
	 * with a binary file, use {@link #extractResourceToDir(String, File)}
	 * instead.
	 * <p>
	 * For example if resourcePath is "/acme/somefile.txt" and destDir is
	 * "/some/dir/" then the resultant file will be
	 * "some/dir/acme/somefile.txt".
	 * 
	 * @param resourcePath
	 * @param destDir
	 * @return
	 */
    public File filterResourceToDir(String resourcePath, File destDir) {
        File destFile = new File(destDir, resourcePath);
        return copyResourceToFile(resourcePath, destFile, true);
    }

    /**
	 * The same as {@link #filterResourceToDir(String, File)} except no
	 * filtering is applied. This is recommended for binary files.
	 * 
	 * @param resourcePath
	 * @param destDir
	 * @return
	 */
    public File extractResourceToDir(String resourcePath, File destDir) {
        File destFile = new File(destDir, resourcePath);
        return copyResourceToFile(resourcePath, destFile, false);
    }

    /**
	 * This is ideal for extracting resources where the resource directory
	 * structure is the same as the filing system directory structure. The
	 * resource to copy is calculated to be "resourceRoot" + "/" + relativePath.
	 * <p>
	 * For example if resourceRoot is "/acme" and relativePath is
	 * "some/dir/somefile.txt" and destDir is "/foo" then the resource being
	 * copied is "/acme/some/dir/somefile.txt" and the destination file is
	 * "/foo/some/dir/somefile.txt".
	 * 
	 * @param resourceRoot
	 * @param relativePath
	 * @param destDir
	 * @return
	 */
    public File filterResourceToDir(String resourceRoot, String relativePath, File destDir) {
        String resourcePath = resourceRoot + "/" + relativePath;
        File destFile = new File(destDir, relativePath);
        return copyResourceToFile(resourcePath, destFile, true);
    }

    /**
	 * The same as {@link #filterResourceToDir(String, String, File)} except no
	 * filtering is applied. This is recommended for binary files.
	 * 
	 * @param resourceRoot
	 * @param relativePath
	 * @param destDir
	 * @return
	 */
    public File extractResourceToDir(String resourceRoot, String relativePath, File destDir) {
        String resourcePath = resourceRoot + "/" + relativePath;
        File destFile = new File(destDir, relativePath);
        return copyResourceToFile(resourcePath, destFile, false);
    }

    /**
	 * Copies the given resourcePath contents to the given destFile. The filter
	 * parameter is used to determine whether or not filtering is applied.
	 * anyway.
	 * 
	 * @param resourcePath
	 * @param destFile
	 * @param filter
	 * @return
	 */
    public File copyResourceToFile(String resourcePath, File destFile, boolean filter) {
        if (filter) {
            filterResourceToFile(resourcePath, destFile);
        } else {
            extractResourceToFile(resourcePath, destFile);
        }
        return destFile;
    }

    /**
	 * Extracts the classpath resource with the specified name to the specified
	 * destination file. No replacements are applied, so this is ideal for
	 * binary files.
	 * 
	 * @param resourcePath
	 *            for example "/somefile.txt"
	 * @param dest
	 */
    public void extractResourceToFile(String resourcePath, File dest) {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        try {
            FileOutputStream out = FileUtils.openOutputStream(dest);
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
    }

    /**
	 * Copies the specified resource to the specified dest. The resource can be
	 * a velocity template and will therefore be able to use objects added to
	 * the velocity context.
	 * 
	 * @param resourcePath
	 * @param dest
	 */
    public void filterResourceToFile(String resourcePath, File dest) {
        VelocityEngine ve = getVelocityEngine();
        Template t = ve.getTemplate(resourcePath);
        File destParent = dest.getParentFile();
        if (!destParent.exists()) {
            destParent.mkdirs();
        }
        dest.delete();
        dest.createNewFile();
        FileWriter writer = new FileWriter(dest);
        t.merge(getContext(), writer);
        writer.close();
    }

    /**
	 * Copies the specified resource to a returned string. The resource can be a
	 * velocity template and will therefore be able to use objects added to the
	 * velocity context.
	 * 
	 * @param resourcePath
	 * @return
	 */
    public String filterResourceToString(String resourcePath) {
        VelocityEngine ve = getVelocityEngine();
        Template t = ve.getTemplate(resourcePath);
        StringWriter writer = new StringWriter();
        t.merge(getContext(), writer);
        writer.close();
        return writer.toString();
    }

    /**
	 * Lazy getter for the velocity engine as a helper for
	 * {@link #filterResourceToFile(String, File)}.
	 * 
	 * @return
	 */
    private VelocityEngine getVelocityEngine() {
        if (velocityEngine == null) {
            velocityEngine = new VelocityEngine();
            Properties props = new Properties();
            props.setProperty("resource.loader", "class, file");
            props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            props.setProperty("file.resource.loader.path", getParent().getLayout().getRootPath());
            props.setProperty("file.resource.loader.cache", "false");
            props.setProperty("file.resource.loader.modificationCheckInterval", "2");
            velocityEngine.init(props);
        }
        return velocityEngine;
    }
}
