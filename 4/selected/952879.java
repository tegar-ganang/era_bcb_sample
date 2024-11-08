package jpfm.fs.splitfs;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import jpfm.DirectoryStream;
import jpfm.FileAttributesProvider;
import jpfm.FileDescriptor;
import jpfm.FileFlags;
import jpfm.FileId;
import jpfm.FileType;
import jpfm.FormatterEvent;
import jpfm.JPfmError;
import jpfm.MountListener;
import jpfm.annotations.NonBlocking;
import jpfm.fs.BasicCascadableProvider;
import jpfm.fs.BasicFileSystem;
import jpfm.fs.ReadOnlyRawFileData;
import jpfm.fs.Type.BASIC;
import jpfm.mount.Mount;
import jpfm.operations.AlreadyCompleteException;
import jpfm.operations.Read;
import jpfm.operations.readwrite.Completer;
import jpfm.operations.readwrite.ReadRequest;

/**
 *
 * @author Shashank Tulsyan
 */
public final class CascadableSplitFS implements BasicFileSystem, DirectoryStream {

    private ReadOnlyRawFileData[] partFiles = null;

    private long[] fileSizes = null;

    private long[] cumulativeSize = null;

    private Root root = null;

    private File file = null;

    private long volumeSize = 0;

    static {
    }

    public static final class CascadableSplitFSProvider implements BasicCascadableProvider {

        private final Set<FileAttributesProvider> filesCascadingOver;

        private final String suggestedName;

        public CascadableSplitFSProvider(Set<FileAttributesProvider> filesCascadingOver, String suggestedName) {
            this.filesCascadingOver = filesCascadingOver;
            this.suggestedName = suggestedName;
        }

        public BasicFileSystem getFileSystem(Set<ReadOnlyRawFileData> dataProviders, FileDescriptor parentFD) {
            return new CascadableSplitFS(dataProviders, suggestedName, parentFD);
        }

        public Set<FileAttributesProvider> filesCascadingOver() {
            return filesCascadingOver;
        }

        public int suggestDataGlimpseSize() {
            return 0;
        }

        public String suggestedName() {
            return suggestedName;
        }
    }

    public CascadableSplitFS(Set<ReadOnlyRawFileData> datas, String suggestedName, FileDescriptor parentFD) {
        partFiles = datas.toArray(new ReadOnlyRawFileData[datas.size()]);
        Arrays.sort(partFiles, new Comparator<ReadOnlyRawFileData>() {

            public int compare(ReadOnlyRawFileData o1, ReadOnlyRawFileData o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (!getAllFileSizes()) ;
        String name = partFiles[0].getName();
        root = new Root(suggestedName, parentFD, this);
        file = new File(volumeSize, name.substring(0, name.lastIndexOf('.')), root);
        System.out.println("++++++++++Cumulative Size info++++++++++++");
        for (int i = 0; i < cumulativeSize.length; i++) {
            System.out.println("[" + (i + 1) + "]" + cumulativeSize[i]);
        }
        System.out.println("---------Cumulative Size info------------");
    }

    public final boolean getAllFileSizes() {
        volumeSize = 0;
        synchronized (this) {
            if (partFiles == null) {
                return false;
            }
            fileSizes = new long[partFiles.length];
            cumulativeSize = new long[partFiles.length];
            for (int i = 0; i < partFiles.length; i++) {
                try {
                    fileSizes[i] = partFiles[i].getFileSize();
                    if (i == 0) cumulativeSize[0] = 0 + fileSizes[0]; else cumulativeSize[i] = cumulativeSize[i - 1] + fileSizes[i];
                    volumeSize += fileSizes[i];
                } catch (Exception a) {
                    a.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public FileAttributesProvider getRootAttributes() {
        return root;
    }

    @Override
    public FileAttributesProvider open(String[] filePath) {
        if (filePath.length > 1) {
            return null;
        }
        if (filePath.length == 0) {
            return root;
        }
        if (filePath[0].equalsIgnoreCase(file.getName())) {
            return file;
        }
        return null;
    }

    @Override
    public void open(FileAttributesProvider descriptor) {
    }

    @Override
    public FileAttributesProvider getFileAttributes(jpfm.FileId fileId) {
        if (root.implies(fileId)) {
            return root;
        }
        if (file.implies(fileId)) {
            return file;
        }
        return null;
    }

    @Override
    public DirectoryStream list(jpfm.FileId folderToList) {
        return this;
    }

    @Override
    public void close(jpfm.FileId file) {
    }

    @Override
    public long capacity() {
        if (volumeSize == 0) {
            if (!getAllFileSizes()) {
                return 0;
            }
        }
        return volumeSize;
    }

    @Override
    public void delete(FileId fileToDelete) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public BASIC getType() {
        return null;
    }

    public Mount cascadeMount(BasicCascadableProvider basicCascadable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static final class SplitFSReadCompleter implements Completer {

        private static SplitFSReadCompleter INSTANCE = new SplitFSReadCompleter();

        private SplitFSReadCompleter() {
        }

        public int getBytesFilledTillNow(ReadRequest pendingRequest) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void completeNow(ReadRequest pendingRequest) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public StackTraceElement[] getStackTrace() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public Iterator<FileAttributesProvider> iterator() {
        return new RootIterator(file);
    }

    public FileType getFileType() {
        return root.getFileType();
    }

    public FileDescriptor getFileDescriptor() {
        return root.getFileDescriptor();
    }

    public long getFileSize() {
        return root.getFileSize();
    }

    public long getCreateTime() {
        return root.getCreateTime();
    }

    public long getAccessTime() {
        return root.getAccessTime();
    }

    public long getWriteTime() {
        return root.getWriteTime();
    }

    public long getChangeTime() {
        return root.getChangeTime();
    }

    public String getName() {
        return root.getName();
    }

    public FileDescriptor getParentFileDescriptor() {
        return root.getFileDescriptor();
    }

    public FileFlags getFileFlags() {
        return null;
    }

    @NonBlocking(usesJava1_7NIOClasses = true)
    @Override
    public void read(Read read) throws Exception {
        if (!file.implies(read.getFileId())) {
            read.complete(JPfmError.SUCCESS, 0, SplitFSReadCompleter.INSTANCE);
            return;
        }
        if (volumeSize == 0) {
            if (!getAllFileSizes()) {
                read.complete(JPfmError.END_OF_DATA, 0, SplitFSReadCompleter.INSTANCE);
                return;
            }
        }
        long sizeSum = 0;
        long firstFileOffset = 0;
        long currentFileSize = 0;
        int startIndex = -1;
        for (int i = 0; i < fileSizes.length && (startIndex == -1); i++) {
            currentFileSize = fileSizes[i];
            sizeSum += currentFileSize;
            if (sizeSum > read.getFileOffset()) {
                startIndex = i;
                firstFileOffset = sizeSum - currentFileSize;
            }
        }
        if (startIndex == -1) {
            read.complete(JPfmError.END_OF_DATA, 0, SplitFSReadCompleter.INSTANCE);
            return;
        }
        if (read.getFileOffset() + read.getByteBuffer().capacity() > sizeSum) {
            int lastIndex = startIndex;
            long lastOffset = read.getFileOffset() + read.getByteBuffer().capacity() - 1;
            for (int i = startIndex; i < cumulativeSize.length; i++) {
                if (lastOffset < cumulativeSize[i]) {
                    lastIndex = i;
                    break;
                }
            }
            int[] expectedSize = new int[lastIndex - startIndex + 1];
            for (int i = startIndex; i <= lastIndex; i++) {
                if (i == startIndex) {
                    expectedSize[0] = (int) (cumulativeSize[i] - read.getFileOffset());
                }
            }
            for (int i = startIndex; i <= lastIndex; i++) {
                if (i == startIndex) {
                    expectedSize[0] = (int) (cumulativeSize[i] - read.getFileOffset());
                } else {
                    if (lastOffset < cumulativeSize[i]) {
                        expectedSize[i - startIndex] = (int) (lastOffset - cumulativeSize[i - 1] + 1);
                    } else {
                        expectedSize[i - startIndex] = (int) (fileSizes[i]);
                    }
                }
            }
            SplitRequestCompletionHandler new_splitreqcompletionHandler = new SplitRequestCompletionHandler(expectedSize, read);
            read.setCompleter(new_splitreqcompletionHandler);
            int splitPoint = 0;
            sizeSum = 0;
            read.getByteBuffer().limit(read.getByteBuffer().capacity());
            for (int i = startIndex; i <= lastIndex; i++) {
                if (i == startIndex) {
                    read.getByteBuffer().limit(read.getByteBuffer().capacity());
                    long referencePoint = 0;
                    if (startIndex != 0) referencePoint = cumulativeSize[startIndex - 1];
                    SplitedReadRequest part1 = new SplitedReadRequest((((ByteBuffer) read.getByteBuffer().position(0).limit((int) (cumulativeSize[startIndex] - read.getFileOffset()))).slice()), read.getFileOffset() - referencePoint, new Integer(0), new_splitreqcompletionHandler);
                    partFiles[startIndex].read(part1);
                } else {
                    read.getByteBuffer().limit(read.getByteBuffer().capacity());
                    SplitedReadRequest part2 = new SplitedReadRequest((ByteBuffer) (((ByteBuffer) read.getByteBuffer().position(splitPoint)).slice()).position(0), 0, new Integer(i - startIndex), new_splitreqcompletionHandler);
                    partFiles[startIndex].read(part2);
                }
                splitPoint += expectedSize[i - startIndex];
            }
            read.getByteBuffer().position(0);
        } else {
            try {
                partFiles[startIndex].read(new jpfm.operations.readwrite.RequestWrapper(read, read.getFileOffset() - firstFileOffset));
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
                read.complete(JPfmError.FAILED, 0, SplitFSReadCompleter.INSTANCE);
            }
        }
    }

    static final class SplitedReadRequest implements ReadRequest {

        private final ByteBuffer byteBuffer;

        private final long fileOffset;

        private final AtomicBoolean completed = new AtomicBoolean(false);

        private final SplitRequestCompletionHandler splitRequestCompletionHandler;

        private final Integer myIndex;

        private final long creationTime = System.currentTimeMillis();

        private long completionTime = creationTime;

        private Completer completer = null;

        public SplitedReadRequest(ByteBuffer byteBuffer, long fileOffset, Integer myIndex, SplitRequestCompletionHandler splitRequestCompletionHandler) {
            this.byteBuffer = byteBuffer;
            this.fileOffset = fileOffset;
            this.splitRequestCompletionHandler = splitRequestCompletionHandler;
            this.myIndex = myIndex;
        }

        @Override
        public String toString() {
            return "{offset=" + fileOffset + " ,size=" + byteBuffer.capacity() + " , myindex=" + myIndex + "}";
        }

        public void complete(JPfmError error, int actualRead, Completer completer) throws IllegalArgumentException, IllegalStateException {
            if (!completed.compareAndSet(false, true)) {
                throw new AlreadyCompleteException();
            }
            completionTime = System.currentTimeMillis();
            if (error == JPfmError.SUCCESS) splitRequestCompletionHandler.completed(actualRead, myIndex); else splitRequestCompletionHandler.failed(new Throwable(error.toString()), myIndex);
        }

        public ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        public long getFileOffset() {
            return fileOffset;
        }

        public boolean isCompleted() {
            return completed.get();
        }

        public void complete(JPfmError error) throws IllegalArgumentException, IllegalStateException {
            complete(error, getByteBuffer().capacity(), null);
        }

        public void handleUnexpectedCompletion(Exception exception) {
            complete(JPfmError.FAILED, myIndex, splitRequestCompletionHandler);
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getCompletionTime() {
            if (!isCompleted()) throw new IllegalStateException("Not completed yet");
            return completionTime;
        }

        public JPfmError getError() {
            if (!isCompleted()) throw new IllegalStateException("not completed yet");
            return JPfmError.SUCCESS;
        }

        public void setCompleter(Completer completehandler) {
            if (isCompleted()) throw new AlreadyCompleteException("Complete method has already been called by " + completer + " . Read.complete(..) method can be invoked only once.");
            if (this.completer != null) {
                throw new IllegalStateException("Already being handled by " + this.completer);
            }
            if (completehandler == null) {
                throw new IllegalArgumentException("Setting handler to null is not allowed.");
            }
            this.completer = completehandler;
        }

        public boolean canComplete(Completer completehandler) {
            if (this.completer == null) return true;
            return (this.completer == completehandler);
        }

        public Completer getCompleter() {
            return completer;
        }
    }

    private static final class RootIterator implements Iterator<FileAttributesProvider> {

        private File file;

        private boolean served = false;

        public RootIterator(File file) {
            this.file = file;
        }

        public boolean hasNext() {
            return !served;
        }

        public File next() {
            if (served) {
                return null;
            }
            served = true;
            return file;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public static boolean canManage(String name, java.nio.ByteBuffer volumeRawData, FileChannel volumeFileChannel) {
        if (name.endsWith(".001")) {
            return true;
        }
        return false;
    }

    private static String make3DigitString(int n) {
        int noDig = (int) Math.log10(n) + 1;
        String ret = "";
        int j = 0;
        for (; j < 3 - noDig; j++) {
            ret = ret + '0';
        }
        ret += n;
        return ret.toString();
    }

    private static class Unlocker implements MountListener {

        CountDownLatch latch;

        public Unlocker(CountDownLatch latch) {
            this.latch = latch;
        }

        public void eventOccurred(FormatterEvent event) {
            if (event.getEventType() == FormatterEvent.EVENT.DETACHED) {
                latch.countDown();
            }
        }
    }
}
