package com.ivis.xprocess.ui.util;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.osgi.framework.Bundle;
import com.ivis.xprocess.core.Organization;
import com.ivis.xprocess.core.Person;
import com.ivis.xprocess.core.Portfolio;
import com.ivis.xprocess.core.impl.PortfolioImpl;
import com.ivis.xprocess.framework.exceptions.AuthenticationException;
import com.ivis.xprocess.framework.exceptions.AuthenticationException.Reason;
import com.ivis.xprocess.framework.impl.DatasourceDescriptor;
import com.ivis.xprocess.framework.migratetov3.DatasourceConversionException;
import com.ivis.xprocess.framework.migratetov3.IProgressListener;
import com.ivis.xprocess.framework.migratetov3.MigrateToV3;
import com.ivis.xprocess.framework.vcs.Proxy;
import com.ivis.xprocess.framework.vcs.VCSUtil;
import com.ivis.xprocess.framework.vcs.auth.VCSPasswordAuth;
import com.ivis.xprocess.framework.vcs.exceptions.VCSException;
import com.ivis.xprocess.framework.xml.GhostBuster;
import com.ivis.xprocess.framework.xml.IPersistenceHelper;
import com.ivis.xprocess.ui.UIConstants;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.UIType;
import com.ivis.xprocess.ui.datawrappers.DataCacheManager;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.datawrappers.PersonWrapper;
import com.ivis.xprocess.ui.datawrappers.RootWrapper;
import com.ivis.xprocess.ui.dialogs.CheckBoxMessageDialog;
import com.ivis.xprocess.ui.dialogs.SelectUserDialog;
import com.ivis.xprocess.ui.factories.SaveAndAddFactory;
import com.ivis.xprocess.ui.properties.DialogMessages;
import com.ivis.xprocess.ui.properties.ElementMessages;
import com.ivis.xprocess.ui.properties.ProgressMessages;
import com.ivis.xprocess.ui.properties.VCSMessages;
import com.ivis.xprocess.ui.properties.WizardMessages;
import com.ivis.xprocess.ui.refresh.ChangeEventFactory;
import com.ivis.xprocess.ui.refresh.ChangeEventFactory.ChangeEvent;
import com.ivis.xprocess.ui.threadmanagers.ThreadManager;
import com.ivis.xprocess.util.FileUtils;
import com.ivis.xprocess.util.License;

public class DatasourceUtil {

    private static String defaultDatasourceName = ElementMessages.default_datasource;

    private static String defaultRootLocation = System.getProperty("user.home") + File.separator + ".xprocess";

    private static String defaultLocation = defaultRootLocation + File.separator + "Default_Data_Source";

    public static boolean isExistingDatasource(String location) {
        boolean openDirFound = false;
        boolean localDirFound = false;
        File file = new File(location);
        if (!file.exists()) {
            return false;
        }
        if (file.isFile()) {
            return false;
        }
        if (file.list().length == 0) {
            return false;
        }
        boolean foundDatasourceXML = false;
        boolean foundLocalXML = false;
        for (File subfile : file.listFiles()) {
            if (subfile.getName().equals(DatasourceDescriptor.DATASOURCE_DESCRIPTOR_NAME)) {
                foundDatasourceXML = true;
            }
            if (subfile.getName().equals("local")) {
                localDirFound = true;
                if ((subfile.listFiles() != null) && ((subfile.listFiles().length > 0) && (subfile.listFiles()[0] != null))) {
                    for (File localfile : subfile.listFiles()) {
                        if (localfile.getName().equals(DatasourceDescriptor.LOCAL_DESCRIPTOR_NAME)) {
                            foundLocalXML = true;
                        }
                    }
                }
            }
            if (subfile.getName().equals("open")) {
                openDirFound = true;
            }
        }
        if (!foundDatasourceXML || !foundLocalXML) {
            localDirFound = false;
        }
        return openDirFound && localDirFound;
    }

    public static boolean createDefaultDatasource(String location, String datasourceName) {
        UIPlugin.createSourceLocation(location, datasourceName);
        if (UIPlugin.getDataSource() != null) {
            Portfolio portfolio = (Portfolio) UIPlugin.getRootPortfolioWrapper().getElement();
            portfolio.setName(PortfolioImpl.ROOT_PORTFOLIO_NAME);
            SaveAndAddFactory.save();
            UIPlugin.getDataSource().getDescriptor().saveAll();
            UIPlugin.getDefault().notifyNewDataSourceListeners();
            return true;
        }
        return false;
    }

    public static void createSamples() {
        if (UIPlugin.getDefault().getBundle().getState() != Bundle.ACTIVE) {
            return;
        }
        String destination = UIPlugin.getDefault().getPluginPreferences().getString(UIConstants.xprocess_importdir);
        File destinationSampleDir = new File(destination);
        if (!destinationSampleDir.exists()) {
            destinationSampleDir.mkdirs();
        }
        String source = UIPlugin.getDefault().getPluginLocation() + "lib" + File.separator + "samples";
        File sourceSampleDir = new File(source);
        if (sourceSampleDir.exists()) {
            for (File file : sourceSampleDir.listFiles()) {
                if (file.getName().endsWith(".xpe")) {
                    File out = new File(destination + File.separator + file.getName());
                    if (!out.exists()) {
                        try {
                            FileUtils.copyFile(file, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static int getNextName(File rootDir, String nameOfFile) {
        int copyNumber = 1;
        for (String filename : rootDir.list()) {
            if (filename.equals(nameOfFile)) {
                copyNumber++;
            } else {
                String strippedName = stripFilename(filename);
                if ((strippedName != null) && strippedName.equals(nameOfFile)) {
                    int num = grabCopyNumber(filename);
                    if (num > copyNumber) {
                        copyNumber = num;
                    }
                }
            }
        }
        return copyNumber;
    }

    public static String createCopyOfDatasource(String postfix, boolean closeDatasource) {
        int copyNumber = getNextName(new File(getDatasourceRoot()), UIPlugin.getDataSource().getName());
        String copiedDatasourceName = UIPlugin.getDataSource().getName() + " " + copyNumber;
        copiedDatasourceName = makeCrossPlatform(copiedDatasourceName);
        File original = new File(UIPlugin.getDataSource().getLocalRootDirectory());
        File newCopy = new File(getDatasourceRoot() + File.separator + copiedDatasourceName);
        if (closeDatasource) {
            UIPlugin.closeDataSource(null);
        }
        FileUtils.copyDir(original, newCopy);
        return newCopy.getAbsolutePath();
    }

    private static int grabCopyNumber(String filename) {
        int copyNumber = 1;
        if ((filename.lastIndexOf("(") != -1) && (filename.lastIndexOf(")") != -1)) {
            int leftBracketIndex = filename.lastIndexOf("(") + 1;
            if ((leftBracketIndex + 1) != filename.length()) {
                int rightBracketIndex = filename.lastIndexOf(")");
                if (rightBracketIndex > leftBracketIndex) {
                    String number = filename.substring(leftBracketIndex, rightBracketIndex);
                    try {
                        int num = Integer.parseInt(number);
                        if (num >= copyNumber) {
                            copyNumber = num + 1;
                        }
                    } catch (NumberFormatException numberFormatException) {
                    }
                }
            }
        }
        return copyNumber;
    }

    private static String stripFilename(String fileName) {
        if (fileName.lastIndexOf("(") == -1) {
            return "";
        }
        return fileName.substring(0, fileName.indexOf("("));
    }

    public static String getDefaultDatasourceName() {
        return defaultDatasourceName;
    }

    public static String getDefaultLocation() {
        return defaultLocation;
    }

    public static String getDatasourceRoot() {
        return defaultRootLocation;
    }

    public static void authenticateUser() throws VCSException {
        VCSPasswordAuth auth = new VCSPasswordAuth(UIPlugin.getDataSource().getDescriptor().getUserName(), UIPlugin.getDataSource().getDescriptor().getPassword());
        UIPlugin.getDataSource().authenticateVCS(auth);
    }

    public static void initializeVCSProvider() {
        VCSPasswordAuth auth = new VCSPasswordAuth(UIPlugin.getDataSource().getDescriptor().getUserName(), UIPlugin.getDataSource().getDescriptor().getPassword());
        UIPlugin.getDataSource().offlineAuthenticateVCS(auth);
    }

    public static void deleteDatasource(String locationURL) {
        File file = new File(locationURL);
        if (file.isDirectory()) {
            boolean successfulDelete = FileUtils.deleteDir(file);
            if (!successfulDelete) {
                throw new AssertionError(DialogMessages.delete_datasource_unable_to_delete + " - " + file.getName());
            }
        }
    }

    public static String getDatasourceLocation(String datasourceName) {
        return getDatasourceRoot() + File.separator + makeCrossPlatform(datasourceName);
    }

    public static String makeCrossPlatform(String path) {
        String butchered = path.replace(' ', '_');
        return butchered;
    }

    public static boolean legalPlatformName(String text) {
        for (char c : text.toCharArray()) {
            if (!isLegal(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLegal(char c) {
        if (c == ' ') {
            return true;
        }
        if (c == '_') {
            return true;
        }
        if (c == '-') {
            return true;
        }
        if ((c >= '0') && (c <= '9')) {
            return true;
        }
        if ((c >= 'A') && (c <= 'Z')) {
            return true;
        }
        if ((c >= 'a') && (c <= 'z')) {
            return true;
        }
        return false;
    }

    public static void createDefaultPerson() {
        Organization organization = null;
        for (IElementWrapper elementWrapper : ElementUtil.getAllElementsOfType(UIType.organization)) {
            if (elementWrapper.getLabel().equals(License.getLicense().getOrganization())) {
                organization = (Organization) elementWrapper.getElement();
            }
        }
        if (organization == null) {
            organization = UIPlugin.getRootPortfolio().createOrganization(License.getLicense().getOrganization());
        }
        IElementWrapper organizationWrapper = DataCacheManager.getWrapperByElement(organization);
        License lic = License.getLicense();
        Person person = organization.createPerson(lic.getAccountName());
        person.setEMail(lic.getEmail());
        person.setFirstName(lic.getFirstName());
        person.setLastName(lic.getLastName());
        person.setLicenseId(lic.getCheckSum());
        ChangeEventFactory.startChangeRecording(RootWrapper.getInstance());
        ChangeEventFactory.addChange(RootWrapper.getInstance(), ChangeEvent.NEW_CHILD);
        ChangeEventFactory.startChangeRecording(organizationWrapper);
        ChangeEventFactory.addChange(organizationWrapper, ChangeEvent.NEW_CHILD);
        IElementWrapper personWrapper = DataCacheManager.getWrapperByElement(person);
        ChangeEventFactory.startChangeRecording(personWrapper);
        ChangeEventFactory.addChange(personWrapper, ChangeEvent.NEW_CHILD);
        try {
            assert (UIPlugin.getDataSource().getAuthenticatedPerson() != null);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            return;
        }
    }

    public static void checkAuthenticatedPerson(boolean userDeleted) {
        if (UIPlugin.getDataSource() != null) {
            try {
                Person person = UIPlugin.getDataSource().getAuthenticatedPerson();
                if (person.isGhost()) {
                    throw new AuthenticationException(Reason.PERSON_WITH_MATCHING_CREDENTAILS_NOT_FOUND);
                }
            } catch (AuthenticationException e) {
                if (e.getReason() == Reason.PERSON_WITH_MATCHING_CREDENTAILS_NOT_FOUND) {
                    IElementWrapper[] elementWrappers = ElementUtil.getAllElementsOfType(UIType.person);
                    if ((elementWrappers.length == 0) && !userDeleted) {
                        DatasourceUtil.createDefaultPerson();
                    } else {
                        DatasourceUtil.selectPerson();
                    }
                    ChangeEventFactory.saveChanges();
                    ChangeEventFactory.stopChangeRecording();
                }
            }
            MainToolbarButtonManager.updatePersonalPlannerButtonAsynch();
        }
    }

    private static void selectPerson() {
        SelectUserDialog selectUserWizard = new SelectUserDialog(ViewUtil.getCurrentShell());
        if (selectUserWizard.open() == IDialogConstants.OK_ID) {
            if (selectUserWizard.useExistingUser()) {
                final IElementWrapper elementWrapper = selectUserWizard.getExistingUser();
                if (elementWrapper instanceof PersonWrapper) {
                    ChangeEventFactory.startChangeRecording(elementWrapper);
                    Person person = (Person) elementWrapper.getElement();
                    person.setLicenseId(License.getLicense().getCheckSum());
                    ChangeEventFactory.addPropertyChange(elementWrapper, Person.NAME);
                    ChangeEventFactory.addPropertyChange(elementWrapper, Person.ACCOUNT_NAME);
                }
            } else {
                DatasourceUtil.createDefaultPerson();
            }
        }
    }

    public static IElementWrapper getUser() {
        if (UIPlugin.getDataSource() == null) {
            return null;
        }
        Person person;
        try {
            person = UIPlugin.getDataSource().getAuthenticatedPerson();
            IElementWrapper personWrapper = DataCacheManager.getWrapperByElement(person);
            return personWrapper;
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DatasourceDescriptor recoverVCSIoError(IProgressMonitor monitor) {
        IPersistenceHelper ph = null;
        try {
            monitor.beginTask(ProgressMessages.datasource_recovery_taskname, IProgressMonitor.UNKNOWN);
            monitor.setTaskName(ProgressMessages.datasource_recovery_taskname);
            String oldDatasourcelocation = UIPlugin.getDataSource().getLocalRootDirectory();
            String datasourceUrl = UIPlugin.getDataSource().getDatasourceURL();
            String username = UIPlugin.getDataSource().getUserName();
            String password = UIPlugin.getDataSource().getPassword();
            if (!VCSUtil.isDatasource(datasourceUrl, Portfolio.class, username, password)) {
                DialogUtil.openErrorDialog(VCSMessages.vcs_connection_error, WizardMessages.error_existing_repository_expected_butnotfound_prefix + datasourceUrl + WizardMessages.error_existing_repository_expected_butnotfound_suffix);
                return null;
            }
            String location = getDatasourceLocation(UIPlugin.getDataSource().getName() + " " + ProgressMessages.datasource_recovery_filepostfix);
            File locationFile = new File(location);
            if (locationFile.exists()) {
                location = location + "(" + DatasourceUtil.getNextName(locationFile.getParentFile(), locationFile.getName()) + ")";
            }
            String datasourceName = UIPlugin.getDataSource().getName();
            String vcsType = UIPlugin.getDataSource().getVCSType();
            Proxy proxy = UIPlugin.getDataSource().getProxy();
            ph = UIPlugin.createNewPersistenceHelper(location, null);
            ph.getDataSource().setName(datasourceName);
            ph.getDataSource().setDatasourceURL(datasourceUrl);
            ph.getDataSource().setVCSType(vcsType);
            ph.getDataSource().setUserName(username);
            ph.getDataSource().setPassword(password);
            if (proxy != null) {
                ph.getDataSource().setProxy(proxy);
            }
            VCSPasswordAuth auth = new VCSPasswordAuth(username, password);
            monitor.subTask(VCSMessages.vcs_authenticating_user);
            ph.getDataSource().authenticateVCS(auth);
            monitor.subTask(VCSMessages.vcs_downloading_elements);
            ph.getDataSource().getVcsProvider().checkoutHead();
            File descriptorFile = new File(location + File.separator + DatasourceDescriptor.DATASOURCE_DESCRIPTOR_NAME);
            if (descriptorFile.exists()) {
                ph.getDataSource().getDescriptor().saveLocal();
                ph.getDataSource().getDescriptor().restoreAll();
            } else {
                ph.getDataSource().getDescriptor().saveAll();
                ph.getDataSource().getVcsProvider().add(descriptorFile);
            }
            File file = new File(oldDatasourcelocation);
            FileUtils.deleteDir(file);
            return ph.getDataSource().getDescriptor();
        } catch (final VCSException e) {
            DialogUtil.openErrorDialog(VCSMessages.vcs_error_dialog_title, e.getMessage());
            return null;
        }
    }

    /**
     * Any caller should have already determined that the datasource is old
     * before calling this method.
     *
     * @param descriptor
     * @return
     */
    public static Boolean[] handleOldDatasource() {
        String title = DialogMessages.datasource_conversion_title;
        String conversionMessage = DialogMessages.datasource_conversion_message;
        CheckBoxMessageDialog checkBoxMessageDialog = new CheckBoxMessageDialog(ViewUtil.getCurrentShell(), title, conversionMessage, DialogMessages.datasource_conversion_update_process_checkbox);
        if (checkBoxMessageDialog.open() == IDialogConstants.CANCEL_ID) {
            return new Boolean[] { false, false };
        }
        return new Boolean[] { true, checkBoxMessageDialog.isSelected() };
    }

    public static boolean convertOldDatasource(String location, DatasourceDescriptor datasourceDescriptor, boolean updateProcess, IProgressMonitor monitor) {
        if (datasourceDescriptor.getOpenDir().listFiles().length == 1) {
            return convertPre3(location, datasourceDescriptor, updateProcess, monitor);
        }
        if (UIPlugin.getPersistenceHelper().getDataSource().getDescriptor().isOld()) {
            convertPre2dot5(updateProcess, monitor);
        }
        return true;
    }

    public static boolean convertPre3(String location, DatasourceDescriptor datasourceDescriptor, boolean updateProcess, final IProgressMonitor monitor) {
        location = datasourceDescriptor.getAbsolutePath().substring(0, datasourceDescriptor.getAbsolutePath().lastIndexOf(File.separator));
        if (monitor != null) {
            monitor.setTaskName("Backing up Datasource - " + datasourceDescriptor.getName());
        }
        int copyNumber = getNextName(new File(location), datasourceDescriptor.getFile().getName());
        File destinationToOldDatasource = new File(location + File.separator + datasourceDescriptor.getFile().getName() + "(" + copyNumber + ")");
        FileUtils.copyDir(new File(datasourceDescriptor.getAbsolutePath()), destinationToOldDatasource);
        FileUtils.deleteDir(new File(datasourceDescriptor.getAbsolutePath()));
        MigrateToV3 migrateToV3 = new MigrateToV3(destinationToOldDatasource.getAbsolutePath(), datasourceDescriptor.getAbsolutePath());
        migrateToV3.setProgressListener(new IProgressListener() {

            public void progressMessage(String progressMessage) {
                if (monitor != null) {
                    monitor.setTaskName(progressMessage);
                }
            }
        });
        try {
            migrateToV3.migrate();
        } catch (DatasourceConversionException datasourceConversionException) {
            DialogUtil.openErrorDialog("Datasource Conversion", datasourceConversionException.getMessage());
            return false;
        }
        return true;
    }

    public static void convertPre2dot5(boolean updateProcess, IProgressMonitor monitor) {
        IPersistenceHelper ph = UIPlugin.getPersistenceHelper();
        if (monitor != null) {
            monitor.subTask("Preparing to convert Datasource");
        }
        ThreadManager.stopThreads(null);
        if (!UIPlugin.hasVCS()) {
            if (monitor != null) {
                monitor.subTask("Making a backup of the Datasource");
            }
            DatasourceUtil.createCopyOfDatasource("backup", false);
        }
        if (UIPlugin.hasVCS()) {
            if (monitor != null) {
                monitor.subTask("Making the Datasource standalone");
            }
            UIPlugin.getDataSource().makeStandalone();
        }
        if (monitor != null) {
            monitor.worked(5);
        }
        if (monitor != null) {
            monitor.subTask("Converting the data");
        }
        GhostBuster.dataClean(ph);
        SaveAndAddFactory.save();
        if (monitor != null) {
            monitor.worked(10);
        }
        File descriptorFile = new File(UIPlugin.getDataSource().getLocalRootDirectory() + File.separator + DatasourceDescriptor.DATASOURCE_DESCRIPTOR_NAME);
        String local = UIPlugin.getDataSource().getLocalRootDirectory() + File.separator + "local";
        File localDir = new File(local);
        if (localDir.exists()) {
            for (File file : localDir.listFiles()) {
                if (file.isFile()) {
                    if (!file.getName().equals("xprocess.lock")) {
                        file.delete();
                    }
                }
            }
        }
        ph.getDataSource().getDescriptor().saveAll();
        if (UIPlugin.hasVCS()) {
            try {
                ph.getDataSource().getVcsProvider().add(descriptorFile);
            } catch (VCSException e) {
                e.printStackTrace();
            }
        }
        if (monitor != null) {
            monitor.subTask("Conversion completed");
        }
        if (!updateProcess) {
            ChangeEventFactory.stopChangeRecording();
        }
        ThreadManager.startThreads();
        if (monitor != null) {
            monitor.worked(5);
        }
    }

    /**
     * If there was an issue opening the Data source we need to inform the user.
     */
    public static void datasourceNotOpened() {
        DialogUtil.openErrorDialog("Error Loading Datasource", "Unable to open Data Source");
    }

    public static void datasourceNotOpened(String message) {
        DialogUtil.openErrorDialog("Error Loading Datasource", message);
    }
}
