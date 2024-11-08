package org.eclipse.rap.flexdraw2d.examples;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.examples.shapes.ShapesEditorContextMenuProvider;
import org.eclipse.gef.examples.shapes.ShapesEditorPaletteFactory;
import org.eclipse.gef.examples.shapes.model.EllipticalShape;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.ShapesDiagram;
import org.eclipse.gef.examples.shapes.parts.ShapesEditPartFactory;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.rap.flexdraw2d.examples.gef.GraphicalViewWithFlyoutPalette;

public class GEFExShapeView extends GraphicalViewWithFlyoutPalette {

    public GEFExShapeView() {
        System.err.println("** GEFExShapeView instantiated");
        diagram = new ShapesDiagram();
        Shape s1 = new EllipticalShape();
        s1.setLocation(new Point(10, 10));
        diagram.addChild(s1);
        Shape s2 = new RectangularShape();
        s2.setLocation(new Point(100, 100));
        diagram.addChild(s2);
        setEditDomain(new DefaultEditDomain(null));
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
    }

    @Override
    public void setFocus() {
    }

    public ShapesDiagram getModel() {
        return diagram;
    }

    private ShapesDiagram diagram = null;

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ShapesEditPartFactory());
        viewer.setRootEditPart(new FreeformGraphicalRootEditPart());
        ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
    }

    private static PaletteRoot PALETTE_MODEL;

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = ShapesEditorPaletteFactory.createPalette();
        return PALETTE_MODEL;
    }
}
