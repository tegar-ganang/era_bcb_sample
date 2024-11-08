package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;
import org.jedits.comm.Util;

/**
* A command for retrieving the last changed module.
**/
public class LastChangedCommand extends Command {

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new command for retrieving the last changed module.
    **/
    public LastChangedCommand() {
        write_data = new byte[1];
        write_data[0] = (byte) 190;
    }

    /**
    * Execute this command
    **/
    protected void doExecute(Channel channel) throws CommException {
        read_data = channel.send(write_data);
        int value = read_data[0] & 0xFF;
        try {
            if (value > 0) {
                Util.testModule(value - 1);
            }
            ((MptcChannel) channel).setLastChanged(value);
        } catch (IllegalArgumentException ex) {
            System.err.println("Illegal last changed module: " + value + " (" + read_data[0] + ")");
        }
    }
}
