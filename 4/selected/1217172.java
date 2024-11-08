package net.sf.lpr.daemon.commands.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Locale;
import net.sf.lpr.daemon.DaemonCommand;
import net.sf.lpr.exception.IORuntimeException;

/**
 * This class houses the shared code that is used for querying the LPD server for it's state.  Since the only part of the request to the server that changes is the numeric code all the work can be done here leaving
 * the code for the command to subclasses.
 * @author x_sid
 *
 */
public abstract class AbstractSendQueueState implements DaemonCommand {

    private PrintStream print;

    private InputStream serverIn;

    private String queue;

    private Writer response;

    /**
    * Concrete subclasses must implement this method to return the two digit code encoded with \0XX
    * @return String that is the \0XX code that is to be used to send the request for the state of the queue
    */
    protected abstract String getCode();

    /**
    * Constructor for creating the parent class that does the boiler plate work.
    * @param print stream to write out the query for the state of the queue to.
    * @param serverIn the response stream to read responses from the server.
    * @param queue that is to have it's state queried for.
    * @param response the {@link Writer} to which the response from the server will be written.
    */
    protected AbstractSendQueueState(PrintStream print, InputStream serverIn, String queue, Writer response) {
        this.print = print;
        this.serverIn = serverIn;
        this.queue = queue;
        this.response = response;
    }

    /**
    * A call to this method will send the query and read the response from the LPD server that is pointed at by the constructor provided {@link PrintStream}
    * @throws IORuntimeException if an IO error occurs or the response from the LPD server is not the expected 2 bytes of 0
    */
    public void print() throws IORuntimeException {
        BufferedReader bis = new BufferedReader(new InputStreamReader(serverIn));
        String line;
        try {
            print.printf(Locale.US, "%s%s\n", getCode(), queue);
            while ((line = bis.readLine()) != null) response.write(line + "\n");
            response.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}
