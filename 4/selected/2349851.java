package com.bluemarsh.jswat.command;

import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.util.Threads;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

/**
 * Defines the class that handles the 'suspend' command.
 *
 * @author  Nathan Fiedler
 */
public class suspendCommand extends JSwatCommand {

    /**
     * Perform the 'suspend' command.
     *
     * @param  session  JSwat session on which to operate.
     * @param  args     Tokenized string of command arguments.
     * @param  out      Output to write messages to.
     */
    public void perform(Session session, CommandArguments args, Log out) {
        if (!session.isActive()) {
            throw new CommandException(Bundle.getString("noActiveSession"));
        }
        if (args.hasMoreTokens() && !args.peek().equals("all")) {
            String token = args.nextToken();
            VirtualMachine vm = session.getConnection().getVM();
            suspendThread(vm, token, out);
            while (args.hasMoreTokens()) {
                suspendThread(vm, args.nextToken(), out);
            }
        } else {
            session.suspendVM(this);
        }
    }

    /**
     * Suspend the thread given by the ID token string.
     *
     * @param  vm   debuggee virtual machine.
     * @param  tid  thread ID as a string.
     * @param  out  output to write to.
     */
    protected void suspendThread(VirtualMachine vm, String tid, Log out) {
        ThreadReference thread = Threads.getThreadByID(vm, tid);
        if (thread != null) {
            thread.suspend();
            out.writeln(Bundle.getString("suspend.threadSuspended"));
        } else {
            throw new CommandException(Bundle.getString("threadNotFound") + ' ' + tid);
        }
    }
}
