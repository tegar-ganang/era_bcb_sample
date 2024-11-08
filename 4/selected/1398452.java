package com.fujitsu.arcon.njs.streaming;

import com.fujitsu.arcon.common.Chunk;
import com.fujitsu.arcon.common.FileTransferEngine;
import com.fujitsu.arcon.common.Logger;
import com.fujitsu.arcon.common.Reader;
import com.fujitsu.arcon.njs.priest.TargetSystem;

/**
 * @author Sven van den Berghe, Fujitsu Laboratories of Europe
 * 
 * Copyright Fujitsu Laboratories of Europe 2003
 *  
 * Created Oct 20, 2003
 */
public class TSIFileReader implements Reader, Runnable {

    private Chunk chunk;

    private boolean something_to_do;

    private FileTransferEngine fte;

    private TSIFileReaderFactory creator;

    private Logger logger;

    /**
	 * 
	 */
    public TSIFileReader(FileTransferEngine fte, TSIFileReaderFactory creator, Logger logger) {
        this.fte = fte;
        something_to_do = true;
        this.creator = creator;
        this.logger = logger;
    }

    public void setChunk(Chunk c) {
        chunk = c;
    }

    public Chunk getChunk() {
        return chunk;
    }

    /**
	 * That's all. Finish off and tidy up.
	 * 
	 * Pre-condition: Reader is not reading (usually is in run() though)
	 */
    public void terminate(boolean with_error) {
        something_to_do = false;
    }

    public void run() {
        fte.finishedARead(this);
        try {
            while (something_to_do) {
                if (chunk.getFileName().equals("NO_OVERWRITE")) {
                    if (logger.writeComments()) logger.logComment("Writing no-overwrite marker to buffer.");
                    fte.finishedARead(this);
                }
                if (logger.writeComments()) logger.logComment("Reading a chunk from file <" + chunk.getFileName() + "> from <" + chunk.getStartByte() + ">");
                int read = TargetSystem.getTargetSystem().readFile(creator.getRootDir(chunk.getPortfolio()) + chunk.getFileName(), chunk.getBuffer(), chunk.getStartByte(), chunk.getChunkLength(), 0, creator.getIncarnatedUser());
                if (logger.writeComments()) logger.logComment("Finished reading a chunk from file <" + chunk.getFileName() + "> from <" + chunk.getStartByte() + "> now at <" + (chunk.getStartByte() + read) + "> <" + (char) chunk.getBuffer()[0] + ">");
                fte.finishedARead(this);
            }
        } catch (Exception e) {
            terminate(true);
            fte.notifyError(chunk, e);
            chunk.setFailed(true);
            fte.finishedARead(this);
        }
        creator.notifyTerminated(this);
        creator = null;
        fte = null;
        chunk = null;
    }
}
