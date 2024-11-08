package org.gjt.sp.jedit;

import org.gjt.sp.jedit.buffer.*;
import org.gjt.sp.jedit.bufferio.BufferIORequest;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.IntegerArray;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Segment;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A <code>Buffer</code> represents the contents of an open text
 * file as it is maintained in the computer's memory (as opposed to
 * how it may be stored on a disk).<p>
 *
 * In a BeanShell script, you can obtain the current buffer instance from the
 * <code>buffer</code> variable.<p>
 *
 * This class does not have a public constructor.
 * Buffers can be opened and closed using methods in the <code>jEdit</code>
 * class.<p>
 *
 * This class is partially thread-safe, however you must pay attention to two
 * very important guidelines:
 * <ul>
 * <li>Changes to a buffer can only be made from the AWT thread.
 * <li>When accessing the buffer from another thread, you must
 * grab a read lock if you plan on performing more than one call, to ensure that
 * the buffer contents are not changed by the AWT thread for the duration of the
 * lock. Only methods whose descriptions specify thread safety can be invoked
 * from other threads.
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id: Buffer.java 12867 2008-06-21 12:18:16Z k_satoda $
 */
public class Buffer extends JEditBuffer {

    /**
	 * Backed up property.
	 * @since jEdit 3.2pre2
	 */
    public static final String BACKED_UP = "Buffer__backedUp";

    /**
	 * Caret info properties.
	 * @since jEdit 3.2pre1
	 */
    public static final String CARET = "Buffer__caret";

    public static final String CARET_POSITIONED = "Buffer__caretPositioned";

    /**
	 * Stores a List of {@link org.gjt.sp.jedit.textarea.Selection} instances.
	 */
    public static final String SELECTION = "Buffer__selection";

    /**
	 * This should be a physical line number, so that the scroll
	 * position is preserved correctly across reloads (which will
	 * affect virtual line numbers, due to fold being reset)
	 */
    public static final String SCROLL_VERT = "Buffer__scrollVert";

    public static final String SCROLL_HORIZ = "Buffer__scrollHoriz";

    /**
	 * Should jEdit try to set the encoding based on a UTF8, UTF16 or
	 * XML signature at the beginning of the file?
	 */
    public static final String ENCODING_AUTODETECT = "encodingAutodetect";

    /**
	 * This property is set to 'true' if the file has a trailing newline.
	 * @since jEdit 4.0pre1
	 */
    public static final String TRAILING_EOL = "trailingEOL";

    /**
	 * This property is set to 'true' if the file should be GZipped.
	 * @since jEdit 4.0pre4
	 */
    public static final String GZIPPED = "gzipped";

    /**
	 * Reloads the buffer from disk, asking for confirmation if the buffer
	 * has unsaved changes.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
    public void reload(View view) {
        if (getFlag(UNTITLED)) return;
        if (isDirty()) {
            String[] args = { path };
        }
        load(view, true);
    }

    /**
	 * Loads the buffer from disk.
	 * @param view The view
	 * @param reload If true, user will not be asked to recover autosave
	 * file, if any
	 *
	 * @since 2.5pre1
	 */
    public boolean load(final View view, final boolean reload) {
        if (isPerformingIO()) {
            return false;
        }
        setBooleanProperty(BufferIORequest.ERROR_OCCURRED, false);
        setLoading(true);
        if (!getFlag(TEMPORARY)) EditBus.send(new BufferUpdate(this, view, BufferUpdate.LOAD_STARTED));
        final boolean loadAutosave;
        if (reload || !getFlag(NEW_FILE)) {
            if (file != null) modTime = file.lastModified();
            if (!reload && autosaveFile != null && autosaveFile.exists()) loadAutosave = recoverAutosave(view); else {
                if (autosaveFile != null) autosaveFile.delete();
                loadAutosave = false;
            }
            if (!loadAutosave) {
                VFS vfs = VFSManager.getVFSForPath(path);
                if (!checkFileForLoad(view, vfs, path)) {
                    setLoading(false);
                    return false;
                }
                if (reload || !getFlag(NEW_FILE)) {
                    if (!vfs.load(view, this, path)) {
                        setLoading(false);
                        return false;
                    }
                }
            }
        } else loadAutosave = false;
        Runnable runnable = new Runnable() {

            public void run() {
                String newPath = getStringProperty(BufferIORequest.NEW_PATH);
                Segment seg = (Segment) getProperty(BufferIORequest.LOAD_DATA);
                IntegerArray endOffsets = (IntegerArray) getProperty(BufferIORequest.END_OFFSETS);
                loadText(seg, endOffsets);
                unsetProperty(BufferIORequest.LOAD_DATA);
                unsetProperty(BufferIORequest.END_OFFSETS);
                unsetProperty(BufferIORequest.NEW_PATH);
                undoMgr.clear();
                undoMgr.setLimit(jEdit.getIntegerProperty("buffer.undoCount", 100));
                if (!getFlag(TEMPORARY)) finishLoading();
                setLoading(false);
                if (reload) setDirty(false);
                if (!loadAutosave && newPath != null) setPath(newPath);
                if (loadAutosave) Buffer.super.setDirty(true);
                if (!getFlag(TEMPORARY)) {
                    fireBufferLoaded();
                    EditBus.send(new BufferUpdate(Buffer.this, view, BufferUpdate.LOADED));
                }
            }
        };
        if (getFlag(TEMPORARY)) runnable.run();
        return true;
    }

    /**
	 * Loads a file from disk, and inserts it into this buffer.
	 * @param view The view
	 * @param path the path of the file to insert
	 *
	 * @since 4.0pre1
	 */
    public boolean insertFile(View view, String path) {
        if (isPerformingIO()) {
            return false;
        }
        setBooleanProperty(BufferIORequest.ERROR_OCCURRED, false);
        path = MiscUtilities.constructPath(this.path, path);
        VFS vfs = VFSManager.getVFSForPath(path);
        return vfs.insert(view, this, path);
    }

    /**
	 * Autosaves this buffer.
	 */
    public void autosave() {
        if (autosaveFile == null || !getFlag(AUTOSAVE_DIRTY) || !isDirty() || isPerformingIO() || !autosaveFile.getParentFile().exists()) return;
        setFlag(AUTOSAVE_DIRTY, false);
    }

    /**
	 * Prompts the user for a file to save this buffer to.
	 * @param view The view
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @since jEdit 2.6pre5
	 */
    public boolean saveAs(View view, boolean rename) {
        return true;
    }

    /**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 */
    public boolean save(View view, String path) {
        return save(view, path, true, false);
    }

    /**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @since jEdit 2.6pre5
	 */
    public boolean save(final View view, String path, final boolean rename) {
        return save(view, path, rename, false);
    }

    /**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @param disableFileStatusCheck  Disables file status checking
	 * regardless of the state of the checkFileStatus property
	 */
    public boolean save(final View view, String path, final boolean rename, boolean disableFileStatusCheck) {
        if (isPerformingIO()) {
            return false;
        }
        setBooleanProperty(BufferIORequest.ERROR_OCCURRED, false);
        if (path == null && getFlag(NEW_FILE)) return saveAs(view, rename);
        if (path == null && file != null) {
            long newModTime = file.lastModified();
            if (newModTime != modTime && jEdit.getBooleanProperty("view.checkModStatus")) {
                Object[] args = { this.path };
            }
        }
        EditBus.send(new BufferUpdate(this, view, BufferUpdate.SAVING));
        setPerformingIO(true);
        final String oldPath = this.path;
        final String oldSymlinkPath = symlinkPath;
        final String newPath = path == null ? this.path : path;
        VFS vfs = VFSManager.getVFSForPath(newPath);
        if (!checkFileForSave(view, vfs, newPath)) {
            setPerformingIO(false);
            return false;
        }
        Object session = vfs.createVFSSession(newPath, view);
        if (session == null) {
            setPerformingIO(false);
            return false;
        }
        unsetProperty("overwriteReadonly");
        unsetProperty("forbidTwoStageSave");
        try {
            VFSFile file = vfs._getFile(session, newPath, view);
            if (file != null) {
                boolean vfsRenameCap = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;
                if (!file.isWriteable()) {
                    Log.log(Log.WARNING, this, "Buffer saving : File " + file + " is readOnly");
                    if (vfsRenameCap) {
                        Log.log(Log.DEBUG, this, "Buffer saving : VFS can rename files");
                        String savePath = vfs._canonPath(session, newPath, view);
                        if (!MiscUtilities.isURL(savePath)) savePath = MiscUtilities.resolveSymlinks(savePath);
                        savePath = vfs.getTwoStageSaveName(savePath);
                        if (savePath == null) {
                            Log.log(Log.DEBUG, this, "Buffer saving : two stage save impossible because path is null");
                            VFSManager.error(view, newPath, "ioerror.save-readonly-twostagefail", null);
                            setPerformingIO(false);
                            return false;
                        }
                    } else {
                        Log.log(Log.WARNING, this, "Buffer saving : file is readonly and vfs cannot do two stage save");
                        VFSManager.error(view, newPath, "ioerror.write-error-readonly", null);
                        setPerformingIO(false);
                        return false;
                    }
                } else {
                    String savePath = vfs._canonPath(session, newPath, view);
                    if (!MiscUtilities.isURL(savePath)) savePath = MiscUtilities.resolveSymlinks(savePath);
                    savePath = vfs.getTwoStageSaveName(savePath);
                }
            }
        } catch (IOException io) {
            VFSManager.error(view, newPath, "ioerror", new String[] { io.toString() });
            setPerformingIO(false);
            return false;
        } finally {
            try {
                vfs._endVFSSession(session, view);
            } catch (IOException io) {
                VFSManager.error(view, newPath, "ioerror", new String[] { io.toString() });
                setPerformingIO(false);
                return false;
            }
        }
        if (!vfs.save(view, this, newPath)) {
            setPerformingIO(false);
            return false;
        }
        int check = jEdit.getIntegerProperty("checkFileStatus");
        return true;
    }

    public static final int FILE_NOT_CHANGED = 0;

    public static final int FILE_CHANGED = 1;

    public static final int FILE_DELETED = 2;

    /**
	 * Check if the buffer has changed on disk.
	 * @return One of <code>NOT_CHANGED</code>, <code>CHANGED</code>, or
	 * <code>DELETED</code>.
	 *
	 * @since jEdit 4.2pre1
	 */
    public int checkFileStatus(View view) {
        if (!isPerformingIO() && file != null && !getFlag(NEW_FILE)) {
            boolean newReadOnly = (file.exists() && !file.canWrite());
            if (newReadOnly != isFileReadOnly()) {
                setFileReadOnly(newReadOnly);
                EditBus.send(new BufferUpdate(this, null, BufferUpdate.DIRTY_CHANGED));
            }
            long oldModTime = modTime;
            long newModTime = file.lastModified();
            if (newModTime != oldModTime) {
                modTime = newModTime;
                if (!file.exists()) {
                    setFlag(NEW_FILE, true);
                    setDirty(true);
                    return FILE_DELETED;
                } else {
                    return FILE_CHANGED;
                }
            }
        }
        return FILE_NOT_CHANGED;
    }

    /**
	 * Returns the last time jEdit modified the file on disk.
	 * This method is thread-safe.
	 */
    public long getLastModified() {
        return modTime;
    }

    /**
	 * Sets the last time jEdit modified the file on disk.
	 * @param modTime The new modification time
	 */
    public void setLastModified(long modTime) {
        this.modTime = modTime;
    }

    /**
	 * Returns the status of the AUTORELOAD flag
	 * If true, reload changed files automatically
	 */
    public boolean getAutoReload() {
        return getFlag(AUTORELOAD);
    }

    /**
	 * Sets the status of the AUTORELOAD flag
	 * @param value # If true, reload changed files automatically
	 */
    public void setAutoReload(boolean value) {
        setFlag(AUTORELOAD, value);
    }

    /**
	 * Returns the status of the AUTORELOAD_DIALOG flag
	 * If true, prompt for reloading or notify user
	 * when the file has changed on disk
	 */
    public boolean getAutoReloadDialog() {
        return getFlag(AUTORELOAD_DIALOG);
    }

    /**
	 * Sets the status of the AUTORELOAD_DIALOG flag
	 * @param value # If true, prompt for reloading or notify user
	 * when the file has changed on disk

	 */
    public void setAutoReloadDialog(boolean value) {
        setFlag(AUTORELOAD_DIALOG, value);
    }

    /**
	 * Returns the virtual filesystem responsible for loading and
	 * saving this buffer. This method is thread-safe.
	 */
    public VFS getVFS() {
        return VFSManager.getVFSForPath(path);
    }

    /**
	 * Returns the autosave file for this buffer. This may be null if
	 * the file is non-local.
	 */
    public File getAutosaveFile() {
        return autosaveFile;
    }

    /**
	 * Remove the autosave file.
	 * @since jEdit 4.3pre12
	 */
    public void removeAutosaveFile() {
        if (autosaveFile != null) {
            autosaveFile.delete();
            setFlag(AUTOSAVE_DIRTY, true);
        }
    }

    /**
	 * Returns the name of this buffer. This method is thread-safe.
	 */
    public String getName() {
        return name;
    }

    /**
	 * Returns the path name of this buffer. This method is thread-safe.
	 */
    public String getPath() {
        return path;
    }

    /**
	 * If this file is a symbolic link, returns the link destination.
	 * Otherwise returns the file's path. This method is thread-safe.
	 * @since jEdit 4.2pre1
	 */
    public String getSymlinkPath() {
        return symlinkPath;
    }

    /**
	 * Returns the directory containing this buffer.
	 * @since jEdit 4.1pre11
	 */
    public String getDirectory() {
        return directory;
    }

    /**
	 * Returns true if this buffer has been closed with
	 * {@link org.gjt.sp.jedit.jEdit#closeBuffer(View,Buffer)}.
	 * This method is thread-safe.
	 */
    public boolean isClosed() {
        return getFlag(CLOSED);
    }

    /**
	 * Returns true if the buffer is loaded. This method is thread-safe.
	 */
    public boolean isLoaded() {
        return !isLoading();
    }

    /**
	 * Returns whether this buffer lacks a corresponding version on disk.
	 * This method is thread-safe.
	 */
    public boolean isNewFile() {
        return getFlag(NEW_FILE);
    }

    /**
	 * Sets the new file flag.
	 * @param newFile The new file flag
	 */
    public void setNewFile(boolean newFile) {
        setFlag(NEW_FILE, newFile);
        if (!newFile) setFlag(UNTITLED, false);
    }

    /**
	 * Returns true if this file is 'untitled'. This method is thread-safe.
	 */
    public boolean isUntitled() {
        return getFlag(UNTITLED);
    }

    /**
	 * Sets the 'dirty' (changed since last save) flag of this buffer.
	 */
    @Override
    public void setDirty(boolean d) {
        boolean old_d = isDirty();
        super.setDirty(d);
        boolean editable = isEditable();
        if (d) {
            if (editable) setFlag(AUTOSAVE_DIRTY, true);
        } else {
            setFlag(AUTOSAVE_DIRTY, false);
            if (autosaveFile != null) autosaveFile.delete();
        }
        if (d != old_d && editable) {
            EditBus.send(new BufferUpdate(this, null, BufferUpdate.DIRTY_CHANGED));
        }
    }

    /**
	 * Returns if this is a temporary buffer. This method is thread-safe.
	 * @see jEdit#openTemporary(View,String,String,boolean)
	 * @see jEdit#commitTemporary(Buffer)
	 * @since jEdit 2.2pre7
	 */
    public boolean isTemporary() {
        return getFlag(TEMPORARY);
    }

    /**
	 * Returns this buffer's icon.
	 * @since jEdit 2.6pre6
	 */
    public Icon getIcon() {
        return null;
    }

    /**
	 * Reloads settings from the properties. This should be called
	 * after the <code>syntax</code> or <code>folding</code>
	 * buffer-local properties are changed.
	 */
    @Override
    public void propertiesChanged() {
        super.propertiesChanged();
        EditBus.send(new BufferUpdate(this, null, BufferUpdate.PROPERTIES_CHANGED));
    }

    @Override
    public Object getDefaultProperty(String name) {
        Object retVal;
        if (mode != null) {
            retVal = mode.getProperty(name);
            if (retVal == null) return null;
            setDefaultProperty(name, retVal);
            return retVal;
        }
        String value = jEdit.getProperty("buffer." + name);
        if (value == null) return null;
        try {
            retVal = new Integer(value);
        } catch (NumberFormatException nf) {
            retVal = value;
        }
        return retVal;
    }

    /**
	 * Toggles word wrap between the three available modes. This is used
	 * by the status bar.
	 * @param view We show a message in the view's status bar
	 * @since jEdit 4.1pre3
	 */
    public void toggleWordWrap(View view) {
        String wrap = getStringProperty("wrap");
        if (wrap.equals("none")) wrap = "soft"; else if (wrap.equals("soft")) wrap = "hard"; else if (wrap.equals("hard")) wrap = "none";
        setProperty("wrap", wrap);
        propertiesChanged();
    }

    /**
	 * Toggles the line separator between the three available settings.
	 * This is used by the status bar.
	 * @param view We show a message in the view's status bar
	 * @since jEdit 4.1pre3
	 */
    public void toggleLineSeparator(View view) {
        String status = null;
        String lineSep = getStringProperty("lineSeparator");
        if ("\n".equals(lineSep)) {
            status = "windows";
            lineSep = "\r\n";
        } else if ("\r\n".equals(lineSep)) {
            status = "mac";
            lineSep = "\r";
        } else if ("\r".equals(lineSep)) {
            status = "unix";
            lineSep = "\n";
        }
        setProperty("lineSeparator", lineSep);
        setDirty(true);
        propertiesChanged();
    }

    /**
	 * Some settings, like comment start and end strings, can
	 * vary between different parts of a buffer (HTML text and inline
	 * JavaScript, for example).
	 * @param offset The offset
	 * @param name The property name
	 * @since jEdit 4.0pre3
	 */
    @Override
    public String getContextSensitiveProperty(int offset, String name) {
        Object value = super.getContextSensitiveProperty(offset, name);
        if (value == null) {
            ParserRuleSet rules = getRuleSetAtOffset(offset);
            value = jEdit.getMode(rules.getModeName()).getProperty(name);
            if (value == null) value = mode.getProperty(name);
        }
        if (value == null) return null; else return String.valueOf(value);
    }

    /**
	 * Sets this buffer's edit mode by calling the accept() method
	 * of each registered edit mode.
	 */
    public void setMode() {
        String userMode = getStringProperty("mode");
        if (userMode != null) {
            unsetProperty("mode");
            Mode m = ModeProvider.instance.getMode(userMode);
            if (m != null) {
                setMode(m);
                return;
            }
        }
        String firstLine = getLineText(0);
        Mode mode = ModeProvider.instance.getModeForFile(name, firstLine);
        if (mode != null) {
            setMode(mode);
            return;
        }
        Mode defaultMode = jEdit.getMode(jEdit.getProperty("buffer.defaultMode"));
        if (defaultMode == null) defaultMode = jEdit.getMode("text");
        setMode(defaultMode);
    }

    /**
	 * @deprecated Call <code>setProperty()</code> instead.
	 */
    @Deprecated
    public void putProperty(Object name, Object value) {
        if (!(name instanceof String)) return;
        setProperty((String) name, value);
    }

    /**
	 * @deprecated Call <code>setBooleanProperty()</code> instead
	 */
    @Deprecated
    public void putBooleanProperty(String name, boolean value) {
        setBooleanProperty(name, value);
    }

    /**
	 * @deprecated Use org.gjt.sp.jedit.syntax.DefaultTokenHandler instead
	 */
    @Deprecated
    public static class TokenList extends DefaultTokenHandler {

        public Token getFirstToken() {
            return getTokens();
        }
    }

    /**
	 * @deprecated Use the other form of <code>markTokens()</code> instead
	 */
    @Deprecated
    public TokenList markTokens(int lineIndex) {
        TokenList list = new TokenList();
        markTokens(lineIndex, list);
        return list;
    }

    /**
	 * @deprecated Call <code>insert()</code> instead.
	 */
    @Deprecated
    public void insertString(int offset, String str, AttributeSet attr) {
        insert(offset, str);
    }

    /**
	 * @deprecated Do not call this method, use {@link #getPath()}
	 * instead.
	 */
    @Deprecated
    public File getFile() {
        return file;
    }

    /**
	 * Returns the status prompt for the given marker action. Only
	 * intended to be called from <code>actions.xml</code>.
	 * @since jEdit 4.2pre2
	 */
    public String getMarkerStatusPrompt(String action) {
        return jEdit.getProperty("view.status." + action, new String[] { getMarkerNameString() });
    }

    /**
	 * Returns a string of all set markers, used by the status bar
	 * (eg, "a b $ % ^").
	 * @since jEdit 4.2pre2
	 */
    public String getMarkerNameString() {
        StringBuilder buf = new StringBuilder();
        if (buf.length() == 0) return jEdit.getProperty("view.status.no-markers"); else return buf.toString();
    }

    /**
	 * If a marker is set on the line of the position, it is removed. Otherwise
	 * a new marker with the specified shortcut is added.
	 * @param pos The position of the marker
	 * @param shortcut The shortcut ('\0' if none)
	 * @since jEdit 3.2pre5
	 */
    public void addOrRemoveMarker(char shortcut, int pos) {
        int line = getLineOfOffset(pos);
    }

    /**
	 * Adds a marker to this buffer.
	 * @param pos The position of the marker
	 * @param shortcut The shortcut ('\0' if none)
	 * @since jEdit 3.2pre1
	 */
    public void addMarker(char shortcut, int pos) {
        boolean added = false;
        if (isLoaded()) {
        }
        if (isLoaded() && !getFlag(TEMPORARY)) {
            EditBus.send(new BufferUpdate(this, null, BufferUpdate.MARKERS_CHANGED));
        }
    }

    /**
	 * Removes all defined markers.
	 * @since jEdit 2.6pre1
	 */
    public void removeAllMarkers() {
        setFlag(MARKERS_CHANGED, true);
        if (isLoaded()) {
            EditBus.send(new BufferUpdate(this, null, BufferUpdate.MARKERS_CHANGED));
        }
    }

    /**
	 * Returns the path for this buffer's markers file
	 * @param vfs The appropriate VFS
	 * @since jEdit 4.3pre7
	 * @deprecated it will fail if you save to another VFS. use {@link #getMarkersPath(VFS, String)}
	 */
    @Deprecated
    public String getMarkersPath(VFS vfs) {
        return getMarkersPath(vfs, path);
    }

    /**
	 * Returns the path for this buffer's markers file
	 * @param vfs The appropriate VFS
	 * @param path the path of the buffer, it can be different from the field
	 * when using save-as
	 * @since jEdit 4.3pre10
	 */
    public static String getMarkersPath(VFS vfs, String path) {
        return vfs.getParentOfPath(path) + '.' + vfs.getFileName(path) + ".marks";
    }

    /**
	 * Save the markers file, or delete it when there are mo markers left
	 * Handling markers is now independent from saving the buffer.
	 * Changing markers will not set the buffer dirty any longer.
	 * @param view The current view
	 * @since jEdit 4.3pre7
	 */
    public boolean updateMarkersFile(View view) {
        if (!markersChanged()) return true;
        VFS vfs = VFSManager.getVFSForPath(getPath());
        if (((vfs.getCapabilities() & VFS.WRITE_CAP) == 0) || (!vfs.isMarkersFileSupported())) {
            VFSManager.error(view, path, "vfs.not-supported.save", new String[] { "markers file" });
            return false;
        }
        Object session = vfs.createVFSSession(path, view);
        if (session == null) return false;
        return true;
    }

    /**
	 * Return true when markers have changed and the markers file needs
	 * to be updated
	 * @since jEdit 4.3pre7
	 */
    public boolean markersChanged() {
        return getFlag(MARKERS_CHANGED);
    }

    /**
	 * Sets/unsets the MARKERS_CHANGED flag
	 * @since jEdit 4.3pre7
	 */
    public void setMarkersChanged(boolean changed) {
        setFlag(MARKERS_CHANGED, changed);
    }

    /**
	 * This socket is closed when the buffer is closed.
	 */
    public void setWaitSocket(Socket waitSocket) {
        this.waitSocket = waitSocket;
    }

    /**
	 * Returns the next buffer in the list.
	 */
    public Buffer getNext() {
        return next;
    }

    /**
	 * Returns the previous buffer in the list.
	 */
    public Buffer getPrev() {
        return prev;
    }

    /**
	 * Returns the position of this buffer in the buffer list.
	 */
    public int getIndex() {
        int count = 0;
        Buffer buffer = prev;
        while (true) {
            if (buffer == null) break;
            count++;
            buffer = buffer.prev;
        }
        return count;
    }

    /**
	 * Returns a string representation of this buffer.
	 * This simply returns the path name.
	 */
    @Override
    public String toString() {
        return name + " (" + directory + ')';
    }

    /** The previous buffer in the list. */
    Buffer prev;

    /** The next buffer in the list. */
    Buffer next;

    Buffer(String path, boolean newFile, boolean temp, Hashtable props) {
        super(props);
        setFlag(TEMPORARY, temp);
        setPath(path);
        setFlag(UNTITLED, newFile);
        setFlag(NEW_FILE, newFile);
        setFlag(AUTORELOAD, jEdit.getBooleanProperty("autoReload"));
        setFlag(AUTORELOAD_DIALOG, jEdit.getBooleanProperty("autoReloadDialog"));
    }

    void commitTemporary() {
        setFlag(TEMPORARY, false);
        finishLoading();
    }

    void close() {
        setFlag(CLOSED, true);
        if (autosaveFile != null) autosaveFile.delete();
        if (waitSocket != null) {
            try {
                waitSocket.getOutputStream().write('\0');
                waitSocket.getOutputStream().flush();
                waitSocket.getInputStream().close();
                waitSocket.getOutputStream().close();
                waitSocket.close();
            } catch (IOException io) {
            }
        }
    }

    private void setFlag(int flag, boolean value) {
        if (value) flags |= (1 << flag); else flags &= ~(1 << flag);
    }

    private boolean getFlag(int flag) {
        int mask = (1 << flag);
        return (flags & mask) == mask;
    }

    private static final int CLOSED = 0;

    private static final int NEW_FILE = 3;

    private static final int UNTITLED = 4;

    private static final int AUTOSAVE_DIRTY = 5;

    private static final int AUTORELOAD = 6;

    private static final int AUTORELOAD_DIALOG = 7;

    private static final int TEMPORARY = 10;

    private static final int MARKERS_CHANGED = 12;

    private int flags;

    private String path;

    private String symlinkPath;

    private String name;

    private String directory;

    private File file;

    private File autosaveFile;

    private long modTime;

    private Socket waitSocket;

    private void setPath(final String path) {
        this.path = path;
        VFS vfs = VFSManager.getVFSForPath(path);
        if ((vfs.getCapabilities() & VFS.WRITE_CAP) == 0) setFileReadOnly(true);
        name = vfs.getFileName(path);
        directory = vfs.getParentOfPath(path);
        if (vfs instanceof FileVFS) {
            file = new File(path);
            symlinkPath = MiscUtilities.resolveSymlinks(path);
            if (autosaveFile != null) autosaveFile.delete();
            autosaveFile = new File(file.getParent(), '#' + name + '#');
        } else {
            file = null;
            autosaveFile = null;
            symlinkPath = path;
        }
    }

    private boolean recoverAutosave(final View view) {
        if (!autosaveFile.canRead()) return false;
        final Object[] args = { autosaveFile.getPath() };
        return false;
    }

    private boolean checkFileForLoad(View view, VFS vfs, String path) {
        if ((vfs.getCapabilities() & VFS.LOW_LATENCY_CAP) != 0) {
            Object session = vfs.createVFSSession(path, view);
            if (session == null) return false;
            try {
                VFSFile file = vfs._getFile(session, path, view);
                if (file == null) {
                    setNewFile(true);
                    return true;
                }
                if (!file.isReadable()) {
                    VFSManager.error(view, path, "ioerror.no-read", null);
                    setNewFile(false);
                    return false;
                }
                setFileReadOnly(!file.isWriteable());
                if (file.getType() != VFSFile.FILE) {
                    VFSManager.error(view, path, "ioerror.open-directory", null);
                    setNewFile(false);
                    return false;
                }
            } catch (IOException io) {
                VFSManager.error(view, path, "ioerror", new String[] { io.toString() });
                return false;
            } finally {
                try {
                    vfs._endVFSSession(session, view);
                } catch (IOException io) {
                    VFSManager.error(view, path, "ioerror", new String[] { io.toString() });
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkFileForSave(View view, VFS vfs, String path) {
        if ((vfs.getCapabilities() & VFS.LOW_LATENCY_CAP) != 0) {
            Object session = vfs.createVFSSession(path, view);
            if (session == null) return false;
            try {
                VFSFile file = vfs._getFile(session, path, view);
                if (file == null) return true;
                if (file.getType() != VFSFile.FILE) {
                    VFSManager.error(view, path, "ioerror.save-directory", null);
                    return false;
                }
            } catch (IOException io) {
                VFSManager.error(view, path, "ioerror", new String[] { io.toString() });
                return false;
            } finally {
                try {
                    vfs._endVFSSession(session, view);
                } catch (IOException io) {
                    VFSManager.error(view, path, "ioerror", new String[] { io.toString() });
                    return false;
                }
            }
        }
        return true;
    }

    private void finishLoading() {
        parseBufferLocalProperties();
        FoldHandler oldFoldHandler = getFoldHandler();
        setMode();
        if (getFoldHandler() == oldFoldHandler) {
            invalidateFoldLevels();
            fireFoldHandlerChanged();
        }
    }

    private void finishSaving(View view, String oldPath, String oldSymlinkPath, String path, boolean rename, boolean error) {
        if (!error && !path.equals(oldPath)) {
            Buffer buffer = jEdit.getBuffer(path);
            if (rename) {
                if (buffer != null && !buffer.getPath().equals(oldPath)) {
                    buffer.setDirty(false);
                    jEdit.closeBuffer(view, buffer);
                }
                setPath(path);
            } else {
                if (buffer != null && !buffer.getPath().equals(oldPath)) {
                    buffer.load(view, true);
                }
            }
        }
        if (rename) {
            if (file != null) modTime = file.lastModified();
            if (!error) {
                try {
                    writeLock();
                    if (autosaveFile != null) autosaveFile.delete();
                    setFlag(AUTOSAVE_DIRTY, false);
                    setFileReadOnly(false);
                    setFlag(NEW_FILE, false);
                    setFlag(UNTITLED, false);
                    super.setDirty(false);
                    if (jEdit.getBooleanProperty("resetUndoOnSave")) {
                        undoMgr.clear();
                    }
                } finally {
                    writeUnlock();
                }
                parseBufferLocalProperties();
                if (!getPath().equals(oldPath)) {
                    if (!isTemporary()) jEdit.updatePosition(oldSymlinkPath, this);
                    setMode();
                } else {
                    String newMode = getStringProperty("mode");
                    if (newMode != null && !newMode.equals(getMode().getName())) setMode(); else propertiesChanged();
                }
                if (!isTemporary()) {
                    EditBus.send(new BufferUpdate(this, view, BufferUpdate.DIRTY_CHANGED));
                    EditBus.send(new BufferUpdate(this, view, BufferUpdate.SAVED));
                }
            }
        }
    }

    /**
	 * Edit the syntax style of the token under the caret.
	 *
	 * @param textArea the textarea where your caret is
	 * @since jEdit 4.3pre11
	 */
    void editSyntaxStyle(JEditTextArea textArea) {
        int lineNum = textArea.getCaretLine();
        int start = getLineStartOffset(lineNum);
        int position = textArea.getCaretPosition();
        DefaultTokenHandler tokenHandler = new DefaultTokenHandler();
        markTokens(lineNum, tokenHandler);
        Token token = tokenHandler.getTokens();
        while (token.id != Token.END) {
            int next = start + token.length;
            if (start <= position && next > position) break;
            start = next;
            token = token.next;
        }
        if (token.id == Token.END || token.id == Token.NULL) {
            JOptionPane.showMessageDialog(jEdit.getActiveView(), jEdit.getProperty("syntax-style-no-token.message"), jEdit.getProperty("syntax-style-no-token.title"), JOptionPane.PLAIN_MESSAGE);
            return;
        }
        String typeName = Token.tokenToString(token.id);
        String property = "view.style." + typeName.toLowerCase();
        jEdit.propertiesChanged();
    }
}
