package org.eclipse.genforms.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.palette.CreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.genforms.editor.editparts.MyEditPartFactory;
import org.eclipse.genforms.editor.model.ButtonElement;
import org.eclipse.genforms.editor.model.EditElement;
import org.eclipse.genforms.editor.model.Form;
import org.eclipse.genforms.editor.model.LabelElement;
import org.eclipse.genforms.editor.model.PanelElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;

public class MyGraphicalEditor extends GraphicalEditorWithPalette {

    private Form form;

    private PaletteRoot paletteRoot;

    public MyGraphicalEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        form = new Form();
    }

    protected void initializeGraphicalViewer() {
        getGraphicalViewer().setEditPartFactory(new MyEditPartFactory(form));
        getGraphicalViewer().setContents(form);
        System.out.println(getGraphicalViewer());
        System.out.println(getPaletteViewer());
        System.out.println(getPaletteRoot());
        System.out.println(getInitialPaletteSize());
        System.out.println(getPaletteViewer().getControl());
        Composite parent = (Composite) getPaletteViewer().getControl();
        int i = 1;
        do {
            System.out.println("Parent No. " + i + " is a " + parent.getClass());
            System.out.println("***********************************************");
            System.out.println("Children : ");
            for (int j = 0; j < parent.getChildren().length; j++) {
                System.out.println(j + ". is a " + parent.getChildren()[j].getClass());
                System.out.println("... Size : " + parent.getChildren()[j].getBounds());
            }
            i++;
            parent = parent.getParent();
        } while (parent != null);
    }

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = new PaletteRoot();
            PaletteGroup paletteGroup = new PaletteGroup("Tools");
            paletteRoot.add(paletteGroup);
            SelectionToolEntry selectionToolEntry = new SelectionToolEntry("Auswahl");
            paletteGroup.add(selectionToolEntry);
            paletteGroup.add(new MarqueeToolEntry("Markieren"));
            paletteRoot.setDefaultEntry(selectionToolEntry);
            ImageDescriptor icon = GenformsPlugin.getImageDescriptor("src/icons/edit.gif");
            paletteGroup.add(new CreationToolEntry("Edit", "Edit", new CreationFactory() {

                public Object getNewObject() {
                    return new EditElement();
                }

                public Object getObjectType() {
                    return EditElement.class;
                }
            }, icon, icon));
            icon = GenformsPlugin.getImageDescriptor("src/icons/label.gif");
            paletteGroup.add(new CreationToolEntry("Label", "Label", new CreationFactory() {

                public Object getNewObject() {
                    return new LabelElement();
                }

                public Object getObjectType() {
                    return LabelElement.class;
                }
            }, icon, icon));
            icon = GenformsPlugin.getImageDescriptor("src/icons/label.gif");
            paletteGroup.add(new CreationToolEntry("Button", "Button", new CreationFactory() {

                public Object getNewObject() {
                    ButtonElement button = new ButtonElement();
                    button.setStyle("SWT.RADIO");
                    return button;
                }

                public Object getObjectType() {
                    return ButtonElement.class;
                }
            }, icon, icon));
            icon = GenformsPlugin.getImageDescriptor("src/icons/panel.gif");
            paletteGroup.add(new CreationToolEntry("Layout", "Layout", new CreationFactory() {

                public Object getNewObject() {
                    return new PanelElement();
                }

                public Object getObjectType() {
                    return PanelElement.class;
                }
            }, icon, icon));
        }
        return paletteRoot;
    }
}
