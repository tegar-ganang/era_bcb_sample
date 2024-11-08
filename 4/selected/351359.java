package ftraq.fs;

import ftraq.gui.UiObserver;
import ftraq.fs.exceptions.ReadFromSourceFileFailure;
import ftraq.fs.exceptions.WriteToTargetFileFailure;

/**
 * most simple implementation of a text editor
 * 
 * @author <a href="mailto:jssauder@tfh-berlin.de">Steffen Sauder</a>
 * @version 1.0 *
 */
public class LgTextFileEditorImpl implements LgTextFileEditor {

    /** a set of observers do notify when the text editors state changes */
    private java.util.Set _observerSet = new java.util.HashSet(2);

    /** the file that is being edited in this editor */
    private LgFile _editedFile;

    /** the content of the file as a large String */
    private String _fileContent = new String();

    /** true if the editor is currently reading the file from the source */
    private boolean _isReading = false;

    /** true if the editor is currently saving the file */
    private boolean _isWriting = false;

    /** the number of lines already transferred when reading or writing */
    private int _linesTransferred;

    public LgTextFileEditorImpl() {
    }

    public void openTextFile(LgFile i_fileToOpen) throws ftraq.fs.exceptions.ReadFromSourceFileFailure {
        this._isReading = true;
        this._editedFile = i_fileToOpen;
        this.notifyObserversCompleteUpdate();
        this._fileContent = this._readFileContent();
        this._isReading = false;
        this.notifyObserversCompleteUpdate();
    }

    public void reloadOriginalContent() throws ReadFromSourceFileFailure {
        this._linesTransferred = 0;
        this._isReading = true;
        this._fileContent = new String();
        this.notifyObserversCompleteUpdate();
        this._fileContent = this._readFileContent();
        this._isReading = false;
        this.notifyObserversCompleteUpdate();
    }

    public void saveChanges(String i_newContent) throws WriteToTargetFileFailure {
        this._isWriting = true;
        OutputStreamWithWriteLine outputStream = null;
        this._linesTransferred = 0;
        this.notifyObserversUpdateStatusLine();
        try {
            outputStream = this._editedFile.getOutputStream(true);
            java.io.StringReader stringReader = new java.io.StringReader(i_newContent);
            java.io.LineNumberReader lineNumberReader = new java.io.LineNumberReader(stringReader);
            while (true) {
                String nextLine = lineNumberReader.readLine();
                if (nextLine == null) {
                    break;
                }
                outputStream.writeLine(nextLine);
                this._linesTransferred++;
                this.notifyObserversUpdateStatusLine();
            }
            outputStream.close();
            this._isWriting = false;
        } catch (Exception e) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (java.io.IOException ex) {
                }
            }
            this._isWriting = false;
            if (e instanceof ftraq.fs.exceptions.WriteToTargetFileFailure) {
                throw (ftraq.fs.exceptions.WriteToTargetFileFailure) e;
            }
            throw new ftraq.fs.exceptions.WriteToTargetFileFailure(e, this._editedFile);
        }
    }

    public void close() {
        this._editedFile = null;
        this._fileContent = null;
        System.gc();
    }

    public boolean shouldEditingBeEnabled() {
        return !(this._isReading || this._isWriting);
    }

    public String getFileContent() {
        return this._fileContent;
    }

    public int getLinesRead() {
        return this._linesTransferred;
    }

    public String getStatusString() {
        if (this._editedFile == null) {
            return "not used";
        }
        if (this._isReading) {
            if (this._linesTransferred == 0) {
                return "connecting to " + this._editedFile.getURL() + "...";
            }
            return "received " + this._linesTransferred + " lines from " + this._editedFile.getURL();
        }
        if (this._isWriting) {
            if (this._linesTransferred == 0) {
                return "trying to write to " + this._editedFile.getURL() + "...";
            }
            return "wrote " + this._linesTransferred + " lines to " + this._editedFile.getURL();
        }
        return "the document contains " + this._linesTransferred + " lines.";
    }

    public String getFrameTitle() {
        if (this._editedFile != null) {
            return "editing " + this._editedFile.getURL();
        }
        return "connecting...";
    }

    public void addObserver(UiObserver i_observer) {
        synchronized (this._observerSet) {
            this._observerSet.add(i_observer);
        }
    }

    public void removeObserver(UiObserver i_observer) {
        synchronized (this._observerSet) {
            this._observerSet.remove(i_observer);
        }
    }

    public void notifyObserversCompleteUpdate() {
        synchronized (this._observerSet) {
            java.util.Iterator it = this._observerSet.iterator();
            while (it.hasNext()) {
                ((ftraq.gui.UiObserver) it.next()).updateGui();
            }
        }
    }

    public void notifyObserversUpdateStatusLine() {
        synchronized (this._observerSet) {
            java.util.Iterator it = this._observerSet.iterator();
            while (it.hasNext()) {
                ((ftraq.gui.UiObserver) it.next()).updateStatusLine();
            }
        }
    }

    private String _readFileContent() throws ReadFromSourceFileFailure {
        InputStreamWithReadLine inputStream = null;
        try {
            StringBuffer contentBuffer = new StringBuffer();
            inputStream = this._editedFile.getInputStream(true);
            while (true) {
                String nextLine = inputStream.readLine();
                if (nextLine == null) {
                    break;
                }
                contentBuffer.append(nextLine);
                contentBuffer.append("\n");
                this._linesTransferred++;
                if (this._linesTransferred == 50) {
                    this._fileContent = contentBuffer.toString() + "\n\n[this is not the end of the file, still reading ...]";
                    this.notifyObserversCompleteUpdate();
                }
                this.notifyObserversUpdateStatusLine();
            }
            inputStream.close();
            return contentBuffer.toString();
        } catch (Exception e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException ex) {
                }
            }
            if (e instanceof ftraq.fs.exceptions.ReadFromSourceFileFailure) {
                throw (ftraq.fs.exceptions.ReadFromSourceFileFailure) e;
            }
            throw new ftraq.fs.exceptions.ReadFromSourceFileFailure(e, this._editedFile);
        }
    }
}
