package tcl.lang;

import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "seek" command in Tcl.
 */
class SeekCmd implements Command {

    private static final String validOrigins[] = { "start", "current", "end" };

    static final int OPT_START = 0;

    static final int OPT_CURRENT = 1;

    static final int OPT_END = 2;

    /**
     * This procedure is invoked to process the "seek" Tcl command.
     * See the user documentation for details on what it does.
     */
    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
        Channel chan;
        int mode;
        if (argv.length != 3 && argv.length != 4) {
            throw new TclNumArgsException(interp, 1, argv, "channelId offset ?origin?");
        }
        mode = TclIO.SEEK_SET;
        if (argv.length == 4) {
            int index = TclIndex.get(interp, argv[3], validOrigins, "origin", 0);
            switch(index) {
                case OPT_START:
                    {
                        mode = TclIO.SEEK_SET;
                        break;
                    }
                case OPT_CURRENT:
                    {
                        mode = TclIO.SEEK_CUR;
                        break;
                    }
                case OPT_END:
                    {
                        mode = TclIO.SEEK_END;
                        break;
                    }
            }
        }
        chan = TclIO.getChannel(interp, argv[1].toString());
        if (chan == null) {
            throw new TclException(interp, "can not find channel named \"" + argv[1].toString() + "\"");
        }
        long offset = TclInteger.get(interp, argv[2]);
        try {
            chan.seek(interp, offset, mode);
        } catch (IOException e) {
            throw new TclRuntimeError("SeekCmd.cmdProc() Error: IOException when seeking " + chan.getChanName() + ":" + e.toString());
        }
    }
}
