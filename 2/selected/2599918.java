package CH.ifa.draw.samples.javadraw;

import CH.ifa.draw.framework.*;
import CH.ifa.draw.standard.*;
import CH.ifa.draw.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * @version <$CURRENT_VERSION$>
 */
public class JavaDrawViewer extends JApplet implements DrawingEditor {

    private Drawing fDrawing;

    private Tool fTool;

    private StandardDrawingView fView;

    private Iconkit fIconkit;

    private transient UndoManager myUndoManager;

    public void init() {
        setUndoManager(new UndoManager());
        getContentPane().setLayout(new BorderLayout());
        fView = new StandardDrawingView(this, 400, 370);
        getContentPane().add("Center", fView);
        fTool = new FollowURLTool(this, this);
        fIconkit = new Iconkit(this);
        String filename = getParameter("Drawing");
        if (filename != null) {
            loadDrawing(filename);
            fView.setDrawing(fDrawing);
        } else {
            showStatus("Unable to load drawing");
        }
    }

    public void addViewChangeListener(ViewChangeListener vsl) {
    }

    public void removeViewChangeListener(ViewChangeListener vsl) {
    }

    private void loadDrawing(String filename) {
        try {
            URL url = new URL(getCodeBase(), filename);
            InputStream stream = url.openStream();
            StorableInput reader = new StorableInput(stream);
            fDrawing = (Drawing) reader.readStorable();
        } catch (IOException e) {
            fDrawing = new StandardDrawing();
            System.err.println("Error when Loading: " + e);
            showStatus("Error when Loading: " + e);
        }
    }

    /**
	 * Gets the editor's drawing view.
	 */
    public DrawingView view() {
        return fView;
    }

    public DrawingView[] views() {
        return new DrawingView[] { view() };
    }

    /**
	 * Gets the editor's drawing.
	 */
    public Drawing drawing() {
        return fDrawing;
    }

    /**
	 * Gets the current the tool (there is only one):
	 */
    public Tool tool() {
        return fTool;
    }

    /**
	 * Sets the editor's default tool. Do nothing since we only have one tool.
	 */
    public void toolDone() {
    }

    /**
	 * Ignore selection changes, we don't show any selection
	 */
    public void figureSelectionChanged(DrawingView view) {
    }

    protected void setUndoManager(UndoManager newUndoManager) {
        myUndoManager = newUndoManager;
    }

    public UndoManager getUndoManager() {
        return myUndoManager;
    }
}
