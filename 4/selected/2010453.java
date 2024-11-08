package tcl.lang;

import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "read" command in Tcl.
 */
class ReadCmd implements Command {

    /**
     * This procedure is invoked to process the "read" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */
    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
        Channel chan;
        int i = 1;
        int toRead = 0;
        int charactersRead;
        boolean readAll = true;
        boolean noNewline = false;
        TclObject result;
        if ((argv.length != 2) && (argv.length != 3)) {
            errorWrongNumArgs(interp, argv[0].toString());
        }
        if (argv[i].toString().equals("-nonewline")) {
            noNewline = true;
            i++;
        }
        if (i == argv.length) {
            errorWrongNumArgs(interp, argv[0].toString());
        }
        chan = TclIO.getChannel(interp, argv[i].toString());
        if (chan == null) {
            throw new TclException(interp, "can not find channel named \"" + argv[i].toString() + "\"");
        }
        i++;
        if (i < argv.length) {
            String arg = argv[i].toString();
            if (Character.isDigit(arg.charAt(0))) {
                toRead = TclInteger.get(interp, argv[i]);
                readAll = false;
            } else if (arg.equals("nonewline")) {
                noNewline = true;
            } else {
                throw new TclException(interp, "bad argument \"" + arg + "\": should be \"nonewline\"");
            }
        }
        try {
            if (chan.getEncoding() == null) {
                result = TclByteArray.newInstance();
            } else {
                result = TclString.newInstance(new StringBuffer(64));
            }
            if (readAll) {
                charactersRead = chan.read(interp, result, TclIO.READ_ALL, 0);
                if (noNewline) {
                    String inStr = result.toString();
                    if ((charactersRead > 0) && (inStr.charAt(charactersRead - 1) == '\n')) {
                        interp.setResult(inStr.substring(0, (charactersRead - 1)));
                        return;
                    }
                }
            } else {
                charactersRead = chan.read(interp, result, TclIO.READ_N_BYTES, toRead);
            }
            interp.setResult(result);
        } catch (IOException e) {
            throw new TclRuntimeError("ReadCmd.cmdProc() Error: IOException when reading " + chan.getChanName());
        }
    }

    /**
     * A unique error msg is printed for read, therefore dont call this 
     * instead of the standard TclNumArgsException().
     *
     * @param interp the current interpreter.
     * @param cmd the name of the command (extracted form argv[0] of cmdProc)
     */
    private void errorWrongNumArgs(Interp interp, String cmd) throws TclException {
        throw new TclException(interp, "wrong # args: should be \"" + "read channelId ?numChars?\" " + "or \"read ?-nonewline? channelId\"");
    }
}
