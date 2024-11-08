package com.sts.webmeet.content.client.audio;

import java.io.IOException;
import com.sts.webmeet.content.common.audio.WHAudioFormat;

public class MicDataSource implements ConfigurableAtomicDataSource {

    private MicDataSource() {
    }

    public MicDataSource(Microphone mic, WHAudioFormat format) {
        this.audioFormat = format;
        this.mic = mic;
    }

    public int setAtomSize(int iAtomSize) {
        this.iAtomSize = iAtomSize;
        this.baAtom = new byte[iAtomSize];
        baAudioData = new byte[iAtomSize * this.iBufferSizeInAtoms];
        return 0;
    }

    public int getAtomSize() {
        return iAtomSize;
    }

    public byte[] getData() throws IOException {
        if (bClosePending) {
            throw new IOException("close pending");
        }
        if (0 == this.iAtomCursor) {
            mic.getBuffer(baAudioData, 0, baAudioData.length);
        }
        System.arraycopy(baAudioData, this.iAtomCursor * this.iAtomSize, this.baAtom, 0, this.baAtom.length);
        this.iAtomCursor++;
        if (this.iAtomCursor == this.iBufferSizeInAtoms) {
            this.iAtomCursor = 0;
        }
        return this.baAtom;
    }

    boolean openMic() {
        bClosePending = false;
        boolean bResult = mic.open(audioFormat.getSamplesPerSecond(), audioFormat.getChannelCount(), audioFormat.getBitsPerSample(), this.iBufferSizeInAtoms * iAtomSize, iBufferCount);
        return bResult;
    }

    public void interruptConsumers() {
        bClosePending = true;
        mic.close();
    }

    private boolean bClosePending;

    private WHAudioFormat audioFormat;

    private Microphone mic;

    private int iAtomSize;

    private int iBufferSizeInAtoms = 10;

    private int iAtomCursor = 0;

    private byte[] baAudioData;

    private byte[] baAtom;

    private int iBufferCount = 3;
}
