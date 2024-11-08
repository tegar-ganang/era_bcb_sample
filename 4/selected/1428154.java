package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;

/**
* A command for changing the speed, direction and/or functions of
* a single train.
**/
public class TrainCommand extends Command {

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new instance for setting the speed in OLD MOTOROLA format
    * @param address The address of the train (0..80)
    * @param speed The new speed of the train (0..15)
    * @param forward Set the direction
    * @param lights Set the FL function on/off
    **/
    public TrainCommand(int address, int speed, boolean forward, boolean lights) {
        write_data = new byte[3];
        if (speed < 0) {
            write_data[0] = 1;
        } else if (speed == 0) {
            write_data[0] = 0;
        } else {
            speed = Math.min(14, speed);
            write_data[0] = (byte) (speed + 1);
        }
        write_data[1] = (byte) address;
        int b3 = 0;
        if (forward) {
            b3 |= 0x01;
        }
        if (lights) {
            b3 |= 0x02;
        }
        write_data[2] = (byte) b3;
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
        if (!(o instanceof TrainCommand)) {
            return false;
        }
        TrainCommand otc = (TrainCommand) o;
        if (otc.write_data[1] != this.write_data[1]) {
            return false;
        }
        return true;
    }
}
