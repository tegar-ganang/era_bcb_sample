package jgame.platform;

import jgame.impl.JGameError;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.colorchooser.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

/** A generic class for handling the user configuration of application
 * settings.  It manages a set of fields that can be loaded, saved, and
 * edited. It can load and save configurations from/to both a file and an
 * object (reading/setting fields in the object), and it can create a GUI for
 * editing them.  It can also be run on just an object (excluding the
 * possibility of saving to a file) or on just a file.  The class has a
 * standard main() for editing a previously saved file, use <code>java
 * jgame.AppConfig [filename]</code>.

 * <p>The types and names of the fields can be obtained by either loading them
 * from a previously saved file, or by deriving them from the object to be
 * configured.  AppConfig maintains a copy of the configurable fields and
 * their types.  You can choose when to load or save the contents of the
 * AppConfig object to/from object or file.  The GUI comes with a "Save"
 * button (save to file and to object) and an "Apply" button (save to object,
 * not to file).  This class does not handle mismatches between fields in an
 * object and in a corresponding file very robustly yet.

 * <p>The Gui consists of a list of editable form fields.  The name to
 * display next to each field can be set manually or determined automatically
 * from the field name in the object.  The order in which the Gui fields are
 * displayed is the same as that in which they are defined.

 * <p>GameConfig is able to handle several types of fields.  It determines the
 * type of Gui element and the file representation automatically from the
 * type.  The following file format is used: each field is found on a separate
 * line, in the format [fieldname] '\t' [guiname] '\t' [type] '\t' [value] It
 * handles the following field types:

 *<p>
 * <pre>
 * Class/Type    GUI representation    file representation
 * int            number field          int [number]
 * int            keystroke field       key [keycode]
 * double         number field          double [number]
 * boolean        button                boolean [true/false]
 * String         textfield             String [string, \n is escaped as '\n']
 * </pre>

 */
public class AppConfig {

    /** The list of field names; this defined the order in which the fields
	 * are to be displayed and saved in a file. */
    Vector fields = new Vector(20, 50);

    /** String (fieldname) to type */
    Hashtable fieldtypes = new Hashtable();

    /** String (fieldname) to String (GUI name) */
    Hashtable fieldguinames = new Hashtable();

    /** The GUI components corresponding to the fields */
    Hashtable fieldcomponents = new Hashtable();

    /** The fields' values */
    Hashtable fieldvalues = new Hashtable();

    /** The object in which the fields are to be configured, null means either
	 * only the class is defined or we have a file only. */
    Object obj = null;

    /** The class in which the (static) fields are to be configured.  If
	 * obj!=null, is the same as the class of obj. */
    Class cls = null;

    /** The file in which the fields are to be configured, null means none
	 * (object only). */
    String filename = null;

    String title;

    public String toString() {
        return title;
    }

    Object gui_lock = new Object();

    boolean gui_open = false;

    /** Listener, currently only used for the quit event. */
    ActionListener listener = null;

    Font mainfont = new Font("Helvetica", 0, 16);

    Font bigfont = new Font("Helvetica", Font.BOLD, 20);

    Color textcolor = new Color(0, 0, 100);

    Color bgcolor = new Color(180, 180, 255);

    Color hltcolor = new Color(230, 230, 255);

    Border border = BorderFactory.createEtchedBorder(hltcolor, textcolor);

    /** Configure the look of the GUI
	* @param mainfont  font used for labels and fields
	* @param bigfont  font used for title and buttons
	* @param textcolor  colour used for all text
	* @param bgcolor  colour used for background
	* @param hltcolor  colour used for highlighted background and fields
	*/
    public void setGuiSettings(Font mainfont, Font bigfont, Color textcolor, Color bgcolor, Color hltcolor) {
        this.mainfont = mainfont;
        this.bigfont = bigfont;
        this.textcolor = textcolor;
        this.bgcolor = bgcolor;
        this.hltcolor = hltcolor;
    }

    JPanel mainpanel = new JPanel();

    JPanel butpanel = new JPanel();

    JPanel toppanel = new JPanel();

    JButton save_but = new JButton("Save");

    JButton apply_but = new JButton("Apply");

    JButton cancel_but = new JButton("Close");

    JFrame topframe = new JFrame("Application Config");

    JScrollPane scrollpane = null;

    /** Edit a previously created file. The first argument
	 * is the filepath. */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Please supply filename of config to edit.");
        }
        AppConfig appconf = new AppConfig(args[0], args[0]);
        appconf.loadFromFile();
        appconf.openGui();
        appconf.waitUntilGuiClosed();
        System.exit(0);
    }

    /** Get the object that this AppConfig is associated with, null if not
	 * defined. */
    public Object getObject() {
        return obj;
    }

    /** Set or replace the object that this AppConfig should be associated
	* with. */
    public void setObject(Object obj) {
        this.obj = obj;
        this.cls = obj.getClass();
    }

    /** Get the class that this AppConfig is associated with, null if only
	 * a file is defined. */
    public Class getObjectClass() {
        return cls;
    }

    /** Get the filespec of the file that this AppConfig is associated with,
	* null if only object or class is defined. */
    public String getFilename() {
        return filename;
    }

    /** Configure a file.  No settings are defined or loaded from
	 * the file. */
    public AppConfig(String title, String filename) {
        this.title = title;
        this.filename = filename;
        initGui();
    }

    /** Configure an object with corresponding file. No settings are defined
	 * or loaded from the object or file.
	 * @param filename  filespec, or null if no file */
    public AppConfig(String title, Object obj, String filename) {
        this.title = title;
        this.filename = filename;
        this.obj = obj;
        this.cls = obj.getClass();
        initGui();
    }

    /** Configure a class with static fields with corresponding file. No
	* settings are defined or loaded from the object or file.
	 * @param filename  filespec, or null if no file */
    public AppConfig(String title, Class cls, String filename) {
        this.title = title;
        this.filename = filename;
        this.cls = cls;
        initGui();
    }

    /** Configure an object without the possibility of file storage.  No
	 * settings are defined or loaded from the object. */
    public AppConfig(String title, Object obj) {
        this.title = title;
        this.obj = obj;
        this.cls = obj.getClass();
        initGui();
    }

    /** Configure a class with static fields without the possibility of
	* file storage.  No settings are defined or loaded from the object. */
    public AppConfig(String title, Class cls) {
        this.title = title;
        this.cls = cls;
        initGui();
    }

    void initGui() {
        mainpanel.setBackground(bgcolor);
        butpanel.setBackground(bgcolor);
        toppanel.setBackground(bgcolor);
        save_but.setBackground(bgcolor);
        save_but.setForeground(textcolor);
        save_but.setFont(bigfont);
        apply_but.setBackground(bgcolor);
        apply_but.setForeground(textcolor);
        apply_but.setFont(bigfont);
        cancel_but.setBackground(bgcolor);
        cancel_but.setForeground(textcolor);
        cancel_but.setFont(bigfont);
        mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
        if (filename != null) {
            butpanel.add(save_but);
            save_but.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    saveToFile();
                    if (cls != null) saveToObject();
                    topframe.setVisible(false);
                    signalCloseGui();
                }
            });
        }
        if (cls != null) {
            butpanel.add(apply_but);
            apply_but.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    saveToObject();
                    topframe.setVisible(false);
                    signalCloseGui();
                }
            });
        }
        butpanel.add(cancel_but);
        cancel_but.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                topframe.setVisible(false);
                signalCloseGui();
            }
        });
        toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.Y_AXIS));
        JLabel titlelabel = new JLabel(title);
        JPanel titlepanel = new JPanel();
        titlepanel.setBackground(bgcolor);
        titlelabel.setForeground(textcolor);
        titlelabel.setFont(bigfont);
        titlepanel.setLayout(new BoxLayout(titlepanel, BoxLayout.X_AXIS));
        titlepanel.add(titlelabel);
        toppanel.add(titlepanel);
        toppanel.add(mainpanel);
        toppanel.add(butpanel);
        scrollpane = new JScrollPane(toppanel);
        topframe.getContentPane().add(scrollpane);
        topframe.setLocation(50, 50);
        topframe.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                topframe.setVisible(false);
                signalCloseGui();
            }
        });
    }

    /** Set listener for special events.  Currently, there is only the quit
	 * event: the action listener is called when the appconfig window is
	 * closed. 
	 * @param list  null means disable */
    public void setListener(ActionListener list) {
        listener = list;
    }

    /** Load settings from file.  Is ignored if filename is not specified or
	 * file does not exist.  Throws error if i/o error occurs during read.  If
	 * a field name found in the file is not yet defined, it is defined here.
	 * Behaviour is undefined if fields in object and file do not match. */
    public void loadFromFile() {
        if (filename == null) return;
        try {
            BufferedReader reader;
            try {
                File file = new File(filename);
                if (!file.exists()) return;
                reader = new BufferedReader(new FileReader(file));
            } catch (java.security.AccessControlException e) {
                URL url = new URL(filename);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) break;
                String name = line.substring(0, line.indexOf("\t"));
                String rest1 = line.substring(line.indexOf("\t") + 1);
                String guiname = rest1.substring(0, rest1.indexOf("\t"));
                String rest2 = rest1.substring(rest1.indexOf("\t") + 1);
                String type = rest2.substring(0, rest2.indexOf("\t"));
                String value = rest2.substring(rest2.indexOf("\t") + 1);
                defineField(name, guiname, type);
                setField(name, value, true);
            }
            reader.close();
        } catch (IOException e) {
            throw new JGameError("Error reading file '" + filename + "'.", false);
        }
    }

    /** Save settings to file.  Is ignored if filename is not
	 * specified.  Throws error if file cannot be accessed. */
    public void saveToFile() {
        try {
            if (filename == null) return;
            PrintWriter writer = new PrintWriter(new FileWriter(new File(filename)));
            for (int i = 0; i < fields.size(); i++) {
                String fieldname = (String) fields.get(i);
                String fieldguiname = (String) fieldguinames.get(fieldname);
                Object fieldvalue = fieldvalues.get(fieldname);
                String fieldtype = (String) fieldtypes.get(fieldname);
                writer.println(fieldname + "\t" + fieldguiname + "\t" + fieldtype + "\t" + fieldvalue);
            }
            writer.close();
        } catch (IOException e) {
            throw new JGameError("Error writing file '" + filename + "'.", false);
        }
    }

    /** Load settings from previously specified fields in object. */
    public void loadFromObject() {
        try {
            if (cls == null) return;
            for (int i = 0; i < fields.size(); i++) {
                String fieldname = (String) fields.get(i);
                String fieldtype = (String) fieldtypes.get(fieldname);
                if (fieldtype.equals("key") || fieldtype.equals("int") || fieldtype.equals("double") || fieldtype.equals("boolean") || fieldtype.equals("String")) {
                    Field field = cls.getField(fieldname);
                    fieldvalues.put(fieldname, field.get(obj));
                } else {
                    System.out.println("not implemented!");
                }
            }
        } catch (NoSuchFieldException e) {
            throw new JGameError("Field not found.");
        } catch (IllegalAccessException e) {
            throw new JGameError("Field cannot be accessed.");
        }
    }

    /** Save settings into object.   */
    public void saveToObject() {
        String fieldname = null;
        try {
            if (cls == null) return;
            for (int i = 0; i < fields.size(); i++) {
                fieldname = (String) fields.get(i);
                Object fieldvalue = fieldvalues.get(fieldname);
                String fieldtype = (String) fieldtypes.get(fieldname);
                if (fieldtype.equals("key") || fieldtype.equals("int") || fieldtype.equals("double") || fieldtype.equals("boolean") || fieldtype.equals("String")) {
                    Field field = cls.getField(fieldname);
                    field.set(obj, fieldvalue);
                } else {
                    System.out.println("not implemented!");
                }
            }
        } catch (NoSuchFieldException e) {
            System.out.println("Cls:" + cls);
            throw new JGameError("Field " + fieldname + " not found.");
        } catch (IllegalAccessException e) {
            throw new JGameError("Field " + fieldname + " cannot be accessed.");
        } catch (IllegalArgumentException e) {
            throw new JGameError("Illegal field argument for field " + fieldname + ".");
        }
    }

    /** Define a field as configurable, ignore if field already exists.  If
	 * guiname is null, fieldname is used as the name to display in the GUI.
	 * The type is given by type.  The value of the field is set to the value
	 * found in the object or class, if defined, or null otherwise.  If we
	 * have only a class, the field must be static.
	 */
    public void defineField(String fieldname, String guiname, String type) {
        try {
            if (fields.contains(fieldname)) return;
            if (guiname == null) guiname = fieldname;
            fields.add(fieldname);
            fieldtypes.put(fieldname, type);
            fieldguinames.put(fieldname, guiname);
            addGuiComponent(fieldname);
            if (cls != null) {
                Field field = cls.getField(fieldname);
                setField(fieldname, field.get(obj), true);
            }
        } catch (NoSuchFieldException e) {
            System.out.println("Cls:" + cls);
            throw new JGameError("Field " + fieldname + " not found.");
        } catch (IllegalAccessException e) {
            throw new JGameError("Field " + fieldname + " has access error.");
        }
    }

    /** @return null if not known */
    String classToType(Class cls) {
        if (cls == Point.class) {
            return "int2";
        } else if (cls == Integer.TYPE || cls == Integer.class) {
            return "int";
        } else if (cls == Double.TYPE || cls == Double.class) {
            return "double";
        } else if (cls == String.class) {
            return "String";
        } else if (cls == Boolean.TYPE || cls == Boolean.class) {
            return "boolean";
        } else {
            return null;
        }
    }

    /** Define a set of fields as configurable, by matching the object's
	 * fields with the given pattern.  
	 * Field type is derived from the field.
	 * @see #defineFields(String,String,String,String,String)
	 */
    public void defineFields(String prefix, String suffix, String guiprefix, String guisuffix) {
        defineFields(prefix, suffix, guiprefix, guisuffix, null);
    }

    /** Define a set of fields as configurable, by matching the object's
	 * fields with the given pattern.  The configured object is searched for
	 * public fields matching the given prefix and suffix.  The name displayed
	 * in the gui is obtained by removing the prefix and suffix, and adding
	 * the given guiprefix and guisuffix.  Note that underscores in the fields
	 * are replaced by spaces.
	 * @param type  fieldtype, null means derive from field
	 * @see #defineField(String,String,String)
	 */
    public void defineFields(String prefix, String suffix, String guiprefix, String guisuffix, String type) {
        Field[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (!Modifier.isPublic(fields[i].getModifiers())) continue;
            String fieldname = fields[i].getName();
            if (fieldname.startsWith(prefix) && fieldname.endsWith(suffix)) {
                String guiname = guiprefix + fieldname.substring(prefix.length(), fieldname.length() - suffix.length()) + guisuffix;
                String fieldtype = type;
                if (fieldtype == null) {
                    fieldtype = classToType(fields[i].getType());
                }
                defineField(fieldname, guiname, fieldtype);
            }
        }
    }

    /** Set field's value, given that field is already defined.  Value may be
	* any class or String for String representations of fields. */
    public void setField(String field, Object value, boolean update_gui) {
        String fieldtype = (String) fieldtypes.get(field);
        if (fieldtype.equals("String")) {
            if (value != null) {
                fieldvalues.put(field, value);
            } else {
                fieldvalues.remove(field);
            }
        } else if (!(value instanceof String)) {
            fieldvalues.put(field, value);
        } else {
            String valstr = (String) value;
            if (fieldtype.equals("int")) {
                try {
                    fieldvalues.put(field, new Integer(Integer.parseInt(valstr)));
                } catch (Exception e) {
                }
            } else if (fieldtype.equals("key")) {
                try {
                    fieldvalues.put(field, new Integer(Integer.parseInt(valstr)));
                } catch (Exception e) {
                }
            } else if (fieldtype.equals("double")) {
                try {
                    fieldvalues.put(field, new Double(Double.parseDouble(valstr)));
                } catch (Exception e) {
                }
            } else if (fieldtype.equals("boolean")) {
                fieldvalues.put(field, new Boolean(valstr.equals("true")));
            } else if (fieldtype.equals("int2")) {
                throw new JGameError("Unimplemented type " + fieldtype, false);
            } else if (fieldtype.equals("int4")) {
                throw new JGameError("Unimplemented type " + fieldtype, false);
            } else if (fieldtype.equals("File")) {
                throw new JGameError("Unimplemented type " + fieldtype, false);
            } else if (fieldtype.equals("Color")) {
                throw new JGameError("Unimplemented type " + fieldtype, false);
            } else if (fieldtype.equals("Font")) {
                throw new JGameError("Unimplemented type " + fieldtype, false);
            } else {
                throw new JGameError("Unknown type " + fieldtype, false);
            }
        }
        if (update_gui) updateGui(field);
    }

    void addGuiComponent(String fieldname) {
        String fieldguiname = (String) fieldguinames.get(fieldname) + "  ";
        String fieldtype = (String) fieldtypes.get(fieldname);
        Object fieldval = fieldvalues.get(fieldname);
        JPanel subpanel = new JPanel(new GridLayout(1, 2));
        subpanel.setBackground(bgcolor);
        mainpanel.add(subpanel);
        JLabel namelabel = new JLabel(fieldguiname, JLabel.RIGHT);
        namelabel.setBackground(bgcolor);
        namelabel.setForeground(textcolor);
        namelabel.setFont(mainfont);
        if (fieldtype.equals("int") || fieldtype.equals("double")) {
            JTextField numfield = new JTextField("" + fieldval, 8);
            numfield.addCaretListener(new FieldChangeHandler(fieldname) {

                public void caretUpdate(CaretEvent e) {
                    JTextField tf = (JTextField) fieldcomponents.get(this.fieldname);
                    processAction(this.fieldname, tf.getText());
                }
            });
            if (fieldtype.equals("int")) {
                numfield.setToolTipText("Enter an integer.");
            } else {
                numfield.setToolTipText("Enter a real number.");
            }
            numfield.setFont(mainfont);
            numfield.setForeground(textcolor);
            numfield.setBackground(hltcolor);
            fieldcomponents.put(fieldname, numfield);
            subpanel.add(namelabel);
            subpanel.add(numfield);
        } else if (fieldtype.equals("String")) {
            JTextField strfield = new JTextField("" + fieldval, 8);
            strfield.addCaretListener(new FieldChangeHandler(fieldname) {

                public void caretUpdate(CaretEvent e) {
                    JTextField tf = (JTextField) fieldcomponents.get(this.fieldname);
                    processAction(this.fieldname, tf.getText());
                }
            });
            strfield.setToolTipText("Enter a text.");
            strfield.setFont(mainfont);
            strfield.setForeground(textcolor);
            strfield.setBackground(hltcolor);
            fieldcomponents.put(fieldname, strfield);
            subpanel.add(namelabel);
            subpanel.add(strfield);
        } else if (fieldtype.equals("boolean")) {
            JCheckBox boolfield = new JCheckBox("yes");
            boolfield.addItemListener(new FieldChangeHandler(fieldname) {

                public void itemStateChanged(ItemEvent e) {
                    Boolean val = new Boolean(e.getStateChange() == ItemEvent.SELECTED);
                    processAction(this.fieldname, val);
                }
            });
            boolfield.setFont(mainfont);
            boolfield.setForeground(textcolor);
            boolfield.setBackground(bgcolor);
            fieldcomponents.put(fieldname, boolfield);
            subpanel.add(namelabel);
            subpanel.add(boolfield);
        } else if (fieldtype.equals("key")) {
            KeyField keyfield = new KeyField(fieldname, bgcolor, hltcolor);
            fieldcomponents.put(fieldname, keyfield);
            keyfield.setToolTipText("Click then press a key to define keystroke.");
            keyfield.setFont(mainfont);
            keyfield.setForeground(textcolor);
            subpanel.add(namelabel);
            subpanel.add(keyfield);
        }
    }

    class FieldChangeHandler implements ActionListener, ChangeListener, ItemListener, CaretListener {

        String fieldname = null;

        Component component;

        public FieldChangeHandler(String fname) {
            fieldname = fname;
        }

        public FieldChangeHandler(String fname, Component comp) {
            fieldname = fname;
            component = comp;
        }

        public FieldChangeHandler(Component comp) {
            component = comp;
        }

        public void actionPerformed(ActionEvent e) {
            System.out.println(e);
        }

        public void stateChanged(ChangeEvent e) {
            System.out.println(e);
        }

        public void itemStateChanged(ItemEvent e) {
            System.out.println(e);
        }

        public void caretUpdate(CaretEvent e) {
            System.out.println(e);
        }
    }

    /** Process Gui change. */
    void processAction(String name, Object value) {
        setField(name, value, false);
    }

    /** Make the Gui visible on screen, and let the user make a selection
	 * whether to save, apply, or just close.  */
    public void openGui() {
        gui_open = true;
        topframe.pack();
        topframe.setVisible(true);
        topframe.requestFocus();
    }

    /** Hide the Gui in case it was visible. */
    public void closeGui() {
        topframe.setVisible(false);
        signalCloseGui();
    }

    void signalCloseGui() {
        synchronized (gui_lock) {
            gui_open = false;
            gui_lock.notifyAll();
        }
        if (listener != null) listener.actionPerformed(null);
    }

    /** Check if GUI is visible right now. */
    public boolean isGuiOpen() {
        return gui_open;
    }

    /** Block until the Gui closes. */
    public void waitUntilGuiClosed() {
        synchronized (gui_lock) {
            try {
                while (gui_open) {
                    gui_lock.wait();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    /** Update gui component value of given field name */
    void updateGui(String fieldname) {
        Object fieldval = fieldvalues.get(fieldname);
        Object fieldcom = fieldcomponents.get(fieldname);
        String fieldtype = (String) fieldtypes.get(fieldname);
        if (fieldcom instanceof JCheckBox) {
            ((JCheckBox) fieldcom).setSelected(((Boolean) fieldval).booleanValue());
        } else if (fieldcom instanceof JColorChooser) {
            JColorChooser jcc = (JColorChooser) fieldcom;
            jcc.setColor((Color) fieldval);
        } else if (fieldcom instanceof KeyField) {
            ((KeyField) fieldcom).setValue(((Integer) fieldval).intValue());
        } else if (fieldcom instanceof JTextField) {
            JTextField textfield = (JTextField) fieldcom;
            if (fieldtype.equals("int") || fieldtype.equals("double")) {
                textfield.setText("" + fieldval);
            } else if (fieldtype.equals("String")) {
                textfield.setText("" + fieldval);
            }
        }
    }

    /** A Gui component for editing a keystroke. */
    class KeyField extends JTextField implements KeyListener, FocusListener {

        String fieldname;

        Color norm_bg, hlt_bg;

        public KeyField(String fieldname, Color norm_bg, Color hlt_bg) {
            super(10);
            this.fieldname = fieldname;
            setEditable(true);
            addKeyListener(this);
            addFocusListener(this);
            this.norm_bg = norm_bg;
            this.hlt_bg = hlt_bg;
            setBackground(norm_bg);
            setBorder(border);
        }

        public void setValue(int keyval) {
            setText(JGEngine.getKeyDescStatic(keyval));
        }

        public int getValue() {
            return JGEngine.getKeyCodeStatic(getText());
        }

        public void processKeyEvent(KeyEvent e) {
            int keycode = e.getKeyCode();
            if (keycode != 0) {
                setText(JGEngine.getKeyDescStatic(keycode));
                processAction(fieldname, new Integer(keycode));
            }
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }

        public void focusGained(FocusEvent e) {
            setBackground(hlt_bg);
        }

        public void focusLost(FocusEvent e) {
            setBackground(norm_bg);
        }
    }
}
