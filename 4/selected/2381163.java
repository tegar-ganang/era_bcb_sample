package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;

/**
* A command for changing the state of a turnout.
**/
public class ResetTurnoutCommand extends Command {

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new instance for setting the turnout
    **/
    public ResetTurnoutCommand() {
        write_data = new byte[1];
        write_data[0] = (byte) 32;
    }

    /**
    * Execute this command
    **/
    protected void doExecute(Channel channel) throws CommException {
        read_data = channel.send(write_data);
        for (int i = 0; i < write_data.length; i++) {
            if (read_data[i] != write_data[i]) {
                throw new CommException("Invalid return data");
            }
        }
    }
}
