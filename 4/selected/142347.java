package com.sun.mmedia;

import javax.microedition.media.*;
import javax.microedition.media.control.*;
import com.sun.mmedia.ABBBasicPlayer;
import java.io.*;

public class QSoundABBToneSequencePlayer extends ABBBasicPlayer implements Runnable {

    private QSoundABBMIDIPlayControl qsmc;

    private Object playLock = new Object();

    private Thread playThread;

    private boolean stopped;

    private QSoundABBToneCtrl tctrl;

    private final int bufferSize = 2048;

    public QSoundABBToneSequencePlayer() {
        qsmc = new QSoundABBMIDIPlayControl(this);
        tctrl = new QSoundABBToneCtrl(this);
    }

    /**
     * Subclasses need to implement this to realize
     * the <code>Player</code>.
     *
     * @exception  MediaException  Description of the Exception
     */
    protected void doRealize() throws MediaException {
        qsmc.open();
        stopped = true;
        if (source != null) {
            int count;
            byte[] b = new byte[bufferSize];
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
            try {
                while ((count = source.read(b, 0, bufferSize)) > 0) baos.write(b, 0, count);
                boolean r = qsmc.fillBuffer(baos.toByteArray());
                baos.close();
                if (!r) throw new MediaException("Bad Tone Format");
            } catch (IOException ioe) {
                throw new MediaException("Failure occured with read stream");
            }
            baos = null;
        }
    }

    /**
     * Subclasses need to implement this to prefetch
     * the <code>Player</code>.
     *
     * @exception  MediaException  Description of the Exception
     */
    protected void doPrefetch() throws MediaException {
    }

    /**
     * Subclasses need to implement this start
     * the <code>Player</code>.
     *
     * @return    Description of the Return Value
     */
    protected boolean doStart() {
        if (!stopped) try {
            doStop();
        } catch (MediaException me) {
        }
        ;
        stopped = false;
        synchronized (playLock) {
            playThread = new Thread(this);
            playThread.start();
            try {
                playLock.wait();
            } catch (InterruptedException ie) {
            }
            ;
        }
        return true;
    }

    /**
     * Subclasses need to implement this to realize
     * the <code>Player</code>.
     *
     * @exception  MediaException  Description of the Exception
     */
    protected void doStop() throws MediaException {
        qsmc.stop();
        stopped = true;
        synchronized (playLock) {
            try {
                playThread.join();
            } catch (InterruptedException ie) {
            }
            ;
        }
    }

    /**
     * Subclasses need to implement this to deallocate
     * the <code>Player</code>.
     */
    protected void doDeallocate() {
    }

    /**
     * Subclasses need to implement this to close
     * the <code>Player</code>.
     */
    protected void doClose() {
        if (state != Player.UNREALIZED) {
            if (!stopped) try {
                doStop();
            } catch (MediaException me) {
            }
            ;
            qsmc.close();
            qsmc = null;
        }
    }

    /**
     * Subclasses need to implement this to set the media time
     * of the <code>Player</code>.
     *
     * @param  now                 Description of the Parameter
     * @return                     Description of the Return Value
     * @exception  MediaException  Description of the Exception
     */
    protected long doSetMediaTime(long now) throws MediaException {
        return qsmc.setMediaTime(now);
    }

    /**
     * Subclasses need to implement this to get the media time
     * of the <code>Player</code>
     *
     * @return    Description of the Return Value
     */
    protected long doGetMediaTime() {
        return qsmc.getMediaTime();
    }

    /**
     * Subclasses need to implement this to get the duration
     * of the <code>Player</code>.
     *
     * @return    Description of the Return Value
     */
    protected long doGetDuration() {
        return qsmc.getDuration();
    }

    /**
     * The worker method to actually obtain the control.
     *
     * @param  type  the class name of the <code>Control</code>.
     * @return       <code>Control</code> for the class or interface
     * name.
     */
    protected Control doGetControl(String controlType) {
        Control r = null;
        if ((getState() != UNREALIZED) && controlType.startsWith(ABBBasicPlayer.pkgName)) {
            controlType = controlType.substring(ABBBasicPlayer.pkgName.length());
            if (controlType.equals(ABBBasicPlayer.tocName)) {
                r = tctrl;
            } else {
                r = qsmc.getControl(controlType);
            }
        }
        return r;
    }

    void doNextLoopIteration() {
    }

    void doFinishLoopIteration() {
    }

    protected void doSetLoopCount(int count) {
        qsmc.setLoopCount(count);
    }

    public void run() {
        qsmc.start();
        synchronized (playLock) {
            playLock.notify();
        }
        boolean done = false;
        int numLoopComplete = 0;
        while (!stopped) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
            ;
            done = qsmc.isDone();
            numLoopComplete = qsmc.numLoopComplete();
            if (!done && (numLoopComplete > 0)) {
                Long medt = new Long(qsmc.getMediaTime());
                while (numLoopComplete-- > 0) sendEvent(PlayerListener.END_OF_MEDIA, medt);
            }
            if (done) stopped = true;
        }
        if (done) {
            state = Player.PREFETCHED;
            sendEvent(PlayerListener.END_OF_MEDIA, new Long(qsmc.getMediaTime()));
        }
    }

    boolean setSequence(byte[] seq) throws IllegalArgumentException {
        if (state == UNREALIZED) qsmc.open();
        return qsmc.fillBuffer(seq);
    }

    public String getContentType() {
        chkClosed(true);
        return DefaultConfiguration.MIME_AUDIO_TONE;
    }
}
