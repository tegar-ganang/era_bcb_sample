package tcl.lang;

import java.util.*;
import java.io.*;

/**
 * Subclass of the abstract class Channel.  It implements all of the 
 * methods to perform read, write, open, close, etc on a file.
 */
class FileChannel extends Channel {

    /**
     * The file needs to have a file pointer that can be moved randomly
     * within the file.  The RandomAccessFile is the only java.io class
     * that allows this behavior.
     */
    private RandomAccessFile file = null;

    /**
     * Open a file with the read/write permissions determined by modeFlags.
     * This method must be called before any other methods will function
     * properly.
     *
     * @param interp currrent interpreter.
     * @param fileName the absolute path or name of file in the current 
     *                 directory to open
     * @param modeFlags modes used to open a file for reading, writing, etc
     * @return the channelId of the file.
     * @exception TclException is thrown when the modeFlags try to open
     *                a file it does not have permission for or if the
     *                file dosent exist and CREAT wasnt specified.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */
    String open(Interp interp, String fileName, int modeFlags) throws IOException, TclException {
        mode = modeFlags;
        File fileObj = FileUtil.getNewFileObj(interp, fileName);
        if (((modeFlags & TclIO.CREAT) != 0) && ((modeFlags & TclIO.EXCL) != 0) && fileObj.exists()) {
            throw new TclException(interp, "couldn't open \"" + fileName + "\": file exists");
        }
        if (((modeFlags & TclIO.CREAT) != 0) && !fileObj.exists()) {
            file = new RandomAccessFile(fileObj, "rw");
            file.close();
        }
        if ((modeFlags & TclIO.RDWR) != 0) {
            checkFileExists(interp, fileObj);
            checkReadWritePerm(interp, fileObj, 0);
            if (fileObj.isDirectory()) {
                throw new TclException(interp, "couldn't open \"" + fileName + "\": illegal operation on a directory");
            }
            file = new RandomAccessFile(fileObj, "rw");
        } else if ((modeFlags & TclIO.RDONLY) != 0) {
            checkFileExists(interp, fileObj);
            checkReadWritePerm(interp, fileObj, -1);
            if (fileObj.isDirectory()) {
                throw new TclException(interp, "couldn't open \"" + fileName + "\": illegal operation on a directory");
            }
            file = new RandomAccessFile(fileObj, "r");
        } else if ((modeFlags & TclIO.WRONLY) != 0) {
            checkFileExists(interp, fileObj);
            checkReadWritePerm(interp, fileObj, 1);
            if (fileObj.isDirectory()) {
                throw new TclException(interp, "couldn't open \"" + fileName + "\": illegal operation on a directory");
            }
            if (!fileObj.canRead()) {
                throw new TclException(interp, "Java IO limitation: Cannot open a file " + "that has only write permissions set.");
            }
            file = new RandomAccessFile(fileObj, "rw");
        } else {
            throw new TclRuntimeError("FileChannel.java: invalid mode value");
        }
        if ((modeFlags & TclIO.APPEND) != 0) {
            file.seek(file.length());
        }
        if ((modeFlags & TclIO.TRUNC) != 0) {
            java.nio.channels.FileChannel chan = file.getChannel();
            chan.truncate(0);
        }
        String fName = TclIO.getNextDescriptor(interp, "file");
        setChanName(fName);
        return fName;
    }

    /**
     * Close the file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     */
    void close() throws IOException {
        if (file == null) {
            throw new TclRuntimeError("FileChannel.close(): null file object");
        }
        try {
            super.close();
        } finally {
            file.close();
            file = null;
        }
    }

    /**
     * Move the file pointer internal to the RandomAccessFile object. 
     * The file MUST be open or a TclRuntimeError is thrown.
     * 
     * @param offset The number of bytes to move the file pointer.
     * @param inmode to begin incrementing the file pointer; beginning,
     * current, or end of the file.
     */
    void seek(Interp interp, long offset, int inmode) throws IOException, TclException {
        if (file == null) {
            throw new TclRuntimeError("FileChannel.seek(): null file object");
        }
        int inputBuffered = getNumBufferedInputBytes();
        int outputBuffered = getNumBufferedOutputBytes();
        if ((inputBuffered != 0) && (outputBuffered != 0)) {
            throw new TclPosixException(interp, TclPosixException.EFAULT, true, "error during seek on \"" + getChanName() + "\"");
        }
        if (inmode == TclIO.SEEK_CUR) {
            offset -= inputBuffered;
        }
        if (input != null) {
            input.seekReset();
        }
        boolean wasAsync = false;
        if (false && !getBlocking()) {
            wasAsync = true;
            setBlocking(true);
            if (isBgFlushScheduled()) {
            }
        }
        if (output != null) {
            output.seekCheckBuferReady();
        }
        if (output != null && output.flushChannel(null, false) != 0) {
            throw new IOException("flush error while seeking");
        } else {
            long actual_offset;
            switch(inmode) {
                case TclIO.SEEK_SET:
                    {
                        actual_offset = offset;
                        break;
                    }
                case TclIO.SEEK_CUR:
                    {
                        actual_offset = file.getFilePointer() + offset;
                        break;
                    }
                case TclIO.SEEK_END:
                    {
                        actual_offset = file.length() + offset;
                        break;
                    }
                default:
                    {
                        throw new TclRuntimeError("invalid seek mode");
                    }
            }
            if (actual_offset < 0) {
                throw new TclPosixException(interp, TclPosixException.EINVAL, true, "error during seek on \"" + getChanName() + "\"");
            }
            file.seek(actual_offset);
        }
        if (wasAsync) {
            setBlocking(false);
        }
    }

    /**
     * Tcl_Tell -> tell
     *
     * Return the current offset of the file pointer in number of bytes from 
     * the beginning of the file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     *
     * @return The current value of the file pointer.
     */
    long tell() throws IOException {
        if (file == null) {
            throw new TclRuntimeError("FileChannel.tell(): null file object");
        }
        int inputBuffered = getNumBufferedInputBytes();
        int outputBuffered = getNumBufferedOutputBytes();
        if ((inputBuffered != 0) && (outputBuffered != 0)) {
            return -1;
        }
        long curPos = file.getFilePointer();
        if (curPos == -1) {
            return -1;
        }
        if (inputBuffered != 0) {
            return curPos - inputBuffered;
        }
        return curPos + outputBuffered;
    }

    /**
     * If the file dosent exist then a TclExcpetion is thrown. 
     *
     * @param interp currrent interpreter.
     * @param fileObj a java.io.File object of the file for this channel.
     */
    private void checkFileExists(Interp interp, File fileObj) throws TclException {
        if (!fileObj.exists()) {
            throw new TclPosixException(interp, TclPosixException.ENOENT, true, "couldn't open \"" + fileObj.getName() + "\"");
        }
    }

    /**
     * Checks the read/write permissions on the File object.  If inmode is less
     * than 0 it checks for read permissions, if mode greater than 0 it checks
     * for write permissions, and if it equals 0 then it checks both.
     *
     * @param interp currrent interpreter.
     * @param fileObj a java.io.File object of the file for this channel.
     * @param inmode what permissions to check for.
     */
    private void checkReadWritePerm(Interp interp, File fileObj, int inmode) throws TclException {
        boolean error = false;
        if (inmode <= 0) {
            if (!fileObj.canRead()) {
                error = true;
            }
        }
        if (inmode >= 0) {
            if (!fileObj.canWrite()) {
                error = true;
            }
        }
        if (error) {
            throw new TclPosixException(interp, TclPosixException.EACCES, true, "couldn't open \"" + fileObj.getName() + "\"");
        }
    }

    String getChanType() {
        return "file";
    }

    protected InputStream getInputStream() throws IOException {
        return new FileInputStream(file.getFD());
    }

    protected OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(file.getFD());
    }
}
