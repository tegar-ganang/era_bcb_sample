package com.frinika.sequencer.model.audio;

import com.frinika.audio.io.AudioReader;
import com.frinika.audio.toot.SynchronizedAudioProcess;
import java.io.IOException;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.server.AudioServer;
import com.frinika.sequencer.FrinikaSequencer;
import com.frinika.sequencer.SongPositionListener;

public class AudioStreamVoice extends SynchronizedAudioProcess {

    AudioReader ais;

    boolean running = false;

    byte[] byteBuffer = null;

    int nChannel;

    long clipStartPositionInMillis;

    long clipStartPositionInFrames;

    long framePos = -1;

    private float sampleRate;

    /**
	 * Construct a DAAudioStreamVoice. This is an extension of the
	 * SynchronizedVoice which uses a sequencer as synchronization source.
	 * 
	 * The extended SynchronizedVoice class requires that we initially provide
	 * an offset for where in the clip to start (initialFramePos parameter), but
	 * the synchronization will then correct the position as the clip is
	 * playing. Thus we'll use the same formula as the synchronization to
	 * calculate the initialFramePos.
	 * 
	 * @param voiceServer -
	 *            The voice server we're playing in
	 * @param sequencer -
	 *            the sequencer that we are playing in
	 * @param ais -
	 *            The audio clip input stream
	 * @param clipStartTimePosition -
	 *            The start time in microseconds relative to Start time relative
	 *            to sequencer zero time
	 * @param modulator
	 *            Evelope for the audio (can be null)
	 * @throws Exception
	 */
    public AudioStreamVoice(final AudioServer audioServer, final FrinikaSequencer sequencer, final AudioReader ais, final long clipStartTimePosition1) throws Exception {
        super(audioServer, 0);
        this.sampleRate = audioServer.getSampleRate();
        this.ais = ais;
        setRealStartTime(clipStartTimePosition1);
        nChannel = ais.getFormat().getChannels();
        sequencer.addSongPositionListener(new SongPositionListener() {

            public void notifyTickPosition(long tick) {
                setRunning(sequencer.isRunning());
                setFramePos(getFramePos(sequencer, audioServer, clipStartPositionInMillis));
            }

            public boolean requiresNotificationOnEachTick() {
                return false;
            }
        });
    }

    long milliToFrame(double t) {
        return (long) ((t * sampleRate) / 1000000);
    }

    private static long getFramePos(FrinikaSequencer sequencer, AudioServer audioServer, long clipStartTimePosition) {
        return (long) (((sequencer.getMicrosecondPosition() - clipStartTimePosition) * audioServer.getSampleRate()) / 1000000);
    }

    /**
	 * Tell the voice whether to play or not (if the sequencer is running)
	 */
    public void setRunning(final boolean running) {
        AudioStreamVoice.this.running = running;
    }

    @Override
    public void processAudioSynchronized(AudioBuffer buffer) {
        if (!running) return;
        boolean realTime = buffer.isRealTime();
        if (byteBuffer == null || byteBuffer.length != buffer.getSampleCount() * 2 * nChannel) byteBuffer = new byte[buffer.getSampleCount() * 2 * nChannel];
        long seekPos = getFramePos();
        if (seekPos != framePos) {
            try {
                ais.seekFrame(seekPos, realTime);
                framePos = seekPos;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        ais.processAudio(buffer);
        if (ais.getChannels() == 1) {
            buffer.copyChannel(0, 1);
        }
        framePos += buffer.getSampleCount();
    }

    public void setRealStartTime(long realStartTime) {
        clipStartPositionInMillis = realStartTime;
        clipStartPositionInFrames = (long) ((clipStartPositionInMillis * sampleRate) / 1000000);
    }

    public void close() {
    }

    public void open() {
    }
}
