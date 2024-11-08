package fr.insa.rennes.pelias.pcreator.views;

import java.util.List;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import fr.insa.rennes.pelias.framework.Chain;
import fr.insa.rennes.pelias.framework.Service;
import fr.insa.rennes.pelias.pcreator.Application;
import fr.insa.rennes.pelias.pcreator.action.OpenServiceAction;
import fr.insa.rennes.pelias.pcreator.editors.ChainEditor;
import fr.insa.rennes.pelias.pcreator.editors.ChainEditorInput;
import fr.insa.rennes.pelias.pcreator.editors.ServiceEditor;
import fr.insa.rennes.pelias.pcreator.editors.ServiceEditorInput;
import fr.insa.rennes.pelias.pcreator.wizards.NewChainWizard;
import fr.insa.rennes.pelias.pcreator.wizards.NewServiceWizard;
import fr.insa.rennes.pelias.platform.ISxSRepository;
import fr.insa.rennes.pelias.platform.PObjectNotFoundException;
import fr.insa.rennes.pelias.platform.PObjectReference;
import fr.insa.rennes.pelias.platform.PSxSObjectReference;

/**
 * 
 * @author otilia damian
 *
 */
public class ServiceNavigator extends Navigator<Object> {

    public static final String ID = "fr.insa.rennes.pelias.pcreator.views.services";

    /**
	 * Action de création d'un service
	 */
    private Action newServiceAction = null;

    /**
	 * Action de suppression d'un service
	 */
    private Action deleteServiceAction = null;

    /**
	 * Action de sauvegarde d'un service
	 */
    private Action saveServiceAction = null;

    /**
	 * Action du passage au tri par ordre alphabetique
	 */
    private Action triAlphaAction = null;

    /**
	 * Action du passage au tri par types de services
	 */
    private Action triTypesAction = null;

    /**
	 * Action du passage au tri par dates
	 */
    private Action triDatesAction = null;

    private IAdapterFactory adapterFactory = new PCreatorAdapterFactory() {
    };

    /**
	 * constructeur par default
	 */
    public ServiceNavigator() {
        super();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        getTreeViewer().setInput(PCreatorAdapterFactory.serviceROOT);
        getTreeViewer().addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                OpenServiceAction openAction = new OpenServiceAction();
                openAction.init(getSite().getWorkbenchWindow());
                openAction.selectionChanged(openAction, (IStructuredSelection) event.getSelection());
                openAction.run(openAction);
            }
        });
        Platform.getAdapterManager().registerAdapters(adapterFactory, PObjectReference.class);
        createActions();
        hookContextMenu();
    }

    @Override
    public void dispose() {
        Platform.getAdapterManager().unregisterAdapters(adapterFactory);
        super.dispose();
    }

    @Override
    public void setFocus() {
        IContextService contextService = (IContextService) getSite().getService(IContextService.class);
        contextService.activateContext("fr.insa.rennes.pelias.pcreator.serviceNavigatorContext");
    }

    /**
	 * Méthode qui crée le menu contextuel
	 */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                ServiceNavigator.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(getTreeViewer().getControl());
        getTreeViewer().getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, getTreeViewer());
    }

    /**
	 * Méthode qui remplit le menu contextuel
	 * @param manager
	 */
    private void fillContextMenu(IMenuManager manager) {
        manager.add(newServiceAction);
        manager.add(deleteServiceAction);
        manager.add(saveServiceAction);
        manager.add(triAlphaAction);
        manager.add(triTypesAction);
        manager.add(triDatesAction);
    }

    /**
	 * Méthode qui crée et initialise toutes les actions
	 */
    private void createActions() {
        newServiceAction = new Action() {

            public void run() {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
                Wizard wizard = new NewServiceWizard();
                WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
                dialog.open();
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        newServiceAction.setText("Nouveau Service");
        newServiceAction.setToolTipText("Nouveau Service tool tip");
        deleteServiceAction = new Action() {

            public void run() {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
                IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
                if (selection.getFirstElement() instanceof PObjectReference) {
                    if (((PObjectReference) selection.getFirstElement()).getReferencedClass() == Service.class) {
                        if (selection.getFirstElement() instanceof PSxSObjectReference) {
                            PSxSObjectReference ref = (PSxSObjectReference) selection.getFirstElement();
                            MessageBox box = new MessageBox(window.getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
                            box.setMessage("Etes vous sûr de vouloir supprimer \"" + ref.getLabel() + " " + ref.getVersion() + "\" ?");
                            box.setText("Confirmation de suppression de service");
                            int res = box.open();
                            if (res == SWT.NO) {
                                return;
                            }
                            boolean reponse = avertissementServiceUtilise(ref);
                            if (reponse) {
                                Service suppr = ref.resolve(Application.getCurrentServiceRepository(), false, false, false);
                                actualiseEditeursChaines(suppr);
                                Application.getCurrentServiceRepository().removeObject(ref.getId(), ref.getVersion());
                                IWorkbenchPage page = window.getActivePage();
                                IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                                for (IEditorReference refEditor : tabEditorsRefs) {
                                    IEditorPart editor = refEditor.getEditor(false);
                                    if (editor instanceof ServiceEditor) {
                                        Service s = ((ServiceEditor) editor).getService();
                                        if (s.getId().equals(ref.getId()) && s.getVersion().equals(ref.getVersion())) {
                                            page.closeEditor(editor, false);
                                        }
                                    }
                                }
                                System.out.println("PCREATOR - Suppression du service \"" + ref.getLabel() + " " + ref.getVersion() + "\"");
                            }
                        } else {
                            PObjectReference ref = (PObjectReference) selection.getFirstElement();
                            List<PSxSObjectReference> listeVersion = Application.getCurrentServiceRepository().enumerateObjectVersions(ref.getId());
                            MessageBox box = new MessageBox(window.getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
                            box.setMessage("Etes vous sûr de vouloir supprimer toutes les versions de \"" + ref.getLabel() + "\" ?");
                            box.setText("Confirmation de suppression de service");
                            int res = box.open();
                            if (res == SWT.NO) {
                                return;
                            }
                            for (PSxSObjectReference reference : listeVersion) {
                                boolean reponse = avertissementServiceUtilise(reference);
                                if (reponse) {
                                    Service suppr = reference.resolve(Application.getCurrentServiceRepository(), false, false, false);
                                    actualiseEditeursChaines(suppr);
                                    Application.getCurrentServiceRepository().removeObject(reference.getId(), reference.getVersion());
                                    IWorkbenchPage page = window.getActivePage();
                                    IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                                    for (IEditorReference refEditor : tabEditorsRefs) {
                                        IEditorPart editor = refEditor.getEditor(false);
                                        if (editor instanceof ServiceEditor) {
                                            Service s = ((ServiceEditor) editor).getService();
                                            if (s.getId().equals(reference.getId()) && s.getVersion().equals(reference.getVersion())) {
                                                page.closeEditor(editor, false);
                                            }
                                        }
                                    }
                                }
                            }
                            System.out.println("PCREATOR - Suppression de toutes les versions du service \"" + ref.getLabel() + "\"");
                        }
                        ((ServiceNavigator) window.getActivePage().findView(ServiceNavigator.ID)).getTreeViewer().refresh();
                    }
                }
            }

            private boolean avertissementServiceUtilise(PSxSObjectReference ref) {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
                Service s = ref.resolve(Application.getCurrentServiceRepository(), false, false);
                if (s == null) {
                    MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.ICON_ERROR);
                    messageBox.setText("Erreur");
                    messageBox.setMessage("Impossible de supprimer le service car celui-ci n'existe plus.");
                    messageBox.open();
                    return false;
                }
                try {
                    List<PObjectReference> list = Application.getCurrentServiceRepository().getObjectRegisteredConsumers(s.getId(), s.getVersion(), false);
                    if (list.size() > 0) {
                        MessageBox box = new MessageBox(window.getShell(), SWT.YES | SWT.NO | SWT.ICON_INFORMATION);
                        String listChain = "";
                        for (PObjectReference reference : list) {
                            Chain c = reference.resolve(Application.getCurrentChainRepository(), false, false);
                            if (c != null) listChain += c.getLabel() + " " + c.getVersion() + "\n";
                        }
                        box.setMessage("Le service \"" + s.getLabel() + " " + s.getVersion() + "\" est utilisé par les chaînes suivantes :\n\n" + listChain + "\nÊtes-vous sûr de vouloir continuer ? (si oui, il est préférable d'éditer à nouveau ces chaînes pour éviter tout conflit)");
                        box.setText("Avertissement de service utilisé");
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

            public void actualiseEditeursChaines(Service oldService) {
                try {
                    List<PObjectReference> chaineAMAJ = Application.getCurrentServiceRepository().getObjectRegisteredConsumers(oldService.getId(), oldService.getVersion(), false);
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
                                        System.out.println("Cet éditeur " + editor.getTitle() + " utilise notre service");
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

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        deleteServiceAction.setText("Supprimer Service");
        deleteServiceAction.setToolTipText("Supprimer Service tool tip");
        saveServiceAction = new Action() {

            public void run() {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
                IEditorPart editeurActif = window.getActivePage().getActiveEditor();
                if (editeurActif instanceof ServiceEditor) {
                    if (((ServiceEditor) editeurActif).isDirty()) {
                        ISxSRepository<Service> oldRepository = ((ServiceEditor) editeurActif).getRepository();
                        boolean major = ((ServiceEditor) editeurActif).isMajor();
                        boolean minor = ((ServiceEditor) editeurActif).isMinor();
                        editeurActif.doSave(null);
                        if (!((ServiceEditor) editeurActif).getKeepVersion().getSelection() && (major || minor)) {
                            try {
                                Service newService = ((ServiceEditor) editeurActif).getService();
                                if (Application.getCurrentServiceRepository() == oldRepository) {
                                    ServiceEditor newEditor = (ServiceEditor) window.getActivePage().openEditor(new ServiceEditorInput(newService.getSelfSxSReference()), ServiceEditor.ID);
                                    window.getActivePage().activate(newEditor);
                                    IWorkbenchPage page = window.getActivePage();
                                    IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                                    for (IEditorReference refEditor : tabEditorsRefs) {
                                        IEditorPart editor = refEditor.getEditor(false);
                                        if ((editor instanceof ServiceEditor) && (!editor.equals(newEditor))) {
                                            Service s = ((ServiceEditor) editor).getService();
                                            if (s.getId().equals(newService.getId()) && s.getVersion().equals(newService.getVersion())) {
                                                page.closeEditor(editor, false);
                                            }
                                        }
                                    }
                                    newEditor.getKeepVersion().setSelection(true);
                                } else {
                                    if (Application.isConnected()) {
                                        System.out.println("PCREATOR - Veuillez vous déconnecter pour ouvrir le nouveau service");
                                        MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.ICON_INFORMATION);
                                        messageBox.setText("Information");
                                        messageBox.setMessage("Nouveau service créé avec succès. Veuillez vous déconnecter pour l'ouvrir.");
                                        messageBox.open();
                                    } else {
                                        System.out.println("PCREATOR - Veuillez vous connecter pour ouvrir le nouveau service");
                                        MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.ICON_INFORMATION);
                                        messageBox.setText("Information");
                                        messageBox.setMessage("Nouveau service créé avec succès. Veuillez vous connecter pour l'ouvrir.");
                                        messageBox.open();
                                    }
                                }
                                window.getActivePage().closeEditor(editeurActif, false);
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("PCREATOR - Aucune modification à sauvegarder");
                    }
                }
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        saveServiceAction.setText("Enregistrer Service");
        saveServiceAction.setToolTipText("Enregistrer Service tool tip");
        triAlphaAction = new Action() {

            public void run() {
                if (sortType != 0) alphaSwitch = 0; else alphaSwitch = (alphaSwitch + 1) % 2;
                sortType = 0;
                refresh();
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        triAlphaAction.setText("Tri par ordre alphabétique");
        triAlphaAction.setToolTipText("Tri par ordre alphabétique tool tip");
        triTypesAction = new Action() {

            public void run() {
                if (sortType != 1) typeSwitch = 0; else typeSwitch = (typeSwitch + 1) % 2;
                sortType = 1;
                refresh();
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        triTypesAction.setText("Tri par genres de services");
        triTypesAction.setToolTipText("Tri par genres de services tool tip");
        triDatesAction = new Action() {

            public void run() {
                if (sortType != 2) dateSwitch = 0; else dateSwitch = (dateSwitch + 1) % 2;
                sortType = 2;
                refresh();
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
        triDatesAction.setText("Tri par dates de créations");
        triDatesAction.setToolTipText("Tri par dates de créations tool tip");
    }
}
