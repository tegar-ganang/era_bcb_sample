package com.mmbreakfast.unlod.lod;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.gamenet.application.mm8leveleditor.lod.FileBasedModifiedLodResource;
import org.gamenet.application.mm8leveleditor.lod.LodEntry;
import org.gamenet.application.mm8leveleditor.lod.LodResource;
import org.gamenet.util.ByteConversions;
import org.gamenet.util.TaskObserver;
import com.mmbreakfast.unlod.app.LodFileList.LodEntryComparator;
import com.mmbreakfast.util.RandomAccessFileOutputStream;

public abstract class LodFile {

    protected static final Class[] ENTRY_CONSTRUCTOR = new Class[] { LodFile.class, Long.TYPE };

    protected File file;

    protected RandomAccessFileInputStream in;

    protected byte fileHeader[] = null;

    protected HashMap entries = new HashMap();

    protected List orderedEntries = new ArrayList();

    protected static final long FILE_HEADER_SIZE_OFFSET = 272L;

    protected static final long FILE_SIZE_MINUS_FILE_HEADER_SIZE_OFFSET = 276L;

    protected LodFile(File file, RandomAccessFileInputStream inputStream) throws IOException, InvalidLodFileException {
        this.file = file;
        this.in = inputStream;
        RandomAccessFile raf = in.getFile();
        verify(raf);
        fileHeader = readFileHeader(raf);
        long entryCount = readEntryCount(raf);
        readEntries(raf, entryCount);
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return file.getName();
    }

    protected void verifySignature(RandomAccessFile raf) throws IOException, InvalidLodFileException {
    }

    protected void verify(RandomAccessFile raf) throws IOException, InvalidLodFileException {
        verifySignature(raf);
    }

    protected byte[] readFileHeader(RandomAccessFile raf) throws IOException {
        int headerEndLocation = (int) getHeaderOffset();
        byte[] fileHeader = new byte[headerEndLocation];
        raf.seek(0L);
        raf.read(fileHeader, 0, headerEndLocation);
        return fileHeader;
    }

    protected long readEntryCount(RandomAccessFile raf) throws IOException, InvalidLodFileException {
        raf.seek(getEntriesNumberOffset());
        long entryCount = 0L;
        int aByte = raf.read();
        if (aByte != -1) entryCount = (long) aByte;
        for (int byteIndex = 1; byteIndex < 4 && aByte != -1; byteIndex++) {
            aByte = raf.read();
            entryCount += (long) aByte << 8 * byteIndex;
        }
        if (entryCount == 0L) throw new InvalidLodFileException("Number of entries is NUL");
        return entryCount;
    }

    protected void readEntries(RandomAccessFile raf, long entryCount) throws IOException {
        raf.seek(getHeaderOffset());
        LodEntry currentEntry;
        for (int entryIndex = 0; ((long) entryIndex < entryCount && (currentEntry = getNextEntry(entryIndex, raf)) != null); entryIndex++) {
            String entryNameToStore = currentEntry.getFileName().toLowerCase();
            if (null != entries.get(entryNameToStore)) {
                if (entryNameToStore.equals("header.bin")) {
                    System.out.println("Unclear what to do with duplicate header.bin in new.lod files.");
                    entryNameToStore = "header.bin.duplicate";
                } else {
                    throw new RuntimeException("Duplicate entry " + entryIndex + " named '" + currentEntry.getFileName() + "'");
                }
            }
            entries.put(entryNameToStore, currentEntry);
            orderedEntries.add(currentEntry);
        }
    }

    private LodResource findFileBasedModifiedLodResource(List modifiedFileList, LodEntry lodEntry) {
        Iterator fileIterator = modifiedFileList.iterator();
        while (fileIterator.hasNext()) {
            File file = (File) fileIterator.next();
            String fileName = file.getName();
            if (lodEntry.getFormatConverter().isFileNameComponentForLodEntryFileName(file.getName(), lodEntry.getFileName())) return new FileBasedModifiedLodResource(lodEntry.getFormatConverter(), file);
        }
        return null;
    }

    public LodEntry findLodEntryForFile(File file) {
        List lodEntries = new ArrayList(getLodEntries().values());
        Iterator lodEntryIterator = lodEntries.iterator();
        while (lodEntryIterator.hasNext()) {
            LodEntry lodEntry = (LodEntry) lodEntryIterator.next();
            if (lodEntry.getFormatConverter().isFileNameComponentForLodEntryFileName(file.getName(), lodEntry.getFileName())) return lodEntry;
        }
        return null;
    }

    public byte[] getFileHeader() {
        return fileHeader;
    }

    public Map getLodEntries() {
        return entries;
    }

    public LodEntry findLodEntryByFileName(String string) throws NoSuchEntryException {
        if (false == entries.containsKey(string.toLowerCase())) throw new NoSuchEntryException(string);
        return (LodEntry) entries.get(string.toLowerCase());
    }

    protected LodEntry getNextEntry(int entriesSoFar, RandomAccessFile raf) throws IOException {
        long offset = this.getHeaderOffset() + this.getEntryHeaderLength() * (long) entriesSoFar;
        raf.seek(offset);
        try {
            return (LodEntry) this.getEntryClass().getConstructor(ENTRY_CONSTRUCTOR).newInstance(new Object[] { this, new Long(offset) });
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void write(TaskObserver taskObserver, RandomAccessFile randomAccessFile, List modifiedFileList, LodResource resourceToImport) throws IOException, InterruptedException {
        RandomAccessFileOutputStream randomAccessFileOutputStream = new RandomAccessFileOutputStream(randomAccessFile);
        LodEntryComparator dataOffsetLodEntryComparator = new LodEntryComparator() {

            public int compare(Object o1, Object o2) {
                LodEntry le1 = (LodEntry) o1;
                LodEntry le2 = (LodEntry) o2;
                return le1.getDataOffset() < le2.getDataOffset() ? -1 : 1;
            }

            public String getDisplayName() {
                return "data offset";
            }
        };
        List lodEntriesSortedByDataOffset = new ArrayList(getLodEntries().values());
        Collections.sort(lodEntriesSortedByDataOffset, dataOffsetLodEntryComparator);
        int oldEntriesCount = lodEntriesSortedByDataOffset.size();
        int newEntriesCount = 0;
        int totalEntriesCount = oldEntriesCount + newEntriesCount;
        long computedDataOffset = this.getFileHeader().length + totalEntriesCount * this.getEntryHeaderLength();
        float counter = 0;
        Iterator lodEntryByDataOffsetIterator = lodEntriesSortedByDataOffset.iterator();
        while (lodEntryByDataOffsetIterator.hasNext()) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("write thread was interrupted.");
            long newComputedDataOffset = 0;
            LodEntry lodEntry = (LodEntry) lodEntryByDataOffsetIterator.next();
            randomAccessFileOutputStream.getFile().seek(computedDataOffset);
            LodResource lodResource = null;
            if (null != modifiedFileList) {
                lodResource = findFileBasedModifiedLodResource(modifiedFileList, lodEntry);
            }
            if (null != resourceToImport) {
                if (resourceToImport.getName().equals(lodEntry.getEntryName())) {
                    lodResource = resourceToImport;
                }
            }
            if (null == lodResource) {
                taskObserver.taskProgress("Reusing data " + lodEntry.getFileName(), counter++ / totalEntriesCount);
                System.out.println("Reusing data " + lodEntry.getFileName());
                newComputedDataOffset = lodEntry.rewriteData(randomAccessFileOutputStream, computedDataOffset);
            } else {
                taskObserver.taskProgress("Replacing data " + lodEntry.getFileName(), counter++ / totalEntriesCount);
                System.out.println("Replacing data " + lodEntry.getFileName());
                newComputedDataOffset = lodEntry.updateData(lodResource, randomAccessFileOutputStream, computedDataOffset);
            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("write thread was interrupted.");
            long entryOffset = lodEntry.getEntryOffset();
            randomAccessFileOutputStream.getFile().seek(entryOffset);
            if (null == lodResource) {
                System.out.println("Reusing entry " + lodEntry.getFileName());
                long newDataOffset = lodEntry.rewriteEntry(randomAccessFileOutputStream, this.getFileHeader().length, entryOffset, computedDataOffset);
                if (newDataOffset != newComputedDataOffset) {
                    throw new RuntimeException("newDataOffset:" + newDataOffset + " != newComputedDataOffset:" + newComputedDataOffset);
                }
            } else {
                System.out.println("Replacing entry " + lodEntry.getFileName());
                long newDataOffset = lodEntry.updateEntry(lodResource, randomAccessFileOutputStream, this.getFileHeader().length, entryOffset, computedDataOffset);
                if (newDataOffset != newComputedDataOffset) {
                    throw new RuntimeException("newDataOffset:" + newDataOffset + " != newComputedDataOffset:" + newComputedDataOffset);
                }
            }
            computedDataOffset = newComputedDataOffset;
        }
        byte newFileHeader[] = new byte[this.fileHeader.length];
        System.arraycopy(this.fileHeader, 0, newFileHeader, 0, this.fileHeader.length);
        ByteConversions.setIntegerInByteArrayAtPosition(totalEntriesCount, newFileHeader, (int) this.getEntriesNumberOffset());
        if (-1 != getFileHeaderSizeOffset()) ByteConversions.setIntegerInByteArrayAtPosition(this.getFileHeader().length, newFileHeader, (int) getFileHeaderSizeOffset());
        if (-1 != getFileSizeMinusFileHeaderSizeOffset()) ByteConversions.setIntegerInByteArrayAtPosition(computedDataOffset - this.getFileHeader().length, newFileHeader, (int) getFileSizeMinusFileHeaderSizeOffset());
        randomAccessFile.seek(0L);
        randomAccessFile.write(newFileHeader);
    }

    public void updateByAppendingData(TaskObserver taskObserver, RandomAccessFile randomAccessFile, LodResource resourceToImport) throws IOException, InterruptedException {
        taskObserver.taskProgress(resourceToImport.getName(), 0);
        LodEntry lodEntryToUpdate = null;
        float counter = 0;
        Iterator lodEntryByDataOffsetIterator = getLodEntries().values().iterator();
        while (lodEntryByDataOffsetIterator.hasNext()) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("append thread was interrupted.");
            LodEntry lodEntry = (LodEntry) lodEntryByDataOffsetIterator.next();
            if (resourceToImport.getName().equals(lodEntry.getEntryName())) {
                lodEntryToUpdate = lodEntry;
                break;
            } else if (resourceToImport.getName().equals(lodEntry.getFileName())) {
                lodEntryToUpdate = lodEntry;
                break;
            }
        }
        taskObserver.taskProgress(resourceToImport.getName(), 0.10f);
        if (null == lodEntryToUpdate) {
            throw new RuntimeException("Unable to find lod entry for '" + resourceToImport.getName() + "'.");
        }
        long oldFileSize = lodEntryToUpdate.getLodFile().getFile().length();
        RandomAccessFileOutputStream randomAccessFileOutputStream = new RandomAccessFileOutputStream(randomAccessFile);
        randomAccessFileOutputStream.getFile().seek(oldFileSize);
        long newFileSize = lodEntryToUpdate.updateData(resourceToImport, randomAccessFileOutputStream, oldFileSize);
        taskObserver.taskProgress(resourceToImport.getName(), 0.90f);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("append thread was interrupted.");
        long entryOffset = lodEntryToUpdate.getEntryOffset();
        randomAccessFileOutputStream.getFile().seek(entryOffset);
        long newFileSize2 = lodEntryToUpdate.updateEntry(resourceToImport, randomAccessFileOutputStream, this.getFileHeader().length, entryOffset, oldFileSize);
        if (newFileSize != newFileSize2) throw new RuntimeException("Data size was " + newFileSize + ", but entry computed size as " + newFileSize2);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("append thread was interrupted.");
        taskObserver.taskProgress(resourceToImport.getName(), 0.99f);
        long fileSizeMinusFileHeaderSize = newFileSize - this.getFileHeader().length;
        if (-1 != getFileSizeMinusFileHeaderSizeOffset()) {
            byte newFileSizeMinusFileHeaderSize[] = new byte[4];
            ByteConversions.setIntegerInByteArrayAtPosition(fileSizeMinusFileHeaderSize, newFileSizeMinusFileHeaderSize, 0);
            randomAccessFile.seek(getFileSizeMinusFileHeaderSizeOffset());
            randomAccessFile.write(newFileSizeMinusFileHeaderSize);
        }
    }

    public RandomAccessFileInputStream getRandomAccessFileInputStream() {
        return in;
    }

    protected long getFileHeaderSizeOffset() {
        return FILE_HEADER_SIZE_OFFSET;
    }

    protected long getFileSizeMinusFileHeaderSizeOffset() {
        return FILE_SIZE_MINUS_FILE_HEADER_SIZE_OFFSET;
    }

    public abstract long getHeaderOffset();

    public abstract long getEntriesNumberOffset();

    public abstract long getEntryHeaderLength();

    public abstract Class getEntryClass();
}
