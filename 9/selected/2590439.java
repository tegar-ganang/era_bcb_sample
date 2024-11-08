package ieditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public abstract class GraphicalEditorWithPalettes extends GraphicalEditorWithPalette {

    protected void createPaletteViewer(Composite parent) {
        PaletteViewer viewer = new PaletteViewer();
        setPaletteViewer(viewer);
        viewer.createControl(parent);
        configurePaletteViewer();
        hookPaletteViewer();
        initializePaletteViewer();
    }

    /**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 
	public void createPartControl(Composite parent) {
		//Composite canvas = new Composite(parent, SWT.HORIZONTAL);
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT | SWT.BORDER);
	    toolbar.setBounds(0, 0, 200, 70);
	    new ToolItem(toolbar, SWT.SEPARATOR);
	    
	    GridLayout layout = new GridLayout(1, true);
		parent.setLayout(layout);
		
		Splitter splitter = new Splitter(parent, SWT.WRAP);
		createPaletteViewer(splitter);
		createGraphicalViewer(splitter);
		splitter.maintainSize(getPaletteViewer().getControl());
		splitter.setFixedSize(getInitialPaletteSize());
		splitter.addFixedSizeChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				handlePaletteResized(((Splitter)evt.getSource()).getFixedSize());
			}
		});
	}
*/
    public void createPartControl(Composite parent) {
        Splitter splitter = new Splitter(parent, SWT.CENTER);
        createPaletteViewer(splitter);
        Canvas canvas = new Canvas(splitter, SWT.CENTER);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        canvas.setSize(720, 560);
        canvas.setBounds(100, 100, 720, 560);
        canvas.setBackground(new Color(Display.getCurrent(), 200, 200, 200));
        createGraphicalViewer(canvas);
        splitter.maintainSize(getPaletteViewer().getControl());
        splitter.setFixedSize(getInitialPaletteSize());
        splitter.addFixedSizeChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                handlePaletteResized(((Splitter) evt.getSource()).getFixedSize());
            }
        });
    }
}
