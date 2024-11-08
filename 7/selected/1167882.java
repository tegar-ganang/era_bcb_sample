package jitt64;

import java.io.File;

/**
 * A JITT64 Song representation
 *  
 * @author ice
 */
public class Song {

    /** Chip of type 6581 */
    public static final String CHIP_6581 = "6581";

    /** Chip of type 8580 */
    public static final String CHIP_8580 = "8580";

    /** Chip of type ANY */
    public static final String CHIP_ANY = "ANY";

    /** Instrument AD */
    public static final int INSTR_AD = 0;

    /** Instrument SR */
    public static final int INSTR_SR = 1;

    /** Instrument Wave */
    public static final int INSTR_WAVE = 2;

    /** Instrument Freq. */
    public static final int INSTR_FREQ = 3;

    /** Instrument Pulse */
    public static final int INSTR_PULSE = 4;

    /** Instrument Filter */
    public static final int INSTR_FILTER = 5;

    /** Instrument Res. */
    public static final int INSTR_RES = 6;

    /** Instrument Type */
    public static final int INSTR_TYPE = 7;

    /** Command set tempo */
    public static final int CMD_TEMPO = 0;

    /** Command set AD */
    public static final int CMD_AD = 1;

    /** Command set SR */
    public static final int CMD_SR = 2;

    /** Command set Volume */
    public static final int CMD_VOLUME = 3;

    /** Command set Arpeggio */
    public static final int CMD_ARP = 4;

    /** Command set Portamento Up */
    public static final int CMD_PUP = 5;

    /** Command set Portamento Dn */
    public static final int CMD_PDN = 6;

    /** Command set Toneportamento */
    public static final int CMD_TPO = 7;

    /** Command set Vibrato */
    public static final int CMD_VIB = 8;

    /** Command set Slide Up */
    public static final int CMD_SUP = 9;

    /** Command set Slide Dn */
    public static final int CMD_SDN = 10;

    /** Command set Fade Out */
    public static final int CMD_FOUT = 11;

    /** Command set Filter Type */
    public static final int CMD_FTY = 12;

    /** Command set Filter Resonance */
    public static final int CMD_FRS = 13;

    /** Command set Filter Cut Off */
    public static final int CMD_FCU = 14;

    /** Command set Gatye SR */
    public static final int CMD_GSR = 15;

    /** internal offset for instrument operation */
    private static final int OFFSET = 555;

    /** number of tunes that the player supports  */
    public static final int NUM_TUNES = 256;

    /** Instruments to use */
    protected Instrument[] instruments;

    /** Tracks used for each tune and voices */
    protected Track[][] tracks;

    /** Patterns used for all the tunes */
    protected Pattern[] patterns;

    /** Name of the sid tune */
    protected String sidName;

    /** Author of the sid tune */
    protected String sidAuthor;

    /** Copyright of the sid tune */
    protected String sidCopyright;

    /** Number of tunes in song  */
    protected int numberOfTunes;

    /** Speed of tune */
    protected String speed;

    /** Chip to use (6581, 8t80, ANY) */
    protected String chip;

    /** The file used in read/write operation */
    protected File file;

    /** Comment to the song(s) */
    protected String songComment;

    /** Comment to the song(s) to insert into the player */
    protected String playerComment;

    /**
   * Create a empty song and instruments
   */
    public Song() {
        instruments = new Instrument[255];
        for (int i = 0; i < instruments.length; i++) {
            instruments[i] = new Instrument();
        }
        tracks = new Track[NUM_TUNES][3];
        for (int i = 0; i < NUM_TUNES; i++) {
            for (int j = 0; j < 3; j++) {
                tracks[i][j] = new Track();
            }
        }
        patterns = new Pattern[Pattern.MAX_PATTERNS];
        for (int i = 0; i < Pattern.MAX_PATTERNS; i++) {
            patterns[i] = new Pattern();
        }
        clear();
        file = null;
    }

    /**
   * Create a special song for play the given instrument in all notes
   *
   * @param song the actual song where to copy some information
   * @param instrument the instrument to use
   */
    public Song(Song song, int instrument) {
        instruments = song.instruments;
        speed = song.speed;
        chip = song.chip;
        numberOfTunes = 96;
        sidName = "Instrument " + instrument;
        sidAuthor = "JITT64 thread";
        sidCopyright = "Not to share";
        playerComment = "Not to share";
        tracks = new Track[NUM_TUNES][3];
        for (int i = 0; i < NUM_TUNES; i++) {
            for (int j = 0; j < 3; j++) {
                if (i < numberOfTunes && j == 0) tracks[i][j] = new Track(i + 1); else tracks[i][j] = new Track();
            }
        }
        patterns = new Pattern[Pattern.MAX_PATTERNS];
        for (int i = 0; i < Pattern.MAX_PATTERNS; i++) {
            if (i == 0 || i > 96) patterns[i] = new Pattern(); else patterns[i] = new Pattern(i + 1, instrument);
        }
    }

    /**
   * Clear the song (instruments, tracks, patterns)
   */
    public void clear() {
        sidName = "";
        sidAuthor = "";
        sidCopyright = "";
        songComment = "";
        playerComment = "";
        speed = "1x";
        chip = CHIP_ANY;
        numberOfTunes = 1;
        for (int i = 0; i < instruments.length; i++) {
            instruments[i].clear();
        }
        for (int i = 0; i < NUM_TUNES; i++) {
            for (int j = 0; j < 3; j++) {
                tracks[i][j].clear();
            }
        }
        for (int i = 0; i < Pattern.MAX_PATTERNS; i++) {
            patterns[i].clear();
        }
    }

    /**
   * Clear the song (instruments, tracks, patterns at default)
   *
   * @param tempo the default pattern tempo
   * @param dimension the defualt pattern dimension
   */
    public void clear(int tempo, int dimension) {
        clear();
        for (int i = 0; i < Pattern.MAX_PATTERNS; i++) {
            patterns[i].setTempo(tempo);
            patterns[i].setSize(dimension);
        }
    }

    /**
   * Get the number of tunes of the song
   * 
   * @return the number of tunes 
   */
    public int getNumberOfTunes() {
        return numberOfTunes;
    }

    /**
   * Set the number of tunes of the song
   * 
   * @param numberOfTunes the number of tunes
   */
    public void setNumberOfTunes(int numberOfTunes) {
        if (numberOfTunes > NUM_TUNES) numberOfTunes = NUM_TUNES;
        this.numberOfTunes = numberOfTunes;
    }

    /**
   * Get the author of the sid
   * 
   * @return the author of the sid
   */
    public String getSidAuthor() {
        return sidAuthor;
    }

    /**
   * Set the author of the sid
   * 
   * @param sidAuthor the author of the sid
   */
    public void setSidAuthor(String sidAuthor) {
        this.sidAuthor = sidAuthor;
    }

    /**
   * Return the copyroght of the sid
   * 
   * @return the copyright of the sid
   */
    public String getSidCopyright() {
        return sidCopyright;
    }

    /**
   * Set the copyright of the sid
   * 
   * @param sidCopyright the copyright of the sid
   */
    public void setSidCopyright(String sidCopyright) {
        this.sidCopyright = sidCopyright;
    }

    /**
   * Get the player comment
   *
   * @return the player comment
   */
    public String getPlayerComment() {
        return playerComment;
    }

    /**
   * Set the player comment
   *
   * @param playerComment the player comment
   */
    public void setPlayerComment(String playerComment) {
        this.playerComment = playerComment;
    }

    /**
   * Get the song comment
   *
   * @return the song comment
   */
    public String getSongComment() {
        return songComment;
    }

    /**
   * Set the song comment
   *
   * @param songComment the song comment
   */
    public void setSongComment(String songComment) {
        this.songComment = songComment;
    }

    /**
   * Get the name of the sid
   * 
   * @return the name of the sid
   */
    public String getSidName() {
        return sidName;
    }

    /**
   * Set the name of the sid
   * 
   * @param sidName the name of the sid
   */
    public void setSidName(String sidName) {
        this.sidName = sidName;
    }

    /**
   * Get the speed of tune 
   * 
   * @return the speed of tune
   */
    public String getSpeed() {
        return speed;
    }

    /**
   * Set the speed of tune
   * 
   * @param speed the speed of tune
   */
    public void setSpeed(String speed) {
        this.speed = speed;
    }

    /**
   * Get the chip to use
   *
   * @return the chip to use
   */
    public String getChip() {
        return chip;
    }

    /**
   * Set the chip to use
   *
   * @param chip the chip to use
   */
    public void setChip(String chip) {
        this.chip = chip;
    }

    /**
   * Get the instruments of this song
   * 
   * @return the instruments
   */
    public Instrument[] getInstruments() {
        return instruments;
    }

    /**
   * Set the instruments used by this song
   * 
   * @param instruments the instruments
  */
    public void setInstruments(Instrument[] instruments) {
        this.instruments = instruments;
    }

    /**
   * Get the tracks of this song
   * 
   * @return the tracks
   */
    public Track[][] getTracks() {
        return tracks;
    }

    /**
   * Set the tracks of this song
   * 
   * @param tracks the tracks
   */
    public void setTracks(Track[][] tracks) {
        this.tracks = tracks;
    }

    /**
   * Get the patterns of this song
   * 
   * @return the patterns
   */
    public Pattern[] getPatterns() {
        return patterns;
    }

    /**
   * Set the patterns of this song
   * 
   * @param patterns the patterns
   */
    public void setPatterns(Pattern[] patterns) {
        this.patterns = patterns;
    }

    /**
   * Get the File handled (null for no one used)
   * 
   * @return the file handled
   */
    public File getFile() {
        return file;
    }

    /**
   * Set the file handled
   * 
   * @param file the file handled to use
   */
    public void setFile(File file) {
        this.file = file;
    }

    /**
   * Get the max number of pattern used into the songs
   * 
   * @return the max number of pattern
   */
    public int getMaxPattern() {
        Track track;
        int size;
        int value;
        boolean skip;
        int max = 0;
        for (int i = 0; i < numberOfTunes; i++) {
            for (int j = 0; j < 3; j++) {
                track = tracks[i][j];
                size = track.getSize();
                skip = false;
                for (int k = 0; k < size - 2; k++) {
                    if (skip) {
                        skip = false;
                        continue;
                    }
                    value = track.getValueAt(k);
                    if (value == Track.PATTERN_REP) {
                        skip = true;
                        continue;
                    }
                    if (value > Track.PATTERN_REP) continue;
                    if (value > max) max = value;
                }
            }
        }
        return max;
    }

    /**
   * Get the max number of instrument used into the songs
   * 
   * @return the max number of instrument
   */
    public int getMaxInstrument() {
        Track track;
        int[] pats;
        int size;
        int value;
        boolean skip;
        int max = 1;
        for (int i = 0; i < numberOfTunes; i++) {
            for (int j = 0; j < 3; j++) {
                track = tracks[i][j];
                size = track.getSize();
                skip = false;
                for (int k = 0; k < size - 2; k++) {
                    if (skip) {
                        skip = false;
                        continue;
                    }
                    value = track.getValueAt(k);
                    if (value == Track.PATTERN_REP) {
                        skip = true;
                        continue;
                    }
                    if (value > Track.PATTERN_REP) continue;
                    pats = patterns[value].getPatInstr();
                    for (int l = 0; l < pats.length; l++) {
                        value = pats[l];
                        if (value < Pattern.VAL_REST) {
                            if (value > max) max = value;
                        }
                    }
                }
            }
        }
        return max;
    }

    /**
   * Change one instrument with another in patterns
   * Be sure to change instrument that are unique in patterns
   * 
   * @param oldVal old instrument value to change
   * @param newVal new instrument value to change
   */
    private void changeInstrument(int oldVal, int newVal) {
        oldVal++;
        newVal++;
        for (int i = 0; i < patterns.length; i++) {
            int[] instrs = patterns[i].getPatInstr();
            for (int j = 0; j < patterns[i].DIMENSION; j++) {
                if (instrs[j] == oldVal) instrs[j] = newVal;
            }
        }
    }

    /**
   * Normalize the instrument in pattern
   */
    private void normalizeInstrument() {
        for (int i = 0; i < patterns.length; i++) {
            int[] instrs = patterns[i].getPatInstr();
            for (int j = 0; j < patterns[i].DIMENSION; j++) {
                if (instrs[j] != 0 && instrs[j] > OFFSET) instrs[j] = instrs[j] - OFFSET;
            }
        }
    }

    /**
   * Move instrument from one position to another
   * 
   * @param fromPos the from position
   * @param toPos the to position
   */
    public void moveInstrument(int fromPos, int toPos) {
        Instrument tempInstr;
        if (toPos == fromPos) return;
        tempInstr = instruments[fromPos];
        changeInstrument(toPos, fromPos + OFFSET);
        if (toPos < fromPos) {
            for (int i = fromPos; i > toPos; i--) {
                instruments[i] = instruments[i - 1];
                changeInstrument(i, i - 1 + OFFSET);
            }
        } else {
            for (int i = fromPos; i < toPos; i++) {
                instruments[i] = instruments[i + 1];
                changeInstrument(i, i + 1 + OFFSET);
            }
        }
        instruments[toPos] = tempInstr;
        normalizeInstrument();
    }

    /**
   * Get the speed of the tune
   * Used for converting the 0.5x 1x 2x 3x 4x to 0,1,2,3,4
   * 
   * @return the speed
   */
    public int getIntSpeed() {
        int res = 1;
        if (speed.equals("0.5x")) res = 0;
        if (speed.equals("2x")) res = 2;
        if (speed.equals("3x")) res = 3;
        if (speed.equals("4x")) res = 4;
        return res;
    }

    /**
   * Get the chip of the tune
   * Used for converting it in integer
   *
   * @return the speed
   */
    public int getIntChip() {
        int res = 1;
        if (chip.equals(CHIP_6581)) res = 1;
        if (chip.equals(CHIP_8580)) res = 2;
        if (chip.equals(CHIP_ANY)) res = 3;
        return res;
    }

    /**
   * Get the usage of pattern regarding the inserted songs and tracks
   *
   * @return the patterns usage
   */
    private boolean[] getPatternUsage() {
        Track track;
        int size;
        int value;
        boolean skip;
        boolean patNotFree[] = new boolean[Pattern.MAX_PATTERNS];
        for (int i = 0; i < numberOfTunes; i++) {
            for (int j = 0; j < 3; j++) {
                track = tracks[i][j];
                size = track.getSize();
                skip = false;
                for (int k = 0; k < size - 2; k++) {
                    if (skip) {
                        skip = false;
                        continue;
                    }
                    value = track.getValueAt(k);
                    if (value == Track.PATTERN_REP) {
                        skip = true;
                        continue;
                    }
                    if (value > Track.PATTERN_REP) continue;
                    patNotFree[value] = true;
                }
            }
        }
        return patNotFree;
    }

    /**
   * Insert the default tempo and pattern into not used pattern
   *
   * @param tempo the pattern tempo
   * @param dimension the pattern dimension
   */
    public void insertDefaultInPattern(int tempo, int dimension) {
        boolean patNotFree[] = getPatternUsage();
        for (int i = 0; i < Pattern.MAX_PATTERNS; i++) {
            if (!patNotFree[i]) {
                patterns[i].setTempo(tempo);
                patterns[i].setSize(dimension);
            }
        }
    }

    /**
   * Get if the instr. table is used
   *
   * @param type the type of table to look for
   * @return 1 if used, 0 if not
   */
    public int getUseInstrTable(int type) {
        if (Shared.option.disableCondComp) return 1;
        for (int i = 0; i < instruments.length; i++) {
            switch(type) {
                case INSTR_AD:
                    if (instruments[i].instrTableAD.getDimension() > 0) return 1;
                    break;
                case INSTR_SR:
                    if (instruments[i].instrTableSR.getDimension() > 0) return 1;
                    break;
                case INSTR_WAVE:
                    if (instruments[i].instrTableWave.getDimension() > 0) return 1;
                    break;
                case INSTR_FREQ:
                    if (instruments[i].instrTableFreq.getDimension() > 0) return 1;
                    break;
                case INSTR_PULSE:
                    if (instruments[i].instrTablePulse.getDimension() > 0) return 1;
                    break;
                case INSTR_FILTER:
                    if (instruments[i].instrTableFilter.getDimension() > 0) return 1;
                    break;
                case INSTR_RES:
                    if (instruments[i].instrTableFilterRes.getDimension() > 0) return 1;
                    break;
                case INSTR_TYPE:
                    if (instruments[i].instrTableFilterType.getDimension() > 0) return 1;
                    break;
            }
        }
        return 0;
    }

    /**
   * Get if the pattern command is used
   *
   * @param type the type of command to look for
   * @return 1 if used, 0 if not
   */
    public int getUsePatternCmd(int type) {
        if (Shared.option.disableCondComp) return 1;
        boolean patNotFree[] = getPatternUsage();
        for (int i = 0; i < Pattern.MAX_PATTERNS; i++) {
            if (patNotFree[i]) {
                int cmds[] = patterns[i].getPatCommand();
                for (int j = 1; j < patterns[i].size; j++) {
                    switch(type) {
                        case CMD_TEMPO:
                            if (cmds[j] == 0x01) return 1;
                            break;
                        case CMD_AD:
                            if (cmds[j] == 0x02) return 1;
                            break;
                        case CMD_SR:
                            if (cmds[j] == 0x03) return 1;
                            break;
                        case CMD_VOLUME:
                            if (cmds[j] == 0x04) return 1;
                            break;
                        case CMD_ARP:
                            if (cmds[j] == 0x05) return 1;
                            break;
                        case CMD_PUP:
                            if (cmds[j] == 0x06) return 1;
                            break;
                        case CMD_PDN:
                            if (cmds[j] == 0x07) return 1;
                            break;
                        case CMD_TPO:
                            if (cmds[j] == 0x08) return 1;
                            break;
                        case CMD_VIB:
                            if (cmds[j] == 0x09) return 1;
                            break;
                        case CMD_SUP:
                            if (cmds[j] == 0x0A) return 1;
                            break;
                        case CMD_SDN:
                            if (cmds[j] == 0x0B) return 1;
                            break;
                        case CMD_FOUT:
                            if (cmds[j] == 0x0C) return 1;
                            break;
                        case CMD_FTY:
                            if (cmds[j] == 0x0D) return 1;
                            break;
                        case CMD_FRS:
                            if (cmds[j] == 0x0E) return 1;
                            break;
                        case CMD_FCU:
                            if (cmds[j] == 0x0F) return 1;
                            break;
                        case CMD_GSR:
                            if (cmds[j] == 0x10) return 1;
                            break;
                    }
                }
            }
        }
        return 0;
    }
}
