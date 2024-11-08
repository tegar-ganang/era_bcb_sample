package net.sf.syracuse.impl;

import net.sf.syracuse.net.NetworkRequest;
import net.sf.syracuse.net.WriteServicer;
import org.apache.commons.logging.Log;
import java.io.IOException;

/**
 * A contribution to the {@code WriteServicer} pipeline which closes the connections of completed requests.
 *
 * @author Chris Conrad
 * @since 1.0.0
 */
public final class RequestTerminator implements WriteServicer {

    private Log log;

    /**
     * Closes the connections of completed requests.
     *
     * @param networkRequest {@inheritDoc}
     */
    public void write(NetworkRequest networkRequest) {
        try {
            networkRequest.getChannel().close();
        } catch (IOException e) {
            log.info("Error closing channel", e);
        }
    }

    /**
     * Sets the {@code Log}.
     *
     * @param log the {@code Log} to set
     */
    public void setLog(Log log) {
        this.log = log;
    }
}
