package it.bz.distefano.wavecom;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import sun.net.ProgressSource;

public class WavecomClient {

    private boolean debug;

    private Connector s;

    private InputStream is;

    private OutputStream os;

    private int port = 2000;

    private String host = "192.168.192.4";

    private String com = null;

    private boolean buffered = true;

    public static final int SIZE_CMD = 8;

    public static final int SIZE_SIZE = 10;

    public static final int SIZE_AT = 128;

    public static final int SIZE_CALLNUMBER = 30;

    private static final String CMD_SEND_FILE = "SEND_FILE_CMD";

    private static final String CMD_RECEIVE = "RECEIVE_DATA";

    private IOListener listener;

    public WavecomClient() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                try {
                    disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public WavecomClient(String host, int port) {
        this();
        this.host = host;
        this.port = port;
    }

    public WavecomClient(String com) {
        this();
        this.com = com;
    }

    public void playStream(File file) throws IOException {
        playAndWait();
        streamSound(file);
    }

    public void uploadAndPlay(File file) throws IOException {
        uploadSound(file);
        play();
    }

    public void connect() throws UnknownHostException, IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        if (s == null) {
            if (com != null) s = new SerialConnector(com); else s = new SocketConnector(host, port);
            os = s.getOutputStream();
            is = s.getInputStream();
            if (buffered) {
                os = new BufferedOutputStream(os);
                is = new BufferedInputStream(is);
            }
        }
    }

    public void disconnect() throws IOException {
        if (s != null) {
            s.close();
            if (debug) System.out.println("Disconnected");
        }
        s = null;
        is = null;
        os = null;
    }

    /**
	 * close all other connections, resets all buffers and settings
	 * 
	 * @throws IOException
	 */
    public void reset() throws IOException {
        sendCmd("RESET");
        disconnect();
    }

    public void ping() throws IOException {
        sendCmd("PING");
    }

    /**
	 * stops the execution of the server application on fastrack device.
	 * 
	 * @throws IOException
	 */
    public void shutdown() throws IOException {
        sendCmd("EXIT");
        s = null;
        is = null;
        os = null;
    }

    public void status() throws IOException {
        sendCmd("STATUS");
    }

    /**
	 * play the uploaded stream if available. If called while already playing,
	 * stops the play
	 * 
	 * @throws IOException
	 */
    public void play() throws IOException {
        sendCmd("PLAY");
    }

    /**
	 * play the uploaded stream if available and waits up to the end of the
	 * play.
	 * 
	 * @throws IOException
	 */
    public void playSync() throws IOException {
        sendCmd("PLAYS");
        checkResponse();
    }

    /**
	 * activate the audio device and waits for a stream. If called while already
	 * playing, stops the play
	 * 
	 * @throws IOException
	 */
    public void playAndWait() throws IOException {
        sendCmd("PLAYW");
    }

    public void setSoundDeviceSpeaker() throws IOException {
        sendCmd("DESTSPK");
    }

    public void setSoundDeviceGSM() throws IOException {
        sendCmd("DESTGSM");
    }

    public void boost() throws IOException {
        sendCmd("BOOST");
    }

    public void startWatchDog(int sec) throws IOException {
        sendCmd("STARTWD");
        sendSize(sec);
    }

    public void stopWatchDog() throws IOException {
        sendCmd("STOPWD");
    }

    public void listenDTMF() throws IOException {
        sendCmd("DTMFL");
    }

    public void setBlankDuration(int duration) throws IOException {
        sendCmd("DTMFD");
        sendSize(duration);
    }

    public void stopDTMF() throws IOException {
        sendCmd("DTMFS");
    }

    public void hangUp() throws IOException {
        sendCmd("HANGUP");
    }

    public void codecPCM() throws IOException {
        sendCmd("PCM");
    }

    public void codecAMR() throws IOException {
        sendCmd("AMR");
    }

    public void codecAMRWB() throws IOException {
        sendCmd("AMRWB");
    }

    /**
	 * make the voice call without to wait the answer or the fail. the call
	 * status can be checked using {@link #lastResult()} if a stream was
	 * prevously uploaded, it is played automatically at the answer
	 * 
	 * @param number
	 * @throws IOException
	 */
    public void call(String number) throws IOException {
        sendCmd("CALL");
        send(number, SIZE_CALLNUMBER);
    }

    /**
	 * make the voice call and waits the answer or the call fail then a stream
	 * can be played using {@link #playStream(File)}
	 * 
	 * @param number
	 * @throws IOException
	 */
    public void callSync(String number) throws IOException {
        sendCmd("CALLS");
        send(number, SIZE_CALLNUMBER);
        checkResponse();
    }

    public void emergencyCall(String number, File file) throws IOException {
        sendCmd("EMCALL");
        send(number, SIZE_CALLNUMBER);
        send(file);
    }

    public String log() throws IOException {
        return receiveData("LOG");
    }

    public String lastResult() throws IOException {
        return receiveData("RESP");
    }

    public void debug() throws IOException {
        sendCmd("DEBUG");
    }

    public void at(String cmd) throws IOException {
        sendCmd("AT");
        send(cmd, SIZE_AT);
    }

    /**
	 * set the number of repetitions (default 1). If repetitions is 0 the
	 * uploaded stream is replayed forever.
	 * 
	 * @param repetitions
	 * @throws IOException
	 */
    public void repetitions(int repetitions) throws IOException {
        sendCmd("REP");
        sendSize(repetitions);
    }

    public void uploadSound(File file) throws IOException {
        sendCmd("STREAM");
        send(file);
    }

    public void streamSound(File file) throws IOException {
        sendCmd("STREAM");
        long size = -file.length();
        sendSize(size);
        send(new FileInputStream(file), size);
    }

    public boolean isConnected() {
        return s != null;
    }

    private byte[] receive(int size) throws IOException {
        byte[] buffer = new byte[size];
        int tot = 0;
        ioProgress(CMD_RECEIVE, tot, size, ProgressSource.State.NEW);
        while (true) {
            int read = is.read(buffer, tot, size - tot);
            tot += read;
            ioProgress(CMD_RECEIVE, tot, size, ProgressSource.State.UPDATE);
            if (read < 0 || tot == size) break;
        }
        ioProgress(CMD_RECEIVE, tot, size, ProgressSource.State.DELETE);
        return buffer;
    }

    private int receiveSize() throws IOException {
        String size = new String(receive(SIZE_SIZE));
        int idx = size.indexOf('\0');
        if (idx > 0) size = size.substring(0, idx);
        return Integer.valueOf(size);
    }

    private String receiveData(String cmd) throws IOException {
        sendCmd(cmd);
        int size = receiveSize();
        return new String(receive(size));
    }

    private void sendSize(long size) throws IOException {
        send(String.valueOf(size), SIZE_SIZE);
    }

    private void send(File file) throws IOException {
        long size = file.length();
        sendSize(size);
        send(new FileInputStream(file), size);
    }

    private void checkResponse() throws IOException {
        if (debug) System.out.print("Response: ");
        int resp = is.read();
        if (debug) System.out.println(((char) resp));
        if (resp != '0') {
            throw new IllegalStateException("Command failed");
        }
    }

    private void send(InputStream in, long size) throws IOException {
        in = new BufferedInputStream(in);
        byte[] buffer = new byte[1024 * 100];
        long tot = 0;
        ioProgress(CMD_SEND_FILE, tot, size, ProgressSource.State.NEW);
        while (true) {
            int read = in.read(buffer);
            if (read < 0) break;
            os.write(buffer, 0, read);
            tot += read;
            ioProgress(CMD_SEND_FILE, tot, size, ProgressSource.State.UPDATE);
        }
        in.close();
        os.flush();
        ioProgress(CMD_SEND_FILE, tot, size, ProgressSource.State.DELETE);
        if (debug) System.out.println("Sent " + tot + "/" + size + " bytes");
        checkResponse();
    }

    private void send(String cmd, int size) throws IOException {
        int len = cmd.getBytes().length;
        byte[] token = new byte[size];
        System.arraycopy(cmd.getBytes(), 0, token, 0, len);
        for (int i = len; i < size; i++) {
            token[i] = 0;
        }
        os.write(token);
        os.flush();
        if (debug) System.out.println("Sent " + cmd);
        checkResponse();
    }

    private void sendCmd(String cmd) throws IOException {
        send(cmd, SIZE_CMD);
    }

    protected void ioProgress(String cmd, long done, long size, ProgressSource.State state) {
        if (listener != null) {
            if (state == ProgressSource.State.UPDATE) {
                listener.ioProgress(cmd, done, size);
            } else if (state == ProgressSource.State.NEW) {
                listener.ioStarted(cmd, done, size);
            } else if (state == ProgressSource.State.DELETE) {
                listener.ioFinished(cmd, done, size);
            }
        }
    }

    public void setListener(IOListener listener) {
        this.listener = listener;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
