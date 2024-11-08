package net.sf.jnclib.tp.ssh2;

/**
 * An exception raised when an attempt to open a channel (via
 * {@link ClientTransport#openSession} or {@link ClientTransport#openChannel}) fails.
 * The exception contains an error code from the SSH server.
 */
public class ChannelException extends SSHException {

    public ChannelException(int code) {
        super(ChannelError.getDescription(code));
        mChannelError = code;
    }

    /**
     * Return the error code received from the server.  The code should be
     * one of the constants in {@link ChannelError}.
     * 
     * @return the error code from the server
     */
    public int getChannelError() {
        return mChannelError;
    }

    private int mChannelError;

    private static final long serialVersionUID = 0;
}
