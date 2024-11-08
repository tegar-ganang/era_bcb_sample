package unclej.utasks.file;

import unclej.filepath.FileSpec;
import unclej.filepath.Filelike;
import unclej.filepath.PathSpec;
import unclej.framework.FailurePolicies;
import unclej.framework.FailurePolicy;
import unclej.framework.UTask;
import unclej.framework.UTaskFailedException;
import unclej.log.ULog;
import unclej.util.Counter;
import unclej.util.EngineeringFormat;
import unclej.util.FlagBuffer;
import unclej.util.Quote;
import unclej.validate.AutoValidate;
import unclej.validate.NonNull;
import unclej.validate.ValidationException;
import unclej.validate.Writable;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.Format;
import java.text.MessageFormat;

/**
 * Copies a single file or set of files to a target location.
 * @author scottv
 */
public class CopyUTask implements UTask {

    public CopyUTask() {
    }

    public CopyUTask(File from, File to) {
        setFrom(new PathSpec(from));
        setTo(to);
    }

    public CopyUTask(FileSpec from, File to) {
        setFrom(from);
        setTo(to);
    }

    public void setFrom(FileSpec from) {
        this.from = from;
    }

    public void setTo(File to) {
        this.to = to;
    }

    public void describe(ULog log, boolean simulate) {
        FlagBuffer flags = new FlagBuffer();
        flags.add(overwrite, "overwrite");
        flags.add(preserveTimeStamp, "preserve timestamp");
        log.info("copying {0}{1} to {2}", flags, from, new Quote(to));
    }

    public void validate() throws ValidationException {
        AutoValidate.basic(this);
    }

    public void execute(ULog log) throws UTaskFailedException {
        AutoValidate.full(this);
        Counters counters = new Counters();
        for (Filelike source : from.listMatches()) {
            File dest = new File(to, source.getRelativeName());
            boolean isDir = source.isDirectory();
            if (isDir && !excludeEmptyDirs) {
                if (!copyDir(dest, log)) {
                    counters.getFailures().increment();
                }
            } else if (!isDir && (!isUpToDate(source, dest) || overwrite)) {
                processFile(source, dest, counters, log);
            } else {
                counters.getSkipped().increment();
            }
        }
        log.fine("{0} files ({1} bytes) copied; {2} files skipped", counters.getSuccesses(), SIZE_FORMAT.format(counters.getTotalBytes()), counters.getSkipped());
        if (counters.getFailures().intValue() > 0) {
            String message = MessageFormat.format("failed to copy {0} files from {1}", counters.getFailures(), new Quote(from));
            throw new UTaskFailedException(this, message);
        }
    }

    private void processFile(Filelike source, File dest, Counters counters, ULog log) throws UTaskFailedException {
        if (copyDir(dest.getParentFile(), log)) {
            if (copyFile(source, dest, source.getSize(), log)) {
                counters.getSuccesses().increment();
                counters.getTotalBytes().increment(source.getSize());
                if (preserveTimeStamp) {
                    dest.setLastModified(source.getModTime());
                }
            } else {
                counters.getFailures().increment();
            }
        } else {
            counters.getFailures().increment();
        }
    }

    protected boolean isUpToDate(Filelike source, File dest) {
        return dest.exists() && dest.length() == source.getSize() && dest.lastModified() >= source.getModTime();
    }

    protected boolean copyFile(Filelike source, File dest, long size, ULog log) throws UTaskFailedException {
        boolean retryFile = true;
        while (retryFile) {
            log.fine("copying {0} ({1} bytes)", new Quote(source), SIZE_FORMAT.format(size));
            try {
                if (source.toFile() != null) {
                    channelCopy(source.toFile(), dest);
                } else {
                    streamCopy(source.getInputStream(), dest);
                }
                return true;
            } catch (IOException x) {
                String message = MessageFormat.format("failed to copy {0} to {1}", new Quote(source), new Quote(dest));
                retryFile = fileFailurePolicy.handle(this, log, x, message);
            }
        }
        return false;
    }

    private void channelCopy(File source, File dest) throws IOException {
        FileChannel srcChannel = new FileInputStream(source).getChannel();
        FileChannel dstChannel = new FileOutputStream(dest).getChannel();
        try {
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            srcChannel.close();
            dstChannel.close();
        }
    }

    private void streamCopy(InputStream in, File dest) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        try {
            byte[] buf = new byte[BUF_SIZE];
            int byteCount = 0;
            while (byteCount >= 0) {
                byteCount = in.read(buf);
                if (byteCount > 0) {
                    out.write(buf, 0, byteCount);
                }
            }
        } finally {
            in.close();
            out.flush();
            out.close();
        }
    }

    private boolean copyDir(File dest, ULog log) throws UTaskFailedException {
        boolean retry = true;
        while (retry) {
            dest.mkdirs();
            if (dest.isDirectory()) {
                return true;
            } else {
                retry = fileFailurePolicy.handle(this, log, null, "could not create " + new Quote(dest));
            }
        }
        return false;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setPreserveTimeStamp(boolean preserveTimeStamp) {
        this.preserveTimeStamp = preserveTimeStamp;
    }

    public void setExcludeEmptyDirs(boolean excludeEmptyDirs) {
        this.excludeEmptyDirs = excludeEmptyDirs;
    }

    /**
   * Sets the failure policy to use immediately after a file cannot be copied. The default is
   * {@link unclej.framework.FailurePolicies#ERROR}. Even if this failure policy does not throw an exception, a
   * {@link UTaskFailedException} will be thrown when the task completes with failures.  Use a
   * {@link unclej.utasks.framework.FailurePolicyUTask} to establish a different policy at the task level.
   * @param fileFailurePolicy policy to use immediately after a file cannot be deleted
   */
    public void setFileFailurePolicy(FailurePolicy fileFailurePolicy) {
        this.fileFailurePolicy = fileFailurePolicy;
    }

    protected FailurePolicy getFileFailurePolicy() {
        return fileFailurePolicy;
    }

    @NonNull
    private FileSpec from;

    @NonNull
    @Writable
    private File to;

    private boolean overwrite;

    private boolean preserveTimeStamp;

    private boolean excludeEmptyDirs;

    private FailurePolicy fileFailurePolicy = FailurePolicies.ERROR;

    protected static final int BUF_SIZE = 1024 * 1024;

    protected static final Format SIZE_FORMAT = new EngineeringFormat(null);

    private class Counters {

        public Counter getSuccesses() {
            return successes;
        }

        public Counter getFailures() {
            return failures;
        }

        public Counter getSkipped() {
            return skipped;
        }

        public Counter getTotalBytes() {
            return totalBytes;
        }

        private Counter successes = new Counter();

        private Counter failures = new Counter();

        private Counter skipped = new Counter();

        private Counter totalBytes = new Counter();
    }
}
