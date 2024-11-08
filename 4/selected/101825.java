package com.android.sdklib.internal.project;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;
import com.android.sdklib.xml.AndroidManifest;
import com.android.sdklib.xml.AndroidXPathFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Creates the basic files needed to get an Android project up and running.
 *
 * @hide
 */
public class ProjectCreator {

    /** Package path substitution string used in template files, i.e. "PACKAGE_PATH" */
    private static final String PH_JAVA_FOLDER = "PACKAGE_PATH";

    /** Package name substitution string used in template files, i.e. "PACKAGE" */
    private static final String PH_PACKAGE = "PACKAGE";

    /** Activity name substitution string used in template files, i.e. "ACTIVITY_NAME".
     * @deprecated This is only used for older templates. For new ones see
     * {@link #PH_ACTIVITY_ENTRY_NAME}, and {@link #PH_ACTIVITY_CLASS_NAME}. */
    private static final String PH_ACTIVITY_NAME = "ACTIVITY_NAME";

    /** Activity name substitution string used in manifest templates, i.e. "ACTIVITY_ENTRY_NAME".*/
    private static final String PH_ACTIVITY_ENTRY_NAME = "ACTIVITY_ENTRY_NAME";

    /** Activity name substitution string used in class templates, i.e. "ACTIVITY_CLASS_NAME".*/
    private static final String PH_ACTIVITY_CLASS_NAME = "ACTIVITY_CLASS_NAME";

    /** Activity FQ-name substitution string used in class templates, i.e. "ACTIVITY_FQ_NAME".*/
    private static final String PH_ACTIVITY_FQ_NAME = "ACTIVITY_FQ_NAME";

    /** Original Activity class name substitution string used in class templates, i.e.
     * "ACTIVITY_TESTED_CLASS_NAME".*/
    private static final String PH_ACTIVITY_TESTED_CLASS_NAME = "ACTIVITY_TESTED_CLASS_NAME";

    /** Project name substitution string used in template files, i.e. "PROJECT_NAME". */
    private static final String PH_PROJECT_NAME = "PROJECT_NAME";

    /** Application icon substitution string used in the manifest template */
    private static final String PH_ICON = "ICON";

    /** Pattern for characters accepted in a project name. Since this will be used as a
     * directory name, we're being a bit conservative on purpose: dot and space cannot be used. */
    public static final Pattern RE_PROJECT_NAME = Pattern.compile("[a-zA-Z0-9_]+");

    /** List of valid characters for a project name. Used for display purposes. */
    public static final String CHARS_PROJECT_NAME = "a-z A-Z 0-9 _";

    /** Pattern for characters accepted in a package name. A package is list of Java identifier
     * separated by a dot. We need to have at least one dot (e.g. a two-level package name).
     * A Java identifier cannot start by a digit. */
    public static final Pattern RE_PACKAGE_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)+");

    /** List of valid characters for a project name. Used for display purposes. */
    public static final String CHARS_PACKAGE_NAME = "a-z A-Z 0-9 _";

    /** Pattern for characters accepted in an activity name, which is a Java identifier. */
    public static final Pattern RE_ACTIVITY_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /** List of valid characters for a project name. Used for display purposes. */
    public static final String CHARS_ACTIVITY_NAME = "a-z A-Z 0-9 _";

    public enum OutputLevel {

        /** Silent mode. Project creation will only display errors. */
        SILENT, /** Normal mode. Project creation will display what's being done, display
         * error but not warnings. */
        NORMAL, /** Verbose mode. Project creation will display what's being done, errors and warnings. */
        VERBOSE
    }

    /**
     * Exception thrown when a project creation fails, typically because a template
     * file cannot be written.
     */
    private static class ProjectCreateException extends Exception {

        /** default UID. This will not be serialized anyway. */
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        ProjectCreateException(String message) {
            super(message);
        }

        ProjectCreateException(Throwable t, String format, Object... args) {
            super(format != null ? String.format(format, args) : format, t);
        }

        ProjectCreateException(String format, Object... args) {
            super(String.format(format, args));
        }
    }

    private final OutputLevel mLevel;

    private final ISdkLog mLog;

    private final String mSdkFolder;

    public ProjectCreator(String sdkFolder, OutputLevel level, ISdkLog log) {
        mSdkFolder = sdkFolder;
        mLevel = level;
        mLog = log;
    }

    /**
     * Creates a new project.
     * <p/>
     * The caller should have already checked and sanitized the parameters.
     *
     * @param folderPath the folder of the project to create.
     * @param projectName the name of the project. The name must match the
     *          {@link #RE_PROJECT_NAME} regex.
     * @param packageName the package of the project. The name must match the
     *          {@link #RE_PACKAGE_NAME} regex.
     * @param activityEntry the activity of the project as it will appear in the manifest. Can be
     *          null if no activity should be created. The name must match the
     *          {@link #RE_ACTIVITY_NAME} regex.
     * @param target the project target.
     * @param pathToMainProject if non-null the project will be setup to test a main project
     * located at the given path.
     */
    public void createProject(String folderPath, String projectName, String packageName, String activityEntry, IAndroidTarget target, String pathToMainProject) {
        File projectFolder = new File(folderPath);
        if (!projectFolder.exists()) {
            boolean created = false;
            Throwable t = null;
            try {
                created = projectFolder.mkdirs();
            } catch (Exception e) {
                t = e;
            }
            if (created) {
                println("Created project directory: %1$s", projectFolder);
            } else {
                mLog.error(t, "Could not create directory: %1$s", projectFolder);
                return;
            }
        } else {
            Exception e = null;
            String error = null;
            try {
                String[] content = projectFolder.list();
                if (content == null) {
                    error = "Project folder '%1$s' is not a directory.";
                } else if (content.length != 0) {
                    error = "Project folder '%1$s' is not empty. Please consider using '%2$s update' instead.";
                }
            } catch (Exception e1) {
                e = e1;
            }
            if (e != null || error != null) {
                mLog.error(e, error, projectFolder, SdkConstants.androidCmdName());
            }
        }
        try {
            boolean isTestProject = pathToMainProject != null;
            ProjectProperties localProperties = ProjectProperties.create(folderPath, PropertyType.LOCAL);
            localProperties.setProperty(ProjectProperties.PROPERTY_SDK, mSdkFolder);
            localProperties.save();
            ProjectProperties defaultProperties = ProjectProperties.create(folderPath, PropertyType.DEFAULT);
            defaultProperties.setAndroidTarget(target);
            defaultProperties.save();
            ProjectProperties buildProperties = ProjectProperties.create(folderPath, PropertyType.BUILD);
            if (target.getVersion().getApiLevel() < 4) {
                buildProperties.setProperty(ProjectProperties.PROPERTY_APP_PACKAGE, packageName);
            }
            if (isTestProject) {
                buildProperties.setProperty(ProjectProperties.PROPERTY_TESTED_PROJECT, pathToMainProject);
            }
            buildProperties.save();
            final HashMap<String, String> keywords = new HashMap<String, String>();
            final String packagePath = stripString(packageName.replace(".", File.separator), File.separatorChar);
            keywords.put(PH_JAVA_FOLDER, packagePath);
            keywords.put(PH_PACKAGE, packageName);
            String fqActivityName = null, activityPath = null, activityClassName = null;
            String originalActivityEntry = activityEntry;
            String originalActivityClassName = null;
            if (activityEntry != null) {
                if (isTestProject) {
                    activityEntry += "Test";
                    int pos = originalActivityEntry.lastIndexOf('.');
                    if (pos != -1) {
                        originalActivityClassName = originalActivityEntry.substring(pos + 1);
                    } else {
                        originalActivityClassName = originalActivityEntry;
                    }
                }
                fqActivityName = AndroidManifest.combinePackageAndClassName(packageName, activityEntry);
                activityPath = stripString(fqActivityName.replace(".", File.separator), File.separatorChar);
                activityPath = activityPath.substring(0, activityPath.lastIndexOf(File.separatorChar));
                activityClassName = fqActivityName.substring(fqActivityName.lastIndexOf('.') + 1);
            }
            if (target.getVersion().getApiLevel() < 4) {
                if (originalActivityEntry != null) {
                    keywords.put(PH_ACTIVITY_NAME, originalActivityEntry);
                }
            } else {
                if (activityEntry != null) {
                    keywords.put(PH_ACTIVITY_ENTRY_NAME, activityEntry);
                    keywords.put(PH_ACTIVITY_CLASS_NAME, activityClassName);
                    keywords.put(PH_ACTIVITY_FQ_NAME, fqActivityName);
                    if (originalActivityClassName != null) {
                        keywords.put(PH_ACTIVITY_TESTED_CLASS_NAME, originalActivityClassName);
                    }
                }
            }
            if (projectName != null) {
                keywords.put(PH_PROJECT_NAME, projectName);
            } else {
                if (activityClassName != null) {
                    keywords.put(PH_PROJECT_NAME, activityClassName);
                } else {
                    projectName = projectFolder.getName();
                    keywords.put(PH_PROJECT_NAME, projectName);
                }
            }
            if (activityClassName != null) {
                String srcActivityFolderPath = SdkConstants.FD_SOURCES + File.separator + activityPath;
                File sourceFolder = createDirs(projectFolder, srcActivityFolderPath);
                String javaTemplate = isTestProject ? "java_tests_file.template" : "java_file.template";
                String activityFileName = activityClassName + ".java";
                installTemplate(javaTemplate, new File(sourceFolder, activityFileName), keywords, target);
            } else {
                createDirs(projectFolder, SdkConstants.FD_SOURCES);
            }
            File resourceFolder = createDirs(projectFolder, SdkConstants.FD_RESOURCES);
            createDirs(projectFolder, SdkConstants.FD_OUTPUT);
            createDirs(projectFolder, SdkConstants.FD_NATIVE_LIBS);
            if (isTestProject == false) {
                File valueFolder = createDirs(resourceFolder, SdkConstants.FD_VALUES);
                installTemplate("strings.template", new File(valueFolder, "strings.xml"), keywords, target);
                File layoutFolder = createDirs(resourceFolder, SdkConstants.FD_LAYOUT);
                installTemplate("layout.template", new File(layoutFolder, "main.xml"), keywords, target);
                if (installIcons(resourceFolder, target)) {
                    keywords.put(PH_ICON, "android:icon=\"@drawable/icon\"");
                } else {
                    keywords.put(PH_ICON, "");
                }
            }
            String manifestTemplate = "AndroidManifest.template";
            if (isTestProject) {
                manifestTemplate = "AndroidManifest.tests.template";
            }
            installTemplate(manifestTemplate, new File(projectFolder, SdkConstants.FN_ANDROID_MANIFEST_XML), keywords, target);
            installTemplate("build.template", new File(projectFolder, SdkConstants.FN_BUILD_XML), keywords);
        } catch (ProjectCreateException e) {
            mLog.error(e, null);
        } catch (IOException e) {
            mLog.error(e, null);
        }
    }

    /**
     * Updates an existing project.
     * <p/>
     * Workflow:
     * <ul>
     * <li> Check AndroidManifest.xml is present (required)
     * <li> Check there's a default.properties with a target *or* --target was specified
     * <li> Update default.prop if --target was specified
     * <li> Refresh/create "sdk" in local.properties
     * <li> Build.xml: create if not present or no <androidinit(\w|/>) in it
     * </ul>
     *
     * @param folderPath the folder of the project to update. This folder must exist.
     * @param target the project target. Can be null.
     * @param projectName The project name from --name. Can be null.
     * @return true if the project was successfully updated.
     */
    public boolean updateProject(String folderPath, IAndroidTarget target, String projectName) {
        File androidManifest = checkProjectFolder(folderPath);
        if (androidManifest == null) {
            return false;
        }
        File projectFolder = androidManifest.getParentFile();
        ProjectProperties props = ProjectProperties.load(folderPath, PropertyType.DEFAULT);
        if (props == null || props.getProperty(ProjectProperties.PROPERTY_TARGET) == null) {
            if (target == null) {
                mLog.error(null, "There is no %1$s file in '%2$s'. Please provide a --target to the '%3$s update' command.", PropertyType.DEFAULT.getFilename(), folderPath, SdkConstants.androidCmdName());
                return false;
            }
        }
        if (target != null) {
            if (props == null) {
                props = ProjectProperties.create(folderPath, PropertyType.DEFAULT);
            }
            props.setAndroidTarget(target);
            try {
                props.save();
                println("Updated %1$s", PropertyType.DEFAULT.getFilename());
            } catch (IOException e) {
                mLog.error(e, "Failed to write %1$s file in '%2$s'", PropertyType.DEFAULT.getFilename(), folderPath);
                return false;
            }
        }
        props = ProjectProperties.load(folderPath, PropertyType.LOCAL);
        if (props == null) {
            props = ProjectProperties.create(folderPath, PropertyType.LOCAL);
        }
        props.setProperty(ProjectProperties.PROPERTY_SDK, mSdkFolder);
        try {
            props.save();
            println("Updated %1$s", PropertyType.LOCAL.getFilename());
        } catch (IOException e) {
            mLog.error(e, "Failed to write %1$s file in '%2$s'", PropertyType.LOCAL.getFilename(), folderPath);
            return false;
        }
        File buildXml = new File(projectFolder, SdkConstants.FN_BUILD_XML);
        boolean needsBuildXml = projectName != null || !buildXml.exists();
        if (!needsBuildXml) {
            needsBuildXml = !checkFileContainsRegexp(buildXml, "classname=\"com.android.ant.SetupTask\"");
        }
        if (!needsBuildXml) {
            needsBuildXml = !checkFileContainsRegexp(buildXml, "<setup(?:\\s|/|$)");
        }
        if (needsBuildXml) {
            if (buildXml.exists()) {
                println("File %1$s is too old and needs to be updated.", SdkConstants.FN_BUILD_XML);
            }
        }
        if (needsBuildXml) {
            final HashMap<String, String> keywords = new HashMap<String, String>();
            if (projectName != null) {
                keywords.put(PH_PROJECT_NAME, projectName);
            } else {
                extractPackageFromManifest(androidManifest, keywords);
                if (keywords.containsKey(PH_ACTIVITY_ENTRY_NAME)) {
                    String activity = keywords.get(PH_ACTIVITY_ENTRY_NAME);
                    int pos = activity.lastIndexOf('.');
                    if (pos != -1) {
                        activity = activity.substring(pos + 1);
                    }
                    keywords.put(PH_PROJECT_NAME, activity);
                } else {
                    projectName = projectFolder.getName();
                    keywords.put(PH_PROJECT_NAME, projectName);
                }
            }
            if (mLevel == OutputLevel.VERBOSE) {
                println("Regenerating %1$s with project name %2$s", SdkConstants.FN_BUILD_XML, keywords.get(PH_PROJECT_NAME));
            }
            try {
                installTemplate("build.template", new File(projectFolder, SdkConstants.FN_BUILD_XML), keywords);
            } catch (ProjectCreateException e) {
                mLog.error(e, null);
                return false;
            }
        }
        return true;
    }

    /**
     * Updates a test project with a new path to the main (tested) project.
     * @param folderPath the path of the test project.
     * @param pathToMainProject the path to the main project, relative to the test project.
     */
    public void updateTestProject(final String folderPath, final String pathToMainProject, final SdkManager sdkManager) {
        if (checkProjectFolder(folderPath) == null) {
            return;
        }
        File mainProject = new File(pathToMainProject);
        String resolvedPath;
        if (mainProject.isAbsolute() == false) {
            mainProject = new File(folderPath, pathToMainProject);
            try {
                resolvedPath = mainProject.getCanonicalPath();
            } catch (IOException e) {
                mLog.error(e, "Unable to resolve path to main project: %1$s", pathToMainProject);
                return;
            }
        } else {
            resolvedPath = mainProject.getAbsolutePath();
        }
        println("Resolved location of main project to: %1$s", resolvedPath);
        if (checkProjectFolder(resolvedPath) == null) {
            mLog.error(null, "No Android Manifest at: %1$s", resolvedPath);
            return;
        }
        ProjectProperties defaultProp = ProjectProperties.load(resolvedPath, PropertyType.DEFAULT);
        if (defaultProp == null) {
            mLog.error(null, "No %1$s at: %2$s", PropertyType.DEFAULT.getFilename(), resolvedPath);
            return;
        }
        String targetHash = defaultProp.getProperty(ProjectProperties.PROPERTY_TARGET);
        if (targetHash == null) {
            mLog.error(null, "%1$s in the main project has no target property.", PropertyType.DEFAULT.getFilename());
            return;
        }
        IAndroidTarget target = sdkManager.getTargetFromHashString(targetHash);
        if (target == null) {
            mLog.error(null, "Main project target %1$s is not a valid target.", targetHash);
            return;
        }
        String projectName = null;
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        File testBuildXml = new File(folderPath, "build.xml");
        if (testBuildXml.isFile()) {
            try {
                projectName = xpath.evaluate("/project/@name", new InputSource(new FileInputStream(testBuildXml)));
            } catch (XPathExpressionException e) {
            } catch (FileNotFoundException e) {
            }
        }
        if (projectName == null) {
            try {
                String mainProjectName = xpath.evaluate("/project/@name", new InputSource(new FileInputStream(new File(resolvedPath, "build.xml"))));
                projectName = mainProjectName + "Test";
            } catch (XPathExpressionException e) {
                mLog.error(e, "Unable to query main project name.");
                return;
            } catch (FileNotFoundException e) {
                mLog.error(e, "Unable to query main project name.");
                return;
            }
        }
        if (updateProject(folderPath, target, projectName) == false) {
            return;
        }
        ProjectProperties buildProps = ProjectProperties.load(folderPath, PropertyType.BUILD);
        if (buildProps == null) {
            buildProps = ProjectProperties.create(folderPath, PropertyType.BUILD);
        }
        buildProps.setProperty(ProjectProperties.PROPERTY_TESTED_PROJECT, pathToMainProject);
        try {
            buildProps.save();
            println("Updated %1$s", PropertyType.BUILD.getFilename());
        } catch (IOException e) {
            mLog.error(e, "Failed to write %1$s file in '%2$s'", PropertyType.BUILD.getFilename(), folderPath);
            return;
        }
    }

    /**
     * Checks whether the give <var>folderPath</var> is a valid project folder, and returns
     * a {@link File} to the AndroidManifest.xml file.
     * <p/>This checks that the folder exists and contains an AndroidManifest.xml file in it.
     * <p/>Any error are output using {@link #mLog}.
     * @param folderPath the folder to check
     * @return a {@link File} to the AndroidManifest.xml file, or null otherwise.
     */
    private File checkProjectFolder(String folderPath) {
        File projectFolder = new File(folderPath);
        if (!projectFolder.isDirectory()) {
            mLog.error(null, "Project folder '%1$s' is not a valid directory, this is not an Android project you can update.", projectFolder);
            return null;
        }
        File androidManifest = new File(projectFolder, SdkConstants.FN_ANDROID_MANIFEST_XML);
        if (!androidManifest.isFile()) {
            mLog.error(null, "%1$s not found in '%2$s', this is not an Android project you can update.", SdkConstants.FN_ANDROID_MANIFEST_XML, folderPath);
            return null;
        }
        return androidManifest;
    }

    /**
     * Returns true if any line of the input file contains the requested regexp.
     */
    private boolean checkFileContainsRegexp(File file, String regexp) {
        Pattern p = Pattern.compile(regexp);
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                if (p.matcher(line).find()) {
                    return true;
                }
            }
            in.close();
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Extracts a "full" package & activity name from an AndroidManifest.xml.
     * <p/>
     * The keywords dictionary is always filed the package name under the key {@link #PH_PACKAGE}.
     * If an activity name can be found, it is filed under the key {@link #PH_ACTIVITY_ENTRY_NAME}.
     * When no activity is found, this key is not created.
     *
     * @param manifestFile The AndroidManifest.xml file
     * @param outKeywords  Place where to put the out parameters: package and activity names.
     * @return True if the package/activity was parsed and updated in the keyword dictionary.
     */
    private boolean extractPackageFromManifest(File manifestFile, Map<String, String> outKeywords) {
        try {
            XPath xpath = AndroidXPathFactory.newXPath();
            InputSource source = new InputSource(new FileReader(manifestFile));
            String packageName = xpath.evaluate("/manifest/@package", source);
            source = new InputSource(new FileReader(manifestFile));
            String expression = String.format("/manifest/application/activity" + "[intent-filter/action/@%1$s:name='android.intent.action.MAIN' and " + "intent-filter/category/@%1$s:name='android.intent.category.LAUNCHER']" + "/@%1$s:name", AndroidXPathFactory.DEFAULT_NS_PREFIX);
            NodeList activityNames = (NodeList) xpath.evaluate(expression, source, XPathConstants.NODESET);
            if (packageName == null || packageName.length() == 0) {
                mLog.error(null, "Missing <manifest package=\"...\"> in '%1$s'", manifestFile.getName());
                return false;
            }
            String activityName = "";
            if (activityNames.getLength() > 0) {
                activityName = activityNames.item(0).getNodeValue();
            }
            if (mLevel == OutputLevel.VERBOSE && activityNames.getLength() > 1) {
                println("WARNING: There is more than one activity defined in '%1$s'.\n" + "Only the first one will be used. If this is not appropriate, you need\n" + "to specify one of these values manually instead:", manifestFile.getName());
                for (int i = 0; i < activityNames.getLength(); i++) {
                    String name = activityNames.item(i).getNodeValue();
                    name = combinePackageActivityNames(packageName, name);
                    println("- %1$s", name);
                }
            }
            if (activityName.length() == 0) {
                mLog.warning("Missing <activity %1$s:name=\"...\"> in '%2$s'.\n" + "No activity will be generated.", AndroidXPathFactory.DEFAULT_NS_PREFIX, manifestFile.getName());
            } else {
                outKeywords.put(PH_ACTIVITY_ENTRY_NAME, activityName);
            }
            outKeywords.put(PH_PACKAGE, packageName);
            return true;
        } catch (IOException e) {
            mLog.error(e, "Failed to read %1$s", manifestFile.getName());
        } catch (XPathExpressionException e) {
            Throwable t = e.getCause();
            mLog.error(t == null ? e : t, "Failed to parse %1$s", manifestFile.getName());
        }
        return false;
    }

    private String combinePackageActivityNames(String packageName, String activityName) {
        int pos = activityName.indexOf('.');
        if (pos == 0) {
            return packageName + activityName;
        } else if (pos > 0) {
            return activityName;
        } else {
            return packageName + "." + activityName;
        }
    }

    /**
     * Installs a new file that is based on a template file provided by a given target.
     * Each match of each key from the place-holder map in the template will be replaced with its
     * corresponding value in the created file.
     *
     * @param templateName the name of to the template file
     * @param destFile the path to the destination file, relative to the project
     * @param placeholderMap a map of (place-holder, value) to create the file from the template.
     * @param target the Target of the project that will be providing the template.
     * @throws ProjectCreateException
     */
    private void installTemplate(String templateName, File destFile, Map<String, String> placeholderMap, IAndroidTarget target) throws ProjectCreateException {
        String templateFolder = target.getPath(IAndroidTarget.TEMPLATES);
        final String sourcePath = templateFolder + File.separator + templateName;
        installFullPathTemplate(sourcePath, destFile, placeholderMap);
    }

    /**
     * Installs a new file that is based on a template file provided by the tools folder.
     * Each match of each key from the place-holder map in the template will be replaced with its
     * corresponding value in the created file.
     *
     * @param templateName the name of to the template file
     * @param destFile the path to the destination file, relative to the project
     * @param placeholderMap a map of (place-holder, value) to create the file from the template.
     * @throws ProjectCreateException
     */
    private void installTemplate(String templateName, File destFile, Map<String, String> placeholderMap) throws ProjectCreateException {
        String templateFolder = mSdkFolder + File.separator + SdkConstants.OS_SDK_TOOLS_LIB_FOLDER;
        final String sourcePath = templateFolder + File.separator + templateName;
        installFullPathTemplate(sourcePath, destFile, placeholderMap);
    }

    /**
     * Installs a new file that is based on a template.
     * Each match of each key from the place-holder map in the template will be replaced with its
     * corresponding value in the created file.
     *
     * @param sourcePath the full path to the source template file
     * @param destFile the destination file
     * @param placeholderMap a map of (place-holder, value) to create the file from the template.
     * @throws ProjectCreateException
     */
    private void installFullPathTemplate(String sourcePath, File destFile, Map<String, String> placeholderMap) throws ProjectCreateException {
        boolean existed = destFile.exists();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(destFile));
            BufferedReader in = new BufferedReader(new FileReader(sourcePath));
            String line;
            while ((line = in.readLine()) != null) {
                if (placeholderMap != null) {
                    for (String key : placeholderMap.keySet()) {
                        line = line.replace(key, placeholderMap.get(key));
                    }
                }
                out.write(line);
                out.newLine();
            }
            out.close();
            in.close();
        } catch (Exception e) {
            throw new ProjectCreateException(e, "Could not access %1$s: %2$s", destFile, e.getMessage());
        }
        println("%1$s file %2$s", existed ? "Updated" : "Added", destFile);
    }

    /**
     * Installs the project icons.
     * @param resourceFolder the resource folder
     * @param target the target of the project.
     * @return true if any icon was installed.
     */
    private boolean installIcons(File resourceFolder, IAndroidTarget target) throws ProjectCreateException {
        String templateFolder = target.getPath(IAndroidTarget.TEMPLATES);
        boolean installedIcon = false;
        installedIcon |= installIcon(templateFolder, "icon_hdpi.png", resourceFolder, "drawable-hdpi");
        installedIcon |= installIcon(templateFolder, "icon_mdpi.png", resourceFolder, "drawable-mdpi");
        installedIcon |= installIcon(templateFolder, "icon_ldpi.png", resourceFolder, "drawable-ldpi");
        return installedIcon;
    }

    /**
     * Installs an Icon in the project.
     * @return true if the icon was installed.
     */
    private boolean installIcon(String templateFolder, String iconName, File resourceFolder, String folderName) throws ProjectCreateException {
        File icon = new File(templateFolder, iconName);
        if (icon.exists()) {
            File drawable = createDirs(resourceFolder, folderName);
            installBinaryFile(icon, new File(drawable, "icon.png"));
            return true;
        }
        return false;
    }

    /**
     * Installs a binary file
     * @param source the source file to copy
     * @param destination the destination file to write
     */
    private void installBinaryFile(File source, File destination) {
        byte[] buffer = new byte[8192];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            new ProjectCreateException(e, "Failed to read binary file: %1$s", source.getAbsolutePath());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Prints a message unless silence is enabled.
     * <p/>
     * This is just a convenience wrapper around {@link ISdkLog#printf(String, Object...)} from
     * {@link #mLog} after testing if ouput level is {@link OutputLevel#VERBOSE}.
     *
     * @param format Format for String.format
     * @param args Arguments for String.format
     */
    private void println(String format, Object... args) {
        if (mLevel != OutputLevel.SILENT) {
            if (!format.endsWith("\n")) {
                format += "\n";
            }
            mLog.printf(format, args);
        }
    }

    /**
     * Creates a new folder, along with any parent folders that do not exists.
     *
     * @param parent the parent folder
     * @param name the name of the directory to create.
     * @throws ProjectCreateException
     */
    private File createDirs(File parent, String name) throws ProjectCreateException {
        final File newFolder = new File(parent, name);
        boolean existedBefore = true;
        if (!newFolder.exists()) {
            if (!newFolder.mkdirs()) {
                throw new ProjectCreateException("Could not create directory: %1$s", newFolder);
            }
            existedBefore = false;
        }
        if (newFolder.isDirectory()) {
            if (!newFolder.canWrite()) {
                throw new ProjectCreateException("Path is not writable: %1$s", newFolder);
            }
        } else {
            throw new ProjectCreateException("Path is not a directory: %1$s", newFolder);
        }
        if (!existedBefore) {
            try {
                println("Created directory %1$s", newFolder.getCanonicalPath());
            } catch (IOException e) {
                throw new ProjectCreateException("Could not determine canonical path of created directory", e);
            }
        }
        return newFolder;
    }

    /**
     * Strips the string of beginning and trailing characters (multiple
     * characters will be stripped, example stripString("..test...", '.')
     * results in "test";
     *
     * @param s the string to strip
     * @param strip the character to strip from beginning and end
     * @return the stripped string or the empty string if everything is stripped.
     */
    private static String stripString(String s, char strip) {
        final int sLen = s.length();
        int newStart = 0, newEnd = sLen - 1;
        while (newStart < sLen && s.charAt(newStart) == strip) {
            newStart++;
        }
        while (newEnd >= 0 && s.charAt(newEnd) == strip) {
            newEnd--;
        }
        newEnd++;
        if (newStart >= sLen || newEnd < 0) {
            return "";
        }
        return s.substring(newStart, newEnd);
    }
}
