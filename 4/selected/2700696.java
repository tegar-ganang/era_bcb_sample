package com.googlecode.acpj.patterns;

import com.googlecode.acpj.channels.ChannelPoisonedException;
import com.googlecode.acpj.channels.ReadPort;
import com.googlecode.acpj.channels.WritePort;

/**
 * <p>
 * A pre-built actor that reads from one channel, filters the messages it
 * reads with a {@link ChannelOperation} instance and then writes the 
 * unfiltered messages back to another channel.
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class ChannelFilter<Tin, Tout> implements Runnable {

    private ReadPort<Tin> readPort = null;

    private WritePort<Tout> writePort = null;

    private ChannelOperation<Tin, Tout> filter = null;

    private boolean propogatePoison = false;

    public ChannelFilter(ReadPort<Tin> readPort, WritePort<Tout> writePort, ChannelOperation<Tin, Tout> filter, boolean propogatePoison) {
        if (readPort == null) {
            throw new IllegalArgumentException("Read port may not be null.");
        }
        if (writePort == null) {
            throw new IllegalArgumentException("Write port may not be null.");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter operation may not be null.");
        }
        this.readPort = readPort;
        this.writePort = writePort;
        this.filter = filter;
        this.propogatePoison = propogatePoison;
    }

    public void run() {
        this.readPort.claim();
        this.writePort.claim();
        Tin input = null;
        Tout output = null;
        boolean poisoned = false;
        while (!poisoned) {
            try {
                input = this.readPort.read();
            } catch (ChannelPoisonedException e) {
                if (this.propogatePoison) {
                    this.writePort.poison();
                    poisoned = true;
                }
            }
            if (!poisoned) {
                output = this.filter.process(input);
                if (output != null) {
                    try {
                        this.writePort.write(output);
                    } catch (ChannelPoisonedException e) {
                        if (this.propogatePoison) {
                            this.readPort.poison();
                            poisoned = true;
                        }
                    }
                }
            }
        }
    }
}
