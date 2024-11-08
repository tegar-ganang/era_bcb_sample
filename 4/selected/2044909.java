package org.tracfoundation.trac2001;

import org.tracfoundation.trac2001.form.*;
import org.tracfoundation.trac2001.primitive.*;
import org.tracfoundation.trac2001.util.*;
import org.tracfoundation.trac2001.gui.*;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

/**
 * This is the main class of a reference implementation
 * for the TRAC 2001 programming language.
 *
 * @author  Edith Mooers, Trac Foundation http://tracfoundation.org
 * @version 1.0 (c) 2001
 */
public class TRAC2001 {

    public Interpreter interpreter;

    public ByteBuffer inputBuffer;

    protected FIFOByteBuffer activeBuffer;

    protected PrimitiveBuffer neutralBuffer;

    protected Primitive activePrimitive;

    public RootNode formBuffer;

    public Channel[] fileChannels;

    public IOHandler ioHandler;

    public byte DUMP = (byte) '@';

    public byte META = (byte) '\'';

    public boolean ECHO = true;

    /**
     * Set up the interpreter by loading the idling script.  
     * This constructor always uses frames.  
     * Mostly just needed to keep the extending classes happy
     */
    public TRAC2001() {
        this(TRACUtil.idlingScript, true);
    }

    /**
     * Set up the interpreter by loading the idling script.
     *
     * @param <CODE>boolean</CODE> if true, the program uses 
     * a frame even if it knows the os.
     */
    public TRAC2001(boolean frame) {
        this(TRACUtil.idlingScript, frame);
    }

    /**
     * This constructor is for loading scripts at startup.
     *
     * @param <CODE>byte []</CODE> the script to load as 
     * in the active buffer when the interpreter starts up.
     *
     * @param <CODE>boolean</CODE>frame if true, 
     * the program uses a frame even if it knows the os.
     */
    public TRAC2001(byte[] startUp, boolean frame) {
        interpreter = new Interpreter(this);
        inputBuffer = new ByteBuffer();
        activeBuffer = new FIFOByteBuffer(startUp);
        neutralBuffer = new PrimitiveBuffer();
        activePrimitive = new Primitive();
        formBuffer = new RootNode();
        ioHandler = new IOHandler(this, frame);
        fileChannels = new Channel[TRACUtil.MAXCHANNELS];
        checkActiveBuffer();
    }

    /**
     * Parse the active buffer for Primitives and call the interpreter
     * the loop is executed every time a primitive has been fully parsed
     * and executed. This method exits with a call to the interpreter
     * when a complete primitive has been parsed.
     */
    private void checkActiveBuffer() {
        int pc = 0;
        while (true) {
            if (activeBuffer.empty()) {
                neutralBuffer.clear();
                activeBuffer.add(TRACUtil.idlingScript);
            }
            pc = parser(pc);
        }
    }

    /**
     * Do the actual parsing, keeping track of the 
     * parenthesis count.
     *
     * @param <CODE>int</CODE> the initial parenthesis count.
     *
     * @param <CODE>int</CODE> the final parenthesis count.
     */
    public int parser(int pc) {
        byte b = activeBuffer.pop();
        if (pc <= 0) {
            if (b == (byte) ':') {
                if (!activeBuffer.empty()) {
                    if (activeBuffer.next() == (byte) '(') {
                        activeBuffer.pop();
                        neutralBuffer.newPrimitive();
                    }
                }
            } else if (b == (byte) '(') {
                pc = 1;
            } else if (b == (byte) ',') {
                neutralBuffer.newArg();
            } else if (b == (byte) ')') {
                if (!neutralBuffer.empty()) activePrimitive = neutralBuffer.pop();
                interpreter.interpret();
            } else if (Character.isWhitespace((char) b)) ; else neutralBuffer.add(b);
        } else {
            if (b == (byte) '(') {
                ++pc;
                neutralBuffer.add(b);
            } else if (b == (byte) ')') {
                if (--pc > 0) neutralBuffer.add(b);
            } else neutralBuffer.add(b);
        }
        return pc;
    }

    /**
     * Supply a z-return argument.
     *
     * @param <CODE>byte []</CODE> the z-return value.
     */
    public void zReturn(byte[] ret) {
        activeBuffer.add(ret);
    }

    /**
     * Returns the Channel, null if the channel number isn't valid.
     *
     * @param <CODE>byte</CODE> the number of the channel to get.
     */
    public Channel getChannel(byte no) {
        if (no < TRACUtil.ZERO + 1 || no > TRACUtil.ZERO + TRACUtil.MAXCHANNELS) return null;
        if (fileChannels[no - TRACUtil.ZERO] == null) fileChannels[no - TRACUtil.ZERO] = new Channel(no);
        return fileChannels[no - TRACUtil.ZERO];
    }

    /**
     * Add an array of bytes to the active buffer.
     *
     * @param <CODE>byte []</CODE> the value to add.
     */
    public void addToActiveBuffer(byte[] value) {
        activeBuffer.add(value);
    }

    /**
     * Add an array of bytes to the neutral buffer.
     *
     * @param <CODE>byte []</CODE> the value to add.
     */
    public void addToNeutralBuffer(byte[] value) {
        neutralBuffer.add(value);
    }

    /**
     * Returns the primitive currently being parsed.
     *
     * @param <CODE>Primitive</CODE> the primitive currently
     * being interpreted.
     */
    public Primitive getActivePrimitive() {
        return activePrimitive;
    }

    /**
   * Run the trac2001 interpreter using frames.
   *
   * @param <CODE>String []</CODE> the input 
   * can accept the flag "-f" to indicate that a frame should be
   * used no matter which os the process is running on.  After any flags, a
   * file name that contains a valid TRAC 2001 script can be supplied to be
   * loaded in the active buffer at startup instead of the idling script.
   */
    public static void main(String[] args) {
        boolean frame = false;
        if (args.length > 0) {
            int i = 0;
            System.out.println(args[0]);
            if (args[i].equals("-f")) {
                frame = true;
                i++;
            }
            if (i < args.length) try {
                File in = new File(args[i]);
                byte[] startUp = new byte[(int) in.length()];
                FileInputStream inStream = new FileInputStream(in);
                int len = inStream.read(startUp);
                if (len == startUp.length) new TRAC2001(startUp, frame); else System.out.println("Error reading the start up script");
                inStream.close();
            } catch (IOException ioe) {
                System.out.println("Trouble reading the start up script");
                ioe.printStackTrace();
            }
        }
        new TRAC2001(frame);
    }
}
