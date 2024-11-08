package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;

/**
* A command for changing the speed and direction of
* a single train.
**/
public class DccSpeedCommand extends Command {

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new instance for setting the speed in DCC format
    * this command is based on 28 speedsteps
    * @param address The address of the train (0..80)
    * @param speed The new speed of the train (0..28)
    **/
    public DccSpeedCommand(int address, int speed, boolean forward) {
        write_data = new byte[3];
        write_data[0] = (byte) 128;
        write_data[1] = (byte) address;
        speed = Math.min(28, speed);
        int data;
        if (forward) {
            data = 0x60;
        } else {
            data = 0x40;
        }
        if (speed < 0) {
            data |= 0x01;
        } else if (speed > 0) {
            data |= (speed + 1) / 2 + 1;
            if ((speed + 1) % 2 == 1) {
                data |= 0x10;
            }
        }
        write_data[2] = (byte) data;
    }

    /**
     * Execute this command
     */
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
        if (!(o instanceof DccSpeedCommand)) {
            return false;
        }
        DccSpeedCommand otc = (DccSpeedCommand) o;
        if (otc.write_data[1] != this.write_data[1]) {
            return false;
        }
        return true;
    }
}
