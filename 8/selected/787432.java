package com.prolix.editor.main.workspace.prolix.mainEditor;

import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;

/**
 * @author naien
 * 
 */
public class ProlixScrollingGraphicalViewer extends ScrollingGraphicalViewer {

    private GEFEditor editor;

    public ProlixScrollingGraphicalViewer(GEFEditor editor) {
        super();
        this.editor = editor;
    }

    public GEFEditor getEditor() {
        return editor;
    }
}
