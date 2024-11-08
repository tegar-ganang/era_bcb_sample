package com.memoire.vainstall.ant;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;
import com.memoire.vainstall.*;
import java.util.zip.*;
import java.util.jar.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;

/**
 * Ant Task that integrates VAInstall into Ant
 *
 * The code is based on com.memoire.vainstall.VAArchiver
 *
 * In this version we extends Task which is a problem
 * since we would like to extend VAArchiver too.
 *
 * @see com.memoire.vainstall.VAArchiver
 *
 * @author Henrik Falk
 * @version $Id: VAInstallTask.java,v 1.25 2005/10/11 09:51:55 deniger Exp $
 *
 */
public class VAInstallTask extends Task {

    private static final String JAVA_HOME = System.getProperty("java.home");

    private static final String JDK_HOME = System.getProperty("java.home") + File.separator + "..";

    private static String VAILOGO = null;

    private static final String[] JAR_FILES_COMMON = new String[] { "com/memoire/vainstall/Language_da_DK.class", "com/memoire/vainstall/Language_de_DE.class", "com/memoire/vainstall/Language_en_UK.class", "com/memoire/vainstall/Language_fr_FR.class", "com/memoire/vainstall/Language_ja_JP.class", "com/memoire/vainstall/Language_it_IT.class", "com/memoire/vainstall/AbstractInstall.class", "com/memoire/vainstall/Setup.class", "com/memoire/vainstall/VAClassLoader.class", "com/memoire/vainstall/SetupFileFilter.class", "com/memoire/vainstall/UpgradeInfo.class", "com/memoire/vainstall/LogInfo.class", "com/memoire/vainstall/Uninstall.class", "com/memoire/vainstall/VAGlobals.class", "com/memoire/vainstall/VAStats.class", "com/memoire/vainstall/VAStep.class", "com/memoire/vainstall/VAStepFactory.class", "com/memoire/vainstall/VAWelcomeStep.class", "com/memoire/vainstall/VAReadmeStep.class", "com/memoire/vainstall/VALanguageStep.class", "com/memoire/vainstall/VALicenseStep.class", "com/memoire/vainstall/VALicenseKeyStep.class", "com/memoire/vainstall/VADirectoryStep.class", "com/memoire/vainstall/VAInstallStep.class", "com/memoire/vainstall/VAUpgradeStep.class", "com/memoire/vainstall/VAShortcutStep.class", "com/memoire/vainstall/VAEndStep.class", "com/memoire/vainstall/VAWizardInterface.class", "com/memoire/vainstall/VALinkDebian.class", "com/memoire/vainstall/VALinkGnome.class", "com/memoire/vainstall/VALinkWindows.class", "com/memoire/vainstall/VALinkKDE.class", "com/memoire/vainstall/LicenseKeySupport.class", "com/memoire/vainstall/LicenseKeySupport$FieldInfo.class", "com/memoire/vainstall/DefaultLicenseKeySupport.class", "com/memoire/vainstall/TestLicenseKeySupport.class" };

    private static final String[] JAR_FILES_JNISHORTCUT = new String[] { "JNIWindowsShortcut.dll", "com/memoire/vainstall/JNIWindowsShortcut.class" };

    private static final String[] JAR_FILES_JNIREGISTRY = new String[] { "ICE_JNIRegistry.dll", "com/ice/jni/registry/NoSuchKeyException.class", "com/ice/jni/registry/NoSuchValueException.class", "com/ice/jni/registry/RegBinaryValue.class", "com/ice/jni/registry/RegDWordValue.class", "com/ice/jni/registry/RegMultiStringValue.class", "com/ice/jni/registry/RegStringValue.class", "com/ice/jni/registry/Registry.class", "com/ice/jni/registry/RegistryException.class", "com/ice/jni/registry/RegistryKey.class", "com/ice/jni/registry/RegistryValue.class", "com/ice/text/HexNumberFormat.class", "com/ice/util/AWTUtilities.class", "com/ice/util/ClassUtilities.class", "com/ice/util/FileLog.class", "com/ice/util/HTTPUtilities.class", "com/ice/util/HexDump.class", "com/ice/util/StringUtilities.class", "com/ice/util/URLUtilities.class", "com/ice/util/UserProperties.class" };

    private static final String[] JAR_FILES_TEXT_UI = new String[] { "com/memoire/vainstall/tui/Language_da_DK.class", "com/memoire/vainstall/tui/Language_de_DE.class", "com/memoire/vainstall/tui/Language_en_UK.class", "com/memoire/vainstall/tui/Language_fr_FR.class", "com/memoire/vainstall/tui/Language_ja_JP.class", "com/memoire/vainstall/tui/NullOutputStream.class", "com/memoire/vainstall/tui/TuiDefaultStep.class", "com/memoire/vainstall/tui/TuiDirectoryStep.class", "com/memoire/vainstall/tui/TuiInstallStep.class", "com/memoire/vainstall/tui/TuiLanguageStep.class", "com/memoire/vainstall/tui/TuiLicenseStep.class", "com/memoire/vainstall/tui/TuiReadmeStep.class", "com/memoire/vainstall/tui/TuiShortcutStep.class", "com/memoire/vainstall/tui/TuiWelcomeStep.class", "com/memoire/vainstall/tui/TuiWizard.class", "com/memoire/vainstall/tui/VATextUI.class", "com/memoire/vainstall/tui/TuiUpgradeStep.class", "com/memoire/vainstall/tui/TuiEndStep.class" };

    private static final String[] JAR_FILES_ANSI_UI = new String[] { "com/memoire/vainstall/aui/VAAnsiUI.class" };

    private static final String[] JAR_FILES_GRAPHIC_UI = new String[] { "com/memoire/vainstall/gui/Language_da_DK.class", "com/memoire/vainstall/gui/Language_de_DE.class", "com/memoire/vainstall/gui/Language_en_UK.class", "com/memoire/vainstall/gui/Language_fr_FR.class", "com/memoire/vainstall/gui/Language_ja_JP.class", "com/memoire/vainstall/gui/VABlueScreen.class", "com/memoire/vainstall/gui/VABlueScreen$1.class", "com/memoire/vainstall/gui/VAGraphicUI.class", "com/memoire/vainstall/gui/VAWizard.class", "com/memoire/vainstall/gui/VAPanel.class", "com/memoire/vainstall/gui/VAWelcomePanel.class", "com/memoire/vainstall/gui/VAImagePanel.class", "com/memoire/vainstall/gui/VAInstallPanel.class", "com/memoire/vainstall/gui/VALanguagePanel.class", "com/memoire/vainstall/gui/VALicensePanel.class", "com/memoire/vainstall/gui/VALicenseKeyPanel.class", "com/memoire/vainstall/gui/VALicenseKeyPanel$1.class", "com/memoire/vainstall/gui/VAReadmePanel.class", "com/memoire/vainstall/gui/VADirectoryPanel.class", "com/memoire/vainstall/gui/VADirectoryPanel$1.class", "com/memoire/vainstall/gui/VAUpgradePanel.class", "com/memoire/vainstall/gui/VAShortcutPanel.class", "com/memoire/vainstall/gui/VAEndPanel.class" };

    private static final String[] JAR_FILES_XTRA_UI = new String[] { "com/memoire/vainstall/xui/Language_da_DK.class", "com/memoire/vainstall/xui/Language_de_DE.class", "com/memoire/vainstall/xui/Language_en_UK.class", "com/memoire/vainstall/xui/Language_fr_FR.class", "com/memoire/vainstall/xui/Language_ja_JP.class", "com/memoire/vainstall/xui/XuiBlueScreen.class", "com/memoire/vainstall/xui/XuiWizard.class", "com/memoire/vainstall/xui/XuiAbstractPanel.class", "com/memoire/vainstall/xui/XuiPanel.class", "com/memoire/vainstall/xui/XuiImagePanel.class", "com/memoire/vainstall/xui/XuiTitle.class", "com/memoire/vainstall/xui/XuiButton.class", "com/memoire/vainstall/xui/XuiButtonBorder.class", "com/memoire/vainstall/xui/XuiLabel.class", "com/memoire/vainstall/xui/XuiRadioButton.class", "com/memoire/vainstall/xui/XuiOptionPane.class", "com/memoire/vainstall/xui/XuiWelcomePanel.class", "com/memoire/vainstall/xui/XuiReadmePanel.class", "com/memoire/vainstall/xui/XuiLicensePanel.class", "com/memoire/vainstall/xui/XuiInstallPanel.class", "com/memoire/vainstall/xui/XuiShortcutPanel.class", "com/memoire/vainstall/xui/XuiUpgradePanel.class", "com/memoire/vainstall/xui/XuiLanguagePanel.class", "com/memoire/vainstall/xui/XuiEndPanel.class", "com/memoire/vainstall/xui/XuiDirectoryPanel$1.class", "com/memoire/vainstall/xui/XuiDirectoryPanel.class", "com/memoire/vainstall/xui/VAXtraUI.class" };

    private static final String IMAGE_KEY = "com/memoire/vainstall/resources/banner.gif";

    private File filelist_;

    private String destPath_;

    private String archMethod_;

    private long archOffset_;

    private long installClassOffset_;

    private long installClassSize_;

    private long jarSize_;

    private String licenseKeySupportClassName_ = "com.memoire.vainstall.DefaultLicenseKeySupport";

    private String encodeKey_;

    private LicenseKeySupport licenseKeySupport_;

    private String additionalFiles_;

    private String uiMode_;

    private String uiBluescreen_;

    private String uiBluescreenColor_;

    private String image_;

    private String appName_, appVersion_;

    private String linkSectionName_, linkSectionIcon_, linkEntryName_, linkEntryIcon_;

    private String instClassName_;

    private String[] targets_;

    private String currentTarget_;

    private String jarAlias_, jarPassphrase_, jarCodebase_, jarVendor_, jarHomepage_;

    private File license_;

    private File readme_;

    private ByteArrayOutputStream archiveInfos_;

    private int archivecount_;

    private Vector archiveEntryList_;

    private Properties installProperties = new Properties();

    protected Vector filesets = new Vector();

    protected Vector javaLauncherList = new Vector();

    /**
 * Adds a set of files (nested fileset attribute).
 */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    public JavaLauncherArgument createJavaLauncher() {
        JavaLauncherArgument ga = new JavaLauncherArgument();
        javaLauncherList.addElement(ga);
        return ga;
    }

    public class JavaLauncherArgument {

        private String scriptname;

        private String javaclass;

        private String classargs;

        private String javamode;

        private String javaargs;

        private String classpath;

        public JavaLauncherArgument() {
        }

        public void setScriptname(String arg) {
            scriptname = arg;
        }

        public String getScriptname() {
            return scriptname;
        }

        public void setJavaclass(String arg) {
            javaclass = arg;
        }

        public String getJavaclass() {
            return javaclass;
        }

        public void setClassargs(String arg) {
            classargs = arg;
        }

        public String getClassargs() {
            return classargs;
        }

        public void setJavamode(String arg) {
            javamode = arg;
        }

        public String getJavamode() {
            return javamode;
        }

        public void setJavaargs(String arg) {
            javaargs = arg;
        }

        public String getJavaargs() {
            return javaargs;
        }

        public void setClasspath(String arg) {
            classpath = arg;
        }

        public String getClasspath() {
            return classpath;
        }
    }

    public void execute() throws BuildException {
        validateAttributes();
        start();
    }

    protected void validateAttributes() throws BuildException {
        if (filesets.size() == 0) {
            throw new BuildException("Specify at least one fileset.");
        }
        for (int l = 0; l < javaLauncherList.size(); l++) {
            JavaLauncherArgument arg = (JavaLauncherArgument) javaLauncherList.elementAt(l);
            if (arg.getScriptname() == null || arg.getJavaclass() == null) {
                throw new BuildException("'scriptname' and 'javaclass' is required for 'javalauncher'.");
            }
        }
    }

    private void addFilesetsToArchive(ZipOutputStream stream) throws java.io.IOException {
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            String workDirAsString = fs.getDir(project).toString();
            String[] srcFiles = ds.getIncludedFiles();
            String[] srcDirs = ds.getIncludedDirectories();
            for (int j = 0; j < srcFiles.length; j++) {
                System.out.println("file=" + srcFiles[j]);
                ZipEntry newEntry = new ZipEntry(srcFiles[j]);
                stream.putNextEntry(newEntry);
                FileInputStream in = new FileInputStream(workDirAsString + File.separator + srcFiles[j]);
                byte[] buf = new byte[2048];
                int read = in.read(buf, 0, buf.length);
                while (read > 0) {
                    stream.write(buf, 0, read);
                    read = in.read(buf, 0, buf.length);
                }
                in.close();
                stream.closeEntry();
                archivecount_++;
                archiveEntryList_.add(srcFiles[j]);
            }
        }
    }

    public VAInstallTask() {
        filelist_ = null;
        destPath_ = null;
        archMethod_ = null;
        installClassOffset_ = -10987654321L;
        installClassSize_ = -1234543210L;
        archOffset_ = -1234567890L;
        uiMode_ = null;
        uiBluescreenColor_ = null;
        uiBluescreen_ = null;
        appName_ = appVersion_ = null;
        linkSectionName_ = linkSectionIcon_ = linkEntryName_ = linkEntryIcon_ = null;
        instClassName_ = null;
        targets_ = new String[0];
        jarAlias_ = jarPassphrase_ = jarCodebase_ = jarHomepage_ = jarVendor_ = null;
        license_ = null;
        readme_ = null;
        archiveInfos_ = null;
        archivecount_ = 0;
        archiveEntryList_ = new Vector();
    }

    public void start() {
        if (!processProperties()) return;
        try {
            log(VAGlobals.i18n("VAArchiver_CompressingFiles"));
            archiveInfos_ = new ByteArrayOutputStream();
            File zip = makeArchive("archive.zip");
            zip.deleteOnExit();
            System.out.println(VAGlobals.i18n("VAArchiver_CreatingJarFiles"));
            archiveInfos_.flush();
            byte[] infosbytes = archiveInfos_.toByteArray();
            File jar = makeJar("install.jar", zip, license_, readme_, infosbytes);
            jarSize_ = jar.length();
            archiveInfos_.close();
            jar.deleteOnExit();
            File installJavaFile = new File(instClassName_ + ".java");
            installJavaFile.deleteOnExit();
            File instClass = null;
            boolean deleteInstallClass = true;
            for (int i = 0; i < targets_.length; i++) {
                System.out.println();
                System.out.println(targets_[i] + VAGlobals.i18n("VAArchiver_Target"));
                currentTarget_ = targets_[i];
                System.out.println(VAGlobals.i18n("VAArchiver_GeneratingInstallClass"));
                generateInstallCode(installJavaFile, "com/memoire/vainstall/resources/Install.vaitpl", "com/memoire/vainstall/VAClassLoader.class");
                System.out.println(VAGlobals.i18n("VAArchiver_CompilingInstallClass"));
                instClass = compile(installJavaFile.getName());
                installClassSize_ = instClass.length();
                VAGlobals.printDebug("  InstallClass size=" + installClassSize_);
                if ("jar".equals(targets_[i])) {
                    File jarTarget = new File(instClassName_ + ".jar");
                    File mfFile = new File(instClassName_ + ".mf");
                    mfFile.deleteOnExit();
                    System.out.println(VAGlobals.i18n("VAArchiver_CreatingManifestFile"));
                    generateManifestFile(mfFile);
                    System.out.println(VAGlobals.i18n("VAArchiver_UpdatingJarFile"));
                    copy(jar, jarTarget);
                    jar("uvfm", jarTarget, new File[] { mfFile, instClass });
                    if (jarAlias_ != null && !"".equals(jarAlias_.trim()) && jarPassphrase_ != null && !"".equals(jarPassphrase_.trim())) {
                        System.out.println(VAGlobals.i18n("VAArchiver_SigningJarFile"));
                        jarsign(jarPassphrase_, jarTarget, jarAlias_);
                    }
                } else if ("jnlp".equals(targets_[i])) {
                    File jnlpFile = new File(instClassName_ + ".jnlp");
                    System.out.println(VAGlobals.i18n("VAArchiver_CreatingJnlpFile"));
                    generateJnlpFile(jnlpFile);
                } else if ("java".equals(targets_[i])) {
                    installClassOffset_ = -10987654321L;
                    deleteInstallClass = false;
                    if (archMethod_.equals("append")) {
                        archOffset_ = instClass.length();
                        VAGlobals.printDebug(VAGlobals.i18n("VAArchiver_ArchiveOffset") + archOffset_);
                        generateInstallCode(installJavaFile, "com/memoire/vainstall/resources/Install.vaitpl", "com/memoire/vainstall/VAClassLoader.class");
                        System.out.println(VAGlobals.i18n("VAArchiver_CompilingInstallClass"));
                        compile(installJavaFile.getName());
                        System.out.println(VAGlobals.i18n("VAArchiver_AppendingArchive"));
                        appendArchive(instClass);
                    }
                } else if ("unix".equals(targets_[i])) {
                    File unixShellFile = new File(instClassName_ + ".sh");
                    installClassOffset_ = generateUnixInstallShell(unixShellFile, "com/memoire/vainstall/resources/Install-sh.vaitpl", instClass);
                    VAGlobals.printDebug(VAGlobals.i18n("VAArchiver_InstallClassOffset") + installClassOffset_);
                    if (archMethod_.equals("append")) {
                        archOffset_ = unixShellFile.length();
                        VAGlobals.printDebug(VAGlobals.i18n("VAArchiver_ArchiveOffset") + archOffset_);
                        generateInstallCode(installJavaFile, "com/memoire/vainstall/resources/Install.vaitpl", "com/memoire/vainstall/VAClassLoader.class");
                        System.out.println(VAGlobals.i18n("VAArchiver_CompilingInstallClass"));
                        compile(installJavaFile.getName());
                        generateUnixInstallShell(unixShellFile, "com/memoire/vainstall/resources/Install-sh.vaitpl", instClass);
                        System.out.println(VAGlobals.i18n("VAArchiver_AppendingArchive"));
                        appendArchive(unixShellFile);
                    }
                } else if (("win95".equals(targets_[i])) || ("linux-i386".equals(targets_[i]))) {
                    File nativeExeFile = null;
                    if ("win95".equals(targets_[i])) nativeExeFile = new File(instClassName_ + ".exe"); else if ("linux-i386".equals(targets_[i])) nativeExeFile = new File(instClassName_ + ".lin");
                    installClassOffset_ = generateNativeInstallExe(nativeExeFile, "com/memoire/vainstall/resources/Install-" + targets_[i] + "-exe.vaitpl", instClass);
                    VAGlobals.printDebug(VAGlobals.i18n("VAArchiver_InstallClassOffset") + installClassOffset_);
                    if (archMethod_.equals("append")) {
                        archOffset_ = nativeExeFile.length();
                        VAGlobals.printDebug(VAGlobals.i18n("VAArchiver_ArchiveOffset") + archOffset_);
                        generateInstallCode(installJavaFile, "com/memoire/vainstall/resources/Install.vaitpl", "com/memoire/vainstall/VAClassLoader.class");
                        System.out.println(VAGlobals.i18n("VAArchiver_CompilingInstallClass"));
                        compile(installJavaFile.getName());
                        generateNativeInstallExe(nativeExeFile, "com/memoire/vainstall/resources/Install-" + targets_[i] + "-exe.vaitpl", instClass);
                        System.out.println(VAGlobals.i18n("VAArchiver_AppendingArchive"));
                        appendArchive(nativeExeFile);
                    }
                }
            }
            if (instClass != null && deleteInstallClass) instClass.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean processProperties() {
        VAILOGO = "com/memoire/vainstall/resources/vailogo.gif";
        VAGlobals.setLanguage("default");
        String tmp = getProject().getProperty("vainstall.destination.language");
        if (tmp != null) {
            installProperties.put("vainstall.destination.language", tmp);
        }
        String useFullPath = getProject().getProperty("vainstall.script.java.fullpath");
        System.err.println("use full path " + useFullPath);
        if (useFullPath != null && "true".equalsIgnoreCase(useFullPath)) {
            installProperties.put("vainstall.script.java.fullpath", Boolean.TRUE);
        }
        destPath_ = getProject().getProperty("vainstall.destination.defaultPath");
        if (destPath_ == null) {
            log("Note: Property 'vainstall.destination.defaultPath' = null");
            return false;
        } else {
            if (checkVaiPath(destPath_) == false) {
                log("Note: Property 'vainstall.destination.defaultPath' has incorrect format.");
                return false;
            }
        }
        tmp = getProject().getProperty("vainstall.destination.installMode");
        if ("update".equals(tmp) == true) {
            destPath_ = "[UPDATE]";
        } else {
            if (!"install".equals(tmp)) {
                log("Note: Property 'vainstall.destination.installMode'. No valid install mode specified: Defaulting to 'install'.");
                System.err.println("no valid installMode specified: defaulting to install");
            }
        }
        tmp = getProject().getProperty("vainstall.destination.targets");
        if (tmp == null) {
            log("Note: Property 'vainstall.destination.targets'. No target specified.");
            return false;
        }
        StringTokenizer tok = new StringTokenizer(tmp, ",");
        Vector v = new Vector();
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken().trim().toLowerCase();
            if ((!"java".equals(t)) && (!"jar".equals(t)) && (!"jnlp".equals(t)) && (!"unix".equals(t)) && (!"win95".equals(t)) && (!"linux-i386".equals(t))) {
                System.err.println("unknown target: " + t);
                return false;
            }
            if (!v.contains(t)) v.add(t);
        }
        if (v.contains("jnlp") && !v.contains("jar")) v.add("jar");
        targets_ = new String[v.size()];
        int i = 0;
        if (v.contains("jar")) targets_[i++] = "jar";
        if (v.contains("jnlp")) targets_[i++] = "jnlp";
        if (v.contains("unix")) targets_[i++] = "unix";
        if (v.contains("linux-i386")) targets_[i++] = "linux-i386";
        if (v.contains("win95")) targets_[i++] = "win95";
        if (v.contains("java")) targets_[i++] = "java";
        if (v.contains("jar")) {
            jarAlias_ = VAProperties.PROPERTIES.getProperty("vainstall.jarsigner.alias");
            jarPassphrase_ = VAProperties.PROPERTIES.getProperty("vainstall.jarsigner.passphrase");
        }
        if (v.contains("jnlp")) {
            jarCodebase_ = VAProperties.PROPERTIES.getProperty("vainstall.jnlp.codebase");
            if (jarCodebase_ == null || "".equals(jarCodebase_)) {
                System.err.println("vainstall.jnlp.codebase null");
                return false;
            }
            jarHomepage_ = VAProperties.PROPERTIES.getProperty("vainstall.jnlp.homepage");
            if (jarHomepage_ == null || "".equals(jarHomepage_)) {
                System.err.println("vainstall.jnlp.homepage null");
                return false;
            }
            jarVendor_ = VAProperties.PROPERTIES.getProperty("vainstall.jnlp.vendor");
            if (jarVendor_ == null || "".equals(jarVendor_)) {
                System.err.println("vainstall.jnlp.vendor null");
                return false;
            }
        }
        archMethod_ = getProject().getProperty("vainstall.archive.archivingMethod");
        if (archMethod_ == null) {
            archMethod_ = "append";
        }
        uiMode_ = getProject().getProperty("vainstall.destination.ui");
        if (uiMode_ == null) {
            uiMode_ = "graphic";
        }
        uiBluescreen_ = getProject().getProperty("vainstall.destination.ui.bluescreen");
        if (uiBluescreen_ == null) {
            uiBluescreen_ = "yes";
        }
        uiBluescreenColor_ = getProject().getProperty("vainstall.destination.ui.bluescreen.colour");
        if ((uiBluescreenColor_ != null) && (!"".equals(uiBluescreenColor_))) {
            try {
                Integer.parseInt(uiBluescreenColor_, 16);
            } catch (NumberFormatException nfe) {
                log("Note: Property 'vainstall.destination.ui.bluescreen.colour' has invalid format.");
                return false;
            }
        }
        tmp = getProject().getProperty("vainstall.destination.ui.image");
        if (tmp == null) {
            log("Note: Property 'vainstall.destination.ui.image' = null: Will use default image.");
            image_ = IMAGE_KEY;
        } else {
            image_ = tmp;
        }
        appName_ = getProject().getProperty("vainstall.destination.appName");
        if (appName_ == null) {
            log("Note: Property 'vainstall.destination.appName' = null.");
            return false;
        }
        appVersion_ = getProject().getProperty("vainstall.destination.appVersion");
        if (appVersion_ == null) {
            log("Note: Property 'vainstall.destination.appVersion' = null.");
            return false;
        }
        linkSectionName_ = getProject().getProperty("vainstall.destination.linkSectionName");
        if (linkSectionName_ == null) {
            log("Note: Property 'vainstall.destination.appVersion' = null : Defaulting to 'Applications'.");
            linkSectionName_ = "Applications";
        }
        linkSectionIcon_ = getProject().getProperty("vainstall.destination.linkSectionIcon");
        if (linkSectionIcon_ == null) {
            log("Note: Property 'vainstall.destination.linkSectionIcon' = null.");
            linkSectionIcon_ = "";
        }
        linkEntryName_ = getProject().getProperty("vainstall.destination.linkEntryName");
        if (linkEntryName_ == null) {
            log("Note: Property 'vainstall.destination.linkEntryName' = null : Defaulting to '" + appName_ + "'.");
            linkEntryName_ = appName_;
        }
        linkEntryIcon_ = getProject().getProperty("vainstall.destination.linkEntryIcon");
        if (linkEntryIcon_ == null) {
            log("Note: Property 'vainstall.destination.linkEntryIcon' = null.");
            linkEntryIcon_ = "";
        }
        instClassName_ = getProject().getProperty("vainstall.archive.installClassName");
        if (instClassName_ == null) {
            instClassName_ = "Install_" + appName_;
        }
        tmp = getProject().getProperty("vainstall.archive.license");
        if (tmp == null) {
            log("Note: Property 'vainstall.archive.license' = null.");
            return false;
        }
        license_ = new File(tmp);
        if ((!license_.exists()) || (!license_.canRead())) {
            log("Note: Property 'vainstall.archive.license' : Can not read " + license_ + ".");
            return false;
        }
        try {
            InputStream licenseStream = new FileInputStream(license_);
            String licenseEncoding = VAProperties.PROPERTIES.getProperty("vainstall.archive.license.encoding");
            if (licenseEncoding != null && licenseEncoding.equals("") == false) {
                try {
                    InputStreamReader isrLicense = new InputStreamReader(licenseStream, licenseEncoding);
                } catch (UnsupportedEncodingException exc) {
                    log("Note: Unsuported encoding for license!");
                    return false;
                }
            }
        } catch (Exception exc) {
            log("Note: Problems reading license file!");
            return false;
        }
        tmp = getProject().getProperty("vainstall.license.key.support");
        if (tmp != null && !"".equals(tmp)) {
            licenseKeySupportClassName_ = tmp;
        }
        encodeKey_ = getProject().getProperty("vainstall.license.key.support.encode.key");
        additionalFiles_ = getProject().getProperty("vainstall.additional.files");
        Class cls = null;
        try {
            cls = Class.forName(licenseKeySupportClassName_);
        } catch (Exception ex) {
            if ((cls == null) && (additionalFiles_ != null)) {
                StringTokenizer fmi = new StringTokenizer(additionalFiles_, ",");
                while (fmi.hasMoreTokens()) {
                    StringTokenizer fm = new StringTokenizer(fmi.nextToken(), "!");
                    String classFound = fm.nextToken();
                    if ((classFound != null) && (classFound.indexOf(licenseKeySupportClassName_) > -1)) {
                        try {
                            URLClassLoader urlcl = new URLClassLoader(new URL[] { new File(classFound).getParentFile().toURL() });
                            cls = urlcl.loadClass(licenseKeySupportClassName_);
                        } catch (Exception ex2) {
                            System.out.println("License key support could not be initialized  with specific URLCLassLoader" + ex2);
                        }
                        break;
                    }
                }
            }
        }
        if (cls == null) {
            throw new RuntimeException("License key support could not be initialized: ");
        }
        try {
            System.out.println(cls.getName());
            licenseKeySupport_ = (LicenseKeySupport) cls.newInstance();
        } catch (Exception ex) {
            System.err.println("LicenseKeySupport can't be instantiated" + ex);
        }
        tmp = getProject().getProperty("vainstall.archive.readme");
        if (tmp == null) {
            log("Note: Property 'vainstall.archive.readme' = null.");
            return false;
        }
        readme_ = new File(tmp);
        if ((!readme_.exists()) || (!readme_.canRead())) {
            log("Note: Property 'vainstall.archive.readme' : Can not read " + readme_ + ".");
            return false;
        }
        try {
            InputStream readmeStream = new FileInputStream(readme_);
            String readmeEncoding = VAProperties.PROPERTIES.getProperty("vainstall.archive.readme.encoding");
            if (readmeEncoding != null && readmeEncoding.equals("") == false) {
                try {
                    InputStreamReader isrReadme = new InputStreamReader(readmeStream, readmeEncoding);
                } catch (UnsupportedEncodingException exc) {
                    log("Note: Unsupported encoding for readme!");
                    return false;
                }
            }
        } catch (Exception exc) {
            log("Note: Problems reading readme file!");
            return false;
        }
        return true;
    }

    private File makeArchive(String filename) throws IOException {
        Vector scripts = new Vector();
        Vector archiveExeList = new Vector();
        for (int l = 0; l < javaLauncherList.size(); l++) {
            JavaLauncherArgument arg = (JavaLauncherArgument) javaLauncherList.elementAt(l);
            String dest = "JavaLauncher\n";
            dest += "Class=" + arg.getJavaclass() + "\n";
            dest += "ClassPath=" + (arg.getClasspath() == null ? "" : arg.getClasspath()) + "\n";
            dest += "JavaMode=" + (arg.getJavamode() == null ? "console" : arg.getJavamode()) + "\n";
            dest += "JavaArgs=" + (arg.getJavaargs() == null ? "" : arg.getJavaargs()) + "\n";
            dest += "ClassArgs=" + (arg.getClassargs() == null ? "" : arg.getClassargs()) + "\n";
            dest += "ScriptName=" + arg.getScriptname() + "\n";
            scripts.add(dest);
            archiveExeList.add("[SCRIPT]" + arg.getScriptname());
        }
        File zipFile = new File(filename);
        File parent = zipFile.getParentFile();
        if ((parent != null) && (!parent.canWrite())) throw new IOException(zipFile + " can not be written");
        ZipOutputStream stream = new ZipOutputStream(new GZIPOutputStream(new FileOutputStream(zipFile)));
        stream.setLevel(0);
        addFilesetsToArchive(stream);
        stream.close();
        ObjectOutputStream infos = new ObjectOutputStream(archiveInfos_);
        infos.writeInt(archivecount_);
        infos.writeObject(scripts);
        infos.writeObject(archiveExeList);
        infos.flush();
        return zipFile;
    }

    private String convertToLocalPath(String entry) throws IOException {
        String res = Setup.expandDirectory(entry, true, null);
        if (res == null) throw new IOException("Invalid path: " + entry);
        return res.replace('/', File.separatorChar);
    }

    private String convertToGenericPath(String line) {
        return line.replace(File.separatorChar, '/');
    }

    private void generateInstallCode(File javaFile, String instTemplate, String classLoader) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(javaFile));
        int read = 0;
        byte[] buf = new byte[128];
        InputStream is = getClass().getResourceAsStream("/" + instTemplate);
        InputStreamReader isr = new InputStreamReader(is);
        LineNumberReader reader = new LineNumberReader(isr);
        System.out.println(VAGlobals.i18n("VAArchiver_GeneratingInstallClassCode"));
        String line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> InstallClassName"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("public class " + instClassName_ + " {");
        writer.println("  private static final Class installClass=new " + instClassName_ + "().getClass();");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> ArchivingMethod"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String ARCH_METHOD=\"" + archMethod_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> TargetType"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String TARGET_TYPE=\"" + currentTarget_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> InstallClassOffset"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static long ICLASS_OFFSET=" + installClassOffset_ + "L;");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> InstallClassSize"))) {
            writer.println(line);
            line = reader.readLine();
        }
        if (installClassSize_ != archOffset_) writer.println("  private static long ICLASS_SIZE=" + installClassSize_ + "L;"); else writer.println("  private static long ICLASS_SIZE=-1234543210L;");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> ArchiveOffset"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static long ARCH_OFFSET=" + archOffset_ + "L;");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> JarSize"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static long JAR_SIZE=" + jarSize_ + "L;");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> UIMode"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String UI_MODE=\"" + uiMode_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> UIBluescreen"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String UI_BLUESCREEN=\"" + uiBluescreen_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> UIBluescreenColor"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String UI_BLUESCREEN_COLOR=\"" + uiBluescreenColor_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> DestPath"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String DEST_PATH=\"" + destPath_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> AppInfo"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String APP_NAME=\"" + appName_ + "\";");
        writer.println("  private static String APP_VERSION=\"" + appVersion_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> LinkInfos"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String LINK_SECTION_NAME=\"" + linkSectionName_ + "\";");
        writer.println("  private static String LINK_SECTION_ICON=\"" + linkSectionIcon_ + "\";");
        writer.println("  private static String LINK_ENTRY_NAME=\"" + linkEntryName_ + "\";");
        writer.println("  private static String LINK_ENTRY_ICON=\"" + linkEntryIcon_ + "\";");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> LicenseKey"))) {
            writer.println(line);
            line = reader.readLine();
        }
        writer.println("  private static String LICENSE_KEY_SUPPORT_NAME=\"" + licenseKeySupportClassName_ + "\";");
        System.out.println(VAGlobals.i18n("VAArchiver_AppendingClassloader"));
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("// --> ClassLoader"))) {
            writer.println(line);
            line = reader.readLine();
        }
        InputStream isClassLoader = getClass().getResourceAsStream("/" + classLoader);
        System.out.println("  CLASSLOADER = " + "/" + classLoader);
        writer.println("  private static String[] CL_CLASS={");
        read = isClassLoader.read(buf);
        while (read > 0) {
            writer.println("\"" + codeLine(buf, read) + "\",");
            read = isClassLoader.read(buf);
        }
        isClassLoader.close();
        writer.println("  };\n}");
        reader.close();
        writer.close();
        is.close();
        isr.close();
    }

    private void generateJnlpFile(File jnlpFile) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jnlpFile), "UTF-8")));
        out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        out.println("<jnlp spec=\"1.0\"");
        out.println("      codebase=\"" + jarCodebase_ + "\"");
        out.println("      href=\"" + instClassName_ + ".jnlp\">");
        out.println("  <information>");
        out.println("    <title>" + appName_ + " " + appVersion_ + " Installer</title>");
        out.println("    <vendor>" + jarVendor_ + "</vendor>");
        out.println("    <homepage href=\"" + jarHomepage_ + "\"/>");
        out.println("    <description>Installer for " + appName_ + " " + appVersion_ + "</description>");
        out.println("    <offline/>");
        out.println("  </information>");
        out.println("  <resources>");
        out.println("    <j2se version=\"1.3 1.2\"/>");
        out.println("    <jar href=\"" + instClassName_ + ".jar\"/>");
        out.println("  </resources>");
        out.println("  <security>");
        out.println("    <all-permissions/>");
        out.println("  </security>");
        out.println("  <application-desc main-class=\"" + instClassName_ + "\"/>");
        out.println("</jnlp>");
        out.close();
    }

    private void generateManifestFile(File mfFile) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mfFile), "UTF-8")));
        out.println("Manifest-Version: 1.0");
        out.println("Main-Class: " + instClassName_);
        out.close();
    }

    private long generateUnixInstallShell(File unixShellFile, String instTemplate, File instClassFile) throws IOException {
        FileOutputStream byteWriter = new FileOutputStream(unixShellFile);
        InputStream is = getClass().getResourceAsStream("/" + instTemplate);
        InputStreamReader isr = new InputStreamReader(is);
        LineNumberReader reader = new LineNumberReader(isr);
        String content = "";
        String installClassStartStr = "000000000000";
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setGroupingUsed(false);
        nf.setMinimumIntegerDigits(installClassStartStr.length());
        int installClassStartPos = 0;
        long installClassOffset = 0;
        System.out.println(VAGlobals.i18n("VAArchiver_GenerateInstallShell"));
        String line = reader.readLine();
        while ((line != null) && (!line.startsWith("# InstallClassStart"))) {
            content += line + "\n";
            line = reader.readLine();
        }
        content += "InstallClassStart=" + installClassStartStr + "\n";
        installClassStartPos = content.length() - 1 - 1 - installClassStartStr.length();
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("# InstallClassSize"))) {
            content += line + "\n";
            line = reader.readLine();
        }
        content += new String("InstallClassSize=" + instClassFile.length() + "\n");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("# InstallClassName"))) {
            content += line + "\n";
            line = reader.readLine();
        }
        content += new String("InstallClassName=" + instClassName_ + "\n");
        line = reader.readLine();
        while ((line != null) && (!line.startsWith("# Install class"))) {
            content += line + "\n";
            line = reader.readLine();
        }
        if (line != null) content += line + "\n";
        byteWriter.write(content.substring(0, installClassStartPos + 1).getBytes());
        byteWriter.write(nf.format(content.length()).getBytes());
        byteWriter.write(content.substring(installClassStartPos + 1 + installClassStartStr.length()).getBytes());
        installClassOffset = content.length();
        content = null;
        FileInputStream classStream = new FileInputStream(instClassFile);
        byte[] buf = new byte[2048];
        int read = classStream.read(buf);
        while (read > 0) {
            byteWriter.write(buf, 0, read);
            read = classStream.read(buf);
        }
        classStream.close();
        reader.close();
        byteWriter.close();
        return installClassOffset;
    }

    private void shiftArray(byte[] array) {
        for (int i = 0; i < (array.length - 1); i++) array[i] = array[i + 1];
        array[array.length - 1] = 0;
    }

    private long generateNativeInstallExe(File nativeInstallFile, String instTemplate, File instClassFile) throws IOException {
        InputStream reader = getClass().getResourceAsStream("/" + instTemplate);
        System.out.println("generateNativeInstallExe = /" + instTemplate);
        System.out.println("reader length=" + reader.available());
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        String installClassVarStr = "000000000000";
        byte[] buf = new byte[installClassVarStr.length()];
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setGroupingUsed(false);
        nf.setMinimumIntegerDigits(installClassVarStr.length());
        int installClassStopPos = 0;
        long installClassOffset = reader.available();
        int position = 0;
        System.out.println(VAGlobals.i18n("VAArchiver_GenerateInstallExe"));
        reader.read(buf, 0, buf.length);
        position = 1;
        for (int n = 0; n < 3; n++) {
            while ((!new String(buf).equals("clname_here_")) && (!new String(buf).equals("clstart_here")) && (!new String(buf).equals("clstop_here_"))) {
                content.write(buf[0]);
                int nextb = reader.read();
                position++;
                shiftArray(buf);
                buf[buf.length - 1] = (byte) nextb;
            }
            if (new String(buf).equals("clname_here_")) {
                System.err.println("  clname_here_ found at " + (position - 1));
                StringBuffer clnameBuffer = new StringBuffer(64);
                clnameBuffer.append(instClassName_);
                for (int i = clnameBuffer.length() - 1; i < 64; i++) {
                    clnameBuffer.append('.');
                }
                byte[] clnameBytes = clnameBuffer.toString().getBytes();
                for (int i = 0; i < 64; i++) {
                    content.write(clnameBytes[i]);
                    position++;
                }
                reader.skip(64 - buf.length);
                reader.read(buf, 0, buf.length);
            } else if (new String(buf).equals("clstart_here")) {
                System.err.println("  clstart_here found at " + (position - 1));
                buf = nf.format(installClassOffset).getBytes();
                for (int i = 0; i < buf.length; i++) {
                    content.write(buf[i]);
                    position++;
                }
                reader.read(buf, 0, buf.length);
            } else if (new String(buf).equals("clstop_here_")) {
                System.err.println("  clstop_here_ found at " + (position - 1));
                installClassStopPos = position - 1;
                content.write(buf);
                position += 12;
                reader.read(buf, 0, buf.length);
            }
        }
        content.write(buf);
        buf = new byte[2048];
        int read = reader.read(buf);
        while (read > 0) {
            content.write(buf, 0, read);
            read = reader.read(buf);
        }
        reader.close();
        FileInputStream classStream = new FileInputStream(instClassFile);
        read = classStream.read(buf);
        while (read > 0) {
            content.write(buf, 0, read);
            read = classStream.read(buf);
        }
        classStream.close();
        content.close();
        byte[] contentBytes = content.toByteArray();
        installClassVarStr = nf.format(contentBytes.length);
        byte[] installClassVarBytes = installClassVarStr.getBytes();
        for (int i = 0; i < installClassVarBytes.length; i++) {
            contentBytes[installClassStopPos + i] = installClassVarBytes[i];
        }
        FileOutputStream out = new FileOutputStream(nativeInstallFile);
        out.write(contentBytes);
        out.close();
        return installClassOffset;
    }

    private void appendArchive(File instClass) throws IOException {
        FileOutputStream out = new FileOutputStream(instClass.getName(), true);
        FileInputStream zipStream = new FileInputStream("install.jar");
        byte[] buf = new byte[2048];
        int read = zipStream.read(buf);
        while (read > 0) {
            out.write(buf, 0, read);
            read = zipStream.read(buf);
        }
        zipStream.close();
        out.close();
    }

    private void copy(File fin, File fout) throws IOException {
        FileOutputStream out = new FileOutputStream(fout);
        FileInputStream in = new FileInputStream(fin);
        byte[] buf = new byte[2048];
        int read = in.read(buf);
        while (read > 0) {
            out.write(buf, 0, read);
            read = in.read(buf);
        }
        in.close();
        out.close();
    }

    private void jar(String options, File jarFile, File[] files) throws IOException {
        Process p = null;
        Vector argsv = new Vector();
        argsv.add(JDK_HOME + File.separator + "bin" + File.separator + "jar");
        if (options != null && !options.equals("")) argsv.add(options);
        argsv.add(jarFile.getName());
        for (int i = 0; i < files.length; i++) argsv.add(files[i].getName());
        String[] args = new String[argsv.size()];
        for (int i = 0; i < args.length; i++) args[i] = (String) argsv.get(i);
        try {
            p = Runtime.getRuntime().exec(args);
            p.waitFor();
        } catch (Exception rte) {
            throw new IOException("Runtime exception: check if you have installed the JDK and run java from the JDK\n" + "Exception message: " + rte.getMessage());
        }
        printCmdOutput(p, "jar");
        if (p.exitValue() != 0) throw new RuntimeException("  abnormal exit");
    }

    private void jarsign(String passphrase, File jarFile, String alias) throws IOException {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[] { JDK_HOME + File.separator + "bin" + File.separator + "jarsigner", "-storepass", passphrase, jarFile.getName(), alias });
            p.waitFor();
        } catch (Exception rte) {
            throw new IOException("Runtime exception: check if you have installed the JDK and run java from the JDK\n" + "Exception message: " + rte.getMessage());
        }
        printCmdOutput(p, "jarsign");
        if (p.exitValue() != 0) throw new RuntimeException("  abnormal exit");
    }

    private File compile(String javafile) throws IOException {
        File classFile = null;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[] { JDK_HOME + File.separator + "bin" + File.separator + "javac", javafile });
            p.waitFor();
        } catch (Exception rte) {
            throw new IOException("Runtime exception: check if you have installed the JDK and run java from the JDK\n" + "Exception message: " + rte.getMessage());
        }
        printCmdOutput(p, "javac");
        if (p.exitValue() != 0) throw new RuntimeException("  abnormal exit");
        classFile = new File(javafile.substring(0, javafile.lastIndexOf('.')) + ".class");
        return classFile;
    }

    private void printCmdOutput(Process p, String cmdName) throws IOException {
        BufferedReader psIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader psErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        int n = 0;
        System.out.println("  --- start " + cmdName + " ---");
        String inLine = psIn.readLine();
        String errLine = psErr.readLine();
        while ((inLine != null) || (errLine != null)) {
            if (inLine != null) System.out.println("  " + inLine);
            if (errLine != null) System.err.println("  " + errLine);
            inLine = psIn.readLine();
            errLine = psErr.readLine();
        }
        psIn.close();
        psErr.close();
        System.out.println("  --- end   " + cmdName + " ---");
    }

    private File makeJar(String filename, File archive, File license, File readme, byte[] archiveInfos) throws IOException {
        File jar = new File(filename);
        JarOutputStream out = new JarOutputStream(new FileOutputStream(jar));
        copyInternalToJar(out, JAR_FILES_COMMON);
        copyInternalToJar(out, JAR_FILES_JNIREGISTRY);
        copyInternalToJar(out, JAR_FILES_JNISHORTCUT);
        copyInternalToJar(out, JAR_FILES_TEXT_UI);
        copyInternalToJar(out, JAR_FILES_ANSI_UI);
        if (uiMode_.equals("graphic") == true) {
            copyInternalToJar(out, JAR_FILES_GRAPHIC_UI);
        }
        if (uiMode_.equals("xtra") == true) {
            copyInternalToJar(out, JAR_FILES_XTRA_UI);
        }
        addToJar(out, new ByteArrayInputStream(archiveInfos), "com/memoire/vainstall/archive_infos", archiveInfos.length);
        addToJar(out, new FileInputStream(archive), "com/memoire/vainstall/archive.zip", archive.length());
        InputStream licenseStream = new FileInputStream(license);
        String licenseEncoding = VAProperties.PROPERTIES.getProperty("vainstall.archive.license.encoding");
        if (licenseEncoding == null || licenseEncoding.equals("") == true) {
            licenseEncoding = new InputStreamReader(licenseStream).getEncoding();
        }
        InputStreamReader isrLicense = new InputStreamReader(licenseStream, licenseEncoding);
        addToJarEncoded(out, isrLicense, "com/memoire/vainstall/license.txt", license.length());
        InputStream readmeStream = new FileInputStream(readme);
        String readmeEncoding = VAProperties.PROPERTIES.getProperty("vainstall.archive.readme.encoding");
        if (readmeEncoding == null || readmeEncoding.equals("") == true) {
            readmeEncoding = new InputStreamReader(readmeStream).getEncoding();
        }
        InputStreamReader isrReadme = new InputStreamReader(readmeStream, readmeEncoding);
        addToJarEncoded(out, isrReadme, "com/memoire/vainstall/readme.txt", readme.length());
        InputStream invaiimage = null;
        try {
            invaiimage = new FileInputStream(new File(image_));
        } catch (Exception exc) {
            image_ = "/" + image_;
            invaiimage = getClass().getResourceAsStream(image_);
        }
        if (invaiimage == null) {
            invaiimage = getClass().getResourceAsStream(IMAGE_KEY);
            image_ = "com/memoire/vainstall/resources/banner.gif";
        }
        if (invaiimage != null) {
            addToJar(out, invaiimage, IMAGE_KEY, invaiimage.available());
        }
        InputStream invailogo = getClass().getResourceAsStream("/" + VAILOGO);
        addToJar(out, invailogo, VAILOGO, invailogo.available());
        invailogo.close();
        ByteArrayOutputStream poutstream = new ByteArrayOutputStream();
        installProperties.store(poutstream, VAGlobals.NAME + " " + VAGlobals.VERSION);
        ByteArrayInputStream pinstream = new ByteArrayInputStream(poutstream.toByteArray());
        addToJar(out, pinstream, "com/memoire/vainstall/resources/vainstall.properties", poutstream.toByteArray().length);
        out.close();
        return jar;
    }

    /**
   *  Copy all files from a jar file from inside a jar file
   *  to a target jar file
   * @param out JarOutputStream
   * @param jarSourceName String
   */
    private void copyInternalToJar(JarOutputStream out, String[] JAR_FILES) throws IOException {
        for (int i = 0; i < JAR_FILES.length; i++) {
            String sourceName = JAR_FILES[i];
            if (sourceName.endsWith(".jar")) {
                copyJarFilesToJar(out, sourceName);
            } else {
                InputStream is = getClass().getResourceAsStream("/" + sourceName);
                addToJar(out, is, sourceName, is.available());
            }
        }
    }

    /**
   *  Copy all files from a jar file from inside a jar file
   *  to a target jar file
   * @param out JarOutputStream
   * @param jarSourceName String
   */
    private void copyJarFilesToJar(JarOutputStream out, String jarSourceName) throws IOException {
        byte[] buffer = new byte[2048];
        InputStream isJar = getClass().getResourceAsStream("/" + jarSourceName);
        JarInputStream zin = new JarInputStream(isJar);
        JarEntry entry = null;
        while ((entry = zin.getNextJarEntry()) != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (true) {
                int read = zin.read(buffer);
                if (read == -1) break;
                bos.write(buffer, 0, read);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
            addToJar(out, bais, entry.getName(), bos.toByteArray().length);
            bos.close();
            zin.closeEntry();
        }
        zin.close();
        isJar.close();
    }

    private void addToJar(JarOutputStream out, InputStream in, String entryName, long length) throws IOException {
        byte[] buf = new byte[2048];
        ZipEntry entry = new ZipEntry(entryName);
        CRC32 crc = new CRC32();
        entry.setSize(length);
        entry.setCrc(crc.getValue());
        out.putNextEntry(entry);
        int read = in.read(buf);
        while (read > 0) {
            crc.update(buf, 0, read);
            out.write(buf, 0, read);
            read = in.read(buf);
        }
        entry.setCrc(crc.getValue());
        in.close();
        out.closeEntry();
    }

    private void addToJarEncoded(JarOutputStream out, InputStreamReader isr, String entryName, long length) throws IOException {
        StringBuffer buffer = new StringBuffer();
        Reader reader = new BufferedReader(isr);
        int ch;
        while ((ch = reader.read()) > -1) {
            buffer.append((char) ch);
        }
        reader.close();
        isr.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF8");
        osw.write(buffer.toString());
        osw.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        addToJar(out, bais, entryName, length);
    }

    private boolean checkVaiPath(String dir) {
        int index = dir.indexOf(']');
        if (!dir.startsWith("[")) return true;
        if (index <= 2) return false;
        String prefix = dir.substring(1, index).trim();
        System.out.println(dir);
        System.out.println(prefix);
        if (("HOME".equals(prefix)) || ("PROGRAM".equals(prefix))) return true;
        if ((prefix.length() == 2) && (prefix.endsWith(":")) && (Character.isLetter(prefix.charAt(0)))) return true;
        return false;
    }

    private String codeLine(byte[] data, int siz) {
        String res = null;
        byte[] convert = new byte[2 * siz];
        for (int i = 0; i < siz; i++) {
            convert[2 * i] = (byte) (65 + (data[i] & 0x0F));
            convert[2 * i + 1] = (byte) (65 + (data[i] & 0xF0) / 16);
        }
        res = new String(convert);
        return res;
    }
}
