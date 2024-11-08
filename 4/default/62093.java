import java.io.*;
import java.net.*;

public class WAVFile {

    static int RIFF_START = 0;

    static int FORMAT_START = 12;

    private int riff;

    private int totallength;

    private int wave;

    private int fmt;

    private int fmt_length;

    private int always_01;

    private int channels;

    private int rate;

    private int bytes_per_second;

    private int bytes_per_sample;

    private int bits_per_sample;

    private int data;

    private int datalength;

    String file;

    public WAVFile(String filename) throws Exception {
        file = filename;
        byte[] buffer = new byte[1024];
        FileInputStream fin = new FileInputStream(filename);
        int amount = fin.read(buffer);
        System.out.println("First chunk: " + amount + " bytes");
        riff = getIntASCII(buffer, 0);
        totallength = getInt(buffer, 4);
        wave = getIntASCII(buffer, 8);
        fmt = getIntASCII(buffer, FORMAT_START);
        fmt_length = getInt(buffer, FORMAT_START + 4);
        always_01 = getShort(buffer, FORMAT_START + 8);
        channels = getShort(buffer, FORMAT_START + 10);
        rate = getInt(buffer, FORMAT_START + 12);
        bytes_per_second = getInt(buffer, FORMAT_START + 16);
        bytes_per_sample = getShort(buffer, FORMAT_START + 20);
        bits_per_sample = getShort(buffer, FORMAT_START + 22);
        int DATA_START = FORMAT_START + 8 + fmt_length;
        data = getIntASCII(buffer, DATA_START);
        datalength = getInt(buffer, DATA_START + 4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(buffer, DATA_START + 8, buffer.length - (DATA_START + 8));
        amount = fin.read(buffer);
        while (amount != -1) {
            baos.write(buffer, 0, amount);
            amount = fin.read(buffer);
        }
        buffer = baos.toByteArray();
        processAudioData(buffer, 0, bytes_per_sample);
        int mult = rate / 11025;
        if ((mult * 11025) != rate) throw new RuntimeException("Rate must be multiple of 11025");
    }

    void printStructure() {
        System.out.println("************* " + file);
        System.out.println("RIFF          : " + Integer.toHexString(riff));
        System.out.println("Total length  : " + totallength);
        System.out.println("WAVE          : " + Integer.toHexString(wave));
        System.out.println("FMT           : " + Integer.toHexString(fmt));
        System.out.println("Format length : " + fmt_length);
        System.out.println("Always 01?    : " + always_01);
        System.out.println("Channels      : " + channels);
        System.out.println("Rate          : " + rate);
        System.out.println("Bytes/sec     : " + bytes_per_second);
        System.out.println("Bytes/sample  : " + bytes_per_sample);
        System.out.println("Bits/sample   : " + bits_per_sample);
        System.out.println("DATA          : " + Integer.toHexString(data));
        System.out.println("Data length   : " + datalength);
    }

    byte[] audiodata;

    public byte[] getAudioData() {
        return audiodata;
    }

    void processAudioData(byte[] buffer, int index, int bytes_per_sample) {
        byte[] dummybuffer = new byte[100];
        int samples_at_11 = 0;
        switch(rate) {
            case 44100:
                samples_at_11 = 4;
                break;
            case 22050:
                samples_at_11 = 2;
                break;
            case 11025:
                samples_at_11 = 1;
                break;
            default:
                System.out.println("Huh? Here is my rate: " + rate);
        }
        int samplestoskip = samples_at_11 - 1;
        if (channels == 2) {
            samplestoskip += samples_at_11;
        }
        int bytestoskip = samplestoskip * (bits_per_sample / 8);
        System.out.println("Bytes to skip: " + bytestoskip);
        System.out.println("Samples to skip: " + samplestoskip);
        ByteArrayInputStream in = new ByteArrayInputStream(buffer);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (in.available() > 0) {
            baos.write(in.read());
            if (bits_per_sample == 16) baos.write(in.read()); else baos.write(0);
            try {
                if (bytestoskip != 0) {
                    int amount = in.read(dummybuffer, 0, bytestoskip);
                    if (amount != bytestoskip) System.out.println("MASSIVE PROBLEM, could not dummy buffer read: " + in.available() + ", " + amount);
                }
            } catch (Exception e) {
                System.out.println("BAD BAD BAD " + e);
                System.out.println("Length requested: " + bytes_per_sample);
                throw new RuntimeException(e.toString());
            }
        }
        audiodata = baos.toByteArray();
    }

    static String hexStr(int b) {
        b = b & 0x0ff;
        if (b < 0x10) return "0" + Integer.toHexString(b);
        return Integer.toHexString(b);
    }

    static int getInt(byte[] buff, int index) {
        return (buff[index] & 0x0ff) | ((buff[index + 1] & 0x0ff) << 8) | ((buff[index + 2] & 0x0ff) << 16) | ((buff[index + 3] & 0x0ff) << 24);
    }

    static int getIntASCII(byte[] buff, int index) {
        return ((buff[index] & 0x0ff) << 24) | ((buff[index + 1] & 0x0ff) << 16) | ((buff[index + 2] & 0x0ff) << 8) | (buff[index + 3] & 0x0ff);
    }

    static int getShort(byte[] buff, int index) {
        return (buff[index] & 0x0ff) | ((buff[index + 1] & 0x0ff) << 8);
    }
}
