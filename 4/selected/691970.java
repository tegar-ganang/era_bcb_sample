package Logica;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author rickel
 */
public class Sonido {

    private String archivo;

    private File soundFile;

    private Clip clip;

    private Vector<File> listaLoops;

    private int numCancion = 0;

    public Sonido() {
    }

    public void sonido(String nombre) {
        try {
            String archivo;
            archivo = new String(getClass().getResource("/recursos/audio/" + nombre + ".wav").getFile());
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(archivo));
            AudioFormat af = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("unsupported line");
                System.exit(0);
            }
            int frameRate = (int) af.getFrameRate();
            int frameSize = af.getFrameSize();
            int bufSize = frameRate * frameSize / 10;
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(af, bufSize);
            line.start();
            byte[] data = new byte[bufSize];
            int bytesRead;
            while ((bytesRead = ais.read(data, 0, data.length)) != -1) line.write(data, 0, bytesRead);
            line.drain();
            line.stop();
            line.close();
        } catch (Exception e2) {
            System.out.println("Error Audio");
        }
    }

    public void loop() throws Exception {
        AudioInputStream sound = AudioSystem.getAudioInputStream(listaLoops.get(numCancion));
        DataLine.Info info = new DataLine.Info(Clip.class, sound.getFormat());
        clip = (Clip) AudioSystem.getLine(info);
        clip.open(sound);
        clip.addLineListener(new LineListener() {

            public void update(LineEvent event) {
                if (event.getType() == LineEvent.Type.STOP) {
                }
            }
        });
        clip.loop(10);
    }

    public void terminarLoop() {
        clip.close();
    }

    public void setLoops(Vector<File> listaLoops) {
        this.listaLoops = listaLoops;
    }

    public void siguienteCancion() throws Exception {
        numCancion++;
        if (numCancion > 4) numCancion = 0;
        loop();
    }

    public void anteriorCancion() throws Exception {
        numCancion--;
        if (numCancion < 0) numCancion = 4;
        loop();
    }
}
