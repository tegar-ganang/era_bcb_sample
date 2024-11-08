package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.breakpoint.Breakpoint;
import com.bluemarsh.jswat.core.breakpoint.BreakpointFactory;
import com.bluemarsh.jswat.core.breakpoint.BreakpointManager;
import com.bluemarsh.jswat.core.breakpoint.BreakpointProvider;
import com.bluemarsh.jswat.core.session.Session;
import com.sun.jdi.request.EventRequest;
import java.io.PrintWriter;
import org.openide.util.NbBundle;

/**
 * Sets a breakpoint to stop when threads start and die.
 *
 * @author Nathan Fiedler
 */
public class ThreadBreakpointCommand extends AbstractCommand {

    @Override
    public String getName() {
        return "threadbrk";
    }

    @Override
    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        Session session = context.getSession();
        PrintWriter writer = context.getWriter();
        int suspendPolicy = EventRequest.SUSPEND_ALL;
        boolean onStart = true;
        boolean onDeath = true;
        String filter = null;
        while (arguments.hasMoreTokens()) {
            String token = arguments.nextToken();
            if (token.equals("go")) {
                suspendPolicy = EventRequest.SUSPEND_NONE;
            } else if (token.equals("thread")) {
                suspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
            } else if (token.equals("start")) {
                onDeath = false;
            } else if (token.equals("death")) {
                onStart = false;
            } else {
                filter = token;
                break;
            }
        }
        BreakpointFactory bf = BreakpointProvider.getBreakpointFactory();
        Breakpoint bp = bf.createThreadBreakpoint(filter, onStart, onDeath);
        bp.setEnabled(false);
        bp.setSuspendPolicy(suspendPolicy);
        bp.setEnabled(true);
        BreakpointManager bm = BreakpointProvider.getBreakpointManager(session);
        bm.addBreakpoint(bp);
        writer.println(NbBundle.getMessage(ThreadBreakpointCommand.class, "CTL_threadbrk_Added"));
    }
}
