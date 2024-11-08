package org.cspoz.jcsp;

import jcsp.lang.*;
import jcsp.plugNplay.ProcessWrite;
import java.util.logging.Logger;

/**
 * Class Synchronizer is the opposite to jcsp.plugNplay.DynamicDelta. It reads
 * from all input channels in parallel. When data was received from all input 
 * channels and was the expected data then the process will write the data and
 * start from the beginning.
 */
public class FullSynchronizer implements CSProcess {

    private final AltingChannelInput[] toRead;

    private final ProcessWrite writer;

    /**
     * Create a new Synchronizer.
     * @param in  the set of input channels that must provide the expected data
     * @param out  the channel to write to (after successful reading)
     * @param expect  the data that is expected. If this is a null reference all 
     *                read data must be null, otherwise read data must be equal
     *                to this object (according to the equals(Object) method).
     */
    public FullSynchronizer(AltingChannelInput[] in, ChannelOutput out, Object expect) {
        toRead = in;
        writer = new ProcessWrite(out);
        writer.value = expect;
    }

    /**
     * Start the process in an endless loop, beginning with parallel reading.
     * Deadlock on error, meaning unexpected data was read.
     */
    public void run() {
        boolean loop = true;
        while (loop) {
            if (pending()) {
                if (checkAndReadData()) {
                    writer.run();
                } else {
                    loop = false;
                }
            }
        }
    }

    /**
     * Internal check for correct data: If the expected data (the value of the
     * writer process) is null all readers must have received a null reference.
     * Otherwise every reader must have received a reference equal to the expected
     * data (according to the equals(Object) method).
     */
    private boolean checkAndReadData() {
        Object read;
        for (int i = 0; i < toRead.length; i++) {
            read = toRead[i].read();
            if (writer.value == null) {
                if (read != null) {
                    return false;
                }
            } else {
                if (!writer.value.equals(read)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Internal check for correct data: If the expected data (the value of the
     * writer process) is null all readers must have received a null reference.
     * Otherwise every reader must have received a reference equal to the expected
     * data (according to the equals(Object) method).
     */
    private boolean pending() {
        for (int i = 0; i < toRead.length; i++) {
            if (!toRead[i].pending()) {
                return false;
            }
        }
        return true;
    }
}
