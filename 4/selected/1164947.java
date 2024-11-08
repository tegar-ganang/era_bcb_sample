package com.audio;

import ade.exceptions.ADETimeoutException;
import ade.exceptions.ADECallException;
import ade.*;
import ade.ADEGlobals.ServerState;
import ade.ADEPreferences.*;
import com.interfaces.*;
import static utilities.Util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import javax.sound.sampled.*;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.spi.*;
import java.security.*;

/**
 * Server to record an audio stream from a mic, normally to ship it to an
 * instance of {@link com.audio.AudioPlayerServerImpl AudioPlayerServerImpl}.
 */
public class AudioRecorderServerImpl extends ADEServerImpl implements AudioRecorderServer {

    private static String prg = null;

    private static String type = null;

    private static boolean verbose = true;

    private static boolean debug = true;

    private String playerServer = "AudioPlayerServerImpl";

    protected static boolean usePlayer = true;

    protected boolean gotPlayer = false;

    public Object aps;

    public static float DEF_AUDIO_SAMPLE_RATE = 16000;

    public static int DEF_AUDIO_SAMPLE_BITS = 16;

    public static int DEF_AUDIO_CHANNELS = 1;

    public static boolean DEF_AUDIO_SIGNED = true;

    public static boolean DEF_AUDIO_BIGENDIAN = false;

    public static int DEF_AUDIO_BYTES = 8000;

    private float sampRate;

    private int sampBits;

    private int numChannels;

    private boolean isSigned;

    private boolean bigendian;

    private int bytesWaitFor;

    private long sampleSleep;

    private static int recDev = 0;

    private static int playDev = 0;

    private TargetDataLine inLine = null;

    private SourceDataLine outLine = null;

    private static boolean playAudio = false;

    private AudioInputStream ais = null;

    private recordLoop recorder = null;

    private writeLoop writer = null;

    private playLoop player = null;

    private AudioFormat audioFormat = null;

    private static File outFile = null;

    private AudioFileFormat.Type outFileType = AudioFileFormat.Type.WAVE;

    private AudioInputStream fin = null;

    private PipedInputStream pin = null;

    private PipedOutputStream pout = null;

    private byte[] poutData;

    private static boolean shouldSave = false;

    private static String saveName;

    private byte[] abytes;

    private int audioread;

    private boolean newbytes = false;

    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    protected void clientConnectReact(String user) {
        System.out.println(myID + ": got connection from " + user + "!");
        return;
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     */
    protected boolean clientDownReact(String user) {
        System.out.println(myID + ": lost connection with " + user + "!");
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed). 
     */
    protected void serverDownReact(String s) {
        System.out.println(prg + ": reacting to down " + s + "...");
        if (s.indexOf("AudioPlayerServer") >= 0) {
            gotPlayer = false;
        }
        return;
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>NOTE:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object.
     * @param s the ID of the {@link ade.ADEServer ADEServer} that connected
     * @param ref the pseudo-reference for the requested server */
    protected void serverConnectReact(String s, Object ref) {
        System.out.println(myID + ": reacting to connecting " + s + " server...");
        if (s.indexOf("AudioPlayerServer") >= 0) {
            gotPlayer = true;
        }
        return;
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    protected void localshutdown() {
        System.out.println(myID + ": shutting down...");
        recorder.halt();
        writer.halt();
        if (shouldSave) {
            try {
                setADEServerLogging(false);
            } catch (Exception e) {
            }
        }
        System.out.println("done.");
    }

    /**
     * Provide additional information for usage...
     */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server-specific options:\n\n");
        sb.append("  -aps name	    <use named server as target>\n");
        sb.append("  -savename S    <save frames, using S as filename base>\n");
        sb.append("  -outfile name  <save audio data in named file>\n");
        sb.append("  -noplayer	    <don't send audio to a remote player>\n");
        sb.append("  -playaudio	    <play audio locally as well>\n");
        sb.append("  -recdev dev    <use audio device #dev for recording>\n");
        sb.append("  -playdev dev   <use audio device #dev for playing>\n");
        return sb.toString();
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-aps")) {
                playerServer = args[++i];
                found = true;
            } else if (args[i].equalsIgnoreCase("-savename")) {
                shouldSave = true;
                saveName = args[++i];
                found = true;
            } else if (args[i].equalsIgnoreCase("-outfile")) {
                outFile = new File(args[++i]);
                found = true;
            } else if (args[i].equalsIgnoreCase("-noplayer")) {
                usePlayer = false;
                found = true;
            } else if (args[i].equalsIgnoreCase("-playaudio")) {
                playAudio = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-recdev")) {
                int rd;
                try {
                    rd = Integer.parseInt(args[i + 1]);
                    i++;
                    recDev = rd;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": recdev " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-playdev")) {
                int pd;
                try {
                    pd = Integer.parseInt(args[i + 1]);
                    i++;
                    playDev = pd;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": playdev " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                }
                found = true;
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;
            }
        }
        return found;
    }

    /**
     * This method will be activated whenever a client that has called
     * the requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper). If it returns
     * true, the client's connection is removed. 
     */
    protected boolean attemptRecoveryClientDown(String user) {
        return false;
    }

    /**
     * The thread that grabs the audio from the mic.
     */
    class recordLoop extends Thread {

        private boolean shouldRecord = true;

        public void run() {
            String ts = null;
            long millis;
            String savename;
            FileOutputStream file;
            boolean myLocalLogging = false;
            if (shouldSave) {
                try {
                    setADEServerLogging(true);
                    myLocalLogging = true;
                } catch (ADETimeoutException ate) {
                    System.err.println(prg + " ATE: " + ate);
                } catch (ADECallException ace) {
                    System.err.println(prg + " ACE: " + ace);
                } catch (AccessControlException access) {
                    System.err.println(prg + " ACCESS: " + access);
                } catch (Exception e) {
                    System.err.println(prg + ": unable to start logging");
                    System.err.println(prg + ": " + e);
                }
            }
            while (shouldRecord) {
                try {
                    while ((ais.available() < bytesWaitFor) && shouldRecord) {
                        Sleep(sampleSleep);
                    }
                    synchronized (ais) {
                        audioread = ais.read(abytes, 0, bytesWaitFor);
                        newbytes = true;
                    }
                    if (playAudio) outLine.write(abytes, 0, audioread);
                    if (shouldSave) {
                        try {
                            ts = logIt("Audio frame");
                        } catch (IOException ioe2) {
                            System.err.println(prg + ": error logging");
                        }
                        savename = "logs/ar_" + saveName + "_" + ts + ".raw";
                        System.out.println("saving " + savename);
                        file = new FileOutputStream(savename);
                        file.write(abytes, 0, audioread);
                        file.close();
                    }
                    if (outFile != null) pout.write(abytes, 0, audioread);
                } catch (IOException ioe) {
                    System.out.println(prg + ": Error accessing ais: " + ioe);
                }
            }
        }

        public void halt() {
            shouldRecord = false;
            try {
                pout.flush();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * The thread that writes the stream to a file, if requested.
     */
    class writeLoop extends Thread {

        public void run() {
            try {
                AudioSystem.write(fin, outFileType, outFile);
            } catch (IOException ioe) {
                System.err.println(prg + ": error writing " + outFile);
                System.err.println(prg + ": " + ioe);
            }
        }

        public void halt() {
            try {
                while (fin.available() > 0) ;
                pout.close();
                pin.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * The thread that passes the buffers along to the player, if requested.
     */
    class playLoop extends Thread {

        private boolean shouldPlay = true;

        public void run() {
            while (shouldPlay) {
                synchronized (ais) {
                    if (newbytes && gotPlayer) {
                        newbytes = false;
                        try {
                            call(aps, "playAudio", abytes, audioread);
                        } catch (ADECallException ace) {
                            System.err.println(prg + ": remote play");
                            System.err.println(prg + ": " + ace);
                        }
                    }
                }
                Sleep(sampleSleep);
            }
        }

        public void halt() {
            shouldPlay = false;
        }
    }

    /** The server is always ready to provide its service after it has come up */
    protected boolean localServicesReady() {
        return true;
    }

    /**
     * Construct the <code>AudioRecorderServerImpl</code>.  Have to do a lot
     * of initializing of audio devices, etc.
     */
    public AudioRecorderServerImpl() throws RemoteException {
        super();
        int totBps;
        prg = new String("AudioRecorderServerImpl");
        type = new String("AudioRecorderServer");
        sampRate = DEF_AUDIO_SAMPLE_RATE;
        sampBits = DEF_AUDIO_SAMPLE_BITS;
        numChannels = DEF_AUDIO_CHANNELS;
        isSigned = DEF_AUDIO_SIGNED;
        bigendian = DEF_AUDIO_BIGENDIAN;
        bytesWaitFor = DEF_AUDIO_BYTES;
        bytesWaitFor = 4000;
        totBps = (int) ((sampBits / 8) * numChannels * sampRate);
        sampleSleep = (1000 / (totBps / bytesWaitFor)) * 2 / 4;
        audioFormat = new AudioFormat(sampRate, sampBits, numChannels, isSigned, bigendian);
        abytes = new byte[bytesWaitFor];
        System.out.println("audioFormat frame size: " + audioFormat.getFrameSize());
        Info info = new Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println(prg + ": error, line is not supported by AudioSystem");
        }
        Mixer.Info[] mixInf = AudioSystem.getMixerInfo();
        for (int i = 0; i < Array.getLength(mixInf); i++) {
            System.out.println("*****MIXER INFO*****: " + mixInf[i].toString());
        }
        Mixer myMixer = AudioSystem.getMixer(mixInf[recDev]);
        try {
            inLine = (TargetDataLine) myMixer.getLine(info);
            inLine.open(audioFormat);
            System.out.println(prg + ": inLine open? " + inLine.isOpen());
            inLine.start();
            ais = new AudioInputStream(inLine);
            System.out.println(prg + ": ais = null? " + (ais == null));
        } catch (Exception e) {
            System.err.println(prg + ": error opening audio input dataline");
            System.err.println(e);
        }
        if (playAudio) {
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println(prg + ": error, line is not supported by AudioSystem");
            }
            myMixer = AudioSystem.getMixer(mixInf[playDev]);
            try {
                outLine = (SourceDataLine) myMixer.getLine(info);
                outLine.open(audioFormat);
                System.out.println(prg + ": outLine open? " + outLine.isOpen());
                outLine.start();
            } catch (LineUnavailableException lue) {
                System.err.println(prg + ": error opening audio output dataline");
                System.err.println(lue);
            }
        }
        pin = new PipedInputStream();
        try {
            pout = new PipedOutputStream(pin);
        } catch (IOException ioe) {
            System.err.println("Error associating pout with pin: " + ioe);
            System.exit(-1);
        }
        poutData = new byte[bytesWaitFor];
        fin = new AudioInputStream(pin, audioFormat, AudioSystem.NOT_SPECIFIED);
        System.out.println("Ready to start record loop.");
        (recorder = new recordLoop()).start();
        if (usePlayer) {
            aps = getClient(playerServer);
            (player = new playLoop()).start();
        }
        if (outFile != null) {
            (writer = new writeLoop()).start();
        }
        dbg = 3;
    }

    /**
     * <code>main</code> passes the arguments up to the ADEServerImpl
     * parent.  The parent does some magic and gets the system going.
     */
    public static void main(String[] args) throws Exception {
        ADEServerImpl.main(args);
    }
}
