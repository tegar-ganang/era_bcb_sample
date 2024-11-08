package de.mnit.basis.sys.datei;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import de.mnit.basis.daten.konstant.sys.SYSTEM;
import de.mnit.basis.fehler.Fehler;
import de.mnit.basis.fehler.system.F_Sys_Datei;
import de.mnit.basis.fehler.system.F_Sys_InOut;
import de.mnit.basis.sys.Ausgabe;
import de.mnit.basis.sys.Sys;

/**
 * 05.12.2008	Kopieren überarbeitet, ist nun schneller, stabiler, besseres Fehlermanagement und funzt auch mit großen Dateien
 *
 * "start" öffnet Dateien mit der Standard-Anwendung;
 * Für den Start von speziellen Anwendungen usw, siehe DirektStarter
 *
 * TODO Mit DateiSystem abgleichen!!!
 */
public class DateiSystem {

    public static Process start(S_DS_Element ds) throws F_Sys_Datei {
        if (SYSTEM.gAktuell().istWindows()) return startWindows(ds.gPfadKomplett());
        if (SYSTEM.gAktuell().istLinux()) return startLinux(ds.gPfadKomplett());
        throw Fehler.sonstige.da_ToDo("Betriebssystem nicht unterstützt", SYSTEM.gAktuell());
    }

    private static Process startWindows(String pfad) throws F_Sys_Datei {
        try {
            return new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", pfad).start();
        } catch (IOException e) {
            throw F_Sys_Datei.neu("Dateifehler", e, pfad);
        }
    }

    private static Process startLinux(String pfad) throws F_Sys_Datei {
        try {
            String[] parameter = null;
            String env = System.getenv("DESKTOP_SESSION");
            Fehler.objekt.wenn_Null(env);
            if (env.equals("gnome")) parameter = new String[] { "gnome-open", pfad };
            if (env.equals("xfce4")) parameter = new String[] { "gnome-open", pfad };
            if (env.equals("kde")) parameter = new String[] { "kfmclient", "exec", pfad };
            if (env.equals("default")) parameter = new String[] { "gnome-open", pfad };
            if (parameter == null) Fehler.direkt("Unbekanntes Desktop-System", env);
            return new ProcessBuilder(parameter).start();
        } catch (IOException e) {
            throw F_Sys_Datei.neu("Dateifehler", e, pfad);
        }
    }

    public static boolean umbenennen(File von, File zu) throws F_Sys_Datei {
        Fehler.datei.wenn_Fehlt(von);
        Fehler.datei.wenn_Existiert(zu);
        return von.renameTo(zu);
    }

    public static void kopierenDatei(File von, File zu) throws F_Sys_InOut {
        if (!von.isFile()) Fehler.sonstige.da_Verboten("Ordner statt Datei", von, zu);
        Fehler.datei.wenn_Fehlt(von);
        Fehler.datei.wenn_Existiert(zu);
        iDateiKopieren(von, zu);
    }

    public static void verschieben(File von, File zu) throws F_Sys_Datei {
        boolean erg = von.renameTo(zu);
        if (erg != true) throw F_Sys_Datei.neu("Verschieben gescheitert", von, zu);
    }

    /**
	 * Legt, wenn möglich, alle Verzeichnisse so an, dass dieser Pfad gültig ist.
	 */
    public static void pfadAnlegen(String pfad) throws F_Sys_Datei {
        String t = Sys.gTrennerVerz();
        String[] elemente = pfad.split(t);
        Ausgabe.debug(elemente, t);
        String aktuell = SYSTEM.gAktuell().istWindows() ? "" : t;
        boolean erster = true;
        for (String element : elemente) {
            if (element.length() == 0) continue;
            if (erster) erster = false; else aktuell += t;
            aktuell += element;
            File f = new File(aktuell);
            if (!f.exists()) if (!f.mkdir()) throw F_Sys_Datei.neu("Verzeichnis konnte nicht angelegt werden!", aktuell);
        }
    }

    public static byte[] lies(S_Datei datei) throws F_Sys_Datei {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(datei.gFile(), "r");
            Vector<Byte> v = new Vector<Byte>();
            while (raf.getFilePointer() < raf.length()) v.add(raf.readByte());
            v.toArray(new Byte[0]);
            byte[] ba = new byte[v.size()];
            for (int p = 0; p < v.size(); p++) ba[p] = v.get(p);
            return ba;
        } catch (Exception err) {
            throw Fehler.datei.da_Zugriffsfehler(err);
        } finally {
            try {
                if (raf != null) raf.close();
            } catch (Exception e) {
                Fehler.zeig(e, false);
            }
        }
    }

    /**
	 * Cp850 = DOS
	 */
    public static String lies(S_Datei datei, String zeichensatz) throws F_Sys_Datei, UnsupportedEncodingException {
        byte[] z = DateiSystem.lies(datei);
        return new String(z, zeichensatz);
    }

    private static void iDateiKopieren(File von, File nach) throws F_Sys_InOut {
        byte[] buffer = new byte[8192];
        int read = 0;
        RandomAccessFile raf_von = null;
        RandomAccessFile raf_nach = null;
        try {
            raf_von = new RandomAccessFile(von, "r");
            raf_nach = new RandomAccessFile(nach, "rw");
            raf_nach.setLength(raf_von.length());
            while ((read = raf_von.read(buffer)) > 0) raf_nach.write(buffer, 0, read);
        } catch (IOException e) {
            throw Fehler.weitergeben(e, "Datei kann nicht verarbeitet werden!");
        } finally {
            try {
                raf_von.close();
            } catch (IOException e) {
                Ausgabe.fehler(e, "Datei kann nicht geschlossen werden!");
            } finally {
                try {
                    raf_nach.close();
                } catch (IOException e) {
                    Ausgabe.fehler(e, "Datei kann nicht geschlossen werden!");
                }
            }
        }
    }
}
