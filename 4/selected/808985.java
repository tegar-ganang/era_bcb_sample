package com.bluemarsh.jswat.command;

import com.bluemarsh.jswat.ContextManager;
import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.util.Threads;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

/**
 * Defines the class that handles the 'thread' command.
 *
 * @author  Nathan Fiedler
 */
public class threadCommand extends JSwatCommand {

    /**
     * Builds up a description of the given thread. This includes the
     * thread ID value, it's full name, and the status of the thread.
     *
     * @param  buffer  StringBuffer to append description to, if null
     *                 a buffer will be allocated.
     * @param  thrd    Thread reference.
     * @return  String containing thread description and status.
     */
    protected static String buildDescriptor(StringBuffer buffer, ThreadReference thrd) {
        if (thrd == null) {
            return null;
        }
        if (buffer == null) {
            buffer = new StringBuffer();
        }
        try {
            buffer.append("id[");
            buffer.append(thrd.uniqueID());
            buffer.append("] ");
            buffer.append(thrd.name());
            buffer.append(": ");
            buffer.append(Threads.threadStatus(thrd));
        } catch (Exception e) {
            return e.toString();
        }
        return buffer.toString();
    }

    /**
     * Perform the 'thread' command.
     *
     * @param  session  JSwat session on which to operate.
     * @param  args     Tokenized string of command arguments.
     * @param  out      Output to write messages to.
     */
    public void perform(Session session, CommandArguments args, Log out) {
        if (!session.isActive()) {
            throw new CommandException(Bundle.getString("noActiveSession"));
        }
        ContextManager contextManager = (ContextManager) session.getManager(ContextManager.class);
        if (!args.hasMoreTokens()) {
            ThreadReference thread = contextManager.getCurrentThread();
            if (thread == null) {
                throw new CommandException(Bundle.getString("noCurrentThread"));
            } else {
                out.writeln(Bundle.getString("thread.currentThread") + ' ' + buildDescriptor(null, thread));
            }
            return;
        }
        VirtualMachine vm = session.getConnection().getVM();
        ThreadReference thread = Threads.getThreadByID(vm, args.nextToken());
        if (thread != null) {
            contextManager.setCurrentThread(thread);
        } else {
            throw new CommandException(Bundle.getString("invalidThreadID"));
        }
    }
}
