package com.google.code.sagetvaddons.sjq.agent.commands;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.google.code.sagetvaddons.sjq.agent.ProcessRunner;
import com.google.code.sagetvaddons.sjq.listener.Command;
import com.google.code.sagetvaddons.sjq.listener.NetworkAck;
import com.google.code.sagetvaddons.sjq.shared.QueuedTask;

/**
 * @author dbattams
 *
 */
public class Kill extends Command {

    /**
	 * @param in
	 * @param out
	 */
    public Kill(ObjectInputStream in, ObjectOutputStream out) {
        super(in, out);
    }

    @Override
    public void execute() throws IOException {
        try {
            QueuedTask qt = (QueuedTask) getIn().readObject();
            getOut().writeObject(NetworkAck.get(ProcessRunner.kill(ProcessRunner.genThreadName(qt)) ? NetworkAck.OK : NetworkAck.ERR + "Unable to kill specified task!"));
            getOut().flush();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
