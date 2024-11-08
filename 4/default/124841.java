import javax.sound.midi.*;

/**
 * Classe statique (abstraite) permettant d'acc�der � diverses fonctions de traitement
 * des objets de la biblioth�que javax.sound.midi
 * @author Feanor
 *
 */
public abstract class MidiTools {

    private static final String[] notes = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    /**
	 * Renvoie la note et l'octave d'une hauteur d�finie par le nKeyNumber
	 * @param nKeyNumber Hauteur midi de la note.
	 * @return note et octave
	 */
    static String getKeyName(int nKeyNumber) {
        int nNote = nKeyNumber % 12;
        int nOctave = nKeyNumber / 12;
        return notes[nNote] + (nOctave - 1);
    }

    /**
	 * Renvoie la note jou�e entre 0 et 11 (do � si) s'il y a lieu, -1 sinon.
	 * @param message Message � analyser
	 * @return Note jou�e
	 */
    public static int getNote(MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage smessage = (ShortMessage) message;
            return smessage.getData1() % 12;
        } else return -1;
    }

    /**
	 * Renvoie l'octave (octave 4 correspond au milieu du clavier) du message si possible, -1 sinon.
	 * @param message Message � analyser
	 * @return Octave du message si possible, -1 sinon
	 */
    public static int getOctave(MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage smessage = (ShortMessage) message;
            return smessage.getData1() / 12 - 1;
        } else return -1;
    }

    /**
	 * Renvoie la v�locit� d'un message s'il y a lieu, -1 sinon.
	 * @param message Message � analyser
	 * @return int V�locit� du message s'il y a lieu, -1 sinon
	 */
    public static int getVelocity(MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage smessage = (ShortMessage) message;
            return smessage.getData2();
        } else return -1;
    }

    /**
	 * Divise l'intensit� sonore (v�locit�) d'un midi message si c'est possible,
	 * ne fait rien sinon.
	 * @param message Message � analyser
	 */
    public static void halfVelocity(MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage smessage = (ShortMessage) message;
            try {
                ((ShortMessage) message).setMessage(message.getStatus(), smessage.getData1(), (3 * smessage.getData2() / 4) + 1);
            } catch (InvalidMidiDataException e) {
            }
        }
    }

    /**
	 * Renvoie une explication en String du message
	 * @param message Message � analyser
	 * @return String Message intelligible traduisant le Message Midi.
	 */
    public static String messageToString(MidiMessage message) {
        String strMessage = null;
        if (message instanceof ShortMessage) {
            ShortMessage smessage = (ShortMessage) message;
            switch(smessage.getCommand()) {
                case 0x80:
                    strMessage = "note Off " + getKeyName(smessage.getData1()) + " velocit� : " + smessage.getData2();
                    break;
                case 0x90:
                    strMessage = "note On " + getKeyName(smessage.getData1()) + " velocit� : " + smessage.getData2();
                    break;
            }
            if (smessage.getCommand() != 0xF0) {
                int nChannel = smessage.getChannel() + 1;
                String strChannel = "channel " + nChannel + ": ";
                strMessage = strChannel + strMessage;
            }
            return strMessage;
        } else return null;
    }

    /**
	 * Renvoie vrai si le message 2 correspond � l'arr�t de la note du message 1, faux sinon
	 * @param message Message de la note initiale
	 * @param message2 Message terminant peut-�tre la note initiale.
	 * @return Vrai si le message 2 correspond � l'arr�t de la note du message 1, faux sinon
	 */
    public static boolean stopsMessage(MidiMessage message, MidiMessage message2) {
        {
            if (message instanceof ShortMessage && message2 instanceof ShortMessage) {
                ShortMessage smessage = (ShortMessage) message;
                ShortMessage smessage2 = (ShortMessage) message2;
                if (smessage.getData1() == smessage2.getData1()) {
                    return (typeOfMessage(smessage2) == 0 || getVelocity(smessage2) >= 0);
                } else return false;
            } else return false;
        }
    }

    /**
	 * Renvoie le type de message midi
	 * 0 si NoteOff, 1 si NoteOn, -1 sinon
	 * @param message MidiMessage � analyser
	 * @return 0 si NoteOff, 1 si NoteOn, -1 sinon 
	 */
    public static int typeOfMessage(MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage smessage = (ShortMessage) message;
            switch(smessage.getCommand()) {
                case 0x80:
                    return 0;
                case 0x90:
                    return 1;
                default:
                    return -1;
            }
        } else return (-1);
    }
}
