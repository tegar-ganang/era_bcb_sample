package com.metanology.mde.ui.pimEditor.diagrams;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Point;
import com.metanology.mde.ui.pimEditor.model.*;
import com.metanology.mde.ui.pimExplorer.PimContentProvider;
import com.metanology.mde.core.ui.plugin.MDEPlugin;
import com.metanology.mde.utils.Messages;
import com.metanology.mde.core.metaModel.Component;
import com.metanology.mde.core.metaModel.ComponentCollection;
import com.metanology.mde.core.metaModel.MetaModelCache;
import com.metanology.mde.core.metaModel.Subsystem;
import com.metanology.mde.core.metaModel.SubsystemCollection;
import com.metanology.mde.core.ui.common.MDEPluginImages;
import com.metanology.mde.ui.pimEditor.edit.*;

/**
 * Editor for the class diagram
 * 
 * @since 2.0
 * @author wwang
 */
public class ComponentDiagramEditor extends AbstractDiagramEditor {

    public static final String ID = "com.metanology.mde.ui.pimEditor.diagrams.ComponentDiagramEditor";

    private PaletteRoot root;

    /**
     * Constructor for ClassDiagramEditor.
     */
    public ComponentDiagramEditor() {
        super();
    }

    public void handleDropTargetEvent(IResource[] data, DropTargetEvent evt) {
        for (int i = 0; i < data.length; i++) {
            IResource r = data[i];
            if (PimContentProvider.isComponent(r)) {
                Component cmp = PimContentProvider.getComponent((IFile) r);
                handleDropComponent(evt, cmp);
            } else if (!(r instanceof IFile) && PimContentProvider.isSubsystem(r)) {
                Subsystem s = PimContentProvider.getSubsystem((IContainer) r);
                handleDropSubsystem(evt, s);
            }
        }
    }

    protected String getEditorTitle(IFile file) {
        String name = file.getName();
        String ext = file.getFileExtension();
        return file.getParent().getName() + "/" + name.substring(0, name.length() - ext.length() - 1) + Messages.MSG_COMPONENT_DIAGRAM;
    }

    private void handleDropComponent(DropTargetEvent evt, Component mcmp) {
        PIMDiagram diagram = getPIMDiagram();
        if (mcmp != null) {
            ComponentNode cmpNode = new ComponentNode();
            cmpNode.init(mcmp);
            Point loc = this.getGraphicalViewer().getControl().toControl(new Point(evt.x, evt.y));
            cmpNode.setLocation(loc.x, loc.y);
            cmpNode.setSize(0, 0);
            AddCommand addCmd = new AddCommand();
            addCmd.setChild(cmpNode);
            addCmd.setParent(diagram);
            this.getCommandStack().execute(addCmd);
            Command cmd = PIMEditPolicy.createRefreshRelationCommand(cmpNode, diagram);
            if (cmd != null && cmd.canExecute()) {
                this.getCommandStack().execute(cmd);
            }
        }
    }

    private void handleDropSubsystem(DropTargetEvent evt, Subsystem msub) {
        PIMDiagram diagram = getPIMDiagram();
        if (msub != null) {
            SubsystemNode subNode = new SubsystemNode();
            subNode.init(msub);
            Point loc = this.getGraphicalViewer().getControl().toControl(new Point(evt.x, evt.y));
            subNode.setLocation(loc.x, loc.y);
            Hashtable subsInDiagram = new Hashtable();
            for (Iterator i = diagram.getChildren().iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o instanceof SubsystemNode) {
                    SubsystemNode node = (SubsystemNode) o;
                    Subsystem sub = node.getMsubsystem();
                    if (sub != null) subsInDiagram.put(sub, node);
                    if (msub == sub) {
                        evt.detail = DND.DROP_NONE;
                        return;
                    }
                }
            }
            subNode.setSize(0, 0);
            AddCommand cmd = new AddCommand();
            cmd.setChild(subNode);
            cmd.setParent(diagram);
            this.getCommandStack().execute(cmd);
        }
    }

    /**
     * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
     */
    protected PaletteRoot getPaletteRoot() {
        return createPalette();
    }

    private static List createCategories(PaletteRoot root) {
        List categories = new ArrayList();
        categories.add(createControlGroup(root));
        categories.add(createComponentNodesDrawer());
        return categories;
    }

    private static PaletteContainer createComponentNodesDrawer() {
        PaletteContainer drawer = new PaletteGroup(Messages.UI_PIMDiagramEditor_palette_componentNodesGroup_Label);
        drawer.setUserModificationPermission(PaletteDrawer.PERMISSION_NO_MODIFICATION);
        List entries = new ArrayList();
        addNodesToolEntries(entries);
        PaletteSeparator sep = new PaletteSeparator("com.metanology.mde.ui.pimEditor.diagrams.ComponentDiagram.sep1");
        sep.setUserModificationPermission(PaletteSeparator.PERMISSION_NO_MODIFICATION);
        entries.add(sep);
        addRelationsEntries(entries);
        drawer.addAll(entries);
        return drawer;
    }

    private static void addNodesToolEntries(List entries) {
        CombinedTemplateCreationEntry tool = null;
        tool = new CombinedTemplateCreationEntry(Messages.UI_PIMDiagramEditor_tool_Component, Messages.UI_PIMDiagramEditor_tool_Component_tooltip, TemplateConstants.TEMPLATE_COMPONENT, new SimpleFactory(ComponentNode.class), MDEPluginImages.DESC_PIM_EDT_COMPONENT, MDEPluginImages.DESC_PIM_EDT_COMPONENT24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new CombinedTemplateCreationEntry(Messages.UI_PIMDiagramEditor_tool_Subsystem, Messages.UI_PIMDiagramEditor_tool_Subsystem_tooltip, TemplateConstants.TEMPLATE_SUBSYSTEM, new SimpleFactory(SubsystemNode.class), MDEPluginImages.DESC_PIM_EDT_SUBSYSTEM, MDEPluginImages.DESC_PIM_EDT_SUBSYSTEM24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new CombinedTemplateCreationEntry(Messages.UI_PIMDiagramEditor_tool_Note, Messages.UI_PIMDiagramEditor_tool_Note_tooltip, TemplateConstants.TEMPLATE_NOTE, new SimpleFactory(NoteNode.class), MDEPluginImages.DESC_PIM_EDT_NOTE, MDEPluginImages.DESC_PIM_EDT_NOTE24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
    }

    private static void addRelationsEntries(List entries) {
        PIMRelationCreationToolEntry tool = null;
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_Dependency, Messages.UI_PIMDiagramEditor_tool_Dependency_tooltip, TemplateConstants.TEMPLATE_COMPONENT_DEPENDENCY, MDEPluginImages.DESC_PIM_EDT_DEPENDENCY, MDEPluginImages.DESC_PIM_EDT_DEPENDENCY24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_AnchorNote, Messages.UI_PIMDiagramEditor_tool_AnchorNote_tooltip, TemplateConstants.TEMPLATE_ANCHOR_NOTE, MDEPluginImages.DESC_PIM_EDT_ANCHORNOTE, MDEPluginImages.DESC_PIM_EDT_ANCHORNOTE24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
    }

    private static PaletteContainer createControlGroup(PaletteRoot root) {
        PaletteGroup controlGroup = new PaletteGroup(Messages.UI_PIMDiagramEditor_ControlGroup_Label);
        List entries = new ArrayList();
        ToolEntry tool = new SelectionToolEntry();
        entries.add(tool);
        root.setDefaultEntry(tool);
        controlGroup.addAll(entries);
        return controlGroup;
    }

    private static PaletteRoot createPalette() {
        PaletteRoot palette = new PaletteRoot();
        palette.addAll(createCategories(palette));
        return palette;
    }

    public static Subsystem getCurrentParentSubsystem(PIMDiagram diagram) {
        if (diagram == null) return null;
        String parentPath = diagram.getParentPath();
        MetaModelCache cache = MDEPlugin.getDefault().getRuntime().getModelCache();
        if (cache != null) {
            return (Subsystem) cache.getSubsystemByPath(parentPath);
        }
        return null;
    }

    public static String getUniqueComponentName(String name, PIMDiagram diagram) {
        Subsystem sub = getCurrentParentSubsystem(diagram);
        MetaModelCache cache = MDEPlugin.getDefault().getRuntime().getModelCache();
        if (cache == null) return name;
        String newName = name;
        int i = 1;
        ComponentCollection mcmps = cache.getComponents(sub);
        boolean hasName = true;
        while (hasName) {
            hasName = false;
            for (Enumeration e = mcmps.elements(); e.hasMoreElements(); ) {
                Component mcmp = (Component) e.nextElement();
                if (mcmp.getName().equalsIgnoreCase(newName)) {
                    newName = name + (i++);
                    hasName = true;
                    break;
                }
            }
        }
        return newName;
    }

    public static String getUniqueSubsystemName(String name, PIMDiagram diagram) {
        Subsystem sub = getCurrentParentSubsystem(diagram);
        MetaModelCache cache = MDEPlugin.getDefault().getRuntime().getModelCache();
        if (cache == null) return name;
        String newName = name;
        int i = 1;
        SubsystemCollection msubs = cache.getSubsystems(sub);
        boolean hasName = true;
        while (hasName) {
            hasName = false;
            for (Enumeration e = msubs.elements(); e.hasMoreElements(); ) {
                Subsystem msub = (Subsystem) e.nextElement();
                if (msub.getName().equalsIgnoreCase(newName)) {
                    newName = name + (i++);
                    hasName = true;
                    break;
                }
            }
        }
        return newName;
    }
}
