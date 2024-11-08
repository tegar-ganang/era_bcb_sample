package tcl.lang;

import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "flush" command in Tcl.
 */
class FlushCmd implements Command {

    /**
     * This procedure is invoked to process the "flush" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */
    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
        Channel chan;
        if (argv.length != 2) {
            throw new TclNumArgsException(interp, 1, argv, "channelId");
        }
        chan = TclIO.getChannel(interp, argv[1].toString());
        if (chan == null) {
            throw new TclException(interp, "can not find channel named \"" + argv[1].toString() + "\"");
        }
        try {
            chan.flush(interp);
        } catch (IOException e) {
            e.printStackTrace();
            throw new TclRuntimeError("FlushCmd.cmdProc() Error: IOException when flushing " + chan.getChanName());
        }
    }
}
