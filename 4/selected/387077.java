package cubeworld;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

/**
 * @author Garg Oyle (garg_oyle@users.sourceforge.net)
 */
public class SocketSession implements Session {

    /** */
    private ArrayList<String> mOutput = new ArrayList<String>();

    /** */
    private Socket mSocket;

    /** */
    private ArrayList<String> mInput = new ArrayList<String>();

    /** Turned on channels. */
    private ArrayList<String> mChannels = new ArrayList<String>();

    /** Name. */
    private String mName = null;

    /**
     * Alive flag.
     */
    private boolean mAlive = true;

    /**
     * Verified name and password?
     */
    private boolean mVerified = false;

    /**
     * Size of read buffer.
     */
    static final int READ_BUFFER_SIZE = 1024;

    /** */
    private byte[] mBuffer = new byte[READ_BUFFER_SIZE];

    /** */
    private int mBufferIndex = 0;

    /**
     * Constructor with given socket.
     *
     * @param socket the socket.
     */
    public SocketSession(final Socket socket) {
        mSocket = socket;
    }

    /**
     * @return the socket
     */
    public final Socket getSocket() {
        return mSocket;
    }

    /**
     * @param socket the socket to set
     */
    public final void setSocket(final Socket socket) {
        mSocket = socket;
    }

    /**
     * Get output.
     *
     * @return collection of output lines.
     */
    public final Collection<String> getOutput() {
        Vector<String> output = new Vector<String>();
        output.addAll(mOutput);
        mOutput.clear();
        return output;
    }

    /**
     * Add input lines.
     *
     * @param line input line
     */
    public final void addInput(final String line) {
        mInput.add(line);
    }

    /**
     * Add output line.
     *
     * @param line output line
     */
    public final void addOutput(final String line) {
        mOutput.add(line + "\n");
    }

    /**
     * @throws IOException in case of failures
     */
    public final void read() throws IOException {
        InputStream inputStream = mSocket.getInputStream();
        try {
            int count = inputStream.read(mBuffer, mBufferIndex, mBuffer.length - mBufferIndex);
            if (0 < count) {
                getLinesFromBuffer(count);
            }
        } catch (SocketTimeoutException e) {
            assert true;
        }
    }

    /**
     * Get input lines from buffer.
     * @param count of bytes the last reading call put into the buffer
     */
    private void getLinesFromBuffer(final int count) {
        int actualSize = mBufferIndex + count;
        ByteArrayInputStream bis = new ByteArrayInputStream(mBuffer, 0, actualSize);
        mBufferIndex = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(bis));
        try {
            String line = reader.readLine();
            while (null != line) {
                addInput(line);
                line = reader.readLine();
            }
        } catch (IOException i) {
            assert true;
        }
        int remaining = bis.available();
        System.arraycopy(mBuffer, actualSize - remaining, mBuffer, 0, remaining);
        mBufferIndex = remaining;
    }

    /**
     * Write output to socket.
     *
     * @throws IOException in case of failures
     */
    public final void write() throws IOException {
        OutputStream outputStream = mSocket.getOutputStream();
        for (String line : getOutput()) {
            outputStream.write(line.getBytes());
        }
        outputStream.flush();
    }

    /**
     * Get all input lines.
     *
     * @return collection of input lines.
     */
    public final Collection<String> getInput() {
        Vector<String> input = new Vector<String>();
        input.addAll(mInput);
        mInput.clear();
        return input;
    }

    /**
     * Check weather or not the session is alive.
     *
     * @return true if it's alive
     */
    public final boolean isAlive() {
        return mAlive;
    }

    /**
     * Set the alive flag.
     *
     * @param aliveFlag
     *            to set.
     */
    public final void setAlive(final boolean aliveFlag) {
        mAlive = aliveFlag;
    }

    /**
     * Get the session name.
     *
     * @return session name
     */
    public final String getName() {
        return mName;
    }

    /**
     * Add channel to 'turned on' list of channels.
     *
     * @param channel
     *            channel's name
     */
    public final void addChannel(final String channel) {
        if (!mChannels.contains(channel)) {
            mChannels.add(channel);
        }
    }

    /**
     * Remove channel from 'turned on' list of channels.
     *
     * @param channel
     *            channel's name
     */
    public final void removeChannel(final String channel) {
        if (mChannels.contains(channel)) {
            mChannels.remove(channel);
        }
    }

    /**
     * @return the channels
     */
    public final Collection<String> getChannels() {
        return mChannels;
    }

    /**
     * We've passed password verification?
     *
     * @return true when session is verified.
     */
    public final boolean isVerified() {
        return mVerified;
    }

    /**
     * Set name.
     * @param name to set.
     */
    public final void setName(final String name) {
        mName = name;
    }

    /**
     * @param verified indicates if session is verified or not.
     */
    public final void setVerified(final boolean verified) {
        mVerified = verified;
    }

    /**
     * Set the player that plays on that session.
     * @param player that plays the session.
     */
    public void setPlayer(final Player player) {
    }
}
