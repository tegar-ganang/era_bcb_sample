package nl.utwente.ewi.hmi.deira.om;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
public class HaptekOutputLink {

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
    private String animationDir;

    private HashMap<String, Integer> voiceMap = new HashMap<String, Integer>();

    static Logger log = Logger.getLogger("deira.om");

    /** < Logging instance */
    private static HashMap<String, String> animationTranslationTable = new HashMap<String, String>();

    private static HashMap<String, String> customAnimations = new HashMap<String, String>();

    private double min_speed = 0.8;

    private double max_speed = 1.4;

    private double min_pitch = 100.0;

    private double max_pitch = 240.0;

    private double min_volume = 60;

    private double max_volume = 200;

    public boolean ttsDone = false;

    {
        initAnimationTables();
    }

    /**
	 * Sets up and initializes a link to an HaptekPlayer. Note: Remember to call
	 * close() if you are done with the OutputLink.
	 * 
	 * @param host
	 *            Hostname (usually localhost or 127.0.0.1).
	 * @param port
	 *            Port to connect to (HaptekPlayer default is 9999).
	 */
    HaptekOutputLink(String host, int port, String animationDir) throws Exception {
        try {
            log.info("Opening Connection to '" + host + ":" + port + "'");
            this.socket = new Socket(host, port);
            log.finer("Creating buffered reader & writers");
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.inputStrings = new ArrayList<String>();
            this.animationDir = animationDir;
            loadVoiceMap();
        } catch (Exception e) {
            log.severe("Error setting up connection: " + e);
            close();
        }
        log.info("Connection made..");
    }

    private void loadVoiceMap() {
        try {
            BufferedReader voiceMapReader = new BufferedReader(new FileReader("hapteklink\\voice_map.ini"));
            String currentline;
            String name;
            int index;
            while (voiceMapReader.ready()) {
                currentline = voiceMapReader.readLine();
                int indexOfEqualSign = currentline.indexOf('=');
                name = currentline.substring(0, indexOfEqualSign).trim();
                index = Integer.parseInt(currentline.substring(indexOfEqualSign + 1).trim());
                voiceMap.put(name, index);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
    }

    private static void initAnimationTables() {
        animationTranslationTable.put("base", "baseanimation.hap");
        animationTranslationTable.put("pity", "pity.hap");
        animationTranslationTable.put("pity_intense", "pity_intense.hap");
        animationTranslationTable.put("anger", "anger.hap");
        animationTranslationTable.put("anger_intense", "anger_intense.hap");
        animationTranslationTable.put("sadness", "sadness.hap");
        animationTranslationTable.put("sadness_intense", "sadness_intense.hap");
        animationTranslationTable.put("surprise", "surprise.hap");
        animationTranslationTable.put("surprise_short", "surpriseshort.hap");
        animationTranslationTable.put("surprise_intense", "surpriseintense.hap");
        animationTranslationTable.put("surprise_intense_short", "surpriseintenseshort.hap");
        animationTranslationTable.put("happy", "hap_happy.hap");
        animationTranslationTable.put("smile_open_1s", "smile_open_1s.hap");
        animationTranslationTable.put("smile_open_3s", "smile_open_3s.hap");
        animationTranslationTable.put("smile_closed_1s", "smile_closed_1s.hap");
        animationTranslationTable.put("smile_closed_3s", "smile_closed_3s.hap");
        animationTranslationTable.put("tension_low", "tension_level1-2.hap");
        animationTranslationTable.put("tension_medium", "tension_level2-4.hap");
        animationTranslationTable.put("tension_high", "tension_level4-5.hap");
        animationTranslationTable.put("look_touser_18s", "looktouser18s.hap");
        animationTranslationTable.put("look_touser_11s", "looktouser11s.hap");
        animationTranslationTable.put("look_left", "lookleft.hap");
        animationTranslationTable.put("look_right", "lookright.hap");
        animationTranslationTable.put("look_up", "lookup.hap");
        animationTranslationTable.put("look_down", "lookdown.hap");
        animationTranslationTable.put("blink_1", "blink.hap");
        animationTranslationTable.put("blink_2", "blink2.hap");
        animationTranslationTable.put("blink_3", "blink3.hap");
        animationTranslationTable.put("headmotion_verylow", "headmotion1.hap");
        animationTranslationTable.put("headmotion_low", "headmotion2.hap");
        animationTranslationTable.put("headmotion_medium", "headmotion3.hap");
        animationTranslationTable.put("headmotion_high", "headmotion4.hap");
        animationTranslationTable.put("glance_up_right", "glance_up_right.hap");
        animationTranslationTable.put("glance_up_middle", "glance_up_middle.hap");
        animationTranslationTable.put("glance_up_left", "glance_up_left.hap");
        animationTranslationTable.put("glance_down_right", "glance_down_right.hap");
        animationTranslationTable.put("glance_down_middle", "glance_down_middle.hap");
        animationTranslationTable.put("glance_down_left", "glance_down_left.hap");
        double base = 0.1;
        String crowdLookString = "[NAC]\\SetSwitch [switch= lookright state= a f0= {0}";
        String crowdLookStringTail = "]\n";
        customAnimations.put("look_right5", MessageFormat.format(crowdLookString, (base * 5.0)) + crowdLookStringTail);
        customAnimations.put("look_right4", MessageFormat.format(crowdLookString, (base * 4.0)) + crowdLookStringTail);
        customAnimations.put("look_right3", MessageFormat.format(crowdLookString, (base * 3.0)) + crowdLookStringTail);
        customAnimations.put("look_right2", MessageFormat.format(crowdLookString, (base * 2.0)) + crowdLookStringTail);
        customAnimations.put("look_right1", MessageFormat.format(crowdLookString, (base * 1.0)) + crowdLookStringTail);
        customAnimations.put("look_center", MessageFormat.format(crowdLookString, (base * 0.0 + 0.001)) + crowdLookStringTail);
        customAnimations.put("look_left1", MessageFormat.format(crowdLookString, (base * -1.0)) + crowdLookStringTail);
        customAnimations.put("look_left2", MessageFormat.format(crowdLookString, (base * -2.0)) + crowdLookStringTail);
        customAnimations.put("look_left3", MessageFormat.format(crowdLookString, (base * -3.0)) + crowdLookStringTail);
        customAnimations.put("look_left4", MessageFormat.format(crowdLookString, (base * -4.0)) + crowdLookStringTail);
        customAnimations.put("look_left5", MessageFormat.format(crowdLookString, (base * -5.0)) + crowdLookStringTail);
        customAnimations.put("glance_stop", "[NAC]\\clock [t= {duration}] \\SetSwitch [switch= lookdown state= off f1= 0.1]\n[NAC]\\clock [t= {duration}] \\SetSwitch [switch= lookleft state= off f1= 0.1]\n");
        customAnimations.put("glance_stop2", "[NAC]\\SetSwitch [switch= lookdown state= off f1= 0.1]\n[NAC]\\SetSwitch [switch= lookup state= off f1= 0.1]\n[NAC]\\SetSwitch [switch= lookleft state= off f1= 0.1]\n");
    }

    /**
	 * Sends basic setup information to HaptekLink.
	 * 
	 * @param string
	 * 
	 * @return
	 */
    public boolean sendSetup(String face, String voice) throws Exception {
        assert this.socket != null;
        boolean success;
        String loadAnimationDir = MessageFormat.format("[NAC]\\PathAdd [name= [{0}]]\n", animationDir);
        log.info("HapOM: Loading base animation: " + loadAnimationDir);
        this.output.write(loadAnimationDir);
        HashMap<String, String> modelMap = new HashMap<String, String>();
        modelMap.put("male", "body_male/body_maleStartup.hap");
        modelMap.put("female", "torso_female/torso_femaleStartup.hap");
        modelMap.put("leno", "body_leno/Lenostartup.hap");
        modelMap.put("head_female", "head_female/standardStartup.hap");
        modelMap.put("torso_female", "torso_female/torso_femaleStartup.hap");
        modelMap.put("torso_male", "torso_male/torso_maleStartup.hap");
        modelMap.put("body_male", "body_male/body_maleStartup.hap");
        modelMap.put("body_female", "body_female/body_femaleStartup.hap");
        String modelFile = "torso_male.htr";
        if (modelMap.containsKey(face)) {
            modelFile = modelMap.get(face);
        }
        int voiceIndex = voiceMap.get(voice);
        String clearSceneCommand = "[NAC]\\Load [file= [/data/standard/emptyScene.hap]]\n";
        log.info("HapOM: Loading model: " + clearSceneCommand);
        this.output.write(clearSceneCommand);
        String loadModelCommand = MessageFormat.format("[NAC]\\Load [file= [{0}]]\n", modelFile);
        log.info("HapOM: Loading model: " + loadModelCommand);
        this.output.write(loadModelCommand);
        String selectVoiceCommand = MessageFormat.format("[NAC]\\SapiTTSLoad [i0= {0}]\n", voiceIndex);
        log.info("HapOM: Selecting voice: " + selectVoiceCommand);
        this.output.write(selectVoiceCommand);
        String baseAnimationScript = "baseAnimation.hap";
        String loadBaseAnimation = MessageFormat.format("[NAC]\\Load [file= [{0}]]\n", baseAnimationScript);
        log.info("HapOM: Loading base animation: " + loadBaseAnimation);
        this.output.write(loadBaseAnimation);
        String background = "stage.jpg";
        String selectBackgroundCommand = MessageFormat.format("[NAC]\\loadbackgrnd [file= {0}]]\n", background);
        log.info("HapOM: Selecting background: " + selectBackgroundCommand);
        this.output.write(selectBackgroundCommand);
        log.info("HapOM: Initializing TTS...\n");
        this.output.write("[HAP]\\Q2 [s0= [Init.] i0=[3]]\n");
        log.info("HapOM: Flushing stream...");
        this.output.flush();
        NotificationChecker nc = queueSingleNotify();
        int totalWait = 0;
        synchronized (this) {
            while (!nc.done && totalWait < 2000) {
                int waitTime = 50;
                wait(waitTime);
                totalWait += waitTime;
            }
        }
        if (nc.done) {
            log.info("Setup successfully sent!");
            return true;
        } else {
            if (nc != null) {
                nc.discard();
            }
            return false;
        }
    }

    /**
	 * Sends a new play order OutputLink.
	 * 
	 * @param text
	 *            The text to be sent over the socket.
	 * @param animations
	 *            ArrayList with animation filenames.
	 * @param speed
	 *            Speech rate
	 * @param pitch
	 *            Pitch of speech
	 * @param volume
	 *            Loudness of speech
	 * @throws Exception
	 */
    public NotificationChecker sendAndWait(String text, ArrayList<String> animations, int speed, int pitch, int volume) throws Exception {
        assert this.socket != null;
        String commandText = "";
        for (String animation : animations) {
            String animationScript = animationTranslationTable.get(animation);
            if (animationScript != null) {
                if (customAnimations.containsKey(animation)) {
                    commandText += customAnimations.get(animation);
                } else {
                    commandText += MessageFormat.format("[NAC]\\Load [file= [{0}]]\n", animationScript);
                }
            } else {
                log.info(MessageFormat.format("HapOM: Couldn`t find animation script for animation [{0}]", animation));
            }
        }
        double realspeed = min_speed + ((double) speed / 10) * (max_speed - min_speed);
        double realpitch = min_pitch + ((double) pitch / 10) * (max_pitch - min_pitch);
        double realvolume = min_volume + ((double) volume / 10) * (max_volume - min_volume);
        String speedString = "" + realspeed;
        String pitchString = "" + realpitch + "Hz";
        String volumeString = "" + realvolume;
        text = MessageFormat.format("<prosody rate=\"{0}\" pitch=\"{1}\" volume=\"{2}\">{3}</prosody>", speedString, pitchString, volumeString, text);
        commandText += MessageFormat.format("[TTS]{0}\n", text);
        log.info("OM: Sending: " + commandText);
        this.output.write(commandText);
        log.info("OM: Flushing stream...");
        this.output.flush();
        return queueSingleNotify();
    }

    public boolean sendAndWaitAnimationsOnly(ArrayList<String> animations) throws Exception {
        return this.sendAndWaitAnimationsOnly(animations, new HashMap<String, Double>());
    }

    public boolean sendAndWaitAnimationsOnly(ArrayList<String> animations, HashMap<String, Double> parameters) throws Exception {
        assert this.socket != null;
        String commandText = "";
        for (String animation : animations) {
            String animationScript = animationTranslationTable.get(animation);
            if (animationScript != null) {
                commandText += MessageFormat.format("[NAC]\\Load [file= [{0}]]\n", animationScript);
            } else if (customAnimations.containsKey(animation)) {
                String command = customAnimations.get(animation);
                command = this.replaceParameters(command, parameters);
                commandText += command;
            } else {
                log.info(MessageFormat.format("HapOM: Couldn`t find animation script for animation [{0}]", animation));
            }
        }
        log.info("OM: Sending: " + commandText);
        this.output.write(commandText);
        log.info("OM: Flushing stream...");
        this.output.flush();
        return true;
    }

    private String replaceParameters(String command, HashMap<String, Double> parameters) {
        boolean foundParameter = true;
        while (foundParameter) {
            int paramBeginIndex = command.indexOf('{');
            int paramEndIndex = command.indexOf('}');
            if (paramBeginIndex == -1 || paramEndIndex == -1) {
                foundParameter = false;
            } else {
                String param = command.substring(paramBeginIndex + 1, paramEndIndex);
                Double paramValue = parameters.get(param);
                if (paramValue != null) {
                    command = command.replaceAll("[{]" + param + "[}]", "" + paramValue);
                }
            }
        }
        return command;
    }

    /**
	 * Wait for a DONE notification from the server.
	 * 
	 * @return True if a success notification was received.
	 * @throws Exception.
	 */
    private boolean waitSingleNotify() throws Exception {
        NotificationChecker nc = queueSingleNotify();
        synchronized (this) {
            while (!nc.done) {
                this.wait(30);
            }
        }
        return true;
    }

    private NotificationChecker queueSingleNotify() throws Exception {
        NotificationChecker nc = new NotificationChecker(this);
        nc.start();
        return nc;
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
	 * Reads all pending notification from the sockets and puts the in the
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
    private void readAllNotifications() throws Exception {
        assert this.socket != null;
        CharBuffer buffer = CharBuffer.allocate(HaptekOutputLink.MAX_READ_LENGTH);
        input.read(buffer.array(), 0, HaptekOutputLink.MAX_READ_LENGTH);
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

    public String getNotification() {
        try {
            readAllNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = inputStrings.get(0).trim();
        inputStrings.remove(0);
        return response;
    }
}
