package org.jd3lib.archoslib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import org.jd3lib.compatible.Logger;
import org.jd3lib.compatible.Queue;

/**
 * @author Grunewald
 */
public class LibraryWriter implements Runnable {

    public static final String FILE_NAME = "lib.jbm";

    Files files;

    ListEntries listentries;

    Lists lists;

    Paths paths;

    private File root;

    Strings strings;

    private int percentage;

    public Header header;

    private int fileCount = 0;

    private int processed = 0;

    private Queue stringQ;

    private Queue foundFiles;

    private boolean backup;

    private boolean ready;

    private Thread thread;

    /**
   * @param file
   */
    public LibraryWriter(File workingDirectory) {
        init(workingDirectory);
    }

    /**
   *  
   */
    public LibraryWriter() {
        ready = false;
    }

    public void init(File workingDirectory) {
        ready = true;
        root = workingDirectory;
        initializeClassVariables();
    }

    /**
   * @param makeBackup
   * @param queue
   *          Iterates over the Found MP3s and inserts the MetaData into the
   *          Strings and
   *  
   */
    public void stepTwo(Queue inStrings, Queue inFound) {
        if (ready) {
            stringQ = inStrings;
            foundFiles = inFound;
            go();
        } else throw new RuntimeException("Library not initialized please call init() first");
    }

    /**
   *  
   */
    private void go() {
        thread.start();
    }

    /**
   * Add the strings from the queue to the normal strings
   * @param strings2
   */
    private void addStrings(Queue strings2) {
        while (!strings2.isEmpty()) {
            strings.put((String) strings2.remove());
        }
    }

    /**
   * @return
   */
    private int getWorkingDirLength() {
        try {
            String myPath = root.getCanonicalPath();
            return myPath.length();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void initializeClassVariables() {
        header = new Header();
        listentries = new ListEntries();
        strings = new Strings();
        paths = new Paths(getWorkingDirLength(), this.strings);
        files = new Files(this.strings, this.paths);
        lists = new Lists(this.strings);
        thread = new Thread(this);
    }

    public int getFileCount() {
        return fileCount;
    }

    public synchronized int getProcessed() {
        return processed;
    }

    private synchronized void setPercentage(int value) {
        percentage = value;
    }

    /**
   * @return
   */
    public synchronized int getPercentage() {
        return percentage;
    }

    /**
   *  
   */
    public void write() {
        if (backup) makeBackup();
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(root.getCanonicalPath() + "/" + FILE_NAME);
            FileChannel out = outStream.getChannel();
            out.position(512);
            header.setOffset_files((int) out.position());
            out.write(files.toByteBuffer());
            out.position(out.position() + (512 - out.position() % 512));
            header.setOffset_lists((int) out.position());
            out.write(lists.toByteBuffer());
            out.position(out.position() + (512 - out.position() % 512));
            header.setOffset_list_entries((int) out.position());
            header.setSearch_list(lists.getSearchListId());
            out.write(listentries.toByteBuffer());
            out.position(out.position() + (512 - out.position() % 512));
            header.setOffset_paths((int) out.position());
            out.write(paths.toByteBuffer());
            out.position(out.position() + (512 - out.position() % 512));
            header.setOffset_strings((int) out.position());
            out.write(strings.toByteBuffer());
            out.position(out.position() + (512 - out.position() % 512));
            header.setOffset_private_data((int) out.position());
            out.position(0);
            out.write(header.toByteBuffer());
            out.position(502);
            out.close();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   *  
   */
    private void makeBackup() {
        try {
            File original = new File(root.getCanonicalPath() + "/" + FILE_NAME);
            File backupFile = new File(root.getCanonicalPath() + "/" + FILE_NAME + ".bak");
            backupFile.delete();
            original.renameTo(new File(root.getCanonicalPath() + "/" + FILE_NAME + ".bak"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        processed = 0;
        addStrings(stringQ);
        fileCount = foundFiles.size();
        System.out.println("set filecount to:" + fileCount);
        ArcAudioFileDecorator data;
        Iterator audioFileIterator = foundFiles.iterator();
        System.out.println("got the Iterator");
        long last = System.currentTimeMillis();
        while (audioFileIterator.hasNext() && Thread.currentThread() == thread) {
            File nextFile = (File) audioFileIterator.next();
            Logger.global.finest("START: " + nextFile.getAbsolutePath());
            data = new ArcAudioFileDecorator(ArcLibUtils.getDecodedFile(nextFile));
            if (data.getMetaData() != null) {
                strings.take(data);
                paths.take(data);
                files.take(data);
                lists.take(data);
            }
            ++processed;
            Logger.global.finest("DONE: " + nextFile.getAbsolutePath());
            if (System.currentTimeMillis() - last > 20) {
                setPercentage(processed * 100 / fileCount);
                last = System.currentTimeMillis();
            }
        }
        if (thread != null) {
            lists.sort(true);
            listentries.take(lists, files);
            header.setNum_files(files.size());
            header.setNum_lists(lists.size());
            Logger.global.finest("Starting to write the lib.jbm file");
            write();
            Logger.global.finest("Done writing. Everything should be fine.");
            setPercentage(processed * 100 / fileCount);
        }
    }

    /**
   *  
   */
    public void cancel() {
        System.out.println("CANCEL call");
        thread = null;
    }

    /**
   * @param backupFlag
   */
    public void setMakeBackup(boolean backupFlag) {
        backup = backupFlag;
    }
}
