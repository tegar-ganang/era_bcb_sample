package com.bluemarsh.jswat.command;

import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.breakpoint.Breakpoint;
import com.bluemarsh.jswat.breakpoint.BreakpointManager;
import com.bluemarsh.jswat.breakpoint.ResolveException;
import com.bluemarsh.jswat.breakpoint.ThreadBreakpoint;
import com.sun.jdi.request.EventRequest;

/**
 * Defines the class that handles the 'threadbrk' command.
 *
 * @author  Nathan Fiedler
 */
public class threadbrkCommand extends JSwatCommand {

    /**
     * Perform the 'threadbrk' command.
     *
     * @param  session  JSwat session on which to operate.
     * @param  args     Tokenized string of command arguments.
     * @param  out      Output to write messages to.
     */
    public void perform(Session session, CommandArguments args, Log out) {
        int suspendPolicy = EventRequest.SUSPEND_ALL;
        String threadName = null;
        boolean onStart = true;
        boolean onDeath = true;
        while (args.hasMoreTokens()) {
            String arg = args.nextToken();
            if (arg.equals("start")) {
                onDeath = false;
            } else if (arg.equals("death")) {
                onStart = false;
            } else if (arg.equals("go")) {
                suspendPolicy = EventRequest.SUSPEND_NONE;
            } else if (arg.equals("thread")) {
                suspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
            } else {
                threadName = arg;
            }
        }
        BreakpointManager brkman = (BreakpointManager) session.getManager(BreakpointManager.class);
        Breakpoint bp = new ThreadBreakpoint(threadName, onStart, onDeath);
        try {
            brkman.addNewBreakpoint(bp);
            bp.setEnabled(false);
            bp.setSuspendPolicy(suspendPolicy);
            bp.setEnabled(true);
            out.writeln(Bundle.getString("threadbrk.breakpointAdded"));
        } catch (ResolveException re) {
            throw new CommandException(re);
        }
    }
}
