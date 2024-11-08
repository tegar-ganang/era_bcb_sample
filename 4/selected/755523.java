package net.sourceforge.xjftp.transmissionmode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import net.sourceforge.xjftp.representation.Representation;

/**
 * This class handles transmissions using STREAM mode. The data is sent as a
 * continuous stream of data.
 * <p>
 * Copyright &copy; 2005 <a href="http://xjftp.sourceforge.net/">XJFTP Team</a>.
 * All rights reserved. Use is subject to <a href="http://xjftp.sourceforge.net/LICENSE.TXT">licence terms</a> (<a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache License v2.0</a>)<br/>
 * <p>
 * Last modified: $Date: 2005/01/21 00:03:30 $, by $Author: mmcnamee $
 * <p>
 * @author Mark McNamee (<a href="mailto:mmcnamee@users.sourceforge.net">mmcnamee at users.sourceforge.net</a>)
 * @version $Revision: 1.3 $
 */
public class StreamTransmissionMode extends AbstractTransmissionMode {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * I control the amount of data sent written to and read from
     * the Socket at a time. I can be tweaked if necessary! 
     */
    private final int bufferSize;

    public StreamTransmissionMode() {
        super(TransmissionMode.TYPE_STREAM);
        this.bufferSize = DEFAULT_BUFFER_SIZE;
    }

    public StreamTransmissionMode(int bufferSize) {
        super(TransmissionMode.TYPE_STREAM);
        this.bufferSize = bufferSize;
    }

    /**
     * Reads the contents of the file from "in", and writes the data to the
     * given socket using the specified representation.
     */
    public void sendFile(InputStream in, Socket s, Representation representation) throws IOException {
        OutputStream out = representation.getOutputStream(s);
        byte buf[] = new byte[this.bufferSize];
        int nread;
        while ((nread = in.read(buf)) > 0) {
            out.write(buf, 0, nread);
        }
        out.close();
    }

    /**
     * Reads data from the given socket and converts it from the specified
     * representation to local representation, writing the result to a file via
     * "out".
     */
    public void receiveFile(Socket s, OutputStream out, Representation representation) throws IOException {
        InputStream in = representation.getInputStream(s);
        byte buf[] = new byte[this.bufferSize];
        int nread;
        while ((nread = in.read(buf, 0, this.bufferSize)) > 0) {
            out.write(buf, 0, nread);
        }
        in.close();
    }
}
