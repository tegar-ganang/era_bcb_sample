package org.jedits.comm.mptc;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;
import org.jedits.util.MotorolaUtils;

/**
* A command for changing the state of a turnout.
**/
public class TurnoutCommand extends Command {

    protected static final int[] SWITCH_DATA = { 0, 3, 12, 15, 48, 51, 60, 63 };

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new instance for setting the turnout
    **/
    public TurnoutCommand(int nr, boolean straight) {
        write_data = new byte[3];
        int addr = MotorolaUtils.address2byte(nr / 4);
        int ofs = nr % 4;
        int data = (straight ? SWITCH_DATA[ofs * 2] : SWITCH_DATA[ofs * 2 + 1]) | 192;
        write_data[0] = (byte) 144;
        write_data[1] = (byte) addr;
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
}
