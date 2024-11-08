package au.gov.naa.digipres.xena.plugin.audio;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.xml.transform.stream.StreamResult;
import org.kc7bfi.jflac.sound.spi.FlacAudioFileReader;
import org.kc7bfi.jflac.sound.spi.FlacFormatConversionProvider;
import org.xml.sax.ContentHandler;
import au.gov.naa.digipres.xena.kernel.XenaException;
import au.gov.naa.digipres.xena.kernel.view.XenaView;
import au.gov.naa.digipres.xena.util.BinaryDeNormaliser;

public class AudioPlayerView extends XenaView {

    private static final int PLAYER_SAMPLE_SIZE_BITS = 16;

    private static final String PLAY_TEXT = "Play";

    private static final String PAUSE_TEXT = "Pause";

    private static final int STOPPED = 0;

    private static final int PLAYING = 1;

    private static final int PAUSED = 2;

    private int playerStatus = STOPPED;

    private File flacFile;

    private SourceDataLine sourceLine;

    private JButton playPauseButton;

    public AudioPlayerView() {
        super();
        initGUI();
    }

    private void initGUI() {
        JPanel playerPanel = new JPanel(new FlowLayout());
        playPauseButton = new JButton(PLAY_TEXT);
        JButton stopButton = new JButton("Stop");
        playerPanel.add(playPauseButton);
        playerPanel.add(stopButton);
        this.add(playerPanel);
        playPauseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (playerStatus == PLAYING) {
                    sourceLine.stop();
                    playerStatus = PAUSED;
                    playPauseButton.setText(PLAY_TEXT);
                } else if (playerStatus == PAUSED) {
                    sourceLine.start();
                    playerStatus = PLAYING;
                    playPauseButton.setText(PAUSE_TEXT);
                } else if (playerStatus == STOPPED) {
                    playerStatus = PLAYING;
                    playPauseButton.setText(PAUSE_TEXT);
                    try {
                        FlacAudioFileReader flacReader = new FlacAudioFileReader();
                        AudioInputStream flacStream = flacReader.getAudioInputStream(flacFile);
                        initAudioLine(flacStream);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sourceLine.stop();
                playerStatus = STOPPED;
                playPauseButton.setText(PLAY_TEXT);
            }
        });
    }

    private void initAudioLine(AudioInputStream audioStream) throws LineUnavailableException {
        AudioFormat audioFormat = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        if (!AudioSystem.isLineSupported(info)) {
            AudioFormat sourceFormat = audioFormat;
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), PLAYER_SAMPLE_SIZE_BITS, sourceFormat.getChannels(), sourceFormat.getChannels() * (PLAYER_SAMPLE_SIZE_BITS / 8), sourceFormat.getSampleRate(), sourceFormat.isBigEndian());
            FlacFormatConversionProvider flacCoverter = new FlacFormatConversionProvider();
            audioStream = flacCoverter.getAudioInputStream(targetFormat, audioStream);
            audioFormat = audioStream.getFormat();
        }
        sourceLine = getSourceDataLine(audioFormat);
        sourceLine.start();
        LineWriterThread lwThread = new LineWriterThread(audioStream, audioFormat.getFrameSize());
        lwThread.start();
    }

    /**
	 * Need to stop playback if the enclosing window or dialog is closed
	 */
    @Override
    protected void close() {
        if (sourceLine != null) {
            sourceLine.stop();
            sourceLine.close();
        }
        playerStatus = STOPPED;
        super.close();
    }

    private SourceDataLine getSourceDataLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open();
        return line;
    }

    @Override
    public String getViewName() {
        return "audio";
    }

    @Override
    public boolean canShowTag(String tag) throws XenaException {
        String flacTag = DirectAudioNormaliser.AUDIO_PREFIX + ":" + DirectAudioNormaliser.FLAC_TAG;
        return tag.equals(flacTag);
    }

    @Override
    public ContentHandler getContentHandler() throws XenaException {
        FileOutputStream xenaTempOS = null;
        try {
            flacFile = File.createTempFile("tmpview", ".flac");
            flacFile.deleteOnExit();
            xenaTempOS = new FileOutputStream(flacFile);
        } catch (IOException e) {
            throw new XenaException("Problem creating temporary xena output file", e);
        }
        BinaryDeNormaliser base64Handler = new BinaryDeNormaliser();
        StreamResult result = new StreamResult(xenaTempOS);
        base64Handler.setResult(result);
        return base64Handler;
    }

    private class LineWriterThread extends Thread {

        private AudioInputStream audioStream;

        private int frameSize;

        public LineWriterThread(AudioInputStream audioStream, int frameSize) {
            this.audioStream = audioStream;
            this.frameSize = frameSize;
        }

        @Override
        public void run() {
            try {
                int bytesRead;
                byte[] buffer = new byte[frameSize];
                while (true) {
                    if (playerStatus == PLAYING) {
                        if (0 < (bytesRead = audioStream.read(buffer))) {
                            sourceLine.write(buffer, 0, bytesRead);
                        } else {
                            playerStatus = STOPPED;
                            playPauseButton.setText(PLAY_TEXT);
                        }
                    } else if (playerStatus == PAUSED) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (playerStatus == STOPPED) {
                        audioStream.close();
                        sourceLine.flush();
                        sourceLine.close();
                        break;
                    }
                }
            } catch (IOException iex) {
                iex.printStackTrace();
            }
        }
    }
}
