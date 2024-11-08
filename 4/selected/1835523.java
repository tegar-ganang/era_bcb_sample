package org.happycomp.radiog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.internal.resolver.StateImpl;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.happycomp.radio.StoreState;
import org.happycomp.radio.StoreStateException;
import org.happycomp.radio.downloader.DownloadingItem;
import org.happycomp.radio.guice.RadioModule;
import org.happycomp.radio.io.IOUtils;
import org.happycomp.radio.scheduler.Scheduler;
import org.happycomp.radio.scheduler.SchedulerListener;
import org.osgi.framework.BundleContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    private static final String RADIOG_AND_VERSION = ".radiog_1_x_0";

    private static final String STATE_POSTFIX = "_state";

    public static final String PLUGIN_ID = "plg";

    private static Activator plugin;

    private Injector injector;

    private ShutdownSystem shutdownSystem = new ShutdownSystem();

    /**
	 * The constructor
	 */
    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        this.injector = Guice.createInjector(new RadioModule(), new DownloaderRuntimeModule());
        File baseDir = new File(Activator.baseDir());
        if (!baseDir.exists()) if (!baseDir.mkdirs()) {
            String message = "cannot create directory " + baseDir();
            MessageDialog.openError(null, "creating directory", message);
            throw new IllegalStateException(message);
        }
        File[] listFolders = baseDir.listFiles();
        for (File oneFolder : listFolders) {
            if (oneFolder.isDirectory()) {
                IOUtils.deleteDirectory(oneFolder);
            }
        }
        getRadioScheduler().addSchedulerListener(this.shutdownSystem);
    }

    public void stop(BundleContext context) throws Exception {
        DownloadingItem[] downloadingItems = getRadioScheduler().getDownloader().getDownloadingItems();
        for (DownloadingItem itm : downloadingItems) {
            if (!itm.isPersistedItem()) {
                itm.getFile().deleteOnExit();
            }
        }
        plugin = null;
        this.injector = null;
        super.stop(context);
    }

    /**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
    public static Activator getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public Injector getInjector() {
        return this.injector;
    }

    public Scheduler getRadioScheduler() {
        return this.injector.getInstance(Scheduler.class);
    }

    public StoreState getStoreInfos() {
        return this.injector.getInstance(StoreState.class);
    }

    public String getOggDecCommand() throws IOException {
        File oggDecDir = new File(baseDir());
        if (!oggDecDir.exists()) {
            boolean mkdirs = oggDecDir.mkdirs();
            if (!mkdirs) throw new IOException("cannot create directory '" + mkdirs + "'");
        }
        String retVal = baseDir() + File.separator + "oggdec.exe";
        if (!new File(retVal).exists()) {
            String oggCommandFile = "oggdec.exe";
            String toWavFile = "toWav.bat";
            String[] resources = { oggCommandFile, toWavFile };
            String resPrefix = "res/";
            copyResources(oggDecDir, resources, resPrefix);
        }
        return retVal;
    }

    public String getLameCommand() throws FileNotFoundException, IOException {
        String retVal = baseDir() + File.separator + "lame.exe";
        copyDefaultExecs();
        return retVal;
    }

    public String getSoxCommand() throws FileNotFoundException, IOException {
        String retVal = baseDir() + File.separator + "sox.exe";
        copyDefaultExecs();
        return retVal;
    }

    public Map<String, String> getCommandsMap() throws FileNotFoundException, IOException {
        Map<String, String> commands = new HashMap<String, String>();
        commands.put("sox", getSoxCommand());
        commands.put("oggdec", getOggDecCommand());
        commands.put("lame", getLameCommand());
        return commands;
    }

    private void copyDefaultExecs() throws IOException, FileNotFoundException {
        File lameDir = new File(baseDir());
        if (!lameDir.exists()) {
            boolean mkdirs = lameDir.mkdirs();
            if (!mkdirs) throw new IOException("cannot create directory '" + mkdirs + "'");
        }
        String lameCommandFile = "lame.exe";
        String soxCommand = "sox.exe";
        String[] resources = { lameCommandFile, "lame_enc.dll", "toWavToMP3.bat", "toWavToMp3Delete.bat", soxCommand, "libgomp-1.dll", "pthreadgc2.dll", "zlib1.dll" };
        String resPrefix = "res/";
        copyResources(lameDir, resources, resPrefix);
    }

    private void copyResources(File oggDecDir, String[] resources, String resPrefix) throws FileNotFoundException, IOException {
        for (int i = 0; i < resources.length; i++) {
            String res = resPrefix + resources[i];
            InputStream is = this.getClass().getResourceAsStream(res);
            if (is == null) throw new IllegalArgumentException("cannot find resource '" + res + "'");
            File file = new File(oggDecDir, resources[i]);
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    IOUtils.copyStreams(is, fos);
                } finally {
                    fos.close();
                }
            }
        }
    }

    public static String baseDir() {
        return System.getProperty("user.home") + File.separator + RADIOG_AND_VERSION;
    }

    public static String stateDir() {
        return baseDir() + STATE_POSTFIX;
    }

    class ShutdownSystem implements SchedulerListener {

        @Override
        public void exceptionOccured(Throwable arg0) {
        }

        @Override
        public void refreshInterval() {
        }

        @Override
        public void shutDown() {
            for (DownloadingItem ditm : getRadioScheduler().getDownloader().getDownloadingItems()) {
                try {
                    Activator.getDefault().getStoreInfos().storeDownloadingItem(ditm);
                } catch (StoreStateException e1) {
                    e1.printStackTrace();
                }
            }
            new SyncExecTemplate(Activator.getDefault().getWorkbench().getDisplay(), new Runnable() {

                @Override
                public void run() {
                    getWorkbench().close();
                    try {
                        String[] programsArgs = { "cmd", "/c", "rundll32", "shell32.dll", "ShellExec_RunDLL", "shutdown", "-s" };
                        System.out.println("Program and arguments :" + Arrays.asList(programsArgs));
                        ProcessBuilder processBuilder = new ProcessBuilder(programsArgs);
                        processBuilder.directory(new File(Activator.baseDir()));
                        Process start = processBuilder.start();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).execute();
        }

        @Override
        public void startDownloading() {
        }

        @Override
        public void stopDownloading(DownloadingItem arg0) {
        }
    }
}
