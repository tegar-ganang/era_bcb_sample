package org.jedits.comm.editspro;

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

    private EditsProController controller;

    /**
    * Create a new instance
    * @param module The module number (0..31)
    **/
    public FeedbackCommand(EditsProController controller, int module) {
        Util.testModule(module);
        this.controller = controller;
        this.module = module;
        write_data = new byte[2];
        write_data[0] = 0x37;
        write_data[1] = (byte) (0x20 | module);
    }

    /**
    * Execute this command
    **/
    protected void doExecute(Channel channel) throws CommException {
        read_data = channel.send(write_data, 1);
        for (int i = 0; i < write_data.length; i++) {
            if (read_data[i] != write_data[i]) {
                throw new CommException("Invalid return data");
            }
        }
        value = read_data[2] & 0xFF;
        controller.processFeedbackData(module, value);
    }
}
