package enigma.morse;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Klasse welche Morse Zeichen als Biepton ausgeben kann
 * 
 * @author Sebastian Chlan <br />
 * <br />
 * ENIGMA_TEC 2010 <br />
 * technik[at]enigma-ausstellung.at <br />
 * http://enigma-ausstellung.at <br />
 * <br />
 * HTL Rennweg <br />
 * Rennweg 89b <br />
 * A-1030 Wien <br />
 * 
 */
public class MorseSound extends Thread {

    private String morseCode;

    private boolean stopThread = false;

    /**
	 * Konstruktor: Klasse kann nur aufgerufen werden, wenn Morsezeichen
	 * uebergeben werden
	 * 
	 * @param s
	 *            String mit vielen Morsezeichen
	 */
    public MorseSound(String s) {
        chkInput(s);
        morseCode = s;
    }

    /**
	 * Startet das gepiepse
	 */
    public void run() {
        Clip longBeep = loadClipfromRessources("longBeep.au");
        Clip shortBeep = loadClipfromRessources("shortBeep.au");
        System.out.println("los");
        try {
            for (int i = 0; i < morseCode.length(); i++) {
                if (!stopThread) {
                    if (!" ".equals(morseCode.charAt(i) + "")) {
                        if (".".equals(morseCode.charAt(i) + "")) {
                            shortBeep.start();
                            Morse.sleep(200);
                            shortBeep.stop();
                        } else if ("-".equals(morseCode.charAt(i) + "")) {
                            longBeep.start();
                            Morse.sleep(300);
                            longBeep.stop();
                        }
                    } else {
                        Morse.sleep(400);
                    }
                } else {
                }
            }
        } catch (Exception e) {
        }
    }

    /**
	 * Laedt das Audiofile aus dem ressources Ordner
	 * 
	 * @param fname
	 *            Name des AudioFiles
	 * @return File in Form eines Audioclips
	 */
    private Clip loadClipfromRessources(String fname) {
        File f = null;
        AudioInputStream ais = null;
        AudioFormat format;
        Clip cl = null;
        try {
            f = new File("ressources/" + fname);
            ais = AudioSystem.getAudioInputStream(f);
        } catch (IOException e) {
            try {
                ais = AudioSystem.getAudioInputStream(new Object().getClass().getResource("/ressources/" + fname));
            } catch (UnsupportedAudioFileException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (UnsupportedAudioFileException e) {
            System.out.println("Boeses Fileformat");
            e.printStackTrace();
        }
        try {
            format = ais.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                ais = AudioSystem.getAudioInputStream(tmp, ais);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat(), ((int) ais.getFrameLength() * format.getFrameSize()));
            cl = (Clip) AudioSystem.getLine(info);
            cl.open(ais);
        } catch (IOException e) {
            System.out.println("Fehler beim Einlesen der Sounddatei!");
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.out.println("Line nicht verf?gbar!");
        }
        return cl;
    }

    /**
	 * Wird aufgerufen falls der User auf stop drueckt.
	 */
    public void done() {
        stopThread = true;
    }

    /**
	 * Ueberprueft ob der String nur aus Leerzeichen, '.' oder '-' besteht.
	 * Falls nicht --> Exception
	 * 
	 * @param s
	 *            Der zu ueberpruefende String
	 */
    private void chkInput(String s) {
        if (!s.matches("[-. ]*")) throw new IllegalArgumentException("Illigal characters used, only allowed: '-','.',' '");
    }
}
