package src.command;

import src.util.CRC;
import src.util.Zs710Exception;
import ewe.io.FileOutputStream;
import ewe.io.IOException;
import ewe.io.SerialPort;
import ewe.io.StringWriter;
import ewe.sys.Convert;
import ewe.sys.TimeOut;
import ewe.sys.Vm;
import ewe.sys.mThread;
import ewe.util.ByteArray;
import ewe.util.Vector;

/**
 * Communication s710 and ir (pc), byte mapping is not necassery
 * 
 * @author Karl Neuhold
 */
public class IRCommunication extends Communication {

    /** used for communication */
    private SerialPort serialPort;

    /** useed for progressBar */
    private int sumOfReadedBytes, bytesToReadHandle;

    /** received packet */
    private Packet receivedPacket;

    /** Timeouts: get file */
    private int timeOutAfterReading, timeOutAfterContinue;

    /** 
     private Handle comHandle;

     /**
     * 
     * @param portName
     *            used ir-port (serial port)
     * @throws IOException -
     */
    public IRCommunication(String portName) throws IOException {
        serialPort = new SerialPort(portName, 9600, 8, SerialPort.NOPARITY, 2);
        consoleStream = new StringWriter();
        consoleStream.write("Connected to " + portName + "\n");
        receivedPacket = null;
        downloadedWorkouts = null;
        if (!serialPort.isOpen()) {
            throw new IOException("Could not open port!");
        }
    }

    /**
     * 
     * Only used for unit test.
     */
    public IRCommunication() {
    }

    /**
     * Check if all bytes are read.
     * @param readBytes Number of bytes to receive.
     * @param bytesToRead Bytes received.
     * @param offset Offset bytes
     * @throws IOException Beschreibung Parameter 
     */
    private void checkReadError(int readBytes, byte[] bytesToRead, int offset) throws IOException {
        if (readBytes < bytesToRead.length) {
            displayReceivedBytes(new ByteArray(bytesToRead));
            throw new IOException("Only " + (readBytes + offset) + " bytes readed, but " + (bytesToRead.length + offset) + " bytes to read!\n" + "Timout is too low!");
        }
        if (readBytes > 1040) {
            throw new IOException("Packet size was too big!");
        }
    }

    /**
     * @param packet
     *            Readed bytes.
     */
    private void displayReceivedBytes(ByteArray packet) {
        for (int i = 0; i < packet.length; i++) {
            Vm.debug("read: [" + i + "] " + Convert.intToHexString(packet.toBytes()[i]));
        }
    }

    /**
     * @see Communication#sendPacket(Packet)
     */
    public void sendPacket(final Packet sendPacket) throws IOException {
        Vm.debug("sendPacket:" + sendPacket.toString());
        int length = sendPacket.getPacketAsByteArray().length;
        int writeBytes = serialPort.writeBytes(sendPacket.getPacketAsByteArray(), 0, length);
        if (writeBytes < 0) {
            throw new IOException("Sending was not succesfully");
        }
        Vm.debug("sendPacket: " + writeBytes + " Bytes written");
    }

    /**
     * Receive Packets used by getFiles.
     * 
     * @throws IOException
     *             If an communication error occurs.
     *  
     */
    public void receivePackets() throws IOException {
        new mThread() {

            public void run() {
                Vm.debug("Receive Packet");
                consoleStream.write("Receive Packets!\n");
                int readBytes;
                ByteArray readedWorkouts = new ByteArray();
                int numberOfPackets = 1;
                byte[] sendContinue = createContinueTransmissionPacket();
                int packetBytes = 0;
                int fileBytes = 0;
                boolean isFirstPacket = true;
                byte[] tmp;
                try {
                    while (serialPort.read() != (byte) 0x5c) {
                        nap(1000);
                    }
                    Vm.debug("... start reading ...");
                    while (numberOfPackets != 0) {
                        getFilesReadPacket(readedWorkouts, sendContinue, isFirstPacket);
                    }
                    Vm.debug("** END: Last Packet received **\n");
                    saveWorkoutsAsSRD(readedWorkouts);
                } catch (IOException e) {
                    getFilesHandleIOException(e);
                }
            }
        }.start();
    }

    /**
     * Readable output for a byte array.
     * @param bytesToShow Byte array to show.
     * @return Readable output (hex strings) for a byte array.
     */
    public String toString(ByteArray bytesToShow) {
        String output = new String();
        for (int i = 0; i < bytesToShow.length; i++) {
            output += "[" + i + "] = " + Convert.intToHexString(bytesToShow.toBytes()[i]) + "\n";
        }
        return output;
    }

    /**
     * Remove the PacketHeader and PacketTrailer to receive the payload (= data)
     * 
     * @param receivedPacket
     *            Received Packet
     * @param isFirstPacket
     *            First Packet received?
     * @param isLastPacket
     *            Last Packet received=
     * @return Payload of the packet.
     */
    private ByteArray removeHeaderAndTrailer(ByteArray receivedPacket, boolean isFirstPacket, boolean isLastPacket) {
        if (isFirstPacket) {
            receivedPacket.delete(0, 10);
            receivedPacket.delete(receivedPacket.length - 3, 3);
        } else if (!isLastPacket) {
            receivedPacket.delete(0, 5);
            receivedPacket.delete(receivedPacket.length - 3, 3);
        } else if (isLastPacket) {
            receivedPacket.delete(0, 5);
            receivedPacket.delete(receivedPacket.length - 2, 2);
        }
        return receivedPacket;
    }

    /**
     * Receive a Packet from S710.
     * 
     * @param bytesToRead
     *            Length of the packet to receive.
     * @throws IOException
     *             If an error occurs.
     */
    public void receivePacket(final int bytesToRead) throws IOException {
        Vm.debug("\nReceivePacket! Bytes to receive = " + bytesToRead);
        int readBytes = 0;
        TimeOut timeOut = new TimeOut(10000);
        byte[] packet = new byte[bytesToRead];
        try {
            readBytes = handleGarbageData(readBytes, packet);
            while (readBytes <= bytesToRead) {
                Vm.debug("readed Bytes = " + readBytes);
                mThread.nap(timeOutAfterReading);
                readBytes += serialPort.read(packet, readBytes, bytesToRead - 1);
                if (readBytes == bytesToRead) {
                    break;
                }
                checkTimeOut(timeOut);
            }
            displayReceivedBytes(new ByteArray(packet));
            checkReadError(readBytes, packet, 0);
            testCRC(packet);
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    /**
     * @param e Exception to handle 
     */
    private void handleIOException(IOException e) {
        serialPort.close();
        Vm.debug(e.getMessage());
        new Zs710Exception(e.getMessage());
    }

    /**
     * @param packet Packet to test.
     * @throws IOException Beschreibung Parameter 
     */
    private void testCRC(byte[] packet) throws IOException {
        if (checkCRC(new ByteArray(packet), getReceivedCRC(new ByteArray(packet)))) {
            receivedPacket = new Packet(packet[0], packet[1], extractBody(packet));
        } else {
            receivedPacket = null;
            throw new IOException("Checksum is wrong!");
        }
    }

    /**
     * @param readBytes Number of bytes to read.
     * @param packet Store readed bytes.
     * @return Number of bytes without garbage.
     * @throws IOException If to much garbarge data was received. 
     */
    private int handleGarbageData(int readBytes, byte[] packet) throws IOException {
        int readedBytes = 0;
        while (readBytes != (byte) 0x5c) {
            readBytes = serialPort.read();
            readedBytes++;
            if (readedBytes > 10) {
                throw new IOException("No data received!");
            }
        }
        readBytes = 1;
        packet[0] = (byte) 0x5c;
        return readBytes;
    }

    /**
     * 
     * @param packet
     *            received packet
     * @return payload of received packet
     */
    private byte[] extractBody(byte[] packet) {
        byte[] tmp;
        if (packet.length > 7) {
            tmp = new byte[packet.length - (5 + 2)];
            Vm.arraycopy(packet, 5, tmp, 0, tmp.length);
        } else {
            tmp = null;
        }
        return tmp;
    }

    /**
     * get Checksum from received Bytes
     * 
     * @return received checksum
     * @param receivedPacket
     *            extract checksum from receivedPacket
     */
    private int getReceivedCRC(ByteArray receivedPacket) {
        return (((receivedPacket.toBytes()[receivedPacket.length - 2] & 0xff) << 8) + (receivedPacket.toBytes()[receivedPacket.length - 1] & 0xff));
    }

    /**
     * Check checksum. Compute with header and body a checksum, this should be
     * the same as the received CRC.
     * 
     * @param receivedBytes
     *            read bytes
     * @param receivedCRC
     *            transmitted checksum
     * @return true or false
     */
    private boolean checkCRC(ByteArray receivedBytes, int receivedCRC) {
        CRC crc = new CRC();
        ByteArray headerAndBody = new ByteArray(receivedBytes.toBytes());
        headerAndBody.delete(headerAndBody.length - 2, 2);
        int lastCRC = 0;
        lastCRC = crc.crcBlock(lastCRC, headerAndBody.toBytes());
        Vm.debug("receive CRC: " + receivedCRC + " computed CRC: " + lastCRC);
        return (lastCRC == receivedCRC);
    }

    /**
     * close IR-Connection
     */
    public void close() {
        serialPort.close();
    }

    /**
     * Used for ProgressBar.
     * @return Bytes to read.
     */
    public int getBytesToRead() {
        return bytesToReadHandle;
    }

    /**
     * Used for ProgressBar.
     * @return Sum of bytes now readed.
     */
    public int getBytesNowReaded() {
        return sumOfReadedBytes;
    }

    /**
     * Used for Output.
     * @return StringWriter for Console.
     */
    public StringWriter getConsoleOutput() {
        return consoleStream;
    }

    /**
     * @param path set path to workouts (save) 
     */
    public void setPathToWorkouts(String path) {
        pathToWorkouts = path;
    }

    /**
     * @return Return all downloaded Workouts 
     */
    public Vector getDownloadedWorkouts() {
        return downloadedWorkouts;
    }

    /**
     * @return Returns the receivedPacket.
     */
    public Packet getReceivedPacket() {
        return receivedPacket;
    }

    /**
     * @param timeOutAfterContinue The timeOutAfterContinue to set.
     */
    public void setTimeOutAfterContinue(int timeOutAfterContinue) {
        this.timeOutAfterContinue = timeOutAfterContinue;
    }

    /**
     * @param timeOutAfterReading The timeOutAfterReading to set.
     */
    public void setTimeOutAfterReading(int timeOutAfterReading) {
        this.timeOutAfterReading = timeOutAfterReading;
    }

    /**
     * @param readedPacket Packet received from hr monitor.
     * @throws IOException If checksum was wrong 
     */
    private void saveCoreDump(ByteArray readedPacket) throws IOException {
        FileOutputStream saveFile = new FileOutputStream("in710.bin");
        saveFile.write(readedPacket.toBytes());
        saveFile.flush();
        saveFile.close();
        throw new IOException("Wrong Checksum!\n " + "Open file 'in710.bin' for information");
    }

    /**
     * @param numberOfPackets Number of packets
     * @param fileBytes Bytes file 
     */
    private void consoleFirstPacketOutput(int numberOfPackets, int fileBytes) {
        Vm.debug("Bytes of all workouts: " + fileBytes);
        consoleStream.write("Bytes to download: " + fileBytes + "\n");
        consoleStream.write("------------------------------------------" + numberOfPackets + "\n");
    }

    /**
     * @param numberOfPackets Number of packets
     * @param packetBytes Bytes packet 
     */
    private void consoleSumOutput(int numberOfPackets, int packetBytes) {
        consoleStream.write("Packet: " + numberOfPackets + "\n");
        consoleStream.write("Bytes this packet: " + packetBytes + "\n");
        consoleStream.write("Sum of readed bytes: " + numberOfPackets + "\n");
        consoleStream.write("------------------------------------------\n");
    }

    /**
     * @param isFirstPacket First Packet?
     * @param tmp Save temp. data
     * @return Bytes readed from ir port
     * @throws IOException If wrong number of bytes received 
     */
    private int getFilesHandleFirstPacketHeader(boolean isFirstPacket, byte[] tmp) throws IOException {
        int readBytes;
        if (isFirstPacket) {
            tmp[0] = (byte) 0x5c;
            readBytes = serialPort.read(tmp, 1, 7);
            readBytes++;
        } else {
            readBytes = serialPort.read(tmp, 0, 8);
        }
        if (readBytes != 8) {
            throw new IOException("Wrong Header received!");
        }
        return readBytes;
    }

    /**
     * @param packetBytes Bytes in this packet.
     * @param tmp Save temp. data.
     * @return Number of packets to receive. 
     */
    private int getFilesSetNumberOfPackets(int packetBytes, byte[] tmp) {
        int numberOfPackets;
        numberOfPackets = tmp[5] & 0x7f;
        Vm.debug("Packetnumber: " + numberOfPackets);
        Vm.debug("Bytes this packet: " + packetBytes);
        return numberOfPackets;
    }

    /**
     * @param timeOut Time out
     * @throws IOException If TimeOut has expired. 
     */
    private void checkTimeOut(TimeOut timeOut) throws IOException {
        if (timeOut.hasExpired()) {
            throw new IOException("Timeout expired!");
        }
    }

    /**
     * @param sendContinue Sending to hr monitor.
     * @throws IOException Send error. 
     */
    private void getFilesSendContinue(byte[] sendContinue) throws IOException {
        int writeBytes = serialPort.writeBytes(sendContinue, 0, sendContinue.length);
        if (writeBytes < 0 || writeBytes < 7) {
            throw new IOException("Sending CONTINUE was not succesful");
        }
    }

    /**
     * @param numberOfPackets Number of packets.
     * @param isFirstPacket Is it first packet?
     * @param readedPacket Packet received from hr monitor.
     * @return Packet without header and trailer.
     */
    private ByteArray getFilesHandleRemoveHeaderAndTrailer(int numberOfPackets, boolean isFirstPacket, ByteArray readedPacket) {
        int fileBytes;
        if (isFirstPacket) {
            fileBytes = (((readedPacket.toBytes()[6] & 0xff) << 8) + (readedPacket.toBytes()[7] & 0xff));
            bytesToReadHandle = fileBytes;
            readedPacket = removeHeaderAndTrailer(readedPacket, true, false);
            isFirstPacket = false;
            consoleFirstPacketOutput(numberOfPackets, fileBytes);
        } else if (numberOfPackets > 0) {
            readedPacket = removeHeaderAndTrailer(readedPacket, false, false);
        } else if (numberOfPackets == 0) {
            readedPacket = removeHeaderAndTrailer(readedPacket, false, true);
        }
        return readedPacket;
    }

    /**
     * @param e
     *            IOException
     */
    private void getFilesHandleIOException(IOException e) {
        ByteArray readedWorkouts;
        sumOfReadedBytes = 100000;
        readedWorkouts = null;
        consoleStream.write(e.getMessage() + "\n");
        consoleStream.write("\nNo workouts received" + "\nRestart downloading ...\n");
        handleIOException(e);
    }

    /**
     * @param tmp
     *            Tmp. data.
     * @param readedPacket
     *            Packet now readed,
     * @throws IOException
     *             Read error.
     */
    private void getFilesReadOnePacket(byte[] tmp, ByteArray readedPacket) throws IOException {
        int readBytes;
        readBytes = serialPort.readBytes(tmp, 0, tmp.length);
        sumOfReadedBytes += readBytes;
        checkReadError(readBytes, tmp, 8);
        Vm.debug("Readed " + (readBytes + 8) + " bytes");
        Vm.debug("Now " + sumOfReadedBytes + " bytes readed.");
        readedPacket.append(tmp, 0, tmp.length);
    }

    /**
     * @param readedPacket
     *            Packet received from hr monitor.
     * @throws IOException
     *             Read error.
     */
    private void getFilesHandleWrongCRC(ByteArray readedPacket) throws IOException {
        if (!checkCRC(readedPacket, getReceivedCRC(readedPacket))) {
            saveCoreDump(readedPacket);
        }
    }

    /**
     * @param readedWorkouts Workout readed
     * @param sendContinue Sending to hr monitor.
     * @param isFirstPacket Is first packet received?
     * @throws IOException Read Error.
     */
    private void getFilesReadPacket(ByteArray readedWorkouts, byte[] sendContinue, boolean isFirstPacket) throws IOException {
        int readBytes;
        int numberOfPackets;
        int packetBytes;
        byte[] tmp;
        ByteArray readedPacket = new ByteArray();
        TimeOut timeOut = new TimeOut(6000);
        tmp = new byte[8];
        readBytes = getFilesHandleFirstPacketHeader(isFirstPacket, tmp);
        sumOfReadedBytes += readBytes;
        packetBytes = ((tmp[3] & 0xff) << 8) + (tmp[4] & 0xff);
        readedPacket.append(tmp, 0, tmp.length);
        packetBytes += 2;
        numberOfPackets = getFilesSetNumberOfPackets(packetBytes, tmp);
        if (packetBytes > 8) {
            tmp = new byte[packetBytes - 8];
        } else {
            tmp = new byte[1032];
        }
        mThread.nap(timeOutAfterReading);
        getFilesReadOnePacket(tmp, readedPacket);
        getFilesHandleWrongCRC(readedPacket);
        readedPacket = getFilesHandleRemoveHeaderAndTrailer(numberOfPackets, isFirstPacket, readedPacket);
        readedWorkouts.append(readedPacket.toBytes(), 0, readedPacket.length);
        checkTimeOut(timeOut);
        consoleSumOutput(numberOfPackets, packetBytes);
        getFilesSendContinue(sendContinue);
        mThread.nap(timeOutAfterContinue);
        Vm.debug("-------------------------");
    }

    /**
     * @return Make packet for continueTransmission 
     */
    private byte[] createContinueTransmissionPacket() {
        ContinueTransmission contTransmission = new ContinueTransmission();
        byte[] sendContinue = contTransmission.getSendPacket().getPacketAsByteArray();
        return sendContinue;
    }
}
