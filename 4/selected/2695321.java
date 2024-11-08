package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;

/**
* A command for changing the speed and direction of
* a single train.
**/
public class DccFunctionCommand extends Command {

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new instance for setting the speed in DCC format
    * this command is based on 28 speedsteps
    * @param address The address of the train (0..80)
    * @param fl The new value of the FL function
    * @param f1 The new value of the F1 function
    * @param f2 The new value of the F2 function
    * @param f3 The new value of the F3 function
    * @param f4 The new value of the F4 function
    **/
    public DccFunctionCommand(int address, boolean fl, boolean f1, boolean f2, boolean f3, boolean f4) {
        write_data = new byte[3];
        write_data[0] = (byte) 128;
        write_data[1] = (byte) address;
        int data = 0x80;
        data |= (f1) ? 0x01 : 0;
        data |= (f2) ? 0x02 : 0;
        data |= (f3) ? 0x04 : 0;
        data |= (f4) ? 0x08 : 0;
        data |= (fl) ? 0x10 : 0;
        write_data[2] = (byte) data;
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

    /** A'm I equal to the given object? */
    public boolean equals(Object o) {
        if (!(o instanceof DccFunctionCommand)) {
            return false;
        }
        DccFunctionCommand otc = (DccFunctionCommand) o;
        if (otc.write_data[1] != this.write_data[1]) {
            return false;
        }
        return true;
    }
}
