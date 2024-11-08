package mathgame.common;

import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.IOException;

public class LevelData {

    public static class ObjectEntry {

        public int layer;

        public int type;

        public int x;

        public int y;

        public String imageRelativeFilename;

        public boolean loadTranslucent;
    }

    public static class DoorEntry extends ObjectEntry {

        public String nextLevelRelativeFilename;
    }

    public static class LegacyQuestionEntry extends ObjectEntry {

        public String question;

        public int correctAnswerIndex;

        public String[] answers = new String[0];
    }

    private static final String LEVELFILE_HEADER = "## Math-game level ##";

    /** The width of the level (in pixels). */
    public int width;

    /** The height of the level (in pixels). */
    public int height;

    /** The time (in seconds) that the player has available to finish the level. */
    public int timeLimit;

    /** The number of questions that has to be answered correctly to get the key. */
    public int keyLimit;

    /** The number of questions that has to be answered correctly to get the force. */
    public int forceLimit = -1;

    /** The number of questions that has to be answered correctly to get the force
	after the force limit has been reached. */
    public int forceInterval = -1;

    /** A relative path to the image that will paint the background of the level
	(will be diplayed repeated times to fill the background). */
    public String backgroundImage;

    /** A relative path to the MIDI-file containing the music for this level. */
    public String musicFilename;

    /** The names of the subjects which are to be chosen among when selecting the
	questions appearing in the question boxes for this level */
    public String[] subjectNames = new String[0];

    /** The list of weights for the subjects in this level. This list must be equal
	in size to <code>subjectNames</code> */
    public int[] subjectWeights = new int[0];

    /** The requested level of difficulty for the questions selected in this level. */
    public int difficultyLevel;

    /** The amount of 'random boxes' that should contain a question, in percent. */
    public float questionBoxPercentage;

    /** The amount of 'random boxes' that should give points, in percent. */
    public float pointsBoxPercentage;

    /** The amount of 'random boxes' that should contain an extra life, in percent. */
    public float heartBoxPercentage;

    /** The player's position x-wise. */
    public int playerx;

    /** The player's position y-wise. */
    public int playery;

    /** The list of object entries that makes up the map. */
    public ObjectEntry[] entries = new ObjectEntry[0];

    /** Parses level data from a BufferedReader and creates a LevelData
	object containing all level information. */
    public static LevelData loadLevelData(BufferedReader in) throws BadLevelDataException {
        int lineNumber = 0;
        int lastSubjectRelatedLineNumber = 0;
        try {
            String header = in.readLine();
            ++lineNumber;
            if (!header.trim().equals(LEVELFILE_HEADER)) throw new BadLevelDataException(lineNumber);
            LevelData ldata = new LevelData();
            LinkedList<ObjectEntry> entries = new LinkedList<ObjectEntry>();
            while (in.ready()) {
                String currentLine = in.readLine().trim();
                ++lineNumber;
                String[] tokens = currentLine.split("\\s");
                if (currentLine.equals("")) continue; else if (tokens.length > 9 && tokens[0].equalsIgnoreCase("layer:")) {
                    int type = Integer.parseInt(tokens[3]);
                    ObjectEntry newEntry;
                    if (tokens.length == 10 && type != SpriteType.DOOR && type != SpriteType.CLOSED_DOOR) newEntry = new ObjectEntry(); else if (tokens.length == 11 && (type == SpriteType.DOOR || type == SpriteType.CLOSED_DOOR)) {
                        DoorEntry de = new DoorEntry();
                        de.nextLevelRelativeFilename = tokens[10];
                        newEntry = de;
                    } else if (tokens.length >= 12 && type == SpriteType.BOX_QUESTION) {
                        LegacyQuestionEntry lqe = new LegacyQuestionEntry();
                        lqe.question = tokens[10].replace('_', ' ');
                        lqe.correctAnswerIndex = Integer.parseInt(tokens[11]);
                        lqe.answers = new String[tokens.length - 12];
                        for (int i = 12; i < tokens.length; i++) lqe.answers[i - 12] = tokens[i].replace('_', ' ');
                        newEntry = lqe;
                    } else throw new BadLevelDataException(lineNumber);
                    newEntry.layer = Integer.parseInt(tokens[1]);
                    newEntry.type = type;
                    newEntry.x = Integer.parseInt(tokens[5]);
                    newEntry.y = Integer.parseInt(tokens[7]);
                    newEntry.imageRelativeFilename = tokens[9];
                    if (tokens[9].trim().equals("plattform/spikes.png")) newEntry.loadTranslucent = true; else newEntry.loadTranslucent = false;
                    entries.add(newEntry);
                } else if (tokens.length == 4 && tokens[0].equalsIgnoreCase("width:")) {
                    ldata.width = Integer.parseInt(tokens[1]);
                    ldata.height = Integer.parseInt(tokens[3]);
                } else if (tokens.length >= 1 && tokens[0].equalsIgnoreCase("mathematics:")) {
                    String[] subjects = new String[tokens.length - 1];
                    for (int i = 0; i < subjects.length; i++) subjects[i] = tokens[i + 1];
                    ldata.subjectNames = subjects;
                    lastSubjectRelatedLineNumber = lineNumber;
                } else if (tokens.length == 5 && tokens[0].equalsIgnoreCase("player:")) {
                    ldata.playerx = Integer.parseInt(tokens[2]);
                    ldata.playery = Integer.parseInt(tokens[4]);
                } else if (tokens.length == 3 && (tokens[0].equalsIgnoreCase("time") && tokens[1].equalsIgnoreCase("limit:"))) {
                    ldata.timeLimit = Integer.parseInt(tokens[2]);
                } else if (tokens.length == 3 && (tokens[0].equalsIgnoreCase("game") && tokens[1].equalsIgnoreCase("tune:"))) {
                    ldata.musicFilename = tokens[2];
                } else if (tokens.length == 3 && (tokens[0].equalsIgnoreCase("key") && tokens[1].equalsIgnoreCase("limit:"))) {
                    ldata.keyLimit = Integer.parseInt(tokens[2]);
                } else if (tokens.length >= 2 && (tokens[0].equalsIgnoreCase("subject") && tokens[1].equalsIgnoreCase("weights:"))) {
                    int[] subjWeights = new int[tokens.length - 2];
                    for (int i = 0; i < subjWeights.length; i++) {
                        subjWeights[i] = Integer.parseInt(tokens[i + 2]);
                    }
                    ldata.subjectWeights = subjWeights;
                    lastSubjectRelatedLineNumber = lineNumber;
                } else if (tokens.length == 4 && (tokens[0].equalsIgnoreCase("degree") && tokens[1].equalsIgnoreCase("of") && tokens[2].equalsIgnoreCase("difficulty:"))) {
                    ldata.difficultyLevel = Integer.parseInt(tokens[3]);
                } else if (tokens.length == 6 && (tokens[0].equalsIgnoreCase("distribution") && tokens[1].equalsIgnoreCase("of") && tokens[2].equalsIgnoreCase("boxes:"))) {
                    int[] parts = new int[3];
                    ldata.questionBoxPercentage = Float.parseFloat(tokens[3]);
                    ldata.pointsBoxPercentage = Float.parseFloat(tokens[4]);
                    ldata.heartBoxPercentage = Float.parseFloat(tokens[5]);
                    if ((ldata.questionBoxPercentage + ldata.pointsBoxPercentage + ldata.heartBoxPercentage) != 100.0) throw new BadLevelDataException(lineNumber);
                } else if (tokens.length == 3 && (tokens[0].equalsIgnoreCase("force") && tokens[1].equalsIgnoreCase("limit:"))) {
                    ldata.forceLimit = Integer.parseInt(tokens[2]);
                } else if (tokens.length == 3 && (tokens[0].equalsIgnoreCase("force") && tokens[1].equalsIgnoreCase("interval:"))) {
                    ldata.forceInterval = Integer.parseInt(tokens[2]);
                } else if (tokens.length == 3 && (tokens[0].equalsIgnoreCase("background") && tokens[1].equalsIgnoreCase("image:"))) {
                    ldata.backgroundImage = tokens[2];
                } else {
                    System.err.println("tokens.length = " + tokens.length);
                    for (String s : tokens) System.err.println("  " + s);
                    throw new BadLevelDataException(lineNumber);
                }
            }
            if ((ldata.subjectWeights != null && ldata.subjectWeights != null) && (ldata.subjectWeights.length != ldata.subjectWeights.length)) throw new BadLevelDataException(lastSubjectRelatedLineNumber);
            ldata.entries = entries.toArray(new ObjectEntry[entries.size()]);
            return ldata;
        } catch (NumberFormatException nfe) {
            throw new BadLevelDataException(lineNumber);
        } catch (IOException nfe) {
            throw new BadLevelDataException(lineNumber);
        }
    }

    public static class BadLevelDataException extends Exception {

        private int lineNumber;

        public BadLevelDataException(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }
}
