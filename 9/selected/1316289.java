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
import com.metanology.mde.core.metaModel.MetaClass;
import com.metanology.mde.core.metaModel.MetaClassCollection;
import com.metanology.mde.core.metaModel.MetaModelCache;
import com.metanology.mde.core.metaModel.Package;
import com.metanology.mde.core.metaModel.PackageCollection;
import com.metanology.mde.core.ui.common.MDEPluginImages;
import com.metanology.mde.ui.pimEditor.edit.*;

/**
 * Editor for the class diagram
 * 
 * @since 2.0
 * @author wwang
 */
public class ClassDiagramEditor extends AbstractDiagramEditor {

    public static final String ID = "com.metanology.mde.ui.pimEditor.diagrams.ClassDiagramEditor";

    private PaletteRoot root;

    /**
     * Constructor for ClassDiagramEditor.
     */
    public ClassDiagramEditor() {
        super();
    }

    public void handleDropTargetEvent(IResource[] data, DropTargetEvent evt) {
        for (int i = 0; i < data.length; i++) {
            IResource r = data[i];
            if (PimContentProvider.isMetaClass(r)) {
                MetaClass mcls = PimContentProvider.getMetaClass((IFile) r);
                handleDropMetaClass(evt, mcls);
            } else if (!(r instanceof IFile) && PimContentProvider.isPackage(r)) {
                Package p = PimContentProvider.getPackage((IContainer) r);
                handleDropPackage(evt, p);
            }
        }
    }

    protected String getEditorTitle(IFile file) {
        String name = file.getName();
        String ext = file.getFileExtension();
        String title = file.getParent().getName() + "/" + name.substring(0, name.length() - ext.length() - 1) + Messages.MSG_CLASS_DIAGRAM;
        return title;
    }

    private void handleDropMetaClass(DropTargetEvent evt, MetaClass mclass) {
        PIMDiagram diagram = getPIMDiagram();
        if (mclass != null) {
            ClassNode clsNode = new ClassNode();
            clsNode.init(mclass);
            Point loc = this.getGraphicalViewer().getControl().toControl(new Point(evt.x, evt.y));
            clsNode.setLocation(loc.x, loc.y);
            clsNode.setSize(0, 0);
            AddCommand addcmd = new AddCommand();
            addcmd.setChild(clsNode);
            addcmd.setParent(diagram);
            this.getCommandStack().execute(addcmd);
            Command cmd = PIMEditPolicy.createRefreshRelationCommand(clsNode, diagram);
            if (cmd != null && cmd.canExecute()) {
                this.getCommandStack().execute(cmd);
            }
        }
    }

    private void handleDropPackage(DropTargetEvent evt, Package mpkg) {
        PIMDiagram diagram = getPIMDiagram();
        if (mpkg != null) {
            PackageNode pkgNode = new PackageNode();
            pkgNode.init(mpkg);
            Point loc = this.getGraphicalViewer().getControl().toControl(new Point(evt.x, evt.y));
            pkgNode.setLocation(loc.x, loc.y);
            Hashtable pkgsInDiagram = new Hashtable();
            for (Iterator i = diagram.getChildren().iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o instanceof PackageNode) {
                    PackageNode node = (PackageNode) o;
                    com.metanology.mde.core.metaModel.Package pkg = node.getMpackage();
                    if (pkg != null) pkgsInDiagram.put(pkg, node);
                    if (mpkg == pkg) {
                        evt.detail = DND.DROP_NONE;
                        return;
                    }
                }
            }
            pkgNode.setSize(0, 0);
            AddCommand command = new AddCommand();
            command.setChild(pkgNode);
            command.setParent(diagram);
            this.getCommandStack().execute(command);
        }
    }

    /**
     * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
     */
    protected PaletteRoot getPaletteRoot() {
        if (root == null) {
            root = createPalette();
        }
        return root;
    }

    private List createCategories(PaletteRoot root) {
        List categories = new ArrayList();
        categories.add(createControlGroup(root));
        categories.add(createNodesDrawer());
        return categories;
    }

    private PaletteContainer createNodesDrawer() {
        PaletteContainer drawer = new PaletteGroup(Messages.UI_PIMDiagramEditor_palette_classNodesGroup_Label);
        drawer.setUserModificationPermission(PaletteDrawer.PERMISSION_NO_MODIFICATION);
        List entries = new ArrayList();
        addNodesToolEntries(entries);
        PaletteSeparator sep = new PaletteSeparator("com.metanology.mde.ui.pimEditor.diagrams.ClassDiagram.sep1");
        sep.setUserModificationPermission(PaletteSeparator.PERMISSION_NO_MODIFICATION);
        entries.add(sep);
        addRelationsToolEntries(entries);
        drawer.addAll(entries);
        return drawer;
    }

    protected void addNodesToolEntries(List entries) {
        CombinedTemplateCreationEntry tool = null;
        tool = new CombinedTemplateCreationEntry(Messages.UI_PIMDiagramEditor_tool_Class, Messages.UI_PIMDiagramEditor_tool_Class_tooltip, TemplateConstants.TEMPLATE_CLASS, new SimpleFactory(ClassNode.class), MDEPluginImages.DESC_PIM_EDT_CLASS, MDEPluginImages.DESC_PIM_EDT_CLASS24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new CombinedTemplateCreationEntry(Messages.UI_PIMDiagramEditor_tool_Package, Messages.UI_PIMDiagramEditor_tool_Package_tooltip, TemplateConstants.TEMPLATE_PACKAGE, new SimpleFactory(PackageNode.class), MDEPluginImages.DESC_PIM_EDT_PACKAGE, MDEPluginImages.DESC_PIM_EDT_PACKAGE24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new CombinedTemplateCreationEntry(Messages.UI_PIMDiagramEditor_tool_Note, Messages.UI_PIMDiagramEditor_tool_Note_tooltip, TemplateConstants.TEMPLATE_NOTE, new SimpleFactory(NoteNode.class), MDEPluginImages.DESC_PIM_EDT_NOTE, MDEPluginImages.DESC_PIM_EDT_NOTE24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
    }

    protected void addRelationsToolEntries(List entries) {
        ToolEntry tool = null;
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_Generalization, Messages.UI_PIMDiagramEditor_tool_Generalization_tooltip, TemplateConstants.TEMPLATE_GENERALIZATION, MDEPluginImages.DESC_PIM_EDT_GENERALIZATION, MDEPluginImages.DESC_PIM_EDT_GENERALIZATION24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_Realization, Messages.UI_PIMDiagramEditor_tool_Realization_tooltip, TemplateConstants.TEMPLATE_REALIZATION, MDEPluginImages.DESC_PIM_EDT_REALIZATION, MDEPluginImages.DESC_PIM_EDT_REALIZATION24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_Association, Messages.UI_PIMDiagramEditor_tool_Association_tooltip, TemplateConstants.TEMPLATE_ASSOCIATION, MDEPluginImages.DESC_PIM_EDT_ASSOCIATION, MDEPluginImages.DESC_PIM_EDT_ASSOCIATION24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_Aggregation, Messages.UI_PIMDiagramEditor_tool_Aggregation_tooltip, TemplateConstants.TEMPLATE_AGGREGATION, MDEPluginImages.DESC_PIM_EDT_AGGREGATION, MDEPluginImages.DESC_PIM_EDT_AGGREGATION24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_UnidirectionalAssociation, Messages.UI_PIMDiagramEditor_tool_UnidirectionalAssociation_tooltip, TemplateConstants.TEMPLATE_UNIDIRECTIONAL_ASSOCIATION, MDEPluginImages.DESC_PIM_EDT_UNIDIRECTIONAL_ASSOC, MDEPluginImages.DESC_PIM_EDT_UNIDIRECTIONAL_ASSOC24);
        tool.setUserModificationPermission(PaletteEntry.PERMISSION_HIDE_ONLY);
        entries.add(tool);
        tool = new PIMRelationCreationToolEntry(Messages.UI_PIMDiagramEditor_tool_AssociationClass, Messages.UI_PIMDiagramEditor_tool_AssociationClass_tooltip, TemplateConstants.TEMPLATE_ASSOCIATION_CLASS, MDEPluginImages.DESC_PIM_EDT_ASSOCIATIONCLASS, MDEPluginImages.DESC_PIM_EDT_ASSOCIATIONCLASS24);
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

    private PaletteRoot createPalette() {
        PaletteRoot palette = new PaletteRoot();
        palette.addAll(createCategories(palette));
        return palette;
    }

    public static Package getCurrentParentPackage(PIMDiagram diagram) {
        if (diagram == null) return null;
        String parentPath = diagram.getParentPath();
        if (parentPath != null) {
            MetaModelCache cache = MDEPlugin.getDefault().getRuntime().getModelCache();
            if (cache != null) {
                return (Package) cache.getPackageByPath(parentPath);
            }
        }
        return null;
    }

    public static String getUniqueClassName(String name, PIMDiagram diagram) {
        Package pkg = getCurrentParentPackage(diagram);
        MetaModelCache cache = MDEPlugin.getDefault().getRuntime().getModelCache();
        if (cache == null) return name;
        String newName = name;
        int i = 1;
        MetaClassCollection mclss = cache.getMetaClasses(pkg);
        boolean hasName = true;
        while (hasName) {
            hasName = false;
            for (Enumeration e = mclss.elements(); e.hasMoreElements(); ) {
                MetaClass mcls = (MetaClass) e.nextElement();
                if (mcls.getName().equalsIgnoreCase(newName)) {
                    newName = name + (i++);
                    hasName = true;
                    break;
                }
            }
        }
        return newName;
    }

    public static String getUniquePackageName(String name, PIMDiagram diagram) {
        Package pkg = getCurrentParentPackage(diagram);
        MetaModelCache cache = MDEPlugin.getDefault().getRuntime().getModelCache();
        if (cache == null) return name;
        String newName = name;
        int i = 1;
        PackageCollection mpkgs = cache.getPackages(pkg);
        boolean hasName = true;
        while (hasName) {
            hasName = false;
            for (Enumeration e = mpkgs.elements(); e.hasMoreElements(); ) {
                Package mpkg = (Package) e.nextElement();
                if (mpkg.getName().equalsIgnoreCase(newName)) {
                    newName = name + (i++);
                    hasName = true;
                    break;
                }
            }
        }
        return newName;
    }
}
