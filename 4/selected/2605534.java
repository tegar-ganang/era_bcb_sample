package protoj.lang;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.taskdefs.Chmod;
import protoj.core.ProjectLayout;
import protoj.core.internal.AntTarget;

/**
 * Responsible for creating a sample project on the filing system.
 * 
 * @author Ashley Williams
 * 
 */
public final class SampleProjectFeature {

    /**
	 * A reference to the sample project layout.
	 */
    private ProjectLayout layout;

    /**
	 * Creates the simple project under the specified parentDir. This is a
	 * pass-thru method to {@link #createProject(File, String, File, File...)}.
	 * 
	 * @param parentDir
	 * @param jarFile
	 */
    public void createSimpleProject(File parentDir, File jarFile) {
        createProject(parentDir, "simple", jarFile, "bin/simple.sh", "bin/simple.bat", "src/resources/simple/ivy.xml", "conf/dev.properties", "README.txt", "NOTICE.txt", "src/java/org/simple/core/SimpleCore.java", "src/java/org/simple/system/SimpleProject.java");
    }

    /**
	 * Creates the ajc project with the specified rootDir. This is a pass-thru
	 * method to {@link #createProject(File, String, File, File...)}.
	 * 
	 * @param parentDir
	 * @param jarFile
	 */
    public void createAjcProject(File parentDir, File jarFile) {
        createProject(parentDir, "ajc", jarFile, "bin/ajc.sh", "bin/ajc.bat", "src/resources/ajc/ivy.xml", "src/resources/META-INF/aop.xml", "conf/dev.properties", "README.txt", "NOTICE.txt", "src/java/org/ajc/core/AjcCore.java", "src/java/org/ajc/system/AjcProject.java", "src/java/org/ajc/system/AjcTrace.aj", "src/java/org/ajc/system/SoftException.aj");
        System.out.println("Please place a copy of version 1.6.3 aspectjweaver.jar, aspectjrt.jar and aspectjtools.jar in the");
        System.out.println("lib directory since due to aspectj restrictions these aren't included in the ProtoJ no dependencies jar.");
        System.out.println("Use the protoj jar file with no included dependencies for better control over specific versions.");
    }

    /**
	 * Creates a project under the specified parent with the specified
	 * projectName and resource file. The specified jarFile will be used to copy
	 * into the lib directory and will typically be either the minimal protoj
	 * jar file or the no dependencies jar file.
	 * 
	 * @param parentDir
	 *            the directory where the project is to be extracted to
	 * @param projectName
	 *            the name of the project under the root directory and the name
	 *            of the resources parent containing the files to be extracted
	 * @param jarFile
	 *            the jar file that will be placed in the project lib directory,
	 *            usually the protoj jar file that is executing this method
	 */
    private void createProject(File parentDir, String projectName, File jarFile, String... relResourceNames) {
        File rootDir = new File(parentDir, projectName);
        StandardProject project = new StandardProject(rootDir, "projectName", null);
        this.layout = project.getLayout();
        layout.createPhysicalLayout();
        copyJarFile(jarFile);
        for (String relResourceName : relResourceNames) {
            copyResource(relResourceName);
        }
        relaxPermissions();
        System.out.println("created project at " + layout.getRootDir().getCanonicalPath());
    }

    /**
	 * Copies the file under the /protoj resource path to the project root
	 * directory on the filing system at the matching relative location.
	 * 
	 * @param name
	 *            the name of the resource relative to the resource root
	 */
    private void copyResource(String relResourceName) {
        String projectName = layout.getRootDir().getName();
        String resourceName = "/protoj/" + projectName + "/" + relResourceName;
        InputStream source = getClass().getResourceAsStream(resourceName);
        if (source == null) {
            throw new RuntimeException("couldn't find resource " + resourceName);
        }
        File dest = new File(layout.getRootDir(), relResourceName);
        dest.getParentFile().mkdirs();
        dest.createNewFile();
        InputStreamReader in = new InputStreamReader(source);
        try {
            FileOutputStream out = new FileOutputStream(dest);
            try {
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(out);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
	 * Copies the jar file into the sample lib directory. The file is stripped
	 * of the version string so that the calling the sample shell script won't
	 * become outdated.
	 * 
	 * @param jarFile
	 */
    private void copyJarFile(File jarFile) {
        File destFile = new File(layout.getLibDir(), "protoj.jar");
        FileUtils.copyFile(jarFile, destFile);
    }

    /**
	 * Apply 777 permissions all the way down.
	 */
    private void relaxPermissions() {
        AntTarget target = new AntTarget("relax-permissions");
        Chmod chmod = new Chmod();
        chmod.setTaskName("sample-permissions");
        target.addTask(chmod);
        chmod.setDir(layout.getRootDir().getParentFile());
        chmod.setIncludes(layout.getRootName() + "/**/*.*");
        chmod.setPerm("777");
        target.execute();
    }
}
