package jm.music.data;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import jm.JMC;
import ren.env.ValueGraphModel;
import ren.util.PO;

/**
 * The Part class is representative of a single instrumental part.
 * An Part is made up of a number of Phrase objects, and 
 * Parts in turn are contained by Score objects which form the 
 * highest level in the jMusic data structure. Parts are added to 
 * Score objects like this...
 * <pre>
 *     Score score = new Score("Concerto for Solo Clarinet");
 *     Part inst = new Part("Clarinet");
 *     score.addPart(inst);
 * </pre>
 * @see Score 
 * @see Phrase
 * @author Andrew Sorensen
 * @version 1.0,Sun Feb 25 18:43:32  2001
 */
public class Part implements Cloneable, Serializable, JMC {

    public static final String DEFAULT_TITLE = "Untitled Part";

    public static final int DEFAULT_INSTRUMENT = 0;

    public static final int DEFAULT_CHANNEL = 1;

    public static final int DEFAULT_ID_CHANNEL = -1;

    public static final double DEFAULT_TEMPO = Phrase.DEFAULT_TEMPO;

    public static final int DEFAULT_VOLUME = Phrase.DEFAULT_VOLUME;

    public static final int DEFAULT_KEY_SIGNATURE = JMC.NO_KEY_SIGNATURE;

    public static final int DEFAULT_KEY_QUALITY = JMC.NO_KEY_QUALITY;

    public static final int DEFAULT_NUMERATOR = JMC.NO_NUMERATOR;

    public static final int DEFAULT_DENOMINATOR = JMC.NO_DENOMINATOR;

    public static final double DEFAULT_PAN = Note.DEFAULT_PAN;

    /** A container holding phrase objects */
    private Vector phraseList;

    /** The title of this Part */
    private String title;

    /** The channel on which this part is to be played */
    private int channel;

    /** idchannel used to identify this part in unusual applications */
    private int idChannel;

    /** Optional instrument number/ MIDI program change to apply to this Part */
    private int instrument;

    /** the speed for this part */
    private double tempo;

    /** the loudness for this part */
    private int volume;

    /** break points*/
    private double[] points = null;

    private long[] time = null;

    private int timeIndex = 0;

    /** A reference to the Score containing this part */
    private Score myScore = null;

    /** the number of accidents this part
	* -1 is One Flat, 1 is One Sharp
	*/
    private int keySignature;

    /** 0 = major, 1 = minor, others modes not specified */
    private int keyQuality;

    private int numerator;

    /** the bottom number of the time signature */
    private int denominator;

    /** The stereo (quad etc.) postion of all notes in this part. */
    private double pan = DEFAULT_PAN;

    private boolean modifiedET = true;

    private Hashtable ccEnvs = new Hashtable();

    /**
	* Creates an empty Part
	*/
    public Part() {
        this(DEFAULT_TITLE);
    }

    /**
	* Creates an empty Part
	* @param String title the title of the Part
	*/
    public Part(String title) {
        this(title, DEFAULT_INSTRUMENT);
    }

    /**
	* Creates an empty Part
	* @param int instrument the instrument number for this Part
	*/
    public Part(int instrument) {
        this("", instrument);
    }

    /**
	* Creates an empty Part
	* @param String title the title of the Part
	* @param int instrument
	*/
    public Part(String title, int instrument) {
        this(title, instrument, DEFAULT_CHANNEL);
    }

    /**
	* Creates an empty Part
	* @param int instrument The instrument number for this Part
	* @param int channel The channel number for the part - only required for MIDI writing
	*/
    public Part(int instrument, int channel) {
        this("", instrument, channel);
    }

    /**
	* Creates an empty Part
	* @param String title the title of the Part
	* @param int instrument
	* @param int channel
	*/
    public Part(String title, int instrument, int channel) {
        this.title = title;
        this.phraseList = new Vector();
        if (this.channel > 16) {
            System.err.println(new Exception("jMusic Warning: A MIDI Channel " + "cannot be greater than 16. " + "There can be any number of Audio channels."));
            (new Exception()).printStackTrace();
        }
        this.channel = channel;
        if (instrument < NO_INSTRUMENT) {
            System.err.println(new Exception("jMusic EXCEPTION: instrument " + "value must be greater than 0"));
            (new Exception()).printStackTrace();
            System.exit(1);
        }
        this.instrument = instrument;
        this.tempo = DEFAULT_TEMPO;
        this.volume = DEFAULT_VOLUME;
        this.keySignature = DEFAULT_KEY_SIGNATURE;
        this.keyQuality = DEFAULT_KEY_QUALITY;
        this.numerator = DEFAULT_NUMERATOR;
        this.denominator = DEFAULT_DENOMINATOR;
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE>.
	*
	* @param phrase    Phrase to be contained in the Part
	*/
    public Part(Phrase phrase) {
        this();
        phrase.setMyPart(this);
        addPhrase(phrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE>.
	* @param String Title of the Part
	* @param int 	The instrument number
	* @param phrase	Phrase to be contained in the Part
	*/
    public Part(String title, int instrument, Phrase phrase) {
        this(title, instrument, DEFAULT_CHANNEL, phrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE>.
	* @param String Title of the Part
	* @param int 	The instrument number
	* @param int 	The channel number
	* @param phrase	Phrase to be contained in the Part
	*/
    public Part(String title, int instrument, int channel, Phrase phrase) {
        this(title, instrument, channel);
        phrase.setMyPart(this);
        addPhrase(phrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrases</CODE>.
	*
	* @param phrases   array of Phrases to be contained in the Score
	*/
    public Part(Phrase[] phrases) {
        this();
        addPhraseList(phrases);
    }

    /**
	* Constructs a Part containing the specified <CODE>cphrase</CODE>.
	*
	* @param cphrase    CPhrase to be contained in the Score
	*/
    public Part(CPhrase cphrase) {
        this();
        addCPhrase(cphrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE> with the
	* specified <CODE>title</CODE>.
	*
	* @param phrase    Phrase to be contained in the Part
	* @param title     String describing the title of the Part
	*/
    public Part(Phrase phrase, String title) {
        this(title);
        addPhrase(phrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE> with the
	* specified <CODE>title</CODE>.
	*
	* @param phrases   array of Phrases to be contained in the Score
	* @param title     String describing the title of the Part
	*/
    public Part(Phrase[] phrases, String title) {
        this(title);
        addPhraseList(phrases);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE> with the
	* specified <CODE>title</CODE> and with the timbre of the specified
	* <CODE>instrument</CODE>.
	*
	* @param phrase        Phrase to be contained in the Part
	* @param title         String describing the title of the Part
	* @param instrument    integer describing the MIDI instrument number
	*/
    public Part(Phrase phrase, String title, int instrument) {
        this(title, instrument);
        addPhrase(phrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE> with the
	* specified <CODE>title</CODE> and with the timbre of the specified
	* <CODE>instrument</CODE>.
	*
	* @param phrases       array of Phrases to be contained in the Score
	* @param title         String describing the title of the Part
	* @param instrument    integer describing the MIDI instrument number
	*/
    public Part(Phrase[] phrases, String title, int instrument) {
        this(title, instrument);
        addPhraseList(phrases);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE> with the
	* specified <CODE>title</CODE>, with the timbre of the specified
	* <CODE>instrument</CODE> and using the specified MIDI channel.
	*
	* @param phrase        Phrase to be contained in the Part
	* @param title         String describing the title of the Part
	* @param instrument    integer describing the MIDI instrument number
	* @param channel       integer describing the MIDI channel
	*/
    public Part(Phrase phrase, String title, int instrument, int channel) {
        this(title, instrument, channel);
        addPhrase(phrase);
    }

    /**
	* Constructs a Part containing the specified <CODE>phrase</CODE> with the
	* specified <CODE>title</CODE>, with the timbre of the specified
	* <CODE>instrument</CODE> and using the specified MIDI channel.
	*
	* @param phrases       array of Phrases to be contained in the Score
	* @param title         String describing the title of the Part
	* @param instrument    integer describing the MIDI instrument number        
	* @param channel       integer describing the MIDI channel
	*/
    public Part(Phrase[] phrases, String title, int instrument, int channel) {
        this(title, instrument, channel);
        addPhraseList(phrases);
    }

    /**
	 * Get an individual phrase object from its number 
	 * @param int number - the number of the Track to return
	 * @return phrase answer - the phrase to return
	 */
    public Phrase getPhrase(int number) {
        if (number >= this.size()) return null;
        return (Phrase) phraseList.get(number);
    }

    public void addCCEnv(int ccType, ValueGraphModel env) {
        this.ccEnvs.put(new Integer(ccType), env);
    }

    public void addCCEnv(int ccType, double[] pos, double[] val) {
        if (pos.length != val.length) {
            try {
                Exception e = new Exception(" pos and val mus be equal length");
                e.fillInStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ValueGraphModel env = new ValueGraphModel();
        for (int i = 0; i < pos.length; i++) {
            env.addNode(pos[i], val[i]);
        }
        addCCEnv(ccType, env);
    }

    /**
	 * Add a phrase to this Part
	 * @param Phrase phrase - add a phrase to this Part
	 */
    public void add(Phrase phrase) {
        this.addPhrase(phrase);
    }

    /**
	* Add a phrase to this Part
	* Phrases with a 'true' append flag are added to the end of the part.
	* @param Phrase phrase - add a phrase to this Part
	*/
    public void addPhrase(Phrase phrase) {
        phrase.setMyPart(this);
        this.phraseList.addElement(phrase);
        this.modifiedET = true;
    }

    /**
	 * Add a copy of a phrase to the end of this Part
	 * @param Phrase the phrase to add
         */
    public void appendPhrase(Phrase phrase) {
        Phrase newPhrase = phrase.copy();
        newPhrase.setStartTime(this.getEndTime());
        this.addPhrase(newPhrase);
    }

    /**
	 * Adds multiple phrases to the part from an array of phrases
	 * @param phraseArray
	 */
    public void addPhraseList(Phrase[] phraseArray) {
        for (int i = 0; i < phraseArray.length; i++) {
            if (phraseArray[i].getAppend()) {
                Phrase newPhrase = phraseArray[i].copy();
                newPhrase.setStartTime(this.getEndTime());
                this.addPhrase(newPhrase);
            } else this.addPhrase(phraseArray[i]);
        }
    }

    public void addPhraseList(Phrase[] phraseArray, boolean append) {
        for (int i = 0; i < phraseArray.length; i++) {
            if (append) {
                Phrase newPhrase = phraseArray[i].copy();
                newPhrase.setStartTime(this.getEndTime());
                this.addPhrase(newPhrase);
            } else this.addPhrase(phraseArray[i]);
        }
    }

    /**
	 * Deletes the specified phrase in the part
	 * @param int noteNumb the index of the note to be deleted
	 */
    public void removePhrase(int phraseNumb) {
        Vector vct = (Vector) this.phraseList;
        try {
            vct.remove(phraseNumb);
        } catch (RuntimeException re) {
            System.err.println("The Phrase index to be deleted must be within the part.");
        }
    }

    /**
	* Deletes the first occurence of the specified phrase in the Part.
	* @param phrase  the Phrase object to be deleted.
	*/
    public void removePhrase(Phrase phrase) {
        this.phraseList.removeElement(phrase);
    }

    /**
	 * Deletes the last phrase added to the part
	 */
    public void removeLastPhrase() {
        Vector vct = (Vector) this.phraseList;
        vct.removeElement(vct.lastElement());
    }

    /**
	 * Deletes all the phrases previously added to the part
	 */
    public void removeAllPhrases() {
        this.phraseList.removeAllElements();
    }

    /**
	 * Returns the entire phrase list
	 * @return Vector - A vector containing all this Parts phrase objects
	 */
    public Vector getPhraseList() {
        return this.phraseList;
    }

    /**
	 * Updates the entire phrase list
	 * @param Vector containing phrase objects
	 */
    public void setPhraseList(Vector newPhraseList) {
        this.phraseList = newPhraseList;
    }

    /**
	 * Returns the all phrases in this part as a array
	 * @return Phrase[] An array containing all Phrase objects in this part
	 */
    public Phrase[] getPhraseArray() {
        Vector vct = (Vector) this.phraseList;
        Phrase[] phraseArray = new Phrase[vct.size()];
        for (int i = 0; i < phraseArray.length; i++) {
            phraseArray[i] = (Phrase) vct.elementAt(i);
        }
        return phraseArray;
    }

    public void sort() {
        Phrase[] arr = this.getPhraseArray();
        quickSort(arr, 0, arr.length - 1);
        this.phraseList.removeAllElements();
        this.phraseList.ensureCapacity(arr.length);
        for (int i = 0; i < arr.length; i++) {
            phraseList.add(arr[i]);
        }
    }

    private void quickSort(Phrase[] a, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        Phrase mid;
        if (hi0 > lo0) {
            mid = a[(lo0 + hi0) / 2];
            while (lo <= hi) {
                while ((lo < hi0) && (a[lo].getStartTime() < mid.getStartTime())) ++lo;
                while ((hi > lo0) && (a[hi].getStartTime() > mid.getStartTime())) --hi;
                if (lo <= hi) {
                    swap(a, lo, hi);
                    ++lo;
                    --hi;
                }
            }
            if (lo0 < hi) quickSort(a, lo0, hi);
            if (lo < hi0) quickSort(a, lo, hi0);
        }
    }

    private void swap(Phrase[] parr, int i, int j) {
        Phrase temp;
        temp = parr[i];
        parr[i] = parr[j];
        parr[j] = temp;
    }

    /**
	 * Add a chord phrase to the part
	 * as part of this process we need to remove the
	 * CPhrase's phrase list and add them to the Part's 
	 * normal phrase list.
	 * CPhrases with a 'true' append flag are adeed to the end of the part.
	 * @param CPhrase
	 */
    public void addCPhrase(CPhrase cphrase) {
        if (cphrase.getAppend()) cphrase.setStartTime(this.getEndTime());
        Enumeration enumr = cphrase.getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phr = (Phrase) enumr.nextElement();
            this.addPhrase(phr);
        }
    }

    /**
	 * Returns the Parts title 
	 * @return String title
	 */
    public String getTitle() {
        return this.title;
    }

    /**
	 * Sets the Parts title 
	 * @param String title
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * Gets the channel for this part
	 * @return short channel
	 */
    public int getChannel() {
        return this.channel;
    }

    /**
	 * Sets the MidiChannel for this part
	 * @param short midiChannel
	 */
    public void setChannel(int channel) {
        this.channel = channel;
    }

    /**
     * gets the identifier channel that can 
     * be used internally by other applications
     * while maintaining the midi channel
     * @return idchannel
     */
    public int getIdChannel() {
        return idChannel;
    }

    /**
     * sets the identifier channel that can 
     * be used internally by other applications
     * while maintaining the midi channel
     * @return idchannel
     */
    public void setIdChannel(int idChannel) {
        this.idChannel = idChannel;
        for (int i = 0; i < phraseList.size(); i++) {
            getPhrase(i).setIdChannel(idChannel);
        }
    }

    /**
	 * Get Instrument number / MIDI Program Change
	 * @return program change
	 */
    public int getInstrument() {
        return this.instrument;
    }

    /**
	 * Set instrument number / MIDI Program Change
	 * @param int program change
	 */
    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }

    /**
	 * Set instrument number / MIDI Program Change
	 * This method is deprecated in favour of setInstrument!!
	 * @param int program change
	 */
    public void setProgChg(int program) {
        this.instrument = program;
    }

    /**
	 * Returns the Part's tempo 
	 * @return double tempo
	 */
    public double getTempo() {
        return this.tempo;
    }

    /**
	 * Sets the Part's tempo 
	 * @param double tempo
	 */
    public void setTempo(double tempo) {
        this.tempo = tempo;
    }

    /**
	 * Returns the Part's volume 
	 * @return int volume
	 */
    public int getVolume() {
        return this.volume;
    }

    /**
	 * Sets the Part's volume 
	 * @param int volume
	 */
    public void setVolume(int volume) {
        this.volume = volume;
    }

    /**'
	 * Returns the Parts key signature 
	 * The number of sharps (+) or flats (-)
	 * @return int key signature
	 */
    public int getKeySignature() {
        return this.keySignature;
    }

    /**
	 * Specifies the Part's key signature 
	 * The number of sharps (+) or flats (-)
	 * @param int key signature
	 */
    public void setKeySignature(int newSig) {
        this.keySignature = newSig;
    }

    /**
	 * Returns the Parts key quality 
	 * 0 is Major, 1 is minor
	 * @return int key quality
	 */
    public int getKeyQuality() {
        return this.keyQuality;
    }

    /**
	 * Specifies the Part's key quality 
	 * 0 is Major, 1 is minor
	 * @param int key quality (modality)
	 */
    public void setKeyQuality(int newQual) {
        this.keyQuality = newQual;
    }

    /**
	 * Returns the Parts time signature numerator 
	 * @return int time signature numerator
	 */
    public int getNumerator() {
        return this.numerator;
    }

    /**
	 * Specifies the Part's time signature numerator 
	 * @param int time signature numerator
	 */
    public void setNumerator(int num) {
        this.numerator = num;
    }

    /**
	 * Returns the Parts time signature denominator 
	 * @return int time signature denominator
	 */
    public int getDenominator() {
        return this.denominator;
    }

    /**
	 * Specifies the Part's time signature denominator
	 * @param int time signature denominator
	 */
    public void setDenominator(int dem) {
        this.denominator = dem;
    }

    /**
	 * Return the pan position for this part
	 * @return double the part's pan setting
	 */
    public double getPan() {
        return this.pan;
    }

    /**
	 * Determine the pan position for all notes in this part. 
	 * @param double the part's pan setting
	 */
    public void setPan(double pan) {
        this.pan = pan;
        Enumeration enumr = phraseList.elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            phrase.setPan(pan);
        }
    }

    /** set a reference to the score containing this part */
    public void setMyScore(Score scr) {
        this.myScore = scr;
    }

    /** get a reference to the score that contains this part */
    public Score getMyScore() {
        return myScore;
    }

    /** Make a duplicate of the current part */
    public Part copy() {
        Part i;
        i = new Part();
        Enumeration enumr = this.phraseList.elements();
        while (enumr.hasMoreElements()) {
            Phrase oldPhrase = (Phrase) enumr.nextElement();
            i.addPhrase((Phrase) oldPhrase.copy());
        }
        copyAttributes(i);
        return (Part) i;
    }

    /**
	 * copies the attributes from this one into the passed part
	 * @param i
	 */
    public void copyAttributes(Part i) {
        i.setInstrument(this.getInstrument());
        i.setChannel(this.getChannel());
        i.setIdChannel(this.getIdChannel());
        i.setTitle(this.getTitle());
        i.setTempo(this.tempo);
        i.setVolume(this.volume);
        i.setPoints(this.points);
        i.setTime(this.time);
        i.setTimeIndex(this.timeIndex);
        i.setMyScore(this.getMyScore());
    }

    /**
     * to facilitate memory saving, you can copy a part
     * into this one rather than abandoning it to the garbage collector
     * @param p
     */
    public void copyFrom(Part p) {
        copyAttributes(p);
        this.phraseList = p.getPhraseList();
    }

    /**
	* Returns a copy of the Part between specified loactions
	* @param double start of copy section in beats
	* @param double end of copy section in beats
	* @return Part a copy of section of the Part
	*/
    public Part copy(double startLoc, double endLoc) {
        Part cp;
        Vector tempVect = new Vector();
        cp = new Part();
        copyAttributes(cp);
        Enumeration enumr = this.phraseList.elements();
        while (enumr.hasMoreElements()) {
            Phrase ph = (Phrase) enumr.nextElement();
            if (ph.getStartTime() < endLoc && ph.getEndTime() > startLoc) {
                tempVect.addElement(ph.copy(startLoc, endLoc));
            }
        }
        cp.setPhraseList(tempVect);
        return (Part) cp;
    }

    /**
         * Returns a copy of the Part between specified loactions
          * @param boolean wether to trim the notes or not
          * @param boolean wether to truncated the notes duration
          * when trimming them or not
          * @param boolean wether to set the start time of the phrases
          * in relation to the start of the
          * <br> old part (true) or the new one (false) maybe should be
          * called "relative to old"
          * @param double start of copy section in beats
          * @param double end of copy section in beats
          * @return Part a copy of section of the Part
          */
    public Part copy(double startLoc, double endLoc, boolean trimmed, boolean truncated, boolean zeroStartTime) {
        return copy(startLoc, endLoc, trimmed, truncated, zeroStartTime, true);
    }

    private static final int DAC = 10000;

    public Part copy(double startLoc, double endLoc, boolean trimmed, boolean truncated, boolean zeroStartTime, boolean rests) {
        System.out.println("don't use copy - its crap.  Use copyRT for timeslicing");
        Part cp;
        Vector tempVect = new Vector();
        cp = new Part(this.title, this.instrument, this.channel);
        cp.setMyScore(this.getMyScore());
        Enumeration enumr = this.phraseList.elements();
        while (enumr.hasMoreElements()) {
            Phrase ph = (Phrase) enumr.nextElement();
            double startTime = ph.getStartTime();
            if (startTime < endLoc && ph.getEndTime() > startLoc) {
                Phrase cpy = ph.copy(startLoc, endLoc, trimmed, truncated, zeroStartTime, rests);
                if (cpy.size() > 1 || (cpy.size() == 1 && (rests || !cpy.getNote(0).isRest()))) tempVect.addElement(cpy);
            }
        }
        cp.setPhraseList(tempVect);
        if (rests) {
            if (zeroStartTime) {
                if (cp.getEndTime() < ((int) (endLoc * DAC) - (int) (startLoc * DAC)) / DAC) {
                    cp.addPhrase(new Phrase(new Note(REST, ((int) (endLoc * DAC) - (int) (startLoc * DAC)) / DAC), 0.0));
                }
            } else {
                if (cp.getEndTime() < endLoc) {
                    cp.addPhrase(new Phrase(new Note(REST, endLoc), 0.0));
                }
            }
        }
        return (Part) cp;
    }

    /**
          * a way to copy a segment of the part for time
          * critical time-slicing
          */
    public Part copyRT(double st, double en) {
        Part ret = copyEmpty();
        int sz = this.size();
        for (int i = 0; i < sz; i++) {
            if (this.getPhrase(i).getStartTime() >= st && this.getPhrase(i).getStartTime() < en) {
                Phrase phr = this.getPhrase(i).copy();
                phr.setStartTime(phr.getStartTime() - st);
                if (phr.getNote(0).getRhythmValue() + phr.getStartTime() > (en - st)) {
                    phr.getNote(0).setRhythmValue((en - st) - phr.getStartTime());
                }
                ret.add(phr);
            }
        }
        return ret;
    }

    /**
	 * Return the Parts endTime
	 * @return double the Parts endTime
	 */
    private double lastET = -1;

    public double getEndTime() {
        if (!modifiedET) {
            return lastET;
        }
        double endTime = 0.0;
        double phraseEnd;
        Enumeration enumr;
        Phrase nextPhr;
        enumr = this.phraseList.elements();
        while (enumr.hasMoreElements()) {
            nextPhr = (Phrase) enumr.nextElement();
            if (nextPhr == null) {
                phraseEnd = 0;
            } else {
                phraseEnd = nextPhr.getEndTime() + nextPhr.getStartTime();
            }
            if (phraseEnd > endTime) {
                endTime = phraseEnd;
            }
        }
        modifiedET = false;
        lastET = endTime;
        return endTime;
    }

    /**
	 * Collects the Parts attributes to a string
	 */
    public String toString() {
        String partData = new String("----- jMusic PART: '" + title + "' contains " + this.size() + " phrases.  -----" + '\n');
        partData += "Channel = " + channel + '\n';
        partData += "IdChannel = " + this.idChannel + "\n";
        partData += "Instrument = " + instrument + '\n';
        if (this.tempo > 0) partData += "Part Tempo = " + this.tempo + '\n';
        Enumeration enumr = phraseList.elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            if (phrase == null) partData = partData + "null phrase \n"; else partData = partData + phrase.toString() + '\n';
        }
        return partData;
    }

    /**
	* Empty removes all elements in the vector
	*/
    public void empty() {
        phraseList.removeAllElements();
        modifiedET = true;
    }

    /**
	 * Get the number of phrases in this part
	 * @return int  The number of phrases
	 */
    public int length() {
        return size();
    }

    /**
	 * get the number of phrases in this part
	 * @return int  length - the number of phrases
	 */
    public int size() {
        return (phraseList.size());
    }

    /**
	* get the number of phrases in this part
	* @return int  length - the number of phrases
	*/
    public int getSize() {
        return (phraseList.size());
    }

    /**
	* Remove phrases from the score.
	*/
    public void clean() {
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            if (phrase.getInstrument() == this.instrument) phrase.setInstrument(Phrase.DEFAULT_INSTRUMENT);
            if (phrase.getNoteList().size() == 0) {
                this.removePhrase(phrase);
            }
        }
    }

    /**
	* Return the value of the highest note in the part.
	*/
    public int getHighestPitch() {
        int max = 0;
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            if (phrase.getHighestPitch() > max) max = phrase.getHighestPitch();
        }
        return max;
    }

    /**
	* Return the value of the lowest note in the part.
	*/
    public int getLowestPitch() {
        int min = 127;
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            if (phrase.getLowestPitch() < min) min = phrase.getLowestPitch();
        }
        return min;
    }

    /**
	* Return the value of the longest rhythm value in the part.
	*/
    public double getLongestRhythmValue() {
        double max = 0.0;
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            if (phrase.getLongestRhythmValue() > max) max = phrase.getLongestRhythmValue();
        }
        return max;
    }

    /**
	* Return the value of the shortest rhythm value in the part.
	*/
    public double getShortestRhythmValue() {
        double min = 1000.0;
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            if (phrase.getShortestRhythmValue() < min) min = phrase.getShortestRhythmValue();
        }
        return min;
    }

    /**
	* Change the dynamic value of each note in the Part.
	*/
    public void setDynamic(int dyn) {
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            phrase.setDynamic(dyn);
        }
    }

    /**
	* Change the Pitch value of each note in the Part.
	*/
    public void setPitch(int val) {
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            phrase.setPitch(val);
        }
    }

    /**
	* Change the rhythmValue value of each note in the Part.
	*/
    public void setRhythmValue(double val) {
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            phrase.setRhythmValue(val);
        }
    }

    /**
	* Change the duration value of each note in the Part.
	*/
    public void setDuration(double val) {
        Enumeration enumr = getPhraseList().elements();
        while (enumr.hasMoreElements()) {
            Phrase phrase = (Phrase) enumr.nextElement();
            phrase.setDuration(val);
        }
    }

    public void setPoints(double[] p) {
        points = p;
    }

    public double getPoint() {
        return this.points[timeIndex];
    }

    public void setTime(long[] t) {
        this.time = t;
    }

    public long getTime() {
        return this.time[timeIndex++];
    }

    public void setTimeIndex(int index) {
        this.timeIndex = index;
    }

    public int getTimeIndex() {
        return this.timeIndex;
    }

    /**
	* Add a note directly to a part, this method
	* automatically encapsulates the note within a phrase.
	* @param Note the note to be added.
	* @param startTime the beat position where the note (phrase) will be placed
	*/
    public void addNote(Note n, double startTime) {
        Phrase phrase = new Phrase("Generated by Part.addNote()", startTime);
        phrase.addNote(n);
        this.addPhrase(phrase);
    }

    /**
        * Generates and returns a new empty phrase 
        * and adds it to this part.
        */
    public Phrase createPhrase() {
        Phrase p = new Phrase();
        this.addPhrase(p);
        return p;
    }

    /**
    * Change both the rhythmValue and duration of each note in the part.
     * @param newLength The new rhythmValue for the note (Duration is a proportion of this value)
     */
    public void setLength(double newLength) {
        this.setRhythmValue(newLength);
        this.setDuration(newLength * Note.DEFAULT_DURATION_MULTIPLIER);
    }

    /**
     * Conflates all the phrases within the Part to one phrase. 
     * This method adjusts rhythm values of Notes as required in the process - be warned.
     */
    public void mergePhrases() {
        Vector noteTable = new Vector();
        Enumeration en = getPhraseList().elements();
        while (en.hasMoreElements()) {
            Phrase phrase = (Phrase) en.nextElement();
            double currentStartTime = phrase.getStartTime();
            Enumeration en2 = phrase.getNoteList().elements();
            while (en2.hasMoreElements()) {
                Note n = (Note) en2.nextElement();
                n.setSampleStartTime(currentStartTime);
                noteTable.addElement(n);
                currentStartTime += n.getRhythmValue();
            }
        }
        Object[] coll = noteTable.toArray();
        java.util.Arrays.sort(coll, new MergeComparator());
        Note n1 = (Note) coll[0];
        Note n2 = (Note) coll[1];
        Phrase newPhrase = new Phrase(n1.getSampleStartTime());
        for (int i = 2; i < coll.length; i++) {
            if (n2.getSampleStartTime() < n1.getSampleStartTime() + n1.getRhythmValue()) {
                n1.setRhythmValue(n2.getSampleStartTime() - n1.getSampleStartTime());
            }
            newPhrase.addNote(n1);
            if (n2.getSampleStartTime() > n1.getSampleStartTime() + n1.getRhythmValue()) {
                System.out.println("n2 st = " + n2.getSampleStartTime() + " n1 st = " + n1.getSampleStartTime());
                newPhrase.addNote(new Rest(n2.getSampleStartTime() - n1.getSampleStartTime() - n1.getRhythmValue()));
            }
            n1.setSampleStartTime(0);
            n1 = n2;
            n2 = (Note) coll[i];
        }
        if (n2.getSampleStartTime() < n1.getSampleStartTime() + n1.getRhythmValue()) {
            n1.setRhythmValue(n2.getSampleStartTime() - n1.getSampleStartTime());
        }
        newPhrase.addNote(n1);
        newPhrase.addNote(n2);
        this.phraseList.removeAllElements();
        this.phraseList.add(newPhrase);
    }

    public Part copyLast(int num) {
        Part np = this.copyEmpty();
        num = this.size() - num;
        if (num < 0) num = 0;
        for (; num < this.size(); num++) {
            np.add(this.getPhrase(num));
        }
        return np;
    }

    public Part copyEmpty() {
        Part p = new Part();
        this.copyAttributes(p);
        return p;
    }

    public int getIDChannel() {
        return this.idChannel;
    }

    public Hashtable getCCEnvs() {
        return this.ccEnvs;
    }

    public void setCCEnvs(Hashtable nccenvs) {
        this.ccEnvs = nccenvs;
    }

    /**
	 * Go through each control change envelope in this part, and create a
	 * coped portion from from to to.  This portion is returned in another 
	 * hashtable of ValueGraphModels.  The starting point of the copied section
	 * is 0.
	 * @param ccenvs must be a hashtable full of ValueGraphModel envelopes
	 * @param from the point to start the cut
	 * @param to the point to end the cut
	 * @return
	 */
    public Hashtable copyCCEnv(double from, double to) {
        Enumeration keys = ccEnvs.keys();
        Hashtable toRet = new Hashtable(ccEnvs.size());
        while (keys.hasMoreElements()) {
            Integer key = (Integer) keys.nextElement();
            if (!((ValueGraphModel) (ccEnvs.get(key))).isMuted()) {
                toRet.put(new Integer(key.intValue()), ((ValueGraphModel) (ccEnvs.get(key))).copySegment(from, to, true));
            } else {
            }
        }
        return toRet;
    }
}

class MergeComparator implements java.util.Comparator {

    public int compare(Object o1, Object o2) {
        Note n1 = (Note) o1;
        Note n2 = (Note) o2;
        if (n1.getSampleStartTime() < n2.getSampleStartTime()) return -1;
        if (n1.getSampleStartTime() > n2.getSampleStartTime()) return 1;
        return 0;
    }

    public boolean equals(Object obj) {
        return false;
    }
}
