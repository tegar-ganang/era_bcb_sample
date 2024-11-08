package org.cspoz.jcsp;

import jcsp.lang.*;
import jcsp.plugNplay.ProcessRead;
import jcsp.plugNplay.ProcessWrite;
import java.util.logging.Logger;

/**
 * Class Synchronizer is the opposite to jcsp.plugNplay.DynamicDelta. It reads
 * from all input channels in parallel. When data was received from all input 
 * channels and was the expected data then the process will write the data and
 * start from the beginning.
 */
public class Synchronizer implements CSProcess {

    private final Parallel par;

    private final ProcessRead[] reader;

    private final ProcessWrite writer;

    /**
     * Create a new Synchronizer.
     * @param in  the set of input channels that must provide the expected data
     * @param out  the channel to write to (after successful reading)
     * @param expect  the data that is expected. If this is a null reference all 
     *                read data must be null, otherwise read data must be equal
     *                to this object (according to the equals(Object) method).
     */
    public Synchronizer(ChannelInput[] in, ChannelOutput out, Object expect) {
        reader = new ProcessRead[in.length];
        for (int i = 0; i < in.length; i++) {
            reader[i] = new ProcessRead(in[i]);
        }
        writer = new ProcessWrite(out);
        writer.value = expect;
        par = new Parallel(reader);
    }

    /**
     * Start the process in an endless loop, beginning with parallel reading.
     * Deadlock on error, meaning unexpected data was read.
     */
    public void run() {
        boolean loop = true;
        while (loop) {
            par.run();
            if (checkReadData()) {
                writer.run();
            } else {
                loop = false;
            }
        }
    }

    /**
     * Internal check for correct data: If the expected data (the value of the
     * writer process) is null all readers must have received a null reference.
     * Otherwise every reader must have received a reference equal to the expected
     * data (according to the equals(Object) method).
     */
    private boolean checkReadData() {
        for (int i = 0; i < reader.length; i++) {
            if (writer.value == null) {
                if (reader[i].value != null) {
                    return false;
                }
            } else {
                if (!writer.value.equals(reader[i].value)) {
                    return false;
                }
            }
        }
        return true;
    }
}
