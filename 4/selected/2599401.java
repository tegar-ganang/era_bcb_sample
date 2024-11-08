package control.implementations.scene;

import java.util.Stack;
import model.abstractions.file.AbstractDirectory;
import model.abstractions.file.AbstractFileNode;
import model.implementations.eventhandling.ControlEvent;
import view.abstractions.window.IMoveableScene;
import view.abstractions.window.ISceneWindow;
import control.abstractions.eventhandling.AbstractControlObservable;
import control.abstractions.eventhandling.IControlObserver;
import control.abstractions.file.AbstractFileFactory;
import control.abstractions.file.AbstractFileWalker;
import control.abstractions.log.AbstractLogWriter;
import control.abstractions.scene.AbstractFilePlacer;
import control.implementations.window.MainEventListener;

/**
 * Places the files in the current directory onto the frames.
 * 
 * @author falk
 *
 */
public class FilePlacer extends AbstractFilePlacer implements IControlObserver {

    /**
	 * Stores the active file factory. 
	 */
    private AbstractFileFactory fileFactory;

    /**
	 * Stores all opened directories. (Allowes to step one directory back!)
	 */
    private Stack fileNodeStack = new Stack();

    /**
	 * Stores the root node. This is the last node that could not be left.
	 */
    private AbstractFileNode rootNode;

    /**
	 * Stores the scene.
	 * This object is needed for all scene acesses (i.e. the frames, the rotation, etc).
	 */
    private IMoveableScene sceneView;

    /**
	 * Stores the scene window.
	 * This is not used localy but will be needed for the handling classes that open
	 * the videos to disable the main window.
	 */
    private ISceneWindow sceneWindow;

    /**
	 * Stores the main event listener to add itself as observer (GoF observer),
	 */
    private MainEventListener eventListener = MainEventListener.getInstance();

    /**
	 * The file walker is needed to read sub files of an directory.
	 */
    private AbstractFileWalker fileWalker;

    /**
	 * Stores all the subfiles of the active directory.
	 */
    private AbstractFileNode[] subFiles;

    /**
	 * Stores the active shown files as an array.
	 * This array is sorted the same way as the files on the frames.
	 * The fileNode of frame 0 of the active group is stored in showFiles[0] and so on.
	 */
    private AbstractFileNode[] showFiles;

    /**
	 * Stores the file number of the active file in the subFiles array.
	 * This is nessesary to translate subFiles into showFiles.
	 */
    private int activeFileNumber = 0;

    /**
	 * Object of the file opener. It handles all open events.
	 */
    private FileOpener fileOpener;

    public FilePlacer(ISceneWindow sceneWindow, IMoveableScene sceneView, AbstractFileNode entryNode, AbstractFileFactory fileFactory) {
        this.sceneWindow = sceneWindow;
        this.sceneView = sceneView;
        this.fileFactory = fileFactory;
        this.rootNode = entryNode;
        this.fileNodeStack.add(this.rootNode);
        this.fileWalker = this.fileFactory.createFileWalker();
        this.showFiles = new AbstractFileNode[sceneView.getFramesPerGroup()];
        this.fileOpener = new FileOpener(this.sceneWindow, this.sceneView, this);
        this.updateFileArray(this.rootNode);
        this.initFileArray();
        this.eventListener.addObserver(this);
    }

    /**
	 * Moves places the showFiles array onto the frames.
	 * This is called before every rotation.
	 * This will be optimized in the future.
	 *
	 */
    private void showFileArray() {
        this.sceneView.setFiles(this.sceneView.getActive(), this.showFiles);
    }

    /**
	 * Creates an initial showFiles.
	 * This will be optimized in the future.
	 *
	 */
    private void initFileArray() {
        AbstractLogWriter.getInstance().write(this, "active file number is " + this.getActiveFileNumber(), AbstractLogWriter.VERBOSE);
        for (int i = -3; i < this.sceneView.getFramesPerGroup() - 3; i++) {
            AbstractLogWriter.getInstance().write(this, i + ": -> " + this.getRelativeSubFile(i), AbstractLogWriter.VERBOSE);
            this.setRelativeShowFile(i, this.getRelativeSubFile(i));
        }
        this.showFileArray();
    }

    /**
	 * Updates the data in the subFiles array.
	 * @param fileNode
	 */
    private void updateFileArray(AbstractFileNode fileNode) {
        if (!fileNode.isLeaf()) {
            this.fileWalker.readData(fileNode);
            AbstractDirectory dirNode = (AbstractDirectory) fileNode;
            subFiles = new AbstractFileNode[dirNode.Count()];
            for (int i = 0; i < this.subFilesCount(); i++) {
                this.subFiles[i] = dirNode.getChild(i);
                AbstractLogWriter.getInstance().write(this, i + ": " + this.subFiles[i].getName(), AbstractLogWriter.VERBOSE);
            }
            AbstractLogWriter.getInstance().write(this, "---------------------------------------", AbstractLogWriter.VERBOSE);
        } else {
            subFiles = null;
            AbstractLogWriter.getInstance().write(this, "file is not a directory.", AbstractLogWriter.ERROR);
        }
    }

    /**
	 * Receives all key events.
	 * @param event Intput event.
	 */
    private void handleKeyEvent(ControlEvent event) {
        switch(((Integer) event.getEventValue()).intValue()) {
            case 37:
                if (this.sceneView.cursorLeft()) {
                    this.setActiveFileNumber(this.getActiveFileNumber() + 1);
                    this.setRelativeShowFile(this.sceneView.getFramesPerGroup() / 2, this.getRelativeSubFile(this.sceneView.getFramesPerGroup() / 2));
                    this.showFileArray();
                }
                break;
            case 39:
                if (this.sceneView.cursorRight()) {
                    this.setActiveFileNumber(this.getActiveFileNumber() - 1);
                    this.setRelativeShowFile(1 - (this.sceneView.getFramesPerGroup() / 2), this.getRelativeSubFile(1 - (this.sceneView.getFramesPerGroup() / 2)));
                    this.showFileArray();
                }
                break;
            case 10:
            default:
                AbstractLogWriter.getInstance().write(this, "unhandled key: " + event.getEventValue(), AbstractLogWriter.VERBOSE);
                break;
        }
    }

    /**
	 * Receives all input events.
	 * @see AbstractControlObservable
	 */
    public void receiveEvent(ControlEvent event) {
        if (event.getEventType() == ControlEvent.KEY_EVENT) handleKeyEvent(event);
    }

    /**
	 * 
	 * @return Returns the number of the active file.
	 */
    private int getActiveFileNumber() {
        return activeFileNumber;
    }

    /**
	 * Changes the number of the active file.
	 * Using this function avoids array out of bound exceptions.
	 * @param activeFileNumber New number of the active file.
	 */
    public void setActiveFileNumber(int activeFileNumber) {
        activeFileNumber %= this.subFilesCount();
        if (activeFileNumber < 0) activeFileNumber += this.subFilesCount();
        this.activeFileNumber = activeFileNumber;
    }

    /**
	 * Returns a sub file relative from the active position.
	 * @param i Offset value.
	 * @return AbstractFile.
	 */
    private AbstractFileNode getRelativeSubFile(int i) {
        i = this.getActiveFileNumber() + i;
        i %= this.subFilesCount();
        if (i < 0) {
            i += this.subFilesCount();
        }
        AbstractLogWriter.getInstance().write(this, "read sub file: " + this.getSubFile(i), AbstractLogWriter.VERBOSE);
        return this.getSubFile(i);
    }

    /**
	 * 
	 * @param i Index of the subFile.
	 * @return Returns a specific subFile.
	 */
    private AbstractFileNode getSubFile(int i) {
        return this.subFiles[i];
    }

    /**
	 * 
	 * @return Number of subFieles.
	 */
    private int subFilesCount() {
        return this.subFiles.length;
    }

    /**
	 * Changes a file inside of the showFile array relative to the active file.
	 * 
	 * @param i Offset.
	 * @param file New file object.
	 */
    private void setRelativeShowFile(int i, AbstractFileNode file) {
        i += this.sceneView.getActiveFrame();
        i %= this.sceneView.getFramesPerGroup();
        if (i < 0) i += this.sceneView.getFramesPerGroup();
        this.showFiles[i] = file;
        for (int j = 0; j < this.showFiles.length; j++) {
            AbstractLogWriter.getInstance().write(this, " ->> " + j + ": " + getRelativeShowFile(j), AbstractLogWriter.VERBOSE);
        }
    }

    /**
	 * Reads a file inside the showFile array relatively.
	 * 
	 * @param i Offset.
	 * @return File.
	 */
    private AbstractFileNode getRelativeShowFile(int i) {
        i += this.sceneView.getActiveFrame();
        i %= this.sceneView.getFramesPerGroup();
        if (i < 0) i += this.sceneView.getFramesPerGroup();
        return this.showFiles[i];
    }
}
