package temp2;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class ReadSound {

    AudioInputStream audioInputStream;

    private int PACKET_SIZE = 5000;

    private CHANNEl channel;

    int nBytesRead = 0;

    byte[] bufferFile;

    long lenghtFile;

    byte[] abData;

    int quantPacket;

    int rest;

    int channels;

    AudioFormat audioFormat;

    public ReadSound(String filename) throws Exception {
        File soundFile = new File(filename);
        audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        audioFormat = audioInputStream.getFormat();
        lenghtFile = soundFile.length();
        channels = audioFormat.getChannels();
        this.quantPacket = (int) lenghtFile / (this.channels * PACKET_SIZE);
        this.channel = choseChannel(audioFormat.getChannels());
        this.rest = (int) (lenghtFile - (((int) (this.quantPacket)) * PACKET_SIZE * this.channels));
        bufferFile = new byte[(int) (lenghtFile / this.channels)];
        abData = new byte[channels * PACKET_SIZE];
    }

    private CHANNEl choseChannel(int n) {
        if (n == 1) return CHANNEl.MONO;
        return CHANNEl.ESTEREO;
    }

    public void fillBuffer() {
        int flag;
        for (int i = 0; i < quantPacket; i++) {
            try {
                flag = getNextFrame();
                for (int j = 0, k = 0; j < abData.length; j += channels, k++) {
                    bufferFile[i * PACKET_SIZE + k] = abData[j];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            getNextFrame();
            for (int i = 0, k = 0; i < getRest(); i += channels, k++) {
                bufferFile[quantPacket * PACKET_SIZE + k] = abData[i];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNextFrame() throws Exception {
        return audioInputStream.read(abData, 0, abData.length);
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }

    public int getQuantPacket() {
        return quantPacket;
    }

    public void printBuffer() {
        for (int i = 0; i < bufferFile.length; i++) {
            if (bufferFile[i] == 0) System.out.println("--------");
            System.out.println(bufferFile[i]);
        }
    }

    public void setQuantPacket(int quantPacket) {
        this.quantPacket = quantPacket;
    }

    public int getRest() {
        return rest;
    }

    public void setRest(int rest) {
        this.rest = rest;
    }

    public byte[] getBufferFile() {
        return bufferFile;
    }
}
