package de.velian.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Fungiert als Wrapper der Klassen File und BufferedWriter.<br>
 * Für jedes geöffnete File wird auch ein BufferedWriter erzeugt
 * @author rogo
 */
public class File extends OutputStream {

    public static final int OPEN_WRITE_APPEND = 1;

    public static final int OPEN_OVERWRITE = 3;

    private java.io.File m_File;

    private BufferedWriter m_Writer;

    /**
	 * Versucht mit dem übergebenen Pfad eine Datei zu erzeugen und zum Schreiben zu öffnen<br>
	 * @param name relativer oder absoluter Pfad gefolgt vom Namen
	 * @throws FileException - wenn die Datei nicht erzeugt werden kann
	 * @throws FileNotFoundException
	 */
    public File(String name, int mode) throws FileException, FileNotFoundException {
        boolean read = false;
        boolean write = false;
        boolean append = false;
        switch(mode) {
            case OPEN_OVERWRITE:
                write = true;
                append = false;
                break;
            case OPEN_WRITE_APPEND:
                write = true;
                append = true;
                break;
        }
        this.init(name, read, write, append);
    }

    /** Falls bei der Erzeugung des Objektes die zu öffnende Datei noch nicht feststeht */
    public File() {
    }

    /**
	 * Versucht die durch <code>name</code> spezifizierte Datei zu öffnen.<br>
	 * - Überprüfung ob die Datei existiert<br>
	 * - existiert die Datei nicht und soll nur zum Lesen geöffnet werden -> Fehler<br>
	 * - existiert die Datei nicht, wird sie erzeugt<br>
	 * - ansonsten Überprüfung ob angehängt werden soll<br>
	 * 		- ja	-> keine Aktion<br>
	 * 		- nein	-> Datei wird gelöscht und neu erzeugt<br>
	 * - Überprüfung ob zum Schreiben geöffnet werden soll; nur wenn "true", wird ein BufferedWriter erzeugt<br>
	 * @param name
	 * @param read
	 * @param write
	 * @param append
	 * @throws FileException - wenn die Datei nicht erzeugt werden kann
	 * @throws FileNotFoundException
	 */
    public void init(String name, boolean read, boolean write, boolean append) throws FileException, FileNotFoundException {
        this.m_File = new java.io.File(name);
        boolean fileExists = this.m_File.exists();
        if (!fileExists && read && !write) throw new FileNotFoundException("File doesn't exist!");
        if (!fileExists) this.m_File = createFile(name); else if (!append) {
            this.m_File.delete();
            this.m_File = createFile(name);
        }
        if (write) this.m_Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.m_File)));
    }

    /**
	 * Erzeugt eine Datei des übergebenen Namens und alle nötigen übergeordneten Verzeichnisse,
	 * falls der volle Pfad angegeben ist.<br>
	 */
    public static java.io.File createFile(String name) throws FileException {
        java.io.File file = new java.io.File(name);
        String parent = file.getParent();
        if (parent != null) new java.io.File(parent).mkdirs();
        try {
            if (!file.createNewFile()) throw new FileException("Couldn't create File " + name + "!");
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e);
            throw new FileException(e.getCause());
        }
        return file;
    }

    /**
	 * Schreibt den übergebenen String als Zeile in die Datei<br>
	 * @param line
	 * @throws IllegalAccessException - wenn keine Dateizugriffsberechtigung besteht
	 * @throws IllegalArgumentException - wenn die zu schreibende line ein Nullpointer ist
	 * @throws IOException
	 */
    public void writeLine(String line) throws IllegalAccessException, IllegalArgumentException, IOException {
        this.testFile(false, true);
        if (line == null) throw new IllegalArgumentException("[File.writeLine] : Line to write is null.");
        this.m_Writer.write(line);
    }

    /**
	 * Schreibt einen Text in die Datei und hängt danach einen Linefeed an.<br>
	 * @param text
	 * @throws IllegalAccessException - wenn keine Dateizugriffsberechtigung besteht
	 * @throws IllegalArgumentException - wenn die zu schreibende line ein Nullpointer ist
	 * @throws IOException
	 */
    public void writeText(String text) throws IllegalAccessException, IllegalArgumentException, IOException {
        this.writeLine(text);
    }

    public void linefeeds(int feeds) {
        try {
            this.testFile(false, true);
            for (int i = 0; i < feeds; i++) this.m_Writer.newLine();
            this.flush();
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(null, e, false, true, false, 0);
        }
    }

    public void flush() {
        try {
            this.m_Writer.flush();
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(null, e, false, true, false, 0);
        }
    }

    /**
	 * Flusht den Stream, schließt den <code>BufferedWriter</code> und finalized das Objekt<br>
	 */
    protected void finalize() throws Throwable {
        if (this.m_Writer != null) {
            this.flush();
            this.m_Writer.close();
        }
        this.m_Writer = null;
        this.m_File = null;
        super.finalize();
    }

    /**
	 * räumt dieses Objekt auf<br>
	 */
    public void close() {
        try {
            this.finalize();
        } catch (Throwable t) {
            ExceptionHandler.getInstance().handleThrowable(null, t, false);
        }
    }

    protected void testFile(boolean read, boolean write) throws IllegalAccessException, NullPointerException {
        if (this.m_File == null) throw new NullPointerException("This File-Object is null!");
        if (write && this.m_Writer == null) throw new IllegalAccessException("This File was opened read-only!");
    }

    public static class FileException extends AbstractException {

        private static final long serialVersionUID = 1L;

        protected FileException(String message) {
            super(message);
        }

        protected FileException(Throwable cause) {
            super(cause);
        }
    }

    @Override
    public void write(int b) throws IOException {
        this.m_Writer.write((byte) b);
    }
}
