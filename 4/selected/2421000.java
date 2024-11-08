package de.lamasep.sound;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * NavigationSoundFile repräsentiert ein Audio-File, das Teile der
 * Sprachausgabe eines Navigationshinweises beinhaltet.
 *
 * Es kann über einen InputStream ausgelesen werden.
 *
 * @author Anja Kastenmayer
 *
 * @see SoundFile
 */
public class NavigationSoundFile implements SoundFile {

    /**
     * Audio-InputStream der verwalteten Sound-Datei.
     */
    private AudioInputStream in;

    /**
     * Relativer Pfad zur Audiodatei.
     */
    private String path;

    /**
     * Abspielbare DataLine, die den InputStream des Audiofiles lädt.
     */
    private Clip line;

    /**
     * Konstruktor - Initialisierung mit dem Pfad zu einer Audiodatei.
     *
     * @param path  Pfad zur Audiodatei, die in diesem NavigationSoundFile
     *              verwaltet wird, <code> path != null && path != "" </code>
     *
     * @throws IllegalArgumentException falls <code> path == null ||
     *                                  path.length == 0 </code>
     * @throws IOException  falls die spezifizierte Datei nicht lesbar ist
     * @throws AudioInputException  falls die spezifizierte Date keine
     *                              WAVE-Datei ist oder ein Fehler während des
     *                              Ladens des AudioInputStream über
     *                              <code>line</code> auftritt
     */
    public NavigationSoundFile(final String path) throws IOException, AudioInputException {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.path = path;
        InputStream fileIn = SoundFile.class.getResourceAsStream(path);
        if (fileIn == null) {
            throw new FileNotFoundException("path: " + path);
        }
        try {
            in = AudioSystem.getAudioInputStream(fileIn);
            this.loadDataLine();
        } catch (UnsupportedAudioFileException ex) {
            throw new AudioInputException(ex);
        } catch (LineUnavailableException ex) {
            throw new AudioInputException(ex);
        }
    }

    /**
     * Liest das SoundFile aus und gibt seinen Inhalt als DataLine zurück.
     *
     * @return Abspielbare DataLine, die den InputStream des Audiofiles geladen
     *         hat
     *
     * @throws IOException falls ein Input-/Output-Fehler auftritt
     *
     * @see SoundFile#open()
     * @see DataLine
     * @see DataLine#open()
     */
    @Override
    public DataLine open() throws IOException {
        try {
            line.open(in);
        } catch (LineUnavailableException ex) {
            throw new AudioInputException(ex);
        }
        return line;
    }

    /**
     * Öffnet den AudioInputStream der verwalteten Sound-Datei und
     * initialisiert <code>line</code> entsprechend.
     *
     * @throws LineUnavailableException Fehler beim Öffnen des InputStreams
     *         durch die DataLine
     *
     * @throws AudioInputException falls das AudioFormat der Datei nicht
     *         unterstützt ist
     */
    private void loadDataLine() throws LineUnavailableException, AudioInputException {
        AudioFormat format = in.getFormat();
        if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
            AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
            in = AudioSystem.getAudioInputStream(tmp, in);
            format = tmp;
        } else if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED && format.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            throw new AudioInputException("Unexpected AudioFormat " + format.getEncoding());
        }
        int size = (int) in.getFrameLength() * format.getFrameSize();
        DataLine.Info info = new DataLine.Info(Clip.class, format, size);
        line = (Clip) AudioSystem.getLine(info);
    }

    /**
     * String-Repräsentation: Pfad zur Datei.
     * @return  String-Repräsentation
     */
    @Override
    public String toString() {
        return path;
    }
}
