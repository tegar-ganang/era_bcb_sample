package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.session.Session;
import com.bluemarsh.jswat.core.util.Threads;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import java.io.PrintWriter;
import org.openide.util.NbBundle;

/**
 * Signals an interrupt for a given thread.
 *
 * @author Nathan Fiedler
 */
public class InterruptCommand extends AbstractCommand {

    @Override
    public String getName() {
        return "interrupt";
    }

    @Override
    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        Session session = context.getSession();
        VirtualMachine vm = session.getConnection().getVM();
        PrintWriter writer = context.getWriter();
        String token = arguments.nextToken();
        ThreadReference thread = Threads.findThread(vm, token);
        if (thread == null) {
            throw new CommandException(NbBundle.getMessage(InterruptCommand.class, "ERR_ThreadNotFound", token));
        } else {
            thread.interrupt();
            writer.println(NbBundle.getMessage(InterruptCommand.class, "CTL_interrupt_Interrupted", thread.uniqueID()));
        }
    }

    @Override
    public boolean requiresArguments() {
        return true;
    }

    @Override
    public boolean requiresDebuggee() {
        return true;
    }
}
