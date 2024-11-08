package net.java.amateras.uml.activitydiagram;

import net.java.amateras.uml.DiagramEditor;
import net.java.amateras.uml.UMLPlugin;
import net.java.amateras.uml.activitydiagram.action.CopyAction;
import net.java.amateras.uml.activitydiagram.action.PasteAction;
import net.java.amateras.uml.activitydiagram.editpart.ActivityEditPartFactory;
import net.java.amateras.uml.activitydiagram.model.ActionModel;
import net.java.amateras.uml.activitydiagram.model.ActivityModel;
import net.java.amateras.uml.activitydiagram.model.DecisionModel;
import net.java.amateras.uml.activitydiagram.model.FinalStateModel;
import net.java.amateras.uml.activitydiagram.model.FlowModel;
import net.java.amateras.uml.activitydiagram.model.ForkNodeModel;
import net.java.amateras.uml.activitydiagram.model.InitialStateModel;
import net.java.amateras.uml.activitydiagram.model.JoinNodeModel;
import net.java.amateras.uml.activitydiagram.model.ObjectModel;
import net.java.amateras.uml.activitydiagram.model.VerticalPartitionModel;
import net.java.amateras.uml.classdiagram.figure.UMLClassFigure;
import net.java.amateras.uml.model.AnchorModel;
import net.java.amateras.uml.model.NoteModel;
import net.java.amateras.uml.model.RootModel;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;

/**
 * 
 * @author Takahiro Shida
 * @author Naoki Takezoe
 */
public class ActivityDiagramEditor extends DiagramEditor {

    private CopyAction copyAction;

    private PasteAction pasteAction;

    protected RootModel createInitializeModel() {
        ActivityModel model = new ActivityModel();
        model.setShowIcon(true);
        model.setBackgroundColor(UMLClassFigure.classColor.getRGB());
        model.setForegroundColor(ColorConstants.black.getRGB());
        return model;
    }

    protected String getDiagramType() {
        return "activity";
    }

    protected EditPartFactory createEditPartFactory() {
        return new ActivityEditPartFactory();
    }

    protected void createDiagramAction(GraphicalViewer viewer) {
    }

    protected void fillDiagramPopupMenu(MenuManager manager) {
        manager.add(new Separator("copy"));
        manager.add(copyAction);
        manager.add(pasteAction);
    }

    protected void updateDiagramAction(ISelection selection) {
    }

    protected void createActions() {
        super.createActions();
        pasteAction = new PasteAction(this);
        getActionRegistry().registerAction(pasteAction);
        getSelectionActions().add(pasteAction.getId());
        copyAction = new CopyAction(this, pasteAction);
        getActionRegistry().registerAction(copyAction);
        getSelectionActions().add(copyAction.getId());
    }

    protected PaletteRoot getPaletteRoot() {
        PaletteRoot root = new PaletteRoot();
        UMLPlugin plugin = UMLPlugin.getDefault();
        PaletteGroup tools = new PaletteGroup(plugin.getResourceString("palette.tool"));
        ToolEntry tool = new SelectionToolEntry();
        tools.add(tool);
        root.setDefaultEntry(tool);
        tool = new MarqueeToolEntry();
        tools.add(tool);
        PaletteDrawer common = new PaletteDrawer(plugin.getResourceString("palette.common"));
        common.add(createEntityEntry(plugin.getResourceString("palette.common.note"), NoteModel.class, "icons/note.gif"));
        common.add(createConnectionEntry(plugin.getResourceString("palette.common.anchor"), AnchorModel.class, "icons/anchor.gif"));
        PaletteDrawer entities = new PaletteDrawer(plugin.getResourceString("palette.entity"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.action"), ActionModel.class, "icons/usecase.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.object"), ObjectModel.class, "icons/icon_object.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.initialState"), InitialStateModel.class, "icons/icon_init_state.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.finalState"), FinalStateModel.class, "icons/icon_final_state.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.decision"), DecisionModel.class, "icons/icon_decision.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.fork"), ForkNodeModel.class, "icons/icon_fork.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.join"), JoinNodeModel.class, "icons/icon_join.gif"));
        entities.add(createEntityEntry(plugin.getResourceString("palette.activity.v_partition"), VerticalPartitionModel.class, "icons/partition_v.gif"));
        PaletteDrawer relations = new PaletteDrawer(plugin.getResourceString("palette.relation"));
        relations.add(createConnectionEntry(plugin.getResourceString("palette.activity.flow"), FlowModel.class, "icons/flow.gif"));
        root.add(tools);
        root.add(common);
        root.add(entities);
        root.add(relations);
        return root;
    }
}
