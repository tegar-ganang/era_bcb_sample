package org.jcvi.vics.shared.fasta;

import org.apache.log4j.Logger;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 1:42:08 PM
 *
 * @version $Id: FastaFile.java 1 2011-02-16 21:07:19Z tprindle $
 */
public class FastaFile extends File {

    static transient Logger logger = Logger.getLogger(FastaFile.class.getName());

    public FastaFile(String filename) {
        super(filename);
    }

    public FastaFile(File file) {
        super(file.getAbsolutePath());
    }

    public FastaFile(File file, String name) {
        super(file, name);
    }

    private FastaFileSize size = new FastaFileSize();

    enum InputState {

        UNSET, TARGET_DIR, SOURCE_FILES, PREFIX, SIZE, ENTRIES
    }

    private enum ACTION {

        DEFAULT, SPLIT, COMPUTESIZE
    }

    public void setSize(long basePairs, long entries) {
        size.setBases(basePairs);
        size.setEntries(entries);
    }

    public FastaFileSize getSize() throws IOException {
        if (size.isUnknown()) {
            readFASTASizeUsingChannel();
        }
        return size;
    }

    public Long split(File targetDirectory, String prefix, long maxUnitBases, long maxUnitEntries) throws Exception {
        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) throw new Exception("Could not create target directory " + targetDirectory.getAbsolutePath());
        }
        if (!size.isUnknown() && size.getBases() < maxUnitBases && (maxUnitEntries <= 0 || size.getEntries() < maxUnitEntries)) {
            FileInputStream fis = new FileInputStream(this);
            FileChannel fci = fis.getChannel();
            FileOutputStream fos = new FileOutputStream(new File(targetDirectory, prefix + "_0" + ".fasta"));
            FileChannel fco = fos.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(64000);
            while (fci.read(buffer) > 0) {
                buffer.flip();
                fco.write(buffer);
                buffer.clear();
            }
            fci.close();
            fco.close();
            return (long) 1;
        } else {
            long currentBasesCount = 0;
            long currentEntriesCount = 0;
            int targetCount = 0;
            FileChannel fastaChannel = new FileInputStream(this).getChannel();
            int totalSeqCount = 0;
            long totalResiduesCount = 0;
            try {
                long prevTime = System.currentTimeMillis();
                long fastaFileSize = this.length();
                long fastaFileReadOffset = 0L;
                long partitionStartOffset = 0L;
                final int bufferSize = 1024 * 1024;
                ByteBuffer fastaBuffer = ByteBuffer.allocateDirect(bufferSize);
                int fastaReadState = FASTAFileTokenizer.UNKNOWN;
                for (; fastaFileReadOffset < fastaFileSize; ) {
                    long nBytes = fastaChannel.read(fastaBuffer);
                    if (nBytes <= 0) {
                        fastaBuffer.limit(0);
                        break;
                    } else {
                        fastaBuffer.flip();
                        fastaFileReadOffset += nBytes;
                    }
                    for (; ; ) {
                        if (!fastaBuffer.hasRemaining()) {
                            fastaBuffer.clear();
                            break;
                        }
                        int b = fastaBuffer.get();
                        if (b == '\r') {
                        } else if (b == '\n') {
                            if (fastaReadState == FASTAFileTokenizer.DEFLINE) {
                                fastaReadState = FASTAFileTokenizer.SEQUENCELINE;
                            }
                        } else if (b == '>') {
                            if (fastaReadState == FASTAFileTokenizer.UNKNOWN) {
                                fastaReadState = FASTAFileTokenizer.STARTDEFLINE;
                            } else if (fastaReadState == FASTAFileTokenizer.SEQUENCELINE) {
                                fastaReadState = FASTAFileTokenizer.STARTDEFLINE;
                            }
                            if (fastaReadState == FASTAFileTokenizer.STARTDEFLINE) {
                                if (currentBasesCount >= maxUnitBases || maxUnitEntries > 0 && currentEntriesCount >= maxUnitEntries) {
                                    fastaBuffer.position(fastaBuffer.position() - 1);
                                    long currentTime = System.currentTimeMillis();
                                    System.out.println(new java.util.Date() + " Partition " + targetCount + " containing " + currentEntriesCount + " sequences and " + currentBasesCount + " residues ends at " + (fastaFileReadOffset - fastaBuffer.remaining()) + " and was created in " + (currentTime - prevTime) + " ms");
                                    prevTime = currentTime;
                                    long partitionEndOffset = fastaFileReadOffset - fastaBuffer.remaining();
                                    FileChannel partitionChannel = new FileOutputStream(new File(targetDirectory, prefix + "_" + targetCount + ".fasta")).getChannel();
                                    nBytes = fastaChannel.transferTo(partitionStartOffset, partitionEndOffset - partitionStartOffset, partitionChannel);
                                    partitionChannel.force(true);
                                    partitionChannel.close();
                                    targetCount++;
                                    partitionStartOffset += nBytes;
                                    currentBasesCount = 0;
                                    currentEntriesCount = 0;
                                    fastaReadState = FASTAFileTokenizer.UNKNOWN;
                                } else {
                                    fastaReadState = FASTAFileTokenizer.DEFLINE;
                                    currentEntriesCount++;
                                }
                                totalSeqCount++;
                            }
                        } else {
                            if (fastaReadState == FASTAFileTokenizer.SEQUENCELINE) {
                                totalResiduesCount++;
                                currentBasesCount++;
                            }
                        }
                    }
                }
                if (partitionStartOffset < fastaFileSize) {
                    long currentTime = System.currentTimeMillis();
                    System.out.println(new java.util.Date() + " Partition " + targetCount + " containing " + currentEntriesCount + " sequences and " + currentBasesCount + " residues ends at " + (fastaFileSize) + " and was created in " + (currentTime - prevTime) + " ms");
                    FileChannel partitionChannel = new FileOutputStream(new File(targetDirectory, prefix + "_" + targetCount + ".fasta")).getChannel();
                    fastaChannel.transferTo(partitionStartOffset, fastaFileSize - partitionStartOffset, partitionChannel);
                    partitionChannel.force(true);
                    partitionChannel.close();
                    targetCount++;
                }
                if (size.isUnknown()) {
                    size.setBases(totalResiduesCount);
                    size.setEntries(totalSeqCount);
                }
            } finally {
                fastaChannel.close();
            }
            return (long) targetCount;
        }
    }

    public static synchronized void repartition(File[] sourceFiles, File targetDirectory, String prefix, long maxUnitBases, long maxUnitEntries) throws Exception {
        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) throw new Exception("Could not create directory " + targetDirectory.getAbsolutePath());
        }
        File tmpFile = new File(targetDirectory, "tmp.fasta");
        FileOutputStream fos = new FileOutputStream(tmpFile);
        FileChannel fco = fos.getChannel();
        for (File file : sourceFiles) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fci = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(64000);
            while (fci.read(buffer) > 0) {
                buffer.flip();
                fco.write(buffer);
                buffer.clear();
            }
            fci.close();
        }
        fco.close();
        FastaFile fastaFile = new FastaFile(tmpFile);
        fastaFile.split(targetDirectory, prefix, maxUnitBases, maxUnitEntries);
        tmpFile.delete();
    }

    public long getEntrySize(String entry) {
        long size = 0;
        String[] lines = entry.split("\n");
        for (String line : lines) {
            if (line.length() > 0 && (!line.substring(0, 1).equals(">"))) {
                size += line.trim().length();
            }
        }
        return size;
    }

    public void readFASTASizeUsingChannel() throws IOException {
        FileChannel fastaChannel = new FileInputStream(this).getChannel();
        int totalSeqCount = 0;
        long totalResiduesCount = 0;
        try {
            long fastaFileSize = this.length();
            long fastaFileReadOffset = 0L;
            final int bufferSize = 1024 * 1024;
            ByteBuffer fastaBuffer = ByteBuffer.allocateDirect(bufferSize);
            int fastaReadState = FASTAFileTokenizer.UNKNOWN;
            for (; fastaFileReadOffset < fastaFileSize; ) {
                long nBytes = fastaChannel.read(fastaBuffer);
                if (nBytes <= 0) {
                    fastaBuffer.limit(0);
                    break;
                } else {
                    fastaBuffer.flip();
                    fastaFileReadOffset += nBytes;
                }
                for (; ; ) {
                    if (!fastaBuffer.hasRemaining()) {
                        fastaBuffer.clear();
                        break;
                    }
                    int b = fastaBuffer.get();
                    if (b == '\r') {
                    } else if (b == '\n') {
                        if (fastaReadState == FASTAFileTokenizer.DEFLINE) {
                            fastaReadState = FASTAFileTokenizer.SEQUENCELINE;
                        }
                    } else if (b == '>') {
                        if (fastaReadState == FASTAFileTokenizer.UNKNOWN) {
                            fastaReadState = FASTAFileTokenizer.STARTDEFLINE;
                        } else if (fastaReadState == FASTAFileTokenizer.SEQUENCELINE) {
                            fastaReadState = FASTAFileTokenizer.STARTDEFLINE;
                        }
                        if (fastaReadState == FASTAFileTokenizer.STARTDEFLINE) {
                            fastaReadState = FASTAFileTokenizer.DEFLINE;
                            totalSeqCount++;
                        }
                    } else {
                        if (fastaReadState == FASTAFileTokenizer.SEQUENCELINE) {
                            totalResiduesCount++;
                        }
                    }
                }
            }
            size.setBases(totalResiduesCount);
            size.setEntries(totalSeqCount);
        } finally {
            fastaChannel.close();
        }
    }

    public static void main(String[] args) {
        InputState inputState = InputState.UNSET;
        File targetDir = null;
        ArrayList<File> sourceList = new ArrayList<File>();
        String prefix = null;
        long bases = 0;
        long entries = 0;
        ACTION action = ACTION.DEFAULT;
        for (String arg : args) {
            if (arg.equals("-t")) {
                inputState = InputState.TARGET_DIR;
            } else if (arg.equals("-s")) {
                inputState = InputState.SOURCE_FILES;
            } else if (arg.equals("-p")) {
                inputState = InputState.PREFIX;
            } else if (arg.equals("-n")) {
                inputState = InputState.SIZE;
            } else if (arg.equals("-e")) {
                inputState = InputState.ENTRIES;
            } else if (arg.equals("-split")) {
                action = ACTION.SPLIT;
            } else if (arg.equals("-computesize")) {
                action = ACTION.COMPUTESIZE;
            } else if (inputState == InputState.TARGET_DIR) {
                targetDir = new File(arg);
            } else if (inputState == InputState.SOURCE_FILES) {
                File argFile = new File(arg);
                if (argFile.isDirectory()) {
                    File[] argFiles = argFile.listFiles();
                    sourceList.addAll(Arrays.asList(argFiles));
                } else {
                    sourceList.add(new File(arg));
                }
            } else if (inputState == InputState.PREFIX) {
                prefix = arg;
            } else if (inputState == InputState.SIZE) {
                bases = Long.parseLong(arg);
            } else if (inputState == InputState.ENTRIES) {
                entries = Long.parseLong(arg);
            }
        }
        if (targetDir == null) {
            logger.info("target directory not defined.");
        }
        if (bases == 0) {
            logger.info("partition size not defined.");
        }
        if (prefix == null) {
            logger.info("prefix not defined.");
        }
        if (sourceList.size() == 0) {
            logger.error("source list must not be empty.");
        }
        try {
            if (action == ACTION.SPLIT) {
                if (targetDir == null || bases == 0 || prefix == null || sourceList.size() == 0) {
                    usage();
                }
                for (File f : sourceList) {
                    FastaFile ff = new FastaFile(f);
                    ff.split(targetDir, prefix, bases, entries);
                }
            } else if (action == ACTION.COMPUTESIZE) {
                if (sourceList.size() == 0) {
                    usage();
                }
                for (File f : sourceList) {
                    FastaFile ff = new FastaFile(f);
                    FastaFileSize s = ff.getSize();
                    System.out.println("Number of sequences/bases: " + s.getEntries() + "/" + s.getBases());
                }
            } else {
                if (targetDir == null || bases == 0 || prefix == null || sourceList.size() == 0) {
                    usage();
                }
                FastaFile.repartition(sourceList.toArray(new File[sourceList.size()]), targetDir, prefix, bases, entries);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void usage() {
        logger.fatal("usage: [-split|-computesize] -t <target dir> -s <source file/dir> ... -p <target prefix> -n <unit size> [-e <max entries>]");
        System.exit(1);
    }

    public static class FastaFileSize implements Serializable {

        private long bases;

        private long entries;

        public FastaFileSize() {
            bases = -1;
            entries = -1;
        }

        FastaFileSize(long bases, long entries) {
            this.bases = bases;
            this.entries = entries;
        }

        public long getBases() {
            return bases;
        }

        public void setBases(long bases) {
            this.bases = bases;
        }

        public long getEntries() {
            return entries;
        }

        public void setEntries(long entries) {
            this.entries = entries;
        }

        public boolean isUnknown() {
            return bases <= 0;
        }
    }
}
