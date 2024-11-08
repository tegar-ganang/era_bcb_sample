package jstella.core;

import jstella.core.JSConstants.*;

/**
 * This class is responsible for getting pokes from the TIA and converting
 * them into sound.
 *
 * <p>
 * This class uses Java Sound, specifically the javax.sound.sampled package.
 * It synthesizes sound based on the values of the TIA sound registers.
 * <p>
 * The sound is created in a simple format: linear PCM.  The sample rate (i.e. how many
 * values per second) used is 44,100 (samples/sec), because that seems to be the most
 * commonly supported sample rate among java implementations (based on my very quick
 * not-very-good assessment.)  Each sample is a single signed byte (because that's
 * what the Java 'byte' class is.) Therefore, each sample must be between (inclusive)
 * -128 and 127.  (And so on...)
 * <p>
 * The source data line is used to play the sound.  Make sure that its internal buffer
 * doesn't completely empty, or otherwise, expect popping sounds.
 * <p>
 * Some definitions:
 * <dl>
 *  <dt>sample</dt>
 *  <dd>A single value of digital audio data, representing the amplitude of the wave at that point.  
 * Every sound can be represented as a wave diagram.  Each sample corresponds with a given x value, and the 
 * value of the sample corresponds to the y value at that x value.  </dd>
 * </dl>
 * <dl>
 *<dt>display frame</dt>
 *<dd> An animation frame. Animation is simply a bunch of pictures shown one after the other very fast.  Each one of these pictures is called 
 * a frame.
 * </dd>
 * </dl>
 * <dl>
 * <dt>audio frame</dt>
 * <dd>A group of samples corresponding to the same moment of time...one sample for each channel.
 * Thus, when there is only one channel (mono), samples=audio frames.  In stereo music, there are 
 * twice as many samples as audio frames.  (However, a lot of time, "sample rate" means "sample rate per channel".)
 *</dd>
 *</dl>
 *
 *
 * @author Bradford W. Mott and the Stella team (original)
 * J.L. Allen (Java translation)
 */
public class JSAudio {

    private static final long serialVersionUID = -2127696001297260042L;

    public static final double CPU_SPEED = 1193191.66666667;

    private static final int JAVA_SOUND_SAMPLE_RATE = 44100;

    private static final int TIA_SAMPLE_RATE = 31400;

    public static final double CYCLES_PER_SAMPLE = CPU_SPEED / (double) JAVA_SOUND_SAMPLE_RATE;

    private static final int BYTES_PER_SAMPLE = 1;

    private static final int BITS_PER_SAMPLE = BYTES_PER_SAMPLE * 8;

    private static final boolean BIG_ENDIAN = true;

    private static final boolean SIGNED_VALUE = true;

    private static final int CHANNELS = 1;

    private static final int DEFAULT_BUFFER_CUSHION = 4000;

    private JSConsole myConsole = null;

    private byte[] myPreOutputBuffer = new byte[JAVA_SOUND_SAMPLE_RATE * 4];

    private boolean myInitSuccess = false;

    private double myCyclePool = 0;

    private int myPreviousCycle = 0;

    private AudioRegisterPoke myPreviousPoke = null;

    private int[] myAUDC = new int[2];

    private int[] myAUDF = new int[2];

    private int[] myAUDV = { 1, 1 };

    private int[] myFutureAUDC = new int[2];

    private int[] myFutureAUDF = new int[2];

    private int[] myFutureAUDV = new int[2];

    private FrequencyDivider[] myFrequencyDivider = { new FrequencyDivider(), new FrequencyDivider() };

    private int[] myP4 = new int[2];

    private int[] myP5 = new int[2];

    private int myOutputCounter = 0;

    private int myChannels = CHANNELS;

    private int myVolumePercentage = 100;

    private int myVolumeClip = 128;

    private int myNominalDisplayFrameRate = 60;

    private double myRealDisplayFrameRate = 60.0;

    private double myCycleSampleFactor = 1.0;

    private double myAdjustedCyclesPerAudioFrame = CYCLES_PER_SAMPLE;

    private int myBufferCushion = DEFAULT_BUFFER_CUSHION * myChannels;

    private boolean mySoundEnabled = true;

    /**
     * Creates a new instance of JSAudio
     * @param aConsole the console that owns this object
     */
    protected JSAudio(JSConsole aConsole) {
        myConsole = aConsole;
        reset();
    }

    public boolean isSoundEnabled() {
        return mySoundEnabled;
    }

    public void setVolume(int percent) {
        myVolumePercentage = percent;
    }

    public void setSoundEnabled(boolean aSoundEnabled) {
        mySoundEnabled = aSoundEnabled;
    }

    public void pauseAudio() {
    }

    /**
     * Sets the nominal (theoretical) display frame rate.  This is usually 60 or 50.
     * See documentation for setRealDisplayFrameRate(...).
     * @param aFrameRate 
     */
    protected void setNominalDisplayFrameRate(int aFrameRate) {
        myNominalDisplayFrameRate = aFrameRate;
        myPreviousCycle = 0;
        updateCycleSampleFactor();
    }

    /**
     * The sound object bases its frequency on the display's frame rate.  If the game
     * was designed for 60 frames per second display rate, then it expects to be
     * called that many times per second to accurately produce the correct pitch.
     * Because the timers that are used to run the emulator use "millisecond deley" rather
     * than "frequency", certain frame rates can only be approximated.  
     * For example, a delay of 16 ms is 62.5 frames per second, and a delay of 17 ms
     * is 58.82 frames per second.  
     * To remedy this approximation, the class responsible for timing will notify
     * this class (via JSConsole) to compensate for this inexact.
     * @param aRealFrameRate 
     */
    public void setRealDisplayFrameRate(double aRealFrameRate) {
        myRealDisplayFrameRate = aRealFrameRate;
        updateCycleSampleFactor();
    }

    private void updateCycleSampleFactor() {
        myCycleSampleFactor = myRealDisplayFrameRate / (double) myNominalDisplayFrameRate;
        myAdjustedCyclesPerAudioFrame = CYCLES_PER_SAMPLE * myCycleSampleFactor;
    }

    protected void systemCyclesReset(int aCurrentCycle) {
        myPreviousCycle -= aCurrentCycle;
    }

    /**
     * Used when user is saving game (aka a state save).  The only data from this class
     * that needs to be saved are the contents of the registers.  (Sometimes a game
     * will set the register early on and not touch it again.)  This is called
     * during the serialization method of JSConsole.
     * @return The audio register data in array form.
     */
    protected int[] getAudioRegisterData() {
        int[] zReturn = new int[6];
        zReturn[0] = myAUDC[0];
        zReturn[1] = myAUDC[1];
        zReturn[2] = myAUDF[0];
        zReturn[3] = myAUDF[1];
        zReturn[4] = myAUDV[0];
        zReturn[5] = myAUDV[1];
        return zReturn;
    }

    /**
     * This is the opposite of the getAudioRegisterData() method.  It is called 
     * by JSConsole after a saved game has been read.
     * @param aData an array of the register values
     */
    protected void setAudioRegisterData(int[] aData) {
        setAudioRegister(JSConstants.AUDC0, aData[0]);
        setAudioRegister(JSConstants.AUDC1, aData[1]);
        setAudioRegister(JSConstants.AUDF0, aData[2]);
        setAudioRegister(JSConstants.AUDF1, aData[3]);
        setAudioRegister(JSConstants.AUDV0, aData[4]);
        setAudioRegister(JSConstants.AUDV1, aData[5]);
    }

    /**
     * Changes the audio mode to either mono (channels=1), or stereo (channels=2).
     * @param aChannels Number of channels (1 or 2)
     */
    protected void setChannelNumber(int aChannels) {
        if (myChannels != aChannels) {
            myChannels = aChannels;
            initialize();
        }
    }

    /**
     * Returns the number of channels the audio system is using.
     * @return returns a 1 for mono, 2 for stereo
     */
    protected int getChannelNumber() {
        return myChannels;
    }

    /**
     * Closes the sound, freeing up system sound resources.
     */
    protected void close() {
    }

    /**
     * Initializes the sound.  After calling this, one should make sure that
     * close() is called when this class is finished.
     */
    protected void initialize() {
        try {
            boolean zPreviouslyEnabled = mySoundEnabled;
            mySoundEnabled = false;
            clearPokeQueue();
            myInitSuccess = true;
            mySoundEnabled = zPreviouslyEnabled;
            myBufferCushion = DEFAULT_BUFFER_CUSHION * myChannels;
            updateCycleSampleFactor();
        } catch (Exception e) {
            myInitSuccess = false;
        }
    }

    /**
     * Checks to see if the required Java Sound objects were created successfully.
     * @return true if sound objects created successfully
     */
    protected boolean isSuccessfullyInitialized() {
        return myInitSuccess;
    }

    protected void reset() {
        myPreviousCycle = 0;
        clearPokeQueue();
        myAUDC[0] = myAUDC[1] = myAUDF[0] = myAUDF[1] = myAUDV[0] = myAUDV[1] = 0;
        myFutureAUDC[0] = myFutureAUDC[1] = myFutureAUDF[0] = myFutureAUDF[1] = myFutureAUDV[0] = myFutureAUDV[1] = 0;
        myP4[0] = myP5[0] = myP4[1] = myP5[1] = 1;
        myFrequencyDivider[0].set(0);
        myFrequencyDivider[1].set(0);
        myOutputCounter = 0;
    }

    /**
     * This is the method that actually changes the sound register
     * variables.  It is called by processPokeQueue(...) shortly before
     * process(...) is called.
     * @param address Address of a sound register
     * @param value The value to assign to the given register
     */
    private void setAudioRegister(int address, int value) {
        switch(address) {
            case 0x15:
                myAUDC[0] = value & 0x0f;
                break;
            case 0x16:
                myAUDC[1] = value & 0x0f;
                break;
            case 0x17:
                myAUDF[0] = value & 0x1f;
                myFrequencyDivider[0].set(myAUDF[0]);
                break;
            case 0x18:
                myAUDF[1] = value & 0x1f;
                myFrequencyDivider[1].set(myAUDF[1]);
                break;
            case 0x19:
                myAUDV[0] = value & 0x0f;
                break;
            case 0x1a:
                myAUDV[1] = value & 0x0f;
                break;
            default:
                break;
        }
    }

    private int getAudioRegister(char address) {
        switch(address) {
            case 0x15:
                return myAUDC[0];
            case 0x16:
                return myAUDC[1];
            case 0x17:
                return myAUDF[0];
            case 0x18:
                return myAUDF[1];
            case 0x19:
                return myAUDV[0];
            case 0x1a:
                return myAUDV[1];
            default:
                return 0;
        }
    }

    private void setFutureAudioRegister(char address, int value) {
        switch(address) {
            case 0x15:
                myFutureAUDC[0] = value & 0x0f;
                break;
            case 0x16:
                myFutureAUDC[1] = value & 0x0f;
                break;
            case 0x17:
                myFutureAUDF[0] = value & 0x1f;
                break;
            case 0x18:
                myFutureAUDF[1] = value & 0x1f;
                break;
            case 0x19:
                myFutureAUDV[0] = value & 0x0f;
                break;
            case 0x1a:
                myFutureAUDV[1] = value & 0x0f;
                break;
            default:
                break;
        }
    }

    private int getFutureAudioRegister(char address) {
        switch(address) {
            case 0x15:
                return myFutureAUDC[0];
            case 0x16:
                return myFutureAUDC[1];
            case 0x17:
                return myFutureAUDF[0];
            case 0x18:
                return myFutureAUDF[1];
            case 0x19:
                return myFutureAUDV[0];
            case 0x1a:
                return myFutureAUDV[1];
            default:
                return 0;
        }
    }

    private boolean bool(int aValue) {
        return (aValue != 0);
    }

    /**
     * This method creates sound samples based on the current settings of the
     * TIA sound registers.
     * @param buffer the array into which the newly calculated byte values are to be placed
     * @param aStartIndex the array index at which the method should start placing the samples
     * @param samples the number of samples to create
     */
    private void synthesizeAudioData(byte[] aPreOutputBuffer, int aStartIndex, int aAudioFrames) {
        int zSamples = aAudioFrames * myChannels;
        int zVolChannelZero = ((myAUDV[0] << 2) * myVolumePercentage) / 100;
        int zVolChannelOne = ((myAUDV[1] << 2) * myVolumePercentage) / 100;
        int zIndex = aStartIndex;
        while (zSamples > 0) {
            for (int c = 0; c < 2; ++c) {
                if ((myFrequencyDivider[c].clock())) {
                    switch(myAUDC[c]) {
                        case 0x00:
                            {
                                myP4[c] = (myP4[c] << 1) | 0x01;
                                break;
                            }
                        case 0x01:
                            {
                                myP4[c] = bool(myP4[c] & 0x0f) ? ((myP4[c] << 1) | ((bool(myP4[c] & 0x08) ? 1 : 0) ^ (bool(myP4[c] & 0x04) ? 1 : 0))) : 1;
                                break;
                            }
                        case 0x02:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if ((myP5[c] & 0x0f) == 0x08) {
                                    myP4[c] = bool(myP4[c] & 0x0f) ? ((myP4[c] << 1) | ((bool(myP4[c] & 0x08) ? 1 : 0) ^ (bool(myP4[c] & 0x04) ? 1 : 0))) : 1;
                                }
                                break;
                            }
                        case 0x03:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if (bool(myP5[c] & 0x10)) {
                                    myP4[c] = bool(myP4[c] & 0x0f) ? ((myP4[c] << 1) | ((bool(myP4[c] & 0x08) ? 1 : 0) ^ (bool(myP4[c] & 0x04) ? 1 : 0))) : 1;
                                }
                                break;
                            }
                        case 0x04:
                            {
                                myP4[c] = (myP4[c] << 1) | (bool(myP4[c] & 0x01) ? 0 : 1);
                                break;
                            }
                        case 0x05:
                            {
                                myP4[c] = (myP4[c] << 1) | (bool(myP4[c] & 0x01) ? 0 : 1);
                                break;
                            }
                        case 0x06:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if ((myP5[c] & 0x0f) == 0x08) {
                                    myP4[c] = (myP4[c] << 1) | (bool(myP4[c] & 0x01) ? 0 : 1);
                                }
                                break;
                            }
                        case 0x07:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if (bool(myP5[c] & 0x10)) {
                                    myP4[c] = (myP4[c] << 1) | (bool(myP4[c] & 0x01) ? 0 : 1);
                                }
                                break;
                            }
                        case 0x08:
                            {
                                myP5[c] = (bool(myP5[c] & 0x1f) || bool(myP4[c] & 0x0f)) ? ((myP5[c] << 1) | ((bool(myP4[c] & 0x08) ? 1 : 0) ^ (bool(myP5[c] & 0x10) ? 1 : 0))) : 1;
                                myP4[c] = (myP4[c] << 1) | (bool(myP5[c] & 0x20) ? 1 : 0);
                                break;
                            }
                        case 0x09:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                myP4[c] = (myP4[c] << 1) | (bool(myP5[c] & 0x20) ? 1 : 0);
                                break;
                            }
                        case 0x0a:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if ((myP5[c] & 0x0f) == 0x08) {
                                    myP4[c] = (myP4[c] << 1) | (bool(myP5[c] & 0x10) ? 1 : 0);
                                }
                                break;
                            }
                        case 0x0b:
                            {
                                myP4[c] = (myP4[c] << 1) | 0x01;
                                break;
                            }
                        case 0x0c:
                            {
                                myP4[c] = (~myP4[c] << 1) | ((!(!bool(myP4[c] & 4) && (bool(myP4[c] & 7)))) ? 0 : 1);
                                break;
                            }
                        case 0x0d:
                            {
                                myP4[c] = (~myP4[c] << 1) | ((!(!bool(myP4[c] & 4) && (bool(myP4[c] & 7)))) ? 0 : 1);
                                break;
                            }
                        case 0x0e:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if ((myP5[c] & 0x0f) == 0x08) {
                                    myP4[c] = (~myP4[c] << 1) | ((!(!bool(myP4[c] & 4) && (bool(myP4[c] & 7)))) ? 0 : 1);
                                }
                                break;
                            }
                        case 0x0f:
                            {
                                myP5[c] = bool(myP5[c] & 0x1f) ? ((myP5[c] << 1) | ((bool(myP5[c] & 0x10) ? 1 : 0) ^ (bool(myP5[c] & 0x04) ? 1 : 0))) : 1;
                                if (bool(myP5[c] & 0x10)) {
                                    myP4[c] = (~myP4[c] << 1) | ((!(!bool(myP4[c] & 4) && (bool(myP4[c] & 7)))) ? 0 : 1);
                                }
                                break;
                            }
                    }
                }
            }
            myOutputCounter += JAVA_SOUND_SAMPLE_RATE;
            if (myChannels == 1) {
                while ((zSamples > 0) && (myOutputCounter >= TIA_SAMPLE_RATE)) {
                    int zChannelZero = (bool(myP4[0] & 8) ? zVolChannelZero : 0);
                    int zChannelOne = (bool(myP4[1] & 8) ? zVolChannelOne : 0);
                    int zBothChannels = zChannelZero + zChannelOne + myVolumeClip;
                    aPreOutputBuffer[zIndex] = (byte) (zBothChannels - 128);
                    myOutputCounter -= TIA_SAMPLE_RATE;
                    zIndex++;
                    zSamples--;
                }
            } else {
                while ((zSamples > 0) && (myOutputCounter >= TIA_SAMPLE_RATE)) {
                    int zChannelZero = (bool(myP4[0] & 8) ? zVolChannelZero : 0) + myVolumeClip;
                    int zChannelOne = (bool(myP4[1] & 8) ? zVolChannelOne : 0) + myVolumeClip;
                    aPreOutputBuffer[zIndex] = (byte) (zChannelZero - 128);
                    zIndex++;
                    zSamples--;
                    aPreOutputBuffer[zIndex] = (byte) (zChannelOne - 128);
                    zIndex++;
                    zSamples--;
                    myOutputCounter -= TIA_SAMPLE_RATE;
                }
            }
        }
    }

    /**
     * This takes register values off the register queue and submits them to the process of
     * sample creation by calling the process(...) method.
     *
     * This is called by the doFrameAudio() method, which is called once per frame.
     * <b>
     * When the ROM tells the CPU to poke the sound registers in the TIA, the TIA forwards
     * the data to this class (to the set() method).  Instead of setting the register
     * variables immediately, it saves the poke data as a AudioRegisterPoke object and puts it
     * in a queue object.  The processPokeQueue method takes these one-by-one off the front
     * of the queue and, one-by-one, sets the register variables, and calls the process(...)
     * method, creating a sound with a sample length corresponding to the number of
     * processor cycles that elapsed between it and the sound register change after
     * it.
     * @return total number of samples created
     */
    private int processPokeQueue() {
        int zCurrentBufferIndex = 0;
        boolean zEndOfFrame = false;
        while (zEndOfFrame == false) {
            AudioRegisterPoke zRW = null;
            if ((zRW == null)) {
                zEndOfFrame = true;
            }
            if (zRW != null) {
                if (zRW.myAddr != 0) {
                    setAudioRegister((char) zRW.myAddr, zRW.myByteValue);
                }
                myCyclePool += zRW.myDeltaCycles;
                double zAudioFramesInPool = myCyclePool / myAdjustedCyclesPerAudioFrame;
                int zWholeAudioFramesInPool = (int) (zAudioFramesInPool);
                myCyclePool = myCyclePool - ((double) zWholeAudioFramesInPool * myAdjustedCyclesPerAudioFrame);
                synthesizeAudioData(myPreOutputBuffer, zCurrentBufferIndex, zWholeAudioFramesInPool);
                zCurrentBufferIndex += (zWholeAudioFramesInPool * myChannels);
            }
            AudioRegisterPoke zNextARP = null;
            if ((zNextARP == null) || (zNextARP.myFrameEnd == true)) {
                zEndOfFrame = true;
            }
        }
        return zCurrentBufferIndex;
    }

    /**
     * A very imperfect method that plays the sound information that has accumulated over
     * the past video frame.
     * @param aCycles the current CPU system cycle count
     * @param aFPS the console's framerate, in frames per second
     */
    protected void doFrameAudio(int aCycles, int aFPS) {
        if (isSoundEnabled() == true) {
            addPokeToQueue(true, aCycles, 0, 0);
            int zSamples = processPokeQueue();
            int zBufferSize = 0;
            int zAvailable = 0;
            int zInBuffer = zBufferSize - zAvailable;
            double zPercentFull = 100.0 * ((double) zInBuffer / (double) zBufferSize);
            int zToPlay = Math.min(myBufferCushion - zInBuffer, zSamples);
            zToPlay = Math.max(0, zToPlay);
            if (myChannels == 2) {
                zToPlay = roundToEven(zToPlay);
            }
        }
    }

    private static int roundToEven(int aNumber) {
        return (aNumber % 2 != 0) ? (aNumber - 1) : aNumber;
    }

    private void clearPokeQueue() {
        addPokeToQueue(true, 0, 0, 0);
    }

    private void addPokeToQueue(boolean aFrameEnd, int aCycleNumber, int aAddress, int aByteValue) {
        int zDeltaCycles = aCycleNumber - myPreviousCycle;
        if (myPreviousPoke != null) {
            myPreviousPoke.myDeltaCycles = zDeltaCycles;
        }
        int zValueToOverwrite = getFutureAudioRegister((char) aAddress);
        {
            AudioRegisterPoke zRW = new AudioRegisterPoke(aFrameEnd, aAddress, aByteValue);
            setFutureAudioRegister((char) aAddress, aByteValue);
            myPreviousPoke = zRW;
            myPreviousCycle = aCycleNumber;
        }
    }

    /**
     * This method is called by TIA when it receives a poke command destined for a
     * sound register.  This is the method that converts the data received into a
     * AudioRegisterPoke object and stores that object on a queue for later processing.
     * Check out the processPokeQueue(...) description for details.
     * @param addr address to poke
     * @param aByteValue byte value of the poke
     * @param cycle the CPU system cycle number
     * @see processPokeQueue(int, int, int)
     */
    protected void pokeAudioRegister(int addr, int aByteValue, int cycle) {
        if (isSoundEnabled() == true) {
            addPokeToQueue(false, cycle, addr, aByteValue);
        }
    }

    private class FrequencyDivider {

        private int myDivideByValue = 0;

        private int myCounter = 0;

        public FrequencyDivider() {
            myDivideByValue = myCounter = 0;
        }

        public void set(int divideBy) {
            myDivideByValue = divideBy;
        }

        public boolean clock() {
            myCounter++;
            if (myCounter > myDivideByValue) {
                myCounter = 0;
                return true;
            }
            return false;
        }
    }

    private class AudioRegisterPoke {

        private int myAddr;

        private int myByteValue;

        private int myDeltaCycles = 0;

        private boolean myFrameEnd = false;

        public AudioRegisterPoke(boolean aFrameEnd, int aAddr, int aByteValue) {
            myAddr = aAddr;
            myByteValue = aByteValue;
            myFrameEnd = aFrameEnd;
        }
    }

    public void debugSetReg(char address, int value) {
        setAudioRegister(address, value);
    }
}
