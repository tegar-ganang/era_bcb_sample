package net.sourceforge.coffea.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sourceforge.coffea.actors.abilities.IModelBuilding;
import net.sourceforge.coffea.editors.policies.OpenUMLClassDiagramJavaEditPolicy;
import net.sourceforge.coffea.editors.policies.OpenUMLClassJavaEditPolicy;
import net.sourceforge.coffea.handlers.ReverseHandler;
import net.sourceforge.coffea.tools.ClassHandler;
import net.sourceforge.coffea.tools.PackageHandler;
import net.sourceforge.coffea.tools.capacities.IAttributeHandling;
import net.sourceforge.coffea.tools.capacities.IClassHandling;
import net.sourceforge.coffea.tools.capacities.IClassifierHandling;
import net.sourceforge.coffea.tools.capacities.IContainableElementHandling;
import net.sourceforge.coffea.tools.capacities.IElementHandling;
import net.sourceforge.coffea.tools.capacities.IMemberHandling;
import net.sourceforge.coffea.tools.capacities.IMethodHandling;
import net.sourceforge.coffea.tools.capacities.IModelHandling;
import net.sourceforge.coffea.tools.capacities.IPackageHandling;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.transaction.ResourceSetListener;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.impl.DiagramImpl;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.uml2.diagram.clazz.edit.parts.Class2EditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.Class3EditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.Class4EditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.ClassEditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.Package2EditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.Package4EditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.Package6EditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.PackageAsFrameEditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.PackageEditPart;
import org.eclipse.uml2.diagram.clazz.edit.parts.UMLEditPartFactory;
import org.eclipse.uml2.diagram.clazz.part.UMLDiagramEditor;
import org.eclipse.uml2.diagram.clazz.part.UMLDiagramEditorPlugin;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

/** 
 * Class diagram editor synchronizing Java code on structural UML elements 
 * edition 
 */
public class UMLClassDiagramJavaEditor extends UMLDiagramEditor implements ISelectionListener {

    /** Editor identifier */
    public static final String ID = "net.sourceforge.coffea.editors.UMLClassDiagramJavaEditor";

    /** Worker linking the UML model and the Java source */
    protected IModelBuilding worker;

    /** 
	 * Boolean value indicating if the editor has been correctly 
	 * initialized
	 */
    protected boolean initialized;

    /** 
	 * Table of listeners listening to diagram elements changes, the key for 
	 * each element is its name
	 */
    protected Map<String, ResourceSetListener> listeners;

    /**
	 * Table of UML elements edit policies. Each element having only one edit 
	 * policy, the key for this policy is the element itself
	 */
    protected Map<EditPart, EditPolicy> editPolicies;

    /** EMF editing domain */
    protected TransactionalEditingDomain editingDomain;

    /** UML edit part factory */
    protected EditPartFactory factory;

    /** UML elements modifications listeners initialization */
    protected void initListeners() {
        EditPart p = getDiagramEditPart();
        if ((p != null) && (p instanceof IGraphicalEditPart)) {
            IGraphicalEditPart g = (IGraphicalEditPart) p;
            editingDomain = g.getEditingDomain();
            Diagram d = null;
            Object m = g.getModel();
            String name = null;
            if ((m != null) && (m instanceof Diagram)) {
                d = (Diagram) m;
                EObject eo = d.getElement();
                if (eo instanceof Package) {
                    name = PackageHandler.resolveFullyQualifiedName((Package) eo);
                } else if (eo instanceof Class) {
                    Class cl = (Class) eo;
                    name = ClassHandler.resolveFullyQualifiedName(cl);
                }
                if (name != null) {
                    TransactionalEditingDomain domain = g.getEditingDomain();
                    if (listeners.get(name) == null) {
                        IElementHandling elH = findHandler(g);
                        if (elH != null) {
                            domain.addResourceSetListener(elH);
                            g.addEditPartListener(elH);
                            listeners.put(name, elH);
                        }
                    } else {
                        domain = null;
                    }
                    listenToChildren(g, name);
                }
            }
        }
    }

    /**
	 * Returns an element handler from the model provided by {@link #worker}
	 * @param name
	 * Name of the element to return the handler for
	 * @return Element handler responding to the given element name
	 */
    protected IElementHandling getElementHandler(String name) {
        IModelHandling mdl = null;
        IElementHandling rt = null;
        if (worker != null) {
            mdl = worker.getModelHandler();
            if (mdl != null) {
                rt = mdl.getElementHandler(name);
            }
        }
        return rt;
    }

    /**
	 * Ensures the correct edit policy has been installed on an edit part
	 * @param p
	 * Edit part on which the policy installation must be ensured
	 * @param elH
	 * Element handler for which the installation could be done
	 */
    protected void ensureEditPolicy(EditPart p, IElementHandling elH) {
        if ((editPolicies != null) && (elH != null)) {
            EditPolicy policy = editPolicies.get(elH.getUMLElement());
            if (policy == null) {
                if (elH instanceof IPackageHandling) {
                    if (p instanceof PackageEditPart) {
                        PackageEditPart packPart = (PackageEditPart) p;
                        packPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassDiagramJavaEditPolicy();
                        packPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof Package2EditPart) {
                        Package2EditPart packPart = (Package2EditPart) p;
                        packPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassDiagramJavaEditPolicy();
                        packPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof Package4EditPart) {
                        Package4EditPart packPart = (Package4EditPart) p;
                        packPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassDiagramJavaEditPolicy();
                        packPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof Package6EditPart) {
                        Package6EditPart packPart = (Package6EditPart) p;
                        packPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassDiagramJavaEditPolicy();
                        packPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof PackageAsFrameEditPart) {
                        PackageAsFrameEditPart packPart = (PackageAsFrameEditPart) p;
                        packPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassDiagramJavaEditPolicy();
                        packPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    }
                }
                if (elH instanceof IClassHandling<?, ?>) {
                    IClassHandling<?, ?> clH = (IClassHandling<?, ?>) elH;
                    if (p instanceof ClassEditPart) {
                        ClassEditPart classPart = (ClassEditPart) p;
                        classPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassJavaEditPolicy(clH);
                        classPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof Class2EditPart) {
                        Class2EditPart classPart = (Class2EditPart) p;
                        classPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassJavaEditPolicy(clH);
                        classPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof Class3EditPart) {
                        Class3EditPart classPart = (Class3EditPart) p;
                        classPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassJavaEditPolicy(clH);
                        classPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    } else if (p instanceof Class4EditPart) {
                        Class4EditPart classPart = (Class4EditPart) p;
                        classPart.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
                        policy = new OpenUMLClassJavaEditPolicy(clH);
                        classPart.installEditPolicy(EditPolicyRoles.OPEN_ROLE, policy);
                    }
                }
                if (policy != null) {
                    editPolicies.put(p, policy);
                }
            }
        }
    }

    /**
	 * Listens to and edit part children resources changes
	 * @param p
	 * Parent edit part which children must be listened to
	 * @param name
	 * Parent edit part resource name
	 */
    protected void listenToChildren(EditPart p, String name) {
        List<?> children = p.getChildren();
        if (children != null) {
            Object child = null;
            EditPart childEditPart = null;
            String childName = null;
            NamedElement childElement = null;
            Node childModel = null;
            IElementHandling childH = null;
            for (int i = 0; i < children.size(); i++) {
                child = children.get(i);
                if ((child != null) && (child instanceof EditPart)) {
                    childEditPart = (EditPart) child;
                    if (childEditPart.getModel() instanceof Node) {
                        childModel = (Node) childEditPart.getModel();
                        if (childModel.getElement() instanceof NamedElement) {
                            childElement = (NamedElement) childModel.getElement();
                            childName = name + '.' + childElement.getName();
                            childH = getElementHandler(childName);
                            if ((childEditPart instanceof IGraphicalEditPart) && (childH != null)) {
                                IGraphicalEditPart graphicalPart = ((IGraphicalEditPart) childEditPart);
                                TransactionalEditingDomain domain = graphicalPart.getEditingDomain();
                                if (listeners.get(name) == null) {
                                    domain.addResourceSetListener(childH);
                                    listeners.put(name, childH);
                                } else {
                                    domain = null;
                                }
                            }
                        }
                    }
                    listenToChildren(childEditPart, childName);
                }
            }
        }
    }

    /**
	 * Sets {@link #worker}
	 * @param w
	 * Value of {@link #worker}
	 */
    public void setWorker(IModelBuilding w) {
        worker = w;
        initListeners();
        initEditPolicy(getDiagramEditPart());
    }

    /**
	 * Initializes the edit policy for an edit part (recursive)
	 * @param p
	 * Edit part for which the edit policy must be initialized
	 */
    private void initEditPolicy(EditPart p) {
        if (p != null) {
            IElementHandling elH = findHandler(p);
            ensureEditPolicy(p, elH);
            List<?> children = p.getChildren();
            if (children != null) {
                Object o = null;
                for (int i = 0; i < children.size(); i++) {
                    o = children.get(i);
                    if (o instanceof EditPart) {
                        initEditPolicy((EditPart) o);
                    }
                }
            }
        }
    }

    /**
	 * Finds the element handler for an edit part
	 * @param p
	 * Edit part for which a handler must be found
	 * @return Handler corresponding to the given edit part
	 */
    public IElementHandling findHandler(EditPart p) {
        Class classSel = null;
        Property propSel = null;
        Operation opSel = null;
        Package packSel = null;
        String name = new String();
        if (p.getModel() instanceof Node) {
            Node model = (Node) p.getModel();
            if (model.getElement() instanceof Class) {
                classSel = (Class) model.getElement();
            } else if (model.getElement() instanceof Operation) {
                opSel = (Operation) model.getElement();
                classSel = opSel.getClass_();
            } else if (model.getElement() instanceof Property) {
                propSel = (Property) model.getElement();
                classSel = propSel.getClass_();
            } else if (model.getElement() instanceof Package) {
                packSel = (Package) model.getElement();
            }
        } else if (p.getModel() instanceof DiagramImpl) {
            EObject el = ((DiagramImpl) p.getModel()).getElement();
            if (el instanceof Package) {
                packSel = (Package) el;
            }
        }
        if (classSel != null) {
            name = ClassHandler.resolveFullyQualifiedName(classSel);
            if (propSel != null) {
                name += '#' + propSel.getName();
            } else if (opSel != null) {
                name += '#' + opSel.getName();
            }
        } else if (packSel != null) {
            name = PackageHandler.resolveFullyQualifiedName(packSel);
        }
        IElementHandling elH = getElementHandler(name);
        return elH;
    }

    /** Diagram and model files removal runnable */
    class FilesRemover implements IRunnableWithProgress {

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            IResource modelResource = worker.getModelHandler().getModelWorkspaceResource();
            IResource classDiagramResource = worker.getModelHandler().getClassDiagramWorkspaceResource();
            try {
                if (classDiagramResource != null) {
                    classDiagramResource.delete(true, monitor);
                }
                if (modelResource != null) {
                    modelResource.delete(true, monitor);
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void closeEditor(boolean save) {
        if ((listeners != null) && (editingDomain != null)) {
            Iterator<String> domains = listeners.keySet().iterator();
            String domain = null;
            while (domains.hasNext()) {
                domain = domains.next();
                if (domain != null) {
                    ResourceSetListener listener = listeners.get(domain);
                    editingDomain.removeResourceSetListener(listener);
                }
            }
        }
        if (editPolicies != null) {
            Iterator<EditPart> parts = editPolicies.keySet().iterator();
            EditPart part = null;
            EditPolicy policy = null;
            while (parts.hasNext()) {
                part = parts.next();
                if (part != null) {
                    policy = editPolicies.get(part);
                    if (policy != null) {
                        part.removeEditPolicy(policy);
                    }
                }
            }
        }
        super.closeEditor(save);
    }

    @Override
    public void dispose() {
        super.dispose();
        if ((listeners != null) && (editingDomain != null)) {
            Iterator<String> domains = listeners.keySet().iterator();
            String domain = null;
            while (domains.hasNext()) {
                domain = domains.next();
                if (domain != null) {
                    ResourceSetListener listener = listeners.get(domain);
                    editingDomain.removeResourceSetListener(listener);
                }
            }
        }
        if (editPolicies != null) {
            Iterator<EditPart> parts = editPolicies.keySet().iterator();
            EditPart part = null;
            EditPolicy policy = null;
            while (parts.hasNext()) {
                part = parts.next();
                if (part != null) {
                    policy = editPolicies.get(part);
                    if (policy != null) {
                        part.removeEditPolicy(policy);
                    }
                }
            }
        }
        try {
            worker.getSourceWorkbenchWindow().run(false, false, new FilesRemover());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        factory = new UMLEditPartFactory();
        listeners = new Hashtable<String, ResourceSetListener>();
        editPolicies = new Hashtable<EditPart, EditPolicy>();
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        if (!initialized) {
            initialized = true;
        } else {
            String name = new String();
            IElementHandling el = worker.getModelHandler().getElementHandler(name);
            if (el instanceof IPackageHandling) {
                IPackageHandling pack = (IPackageHandling) el;
                ReverseHandler.reverse(true, pack.getJavaElement(), worker.getSourceWorkbenchWindow(), worker.getSourceViewId());
            }
        }
    }

    @Override
    public void createGraphicalViewer(Composite parent) {
        super.createGraphicalViewer(parent);
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if ((part.equals(getEditorSite().getPart())) && (worker != null)) {
            IWorkbenchWindow window = worker.getSourceWorkbenchWindow();
            if ((selection instanceof StructuredSelection) && (window != null)) {
                IWorkbenchPage page = window.getActivePage();
                StructuredSelection sel = (StructuredSelection) selection;
                Object first = sel.getFirstElement();
                if (first instanceof EditPart) {
                    EditPart p = (EditPart) first;
                    IElementHandling elH = findHandler(p);
                    ensureEditPolicy(p, elH);
                    if ((elH != null)) {
                        IType tp = null;
                        IJavaElement el = null;
                        if (elH instanceof IMemberHandling<?, ?>) {
                            IMemberHandling<?, ?> memH = (IMemberHandling<?, ?>) elH;
                            IClassifierHandling<?, ?> tpH = null;
                            IElementHandling cont = memH;
                            while ((!(cont instanceof IClassifierHandling<?, ?>)) && (cont instanceof IContainableElementHandling<?, ?>)) {
                                cont = ((IContainableElementHandling<?, ?>) cont).getContainerHandler();
                            }
                            if (cont instanceof IClassifierHandling<?, ?>) {
                                tpH = (IClassifierHandling<?, ?>) cont;
                                try {
                                    tp = tpH.getModelHandler().getJavaProject().findType(tpH.getFullName());
                                    if ((memH instanceof IAttributeHandling) && (tp != null)) {
                                        el = tp.getField(memH.getSimpleName());
                                    } else if ((memH instanceof IMethodHandling) && (tp != null)) {
                                    } else if (memH.equals(tpH)) {
                                        el = tpH.getJavaElement();
                                    } else if (memH instanceof IClassifierHandling<?, ?>) {
                                    }
                                } catch (JavaModelException e) {
                                    UMLDiagramEditorPlugin.getInstance().logError("Unable find Java Element for " + memH, e);
                                }
                            }
                        } else if (elH instanceof IPackageHandling) {
                            IPackageHandling packH = ((IPackageHandling) elH);
                            IModelHandling modelH = packH.getModelHandler();
                            if ((modelH != null)) {
                                el = packH.getJavaElement();
                            }
                        }
                        StructuredSelection javaSelection = null;
                        IWorkbenchPart vPart = null;
                        if (el != null) {
                            javaSelection = new StructuredSelection(el);
                            vPart = page.findView(worker.getSourceViewId());
                            if ((vPart != null) && (!vPart.getSite().getSelectionProvider().getSelection().equals(javaSelection))) {
                                vPart.getSite().getSelectionProvider().setSelection(javaSelection);
                            }
                        }
                    }
                }
            }
        }
    }
}
