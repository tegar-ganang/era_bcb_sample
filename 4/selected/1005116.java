package com.memoire.vainstall;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.ice.jni.registry.RegStringValue;
import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryKey;

public class Setup extends AbstractInstall {

    public static final String JAVA_HOME = System.getProperty("java.home");

    public String javaExePath_;

    public boolean javaExeInPath_;

    private String javaExeQuote_;

    private File fileWithArchive_;

    private String installClassName_;

    private long installClassOffset_;

    private long installClassSize_;

    private long jarOffset_;

    private LicenseKeySupport licenseKeySupport;

    private boolean choosedLanguage;

    private VAShortcutEntry[] launchparms;

    private String customPrePostClassName_;

    /**
   * The original language
   */
    private String originalLanguage;

    public Setup(String uiMode, String uiBluescreen, String uiBluescreenColor, String appName, String appVersion, String destPath, String linkSectionName, String linkSectionIcon, String linkEntryName, String linkEntryIcon, Boolean createUninstallShortcut, String licenseKeySupportName, File fileWithArchive, String installClassName, Long installClassOffset, Long installClassSize, Long jarOffset, File classloaderTempDir) {
        super();
        classloaderTempDir_ = classloaderTempDir;
        try {
            licenseKeySupport = (LicenseKeySupport) getClass().getClassLoader().loadClass(licenseKeySupportName).newInstance();
        } catch (Exception ex) {
            throw new IllegalArgumentException("cannot load license key support: " + ex.toString());
        }
        uInfo_ = new UpgradeInfo();
        VAGlobals.UI_MODE = System.getProperty("uimode", uiMode);
        if ("no".equals(uiBluescreen)) VAGlobals.UI_BLUESCREEN = false; else VAGlobals.UI_BLUESCREEN = true;
        String bsVar = System.getProperty("bluescreen");
        if (bsVar != null) {
            if ("no".equalsIgnoreCase(bsVar)) VAGlobals.UI_BLUESCREEN = false; else VAGlobals.UI_BLUESCREEN = true;
        }
        if ((uiBluescreenColor == null) || ("".equals(uiBluescreenColor)) || ("null".equals(uiBluescreenColor))) VAGlobals.UI_BLUESCREEN_COLOR = null; else VAGlobals.UI_BLUESCREEN_COLOR = new Color(Integer.parseInt(uiBluescreenColor, 16));
        VAGlobals.IMAGE = "com/memoire/vainstall/resources/banner.gif";
        VAGlobals.APP_NAME = appName;
        VAGlobals.APP_VERSION = appVersion;
        try {
            VAGlobals.DEST_PATH = expandDirectory(destPath, false, uInfo_);
        } catch (IOException e) {
            VAGlobals.printDebug(e.toString());
        }
        if (VAGlobals.DEST_PATH == null) {
            try {
                VAGlobals.DEST_PATH = expandDirectory("[HOME]" + getWithoutMacro(destPath), false, uInfo_);
            } catch (IOException e) {
                VAGlobals.printDebug(e.toString());
            }
            if (VAGlobals.DEST_PATH == null) VAGlobals.DEST_PATH = System.getProperty("user.dir" + getWithoutMacro(destPath));
        }
        VAGlobals.LINK_SECTION_NAME = linkSectionName;
        VAGlobals.LINK_SECTION_ICON = linkSectionIcon;
        VAGlobals.LINK_ENTRY_NAME = linkEntryName;
        VAGlobals.LINK_ENTRY_ICON = linkEntryIcon;
        VAGlobals.CREATE_UNINSTALL_SHORTCUT = createUninstallShortcut.booleanValue();
        if (uInfo_.forceUpgrade) VAGlobals.OPERATION = VAGlobals.UPDATE; else VAGlobals.OPERATION = VAGlobals.INSTALL;
        fileWithArchive_ = fileWithArchive;
        installClassName_ = installClassName;
        installClassOffset_ = installClassOffset.longValue();
        installClassSize_ = installClassSize.longValue();
        jarOffset_ = jarOffset.longValue();
        InputStream pin = getClass().getResourceAsStream("resources/vainstall.properties");
        Properties prop = new Properties();
        try {
            prop.load(pin);
        } catch (IOException exc) {
        }
        language = prop.getProperty("vainstall.destination.language");
        if (language != null) {
            if (language.toLowerCase().indexOf("choose") != -1) {
                choosedLanguage = true;
                state_ = LANGUAGE;
            }
            VAGlobals.setLanguage(language);
        }
        originalLanguage = VAGlobals.getCurrentLanguage();
        customPrePostClassName_ = prop.getProperty("vainstall.install.customprepost.className");
        VAStepFactory.setLnf(VAGlobals.UI_MODE, prop, getClass(), classloaderTempDir);
        ui_ = VAStepFactory.createUI(VAGlobals.UI_MODE, this);
        VAGlobals.printDebug("UI created");
        if (!fileWithArchive.exists()) {
            ui_.showError(new Exception(VAGlobals.i18n("Setup_ArchiveNotFound") + fileWithArchive.getAbsolutePath()));
            quit();
        }
        testVersion(prop);
        if ("true".equals(prop.getProperty("vainstall.script.java.fullpath", null))) {
            VAGlobals.USE_FULL_JAVA_PATH = true;
            if (VAGlobals.DEBUG) {
                VAGlobals.printDebug("Full java path always used");
            }
        } else if (VAGlobals.DEBUG) VAGlobals.printDebug("Full java path used only if needed");
        if ("true".equals(prop.getProperty("vainstall.shortcut.in.installdir", null))) {
            VAGlobals.SHORTCUTS_IN_INSTALLDIR = true;
            if (VAGlobals.DEBUG) {
                VAGlobals.printDebug("Shortcuts will be created in install dir");
            }
        }
        String javaExe = IS_WIN ? "java.exe" : "java";
        if (VAGlobals.USE_FULL_JAVA_PATH) {
            javaExePath_ = getAbsoluteJavaExe(javaExe);
        } else {
            javaExeInPath_ = isJavaExeInPath(javaExe);
            javaExePath_ = javaExeInPath_ ? javaExe : getAbsoluteJavaExe(javaExe);
        }
        javaExeQuote_ = javaExePath_.indexOf(" ") > -1 ? "\"" : "";
        nextStep();
        ui_.activateUI();
    }

    public void testVersion(final Properties prop) {
        boolean wrongVersion = false;
        String minVersion = prop.getProperty("vainstall.java.version.min", null);
        String maxVersion = prop.getProperty("vainstall.java.version.max", null);
        String vendor = prop.getProperty("vainstall.java.vendor", null);
        if (minVersion != null || maxVersion != null) {
            String version = System.getProperty("java.version");
            if (minVersion != null && minVersion.compareTo(version) > 0) {
                wrongVersion = true;
            }
            if (maxVersion != null && maxVersion.compareTo(version) < 0) {
                wrongVersion = true;
            }
        }
        if (!wrongVersion && vendor != null) {
            String currentVendor = System.getProperty("java.vendor", null);
            boolean found = false;
            if (currentVendor != null) {
                currentVendor = currentVendor.toLowerCase();
                System.err.println("current vendor " + currentVendor);
                StringTokenizer tk = new StringTokenizer(vendor.toLowerCase(), ",");
                while (!found && tk.hasMoreTokens()) {
                    if (currentVendor.indexOf(tk.nextToken().trim()) != -1) found = true;
                }
            }
            wrongVersion = !found;
        }
        if (wrongVersion) {
            String minMax = "";
            if (minVersion != null) {
                minMax = VAGlobals.i18n("Setup_MinJavaVersion") + ": " + minVersion + "\n";
            }
            if (maxVersion != null) {
                minMax = minMax + VAGlobals.i18n("Setup_MaxJavaVersion") + ": " + maxVersion + "\n";
            }
            if (vendor != null) {
                minMax = minMax + VAGlobals.i18n("Setup_JavaVendor") + ": " + vendor + "\n";
            }
            String error = VAGlobals.i18n("Setup_JavaVersionNotSuitable") + "\n\n" + minMax + "\n" + VAGlobals.i18n("Setup_JavaUsedVersion") + ": " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")" + "\n\n" + VAGlobals.i18n("Setup_JavaDownload") + ":\n" + prop.getProperty("vainstall.java.download.url", "http://www.java.com/download/manual.jsp");
            if (VAGlobals.DEBUG) {
                System.err.println("check version failed ...");
            }
            ui_.showError(new Exception(error));
            quit();
        }
    }

    public void nextStep() {
        switch(state_) {
            case LANGUAGE:
                {
                    VAGlobals.printDebug("LANGUAGE");
                    state_ = START;
                    setActionEnabled(NEXT | CANCEL);
                    step_ = ui_.createSetupLanguageStep();
                    break;
                }
            case START:
                {
                    VAGlobals.printDebug("WELCOME");
                    if (step_ instanceof VALanguageStep) {
                        language = ((VALanguageStep) step_).getLanguage();
                        VAGlobals.setLanguage(language);
                    }
                    state_ = WELCOME;
                    if (choosedLanguage == false) {
                        setActionEnabled(NEXT | CANCEL);
                    } else {
                        setActionEnabled(BACK | NEXT | CANCEL);
                    }
                    step_ = ui_.createWelcomeStep();
                    break;
                }
            case WELCOME:
                {
                    VAGlobals.printDebug("LICENSE");
                    state_ = LICENSE;
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createLicenseStep();
                    ((VALicenseStep) step_).setText(getClass().getResourceAsStream("license.txt"));
                    break;
                }
            case LICENSE:
                {
                    if (!((VALicenseStep) step_).isLicenseAccepted()) {
                        ui_.showError(new Exception(VAGlobals.i18n("Setup_AcceptLicense")));
                    } else {
                        VAGlobals.printDebug("README");
                        state_ = README;
                        setActionEnabled(BACK | NEXT | CANCEL);
                        step_ = ui_.createReadmeStep();
                        ((VAReadmeStep) step_).setText(getClass().getResourceAsStream("readme.txt"));
                    }
                    break;
                }
            case README:
                {
                    if (licenseKeySupport.needsLicenseKey()) {
                        VAGlobals.printDebug("LICENSE_KEY");
                        state_ = LICENSE_KEY;
                        setActionEnabled(0);
                        step_ = ui_.createLicenseKeyStep();
                        ((VALicenseKeyStep) step_).setLicenseKeySupport(licenseKeySupport);
                        setActionEnabled(BACK | NEXT | CANCEL);
                    } else {
                        nextStepUpgrade();
                    }
                    break;
                }
            case LICENSE_KEY:
                {
                    if (((VALicenseKeyStep) step_).getGetFields(licenseKeySupport)) {
                        nextStepUpgrade();
                    } else {
                        ui_.showError(new Exception("invalid license key"));
                    }
                    break;
                }
            case UPGRADE:
                {
                    boolean uConfirm = true;
                    if (uInfo_.forceUpgrade || VAGlobals.APP_VERSION.equals(uInfo_.lastVersion())) uConfirm = true; else {
                        VAUpgradeStep ustep = (VAUpgradeStep) step_;
                        uConfirm = ustep.isConfirmUpgrade();
                    }
                    uInfo_.upgrade = uInfo_.upgrade && uConfirm;
                    if (uInfo_.upgrade) {
                        if (!uInfo_.lastPath().exists()) {
                            exitOnError(new Exception(uInfo_.lastPath() + VAGlobals.i18n("Setup_DirectoryNotAccessible") + "\n" + VAGlobals.i18n("Setup_ReinstallFirst")));
                        }
                        if (uInfo_.module && !VAGlobals.APP_VERSION.equals(uInfo_.lastVersion())) {
                            exitOnError(new Exception(VAGlobals.i18n("Setup_VersionWarning") + "\n" + VAGlobals.i18n("Setup_Current") + uInfo_.lastVersion() + ", " + VAGlobals.i18n("Setup_ThisOne") + VAGlobals.APP_VERSION + "\n" + VAGlobals.i18n("Setup_ReinstallFirst")));
                        }
                        if (uInfo_.lastVersion().compareTo(VAGlobals.APP_VERSION) > 0) {
                            ui_.showError(new Exception(VAGlobals.i18n("Setup_VersionWarning") + "\n" + VAGlobals.i18n("Setup_Current") + uInfo_.lastVersion() + ", " + VAGlobals.i18n("Setup_ThisOne") + VAGlobals.APP_VERSION + "\n" + VAGlobals.i18n("Setup_CurrentVersionNewer") + "\n" + VAGlobals.i18n("Setup_MayCancelUpdate")));
                        }
                        VAGlobals.printDebug("Upgrade from version: " + uInfo_.lastVersion());
                        VAGlobals.DEST_PATH = uInfo_.lastPath().getAbsolutePath();
                        uInfo_.paths = null;
                        VAGlobals.printDebug("INSTALL");
                        state_ = INSTALL;
                        setActionEnabled(BACK | NEXT | CANCEL);
                        step_ = ui_.createInstallStep();
                    } else {
                        if (uInfo_.forceUpgrade) exitOnError(new Exception(VAGlobals.i18n("Setup_UpdateOnly") + "\n" + VAGlobals.i18n("Setup_NoSuitableVersion") + "\n" + VAGlobals.APP_NAME));
                        VAGlobals.printDebug("DIRECTORY");
                        state_ = DIRECTORY;
                        setActionEnabled(BACK | NEXT | CANCEL);
                        step_ = ui_.createDirectoryStep();
                        ((VADirectoryStep) step_).setDirectory(new File(VAGlobals.DEST_PATH));
                    }
                    break;
                }
            case DIRECTORY:
                {
                    VADirectoryStep dstep = (VADirectoryStep) step_;
                    File file = (dstep).getDirectory();
                    if (file == null) return;
                    File errorDir = checkDirectory(file);
                    if (errorDir != null) {
                        dstep.roDirectory(errorDir);
                        VAGlobals.printDebug("  " + errorDir.getAbsolutePath() + " read-only");
                        return;
                    }
                    if (uInfo_.paths != null) {
                        for (int i = 0; i < uInfo_.paths.length; i++) {
                            if ((uInfo_.paths[i] != null) && (uInfo_.paths[i].getAbsolutePath().equals(file.getAbsolutePath()))) {
                                dstep.rejectDirectory();
                                VAGlobals.printDebug("  " + file.getAbsolutePath() + " rejected");
                                return;
                            }
                        }
                    }
                    if (!dstep.acceptDirectory()) {
                        VAGlobals.printDebug("  " + file.getAbsolutePath() + " refused by user");
                        return;
                    }
                    VAGlobals.DEST_PATH = file.getAbsolutePath();
                    VAGlobals.printDebug("  " + file.getAbsolutePath() + " accepted");
                    VAGlobals.printDebug("INSTALL");
                    state_ = INSTALL;
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createInstallStep();
                    break;
                }
            case INSTALL:
                {
                    if (VAGlobals.APP_VERSION == null) exitOnError(new Exception(VAGlobals.i18n("Setup_VersionWarning") + "\n" + VAGlobals.i18n("Setup_Current") + uInfo_.lastVersion() + ", " + VAGlobals.i18n("Setup_ThisOne") + "is null" + "\n" + VAGlobals.i18n("Setup_ReinstallFirst")));
                    setActionEnabled(0);
                    startInstall(sharedDir_, uInfo_);
                    if (uInfo_.upgrade || uInfo_.forceUpgrade) {
                        try {
                            if (!uInfo_.module) {
                                if (cleanShortcuts(new File(sharedDir_.getAbsolutePath() + File.separator + "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION + File.separator))) createShortcuts(sharedDir_);
                            } else {
                                createShortcuts(sharedDir_);
                            }
                        } catch (IOException e) {
                            System.out.println("[Setup]: " + e.getMessage());
                        }
                        VAGlobals.printDebug("END");
                        state_ = END;
                        setActionEnabled(FINISH);
                        step_ = ui_.createEndStep();
                        ((VAEndStep) step_).setStats(stats_);
                    } else {
                        VAGlobals.printDebug("SHORTCUTS");
                        state_ = SHORTCUTS;
                        setActionEnabled(NEXT);
                        step_ = ui_.createShortcutStep();
                    }
                    break;
                }
            case SHORTCUTS:
                {
                    if (((VAShortcutStep) step_).isShortcutAccepted()) {
                        createShortcuts(sharedDir_);
                    }
                    VAGlobals.printDebug("END");
                    state_ = END;
                    setActionEnabled(FINISH);
                    step_ = ui_.createEndStep();
                    ((VAEndStep) step_).setStats(stats_);
                    break;
                }
            case END:
                {
                    ui_.quitUI();
                    quit();
                }
        }
    }

    private void nextStepUpgrade() {
        VAGlobals.printDebug("UPGRADE");
        state_ = UPGRADE;
        setActionEnabled(0);
        step_ = ui_.createUpgradeStep();
        VAGlobals.printDebug(IS_ROOT ? "Root install" : "User install");
        sharedDir_ = findVAISharedDir();
        VAGlobals.printDebug("vainstall directory: !!!" + sharedDir_);
        checkUpgrade(sharedDir_, uInfo_);
        if ((!(VAGlobals.APP_VERSION.equals(uInfo_.lastVersion()))) && uInfo_.upgrade && (!uInfo_.forceUpgrade)) {
            VAUpgradeStep ustep = (VAUpgradeStep) step_;
            ustep.setChoiceEnabled(true);
        }
        setActionEnabled(BACK | NEXT | CANCEL);
    }

    public void previousStep() {
        switch(state_) {
            case WELCOME:
                {
                    VAGlobals.printDebug("LANGUAGE");
                    state_ = START;
                    setActionEnabled(NEXT | CANCEL);
                    VAGlobals.setLanguage(originalLanguage);
                    step_ = ui_.createSetupLanguageStep();
                    break;
                }
            case LICENSE:
                {
                    VAGlobals.printDebug("WELCOME");
                    state_ = WELCOME;
                    if (choosedLanguage == false) {
                        setActionEnabled(NEXT | CANCEL);
                    } else {
                        setActionEnabled(BACK | NEXT | CANCEL);
                    }
                    step_ = ui_.createWelcomeStep();
                    break;
                }
            case README:
                {
                    VAGlobals.printDebug("LICENSE");
                    state_ = LICENSE;
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createLicenseStep();
                    ((VAReadmeStep) step_).setText(getClass().getResourceAsStream("license.txt"));
                    break;
                }
            case LICENSE_KEY:
                {
                    VAGlobals.printDebug("README");
                    state_ = README;
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createReadmeStep();
                    ((VAReadmeStep) step_).setText(getClass().getResourceAsStream("readme.txt"));
                    break;
                }
            case UPGRADE:
                {
                    if (licenseKeySupport.needsLicenseKey()) {
                        VAGlobals.printDebug("LICENSE_KEY");
                        state_ = LICENSE_KEY;
                        setActionEnabled(0);
                        step_ = ui_.createLicenseKeyStep();
                        ((VALicenseKeyStep) step_).setLicenseKeySupport(licenseKeySupport);
                        setActionEnabled(BACK | NEXT | CANCEL);
                    } else {
                        VAGlobals.printDebug("README");
                        state_ = README;
                        setActionEnabled(BACK | NEXT | CANCEL);
                        step_ = ui_.createReadmeStep();
                        ((VAReadmeStep) step_).setText(getClass().getResourceAsStream("readme.txt"));
                    }
                    break;
                }
            case DIRECTORY:
                {
                    VAGlobals.printDebug("UPGRADE");
                    state_ = UPGRADE;
                    setActionEnabled(0);
                    step_ = ui_.createUpgradeStep();
                    checkUpgrade(sharedDir_, uInfo_);
                    if ((!(VAGlobals.APP_VERSION.equals(uInfo_.lastVersion()))) && uInfo_.upgrade && (!uInfo_.forceUpgrade)) {
                        VAUpgradeStep ustep = (VAUpgradeStep) step_;
                        ustep.setChoiceEnabled(true);
                    }
                    setActionEnabled(BACK | NEXT | CANCEL);
                    break;
                }
            case INSTALL:
                {
                    if (uInfo_.upgrade) {
                        state_ = UPGRADE;
                        setActionEnabled(0);
                        step_ = ui_.createUpgradeStep();
                        checkUpgrade(sharedDir_, uInfo_);
                        setActionEnabled(BACK | NEXT | CANCEL);
                    } else {
                        VAGlobals.printDebug("DIRECTORY");
                        state_ = DIRECTORY;
                        setActionEnabled(BACK | NEXT | CANCEL);
                        step_ = ui_.createDirectoryStep();
                        ((VADirectoryStep) step_).setDirectory(new File(VAGlobals.DEST_PATH));
                    }
                    break;
                }
            default:
                {
                    VAGlobals.printDebug("can't go back...");
                    break;
                }
        }
    }

    public void redoStep() {
        switch(state_) {
            case START:
                {
                    setActionEnabled(NEXT | CANCEL);
                    step_ = ui_.createSetupLanguageStep();
                    break;
                }
            case WELCOME:
                {
                    {
                        setActionEnabled(BACK | NEXT | CANCEL);
                    }
                    step_ = ui_.createWelcomeStep();
                    break;
                }
            case LICENSE:
                {
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createLicenseStep();
                    ((VALicenseStep) step_).setText(getClass().getResourceAsStream("license.txt"));
                    break;
                }
            case README:
                {
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createReadmeStep();
                    ((VAReadmeStep) step_).setText(getClass().getResourceAsStream("readme.txt"));
                    break;
                }
            case LICENSE_KEY:
                {
                    VAGlobals.printDebug("LICENSE_KEY");
                    state_ = LICENSE_KEY;
                    setActionEnabled(0);
                    step_ = ui_.createLicenseKeyStep();
                    ((VALicenseKeyStep) step_).setLicenseKeySupport(licenseKeySupport);
                    setActionEnabled(BACK | NEXT | CANCEL);
                    break;
                }
            case UPGRADE:
                {
                    setActionEnabled(0);
                    step_ = ui_.createUpgradeStep();
                    checkUpgrade(sharedDir_, uInfo_);
                    if ((!(VAGlobals.APP_VERSION.equals(uInfo_.lastVersion()))) && uInfo_.upgrade && (!uInfo_.forceUpgrade)) {
                        VAUpgradeStep ustep = (VAUpgradeStep) step_;
                        ustep.setChoiceEnabled(true);
                    }
                    setActionEnabled(BACK | NEXT | CANCEL);
                    break;
                }
            case DIRECTORY:
                {
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createDirectoryStep();
                    ((VADirectoryStep) step_).setDirectory(new File(VAGlobals.DEST_PATH));
                    break;
                }
            case INSTALL:
                {
                    setActionEnabled(BACK | NEXT | CANCEL);
                    step_ = ui_.createInstallStep();
                    break;
                }
            case SHORTCUTS:
                {
                    setActionEnabled(NEXT);
                    step_ = ui_.createShortcutStep();
                    break;
                }
            case END:
                {
                    setActionEnabled(FINISH);
                    step_ = ui_.createEndStep();
                    ((VAEndStep) step_).setStats(stats_);
                    break;
                }
        }
    }

    private File checkDirectory(File dir) {
        File res = null;
        File parent = new VAFile(dir);
        while (parent != null) {
            if (parent.exists()) {
                if (!parent.canWrite() && !IS_WIN) res = parent; else res = null;
                break;
            }
            parent = parent.getParentFile();
        }
        return res;
    }

    private void createShortcuts(File sharedDir) {
        LogInfo logInfo;
        PrintWriter log;
        Set shortcuts;
        Iterator scIt;
        String tempString;
        try {
            Vector oldlog = loadLogFile(new File(sharedDir.getAbsolutePath() + File.separator + "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION + File.separator + "shortcuts.vai"));
            log = new PrintWriter(new FileWriter(sharedDir.getAbsolutePath() + File.separator + "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION + File.separator + "shortcuts.vai"));
            logInfo = new LogInfo();
            logInfo.log = log;
            logInfo.oldlog = oldlog;
            scIt = oldlog.iterator();
            shortcuts = new java.util.LinkedHashSet();
            while (scIt.hasNext()) {
                shortcuts.add(scIt.next());
            }
            if (VAGlobals.CREATE_UNINSTALL_SHORTCUT) {
                if (VAGlobals.DEBUG) VAGlobals.printDebug("create uninstall shortcut");
                List entry = new ArrayList();
                if (launchparms != null) entry.addAll(Arrays.asList(launchparms));
                String exe = VAGlobals.DEST_PATH + System.getProperty("file.separator") + "uninstall_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION;
                if (IS_WIN) exe += ".bat"; else if (IS_UNIX) exe += ".sh";
                VAShortcutEntry e = new VAShortcutEntry("Uninstall " + VAGlobals.APP_NAME, exe);
                e.setUninstall(true);
                e.setCreateOnDesktop(false);
                entry.add(e);
                launchparms = new VAShortcutEntry[entry.size()];
                entry.toArray(launchparms);
            }
            if (launchparms != null) {
                if (IS_UNIX) {
                    VALinkLinux.createAll(this.launchparms, shortcuts);
                } else if (IS_WIN) {
                    VALinkWindows.create(launchparms, sharedDir, installClassName_, shortcuts);
                } else {
                    ui_.showError(new Exception(VAGlobals.i18n("Setup_SorryFeatureNotImplemented")));
                }
            }
            scIt = shortcuts.iterator();
            while (scIt.hasNext()) {
                tempString = (String) scIt.next();
                log.println(tempString);
            }
            logInfo.close();
        } catch (IOException e) {
            ui_.showError(e);
        }
    }

    private void startInstall(File sharedDir, UpgradeInfo uInfo) {
        VAInstallStep step = (VAInstallStep) step_;
        LogInfo logInfo = new LogInfo();
        ObjectInputStream infos = null;
        try {
            if (uInfo.upgrade) VAGlobals.printDebug("Upgrade mode"); else VAGlobals.printDebug("Install mode");
            step.status(VAGlobals.i18n("Setup_ExtractingUninstaller"));
            File oldlogfile = extractUninstallFiles(sharedDir, uInfo.upgrade, uInfo.lastVersion());
            infos = new ObjectInputStream(getClass().getResourceAsStream("archive_infos"));
            FileWriter filewrtr = new FileWriter(sharedDir.getAbsolutePath() + File.separator + "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION + File.separator + "uninstall.vai");
            logInfo.log = new PrintWriter(filewrtr);
            logInfo.filewriter = filewrtr;
            File destPath = new File(computeLocalPath(""));
            step.details("D " + destPath);
            if ((!destPath.exists()) && (!destPath.mkdirs())) {
                throw new IOException(VAGlobals.i18n("Setup_NotCreateDirectory") + " " + destPath);
            }
            logInfo.log.println(destPath.getAbsolutePath());
            if (customPrePostClassName_.length() > 0) callPreCustom(logInfo.log, uInfo_, step);
            step.status(VAGlobals.i18n("Setup_DecompressingFiles"));
            int nbrFiles = infos.readInt();
            unzip(licenseKeySupport.decodeStream(getClass().getResourceAsStream("archive.zip")), nbrFiles, sharedDir, uInfo.upgrade, logInfo, oldlogfile);
            step.status(VAGlobals.i18n("Setup_GeneratingLaunchScripts"));
            launchparms = generateLaunchScripts(infos, logInfo);
            ui_.uiSleep(2000);
            step.status(VAGlobals.i18n("Setup_LaunchScriptsGenerated"));
            ui_.uiSleep(1000);
            step.status(VAGlobals.i18n("Setup_GeneratingUninstallScript"));
            generateUninstallScripts(sharedDir, logInfo);
            ui_.uiSleep(2000);
            step.status(VAGlobals.i18n("Setup_UninstallScriptGenerated"));
            ui_.uiSleep(1000);
            if (IS_UNIX) {
                step.status(VAGlobals.i18n("Setup_RestoringExeAttributes"));
                restoreUnixExecutables(infos);
                ui_.uiSleep(2000);
                step.status(VAGlobals.i18n("Setup_ExeAttributesRestored"));
                ui_.uiSleep(1000);
            }
            if (IS_WIN) {
                step.status(VAGlobals.i18n("Setup_UpdatingWindowsRegistry"));
                updateWindowsRegistry(sharedDir, uInfo.upgrade, uInfo.lastVersion());
                ui_.uiSleep(2000);
                step.status(VAGlobals.i18n("Setup_WindowsRegistryUpdated"));
                ui_.uiSleep(1000);
            }
            if (customPrePostClassName_.length() > 0) callPostCustom(logInfo.log, uInfo_, step);
            logInfo.close();
            infos.close();
            infos = null;
            if (oldlogfile != null) copyShortcutLog(sharedDir, new File(oldlogfile.getParent(), "shortcuts.vai"));
            removeBackups();
            step.status(VAGlobals.i18n("Setup_InstallationComplete"));
            ui_.uiSleep(1000);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            VAGlobals.printDebug("exception caught - closing logInfo");
            if (logInfo != null) {
                logInfo.close();
            } else {
                VAGlobals.printDebug("logInfo is null!");
            }
            if (infos != null) {
                try {
                    infos.close();
                } catch (IOException iox) {
                    VAGlobals.printDebug("exception closing infos " + iox.getMessage());
                }
            }
            exitOnError(e);
        }
    }

    protected void exitOnError(Throwable e) {
        cleanInstall(sharedDir_, uInfo_);
        super.exitOnError(e);
    }

    private final void callPostCustom(PrintWriter filelog, UpgradeInfo uInfo, VAInstallStep step) throws Exception {
        boolean rc;
        VAGlobals.printDebug("begin custom post-install/upgrade");
        step.status(VAGlobals.i18n("Custom_Mopping..."));
        if (uInfo.upgrade) rc = customUpgrade("postUpgrade", filelog, uInfo.lastVersion(), step); else rc = customInstall("postInstall", filelog, step);
        if (!rc) {
            VAGlobals.printDebug("custom postInstall/postUpgrade returned false");
            throw new IOException(VAGlobals.i18n("Setup_PostFailed"));
        }
        VAGlobals.printDebug("end custom post-install/upgrade - OK");
    }

    private final void callPreCustom(PrintWriter filelog, UpgradeInfo uInfo, VAInstallStep step) throws Exception {
        VAGlobals.printDebug("begin custom pre-install/upgrade");
        step.status(VAGlobals.i18n("Custom_Preparing..."));
        boolean rc;
        if (uInfo.upgrade) rc = customUpgrade("preUpgrade", filelog, uInfo.lastVersion(), step); else rc = customInstall("preInstall", filelog, step);
        if (!rc) {
            VAGlobals.printDebug("custom preInstall/preUpgrade returned false");
            throw new IOException(VAGlobals.i18n("Setup_PreFailed"));
        }
        VAGlobals.printDebug("end custom pre-install/upgrade - OK");
    }

    /** Call method that returns boolean. Unwrap InvocationTargetException. */
    public static final boolean callReflect(Method method, Object[] args) throws Exception {
        try {
            return ((Boolean) method.invoke(null, args)).booleanValue();
        } catch (InvocationTargetException tie) {
            throw ((Exception) tie.getTargetException());
        }
    }

    private final boolean customInstall(String method_name, PrintWriter filelog, VAInstallStep step) throws Exception {
        Class custom = Class.forName(customPrePostClassName_);
        Method method = custom.getMethod(method_name, new Class[] { PrintWriter.class, VAInstallStep.class });
        Object[] args = new Object[] { filelog, step };
        return callReflect(method, args);
    }

    private final boolean customUpgrade(String method_name, PrintWriter filelog, String last_version, VAInstallStep step) throws Exception {
        Class custom = Class.forName(customPrePostClassName_);
        Method method = custom.getMethod(method_name, new Class[] { String.class, PrintWriter.class, VAInstallStep.class });
        Object[] args = new Object[] { last_version, filelog, step };
        return callReflect(method, args);
    }

    private File findVAISharedDir() {
        File destPath = null;
        if (IS_ROOT) {
            if (IS_WIN) {
                try {
                    RegistryKey sharedDirKey = Registry.HKEY_LOCAL_MACHINE.openSubKey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion");
                    destPath = new File(sharedDirKey.getStringValue("CommonFilesDir"));
                    sharedDirKey.closeKey();
                } catch (Exception rex) {
                    destPath = null;
                }
            } else if (IS_UNIX) {
                destPath = new File("/usr/share");
                if (!destPath.exists()) {
                    destPath = new File("/opt/share");
                }
            }
            if (destPath != null) {
                destPath = new File(destPath, "vainstall");
                if (!destPath.exists()) {
                    destPath.mkdirs();
                }
            }
        }
        if ((destPath == null) || (!destPath.exists()) || !new VAFile(destPath).canWrite()) {
            if (IS_ROOT) {
                VAGlobals.printDebug("Could not find common dir in registry:");
                VAGlobals.printDebug("user.home/.vainstall");
            }
            destPath = new File(System.getProperty("user.home") + File.separator + ".vainstall");
            if (!destPath.exists()) destPath.mkdirs();
        }
        return destPath;
    }

    private File extractUninstallFiles(File _destPath, boolean upgrade, String lastVer) {
        File oldlog = null;
        try {
            boolean oldClassCopied = false;
            File destPath = new File(_destPath, "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
            if (upgrade) {
                File lastVerPath = new File(_destPath, "vai_" + VAGlobals.APP_NAME + "_" + lastVer);
                if (destPath.equals(lastVerPath)) {
                    File bkdir = new File(destPath.getAbsolutePath() + ".bak");
                    if (!destPath.renameTo(bkdir)) {
                        throw new IOException(VAGlobals.i18n("Setup_NotCreateDirectory") + " " + destPath);
                    }
                    oldlog = new File(bkdir.getAbsolutePath() + System.getProperty("file.separator") + "uninstall.vai");
                    lastVerPath = bkdir;
                } else {
                    oldlog = new File(lastVerPath.getAbsolutePath() + System.getProperty("file.separator") + "uninstall.vai");
                }
                if ((!destPath.exists()) && (!destPath.mkdirs())) {
                    throw new IOException(VAGlobals.i18n("Setup_NotCreateDirectory") + " " + destPath);
                }
                if (uInfo_.module) oldClassCopied = copyOldSetupClass(lastVerPath, destPath);
            } else {
                if ((!destPath.exists()) && (!destPath.mkdirs())) {
                    throw new IOException(VAGlobals.i18n("Setup_NotCreateDirectory") + " " + destPath);
                }
            }
            dirty_ = true;
            File[] ls = destPath.listFiles();
            for (int i = 0; i < ls.length; i++) {
                if (!oldClassCopied) ls[i].delete(); else if (!ls[i].getPath().equals(destPath.getAbsolutePath() + File.separator + installClassName_ + ".class")) ls[i].delete();
            }
            byte[] buf = new byte[0];
            int read = 0;
            if (!oldClassCopied && (installClassSize_ > 0 || jarOffset_ > 0)) {
                final File outClassFile = new File(destPath.getAbsolutePath() + File.separator + installClassName_ + ".class");
                if (outClassFile.exists() && !outClassFile.delete()) {
                    ui_.showError(new Exception(VAGlobals.i18n("Setup_FileNotCreated") + ":\n" + outClassFile.getName()));
                }
                final FileOutputStream out = new FileOutputStream(outClassFile);
                final FileInputStream in = new FileInputStream(fileWithArchive_);
                if (installClassOffset_ > 0) {
                    in.skip(installClassOffset_);
                }
                buf = new byte[0];
                if (installClassSize_ < 0) buf = new byte[(int) jarOffset_]; else buf = new byte[(int) installClassSize_];
                read = in.read(buf, 0, buf.length);
                out.write(buf, 0, read);
                out.close();
                in.close();
            }
            final FileInputStream in = new FileInputStream(fileWithArchive_);
            if (jarOffset_ > 0) {
                in.skip(jarOffset_);
            }
            JarInputStream jar = new JarInputStream(in);
            final File outJarFile = new File(destPath.getAbsolutePath() + File.separator + "install.jar");
            if (outJarFile.exists() && !outJarFile.delete()) {
                ui_.showError(new Exception(VAGlobals.i18n("Setup_FileNotCreated") + ":\n" + outJarFile.getName()));
            }
            JarOutputStream outJar = new JarOutputStream(new FileOutputStream(outJarFile));
            ZipEntry entry = jar.getNextEntry();
            final int bufSize = 32768;
            buf = new byte[bufSize];
            while (entry != null) {
                String entryName = entry.getName();
                if (entryName.equals("com/memoire/vainstall/resources/vainstall.properties")) {
                } else if (entryName.equals(installClassName_ + ".class") && !oldClassCopied) {
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(destPath.getAbsolutePath() + File.separator + installClassName_ + ".class");
                        VAGlobals.copyStream(jar, out, buf);
                    } catch (IOException e) {
                        throw e;
                    } finally {
                        if (out != null) out.close();
                    }
                } else if (!entryName.endsWith(".zip")) {
                    if (VAGlobals.DEBUG) VAGlobals.printDebug("jar entry name " + entryName);
                    ZipEntry outEntry = new ZipEntry(entryName);
                    CRC32 crc = new CRC32();
                    outJar.putNextEntry(outEntry);
                    int size = 0;
                    while ((read = jar.read(buf, 0, bufSize)) >= 0) {
                        size += read;
                        if (read == 0) {
                            Thread.yield();
                        } else {
                            outJar.write(buf, 0, read);
                            crc.update(buf, 0, read);
                        }
                    }
                    outEntry.setSize(size);
                    outEntry.setCrc(crc.getValue());
                    outJar.flush();
                    outJar.closeEntry();
                }
                jar.closeEntry();
                entry = jar.getNextEntry();
            }
            InputStream pin = getClass().getResourceAsStream("resources/vainstall.properties");
            Properties prop = new Properties();
            try {
                prop.load(pin);
            } catch (IOException exc) {
            }
            if (language == null) language = "default";
            prop.setProperty("vainstall.destination.language", language);
            ZipEntry outEntry = new ZipEntry("com/memoire/vainstall/resources/vainstall.properties");
            CRC32 crc = new CRC32();
            outEntry.setCrc(crc.getValue());
            outEntry.setSize(prop.size());
            outJar.putNextEntry(outEntry);
            prop.store(outJar, VAGlobals.NAME + " " + VAGlobals.VERSION);
            outEntry.setCrc(crc.getValue());
            outJar.closeEntry();
            jar.close();
            outJar.close();
            in.close();
        } catch (IOException e) {
            String message = e.getLocalizedMessage();
            message += "\n" + VAGlobals.i18n("Setup_ErrorUninstallScripts");
            e.printStackTrace();
            exitOnError(new IOException(message));
        }
        return oldlog;
    }

    private VAShortcutEntry[] generateLaunchScripts(ObjectInputStream infos, LogInfo logInfo) throws IOException {
        if (VAGlobals.DEBUG) {
            VAGlobals.printDebug("generate launch scripts");
        }
        VAInstallStep step = (VAInstallStep) step_;
        Vector scripts = null;
        try {
            scripts = (Vector) infos.readObject();
        } catch (ClassNotFoundException e) {
            ui_.showError(new Exception(VAGlobals.i18n("Setup_UnableToGenerateScripts")));
            return null;
        }
        if (scripts == null) {
            step.status(VAGlobals.i18n("Setup_NoLaunchScript"));
            VAGlobals.printDebug("No launch script");
            return null;
        }
        if (!IS_WIN && !IS_UNIX) {
            ui_.showError(new Exception(VAGlobals.i18n("Setup_SorryScriptsNotSupported")));
            return null;
        }
        String NL = System.getProperty("line.separator");
        ArrayList links = new ArrayList();
        String javaw = IS_WIN ? "javaw.exe" : "javaw";
        if (VAGlobals.USE_FULL_JAVA_PATH) {
            if (VAGlobals.DEBUG) {
                VAGlobals.printDebug("java path: full path is asked");
            }
            javaw = getAbsoluteJavaExe(javaw);
        } else if (!isJavaExeInPath(javaw)) {
            if (VAGlobals.DEBUG) {
                VAGlobals.printDebug("javaw is not in PATH");
            }
            if (javaExeInPath_) {
                javaw = javaExePath_;
                if (VAGlobals.DEBUG) {
                    VAGlobals.printDebug("javaw: java is in PATH so we use it");
                }
            } else {
                if (VAGlobals.DEBUG) {
                    VAGlobals.printDebug("javaw and java are not in PATH so we absolute path");
                }
                getAbsoluteJavaExe(javaw);
            }
        } else if (VAGlobals.DEBUG) {
            VAGlobals.printDebug("javaw is in path");
        }
        for (int i = 0; i < scripts.size(); i++) {
            String sbloc = (String) scripts.get(i);
            StringTokenizer tk = new StringTokenizer(sbloc, "\n");
            if (tk.countTokens() % 2 != 0) {
                ui_.showError(new Exception(VAGlobals.i18n("Setup_UnSupportedScriptSkipping")));
                System.err.println("bad bloc= " + sbloc);
                return null;
            }
            Properties props = new Properties();
            while (tk.hasMoreTokens()) {
                props.put(tk.nextToken(), tk.nextToken());
            }
            tk = null;
            final String cmd = props.getProperty("CMD");
            if ("JavaLauncher".equals(cmd) || "JarLauncher".equals(cmd)) {
                StringBuffer script = new StringBuffer(300);
                if (IS_WIN) {
                    script.append("REM Launch script").append(NL).append(NL);
                    script.append("set VA_APP_HOME=").append(VAGlobals.DEST_PATH).append(File.separatorChar).append(NL);
                } else {
                    script.append("#!/bin/sh").append(NL);
                    script.append("### Launch Script for ").append(VAGlobals.APP_NAME).append(NL).append(NL);
                    script.append("VA_APP_HOME=`dirname $0`").append(NL);
                }
                String cdTo = props.getProperty("ScriptChangeDirectory", null);
                if (cdTo != null) {
                    File dirTo = new File(expandDirectory(cdTo, false, uInfo_));
                    if (dirTo.exists()) {
                        if (IS_UNIX) {
                            script.append("cd ").append('"').append(dirTo.getAbsolutePath()).append('"').append(NL);
                        } else {
                            String s = dirTo.getAbsolutePath();
                            int idx = s.indexOf(":");
                            if (idx > 0) {
                                script.append(s.substring(0, idx + 1)).append(NL);
                                script.append("cd ").append('"').append(s);
                                if (s.endsWith(":")) script.append(File.pathSeparatorChar);
                                script.append('"');
                            } else script.append("cd ").append('"').append(s).append('"');
                        }
                        script.append(NL);
                    }
                }
                StringBuffer commandBuf = new StringBuffer();
                commandBuf.append(javaExeQuote_);
                boolean isConsole = "console".equals(props.getProperty("JavaMode", "console"));
                if (isConsole) {
                    commandBuf.append(javaExePath_);
                } else {
                    commandBuf.append(javaw);
                }
                commandBuf.append(javaExeQuote_);
                String JavaArgs = props.getProperty("JavaArgs", "");
                StringTokenizer tokenizer = new StringTokenizer(JavaArgs, ",");
                while (tokenizer.hasMoreTokens()) commandBuf.append(" ").append(tokenizer.nextToken());
                String classPath = props.getProperty("ClassPath", "");
                tokenizer = new StringTokenizer(classPath, ",");
                if (tokenizer.countTokens() > 0) {
                    commandBuf.append(" -cp \"");
                    int n = 0;
                    while (tokenizer.hasMoreElements()) {
                        String cp = "";
                        if (n++ > 0) cp += File.pathSeparator;
                        String cpi = tokenizer.nextToken();
                        if (!new File(cpi).isAbsolute()) {
                            if (IS_WIN) cp += "%VA_APP_HOME%"; else cp += "$VA_APP_HOME/";
                        }
                        cp += cpi;
                        if (File.separatorChar != '/') cp = cp.replace('/', File.separatorChar);
                        commandBuf.append(cp);
                    }
                    commandBuf.append("\"");
                }
                String appArgs = "";
                if ("JavaLauncher".equals(cmd)) {
                    String tkClass = props.getProperty("Class");
                    commandBuf.append(" ").append(tkClass);
                    appArgs = props.getProperty("ClassArgs", "");
                } else if ("JarLauncher".equals(cmd)) {
                    String tkJar = props.getProperty("Jar");
                    if (!new File(tkJar).isAbsolute()) {
                        if (IS_WIN) tkJar = "%VA_APP_HOME%" + tkJar; else tkJar = "$VA_APP_HOME/" + tkJar;
                    }
                    commandBuf.append(" -jar \"").append(tkJar).append('"');
                    appArgs = props.getProperty("JarArgs", "");
                }
                tokenizer = new StringTokenizer(appArgs, ",");
                if (tokenizer.countTokens() > 0) {
                    while (tokenizer.hasMoreTokens()) {
                        commandBuf.append(" \"").append(tokenizer.nextToken()).append("\"");
                    }
                }
                commandBuf = new StringBuffer(replace(commandBuf.toString(), "[INSTALL_DIR]", new File(VAGlobals.DEST_PATH).getAbsolutePath()));
                if (IS_WIN) commandBuf.append(" %1 %2 %3 %4 %5 %6 %7 %8 %9"); else commandBuf.append(" $*");
                String sname = props.getProperty("ScriptName");
                String exe = computeLocalPath(sname);
                if (IS_WIN) exe += ".bat"; else exe += ".sh";
                File sfile = new File(exe);
                step.details(sfile.getAbsolutePath());
                if (!logInfo.oldlog.contains(sfile.getAbsolutePath())) logInfo.log.println(sfile.getAbsolutePath());
                PrintWriter scriptFile = null;
                try {
                    scriptFile = new PrintWriter(new FileWriter(sfile));
                    script.append(commandBuf.toString());
                    scriptFile.println(script.toString());
                } catch (IOException _e) {
                    _e.printStackTrace();
                    throw _e;
                } finally {
                    if (scriptFile != null) scriptFile.close();
                }
                VAShortcutEntry e = new VAShortcutEntry(exe);
                String s = props.getProperty("CreateShortcutOnDesktop", null);
                if (s != null && s.toLowerCase().equals("false")) {
                    e.setCreateOnDesktop(false);
                }
                if (isConsole) e.setLaunchInTerminal(true);
                e.setName(sname);
                links.add(e);
            }
        }
        VAShortcutEntry[] r = new VAShortcutEntry[links.size()];
        links.toArray(r);
        if (VAGlobals.DEBUG) {
            VAGlobals.printDebug("generate launch scripts END");
        }
        return r;
    }

    private void generateUninstallScripts(File destPath, LogInfo logInfo) throws IOException {
        VAInstallStep step = (VAInstallStep) step_;
        if (!IS_WIN && !IS_UNIX) {
            ui_.showError(new Exception(VAGlobals.i18n("Setup_NotGenerateScript")));
            return;
        }
        destPath = new File(destPath, "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
        if ((!destPath.exists()) && (!destPath.mkdirs())) {
            ui_.showError(new Exception(VAGlobals.i18n("Setup_NotCreateDirectory") + " " + destPath));
            return;
        }
        File[] ls = new File(VAGlobals.DEST_PATH).listFiles(new SetupFileFilter("uninstall_" + VAGlobals.APP_NAME + "_", SetupFileFilter.STARTS_WITH, SetupFileFilter.FILTER));
        for (int i = 0; i < ls.length; i++) {
            ls[i].delete();
        }
        String sname = "uninstall_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION;
        sname = computeLocalPath(sname);
        if (IS_WIN) sname += ".bat"; else sname += ".sh";
        step.details(sname);
        File sfile = new File(sname);
        PrintWriter script = new PrintWriter(new FileWriter(sfile));
        logInfo.log.println(sfile.getAbsolutePath());
        if (IS_WIN) {
            script.println("REM Uninstall script for " + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
            script.println();
        } else {
            script.println("#!/bin/sh");
            script.println("### Uninstall script for " + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
            script.println();
            script.println("stty -icanon");
        }
        String scriptStr = "";
        scriptStr += javaExeQuote_ + javaExePath_ + javaExeQuote_ + " -cp \"" + destPath.getAbsolutePath() + "\" ";
        if ("no".equalsIgnoreCase(System.getProperty("bluescreen"))) scriptStr += "-Dbluescreen=no ";
        scriptStr += installClassName_ + " uninstall \"" + destPath.getAbsolutePath() + "\"";
        script.println(scriptStr);
        if (IS_UNIX) {
            script.println("stty icanon");
        }
        script.close();
    }

    private Vector loadLogFile(File _logfile) throws IOException {
        Vector res = new Vector();
        if (_logfile == null) return res;
        File logfile = _logfile;
        if (logfile != null && logfile.exists()) {
            LineNumberReader log = new LineNumberReader(new FileReader(logfile));
            String line = log.readLine();
            while (line != null) {
                res.add(line);
                line = log.readLine();
            }
            log.close();
        } else {
            VAGlobals.printDebug("log not found" + _logfile.getAbsoluteFile());
        }
        return res;
    }

    private void unzip(InputStream archive, int nbrFiles, File sharedDir, boolean upgrade, LogInfo logInfo, File oldlogfile) throws IOException {
        VAInstallStep step = (VAInstallStep) step_;
        int count = 0;
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(new GZIPInputStream(archive));
            step.setProgression(0);
            dirty_ = true;
            Vector oldlog = loadLogFile(oldlogfile);
            PrintWriter log = logInfo.log;
            logInfo.oldlog = oldlog;
            for (int i = 1; i < oldlog.size(); i++) {
                String sentry = (String) oldlog.get(i);
                String s = sentry;
                int ind = sentry.lastIndexOf(File.separator);
                if (ind > 0) s = sentry.substring(ind + 1);
                if (!s.startsWith("uninstall_" + VAGlobals.APP_NAME + "_")) log.println(sentry);
            }
            ZipEntry entry = zip.getNextEntry();
            final byte buf[] = new byte[32768];
            while (entry != null) {
                File dest = new File(computeLocalPath(entry.getName()));
                if (!dest.isDirectory() && dest.exists() && !dest.delete()) {
                    ui_.showError(new Exception(VAGlobals.i18n("Setup_FileNotCreated") + ":\n" + dest.getName()));
                    if (VAGlobals.DEBUG) {
                        System.out.println("Can delete file " + dest.getAbsolutePath());
                    }
                    ui_.quitUI();
                    System.exit(0);
                }
                if (entry.isDirectory()) {
                    if (!dest.exists()) {
                        makeDirs(dest, logInfo);
                    }
                } else {
                    File destParent = dest.getParentFile();
                    if ((destParent != null) && (!destParent.exists())) {
                        makeDirs(destParent, logInfo);
                    }
                    step.details("F " + dest);
                    boolean fileExist = dest.exists();
                    if (fileExist && !dest.canWrite()) {
                        ui_.showError(new Exception(VAGlobals.i18n("Setup_FileNotCreated") + "\n" + dest.getAbsolutePath()));
                    }
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(dest);
                        VAGlobals.copyStream(zip, out, buf);
                    } catch (FileNotFoundException _e) {
                    } catch (IOException _e) {
                        if (fileExist) ui_.showError(new Exception(VAGlobals.i18n("Setup_FileNotCreated") + "\n" + dest.getName())); else throw _e;
                        return;
                    } finally {
                        if (out != null) out.close();
                    }
                    stats_.addFile(dest, VAStats.SUCCESS);
                    if (!oldlog.contains(dest.getAbsolutePath())) log.println(dest.getAbsolutePath());
                    step.setProgression(++count * 100 / nbrFiles);
                }
                zip.closeEntry();
                Thread.yield();
                entry = zip.getNextEntry();
            }
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zip != null) zip.close();
        }
    }

    private void makeDirs(File dir, LogInfo logInfo) throws IOException {
        VAInstallStep step = (VAInstallStep) step_;
        File parent = dir.getParentFile();
        if ((parent != null) && (!parent.exists())) {
            makeDirs(parent, logInfo);
        }
        step.details("D " + dir);
        if (dir.mkdir()) {
            stats_.addDirectory(dir, VAStats.SUCCESS);
            if (!logInfo.oldlog.contains(dir.getAbsolutePath())) logInfo.log.println(dir.getAbsolutePath());
        } else {
            throw new IOException(VAGlobals.i18n("Setup_NotCreateDirectory") + " " + dir);
        }
    }

    protected void removeBackups() {
        File destPath = new File(sharedDir_, "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
        if ((uInfo_ == null) || (!uInfo_.upgrade)) return;
        File lastVerPath = new File(sharedDir_, "vai_" + VAGlobals.APP_NAME + "_" + uInfo_.lastVersion());
        if (destPath.equals(lastVerPath)) {
            lastVerPath = new File(destPath.getAbsolutePath() + ".bak");
        }
        if (lastVerPath.exists()) deleteDirRecursive(lastVerPath);
    }

    protected void updateWindowsRegistry(File sharedDir, boolean upgrade, String lastVer) throws IOException {
        File destPath = new File(sharedDir.getAbsolutePath() + File.separator + "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
        if (upgrade) {
            try {
                RegistryKey uninstallKey = Registry.HKEY_LOCAL_MACHINE.openSubKey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall", RegistryKey.ACCESS_WRITE);
                uninstallKey.deleteSubKey(VAGlobals.APP_NAME + " " + lastVer);
                uninstallKey.closeKey();
            } catch (Exception e) {
                ui_.showError(new Exception(VAGlobals.i18n("Setup_NotDeleteRegistryKey") + " " + VAGlobals.APP_NAME + " " + lastVer));
            }
        }
        try {
            RegistryKey newKey = Registry.HKEY_LOCAL_MACHINE.createSubKey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + VAGlobals.APP_NAME + " " + VAGlobals.APP_VERSION, "", RegistryKey.ACCESS_WRITE);
            RegStringValue displayName = new RegStringValue(newKey, "DisplayName");
            displayName.setData(VAGlobals.APP_NAME + " " + VAGlobals.APP_VERSION);
            newKey.setValue(displayName);
            RegStringValue uninstallString = new RegStringValue(newKey, "UninstallString");
            String scriptStr = "";
            scriptStr += javaExeQuote_ + javaExePath_ + javaExeQuote_ + " -cp \"" + destPath.getAbsolutePath() + "\" ";
            if ("no".equalsIgnoreCase(System.getProperty("bluescreen"))) scriptStr += "-Dbluescreen=no ";
            scriptStr += installClassName_ + " uninstall \"" + destPath.getAbsolutePath() + "\"";
            uninstallString.setData(scriptStr);
            newKey.setValue(uninstallString);
            newKey.closeKey();
        } catch (Exception e) {
            throw new IOException("" + e);
        }
    }

    /** @parm winsuffix should be either .exe or w.exe. */
    private static final String getAbsoluteJavaExe(String _javaExe) {
        return JAVA_HOME + File.separator + "bin" + File.separator + _javaExe;
    }

    private static final boolean isJavaExeInPath(String _exec) {
        if (IS_WIN && ("1.2".compareTo(System.getProperty("java.version").substring(0, 3)) > 1)) return false;
        try {
            String[] cmds = null;
            if (IS_WIN) {
                cmds = new String[4];
                String OS = System.getProperty("os.name").toLowerCase();
                if (OS.indexOf("windows 9") > -1) {
                    cmds[0] = "command.com";
                } else if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows 2000") > -1) || (OS.indexOf("windows 2003") > -1) || (OS.indexOf("windows xp") > -1)) {
                    cmds[0] = "cmd.exe";
                }
                cmds[1] = "/c";
            } else {
                cmds = new String[2];
            }
            final int length = cmds.length;
            cmds[length - 2] = _exec;
            cmds[length - 1] = "-version";
            Process p = Runtime.getRuntime().exec(cmds);
            try {
                return p.waitFor() == 0;
            } catch (RuntimeException e) {
                e.printStackTrace();
                return false;
            }
        } catch (Exception _e) {
            return false;
        }
    }

    /**
   * Replace parts of a string.
   *
   * @param _s
   *            the initial string
   * @param _a
   *            the string to be found
   * @param _b
   *            the string which will replace
   * @return the modified string
   */
    public static final String replace(String _s, String _a, String _b) {
        String r = _s;
        int i = 0;
        if (_b == null) _b = "";
        while ((i = r.indexOf(_a, i)) >= 0) {
            r = r.substring(0, i) + _b + r.substring(i + _a.length());
            i = i + _b.length();
        }
        return r;
    }

    private void restoreUnixExecutables(ObjectInputStream infos) {
        VAInstallStep step = (VAInstallStep) step_;
        try {
            Vector exes = (Vector) infos.readObject();
            String[] chmodStr = new String[4 + exes.size()];
            chmodStr[0] = "chmod";
            chmodStr[1] = "-R";
            chmodStr[2] = "a+x";
            chmodStr[3] = "";
            if (IS_WIN || IS_UNIX) {
                chmodStr[3] += computeLocalPath("uninstall_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION);
                if (IS_WIN) chmodStr[3] += ".bat"; else chmodStr[3] += ".sh";
            }
            for (int i = 0; i < exes.size(); i++) {
                String exe = (String) exes.get(i);
                if (exe.startsWith("[SCRIPT]")) {
                    exe = exe.substring(8);
                    if (IS_WIN) exe += ".bat"; else exe += ".sh";
                }
                chmodStr[4 + i] = computeLocalPath(exe);
                step.details(exe);
                stats_.addExecutable(new File(chmodStr[4 + i]));
            }
            String chmod = "";
            for (int i = 0; i < chmodStr.length; i++) {
                chmod += chmodStr[i] + " ";
            }
            VAGlobals.printDebug(chmod);
            Process p = Runtime.getRuntime().exec(chmodStr);
            boolean interr = true;
            while (interr) {
                try {
                    p.waitFor();
                    interr = false;
                } catch (InterruptedException e) {
                    interr = true;
                }
            }
            if (p.exitValue() != 0) throw new Exception();
        } catch (Exception e) {
            VAGlobals.printDebug(e.toString());
            ui_.showError(new Exception(VAGlobals.i18n("Setup_ErrorRestoringExeAttributes")));
        }
    }

    public static String getWithoutMacro(String dir) {
        if (dir == null) return "";
        if (!dir.startsWith("[")) return dir;
        int index = dir.indexOf(']');
        return dir.substring(index + 1);
    }

    public static String expandDirectory(String dir, boolean block, UpgradeInfo uInfo) throws IOException {
        String res = null;
        if (!dir.startsWith("[")) res = dir; else {
            int index = dir.indexOf(']');
            String prefix = dir.substring(1, index).trim();
            if ("HOME".equals(prefix)) {
                res = System.getProperty("user.home");
                if (IS_WIN && IS_ROOT) {
                    if ((res != null) && (res.length() > 3) && (res.charAt(1) == ':')) res = res.substring(0, 3);
                }
            }
            if ("INSTALL_DIR".equals(prefix)) {
                res = new File(VAGlobals.DEST_PATH).getAbsolutePath();
            } else if ("C:".equals(prefix)) {
                if (IS_WIN) res = "C:\\"; else if (IS_UNIX) res = "/"; else if (IS_MAC) res = ":";
            } else if ("PROGRAM".equals(prefix)) {
                if (!IS_ROOT) {
                    res = System.getProperty("user.home");
                } else {
                    if (IS_WIN) {
                        try {
                            RegistryKey programDirKey = Registry.HKEY_LOCAL_MACHINE.openSubKey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion");
                            res = programDirKey.getStringValue("ProgramFilesDir");
                            programDirKey.closeKey();
                        } catch (Exception re) {
                            if (block) throw new IOException(re.getMessage());
                            res = null;
                        }
                        if (res == null) {
                            VAGlobals.printDebug("Could not find program dir in registry:");
                            VAGlobals.printDebug("using 'C:\\Program Files'");
                            res = "C:\\Program Files";
                        }
                    } else if (IS_UNIX) res = "/usr/local";
                }
            } else if ("UPDATE".equals(prefix)) {
                if (uInfo.versions == null) {
                    uInfo.forceUpgrade = true;
                    return dir;
                }
                res = uInfo.lastPath().getAbsolutePath();
            } else if ("MODULE".equals(prefix)) {
                if (uInfo.versions == null) {
                    uInfo.forceUpgrade = true;
                    uInfo.module = true;
                    return dir;
                }
                res = uInfo.lastPath().getAbsolutePath();
            } else if ((prefix.length() == 2) && (prefix.endsWith(":"))) {
                if (IS_WIN) res = prefix.substring(0, 2) + File.separator; else if (IS_UNIX) res = "/";
            }
            String suffix = "";
            if (dir.length() > (index + 1)) {
                suffix += dir.substring(index + 1).trim();
                if (suffix.startsWith("/")) suffix = suffix.substring(1);
            }
            if (res == null) {
                if (block) throw new IOException(VAGlobals.i18n("Setup_InvalidDir") + dir);
                res = System.getProperty("user.dir");
            }
            res += (File.separator + suffix);
        }
        return res;
    }

    private String computeLocalPath(String entry) throws IOException {
        String res = expandDirectory(entry, false, uInfo_);
        if (res == null) throw new IOException(VAGlobals.i18n("Setup_InvalidPath") + entry);
        if ((!res.startsWith("/")) && (!res.startsWith("C:\\")) && (!res.startsWith(":")) && (!new File(res).isAbsolute())) res = VAGlobals.DEST_PATH + "/" + res;
        return res.replace('/', File.separatorChar);
    }

    private void copyShortcutLog(File sharedDir, File oldShortcutLogFile) throws IOException {
        Vector oldlog = loadLogFile(oldShortcutLogFile);
        FileWriter filewrtr = new FileWriter(sharedDir.getAbsolutePath() + File.separator + "vai_" + VAGlobals.APP_NAME + "_" + VAGlobals.APP_VERSION + File.separator + "shortcuts.vai");
        PrintWriter log = new PrintWriter(filewrtr);
        for (int i = 0; i < oldlog.size(); i++) {
            String sentry = (String) oldlog.get(i);
            log.println(sentry);
        }
        log.flush();
        log.close();
    }

    private boolean copyOldSetupClass(File lastVerPath, File destPath) throws java.io.FileNotFoundException, IOException {
        byte[] buf;
        File oldClass = new File(lastVerPath.getAbsolutePath() + File.separator + installClassName_ + ".class");
        if (oldClass.exists()) {
            FileOutputStream out = new FileOutputStream(destPath.getAbsolutePath() + File.separator + installClassName_ + ".class");
            FileInputStream in = new FileInputStream(oldClass);
            buf = new byte[(new Long(oldClass.length())).intValue()];
            int read = in.read(buf, 0, buf.length);
            out.write(buf, 0, read);
            out.close();
            in.close();
            return true;
        }
        return false;
    }
}

class UpgradeInfo {

    public boolean module = false;

    public boolean forceUpgrade = false;

    public boolean upgrade = false;

    public String[] versions = null;

    public File[] paths = null;

    public String lastVersion() {
        return versions == null ? null : versions[versions.length - 1];
    }

    public File lastPath() {
        return paths == null ? null : paths[paths.length - 1];
    }
}

class LogInfo {

    public PrintWriter log = null;

    public FileWriter filewriter = null;

    public Vector oldlog = null;

    public void close() {
        if (log == null) {
            if (VAGlobals.DEBUG) System.out.println("log file null");
        } else {
            log.flush();
            log.close();
        }
        try {
            if (filewriter != null) filewriter.close();
        } catch (IOException e) {
            VAGlobals.printDebug("LogInfo.close IOException: " + e.getMessage());
        }
        if (oldlog != null) oldlog.clear();
        oldlog = null;
    }
}

class SetupFileFilter implements FilenameFilter {

    public static final int STARTS_WITH = 1;

    public static final int ENDS_WITH = 2;

    public static final int FILTER = 1;

    public static final int EXCEPT = 2;

    private String pattern_;

    private int mode_;

    private int filter_;

    public SetupFileFilter(String patt, int mode, int filter) {
        pattern_ = patt;
        mode_ = mode;
        filter_ = filter;
    }

    public boolean accept(File dir, String f) {
        boolean res = true;
        if (mode_ == STARTS_WITH) res = f.startsWith(pattern_); else if (mode_ == ENDS_WITH) res = f.endsWith(pattern_);
        if (filter_ == EXCEPT) res = !res;
        return res;
    }
}
