package com.sun.lwuit.automation;

import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.events.SelectionListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.impl.ImplementationFactory;
import com.sun.lwuit.impl.LWUITImplementation;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.GridLayout;
import com.sun.lwuit.list.DefaultListCellRenderer;
import com.sun.lwuit.list.ListCellRenderer;
import com.sun.lwuit.list.DefaultListModel;
import com.sun.lwuit.list.ListModel;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Allows us to instrument LWUIT with tools to debug and test LWUIT applications. 
 * This class provides tools for automatically testing LWUIT applications 
 * by allowing control and inspection of running applications. 
 * <p>It is suggested that standard applications do not reference this class outside
 * of unit testing to avoid overhead.
 * <p>To use this class you must install the DebugImplementation class into the display
 * by using the init methods here rather than invoking display.init
 * 
 * @author Shai Almog
 */
public class DebugController {

    private ScriptStore store;

    /**
     * Indicates that LWUIT calls that are not on the EDT should be ignored
     */
    public static final int EDT_VIOLATION_IGNORE = 0;

    /**
     * Indicates that LWUIT calls that are not on the EDT should produce a log warning
     */
    public static final int EDT_VIOLATION_WARN = 1;

    /**
     * Indicates that LWUIT calls that are not on the EDT should throw an exception
     */
    public static final int EDT_VIOLATION_RUNTIME_EXCEPTION = 2;

    /**
     * Shortcut for activating the recording feature
     */
    public static final int SHORTCUT_START_RECORDING = 0;

    /**
     * Shortcut for finishing the recording feature
     */
    public static final int SHORTCUT_FINISH_RECORDING = 1;

    /**
     * Shortcut for opening the script manager
     */
    public static final int SHORTCUT_SCRIPT_MANAGER = 2;

    /**
     * Shortcut for opening the assertion manager
     */
    public static final int SHORTCUT_ASSERTION_MANAGER = 3;

    private static DebugController INSTANCE = new DebugController();

    static final byte ASSERT_TITLE = 50;

    static final byte ASSERT_FOCUS_LABEL_TEXT = 51;

    static final byte ASSERT_FOCUS_CLASS_TYPE = 52;

    static final byte ASSERT_NO_EXCEPTIONS = 53;

    static final byte ASSERT_FOCUS_COLORS = 54;

    /**
     * Returns the singleton instance of the debug controller
     * 
     * @return the singleton instance of this class
     */
    public static DebugController getInstance() {
        return INSTANCE;
    }

    private static void init() {
        final ImplementationFactory old = ImplementationFactory.getInstance();
        ImplementationFactory.setInstance(new ImplementationFactory() {

            public LWUITImplementation createImplementation() {
                return new DebugImplementation(old.createImplementation());
            }
        });
    }

    /**
     * Initializes the debug controller, this method must be invoked INSTEAD of 
     * Display.init()! 
     * 
     * @param midlet pointer to the MIDlet or similar object allowing binding to the display.
     */
    public static void init(Object midlet) {
        init();
        Display.init(midlet);
    }

    /**
     * Initializes the debug controller, this method must be invoked INSTEAD of 
     * Display.init()! 
     * <p>This method is identical to init however it also binds shortcuts to
     * the keypad to allow easier testing all shortcuts are bound to long presses. 
     * '1' is bound to start recording.
     * '2' is bound to the assertion manager
     * '3' is bound to stop recording.
     * '4' is bound to the script manager
     * 
     * @param midlet pointer to the MIDlet or similar object allowing binding to the display.
     * @param store a storage location
     */
    public static void init(Object midlet, ScriptStore store) {
        init();
        Display.init(midlet);
        INSTANCE.setScriptStore(store);
        INSTANCE.setShortcut('1', true, SHORTCUT_START_RECORDING);
        INSTANCE.setShortcut('2', true, SHORTCUT_ASSERTION_MANAGER);
        INSTANCE.setShortcut('3', true, SHORTCUT_FINISH_RECORDING);
        INSTANCE.setShortcut('4', true, SHORTCUT_SCRIPT_MANAGER);
    }

    /**
     * Allows a developer to manually deliver an event to LWUIT, this method is
     * thread safe.
     * 
     * @param x point for pointer event
     * @param y point for pointer event
     */
    public void pressPointer(int x, int y) {
        Display.getInstance().pointerPressed(new int[] { x }, new int[] { y });
    }

    /**
     * Allows a developer to manually deliver an event to LWUIT, this method is
     * thread safe.
     * 
     * @param x point for pointer event
     * @param y point for pointer event
     */
    public void releasePointer(int x, int y) {
        Display.getInstance().pointerReleased(new int[] { x }, new int[] { y });
    }

    /**
     * Allows a developer to manually deliver an event to LWUIT, this method is
     * thread safe.
     * 
     * @param x point for pointer event
     * @param y point for pointer event
     */
    public void dragPointer(int x, int y) {
        Display.getInstance().pointerDragged(new int[] { x }, new int[] { y });
    }

    /**
     * Allows a developer to manually deliver an event to LWUIT, this method is
     * thread safe.
     * 
     * @param keyCode the code of the key event
     */
    public void pressKey(int keyCode) {
        Display.getInstance().keyPressed(keyCode);
    }

    /**
     * Allows a developer to manually deliver an event to LWUIT, this method is
     * thread safe.
     * 
     * @param keyCode the code of the key event
     */
    public void releaseKey(int keyCode) {
        Display.getInstance().keyReleased(keyCode);
    }

    /**
     * Sets the ratio for playback of scripts, this defaults to 1 but can be slowed
     * down or sped up. Use 0.5 for double speed and use 2 for slowing down.
     * 
     * @param ratio the speed of playback
     */
    public void setPlaybackSpeed(float ratio) {
        DebugImplementation.instance.setPlaybackSpeed(ratio);
    }

    /**
     * Allows a user to record a test script which consists of the events sent 
     * internally in LWUIT. This script can then be executed using the runScript
     * method.
     */
    public void startRecording() {
        DebugImplementation.instance.startRecording();
    }

    /**
     * Returns the "script" of events that occurred in LWUIT since recording 
     * started allowing us to preserve this script for playback in testing.
     * 
     * @return the recorded script 
     */
    public Script finishRecording() {
        Script s = DebugImplementation.instance.finishRecording();
        TextField title = new TextField(30);
        title.setReplaceMenu(false);
        Command ok = new Command("OK");
        Command cancel = new Command("Cancel");
        String old = Dialog.getDefaultDialogPosition();
        Dialog.setDefaultDialogPosition(BorderLayout.CENTER);
        Command response = Dialog.show("Script Title", title, new Command[] { ok, cancel });
        Dialog.setDefaultDialogPosition(old);
        if (response == ok) {
            s.setTitle(title.getText());
            return s;
        }
        return null;
    }

    /**
     * Plays a LWUIT script recorded using the start/finishRecording methods, this method
     * will block the calling thread unless it is called on the EDT
     * 
     * @param script the script to playback
     */
    public void runScript(final Script script) {
        if (Display.getInstance().isEdt()) {
            new Thread() {

                public void run() {
                    DebugImplementation.instance.playScript(script);
                }
            }.start();
            return;
        }
        DebugImplementation.instance.playScript(script);
    }

    /**
     * Fetches any exception thrown on the EDT, returns null if no exception was
     * thrown on the EDT.
     * 
     * @return the exception thrown
     */
    public Throwable fetchEDTException() {
        return DebugImplementation.instance.fetchEDTException();
    }

    /**
     * Indicates the behavior of LWUIT when an EDT violation is detected (not all
     * edt violations can be detected!) this can be one of: EDT_VIOLATION_IGNORE,
     * EDT_VIOLATION_RUNTIME_EXCEPTION, EDT_VIOLATION_WARN
     * 
     * @return the mode of the EDT
     */
    public int getEdtMode() {
        return DebugImplementation.instance.getEdtMode();
    }

    /**
     * Indicates the behavior of LWUIT when an EDT violation is detected (not all
     * edt violations can be detected!) this can be one of: EDT_VIOLATION_IGNORE,
     * EDT_VIOLATION_RUNTIME_EXCEPTION, EDT_VIOLATION_WARN
     * 
     * @param edtMode the mode of the EDT
     */
    public void setEdtMode(int edtMode) {
        DebugImplementation.instance.setEdtMode(edtMode);
    }

    /**
     * Returns a LWUIT image containing a screenshot of the current UI, this method is not 100%
     * accurate and might cause a refresh and ignore some artifacts such as the ones 
     * created during transition.
     * 
     * @return a screenshot image object
     */
    public Image takeScreenshot() {
        Form f = Display.getInstance().getCurrent();
        Image i = Image.createImage(f.getWidth(), f.getHeight());
        f.paintComponent(i.getGraphics());
        return i;
    }

    /**
     * Install a script store into the controller to allow it to save the scripts
     * and load them for later usage
     * 
     * @param store storage into which scripts should be saved
     */
    public void setScriptStore(ScriptStore store) {
        this.store = store;
    }

    /**
     * Opens the script management dialog, this method assumes a script store has been
     * installed using the setScriptStore method.
     */
    private void showScriptManager() {
        try {
            final List scriptList = new List(store.getStoredScriptNameList());
            scriptList.setFixedSelection(List.FIXED_NONE_CYCLIC);
            Command close = new Command("Close") {

                public void actionPerformed(ActionEvent ev) {
                    ((Dialog) Display.getInstance().getCurrent()).dispose();
                }
            };
            Command editor = new Command("Edit") {

                public void actionPerformed(ActionEvent ev) {
                    try {
                        String s = (String) scriptList.getSelectedItem();
                        Script script = store.loadScript(s);
                        editScript(script);
                    } catch (IOException ioErr) {
                        handleScriptIOException(ioErr);
                    }
                }
            };
            Command delete = new Command("Delete") {

                public void actionPerformed(ActionEvent ev) {
                    try {
                        String s = (String) scriptList.getSelectedItem();
                        if (s != null && Dialog.show("Delete Script", "Are you sure you want to delete " + s + "?", "OK", "Cancel")) {
                            store.deleteScript(s);
                        }
                    } catch (IOException ioErr) {
                        handleScriptIOException(ioErr);
                    }
                }
            };
            Command rename = new Command("Rename") {

                public void actionPerformed(ActionEvent ev) {
                    try {
                        String s = (String) scriptList.getSelectedItem();
                        if (s != null) {
                            TextField name = new TextField(s, 30);
                            name.setReplaceMenu(false);
                            Command ok = new Command("OK");
                            Command cancel = new Command("Cancel");
                            String old = Dialog.getDefaultDialogPosition();
                            Dialog.setDefaultDialogPosition(BorderLayout.CENTER);
                            if (Dialog.show("Rename", name, new Command[] { ok, cancel }) == ok) {
                                Script script = store.loadScript(s);
                                store.deleteScript(s);
                                script.setTitle(name.getText());
                                store.storeScript(script);
                            }
                            Dialog.setDefaultDialogPosition(old);
                        }
                    } catch (IOException ioErr) {
                        handleScriptIOException(ioErr);
                    }
                }
            };
            Command play = new Command("Play") {

                public void actionPerformed(ActionEvent ev) {
                    ((Dialog) Display.getInstance().getCurrent()).dispose();
                    new Thread() {

                        public void run() {
                            try {
                                String s = (String) scriptList.getSelectedItem();
                                if (s != null) {
                                    Script script = store.loadScript(s);
                                    runScript(script);
                                }
                                showScriptManager();
                            } catch (IOException ioErr) {
                                handleScriptIOException(ioErr);
                            }
                        }
                    }.start();
                }
            };
            Dialog prompt = new Dialog("Scripts");
            prompt.setScrollable(false);
            prompt.addCommand(close);
            prompt.addCommand(play);
            prompt.addCommand(rename);
            prompt.addCommand(delete);
            prompt.addCommand(editor);
            prompt.setLayout(new BorderLayout());
            prompt.addComponent(BorderLayout.CENTER, scriptList);
            prompt.setAutoDispose(false);
            prompt.show();
        } catch (IOException ioErr) {
            handleScriptIOException(ioErr);
        }
    }

    /**
     * Shows the script editor dialog allowing a developer/tester to modify the
     * scripts flow and behavior.
     * 
     * @param s script for editing in the script editor.
     */
    public void editScript(final Script s) {
        final boolean[] modified = new boolean[1];
        final Form previous = Display.getInstance().getCurrent();
        Form scriptEditor = new Form("Editor");
        scriptEditor.setLayout(new BorderLayout());
        final List scriptContent = new List(s.data);
        scriptContent.setListCellRenderer(new DefaultListCellRenderer() {

            private Container selection;

            private Label[] labels = new Label[] { new Label(), new Label(), new Label(), new Label(), new Label() };

            {
                selection = new Container();
                for (int iter = 0; iter < labels.length; iter++) {
                    selection.addComponent(labels[iter]);
                    labels[iter].getStyle().setBgTransparency(0);
                }
            }

            public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                Script.Instruction instruction = (Script.Instruction) value;
                if (isSelected) {
                    labels[0].setText(Script.Instruction.getType(instruction.type));
                    for (int iter = 0; iter < instruction.data.length; iter++) {
                        labels[1 + iter].setText(instruction.getEntryAsText(s, iter));
                    }
                    return selection;
                }
                String text = Script.Instruction.getType(instruction.type);
                for (int iter = 0; iter < instruction.data.length; iter++) {
                    text += ", " + instruction.data[iter];
                }
                return super.getListCellRendererComponent(list, text, index, isSelected);
            }
        });
        scriptEditor.addComponent(BorderLayout.CENTER, scriptContent);
        scriptEditor.addCommand(new Command("Back") {

            public void actionPerformed(ActionEvent ev) {
                if (modified[0]) {
                    if (!Dialog.show("Script Modified", "You have some unsaved changes in your script do you want to discard those changes?", "Discard", "Cancel")) {
                        return;
                    }
                }
                previous.show();
            }
        });
        scriptEditor.addCommand(new Command("Save") {

            public void actionPerformed(ActionEvent ev) {
                try {
                    store.storeScript(s);
                    modified[0] = false;
                } catch (IOException ex) {
                    handleScriptIOException(ex);
                }
            }
        });
        scriptEditor.addCommand(new Command("Save As...") {

            public void actionPerformed(ActionEvent ev) {
                try {
                    TextField title = new TextField(30);
                    title.setReplaceMenu(false);
                    Command ok = new Command("OK");
                    Command cancel = new Command("Cancel");
                    Command response = Dialog.show("Save As", title, new Command[] { ok, cancel });
                    if (response == ok) {
                        if (overwriteTest(title.getText())) {
                            s.setTitle(title.getText());
                            store.storeScript(s);
                            modified[0] = false;
                        }
                    }
                } catch (IOException ex) {
                    handleScriptIOException(ex);
                }
            }
        });
        scriptEditor.addCommand(new Command("Edit") {

            public void actionPerformed(ActionEvent ev) {
                Script.Instruction i = editInstruction((Script.Instruction) scriptContent.getSelectedItem());
                if (i != null) {
                    int offset = scriptContent.getSelectedIndex();
                    ((DefaultListModel) scriptContent.getModel()).setItem(offset, i);
                    s.data.setElementAt(i, offset);
                    modified[0] = true;
                }
            }
        });
        scriptEditor.addCommand(new Command("Add") {

            public void actionPerformed(ActionEvent ev) {
                Script.Instruction i = editInstruction(null);
                if (i != null) {
                    int offset = scriptContent.getSelectedIndex();
                    s.data.insertElementAt(i, offset);
                    scriptContent.setModel(new DefaultListModel(s.data));
                    modified[0] = true;
                }
            }
        });
        scriptEditor.addCommand(new Command("Move To...") {

            public void actionPerformed(ActionEvent ev) {
                TextField position = new TextField(30);
                position.setInputMode("123");
                position.setReplaceMenu(false);
                Command ok = new Command("OK");
                Command cancel = new Command("Cancel");
                int oldOffset = scriptContent.getSelectedIndex();
                position.setText("" + oldOffset);
                Command response = Dialog.show("Move To...", position, new Command[] { ok, cancel });
                if (response == ok) {
                    int offset = Integer.parseInt(position.getText());
                    if (offset > -1 && offset <= scriptContent.getModel().getSize()) {
                        Object o = s.data.elementAt(oldOffset);
                        s.data.removeElementAt(oldOffset);
                        s.data.insertElementAt(o, offset);
                        scriptContent.setModel(new DefaultListModel(s.data));
                    }
                    modified[0] = true;
                }
            }
        });
        scriptEditor.addCommand(new Command("Delete") {

            public void actionPerformed(ActionEvent ev) {
                int i = scriptContent.getSelectedIndex();
                if (i >= 0 && Dialog.show("Delete", "Are you sure you want to delete this instruction?", "Yes", "No")) {
                    scriptContent.getModel().removeItem(i);
                    s.data.removeElementAt(i);
                    modified[0] = true;
                }
            }
        });
        scriptEditor.show();
    }

    /**
     * Allows the script editor to edit a single instruction within the script
     */
    private Script.Instruction editInstruction(Script.Instruction i) {
        String title = "Edit";
        if (i == null) {
            title = "Add";
        }
        Dialog editor = new Dialog(title);
        editor.setLayout(new GridLayout(5, 2));
        ComboBox type = new ComboBox(Script.Instruction.TYPES_NAMES);
        type.addSelectionListener(new SelectionListener() {

            public void selectionChanged(int oldSelected, int newSelected) {
                int size = Script.Instruction.size(Script.Instruction.TYPES_VALUE[newSelected]);
            }
        });
        editor.addComponent(new Label("Type"));
        editor.addComponent(type);
        Command ok = new Command("OK");
        Command cancel = new Command("Cancel");
        editor.addCommand(ok);
        editor.addCommand(cancel);
        if (editor.showDialog() == ok) {
        }
        return null;
    }

    /**
     * Displays an error message when a script io exception is thrown
     */
    private static void handleScriptIOException(IOException ioErr) {
        ioErr.printStackTrace();
        Dialog.show("Script", "A Script Storage Error Error Occurred: " + ioErr, "OK", null);
    }

    /**
     * Opens the assertion manager allowing a tester to define assertions regarding the current
     * application state. These asseritions are placed into the script.
     */
    private void showAssertionManager(Script s) {
        DebugImplementation.instance.pauseScript();
        final Form currentForm = Display.getInstance().getCurrent();
        class Assertion {

            Assertion(int type, String text) {
                this.type = type;
                this.text = text;
            }

            public int type;

            public String text;

            public boolean selected;
        }
        class Renderer extends Container implements ListCellRenderer {

            private CheckBox check = new CheckBox();

            private TextArea content = new TextArea(3, 30);

            private Label focus = new Label();

            public Renderer() {
                setLayout(new BorderLayout());
                Container checkArea = new Container();
                checkArea.addComponent(check);
                check.setPreferredSize(new Dimension(15, 15));
                check.getStyle().setPadding(4, 0, 0, 0);
                check.getStyle().setMargin(0, 0, 0, 0);
                check.getStyle().setBgTransparency(0);
                addComponent(BorderLayout.WEST, checkArea);
                addComponent(BorderLayout.CENTER, content);
                content.setIsScrollVisible(false);
                content.getStyle().setBorder(null);
                content.getStyle().setBgTransparency(0);
            }

            public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                Assertion a = (Assertion) value;
                check.setSelected(a.selected);
                check.setFocus(isSelected);
                content.setFocus(isSelected);
                content.setText(a.text);
                return this;
            }

            public Component getListFocusComponent(List list) {
                return focus;
            }
        }
        Vector assertionsVector = new Vector();
        assertionsVector.addElement(new Assertion(ASSERT_NO_EXCEPTIONS, "No Exception Occurred"));
        assertionsVector.addElement(new Assertion(ASSERT_TITLE, "Title Is: " + currentForm.getTitle()));
        if (currentForm.getFocused() != null) {
            assertionsVector.addElement(new Assertion(ASSERT_FOCUS_CLASS_TYPE, "Focus is: " + currentForm.getFocused().getClass().getName()));
            assertionsVector.addElement(new Assertion(ASSERT_FOCUS_COLORS, "Focus Colors Identical"));
            if (currentForm.getFocused() instanceof Label) {
                assertionsVector.addElement(new Assertion(ASSERT_FOCUS_LABEL_TEXT, "Focus Text: " + ((Label) currentForm.getFocused()).getText()));
            }
        }
        final List assertions = new List(assertionsVector);
        assertions.setFixedSelection(List.FIXED_NONE_CYCLIC);
        assertions.setListCellRenderer(new Renderer());
        assertions.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Assertion a = (Assertion) assertions.getSelectedItem();
                a.selected = (!a.selected);
            }
        });
        Dialog dialog = new Dialog("Assertions");
        dialog.setScrollable(false);
        dialog.addCommand(new Command("OK"));
        dialog.setLayout(new BorderLayout());
        dialog.addComponent(BorderLayout.CENTER, assertions);
        dialog.showPacked(BorderLayout.CENTER, true);
        for (int iter = 0; iter < assertionsVector.size(); iter++) {
            Assertion a = (Assertion) assertionsVector.elementAt(iter);
            if (a.selected) {
                switch(a.type) {
                    case ASSERT_NO_EXCEPTIONS:
                        s.assertNoExceptions();
                        break;
                    case ASSERT_TITLE:
                        s.assertTitle();
                        break;
                    case ASSERT_FOCUS_CLASS_TYPE:
                        s.assertFocusClassType();
                        break;
                    case ASSERT_FOCUS_COLORS:
                        s.assertFocusColors();
                        break;
                    case ASSERT_FOCUS_LABEL_TEXT:
                        s.assertFocusLabelText();
                        break;
                }
            }
        }
        DebugImplementation.instance.resumeScript();
    }

    /**
     * A shortcut is a keybinding which will be triggered when something happens
     * 
     * @param keyCode keycode to bind to the shortcut
     * @param longPress whether to bind this keycodes long press or normal press to this feature
     * @param feature element that should be activated on this keyCode: SHORTCUT_FINISH_RECORDING, 
     * SHORTCUT_START_RECORDING, SHORTCUT_SCRIPT_MANAGER, SHORTCUT_ASSERTION_MANAGER
     */
    public void setShortcut(int keyCode, boolean longPress, final int feature) {
        if (store == null) {
            Dialog.show("Shortcut", "No script store is defined, shortcuts wouldn't be very useful", "OK", null);
            return;
        }
        DebugImplementation.instance.bindShortcut(keyCode, longPress, new Runnable() {

            public void run() {
                try {
                    switch(feature) {
                        case SHORTCUT_FINISH_RECORDING:
                            Script result = finishRecording();
                            if (result != null) {
                                if (overwriteTest(result)) {
                                    store.storeScript(result);
                                }
                            }
                            break;
                        case SHORTCUT_START_RECORDING:
                            startRecording();
                            break;
                        case SHORTCUT_SCRIPT_MANAGER:
                            showScriptManager();
                            break;
                        case SHORTCUT_ASSERTION_MANAGER:
                            Script s = DebugImplementation.instance.getCurrentRecording();
                            if (s != null) {
                                showAssertionManager(s);
                            }
                            break;
                    }
                } catch (IOException ioErr) {
                    handleScriptIOException(ioErr);
                }
            }
        });
    }

    /**
     * Allows storing a script in an arbitrary stream
     * 
     * @param o destination for the script
     * @param s script to store
     * @throws IOException when the underlying stream throws an exception
     */
    public static void storeScript(OutputStream o, Script s) throws IOException {
        s.store(o);
    }

    /**
     * Allows loading a script from an arbitrary stream
     * 
     * @param o source to loadthe script
     * @throws IOException when the underlying stream throws an exception
     * @return the loaded script
     */
    public static Script loadScript(InputStream o) throws IOException {
        Script s = new Script();
        s.load(o);
        return s;
    }

    /**
     * Allows storing a script in a byte array which is useful for RMS storage
     * 
     * @param s script to convert
     * @return byte array representing the serialized script
     */
    public static byte[] scriptToBytes(Script s) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            storeScript(bo, s);
            bo.close();
            return bo.toByteArray();
        } catch (IOException ex) {
            handleScriptIOException(ex);
            return null;
        }
    }

    /**
     * Allows loading a script from an arbitrary stream
     * 
     * @param bytes byte array representing the serialized script
     * @return converted script
     */
    public static Script scriptFromBytes(byte[] bytes) {
        try {
            Script s = new Script();
            s.load(new ByteArrayInputStream(bytes));
            return s;
        } catch (IOException ex) {
            handleScriptIOException(ex);
            return null;
        }
    }

    /**
     * Returns true if a script with the given name is in the current store
     */
    private boolean containsScript(String name) throws IOException {
        if (store != null) {
            ListModel names = store.getStoredScriptNameList();
            int nameCount = names.getSize();
            for (int iter = 0; iter < nameCount; iter++) {
                if (names.getItemAt(iter).equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the given script is in the store and if it is prompts the user to
     * ask him whether the script can be overwritten returns true for every case unless 
     * the user selects no.
     */
    private boolean overwriteTest(Script s) throws IOException {
        while (!overwriteTest(s.getTitle())) {
            TextField name = new TextField(s.getTitle(), 30);
            name.setReplaceMenu(false);
            Command ok = new Command("OK");
            Command cancel = new Command("Cancel");
            String old = Dialog.getDefaultDialogPosition();
            Dialog.setDefaultDialogPosition(BorderLayout.CENTER);
            if (Dialog.show("Save As...", name, new Command[] { ok, cancel }) != ok) {
                Dialog.setDefaultDialogPosition(old);
                return false;
            }
            Dialog.setDefaultDialogPosition(old);
            s.setTitle(name.getText());
        }
        return true;
    }

    /**
     * Tests if the given script is in the store and if it is prompts the user to
     * ask him whether the script can be overwritten returns true for every case unless 
     * the user selects no.
     */
    private boolean overwriteTest(String name) throws IOException {
        if (containsScript(name)) {
            if (Dialog.show("Script Exists", "The script " + name + " already exists, do you want to overwrite it?", "Cancel", "Overwrite")) {
                return false;
            }
        }
        return true;
    }

    /**
     * A script is an instance of a recording that can be stored/loaded and manipulated
     * by a test agent.
     */
    public static class Script {

        private Vector data = new Vector();

        private String title;

        private Vector constantPool = new Vector();

        private Hashtable gamekeyMap = new Hashtable();

        /**
         * This information is useful for figuring out if a script would work on
         * a device with a different resolution
         */
        private int displayWidth;

        private int displayHeight;

        Script() {
        }

        /**
         * Returns the title of the script
         * 
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        void setTitle(String title) {
            this.title = title;
        }

        /**
         * Stores the script to the given output stream
         */
        void store(OutputStream s) throws IOException {
            DataOutputStream d = new DataOutputStream(s);
            d.writeUTF(title);
            d.writeInt(displayWidth);
            d.writeInt(displayHeight);
            d.writeInt(data.size());
            for (int iter = 0; iter < data.size(); iter++) {
                ((Instruction) data.elementAt(iter)).write(d);
            }
            d.writeInt(constantPool.size());
            for (int iter = 0; iter < constantPool.size(); iter++) {
                d.writeUTF((String) constantPool.elementAt(iter));
            }
            d.writeInt(gamekeyMap.size());
            Enumeration e = gamekeyMap.keys();
            for (int iter = 0; iter < gamekeyMap.size(); iter++) {
                Integer key = (Integer) e.nextElement();
                d.writeInt(key.intValue());
                d.writeInt(((Integer) gamekeyMap.get(key)).intValue());
            }
        }

        Hashtable startScript() {
            displayWidth = Display.getInstance().getDisplayWidth();
            displayHeight = Display.getInstance().getDisplayHeight();
            return gamekeyMap;
        }

        /**
         * Execute a script while blocking the current thread
         */
        void execute(float speed) {
            long startTime = System.currentTimeMillis();
            for (int iter = 0; iter < data.size(); iter++) {
                ((Instruction) data.elementAt(iter)).execute(this, startTime, speed);
            }
        }

        /**
         * Loads the script from the given input stream
         */
        void load(InputStream s) throws IOException {
            data.removeAllElements();
            constantPool.removeAllElements();
            gamekeyMap.clear();
            DataInputStream d = new DataInputStream(s);
            title = d.readUTF();
            displayWidth = d.readInt();
            displayHeight = d.readInt();
            int size = d.readInt();
            for (int iter = 0; iter < size; iter++) {
                data.addElement(Instruction.read(d));
            }
            size = d.readInt();
            for (int iter = 0; iter < size; iter++) {
                constantPool.addElement(d.readUTF());
            }
            size = d.readInt();
            for (int iter = 0; iter < size; iter++) {
                gamekeyMap.put(new Integer(d.readInt()), new Integer(d.readInt()));
            }
        }

        void pushKeyEvent(int time, int type, int key) {
            data.addElement(Instruction.createTimeStampActualTime(time));
            if (type == DebugImplementation.KEY_PRESSED) {
                data.addElement(Instruction.createKeyPressedEvent(key));
            } else {
                data.addElement(Instruction.createKeyReleaseEvent(key));
            }
        }

        void pushPointerEvent(int time, int type, int x, int y) {
            data.addElement(Instruction.createTimeStampActualTime(time));
            switch(type) {
                case DebugImplementation.POINTER_PRESSED:
                    data.addElement(Instruction.createPointerPressEvent(x, y));
                    break;
                case DebugImplementation.POINTER_RELEASED:
                    data.addElement(Instruction.createPointerReleasedEvent(x, y));
                    break;
                default:
                    data.addElement(Instruction.createPointerDraggedEvent(x, y));
                    break;
            }
        }

        void popKey() {
            data.removeElementAt(data.size() - 1);
        }

        int storeInPool(String value) {
            int index = constantPool.indexOf(value);
            if (index < 0) {
                constantPool.addElement(value);
                index = constantPool.indexOf(value);
            }
            return index;
        }

        void assertTitle() {
            data.addElement(Instruction.createAssertTitle(this));
        }

        void assertFocusLabelText() {
            data.addElement(Instruction.createAssertLabel(this));
        }

        void assertFocusClassType() {
            data.addElement(Instruction.createAssertFocusClassType(this));
        }

        void assertNoExceptions() {
            data.addElement(Instruction.createAssertNoException());
        }

        void assertFocusColors() {
            data.addElement(Instruction.createAssertFocusColors(this));
        }

        static class Instruction {

            private static final byte TIME_STAMP = -1;

            private static final byte POINTER_PRESSED = 1;

            private static final byte POINTER_RELEASED = 2;

            private static final byte POINTER_DRAGGED = 3;

            private static final byte KEY_PRESSED = 4;

            private static final byte KEY_RELEASED = 5;

            private static final byte GAME_KEY_PRESS = 6;

            private static final byte GAME_KEY_RELEASE = 7;

            private static final byte SOFT_LEFT_PRESS = 8;

            private static final byte SOFT_RIGHT_PRESS = 9;

            private static final byte SOFT_LEFT_RELEASE = 10;

            private static final byte SOFT_RIGHT_RELEASE = 11;

            static final int[] TYPES_VALUE = new int[] { TIME_STAMP, POINTER_PRESSED, POINTER_RELEASED, POINTER_DRAGGED, KEY_PRESSED, KEY_RELEASED, GAME_KEY_PRESS, GAME_KEY_RELEASE, SOFT_LEFT_PRESS, SOFT_RIGHT_PRESS, SOFT_LEFT_RELEASE, SOFT_RIGHT_RELEASE, ASSERT_TITLE, ASSERT_FOCUS_LABEL_TEXT, ASSERT_FOCUS_CLASS_TYPE, ASSERT_NO_EXCEPTIONS, ASSERT_FOCUS_COLORS };

            static final String[] TYPES_NAMES = new String[] { "TIME_STAMP", "POINTER_PRESSED", "POINTER_RELEASED", "POINTER_DRAGGED", "KEY_PRESSED", "KEY_RELEASED", "GAME_KEY_PRESS", "GAME_KEY_RELEASE", "SOFT_LEFT_PRESS", "SOFT_RIGHT_PRESS", "SOFT_LEFT_RELEASE", "SOFT_RIGHT_RELEASE", "ASSERT_TITLE", "ASSERT_FOCUS_LABEL_TEXT", "ASSERT_FOCUS_CLASS_TYPE", "ASSERT_NO_EXCEPTIONS", "ASSERT_FOCUS_COLORS" };

            static String getType(int type) {
                for (int iter = 0; iter < TYPES_VALUE.length; iter++) {
                    if (TYPES_VALUE[iter] == type) {
                        return TYPES_NAMES[iter];
                    }
                }
                throw new IllegalArgumentException("Unknown type: " + type);
            }

            private byte type;

            private int[] data;

            private Instruction(byte type, int[] data) {
                this.type = type;
                this.data = data;
            }

            private void assertString(Script parentScript, String s) {
                String constant = (String) parentScript.constantPool.elementAt(data[0]);
                Log.p("script: assert " + s + " == " + constant, Log.DEBUG);
                if (constant == null && s == null) {
                    return;
                }
                if (!constant.equals(s)) {
                    throw new RuntimeException("Assertion failed: " + constant + " != " + s);
                }
            }

            public static Instruction createTimeStamp(long startTime) {
                return new Instruction(TIME_STAMP, new int[] { (int) (System.currentTimeMillis() - startTime) });
            }

            public static Instruction createTimeStampActualTime(long time) {
                return new Instruction(TIME_STAMP, new int[] { (int) (time) });
            }

            public static Instruction createPointerPressEvent(int x, int y) {
                return new Instruction(POINTER_PRESSED, new int[] { x, y });
            }

            public static Instruction createPointerReleasedEvent(int x, int y) {
                return new Instruction(POINTER_RELEASED, new int[] { x, y });
            }

            public static Instruction createPointerDraggedEvent(int x, int y) {
                return new Instruction(POINTER_DRAGGED, new int[] { x, y });
            }

            public static Instruction createKeyPressedEvent(int code) {
                if (code == DebugImplementation.instance.getSoftkeyCode(0)[0]) {
                    return new Instruction(SOFT_LEFT_PRESS, new int[0]);
                }
                if (code == DebugImplementation.instance.getSoftkeyCode(1)[0]) {
                    return new Instruction(SOFT_RIGHT_PRESS, new int[0]);
                }
                int game = Display.getInstance().getGameAction(code);
                if (game != 0) {
                    return new Instruction(GAME_KEY_PRESS, new int[] { game });
                }
                return new Instruction(KEY_PRESSED, new int[] { code });
            }

            public static Instruction createKeyReleaseEvent(int code) {
                if (code == DebugImplementation.instance.getSoftkeyCode(0)[0]) {
                    return new Instruction(SOFT_LEFT_RELEASE, new int[0]);
                }
                if (code == DebugImplementation.instance.getSoftkeyCode(1)[0]) {
                    return new Instruction(SOFT_RIGHT_RELEASE, new int[0]);
                }
                int game = Display.getInstance().getGameAction(code);
                if (game != 0) {
                    return new Instruction(GAME_KEY_RELEASE, new int[] { game });
                }
                return new Instruction(KEY_RELEASED, new int[] { code });
            }

            String getEntryAsText(Script parent, int offset) {
                if (offset == 0) {
                    if (type == ASSERT_TITLE || type == ASSERT_FOCUS_LABEL_TEXT || type == ASSERT_FOCUS_CLASS_TYPE) {
                        return (String) parent.constantPool.elementAt(data[offset]);
                    }
                }
                return "" + data[offset];
            }

            void setEntryText(Script parent, int offset, String text) {
                if (offset == 0) {
                    if (type == ASSERT_TITLE || type == ASSERT_FOCUS_LABEL_TEXT || type == ASSERT_FOCUS_CLASS_TYPE) {
                        parent.constantPool.setElementAt(text, offset);
                    }
                }
                data[offset] = Integer.parseInt(text);
            }

            public static Instruction createAssertTitle(Script s) {
                return new Instruction(ASSERT_TITLE, new int[] { (s.storeInPool(Display.getInstance().getCurrent().getTitle())) });
            }

            public static Instruction createAssertLabel(Script s) {
                String t = ((Label) Display.getInstance().getCurrent().getFocused()).getText();
                return new Instruction(ASSERT_FOCUS_LABEL_TEXT, new int[] { (s.storeInPool(t)) });
            }

            public static Instruction createAssertFocusClassType(Script s) {
                String t = Display.getInstance().getCurrent().getFocused().getClass().getName();
                return new Instruction(ASSERT_FOCUS_CLASS_TYPE, new int[] { (s.storeInPool(t)) });
            }

            public static Instruction createAssertNoException() {
                return new Instruction(ASSERT_NO_EXCEPTIONS, new int[0]);
            }

            public static Instruction createAssertFocusColors(Script s) {
                Style style = Display.getInstance().getCurrent().getFocused().getStyle();
                Style selectionStyle = Display.getInstance().getCurrent().getFocused().getSelectedStyle();
                return new Instruction(ASSERT_FOCUS_COLORS, new int[] { style.getFgColor(), selectionStyle.getFgColor(), style.getBgColor(), selectionStyle.getBgColor() });
            }

            public void execute(Script parentScript, long startTime, float speedRatio) {
                DebugImplementation d = DebugImplementation.instance;
                switch(type) {
                    case TIME_STAMP:
                        int timeForNext = (int) (data[0] - (System.currentTimeMillis() - startTime));
                        timeForNext = (int) (timeForNext * speedRatio);
                        if (timeForNext > 0) {
                            try {
                                Thread.sleep(timeForNext);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case KEY_PRESSED:
                        {
                            Log.p("script: keyPressed(" + data[0] + ")", Log.DEBUG);
                            d.keyPressed(data[0]);
                            break;
                        }
                    case KEY_RELEASED:
                        {
                            Log.p("script: keyReleased(" + data[0] + ")", Log.DEBUG);
                            d.keyReleased(data[0]);
                            break;
                        }
                    case GAME_KEY_PRESS:
                        {
                            Log.p("script: game keyPressed(" + data[0] + ")", Log.DEBUG);
                            d.keyPressed(Display.getInstance().getKeyCode(data[0]));
                            break;
                        }
                    case GAME_KEY_RELEASE:
                        {
                            Log.p("script: game keyReleased(" + data[0] + ")", Log.DEBUG);
                            d.keyReleased(Display.getInstance().getKeyCode(data[0]));
                            break;
                        }
                    case SOFT_LEFT_PRESS:
                        {
                            Log.p("script: soft left keyPressed()", Log.DEBUG);
                            d.keyPressed(DebugImplementation.instance.getSoftkeyCode(0)[0]);
                            break;
                        }
                    case SOFT_LEFT_RELEASE:
                        {
                            Log.p("script: soft left keyReleased()", Log.DEBUG);
                            d.keyReleased(DebugImplementation.instance.getSoftkeyCode(1)[0]);
                            break;
                        }
                    case SOFT_RIGHT_PRESS:
                        {
                            Log.p("script: soft right keyPressed()", Log.DEBUG);
                            d.keyPressed(DebugImplementation.instance.getSoftkeyCode(0)[0]);
                            break;
                        }
                    case SOFT_RIGHT_RELEASE:
                        {
                            Log.p("script: soft right keyReleased()", Log.DEBUG);
                            d.keyReleased(DebugImplementation.instance.getSoftkeyCode(1)[0]);
                            break;
                        }
                    case POINTER_PRESSED:
                        {
                            Log.p("script: pointerPressed(" + data[0] + ", " + data[1] + ")", Log.DEBUG);
                            d.pointerPressed(data[0], data[1]);
                            break;
                        }
                    case POINTER_RELEASED:
                        {
                            Log.p("script: pointerReleased(" + data[0] + ", " + data[1] + ")", Log.DEBUG);
                            d.pointerReleased(data[0], data[1]);
                            break;
                        }
                    case POINTER_DRAGGED:
                        {
                            Log.p("script: pointerDragged(" + data[0] + ", " + data[1] + ")", Log.DEBUG);
                            d.pointerDragged(data[0], data[1]);
                            break;
                        }
                    case ASSERT_TITLE:
                        assertString(parentScript, Display.getInstance().getCurrent().getTitle());
                        break;
                    case ASSERT_FOCUS_CLASS_TYPE:
                        assertString(parentScript, Display.getInstance().getCurrent().getFocused().getClass().getName());
                        break;
                    case ASSERT_FOCUS_LABEL_TEXT:
                        assertString(parentScript, ((Label) Display.getInstance().getCurrent().getFocused()).getText());
                        break;
                    case ASSERT_NO_EXCEPTIONS:
                        Throwable t = d.fetchEDTException();
                        if (t != null) {
                            throw new RuntimeException("Assertion failed exception was thrown: " + t);
                        }
                        break;
                    case ASSERT_FOCUS_COLORS:
                        Log.p("script: assert colors", Log.DEBUG);
                        Style s = Display.getInstance().getCurrent().getFocused().getStyle();
                        Style selection = Display.getInstance().getCurrent().getFocused().getSelectedStyle();
                        if (data[0] != s.getFgColor() || data[1] != s.getBgColor() || data[2] != selection.getFgColor() || data[3] != selection.getBgColor()) {
                            throw new RuntimeException("Color Assertion failed");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid script unrecognized command: " + type);
                }
            }

            public void write(DataOutputStream o) throws IOException {
                o.writeByte(type);
                for (int iter = 0; iter < data.length; iter++) {
                    o.writeInt(data[iter]);
                }
            }

            static Instruction read(DataInputStream i) throws IOException {
                byte type = i.readByte();
                int[] data = new int[size(type)];
                for (int iter = 0; iter < data.length; iter++) {
                    data[iter] = i.readInt();
                }
                return new Instruction(type, data);
            }

            /**
             * Returns the size in integers besides the type
             */
            public int size() {
                return size(type);
            }

            private static int size(int type) {
                switch(type) {
                    case POINTER_DRAGGED:
                    case POINTER_PRESSED:
                    case POINTER_RELEASED:
                        return 2;
                    case SOFT_LEFT_PRESS:
                    case SOFT_LEFT_RELEASE:
                    case SOFT_RIGHT_PRESS:
                    case SOFT_RIGHT_RELEASE:
                    case ASSERT_NO_EXCEPTIONS:
                        return 0;
                    case ASSERT_FOCUS_COLORS:
                        return 4;
                    default:
                        return 1;
                }
            }
        }
    }

    /**
     * A test MIDlet should implement this interface to store and load and list
     * scripts for the automation UI. The store can be implemented over RMS, networking (remote server),
     * file system etc... It is abstracted to allow portability to multiple ME platforms.
     */
    public static interface ScriptStore {

        /**
         * Returns a list model of script names
         * 
         * @return the model of script names
         * @throws IOException if the underlying storage had a problem
         */
        public ListModel getStoredScriptNameList() throws IOException;

        /**
         * Stores a script
         * 
         * @param s script to store
         * @throws IOException if the underlying storage had a problem
         */
        public void storeScript(Script s) throws IOException;

        /**
         * Loads a script from storage
         * 
         * @param name the name of the script
         * @return the loaded script
         * @throws IOException if the underlying storage had a problem
         */
        public Script loadScript(String name) throws IOException;

        /**
         * deletes a script from the store, this is an optional operation.
         * 
         * @param name the name of the script
         * @throws IOException if the underlying storage had a problem
         */
        public void deleteScript(String name) throws IOException;
    }
}
