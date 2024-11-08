package fr.fous.ecore.custom;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.provider.EcoreItemProviderAdapterFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.emf.edit.ui.dnd.LocalTransfer;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.gmf.runtime.notation.util.NotationAdapterFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import fr.fous.ecore.custom.actions.CreateDiagramAction;
import fr.fous.ecore.part.EcoreDiagramEditor;

public class DiagramOutlinePage extends Page implements IAdaptable, IContentOutlinePage {

    public static DiagramOutlinePage DIAGRAM_OUTLINE;

    private TreeViewer modelViewer;

    private ListViewer diagramViewer;

    private EcoreDiagramEditor editor;

    private Composite top;

    private boolean isDispatching = true;

    private boolean consumeNextSelection = false;

    public DiagramOutlinePage(EcoreDiagramEditor editor) {
        this.editor = editor;
        DIAGRAM_OUTLINE = this;
    }

    public void createControl(Composite parent) {
        top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout());
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        ComposedAdapterFactory factories = new ComposedAdapterFactory();
        factories.addAdapterFactory(new EcoreItemProviderAdapterFactory());
        factories.addAdapterFactory(new NotationAdapterFactory());
        factories.addAdapterFactory(new ResourceItemProviderAdapterFactory());
        modelViewer = new TreeViewer(top, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        modelViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        modelViewer.setContentProvider(new AdapterFactoryContentProvider(factories) {

            public boolean hasChildren(Object object) {
                boolean result = super.hasChildren(object);
                if (object instanceof Diagram) {
                    result = false;
                }
                if (object instanceof EPackage && result == false) {
                    result = !DiagramUtil.getDiagrams((EPackage) object, editor.getDiagram().eResource()).isEmpty();
                }
                return result;
            }

            public Object[] getChildren(Object object) {
                Object[] result = super.getChildren(object);
                if (object instanceof EPackage) {
                    List<Diagram> list = DiagramUtil.getDiagrams((EPackage) object, editor.getDiagram().eResource());
                    if (list.size() != 0) {
                        Object[] newResult = new Object[result.length + list.size()];
                        for (int i = 0; i < result.length; i++) {
                            newResult[i] = result[i];
                        }
                        for (int i = 0; i < list.size(); i++) {
                            newResult[result.length + i] = list.get(i);
                        }
                        return newResult;
                    }
                }
                return result;
            }
        });
        modelViewer.setLabelProvider(new AdapterFactoryLabelProvider(factories) {

            public String getText(Object element) {
                String result = super.getText(element);
                if (element instanceof Diagram) {
                    if (editor.getDiagram() == element) {
                        result += " *";
                    }
                }
                return result;
            }

            public String getColumnText(Object object, int columnIndex) {
                String result = super.getText(object);
                if (object instanceof Diagram) {
                    if (editor.getDiagram() == object) {
                        result += " (active)";
                    }
                }
                return result;
            }
        });
        modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                setDiagramSelection(modelViewer.getSelection());
            }
        });
        modelViewer.addDragSupport(DND.DROP_COPY, new Transfer[] { LocalTransfer.getInstance() }, new TreeDragListener());
        modelViewer.addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                Object selectedObject = selection.getFirstElement();
                if (selectedObject instanceof Diagram) {
                    openDiagram((Diagram) selectedObject);
                }
            }
        });
        createContextMenuFor(modelViewer);
        editor.getDiagramGraphicalViewer().addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                selectionInDiagramChange(event);
            }
        });
        this.getSite().setSelectionProvider(modelViewer);
        setInput();
    }

    protected void setInput() {
        EPackage ePackage = getTopPackage();
        modelViewer.setInput(ePackage.eResource());
    }

    public void openDiagram(Diagram diagram) {
        URI uri = EcoreUtil.getURI(diagram);
        String editorName = uri.lastSegment() + "#" + diagram.getName();
        IEditorInput editorInput = new URIEditorInput(uri, editorName);
        if (editor.isDirty()) {
            editor.doSave(new NullProgressMonitor());
        }
        editor.setInput(editorInput);
        setInput();
    }

    private EPackage getTopPackage() {
        EPackage ePackage = (EPackage) editor.getDiagram().getElement();
        while (ePackage.eContainer() != null) {
            ePackage = (EPackage) ePackage.eContainer();
        }
        return ePackage;
    }

    protected void createContextMenuFor(StructuredViewer viewer) {
        MenuManager contextMenu = new MenuManager("#PopUp");
        contextMenu.add(new Separator("diagramAction"));
        contextMenu.setRemoveAllWhenShown(true);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu("id", contextMenu, viewer);
    }

    public TreeViewer getViewer() {
        return modelViewer;
    }

    public Object getAdapter(Class type) {
        return null;
    }

    @Override
    public Control getControl() {
        return top;
    }

    @Override
    public void setFocus() {
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        getViewer().addSelectionChangedListener(listener);
    }

    public ISelection getSelection() {
        return getViewer().getSelection();
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        getViewer().removeSelectionChangedListener(listener);
    }

    public void setSelection(ISelection selection) {
        getViewer().setSelection(selection);
    }

    private void selectionInDiagramChange(SelectionChangedEvent event) {
        if (consumeNextSelection) {
            consumeNextSelection = false;
        } else if (isDispatching) {
            if (event.getSelection() instanceof StructuredSelection) {
                StructuredSelection selection = (StructuredSelection) event.getSelection();
                List<EObject> treeSelection = new ArrayList<EObject>();
                for (Object obj : selection.toList()) {
                    if (obj instanceof GraphicalEditPart) {
                        GraphicalEditPart editPart = (GraphicalEditPart) obj;
                        if (editPart.getModel() instanceof View) {
                            View view = (View) editPart.getModel();
                            treeSelection.add(view.getElement());
                        }
                    }
                }
                if (treeSelection.size() > 0) {
                    isDispatching = false;
                    getViewer().setSelection(new StructuredSelection(treeSelection));
                    isDispatching = true;
                }
            }
        }
    }

    private void setDiagramSelection(ISelection selection) {
        if (isDispatching) {
            ArrayList<EditPart> result = new ArrayList<EditPart>();
            for (Object obj : ((StructuredSelection) selection).toList()) {
                EditPart part = searchEditPart((EObject) obj);
                if (part != null) result.add(part);
            }
            consumeNextSelection = true;
            editor.getDiagramGraphicalViewer().setSelection(new StructuredSelection(result));
            if (result.size() > 0) editor.getDiagramGraphicalViewer().reveal((EditPart) result.get(result.size() - 1));
        }
    }

    private EditPart searchEditPart(EObject obj) {
        for (Object object : editor.getDiagramGraphicalViewer().getEditPartRegistry().values()) {
            EditPart editPart = (EditPart) object;
            if (getEditPartElement(editPart) == obj) {
                while (getEditPartElement(editPart.getParent()) == getEditPartElement(editPart)) {
                    editPart = editPart.getParent();
                }
                return editPart;
            }
        }
        return null;
    }

    private EObject getEditPartElement(EditPart object) {
        if (object instanceof GraphicalEditPart) {
            GraphicalEditPart editPart = (GraphicalEditPart) object;
            if (editPart.getModel() instanceof View) {
                View view = (View) editPart.getModel();
                return view.getElement();
            }
        }
        return null;
    }

    public class TreeDragListener extends DragSourceAdapter {

        public void dragFinished(DragSourceEvent event) {
            if (!event.doit) return;
            LocalTransfer.getInstance().javaToNative(null, null);
        }

        public void dragSetData(DragSourceEvent event) {
            if (LocalTransfer.getInstance().isSupportedType(event.dataType)) {
                event.data = (ITreeSelection) modelViewer.getSelection();
            }
        }

        public void dragStart(DragSourceEvent event) {
            event.doit = true;
        }
    }

    public Resource getDiagramResource() {
        return editor.getDiagram().eResource();
    }

    public EcoreDiagramEditor getEditor() {
        return editor;
    }
}
