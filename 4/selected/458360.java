package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;
import org.jedits.comm.Util;

/**
* A command for reading the value of a feedback module.
**/
public class FeedbackCommand extends Command {

    private byte[] write_data;

    private byte[] read_data;

    private int value;

    private int module;

    MptcController controller;

    /**
    * Create a new instance
    * @param module The module number (0..31)
    **/
    public FeedbackCommand(MptcController controller, int module) {
        Util.testModule(module);
        this.controller = controller;
        this.module = module;
        write_data = new byte[1];
        write_data[0] = (byte) (192 + module);
    }

    /**
    * Execute this command
    **/
    protected void doExecute(Channel channel) throws CommException {
        read_data = channel.send(write_data);
        value = read_data[0] & 0xFF;
        controller.processFeedbackData(module, value);
    }
}
