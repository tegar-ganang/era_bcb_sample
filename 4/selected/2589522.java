package joecohen;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import javax.sound.sampled.*;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import kjdss.KJDigitalSignalSynchronizer;

public class FrequencyDump extends JFrame {

    private static final long serialVersionUID = 1L;

    AudioFormat audioFormat;

    AudioInputStream audioInputStream;

    SourceDataLine sourceDataLine;

    boolean stopPlayback = false;

    final JButton stopBtn = new JButton("Stop");

    final JButton playBtn = new JButton("Play");

    final JTextField textField = new JTextField("Enter Filename Here");

    final KJScopeAndSpectrumAnalyzer spec = new KJScopeAndSpectrumAnalyzer();

    final KJDigitalSignalSynchronizer syn = new KJDigitalSignalSynchronizer(10, 10);

    boolean mute;

    public static void main(String args[]) {
        System.out.println("Frequency Spectrum Dump v1 - Joseph Paul Cohen 2011");
        if (args.length == 0) {
            System.out.println("Usage: FrequencyDump <FILENAME> {-m | -s}");
            System.out.println("{-m} - To mute");
            System.out.println("{-s} - No Window or sound");
            System.out.println();
            System.out.println("Output: <FILENAME>.csv containing the mean values of frequencies in the file");
            new FrequencyDump(false);
        } else if (args.length == 1) {
            new FrequencyDump(args[0], false, false);
        } else if (args.length == 2 && args[1].equals("-m")) {
            new FrequencyDump(args[0], true, false);
        } else if (args.length == 2 && args[1].equals("-s")) {
            new FrequencyDump(args[0], true, true);
        } else {
            System.out.println("arg error");
        }
    }

    public FrequencyDump(String name, boolean mute, boolean hidden) {
        this(hidden);
        this.mute = mute;
        textField.setText(name);
        stopBtn.setEnabled(true);
        playBtn.setEnabled(false);
        playAudio();
    }

    public FrequencyDump(boolean hidden) {
        stopBtn.setEnabled(false);
        playBtn.setEnabled(true);
        playBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                stopBtn.setEnabled(true);
                playBtn.setEnabled(false);
                playAudio();
            }
        });
        stopBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                stopPlayback = true;
                writedata();
            }
        });
        getContentPane().add(playBtn, "West");
        getContentPane().add(stopBtn, "East");
        getContentPane().add(textField, "North");
        getContentPane().add(spec);
        spec.setSpectrumAnalyserBandCount(255);
        spec.setSpectrumAnalyserBandFrequenciesVisible(true);
        spec.setSpectrumAnalyserBandDistribution(KJScopeAndSpectrumAnalyzer.BAND_DISTRIBUTION_LINEAR);
        spec.setDisplayMode(KJScopeAndSpectrumAnalyzer.DISPLAY_MODE_SPECTRUM_ANALYSER);
        syn.add(spec);
        setTitle("Frequency Spectrum Dump - Joseph Paul Cohen 2011");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 300);
        setVisible(!hidden);
    }

    private void playAudio() {
        try {
            if (new File(textField.getText() + ".csv").exists()) {
                System.out.println("Skipping file because csv exists: " + textField.getText() + ".csv");
                System.exit(1);
            }
            audioInputStream = AudioSystem.getAudioInputStream(new File(textField.getText()));
            AudioFormat baseFormat = audioInputStream.getFormat();
            System.out.print(textField.getText() + ": ");
            System.out.println(baseFormat);
            if (textField.getText().endsWith(".mp3") || textField.getText().endsWith(".ogg") || textField.getText().endsWith(".flac")) {
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                AudioInputStream dis = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
                audioInputStream = dis;
                audioFormat = dis.getFormat();
                System.out.println("Converted to: " + audioFormat);
            } else {
                audioFormat = baseFormat;
            }
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            new PlayThread().start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    class PlayThread extends Thread {

        byte tempBuffer[] = new byte[100];

        public void run() {
            try {
                sourceDataLine.open(audioFormat);
                if (mute && sourceDataLine.isControlSupported(FloatControl.Type.VOLUME)) {
                    System.out.println("Muted");
                    FloatControl volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                    volume.setValue(volume.getMinimum());
                } else if (sourceDataLine.isControlSupported(FloatControl.Type.VOLUME)) {
                    FloatControl volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                    volume.setValue(volume.getMaximum());
                }
                syn.start(sourceDataLine);
                sourceDataLine.start();
                int cnt;
                while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1 && stopPlayback == false) {
                    if (cnt > 0) {
                        syn.writeAudioData(tempBuffer);
                    }
                }
                sourceDataLine.drain();
                syn.stop();
                writedata();
                sourceDataLine.close();
                stopBtn.setEnabled(false);
                playBtn.setEnabled(true);
                stopPlayback = false;
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    public void writedata() {
        try {
            DescriptiveStatistics[] stats = spec.stats;
            FileWriter outputFile = new FileWriter(textField.getText() + ".csv", false);
            outputFile.write("\"" + textField.getText() + "\",");
            for (int i = 0; i < stats.length; i++) outputFile.write(((float) stats[i].getMean()) + ",");
            outputFile.write('\n');
            outputFile.flush();
            FileWriter headerFile = new FileWriter("FreqDumpHeader.csv", false);
            headerFile.write("FileName,");
            for (int bd = 0; bd < spec.saBands; bd++) headerFile.write(spec.sabdTable[bd].frequency + ",");
            headerFile.write('\n');
            headerFile.flush();
            System.out.println("Wrote Data!");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
