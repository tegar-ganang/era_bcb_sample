package nl.utwente.ewi.hmi.deira.om;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Establishes and maintains an output link with a VisageLink.
 */
public class VisageOutputLink {

    private static int MAX_READ_LENGTH = 4000;

    /** < Buffer size used for reading out server responses */
    private Socket socket;

    /** < Low-level socket connection to the server */
    private BufferedReader input;

    /** < Input stream attached to the socket */
    private BufferedWriter output;

    /** < Output stream attached to the socket */
    private ArrayList<String> inputStrings;

    /** < Pending input strings read from the socket */
    private static Logger log = Logger.getLogger("deira.om");

    /** < Logging instance */
    private static HashMap<String, String> animationTranslationTable = new HashMap<String, String>();

    {
        initAnimationTranslationTable();
    }

    /**
	 * Sets up and initializes a link to an VisagePlayer. Note: Remember to call
	 * close() if you are done with the OutputLink.
	 * 
	 * @param host
	 *            Hostname (usually localhost or 127.0.0.1).
	 * @param port
	 *            Port to connect to
	 */
    VisageOutputLink(String host, int port) throws Exception {
        try {
            log.info("Opening Connection to '" + host + ":" + port + "'");
            this.socket = new Socket(host, port);
            log.finer("Creating buffered reader & writers");
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.inputStrings = new ArrayList<String>();
        } catch (Exception e) {
            log.severe("Error setting up connection: " + e);
            close();
            throw e;
        }
    }

    private static void initAnimationTranslationTable() {
        animationTranslationTable.put("surprise", "surprise.fba");
        animationTranslationTable.put("surprise_short", "surpriseshort.fba");
        animationTranslationTable.put("smile_open_1s", "smile_open_1s.fba");
        animationTranslationTable.put("smile_open_3s", "smile_open_3s.fba");
        animationTranslationTable.put("smile_closed_1s", "smile_closed_1s.fba");
        animationTranslationTable.put("smile_closed_3s", "smile_closed_3s.fba");
        animationTranslationTable.put("tension_low", "tension_level1-2.fba");
        animationTranslationTable.put("tension_medium", "tension_level2-4.fba");
        animationTranslationTable.put("tension_high", "tension_level4-5.fba");
        animationTranslationTable.put("look_touser_18s", "looktouser18s.fba");
        animationTranslationTable.put("look_touser_11s", "looktouser11s.fba");
        animationTranslationTable.put("look_left", "lookleft.fba");
        animationTranslationTable.put("look_right", "lookright.fba");
        animationTranslationTable.put("look_up", "lookup.fba");
        animationTranslationTable.put("look_down", "lookdown.fba");
        animationTranslationTable.put("blink_1", "blink.fba");
        animationTranslationTable.put("blink_2", "blink2.fba");
        animationTranslationTable.put("blink_3", "blink3.fba");
        animationTranslationTable.put("headmotion_verylow", "headmotion1.fba");
        animationTranslationTable.put("headmotion_low", "headmotion2.fba");
        animationTranslationTable.put("headmotion_medium", "headmotion3.fba");
        animationTranslationTable.put("headmotion_high", "headmotion4.fba");
    }

    /**
	 * Sends basic setup information to VisageLink.
	 * 
	 * @param face
	 *            Full path to the face to use.
	 * @param voice
	 *            Name of the voice to use (MSSAPI Compatible).
	 * @param voiceConfig
	 *            Full path to the configuration file for the voice.
	 * @param animBasePath
	 *            Basepath for loading all animations.
	 * @return
	 */
    public boolean sendSetup(String face, String voice, String voiceConfig, String animBasePath) throws Exception {
        assert this.socket != null;
        String strFinal = "" + "[CFG][FAC]" + face + "\n" + "[CFG][VOC]" + voice + "\n" + "[CFG][LST] \n";
        log.info("OM: Sending Initial Configuration: " + strFinal);
        this.output.write(strFinal);
        log.info("OM: Flushing stream...");
        this.output.flush();
        return waitSingleNotify();
    }

    /**
	 * Sends a new play order (with only animation data) through the OutputLink.
	 * 
	 * @param animations
	 *            ArrayList with animation filenames.
	 */
    public boolean sendAndWaitAnimationsOnly(ArrayList<String> animations) throws Exception {
        assert this.socket != null;
        assert animations.size() > 0;
        String strFinal = generateAnimationCommand(animations);
        log.info("OM: Sending: " + strFinal);
        this.output.write(strFinal);
        log.info("OM: Flushing stream...");
        this.output.flush();
        return true;
    }

    private String generateAnimationCommand(ArrayList<String> animations) {
        String animationCommand = "";
        for (String animation : animations) {
            String animationFile = animationTranslationTable.get(animation);
            if (animationFile != null) {
                animationCommand += MessageFormat.format("[FBA]{0}\n", animationFile);
            } else {
                log.info(MessageFormat.format("VisOM: Couldn't find animation script for animation [{0}]", animation));
            }
        }
        return animationCommand;
    }

    /**
	 * Sends a new play order OutputLink.
	 * 
	 * @param text
	 *            The text to be sent over the socket.
	 * @param animations
	 *            ArrayList with animation filenames.
	 * @param speed
	 *            Speech rate (absolute values or relative ones with +/- sign
	 *            preprended).
	 * @param pitch
	 *            Pitch of speech (absolute or relative).
	 * @param volume
	 *            Loudness of speech (also absolute or relative).
	 * @throws Exception
	 */
    public boolean sendAndWait(String text, ArrayList<String> animations, int speed, int pitch, int volume) throws Exception {
        assert this.socket != null;
        String strFinal = MessageFormat.format("[CTL][SPD]{0}\n[CTL][PIT]{1}\n[CTL][VOL]{2}\n", speed, pitch, volume);
        strFinal += generateAnimationCommand(animations);
        strFinal += MessageFormat.format("[TTS]{0}\n", text);
        log.info("OM: Sending: " + strFinal);
        this.output.write(strFinal);
        log.info("OM: Flushing stream...");
        this.output.flush();
        return waitSingleNotify();
    }

    /**
	 * Wait for a DONE notification from the server.
	 * 
	 * @return True if a success notification was received.
	 * @throws Exception.
	 */
    public boolean waitSingleNotify() throws Exception {
        log.info("OM: Attempting to read notification from server");
        if (inputStrings.isEmpty()) {
            updateFromSocket();
            if (inputStrings.isEmpty()) {
                log.severe("OM: No notifications pending on socket, while one was requested and indicated as available. This should never happen!");
            }
        }
        String response = inputStrings.get(0).trim();
        inputStrings.remove(0);
        log.info("OM: Notification read and trimmed: '" + response + "'");
        return response.equals("DONE");
    }

    /**
	 * Retrieves whether the OutputLink has pending notifications to be read.
	 * 
	 * @return True if there are pending notifications, False otherwise.
	 */
    public boolean hasNotifications() throws Exception {
        try {
            return (this.input.ready() || !inputStrings.isEmpty());
        } catch (Exception e) {
            log.severe("Error determining notification availability: " + e);
            throw e;
        }
    }

    /**
	 * Reads all pending XML notification from the sockets and puts the in the
	 * inputStrings list of strings waiting to be parsed.
	 * 
	 * NOTE: Theoretically this approach won't work if there is too much data in
	 * the buffer (exceeding MAX_READ_LENGTH characters) In such cases the input
	 * is actually cut off and we'd need multiple reads. The chance of this
	 * occurring is very low, so it is not currently handled.
	 * 
	 * @throws Exception
	 *             On socket read error.
	 */
    private void updateFromSocket() throws Exception {
        assert this.socket != null;
        CharBuffer buffer = CharBuffer.allocate(VisageOutputLink.MAX_READ_LENGTH);
        input.read(buffer.array(), 0, VisageOutputLink.MAX_READ_LENGTH);
        String string = "";
        while (buffer.remaining() > 0) {
            char c = buffer.get();
            if (c == '\n') {
                inputStrings.add(new String(string));
                string = "";
            } else {
                string = string + c;
            }
        }
    }

    /**
	 * Closes all connections.
	 * 
	 * Note: Calling this method after all transactions are completed is a
	 * necessity!
	 */
    public void close() throws Exception {
        log.info("Closing connection");
        if (this.output != null) {
            this.output.close();
            this.output = null;
        }
        if (this.input != null) {
            this.input.close();
            this.input = null;
        }
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
    }
}
