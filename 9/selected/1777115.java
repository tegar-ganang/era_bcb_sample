package fr.insa.rennes.pelias.pcreator.action;

import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import fr.insa.rennes.pelias.framework.Chain;
import fr.insa.rennes.pelias.pcreator.Application;
import fr.insa.rennes.pelias.pcreator.editors.ChainEditor;
import fr.insa.rennes.pelias.pcreator.views.ChainNavigator;
import fr.insa.rennes.pelias.platform.PObjectNotFoundException;
import fr.insa.rennes.pelias.platform.PObjectReference;
import fr.insa.rennes.pelias.platform.PSxSObjectReference;

/**
 * 
 * @author Kévin Le Corre
 *
 */
public class DeleteChainAction extends Action implements IWorkbenchWindowActionDelegate, ISelectionListener {

    private IWorkbenchWindow window;

    private IStructuredSelection selection;

    public static final String ID = "fr.insa.rennes.pelias.pcreator.deleteChain";

    public DeleteChainAction() {
        super();
        this.setId(ID);
        this.setText("Suppression d'une chaine");
    }

    public void dispose() {
        window.getSelectionService().removeSelectionListener(this);
    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
        window.getSelectionService().addSelectionListener(this);
    }

    public void run(IAction action) {
        if (selection.getFirstElement() instanceof PObjectReference) {
            if (((PObjectReference) selection.getFirstElement()).getReferencedClass() == Chain.class) {
                if (selection.getFirstElement() instanceof PSxSObjectReference) {
                    PSxSObjectReference ref = (PSxSObjectReference) selection.getFirstElement();
                    MessageBox box = new MessageBox(window.getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
                    box.setMessage("Etes vous sûr de vouloir supprimer \"" + ref.getLabel() + " " + ref.getVersion() + "\" ?");
                    box.setText("Confirmation de suppression de chaine");
                    int res = box.open();
                    if (res == SWT.NO) {
                        return;
                    }
                    boolean reponse = avertissementChaineUtilise(ref);
                    if (reponse) {
                        Chain suppr = ref.resolve(Application.getCurrentChainRepository(), false, false, false);
                        actualiseEditeursChaines(suppr);
                        Application.getCurrentChainRepository().removeObject(ref.getId(), ref.getVersion());
                        IWorkbenchPage page = window.getActivePage();
                        IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                        for (IEditorReference refEditor : tabEditorsRefs) {
                            IEditorPart editor = refEditor.getEditor(false);
                            if (editor instanceof ChainEditor) {
                                Chain c = ((ChainEditor) editor).getChain();
                                if (c.getId().equals(ref.getId()) && c.getVersion().equals(ref.getVersion())) {
                                    page.closeEditor(editor, false);
                                }
                            }
                        }
                        System.out.println("PCREATOR - Suppression de la chaine \"" + ref.getLabel() + " " + ref.getVersion() + "\"");
                    }
                } else {
                    PObjectReference ref = (PObjectReference) selection.getFirstElement();
                    MessageBox box = new MessageBox(window.getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
                    box.setMessage("Etes vous sûr de vouloir supprimer toutes les versions de \"" + ref.getLabel() + "\" ?");
                    box.setText("Confirmation de suppression de chaine");
                    int res = box.open();
                    if (res == SWT.NO) {
                        return;
                    }
                    List<PSxSObjectReference> listeVersion = Application.getCurrentChainRepository().enumerateObjectVersions(ref.getId());
                    for (PSxSObjectReference reference : listeVersion) {
                        boolean reponse = avertissementChaineUtilise(reference);
                        if (reponse) {
                            Chain suppr = reference.resolve(Application.getCurrentChainRepository(), false, false, false);
                            actualiseEditeursChaines(suppr);
                            Application.getCurrentChainRepository().removeObject(reference.getId(), reference.getVersion());
                            IWorkbenchPage page = window.getActivePage();
                            IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                            for (IEditorReference refEditor : tabEditorsRefs) {
                                IEditorPart editor = refEditor.getEditor(false);
                                if (editor instanceof ChainEditor) {
                                    Chain c = ((ChainEditor) editor).getChain();
                                    if (c.getId().equals(reference.getId()) && c.getVersion().equals(reference.getVersion())) {
                                        page.closeEditor(editor, false);
                                    }
                                }
                            }
                        }
                    }
                    System.out.println("PCREATOR - Suppression de toutes les versions de la chaine \"" + ref.getLabel() + "\"");
                }
                ((ChainNavigator) window.getActivePage().findView(ChainNavigator.ID)).getTreeViewer().refresh();
            }
        }
    }

    public void selectionChanged(IAction action, ISelection incoming) {
        if (incoming instanceof IStructuredSelection) {
            this.selection = (IStructuredSelection) incoming;
            if (selection.size() == 1 && selection.getFirstElement() instanceof PObjectReference) {
                if (((PObjectReference) selection.getFirstElement()).getReferencedClass() == Chain.class) {
                    action.setEnabled(true);
                } else {
                    action.setEnabled(false);
                }
            } else {
                action.setEnabled(false);
            }
        } else {
            action.setEnabled(false);
        }
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    }

    private boolean avertissementChaineUtilise(PSxSObjectReference ref) {
        Chain chaine = ref.resolve(Application.getCurrentChainRepository(), false, false);
        if (chaine == null) {
            MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.ICON_ERROR);
            messageBox.setText("Erreur");
            messageBox.setMessage("Impossible de supprimer la chaine car celle-ci n'existe plus.");
            messageBox.open();
            return false;
        }
        try {
            List<PObjectReference> list = Application.getCurrentChainRepository().getObjectRegisteredConsumers(chaine.getId(), chaine.getVersion(), false);
            if (list.size() > 0) {
                MessageBox box = new MessageBox(window.getShell(), SWT.YES | SWT.NO | SWT.ICON_INFORMATION);
                String listChain = "";
                for (PObjectReference reference : list) {
                    Chain c = reference.resolve(Application.getCurrentChainRepository(), false, false);
                    if (c != null) listChain += c.getLabel() + " " + c.getVersion() + "\n";
                }
                box.setMessage("La chaîne \"" + chaine.getLabel() + " " + chaine.getVersion() + "\" est utilisée par les chaînes suivantes :\n\n" + listChain + "\nÊtes-vous sûr de vouloir continuer ?  (si oui, il est préférable d'éditer à nouveau ces chaînes pour éviter tout conflit)");
                box.setText("Avertissement de chaîne utilisée");
                int res = box.open();
                if (res == SWT.NO) {
                    return false;
                }
                return true;
            }
        } catch (PObjectNotFoundException e) {
            System.out.println("Aucune chaîne n'utilise ce service");
        }
        return true;
    }

    public void actualiseEditeursChaines(Chain oldChain) {
        try {
            List<PObjectReference> chaineAMAJ = Application.getCurrentChainRepository().getObjectRegisteredConsumers(oldChain.getId(), oldChain.getVersion(), false);
            if (chaineAMAJ.size() > 0) {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                for (IEditorReference refEditor : tabEditorsRefs) {
                    IEditorPart editor = refEditor.getEditor(false);
                    if (editor instanceof ChainEditor) {
                        Chain c = ((ChainEditor) editor).getChain();
                        for (PObjectReference ref : chaineAMAJ) {
                            Chain dep = ref.resolve(Application.getCurrentChainRepository(), false, false);
                            if (dep != null && dep.getId().equals(c.getId()) && dep.getVersion().equals(c.getVersion())) {
                                System.out.println("Cet éditeur " + editor.getTitle() + " utilise notre chaine");
                                ((ChainEditor) editor).initializeGraphicalViewer();
                            }
                        }
                    }
                }
            }
        } catch (PObjectNotFoundException e) {
            e.printStackTrace();
        }
    }
}
