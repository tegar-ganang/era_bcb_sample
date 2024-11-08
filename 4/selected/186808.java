package com.fujitsu.arcon.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Sven van den Berghe, Fujitsu Laboratories of Europe
 * 
 * Copyright Fujitsu Laboratories of Europe 2003
 *  
 * Created Jul 30, 2003
 */
public class FileWriterFactory implements WriterFactory {

    private FileTransferEngine fte;

    private int limit;

    private String base_dir;

    private Set open_files;

    private Logger logger;

    private Set all_writers = new HashSet();

    public FileWriterFactory(int limit, String base_dir, Logger logger) {
        this.limit = 1;
        open_files = new HashSet();
        this.base_dir = base_dir;
        this.logger = logger;
    }

    private static int counter = 0;

    public boolean makeWriter() {
        if (all_writers.size() < limit) {
            FileWriter fw = new FileWriter(fte, this, open_files, base_dir, logger);
            all_writers.add(fw);
            (new Thread(fw, "ConcurrentFTWriter-" + counter++)).start();
            logger.logComment("Creating new writer" + counter);
            return true;
        } else {
            return false;
        }
    }

    public void notifyTerminated(Writer r) {
        all_writers.remove(r);
        if (all_writers.isEmpty()) {
            all_terminated = true;
            Iterator iterator = open_files.iterator();
            while (iterator.hasNext()) {
                FileWriter.FileChunk fc = (FileWriter.FileChunk) iterator.next();
                do {
                    try {
                        fc.stream.flush();
                        fc.stream.close();
                    } catch (IOException e) {
                    }
                    fc = fc.next;
                } while (fc != null);
            }
            iterator = open_files.iterator();
            boolean all_ok = true;
            while (iterator.hasNext()) {
                FileWriter.FileChunk fc = (FileWriter.FileChunk) iterator.next();
                logger.logComment("File chunk <" + fc.name + "> " + fc.start_byte + " " + fc.position + " " + fc.actual_file);
                boolean ok = true;
                while (fc.next != null) {
                    ok = ok && (fc.start_byte + fc.actual_file.length()) == fc.next.start_byte;
                    fc = fc.next;
                }
                if (ok) {
                    logger.logComment("Received file <" + fc.name + "> is contiguous (and hopefully complete)");
                } else {
                    logger.logError("Received file <" + fc.name + "> is NOT contiguous");
                    all_ok = false;
                }
            }
            if (all_ok) {
                byte[] buffer = new byte[16384];
                iterator = open_files.iterator();
                while (iterator.hasNext()) {
                    FileWriter.FileChunk fc = (FileWriter.FileChunk) iterator.next();
                    try {
                        if (fc.next != null) {
                            FileOutputStream fos = new FileOutputStream(fc.actual_file, true);
                            fc = fc.next;
                            while (fc != null) {
                                FileInputStream fis = new FileInputStream(fc.actual_file);
                                int actually_read = fis.read(buffer);
                                while (actually_read != -1) {
                                    fos.write(buffer, 0, actually_read);
                                    actually_read = fis.read(buffer);
                                }
                                fc.actual_file.delete();
                                fc = fc.next;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            fte.allWritersTerminated();
            fte = null;
        }
    }

    private boolean all_terminated = false;

    public boolean allTerminated() {
        return all_terminated;
    }

    public void setFileTransferEngine(FileTransferEngine fte) {
        this.fte = fte;
    }

    /**
	 * Return an iterator over the java.io.Files created by this file
	 * transfer. This will only be reliable if called after the transfer
	 * is complete and it completed successfully.
	 * 
	 * @return Iterator containing 0 or more java.io.Files
	 */
    public Iterator createdFiles() {
        final Iterator iterator = open_files.iterator();
        return new Iterator() {

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Object next() {
                return ((FileWriter.FileChunk) iterator.next()).actual_file;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean canMakeReader() {
        return true;
    }
}
