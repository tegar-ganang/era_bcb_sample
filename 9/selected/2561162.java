package abbot.swt.gef.util.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.examples.logicdesigner.LogicEditor;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.PaletteViewer;
import abbot.swt.gef.test.Util;
import abbot.swt.gef.util.GEFWorkbenchUtilities;
import abbot.swt.hierarchy.VisitableImpl;
import abbot.swt.hierarchy.Visitor;
import abbot.swt.junit.extensions.SWTTestCase;

public class WorkbenchUtilitiesTest extends SWTTestCase {

    public void testGetViewer() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        GraphicalViewer viewer = GEFWorkbenchUtilities.getViewer(editor);
        DefaultEditDomain domain = (DefaultEditDomain) viewer.getEditDomain();
        assertSame(editor, domain.getEditorPart());
    }

    public void testGetRootEditPart() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        RootEditPart root = GEFWorkbenchUtilities.getRootEditPart(editor);
        GraphicalViewer viewer = (GraphicalViewer) root.getViewer();
        DefaultEditDomain domain = (DefaultEditDomain) viewer.getEditDomain();
        assertSame(editor, domain.getEditorPart());
    }

    public void testGetRootFigure() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        IFigure figure = GEFWorkbenchUtilities.getRootFigure(editor);
        RootEditPart rootEditPart = GEFWorkbenchUtilities.getRootEditPart(editor);
        IFigure rootFigure = ((GraphicalEditPart) rootEditPart).getFigure();
        assertSame(rootFigure, figure);
    }

    public void testGetPaletteViewer() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        PaletteViewer viewer = GEFWorkbenchUtilities.getPaletteViewer(editor);
        RootEditPart rootPart = viewer.getRootEditPart();
        assertNotNull(findEntry(rootPart, "LED"));
    }

    public void testGetPaletteRootEditPart() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        RootEditPart rootPart = GEFWorkbenchUtilities.getPaletteRootEditPart(editor);
        assertNotNull(findEntry(rootPart, "LED"));
    }

    private PaletteEntry findEntry(EditPart editPart, final String label) {
        final PaletteEntry[] entryWrapper = new PaletteEntry[1];
        new VisitableImpl<EditPart>() {

            public Collection<EditPart> getChildren(EditPart editPart) {
                return editPart.getChildren();
            }
        }.accept(editPart, new Visitor<EditPart>() {

            public Result visit(EditPart editPart) {
                PaletteEntry entry = (PaletteEntry) editPart.getModel();
                if (entry != null && label.equals(entry.getLabel())) {
                    entryWrapper[0] = entry;
                    return Result.stop;
                }
                return Result.ok;
            }
        });
        return entryWrapper[0];
    }

    public void testGetPaletteRootFigure() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        IFigure rootFigure = GEFWorkbenchUtilities.getPaletteRootFigure(editor);
        GraphicalViewer viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
        Map map = viewer.getEditDomain().getPaletteViewer().getVisualPartMap();
        EditPart rootPart = (EditPart) map.get(rootFigure);
        assertNotNull(findEntry(rootPart, "LED"));
    }

    public void testGetEditDomain() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        EditDomain editDomain = GEFWorkbenchUtilities.getEditDomain(editor);
        GraphicalViewer viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
        assertSame(viewer.getEditDomain(), editDomain);
    }

    public void testGetPaletteRoot() {
        Util.activateLogicEditor();
        LogicEditor editor = (LogicEditor) Util.getLogicEditor();
        PaletteRoot root = GEFWorkbenchUtilities.getPaletteRoot(editor);
        assertNotNull(findEntry(root, "LED"));
    }

    private PaletteEntry findEntry(PaletteEntry entry, final String label) {
        final PaletteEntry[] entryWrapper = new PaletteEntry[1];
        new VisitableImpl<PaletteEntry>() {

            public Collection<PaletteEntry> getChildren(PaletteEntry entry) {
                if (entry instanceof PaletteContainer) return ((PaletteContainer) entry).getChildren();
                return Collections.emptyList();
            }
        }.accept(entry, new Visitor<PaletteEntry>() {

            public Result visit(PaletteEntry entry) {
                if (label.equals(entry.getLabel())) {
                    entryWrapper[0] = entry;
                    return Result.stop;
                }
                return Result.ok;
            }
        });
        return entryWrapper[0];
    }
}
