package gui.matchMii.persoUI.mp3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import model.data.TitrePerso;

public class MP3Player implements ObservablePlayer {

    private static MP3Player instance;

    boolean pause = false;

    boolean continuer = true;

    PlayMP3Model playMP3Model;

    TitrePerso titreCourant = null;

    private MP3Player() {
        playMP3Model = new PlayMP3Model();
    }

    public static MP3Player getInstance() {
        if (instance == null) {
            instance = new MP3Player();
        }
        return instance;
    }

    public PlayMP3Model getModel() {
        return playMP3Model;
    }

    public void addTitre(TitrePerso titre) {
        playMP3Model.addTitre(titre);
        if (titreCourant == null) titreCourant = titre;
    }

    public int size() {
        return playMP3Model.getNbTitres();
    }

    private void playMe(final TitrePerso titre) {
        stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread() {

            @Override
            public void run() {
                realPlay(titre);
            }
        }.start();
    }

    private void play() {
        playMe(titreCourant);
    }

    public void play(int row) {
        if (row >= 0 && row < size()) playMe(playMP3Model.getTitre(row)); else play();
    }

    private void realPlay(TitrePerso titre) {
        if (titre == null) return;
        System.out.println("PLAY : " + titre.getNom());
        titreCourant = titre;
        notifyObserversPlayer();
        File file = titre.getFile();
        try {
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            AudioInputStream din = null;
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                rawplay(decodedFormat, din);
                in.close();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public void stop() {
        setContinuer(false);
        System.out.println("STOP");
    }

    public boolean isContinuer() {
        return continuer;
    }

    public void setContinuer(boolean continuer) {
        this.continuer = continuer;
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            setContinuer(true);
            while (nBytesRead != -1 && isContinuer()) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead);
            }
            System.out.println("STOP");
            line.drain();
            line.stop();
            line.close();
            din.close();
            setContinuer(true);
            if (nBytesRead == -1) next();
        }
    }

    public boolean next() {
        if (playMP3Model.hasNext(titreCourant)) {
            stop();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TitrePerso titre = playMP3Model.nextTitre(titreCourant);
            playMe(titre);
            return true;
        }
        return false;
    }

    public boolean previous() {
        if (playMP3Model.hasPrevious(titreCourant)) {
            stop();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TitrePerso titre = playMP3Model.previousTitre(titreCourant);
            playMe(titre);
            return true;
        }
        return false;
    }

    List<ObserverPlayer> observers = new ArrayList<ObserverPlayer>();

    @Override
    public void addObserver(ObserverPlayer obs) {
        observers.add(obs);
    }

    @Override
    public void notifyObserversPlayer() {
        for (ObserverPlayer obs : observers) {
            obs.updatePlayer(playMP3Model.getIndice(titreCourant));
        }
    }
}
