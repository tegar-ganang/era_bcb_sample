package com.cyberoblivion.comm.plc.modbus;

import com.cyberoblivion.comm.TCPDevice;
import java.io.*;
import java.net.*;
import com.cyberoblivion.util.BitByteUtils;
import org.apache.log4j.Logger;

/**
 * Describe class ModbusTCP here.
 *
 *
 * Created: Sat Jan  7 17:39:13 2006
 *
 * @author <a href="mailto:bene@cyberoblivion.com">Ben Erridge</a>
 * @version 1.0
 */
public class ModbusTCPDevice extends TCPDevice {

    /**
     * It is intended that this
     * logger will be shut off in favor of the global logger during
     * production. */
    public static Logger logger = Logger.getLogger(ModbusTCPDevice.class);

    /**
     * It is intended that
     * this logger will be shut off in favor of the global logger during
     * production.  The logger guarantees that loggers obtained with the
     * same name return the same logger instance.*/
    public static Logger modbusTCPDeviceLogger;

    static {
        modbusTCPDeviceLogger = Logger.getLogger(ModbusTCPDevice.class);
    }

    /**
     * Describe constant <code>TRANSACT_IDENTIFIER1</code> here.
     * Transaction number not normally used
     */
    public static final byte TRANSACT_IDENTIFIER1 = 0;

    /**
     * Describe constant <code>TRANSACT_IDENTIFIER2</code> here.
     * copied by server
     */
    public static final byte TRANSACT_IDENTIFIER2 = 1;

    /**
     * Describe constant <code>PROTOCOL_IDENTIFIER1</code> here.
     * always 0
     */
    public static final byte PROTOCOL_IDENTIFIER1 = 2;

    /**
     * Describe constant <code>PROTOCOL_IDENTIFIER2</code> here.
     * always 0
     */
    public static final byte PROTOCOL_IDENTIFIER2 = 3;

    /**
     * Describe constant <code>LENGTH_FIELD_UPPERBYTE</code> here.
     * high byte length of this message
     */
    public static final byte LENGTH_FIELD_UPPERBYTE = 4;

    /**
     * Describe constant <code>LENGTH_FIELD_LOWERBYTE</code> here.
     * low byte length of this message
     */
    public static final byte LENGTH_FIELD_LOWERBYTE = 5;

    /**
     * Describe constant <code>UNIT_IDENTIFIER</code> here.
     * only used for slave devices and not implemented here
     */
    public static final byte UNIT_IDENTIFIER = 6;

    /**
     * Describe constant <code>MODBUS_FUNC_CODE</code> here.
     * what function to perform
     */
    public static final byte MODBUS_FUNC_CODE = 7;

    /**
     * Describe constant <code>REFERRENCE_NUMBER1</code> here.
     * offset into plc register high byte
     */
    public static final byte REFERRENCE_NUMBER1 = 8;

    /**
     * Describe constant <code>REFERRENCE_NUMBER2</code> here.
     * offset into plc register high byte
     */
    public static final byte REFERRENCE_NUMBER2 = 9;

    /**
     * Describe constant <code>WORD_COUNT1</code> here.
     * number of words high byte this is function specific
     */
    public static final byte WORD_COUNT1 = 10;

    /**
     * Describe constant <code>WORD_COUNT2</code> here.
     * number of words low byte this is function specific
     */
    public static final byte WORD_COUNT2 = 11;

    /**
     * Describe constant <code>BYTE_COUNT</code> here.
     * number of bytes left in this message high byte
     * this is not used because max bytes is 200
     */
    public static final byte BYTE_COUNT = 12;

    /**
     * Describe constant <code>READ_COILS</code> here.
     * read coil function id
     */
    public static final byte READ_COILS = 1;

    /**
     * Describe constant <code>READ_INPUT_DESC</code> here.
     * read input function id
     */
    public static final byte READ_INPUT_DESC = 2;

    /**
     * Describe constant <code>READ_MULT_REGS</code> here.
     * read multiple registers functionID
     */
    public static final byte READ_MULT_REGS = 3;

    /**
     * Describe constant <code>READ_INPUT_REGS</code> here.
     * read input registers function ID
     */
    public static final byte READ_INPUT_REGS = 4;

    /**
     * Describe constant <code>WRITE_COIL</code> here.
     * write coil function ID
     */
    public static final byte WRITE_COIL = 5;

    /**
     * Describe constant <code>WRITE_SINGLE_REG</code> here.
     * write single register function id
     */
    public static final byte WRITE_SINGLE_REG = 6;

    /**
     * Describe constant <code>READ_EXCEPT_STAT</code> here.
     * read except stat function id
     */
    public static final byte READ_EXCEPT_STAT = 7;

    /**
     * Describe constant <code>WRITE_MULT_REG</code> here.
     * write multiple registers function ID
     */
    public static final byte WRITE_MULT_REG = 16;

    /**
     * Describe constant <code>READ_GEN_REF</code> here.
     * read general reference function ID
     */
    public static final byte READ_GEN_REF = 20;

    /**
     * Describe constant <code>WRITE_GEN_REF</code> here.
     * write general reference function ID
     */
    public static final byte WRITE_GEN_REF = 21;

    /**
     * Describe constant <code>MASK_WRITE_REGS</code> here.
     *  mask write registers function id
     */
    public static final byte MASK_WRITE_REGS = 22;

    /**
     * Describe constant <code>READ_WRITE_REGS</code> here.
     * read and write registers function ID
     */
    public static final byte READ_WRITE_REGS = 23;

    /**
     * Describe constant <code>READ_FIFO_QUE</code> here.
     * read fifo function ID
     */
    public static final byte READ_FIFO_QUE = 24;

    /**
     * Describe constant <code>READ_MULTREG_RES_SIZE</code> here.
     * the size of a read multiple register reply
     */
    public static final byte READ_MULTREG_RES_SIZE = 9;

    /**
     * Describe constant <code>WRITE_MULTREG_RES_SIZE</code> here.
     * the size of a write multiple register reply
     */
    public static final byte WRITE_MULTREG_RES_SIZE = 8;

    /**
     * Describe constant <code>READ_MULTREG_REQ_SIZE</code> here.
     * the size of a read multiple register request
     */
    public static final byte READ_MULTREG_REQ_SIZE = 6;

    /**
     * Describe constant <code>WRITE_MULTREG_REQ_SIZE</code> here.
     * the size of a write multiple register request
     */
    public static final byte WRITE_MULTREG_REQ_SIZE = 7;

    /**
     * Describe constant <code>MODBUS_TCP_HEADER_SIZE</code> here.  the
     * size of a modbus tcp headerreally only for writing mult regs
     */
    public static final byte MODBUS_TCP_HEADER_SIZE = 6;

    /**
     * Describe constant <code>MAX_DATA_PER_TRANS</code> here.
     * the max size of a single transmission including header
     */
    public static final int MAX_DATA_PER_TRANS = 256;

    /**
     * Describe constant <code>MAX_TCP_RESPONSE</code> here.
     * the max size of a tcp response
     * not really sure but this is good for our purposes
     */
    public static final int MAX_TCP_RESPONSE = 128;

    /**
     * Describe constant <code>MAX_MESSAGE_BYTES</code> here.
     *
     */
    public static final int MAX_MESSAGE_BYTES = 200;

    /**
     * Describe constant <code>DEFAULT_REFERENCE_NUMBER</code> here.
     * default offset into plc
     */
    public static final int DEFAULT_REFERENCE_NUMBER = 1001;

    /**
     * Describe constant <code>DEFAULT_MODBUS_PORT</code> here.
     * default modbus port to connect to on device
     */
    public static final int DEFAULT_MODBUS_PORT = 502;

    /**
     * Describe constant <code>LEAST_SIG_MASK</code> here.  mask to get
     * the least significat byte out of a 16bit variable
     */
    public static final int LEAST_SIG_MASK = 255;

    /**
     * Describe constant <code>MOST_SIG_MASK</code> here.  mask to get
     * the most significant byte out of a 16bit variable
     */
    public static final int MOST_SIG_MASK = 65280;

    /**
     * Describe variable <code>offset</code> here.
     * the offset we will use for this device.
     */
    private int offset;

    /**
     * Creates a new <code>ModbusTCPDevice</code> instance.
     *
     * @param port an <code>int</code> value
     * @param ip an <code>InetAddress</code> value
     * @param offset an <code>int</code> value
     */
    public ModbusTCPDevice(int port, InetAddress ip, int offset) {
        super(ip, port);
        this.offset = offset;
    }

    /**
     * Creates a new <code>ModbusTCPDevice</code> instance.
     *
     * @param port an <code>int</code> value
     * @param ip an <code>InetAddress</code> value
     */
    public ModbusTCPDevice(int port, InetAddress ip) {
        this(port, ip, DEFAULT_REFERENCE_NUMBER);
    }

    public ModbusTCPDevice(InetAddress ip) {
        this(DEFAULT_MODBUS_PORT, ip, DEFAULT_REFERENCE_NUMBER);
    }

    /**
     * This is the PLC register (<code>setOffset</code>) to write or
     * read into.
     *
     * @param offset an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public boolean setOffset(int offset) {
        this.offset = offset;
        return true;
    }

    /**
     * <code>formatReadRequest</code> takes a length, offset.  it
     * formats a byte array into modbusTCP format for a request of the
     * given length and from the given offset
     *
     * @param length an <code>int</code> value the length in bytes in bytes
     * @param offset an <code>int</code> value
     * @return a <code>byte[]</code> value
     */
    public static byte[] formatReadRequest(int length, int offset) {
        int i;
        byte buffer[] = new byte[MODBUS_TCP_HEADER_SIZE + READ_MULTREG_REQ_SIZE];
        buffer[TRANSACT_IDENTIFIER1] = (byte) 0;
        buffer[TRANSACT_IDENTIFIER2] = (byte) 0;
        buffer[PROTOCOL_IDENTIFIER1] = (byte) 0;
        buffer[PROTOCOL_IDENTIFIER2] = (byte) 0;
        buffer[LENGTH_FIELD_UPPERBYTE] = (byte) 0;
        buffer[LENGTH_FIELD_LOWERBYTE] = (byte) (READ_MULTREG_REQ_SIZE);
        buffer[UNIT_IDENTIFIER] = (byte) 0;
        buffer[MODBUS_FUNC_CODE] = (byte) READ_MULT_REGS;
        buffer[MODBUS_FUNC_CODE + 1] = (byte) ((offset & MOST_SIG_MASK) >> 8);
        buffer[MODBUS_FUNC_CODE + 2] = (byte) (((offset) & LEAST_SIG_MASK));
        buffer[MODBUS_FUNC_CODE + 3] = (byte) 0;
        buffer[MODBUS_FUNC_CODE + 4] = (byte) (length / 2);
        if (logger.isDebugEnabled()) {
            String debugInfo = "send buffer length " + buffer.length + " = ";
            for (i = 0; i < buffer.length; i++) {
                debugInfo += " " + (byte) buffer[i];
            }
            logger.debug(debugInfo);
        }
        return buffer;
    }

    /**
     * <code>formatWriteRequest</code> takes a length, and offset.  it
     * formats a byte array into modbusTCP format for a request of the
     * given length and from the given offset
     *
     * @param length an <code>int</code> length in bytes
     * @param offset an <code>int</code> value
     * @param data a <code>byte[]</code> value
     * @return a <code>byte[]</code> value
     */
    public static byte[] formatWriteRequest(int length, int offset, byte[] data) {
        int b = 0, i = 0;
        byte send[];
        byte buffer[] = new byte[MODBUS_TCP_HEADER_SIZE + WRITE_MULTREG_REQ_SIZE];
        buffer[TRANSACT_IDENTIFIER1] = (byte) 1;
        buffer[TRANSACT_IDENTIFIER2] = (byte) 0;
        buffer[PROTOCOL_IDENTIFIER1] = (byte) 0;
        buffer[PROTOCOL_IDENTIFIER2] = (byte) 0;
        buffer[LENGTH_FIELD_UPPERBYTE] = (byte) 0;
        buffer[LENGTH_FIELD_LOWERBYTE] = (byte) ((length) + WRITE_MULTREG_REQ_SIZE);
        buffer[UNIT_IDENTIFIER] = (byte) 0;
        buffer[MODBUS_FUNC_CODE] = (byte) WRITE_MULT_REG;
        buffer[MODBUS_FUNC_CODE + 1] = (byte) ((offset & MOST_SIG_MASK) >> 8);
        buffer[MODBUS_FUNC_CODE + 2] = (byte) (((offset) & LEAST_SIG_MASK));
        buffer[MODBUS_FUNC_CODE + 3] = (byte) 0;
        buffer[MODBUS_FUNC_CODE + 4] = (byte) (length / 2);
        buffer[MODBUS_FUNC_CODE + 5] = (byte) length;
        send = new byte[buffer.length + length];
        for (i = 0; i < buffer.length; i++) {
            send[i] = buffer[i];
        }
        for (; i < (buffer.length + length); i++) {
            send[i] = data[b];
            b++;
        }
        if (logger.isDebugEnabled()) {
            String debugInfo = "send buffer length " + send.length + " = ";
            for (i = 0; i < (buffer.length + length); i++) {
                debugInfo += " " + (int) send[i];
            }
            logger.debug(debugInfo);
        }
        return send;
    }

    /**
     * Describe <code>readMultiReg</code> method here.
     * use default port ofr this read
     *
     * @param offset an <code>int</code> value
     * @param ip an <code>InetAddress</code> value
     * @param length an <code>int</code> value the length in bytes
     * @return a <code>byte[]</code> value
     * @exception IOException if an error occurs
     */
    public static byte[] readMultiReg(int offset, InetAddress ip, int length) throws IOException {
        return readMultiReg(offset, ip, length, DEFAULT_MODBUS_PORT);
    }

    public synchronized byte[] readMultiReg(int offset, int length) throws IOException {
        boolean res = false;
        if (connection == null) {
            res = connect();
            if (!res) {
                logger.error("could not connected");
                return null;
            }
        }
        return readMultiReg(offset, connection, length);
    }

    /**
     * Describe <code>readMultiReg</code> method here.  this function
     * will read multiple registers from a modbusTCP enabled device
     *
     * @param offset an <code>int</code> value
     * @param connection a <code>Socket</code> enables you to keep
     * connection open over multiple requests
     * @param length an <code>int</code> value the length in bytes Length to in bytes read
     * @return a <code>byte[]</code> value
     * @exception IOException if an error occurs
     */
    public static byte[] readMultiReg(int offset, Socket connection, int length) throws IOException {
        byte send[];
        byte recv[];
        int i = 0;
        int regref = 0;
        byte data[] = null;
        byte test[] = new byte[12];
        BufferedInputStream in;
        BufferedOutputStream out;
        in = new BufferedInputStream(connection.getInputStream());
        out = new BufferedOutputStream(connection.getOutputStream());
        send = formatReadRequest(length, offset);
        out.write(send, 0, send.length);
        out.flush();
        logger.debug(READ_MULTREG_RES_SIZE + (length) + "");
        recv = new byte[READ_MULTREG_RES_SIZE + (length)];
        in.read(recv, 0, recv.length);
        data = new byte[length];
        System.arraycopy(recv, READ_MULTREG_RES_SIZE, data, 0, data.length);
        if (logger.isDebugEnabled()) {
            String debugInfo = "rec buffer = ";
            for (i = 0; i < recv.length; i++) {
                debugInfo += " " + (byte) recv[i];
            }
            logger.debug(debugInfo);
            boolean bits[];
            bits = BitByteUtils.getBits(data);
            String debugInfo2 = "";
            for (i = 1; i <= bits.length; i++) {
                debugInfo2 += bits[i - 1] ? 1 : 0;
                if ((i % 16) == 0 && i != 0) {
                    debugInfo2 += "   ";
                } else if ((i % 8) == 0 && i != 0) {
                    debugInfo2 += " ";
                }
            }
            logger.debug(debugInfo2);
            String debugInfo3 = "data buffer = ";
            for (i = 0; i < data.length; i++) {
                debugInfo3 += " " + (byte) data[i];
            }
            logger.debug(debugInfo3);
        }
        return data;
    }

    /**
     * Describe <code>readMultiReg</code> method here.  if not device
     * or connection is given the static method will create a
     * connection do a read the close it down
     *
     * @param offset an <code>int</code> value
     * @param ip an <code>InetAddress</code> value
     * @param length an <code>int</code> value the length in bytes
     * @param port an <code>int</code> value
     * @return a <code>byte[]</code> value
     * @exception IOException if an error occurs
     */
    public static byte[] readMultiReg(int offset, InetAddress ip, int length, int port) throws IOException {
        byte[] data;
        TCPDevice device = new TCPDevice(ip, port);
        device.connect();
        data = readMultiReg(offset, device.connection, length);
        device.disconnect();
        return data;
    }

    /**
     *  <code>writeMultiReg</code> method This is the non-static
     *  version of the write multiregfunction.
     *
     * @param offset an <code>int</code> value
     * @param data a <code>byte[]</code> value
     * @param length an <code>int</code> value
     * @return a <code>boolean</code> value
     * @exception IOException if an error occurs
     */
    public synchronized boolean writeMultiReg(int offset, byte[] data, int length) throws IOException {
        byte send[];
        byte recv[];
        int b = 0, i = 0;
        int regref = 0;
        BufferedInputStream in;
        BufferedOutputStream out;
        boolean res;
        res = connect();
        logger.info("write connected" + res);
        in = new BufferedInputStream(connection.getInputStream());
        out = new BufferedOutputStream(connection.getOutputStream());
        send = formatWriteRequest(length, offset, data);
        out.write(send, 0, send.length);
        out.flush();
        recv = new byte[WRITE_MULTREG_RES_SIZE];
        in.read(recv, 0, WRITE_MULTREG_RES_SIZE);
        disconnect();
        return true;
    }

    /**
     * Describe <code>writeMultiReg</code> method here.  if not given a
     * device connection this static method will create one use it to
     * read registers and then close it down
     *
     * @param offset an <code>int</code> value
     * @param connection a <code>Socket</code> value
     * @param data a <code>byte[]</code> value
     * @param length an <code>int</code> value the length in bytes
     * @return a <code>boolean</code> value
     * @exception IOException if an error occurs
     */
    public static boolean writeMultiReg(int offset, Socket connection, byte[] data, int length) throws IOException {
        byte send[];
        byte recv[];
        int b = 0, i = 0;
        int regref = 0;
        BufferedInputStream in;
        BufferedOutputStream out;
        in = new BufferedInputStream(connection.getInputStream());
        out = new BufferedOutputStream(connection.getOutputStream());
        send = formatWriteRequest(length, offset, data);
        out.write(send, 0, send.length);
        out.flush();
        recv = new byte[WRITE_MULTREG_RES_SIZE];
        in.read(recv, 0, WRITE_MULTREG_RES_SIZE);
        return true;
    }

    /**
     * Describe <code>writeMultiReg</code> method here.
     * this will use the default port to write multiple registers
     *
     * @param offset an <code>int</code> value
     * @param ip an <code>InetAddress</code> value
     * @param data a <code>byte[]</code> value
     * @param length a <code>byte</code> value
     * @return a <code>boolean</code> value
     * @exception IOException if an error occurs
     */
    public static boolean writeMultiReg(int offset, InetAddress ip, byte[] data, byte length) throws IOException {
        return writeMultiReg(offset, ip, data, length, DEFAULT_MODBUS_PORT);
    }

    /**
     * Describe <code>writeMultiReg</code> method here.  write multiple
     * registers to a modbus device and creates a device and connection
     * to do the write then closes it when finished
     *
     * @param offset an <code>int</code> value
     * @param ip an <code>InetAddress</code> value
     * @param data a <code>byte[]</code> value
     * @param length a <code>byte</code> value
     * @param port an <code>int</code> value
     * @return a <code>boolean</code> value true on success
     * @exception IOException if an error occurs
     */
    public static boolean writeMultiReg(int offset, InetAddress ip, byte[] data, byte length, int port) throws IOException {
        TCPDevice device = new TCPDevice(ip, port);
        boolean res = false;
        device.connect();
        res = writeMultiReg(offset, device.connection, data, length);
        device.disconnect();
        return res;
    }

    /**
     * Describe <code>main</code> method here.
     * Test program to write to modbus device
     *
     * write command java com/cyberoblivion/comm/plc/modbus/ModbusTCPDevice
     * --ip 192.168.1.53 --offset 1024 --data 121 --port 502
     *
     *
     *
     * @param args a <code>String[]</code> value
     */
    public static void main(String[] args) throws IOException {
        int i, p = 502, offset = 40000;
        InetAddress a = null;
        byte data[] = new byte[2];
        boolean read = false;
        int readlen = 0;
        byte regdata[];
        int j = 0;
        for (i = 0; i < args.length; i += 2) {
            if (args[i].equals("--ip")) {
                try {
                    a = InetAddress.getByName(args[i + 1]);
                } catch (UnknownHostException e) {
                    logger.error("Can't find host " + args[i + 1] + " " + e);
                    System.exit(1);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(a + "");
                }
            } else if (args[i].equals("--port")) {
                p = Integer.parseInt(args[i + 1]);
                if (logger.isDebugEnabled()) {
                    logger.debug(p + "");
                }
            } else if (args[i].equals("--offset")) {
                offset = Integer.parseInt(args[i + 1]);
                if (logger.isDebugEnabled()) {
                    logger.debug(offset + "");
                }
            } else if (args[i].equals("--r")) {
                readlen = Integer.parseInt(args[i + 1]);
                if (logger.isDebugEnabled()) {
                    logger.debug("read choosen");
                    logger.debug(readlen + "");
                }
                read = true;
            } else if (args[i].equals("--data")) {
                data[1] = (byte) Integer.parseInt(args[i + 1]);
                if (logger.isDebugEnabled()) {
                    logger.debug((int) data[1] + " ");
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bad arg number " + i + " of " + args.length + " >" + args[i] + "<");
                }
                usage();
                System.exit(1);
            }
        }
        if (i < 8) {
            usage();
            System.exit(1);
        }
        try {
            if (!read) {
                logger.info("write choosen");
                ModbusTCPDevice.writeMultiReg(offset, a, data, (byte) data.length, p);
                logger.info("write complete");
            } else {
                regdata = ModbusTCPDevice.readMultiReg(offset, a, readlen, p);
                if (logger.isDebugEnabled()) {
                    String debugInfo = "data buffer = ";
                    for (i = 0; i < regdata.length; i++) {
                        debugInfo += " " + (byte) regdata[i];
                    }
                    logger.debug(debugInfo);
                }
                System.out.println();
            }
        } catch (IOException e) {
            logger.error(e);
            System.exit(1);
        }
        System.exit(0);
    }

    /** Print an explanation how to use the program. */
    public static void usage() {
        System.out.println("ModbusTCPDevice --ip <ip> [--port <port>] [--offset <register>] [[--data <to write>][--read <length in bytes>] ... ");
        System.out.println("<ip> ip to connect to");
        System.out.println("<port> to connect to, bar code readers use next one");
        System.out.println("<offset> offset into plc normally starts at 40000");
        System.out.println("<data> what to write");
        System.out.println("<read> how many byes to read");
    }
}
