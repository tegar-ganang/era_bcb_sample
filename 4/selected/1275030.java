package net.sf.buildbox.worker.subversion;

import net.sf.buildbox.util.*;
import net.sf.buildbox.worker.api.WorkerFeedback;
import net.sf.buildbox.worker.api.WorkerPlugin;
import net.sf.buildbox.worker.util.FireRamp;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

public class SvnFetchWorkerPlugin implements WorkerPlugin {

    private Lock lock;

    private static final String SHARED_WC_PREFIX = "shared";

    private static final String SHARED_WC_SUFFIX = "-wc";

    @Inject
    boolean copyEmptyDirs = true;

    @Inject
    int sharedWcTimeout = 5 * 60000;

    @Inject
    boolean updatePerPartes = false;

    @Inject
    String revision;

    @Inject
    String locationPath;

    @Inject
    String svnRootUrl;

    @Inject
    @Named("ec.cache")
    File ecCache;

    @Inject
    @Named("ec.CODE")
    File ecCode;

    @Inject
    File svnHome;

    /**
     * Prepares shared working copy; that is, a locked directory that can be used for svn checkout/update operations
     * Also, locks it and stores the lock to property {@link #lock}
     *
     * @param projectCacheDir root dir where creating project-scope caches is allowed
     * @return directory reserved for creation/update of a shared working copy. Not necessarily existing upon return.
     * @throws java.io.IOException  -
     * @throws InterruptedException -
     * @throws org.codehaus.plexus.util.cli.CommandLineException
     *                              -
     */
    private File prepareLockedWorkingCopy(final WorkerFeedback workerFeedback, final File projectCacheDir) throws InterruptedException, CommandLineException, IOException {
        final List<File> sharedWorkingCopies = listSharedWc(projectCacheDir);
        File sharedWorkingCopy = null;
        if (!sharedWorkingCopies.isEmpty()) {
            workerFeedback.console("trying to reuse working copies - waiting %d millis for lock on %s", sharedWcTimeout, sharedWorkingCopies);
            final AbstractSampler<File> sampler = new AbstractSampler<File>() {

                @Override
                protected File scan() {
                    final List<File> currentWcList = listSharedWc(projectCacheDir);
                    for (File dir : currentWcList) {
                        final ExtFileLock l = new ExtFileLock(true, dir);
                        if (l.tryLock()) {
                            lock = l;
                            workerFeedback.console("%s was unlocked, using it", dir);
                            return dir;
                        }
                        workerFeedback.console("%s is still locked", dir);
                    }
                    return null;
                }
            };
            sharedWorkingCopy = sampler.produceObject(sharedWcTimeout, 15 * 1000);
        }
        if (sharedWorkingCopy == null) {
            final File tmp = File.createTempFile(SHARED_WC_PREFIX + "-", "-" + revision + SHARED_WC_SUFFIX + ".tmp", projectCacheDir);
            final String tmps = tmp.getAbsolutePath();
            sharedWorkingCopy = new File(tmps.substring(0, tmps.length() - 4));
            tmp.delete();
            workerFeedback.console("%s generated, locking it for use", sharedWorkingCopy);
            sharedWorkingCopy.mkdirs();
            lock = new ExtFileLock(true, sharedWorkingCopy);
            lock.lockInterruptibly();
        }
        return sharedWorkingCopy;
    }

    private List<File> listSharedWc(File projectCacheDir) {
        if (!projectCacheDir.isDirectory()) {
            return Collections.emptyList();
        }
        final File[] sharedWorkingCopies = projectCacheDir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                if (!file.isDirectory()) return false;
                final String name = file.getName();
                if (!name.startsWith(SHARED_WC_PREFIX)) return false;
                if (!name.endsWith(SHARED_WC_SUFFIX)) return false;
                return true;
            }
        });
        Arrays.sort(sharedWorkingCopies, new Comparator<File>() {

            public int compare(File o1, File o2) {
                if (o1.lastModified() < o2.lastModified()) return 1;
                return o1.lastModified() == o2.lastModified() ? 0 : -1;
            }
        });
        return Arrays.asList(sharedWorkingCopies);
    }

    public boolean execute(WorkerFeedback workerFeedback) throws Exception {
        if (revision == null) {
            throw new NullPointerException("No revision specified");
        }
        boolean ok = executeAttempt(workerFeedback);
        int attempt = 0;
        while (!ok) {
            attempt++;
            workerFeedback.console("trying again (#%d)", attempt);
            workerFeedback.reportStatusMessage(String.format("trying again (#%d)", attempt));
            ok = executeAttempt(workerFeedback);
        }
        return true;
    }

    private boolean executeAttempt(WorkerFeedback workerFeedback) throws Exception {
        ecCache.mkdirs();
        try {
            final File sharedWorkingCopy = prepareLockedWorkingCopy(workerFeedback, ecCache);
            final File revFile = new File(sharedWorkingCopy.getAbsolutePath() + ".rev");
            final String currentRev = revFile.exists() ? FileUtils.fileRead(revFile) : "---";
            final File cdir = new File(sharedWorkingCopy, ".svn");
            boolean ok = revision.equals(currentRev);
            if (ok) {
                workerFeedback.console("revision %s already retrieved, no svn interaction needed", revision);
            } else if (cdir.exists()) {
                revFile.delete();
                workerFeedback.console("updating from %s to revision %s", currentRev, revision);
                if (!svnFetch(workerFeedback, sharedWorkingCopy, true)) {
                    scheduleRemoval(sharedWorkingCopy);
                    return false;
                }
            } else {
                workerFeedback.console("retrieving revision %s", revision);
                if (!svnFetch(workerFeedback, sharedWorkingCopy, false)) {
                    scheduleRemoval(sharedWorkingCopy);
                    throw new IOException("svn checkout failed");
                }
            }
            FileUtils.fileWrite(revFile.getAbsolutePath(), revision);
            workerFeedback.reportStatusMessage(String.format("fetched revision %s", revision));
            workerFeedback.console("copying files from %s to %s", sharedWorkingCopy, ecCode);
            final AtomicInteger emptyDirCount = new AtomicInteger();
            final AtomicLong byteCount = new AtomicLong();
            final int fileCount = localFileExport(workerFeedback, sharedWorkingCopy, ecCode, emptyDirCount, byteCount);
            workerFeedback.reportStatusMessage(String.format("fetched %s in %d files", BbxSystemUtils.byteCountToDisplaySize((int) byteCount.get()), fileCount));
            if (emptyDirCount.get() > 0) {
                workerFeedback.setStatusProperty("emptydirs.warning", emptyDirCount.get() + " empty dirs present");
            }
            if (byteCount.get() == 0 && fileCount == 0) {
                throw new IOException("Nothing really fetched");
            }
            return true;
        } finally {
            if (lock != null) {
                workerFeedback.console("unlocking: %s", lock);
                lock.unlock();
                lock = null;
            }
        }
    }

    private void scheduleRemoval(File sharedWorkingCopy) {
        final File deleteMe = new File(sharedWorkingCopy.getAbsolutePath() + ".DELETEME");
        sharedWorkingCopy.renameTo(deleteMe);
    }

    private int localFileExport(final WorkerFeedback workerFeedback, File sharedWorkingCopy, final File exportTo, final AtomicInteger emptyDirCount, final AtomicLong byteCount) throws IOException {
        final int prefixSize = sharedWorkingCopy.getAbsolutePath().length() + 1;
        final AtomicInteger fileCount = new AtomicInteger();
        final PushingDirectoryTraversal t = new PushingDirectoryTraversal(new FileFilter() {

            private static final long LOGGING_INTERVAL = 5000;

            private long nextLogTime = System.currentTimeMillis() + LOGGING_INTERVAL;

            public boolean accept(File srcFile) {
                final String relpath = srcFile.getAbsolutePath().substring(prefixSize);
                final File targetFile = new File(exportTo, relpath);
                if (srcFile.isDirectory()) {
                    if (srcFile.list().length == 1) {
                        targetFile.mkdirs();
                        emptyDirCount.incrementAndGet();
                        workerFeedback.console(" ... create empty dir: %s", relpath);
                        updateProgress();
                    }
                    return true;
                }
                targetFile.getParentFile().mkdirs();
                try {
                    FileUtils.copyFile(srcFile, targetFile);
                    fileCount.incrementAndGet();
                    byteCount.addAndGet(targetFile.length());
                    updateProgress();
                    return true;
                } catch (IOException e) {
                    throw new IllegalStateException(String.format("problem while copying %s to %s", srcFile, targetFile), e);
                }
            }

            private void updateProgress() {
                if (System.currentTimeMillis() > nextLogTime) {
                    nextLogTime = System.currentTimeMillis() + LOGGING_INTERVAL;
                    workerFeedback.console(" ... copied %d bytes in %d files so far", byteCount.get(), fileCount.get());
                }
            }
        });
        t.addDirectoryExclude(".svn");
        t.setWantDirs(copyEmptyDirs);
        t.setWantFiles(true);
        final long startTime = System.currentTimeMillis();
        t.traverse(sharedWorkingCopy);
        workerFeedback.console("Copied %d files in %d millis", fileCount.get(), System.currentTimeMillis() - startTime);
        if (copyEmptyDirs && emptyDirCount.intValue() > 0) {
            workerFeedback.console("Also created %d empty directories", emptyDirCount.get());
        }
        return fileCount.get();
    }

    private boolean svnFetch(final WorkerFeedback workerFeedback, File sharedWorkingCopy, boolean updateOnly) throws Exception {
        final BbxCommandline cl = new BbxCommandline();
        if (svnHome != null) {
            cl.setExecutable(new File(svnHome, "bin/svn").getAbsolutePath());
        } else {
            cl.setExecutable("svn");
        }
        if (updateOnly) {
            cl.addArguments(new String[] { "update", "--revision", revision });
            if (updatePerPartes) {
                final String[] subpaths = sharedWorkingCopy.list(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return !name.startsWith(".");
                    }
                });
                cl.addArguments(subpaths);
                cl.addArguments(new String[] { "." });
            }
        } else {
            sharedWorkingCopy.mkdirs();
            final String svnUrl = svnRootUrl + locationPath;
            cl.addArguments(new String[] { "checkout", svnUrl + "@" + revision, "." });
        }
        cl.addArguments(new String[] { "--non-interactive", "--ignore-externals", "--trust-server-cert" });
        cl.setWorkingDirectory(sharedWorkingCopy);
        final FireRamp fireRamp = new FireRamp("svn-fetch", cl, 0);
        fireRamp.setWorkerFeedback(workerFeedback);
        fireRamp.addStdErrConsumer(new StreamConsumer() {

            public void consumeLine(String line) {
                workerFeedback.setStatusProperty("STDERR", line);
            }
        });
        workerFeedback.reportStatusMessage(CommandLineUtils.toString(cl.getCommandline()));
        return fireRamp.call() == 0;
    }
}
