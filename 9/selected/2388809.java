package sbpme.designer.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jface.resource.ImageDescriptor;
import sbpme.designer.SbpmeDesignerPlugin;
import sbpme.designer.model.Activity;
import sbpme.designer.model.AutoActivity;
import sbpme.designer.model.EndActivity;
import sbpme.designer.model.ExternalActivity;
import sbpme.designer.model.ProcessDiagram;
import sbpme.designer.model.StartActivity;
import sbpme.designer.parts.EditorPartsFactory;

public class ProcessEditor extends GraphicalEditorWithFlyoutPalette {

    private ProcessDiagram processDiagram;

    public ProcessEditor() {
        processDiagram = new ProcessDiagram();
        DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
        setEditDomain(defaultEditDomain);
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalViewer().setRootEditPart(new ScalableRootEditPart());
        getGraphicalViewer().setEditPartFactory(new EditorPartsFactory());
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        PaletteRoot paletteRoot = new PaletteRoot();
        PaletteGroup controlGroup = new PaletteGroup("Control Group");
        ToolEntry tool = new SelectionToolEntry();
        controlGroup.add(tool);
        paletteRoot.setDefaultEntry(tool);
        tool = new MarqueeToolEntry();
        controlGroup.add(tool);
        PaletteSeparator separator = new PaletteSeparator("sbpme.designer.editor.seperator");
        separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
        controlGroup.add(separator);
        tool = new ConnectionCreationToolEntry("Connection Creation", "Creating connections", null, ImageDescriptor.createFromFile(SbpmeDesignerPlugin.class, "images/connection16.gif"), ImageDescriptor.createFromFile(Activity.class, "images/connection16.gif"));
        controlGroup.add(tool);
        PaletteDrawer drawer = new PaletteDrawer("Components", null);
        CombinedTemplateCreationEntry entry = new CombinedTemplateCreationEntry("Start", "Create a new Start Activity", StartActivity.class, new SimpleFactory(StartActivity.class), ImageDescriptor.createFromFile(SbpmeDesignerPlugin.class, "images/gear16.gif"), ImageDescriptor.createFromFile(StartActivity.class, "images/gear16.gif"));
        drawer.add(entry);
        entry = new CombinedTemplateCreationEntry("Auto Activity", "Create a new Auto Activity", AutoActivity.class, new SimpleFactory(AutoActivity.class), ImageDescriptor.createFromFile(SbpmeDesignerPlugin.class, "images/gear16.gif"), ImageDescriptor.createFromFile(AutoActivity.class, "images/gear16.gif"));
        drawer.add(entry);
        entry = new CombinedTemplateCreationEntry("External Activity", "Create a new External Activity", ExternalActivity.class, new SimpleFactory(ExternalActivity.class), ImageDescriptor.createFromFile(SbpmeDesignerPlugin.class, "images/gear16.gif"), ImageDescriptor.createFromFile(ExternalActivity.class, "images/gear16.gif"));
        drawer.add(entry);
        entry = new CombinedTemplateCreationEntry("End", "Create a new End Activity", EndActivity.class, new SimpleFactory(EndActivity.class), ImageDescriptor.createFromFile(SbpmeDesignerPlugin.class, "images/gear16.gif"), ImageDescriptor.createFromFile(EndActivity.class, "images/gear16.gif"));
        drawer.add(entry);
        paletteRoot.add(controlGroup);
        paletteRoot.add(drawer);
        return paletteRoot;
    }

    @Override
    protected void initializeGraphicalViewer() {
        getGraphicalViewer().setContents(processDiagram);
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
    }
}
