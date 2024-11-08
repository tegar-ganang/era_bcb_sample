package org.paquitosoft.namtia.session.actions;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.paquitosoft.namtia.common.NamtiaUtilities;
import org.paquitosoft.namtia.session.SessionController;
import org.paquitosoft.namtia.vo.SongVO;

/**
 *
 * @author telemaco
 */
public class NamtiaPlayer {

    private static NamtiaPlayer instance;

    private static AudioInputStream din;

    private static SourceDataLine line;

    /**
     * Creates a new instance of NamtiaPlayer
     */
    private NamtiaPlayer() {
    }

    public static NamtiaPlayer getInstance() {
        if (instance == null) {
            instance = new NamtiaPlayer();
        }
        return instance;
    }

    public void play(SongVO song) {
        new Thread(new InnerPlayer(song.getPath())).start();
    }

    public void playCurrentSong() {
        System.out.println("---> Vamos a intentar reproducir la cancion...");
        System.out.println("---> Archivo: " + SessionController.getInstance().getCurrentSong().getPath());
        new Thread(new InnerPlayer(SessionController.getInstance().getCurrentSong().getPath())).start();
    }

    public void stopSong() {
        try {
            if (this.getDin() != null) {
                long tiempo = Calendar.getInstance().getTimeInMillis();
                System.out.print("    Vamos a cerrar el audio stream...");
                this.getLine().drain();
                this.getLine().stop();
                this.getLine().close();
                this.getDin().close();
                System.out.println("    Hecho.");
                System.out.println("Tiempo transcurrido en milesimas de segundo -> " + (Calendar.getInstance().getTimeInMillis() - tiempo));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        setDin(null);
    }

    protected static AudioInputStream getDin() {
        return din;
    }

    protected static void setDin(AudioInputStream aDin) {
        din = aDin;
    }

    protected static SourceDataLine getLine() {
        return line;
    }

    protected static void setLine(SourceDataLine aLine) {
        line = aLine;
    }

    class InnerPlayer implements Runnable {

        private String songPath;

        public InnerPlayer(String songPath) {
            this.songPath = songPath;
        }

        public void run() {
            try {
                File file = new File(songPath);
                AudioInputStream in = AudioSystem.getAudioInputStream(file);
                setDin(null);
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                setDin(AudioSystem.getAudioInputStream(decodedFormat, in));
                rawplay(decodedFormat, getDin());
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
            byte[] data = new byte[4096];
            setLine(getInnerLine(targetFormat));
            if (getLine() != null) {
                getLine().start();
                int nBytesRead = 0, nBytesWritten = 0;
                while (nBytesRead != -1) {
                    nBytesRead = din.read(data, 0, data.length);
                    if (nBytesRead != -1) nBytesWritten = getLine().write(data, 0, nBytesRead);
                }
                getLine().drain();
                getLine().stop();
                getLine().close();
                din.close();
            }
        }

        private SourceDataLine getInnerLine(AudioFormat audioFormat) throws LineUnavailableException {
            SourceDataLine res = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            res = (SourceDataLine) AudioSystem.getLine(info);
            res.open(audioFormat);
            return res;
        }
    }
}
