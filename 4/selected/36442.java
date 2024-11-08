package com.cp.vaultclipse.svc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import com.cp.vaultclipse.VaultClipsePlugin;
import com.cp.vaultclipse.diff.FileComparator;
import com.cp.vaultclipse.dto.VaultTransactionDTO;
import com.cp.vaultclipse.dto.VaultTransactionItemDTO;
import com.cp.vaultclipse.helpers.FileHelper;
import com.cp.vaultclipse.helpers.VaultHelper;
import com.cp.vaultclipse.i18n.Localization;
import com.cp.vaultclipse.preferences.PropertyStore;
import com.cp.vaultclipse.ui.views.tree.MergeContentProvider;
import com.cp.vaultclipse.utils.Logger;

/**
 * Service for invoking Vault.
 * 
 * @author daniel.klco
 * @version 20110420
 */
public class VaultSvc {

    protected FileHelper fileHelper;

    protected static boolean jarsLoaded = false;

    protected static final String VAULT_CLASS = "com.day.jcr.vault.cli.VaultFsApp";

    protected static ClassLoader vaultLoader;

    protected PipedOutputStream consoleOut;

    protected VaultTransactionDTO data;

    protected boolean isError = false;

    protected Localization local = Localization.get(VaultSvc.class);

    protected Logger log = new Logger(VaultSvc.class.getName());

    protected String message = "";

    protected PrintStream sysOut;

    protected Thread writerThread;

    public static final String VAULTCLIPSE_TEMP_DIR = "vaultclipse";

    /**
	 * Construct a new VaultService with the specified transaction data.
	 * 
	 * @param data
	 *            the transaction data for this transaction
	 */
    public VaultSvc(VaultTransactionDTO data) {
        this.data = data;
        fileHelper = new FileHelper(data.getIgnorePattern());
    }

    /**
	 * Creates the temporary directory for Vault to use.
	 * 
	 * @param monitor
	 *            the Eclipse progress monitor
	 * @return the temporary directory
	 */
    protected File createTemporaryDirectory(IProgressMonitor monitor) {
        log.debug("createTemporaryDirectory");
        monitor.setTaskName(local.getString(ServiceMessages.CREATING_TEMP_DIRECTORY.getKey()));
        File tempDir = data.getTempDir();
        File baseDir = new File(tempDir.getAbsolutePath() + File.separator + VAULTCLIPSE_TEMP_DIR + File.separator + System.currentTimeMillis());
        baseDir.mkdirs();
        log.debug("Directory created: " + baseDir.getAbsolutePath());
        monitor.worked(5);
        return baseDir;
    }

    /**
	 * Exports files from vault.
	 * 
	 * @param monitor
	 *            the monitor to update with progress
	 * @throws IOException
	 *             an IO Exception occurs, generally meaning on of the
	 *             properties were not set properly or the Vault can't access
	 *             the repository
	 * @throws ClassNotFoundException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws SecurityException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws NoSuchMethodException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalArgumentException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalAccessException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws InvocationTargetException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws CoreException
	 *             an eclipse exception occurs refreshing the view
	 */
    public void export(IProgressMonitor monitor, boolean ignoreWhitespace) throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CoreException {
        log.debug("export");
        File exportBaseDir = null;
        try {
            monitor.beginTask(local.getString(ServiceMessages.STARTING_EXPORT.getKey()), 100);
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            exportBaseDir = createTemporaryDirectory(monitor);
            monitor.setTaskName(local.getString(ServiceMessages.CREATING_CONFIG_FILES.getKey()));
            log.debug("Creating filter file");
            File filterFile = new File(exportBaseDir.getAbsolutePath() + File.separator + "filter.xml");
            List<String> jcrPaths = new ArrayList<String>();
            for (VaultTransactionItemDTO item : data.getItems()) {
                jcrPaths.add(item.getJcrPath());
            }
            VaultHelper.createFilter(filterFile, jcrPaths);
            monitor.worked(10);
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            String[] args = new String[] { "co", "--filter", filterFile.getAbsolutePath(), data.getRepositoryURL(), VaultHelper.convertFiletoVaultPath(exportBaseDir), "--credentials", data.getUsername() + ":" + data.getPassword() };
            log.debug("Export arguments: " + Arrays.toString(args));
            invokeVault(monitor, args);
            if (isError) {
                throw new RuntimeException(getMessage());
            }
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            monitor.setTaskName(local.getString(ServiceMessages.COPYING_EXPORTED_FILES.getKey()));
            log.debug("Copying files");
            for (VaultTransactionItemDTO item : data.getItems()) {
                File copyRoot = new File(exportBaseDir.getAbsolutePath() + File.separator + "jcr_root" + item.getJcrPath().replace("/", File.separator));
                fileHelper.copyDirectory(copyRoot, item.getSelectedFile(), new FileComparator(ignoreWhitespace));
            }
            monitor.worked(10);
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            monitor.setTaskName(local.getString(ServiceMessages.REFRESHING_LOCAL_VIEW.getKey()));
            log.debug("Refreshing local view");
            for (VaultTransactionItemDTO item : data.getItems()) {
                IResource resource = item.getSelectedResource();
                if (resource == null && item.getTreePath().getLastSegment() instanceof IResource) {
                    resource = (IResource) item.getTreePath().getLastSegment();
                }
                if (resource != null) {
                    resource.refreshLocal(IResource.DEPTH_INFINITE, null);
                }
            }
            log.debug("Export complete");
        } finally {
            try {
                log.debug("Cleaning up temporary files");
                monitor.setTaskName(local.getString(ServiceMessages.CLEANING_TEMPORARY_FILES.getKey()));
                if (exportBaseDir != null) {
                    fileHelper.deleteAll(exportBaseDir);
                }
                monitor.done();
            } catch (Throwable t) {
                log.debug("Caught exception deleting temp files: " + t.getMessage());
            }
        }
    }

    /**
	 * Gets the exception message retrieved from vault. Will halt the current
	 * thread until the entire message has been read.
	 * 
	 * @return the exception message
	 */
    protected synchronized String getMessage() {
        log.debug("getMessage");
        while (writerThread.isAlive()) {
            try {
                Thread.currentThread().wait(200L);
            } catch (InterruptedException e) {
            }
        }
        return message;
    }

    /**
	 * Invoke vault to perform the actions specified in the arguments.
	 * 
	 * @param args
	 *            the arguments passed into vault
	 * @param monitor
	 *            the monitor to update with progress
	 * @throws IOException
	 *             an IO Exception occurs, generally meaning on of the
	 *             properties were not set properly or the Vault can't access
	 *             the repository
	 * @throws ClassNotFoundException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws SecurityException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws NoSuchMethodException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalArgumentException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalAccessException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws InvocationTargetException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws CoreException
	 *             an eclipse exception occurs refreshing the view
	 */
    protected void invokeVault(IProgressMonitor monitor, String[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException, ClassNotFoundException {
        log.debug("invokeVault");
        if (monitor != null) {
            monitor.setTaskName(local.getString(ServiceMessages.LOADING_VAULT_DEPENDENCIES.getKey()));
        }
        loadJars(data.getVaultPath());
        if (monitor != null && monitor.isCanceled()) {
            log.debug("Operation canceled");
            return;
        }
        log.debug("Getting vault through reflection");
        Class<?> mainClass = Class.forName(VAULT_CLASS, true, vaultLoader);
        Method main = mainClass.getMethod("main", new Class[] { java.lang.String[].class });
        if (data.isVerbose()) {
            log.debug("Setting vault to run in verbose mode");
            List<String> argsList = new ArrayList<String>();
            for (int i = 0; i < args.length; i++) {
                argsList.add(args[i]);
            }
            argsList.add("--verbose");
            args = argsList.toArray(new String[argsList.size()]);
        }
        if (monitor != null) {
            monitor.worked(5);
            monitor.setTaskName(local.getString(ServiceMessages.RUNNING_VAULT.getKey()));
        }
        log.debug("Invoking vault");
        redirectSysOut();
        main.invoke(null, new Object[] { args });
        restoreSysOut();
        log.debug("Vault complete");
        if (monitor != null) {
            monitor.worked(30);
        }
    }

    /**
	 * Load the VaultJars into the vaultLoader class loader.
	 * 
	 * @param vaultDir
	 *            the directory to load. If null or empty exception will be
	 *            thrown
	 * @throws IOException
	 *             an exception occurs loading the jars
	 */
    protected void loadJars(String vaultDir) throws IOException {
        log.debug("loadJars");
        if (!jarsLoaded) {
            if (vaultDir == null || vaultDir.trim().length() == 0) {
                throw new IOException("Vault Directory not set");
            }
            if (!vaultDir.endsWith("lib")) {
                vaultDir += File.separator + "lib";
            }
            File[] vaultLibs = new File(vaultDir).listFiles();
            URL[] urls = new URL[vaultLibs.length];
            for (int i = 0; i < vaultLibs.length; i++) {
                File lib = vaultLibs[i];
                log.debug("Adding library: " + lib.getAbsolutePath());
                urls[i] = lib.toURI().toURL();
            }
            log.debug("Libraries loaded");
            vaultLoader = URLClassLoader.newInstance(urls, getClass().getClassLoader());
            jarsLoaded = true;
        }
    }

    /**
	 * Exports files from vault.
	 * 
	 * @param monitor
	 *            the monitor to update with progress
	 * @throws IOException
	 *             an IO Exception occurs, generally meaning on of the
	 *             properties were not set properly or the Vault can't access
	 *             the repository
	 * @throws ClassNotFoundException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws SecurityException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws NoSuchMethodException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalArgumentException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalAccessException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws InvocationTargetException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws CoreException
	 *             an eclipse exception occurs refreshing the view
	 */
    public MergeContentProvider merge(IProgressMonitor monitor, PropertyStore prop) throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CoreException {
        log.debug("export");
        File exportBaseDir = null;
        MergeContentProvider provider = null;
        monitor.beginTask(local.getString(ServiceMessages.STARTING_EXPORT.getKey()), 100);
        if (monitor.isCanceled()) {
            log.debug("Operation canceled");
            return provider;
        }
        exportBaseDir = createTemporaryDirectory(monitor);
        monitor.setTaskName(local.getString(ServiceMessages.CREATING_CONFIG_FILES.getKey()));
        log.debug("Creating filter file");
        File filterFile = new File(exportBaseDir.getAbsolutePath() + File.separator + "filter.xml");
        List<String> jcrPaths = new ArrayList<String>();
        for (VaultTransactionItemDTO item : data.getItems()) {
            jcrPaths.add(item.getJcrPath());
        }
        VaultHelper.createFilter(filterFile, jcrPaths);
        monitor.worked(10);
        if (monitor.isCanceled()) {
            log.debug("Operation canceled");
            return provider;
        }
        String[] args = new String[] { "co", "--filter", filterFile.getAbsolutePath(), data.getRepositoryURL(), VaultHelper.convertFiletoVaultPath(exportBaseDir), "--credentials", data.getUsername() + ":" + data.getPassword() };
        log.debug("Export arguments: " + Arrays.toString(args));
        invokeVault(monitor, args);
        if (isError) {
            throw new RuntimeException(getMessage());
        }
        if (monitor.isCanceled()) {
            log.debug("Operation canceled");
            return provider;
        }
        monitor.setTaskName("Comparing files...");
        log.debug("Comparing files");
        provider = new MergeContentProvider(data, exportBaseDir, prop);
        log.debug("Merge complete");
        return provider;
    }

    /**
	 * Redirects the System.out stream to the Eclipse console.
	 * 
	 * @throws IOException
	 *             an exception occurs redirecting the output stream
	 */
    protected void redirectSysOut() throws IOException {
        log.debug("redirectSysOut");
        sysOut = System.out;
        final PipedInputStream in = new PipedInputStream();
        consoleOut = new PipedOutputStream(in);
        System.setOut(new PrintStream(consoleOut));
        final IOConsoleOutputStream console = VaultClipsePlugin.getDefault().getConsole().newOutputStream();
        console.setEncoding("UTF-8");
        writerThread = new Thread() {

            public void run() {
                log.debug("Starting input listener");
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    PrintStream writer = new PrintStream(console);
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        if (!isError && line.contains("[ERROR]")) {
                            isError = true;
                            message = line;
                        }
                        writer.println(line);
                    }
                } catch (IOException e) {
                    log.error("Exception reading output stream", e);
                }
                log.debug("Input reader closing");
            }
        };
        writerThread.start();
    }

    /**
	 * Restores the System.out stream to it's default.
	 * 
	 * @throws IOException
	 *             an exception occurs restoring the output stream
	 */
    protected void restoreSysOut() throws IOException {
        log.debug("restoreSysOut");
        System.setOut(sysOut);
        consoleOut.close();
    }

    /**
	 * Validate that the Vault Directory is set correctly.
	 * 
	 * @return true if able to invoke vault, false otherwise
	 */
    public boolean validateVaultDir() {
        log.debug("validateVaultDir");
        try {
            String[] args = new String[] { "--version" };
            log.debug("Validating vault configuration");
            invokeVault(null, args);
            return true;
        } catch (IOException e) {
            log.warn("Caught File IO Exception, Vault directory probably doesn't exist.", e);
            return false;
        } catch (Exception e) {
            log.warn("Caught Exception of type: " + e.getClass().getName() + " vault directory probably incorrect.", e);
            return false;
        }
    }

    /**
	 * Imports files into vault.
	 * 
	 * @param monitor
	 *            the monitor to update with progress
	 * @throws IOException
	 *             an IO Exception occurs, generally meaning on of the
	 *             properties were not set properly or the Vault can't access
	 *             the repository
	 * @throws ClassNotFoundException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws SecurityException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws NoSuchMethodException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalArgumentException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws IllegalAccessException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws InvocationTargetException
	 *             the program was unable to load the vault classes, the path to
	 *             vault is wrong
	 * @throws CoreException
	 *             an eclipse exception occurs refreshing the view
	 */
    public void vltImport(IProgressMonitor monitor) throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CoreException {
        log.debug("import");
        File importBaseDir = null;
        try {
            monitor.beginTask(local.getString(ServiceMessages.STARTING_IMPORT.getKey()), 100);
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            importBaseDir = createTemporaryDirectory(monitor);
            monitor.setTaskName(local.getString(ServiceMessages.CREATING_CONFIG_FILES.getKey()));
            log.debug("Creating filter file");
            File filterConfigFldr = new File(importBaseDir.getAbsolutePath() + File.separator + "META-INF" + File.separator + "vault");
            filterConfigFldr.mkdirs();
            File filterFile = new File(filterConfigFldr.getAbsolutePath() + File.separator + "filter.xml");
            List<String> jcrPaths = new ArrayList<String>();
            for (VaultTransactionItemDTO item : data.getItems()) {
                jcrPaths.add(item.getJcrPath());
            }
            VaultHelper.createFilter(filterFile, jcrPaths);
            fileHelper.toFile(getClass().getClassLoader().getResourceAsStream("com/cp/vaultclipse/config.xml"), new File(filterConfigFldr.getAbsolutePath() + File.separator + "config.xml"));
            fileHelper.toFile(getClass().getClassLoader().getResourceAsStream("com/cp/vaultclipse/settings.xml"), new File(filterConfigFldr.getAbsolutePath() + File.separator + "settings.xml"));
            monitor.worked(10);
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            monitor.setTaskName(local.getString(ServiceMessages.COPYING_IMPORT_FILES.getKey()));
            log.debug("Copying files");
            for (VaultTransactionItemDTO item : data.getItems()) {
                File copyRoot = new File(importBaseDir.getAbsolutePath() + File.separator + "jcr_root" + item.getJcrPath().replace("/", File.separator));
                if (!copyRoot.exists()) {
                    copyRoot.mkdirs();
                }
                fileHelper.copyDirectory(item.getSelectedFile(), copyRoot);
            }
            if (monitor.isCanceled()) {
                log.debug("Operation canceled");
                return;
            }
            monitor.worked(20);
            String[] args = new String[] { "--credentials", data.getUsername() + ":" + data.getPassword(), "import", data.getRepositoryURL(), VaultHelper.convertFiletoVaultPath(importBaseDir), "/" };
            log.debug("Import arguments: " + Arrays.toString(args));
            invokeVault(monitor, args);
            if (isError) {
                throw new RuntimeException(getMessage());
            }
            log.debug("Import complete");
        } finally {
            try {
                log.debug("Cleaning up temporary files");
                monitor.setTaskName(local.getString(ServiceMessages.CLEANING_TEMPORARY_FILES.getKey()));
                if (importBaseDir != null) {
                    fileHelper.deleteAll(importBaseDir);
                }
                monitor.done();
            } catch (Throwable t) {
                log.debug("Caught exception deleting temp files: " + t.getMessage());
            }
        }
    }
}
