package org.jedits.comm.editspro;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;
import org.jedits.util.MotorolaUtils;

/**
* A command for changing the speed, direction and/or functions of
* a single train.
**/
public class TrainCommand extends Command {

    public static final int REVERSE_SPEED = 0x01;

    private byte[] write_data;

    private byte[] read_data;

    private int type;

    /**
    * Create a new instance for setting the speed in OLD MOTOROLA format
    * @param address The address of the train (0..80)
    * @param speed The new speed of the train (0..15)
    * @param f0_on Set function 0 on/off
    **/
    public TrainCommand(int address, int speed, boolean f0_on) {
        this.type = 1;
        write_data = new byte[4];
        write_data[0] = 0x37;
        write_data[1] = (byte) (0x08 | MotorolaUtils.TRIT[f0_on ? 1 : 0]);
        write_data[2] = (byte) MotorolaUtils.address2byte(address);
        write_data[3] = (byte) MotorolaUtils.speed2byte(speed);
    }

    /**
    * Create a new instance for setting the speed & direction
    * @param address The address of the train (0..80)
    * @param speed The new speed of the train (0..15)
    * @param direction The new direction of the train (FORWARD, BACKWARDS)
    * @param f0_on Set function 0 on/off
    **/
    public TrainCommand(int address, int speed, int direction, boolean f0_on) {
        this.type = 2;
        write_data = new byte[4];
        write_data[0] = 0x37;
        write_data[1] = (byte) (0x08 | MotorolaUtils.TRIT[f0_on ? 1 : 0]);
        write_data[2] = (byte) MotorolaUtils.address2byte(address);
        write_data[3] = (byte) MotorolaUtils.speedDir2byte(speed, direction);
    }

    /**
    * Create a new instance for setting the speed value of F1,2,3,4
    * @param address The address of the train (0..80)
    * @param speed The new speed of the train (0..15)
    * @param f0_on Set function 0 on/off
    * @param fx_num The number of the function to set (1..4)
    * @param fx_on Set function X on/off
    **/
    public TrainCommand(int address, int speed, boolean f0_on, int fx_num, boolean fx_on) {
        this.type = 3;
        write_data = new byte[4];
        write_data[0] = 0x37;
        write_data[1] = (byte) (0x08 | MotorolaUtils.TRIT[f0_on ? 1 : 0]);
        write_data[2] = (byte) MotorolaUtils.address2byte(address);
        switch(fx_num) {
            case 1:
                write_data[3] = (byte) MotorolaUtils.speedF12byte(speed, fx_on);
                break;
            case 2:
                write_data[3] = (byte) MotorolaUtils.speedF22byte(speed, fx_on);
                break;
            case 3:
                write_data[3] = (byte) MotorolaUtils.speedF32byte(speed, fx_on);
                break;
            case 4:
                write_data[3] = (byte) MotorolaUtils.speedF42byte(speed, fx_on);
                break;
            default:
                throw new IllegalArgumentException("Invalid function number " + fx_num);
        }
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
        if (otc.type != this.type) {
            return false;
        }
        if (otc.write_data[2] != this.write_data[2]) {
            return false;
        }
        return true;
    }
}
