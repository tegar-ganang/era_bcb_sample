package de.mpiwg.vspace.diagram.part;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.validation.model.EvaluationMode;
import org.eclipse.emf.validation.model.IConstraintStatus;
import org.eclipse.emf.validation.service.IBatchValidator;
import org.eclipse.emf.validation.service.ModelValidationService;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gmf.runtime.diagram.ui.OffscreenEditPartFactory;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocumentProvider;
import org.eclipse.gmf.runtime.emf.core.util.EMFCoreUtil;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import de.mpiwg.vspace.diagram.providers.ExhibitionValidationProvider;
import de.mpiwg.vspace.diagram.providers.ValidationResultProvider;
import de.mpiwg.vspace.diagram.util.PropertyHandler;
import de.mpiwg.vspace.extension.ExceptionHandlingService;

/**
 * @generated
 */
public class ValidateAction extends Action {

    /**
	 * @generated
	 */
    private IWorkbenchPage page;

    /**
	 * @generated
	 */
    public ValidateAction(IWorkbenchPage page) {
        setText(Messages.ValidateActionMessage);
        this.page = page;
    }

    private static Diagnostic diagnostic;

    /**
	 * @generated NOT
	 */
    public void run() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null) return;
        IEditorPart[] editors = page.getEditors();
        if (editors == null) return;
        for (IEditorPart editor : editors) {
            if (editor instanceof IDiagramWorkbenchPart) {
                final IDiagramWorkbenchPart part = (IDiagramWorkbenchPart) editor;
                try {
                    if (editor instanceof ExhibitionDiagramEditor) {
                        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                            public void run() {
                                ValidationRunnable runnable = new ValidationRunnable(part);
                                try {
                                    new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).run(true, false, runnable);
                                } catch (InvocationTargetException e) {
                                    ExceptionHandlingService.INSTANCE.handleException(e);
                                } catch (InterruptedException e) {
                                    ExceptionHandlingService.INSTANCE.handleException(e);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    ExceptionHandlingService.INSTANCE.handleException(e);
                    ExhibitionDiagramEditorPlugin.getInstance().logError("Validation action failed", e);
                }
            }
        }
    }

    /**
	 * @generated
	 */
    public static void runValidation(View view) {
        try {
            if (ExhibitionDiagramEditorUtil.openDiagram(view.eResource())) {
                IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                if (editorPart instanceof IDiagramWorkbenchPart) {
                    runValidation(((IDiagramWorkbenchPart) editorPart).getDiagramEditPart(), view);
                } else {
                    runNonUIValidation(view);
                }
            }
        } catch (Exception e) {
            ExhibitionDiagramEditorPlugin.getInstance().logError("Validation action failed", e);
        }
    }

    /**
	 * @generated
	 */
    public static void runNonUIValidation(View view) {
        DiagramEditPart diagramEditPart = OffscreenEditPartFactory.getInstance().createDiagramEditPart(view.getDiagram());
        runValidation(diagramEditPart, view);
    }

    /**
	 * @generated
	 */
    public static void runValidation(DiagramEditPart diagramEditPart, View view) {
        final DiagramEditPart fpart = diagramEditPart;
        final View fview = view;
        TransactionalEditingDomain txDomain = TransactionUtil.getEditingDomain(view);
        ExhibitionValidationProvider.runWithConstraints(txDomain, new Runnable() {

            public void run() {
                validate(fpart, fview);
            }
        });
    }

    /**
	 * @generated
	 */
    private static Diagnostic runEMFValidator(View target) {
        if (target.isSetElement() && target.getElement() != null) {
            return new Diagnostician() {

                public String getObjectLabel(EObject eObject) {
                    return EMFCoreUtil.getQualifiedName(eObject, true);
                }
            }.validate(target.getElement());
        }
        return Diagnostic.OK_INSTANCE;
    }

    /**
	 * @generated
	 */
    private static void validate(DiagramEditPart diagramEditPart, View view) {
        View target = view;
        ValidationMarker.removeAllMarkers(diagramEditPart.getViewer());
        Diagnostic diagnostic = runEMFValidator(view);
        createMarkers(target, diagnostic, diagramEditPart);
        IBatchValidator validator = (IBatchValidator) ModelValidationService.getInstance().newValidator(EvaluationMode.BATCH);
        validator.setIncludeLiveConstraints(true);
        if (view.isSetElement() && view.getElement() != null) {
            IStatus status = validator.validate(view.getElement());
            createMarkers(target, status, diagramEditPart);
        }
    }

    /**
	 * @generated NOT
	 */
    private static void createMarkers(View target, IStatus validationStatus, DiagramEditPart diagramEditPart) {
        if (validationStatus.isOK()) {
            return;
        }
        final IStatus rootStatus = validationStatus;
        List allStatuses = new ArrayList();
        ExhibitionDiagramEditorUtil.LazyElement2ViewMap element2ViewMap = new ExhibitionDiagramEditorUtil.LazyElement2ViewMap(diagramEditPart.getDiagramView(), collectTargetElements(rootStatus, new HashSet(), allStatuses));
        for (Iterator it = allStatuses.iterator(); it.hasNext(); ) {
            IConstraintStatus nextStatus = (IConstraintStatus) it.next();
            View view = ExhibitionDiagramEditorUtil.findView(diagramEditPart, nextStatus.getTarget(), element2ViewMap);
            addMarker(diagramEditPart.getViewer(), target, view.eResource().getURIFragment(view), EMFCoreUtil.getQualifiedName(nextStatus.getTarget(), true), nextStatus.getMessage(), nextStatus.getSeverity(), nextStatus.getTarget());
        }
    }

    /**
	 * @generated NOT
	 */
    private static void createMarkers(View target, Diagnostic emfValidationStatus, DiagramEditPart diagramEditPart) {
        if (emfValidationStatus.getSeverity() == Diagnostic.OK) {
            return;
        }
        final Diagnostic rootStatus = emfValidationStatus;
        List allDiagnostics = new ArrayList();
        ExhibitionDiagramEditorUtil.LazyElement2ViewMap element2ViewMap = new ExhibitionDiagramEditorUtil.LazyElement2ViewMap(diagramEditPart.getDiagramView(), collectTargetElements(rootStatus, new HashSet(), allDiagnostics));
        for (Iterator it = emfValidationStatus.getChildren().iterator(); it.hasNext(); ) {
            Diagnostic nextDiagnostic = (Diagnostic) it.next();
            List data = nextDiagnostic.getData();
            if (data != null && !data.isEmpty() && data.get(0) instanceof EObject) {
                EObject element = (EObject) data.get(0);
                View view = ExhibitionDiagramEditorUtil.findView(diagramEditPart, element, element2ViewMap);
                addMarker(diagramEditPart.getViewer(), target, view.eResource().getURIFragment(view), EMFCoreUtil.getQualifiedName(element, true), nextDiagnostic.getMessage(), diagnosticToStatusSeverity(nextDiagnostic.getSeverity()), element);
            }
        }
    }

    /**
	 * @generated
	 */
    private static void addMarker(EditPartViewer viewer, View target, String elementId, String location, String message, int statusSeverity) {
        if (target == null) {
            return;
        }
        new ValidationMarker(location, message, statusSeverity).add(viewer, elementId);
    }

    /**
	 * @generated NOT
	 */
    private static void addMarker(EditPartViewer viewer, View target, String elementId, String location, String message, int statusSeverity, EObject element) {
        if (target == null) {
            return;
        }
        new ValidationMarker(location, message, statusSeverity, element).add(viewer, elementId);
    }

    /**
	 * @generated
	 */
    private static int diagnosticToStatusSeverity(int diagnosticSeverity) {
        if (diagnosticSeverity == Diagnostic.OK) {
            return IStatus.OK;
        } else if (diagnosticSeverity == Diagnostic.INFO) {
            return IStatus.INFO;
        } else if (diagnosticSeverity == Diagnostic.WARNING) {
            return IStatus.WARNING;
        } else if (diagnosticSeverity == Diagnostic.ERROR || diagnosticSeverity == Diagnostic.CANCEL) {
            return IStatus.ERROR;
        }
        return IStatus.INFO;
    }

    /**
	 * @generated
	 */
    private static Set<EObject> collectTargetElements(IStatus status, Set<EObject> targetElementCollector, List allConstraintStatuses) {
        if (status instanceof IConstraintStatus) {
            targetElementCollector.add(((IConstraintStatus) status).getTarget());
            allConstraintStatuses.add(status);
        }
        if (status.isMultiStatus()) {
            IStatus[] children = status.getChildren();
            for (int i = 0; i < children.length; i++) {
                collectTargetElements(children[i], targetElementCollector, allConstraintStatuses);
            }
        }
        return targetElementCollector;
    }

    /**
	 * @generated
	 */
    private static Set<EObject> collectTargetElements(Diagnostic diagnostic, Set<EObject> targetElementCollector, List allDiagnostics) {
        List data = diagnostic.getData();
        EObject target = null;
        if (data != null && !data.isEmpty() && data.get(0) instanceof EObject) {
            target = (EObject) data.get(0);
            targetElementCollector.add(target);
            allDiagnostics.add(diagnostic);
        }
        if (diagnostic.getChildren() != null && !diagnostic.getChildren().isEmpty()) {
            for (Iterator it = diagnostic.getChildren().iterator(); it.hasNext(); ) {
                collectTargetElements((Diagnostic) it.next(), targetElementCollector, allDiagnostics);
            }
        }
        return targetElementCollector;
    }

    protected class ValidationRunnable implements IRunnableWithProgress {

        private final IDiagramWorkbenchPart editor;

        public ValidationRunnable(IDiagramWorkbenchPart editor) {
            this.editor = editor;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask(PropertyHandler.getInstance().getProperty("_validation_msg_synchronize"), 3);
                final IDocumentProvider provider = ((ExhibitionDiagramEditor) editor).getDocumentProvider();
                final IEditorInput input = ((ExhibitionDiagramEditor) editor).getEditorInput();
                if (!provider.isDeleted(input)) {
                    if (!provider.isSynchronized(input)) try {
                        provider.setProgressMonitor(monitor);
                        provider.synchronize(input);
                    } catch (CoreException e) {
                        ExceptionHandlingService.INSTANCE.handleException(e);
                    }
                }
                monitor.worked(1);
                monitor.setTaskName(PropertyHandler.getInstance().getProperty("_validation_msg_model"));
                if (!provider.isDeleted(input)) {
                    if (!provider.isSynchronized(input)) try {
                        provider.setProgressMonitor(monitor);
                        provider.synchronize(input);
                    } catch (CoreException e) {
                        ExceptionHandlingService.INSTANCE.handleException(e);
                    }
                }
                monitor.worked(1);
                monitor.setTaskName(PropertyHandler.getInstance().getProperty("_validation_msg_model"));
                new IRunnableWithProgress() {

                    public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
                        runValidation(editor.getDiagramEditPart(), editor.getDiagram());
                    }
                }.run(monitor);
                monitor.worked(1);
                monitor.setTaskName(PropertyHandler.getInstance().getProperty("_validation_msg_results"));
                ValidationResultProvider.INSTANCE.setResults(((IDiagramWorkbenchPart) editor).getDiagramGraphicalViewer(), ((IDiagramWorkbenchPart) editor).getDiagram());
                monitor.worked(1);
            } finally {
                monitor.done();
            }
        }
    }
}
