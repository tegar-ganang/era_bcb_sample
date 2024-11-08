package org.jedits.comm.editspro;

import org.jedits.comm.Channel;
import org.jedits.comm.CommException;
import org.jedits.comm.Command;
import org.jedits.util.MotorolaUtils;

/**
* A command for changing the state of a turnout.
**/
public class TurnoutCommand extends Command {

    private static final int SUBADDR[] = { 0, 3, 12, 15, 48, 51, 60, 63 };

    private byte[] write_data;

    private byte[] read_data;

    /**
    * Create a new instance for setting the turnout
    * @param address The address of the turnout (0..80)
    * @param subaddr The connector of the turnout (0..7)
    * @param on Activate the connector?
    **/
    public TurnoutCommand(int address, int subaddr, boolean on) {
        write_data = new byte[4];
        write_data[0] = 0x37;
        write_data[1] = (byte) 0x0c;
        write_data[2] = (byte) MotorolaUtils.address2byte(address);
        write_data[3] = (byte) (SUBADDR[subaddr] + (on ? 192 : 0));
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
