package testspravochniktest;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author user
 */
public class DumpSimContent {

    static Logger logger = Logger.getLogger(PortsListTest.class.getName());

    static OutputStream output;

    static InputStream input;

    static SerialPort port;

    static byte[] readBuf = new byte[10000];

    static int readCount;

    static int timeout = 1000;

    static void initStreams() {
        CommPortIdentifier phonePortId = null;
        try {
            phonePortId = CommPortIdentifier.getPortIdentifier("/dev/ttyACM0");
        } catch (NoSuchPortException ex) {
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        try {
            port = (SerialPort) phonePortId.open("Spravochnik", 1000);
        } catch (PortInUseException ex) {
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        logger.info(String.format("Baud rate: %d", port.getBaudRate()));
        try {
            output = port.getOutputStream();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        try {
            input = port.getInputStream();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        logger.info("Streams initialized");
    }

    /**
     *
     * @return Количество прочитанных байт.
     */
    static int readAll() {
        boolean isTimeOver = false;
        long prevReadTime = System.nanoTime();
        while (!isTimeOver) {
            int toRead = 0;
            try {
                toRead = input.available();
                logger.info(String.format("toRead: %d, readCount: %d", toRead, readCount));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                System.exit(1);
            }
            if (toRead > 0) {
                try {
                    input.read(readBuf, readCount, toRead);
                    logger.info(String.format("have read %d bytes", toRead));
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    System.exit(1);
                }
                readCount += toRead;
                prevReadTime = System.nanoTime();
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            long now = System.nanoTime();
            if ((now - prevReadTime) > (timeout * 1000000)) {
                isTimeOver = true;
            }
        }
        return readCount;
    }

    public static void main(String[] args) {
        initStreams();
        String vyberiSim = "AT+CPBS={SM}\r";
        String dayVseNaSim = "AT+CPBR=1,250\r";
        try {
            output.write(vyberiSim.getBytes());
            readAll();
            output.write(dayVseNaSim.getBytes());
            readAll();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        port.close();
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream("dump.bin", false);
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        try {
            fs.write(readBuf, 0, readCount);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        try {
            fs.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
