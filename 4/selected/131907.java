package ch.ideenarchitekten.vip.logic.exception;

import java.io.*;
import java.lang.Thread.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import ch.ideenarchitekten.vip.data.network.*;
import ch.ideenarchitekten.vip.gui.MessageDialog;
import ch.ideenarchitekten.vip.logic.*;

/**
 * Behandelt auftretende Exceptions. Ist als Singleton realisiert.
 * 
 * @author $LastChangedBy: martinschaub $
 * @version $LastChangedRevision: 292 $
 */
public final class ExceptionHandler implements UncaughtExceptionHandler {

    /**
	 * Speichert die einzige Instanz der Klasse, da sie als Singleton realisiert
	 * wurde.
	 */
    private static ExceptionHandler s_instance = new ExceptionHandler();

    /**
	 * Vector der momentan gesperrten Events.
	 */
    private Vector<ExceptionEvent> m_maskedEvents = null;

    /**
	 * Anfangsgroesse des erzeugten Vectors.
	 */
    private static final int INITIAL_VECTOR_SIZE = 10;

    /**
	 * Der Name Logdatei, in die unvorhergesehene Fehler gespeichert werden.
	 */
    public static final String LOGFILE = "error.log";

    /**
	 * In jedem Eintrag in der Logdatei wird zur Identifikation ein
	 * Datum mitgeschrieben. Dieser String gibt an, wie das Datum formatiert ist.
	 */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
	 * Speichert die diversen M�glichkeiten von Fehlern. M�chte eine neuer
	 * Fehler von dieser Klasse behandelt wird, so muss er in diesem Enum unter
	 * einem eindeutigen Namen registriert werden.
	 */
    public static enum errors {

        /**
		 * Wird verwendet, um zu Testen ob eine g�ltige ErrorNummer verwendet wurde. Muss an erster Stelle stehen.
		 */
        NULL, /**
		 * Wenn beim Handschake zwischen dem Server und dem Client, die IDs nicht korrekt 
		 * ausgetauscht wurden. In diesem Fall Meldet der Server, dass der Client eine 
		 * ung�ltige ID geschickt hat.
		 */
        WrongClientIDFormatClient, /**
		 * Wenn beim Handschake zwischen dem Server und dem Client, die IDs nicht korrekt 
		 * ausgetauscht wurden. In diesem Fall Meldet der Server, dass der Client eine 
		 * ung�ltige ID geschickt hat.
		 */
        WrongClientIDFormatServer, /**
		 * Falls ein Packet �bertragen wurde, dass kein DataEvent ist.
		 */
        WrongObjectReceived, /**
		 * Falls beim Verbinden eines neuen Clients zum Server ein Fehler aufgetretten ist. 
		 */
        HandshakeError, /**
		 * Fehler im NetworkThread, wenn ein dataEvent nicht mehr gesendet werden kann.
		 */
        NetworkSendingError, /**
		 * Der ClientThread meldet, dass beim horchen auf neue Daten, die Verbindung
		 * unterbrochen wurde und eine IOException geworfen wurde.
		 */
        NetworkReceivingError, /**
		 * Falls die Nachricht an einen Client geht, welcher gar nicht existiert.
		 */
        DestinationClientIDNotExist, /**
		 * Falls w�hrend dem Warten auf IDs ein Thread interrupted worden ist.
		 */
        InterruptedDuringGetID, /**
		 * Wenn ein Thread durch eine unbehandelte Exception beendet wurde.
		 */
        ThreadUncaughtException, /**
		 * Falls ein Fehler in der finalize() Methode der Klasse ServerThread aufgetretten ist.
		 */
        ServerThreadFinalize, /**
		 * Ein Fehler beim beenden des SimpleNetworkManagers
		 */
        SimpleNetworkManagerFinalize, /**
		 * Ein Fehler w�hrend dem Finalize des ClientThreads.
		 */
        ClientThreadFinalize, /**
		 * Falls beim Laden des Dispatchers eine EOF Exception auftaucht.
		 */
        DispatcherLoadEOF, /**
		 * Beim setzten der Netzwerkoptionen ist ein Fehler aufgetretten
		 */
        NetParamError, /**
		 * Die XML Konfigrationdateien konnten nicht gelesen werden.
		 */
        XMLFileReadError, /**
		 * Ein Fehler in der XML Datei,
		 */
        XMLFileError, /**
		 * XML Datei konnte nicht gelesen werden, weil ein Problem mit
		 * dem Parser auftratt.
		 */
        XMLParseError, /**
		 * Ein nicht valider XPath Ausdruck wurde zur Ausf�hrung gebracht.
		 */
        XPathError, /**
		 * Falls eine XML Datei nicht aus dem Jar Archiv kopiert werden kann.
		 */
        CouldNotCopyXMLFile, /**
		 * Falls ein Literal nicht in der Datei existiert.
		 */
        LiteralNotFound, /**
		 * Falls in der XML Konfigurationsdatei parameters.xml der gew�nschte Eintrag 
		 * nicht existiert.
		 */
        ParameterNotFound, /**
		 * Wird f�r JUnit Tests verwendet.
		 */
        Test, /**
		 * Die letzte ErrorNummer. Ist diese erreicht, folgen keine anderen mehr. Dies
		 * wird f�r die Vorbedingung der handleException Funktion verwendet.
		 */
        MAX
    }

    ;

    /**
	 * Verbindung zum NetworkManager
	 */
    private NetworkManager m_netManager = null;

    /**
	 * Damit locks von einem Client gel�scht werden k�nnen, der gar nicht mehr existiert.
	 */
    private LockManager m_lockManager = null;

    /**
	 * Serialisiert den Zugriff auf die Log Datei.
	 */
    private Object m_lock = null;

    /**
	 * Der Exceptionhandler kann f�r UnitTests in einen Testmodus gesetzt werden, indem er 
	 * nicht auf Fehler reagiert.
	 */
    private boolean m_testingMode = false;

    /**
	 * Hilfsklasse, die Exceptions speichert, damit sie verglichen werden.
	 * k�nnen. Wichtig f�r das Maskieren.
	 * 
	 * @author $LastChangedBy: martinschaub $
	 */
    private static final class ExceptionEvent {

        /**
		 * Absender der Exception.
		 */
        private Object m_sender = null;

        /**
		 * Exception-Typ
		 */
        private errors m_err = null;

        /**
		 * Erzeugt ein neues Exception Event. Es besteht aus 2 Teilen:
		 * der Exception-Art und dem Absender.
		 * 
		 * @param err
		 *            Die Exception-Art (vom Typ ExceptionHandler.errors)
		 * @param sender
		 *            der Absender der Exception
		 */
        private ExceptionEvent(errors err, Object sender) {
            assert err != null : "Der Errortyp darf keine Nullreferenz sein";
            assert sender != null : "Der Sender darf keine Nullreferenz sein";
            m_err = err;
            m_sender = sender;
        }

        /**
		 * Vergleicht ein ExceptionEvent mit einem anderen.
		 * @param err 
		 * 				der zu vergleichende Error
		 * @param sender 
		 * 				der zu vergleichende Absender
		 * @return <i>true</i>, falls die ExceptionEvents �bereinstimmen. <i>false </i> sonst
		 */
        private boolean equals(errors err, Object sender) {
            if (m_err == err && m_sender.equals(sender)) {
                return true;
            }
            return false;
        }
    }

    /**
	 * Privater Konstruktor, da die Klasse als Singleton realisiert ist.
	 */
    private ExceptionHandler() {
        assert s_instance == null : "Vorbedingung nicht erf�llt: ExceptionHandler Instanz existiert bereits";
        m_maskedEvents = new Vector<ExceptionEvent>(INITIAL_VECTOR_SIZE);
        m_lock = new Object();
    }

    /**
	 * Behandelt einen aufgetretenen Fehler.
	 * 
	 * @param throwable Der aufgetrettene Fehler.
	 * @param errorNumber
	 *            Die eindeutige Error Nummer nach dem errors enum. Dieser
	 *            Parameter entscheidet zudem, welche Fehlerbehandlungsroutine
	 *            aufgerufen wird.
	 * @param sender
	 *            Erzeuger der Exception
	 */
    public void handleException(Throwable throwable, ExceptionHandler.errors errorNumber, Object sender) {
        assert errorNumber.ordinal() > errors.NULL.ordinal() : "Vorbedingung nicht erf�llt: Zu kleine Errornummer";
        assert errorNumber.ordinal() < errors.MAX.ordinal() : "Vorbedingung nicht erf�llt: Zu grosse Errornummer";
        assert sender != null : "Erzeuger der Exception muss mitgeliefert werden";
        MessageDialog dialog = MessageDialog.getInstance();
        if (!isMaskedException(errorNumber, sender) && !m_testingMode) {
            assert m_netManager != null : "Vorbedingung nicht erf�llt: m_netManager ist null " + errorNumber;
            assert m_lockManager != null : "Vorbedingung nicht erf�llt: m_lockManager ist null";
            switch(errorNumber) {
                case WrongClientIDFormatClient:
                    writeLog(throwable, "Falsche ClientID vom Client.");
                    break;
                case WrongClientIDFormatServer:
                    writeLog(throwable, "Falsche ClientID vom Server.");
                    break;
                case WrongObjectReceived:
                    writeLog(throwable, "Falsches Objektformat");
                    break;
                case HandshakeError:
                    break;
                case NetworkSendingError:
                    maskException(errors.NetworkSendingError, sender);
                    maskException(errors.NetworkReceivingError, sender);
                    maskException(errors.WrongObjectReceived, sender);
                    removeClient(sender);
                    break;
                case NetworkReceivingError:
                    maskException(errors.NetworkReceivingError, sender);
                    maskException(errors.NetworkSendingError, sender);
                    maskException(errors.WrongObjectReceived, sender);
                    removeClient(sender);
                    break;
                case DestinationClientIDNotExist:
                    break;
                case InterruptedDuringGetID:
                    break;
                case ThreadUncaughtException:
                    writeLog(throwable, "Ein Thread ist abgest�rtzt.");
                    break;
                case ServerThreadFinalize:
                    break;
                case SimpleNetworkManagerFinalize:
                    break;
                case ClientThreadFinalize:
                    break;
                case DispatcherLoadEOF:
                    maskException(errorNumber, sender);
                    break;
                case NetParamError:
                    writeLog(throwable, "Falscher Socket Parameter");
                    break;
                case Test:
                    break;
                case XMLFileReadError:
                    writeLog(throwable, "XML Datei " + sender + " konnte nicht ge�ffnet werden.");
                    dialog.showMessageDialog("Could not open XML File " + sender + ".\nPlease consult the support.\n" + "Die XML Datei " + sender + " konnte nicht gelesen werden.\nBitte kontaktieren Sie ihren Administrator.", "Could not open XML File", JOptionPane.ERROR_MESSAGE);
                    break;
                case XMLFileError:
                    writeLog(throwable, "XML Datei " + sender + " ist korrupt.");
                    dialog.showMessageDialog("Could not parse XML File " + sender + ".\nPlease consult the support.\n" + "Die XML Konfigurationsdatei " + sender + " ist nicht korrekt.\nBitte kontaktieren Sie ihren Administrator.", "Could not open XML File", JOptionPane.ERROR_MESSAGE);
                    break;
                case XMLParseError:
                    writeLog(throwable, "Parser Error");
                    break;
                case XPathError:
                    writeLog(throwable, "XPath Error");
                    break;
                case CouldNotCopyXMLFile:
                    dialog.showMessageDialog("Could not copy XML file " + sender + "to our application directory.\n" + "Die XML Datei " + sender + " konnte nicht in das Verzeichnis der Applikation kopiert werden.", "Error Copy XML File", JOptionPane.ERROR_MESSAGE);
                    break;
                case LiteralNotFound:
                    dialog.showMessageDialog("Could not read literal " + throwable.getMessage() + " from XML file. Please consult your administrator.\n" + "Konnte den Text f�r " + throwable.getMessage() + " nicht lesen. Bitte kontaktieren Sie ihren Administrator.", "Error Reading XML File", JOptionPane.ERROR_MESSAGE);
                    break;
                case ParameterNotFound:
                    dialog.showMessageDialog("Could not read parameter " + throwable.getMessage() + " from XML file. Please consult your administrator.\n" + "Konnte die Einstellung f�r " + throwable.getMessage() + " nicht lesen. Bitte kontaktieren Sie ihren Administrator.", "Error Reading XML File", JOptionPane.ERROR_MESSAGE);
                    break;
                default:
                    assert false : "Unbekannter ErrorCode ";
                    break;
            }
        }
    }

    /**
	 * Maskiert einen Errortyp, der von einem bestimmten Absender kommt. 
	 * Das Tupel {error,sender} wird bei Aufruf von handleException() nicht mehr beachtet.
	 * @param err Der zu maskierende Error
	 * @param sender Der zu maskierende Sender
	 */
    public void maskException(errors err, Object sender) {
        if (!isMaskedException(err, sender)) {
            m_maskedEvents.addElement(new ExceptionEvent(err, sender));
        }
    }

    /**
	 * De-maskiert einen Errortyp, der von einem bestimmten Absender kommt. 
	 * Das Tupel {error,sender} wird bei Aufruf von handleException() wieder beachtet.
	 * @param err Der zu maskierende Error
	 * @param sender Der zu maskierende Sender
	 */
    public void unmaskException(errors err, Object sender) {
        if (isMaskedException(err, sender)) {
            m_maskedEvents.removeElement(getExceptionEvent(err, sender));
        }
    }

    /**
	 * Testet, ob ein {error, Absender}-Tupel maskiert ist oder nicht.
	 * 
	 * @param error zu testender Error
	 * @param sender zu testender Absender
	 * @return true, falls Tupel maskiert ist. Ansonsten false.
	 */
    public boolean isMaskedException(errors error, Object sender) {
        boolean known = false;
        for (int i = 0; i < m_maskedEvents.size(); i++) {
            if (m_maskedEvents.elementAt(i).equals(error, sender)) {
                known = true;
            }
        }
        return known;
    }

    /**
	 * Gibt die einzige Instanz des Objektes zur�ck. Wird die Methode das erste
	 * Mal aufgefufen, so wird das Objekt erzeugt.
	 * 
	 * @return Gibt das FehlerbehandlungsObjekt zur�ck.
	 */
    public static ExceptionHandler getInstance() {
        synchronized (ExceptionHandler.class) {
            if (s_instance == null) {
                s_instance = new ExceptionHandler();
            }
            return s_instance;
        }
    }

    /**
	 * Wird aufgerufen, wenn ein Thread wegen einer nicht behandelten Exception
	 * stirbt.
	 * 
	 * @param t der �bergebene Thread
	 * @param e die zu �bergebende Exception
	 * 
	 */
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        handleException(e, errors.ThreadUncaughtException, t);
    }

    /**
	 * Setzt den NetworkManager auf den �bergebenen Wert. Dies wird f�r
	 * Fehlerbehandlung gebraucht.
	 * 
	 * @param net
	 *            Der Netzwerkmanager, welcher f�r die Fehlerbehandlungen
	 *            verwendet wird.
	 */
    public void setNetworkManager(NetworkManager net) {
        assert net != null : "Vorbedingungen nicht erf�llt: net ist null";
        m_netManager = net;
    }

    /**
	 * Falls die Verbindung zu einem Client unterbrochen wurde oder dieser
	 * die Verbindung getrennt hat, so m�ssen alle Locks von ihm gel�scht werden.
	 * @param lockManager der verwendete LockManager.
	 */
    public void setLockManager(LockManager lockManager) {
        assert lockManager != null : "Vorbedingungen nicht erf�llt: lockManager ist null";
        m_lockManager = lockManager;
    }

    /**
	 * Hilfsmethode, die eine Referenz auf ein ExceptionEvent in m_maskedEvents zur�ckgibt.
	 * @param err Der Error, durch den das Event identifiziert wird
	 * @param sender Der Absender, durch den das Event identifiziert wird
	 * @return die Referenz auf das ExceptionEvent 
	 */
    private ExceptionEvent getExceptionEvent(errors err, Object sender) {
        assert isMaskedException(err, sender) : "ExceptionEvent muss vorhanden sein!";
        for (int i = 0; i < m_maskedEvents.size(); i++) {
            if (m_maskedEvents.elementAt(i).equals(err, sender)) {
                return m_maskedEvents.elementAt(i);
            }
        }
        return null;
    }

    /**
	 * Schreibt einen Fehlerbericht in eine Logdatei.
	 * @param throwable Exception, welche geloged werden soll.
	 * @param additonalMessage eine optionale, zus�tzlich Nachricht.
	 */
    private void writeLog(Throwable throwable, String additonalMessage) {
        synchronized (m_lock) {
            try {
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                format.setTimeZone(TimeZone.getDefault());
                FileWriter out = new FileWriter(LOGFILE, true);
                out.write("Fehlerbericht von " + format.format(cal.getTime()) + "\n");
                if (additonalMessage != null) {
                    out.write("Nachricht: " + additonalMessage + "\n");
                }
                out.write("Verbunden? " + m_netManager.isConnected() + " Client Nummer " + m_netManager.getClientID() + "\n");
                StackTraceElement[] elements = throwable.getStackTrace();
                out.write("Exception Nachricht: " + throwable.getMessage() + "\n");
                for (int i = 0; i < elements.length; ++i) {
                    out.write(elements[i].toString() + "\n");
                }
                out.write("\n");
                out.close();
            } catch (IOException e) {
                MessageDialog msg = MessageDialog.getInstance();
                msg.showErrorDialog("couldNotWriteLogfile");
            }
        }
    }

    /**
	 * Entfernt bei einem Netzwerkfehler die fehlerhaften Clients.
	 * @param sender Der CliendThread, der den Fehler gemeldet hat.
	 */
    private void removeClient(Object sender) {
        MessageDialog msg = MessageDialog.getInstance();
        if (m_netManager.isServer()) {
            m_netManager.removeClient((ClientThread) sender);
            m_lockManager.removeClient(((ClientThread) sender).getClientID());
            msg.showErrorDialog("clientDisconnectedServer");
        } else {
            m_netManager.disconnect();
            msg.showWarningDialog("clientDisconnectedClient");
        }
    }

    /**
	 * Bereitet die ExceptionHandler Instanz auf das L�schen vor.
	 * Wird anschliessend wieder �ber getInstance die Referenz geholt, 
	 * so wird dies ein anderer ExceptionHandler sein!
	 * @throws Throwable Falls der finalize der Superklasse eine Exception wirft.
	 */
    public void finalize() throws Throwable {
        super.finalize();
        synchronized (ExceptionHandler.class) {
            s_instance = null;
        }
    }

    /**
	 * Setzt bei true den ExceptionHandler in einen Zustand, in dem er keine Fehler mehr 
	 * behandelt
	 * @param test true, behandlet keine Fehler mehr -> false, behandlet neue Fehler
	 */
    public void setTestMode(boolean test) {
        m_testingMode = test;
    }
}
