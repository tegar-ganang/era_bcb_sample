package com.sun.mmedia;

import com.sun.mmedia.DefaultConfiguration;
import com.sun.midp.main.*;
import javax.microedition.media.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.microedition.media.control.*;
import java.util.*;
import com.sun.mmedia.DefaultConfiguration;
import com.sun.midp.log.Logging;
import com.sun.midp.log.LogChannels;
import com.sun.midp.main.*;

/**
 * Java Tone Sequence Player
 * it implements ToneControl
 */
public final class DirectTone extends DirectPlayer implements ToneControl {

    /**
     * It does not need data source
     */
    public DirectTone() {
        hasDataSource = false;
    }

    /**
     * the worker method to realize the player
     *
     * @exception  MediaException  Description of the Exception
     */
    protected void doRealize() throws MediaException {
        int isolateId = MIDletSuiteUtils.getIsolateId();
        if (this.source == null) {
            hNative = nInit(isolateId, pID, Manager.TONE_DEVICE_LOCATOR, Manager.TONE_DEVICE_LOCATOR, -1);
        } else {
            hNative = nInit(isolateId, pID, DefaultConfiguration.MIME_AUDIO_TONE, source.getLocator(), -1);
        }
        if (hNative == 0) {
            throw new MediaException("Unable to realize tone player");
        }
        if (stream == null) {
            return;
        }
        int chunksize = 128;
        byte[] tmpseqs = new byte[chunksize];
        byte[] seqs = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(chunksize);
        try {
            int read;
            while ((read = stream.read(tmpseqs, 0, chunksize)) != -1) {
                baos.write(tmpseqs, 0, read);
            }
            seqs = baos.toByteArray();
            baos.close();
            tmpseqs = null;
            System.gc();
        } catch (IOException ex) {
            throw new MediaException("unable to realize: fail to read from source");
        }
        try {
            this.setSequence(seqs);
        } catch (Exception e) {
            throw new MediaException("unable to realize: " + e.getMessage());
        }
    }

    /**
     * The worker method to actually obtain the control.
     *
     * @param  type  the class name of the <code>Control</code>.
     * @return       <code>Control</code> for the class or interface
     * name.
     */
    protected Control doGetControl(String type) {
        Control c = super.doGetControl(type);
        if (c != null) return c;
        if (getState() >= REALIZED) {
            if (type.equals("javax.microedition.media.control.ToneControl")) {
                return this;
            }
        }
        return null;
    }

    /**
     * Override getContentType from BasicPlayer
     * Always return DefaultConfiguration.TONE content type
     */
    public String getContentType() {
        chkClosed(true);
        return DefaultConfiguration.MIME_AUDIO_TONE;
    }

    /**
     * Sets the tone sequence.<p>
     * 
     * @param sequence The sequence to set.
     * @exception IllegalArgumentException Thrown if the sequence is 
     * <code>null</code> or invalid.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * that this control belongs to is in the <i>PREFETCHED</i> or
     * <i>STARTED</i> state.
     */
    public void setSequence(byte[] sequence) {
        if (this.getState() >= Player.PREFETCHED) throw new IllegalStateException("cannot set seq after prefetched");
        if (sequence == null) throw new IllegalArgumentException("null sequence");
        nFlushBuffer(hNative);
        nBuffering(hNative, sequence, sequence.length);
        if (-1 == nBuffering(hNative, sequence, -1)) throw new IllegalArgumentException("invalid sequence");
        hasToneSequenceSet = true;
    }
}
