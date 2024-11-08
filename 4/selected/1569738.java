package com.fujitsu.arcon.common;

import java.util.LinkedList;

/**
 * Controls the concurrent transfer of a batch of files (from, for
 * example a Portfolio). The transfer details are handled by the instances
 * of Readers and Writers created by the factories. A FileTransferEngine
 * initiates the dynamic creation of new Writers and Readers as necessary, 
 * while limited by the policies of therir respective factories. Part of 
 * the function of a FileTransferEngine is to buffer read bytes for the
 * writers, these may be large and there may be a large number of these,
 * so FileTransferEngines are created by a Factory that can, one day, keep
 * a central view of this resource.
 * <p>
 * The direction of transfer and the file stransferred depends on the instances
 * returned by the factories and is not controlled by a FileTransferEngine.
 * 
 * @author Sven van den Berghe, Fujitsu Laboratories of Europe
 * 
 * Copyright Fujitsu Laboratories of Europe 2003
 *  
 * Created Jul 28, 2003
 */
public class FileTransferEngine {

    public static interface Requestor {

        public void transferDone();

        public boolean done();
    }

    public static FileTransferEngine create(ReaderFactory rf, WriterFactory wf, ChunkManager cm, Requestor r, Logger logger, int buffer_size) {
        FileTransferEngine fte = new FileTransferEngine(rf, wf, cm, r, logger, buffer_size);
        rf.setFileTransferEngine(fte);
        wf.setFileTransferEngine(fte);
        return fte;
    }

    private boolean waiting_for_last_chunks;

    private ReaderFactory reader_factory;

    private WriterFactory writer_factory;

    private ChunkManager chunk_manager;

    private Requestor requestor;

    private Logger logger;

    private int buffer_size;

    /**
	 * 
	 */
    private FileTransferEngine(ReaderFactory rf, WriterFactory wf, ChunkManager cm, Requestor r, Logger logger, int buffer_size) {
        waiting_for_last_chunks = false;
        empty_buffers = new LinkedList();
        full_chunks = new LinkedList();
        reader_factory = rf;
        writer_factory = wf;
        chunk_manager = cm;
        requestor = r;
        this.buffer_size = buffer_size;
        this.logger = logger;
    }

    /**
	 * Try to add a Writer to the active set.
	 * 
	 * @param w
	 */
    public synchronized void addWriter() {
        if (reader_factory.canMakeWriter() && writer_factory.makeWriter()) {
            addBuffer();
        }
    }

    /**
	 * Try to add a Reader to the active set.
	 * 
	 * @param r
	 */
    public synchronized void addReader() {
        if (writer_factory.canMakeReader() && reader_factory.makeReader()) {
            addBuffer();
        }
    }

    /**
	 * Start the transfer up (requires that at least one Reader and one Writer
	 * will start up in this call, if not the wholeprocess is shut down).
	 *
	 */
    public synchronized void doTransfer() {
        if (writer_factory.makeWriter()) {
            if (!reader_factory.makeReader()) {
                logger.logError("First Reader failed to start, aborting file transfer");
                error_found = true;
                main_error_message = "First Reader failed to start, aborting file transfer";
                allWritersTerminated();
            }
        } else {
            logger.logError("First Writer failed to start, aborting file transfer");
            error_found = true;
            main_error_message = "First Writer failed to start, aborting file transfer";
            allWritersTerminated();
        }
        addBuffer();
        addBuffer();
    }

    private boolean error_found = false;

    private String main_error_message = "All OK";

    private Exception offender;

    /**
	 * Call after transfer is complete (see {@link FileTransferEngine.Requestor}).
	 * The offending error message can be found from {@link #errorMessage}.
	 * 
	 * @return True if transfer was OK, false otherwise
	 */
    public boolean transferOK() {
        return !error_found;
    }

    /**
	 * Message from the error that failed the transfer (if any).
	 * 
	 * @return
	 */
    public String errorMessage() {
        return main_error_message;
    }

    /**
	 * Return the Exception causing the main error.
	 * @return
	 */
    public Exception getCause() {
        return offender;
    }

    /**
	 *  A Reader, Writer or Factory encountered some unrecoverable error, 
	 *  shut down the transfer. There is no particular reason to be
	 *  drastic about it. Let current writes continue and try to clear
	 *  read buffers. Obviously, a Writer error may lead to multiple reports
	 *  so suppress some.
	 *
	 */
    public void notifyError(Chunk chunk, Exception e) {
        if (!error_found) {
            main_error_message = "Error while transferring chunk of <" + (chunk != null ? chunk.getFileName() : "") + "> starting at <" + (chunk != null ? chunk.getStartByte() : 0) + "> length <" + (chunk != null ? chunk.getChunkLength() : 0) + "> ";
            logger.logError(main_error_message, e);
            main_error_message += e.getMessage();
            offender = e;
            error_found = true;
        } else {
            logger.logError("Additional error for <" + (chunk != null ? chunk.getFileName() : "") + "> " + e.getMessage());
        }
    }

    /**
	 * A Reader has finished reading a chunk (buffer's worth) of the files.
	 * This buffer will be stored by the Engine (so that it can be passed
	 * onto a Writer). The ChunkManager will be called to find the next 
	 * section of a file for the Reader to get. The Reader is passed the
	 * new chunk and a buffer before return (unless there is nothing left to do,
	 * in this case the Reader is terminated).
	 * 
	 * @param r
	 */
    public void finishedARead(Reader r) {
        boolean terminate_r = false;
        synchronized (this) {
            storeFullChunk(r.getChunk());
            r.setChunk(null);
            if (waiting_for_last_chunks || error_found) {
                terminate_r = true;
            } else {
                if (noEmptyBuffersAvailable()) {
                    addWriter();
                }
                byte[] buffer = getEmptyBuffer();
                if (buffer == null) {
                    terminate_r = true;
                } else {
                    chunk_manager.getNextChunk(r);
                    if (r.getChunk() != null) {
                        r.getChunk().setBuffer(buffer);
                    } else {
                        storeEmptyBuffer(buffer);
                        terminate_r = true;
                    }
                    if (chunk_manager.noMoreChunks()) {
                        waiting_for_last_chunks = true;
                    }
                }
            }
        }
        if (terminate_r) r.terminate(error_found);
    }

    /**
	 * Called by the ReaderFactory when all Readers terminated. Change to 
	 * cleaning up Writers.
	 *
	 */
    public synchronized void allReadersTerminated() {
        notifyAll();
    }

    /**
	 * Usually implies that everything is done. Except where we are completing
	 * on error and all the Writers fail before the Readers are done - Readers will
	 * complete uselessly but harmelessly,
	 *
	 */
    public synchronized void allWritersTerminated() {
        notifyAll();
        writer_factory = null;
        reader_factory = null;
        chunk_manager = null;
        requestor.transferDone();
    }

    /**
	 * A Writer has written a chunk. Need to be allocated another one from the
	 * Engine's buffer.
	 * 
	 * @param w
	 */
    public synchronized void finishedAWrite(Writer w) {
        boolean terminate_w = false;
        if (w.getChunk() != null) {
            storeEmptyBuffer(w.getChunk().getBuffer());
            w.getChunk().setBuffer(null);
            w.setChunk(null);
        }
        if (noFullChunksAvailable()) {
            if (reader_factory.allTerminated()) {
                terminate_w = true;
            } else {
                if (!(waiting_for_last_chunks || error_found)) {
                    addReader();
                }
                w.setChunk(getFullChunk());
            }
        } else {
            w.setChunk(getFullChunk());
        }
        if (terminate_w) w.terminate(error_found);
    }

    public synchronized void disposeWrittenChunk(Chunk c) {
        if (c != null) {
            storeEmptyBuffer(c.getBuffer());
            c.setBuffer(null);
        }
    }

    private LinkedList empty_buffers = new LinkedList();

    private LinkedList full_chunks = new LinkedList();

    private int buffer_count = 0;

    private void addBuffer() {
        storeEmptyBuffer(new byte[buffer_size]);
    }

    private void storeFullChunk(Chunk chunk) {
        if (chunk != null) {
            full_chunks.addLast(chunk);
            notifyAll();
        } else {
        }
    }

    private void storeEmptyBuffer(byte[] buffer) {
        if (buffer != null) {
            empty_buffers.addLast(buffer);
            notifyAll();
        } else {
        }
    }

    private synchronized byte[] getEmptyBuffer() {
        while (noEmptyBuffersAvailable()) {
            if (writer_factory == null || writer_factory.allTerminated()) {
                return (byte[]) null;
            } else {
                try {
                    wait();
                } catch (Exception ex) {
                }
            }
        }
        return (byte[]) empty_buffers.removeFirst();
    }

    private synchronized Chunk getFullChunk() {
        while (noFullChunksAvailable()) {
            if (reader_factory == null || reader_factory.allTerminated()) {
                return (Chunk) null;
            }
            try {
                wait();
            } catch (Exception ex) {
            }
        }
        Chunk chunk = (Chunk) full_chunks.removeFirst();
        return chunk;
    }

    private boolean noFullChunksAvailable() {
        return full_chunks.size() == 0;
    }

    private boolean noEmptyBuffersAvailable() {
        return empty_buffers.size() == 0;
    }
}
