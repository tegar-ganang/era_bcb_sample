package org.fao.waicent.kids.editor.SplitFile;

/**
 * Event throwed when the splitting finishes due to an error
 *
 * @author (c) LuisM Pena, October-1997.
 * @version 1.0
 * SOFTWARE IS PROVIDED "AS IS", WITHOUT ANY WARRANTY OF ANY KIND
 */
public class ErrorSplittingEvent extends SplitFileEvent {

    /**
     * The File to split can not be read
     */
    public static final int NOT_READABLE_FILE = 1;

    /**
     * The File to split doen't exist
     */
    public static final int NOT_EXISTING_FILE = 2;

    /**
     * The File to split is a directory
     */
    public static final int FILE_IS_DIRECTORY = 3;

    /**
     * The File to split is not found
     */
    public static final int FILE_NOT_FOUND = 4;

    /**
     * The File to split is empty
     */
    public static final int FILE_EMPTY = 5;

    /**
     * IOException reading the File to split
     */
    public static final int IOEXCEPTION_READING = 6;

    /**
     * IOException writting on a chunk
     */
    public static final int IOEXCEPTION_WRITTING = 7;

    /**
     * File to split is smaller than expected
     */
    public static final int SPLITFILE_SMALLER = 8;

    /**
     * Splitting has been cancelled
     */
    public static final int SPLITTING_CANCELLED = 9;

    /**
     * Internal Error: there are more bytes to read than to write
     */
    public static final int INTERNAL_ERROR1 = 10;

    /**
     * Internal Error: there are less bytes to read than to write
     */
    public static final int INTERNAL_ERROR2 = 11;

    /**
     * Internal Error InterruptedException
     */
    public static final int INTERNAL_ERROR3 = 12;

    /**
     * This constructor needs the produced error
     * @param error the error produced
     */
    public ErrorSplittingEvent(int reason) {
        this.reason = reason;
        fileName = null;
    }

    /**
     * Gets the reason associated
     * @return the error associated
     */
    public int getError() {
        return reason;
    }

    /**
     * Gets the error associated as an string
     * @return the error associated as an string
     */
    public String toString() {
        return reasonToString(reason);
    }

    /**
     * Gets the string error associated as a reason error
     * @return the string error associated as a reason error
     */
    public static String reasonToString(int reason) {
        String ret = null;
        switch(reason) {
            case NOT_READABLE_FILE:
                ret = new String("Can not read the split file");
                break;
            case NOT_EXISTING_FILE:
                ret = new String("The split file doesn't exist");
                break;
            case FILE_IS_DIRECTORY:
                ret = new String("The split file is a directory");
                break;
            case FILE_NOT_FOUND:
                ret = new String("The split file has not been found");
                break;
            case FILE_EMPTY:
                ret = new String("The split file is empty");
                break;
            case IOEXCEPTION_READING:
                ret = new String("Error reading on file to split");
                break;
            case IOEXCEPTION_WRITTING:
                ret = new String("Error writting on chunk file");
                break;
            case SPLITFILE_SMALLER:
                ret = new String("File to split is smaller than expected");
                break;
            case SPLITTING_CANCELLED:
                ret = new String("Splitting has been cancelled");
                break;
            case INTERNAL_ERROR1:
                ret = new String("Internal error: there are more bytes to read than to write");
                break;
            case INTERNAL_ERROR2:
                ret = new String("Internal error: there are less bytes to read than to write");
                break;
            case INTERNAL_ERROR3:
                ret = new String("Internal error: thread interrupted");
                break;
            default:
                ret = new String("Unknown error");
                break;
        }
        return ret;
    }

    /**
     * Gets the name of the file being splitted. It only applies if this event is launched before
     * an StartSplittingEvent is launched
     * @return the name of the file being splitted
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set the name of the file being splitted.
     */
    public void setFileName(String fileName) {
        this.fileName = new String(fileName);
    }

    private int reason;

    private String fileName;
}
