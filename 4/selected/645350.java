package com.safi.workshop.audio.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.safi.workshop.audio.PCMToGSMConverter;

public class AudioConverter {

    /**
   * Threshold for float comparisions. If the difference between two floats is smaller
   * than DELTA, they are considered equal.
   */
    private static final float DELTA = 1E-9F;

    /**
   * Flag for debugging messages. If true, some messages are dumped to the console during
   * operation.
   */
    private int desiredChannels = AudioSystem.NOT_SPECIFIED;

    ;

    private int desiredSampleSizeInBits = AudioSystem.NOT_SPECIFIED;

    ;

    private AudioFormat.Encoding desiredEncoding;

    private float desiredSampleRate = AudioSystem.NOT_SPECIFIED;

    private Boolean desiredBigEndian = null;

    private String inputFile;

    private AudioFileFormat.Type desiredFileType = AudioFileFormat.Type.WAVE;

    public AudioInputStream convert() throws UnsupportedAudioFileException, IOException {
        File inputFile = new File(this.inputFile);
        AudioFileFormat inputFileFormat = AudioSystem.getAudioFileFormat(inputFile);
        AudioFileFormat.Type defaultFileType = inputFileFormat.getType();
        AudioInputStream stream = null;
        stream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat format = stream.getFormat();
        System.out.println("source format: " + format);
        AudioFormat targetFormat = null;
        if (desiredEncoding == null) {
            desiredEncoding = format.getEncoding();
        }
        if (desiredSampleRate == AudioSystem.NOT_SPECIFIED) {
            desiredSampleRate = format.getSampleRate();
        }
        if (desiredSampleSizeInBits == AudioSystem.NOT_SPECIFIED) {
            desiredSampleSizeInBits = format.getSampleSizeInBits();
        }
        if (desiredChannels == AudioSystem.NOT_SPECIFIED) {
            desiredChannels = format.getChannels();
        }
        if (desiredBigEndian == null) desiredBigEndian = format.isBigEndian();
        if (!AudioCommon.isPcm(format.getEncoding())) {
            System.out.println("converting to PCM...");
            AudioFormat.Encoding targetEncoding = (format.getSampleSizeInBits() == 8) ? AudioFormat.Encoding.PCM_UNSIGNED : AudioFormat.Encoding.PCM_SIGNED;
            stream = convertEncoding(targetEncoding, stream);
            System.out.println("stream: " + stream);
            System.out.println("format: " + stream.getFormat());
            if (desiredSampleSizeInBits == AudioSystem.NOT_SPECIFIED) {
                desiredSampleSizeInBits = format.getSampleSizeInBits();
            }
        }
        if (stream.getFormat().getChannels() != desiredChannels) {
            System.out.println("converting channels...");
            stream = convertChannels(desiredChannels, stream);
            System.out.println("stream: " + stream);
            System.out.println("format: " + stream.getFormat());
        }
        boolean bDoConvertSampleSize = (stream.getFormat().getSampleSizeInBits() != desiredSampleSizeInBits);
        boolean bDoConvertEndianess = (stream.getFormat().isBigEndian() != desiredBigEndian);
        if (bDoConvertSampleSize || bDoConvertEndianess) {
            System.out.println("converting sample size and endianess...");
            stream = convertSampleSizeAndEndianess(desiredSampleSizeInBits, desiredBigEndian, stream);
            System.out.println("stream: " + stream);
            System.out.println("format: " + stream.getFormat());
        }
        if (!equals(stream.getFormat().getSampleRate(), desiredSampleRate)) {
            System.out.println("converting sample rate...");
            stream = convertSampleRate(desiredSampleRate, stream);
            System.out.println("stream: " + stream);
            System.out.println("format: " + stream.getFormat());
        }
        if (!stream.getFormat().getEncoding().equals(desiredEncoding)) {
            System.out.println("converting to " + desiredEncoding + "...");
            stream = convertEncoding(desiredEncoding, stream);
            System.out.println("stream: " + stream);
            System.out.println("format: " + stream.getFormat());
        }
        return stream;
    }

    private static AudioInputStream convertEncoding(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        return AudioSystem.getAudioInputStream(targetEncoding, sourceStream);
    }

    private static AudioInputStream convertChannels(int nChannels, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), nChannels, calculateFrameSize(nChannels, sourceFormat.getSampleSizeInBits()), sourceFormat.getFrameRate(), sourceFormat.isBigEndian());
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }

    private static AudioInputStream convertSampleSizeAndEndianess(int nSampleSizeInBits, boolean bBigEndian, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), nSampleSizeInBits, sourceFormat.getChannels(), calculateFrameSize(sourceFormat.getChannels(), nSampleSizeInBits), sourceFormat.getFrameRate(), bBigEndian);
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }

    private static AudioInputStream convertSampleRate(float fSampleRate, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), fSampleRate, sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), sourceFormat.getFrameSize(), fSampleRate, sourceFormat.isBigEndian());
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }

    public File writeWavFile(AudioInputStream stream) throws IOException {
        File outputFile = File.createTempFile("tmpaudio", '.' + desiredFileType.getExtension());
        outputFile.deleteOnExit();
        int nWrittenBytes = 0;
        AudioFileFormat.Type targetFileType = desiredFileType;
        nWrittenBytes = AudioSystem.write(stream, targetFileType, outputFile);
        System.out.println("Written bytes: " + nWrittenBytes);
        return outputFile;
    }

    private static int calculateFrameSize(int nChannels, int nSampleSizeInBits) {
        return ((nSampleSizeInBits + 7) / 8) * nChannels;
    }

    /**
   * Compares two float values for equality.
   */
    private static boolean equals(float f1, float f2) {
        return (Math.abs(f1 - f2) < DELTA);
    }

    private static void printUsageAndExit() {
        out("AudioConverter: usage:");
        out("\tjava AudioConverter -h");
        out("\tjava AudioConverter -l");
        out("\tjava AudioConverter");
        out("\t\t[-c <channels>]");
        out("\t\t[-s <sample_size_in_bits>]");
        out("\t\t[-e <encoding>]");
        out("\t\t[-f <sample_rate>]");
        out("\t\t[-t <file_type>]");
        out("\t\t[-B|-L] [-D]");
        out("\t\t<sourcefile> <targetfile>");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }

    public int getDesiredChannels() {
        return desiredChannels;
    }

    public void setDesiredChannels(int desiredChannels) {
        this.desiredChannels = desiredChannels;
    }

    public int getDesiredSampleSizeInBits() {
        return desiredSampleSizeInBits;
    }

    public void setDesiredSampleSizeInBits(int desiredSampleSizeInBits) {
        this.desiredSampleSizeInBits = desiredSampleSizeInBits;
    }

    public AudioFormat.Encoding getDesiredEncoding() {
        return desiredEncoding;
    }

    public void setDesiredEncoding(AudioFormat.Encoding desiredEncoding) {
        this.desiredEncoding = desiredEncoding;
    }

    public float getDesiredSampleRate() {
        return desiredSampleRate;
    }

    public void setDesiredSampleRate(float desiredSampleRate) {
        this.desiredSampleRate = desiredSampleRate;
    }

    public Boolean getDesiredBigEndian() {
        return desiredBigEndian;
    }

    public void setDesiredBigEndian(Boolean desiredBigEndian) {
        this.desiredBigEndian = desiredBigEndian;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public AudioFileFormat.Type getDesiredFileType() {
        return desiredFileType;
    }

    public void setDesiredFileType(AudioFileFormat.Type desiredFileType) {
        this.desiredFileType = desiredFileType;
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
        AudioConverter converter = new AudioConverter();
        converter.setDesiredChannels(1);
        converter.setDesiredSampleRate(8000f);
        converter.setDesiredEncoding(new AudioFormat.Encoding("GSM0610"));
        converter.setInputFile(args[0]);
        AudioInputStream stream = converter.convert();
        File file = converter.writeWavFile(stream);
        InputStream is = PCMToGSMConverter.convert(new BufferedInputStream(new FileInputStream(file)));
        stream = AudioSystem.getAudioInputStream(is);
        AudioSystem.write(stream, new AudioFileFormat.Type("GSM", "gsm"), new BufferedOutputStream(new FileOutputStream("d:/temp/out.gsm")));
    }
}
