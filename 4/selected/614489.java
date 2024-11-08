package espider.libs.com.inzyme.jtrm;

import java.io.IOException;
import espider.libs.com.inzyme.jtrm.typeconv.LittleEndianByteBuffer;
import espider.libs.com.inzyme.jtrm.typeconv.LittleEndianShortBuffer;
import espider.libs.com.inzyme.jtrm.typeconv.ShortArrayPointer;
import espider.libs.com.inzyme.jtrm.util.DoubleDeque;
import espider.libs.com.inzyme.jtrm.util.FloatPriorityQueue;
import espider.libs.com.inzyme.jtrm.util.MathUtils;

public class TRM {

    private static final boolean DEBUG = false;

    public static final int FFT_POINTS = 64;

    public static final int NUM_SAMPLES_NEEDED = 288000;

    private int myBitsPerSample;

    private int mySamplesPerSecond;

    private int myNumberOfChannels;

    private int myDownmixSize;

    private int myFinishedFFTs;

    private short[] myDownmixBuffer;

    private short[] myStoreBuffer;

    private int myNumBytesNeeded;

    private int myNumBytesWritten;

    private int myNumSamplesWritten;

    private double[] myFWin = new double[64];

    private double[] myFFTBuffer = new double[64];

    private double[] myFFTBuffer2 = new double[64];

    private double[] myFreqs = new double[32];

    private float[] myLastFFT = new float[32];

    private float[] myBeatStore;

    private int myBeatIndex;

    private String myProxy;

    private int myProxyPort;

    private long mySongSamples;

    private long mySongSeconds;

    public TRM() {
        myProxy = "";
        myProxyPort = 80;
        mySongSeconds = -1;
    }

    public void setProxy(String _proxyAddr, int _proxyPort) {
        myProxy = _proxyAddr;
        myProxyPort = _proxyPort;
    }

    public void setPCMDataInfo(int _samplesPerSecond, int _numChannels, int _bitsPerSample) {
        mySamplesPerSecond = _samplesPerSecond;
        myNumberOfChannels = _numChannels;
        myBitsPerSample = _bitsPerSample;
        myDownmixBuffer = null;
        myStoreBuffer = null;
        myNumSamplesWritten = 0;
        double mult = mySamplesPerSecond / 11025.0;
        mult *= (myBitsPerSample / 8);
        mult *= (myNumberOfChannels);
        mult = Math.ceil(mult);
        myNumBytesWritten = 0;
        myNumBytesNeeded = NUM_SAMPLES_NEEDED * (int) mult;
        myStoreBuffer = new short[myNumBytesNeeded + 20];
        mySongSamples = 0;
        mySongSeconds = -1;
    }

    public void setSongLength(long _seconds) {
        mySongSeconds = _seconds;
    }

    public boolean generateSignature(LittleEndianShortBuffer _data, int _size) {
        if (myNumBytesWritten < myNumBytesNeeded) {
            int i = 0;
            while (i < _size && myNumBytesWritten < myNumBytesNeeded) {
                if (myBitsPerSample == 8) {
                    if (myNumBytesWritten == 0 && (Math.abs(_data.getByte(i)) == 0)) {
                    } else {
                        myStoreBuffer[myNumBytesWritten] = _data.getByte(i);
                        myNumBytesWritten++;
                    }
                    i++;
                } else {
                    if (myNumBytesWritten == 0 && (Math.abs(_data.getByte(i)) == 0) && (Math.abs(_data.getByte(i + 1)) == 0)) {
                    } else {
                        myStoreBuffer[myNumBytesWritten] = _data.getByte(i);
                        myNumBytesWritten++;
                        myStoreBuffer[myNumBytesWritten] = _data.getByte(i + 1);
                        myNumBytesWritten++;
                    }
                    i += 2;
                }
            }
        }
        if (myBitsPerSample == 8) {
            mySongSamples += _size;
        } else {
            mySongSamples += _size / 2;
        }
        if (myNumBytesWritten < myNumBytesNeeded) {
            return false;
        }
        if (mySongSeconds > 0) {
            return true;
        }
        return false;
    }

    public void downmixPCM() {
        int lsum = 0;
        int rsum = 0;
        int numsamps = 0;
        int lDC = 0;
        int rDC = 0;
        short lsample;
        short rsample;
        int readpos = 0;
        if (myBitsPerSample == 16) {
            if (myNumberOfChannels == 2) {
                LittleEndianByteBuffer storeBuffer = new LittleEndianByteBuffer(myStoreBuffer);
                while (readpos < (myNumBytesWritten / 2)) {
                    lsample = storeBuffer.getShort(readpos++);
                    rsample = storeBuffer.getShort(readpos++);
                    lsum += lsample;
                    rsum += rsample;
                    numsamps++;
                }
                lDC = -(lsum / numsamps);
                rDC = -(rsum / numsamps);
                readpos = 0;
                while (readpos < (myNumBytesWritten / 2)) {
                    storeBuffer.setShort(readpos, (short) (storeBuffer.getShort(readpos) + lDC));
                    readpos++;
                    storeBuffer.setShort(readpos, (short) (storeBuffer.getShort(readpos) + rDC));
                    readpos++;
                }
            } else {
                LittleEndianByteBuffer storeBuffer = new LittleEndianByteBuffer(myStoreBuffer);
                while (readpos < myNumBytesWritten / 2) {
                    lsample = storeBuffer.getShort(readpos++);
                    lsum += lsample;
                    numsamps++;
                }
                lDC = -(lsum / numsamps);
                readpos = 0;
                while (readpos < myNumBytesWritten / 2) {
                    storeBuffer.setShort(readpos, (short) (storeBuffer.getShort(readpos) + lDC));
                    readpos++;
                }
            }
        } else {
            if (myNumberOfChannels == 2) {
                while (readpos < (myNumBytesWritten)) {
                    lsample = myStoreBuffer[readpos++];
                    rsample = myStoreBuffer[readpos++];
                    lsum += lsample;
                    rsum += rsample;
                    numsamps++;
                }
                lDC = -(lsum / numsamps);
                rDC = -(rsum / numsamps);
                readpos = 0;
                while (readpos < (myNumBytesWritten)) {
                    myStoreBuffer[readpos] = (short) (myStoreBuffer[readpos] + lDC);
                    readpos++;
                    myStoreBuffer[readpos] = (short) (myStoreBuffer[readpos] + rDC);
                    readpos++;
                }
            } else {
                while (readpos < myNumBytesWritten) {
                    lsample = myStoreBuffer[readpos++];
                    lsum += lsample;
                    numsamps++;
                }
                lDC = -(lsum / numsamps);
                readpos = 0;
                while (readpos < myNumBytesWritten) {
                    myStoreBuffer[readpos] = (short) (myStoreBuffer[readpos] + lDC);
                    readpos++;
                }
            }
        }
        if (myDownmixBuffer == null) {
            myDownmixBuffer = new short[NUM_SAMPLES_NEEDED];
        }
        myDownmixSize = myNumBytesWritten;
        if (mySamplesPerSecond != 11025) {
            myDownmixSize = (int) ((float) myDownmixSize * (11025.0 / (float) mySamplesPerSecond));
        }
        if (myBitsPerSample == 16) {
            myDownmixSize /= 2;
        }
        if (myNumberOfChannels != 1) {
            myDownmixSize /= 2;
        }
        int maxwrite = myDownmixSize;
        int writepos = 0;
        float rateChange = mySamplesPerSecond / 11025.0f;
        if (myBitsPerSample == 8) {
            LittleEndianByteBuffer tempbuf = new LittleEndianByteBuffer(new short[myNumBytesWritten]);
            readpos = 0;
            while (readpos < myNumBytesWritten) {
                int samp = myStoreBuffer[readpos];
                samp = (samp - 128) * 256;
                if (samp >= Short.MAX_VALUE) {
                    samp = Short.MAX_VALUE;
                } else if (samp <= Short.MIN_VALUE) {
                    samp = Short.MIN_VALUE;
                }
                tempbuf.setShort(readpos, (short) samp);
                readpos++;
            }
            myNumBytesWritten *= 2;
            myStoreBuffer = tempbuf.getByteBuffer();
            myBitsPerSample = 16;
        }
        if (myNumberOfChannels == 2) {
            LittleEndianByteBuffer tempbuf = new LittleEndianByteBuffer(new short[myNumBytesWritten / 2]);
            LittleEndianByteBuffer storeBuffer = new LittleEndianByteBuffer(myStoreBuffer);
            readpos = 0;
            writepos = 0;
            while (writepos < myNumBytesWritten / 4) {
                long ls = storeBuffer.getShort(readpos++);
                long rs = storeBuffer.getShort(readpos++);
                tempbuf.setShort(writepos, (short) ((ls + rs) / 2));
                writepos++;
            }
            myNumBytesWritten /= 2;
            myStoreBuffer = tempbuf.getByteBuffer();
        }
        LittleEndianByteBuffer storeBuffer = new LittleEndianByteBuffer(myStoreBuffer);
        writepos = 0;
        while ((writepos < maxwrite) && (myNumSamplesWritten < NUM_SAMPLES_NEEDED)) {
            readpos = (int) ((float) writepos * rateChange);
            short ls = storeBuffer.getShort(readpos++);
            myDownmixBuffer[myNumSamplesWritten] = ls;
            myNumSamplesWritten++;
            writepos++;
        }
        myStoreBuffer = null;
    }

    public int countBeats() {
        int i, j;
        float maxpeak = 0;
        float minimum = 99999;
        boolean isbeat;
        int lastbeat = 0;
        for (i = 0; i < myBeatIndex; i++) {
            if (myBeatStore[i] < minimum) {
                minimum = myBeatStore[i];
            }
        }
        for (i = 0; i < myBeatIndex; i++) {
            myBeatStore[i] -= minimum;
        }
        for (i = 0; i < myBeatIndex; i++) {
            if (myBeatStore[i] > maxpeak) {
                maxpeak = myBeatStore[i];
            }
        }
        int beats = 0;
        maxpeak *= (float) 0.80;
        for (i = 3; i < (myBeatIndex - 4); i++) {
            if (myBeatStore[i] > maxpeak) {
                if (i > lastbeat + 14) {
                    isbeat = true;
                    for (j = i - 3; j < i; j++) {
                        if (myBeatStore[j] > myBeatStore[i]) {
                            isbeat = false;
                        }
                    }
                    for (j = i + 1; j < i + 4; j++) {
                        if (myBeatStore[j] > myBeatStore[i]) {
                            isbeat = false;
                        }
                    }
                    if (isbeat) {
                        beats++;
                        lastbeat = i;
                    }
                }
            }
        }
        return beats;
    }

    public byte[] finalizeSignature(String _collID) throws IOException {
        if (myNumBytesWritten < 2) {
            throw new IllegalStateException("You must have written more than 2 bytes to get a signature.");
        }
        downmixPCM();
        short[] sample = myDownmixBuffer;
        boolean lastNeg = false;
        if (sample == null) {
            throw new IllegalStateException("There are no samples in the downmix buffer.");
        }
        if (sample[0] <= 0) {
            lastNeg = true;
        }
        ShortArrayPointer pCurrent = new ShortArrayPointer(myDownmixBuffer);
        ShortArrayPointer pBegin = new ShortArrayPointer(myDownmixBuffer);
        int iFFTs = (myNumSamplesWritten / 32) - 2;
        int j, k, q;
        float[] fSpectrum = new float[32];
        float[] fAvgFFTDelta = new float[32];
        for (j = 0; j < 32; j++) {
            myLastFFT[j] = 0;
        }
        int iZeroCrossings = 0;
        long sum = 0;
        long sumsquared = 0;
        int iFinishedFFTs = 0;
        float[] energys = new float[10];
        int energySub = 0;
        int energyCounter = 0;
        double mag = 0;
        double tempf = 0;
        float bandDelta = 0;
        float beatavg = 0;
        FFT fft = new FFT(FFT_POINTS, 11025);
        myBeatStore = new float[iFFTs + 2];
        myBeatIndex = 0;
        float[] haar = new float[FFT_POINTS];
        HaarWavelet wavelet = new HaarWavelet(64, 6);
        for (j = 0; j < iFFTs; j++) {
            for (k = 0; k < FFT_POINTS; k++) {
                myFFTBuffer[k] = pCurrent.get(k);
                myFFTBuffer2[k] = pCurrent.get(k + 32);
            }
            wavelet.transform(myFFTBuffer);
            for (k = 0; k < 64; k++) {
                haar[k] += wavelet.getCoef(k);
            }
            wavelet.transform(myFFTBuffer2);
            for (k = 0; k < 64; k++) {
                haar[k] += wavelet.getCoef(k);
            }
            for (k = 0; k < FFT_POINTS; k++) {
                myFFTBuffer[k] = pCurrent.get(k);
                myFFTBuffer2[k] = pCurrent.get(k + 32);
            }
            fft.copyIn2(myFFTBuffer, myFFTBuffer2, FFT_POINTS);
            fft.transform();
            for (k = 0; k < 32; k++) {
                mag = fft.getPower1(k);
                if (mag <= 0) {
                    myFreqs[k] = 0;
                } else {
                    myFreqs[k] = MathUtils.log10(mag / 4096) + 6;
                }
                myFreqs[k] = myFreqs[k] * 6;
            }
            for (k = 0; k < 32; k++) {
                tempf = myFreqs[k];
                bandDelta = Math.abs((float) myFreqs[k] - myLastFFT[k]);
                if (k == 2) {
                    beatavg = (float) (tempf + myFreqs[k - 1]) * 5;
                    myBeatStore[myBeatIndex] = beatavg;
                    myBeatIndex++;
                }
                fSpectrum[k] += tempf;
                fAvgFFTDelta[k] += bandDelta;
                myLastFFT[k] = (float) myFreqs[k];
            }
            j++;
            for (k = 0; k < 32; k++) {
                mag = fft.getPower2(k);
                if (mag <= 0) {
                    myFreqs[k] = 0;
                } else {
                    myFreqs[k] = MathUtils.log10(mag / 4096) + 6;
                }
                myFreqs[k] = myFreqs[k] * 6;
            }
            for (k = 0; k < 32; k++) {
                tempf = myFreqs[k];
                bandDelta = Math.abs((float) myFreqs[k] - myLastFFT[k]);
                if (k == 2) {
                    beatavg = (float) (tempf + myFreqs[k - 1]) * 5;
                    myBeatStore[myBeatIndex] = beatavg;
                    myBeatIndex++;
                }
                fSpectrum[k] += tempf;
                fAvgFFTDelta[k] += bandDelta;
                myLastFFT[k] = (float) myFreqs[k];
            }
            iFinishedFFTs += 2;
            while (pCurrent.getIndex() < pBegin.getIndex() + FFT_POINTS) {
                short value = pCurrent.getCurrent();
                double energy = (value * value);
                sum += Math.abs(value);
                sumsquared += (long) energy;
                energys[energySub] += energy;
                energyCounter++;
                if (energyCounter >= 1000 * 32) {
                    energys[energySub] = energys[energySub] / energyCounter;
                    energyCounter = 0;
                    energySub++;
                }
                if (lastNeg && (pCurrent.getCurrent() > 0)) {
                    lastNeg = false;
                    iZeroCrossings++;
                } else if (!lastNeg && (pCurrent.getCurrent() <= 0)) {
                    lastNeg = true;
                }
                pCurrent.incrementIndex();
            }
            pBegin.setIndex(pCurrent.getIndex());
        }
        if (energyCounter != 0 && energySub < 9) {
            energys[energySub] = energys[energySub] / energyCounter;
        }
        if (energySub >= 9) {
            energySub = 8;
        }
        FFT fft1 = new FFT(512, 1);
        FFT fft2 = new FFT(512, 1);
        double[] dbuffer = new double[512];
        DoubleDeque f2buffer = new DoubleDeque();
        int i, f1count = 0, f2count = 0;
        float[] f2Spec = new float[32];
        for (j = 0; j < myNumSamplesWritten; ) {
            for (i = 0; i < 512; i++) {
                dbuffer[i] = myDownmixBuffer[j];
            }
            fft1.copyIn(dbuffer, 512);
            fft1.transform();
            f2buffer.pushBack(fft1.getLogPower(3));
            if (f2buffer.size() == 512) {
                f1count++;
            }
            if (f1count > 0 && (f1count % 44) == 0) {
                double avg = 0;
                for (i = 0; i < 512; i++) {
                    avg += (dbuffer[i] = f2buffer.get(i));
                }
                avg /= 512;
                for (i = 0; i < 512; i++) {
                    dbuffer[i] -= avg;
                }
                fft2.copyIn(dbuffer, 512);
                fft2.transform();
                for (i = 0; i < 32; i++) {
                    f2Spec[i] += fft2.getLogPower(i);
                }
                f2count++;
            }
            if (f2buffer.size() == 512) {
                f2buffer.popFront();
            }
            j += 64;
        }
        fft1 = null;
        fft2 = null;
        dbuffer = null;
        for (i = 0; i < 32; i++) {
            f2Spec[i] /= f2count;
        }
        float fLength = myNumSamplesWritten / (float) 11025;
        float fAverageZeroCrossing = iZeroCrossings / fLength;
        float smallest = 9999;
        for (j = 0; j < 32; j++) {
            fSpectrum[j] = fSpectrum[j] / iFinishedFFTs;
            if ((j <= 28) && (fSpectrum[j] < smallest)) {
                smallest = fSpectrum[j];
            }
        }
        for (j = 0; j < 32; j++) {
            fSpectrum[j] = fSpectrum[j] - smallest;
            if (fSpectrum[j] < 0) {
                fSpectrum[j] = 0;
            }
        }
        smallest = 9999;
        FloatPriorityQueue haarList = new FloatPriorityQueue(64);
        for (j = 0; j < 64; j++) {
            haar[j] = haar[j] / iFinishedFFTs;
            if (Math.abs(haar[j]) < smallest) {
                smallest = Math.abs(haar[j]);
            }
        }
        for (j = 0; j < 64; j++) {
            if (haar[j] > 0) {
                haar[j] = haar[j] - smallest;
            } else {
                haar[j] = haar[j] + smallest;
            }
            if (Math.abs(haar[j]) < 1) {
                haar[j] = 0;
            } else {
                haar[j] = 20 * MathUtils.log10(Math.abs(haar[j]));
            }
            haarList.push(haar[j]);
        }
        for (j = 0; j < 64; j++) {
            haar[j] = haarList.top();
            haarList.pop();
        }
        double RMS = Math.sqrt((float) sumsquared / (float) myNumSamplesWritten);
        double avg = ((float) sum / (float) myNumSamplesWritten);
        float msratio = (float) (avg / RMS);
        if (DEBUG) {
            System.out.println("sumsquared = " + sumsquared);
            System.out.println("numSamplesWritten = " + myNumSamplesWritten);
            System.out.println("sum = " + sum);
            System.out.println("avg = " + avg);
            System.out.println("RMS = " + RMS);
            System.out.println("msratio  = " + msratio);
        }
        float specsum = 0;
        for (j = 0; j < 31; j++) {
            specsum += Math.abs(fSpectrum[j + 1] - fSpectrum[j]);
        }
        for (j = 0; j < 32; j++) {
            fAvgFFTDelta[j] = fAvgFFTDelta[j] / (iFinishedFFTs - 1);
        }
        int[] energydiffs = new int[8];
        for (q = 0; q < energySub; q++) {
            energydiffs[q] = (int) (energys[q + 1] - energys[q]);
        }
        float avgdiff = 0;
        int numsignchanges = 0;
        boolean lastdiffneg = (energydiffs[0] < 0);
        for (q = 0; q < 8; q++) {
            avgdiff += energydiffs[q];
            if (lastdiffneg && energydiffs[q] > 0) {
                switch(q) {
                    case 0:
                    case 1:
                        numsignchanges |= (1 << 0);
                        break;
                    case 2:
                    case 3:
                        numsignchanges |= (1 << 1);
                        break;
                    case 4:
                    case 5:
                        numsignchanges |= (1 << 2);
                        break;
                    case 6:
                        numsignchanges |= (1 << 3);
                        break;
                    default:
                        numsignchanges |= (1 << 4);
                        break;
                }
                lastdiffneg = false;
            } else if (!lastdiffneg && energydiffs[q] <= 0) {
                lastdiffneg = true;
            }
        }
        avgdiff /= 8;
        int beats = countBeats();
        float estBPM = beats;
        if (mySongSeconds == -1) {
            mySongSeconds = (int) (Math.ceil((mySongSamples * 1.0 / myNumberOfChannels) / mySamplesPerSecond));
        }
        if (DEBUG) {
            System.out.print(fLength + " " + msratio + " " + fAverageZeroCrossing + " ");
            System.out.print(estBPM + " " + avgdiff + " " + numsignchanges + " : ");
            for (j = 0; j < 32; j++) {
                System.out.print(fSpectrum[j] + " ");
            }
            System.out.print(" : ");
            for (j = 0; j < 32; j++) {
                System.out.print(fAvgFFTDelta[j] + " ");
            }
            System.out.print(" : " + specsum + " : ");
            for (j = 0; j < 64; j++) {
                System.out.print(haar[j] + " ");
            }
        }
        AudioSig signature = new AudioSig(msratio, fAverageZeroCrossing, f2Spec, specsum, estBPM, fAvgFFTDelta, haar, avgdiff, numsignchanges, mySongSeconds);
        SigClient sigClient = new SigClient("trm.musicbrainz.org", 4447);
        sigClient.setProxy(myProxy, myProxyPort);
        if (_collID == null || _collID.equals("")) {
            _collID = "EMPTY_COLLECTION";
        }
        byte[] guid = sigClient.getSignature(signature, _collID);
        myDownmixBuffer = null;
        myNumSamplesWritten = 0;
        return guid;
    }

    public String convertSigToASCII(byte[] _sig) {
        UUID uuid = new UUID(_sig);
        return uuid.toString();
    }
}
