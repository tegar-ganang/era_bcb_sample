package nz.org.venice.macro;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import nz.org.venice.prefs.PreferencesManager;

/**
 * @author Dan Makovec venice@makovec.net
 *
 * Reference object for a Jython macro that has been imported into the application
 */
public class StoredMacro {

    /** Name of the stored macro. */
    private String name = "";

    /** Filename holding the macro */
    private String filename = "";

    /** The macro's code */
    private String code = "";

    /** Whether the macro is to be run automatically on startup */
    private boolean on_startup = false;

    /** If the macro is to be run at startup, this is the sequence number */
    private int start_sequence = -1;

    /** If the macro is to be visible from the Macros menu */
    private boolean in_menu = false;

    /** Empty constructor for creating new macros */
    public StoredMacro() {
    }

    /**
     * @param name
     * @param filename
     * @param on_startup
     * @param start_sequence
     * @param in_menu
     */
    public StoredMacro(String name, String filename, boolean on_startup, int start_sequence, boolean in_menu) {
        this.name = name;
        this.filename = filename;
        this.on_startup = on_startup;
        this.start_sequence = start_sequence;
        this.in_menu = in_menu;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        if (filename.length() == 0) {
            return "";
        }
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the on_startup.
     */
    public boolean isOn_startup() {
        return on_startup;
    }

    /**
     * @param on_startup The on_startup to set.
     */
    public void setOn_startup(boolean on_startup) {
        this.on_startup = on_startup;
    }

    /**
     * @return Returns the start_sequence.
     */
    public int getStart_sequence() {
        return start_sequence;
    }

    /**
     * @param start_sequence The start_sequence to set.
     */
    public void setStart_sequence(int start_sequence) {
        this.start_sequence = start_sequence;
    }

    /**
     * @return Returns the in_menu.
     */
    public boolean isIn_menu() {
        return in_menu;
    }

    /**
     * @param in_menu The in_menu to set.
     */
    public void setIn_menu(boolean in_menu) {
        this.in_menu = in_menu;
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        if (filename.length() == 0) {
            return "";
        }
        return filename;
    }

    /**
     * @param filename The filename to set.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Load the macro into memory
     */
    public boolean load() {
        if (getFilename() != null && getFilename().length() > 0) {
            try {
                File file = new File(PreferencesManager.getDirectoryLocation("macros") + File.separator + getFilename());
                URL url = file.toURL();
                InputStreamReader isr = new InputStreamReader(url.openStream());
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();
                String macro_text = "";
                while (line != null) {
                    macro_text = macro_text.concat(line);
                    line = br.readLine();
                    if (line != null) {
                        macro_text = macro_text.concat(System.getProperty("line.separator"));
                    }
                }
                code = macro_text;
            } catch (Exception e) {
                System.err.println("Exception at StoredMacro.load(): " + e.toString());
                return false;
            }
        }
        return true;
    }

    public boolean save() {
        if (getFilename() != null) {
            try {
                File file = new File(PreferencesManager.getMacroHome() + File.separator + getFilename());
                URI uri = file.toURI();
                FileOutputStream fos = new FileOutputStream(new File(uri));
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(this.getCode(), 0, this.getCode().length());
                bw.flush();
                bw.close();
                MacroManager.uncacheCompiledMacro(this);
            } catch (Exception e) {
                System.err.println("Exception at StoredMacro.save(): " + e.toString());
                return false;
            }
        }
        return true;
    }

    public boolean delete() {
        try {
            File file = new File(PreferencesManager.getMacroHome() + File.separator + getFilename());
            file.delete();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * @return Returns the code.
     */
    public String getCode() {
        if (code == "") {
            load();
        }
        if (code == null) {
            return "";
        }
        return code;
    }

    /**
     * @param code The code to set.
     */
    public void setCode(String code) {
        this.code = code;
    }
}
