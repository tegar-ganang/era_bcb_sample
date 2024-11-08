package subget.exceptions;

import subget.bundles.Bundles;

/**
 *
 * @author povder
 */
public class OutputDirNotWriteableException extends DirException {

    /**
     * Creates a new instance of <code>OutputDirNotWriteableException</code> without detail message.
     */
    public OutputDirNotWriteableException() {
    }

    /**
     * Constructs an instance of <code>OutputDirNotWriteableException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public OutputDirNotWriteableException(String msg) {
        super(String.format(Bundles.subgetBundle.getString("Cannot_read/write_to_output_directory_(%s),_aborting."), msg));
    }
}
