package com.fujitsu.arcon.common;

import java.io.IOException;
import java.util.zip.ZipEntry;
import org.unicore.ajo.Portfolio;

/**
 * Reads Chunks from a UPL connection.
 * 
 * @author Sven van den Berghe, Fujitsu Laboratories of Europe
 * 
 * Copyright Fujitsu Laboratories of Europe 2003
 *  
 * Created Aug 1, 2003
 */
public class UPLReader implements Reader, Runnable {

    private static int id = 0;

    private Logger logger;

    private Connection connection;

    private ReaderFactory rf;

    private FileTransferEngine fte;

    private Chunk chunk;

    private boolean not_terminated;

    private int my_id;

    public UPLReader(FileTransferEngine fte, Connection connection, ReaderFactory rf, Logger logger) {
        this.fte = fte;
        this.connection = connection;
        this.rf = rf;
        this.logger = logger;
        not_terminated = true;
        my_id = id++;
    }

    /**
	 * Called by the ChunkManager to set next Chunk, but for UPL reading this
	 * is determined by the incoming data, so should never be called.
	 */
    public void setChunk(Chunk c) {
        chunk = c;
    }

    public Chunk getChunk() {
        return chunk;
    }

    /**
	 * Called to terminate current connection (can be revoked by
	 * setNewConnection). Clean up any open InputStreams here?
	 */
    public void terminate(boolean with_error) {
        if (with_error) {
            connection.closeError();
        } else {
            connection.closeOK();
        }
        not_terminated = false;
        logger.logComment("UPLRreader-" + my_id + " terminated" + (with_error ? " with error" : ""));
    }

    public void setNewConnection(Connection c) {
        this.connection = c;
        not_terminated = true;
        logger.logComment("UPLRreader-" + my_id + " new connection");
    }

    String current_file_name;

    long offset_in_current_file;

    byte mode;

    private Portfolio portfolio;

    private boolean data_on_stream;

    /**
	 * 
	 * @throws Exception Problems with the ZipEntry (file name, format etc)
	 */
    private void openNewInputFileChunk() throws Exception {
        ZipEntry entry;
        try {
            entry = connection.getDataInputStream().getNextEntry();
        } catch (IOException e) {
            throw new Exception("Problems reading new ZIPEntry", e);
        }
        if (entry != null) {
            if (entry.isDirectory()) {
                if (entry.getExtra() != null) {
                    current_file_name = entry.getName();
                    offset_in_current_file = 0;
                    portfolio = new Portfolio("Marker");
                    logger.logComment("UPLRreader-" + my_id + " No overwrite marker for <" + current_file_name + ">");
                } else {
                    openNewInputFileChunk();
                }
            } else if (entry.getName().equals("ERROR")) {
                throw new Exception("There was an error sent by the source of the files.");
            } else {
                current_file_name = entry.getName();
                offset_in_current_file = 0;
                portfolio = null;
                if (current_file_name.startsWith("CHUNK_")) {
                    int i = 6;
                    int j = current_file_name.indexOf("_", i);
                    try {
                        offset_in_current_file = Long.parseLong(current_file_name.substring(i, j));
                    } catch (NumberFormatException e) {
                        Exception ex = new Exception("Problems parsing input file chunk name <" + current_file_name + ">");
                        ex.initCause(e);
                        throw ex;
                    }
                    current_file_name = current_file_name.substring(j + 1);
                }
                if (entry.getExtra() != null) {
                    mode = entry.getExtra()[0];
                } else {
                    mode = 6;
                }
                logger.logComment("UPLReader-" + my_id + " Extracting file <" + current_file_name + ">, size <" + entry.getSize() + ">, mode <" + mode + ">, offset <" + offset_in_current_file + ">");
                data_on_stream = true;
            }
        } else {
            data_on_stream = false;
        }
    }

    private void newChunk() {
        chunk.setFileName(current_file_name);
        chunk.setStartByte(offset_in_current_file);
        chunk.setChunkLength(0);
        chunk.setMode(mode);
        chunk.setPortfolio(portfolio);
    }

    public void run() {
        try {
            fte.finishedARead(this);
            while (not_terminated) {
                try {
                    openNewInputFileChunk();
                    newChunk();
                    while (data_on_stream) {
                        int read = 0;
                        int so_far = (int) chunk.getChunkLength();
                        do {
                            so_far += read;
                            read = connection.getDataInputStream().read(chunk.getBuffer(), so_far, chunk.getBuffer().length - so_far);
                        } while (read > 0);
                        chunk.setChunkLength(so_far);
                        if (read == 0) {
                            offset_in_current_file += chunk.getChunkLength();
                            logger.logComment("UPLReader-" + my_id + " Placing a complete chunk of <" + current_file_name + "> from <" + chunk.getStartByte() + "> length <" + chunk.getChunkLength() + "> <" + (char) chunk.getBuffer()[0] + ">");
                            fte.finishedARead(this);
                            if (!not_terminated) return;
                            newChunk();
                        } else {
                            String old_file_name = current_file_name;
                            long old_offset = offset_in_current_file;
                            openNewInputFileChunk();
                            if (data_on_stream) {
                                if (current_file_name.equals(old_file_name) && offset_in_current_file == old_offset + chunk.getChunkLength()) {
                                    offset_in_current_file = old_offset;
                                    logger.logComment("UPLReader-" + my_id + " new contiguous ZIP entry <" + current_file_name + "> from <" + chunk.getStartByte() + ">");
                                } else {
                                    logger.logComment("UPLReader-" + my_id + " new non-contiguous ZIP entry, dumping <" + old_file_name + "> from <" + chunk.getStartByte() + "> length <" + chunk.getChunkLength() + ">");
                                    if (chunk.getChunkLength() != 0) fte.finishedARead(this);
                                    if (!not_terminated) return;
                                    newChunk();
                                }
                            }
                        }
                    }
                    logger.logComment("UPLReader-" + my_id + " Placing a complete END chunk of <" + current_file_name + "> from <" + chunk.getStartByte() + "> length <" + chunk.getChunkLength() + ">");
                    fte.finishedARead(this);
                    if (not_terminated) terminate(false);
                } catch (Exception e) {
                    terminate(true);
                    fte.notifyError(chunk, e);
                    chunk.setFailed(true);
                    fte.finishedARead(this);
                } finally {
                    rf.notifyTerminated(this);
                }
            }
        } finally {
            rf = null;
            fte = null;
            connection = null;
            chunk = null;
        }
    }
}
