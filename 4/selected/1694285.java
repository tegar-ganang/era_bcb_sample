package subget.exceptions;

import subget.bundles.Bundles;

/**
 *
 * @author povder
 */
public class TmpDirNotWriteableReadableException extends DirException {

    /**
     * Creates a new instance of <code>TmpDirNotWriteableReadableException</code> without detail message.
     */
    public TmpDirNotWriteableReadableException() {
    }

    /**
     * Constructs an instance of <code>TmpDirNotWriteableReadableException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public TmpDirNotWriteableReadableException(String msg) {
        super(String.format(Bundles.subgetBundle.getString("Cannot_read/write_to_temporary_directory_(%s),_aborting."), msg));
    }
}
