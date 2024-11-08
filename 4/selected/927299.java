package temp3;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Teste {

    public static void main(String[] args) throws Exception {
        ReadSound rs = new ReadSound("voz.wav");
        rs.fillBuffer();
        int n = 0;
        Vector buffer = rs.getChannel(1);
        byte[] temp = new byte[buffer.size()];
        for (Iterator i = buffer.iterator(); i.hasNext(); n++) {
            Byte data = (Byte) i.next();
            temp[n] = data;
            System.out.println(temp[n]);
        }
        Receiver r = new Receiver(temp, 100, 10, 20);
        r.generateLoss(3);
        r.generateLoss(4);
        r.generateLoss(5);
        r.generateLoss(6);
        r.generateLoss(7);
        r.getPacket(1);
        r.getPacket(2);
        r.getPacket(3);
        r.getPacket(4);
        r.getPacket(5);
        r.getPacket(6);
        r.getPacket(7);
        byte[] data = new byte[r.getLenghtPacket()];
        r.getPacket(3).getpayload(data);
        byte[] b = r.getBufferBytes();
        player(b, rs.getAudioFormat());
    }

    public static void player(byte[] buffer, AudioFormat audioFormat) {
        SourceDataLine line = null;
        DataLine.Info info = null;
        byte[] abData = new byte[5000];
        int quant = buffer.length / abData.length;
        int readByte = 0;
        info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        line.start();
        int n = 0;
        int fim = 0;
        while (n < quant + 1) {
            abData = Arrays.copyOfRange(buffer, fim, fim + abData.length - 1);
            readByte = line.write(abData, 0, abData.length);
            n++;
            fim += abData.length;
        }
    }
}
