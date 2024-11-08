package org.jsresources.apps.jam.audio;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jsresources.apps.jam.Debug;

/**	Timed start facility for audio samples.
 *	This class provides the facility to start an audio stream at a given time
 *	in the future.
 *	[Thread, daemon]
 *	You need not and you cannot instantiate this class. Instead,
 *	it creates one public instance of itself. That instance can be accessed
 *	by <code>StartScheduler.startScheduler</code>.
 *	For example, to start an audio stream after three seconds:
 *
 *	AudioDataStream	audioDataStream = new ...;
 *	StartScheduler.startScheduler.startTimed(audioDataStream, currentTimeMillis() + 3000);
 *
 *	@author Matthias Pfisterer
 */
public class StartScheduler extends Thread {

    /**	A list containing the AudioPlayers which are waiting for start.
	 *	The list is sorted with the one to be started first coming first
	 *	in the list.
	 */
    private List<StartSchedulerEntry> m_vEntries;

    private AudioFormat m_audioFormat;

    private List<AudioChannel> m_audioChannels;

    public StartScheduler(AudioFormat audioFormat) {
        super("StartScheduler");
        m_audioFormat = audioFormat;
        m_vEntries = new ArrayList<StartSchedulerEntry>();
        m_audioChannels = new ArrayList<AudioChannel>();
        setDaemon(true);
        setPriority(7);
        start();
    }

    public void startTimed(int nChannel, URL url, long lStartTime) {
        if (Debug.getTraceStartScheduler()) {
            Debug.out("StartScheduler.startTimed(): called");
        }
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(url);
        } catch (UnsupportedAudioFileException e) {
            Debug.out(e);
        } catch (IOException e) {
            Debug.out(e);
        }
        StartSchedulerEntry entry = new StartSchedulerEntry(audioInputStream, nChannel, lStartTime);
        if (System.currentTimeMillis() >= lStartTime) {
            if (Debug.getTraceStartScheduler()) {
                Debug.out("StartScheduler.startTimed(): starting stream immediately");
            }
            startImmediately(entry);
        } else {
            synchronized (this) {
                int nEntry;
                for (nEntry = 0; nEntry < m_vEntries.size(); nEntry++) {
                    StartSchedulerEntry en = m_vEntries.get(nEntry);
                    if (en.getStartTime() > lStartTime) {
                        break;
                    }
                }
                if (nEntry < m_vEntries.size()) {
                    m_vEntries.add(nEntry, entry);
                } else {
                    m_vEntries.add(entry);
                }
                if (Debug.getTraceStartScheduler()) {
                    Debug.out("StartScheduler.startTimed(): enqueued stream " + entry);
                }
            }
            interrupt();
        }
    }

    private void startImmediately(StartSchedulerEntry entry) {
        if (Debug.getTraceStartScheduler()) {
            Debug.out("StartScheduler.startImmediately(" + entry + "): called");
        }
        AudioInputStream audioInputStream = entry.getAudioInputStream();
        int nChannel = entry.getChannel();
        AudioChannel channel = getAudioChannel(nChannel);
        channel.addAudioInputStream(audioInputStream);
    }

    public void run() {
        while (true) {
            long lTimeToSleep = Long.MAX_VALUE;
            synchronized (this) {
                while (m_vEntries.size() > 0) {
                    StartSchedulerEntry entry = m_vEntries.get(0);
                    long lTimeDifference = entry.getStartTime() - System.currentTimeMillis();
                    if (lTimeDifference <= 0) {
                        startImmediately(entry);
                        m_vEntries.remove(0);
                    } else {
                        lTimeToSleep = lTimeDifference;
                        break;
                    }
                }
            }
            if (Debug.getTraceStartScheduler()) {
                Debug.out("StartScheduler.run(): TTSleep: " + lTimeToSleep);
            }
            try {
                sleep(lTimeToSleep);
            } catch (InterruptedException e) {
            }
        }
    }

    public AudioFormat getFormat() {
        return m_audioFormat;
    }

    private AudioChannel getAudioChannel(int nChannel) {
        return m_audioChannels.get(nChannel);
    }

    public void setNumberOfChannels(int nChannels) {
        while (m_audioChannels.size() < nChannels) {
            AudioChannel audioChannel = new AudioChannel(getFormat());
            audioChannel.startChannel();
            m_audioChannels.add(audioChannel);
        }
        while (m_audioChannels.size() > nChannels) {
            AudioChannel audioChannel = m_audioChannels.remove(m_audioChannels.size());
            audioChannel.stopChannel();
        }
    }

    public void setChannelMuted(int nChannel, boolean bMute) {
        getAudioChannel(nChannel).setMute(bMute);
    }

    public static class StartSchedulerEntry {

        private AudioInputStream m_audioInputStream;

        private int m_nChannel;

        private long m_lStartTime;

        public StartSchedulerEntry(AudioInputStream audioInputStream, int nChannel, long lStartTime) {
            m_audioInputStream = audioInputStream;
            m_nChannel = nChannel;
            m_lStartTime = lStartTime;
        }

        public AudioInputStream getAudioInputStream() {
            return m_audioInputStream;
        }

        public int getChannel() {
            return m_nChannel;
        }

        public long getStartTime() {
            return m_lStartTime;
        }
    }
}
