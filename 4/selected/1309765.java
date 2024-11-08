package coder.apps;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import coder.utils.FlacToRawDecoder;

public class JFlacPlayer extends Thread {

    private SourceDataLine playLine;

    private AudioInputStream rawData;

    public JFlacPlayer(File f) {
        try {
            rawData = new FlacToRawDecoder(f).getRawAudioInputStream2();
            playLine = AudioSystem.getSourceDataLine(null);
            playLine.open(rawData.getFormat());
            playLine.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        int numRead = 0;
        byte[] buf = new byte[playLine.getBufferSize()];
        try {
            while ((numRead = rawData.read(buf, 0, buf.length)) >= 0) playLine.write(buf, 0, numRead);
        } catch (Exception e) {
            e.printStackTrace();
        }
        playLine.drain();
        stopLine();
    }

    public void stopLine() {
        playLine.stop();
        playLine.close();
        try {
            rawData.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Playing stopped");
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("enter filename next to the jar name");
            System.exit(1);
        }
        JFlacPlayer player = new JFlacPlayer(new File("J:\\ololo.flac"));
        player.start();
        System.out.println("FlacPlayer started");
        System.out.println("Press Enter key to stop playing...");
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        player.stopLine();
    }
}
