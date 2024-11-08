package edu.unc.irss.pakman44.imls;

/**
 * This exception designates a condition when a file designated to be saved to already exists,
 * necessitating an overwrite to save to that file.
 * @author Patrick King (patrick_king@unc.edu)
 * @version 1.0
 */
public class SaveFileExistsException extends java.io.IOException {

    /**
	 * For some reason, Eclipse warns about the absence of this variable for the
	 * Serializable interface.
	 */
    private static final long serialVersionUID = -1L;

    /**
	 * Provide the exception with the name of the file that already exists.
	 * @param filename The name of the file that already exists.
	 */
    public SaveFileExistsException(String filename) {
        super("The file " + filename + " already exists.  Would you like to overwrite it?");
    }
}
