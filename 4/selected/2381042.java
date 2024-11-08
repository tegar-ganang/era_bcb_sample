package blue.soundFile;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import blue.BlueSystem;
import blue.gui.LabelledItemPanel;

/**
 * <p>
 * Title: blue
 * </p>
 * <p>
 * Description: an object composition environment for csound
 * </p>
 * <p>
 * Copyright: Copyright (c) 2001-2002
 * </p>
 * <p>
 * Company: steven yi music
 * </p>
 * 
 * @author unascribed
 * @version 1.0
 */
public class SoundFilePlayer extends JComponent {

    JLabel fileNameLabel = new JLabel();

    JButton playStopButton = new JButton();

    File soundFile = null;

    SoundIOComboBox soundOutOptions = new SoundIOComboBox(SoundIOComboBox.MIXER);

    SoundFilePlayerRunnable audioFilePlayer = null;

    LabelledItemPanel itemPanel = new LabelledItemPanel();

    JProgressBar durationSlider = new JProgressBar();

    JLabel timeDisplay = new JLabel();

    float timeDivisor = -1.0f;

    String duration = "00:00:00";

    String currentTime = "00:00:00";

    public SoundFilePlayer() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setLayout(new BorderLayout());
        playStopButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                playStopButton_actionPerformed(e);
            }
        });
        itemPanel.addItem(BlueSystem.getString("soundfile.player.label"), new JPanel());
        itemPanel.addItem(BlueSystem.getString("soundfile.player.file") + " ", fileNameLabel);
        itemPanel.addItem("", durationSlider);
        itemPanel.addItem("", timeDisplay);
        itemPanel.addItem(BlueSystem.getString("soundfile.player.soundOut") + " ", soundOutOptions);
        itemPanel.addItem("", playStopButton);
        timeDisplay.setHorizontalAlignment(JLabel.RIGHT);
        timeDisplay.setText(currentTime + "/" + duration);
        this.add(itemPanel, BorderLayout.CENTER);
        this.add(new JSeparator(), BorderLayout.SOUTH);
        setSoundFile(null);
    }

    public void setSoundFile(File soundFile) {
        this.soundFile = soundFile;
        boolean isAudioFile = true;
        AudioFileFormat aFormat = null;
        AudioFormat format = null;
        try {
            aFormat = AudioSystem.getAudioFileFormat(soundFile);
            format = aFormat.getFormat();
        } catch (Exception e) {
            isAudioFile = false;
            timeDivisor = -1;
        }
        stop();
        if (this.soundFile == null || !isAudioFile) {
            this.fileNameLabel.setText("");
            this.playStopButton.setEnabled(false);
            playStopButton.setText(BlueSystem.getString("soundfile.player.noFileSelected"));
        } else {
            this.fileNameLabel.setText(soundFile.getAbsolutePath());
            this.playStopButton.setEnabled(true);
            playStopButton.setText(BlueSystem.getString("soundfile.player.playStop"));
            timeDivisor = format.getSampleRate() * (format.getSampleSizeInBits() / 8) * format.getChannels();
            currentTime = "00:00:00";
            duration = getTimeString(aFormat.getByteLength());
            timeDisplay.setText(currentTime + "/" + duration);
        }
    }

    public void stop() {
        if (audioFilePlayer != null && audioFilePlayer.isAlive()) {
            audioFilePlayer.interrupt();
            audioFilePlayer.stopPlaying();
            audioFilePlayer = null;
        }
    }

    public void forcePlay() {
        if (soundFile != null) {
            if (audioFilePlayer == null) {
                audioFilePlayer = new SoundFilePlayerRunnable(this.soundFile, (Mixer.Info) soundOutOptions.getSelectedItem(), this.durationSlider);
                audioFilePlayer.start();
            } else if (audioFilePlayer.isAlive()) {
                audioFilePlayer.interrupt();
                audioFilePlayer.stopPlaying();
                audioFilePlayer = new SoundFilePlayerRunnable(this.soundFile, (Mixer.Info) soundOutOptions.getSelectedItem(), this.durationSlider);
                audioFilePlayer.start();
            } else {
                audioFilePlayer = new SoundFilePlayerRunnable(this.soundFile, (Mixer.Info) soundOutOptions.getSelectedItem(), this.durationSlider);
                audioFilePlayer.start();
            }
        }
    }

    void playStopButton_actionPerformed(ActionEvent e) {
        if (soundFile != null) {
            if (audioFilePlayer == null) {
                audioFilePlayer = new SoundFilePlayerRunnable(this.soundFile, (Mixer.Info) soundOutOptions.getSelectedItem(), this.durationSlider);
                audioFilePlayer.start();
            } else if (audioFilePlayer.isAlive()) {
                audioFilePlayer.interrupt();
                audioFilePlayer.stopPlaying();
                audioFilePlayer = null;
            } else {
                audioFilePlayer = new SoundFilePlayerRunnable(this.soundFile, (Mixer.Info) soundOutOptions.getSelectedItem(), this.durationSlider);
                audioFilePlayer.start();
            }
        }
    }

    protected void updateCurrentTime(int byteLength) {
        String t = getTimeString(byteLength);
        timeDisplay.setText(t + "/" + duration);
    }

    private String getTimeString(int byteLength) {
        if (timeDivisor <= 0.0f) {
            return "00:00:00";
        }
        float duration = byteLength / timeDivisor;
        int hours = (int) duration / 3600;
        duration = duration - (hours * 3600);
        int minutes = (int) duration / 60;
        duration = duration - (minutes * 60);
        String h = Integer.toString(hours);
        String m = Integer.toString(minutes);
        String s = Float.toString(duration);
        if (hours < 10) {
            h = "0" + h;
        }
        if (minutes < 10) {
            m = "0" + m;
        }
        if (duration < 10) {
            s = "0" + s;
        }
        return h + ":" + m + ":" + s.substring(0, 2);
    }

    class SoundFilePlayerRunnable extends Thread {

        File soundFile;

        Mixer.Info mixer;

        JProgressBar slider;

        boolean stopPlaying;

        public SoundFilePlayerRunnable(File soundFile, Mixer.Info mixer, JProgressBar slider) {
            this.soundFile = soundFile;
            this.mixer = mixer;
            this.slider = slider;
        }

        public void stopPlaying() {
            this.stopPlaying = true;
        }

        public void run() {
            playAudioFile(soundFile);
        }

        public void playAudioFile(File soundFile) {
            AudioInputStream ain;
            AudioFileFormat aFormat;
            int bufferSize = 40960;
            try {
                aFormat = AudioSystem.getAudioFileFormat(soundFile);
                ain = AudioSystem.getAudioInputStream(soundFile);
                AudioFormat format = aFormat.getFormat();
                DataLine.Info targetInfo = new DataLine.Info(SourceDataLine.class, format, 40960);
                if (!AudioSystem.isLineSupported(targetInfo)) {
                    JOptionPane.showMessageDialog(null, BlueSystem.getString("soundfile.player.error.lineUnsupported"));
                    return;
                }
                SourceDataLine b = (SourceDataLine) AudioSystem.getLine(targetInfo);
                int read;
                byte[] buffer = new byte[bufferSize];
                b.open(format, bufferSize);
                b.start();
                slider.setMinimum(0);
                slider.setMaximum(aFormat.getByteLength());
                slider.setValue(0);
                while ((read = ain.read(buffer)) != -1) {
                    if (stopPlaying) {
                        break;
                    }
                    b.write(buffer, 0, read);
                    int bytesRead = slider.getValue() + read;
                    slider.setValue(bytesRead);
                    updateCurrentTime(bytesRead);
                }
                if (!stopPlaying) {
                    b.drain();
                }
                b.stop();
                b.close();
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(null, iae.getLocalizedMessage());
            } catch (LineUnavailableException lue) {
                JOptionPane.showMessageDialog(null, lue.getLocalizedMessage());
            } catch (FileNotFoundException fe) {
                JOptionPane.showMessageDialog(null, BlueSystem.getString("message.file.notFound"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            slider.setValue(0);
        }
    }
}
