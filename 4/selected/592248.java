package org.chizar.mp3union;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import javazoom.spi.mpeg.sampled.file.MpegFileFormatType;

public class AudioComposer {

    private InputDataComposer inputDataComposer;

    public AudioComposer(InputDataComposer inputDataComposer) {
        this.inputDataComposer = inputDataComposer;
    }

    public InputDataComposer getInputDataComposer() {
        return inputDataComposer;
    }

    public void setInputDataComposer(InputDataComposer inputDataComposer) {
        this.inputDataComposer = inputDataComposer;
    }

    public void process() throws Exception {
        List<File> files = getInputDataComposer().getFileSequence();
        AudioFormat sourceFormat = null;
        File f = File.createTempFile("pref", "mp3union");
        f.deleteOnExit();
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(f));
        AudioInputStream ais = null, sourceStream = null, finalStream = null;
        try {
            for (int i = 0; i < files.size(); i++) {
                ais = AudioConverter.convert(AudioSystem.getAudioInputStream(files.get(i)));
                try {
                    if (sourceFormat == null) {
                        sourceFormat = ais.getFormat();
                    }
                    byte[] buff = new byte[1024];
                    while (-1 != (ais.read(buff))) {
                        output.write(buff);
                    }
                } finally {
                    if (ais != null) {
                        ais.close();
                    }
                }
            }
            output.flush();
        } finally {
            if (output != null) {
                output.close();
            }
        }
        AudioFormat targetFormat = getTargetFormat(sourceFormat);
        try {
            sourceStream = new AudioInputStream(new BufferedInputStream(new FileInputStream(f)), sourceFormat, AudioSystem.NOT_SPECIFIED);
            finalStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            File outputFile = new File(getInputDataComposer().getOutputFilename());
            AudioSystem.write(finalStream, MpegFileFormatType.MP3, outputFile);
        } finally {
            if (sourceStream != null) {
                sourceStream.close();
            }
            if (finalStream != null) {
                finalStream.close();
            }
        }
    }

    public AudioFormat getTargetFormat(AudioFormat sourceFormat) {
        return new AudioFormat(MpegEncoding.MPEG1L3, sourceFormat.getSampleRate(), AudioSystem.NOT_SPECIFIED, sourceFormat.getChannels(), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false);
    }
}
